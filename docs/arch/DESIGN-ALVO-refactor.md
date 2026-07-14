# Design-alvo — refactor de arquitetura (rodapé)

**Data:** 2026-07-14 · Baseado em leitura COMPLETA de `MainViewModel` (1781 linhas, 56
flows, ~95 funções) e `RemoteRepository` (2654 linhas, ~150 métodos, 25 handlers de
fila), + refs da skill app-android (NowInAndroid) + best practices 2026 (Hilt).

Supera o [PLANO-ARQUITETURA-2026-07-14.md](PLANO-ARQUITETURA-2026-07-14.md) com o
desenho concreto (agora que sei exatamente o código).

---

## Decisões de arquitetura (travadas)

1. **Single module.** NADA de multi-módulo — a própria skill diz "avoid
   over-modularization, start simple". Camadas por **pacote**, não por módulo.
2. **Hilt** pra DI (padrão 2026, integra com ViewModel/Navigation/WorkManager).
3. **SyncEngine = kernel isolado.** A engine offline (SWR + fila + realtime + os 25
   handlers) vira uma classe `@Singleton` autocontida. Descoberta-chave: os handlers
   fazem replay de payload cru → supabase, **não chamam lógica de domínio**. Então a
   engine não depende dos repos — os repos dependem dela.
4. **SessionManager = grafo de sessão compartilhado.** `@Singleton`. Segura o estado
   que quase tudo lê (currentUserId, activeClubId, activeClub, currentBook,
   currentChapters, papel/admin) + os 4 observers do `init` + helpers cross-domain
   (bumpEngagement, existingClubBookId, promoteNextQueuedBook). Resolve o acoplamento
   que trava o split de VM.
5. **Strangler-fig.** Cada passo compila, commita, vai pro device, reverte fácil.

---

## Arquitetura-alvo (pacotes)

```
data/
  remote/
    SyncEngine.kt         @Singleton — kernel offline (infra + 25 handlers)
    RemoteDtos.kt         DTOs internal (✅ F3a já feito)
    Supabase.kt, AuthRepository.kt (como hoje)
    repo/
      UserRepository.kt          (interface) + OfflineFirstUserRepository
      ClubRepository.kt          (clubs + members + admin + RPCs)
      BookRepository.kt          (books/club_books/chapters/summaries/ratings/favorites/search)
      ProgressRepository.kt      (user_progress)
      DiscussionRepository.kt    (comments + reactions)
      VotingRepository.kt        (votes + voting_rounds + book_suggestions)
      MeetingRepository.kt       (meetings + rsvps + patterns + minutes + notes)
      NotificationRepository.kt  (notifications)
      QuoteRepository.kt         (saved_quotes)
      ModerationRepository.kt    (reports + blocks + moderate_remove)
  db/ (Room, como hoje)  datastore/ (DataStoreManager)
  session/
    SessionManager.kt     @Singleton — grafo de sessão + observers
di/
  DataModule.kt           @Provides SyncEngine/dao/supabase/DataStore; @Binds repos
  (Hilt WorkManager p/ DrainQueueWorker)
ui/
  navigation/Routes.kt    (✅ F2 já feito)
  <feature>/
    XRoute.kt  (hiltViewModel + nav)   XScreen.kt (burro, UiState+callbacks)
    XViewModel.kt (@HiltViewModel)      XUiState.kt (sealed)
```

~10 repos de domínio. Cohesivo, não pulverizado (members+admin ficam no Club;
ratings/summaries/favorites/chapters ficam no Book).

---

## SyncEngine — superfície exata (do mapa)

**Estado:** `dao`, `pendingDao`, `supabase`, `json`, `scope`, `lastSyncAt`,
`mutationHandlers`, `immediateDrainInFlight`, `drainMutex`, `realtimeJobs`,
`realtimeChannels`, `tableReloaders`.

**API que os repos usam:** `syncOnce(key,ttl,block)`, `markSynced`, `Ttl(FAST/MED/SLOW)`,
`tryRemoteOrEnqueue(kind,payload,notifyTable,block)`, `notifyLocalMutation(table)`,
`ensureRealtime(table,col,val,reload)`, `dao`, `supabase`, `scope`, `json`,
`escapeJson`, `str`/`strOrNull`, `stateOf`.

**API pública (VM/worker):** `tryDrainPendingQueue()`, `forceRefresh()`,
`pendingMutationsCount`, `clearLocalCache()`, `clearLocalCacheNoDrain()`, `close()`.

**Interno:** `enqueueMutation`, `kickImmediateDrain`, `isPermanentError`,
`maxDrainAttempts`, `registerHandler`, `registerReloader`, `recentlySynced`.

**Os 25 handlers** (init) FICAM na engine — satisfaz a trava de drain-safety (todos
os kinds registrados num mapa só antes de qualquer drain; senão o kind é descartado
como "unknown").

---

## SessionManager — conteúdo (do mapa do VM)

**Estado de sessão (Tier 1/2):** currentUserId, activeClubId (+writer), activeClub,
allClubs, currentBook, currentChapters, currentUserPapel, isAdmin, isSuperAdmin,
currentBooksMap, clubBooks.

**Observers do init (4):** guard de troca de conta, auto-seleção de clube, auto-close
de votação, avatar-default. Rodam num scope interno.

**Helpers cross-domain:** bumpEngagement, existingClubBookId, promoteNextQueuedBook,
selectActiveClub.

Injeta: AuthRepository, DataStoreManager, User/Club/Book/Voting repos.
VMs de tela injetam o SessionManager + os repos que precisam.

---

## Amarras cross-domain (tratadas, do mapa)

| Amarra | Tratamento |
|---|---|
| `insertMeeting` semeia FK de books | fica no MeetingRepository; chama engine + dao.book (dao é compartilhado) |
| `closeVotingRound` notifica 3 tabelas | VotingRepository chama `engine.notifyLocalMutation` p/ votes+club_books+notifications |
| `moderateRemoveContent` toca 4 tabelas | ModerationRepository; `tableForTarget` + dao marks (dao compartilhado) |
| `IdOnlyDto` usado por 4 domínios | vira tipo `internal` compartilhado em RemoteDtos.kt |
| RPCs "no lugar errado" (promote/leave em CLUBS) | realocam pro repo semântico (Club) |
| 25 handlers num registro só | ficam TODOS na engine (não nos repos) |

---

## Sequência de migração (cada passo shipa)

| Passo | O quê | Risco | Verificação |
|---|---|---|---|
| **F3a** ✅ | DTOs → RemoteDtos.kt internal | 🟢 | compile (pendente) |
| **F3b** | Extrair **SyncEngine** (infra+handlers) de dentro do RemoteRepository; RemoteRepository passa a delegar. Comportamento IDÊNTICO. | 🔴 **backbone offline** | compile + **você testa no device**: avião→ação→reconecta→sincroniza; badge de pendências; realtime |
| **F4a** | Hilt scaffolding: `@HiltAndroidApp`, `@AndroidEntryPoint`, `@Provides` engine/dao/supabase/DataStore, `@HiltWorker` no DrainQueueWorker | 🟡 | compile + smoke |
| **F3c** | Fatiar repos de domínio (interface + OfflineFirst*Impl usando engine), 1 por commit, `@Binds`. RemoteRepository vira fachada que delega (strangler) | 🟡 | compile por repo |
| **F4b/F5** | Extrair SessionManager; depois por TELA (folha→hub): `@HiltViewModel` + `sealed UiState` + split Route/Screen, tira do MainViewModel. Ordem: tab→sync→notif→busca→prefs→quotes→moderação→progresso→ratings→votação→meetings→discussão→book→club→sessão | 🟡 | compile + device por tela |
| **F6** | Previews (`@ThemePreviews`/`@DevicePreviews`) por tela já-fatiada | 🟢 | build |
| **F7** | Fakes das interfaces + testes de VM/fluxo (fila, tally, marco, auth) | 🟡 | testDebug |

**F3b é o passo crítico** (mexe no backbone offline). É movimento puro (sem mudar
lógica), mas você valida pesado no device antes de eu seguir. Todo o resto é
incremental e de baixo risco em cima da engine estável.

---

## Registro de risco

- **Perder mutação offline** (F3b): mitigado por mover sem alterar lógica + os 25
  handlers ficam juntos na engine + teste de device com avião.
- **Drain descartar kind desconhecido**: impossível se todos os handlers ficam na
  engine (não espalhados por repos lazy).
- **Config-change/lifecycle**: SessionManager `@Singleton` sobrevive recreation
  (melhor que o `by viewModels()` atual).
- **DrainQueueWorker precisa da engine**: `@HiltWorker` + HiltWorkerFactory.

---

## Progresso

- [x] F1 lifecycle · [x] F2 nav type-safe · [x] F3a DTOs
- [x] F3b SyncEngine (commit 5fd7255 — ⚠️ AGUARDA teste em device: avião→ação→reconecta)
- [x] F4a Hilt scaffolding (commit 5482524 — Hilt 2.60.1; worker injeta engine @Singleton;
      smoke em device junto com o teste do F3b)
- [x] F3c repos — 10 repos fatiados, 1/commit (ff3cdf0→06074fa). RemoteRepository é
      fachada pura (~560 linhas); repos compartilham a engine da fachada; @Binds prontos
      pro F4b/F5. Realocações: closeVotingRoundViaRpc→Voting, deleteOwnAccountViaRpc→User,
      IdOnlyDto/ProfileUpdateDto/escapeJson→internal compartilhados.
- [x] F4b SessionManager (commit bb93bd6 — @Singleton com os 4 observers + helpers;
      engine UNIFICADA: RemoteRepository/SessionManager/worker compartilham a mesma
      SyncEngine via DI; MainViewModel virou @HiltViewModel e consome por aliases;
      sem repository.close() no onCleared)
- [ ] F5 VMs+UiState por tela (folha→hub, ordem no MAP-MainViewModel §5)
- [ ] F6 previews · [ ] F7 testes
