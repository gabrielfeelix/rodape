# Fase 9 — Migrar o app Android pra Supabase

**Data:** 2026-05-24
**Autor:** brainstorm com Claude (decisões já travadas via memória de projeto)
**Depende de:** Fase 8 (backend Supabase aplicado e validado em 2026-05-24)

## Objetivo

Trocar a fundação local do Rodapé (Room + DataStore-session + seeds hardcoded + login fake)
pelo backend Supabase já provisionado. O app passa a:

- Autenticar usuários reais (Email/Senha + Google Sign-In).
- Ler/escrever todos os dados via PostgREST + Realtime.
- Subir capas customizadas pro bucket `book-covers`.
- Nascer **vazio** em produção (sem demo user, sem seeds, sem auto-login).

A UI Compose existente (22 telas, ~1.100 linhas em `WelcomeScreen.kt`, etc.) é
preservada: só as fontes de dados mudam. O `MainViewModel` (1376 linhas) **continua
monolítico** nesta fase — refator de VM por escopo fica para fase futura.

## Estado atual relevante

- **`MainViewModel`** (`app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt`):
  AndroidViewModel único, 64 funções públicas, lê `RodapeRepository` (Room) +
  `DataStoreManager`. `init {}` chama `repository.seedDatabase()` e cria sessão
  fake `user_voce` se DataStore estiver vazio. `login()` aceita qualquer email
  e gera UUID local.
- **`RodapeRepository`** (497 linhas): wrapper fino sobre `RodapeDao`. Todas as
  operações são suspend ou retornam `Flow<*>` do Room.
- **`RodapeDao`** (346 linhas): queries SQL Room sobre 22 entidades em
  `data/model/Entities.kt`.
- **`DataStoreManager`**: guarda `user_id/user_name/user_email/active_club_id`
  (sessão fake) + `rated_app`, `engagement_count`, `font_scale` (preferências).
- **`MainActivity`**: NavHost que entra em `welcome` ou `main_tabs` baseado em
  `viewModel.currentUserId`.
- **`AndroidManifest.xml`**: sem intent-filter (deep link OAuth ainda não plugado).
- **`RodapeApp : Application`**: **não existe**. Vamos criar.
- **`.env`**: já tem `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`, `SUPABASE_ANON_KEY`,
  `GOOGLE_WEB_CLIENT_ID`, `GOOGLE_ANDROID_CLIENT_ID_DEBUG`. `service_role` e
  `secret_key` estão lá mas **não** serão lidos pelo app.
- **Plugin Secrets Gradle** já configurado pra ler `.env` → `BuildConfig`.

## Decisões travadas (não revisitar)

- App nasce vazio em produção (sem seeds, sem demo user, sem auto-login).
- 1 voto por round (PK em `votes` é `(voting_round_id, user_id)`).
- Comentário removido = soft delete + placeholder na UI.
- Catálogo `books` é global; dedup via `club_books`.
- Push FCM fica para outra fase.
- Convite só por código de 6 chars; tab "Com link" do `JoinClubScreen` deve sumir.
- App exige conexão (sem modo offline nesta fase).
- Cliente **nunca** segura `service_role`/`secret_key`. Operações privilegiadas
  via RPC `SECURITY DEFINER` chamadas como usuário autenticado.
- `MainViewModel` permanece monolítico nesta fase (opção A do brainstorm).

## Arquitetura alvo

```
Compose UI (existente, 22 telas, sem mudança estrutural)
        │
        ▼
MainViewModel (existente, monolítico — funções internas trocam de fonte)
        │
        ▼
data/remote/*Repository.kt  ◄── novo (substitui data/repository/RodapeRepository.kt)
        │  Kotlin coroutines + Flow vindo de Realtime
        ▼
SupabaseClient (singleton em RodapeApp)
        │
        ▼
Supabase Cloud (zfbywoeajebvasnsrzfh)
  • Auth (GoTrue)  • PostgREST  • Realtime  • Storage  • Postgres+RLS+RPCs
```

### Singleton SupabaseClient

`com.example.data.remote.Supabase` (object):
- Cria `createSupabaseClient(url, key)` com módulos `Auth`, `Postgrest`, `Realtime`, `Storage`.
- `url` e `key` vêm de `BuildConfig.SUPABASE_URL` / `BuildConfig.SUPABASE_PUBLISHABLE_KEY`.
- `Auth.flowType = PKCE`, `defaultRedirectUrl = "app.rodape://login-callback"`.
- Inicializado lazy no primeiro acesso. `RodapeApp.onCreate()` toca no objeto pra
  forçar warm-up (opcional — Kotlin object é lazy thread-safe por padrão).

### `RodapeApp : Application` (novo)

```kotlin
package com.example
class RodapeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Toca no singleton pra inicializar o cliente cedo.
        com.example.data.remote.Supabase.client
    }
}
```

`AndroidManifest.xml` ganha `android:name=".RodapeApp"` em `<application>`.

### Camada `data/remote/` — repositórios

Um arquivo por agregado, todos no pacote `com.example.data.remote`. Cada repo:

- Usa `Supabase.client.from("tabela").select { ... }` pra leituras.
- Combina select inicial + `Supabase.client.channel("nome").postgresChangeFlow<>()`
  pra Realtime, emitindo `Flow<List<T>>` que a UI coleta.
- Chama RPCs via `Supabase.client.postgrest.rpc("nome", params)`.
- Faz uploads/downloads via `Supabase.client.storage.from("bucket")`.

**Repos previstos:**

| Repo | Tabelas/RPCs principais |
|------|-------------------------|
| `AuthRepository` | Auth (signUp, signIn, signInWithGoogle via IDToken, resetPassword, signOut, observa `sessionStatus`) |
| `ProfileRepository` | `profiles` (perfil do usuário corrente, `font_scale`, avatar) |
| `ClubRepository` | `clubs`, `club_members`, RPCs `create_club`, `join_club_with_code`, `leave_club`, `regenerate_invite_code`, `promote_member`, `demote_admin`, `transfer_super_admin`, `remove_member` |
| `BookRepository` | `books`, `club_books`, `book_summaries`, `book_ratings`, `book_suggestions` |
| `ChapterRepository` | `chapters`, `user_progress` |
| `MeetingRepository` | `meetings`, `meeting_patterns`, `meeting_rsvps`, `meeting_minutes`, `meeting_notes` |
| `VotingRepository` | `voting_rounds`, `votes`, RPC `close_voting_round` |
| `CommentRepository` | `comments`, `reactions` |
| `QuoteRepository` | `saved_quotes` |
| `NotificationRepository` | `notifications` |
| `ModerationRepository` | `member_removals` |

Cada um expõe `Flow<*>` para listas reativas e `suspend fun` para mutações. **Não há
cache local** — leituras vão sempre ao servidor (mas Realtime mantém os fluxos vivos).

### DataStore — o que sobrevive

`DataStoreManager` perde tudo de sessão (`USER_ID_KEY`, `USER_NAME_KEY`,
`USER_EMAIL_KEY`, `ACTIVE_CLUB_ID_KEY`). Sobrevivem:

- `RATED_APP_KEY` (boolean local — não vira coluna no banco).
- `ENGAGEMENT_COUNT_KEY` (int local — idem).
- `FONT_SCALE_KEY` → **migrado** integralmente para `profiles.font_scale` no banco.
  DataStore deixa de guardar font_scale. App lê e escreve via `ProfileRepository`;
  100% no servidor. Sem cache local nesta fase.
- `activeClubId`: vira `MutableStateFlow` em memória no `MainViewModel`, inicializado
  com o primeiro clube do usuário ao logar. **Não persiste entre cold-starts** —
  ao reabrir o app, cai no primeiro clube e usuário troca pelo seletor se quiser.

### Telas de auth

- **`WelcomeScreen`**: sem mudança visual. Botões "Entrar" / "Criar clube" / "Entrar em clube" navegam pra `login`/`signup`/etc. Botão "Continuar com Google" entra em `LoginScreen`.
- **`LoginScreen`** (em `WelcomeScreen.kt`, linha 175): reescrita pra
  validar email/senha reais via `AuthRepository.signIn`. Botão "Convidado Google"
  some — substituído por fluxo Credential Manager + Google ID Token →
  `auth.signInWith(IDToken)`. Adiciona link "Esqueci minha senha".
- **`SignUpScreen` (nova)**: cadastro com email/senha + nome. Após signUp, mostra
  "enviamos um email de confirmação" (porque "Confirm email" está ON). Após
  confirmação, usuário entra normalmente. Trigger `handle_new_user` cria
  `profiles` row automaticamente.
- **`ForgotPasswordScreen` (nova)**: campo email → `AuthRepository.resetPassword(email)`
  → mostra "verifique seu email". Email contém deep link `app.rodape://login-callback`
  que cai em `ResetPasswordScreen` (nova) com campo "nova senha".
- **`JoinClubScreen`** (linha 825): esconder tab "Com link". Manter só tab "Código".

### Deep link OAuth + reset

`AndroidManifest.xml` ganha:

```xml
<activity android:name=".MainActivity" ...>
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
    <intent-filter android:autoVerify="false">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="app.rodape" android:host="login-callback" />
    </intent-filter>
</activity>
```

`MainActivity.onCreate` / `onNewIntent` chama `Supabase.client.handleDeeplinks(intent)`.

### Google Sign-In (Credential Manager)

Usa `androidx.credentials:credentials:1.3.0` + `androidx.credentials:credentials-play-services-auth:1.3.0` + `com.google.android.libraries.identity.googleid:googleid:1.1.1`.

Fluxo em `LoginScreen`:
1. Botão "Entrar com Google" → `CredentialManager.getCredential(...)` com
   `GetGoogleIdOption(serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID)`.
2. Recebe `GoogleIdTokenCredential` → extrai `idToken`.
3. `Supabase.client.auth.signInWith(IDToken) { provider = Google; idToken = it }`.
4. Realtime/Session passa a estar autenticado; UI navega pra `main_tabs`.

### Upload de capa manual

`AddBookManualScreen` hoje grava `file://` local em `book.coverUrl`. Vai mudar:

1. Usuário escolhe foto / tira foto via CameraX.
2. `BookRepository.createManualBook(...)` insere `books` row (cover vazio).
3. Sobe bytes pro path `<club_id>/<book_id>/<uuid>.jpg` no bucket `book-covers`.
4. Atualiza `books.cover_url` com a URL pública.

## Sub-fases

A fase é gigante (~30 arquivos). Quebrada em 3 sub-fases atômicas; cada uma
build verde + commit ao final.

### Sub-fase 9A — fundação Supabase + auth real

Bloco mínimo pra usuário conseguir logar de verdade. Backend Room continua
funcionando em paralelo. Smoke: criar conta, confirmar email, logar.

**Entregáveis:**
- `gradle/libs.versions.toml`: adicionar deps Supabase-kt + Credential Manager + googleid + ktor.
- `app/build.gradle.kts`: incluir as novas deps + plugin `org.jetbrains.kotlin.plugin.serialization`.
- `com.example.RodapeApp` (Application) criado.
- `AndroidManifest.xml`: `android:name=".RodapeApp"` + intent-filter `app.rodape://login-callback`.
- `com.example.data.remote.Supabase` (object singleton).
- `com.example.data.remote.AuthRepository`.
- `MainActivity.onCreate`/`onNewIntent`: `handleDeeplinks`.
- `LoginScreen` reescrita: form email/senha + botão Google + "Esqueci a senha".
- `SignUpScreen`, `ForgotPasswordScreen`, `ResetPasswordScreen` (novas, em arquivos próprios).
- Navegação no `MainActivity` ganha rotas `signup`, `forgot_password`, `reset_password`.
- `MainViewModel.currentUserId` passa a derivar de `AuthRepository.sessionStatus`
  (em paralelo ao DataStore antigo — DataStore some na 9C).

### Sub-fase 9B — 11 repos remotos + trocar `RodapeRepository` por eles

Cirurgia no `MainViewModel`: cada `repository.X(...)` vira `nomeRepository.X(...)`.
Telas continuam coletando os mesmos `StateFlow`s — sem mudança de UI.

**Entregáveis:**
- 11 arquivos em `com.example.data.remote`: `ProfileRepository`, `ClubRepository`,
  `BookRepository`, `ChapterRepository`, `MeetingRepository`, `VotingRepository`,
  `CommentRepository`, `QuoteRepository`, `RatingRepository`, `SummaryRepository`,
  `NotificationRepository`, `ModerationRepository`.
- DTOs em `com.example.data.remote.dto.*` (uma class por tabela do banco com
  `@Serializable`; nomes em camelCase com `@SerialName("snake_case")` quando bater).
- `MainViewModel`: instanciar 11 repos, trocar todas as referências de
  `repository.` para os novos. Remover `repository.seedDatabase()`, `seedNewClubData(clubId)`,
  bloco de auto-login fake no `init {}`, lista `randomPresets` (move pra novo helper
  `AvatarPresets` em `ui/components/` se ainda for usada na criação de profile
  — mas signUp não cria avatar, então provavelmente cai fora).
- `MainActivity.startDestination`: derivar de `Supabase.client.auth.sessionStatus`
  em vez de `DataStore.userIdFlow`.
- Compilar + smoke: criar conta → criar clube → outro user entra por código →
  votar → fechar round → comentar → reagir.

### Sub-fase 9C — limpeza: apagar Room/DataStore + storage de capas

**Entregáveis:**
- Apagar arquivos: `data/db/AppDatabase.kt`, `data/db/RodapeDao.kt`,
  `data/repository/RodapeRepository.kt`, `data/model/Entities.kt` (substituído
  pelos DTOs em `data/remote/dto`).
- Remover deps Room/ksp-room do `libs.versions.toml` + `build.gradle.kts`.
- `DataStoreManager`: remover `USER_ID_KEY`, `USER_NAME_KEY`, `USER_EMAIL_KEY`,
  `ACTIVE_CLUB_ID_KEY`, `saveSession`, `clearSession`, `saveActiveClubId`,
  `userIdFlow`, `userNameFlow`, `userEmailFlow`, `activeClubIdFlow`. Sobrevive
  só `rated_app`, `engagement_count`, `font_scale`.
- `AddBookManualScreen`: trocar `file://` local por upload pro bucket
  `book-covers` (path `<club_id>/<book_id>/<uuid>.jpg`).
- `MainViewModel.setFontScale` passa a chamar `ProfileRepository.updateFontScale`.
  `MainViewModel.fontScale` passa a observar `ProfileRepository.observeMyProfile().map { it.fontScale }`.
  Remover `FONT_SCALE_KEY` do `DataStoreManager` por completo.
- Build verde + smoke completo no emulador.

## Tratamento de erros

- Cada repo embrulha exceções do PostgREST em um `Result<T>` ou `sealed class`
  específica? **Não nesta fase.** Decisão pragmática: deixar exceções propagarem
  e tratar na UI com try/catch + Snackbar genérico ("Algo deu errado, tente
  novamente"). Refator de error handling é fase própria.
- Token expirado: `supabase-kt` faz refresh automático via `AuthRepository`.
  Se refresh falha, sessão cai pra `NotAuthenticated`, NavHost manda pra `welcome`.
- Sem conexão: operação falha com `IOException` — UI mostra Snackbar.
  Cache offline fica para fase futura.

## Testes

- **Unit tests existentes** (testes Room) ficam quebrados quando Room sai.
  Apagamos junto na 9C. Sem reposição imediata — backend Supabase é validado
  por smoke test manual no emulador (a Fase 8 já validou ponta-a-ponta no SQL).
- **Smoke test manual** (em cada sub-fase): roteiro fixo no PR. Para 9C completo:
  1. Cadastrar conta → confirmar email (link no inbox real).
  2. Logar → tela vazia (sem clubes).
  3. Criar clube → ver código gerado.
  4. Em outro emulador / conta: entrar com o código.
  5. Sugerir 2 livros → votar → fechar round → ver vencedor virar "lendo".
  6. Comentar em capítulo → outra conta reage.
  7. Editar resumo de livro → ambas as contas veem ao vivo (Realtime).
  8. Transferir super-admin pra outra conta → checar invariante.
  9. Logout → relogar → estado preservado.

## Segurança

- Cliente nunca segura `service_role` ou `secret_key`. `BuildConfig` só carrega
  `SUPABASE_PUBLISHABLE_KEY` (a constante `SUPABASE_SERVICE_ROLE` no `.env` é
  para ferramentas locais, não vai pro APK).
- Plugin Secrets só injeta o que o `build.gradle.kts` referenciar. **Garantir**
  que `secrets { defaultPropertiesFileName = ".env.example" }` e que nada no
  app lê `BuildConfig.SUPABASE_SERVICE_ROLE` ou `BuildConfig.SUPABASE_SECRET_KEY`.
- RLS é a única autorização. App envia user token; servidor decide. Nenhuma
  checagem de papel client-side substitui a do banco.
- Deep link `app.rodape://login-callback` não é `autoVerify` (precisaria de
  domínio próprio + AssetLinks). Aceitável pra OAuth porque o state PKCE
  protege contra interceptação maliciosa.

## Riscos e mitigação

| Risco | Mitigação |
|-------|-----------|
| `supabase-kt` 3.x tem breaking changes vs 2.x | Travar versão exata no `libs.versions.toml`; consultar docs antes |
| Realtime usa WebSocket; pode falhar em redes restritas | Repos têm fallback de `select` periódico? Não nesta fase — falha fica visível |
| Email de confirmação cai no spam | Documentar pro usuário; futuramente customizar SMTP no Supabase |
| `handle_new_user` trigger falha → profile não criado | Já testado na Fase 8; se acontecer, RPC `ensure_profile()` pode ser fallback (fora desta fase) |
| `service_role` vazar acidentalmente | Conferência manual no PR + grep automático em CI futura |
| Build quebra por incompatibilidade Kotlin 2.2.10 ↔ serialization plugin | Usar versão do plugin pareada com Kotlin (`org.jetbrains.kotlin.plugin.serialization:2.2.10`) |

## Critérios de pronto

- [ ] `./gradlew :app:assembleDebug` passa sem warnings novos.
- [ ] `./gradlew :app:lint` passa.
- [ ] Roteiro de smoke test acima 100% verde em emulador.
- [ ] Nenhuma referência a `Room`, `AppDatabase`, `RodapeDao`, `RodapeRepository`,
      `seedDatabase`, `seedNewClubData`, `user_voce`, `club_mari` no código.
- [ ] `grep -r "service_role\|SECRET_KEY" app/src/main/java` retorna vazio.
- [ ] `.env` continua não-commitado; `.env.example` documenta todos os nomes.

## Fora de escopo (próximas fases)

- Refatorar `MainViewModel` em VMs por escopo.
- Cache offline (Room como cache local opcional, ou alternativa).
- Push notifications (FCM).
- Apple Sign-In (com porte iOS).
- Migrar dados existentes (não há base instalada).
- Testes automatizados E2E contra Supabase de staging.
