# Fase 9B — Remote Repository + ligar UI no Supabase — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans. Inline execution porque a maior parte do trabalho e mecanica e iterativa (93 metodos do RodapeRepository pra portar).

**Goal:** Substituir o `RodapeRepository` (Room) por um `RemoteRepository` (Supabase) que expoe a **mesma interface publica**, trocar 1 linha no `MainViewModel`, e arrancar o auto-login fake / seedDatabase / login(name, email) — sem ainda apagar Room (limpeza fica pra 9C).

**Estrategia chave:** Manter `RemoteRepository` com **assinaturas identicas** ao `RodapeRepository` (mesmos nomes de funcao, mesmos parametros, mesmos tipos de retorno `Flow<T>` / `suspend fun`). Isso reduz a refatoracao dos 150 call-sites no `MainViewModel` para apenas trocar a instancia. Cada metodo internamente bate em Postgrest/Realtime/RPC em vez de Room.

**Tech Stack:** supabase-kt 3.6 (postgrest, realtime, RPC), kotlinx.serialization, kotlinx.coroutines Flow.

---

## File Structure

**Create:**
- `app/src/main/java/com/example/data/remote/dto/Dtos.kt` — `@Serializable data class` por tabela (22 DTOs). Mapeiam para os enums/tipos Postgres. Cada DTO tem extension `toDomain()` retornando o `data class` antigo de `data/model/Entities.kt` (mantemos o domain inalterado pra UI nao mudar nada).
- `app/src/main/java/com/example/data/remote/RemoteRepository.kt` — classe principal com as 93 funcoes. Internamente delega para `Supabase.client.from("tabela")` ou `Supabase.client.postgrest.rpc("nome", args)`.

**Modify:**
- `app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt`:
  - Trocar `private val repository = RodapeRepository(database.rodapeDao())` por `private val repository = RemoteRepository()`.
  - **Apagar** o bloco `init {}` que chama `seedDatabase()` + cria sessao fake.
  - **Apagar** funcao `login(name, email, onCompleted)` antiga inteira (linha ~278-313).
  - **Apagar** funcao `seedNewClubData(clubId)` inteira (linha ~367-409).
  - **Apagar** lista `randomPresets` dentro de `login` (sumiu junto).
  - `currentUserId` passa a derivar de `authRepository.currentUserIdFlow` (que ja existe — em 9A criamos `supabaseUserId` como derivado). Renomear `supabaseUserId` -> `currentUserId` removendo o antigo.
  - `currentUser`: passa a vir de `repository.getProfileFlow(currentUserId)` (que mapeia `profiles` row pra `User` domain).
  - `userName`, `userEmail`: derivam de `currentUser` em vez do DataStore.
  - `activeClubId`: vira `MutableStateFlow<String?>` em memoria. Quando `currentUserId` mudar, `init` carrega `allClubs.firstOrNull()?.id`. `selectActiveClub` so faz `_activeClubId.value = clubId`.
  - `updateUserProfile(nome, email, avatarUrl)`: chama `repository.updateProfile(...)`.
  - `bumpEngagement`, `markAppRated`, `fontScale`, `setFontScale`: por enquanto continuam no `DataStoreManager` (sai na 9C para `profiles.font_scale`).
- `app/src/main/java/com/example/MainActivity.kt`:
  - `startDestination` continua usando `supabaseUserId` (sem mudanca — ja foi feito na 9A). Renomear a referencia se mudar nome na VM.
- `app/src/main/java/com/example/ui/screens/WelcomeScreen.kt`:
  - **Esconder** tab "Com link" do `JoinClubScreen` (esta na linha ~687).

**Not touched:** Room, `RodapeDao`, `RodapeRepository`, `DataStoreManager`. Saem todos na 9C.

---

## Plano executavel

Cada Task tem build verde + commit ao final. Como sao muitos metodos, a Task 1 cria a **estrutura** + 5 metodos representativos pra validar o pattern; as Tasks 2-5 portam por area (clubes / livros / votacao / discussao / encontros) com **freedom para adaptar** o codigo conforme aprendemos como `supabase-kt 3.6` se comporta.

### Task 1: Esqueleto — DTOs core (profiles, clubs, club_members) + RemoteRepository inicial

**Files:**
- Create: `app/src/main/java/com/example/data/remote/dto/Dtos.kt`
- Create: `app/src/main/java/com/example/data/remote/RemoteRepository.kt`

- [ ] **Step 1:** Em `Dtos.kt`, criar `@Serializable` DTOs para: `profiles` (ProfileDto), `clubs` (ClubDto), `club_members` (ClubMemberDto). Cada um com `toDomain()` retornando User / Club / ClubMember de `data/model/Entities.kt`.

  Pattern minimo:

  ```kotlin
  @Serializable
  data class ProfileDto(
      val id: String,
      val nome: String,
      val email: String,
      @SerialName("avatar_url") val avatarUrl: String = "",
  ) {
      fun toDomain() = User(id, nome, email, avatarUrl)
  }
  ```

- [ ] **Step 2:** Em `RemoteRepository.kt`, criar `class RemoteRepository(private val supabase: SupabaseClient = Supabase.client)` com APENAS 6 metodos pra validar o pattern:

  - `fun getUserFlow(userId: String): Flow<User?>` — usa `from("profiles").select { filter { eq("id", userId) } }.decodeAsOrNull<ProfileDto>().toDomain()` + Realtime channel para emitir updates.
  - `suspend fun getUser(userId: String): User?`
  - `suspend fun insertUser(user: User)` — upsert em `profiles` (so atualiza, signup ja criou via trigger).
  - `fun getClubFlow(clubId: String): Flow<Club?>`
  - `suspend fun getClub(clubId: String): Club?`
  - `fun getClubsForUser(userId: String): Flow<List<Club>>` — usa `select { filter { eq("user_id", userId) }; ... }` ou RPC `my_clubs()`.

- [ ] **Step 3:** Build `:app:compileDebugKotlin`. Iterar ate verde.

- [ ] **Step 4:** Commit.

### Task 2: Portar todos os 93 metodos do RodapeRepository pro RemoteRepository

> **Estrategia iterativa:** copiar a assinatura de cada metodo do `RodapeRepository.kt`, implementar usando o pattern do Task 1, agrupar em commits por area:
> - Books + ClubBooks + Chapters + UserProgress
> - Comments + Reactions + SavedQuotes
> - Voting (votes + voting_rounds) + RPC `close_voting_round`
> - Meetings + MeetingRsvps + MeetingPatterns + MeetingMinutes + MeetingNotes
> - Notifications + ModerationLog (MemberRemoval)
> - Book wikis: BookSummary + BookRating + BookSuggestion

- [ ] **Step 1:** Para cada metodo: replicar assinatura, implementar com Postgrest + Realtime + RPC quando aplicavel.
- [ ] **Step 2:** Compilar incrementalmente, commitar por area (~6 commits).
- [ ] **Step 3:** Build final verde.

### Task 3: Trocar referencia no MainViewModel + arrancar seed/login fake

- [ ] **Step 1:** Trocar `RodapeRepository(database.rodapeDao())` por `RemoteRepository()`.
- [ ] **Step 2:** Apagar bloco `init {}` que faz `seedDatabase()` + auto-login.
- [ ] **Step 3:** Apagar funcao `login(name, email, ...)` antiga.
- [ ] **Step 4:** Apagar funcao `seedNewClubData(clubId)` (e a chamada interna em `createClub`).
- [ ] **Step 5:** Reescrever fluxos de sessao:
  - `currentUserId`: derivar de `authRepository.currentUserIdFlow`.
  - `userName`, `userEmail`: derivar de `currentUser` (que vem do `profiles`).
  - `activeClubId`: trocar pra `MutableStateFlow` em memoria.
- [ ] **Step 6:** Build verde. Commit.

### Task 4: Esconder tab "Com link" do JoinClubScreen

- [ ] **Step 1:** Em `WelcomeScreen.kt`, na funcao `JoinClubScreen` (~linha 687), remover/ocultar a aba "Com link" — manter so a aba "Código".
- [ ] **Step 2:** Build. Commit.

### Task 5: Smoke test no emulador

- [ ] **Step 1:** `./gradlew :app:installDebug`
- [ ] **Step 2:** Cadastrar conta nova → criar clube → ver codigo → outro emulador entra com codigo → sugerir livro → votar → fechar round → comentar → reagir.
- [ ] **Step 3:** Verificar Realtime: editar resumo em uma conta, ver atualizar em outra.

---

## Self-review

Plano e propositalmente direcional na Task 2 — listar codigo dos 93 metodos no plano antes da execucao seria desperdicio e desatualizaria rapido. O agente executor (eu mesma inline) decide cada implementacao com base na doc do supabase-kt e no schema real.

## Anti-checklist

- ❌ NAO apagar Room nesta sub-fase (apagar quebra build durante migracao). Sai na 9C.
- ❌ NAO migrar font_scale pra profiles. Sai na 9C.
- ❌ NAO refatorar MainViewModel em VMs por escopo. Fase futura.
- ❌ NAO mexer no upload de capa manual (file:// local continua funcionando). Sai na 9C.
