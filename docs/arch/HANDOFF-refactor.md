# HANDOFF — refactor de arquitetura do rodapé (LEIA PRIMEIRO)

Você está assumindo um refactor de arquitetura **delicado e em fases**, planejado com
calma na sessão anterior. Este doc te dá o estado exato e as regras. Leia na ordem.

## Ordem de leitura (obrigatória antes de tocar código)

1. **Este arquivo** (estado + regras + comandos).
2. [DESIGN-ALVO-refactor.md](DESIGN-ALVO-refactor.md) — a arquitetura-alvo e a sequência
   de migração (F3b→F7). É o mapa mestre.
3. [MAP-RemoteRepository.md](MAP-RemoteRepository.md) — inventário método-a-método +
   os 25 handlers da fila + a trava de drain-safety. Essencial p/ F3b/F3c.
4. [MAP-MainViewModel.md](MAP-MainViewModel.md) — grafo de sessão + ordem de split. P/ F4/F5.
5. [PLANO-ARQUITETURA-2026-07-14.md](PLANO-ARQUITETURA-2026-07-14.md) — visão das 7 fases.

## Estado atual (2026-07-14)

**Feito e no git (branch master, remoto github.com/gabrielfeelix/rodape):**
- F1 — collectAsState → collectAsStateWithLifecycle (143 sites). Commit `acc9812`.
- F2 — navegação type-safe (@Serializable em ui/navigation/Routes.kt). Commit `42026a0`.
- F3a — DTOs movidos p/ RemoteDtos.kt (private→internal). Commit `9f0a35f`, compilou limpo.
- F3b — SyncEngine extraído (infra + 25 handlers; RemoteRepository delega). Commit
  `5fd7255`, compilou limpo. **⚠️ AGUARDA teste em device** (avião→ação→reconecta→
  sincroniza + badge + realtime).
- F4a — Hilt scaffolding (2.60.1): @HiltAndroidApp+Configuration.Provider,
  @AndroidEntryPoint, di/DataModule (engine @Singleton/dao/supabase/DataStore),
  DrainQueueWorker @HiltWorker injetando a engine (sem close; era repo por execução).
  Commit `5482524`, compilou limpo. Usuário autorizou seguir antes do teste de device
  do F3b — o smoke dos dois roda junto. Fix visual avulso: `ee71dbc` (tabs do
  BookDetail sobrepostas dentro do AnimatedContent).
- F3c — 10 repos de domínio fatiados, 1/commit (`ff3cdf0`…`06074fa`), cada um
  compilado: Progress, Notification, Quote, Moderation, Discussion, Voting, Meeting,
  User, Book, Club. Padrão: interface pública + OfflineFirst*Impl internal
  (base repo/OfflineFirstRepository delega pra engine; corpos verbatim) + @Binds em
  di/RepositoryModule. RemoteRepository = fachada pura (~560 linhas) delegando tudo;
  API pública intocada — MainViewModel nem recompilou diferente. ⚠️ Smoke de device
  acumulado: F3b+F4a+F3c testam juntos (roteiro do F3b cobre).

**Compliance/beta:** outro agente já fechou 8 ondas (moderação, Termos, Crashlytics,
age gate, RLS, etc.) — tudo commitado. NÃO mexer nisso.

**Ambiente:** JDK 17 + Android SDK instalados (WSL). SEM device/emulador aqui — quem
testa no aparelho é o usuário. gradle.jvmargs baixado p/ -Xmx2g (RAM da máquina é
apertada, 11Gi).

## PRIMEIRO PASSO obrigatório

O F3a (mover DTOs) pode ter sido commitado ou não. Rode `git status` e `git log --oneline -3`.
- Se F3a NÃO estiver commitado: **compile primeiro** (comando abaixo) pra verificar o
  move, então commite `refactor(F3a): DTOs → RemoteDtos.kt internal`.
- Confirme `git status` limpo antes de começar o F3b.

## Comandos (RAM é crítica — o usuário reclama de estouro)

**Type-check (NÃO buildar APK à toa):**
```
./gradlew :app:compileDebugKotlin -x processDebugGoogleServices --no-configuration-cache --console=plain
```
- `-x processDebugGoogleServices`: applicationId debug não bate com google-services.json.
- **SEMPRE rode `./gradlew --stop` depois** de compilar pra liberar o daemon (-Xmx2g).
- NÃO fique compilando a cada micro-edição — junte mudanças, compile 1x por passo.
- **NUNCA** mate/derrube o WSL. Pra liberar RAM: `./gradlew --stop` + `pkill -f GradleDaemon`
  (só o daemon do gradle, nada de WSL).

**Commit/push:** conventional commits, sufixo
`Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>`. Push só quando o
usuário pedir OU quando um passo fecha limpo e ele confirma.

## Regras do refactor (NÃO negociáveis)

1. **Strangler-fig.** Um passo por vez. Cada passo: compila + commita + o usuário testa
   no device + só então o próximo. NADA de big-bang.
2. **F3b (extrair SyncEngine) é o passo de risco 🔴** — mexe no backbone offline
   (fila/sync/realtime) que a compliance acabou de estender. É **movimento PURO**: mover
   código sem mudar UMA linha de lógica. Depois de compilar, **PARE e peça o usuário
   testar no device**: modo avião → fazer ação (comentar/votar/rsvp) → reconectar →
   confirmar que sincroniza + badge de "aguardando conexão" + realtime. NÃO siga p/ F4
   sem esse OK.
3. **Trava de drain-safety (a armadilha):** os 25 handlers da fila (MAP-RemoteRepository §6)
   precisam TODOS ficar num único `mutationHandlers` na engine, registrados ANTES de
   qualquer drain. Se um kind ficar sem handler, a mutação é DESCARTADA (perda de dado
   do usuário). Por isso os handlers + drainMutex + isPermanentError + enqueue/kickDrain
   FICAM na SyncEngine, nunca espalhados por repos lazy.
4. **Comportamento idêntico.** Este refactor é estrutural. Se você se pegar "melhorando"
   lógica, PARE — separa em outro passo/commit.
5. **Confirme o design antes de desviar.** Se o mapa/design não bater com o código real,
   fale com o usuário — não improvise no backbone.

## Sequência (resumo — detalhe no DESIGN-ALVO)

F3a ✅ → **F3b SyncEngine** (🔴 device) → F4a Hilt scaffolding → F3c fatiar ~10 repos
(1/commit) → F4b SessionManager + F5 VMs/UiState/Route-Screen por tela (folha→hub) →
F6 previews → F7 testes (fakes das interfaces).

## Onde estão as coisas

- god-repo: `app/src/main/java/com/example/data/remote/RemoteRepository.kt`
- DTOs: `.../data/remote/RemoteDtos.kt`
- god-VM: `app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt`
- NavHost/DI-entry: `app/src/main/java/com/example/MainActivity.kt`, `RodapeApp.kt`
- Room: `.../data/db/` · DataStore: `.../data/DataStoreManager.kt`
- Auth: `.../data/remote/AuthRepository.kt` · Supabase client: `.../data/remote/Supabase.kt`
