# Auditoria completa — Rodapé (2026-07-11)

Parecer do estado atual do app após ~6 semanas parado (último commit: 28/05/2026).
Consolidado a partir de 3 análises paralelas (telas/UX, camada de dados, fidelidade
visual vs. Claude Design) + build/testes executados hoje.

---

## TL;DR

| Área | Estado |
|---|---|
| Sincronização GitHub | ✅ local = `origin/master`, working tree limpa |
| Build | ✅ `assembleDebug` compila sem erro |
| Testes | ❌ suíte não compila (imports quebrados pós-refactor) |
| Funcionalidades | ~90% completas — 27 telas, fluxos principais todos funcionais |
| Visual vs. Claude Design | ⚠️ paleta e radii 100% fiéis; sombras, ícones, fontes e microdetalhes divergem — **tudo replicável em Compose** |
| Dados/Supabase | ⚠️ arquitetura offline-first sólida no conceito, mas com bugs reais de perda de dados e 1 vazamento de chave |
| Release | 🔴 travado em config manual (Supabase Dashboard + conta Play Store) |

**Onde o projeto parou:** AAB v1.0.0 assinado e pronto em
`app/build/outputs/bundle/release/`. Faltou executar o checklist manual:
SMTP/Resend no Supabase, email confirmation, linking Google↔email
(`docs/release/supabase-auth-setup.md` — checklist todo desmarcado) e criar a
conta Google Play (US$ 25) (`docs/release/PLAYSTORE-CHECKLIST.md`).

---

## 1. Por que o app ficou "mais feio" que o Claude Design

O design original vive em `claude-design/` (`Tramabook.html` + `tokens.jsx`,
`shell.jsx`, `icons.jsx`, `screens-*.jsx`). A comparação token a token mostrou
que **a paleta de cores está hex a hex idêntica** e os radii também. O gap está
em 5 coisas, todas corrigíveis:

1. **Fontes provavelmente nem carregam.** `Type.kt` depende 100% do provider
   GMS (downloadable fonts). Se o fetch falha (offline no 1º uso, Play Services
   desatualizado), o app cai silenciosamente pra **Roboto** — e todo o caráter
   editorial da Literata some. Não existe nenhum `res/font/` de fallback.
   → Fix: embarcar Literata + Inter em `res/font/` (~30 min). **Suspeito nº 1.**
2. **Sombras.** O design usa 8+ receitas de sombra suave, larga e *tingida*
   (marrom `rgba(45,30,15,…)`, verde-oliva `rgba(41,56,32,…)`). O Compose usa
   elevação Material genérica (2dp cinza; o ticket da Home não tem sombra
   nenhuma). Resultado: tudo chapado.
   → Fix: `Modifier.rodapeShadow()` padronizado com `ambientColor`/`spotColor`
   tingidos + `setShadowLayer` para blur largo.
3. **Ícones Material** em vez dos SVGs próprios de stroke 1.6px do design.
   → Fix: importar os ~30 ícones de `icons.jsx` como `ImageVector`.
4. **Top bar Material genérica.** O header do design (avatar + pill branco de
   clube com overline "CLUBE" + sino circular com badge) é assinatura visual do
   produto; o `CenterAlignedTopAppBar` atual é a peça mais "template" do app.
5. **Microdetalhes cortados:** ticket perfurado com notches e borda tracejada,
   dia em serif itálico 64px (hoje 38sp), dot dourado `#E6BF6B` (cor ausente do
   app inteiro), lombada e ornamentos das capas, gradientes pastel dos avatares
   72px, círculos decorativos do hero, press-scale 0.98 nos botões.

**Veredito: nada no design é irreplicável em Compose.** É cor, tipo, raio,
sombra e recortes de Path. Dá pra chegar a ~100% de fidelidade com trabalho
incremental, sem refatoração estrutural. Referências exatas de arquivo:linha no
relatório visual (seção 3 abaixo aponta os principais).

Divergência estrutural a decidir: o design tem **4 abas** na bottom nav; o app
tem **5** (Estante foi promovida a aba).

## 2. Estado das telas (27 mapeadas)

Completas e bem resolvidas: todo o fluxo de auth (Welcome/Login/SignUp/Forgot/
Reset), Onboarding, CreateClub, Discussion, Suggest (busca Open Library +
cross-check Google Books), AddBookManual, BookDetail (5 sub-tabs), Meeting,
ManageClub (a mais robusta), ManageChapters, ModerationLog, Frases.

### Problemas graves de conteúdo
- **Dados fake exibidos como reais:**
  - Card Estatísticas 100% hardcoded: "1h 45m", "25 pág/h" (`MainTabsScreen.kt:1738,1764`)
  - Rating inventado "★ 4.5 · 8 lendo" (`MainTabsScreen.kt:1408`)
  - Pill "em 3 dias" fixa no card do encontro, ignora a data real (`MainTabsScreen.kt:789`)
  - Fallback de data "DOMINGO, 24 DE OUTUBRO" e ano "de 25" hardcoded (`:712,718`)
  - Fallback de local "Café Lispector, Vila Madalena" (`:838`)
  - Perfil: "Lendo: A Metamorfose" só pra clubes fake; clubes reais mostram
    sempre "Sem livro atual" mesmo tendo livro (`:1975-1981`)
  - Notificações: agrupamento "HOJE/ESTA SEMANA" decidido por conteúdo do
    payload, não por data; easter egg hardcoded pra `userName == "Luciana"`
    (`NotificationsScreen.kt:113-118,198`)
- **AboutScreen mente sobre privacidade:** afirma "seus dados ficam só no seu
  aparelho / não enviamos pra servidor" — contradiz o backend Supabase. Risco
  de rejeição na Play Store (`AboutScreen.kt:137,152-155`).
- **Rótulo invertido:** quem está ≥3 capítulos *atrasado* é rotulado
  "No ritmo" (`MainTabsScreen.kt:1205-1228`).
- **Botão morto:** MoreVert na Discussion com onClick vazio (`DiscussionScreen.kt:103`).
- **Inconsistência de senha:** SignUp exige 8+ com símbolo; ResetPassword aceita
  6+ (servidor pode rejeitar depois).
- Tom de voz mistura "tu" e "você", às vezes na mesma tela.

### Estados ausentes (padrão sistêmico)
- **Zero pull-to-refresh** no app inteiro.
- **Loading não é modelado**: telas mostram empty/erro ("Livro não encontrado",
  "Nenhum encontro") enquanto os dados ainda estão sincronizando. Usuário com
  clube pode ver "Você ainda não está em nenhum clube" por segundos no cold
  start (`MainTabsScreen.kt:127`).
- **Feedback de sucesso quase inexistente**: só ManageClub tem Snackbar. Salvar
  frase, avaliar, RSVP, votar, salvar perfil — nenhum feedback.
- **Zero UI de offline/sync**: a fila offline existe, mas não há indicador
  "sincronizando", badge de pendências nem aviso de falha.
- Sem confirmação: excluir frase, concluir encontro (efeito colateral grande),
  "Marcar progresso" (sem como voltar atrás — não existe decremento).
- JoinClub sem loading/disabled no submit (toque duplo = 2 requests); votação
  permite votar tocando no corpo do card mesmo com limite atingido
  (`NextTabScreen.kt:756` vs `:869`).

### Telas/fluxos que FALTAM
1. **Exclusão de conta** — obrigatória pela política do Google Play. Não existe.
2. **"Sair do clube"** para membro comum — só admin remove membro.
3. Push notifications (FCM) — planejado v1.1, sem ele os avisos têm pouco valor.
4. Deep link de convite (`https://rodape.com/c/...`) — parser existe mas está
   desativado (`WelcomeScreen.kt:796,892-934` é código morto).
5. Tela de Configurações (o Onboarding promete "dá pra mudar nas configurações").
6. Perfil de outro membro (avatares não são clicáveis).
7. Editar/apagar o próprio comentário.
8. Estatísticas reais de leitura (ou remover o card mockado).
9. Scroll automático pro fim ao enviar comentário na Discussion.

## 3. Camada de dados — bugs e riscos

Arquitetura: Room = source of truth, SWR com TTL, Realtime por tabela, fila
offline (`PendingMutation` + `DrainQueueWorker`), RPCs SECURITY DEFINER, RLS.
Conceito bom; execução com 3 padrões de escrita inconsistentes.

### Críticos
1. **`GEMINI_API_KEY` vaza pro APK** — o plugin Secrets injeta todas as chaves
   do `.env` no BuildConfig exceto as da `ignoreList`, e essa chave não está na
   lista (`app/build.gradle.kts:96-111`). Não é usada por nenhum código. Fix:
   adicionar à ignoreList + rotacionar a chave.
2. **Perda silenciosa de dados offline** — `insertUserProgress` (o progresso de
   leitura!), ratings, summaries, meetings, minutes são "remoto-primeiro":
   offline, nada é salvo, nenhum erro aparece (`RemoteRepository.kt:1565-1571`,
   `:2459` etc.). Só 6 tipos de mutação têm fila.
3. **Updates/deletes nunca entram na fila** (`updateClubBookStatus`,
   `softRemoveComment`, `markAllNotificationsAsRead`…): offline, somem sem aviso.
4. **Vazamento acumulativo de Realtime**: canais nunca fazem unsubscribe,
   reloaders nunca são removidos; sessão longa = N reloads redundantes +
   WebSockets acumulando (`RemoteRepository.kt:857-933`). Coleções não
   thread-safe (`mutableMapOf` mutado da main e iterado de IO).
5. **Schema Postgres inteiro fora do repo** — 73 policies RLS, 14 RPCs,
   triggers: tudo só no Dashboard. Inauditável e irrecuperável. Versionar com
   `supabase/migrations/*.sql`.
6. **Qualquer membro pode forjar notificações para outros** — o cliente insere
   rows em `notifications` com `user_id` alheio (`MainViewModel.kt:1245-1257`);
   deveria ser trigger/RPC server-side.

### Médios
- Fila sem limite de tentativas nem distinção 4xx/5xx → poison messages eternas.
- Retry da fila corrompe texto: `JsonElement.toString().trim('"')` reenvia
  `\n`/`\"` literais (`RemoteRepository.kt:794`); usar `jsonPrimitive.content`.
- Logout apaga a fila pendente sem drenar (`RemoteRepository.kt:675-679`).
- PK local de `votes` sem `votingRoundId` → votos colidem entre rodadas
  (`Entities.kt:105`).
- Timezone de reuniões: hora digitada interpretada como UTC
  (`RemoteRepository.kt:2053`); parse falho vira `now()` silenciosamente.
- Signed URL de capa expira em 365 dias e fica persistida — capas quebram em 1
  ano; regeneração prometida em comentário não existe (`RemoteRepository.kt:1373-1385`).
- Duas instâncias de `RemoteRepository` (ViewModel + Worker), scope nunca
  cancelado.

### Qualidade
- **Testes: suíte quebrada** — `VotingTallyTest`/`ChapterFetcherTest` importam
  `com.example.voting.*` mas as classes foram movidas pra `com.example.util.voting`.
  Nenhum teste roda. Zero cobertura da camada de dados (o código mais arriscado).
- Dependências: `composeBom 2024.09.00` (~2 anos atrás, destoa do Kotlin 2.2.10
  / AGP 9.1.1), OkHttp 4.10 (2022), Coil 2.x, supabase-kt 3.6 com workaround de
  bug de auth documentado.
- `README.md` é boilerplate do AI Studio (Gemini) — não descreve o projeto.
- God ViewModel (1.280 linhas) e acoplamento ao SDK Supabase: já documentados
  como decisões conscientes em `docs/release/ARCHITECTURE-DECISIONS.md` e
  `external-audit-notes.md` (com proposta de Clean Architecture por feature).

## 4. Plano estratégico sugerido

### Fase 0 — Higiene (1 dia)
1. `GEMINI_API_KEY` → ignoreList + rotacionar.
2. Consertar imports dos testes (pacote `util.voting`) e recolocar CI verde.
3. Corrigir AboutScreen (texto de privacidade condizente com Supabase).
4. Remover/esconder: card Estatísticas mockado, rating fake, easter egg
   "Luciana", fallbacks "Café Lispector"/"24 DE OUTUBRO"/"de 25", pill "em 3
   dias" (calcular de verdade), botão MoreVert morto.
5. Corrigir rótulo "No ritmo" invertido e "Lendo:" no perfil (usar
   `currentBooksMap`).
6. README real.

### Fase 1 — Fidelidade visual "Claude Design" (3-5 dias)
Ordem de impacto por esforço:
1. Fontes em `res/font/` (fallback embarcado) — maior impacto, menor esforço.
2. `Modifier.rodapeShadow()` tingido + aplicar nas receitas do design (card,
   bottom nav verde, ticket, capas, sheet).
3. Adicionar `#E6BF6B` (dourado) aos tokens; usar em estrelas e dots.
4. Header custom (avatar + pill de clube + sino circular com badge) no lugar do
   TopAppBar Material.
5. Importar os ícones stroke 1.6px de `icons.jsx` como ImageVector.
6. Ticket perfurado (Path com notches + tracejado + stub), dia 64sp itálico.
7. Cover com lombada + ornamentos por hash; sombra dramática no hero.
8. ReaderChips 72dp com gradiente pastel + anéis semânticos.
9. Press-scale 0.98 nos botões; hierarquia tipográfica (28 serif na home, 26 no
   hero, serif nos títulos de capítulo).
10. Decidir: 4 abas (design) vs 5 abas (atual).

### Fase 2 — UX sistêmico (1 semana)
1. `UiState` mínimo: distinguir loading real de empty (resolve os falsos
   "não encontrado" e o flash de NoClubsEmptyState).
2. SnackbarHost global + feedback padrão de sucesso/erro em toda mutação,
   ligado ao `pendingMutationsCount` ("salvo offline, sincronizando…").
3. Pull-to-refresh nas listas principais.
4. Confirmações: excluir frase (com undo), concluir encontro; decremento de
   progresso.
5. Disabled/loading em todo botão de submit (JoinClub, EditProfile, RSVP).
6. Unificar tom de voz (decidir "tu" ou "você") e política de senha do Reset.

### Fase 3 — Dados robustos (1-2 semanas)
1. Unificar TODA escrita no padrão optimistic + fila (envelope único,
   `@Serializable` sealed class no payload, `jsonPrimitive.content`).
2. Higiene da fila: 4xx descarta com aviso, 5xx retry com limite, não limpar no
   logout sem drenar.
3. Ciclo de vida Realtime: coleções concorrentes, unsubscribe/unregister,
   singleton do repositório com scope cancelável.
4. Versionar schema no repo (supabase CLI) + mover notificações pra
   trigger/RPC.
5. PK de votes + Migration; timezone de meetings; capas com URL on-demand.
6. Testes: DAO in-memory, fila (enqueue→drain), mapeadores DTO.
7. Atualizar Compose BOM, OkHttp, Coil, supabase-kt.

### Fase 4 — Gaps de produto pré-launch
1. **Exclusão de conta** (bloqueador de Play Store) + sair do clube.
2. Tela de Configurações.
3. Deep link de convite (reativar o parser).
4. Editar/apagar próprio comentário.
5. Pós-v1: FCM push, perfil de membro, estatísticas reais, descoberta de
   clubes públicos (ou remover a opção "aberto a quem tem link").

### Fase 5 — Release (retomar de onde parou)
Checklist manual: Resend/SMTP, email confirmation, linking Google↔email,
redirect URLs, SHA-1s, conta Play (US$ 25), Privacy Policy em URL público,
assets gráficos, internal testing → produção. Tudo já documentado em
`docs/release/`.
