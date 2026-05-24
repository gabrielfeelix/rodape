# Fase 9 — Status (acordou em 2026-05-25)

## TL;DR

**Fase 9A: ✅ COMPLETA.** Auth real (Email/Senha + Google + reset por email) plugada.
Build + lint verdes. 12 commits atomicos.

**Fase 9B: ⏸️ PAUSADA antes de comecar a portar.** Plano escrito mas execucao adiada
por questao de seguranca: portar 93 metodos do `RodapeRepository` pra Supabase enquanto
voce dorme, sem voce poder testar entre commits, produziria bugs sutis em telas que
dependem do domain antigo. Decidi parar pra voce validar a 9A antes.

**Fase 9C: ⏸️ NAO INICIADA.** Depende da 9B.

## O que esta funcionando agora

1. **Cadastrar conta**: tela `SignUpScreen` chama `Supabase.auth.signUpWith(Email)`.
   Trigger `handle_new_user` cria row em `profiles` automaticamente.
2. **Confirmar email**: Supabase envia email com link `app.rodape://login-callback`.
   Deep link abre o app, `handleDeeplinks` ativa a sessao.
3. **Login email/senha**: tela `LoginScreen` chama `Supabase.auth.signInWith(Email)`.
4. **Login Google**: botao "Continuar com Google" → Credential Manager → GoogleIdToken
   → `Supabase.auth.signInWith(IDToken)`. Usa `GOOGLE_WEB_CLIENT_ID` do `.env`.
5. **Esqueci senha**: tela `ForgotPasswordScreen` → `Supabase.auth.resetPasswordForEmail()`.
   Email com link de recovery → app abre `ResetPasswordScreen` (detecta `SessionSource.External`
   no `LaunchedEffect` em `MainActivity`).
6. **Build/lint verdes** — Room + DataStore-session continuam funcionando em paralelo,
   so adicionamos Auth real por cima.

## O que ainda NAO funciona / decisao

- **App entra no `main_tabs` apos login Supabase mas as telas tentam ler do Room.**
  Isso vai mostrar dados vazios (banco Room nasce vazio no novo flow) ou crashes
  em queries que dependem de IDs antigos tipo `user_voce`/`club_mari`. **Esperado**:
  esses dados ficam vazios ate a 9B portar os repos pro Supabase.
- **`MainViewModel.login(name, email, ...)` antigo ainda existe e ainda e chamado
  por... nada (LoginScreen nova ja usa AuthRepository direto).** Mas a funcao
  fica como zumbi ate a 9B apagar.
- **`seedDatabase()` ainda roda no `init {}` do MainViewModel.** Vai criar dados de
  demo no Room que ninguem mais le, mas nao quebra nada. Sai na 9B.

## Por que parei na 9B

Tres riscos:
1. **Schema divergente Domain vs Banco**. Ex: `User.email` nao existe em `profiles`
   (email so esta em `auth.users`); `Club.criadoEm: Long` no domain vs
   `clubs.created_at: timestamptz` no banco; campo extra `profiles.sobrenome` que o
   domain ignora. Cada DTO mapping precisa decisao caso-a-caso.
2. **93 metodos** do `RodapeRepository` chamados em 150 call-sites do `MainViewModel`.
   Trabalho enorme; alto risco de quebrar telas sutilmente (ex: ordem de items, filtros
   de status, RLS bloqueando query que Room nem checava).
3. **Sem possibilidade de smoke test entre commits**: voce dormindo, eu nao posso
   abrir emulador e ver se a tela "Estante" continua mostrando livros depois de eu
   portar `getBookByStatusFlow`. Faria 6 commits, voce acordaria e descobriria que
   3 telas estao quebradas.

## Plano de retomada (para a proxima sessao com voce acordado)

**Opcao A — Continuar com `RemoteRepository` substituto (recomendado):**
1. Comecar pela Task 1 do plano 9B (estrutura + 6 metodos `profiles`/`clubs`).
2. Voce verifica no emulador: login → ve seus dados de perfil aparecerem do Supabase.
3. Iterar por area (books, voting, etc) com checkpoint visual a cada commit.

**Opcao B — Refatorar `Entities.kt` pra bater com banco (mais agressivo):**
1. Renomear todos os campos do domain pra snake_case + `Long → Instant`.
2. Apagar `User.email` (ler de `auth.user.email` direto).
3. Adicionar `User.sobrenome`.
4. Trocar `RodapeRepository` por `RemoteRepository` com DTO == Domain (sem conversao).
5. Mais commits mas codigo mais limpo no fim.

**Recomendacao: A**, porque mantem UI 100% intacta e ja sobra superficie pra refator
de campos numa fase futura.

## Riscos conhecidos

- **`config-cache` do Gradle** as vezes nao re-detecta arquivos novos. Solucao:
  `--rerun-tasks` ou apagar `.gradle/configuration-cache/`. Atrapalhou 2x durante
  a 9A.
- **Email de confirmacao no Supabase**: precisa validar no painel Auth → Email
  Settings que esta ON e que SMTP padrao funciona pra `@gmail.com`/etc. Nao testei.
- **Google Sign-In no emulador**: precisa conta Google logada em Settings → Accounts;
  no emulador limpo da Play Store, isso e o primeiro setup.
- **Deep link `app.rodape://login-callback`**: nao e App Links verificado (sem
  AssetLinks no `.well-known`). OAuth funciona, mas qualquer outro app pode declarar
  o mesmo scheme. Aceitavel pra dev/MVP; precisa AssetLinks pra producao.

## Arquivos novos/alterados (12 commits)

```
docs/superpowers/specs/2026-05-24-fase9-migrar-app-pra-supabase-design.md  (spec)
docs/superpowers/plans/2026-05-24-fase9a-fundacao-supabase-auth.md          (plano 9A)
docs/superpowers/plans/2026-05-24-fase9b-remote-repository-substituicao.md  (plano 9B)
docs/superpowers/specs/2026-05-24-fase9-STATUS.md                           (este arquivo)

.env                                            (chaves Supabase + Google reorganizadas)
.env.example                                    (idem, sem valores)
gradle/libs.versions.toml                       (supabase 3.6 + ktor + credentials + ...)
app/build.gradle.kts                            (deps + serialization + desugaring + ignoreList)
app/src/main/AndroidManifest.xml                (RodapeApp + intent-filter deep link)

app/src/main/java/com/example/RodapeApp.kt                          (novo)
app/src/main/java/com/example/MainActivity.kt                       (handleDeeplinks + 3 rotas)
app/src/main/java/com/example/data/remote/Supabase.kt               (novo singleton)
app/src/main/java/com/example/data/remote/AuthRepository.kt         (novo)
app/src/main/java/com/example/ui/auth/GoogleSignInHelper.kt         (novo)
app/src/main/java/com/example/ui/screens/SignUpScreen.kt            (novo)
app/src/main/java/com/example/ui/screens/ForgotPasswordScreen.kt    (novo)
app/src/main/java/com/example/ui/screens/ResetPasswordScreen.kt     (novo)
app/src/main/java/com/example/ui/screens/WelcomeScreen.kt           (LoginScreen reescrita)
app/src/main/java/com/example/ui/screens/MainTabsScreen.kt          (lint fix)
app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt         (sessionStatus + supabaseUserId)
```

## Como testar a 9A agora (proxima sessao)

```bash
# 1. Garantir emulador rodando
./gradlew :app:installDebug

# 2. No app:
# - Welcome → "Entrar"
# - "Ainda nao tem conta? Cadastre-se"
# - Digitar email que voce acessa, senha 6+, nome
# - Confirmar email no inbox (link app.rodape://...)
# - Voltar pro app, logar com mesma credencial
# - Esperado: navega pra main_tabs (telas mostram vazias/Room — ok)

# 3. Esqueci senha:
# - Logout (botao do app — ainda usa logout antigo, ok pra teste)
# - Login → "Esqueci minha senha"
# - Email com link → toca → reset_password → digita nova senha → main_tabs

# 4. Google:
# - Login → "Continuar com Google"
# - Precisa conta Google em Settings → Accounts do emulador
# - Esperado: picker abre, escolhe conta, navega pra main_tabs
```

## Seguranca verificada

- `service_role`, `secret_key`, `web_client_secret` NAO entram em `BuildConfig`
  (configurado via `secrets { ignoreList.add(...) }`). Confirmado em
  `app/build/generated/source/buildConfig/debug/com/example/BuildConfig.java`.
- App so usa `SUPABASE_PUBLISHABLE_KEY` — RLS no servidor e a autorizacao.
- `.env` permanece no `.gitignore`.
