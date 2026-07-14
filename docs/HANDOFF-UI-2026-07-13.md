# HANDOFF — Trabalho Visual Rodapé (para o próximo agente)

> Data: 2026-07-13 · Autor da entrega: sessão anterior (Claude) · Leia ISTO + `docs/PLANO-UI-2026-07-13.md` antes de tocar em qualquer coisa.

O `PLANO-UI-2026-07-13.md` é o **plano-mestre** (5 ondas). Este doc diz **onde paramos**, **as regras** e **exatamente o que fazer a seguir** pra você não errar.

> **Atualização 2026-07-13 (sessão 2):** Onda 1 fechada na parte estrutural.
> Desde o handoff original: swap Material→RodapeIcons não-chevron (`ebf0f99`),
> raio → RodapeRadii (`9be7ac9`+`13be272`), tipografia type-misuse parcial
> (`8f57477`), **Overline compartilhado** (`e80c1b6`, render idêntico) e
> **RodapeDialog + ThemedRadio/Checkbox** (`0d1bda9`, AdminDialogs 6 + MainTabs 6).
> **Adiado de propósito pro checkpoint visual** (muda render, não dá pra validar
> só com compile): os `.copy(fontSize=14/16/20)` que espremem headline em
> Shelf/NextTab (1.4). **Follow-up maior:** ~24 `AlertDialog` Material ainda
> espalhados por 9 telas → migrar pra RodapeDialog app-wide, incremental.
>
> **Atualização 2026-07-13 (sessão 2, parte 2): ONDA 2 (movimento) implementada**
> em 9 commits (`d946df3`…`2e2061f`), tudo compilando limpo. **PENDENTE DE
> CHECKPOINT VISUAL — animação não se valida com compile; rodar o app antes da
> Onda 3.** O que entrou:
> - **2.1 infra**: `RodapeMotion` (Dur fast/standard/emphasized=120/240/400 +
>   easings M3) em `ui/theme/RodapeMotion.kt`; `LocalReducedMotion` lido de
>   `ANIMATOR_DURATION_SCALE==0` e provido no `MyApplicationTheme`; helpers
>   `rodapeTween`/`rodapeSpring` degradam pra `snap()` sob reduced-motion.
> - **2.2 navbar**: BottomBarItem UNIFORME (sempre Column; labels sempre
>   visíveis — desvio consciente do plano, decisão de UX pra letramento
>   variado); seleção = cor animada + spring no ícone (draw-only). Badge de
>   votação entra/sai com mola. Zero reflow.
> - **2.3 trocas**: fade-through (`AnimatedContent`) nas 5 abas do MainTabs
>   (`SaveableStateProvider` DENTRO do lambda, keyado no param — estado por aba
>   preservado durante a transição), sub-tabs NextTab, 5 abas BookDetail,
>   cabeçalho do Onboarding. **Deferido**: corpo do step do Onboarding (exige
>   reestruturar LazyColumn + footer fixo).
> - **2.4 entradas**: primitivo único `Modifier.staggeredEntrance(index)`
>   (`ui/components/Entrance.kt`, fade+rise 8dp, delay capado em 6 degraus,
>   draw-only). Aplicado: Home (greeting→headline), Welcome (pílula→headline→
>   subhead→4 lombadas pousando), estante (capas em stagger), notificações.
>   **Falta**: listas de votação/agenda (fácil, mesmo primitivo).
> - **2.5 recompensa**: carimbo de RSVP (punch de mola + check + HÁPTICO — 1º
>   háptico do app), checklist (anel de progresso em Canvas, círculos morfando,
>   ✓ texto→RodapeIcons.Check, conector enchendo), barras de progresso enchem +
>   count-up do %, badge do header pulsa ao incrementar, estrela scale-pop.
>   **Falta**: "marcar capítulo lido" com confete nos marcos (casa com Onda 3).
> - **2.6 shimmer**: varredura de gradiente diagonal ~20° lida SÓ na draw phase
>   (zero recomposição/frame), UMA transition por grupo, raios corrigidos
>   (capa 6/4→xs=3). Reduced-motion → fill estático.
> - **2.7 nav**: transições direcionais no NavHost (avançar desliza da direita,
>   voltar devolve; emphasized decel/accel; None sob reduced-motion).
> Padrões pra manter: specs de `transitionSpec` são hoisteados (não é contexto
> composable); punchs de mola usam guard de 1ª composição; animação de escala é
> sempre `graphicsLayer` (draw-only). Próximo: **checkpoint visual → Onda 3**.
>
> **Atualização 2026-07-13 (sessão 2, parte 3): ONDAS 3 e 4 implementadas**
> (`507e333`…`8899fc1`, 15 commits, tudo compilando limpo). **TUDO pendente de
> checkpoint visual em device.** O que entrou:
>
> **ONDA 3 (momentos-assinatura), 13 batches:**
> - 3.1 Ticket físico na NextTab (canhoto oliva + day-stamp serif + picote +
>   notches ANCORADOS via onGloballyPositioned) + fix bug 8 no ticket do Home.
> - 3.2 MeetingDetail header = canhoto EXPANDIDO do ticket (olivaDeep + day-stamp
>   + picote + ticketShadow; concluído rebaixa pra neutro).
> - 3.3 Votação AO VIVO: cards ordenados por votos reordenando com animateItem,
>   líder com aro dourado + selo "Na frente" (Trophy, liveRegion), countdown com
>   dot pulsando, manchete "N votos · M membros", justificativa unificada
>   (texto-link), stepper com ícones (novo RodapeIcons.Minus), PillToggle real
>   em duração/cadência, renames de desambiguação.
> - 3.4/3.5 RSVP SEMÂNTICO nos 2 pickers: Vou=olivaSoft/olivaDark,
>   Talvez=warningSoft/warning (dourado legível), Não vou=neutro; MeetingDetail
>   ganhou liveRegion + raio pílula. "Quem vai?": barra empilhada + contadores
>   casando + RsvpPeekAvatars nas linhas colapsadas.
> - 3.6 Hero do livro: backdrop AMBIENTE (capa borrada, API 31+ guard), capa em
>   fade+rise, anel de progresso enchendo; spoiler com CADEADO + debate
>   dissolvendo; "Marcar que li" virou TbButton; timeline do histórico só com
>   marcos data-driven.
> - 3.7 Chapter list: linha colapsada "Cap.N · título · ⋮" (ações no menu),
>   reorder anima (animateItem), terracota só no destrutivo.
> - 3.8 QuoteCard KEEPSAKE (gradiente, aspas emoldurando, atribuição interna,
>   acento variado por card, long-press) + **share como PNG** (GraphicsLayer→
>   toImageBitmap→FileProvider NOVO no manifest + xml/file_paths.xml).
> - 3.9 Estante cover-first: sem card branco, capa 128×192 com shelfCoverShadow
>   (novo em Shadows.kt), gutter 12, sem estrelas vazias, filtro SegmentedControl
>   "♥ Favoritos (N)", empty com prateleira geométrica.
> - 3.10 Intro: emoji → spot illustrations GEOMÉTRICAS (livro/prateleira/
>   conversa/calendário) + parallax por currentPageOffsetFraction; spines da
>   Welcome com sombra tingida.
> - 3.11 Notificações: trilho terracota 3dp no unread + peso condicional, swipe
>   = marcar lida (SwipeToDismissBox; não há delete no backend), member_removed
>   neutro / member_finished dourado, espaçamento 10dp, empty com personalidade.
> - 3.12 Moderação como TIMELINE (rail + nós terracota + grupos por data +
>   bloco citado riscado; RodapeDialog no restaurar).
> - 3.13 Polish: Suggest sem CTA duplicado (barra inferior com loading), About
>   app bar unificada + feedback OlivaSoft, ManageClub com HERO dashboard.
> - 3.14 Branded loader: lombadas respirando (CenteredLoading).
>
> **ONDA 4 (fecho), batch 1:**
> - Splash de marca (core-splashscreen; Theme.App.Starting no manifest).
> - Predictive back opt-in; status bar icons acompanham o TEMA DO APP.
> - Coil crossfade nas capas; pull-to-refresh terracota/cream; RodapeSnackbarHost
>   único (ink/cream/dourado) nos 3 Scaffolds. Icon monochrome já existia.
>
> **DEFERIDO (exige device/olho — próxima sessão):** 4.2 dynamic type sweep,
> landscape/tablet (WindowSizeClass), RTL; 4.4 contraste AA dark + TalkBack
> sweep; drag-to-reorder real (3.7); shared-element ticket→detail (3.2, exige
> SharedTransitionLayout no NavHost); ~20 AlertDialog Material restantes →
> RodapeDialog; unificar indicador do first-run + animar preview de fonte
> (3.10); estados de erro com retry padronizado (4.3); ChatTab reusar bolha do
> Discussion (3.6); confete de marco de leitura; corpo do step do Onboarding.
> Os pontos de dynamic type mais flagrados (ticket notches, navbar, chooser)
> JÁ foram resolvidos estruturalmente nas Ondas 2-3.

---

## 0. PRIMEIRA COISA A FAZER (obrigatório)

O tree entregue **compila limpo** (verificado, exit 0, incluindo o swap de chevrons). Ainda assim, rode o type-check ANTES de editar (baseline) e DEPOIS de cada batch:

```bash
cd /home/gabrielbarbosa/dev/gabriel/rodape
./gradlew :app:compileDebugKotlin -x processDebugGoogleServices --no-configuration-cache -q
```

- **Sem saída + exit 0 = compilou limpo.** Pode seguir.
- Imports Material órfãos (após trocar por RodapeIcons) são só *warning*, não quebram build.

### Por que essas flags no gradle
- `-x processDebugGoogleServices`: o applicationId de debug é `app.rodape.debug` e o `app/google-services.json` não tem client pra esse pacote → a task falha. Type-check do Kotlin não precisa dela.
- `--no-configuration-cache`: evita disputa do lock `.gradle/configuration-cache` quando há outro daemon (Android Studio) rodando.
- Toolchain já instalada por `scripts/setup-wsl-build.sh` (JDK 17 + Android SDK em `~/Android/Sdk`). Se estiver em máquina nova, rode esse script uma vez.

---

## 1. CONTEXTO

App Android Jetpack Compose, clube de leitura ("Rodapé"). Objetivo: elevar a **experiência visual** a nível de premiação. Uma auditoria de 6 especialistas gerou o `PLANO-UI`. A fundação de design já é forte: paleta semântica + dark mode próprio, Literata (serif, títulos) + Inter (UI), sombras **tingidas** em camadas, WCAG, a11y.

**Os 5 problemas transversais** (aparecem em todo lugar): (1) movimento ≈ zero, (2) iconografia fraturada, (3) caos de raio/sombra, (4) escala tipográfica combatida com overrides, (5) tokens misturados com valores crus.

---

## 2. O QUE JÁ FOI FEITO (tudo commitado + pushado em `master` → github.com/gabrielfeelix/rodape)

| Commit | Conteúdo |
|---|---|
| `eda1920` | **Onda 0** — 7 bugs: `TbButtonVariant.OlivaSoft` (não compilava), Outline branco no dark→cardSurface, gradiente duplicado do BookDetail, borda dupla do input do Discussion, footprint do avatar preset, anel no badge da nav, imports mortos |
| `814e2ff` | **Onda 1 fundação** (aditivo, sem tocar telas): `RodapeRadii`, token `warning`/`warningSoft`, `TbButton(loading)`, `PillToggle`, `RodapeTextField`, `SegmentedControl<T>`, `EmptyState` |
| `109c702` | **Onda 1 migr.1** — RatingStars (StarFill/Star + token dourado + meia-estrela), warning no banner do Suggest, `RodapeCardElevated`, stat "frases" sem cinza Material |
| `3fbf46e` | **Onda 1 migr.2** — iconografia: +Info/Warning/Camera; obturador 📷, "Sobre" ℹ️, banner ⚠️ → Icon |
| `0ace8b9` | **Onda 1 migr.3** — FIM do emoji-como-ícone: +Back/MoreV/Close/Share/Trash/Image/Link; `TbButton.leadingIcon`; botões AddBook/Suggest/ManageClub com ícone; captions `📖 Caps`/`📍`/`📚` → texto puro |
| (este commit) | **chevrons** — `KeyboardArrow*`/`ArrowDropDown` → `RodapeIcons.Chev*` em ManageChapters/MainTabs/NextTab (3 arquivos). Verificado (exit 0). Vai junto com este doc. |

### Componentes/tokens NOVOS disponíveis — USE, não reinvente
- `RodapeRadii` (em `ui/theme/Tokens.kt`): `.xs`=3 (capas), `.sm`=12 (chips/banners/campos), `.md`=20 (cards), `.full`=999 (pílulas).
- Token `RodapeTheme.colors.warning` / `.warningSoft` (aviso/conflito, base dourada; light+dark).
- `TbButton(..., loading = Boolean, leadingIcon = ImageVector?)` — spinner e ícone-líder. **Todo fluxo async deve usar `loading`; todo botão com ícone usa `leadingIcon`.**
- `RodapeTextField(...)` (`ui/components/RodapeTextField.kt`) — campo único (fill cream, foco no acento, raio sm). **Todo formulário deve migrar pra ele.**
- `SegmentedControl<T>(options, selected, onSelect, label)` (`ui/components/SegmentedControl.kt`).
- `PillToggle(text, selected, onClick)` (em `ui/components/Chips.kt`).
- `EmptyState(title, description?, icon?, illustration?, action?)` (`ui/components/EmptyState.kt`).
- `RodapeCardElevated(containerColor?, contentColor?, ...)` (`ui/components/Cards.kt`) — card com sombra flutuante tingida (sem cinza Material).

### RodapeIcons — inventário atual (`ui/theme/RodapeIcons.kt`)
Existentes: Home, Book, Calendar, User, Bell, ChevD/R/L/U, Plus, Check, CheckCircle, Lock, Search, Send, Smile, More, Arrow, Edit, Pin, Clock, Star, StarFill, Vote, Shelf, Groups, Log, Google, Heart, Reply, Exit, Trophy, Bars, **Info, Warning, Camera, Back, MoreV, Close, Share, Trash, Image, Link** (os 10 em negrito foram adicionados nesta entrega).

**NÃO existem ainda** (por isso continuam como `Icons.*` Material — NÃO invente vetor torto sem conseguir renderizar/ver): engrenagem **Settings** (header admin), **Visibility/VisibilityOff** (olho de senha), **Refresh**, **VerifiedUser** (escudo), **FormatQuote**, **Favorite** (coração preenchido). Se for desenhar algum, faça com muito cuidado e peça pro usuário conferir visualmente — vetor errado compila mas fica feio.

---

## 3. REGRAS (não quebre — vale pra toda edição)

1. **Compile depois de cada batch** com o comando da seção 0. Nunca empilhe muitos arquivos sem compilar.
2. **Commit + push a cada batch coerente** (não PR gigante). Mensagem em pt, formato `feat: Onda X (migração N/…) — …`, e trailer:
   `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>`
3. **Segurança de segredo:** NUNCA `git add -A` cego. Existem `.env` e `aplicativos-*-firebase-adminsdk-*.json` (gitignorados) na raiz — repo é **público**. Sempre `git add <arquivos>` explícitos e rode antes de commitar:
   `git diff --cached --name-only | grep -iE "\.env|adminsdk|secret|key\.json"` (tem que voltar vazio).
4. **Sem hex cravado** de cor → use `RodapeTheme.colors.*` ou tokens. (Exceção legítima: cores da marca Google no ícone Google.)
5. **Sem emoji como ícone/label/marcador** → `RodapeIcons.*` ou `TbButton.leadingIcon`. Emoji em **copy/mensagem/snackbar/texto de share** é OK (🎉💚⭐ etc. — deixe).
6. **Raio** só da escala `RodapeRadii`. **Sombra** só via `RodapeCard`/`RodapeCardElevated`/modifiers de `ui/theme/Shadows.kt` — **nunca** `CardDefaults.cardElevation` (cinza Material).
7. **Tipo** via `MaterialTheme.typography.*` / tokens; evite `.copy(fontSize = …)` que briga com a escala.
8. **Cor sempre por `RodapeTheme.colors.*`** (theme-aware/dark), nunca as constantes cruas (`Terracota`, `Oliva`…) em UI.
9. **A11y:** preserve `contentDescription`, `role`, alvos 48dp (`minimumInteractiveComponentSize`), `liveRegion` já existentes.
10. **Não** rode `git commit -am`/`git add -A`; **não** toque em `.env`, `google-services.json`, `local.properties`, settings.

---

## 4. O QUE FALTA — ONDA 1 (terminar primeiro)

### 4.1 — Iconografia, resto do sweep (1.3) — VETOR Material → RodapeIcons
Emoji-como-ícone JÁ ACABOU. Falta trocar ícones **Material que têm equivalente RodapeIcons**. Chevrons já foram (verifique compile). Mapa a aplicar (grep `Icons\.` por arquivo, trocar `imageVector =`):

| Material | → RodapeIcons | Material | → RodapeIcons |
|---|---|---|---|
| ArrowBack (19×) | `Back` | DateRange / EventNote | `Calendar` |
| ArrowForward | `Arrow` | MenuBook / Book | `Book` |
| Add | `Plus` | Place | `Pin` |
| Delete | `Trash` | Schedule | `Clock` |
| Clear | `Close` | Notifications | `Bell` |
| MoreVert (4×) | `MoreV` | FavoriteBorder | `Heart` |
| Share (3×) | `Share` | List | `Log` |
| Info | `Info` | ThumbUp | `Vote` |
| Warning | `Warning` | PersonAdd | `Groups` (aprox.) |
| Check / CheckCircle | `Check`/`CheckCircle` | AddReaction | `Smile` |
| Search | `Search` | Send / Edit / Lock / Star | `Send`/`Edit`/`Lock`/`StarFill` |

**MANTER Material** (sem vetor equivalente): Settings, Visibility, VisibilityOff, Refresh, VerifiedUser, FormatQuote, Favorite(preenchido).

**Como fazer, por arquivo** (não app-wide cego): para cada arquivo com `Icons.`, trocar `imageVector = Icons.<qualquer>.<Symbol>` → `RodapeIcons.<Target>`; garantir `import com.example.ui.theme.RodapeIcons` OU `import com.example.ui.theme.*` presente (a maioria das telas já tem `theme.*`); imports Material órfãos podem ficar (warning). **Compile a cada 2–3 arquivos.** `ArrowBack` está em TopAppBars via `Icons.AutoMirrored.Outlined.ArrowBack` — o `Back` novo é seta pra esquerda, serve; confira que fica bem em RTL (é `AutoMirrored` no Material; `RodapeIcons.Back` não auto-espelha, então em telas RTL talvez prefira manter comportamento — de baixo risco pro pt-BR).

Arquivos com Material icons: AdminDialogs, QuoteCard, AboutScreen, AddBookManualScreen, BookDetailScreen, DiscussionScreen, ForgotPasswordScreen, FrasesScreen, MainTabsScreen, ManageChaptersScreen, ManageClubScreen, MeetingDetailScreen, ModerationLogScreen, NextTabScreen, NotificationsScreen, ResetPasswordScreen, ShelfTabScreen, SignUpScreen, SuggestScreen, WelcomeScreen.

Ainda dentro de 1.3, o **header admin** (`MainTabsScreen` GlobalHeader) usa `Icons.Outlined.Settings` ao lado do `RodapeIcons.Bell` — inconsistência #1 citada. Só resolve com um vetor de engrenagem novo (não temos). Ou desenhar com cuidado (pedir revisão visual) ou deixar anotado.

### 4.2 — Raio (1.1)
Trocar literais `RoundedCornerShape(6/10/12/14/16/22/24.dp)` pela escala. Mapa: 3→`RodapeRadii.xs`, 12→`.sm`, 20→`.md`, 999→`.full`; 14/16→`.sm` ou `.md` conforme o elemento (chip/banner→sm, card→md). ~40 sites. Import `com.example.ui.theme.RodapeRadii` por arquivo. Cuidado: não force um raio que muda a intenção (ex.: pílula tem que continuar `.full`). Compile por grupo.

### 4.3 — Tipografia (1.4)
- Extrair um `Overline` compartilhado (uppercase + letter-spacing + muted) — hoje redeclarado ~10× inline. Criar em `ui/components` e migrar call-sites.
- Remover `.copy(fontSize = …)` que espreme display/headline (ex.: estante 217/225/235, ticket NextTab 388/409, MeetingDetail 140, stats do perfil). Definir token de escala-média se faltar (em `ui/theme/Type.kt`).
- Corrigir título de `TopAppBar` em `headlineLarge` (Forgot ~59, Reset ~72) → `titleLarge`; frase inteira em `displaySmall` (Forgot ~87, Reset ~96) → `bodyLarge`/`titleMedium`.
- `Type.kt` já aplica Literata em display/headline/title por padrão — telas que NÃO sobrescrevem `fontFamily` já herdam Literata (não precisa "forçar" em toda tela).

### 4.4 — RodapeDialog (1.5)
Criar `RodapeDialog` (shell: título Literata sempre, raio `.md`, sombra tingida, ações `TbButton`) + `ThemedRadio`/`ThemedCheckbox` tingidos oliva. Migrar os **6 dialogs** de `ui/admin/AdminDialogs.kt` (hoje `AlertDialog` Material com raio 28dp + cinza + Radio/Checkbox roxo default) e o rate dialog do `MainTabsScreen`.

---

## 5. PRÓXIMAS ONDAS (detalhe completo no PLANO-UI seções ONDA 2/3/4)

- **ONDA 2 — Movimento** (a maior lacuna). PRÉ-REQUISITO: criar `RodapeMotion` (durations/easings) **e respeitar reduced-motion** (ler `Settings.Global.ANIMATOR_DURATION_SCALE`/preferência do sistema e degradar). Depois: navbar sem pulo (BottomBarItem sempre `Row`, animar label — hoje troca Column↔Row e empurra vizinhos), `AnimatedContent` nas trocas de aba/sub-tab/step, entradas encenadas (Home, Welcome, estante), **carimbo de RSVP**, checklist morfando, barras de progresso enchendo (`animateFloatAsState`), skeleton com shimmer real (varredura de gradiente, uma transição compartilhada — hoje é só pulso de opacidade).
- **ONDA 3 — Momentos-assinatura:** MeetingTicket como ticket físico (notches ancorados via `onGloballyPositioned`, `ticketShadow`) + shared-element pro MeetingDetail; votação ao vivo (barras animando, líder com selo/rank, countdown pulsando); progresso de leitura recompensado (anel na capa, marcar-lido com recompensa); **QuoteCard keepsake + compartilhar-como-imagem** (render pra PNG — recurso viral); estante cover-first (tirar card branco, prateleira com sombra); ilustrações da Welcome/Intro (trocar emoji 📖📚 da IntroArt por vetores de marca); moderação como timeline real; polish About/Suggest/ManageClub.
- **ONDA 4 — Fecho (o que a auditoria não cobriu):** splash screen animada, app icon adaptável/monochrome, **edge-to-edge + status bar clara/escura por tela**, predictive back gesture; passe de **dynamic type** (ticket/checklist/nav já quebram em fonte grande), landscape/tablet (`WindowSizeClass`), RTL; estados erro/offline/loading padronizados, crossfade no load de capa real (Coil); **reduced-motion** (pré-req de motion), contraste dark (labels `olivaSoft` sobre gradiente oliva no ticket NextTab ~402/416/422); identidade de movimento, mapa de **háptico**, microcopy.

---

## 6. BUGS/GOTCHAS CONHECIDOS
- Avatar preset: footprint agora é `size×size` e a ilustração **transborda** pro topo (sem clip). Se um pai clipar, o topo corta — ok pra maioria.
- `MEMORY.md` do usuário tem uma nota `rodape-no-local-build` **desatualizada** (dizia que não dava pra compilar local — agora dá, ver `build-headless-wsl`). Ignore a antiga.
- Emoji em `IntroScreen` (📖📚) e em share text (`AppIntents`, `FrasesScreen` "📖 Frases salvas") foram **deixados de propósito** (arte/onboarding = Onda 3; share text = copy).
- A exceção `AWT-EventQueue … NullPointerException … ksp.com.intellij` no fim do compile é ruído do KSP no shutdown — **não é erro** se `exit 0`.

---

## 7. RESUMO DE 1 LINHA
Onda 0 ✅ · Onda 1 ✅ · Onda 2 (movimento) ✅ · Onda 3 (13 momentos-assinatura) ✅ · Onda 4 (fecho batch 1: splash, predictive back, status bar, crossfade, pull-refresh, snackbar) ✅ — **TUDO pendente de checkpoint visual em device**; deferidos listados no bloco de atualização acima (dynamic type/RTL/tablet/AA/TalkBack + itens que exigem olho). Compile e commite a cada batch. Não vaze segredo.
