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

## Estado atual (2026-07-14 — atualizado após F4b)

**Feito e no git (branch master, remoto github.com/gabrielfeelix/rodape):**
- F1 lifecycle `acc9812` · F2 nav type-safe `42026a0` · F3a DTOs→RemoteDtos `9f0a35f`.
- **F3b** — kernel offline extraído p/ `SyncEngine.kt` (SWR/TTL + write-queue + 25 handlers +
  realtime). Commit `e29ae6d`. Movimento PURO.
- **F4a** — scaffolding Hilt 2.60.1 (@HiltAndroidApp, @AndroidEntryPoint, `di/DataModule`).
  Commit `eac9ff7`. Aditivo.
- **F3c COMPLETO** — os 10 repos de domínio fatiados (interface + `OfflineFirst*Impl @Inject`
  + `@Binds` em `di/RepositoryModule`): Progress `d7607d5`, Notif+Quote `59deb89`, User+Moder
  `54e0622`, Voting `93bb4e5`, Discussion+Meeting `ed5de8c`, Book `2871b29`, Club `b011cec`.
  **RemoteRepository virou fachada pura (2654→574 linhas): SyncEngine + 10 delegadores
  reusando o mesmo engine.**
- **F4b COMPLETO** — `data/session/SessionManager.kt` (Android-free) extraído do MainViewModel.
  Commit `e4ad90a`. Segura o grafo de sessão (currentUserId/activeClubId + derivados
  clube/livro/papel + activeVotingRound) + 3 dos 4 observers do init (avatar ficou na VM,
  depende de AvatarPicker ui) + helpers cross-domain. VM re-expõe via forwarders → comportamento
  IDÊNTICO, lifecycle idêntico (recebe viewModelScope). **MainViewModel 1781→1637L.** Movimento
  estrutural puro. Removido supabaseUserId órfão.

**⚠️ DEVICE-TEST PENDENTE:** F3b+F4a estão no AAB vc20 (aguardando o usuário testar no cel
~2026-07-16, quando o reset da chave de upload do Play ativa). F3c+F4b ainda NÃO foram testados
em device — entram num vc21+ depois. Ver [[rodape-keystore-upload-2026-07-14]] na memória.

**Compliance/beta:** outro agente já fechou 8 ondas — tudo commitado. NÃO mexer.

**Ambiente:** JDK 17 + Android SDK (WSL). SEM device/emulador aqui — o usuário testa no
aparelho. gradle.jvmargs = **-Xmx3g** (subido de 2g: Hilt+KSP dava OOM em 2g).

## PRIMEIRO PASSO (agora: F5 — VMs por tela, folha→hub)

F4b está fechado e compilando (SessionManager pronto — o nó destravou). Próximo é **F5**: por
TELA, folha→hub, extrair `@HiltViewModel` + `sealed UiState` + split Route/Screen, esvaziando o
MainViewModel (1637L). Ordem exata em [MAP-MainViewModel.md](MAP-MainViewModel.md) §5:
tab → sync → notif → busca → prefs → quotes → moderação → progresso → ratings → votação →
meetings → discussão → book → club → **sessão (por último)**. Cada VM injeta o SessionManager +
as interfaces de repo do F3c (já prontas). Aqui entra o @HiltViewModel deferido.

**Ao começar o F5, converta o SessionManager pra `@Singleton` de verdade** (scope próprio em
vez do viewModelScope emprestado + injeta as interfaces fatiadas em vez da fachada
RemoteRepository) — está anotado no KDoc da classe. Confirme `git status` limpo + compile 1x
pra baseline antes de tocar código.

## Comandos (RAM é crítica — 11Gi, o usuário reclama de estouro)

**Type-check:**
```
./gradlew :app:compileDebugKotlin -x processDebugGoogleServices --no-configuration-cache --console=plain --no-daemon -Pkotlin.compiler.execution.strategy=in-process
```
- `-x processDebugGoogleServices`: applicationId debug não bate com google-services.json.
- **`--no-daemon` + `in-process`**: o daemon Gradle separado crashava/sumia ("daemon
  disappeared") de forma intermitente; rodar no JVM do launcher resolve. Buildar em background
  (a compile leva ~2min) e ler o log.
- **SEMPRE limpar após buildar** (o usuário quer a máquina leve): `./gradlew --stop` +
  `pkill -f GradleDaemon` + `pkill -f KotlinCompileDaemon`. Ver [[keep-machine-light-ram]].
- **Gotcha:** `pkill` no INÍCIO de um comando composto (`pkill ...; ./gradlew ...`) buga o
  shell (pkill sai 1 e mata o resto). Rodar o pkill num comando separado.
- **NUNCA** mate/derrube o WSL.

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

F3a ✅ → F3b SyncEngine ✅ → F4a Hilt ✅ → F3c fatiar 10 repos ✅ → F4b SessionManager ✅ →
**F5** VMs/UiState/Route-Screen por tela (próximo; folha→hub; ordem em MAP-MainViewModel §5) →
**F6** previews (@ThemePreviews por tela fatiada) → **F7** testes (fakes das 10 interfaces +
VM/fluxo) → deferidos: **@HiltWorker** no DrainQueueWorker + **@HiltViewModel** (entram no F5).

**Detalhe do que falta:**
- **F4b** ✅ — SessionManager extraído (`e4ad90a`). Ainda é classe simples recebendo
  viewModelScope (não @Singleton) e usa a fachada RemoteRepository — vira @Singleton + repos
  fatiados no F5 (anotado no KDoc). Observer de avatar ficou na VM (dep ui).
- **F5** — por TELA, folha→hub: `@HiltViewModel` + `sealed UiState` + split Route/Screen,
  esvaziando o MainViewModel (1781L). Ordem (MAP-MainViewModel §5): tab → sync → notif →
  busca → prefs → quotes → moderação → progresso → ratings → votação → meetings → discussão
  → book → club → **sessão (por último)**. Injetar as interfaces de repo do F3c (já prontas)
  + o SessionManager.
- **F6/F7** — previews + testes (as interfaces do F3c destravam fakes).

## Onde estão as coisas

- god-repo: `app/src/main/java/com/example/data/remote/RemoteRepository.kt`
- DTOs: `.../data/remote/RemoteDtos.kt`
- god-VM: `app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt`
- NavHost/DI-entry: `app/src/main/java/com/example/MainActivity.kt`, `RodapeApp.kt`
- Room: `.../data/db/` · DataStore: `.../data/DataStoreManager.kt`
- Auth: `.../data/remote/AuthRepository.kt` · Supabase client: `.../data/remote/Supabase.kt`
