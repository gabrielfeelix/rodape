# Fase 4 — Estante + Votação evoluída + polimentos · Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar tudo definido em [2026-05-23-fase4-estante-votacao-design.md](../specs/2026-05-23-fase4-estante-votacao-design.md): consertar 2 bugs visuais, mover Estante pra navbar (5 tabs), enriquecer detalhe do livro com 5 abas (Resumo wiki, Frases, Chat retrospectivo, Avaliações, Histórico) e transformar a Votação em rodadas reais com prazo, N livros e justificativa persistida.

**Architecture:** Tudo offline em Room. Migração v2 → v3 destrutiva (app ainda não publicado). 4 entidades novas (`BookSummary`, `BookRating`, `BookSuggestion`, `VotingRound`), 2 entidades alteradas (`Vote`, `ClubBook`). UI em Compose seguindo design system existente — sem novos tokens nem componentes não-essenciais. Reaproveita `Comment` para Chat retrospectivo e `SavedQuote` para Frases por livro.

**Tech Stack:** Kotlin, Jetpack Compose, Room 2.x, Coroutines/Flow, Material3. Build via `./gradlew assembleDebug` (JDK 21, Android SDK 36). Sem libs novas.

---

## Convenções deste plano

- **Build:** `./gradlew assembleDebug` — toda compilação. Se passar, OK.
- **Testes:** o projeto **não tem suite de teste unitário ativa pra ViewModel/UI** (só screenshot tests Roborazzi). Validação principal é build limpo + sanidade de behavior nas próprias screens. Quando houver lógica determinística (encerrar votação, agrupar comments por capítulo), adicionar teste unitário puro Kotlin em `app/src/test/java/com/example/`.
- **Commits:** atômicos, mensagem em português curto, formato `tipo(escopo): mensagem`. Sempre encerrar com `Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>`.
- **Sem `--no-verify`, sem `--amend`.**
- **Imports:** quando o passo modifica um composable existente, NÃO listar todos os imports — só os imports NOVOS que o passo introduz. Engineer mantém os já existentes.

---

## Task 0: Branch e linha de base

**Files:**
- Verify: working tree limpo na `master`

- [ ] **Step 0.1: Confirmar working tree limpo e pull**

Run:
```bash
git status
git pull --ff-only
```
Expected: `nothing to commit, working tree clean`, `Already up to date`.

- [ ] **Step 0.2: Confirmar build inicial passa**

Run: `./gradlew assembleDebug`  
Expected: `BUILD SUCCESSFUL`. Se falhar, parar e investigar antes de qualquer mudança.

---

## Task 1: Fix do OTP responsivo (§3.1 do spec)

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/WelcomeScreen.kt:950-1028`

- [ ] **Step 1.1: Trocar layout de 2 sub-`for` (3+3 com `Text("-")`) por 1 `Row` único com 6 campos `weight(1f)` + spacer no meio**

Localizar em [WelcomeScreen.kt:950](app/src/main/java/com/example/ui/screens/WelcomeScreen.kt#L950) o bloco que começa com:

```kotlin
Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
) {
    for (i in 0..2) {
        OutlinedTextField(
            ...
            modifier = Modifier
                .width(44.dp)
                .height(56.dp)
                .focusRequester(focusRequesters[i]),
            ...
        )
    }

    Text("-", style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))

    for (i in 3..5) {
        OutlinedTextField(
            ...
        )
    }
}
```

Substituir o Row inteiro por:

```kotlin
Row(
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth()
) {
    for (i in 0..5) {
        OutlinedTextField(
            value = otpValues[i],
            onValueChange = { value ->
                if (value.length <= 1) {
                    otpValues[i] = value
                    if (value.isNotEmpty() && i < 5) {
                        focusRequesters[i + 1].requestFocus()
                    }
                }
            },
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .focusRequester(focusRequesters[i]),
            shape = RoundedCornerShape(12.dp),
            placeholder = {
                Text(
                    "-",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            },
            textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Terracota,
                unfocusedBorderColor = Divider,
                focusedContainerColor = Cream,
                unfocusedContainerColor = Cream
            )
        )
        if (i == 2) {
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}
```

Notas:
- Removido `Text("-")` central; substituído por `Spacer` de 8dp após o 3º campo.
- Largura agora é `weight(1f)` em vez de 44.dp fixo.
- `textStyle` baixou de `titleLarge` pra `titleMedium` (caber melhor em 360dp).
- Lógica de auto-focus avança igual (i < 5).

- [ ] **Step 1.2: Reduzir padding interno do card OTP de 24dp pra 16dp**

No mesmo arquivo, localizar o `Surface` que envolve esse Row (alguns níveis acima, contém `Column(modifier = Modifier.padding(24.dp), ...)` em [WelcomeScreen.kt:934](app/src/main/java/com/example/ui/screens/WelcomeScreen.kt#L934)).

Trocar:
```kotlin
modifier = Modifier.padding(24.dp),
```
por:
```kotlin
modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
```

Isso mantém respiro vertical mas libera 16dp lateral pra OTP caber.

- [ ] **Step 1.3: Build**

Run: `./gradlew assembleDebug`  
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 1.4: Commit**

```bash
git add app/src/main/java/com/example/ui/screens/WelcomeScreen.kt
git commit -m "$(cat <<'EOF'
fix(join-club): OTP de 6 dígitos cabe em telas estreitas

Substitui largura fixa de 44dp por weight(1f) num único Row, troca o
separador "-" por Spacer de 8dp e reduz padding lateral do card. Cabe em
360dp width sem clipar o último campo.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: Fix do card "Próximo Encontro" — DOMINGO cortado (§3.2)

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/MainTabsScreen.kt:556-613`

- [ ] **Step 2.1: Adicionar maxLines/ellipsis no Text do dia e do mês; aumentar width da coluna**

Localizar em [MainTabsScreen.kt:582-613](app/src/main/java/com/example/ui/screens/MainTabsScreen.kt#L582-L613) a `Column` da coluna de data (começa com `Column(modifier = Modifier.width(72.dp), ...)`).

Trocar:
```kotlin
Column(
    modifier = Modifier.width(72.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Text(
        text = dayNameStr,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            color = Cream.copy(alpha = 0.5f),
            letterSpacing = 0.5.sp
        )
    )
```

por:

```kotlin
Column(
    modifier = Modifier.width(80.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Text(
        text = dayNameStr,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            color = Cream.copy(alpha = 0.5f),
            letterSpacing = 0.5.sp
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
```

E mais abaixo, no `Text` do `finalMonthLabel` (mesma coluna):

```kotlin
Text(
    text = finalMonthLabel,
    style = MaterialTheme.typography.bodySmall.copy(
        fontSize = 11.sp,
        color = Cream.copy(alpha = 0.4f)
    )
)
```

substituir por:

```kotlin
Text(
    text = finalMonthLabel,
    style = MaterialTheme.typography.bodySmall.copy(
        fontSize = 11.sp,
        color = Cream.copy(alpha = 0.4f)
    ),
    maxLines = 1,
    overflow = TextOverflow.Ellipsis
)
```

- [ ] **Step 2.2: Build**

Run: `./gradlew assembleDebug`  
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2.3: Commit**

```bash
git add app/src/main/java/com/example/ui/screens/MainTabsScreen.kt
git commit -m "$(cat <<'EOF'
fix(home): card de próximo encontro nunca quebra rótulo do dia

Adiciona maxLines/ellipsis no Text do dia e do mês e aumenta a coluna
de data de 72dp para 80dp.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: Novas entidades no modelo de dados (§9.1)

**Files:**
- Modify: `app/src/main/java/com/example/data/model/Entities.kt`

- [ ] **Step 3.1: Adicionar 4 novas data classes ao final do arquivo Entities.kt**

Abrir [Entities.kt](app/src/main/java/com/example/data/model/Entities.kt) e adicionar ao final, depois de `SavedQuote`:

```kotlin
@Entity(tableName = "book_summaries", primaryKeys = ["bookId", "clubId"])
data class BookSummary(
    val bookId: String,
    val clubId: String,
    val texto: String,
    val lastEditorId: String,
    val updatedAt: Long
)

@Entity(tableName = "book_ratings", primaryKeys = ["bookId", "clubId", "userId"])
data class BookRating(
    val bookId: String,
    val clubId: String,
    val userId: String,
    val stars: Int,
    val comment: String,
    val updatedAt: Long
)

@Entity(tableName = "book_suggestions")
data class BookSuggestion(
    @PrimaryKey val id: String,
    val clubId: String,
    val bookId: String,
    val suggestedByUserId: String,
    val justificativa: String,
    val criadoEm: Long
)

@Entity(tableName = "voting_rounds")
data class VotingRound(
    @PrimaryKey val id: String,
    val clubId: String,
    val criadoPor: String,
    val abertaEm: Long,
    val fechaEm: Long,
    val nLivros: Int,
    val cadencia: String,
    val status: String,
    val vencedoresJson: String
)
```

- [ ] **Step 3.2: Alterar `Vote` para adicionar `votingRoundId: String?`**

Localizar a entidade Vote em [Entities.kt:85-90](app/src/main/java/com/example/data/model/Entities.kt#L85-L90):

```kotlin
@Entity(tableName = "votes", primaryKeys = ["clubBookId", "userId"])
data class Vote(
    val clubBookId: String,
    val userId: String,
    val votedAt: Long
)
```

Substituir por:

```kotlin
@Entity(tableName = "votes", primaryKeys = ["clubBookId", "userId"])
data class Vote(
    val clubBookId: String,
    val userId: String,
    val votedAt: Long,
    val votingRoundId: String?
)
```

- [ ] **Step 3.3: Alterar `ClubBook` para adicionar `dataEncontro: Long?`**

Localizar a entidade ClubBook em [Entities.kt:44-50](app/src/main/java/com/example/data/model/Entities.kt#L44-L50):

```kotlin
@Entity(tableName = "club_books", primaryKeys = ["clubId", "bookId"])
data class ClubBook(
    val clubId: String,
    val bookId: String,
    val status: String,
    val ordem: Int
)
```

Substituir por:

```kotlin
@Entity(tableName = "club_books", primaryKeys = ["clubId", "bookId"])
data class ClubBook(
    val clubId: String,
    val bookId: String,
    val status: String,
    val ordem: Int,
    val dataEncontro: Long?
)
```

- [ ] **Step 3.4: Build (vai quebrar nos call sites de Vote/ClubBook — esperado)**

Run: `./gradlew assembleDebug`  
Expected: **FAIL** com erros do tipo `No value passed for parameter 'votingRoundId'` em chamadas a `Vote(...)` e `No value passed for parameter 'dataEncontro'` em chamadas a `ClubBook(...)`. Isso é esperado — será resolvido na Task 4.

---

## Task 4: Atualizar call-sites quebrados de Vote/ClubBook

**Files:**
- Modify: `app/src/main/java/com/example/data/repository/TramabookRepository.kt` (seed)
- Modify: `app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt` (`voteForBook`)

- [ ] **Step 4.1: Atualizar todas as construções de `Vote` no seed**

Em [TramabookRepository.kt:244-248](app/src/main/java/com/example/data/repository/TramabookRepository.kt#L244-L248), localizar:

```kotlin
dao.insertVote(Vote("sug_1", "user_marina", System.currentTimeMillis()))
dao.insertVote(Vote("sug_1", "user_lucas", System.currentTimeMillis()))
dao.insertVote(Vote("sug_1", "user_sofia", System.currentTimeMillis()))
dao.insertVote(Vote("sug_2", "user_bia", System.currentTimeMillis()))
dao.insertVote(Vote("sug_2", "user_joao", System.currentTimeMillis()))
```

Substituir por (adicionar `"round_mari_1"` como `votingRoundId` — a rodada será criada na Task 9):

```kotlin
dao.insertVote(Vote("sug_1", "user_marina", System.currentTimeMillis(), "round_mari_1"))
dao.insertVote(Vote("sug_1", "user_lucas", System.currentTimeMillis(), "round_mari_1"))
dao.insertVote(Vote("sug_1", "user_sofia", System.currentTimeMillis(), "round_mari_1"))
dao.insertVote(Vote("sug_2", "user_bia", System.currentTimeMillis(), "round_mari_1"))
dao.insertVote(Vote("sug_2", "user_joao", System.currentTimeMillis(), "round_mari_1"))
```

- [ ] **Step 4.2: Atualizar todas as construções de `ClubBook` no seed**

No mesmo arquivo, todos os `ClubBook(...)` precisam de `dataEncontro`. Buscar e substituir todos os `dao.insertClubBook(ClubBook(...))` no seed:

- Linha [TramabookRepository.kt:161](app/src/main/java/com/example/data/repository/TramabookRepository.kt#L161): `dao.insertClubBook(ClubBook("club_mari", "book_metamorfose", "current", 1))` → adicionar `, null` no final, antes do `)`:
  ```kotlin
  dao.insertClubBook(ClubBook("club_mari", "book_metamorfose", "current", 1, null))
  ```
- Linhas [239-241](app/src/main/java/com/example/data/repository/TramabookRepository.kt#L239-L241) (suggested):
  ```kotlin
  dao.insertClubBook(ClubBook("club_mari", "sug_1", "suggested", 1, null))
  dao.insertClubBook(ClubBook("club_mari", "sug_2", "suggested", 2, null))
  dao.insertClubBook(ClubBook("club_mari", "sug_3", "suggested", 3, null))
  ```
- Linhas [259-261](app/src/main/java/com/example/data/repository/TramabookRepository.kt#L259-L261) (finished). Aqui sim queremos preencher `dataEncontro` com datas reais espaçadas no passado:
  ```kotlin
  val agora = System.currentTimeMillis()
  val umMes = 30L * 24 * 60 * 60 * 1000L
  dao.insertClubBook(ClubBook("club_mari", "fin_1", "finished", 1, agora - 3 * umMes))
  dao.insertClubBook(ClubBook("club_mari", "fin_2", "finished", 2, agora - 2 * umMes))
  dao.insertClubBook(ClubBook("club_mari", "fin_3", "finished", 3, agora - 1 * umMes))
  ```

- [ ] **Step 4.3: Atualizar `seedNewClubData` no ViewModel**

Em [MainViewModel.kt:255-256](app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt#L255-L256), localizar:

```kotlin
repository.insertClubBook(ClubBook(clubId, b.id, "current", 1))
```

Trocar por:

```kotlin
repository.insertClubBook(ClubBook(clubId, b.id, "current", 1, null))
```

- [ ] **Step 4.4: Atualizar `voteForBook` no ViewModel pra passar `votingRoundId` (placeholder até Task 9)**

Em [MainViewModel.kt:359-370](app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt#L359-L370), localizar:

```kotlin
fun voteForBook(bookId: String) {
    viewModelScope.launch {
        val userId = currentUserId.value ?: "user_voce"
        val clubId = activeClubId.value ?: return@launch
        
        // clear user previous votes in this club first
        repository.clearVotesForUserInClub(userId, clubId)
        
        // Register vote
        repository.insertVote(Vote(bookId, userId, System.currentTimeMillis()))
    }
}
```

Trocar por (versão temporária — será refeita na Task 11):

```kotlin
fun voteForBook(bookId: String) {
    viewModelScope.launch {
        val userId = currentUserId.value ?: "user_voce"
        val clubId = activeClubId.value ?: return@launch
        
        // clear user previous votes in this club first
        repository.clearVotesForUserInClub(userId, clubId)
        
        // Register vote — votingRoundId é null até Task 11 ligar com a rodada ativa
        repository.insertVote(Vote(bookId, userId, System.currentTimeMillis(), null))
    }
}
```

- [ ] **Step 4.5: Build deve passar**

Run: `./gradlew assembleDebug`  
Expected: `BUILD SUCCESSFUL`. Se ainda quebrar com `No value passed for parameter ...`, procurar o call-site faltante e adicionar o argumento equivalente.

- [ ] **Step 4.6: Commit**

```bash
git add app/src/main/java/com/example/data/model/Entities.kt app/src/main/java/com/example/data/repository/TramabookRepository.kt app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt
git commit -m "$(cat <<'EOF'
feat(data): novas entidades (Summary, Rating, Suggestion, VotingRound) e campos Vote.votingRoundId / ClubBook.dataEncontro

Adiciona 4 entidades pra resumo wiki, avaliação por usuário, justificativa
de sugestão persistida e rodada de votação. Atualiza Vote e ClubBook com
campos opcionais novos e call-sites do seed/ViewModel.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: Atualizar DAO e Database para v3

**Files:**
- Modify: `app/src/main/java/com/example/data/db/TramabookDao.kt`
- Modify: `app/src/main/java/com/example/data/db/AppDatabase.kt`

- [ ] **Step 5.1: Adicionar DAOs novos ao TramabookDao**

Abrir [TramabookDao.kt](app/src/main/java/com/example/data/db/TramabookDao.kt) e adicionar ao final do interface (antes do `}`):

```kotlin
    // --- Book Summaries ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookSummary(summary: BookSummary)

    @Query("SELECT * FROM book_summaries WHERE bookId = :bookId AND clubId = :clubId LIMIT 1")
    fun getBookSummaryFlow(bookId: String, clubId: String): Flow<BookSummary?>

    // --- Book Ratings ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookRating(rating: BookRating)

    @Query("SELECT * FROM book_ratings WHERE bookId = :bookId AND clubId = :clubId")
    fun getBookRatingsFlow(bookId: String, clubId: String): Flow<List<BookRating>>

    @Query("SELECT * FROM book_ratings WHERE bookId = :bookId AND clubId = :clubId AND userId = :userId LIMIT 1")
    fun getBookRatingOfUserFlow(bookId: String, clubId: String, userId: String): Flow<BookRating?>

    // --- Book Suggestions ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookSuggestion(suggestion: BookSuggestion)

    @Query("SELECT * FROM book_suggestions WHERE bookId = :bookId AND clubId = :clubId LIMIT 1")
    fun getBookSuggestionFlow(bookId: String, clubId: String): Flow<BookSuggestion?>

    @Query("SELECT * FROM book_suggestions WHERE clubId = :clubId")
    fun getBookSuggestionsForClubFlow(clubId: String): Flow<List<BookSuggestion>>

    // --- Voting Rounds ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVotingRound(round: VotingRound)

    @Query("SELECT * FROM voting_rounds WHERE clubId = :clubId AND status = 'aberta' ORDER BY abertaEm DESC LIMIT 1")
    fun getActiveVotingRoundFlow(clubId: String): Flow<VotingRound?>

    @Query("SELECT * FROM voting_rounds WHERE clubId = :clubId AND status = 'aberta' ORDER BY abertaEm DESC LIMIT 1")
    suspend fun getActiveVotingRound(clubId: String): VotingRound?

    @Query("UPDATE voting_rounds SET status = 'fechada', vencedoresJson = :vencedoresJson WHERE id = :id")
    suspend fun closeVotingRound(id: String, vencedoresJson: String)

    // --- Votes (extra queries pra rodadas) ---
    @Query("SELECT * FROM votes WHERE votingRoundId = :roundId")
    fun getVotesForRoundFlow(roundId: String): Flow<List<Vote>>

    @Query("SELECT * FROM votes WHERE votingRoundId = :roundId")
    suspend fun getVotesForRound(roundId: String): List<Vote>

    @Query("DELETE FROM votes WHERE userId = :userId AND votingRoundId = :roundId AND clubBookId = :bookId")
    suspend fun removeUserVoteForBookInRound(userId: String, roundId: String, bookId: String)

    @Query("SELECT COUNT(*) FROM votes WHERE userId = :userId AND votingRoundId = :roundId")
    suspend fun countUserVotesInRound(userId: String, roundId: String): Int

    // --- ClubBook update ---
    @Query("UPDATE club_books SET status = :status WHERE clubId = :clubId AND bookId = :bookId")
    suspend fun updateClubBookStatus(clubId: String, bookId: String, status: String)

    @Query("UPDATE club_books SET dataEncontro = :dataEncontro WHERE clubId = :clubId AND bookId = :bookId")
    suspend fun updateClubBookMeetingDate(clubId: String, bookId: String, dataEncontro: Long?)

    // --- Comments por livro (Chat retrospectivo) ---
    @Query("""
        SELECT c.* FROM comments c
        INNER JOIN chapters ch ON c.chapterId = ch.id
        WHERE ch.bookId = :bookId AND c.clubId = :clubId
        ORDER BY ch.numero ASC, c.criadoEm ASC
    """)
    fun getCommentsForBookFlow(bookId: String, clubId: String): Flow<List<Comment>>
```

- [ ] **Step 5.2: Atualizar AppDatabase com novas entidades e bump pra v3**

Substituir [AppDatabase.kt](app/src/main/java/com/example/data/db/AppDatabase.kt) inteiro por:

```kotlin
package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        User::class,
        Club::class,
        ClubMember::class,
        Book::class,
        ClubBook::class,
        Chapter::class,
        UserProgress::class,
        Comment::class,
        Reaction::class,
        Vote::class,
        Meeting::class,
        MeetingRsvp::class,
        DbNotification::class,
        SavedQuote::class,
        BookSummary::class,
        BookRating::class,
        BookSuggestion::class,
        VotingRound::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun tramabookDao(): TramabookDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tramabook_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

- [ ] **Step 5.3: Build**

Run: `./gradlew assembleDebug`  
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5.4: Commit**

```bash
git add app/src/main/java/com/example/data/db/TramabookDao.kt app/src/main/java/com/example/data/db/AppDatabase.kt
git commit -m "$(cat <<'EOF'
feat(db): DAOs novos e schema v3 com migração destrutiva

Adiciona queries pra summaries, ratings, suggestions, voting rounds e
chat retrospectivo (comments por livro). Database vai de v2 pra v3 com
fallback destrutivo — app ainda não publicado.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: Expandir o Repository com métodos novos

**Files:**
- Modify: `app/src/main/java/com/example/data/repository/TramabookRepository.kt`

- [ ] **Step 6.1: Adicionar métodos passthrough no Repository (antes do `seedDatabase`)**

Abrir [TramabookRepository.kt](app/src/main/java/com/example/data/repository/TramabookRepository.kt) e, **antes** do método `suspend fun seedDatabase()`, adicionar:

```kotlin
    // --- Book Summaries ---
    suspend fun insertBookSummary(summary: BookSummary) = dao.insertBookSummary(summary)
    fun getBookSummaryFlow(bookId: String, clubId: String): Flow<BookSummary?> = dao.getBookSummaryFlow(bookId, clubId)

    // --- Book Ratings ---
    suspend fun insertBookRating(rating: BookRating) = dao.insertBookRating(rating)
    fun getBookRatingsFlow(bookId: String, clubId: String): Flow<List<BookRating>> = dao.getBookRatingsFlow(bookId, clubId)
    fun getBookRatingOfUserFlow(bookId: String, clubId: String, userId: String): Flow<BookRating?> = dao.getBookRatingOfUserFlow(bookId, clubId, userId)

    // --- Book Suggestions ---
    suspend fun insertBookSuggestion(suggestion: BookSuggestion) = dao.insertBookSuggestion(suggestion)
    fun getBookSuggestionFlow(bookId: String, clubId: String): Flow<BookSuggestion?> = dao.getBookSuggestionFlow(bookId, clubId)
    fun getBookSuggestionsForClubFlow(clubId: String): Flow<List<BookSuggestion>> = dao.getBookSuggestionsForClubFlow(clubId)

    // --- Voting Rounds ---
    suspend fun insertVotingRound(round: VotingRound) = dao.insertVotingRound(round)
    fun getActiveVotingRoundFlow(clubId: String): Flow<VotingRound?> = dao.getActiveVotingRoundFlow(clubId)
    suspend fun getActiveVotingRound(clubId: String): VotingRound? = dao.getActiveVotingRound(clubId)
    suspend fun closeVotingRound(id: String, vencedoresJson: String) = dao.closeVotingRound(id, vencedoresJson)

    fun getVotesForRoundFlow(roundId: String): Flow<List<Vote>> = dao.getVotesForRoundFlow(roundId)
    suspend fun getVotesForRound(roundId: String): List<Vote> = dao.getVotesForRound(roundId)
    suspend fun removeUserVoteForBookInRound(userId: String, roundId: String, bookId: String) = dao.removeUserVoteForBookInRound(userId, roundId, bookId)
    suspend fun countUserVotesInRound(userId: String, roundId: String): Int = dao.countUserVotesInRound(userId, roundId)

    suspend fun updateClubBookStatus(clubId: String, bookId: String, status: String) = dao.updateClubBookStatus(clubId, bookId, status)
    suspend fun updateClubBookMeetingDate(clubId: String, bookId: String, dataEncontro: Long?) = dao.updateClubBookMeetingDate(clubId, bookId, dataEncontro)

    fun getCommentsForBookFlow(bookId: String, clubId: String): Flow<List<Comment>> = dao.getCommentsForBookFlow(bookId, clubId)
```

- [ ] **Step 6.2: Build**

Run: `./gradlew assembleDebug`  
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6.3: Commit**

```bash
git add app/src/main/java/com/example/data/repository/TramabookRepository.kt
git commit -m "$(cat <<'EOF'
feat(repo): passthrough dos novos DAOs (summaries/ratings/suggestions/rounds/chat)

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 7: Helper utilitário — formatação de data e tempo relativo

**Files:**
- Create: `app/src/main/java/com/example/util/DateFormat.kt`

- [ ] **Step 7.1: Criar arquivo de utilitários**

Criar `app/src/main/java/com/example/util/DateFormat.kt`:

```kotlin
package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ddMMM = SimpleDateFormat("dd 'de' MMM", Locale("pt", "BR"))
private val ddMMMyyyy = SimpleDateFormat("dd 'de' MMM 'de' yyyy", Locale("pt", "BR"))
private val MMMyyyy = SimpleDateFormat("MMM/yyyy", Locale("pt", "BR"))

fun formatShortDate(timestamp: Long): String = ddMMM.format(Date(timestamp))

fun formatFullDate(timestamp: Long): String = ddMMMyyyy.format(Date(timestamp))

fun formatMonthYear(timestamp: Long): String = MMMyyyy.format(Date(timestamp))

fun timeAgo(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val diff = (now - timestamp).coerceAtLeast(0L)
    val min = diff / 60_000
    val hour = diff / 3_600_000
    val day = diff / 86_400_000
    return when {
        min < 1 -> "agora"
        min < 60 -> "há ${min} min"
        hour < 24 -> "há ${hour}h"
        day < 7 -> "há ${day}d"
        else -> formatShortDate(timestamp)
    }
}
```

- [ ] **Step 7.2: Build**

Run: `./gradlew assembleDebug`  
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7.3: Commit**

```bash
git add app/src/main/java/com/example/util/DateFormat.kt
git commit -m "$(cat <<'EOF'
feat(util): helpers de formatação de data e tempo relativo em pt-BR

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 8: Componente reutilizável RatingStars

**Files:**
- Create: `app/src/main/java/com/example/ui/components/RatingStars.kt`

- [ ] **Step 8.1: Criar componente**

Criar `app/src/main/java/com/example/ui/components/RatingStars.kt`:

```kotlin
package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DividerSoft

private val Gold = Color(0xFFE6BF6B)

@Composable
fun RatingStars(
    rating: Float,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    spacing: Dp = 2.dp
) {
    val rounded = rating.toInt().coerceIn(0, 5)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        repeat(5) { idx ->
            Icon(
                imageVector = Icons.Outlined.Star,
                contentDescription = null,
                tint = if (idx < rounded) Gold else DividerSoft,
                modifier = Modifier.size(size)
            )
        }
    }
}

@Composable
fun RatingStarsInput(
    selected: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    spacing: Dp = 8.dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= selected) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                contentDescription = "Estrela $i",
                tint = if (i <= selected) Gold else DividerSoft,
                modifier = Modifier
                    .size(size)
                    .clickable { onChange(i) }
            )
        }
    }
}
```

- [ ] **Step 8.2: Build**

Run: `./gradlew assembleDebug`  
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8.3: Commit**

```bash
git add app/src/main/java/com/example/ui/components/RatingStars.kt
git commit -m "$(cat <<'EOF'
feat(ui): componente RatingStars (display + input)

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 9: Atualizar seed do Repository com nova rodada de votação + summaries + ratings + suggestions + admin

**Files:**
- Modify: `app/src/main/java/com/example/data/repository/TramabookRepository.kt`

- [ ] **Step 9.1: Marcar user_voce também como admin do club_mari**

Em [TramabookRepository.kt:145](app/src/main/java/com/example/data/repository/TramabookRepository.kt#L145), localizar:

```kotlin
dao.insertClubMember(ClubMember("club_mari", "user_voce", "member", System.currentTimeMillis() - 28 * 24 * 60 * 60 * 1000L))
```

Trocar `"member"` por `"admin"` (justificativa em §12 do spec — permite testar fluxos de admin):

```kotlin
dao.insertClubMember(ClubMember("club_mari", "user_voce", "admin", System.currentTimeMillis() - 28 * 24 * 60 * 60 * 1000L))
```

- [ ] **Step 9.2: Adicionar VotingRound aberta + BookSuggestion para cada sug_X**

No `seedDatabase`, **após** o bloco que insere os `Vote` (depois da linha [TramabookRepository.kt:248](app/src/main/java/com/example/data/repository/TramabookRepository.kt#L248)) e **antes** do comentário `// 7. Finished books`, adicionar:

```kotlin
        // Voting round ativa (7 dias à frente, N=1, cadência única)
        val agora = System.currentTimeMillis()
        val seteDias = 7L * 24 * 60 * 60 * 1000L
        dao.insertVotingRound(
            VotingRound(
                id = "round_mari_1",
                clubId = "club_mari",
                criadoPor = "user_marina",
                abertaEm = agora,
                fechaEm = agora + seteDias,
                nLivros = 1,
                cadencia = "unica",
                status = "aberta",
                vencedoresJson = "[]"
            )
        )

        // Justificativas reais persistidas pra cada sugestão
        dao.insertBookSuggestion(
            BookSuggestion(
                id = "bs_1",
                clubId = "club_mari",
                bookId = "sug_1",
                suggestedByUserId = "user_marina",
                justificativa = "Tava querendo um livro brasileiro contemporâneo. Conceição Evaristo escreve memória e afeto como ninguém.",
                criadoEm = agora - 2 * 24 * 60 * 60 * 1000L
            )
        )
        dao.insertBookSuggestion(
            BookSuggestion(
                id = "bs_2",
                clubId = "club_mari",
                bookId = "sug_2",
                suggestedByUserId = "user_bia",
                justificativa = "Já é momento de revisitar Rachel de Queiroz — leitura curta e necessária.",
                criadoEm = agora - 1 * 24 * 60 * 60 * 1000L
            )
        )
        dao.insertBookSuggestion(
            BookSuggestion(
                id = "bs_3",
                clubId = "club_mari",
                bookId = "sug_3",
                suggestedByUserId = "user_lucas",
                justificativa = "Queria algo mais leve depois da intensidade da Clarice. Ficção científica clássica.",
                criadoEm = agora - 3 * 60 * 60 * 1000L
            )
        )
```

- [ ] **Step 9.3: Adicionar BookSummary + BookRating para os finished**

Logo após o bloco de finished books (depois da linha [TramabookRepository.kt:261](app/src/main/java/com/example/data/repository/TramabookRepository.kt#L261), antes do comentário `// 8. Visual notifications`), adicionar:

```kotlin
        // Resumos wiki dos finished
        dao.insertBookSummary(
            BookSummary(
                bookId = "fin_1",
                clubId = "club_mari",
                texto = "Coletânea de contos da Conceição Evaristo. Cada conto é um soco no estômago — mulheres negras periféricas em situações que a gente prefere não ver. A escrita é direta mas atravessada de poesia. A discussão do clube ficou centrada em \"Maria\" e \"Ana Davenga\".",
                lastEditorId = "user_marina",
                updatedAt = agora - 5 * 24 * 60 * 60 * 1000L
            )
        )
        dao.insertBookSummary(
            BookSummary(
                bookId = "fin_3",
                clubId = "club_mari",
                texto = "Romance que ganhou o Jabuti em 2020. Duas irmãs no sertão da Chapada Diamantina herdam uma faca e uma língua. Itamar Vieira Junior constrói uma narrativa em três partes que vai do mítico ao histórico sem perder o chão.",
                lastEditorId = "user_voce",
                updatedAt = agora - 12 * 24 * 60 * 60 * 1000L
            )
        )

        // Avaliações dos finished
        dao.insertBookRating(BookRating("fin_1", "club_mari", "user_marina", 5, "Conceição é incontornável.", agora - 5 * 24 * 60 * 60 * 1000L))
        dao.insertBookRating(BookRating("fin_1", "club_mari", "user_voce", 5, "", agora - 4 * 24 * 60 * 60 * 1000L))
        dao.insertBookRating(BookRating("fin_1", "club_mari", "user_sofia", 4, "Achei intenso demais às vezes, mas brilhante.", agora - 3 * 24 * 60 * 60 * 1000L))
        dao.insertBookRating(BookRating("fin_2", "club_mari", "user_marina", 4, "", agora - 10 * 24 * 60 * 60 * 1000L))
        dao.insertBookRating(BookRating("fin_2", "club_mari", "user_lucas", 3, "Não conectei muito.", agora - 9 * 24 * 60 * 60 * 1000L))
        dao.insertBookRating(BookRating("fin_3", "club_mari", "user_voce", 5, "Um dos melhores livros que já li.", agora - 12 * 24 * 60 * 60 * 1000L))
        dao.insertBookRating(BookRating("fin_3", "club_mari", "user_marina", 5, "Sim.", agora - 11 * 24 * 60 * 60 * 1000L))
        dao.insertBookRating(BookRating("fin_3", "club_mari", "user_bia", 4, "", agora - 10 * 24 * 60 * 60 * 1000L))
```

- [ ] **Step 9.4: Build**

Run: `./gradlew assembleDebug`  
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9.5: Commit**

```bash
git add app/src/main/java/com/example/data/repository/TramabookRepository.kt
git commit -m "$(cat <<'EOF'
feat(seed): votação aberta, justificativas persistidas, resumos e avaliações dos finished

- user_voce vira admin do club_mari pra permitir testar fluxos de admin
- round_mari_1: rodada ativa, 7 dias à frente, N=1
- BookSuggestion pra cada sug_X com justificativa real
- BookSummary e BookRating populados nos finished

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 10: Expandir o MainViewModel com flows e ações novas

**Files:**
- Modify: `app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt`

- [ ] **Step 10.1: Adicionar imports necessários**

No topo de [MainViewModel.kt](app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt), no bloco de imports, garantir que estejam presentes (alguns já estão):

```kotlin
import org.json.JSONArray
```

(Se já existir, não duplicar.)

- [ ] **Step 10.2: Adicionar StateFlows e flows derivados ao final do `init { ... }` (antes dos métodos)**

Encontrar onde os flows estão declarados (entre a linha de `savedQuotes` ~138 e o `init` ~147). Adicionar ANTES do bloco `init`:

```kotlin
    // Active voting round
    val activeVotingRound: StateFlow<VotingRound?> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getActiveVotingRoundFlow(clubId) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Book suggestions for current club
    val bookSuggestionsByBookId: StateFlow<Map<String, BookSuggestion>> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getBookSuggestionsForClubFlow(clubId).map { list ->
            list.associateBy { it.bookId }
        } else flowOf(emptyMap())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Next-queue books (status = "next")
    val nextBooks: StateFlow<List<Book>> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getBookByStatusFlow(clubId, "next") else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Is current user admin of active club?
    val isCurrentUserAdmin: StateFlow<Boolean> = combine(currentUserId, activeClubId) { uid, cid -> Pair(uid, cid) }
        .flatMapLatest { (uid, cid) ->
            if (uid != null && cid != null) {
                flow {
                    val m = repository.getClubMember(cid, uid)
                    emit(m?.papel == "admin")
                }
            } else flowOf(false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
```

- [ ] **Step 10.3: Adicionar métodos pra livro/clube por id (não-flow)**

Antes do método `voteForBook`, adicionar:

```kotlin
    fun getBookSummaryFlow(bookId: String): Flow<BookSummary?> =
        activeClubId.flatMapLatest { clubId ->
            if (clubId != null) repository.getBookSummaryFlow(bookId, clubId) else flowOf(null)
        }

    fun getBookRatingsFlow(bookId: String): Flow<List<BookRating>> =
        activeClubId.flatMapLatest { clubId ->
            if (clubId != null) repository.getBookRatingsFlow(bookId, clubId) else flowOf(emptyList())
        }

    fun getBookRatingOfCurrentUserFlow(bookId: String): Flow<BookRating?> =
        combine(activeClubId, currentUserId) { c, u -> Pair(c, u) }
            .flatMapLatest { (c, u) ->
                if (c != null && u != null) repository.getBookRatingOfUserFlow(bookId, c, u)
                else flowOf(null)
            }

    fun getBookSuggestionFlow(bookId: String): Flow<BookSuggestion?> =
        activeClubId.flatMapLatest { clubId ->
            if (clubId != null) repository.getBookSuggestionFlow(bookId, clubId) else flowOf(null)
        }

    fun getCommentsForBookFlow(bookId: String): Flow<List<Comment>> =
        activeClubId.flatMapLatest { clubId ->
            if (clubId != null) repository.getCommentsForBookFlow(bookId, clubId) else flowOf(emptyList())
        }

    fun saveBookSummary(bookId: String, texto: String) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            val userId = currentUserId.value ?: return@launch
            repository.insertBookSummary(
                BookSummary(bookId, clubId, texto.trim(), userId, System.currentTimeMillis())
            )
        }
    }

    fun saveBookRating(bookId: String, stars: Int, comment: String) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            val userId = currentUserId.value ?: return@launch
            if (stars !in 1..5) return@launch
            repository.insertBookRating(
                BookRating(bookId, clubId, userId, stars, comment.trim(), System.currentTimeMillis())
            )
        }
    }

    fun setBookMeetingDate(bookId: String, dataEncontro: Long?) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            repository.updateClubBookMeetingDate(clubId, bookId, dataEncontro)
        }
    }
```

- [ ] **Step 10.4: Refazer `voteForBook` pra usar a rodada ativa com limite N**

Substituir o método `voteForBook` (que ainda está com `null` na Task 4.4) por:

```kotlin
    fun voteForBook(bookId: String) {
        viewModelScope.launch {
            val userId = currentUserId.value ?: "user_voce"
            val clubId = activeClubId.value ?: return@launch
            val round = repository.getActiveVotingRound(clubId) ?: return@launch

            // Se já votou neste livro, desfaz
            val existingForBook = repository.getVotesForRound(round.id)
                .firstOrNull { it.userId == userId && it.clubBookId == bookId }
            if (existingForBook != null) {
                repository.removeUserVoteForBookInRound(userId, round.id, bookId)
                return@launch
            }

            // Se atingiu o limite N, não faz nada
            val count = repository.countUserVotesInRound(userId, round.id)
            if (count >= round.nLivros) return@launch

            repository.insertVote(Vote(bookId, userId, System.currentTimeMillis(), round.id))
        }
    }
```

- [ ] **Step 10.5: Adicionar ações de votação (abrir/encerrar)**

Após `voteForBook`, adicionar:

```kotlin
    fun openVotingRound(nLivros: Int, durationDays: Int, cadencia: String) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            val userId = currentUserId.value ?: return@launch
            // Idempotência: se já existe rodada aberta, ignora
            val existing = repository.getActiveVotingRound(clubId)
            if (existing != null) return@launch

            val agora = System.currentTimeMillis()
            val round = VotingRound(
                id = "round_${UUID.randomUUID().toString().take(8)}",
                clubId = clubId,
                criadoPor = userId,
                abertaEm = agora,
                fechaEm = agora + durationDays.toLong() * 24 * 60 * 60 * 1000L,
                nLivros = nLivros.coerceIn(1, 12),
                cadencia = cadencia,
                status = "aberta",
                vencedoresJson = "[]"
            )
            repository.insertVotingRound(round)
        }
    }

    fun closeActiveVotingRound() {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            val round = repository.getActiveVotingRound(clubId) ?: return@launch
            closeRoundInternal(round, clubId)
        }
    }

    /**
     * Chamado no init e quando o app abre — se houver rodada com fechaEm <= now,
     * encerra automaticamente.
     */
    fun maybeAutoCloseExpiredRound() {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            val round = repository.getActiveVotingRound(clubId) ?: return@launch
            if (System.currentTimeMillis() >= round.fechaEm) {
                closeRoundInternal(round, clubId)
            }
        }
    }

    private suspend fun closeRoundInternal(round: VotingRound, clubId: String) {
        val votes = repository.getVotesForRound(round.id)
        // Agrupar votos por bookId
        val tally = votes.groupingBy { it.clubBookId }.eachCount()
        // Desempate: o que foi sugerido antes vence. Para isso, buscar suggestions ordenadas.
        val suggestions = repository.getBookSuggestionsForClubFlow(clubId)
            .first()
            .associateBy { it.bookId }

        val ranked = tally.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }
                .thenBy { suggestions[it.key]?.criadoEm ?: Long.MAX_VALUE })
            .map { it.key }

        val winners = ranked.take(round.nLivros)

        // Marcar current atual como finished com dataEncontro = now
        val currentList = repository.getBookByStatusFlow(clubId, "current").first()
        currentList.forEach { b ->
            repository.updateClubBookStatus(clubId, b.id, "finished")
            repository.updateClubBookMeetingDate(clubId, b.id, System.currentTimeMillis())
        }

        // Promover vencedores: 1º vira current, demais viram next
        winners.forEachIndexed { idx, bookId ->
            val newStatus = if (idx == 0) "current" else "next"
            repository.updateClubBookStatus(clubId, bookId, newStatus)
        }

        // Marcar rodada como fechada
        val vencedoresJson = JSONArray().apply { winners.forEach { put(it) } }.toString()
        repository.closeVotingRound(round.id, vencedoresJson)

        // Notificar todos os membros
        val members = repository.getClubMembersFlow(clubId).first()
        val titles = winners.mapNotNull { repository.getBook(it)?.title }
        val payload = JSONArray().apply { titles.forEach { put(it) } }.toString()
        members.forEach { member ->
            repository.insertNotification(
                DbNotification(
                    id = "ntf_${UUID.randomUUID()}",
                    userId = member.id,
                    clubId = clubId,
                    tipo = "voting_closed",
                    payloadJson = "{\"titulos\":$payload,\"n\":${winners.size}}",
                    lida = false,
                    criadoEm = System.currentTimeMillis()
                )
            )
        }
    }
```

- [ ] **Step 10.6: Atualizar `createBookSuggestion` pra persistir a justificativa**

Localizar `createBookSuggestion` em [MainViewModel.kt:407-434](app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt#L407-L434) e substituir o método inteiro por:

```kotlin
    fun createBookSuggestion(doc: OpenLibraryDoc, justification: String, onCompleted: () -> Unit) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            val userId = currentUserId.value ?: "user_voce"
            val bookId = "book_sug_${UUID.randomUUID().toString().take(6)}"
            val coverId = doc.coverI
            val coverUrl = if (coverId != null) {
                "https://covers.openlibrary.org/b/id/${coverId}-M.jpg"
            } else {
                ""
            }

            val newBook = Book(
                id = bookId,
                title = doc.title,
                author = doc.authorName?.firstOrNull() ?: "Autor desconhecido",
                coverUrl = coverUrl,
                openlibraryId = "",
                isbn = doc.isbn?.firstOrNull() ?: ""
            )
            repository.insertBook(newBook)

            // Insert suggestion relation (dataEncontro=null pra sugestão)
            repository.insertClubBook(ClubBook(clubId, bookId, "suggested", 0, null))

            // Persist justificativa de verdade
            if (justification.isNotBlank()) {
                repository.insertBookSuggestion(
                    BookSuggestion(
                        id = "bs_${UUID.randomUUID().toString().take(8)}",
                        clubId = clubId,
                        bookId = bookId,
                        suggestedByUserId = userId,
                        justificativa = justification.trim(),
                        criadoEm = System.currentTimeMillis()
                    )
                )
            }

            onCompleted()
        }
    }
```

- [ ] **Step 10.7: Disparar auto-close no init**

Localizar o bloco `init { viewModelScope.launch { repository.seedDatabase() ... } }` em [MainViewModel.kt:147-160](app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt#L147-L160).

Após a chamada de `repository.seedDatabase()`, dentro do mesmo `launch`, adicionar uma chamada que aguarde 500ms (pra dar tempo do `activeClubId` se hidratar) e dispare o auto-close:

```kotlin
    init {
        viewModelScope.launch {
            repository.seedDatabase()
            dataStoreManager.userIdFlow.first()?.let {
                // Already logged in
            } ?: run {
                dataStoreManager.saveSession("user_voce", "Você", "voce@tramabook.com")
                dataStoreManager.saveActiveClubId("club_mari")
            }
            // Dá um respiro pra activeClubId hidratar antes de tentar fechar rodada expirada
            kotlinx.coroutines.delay(500)
            maybeAutoCloseExpiredRound()
        }
    }
```

- [ ] **Step 10.8: Build**

Run: `./gradlew assembleDebug`  
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 10.9: Commit**

```bash
git add app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt
git commit -m "$(cat <<'EOF'
feat(viewmodel): flows e ações de rodadas, votos N, sumário, avaliações, justificativa

- activeVotingRound, bookSuggestionsByBookId, nextBooks, isCurrentUserAdmin
- voteForBook agora respeita N votos por rodada e desfaz com clique repetido
- openVotingRound / closeActiveVotingRound / maybeAutoCloseExpiredRound
- saveBookSummary, saveBookRating, setBookMeetingDate
- createBookSuggestion persiste a justificativa de verdade

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 11: Teste unitário de tally e desempate

**Files:**
- Create: `app/src/test/java/com/example/voting/VotingTallyTest.kt`

Justificativa: a lógica de tally + desempate (top-N por contagem, desempate por `criadoEm` da sugestão) é determinística e merece teste isolado para evitar regressão.

- [ ] **Step 11.1: Extrair função pura de tally**

Criar `app/src/main/java/com/example/voting/VotingTally.kt`:

```kotlin
package com.example.voting

import com.example.data.model.BookSuggestion
import com.example.data.model.Vote

object VotingTally {
    fun rank(
        votes: List<Vote>,
        suggestionsByBookId: Map<String, BookSuggestion>,
        n: Int
    ): List<String> {
        val tally = votes.groupingBy { it.clubBookId }.eachCount()
        return tally.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> { it.value }
                    .thenBy { suggestionsByBookId[it.key]?.criadoEm ?: Long.MAX_VALUE }
            )
            .map { it.key }
            .take(n)
    }
}
```

- [ ] **Step 11.2: Refatorar `closeRoundInternal` no ViewModel pra usar `VotingTally.rank`**

Em `MainViewModel.kt`, dentro de `closeRoundInternal`, substituir o trecho que computa `ranked` por:

```kotlin
        val ranked = com.example.voting.VotingTally.rank(
            votes = votes,
            suggestionsByBookId = suggestions,
            n = round.nLivros
        )
        val winners = ranked
```

(Remove o `.take(round.nLivros)` separado, já está embutido em `rank`.)

- [ ] **Step 11.3: Criar teste**

Criar `app/src/test/java/com/example/voting/VotingTallyTest.kt`:

```kotlin
package com.example.voting

import com.example.data.model.BookSuggestion
import com.example.data.model.Vote
import org.junit.Assert.assertEquals
import org.junit.Test

class VotingTallyTest {

    private fun sug(bookId: String, criadoEm: Long) = BookSuggestion(
        id = "bs_$bookId", clubId = "c", bookId = bookId,
        suggestedByUserId = "u", justificativa = "", criadoEm = criadoEm
    )

    private fun v(bookId: String, userId: String) = Vote(
        clubBookId = bookId, userId = userId, votedAt = 0L, votingRoundId = "r1"
    )

    @Test
    fun `top N por contagem decrescente`() {
        val votes = listOf(
            v("a", "u1"), v("a", "u2"), v("a", "u3"),
            v("b", "u1"), v("b", "u2"),
            v("c", "u1")
        )
        val sugs = mapOf("a" to sug("a", 1), "b" to sug("b", 2), "c" to sug("c", 3))
        val result = VotingTally.rank(votes, sugs, n = 2)
        assertEquals(listOf("a", "b"), result)
    }

    @Test
    fun `empate desempata pelo criadoEm mais antigo`() {
        val votes = listOf(
            v("a", "u1"), v("a", "u2"),
            v("b", "u1"), v("b", "u2")
        )
        val sugs = mapOf("a" to sug("a", 100), "b" to sug("b", 50))
        val result = VotingTally.rank(votes, sugs, n = 1)
        assertEquals(listOf("b"), result)
    }

    @Test
    fun `N maior que disponivel retorna todos`() {
        val votes = listOf(v("a", "u1"))
        val sugs = mapOf("a" to sug("a", 1))
        val result = VotingTally.rank(votes, sugs, n = 5)
        assertEquals(listOf("a"), result)
    }

    @Test
    fun `sem votos retorna lista vazia`() {
        val result = VotingTally.rank(emptyList(), emptyMap(), n = 3)
        assertEquals(emptyList<String>(), result)
    }
}
```

- [ ] **Step 11.4: Rodar teste**

Run: `./gradlew testDebugUnitTest --tests "com.example.voting.VotingTallyTest"`  
Expected: `BUILD SUCCESSFUL`, 4 tests passed.

- [ ] **Step 11.5: Commit**

```bash
git add app/src/main/java/com/example/voting/VotingTally.kt app/src/test/java/com/example/voting/VotingTallyTest.kt app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt
git commit -m "$(cat <<'EOF'
feat(voting): extrai VotingTally.rank com teste unitário

Lógica de top-N + desempate por criadoEm mais antigo, isolada da camada
async pra teste determinístico.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 12: Nova tab "Estante" na navbar (5 tabs)

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/MainTabsScreen.kt`
- Create: `app/src/main/java/com/example/ui/screens/ShelfTabScreen.kt`

- [ ] **Step 12.1: Criar arquivo ShelfTabScreen.kt**

Criar `app/src/main/java/com/example/ui/screens/ShelfTabScreen.kt`:

```kotlin
package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Book
import com.example.ui.components.*
import com.example.ui.theme.Ink
import com.example.ui.theme.Muted
import com.example.ui.viewmodel.MainViewModel
import com.example.util.formatShortDate
import kotlinx.coroutines.flow.flowOf

@Composable
fun ShelfTabScreen(
    viewModel: MainViewModel,
    onNavigateToBookDetail: (String) -> Unit
) {
    val finishedBooks by viewModel.finishedBooks.collectAsState()
    val clubBooks by viewModel.clubBooks.collectAsState()
    var filterBy by remember { mutableStateOf("Todos") }

    // Resolve dataEncontro + average rating por livro
    val displayList = finishedBooks.map { book ->
        val ratingsFlow = remember(book.id) { viewModel.getBookRatingsFlow(book.id) }
        val ratings by ratingsFlow.collectAsState(initial = emptyList())
        val avg = if (ratings.isNotEmpty()) ratings.sumOf { it.stars }.toFloat() / ratings.size else 0f
        Triple(book, avg, book.id)
    }

    val filtered = if (filterBy == "Favoritos") {
        displayList.filter { it.second >= 4.5f }
    } else displayList

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Todos", "Favoritos").forEach { option ->
                val isSelected = filterBy == option
                Box(modifier = Modifier.clickable { filterBy = option }) {
                    Pill(
                        text = option,
                        variant = if (isSelected) PillVariant.OliveDeep else PillVariant.Default
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Nenhum livro lido ainda pelo clube. Quando vocês terminarem um livro, ele aparece aqui.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(filtered, key = { it.first.id }) { triple ->
                    val book = triple.first
                    val rating = triple.second
                    val clubBookEntry = clubBooks.find { it.id == book.id }
                    // dataEncontro vem do ClubBook — mas clubBooks aqui é List<Book>, não ClubBook.
                    // Precisamos do dataEncontro via VM. Adicionar logo após esta task.
                    ShelfBookCard(
                        book = book,
                        rating = rating,
                        dataEncontroLabel = null,  // será preenchido na Task 12.2 via mapa
                        onClick = { onNavigateToBookDetail(book.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShelfBookCard(
    book: Book,
    rating: Float,
    dataEncontroLabel: String?,
    onClick: () -> Unit
) {
    TramabookCard(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Cover(
                title = book.title,
                author = book.author,
                coverUrl = book.coverUrl,
                width = 100.dp,
                height = 150.dp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = book.title,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 14.sp, color = Ink),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = book.author,
                style = MaterialTheme.typography.bodyLarge.copy(color = Muted, fontSize = 12.sp),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            if (dataEncontroLabel != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Encontro em $dataEncontroLabel",
                    style = MaterialTheme.typography.labelSmall.copy(color = Muted, fontSize = 11.sp),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            RatingStars(rating = rating, size = 16.dp)
        }
    }
}
```

- [ ] **Step 12.2: Expor dataEncontro via VM (mapa por bookId)**

Em `MainViewModel.kt`, adicionar (perto dos outros flows derivados):

```kotlin
    /**
     * Mapa bookId -> dataEncontro do clube ativo. Inclui null se não setada.
     */
    val finishedBooksMeetingDates: StateFlow<Map<String, Long?>> = activeClubId.flatMapLatest { clubId ->
        if (clubId == null) flowOf(emptyMap())
        else kotlinx.coroutines.flow.flow {
            // Como ClubBook não tem flow direto, refazer via raw dao seria intrusivo.
            // Em vez disso, exportar via método one-shot e refrescar em mudança de finishedBooks.
            emit(emptyMap<String, Long?>())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
```

Como esse approach está hacky, vou trocar de estratégia. **Substituir o flow acima** por um método suspend + criar um Flow novo no DAO:

Adicionar ao `TramabookDao`:

```kotlin
    @Query("SELECT * FROM club_books WHERE clubId = :clubId AND status = :status")
    fun getClubBooksByStatusFlow(clubId: String, status: String): Flow<List<ClubBook>>
```

E ao Repository:

```kotlin
    fun getClubBooksByStatusFlow(clubId: String, status: String): Flow<List<ClubBook>> = dao.getClubBooksByStatusFlow(clubId, status)
```

E no ViewModel, **substituir** o `finishedBooksMeetingDates` por:

```kotlin
    val finishedBooksMeetingDates: StateFlow<Map<String, Long?>> = activeClubId.flatMapLatest { clubId ->
        if (clubId == null) flowOf(emptyMap())
        else repository.getClubBooksByStatusFlow(clubId, "finished").map { list ->
            list.associate { it.bookId to it.dataEncontro }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
```

- [ ] **Step 12.3: Conectar dataEncontro no ShelfTabScreen**

No `ShelfTabScreen`, no início do composable, adicionar:

```kotlin
    val meetingDates by viewModel.finishedBooksMeetingDates.collectAsState()
```

E na chamada de `ShelfBookCard`, mudar:
```kotlin
dataEncontroLabel = null,
```
para:
```kotlin
dataEncontroLabel = meetingDates[book.id]?.let { formatShortDate(it) },
```

- [ ] **Step 12.4: Adicionar "shelf" na navbar de MainTabsScreen**

Em [MainTabsScreen.kt](app/src/main/java/com/example/ui/screens/MainTabsScreen.kt), localizar o `CustomBottomBar` (~linha 335). Adicionar um item entre "Próximo" e "Perfil":

```kotlin
                BottomBarItem(
                    label = "Estante",
                    icon = if (selectedTab == "shelf") Icons.Filled.MenuBook else Icons.Outlined.MenuBook,
                    selected = selectedTab == "shelf",
                    onClick = { onTabSelected("shelf") }
                )
```

Adicionar os imports necessários no topo do arquivo:

```kotlin
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.MenuBook
```

- [ ] **Step 12.5: Adicionar case "shelf" no when() do Scaffold**

No bloco `when (selectedTab) { ... }` em [MainTabsScreen.kt:177-199](app/src/main/java/com/example/ui/screens/MainTabsScreen.kt#L177-L199), adicionar entre `"next"` e `"profile"`:

```kotlin
                "shelf" -> ShelfTabScreen(
                    viewModel = viewModel,
                    onNavigateToBookDetail = onNavigateToBookDetail
                )
```

- [ ] **Step 12.6: Ajustar paddings da bottom bar pra 5 tabs caberem**

No `CustomBottomBar` em [MainTabsScreen.kt:336-345](app/src/main/java/com/example/ui/screens/MainTabsScreen.kt#L336-L345), reduzir paddings:

Trocar:
```kotlin
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 8.dp),
```
por:
```kotlin
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 8.dp),
```

E em `BottomBarItem` (parte `if (selected)`) trocar:
```kotlin
                .padding(horizontal = 16.dp, vertical = 10.dp),
```
por:
```kotlin
                .padding(horizontal = 12.dp, vertical = 10.dp),
```

- [ ] **Step 12.7: Remover sub-tab "Estante" de NextTabScreen**

Em [NextTabScreen.kt:61](app/src/main/java/com/example/ui/screens/NextTabScreen.kt#L61), trocar:

```kotlin
            val nextTabs = listOf("encontro" to "Encontro", "votacao" to "Votação", "estante" to "Estante")
```

por:

```kotlin
            val nextTabs = listOf("encontro" to "Encontro", "votacao" to "Votação")
```

E em [NextTabScreen.kt:93](app/src/main/java/com/example/ui/screens/NextTabScreen.kt#L93):

```kotlin
            val nextTabs = listOf("encontro", "votacao", "estante")
```

vira:

```kotlin
            val nextTabs = listOf("encontro", "votacao")
```

E no `when (subTab)` em [NextTabScreen.kt:114-118](app/src/main/java/com/example/ui/screens/NextTabScreen.kt#L114-L118):

```kotlin
            when (subTab) {
                "encontro" -> EncontroTab(viewModel = viewModel)
                "votacao" -> VotacaoTab(viewModel = viewModel, onNavigateToSuggestBook = onNavigateToSuggestBook)
                "estante" -> EstanteTab(viewModel = viewModel, onNavigateToBookDetail = onNavigateToBookDetail)
            }
```

vira:

```kotlin
            when (subTab) {
                "encontro" -> EncontroTab(viewModel = viewModel)
                "votacao" -> VotacaoTab(viewModel = viewModel, onNavigateToSuggestBook = onNavigateToSuggestBook)
            }
```

E remover o composable `EstanteTab` inteiro (linhas ~716-846 — começa com `// --- SUB-TAB 3: ESTANTE (Lidos) ---` e vai até o final do arquivo). Junto com isso, remover a função-parametro `onNavigateToBookDetail` de `NextTabScreen` se não usada:

A assinatura atual é:
```kotlin
fun NextTabScreen(
    viewModel: MainViewModel,
    onNavigateToSuggestBook: () -> Unit,
    onNavigateToBookDetail: (String) -> Unit = {}
)
```

Como removemos a única chamada a `onNavigateToBookDetail` dentro dessa tela (era pra `EstanteTab`), **manter o parâmetro** mesmo assim (não-quebrar a chamada de `MainTabsScreen` que ainda passa). Default `= {}` já cobre.

Atualizar a chamada em `MainTabsScreen.kt`:

```kotlin
                "next" -> NextTabScreen(
                    viewModel = viewModel,
                    onNavigateToSuggestBook = onNavigateToSuggestBook,
                    onNavigateToBookDetail = onNavigateToBookDetail
                )
```

pode virar:

```kotlin
                "next" -> NextTabScreen(
                    viewModel = viewModel,
                    onNavigateToSuggestBook = onNavigateToSuggestBook
                )
```

- [ ] **Step 12.8: Build**

Run: `./gradlew assembleDebug`  
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 12.9: Commit**

```bash
git add app/src/main/java/com/example/ui/screens/ShelfTabScreen.kt app/src/main/java/com/example/ui/screens/MainTabsScreen.kt app/src/main/java/com/example/ui/screens/NextTabScreen.kt app/src/main/java/com/example/data/db/TramabookDao.kt app/src/main/java/com/example/data/repository/TramabookRepository.kt app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt
git commit -m "$(cat <<'EOF'
feat(navbar): Estante vira tab própria com grade enriquecida

- 5 tabs: Início · Livro atual · Próximo · Estante · Perfil
- Sub-tab Estante removida de Próximo
- Cards na grade da Estante mostram data do encontro e média de estrelas real
- Paddings reduzidos pra caber em 360dp

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 13: Detalhe do Livro com 5 abas (Resumo, Frases, Chat, Avaliações, Histórico)

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/BookDetailScreen.kt`

Este é o passo grande. Vou reescrever o `BookDetailScreen` mantendo o hero + banner mas trocando tab content por 5 abas.

- [ ] **Step 13.1: Atualizar banner do hero com dataEncontro**

Em [BookDetailScreen.kt:154-173](app/src/main/java/com/example/ui/screens/BookDetailScreen.kt#L154-L173), localizar o `Box` do banner com "Este livro está na estante do clube.":

Substituir o `Text` do banner por (precisa também buscar `dataEncontro` do livro):

```kotlin
        val meetingDates by viewModel.finishedBooksMeetingDates.collectAsState()
        val dataEncontro = meetingDates[bookId]
        val bannerText = if (dataEncontro != null) {
            "Lido pelo clube · Encontro em ${com.example.util.formatShortDate(dataEncontro)}"
        } else {
            "Lido pelo clube"
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Oliva)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = bannerText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    color = Cream
                ),
                textAlign = TextAlign.Center
            )
        }
```

Mover a leitura de `meetingDates` pra cima (junto com as outras `collectAsState`), pra não ficar fora do escopo:

No topo da `Column` que envolve toda a tela, perto onde `quotesFlow` é declarado, garantir que `meetingDates` está coletado em um único lugar (passar valor a partir daí).

- [ ] **Step 13.2: Atualizar lista de tabs**

Em [BookDetailScreen.kt:178](app/src/main/java/com/example/ui/screens/BookDetailScreen.kt#L178), trocar:

```kotlin
        val tabs = listOf("resumo" to "Resumo", "frases" to "Frases", "historico" to "Histórico")
```

por:

```kotlin
        val tabs = listOf(
            "resumo" to "Resumo",
            "frases" to "Frases",
            "chat" to "Chat",
            "avaliacoes" to "Avaliações",
            "historico" to "Histórico"
        )
```

Reduzir tamanho da label da tab pra caber 5:

No `Text(text = label, ...)` dentro do `forEach` de tabs, trocar:
```kotlin
                        style = MaterialTheme.typography.titleMedium.copy(
```
por:
```kotlin
                        style = MaterialTheme.typography.bodyMedium.copy(
```

E adicionar `maxLines = 1, overflow = TextOverflow.Ellipsis` no Text das labels.

- [ ] **Step 13.3: Reescrever tab "resumo"**

No `when (tab)`, substituir o bloco `"resumo" -> { ... }` inteiro por:

```kotlin
                "resumo" -> {
                    val summaryFlow = remember(bookId) { viewModel.getBookSummaryFlow(bookId) }
                    val summary by summaryFlow.collectAsState(initial = null)
                    val members by viewModel.clubMembers.collectAsState()
                    var showEditDialog by remember { mutableStateOf(false) }
                    var draftText by remember { mutableStateOf("") }

                    if (summary == null) {
                        TramabookCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Ninguém escreveu o resumo ainda. Que tal começar?",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = InterFontFamily,
                                    color = Muted
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        TbButton(
                            text = "Escrever resumo",
                            onClick = { draftText = ""; showEditDialog = true },
                            variant = TbButtonVariant.Outline,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        TramabookCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = summary!!.texto,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = InterFontFamily,
                                    color = InkSoft,
                                    lineHeight = 22.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val editorName = members.find { it.id == summary!!.lastEditorId }?.nome ?: "alguém"
                        Text(
                            text = "Editado por $editorName · ${com.example.util.timeAgo(summary!!.updatedAt)}",
                            style = MaterialTheme.typography.labelSmall.copy(color = Muted),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TbButton(
                            text = "Editar resumo",
                            onClick = { draftText = summary!!.texto; showEditDialog = true },
                            variant = TbButtonVariant.Outline,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (showEditDialog) {
                        AlertDialog(
                            onDismissRequest = { showEditDialog = false },
                            title = { Text("Resumo do livro", style = MaterialTheme.typography.titleLarge.copy(fontFamily = LiterataFontFamily, fontWeight = FontWeight.SemiBold)) },
                            text = {
                                OutlinedTextField(
                                    value = draftText,
                                    onValueChange = { draftText = it },
                                    placeholder = { Text("O clube leu esse livro — conta o que rolou.") },
                                    minLines = 8,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    if (draftText.isNotBlank()) {
                                        viewModel.saveBookSummary(bookId, draftText)
                                    }
                                    showEditDialog = false
                                }) { Text("Salvar", color = Oliva, fontWeight = FontWeight.SemiBold) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showEditDialog = false }) {
                                    Text("Cancelar", color = Muted)
                                }
                            }
                        )
                    }
                }
```

Importar:
```kotlin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import com.example.ui.components.TbButton
import com.example.ui.components.TbButtonVariant
import com.example.ui.theme.InkSoft
```

(InkSoft já é usado no arquivo.)

- [ ] **Step 13.4: Atualizar tab "frases" pra mostrar autor**

Substituir o bloco `"frases" -> { ... }` por:

```kotlin
                "frases" -> {
                    val members by viewModel.clubMembers.collectAsState()
                    if (quotes.isEmpty()) {
                        Text(
                            text = "Nenhuma frase guardada deste livro ainda.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = InterFontFamily,
                                color = Muted
                            ),
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        quotes.forEach { quote ->
                            val author = members.find { it.id == quote.userId }
                            val authorName = author?.nome ?: "Membro"
                            val authorAvatar = author?.avatarUrl ?: ""

                            Column {
                                QuoteCard(
                                    texto = quote.texto,
                                    ref = quote.capituloRef,
                                    onDelete = { viewModel.deleteQuote(quote) }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                                ) {
                                    Avatar(name = authorName, avatarUrl = authorAvatar, size = 20.dp)
                                    Text(
                                        text = "Salva por $authorName",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Muted)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TbButton(
                        text = "Salvar uma frase",
                        onClick = { showQuoteDialog = true },
                        variant = TbButtonVariant.Outline,
                        size = TbButtonSize.Md,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
```

Importar `Avatar` se ainda não está:
```kotlin
import com.example.ui.components.Avatar
```

- [ ] **Step 13.5: Adicionar tab "chat"**

Adicionar dentro do `when (tab)`, antes de `"historico"`:

```kotlin
                "chat" -> {
                    val chapters by viewModel.currentChapters.collectAsState()
                    val commentsFlow = remember(bookId) { viewModel.getCommentsForBookFlow(bookId) }
                    val comments by commentsFlow.collectAsState(initial = emptyList())
                    val members by viewModel.clubMembers.collectAsState()
                    val chapterById = chapters.associateBy { it.id }

                    if (comments.isEmpty()) {
                        Text(
                            text = "Esse livro não rendeu conversa por aqui.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = InterFontFamily,
                                color = Muted
                            ),
                            modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        var lastChapterId: String? = null
                        comments.forEach { c ->
                            val chapter = chapterById[c.chapterId]
                            if (chapter != null && chapter.id != lastChapterId) {
                                lastChapterId = chapter.id
                                Spacer(modifier = Modifier.height(20.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f).height(1.dp).background(DividerSoft))
                                    Text(
                                        text = "CAP. ${chapter.numero} · ${chapter.titulo}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = OlivaMid,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Box(modifier = Modifier.weight(1f).height(1.dp).background(DividerSoft))
                                }
                            }

                            val author = members.find { it.id == c.userId }
                            val authorName = author?.nome ?: "Membro"
                            val authorAvatar = author?.avatarUrl ?: ""

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Avatar(name = authorName, avatarUrl = authorAvatar, size = 32.dp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = authorName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = Ink
                                            )
                                        )
                                        Text(
                                            text = com.example.util.timeAgo(c.criadoEm),
                                            style = MaterialTheme.typography.labelSmall.copy(color = Muted)
                                        )
                                    }
                                    Text(
                                        text = c.texto,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = InkSoft,
                                            lineHeight = 20.sp
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "(somente leitura — comente em \"Livro atual\")",
                            style = MaterialTheme.typography.labelSmall.copy(color = Muted),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
```

Imports adicionais:
```kotlin
import androidx.compose.foundation.background
import com.example.ui.theme.DividerSoft
import com.example.ui.theme.OlivaMid
```

- [ ] **Step 13.6: Adicionar tab "avaliacoes"**

Antes de `"historico"` adicionar:

```kotlin
                "avaliacoes" -> {
                    val ratingsFlow = remember(bookId) { viewModel.getBookRatingsFlow(bookId) }
                    val ratings by ratingsFlow.collectAsState(initial = emptyList())
                    val myRatingFlow = remember(bookId) { viewModel.getBookRatingOfCurrentUserFlow(bookId) }
                    val myRating by myRatingFlow.collectAsState(initial = null)
                    val members by viewModel.clubMembers.collectAsState()

                    var showRatingDialog by remember { mutableStateOf(false) }
                    var draftStars by remember { mutableStateOf(0) }
                    var draftComment by remember { mutableStateOf("") }

                    val avg = if (ratings.isNotEmpty()) ratings.sumOf { it.stars }.toFloat() / ratings.size else 0f

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RatingStars(rating = avg, size = 28.dp, spacing = 4.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${"%.1f".format(avg)} de 5 · ${ratings.size} ${if (ratings.size == 1) "avaliação" else "avaliações"}",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Muted)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TbButton(
                        text = if (myRating != null) "Editar minha avaliação" else "Avaliar este livro",
                        onClick = {
                            draftStars = myRating?.stars ?: 0
                            draftComment = myRating?.comment ?: ""
                            showRatingDialog = true
                        },
                        variant = TbButtonVariant.Terra,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    ratings.sortedByDescending { it.updatedAt }.forEach { r ->
                        val author = members.find { it.id == r.userId }
                        val authorName = author?.nome ?: "Membro"
                        val authorAvatar = author?.avatarUrl ?: ""

                        TramabookCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Avatar(name = authorName, avatarUrl = authorAvatar, size = 28.dp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = authorName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = Ink
                                        )
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        RatingStars(rating = r.stars.toFloat(), size = 12.dp)
                                        Text(
                                            text = com.example.util.timeAgo(r.updatedAt),
                                            style = MaterialTheme.typography.labelSmall.copy(color = Muted)
                                        )
                                    }
                                }
                            }
                            if (r.comment.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = r.comment,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = InkSoft,
                                        lineHeight = 20.sp
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    val totalMembers = members.size
                    val ratedCount = ratings.size
                    if (totalMembers > ratedCount) {
                        Text(
                            text = "${totalMembers - ratedCount} ${if (totalMembers - ratedCount == 1) "membro ainda não avaliou" else "membros ainda não avaliaram"}",
                            style = MaterialTheme.typography.labelSmall.copy(color = Muted),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    if (showRatingDialog) {
                        AlertDialog(
                            onDismissRequest = { showRatingDialog = false },
                            title = { Text("Avaliar livro", style = MaterialTheme.typography.titleLarge.copy(fontFamily = LiterataFontFamily, fontWeight = FontWeight.SemiBold)) },
                            text = {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    RatingStarsInput(selected = draftStars, onChange = { draftStars = it })
                                    OutlinedTextField(
                                        value = draftComment,
                                        onValueChange = { draftComment = it },
                                        placeholder = { Text("Conta o que tu achou (opcional)") },
                                        minLines = 3,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        if (draftStars in 1..5) {
                                            viewModel.saveBookRating(bookId, draftStars, draftComment)
                                            showRatingDialog = false
                                        }
                                    },
                                    enabled = draftStars in 1..5
                                ) { Text("Salvar", color = Oliva, fontWeight = FontWeight.SemiBold) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showRatingDialog = false }) {
                                    Text("Cancelar", color = Muted)
                                }
                            }
                        )
                    }
                }
```

Imports adicionais:
```kotlin
import com.example.ui.components.RatingStars
import com.example.ui.components.RatingStarsInput
```

- [ ] **Step 13.7: Enriquecer tab "historico" com datas + edição admin**

Substituir o bloco `"historico" -> { ... }` por:

```kotlin
                "historico" -> {
                    val suggestionFlow = remember(bookId) { viewModel.getBookSuggestionFlow(bookId) }
                    val suggestion by suggestionFlow.collectAsState(initial = null)
                    val members by viewModel.clubMembers.collectAsState()
                    val isAdmin by viewModel.isCurrentUserAdmin.collectAsState()
                    val meetingDatesAll by viewModel.finishedBooksMeetingDates.collectAsState()
                    val dataEncontroLivro = meetingDatesAll[bookId]

                    val suggester = suggestion?.let { s -> members.find { it.id == s.suggestedByUserId }?.nome }

                    val milestones = buildList<Pair<String, String>> {
                        add(
                            "Sugerido${suggester?.let { " por $it" } ?: ""}" to
                            (suggestion?.let { "em ${com.example.util.formatShortDate(it.criadoEm)}" } ?: "O livro chegou na lista do clube.")
                        )
                        add("Leitura começou" to "O clube começou a ler junto.")
                        add(
                            "Encontro do clube" to (dataEncontroLivro?.let { "em ${com.example.util.formatShortDate(it)}" } ?: "O clube se reuniu pra discutir.")
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        milestones.forEachIndexed { index, (title, desc) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(24.dp)
                                ) {
                                    Box(modifier = Modifier.size(14.dp).background(Oliva, CircleShape))
                                    if (index < milestones.size - 1) {
                                        Box(modifier = Modifier.width(2.dp).height(56.dp).background(OlivaSoft))
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = LiterataFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Ink
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = InterFontFamily,
                                            color = Muted
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(if (index < milestones.size - 1) 44.dp else 0.dp))
                                }
                            }
                        }
                    }

                    if (isAdmin) {
                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(thickness = 0.5.dp, color = Divider)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "ADMIN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Terracota,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Data do encontro:",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Ink)
                            )
                            Text(
                                text = dataEncontroLivro?.let { com.example.util.formatShortDate(it) } ?: "não definida",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Terracota,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TbButton(
                                text = "Marcar como hoje",
                                onClick = { viewModel.setBookMeetingDate(bookId, System.currentTimeMillis()) },
                                variant = TbButtonVariant.Outline,
                                size = TbButtonSize.Sm
                            )
                            if (dataEncontroLivro != null) {
                                TbButton(
                                    text = "Limpar",
                                    onClick = { viewModel.setBookMeetingDate(bookId, null) },
                                    variant = TbButtonVariant.Outline,
                                    size = TbButtonSize.Sm
                                )
                            }
                        }
                    }
                }
```

Imports adicionais (se faltarem):
```kotlin
import androidx.compose.material3.HorizontalDivider
import com.example.ui.components.TbButtonSize
import com.example.ui.theme.Terracota
import com.example.ui.theme.Divider
```

- [ ] **Step 13.8: Build**

Run: `./gradlew assembleDebug`  
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 13.9: Commit**

```bash
git add app/src/main/java/com/example/ui/screens/BookDetailScreen.kt
git commit -m "$(cat <<'EOF'
feat(book-detail): 5 abas (Resumo wiki, Frases com autor, Chat retrospectivo, Avaliações, Histórico)

- Banner mostra data do encontro quando disponível
- Resumo: wiki coletivo editável por qualquer membro
- Frases: avatar + nome de quem salvou
- Chat: feed unificado de comentários com separador por capítulo (read-only)
- Avaliações: 1-5 estrelas + comentário opcional, média no topo
- Histórico: datas reais; admin pode marcar/limpar data do encontro

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 14: Votação evoluída (UI) — header dinâmico + justificativa + admin

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/NextTabScreen.kt`

- [ ] **Step 14.1: Adicionar imports necessários no topo**

Adicionar em [NextTabScreen.kt](app/src/main/java/com/example/ui/screens/NextTabScreen.kt) (topo, no bloco de imports):

```kotlin
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import com.example.ui.components.RatingStars
```

(Alguns já podem estar — não duplicar.)

- [ ] **Step 14.2: Substituir o composable `VotacaoTab` inteiro**

Localizar o composable `VotacaoTab` em [NextTabScreen.kt:549-713](app/src/main/java/com/example/ui/screens/NextTabScreen.kt#L549-L713).

Substituir o composable INTEIRO por:

```kotlin
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun VotacaoTab(
    viewModel: MainViewModel,
    onNavigateToSuggestBook: () -> Unit
) {
    val suggestedBooks by viewModel.suggestedBooks.collectAsState()
    val nextBooks by viewModel.nextBooks.collectAsState()
    val votes by viewModel.suggestionsAndVotes.collectAsState()
    val members by viewModel.clubMembers.collectAsState()
    val activeRound by viewModel.activeVotingRound.collectAsState()
    val suggestionsByBookId by viewModel.bookSuggestionsByBookId.collectAsState()
    val isAdmin by viewModel.isCurrentUserAdmin.collectAsState()

    val currentUserId = viewModel.currentUserId.collectAsState().value ?: "user_voce"

    val totalVotes = votes.size

    var showOpenSheet by remember { mutableStateOf(false) }
    var showCloseDialog by remember { mutableStateOf(false) }
    var justificationSheetFor by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header do card de votação
        item {
            TramabookCard {
                Text(
                    text = "Votação do próximo livro",
                    style = MaterialTheme.typography.headlineLarge.copy(color = OlivaDark),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                if (activeRound != null) {
                    val r = activeRound!!
                    val dataLabel = com.example.util.formatShortDate(r.fechaEm)
                    val nLabel = if (r.nLivros == 1) "Escolham o próximo livro" else "Escolham os próximos ${r.nLivros} livros"
                    Text(
                        text = "Aberta até $dataLabel · $nLabel",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Tertiary
                    )
                } else {
                    Text(
                        text = "Não há votação aberta no momento.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Muted
                    )
                    if (isAdmin) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TbButton(
                            text = "Abrir nova votação",
                            onClick = { showOpenSheet = true },
                            variant = TbButtonVariant.Terra,
                            size = TbButtonSize.Md,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (activeRound != null) {
            if (suggestedBooks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Nenhum livro sugerido ainda.", style = MaterialTheme.typography.bodyLarge, color = Muted)
                    }
                }
            } else {
                items(suggestedBooks) { book ->
                    val bookVotes = votes.filter { it.clubBookId == book.id }
                    val hasUserVoted = bookVotes.any { it.userId == currentUserId }
                    val userVotesInRound = votes.count { it.userId == currentUserId }
                    val limitReached = !hasUserVoted && userVotesInRound >= (activeRound?.nLivros ?: 1)
                    val pct = if (totalVotes > 0) bookVotes.size.toFloat() / totalVotes.toFloat() else 0f
                    val hasJustification = suggestionsByBookId[book.id]?.justificativa?.isNotBlank() == true

                    TramabookCard(modifier = Modifier.clickable { viewModel.voteForBook(book.id) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Cover(
                                title = book.title, author = book.author, coverUrl = book.coverUrl,
                                width = 64.dp, height = 96.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = book.title,
                                                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 16.sp, color = Ink),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (hasJustification) {
                                                Icon(
                                                    imageVector = Icons.Outlined.ChatBubbleOutline,
                                                    contentDescription = "Ver justificativa",
                                                    tint = OlivaMid,
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clickable { justificationSheetFor = book.id }
                                                )
                                            }
                                        }
                                        Text(text = book.author, style = MaterialTheme.typography.bodyLarge.copy(color = Muted))
                                    }
                                    if (hasUserVoted) {
                                        Pill(text = "Teu voto", variant = PillVariant.OliveDeep, modifier = Modifier.padding(start = 8.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                ProgressBar(
                                    value = pct,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = if (hasUserVoted) Oliva else Terracota,
                                    track = DividerSoft,
                                    height = 8.dp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${bookVotes.size} ${if (bookVotes.size == 1) "voto" else "votos"}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Ink)
                                    )
                                    Text(
                                        text = "${(pct * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Muted)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                TbButton(
                                    text = when {
                                        hasUserVoted -> "Teu voto"
                                        limitReached -> "Limite de votos atingido"
                                        else -> "Votar nesse"
                                    },
                                    onClick = { if (!limitReached || hasUserVoted) viewModel.voteForBook(book.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                    variant = if (hasUserVoted) TbButtonVariant.OlivaSoft else TbButtonVariant.Primary,
                                    size = TbButtonSize.Sm,
                                    enabled = !(limitReached && !hasUserVoted)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Fila do clube (status = "next")
        if (nextBooks.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).background(Terracota, androidx.compose.foundation.shape.CircleShape))
                    Text(
                        text = "FILA DO CLUBE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Terracota
                        )
                    )
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(nextBooks, key = { it.id }) { b ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(80.dp)
                        ) {
                            Cover(title = b.title, author = b.author, coverUrl = b.coverUrl, width = 64.dp, height = 96.dp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = b.title,
                                style = MaterialTheme.typography.labelSmall.copy(color = Ink),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            TbButton(
                text = "Sugerir livro",
                onClick = onNavigateToSuggestBook,
                modifier = Modifier.fillMaxWidth(),
                variant = TbButtonVariant.Outline
            )
            if (isAdmin && activeRound != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TbButton(
                    text = "Encerrar votação agora",
                    onClick = { showCloseDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    variant = TbButtonVariant.Outline
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Sheet de justificativa
    if (justificationSheetFor != null) {
        val bookId = justificationSheetFor!!
        val sug = suggestionsByBookId[bookId]
        val author = members.find { it.id == sug?.suggestedByUserId }
        ModalBottomSheet(
            onDismissRequest = { justificationSheetFor = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Avatar(name = author?.nome ?: "Membro", avatarUrl = author?.avatarUrl ?: "", size = 32.dp)
                    Text(
                        text = "${author?.nome ?: "Membro"} sugeriu",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
                Text(
                    text = "\"${sug?.justificativa ?: ""}\"",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = InkSoft,
                        lineHeight = 20.sp
                    )
                )
                TbButton(
                    text = "Fechar",
                    onClick = { justificationSheetFor = null },
                    variant = TbButtonVariant.Outline,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Dialog: encerrar votação
    if (showCloseDialog) {
        AlertDialog(
            onDismissRequest = { showCloseDialog = false },
            title = { Text("Encerrar votação?") },
            text = {
                Text(
                    "Encerrar agora vai escolher os ${activeRound?.nLivros ?: 1} livro(s) mais votado(s). O atual passa para a estante. Sem volta.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.closeActiveVotingRound()
                    showCloseDialog = false
                }) { Text("Encerrar", color = Terracota, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showCloseDialog = false }) { Text("Cancelar", color = Muted) }
            }
        )
    }

    // Sheet: abrir votação
    if (showOpenSheet) {
        OpenVotingSheet(
            onDismiss = { showOpenSheet = false },
            onConfirm = { n, dias, cadencia ->
                viewModel.openVotingRound(n, dias, cadencia)
                showOpenSheet = false
            }
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun OpenVotingSheet(
    onDismiss: () -> Unit,
    onConfirm: (nLivros: Int, durationDays: Int, cadencia: String) -> Unit
) {
    var n by remember { mutableStateOf(1) }
    var dias by remember { mutableStateOf(7) }
    var cadencia by remember { mutableStateOf("unica") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "Abrir votação do clube",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 20.sp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Quantos livros vamos escolher?", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TbButton(text = "−", onClick = { if (n > 1) n-- }, variant = TbButtonVariant.Outline, size = TbButtonSize.Sm)
                    Text("$n", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 22.sp))
                    TbButton(text = "+", onClick = { if (n < 12) n++ }, variant = TbButtonVariant.Outline, size = TbButtonSize.Sm)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Por quanto tempo a votação fica aberta?", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3, 7, 14, 30).forEach { d ->
                        val selected = dias == d
                        Box(modifier = Modifier.clickable { dias = d }) {
                            Pill(
                                text = "$d dias",
                                variant = if (selected) PillVariant.OliveDeep else PillVariant.Default
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Qual a cadência?", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("unica" to "Única", "semanal" to "Semanal", "quinzenal" to "Quinzenal", "mensal" to "Mensal").forEach { (key, label) ->
                        val selected = cadencia == key
                        Box(modifier = Modifier.clickable { cadencia = key }) {
                            Pill(
                                text = label,
                                variant = if (selected) PillVariant.OliveDeep else PillVariant.Default
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TbButton(text = "Cancelar", onClick = onDismiss, variant = TbButtonVariant.Outline, modifier = Modifier.weight(1f))
                TbButton(text = "Abrir votação", onClick = { onConfirm(n, dias, cadencia) }, variant = TbButtonVariant.Terra, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
```

Importar adicionais ao topo se faltarem:
```kotlin
import com.example.ui.theme.InkSoft
```

- [ ] **Step 14.3: Build**

Run: `./gradlew assembleDebug`  
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 14.4: Commit**

```bash
git add app/src/main/java/com/example/ui/screens/NextTabScreen.kt
git commit -m "$(cat <<'EOF'
feat(voting): rodadas reais com prazo, N livros, justificativa e fila

- Header mostra prazo + N livros dinâmico
- Ícone de comentário no card quando há justificativa; clique abre sheet
- Votação respeita N: bloqueia além do limite, desfaz clicando de novo
- Admin vê "Abrir votação" (sheet de N + dias + cadência) e
  "Encerrar votação agora" (com diálogo de confirmação)
- "Fila do clube" mostra livros em status next abaixo das sugestões

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 15: Build final + testes + validação manual

- [ ] **Step 15.1: Build full clean**

Run: `./gradlew clean assembleDebug`  
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 15.2: Rodar suite de testes**

Run: `./gradlew testDebugUnitTest`  
Expected: `BUILD SUCCESSFUL`. Se algum screenshot test falhar por mudanças visuais esperadas (5 tabs na navbar, novo content), atualizar baselines com `./gradlew recordRoborazziDebug` e commitar separadamente.

- [ ] **Step 15.3: Smoke manual via emulador (se disponível)**

Se houver dispositivo/emulador conectado:

```bash
./gradlew installDebug
adb shell am start -n app.tramabook/com.example.MainActivity
```

Verificar manualmente os critérios de aceite §11 do [spec](../specs/2026-05-23-fase4-estante-votacao-design.md):
1. OTP cabe em telas estreitas
2. "DOMINGO" não quebra
3. 5 tabs visíveis na navbar
4. Estante mostra data do encontro e estrelas reais
5. 5 abas no detalhe do livro
6. Resumo wiki funciona
7. Chat retrospectivo agrupado por capítulo
8. Avaliações funcionam
9. Ícone de comentário aparece em sugestões com justificativa
10. Admin pode abrir/encerrar votação
11. Votar em até N livros respeita o limite
12. Fila do clube aparece após encerrar
13. Encerrar manual funciona
14. Encerrar automático funciona

Se algum item falhar, registrar como follow-up. Bugs críticos devem virar tasks novas no plano.

- [ ] **Step 15.4: Push final**

Run:
```bash
git push origin master
```

Expected: push aceito.

---

## Self-Review

### Cobertura do spec

| Seção do spec | Tasks que implementam |
|---|---|
| §3.1 OTP responsivo | Task 1 |
| §3.2 DOMINGO fix | Task 2 |
| §4 Navbar 5 tabs | Task 12 |
| §5 Estante tab nova | Task 12 |
| §6.2 Banner com data | Task 13.1 |
| §6.3 Resumo wiki | Task 13.3 |
| §6.4 Frases com autor | Task 13.4 |
| §6.5 Chat retrospectivo | Task 13.5 |
| §6.6 Avaliações | Task 13.6 |
| §6.7 Histórico com datas | Task 13.7 |
| §7.2 Modelo VotingRound + BookSuggestion | Tasks 3, 5 |
| §7.3 Abrir rodada (admin) | Task 14 (OpenVotingSheet) + Task 10.5 (`openVotingRound`) |
| §7.4 Votar em N + justificativa + fila | Task 14 + Task 10.4 |
| §7.5 Encerrar (manual + auto) | Task 10.5 (`closeActiveVotingRound`, `maybeAutoCloseExpiredRound`), Task 14 (UI), Task 10.7 (auto no init) |
| §7.6 Sugestão persistir justificativa | Task 10.6 |
| §8 Admin marca dataEncontro | Task 13.7 |
| §9 Migração de schema | Tasks 3, 5 |
| §9.4 Seed novo | Task 9 |
| §11 Aceites | Task 15.3 |

### Placeholder scan

Nenhum TBD/TODO/"implement later". Cada step tem código completo.

### Type consistency

- `VotingRound` campos consistentes em §9 do spec e Tasks 3, 5, 6, 10.
- `BookSummary(bookId, clubId, texto, lastEditorId, updatedAt)` consistente.
- `BookRating(bookId, clubId, userId, stars, comment, updatedAt)` consistente.
- Métodos do ViewModel: `getBookSummaryFlow`, `getBookRatingsFlow`, `getBookRatingOfCurrentUserFlow`, `getBookSuggestionFlow`, `getCommentsForBookFlow`, `saveBookSummary`, `saveBookRating`, `setBookMeetingDate`, `openVotingRound`, `closeActiveVotingRound`, `maybeAutoCloseExpiredRound` — todos consumidos exatamente como definidos.
- `VotingTally.rank(votes, suggestionsByBookId, n)` chamado igual no test (Task 11) e no ViewModel (Task 11.2).

### Riscos remanescentes

- **Task 13.1 (banner do BookDetail)** mistura código novo com edits no meio de uma função grande. Engineer precisa atenção pra colocar `meetingDates` no escopo certo. Mitigação: o passo deixa claro "Mover a leitura de meetingDates pra cima".
- **Task 14 (VotacaoTab)** é substituição total — o passo é grande mas autocontido. Justificativa: refazer é mais seguro que editar trecho por trecho num composable de 165 linhas.
- **Screenshot tests podem quebrar.** Mitigação na Task 15.2 (re-record baselines como commit separado).
