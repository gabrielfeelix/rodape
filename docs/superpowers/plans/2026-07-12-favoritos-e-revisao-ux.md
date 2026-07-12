# Favoritos + Revisão de UX — Plano de Implementação

> **Para workers agênticos:** este plano é executado pelo orquestrador (favoritos + arquivos compartilhados) e por 4 subagentes paralelos (telas independentes). Passos com checkbox `- [ ]`.

**Goal:** Adicionar favorito pessoal de verdade (♥) que aparece no Perfil, e corrigir os ~40 achados da revisão de UX (copy enganosa, botões cinza, estados vazios, features escondidas, voz PT-BR).

**Architecture:** Favorito é `(userId, bookId)` — pessoal e cross-clube. Espelha exatamente dois padrões existentes: `book_ratings` (toggle local-first + fila offline `tryRemoteOrEnqueue`) e `saved_quotes` (flow por-usuário com reload + realtime). Zero risco do 22P02: `books.id` é `uuid` e o app já gera UUID.

**Tech Stack:** Kotlin, Jetpack Compose, Room (v3→v4), Supabase (Postgrest + Realtime), fila offline própria.

## Global Constraints
- **PT-BR "você"** em todo texto novo/editado. Proibido "tu", "teu/tua", "vais", imperativo de 2ª ("conta", "tenta"). Usar "você", "seu/sua", "conte", "tente".
- Diffs mínimos; seguir o estilo do arquivo vizinho (mesma densidade de comentário, nomes, idioma dos comentários).
- Não quebrar o build. Um único build+test no fim (orquestrador).
- Nenhum subagente toca em `MainTabsScreen.kt`, `BookDetailScreen.kt`, `ShelfTabScreen.kt` (donos: orquestrador).

---

## PARTE A — Favorito de verdade (orquestrador)

### Task A1: Entidade + migração Room
**Files:** Modify `app/src/main/java/com/example/data/model/Entities.kt`; `app/src/main/java/com/example/data/db/AppDatabase.kt`

- [ ] Entities.kt — adicionar:
```kotlin
@Entity(tableName = "book_favorites", primaryKeys = ["userId", "bookId"])
data class BookFavorite(
    val userId: String,
    val bookId: String,
    val criadoEm: Long,
)
```
- [ ] AppDatabase.kt — registrar `BookFavorite::class` na lista de entities; `version = 4`; adicionar migração e registrá-la:
```kotlin
private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `book_favorites` (" +
                "`userId` TEXT NOT NULL, `bookId` TEXT NOT NULL, " +
                "`criadoEm` INTEGER NOT NULL, PRIMARY KEY(`userId`, `bookId`))"
        )
    }
}
```
`.addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)`

### Task A2: DAO
**Files:** Modify `app/src/main/java/com/example/data/db/RodapeDao.kt`
- [ ] Adicionar seção BOOK FAVORITES:
```kotlin
// ====================== BOOK FAVORITES ======================
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun upsertBookFavorites(fs: List<BookFavorite>)

@Query("DELETE FROM book_favorites WHERE userId = :userId AND bookId = :bookId")
suspend fun deleteBookFavorite(userId: String, bookId: String)

@Query("DELETE FROM book_favorites WHERE userId = :userId AND bookId NOT IN (:keepBookIds)")
suspend fun pruneBookFavoritesExcept(userId: String, keepBookIds: List<String>)

@Query("SELECT EXISTS(SELECT 1 FROM book_favorites WHERE userId = :userId AND bookId = :bookId)")
fun isBookFavoriteFlow(userId: String, bookId: String): Flow<Boolean>

@Query("SELECT b.* FROM books b INNER JOIN book_favorites f ON f.bookId = b.id WHERE f.userId = :userId ORDER BY f.criadoEm DESC")
fun favoriteBooksFlow(userId: String): Flow<List<Book>>

@Query("SELECT clubId FROM club_books WHERE bookId = :bookId LIMIT 1")
suspend fun anyClubIdForBook(bookId: String): String?

@Query("DELETE FROM book_favorites")
suspend fun clearBookFavorites()
```
- [ ] Em `clearAll()` chamar `clearBookFavorites()`.

### Task A3: Repositório (DTO + toggle + flow + replay)
**Files:** Modify `app/src/main/java/com/example/data/remote/RemoteRepository.kt`
- [ ] DTOs (perto de `// ---- book_ratings ----`):
```kotlin
// ---- book_favorites ----
@Serializable
private data class BookFavoriteDto(
    @SerialName("user_id") val userId: String,
    @SerialName("book_id") val bookId: String,
    @SerialName("created_at") val createdAt: String? = null,
)
@Serializable
private data class BookFavoriteInsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("book_id") val bookId: String,
)
```
- [ ] Métodos públicos (perto de `insertBookRating`):
```kotlin
suspend fun setBookFavorite(userId: String, bookId: String, favorite: Boolean) {
    val payload = buildJsonObject { put("userId", userId); put("bookId", bookId) }.toString()
    if (favorite) {
        dao.upsertBookFavorites(listOf(BookFavorite(userId, bookId, System.currentTimeMillis())))
        tryRemoteOrEnqueue("insert_book_favorite", payload, notifyTable = "book_favorites") {
            supabase.from("book_favorites").upsert(BookFavoriteInsertDto(userId, bookId))
        }
    } else {
        dao.deleteBookFavorite(userId, bookId)
        tryRemoteOrEnqueue("delete_book_favorite", payload, notifyTable = "book_favorites") {
            supabase.from("book_favorites").delete { filter { eq("user_id", userId); eq("book_id", bookId) } }
        }
    }
}

fun isBookFavoriteFlow(userId: String, bookId: String): Flow<Boolean> = dao.isBookFavoriteFlow(userId, bookId)

suspend fun anyClubIdForBook(bookId: String): String? = dao.anyClubIdForBook(bookId)

fun getFavoriteBooksForUserFlow(userId: String): Flow<List<Book>> {
    val reload: suspend () -> Unit = {
        runCatching {
            val list = supabase.from("book_favorites").select { filter { eq("user_id", userId) } }
                .decodeList<BookFavoriteDto>()
            dao.upsertBookFavorites(list.map { BookFavorite(it.userId, it.bookId, it.createdAt.fromIso()) })
            dao.pruneBookFavoritesExcept(userId, list.map { it.bookId })
        }
    }
    scope.launch { runCatching { reload() } }
    ensureRealtime("book_favorites", filterColumn = "user_id", filterValue = userId, reload = reload)
    return dao.favoriteBooksFlow(userId)
}
```
- [ ] Replay handlers (junto dos outros `registerHandler`):
```kotlin
registerHandler("insert_book_favorite") { j ->
    val obj = this.json.parseToJsonElement(j) as JsonObject
    supabase.from("book_favorites").upsert(BookFavoriteInsertDto(obj.str("userId"), obj.str("bookId")))
}
registerHandler("delete_book_favorite") { j ->
    val obj = this.json.parseToJsonElement(j) as JsonObject
    supabase.from("book_favorites").delete { filter { eq("user_id", obj.str("userId")); eq("book_id", obj.str("bookId")) } }
}
```

### Task A4: ViewModel
**Files:** Modify `app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt`
- [ ] Perto de `savedQuotes`:
```kotlin
val favoriteBooks: StateFlow<List<Book>> = currentUserId.flatMapLatest { userId ->
    if (userId != null) repository.getFavoriteBooksForUserFlow(userId) else flowOf(emptyList())
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), emptyList())

fun isBookFavoriteFlow(bookId: String): Flow<Boolean> =
    currentUserId.flatMapLatest { userId ->
        if (userId != null) repository.isBookFavoriteFlow(userId, bookId) else flowOf(false)
    }

fun toggleBookFavorite(bookId: String, favorite: Boolean) {
    viewModelScope.launch {
        val userId = currentUserId.value ?: return@launch
        repository.setBookFavorite(userId, bookId, favorite)
    }
}

fun openFavoriteBook(bookId: String, navigate: (String) -> Unit) {
    viewModelScope.launch {
        repository.anyClubIdForBook(bookId)?.let { selectActiveClub(it) }
        navigate(bookId)
    }
}
```

### Task A5: BookDetail — toggle ♥ no hero
**Files:** Modify `app/src/main/java/com/example/ui/screens/BookDetailScreen.kt`
- [ ] No hero `Box` (após o botão Voltar), adicionar toggle alinhado ao topo-direita:
```kotlin
val isFavorite by remember(bookId) { viewModel.isBookFavoriteFlow(bookId) }.collectAsState(initial = false)
Box(
    modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(top = 8.dp, end = 12.dp)
        .size(40.dp)
        .clip(CircleShape)
        .background(CardSurface.copy(alpha = 0.85f))
        .clickable { viewModel.toggleBookFavorite(bookId, !isFavorite) },
    contentAlignment = Alignment.Center
) {
    Icon(
        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
        contentDescription = if (isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos",
        tint = if (isFavorite) Terracota else Ink,
        modifier = Modifier.size(20.dp)
    )
}
```
Imports: `androidx.compose.material.icons.filled.Favorite`, `androidx.compose.material.icons.outlined.FavoriteBorder`.

### Task A6: Perfil — seção "Meus livros favoritos"
**Files:** Modify `app/src/main/java/com/example/ui/screens/MainTabsScreen.kt`
- [ ] `ProfileScreenTab` recebe novo param `onNavigateToBookDetail: (String) -> Unit`; caller (linha ~251) passa `onNavigateToBookDetail = onNavigateToBookDetail`.
- [ ] `val favoriteBooks by viewModel.favoriteBooks.collectAsState()`.
- [ ] Novo `item {}` entre os stat cards e "Teus clubes": header `TbSectionHeader("Meus livros favoritos")` + `LazyRow` de capas (`Cover` 92x138, título 1 linha), toque → `viewModel.openFavoriteBook(book.id, onNavigateToBookDetail)`. Vazio: texto "Toque no ♥ na página de um livro pra guardá-lo aqui." A seção aparece sempre (com vazio educativo).

### Task A7: Shelf — filtro "Favoritos" vira favorito de verdade (corrige P0-1/P0-2)
**Files:** Modify `app/src/main/java/com/example/ui/screens/ShelfTabScreen.kt`
- [ ] `val favoriteIds by viewModel.favoriteBooks.map{ set }...` (coletar `favoriteBooks` → `Set<String>` dos ids).
- [ ] `displayedBooks = if (filterBy=="Favoritos") finishedBooks.filter { it.id in favoriteIds } else finishedBooks` (remove o `>= 4.5f`).
- [ ] Empty do filtro: "Você ainda não favoritou nenhum livro. Toque no ♥ na página de um livro pra guardá-lo aqui."
- [ ] Empty geral: adicionar CTA — passar `onNavigateToSuggest` e botão "Sugerir uma leitura" (fix do achado P1). Wire do caller.

### Task A8: Migração Supabase + aplicar + verificar
**Files:** Create `supabase/migrations/0006_book_favorites.sql`
```sql
create table if not exists public.book_favorites (
  user_id uuid not null references auth.users(id) on delete cascade,
  book_id uuid not null references public.books(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_id, book_id)
);
alter table public.book_favorites enable row level security;
create policy "book_favorites_select_own" on public.book_favorites for select using (auth.uid() = user_id);
create policy "book_favorites_insert_own" on public.book_favorites for insert with check (auth.uid() = user_id);
create policy "book_favorites_delete_own" on public.book_favorites for delete using (auth.uid() = user_id);
alter publication supabase_realtime add table public.book_favorites;
```
- [ ] Aplicar via Management API (token do `.env`, nunca impresso) e verificar com `SELECT` na tabela + policies. Se a rede do sandbox bloquear, deixar o arquivo e sinalizar checkpoint pro dono rodar.

---

## PARTE B — Correções de UX

### B-orquestrador (arquivos compartilhados — MainTabs, BookDetail, Shelf)
- [ ] **Bottom nav** — rótulo sempre visível nas 5 abas; renomear "Próximo" → "Encontros". *(MainTabsScreen.kt:698-703,760-782)*
- [ ] **Home vs Livro** — unificar regra por papel: membro vê "Sugerir livro", só admin vê "Abrir votação" nos dois lugares. *(MainTabsScreen.kt:966-975 vs 1399-1431)*
- [ ] **Header 1 clube** — com 1 clube, rotular "Meus clubes" em vez de "Trocar de clube". *(MainTabsScreen.kt:574,363)*
- [ ] **Home acessibilidade** — pill e `contentDescription` com mesmo texto. *(MainTabsScreen.kt:1204-1209 vs 1262-1272)*
- [ ] **EditProfile** — desacoplar avatar do nome (salvar avatar sem nome completo) e aceitar nome único. *(MainTabsScreen.kt:2853,2942-2946)*
- [ ] **Stat "livros lidos"** — rótulo "livros lidos neste clube". *(MainTabsScreen.kt:2007-2024)*
- [ ] **Stat cards** — chevron no card navegável (frases). *(MainTabsScreen.kt:2009-2088)*
- [ ] **Sair do clube / conta** — "Sair do clube" vira link discreto, afastado de "Sair da conta". *(MainTabsScreen.kt:2510-2534)*
- [ ] **EditProfile** — header com seta de voltar. *(MainTabsScreen.kt:2755-2762)*
- [ ] **Voz** — "TUA LEITURA"→"SUA LEITURA", "Teus clubes"→"Seus clubes", etc. *(MainTabsScreen.kt:1073,2094…)*
- [ ] **Avaliações 0.0** — estado vazio "Ninguém avaliou ainda — seja o primeiro" em vez de "0.0 de 5". *(BookDetailScreen.kt:638-649)*
- [ ] **Salvar frase** — omitir referência quando capítulo vazio (nada de "Cap."). *(BookDetailScreen.kt:305)*
- [ ] **Abas do livro** — `ScrollableTabRow` e reforçar estado inativo. *(BookDetailScreen.kt:195-239)*

### B-Agente A — Onboarding/Auth (WelcomeScreen, SignUpScreen, ForgotPasswordScreen, ResetPasswordScreen, OnboardingScreen, AuthErrors, NoClubsEmptyState)
- P0 email: mostrar email + "Reenviar"/"Corrigir". · P1 Welcome CTA "Criar conta" primário no 1º uso. · P1 Forgot copy instrucional. · P1 botões cinza com regra visível/checklist de senha. · P1 Onboarding submit com caminho de erro. · P2 valor antes do cadastro. · P2 AuthErrors acentuado. · P2 "Com código" não-clicável vira label. · P2 Onboarding "Pular". · Voz "você".

### B-Agente B — Próximo/Encontros (NextTabScreen, MeetingDetailScreen)
- P0 "Teu voto" → "Remover voto"/toggle rotulado. · P1 card-herói clicável → detalhe. · P1 herói mostra "Caps N–M". · P1 ata: confirmar descarte no dismiss. · P2 CTA duplicado no empty. · P2 padronizar cabeçalhos/termos (pauta, RSVP). · P2 `hasData = meeting != null`. · Voz "você" ("Seu voto", "Trocar para este").

### B-Agente C — Discussão/Capítulos (DiscussionScreen, ManageChaptersScreen)
- P0 "Buscar online" confirma antes de sobrescrever. · P1 botão com texto "Buscar capítulos online". · P1 estado vazio de capítulos com 3 caminhos. · P1 confirmar descarte de rascunho. · P1 affordance visível de reagir. · P2 banner de falha em tom neutro/terracota. · P2 microcopy do gerador N. · P2 "Tu"→"Você", fallback "Membro". · P2 inverter ênfase da barreira de spoiler. · Voz "você".

### B-Agente D — Frases/Sugerir/Cadastro (FrasesScreen, SuggestScreen, AddBookManualScreen)
- P1 busca: gatear render por `searching` + skeleton (parar de piscar erro de rede). · P1 Frases empty CTA "Abrir livro atual". · P2 populares "toque pra selecionar" + confirmar perto. · P2 padronizar verbo (Sugerir). · P2 card de frase fallback "Livro removido". · P2 URL de capa: validar no clique com msg https. · Voz "você".

---

## PARTE C — Fechamento (orquestrador)
- [ ] `./gradlew :app:compileDebugKotlin` (ou assembleDebug) verde; corrigir erros.
- [ ] `./gradlew testDebugUnitTest` verde.
- [ ] Bump de versão (versionCode/versionName) no `app/build.gradle(.kts)`.
- [ ] Commit + push.
