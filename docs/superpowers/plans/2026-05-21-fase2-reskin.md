# Fase 2 — Re-skin das Telas: Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Aplicar o design system da Fase 1 (cores oliva/terracota, tipografia Literata, componentes `TbButton`/`Pill`/`Cover`/`Avatar`/`TramabookCard`/`TbSectionHeader`) a todas as telas do app, mantendo a lógica intacta.

**Architecture:** Cada tarefa re-skina uma tela (ou um grupo coeso). A camada de dados (`MainViewModel`, contratos de flow/método) NÃO muda — só a árvore Compose. O app compila e navega ao fim de cada tarefa. Ao final, os aliases de compatibilidade (`VerdeMusgo`, `FrauncesFontFamily`) e o legado de `CommonComponents.kt` que ninguém mais usar são removidos.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, componentes da Fase 1.

---

## Contexto para quem nunca viu o projeto

- Tramabook é um app Android de clube de leitura. Telas em `app/src/main/java/com/example/ui/screens/`.
- A Fase 1 entregou os componentes em `app/src/main/java/com/example/ui/components/` — `Cover`, `Avatar`, `TbButton` (variantes `Primary`/`Terra`/`TerraSoft`/`Outline`/`Dark`/`OlivaSoft`, tamanhos `Sm`/`Md`/`Lg`), `Pill` (`Default`/`Olive`/`OliveDeep`/`Terra`/`Mustard`/`Ink`/`Outline`), `ProgressBar`, `TramabookCard`, `TbSectionHeader`.
- Tokens de cor em `ui/theme/Color.kt`: `Terracota`, `TerracotaDark`, `TerracotaSoft`, `Oliva`, `OlivaDark`, `OlivaDeep`, `OlivaSoft`, `OlivaMid`, `Tertiary`, `TertiarySoft`, `Ink`, `InkSoft`, `Muted`, `Paper`, `PaperDeep`, `CardSurface`, `CardSoft`, `Cream`, `Divider`, `DividerSoft`, `DisabledSurface`. Fontes: `LiterataFontFamily`, `InterFontFamily` em `ui/theme/Type.kt`.
- O protótipo-alvo é `claude-design/` (abrir `Tramabook.html`). Telas: `screens-onboarding.jsx`, `screens-main.jsx`, `screens-next.jsx`, `screens-aux.jsx`.
- O spec completo: `docs/superpowers/specs/2026-05-21-tramabook-redesign-design.md` — **a seção 5 (regras de UI) é o checklist a seguir.**
- Build: `cd ~/dev/tramabook && export JAVA_HOME=$HOME/.local/opt/jdk-21 ANDROID_HOME=$HOME/.local/opt/android-sdk && ./gradlew assembleDebug`.

## Princípios do re-skin (aplicar em TODA tarefa)

1. **Não tocar na lógica.** Callbacks, validações, navegação, chamadas de `viewModel` permanecem idênticos. Só a aparência muda.
2. **Oliva é herói, terracota é acento.** Substituir usos de terracota como cor dominante por oliva. Terracota só em: botão de ação primário (quando o protótipo usa terracota), badge "Atual", barra de progresso do usuário, contador de notificações.
3. **Usar os componentes da Fase 1** no lugar dos legados: `TbButton` em vez de `PillButton`, `Cover` em vez de `BookCover`, `Avatar` em vez de `MemberAvatar`, `TramabookCard` em vez de `StandardCard`, `TbSectionHeader` em vez de `SectionHeader`.
4. **Tipografia:** usar `MaterialTheme.typography.*` (já é Literata/Inter). Onde precisar de família explícita, `LiterataFontFamily` (editorial) / `InterFontFamily` (UI). Não usar `FontFamily.Serif`.
5. **Cores via tokens** de `Color.kt` ou `MaterialTheme.colorScheme`. Eliminar `Color(0x...)` hardcoded — exceto os tons que o protótipo realmente exige e que não têm token (documentar).
6. **Idiomático Android:** manter `Scaffold`, `ModalBottomSheet`, `ripple`, insets. Reproduzir a aparência do protótipo sem copiar comportamento web.
7. Ao fim de cada tarefa: **app compila** (`assembleDebug` verde).

---

## Task 1: Onboarding — Welcome

**Files:**
- Modificar: `app/src/main/java/com/example/ui/screens/WelcomeScreen.kt` (apenas a função `WelcomeScreen`, linhas ~46-148)

A função `WelcomeScreen` mostra logo, headline "Leituras juntas." e 3 botões. Protótipo de referência: `WelcomeScreen` em `claude-design/screens-onboarding.jsx`.

- [ ] **Step 1: Re-skinar `WelcomeScreen`**

Aplicar:
- Headline: "Leituras" + "juntas." onde "juntas" é itálico em `Oliva` e o "." em `Terracota`. Usar `MaterialTheme.typography.displayLarge` com `LiterataFontFamily`. Construir com `buildAnnotatedString` para o trecho colorido/itálico.
- Subtítulo "Um clube. Um livro. Conversa que não dá spoiler." em `MaterialTheme.typography.bodyLarge`, cor `MaterialTheme.colorScheme.onSurfaceVariant`.
- Botão "Criar um clube" → `TbButton(text = "Criar um clube", onClick = onNavigateToCreateClub, variant = TbButtonVariant.Terra, size = TbButtonSize.Lg, modifier = Modifier.fillMaxWidth())`.
- Botão "Entrar num clube" → `TbButton(text = "Entrar num clube", onClick = onNavigateToJoinClub, variant = TbButtonVariant.Outline, size = TbButtonSize.Lg, modifier = Modifier.fillMaxWidth())`.
- "Já tenho conta" permanece como texto clicável, cor `Terracota`.
- Manter a `Image` do `R.drawable.ic_logo` e a animação `AnimatedVisibility`/`fadeIn`.
- Remover imports de `PillButton`; manter `Terracota`; adicionar imports de `TbButton`, `TbButtonVariant`, `TbButtonSize`.

NÃO mudar a assinatura `WelcomeScreen(onNavigateToLogin, onNavigateToCreateClub, onNavigateToJoinClub)` nem o `Scaffold`.

- [ ] **Step 2: Compilar** — `./gradlew compileDebugKotlin`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/example/ui/screens/WelcomeScreen.kt
git commit -m "feat(onboarding): re-skin da tela Welcome com componentes da Fase 1"
```

---

## Task 2: Onboarding — Login

**Files:**
- Modificar: `app/src/main/java/com/example/ui/screens/WelcomeScreen.kt` (apenas a função `LoginScreen`, ~150-449)

`LoginScreen` tem segmented control Entrar/Criar conta, campos de email/senha/nome, botão de submit, botão de conta de teste, botão Google. Referência: `LoginScreen` em `screens-onboarding.jsx`.

- [ ] **Step 1: Re-skinar `LoginScreen`**

Aplicar:
- Substituir `PillButton` de submit por `TbButton(variant = TbButtonVariant.Terra, size = TbButtonSize.Lg, modifier = Modifier.fillMaxWidth(), enabled = isFormValid)`.
- Manter o segmented control (Entrar/Criar conta) mas alinhar cores: fundo `PaperDeep`, item ativo `CardSurface` com leve sombra, texto ativo `Ink`, inativo `Muted`. (O atual usa Terracota no item ativo — trocar para o estilo creme do protótipo.)
- `OutlinedTextField`s: `focusedBorderColor = Terracota`, container `Cream`, shape `RoundedCornerShape(14.dp)`.
- Labels dos campos ("Nome", "Email", "Senha") em overline: `MaterialTheme.typography.labelMedium`, uppercase, cor `Tertiary`.
- Título do card ("Bem-vindo de volta" / "Criar conta") em `MaterialTheme.typography.displaySmall`.
- Botão "Entrar na Conta de Teste" e o card de dica: manter (são úteis para demo), mas alinhar cores ao novo tema — fundo `OlivaSoft` para o card de dica, texto `OlivaDark`.
- Botão Google → `TbButton(variant = TbButtonVariant.Outline, size = TbButtonSize.Lg)`.
- Trocar `FontFamily.Serif` se houver por nada (o `MaterialTheme.typography` já é Literata).
- Manter toda a lógica: `isSignUp`, validações, `onLoginSuccess`.

NÃO mudar a assinatura nem o `Scaffold`/`TopAppBar`.

- [ ] **Step 2: Compilar** — `./gradlew compileDebugKotlin`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/example/ui/screens/WelcomeScreen.kt
git commit -m "feat(onboarding): re-skin da tela Login"
```

---

## Task 3: Onboarding — Criar Clube e Entrar Clube

**Files:**
- Modificar: `app/src/main/java/com/example/ui/screens/WelcomeScreen.kt` (funções `CreateClubScreen` ~451-808 e `JoinClubScreen` ~810-1151)

- [ ] **Step 1: Re-skinar `CreateClubScreen`**

- O picker de cores: usar os 5 presets de `ClubColors` (de `ui/theme/Tokens.kt`) — `ClubColors[i].bg`. **Importante:** o `onCreateCompleted` deve passar o ÍNDICE como string (`selectedColorIndex.toString()`), NÃO o hex. (Ver nota no spec sobre o conflito índice-vs-hex; esta tarefa unifica para índice.)
- Substituir o `Button` final por `TbButton(text = "Criar clube", variant = TbButtonVariant.Terra, size = TbButtonSize.Lg, enabled = isNameValid, modifier = Modifier.fillMaxWidth())`.
- Cards de privacidade e o card branco de inputs: usar `TramabookCard` ou `Surface` com os tokens (`CardSurface`, `Divider`). Seleção ativa com borda `Terracota`.
- `OutlinedTextField`s alinhados como na Task 2.
- Remover `FontFamily.Serif` do `TopAppBar` title — usar `MaterialTheme.typography.titleLarge` (já Literata).

- [ ] **Step 2: Re-skinar `JoinClubScreen`**

- Segmented control "Com código"/"Com link": mesmo estilo creme da Task 2.
- Os 6 `OutlinedTextField` do code-input: borda `Terracota` no foco, container `Cream`, shape 12.dp.
- Botão "Confirmar" → `TbButton(variant = TbButtonVariant.Terra, size = TbButtonSize.Lg)`.
- "Não tenho código" texto clicável `Terracota`.
- Manter `CustomBottomBar` no `bottomBar` se já estiver lá — ou remover se ficar estranho numa tela de onboarding (decisão do implementer: o protótipo de join não tem bottom bar; **remover o `bottomBar`**).
- Remover `FontFamily.Serif` do title.
- Manter toda a lógica de OTP, link parsing, `onJoinWithCodeSubmit`.

- [ ] **Step 3: Compilar** — `./gradlew assembleDebug`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/example/ui/screens/WelcomeScreen.kt
git commit -m "feat(onboarding): re-skin Criar Clube e Entrar Clube"
```

---

## Task 4: Bottom Nav + estrutura do MainTabsScreen

**Files:**
- Modificar: `app/src/main/java/com/example/ui/screens/MainTabsScreen.kt` — funções `CustomBottomBar` (~332-384), `BottomBarItem` (~386-433), e o `Scaffold`/`TopAppBar` de `MainTabsScreen` (~74-178)

- [ ] **Step 1: Re-skinar o bottom nav**

Protótipo: `BottomNav` em `claude-design/shell.jsx` — uma **pílula oliva-escura flutuante**; o item ativo é uma pílula terracota interna com ícone + label; itens inativos mostram só o ícone.

- `CustomBottomBar`: o container vira uma `Surface`/`Box` com `RoundedCornerShape(999.dp)`, cor `OlivaDeep`, flutuante (padding horizontal + sombra), respeitando `windowInsetsPadding(WindowInsets.navigationBars)`.
- `BottomBarItem`: quando ativo → fundo `Terracota` em pílula, ícone `Cream` + label `Cream` visível ao lado. Quando inativo → só ícone, cor `Cream.copy(alpha = 0.7f)`, sem label.
- Manter os 4 tabs (`home`/`book`/`next`/`profile`) e o callback `onTabSelected`.

- [ ] **Step 2: Re-skinar o header de `MainTabsScreen`**

- O `CenterAlignedTopAppBar`: `containerColor = MaterialTheme.colorScheme.background`.
- O nome do clube no título: `MaterialTheme.typography.titleLarge` com `LiterataFontFamily`, cor `Terracota` — mantém.
- Avatar do usuário (navegação) → usar o componente `Avatar` da Fase 1.
- O indicador de notificação não lida: ponto `Terracota`.
- Manter o `ModalBottomSheet` de troca de clube; alinhar suas cores aos tokens (cores de clube via `clubColorFor`).

- [ ] **Step 3: Compilar** — `./gradlew assembleDebug`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/example/ui/screens/MainTabsScreen.kt
git commit -m "feat(main): re-skin bottom nav flutuante e header"
```

---

## Task 5: Aba Início (HomeScreenTab)

**Files:**
- Modificar: `app/src/main/java/com/example/ui/screens/MainTabsScreen.kt` — função `HomeScreenTab` (~436-1009) e seus helpers

- [ ] **Step 1: Re-skinar `HomeScreenTab`**

Protótipo: `HomeScreen` em `claude-design/screens-main.jsx`. Elementos: saudação + headline "A galera tá esperando.", card de encontro (hero), strip de leitura compacto, "Onde a galera tá" (avatares com status), "Sobre o clube".

- Headline com "esperando." itálico em `Oliva` (via `buildAnnotatedString`).
- Card de encontro: fundo `OlivaDeep`, data grande em `LiterataFontFamily` itálico, pílula de contagem regressiva, avatares dos confirmados (componente `Avatar`), botão "Eu vou" como pílula. Manter a lógica de RSVP.
- Strip de leitura: usar `Cover` da Fase 1 (capa pequena), `ProgressBar` da Fase 1 para o progresso, cor `Terracota`.
- "Onde a galera tá": cada membro usa `Avatar` + pílula de status. Membro que terminou → `Pill` variante `OliveDeep`; lendo → `Pill` `Default`.
- "Sobre o clube" → `TramabookCard`.
- Substituir todos os `Color(0x...)` hardcoded por tokens. O `OlivaDeep` (`#293820`) já é token — usar.
- Manter toda a lógica de flows (`activeClub`, `currentBook`, `userProgress`, `clubMembers`, `latestMeeting`, etc.) e `onNavigateToDiscussion`/`onNavigateToTab`.

- [ ] **Step 2: Compilar** — `./gradlew assembleDebug`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/example/ui/screens/MainTabsScreen.kt
git commit -m "feat(main): re-skin da aba Início"
```

---

## Task 6: Aba Livro Atual (BookDetailScreenTab)

**Files:**
- Modificar: `app/src/main/java/com/example/ui/screens/MainTabsScreen.kt` — função `BookDetailScreenTab` (~1019-1411)

- [ ] **Step 1: Re-skinar `BookDetailScreenTab`**

Protótipo: `CurrentBookScreen` em `screens-main.jsx`. Hero oliva curvado com capa flutuante, card de progresso sobreposto, lista de capítulos, estatísticas.

- Hero: fundo `OlivaDeep`, cantos inferiores arredondados (`RoundedCornerShape(bottomStart=36, bottomEnd=36)`), capa via `Cover`, texto em `Cream`.
- Card de progresso sobreposto: `TramabookCard`, "Marcar progresso" → `TbButton(variant = TbButtonVariant.Terra)`. O anel de progresso pode ser mantido (é decorativo).
- Lista de capítulos: cada linha — capítulo concluído com check `Oliva`, atual com borda `Terracota` + `Pill` "Atual" variante `Terra`, bloqueado com cadeado. Usar tokens.
- Estatísticas em `TramabookCard`.
- Substituir `FontFamily.Serif` por `MaterialTheme.typography` (Literata).
- Manter a lógica: `currentBook`, `chapters`, `userProgress`, `updateBookProgress`, `onNavigateToDiscussion`.

- [ ] **Step 2: Compilar** — `./gradlew assembleDebug`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/example/ui/screens/MainTabsScreen.kt
git commit -m "feat(main): re-skin da aba Livro Atual"
```

---

## Task 7: Aba Perfil (ProfileScreenTab + EditProfileView)

**Files:**
- Modificar: `app/src/main/java/com/example/ui/screens/MainTabsScreen.kt` — `ProfileScreenTab` (~1414-fim) e `EditProfileView`

- [ ] **Step 1: Re-skinar `ProfileScreenTab` e `EditProfileView`**

Protótipo: `ProfileScreen` e `EditProfileScreen` em `claude-design/screens-aux.jsx`.

- Cabeçalho com `Avatar` grande (72dp).
- 3 stat cards lado a lado — um deles (frases) com fundo `Oliva` e texto `Cream`; os outros `TramabookCard`.
- Lista de clubes: cada item com bolinha de cor (`clubColorFor`), nome em `LiterataFontFamily`, `Pill` "Atual" variante `Terra` no clube ativo.
- Botão "Sair da conta" → `TbButton(variant = TbButtonVariant.Outline)` ou texto. O seletor de tema (dark mode) **deve ser removido da UI** — o app é light-only. Não chamar `viewModel.updateThemeMode`/`themeMode` na UI.
- `EditProfileView`: campos alinhados, botões "Cancelar" (`Outline`) / "Salvar" (`Terra`).
- Manter a lógica: `currentUser`, `allClubs`, `updateUserProfile`, `logout`, `selectActiveClub`.

- [ ] **Step 2: Compilar** — `./gradlew assembleDebug`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/example/ui/screens/MainTabsScreen.kt
git commit -m "feat(main): re-skin da aba Perfil e remove seletor de tema"
```

---

## Task 8: Aba Próximo (NextTabScreen)

**Files:**
- Modificar: `app/src/main/java/com/example/ui/screens/NextTabScreen.kt` (inteiro — `NextTabScreen`, `EncontroTab`, `VotacaoTab`, `EstanteTab`)

- [ ] **Step 1: Re-skinar as 3 sub-abas**

Protótipo: `NextScreen` + `MeetingTab`/`VotingTab`/`ShelfTab` em `claude-design/screens-next.jsx`.

- Sub-tabs (Encontro/Votação/Estante): texto em `LiterataFontFamily`, indicador `Terracota`.
- `EncontroTab`: card de encontro com gradiente oliva, RSVP em botões, grupos expansíveis de quem vai. `Avatar` da Fase 1.
- `VotacaoTab`: cada livro em `TramabookCard` com `Cover`, `ProgressBar` para os votos, `TbButton` para votar. "Sugerir livro" → `TbButton(variant = Outline)`.
- `EstanteTab`: grid 2 colunas de `Cover`, filtros como `Pill`/chips.
- Substituir `Color(0xFFD4A373)` e `Color(0xFFF4A261)` por tokens (`OlivaMid` para "talvez", e para a estrela usar um amarelo — manter como literal documentado se não houver token, ex.: `Color(0xFFE6BF6B)` como no protótipo).
- Trocar legados: `StandardCard`→`TramabookCard`, `SectionHeader`→`TbSectionHeader`, `MemberAvatar`→`Avatar`, `BookCover`→`Cover`, `PillButton`→`TbButton`.
- Manter toda a lógica de flows e métodos.

- [ ] **Step 2: Compilar** — `./gradlew assembleDebug`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/example/ui/screens/NextTabScreen.kt
git commit -m "feat(next): re-skin da aba Próximo (Encontro, Votação, Estante)"
```

---

## Task 9: Discussão de capítulo (DiscussionScreen)

**Files:**
- Modificar: `app/src/main/java/com/example/ui/screens/DiscussionScreen.kt` (inteiro)

- [ ] **Step 1: Re-skinar `DiscussionScreen`**

Protótipo: `ChapterScreen` + `CommentBubble` em `claude-design/screens-main.jsx`.

- Header com botão voltar, "Capítulo N" overline + título em `LiterataFontFamily`.
- Pill verde "Tu já passou daqui. Tá liberado." com fundo `OlivaSoft`, texto `OlivaDark`.
- Bolhas de comentário: comentário próprio com fundo `TerracotaSoft`; outros com `Cream` + borda `Divider`. Autor em Inter SemiBold, texto em `LiterataFontFamily`.
- Reações (emoji): chips arredondados, ativo com borda `Terracota` + fundo `TerracotaSoft`.
- Input footer: campo arredondado, botão de enviar circular `Terracota`.
- Substituir `Color(0xFF2E2A24)`/`Color(0xFFFBF7EE)` por tokens (`Cream`/`CardSoft`).
- Trocar legados: `StandardCard`→`TramabookCard`, `MemberAvatar`→`Avatar`, `PillButton`→`TbButton`.
- Manter a lógica: `getCommentsForChapter`, `sendComment`, `toggleReaction`, `getReactionsForChapter`.

- [ ] **Step 2: Compilar** — `./gradlew assembleDebug`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/example/ui/screens/DiscussionScreen.kt
git commit -m "feat(discussion): re-skin da tela de discussão de capítulo"
```

---

## Task 10: Notificações + Sugerir Livro

**Files:**
- Modificar: `app/src/main/java/com/example/ui/screens/NotificationsScreen.kt` (inteiro)
- Modificar: `app/src/main/java/com/example/ui/screens/SuggestScreen.kt` (inteiro)

- [ ] **Step 1: Re-skinar `NotificationsScreen`**

Protótipo: `NotificationsScreen` em `screens-aux.jsx`. Agrupado por HOJE/ONTEM/ESTA SEMANA, cada item com ícone colorido ou `Avatar`, ponto `Terracota` para não lido.
- `StandardCard`→`TramabookCard` onde aplicável; itens não lidos com fundo `Cream`.
- Header "Avisos", botão "Ler todas" texto `Terracota`.
- Manter `notifications`, `markAllNotificationsAsRead`, `markNotificationAsRead`.

- [ ] **Step 2: Re-skinar `SuggestScreen`**

Protótipo: `SuggestBookScreen` em `screens-next.jsx`. Busca, resultados com `Cover`, modal "por que esse livro?".
- Campo de busca arredondado, container `Cream`.
- Resultados: cada um com `Cover` da Fase 1, selecionado com borda `Terracota`.
- Modal de justificativa: `TbButton` Outline/Terra.
- `BookCover`→`Cover`, `StandardCard`→`TramabookCard`.
- Manter `searchResults`, `searchLoading`, `searchOpenLibrary`, `createBookSuggestion`.

- [ ] **Step 3: Compilar** — `./gradlew assembleDebug`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/example/ui/screens/NotificationsScreen.kt app/src/main/java/com/example/ui/screens/SuggestScreen.kt
git commit -m "feat(screens): re-skin Notificações e Sugerir Livro"
```

---

## Task 11: Limpeza — remover legado e aliases

**Files:**
- Modificar: `app/src/main/java/com/example/ui/components/CommonComponents.kt`
- Modificar: `app/src/main/java/com/example/ui/theme/Color.kt`, `app/src/main/java/com/example/ui/theme/Type.kt`
- Modificar: `app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt`

- [ ] **Step 1: Verificar uso dos legados**

Rodar `grep -rn "PillButton\|BookCover\|MemberAvatar\|StandardCard\|SectionHeader" app/src/main/java/com/example/ui/screens/` e confirmar que NENHUMA tela usa mais os componentes legados. Se alguma ainda usar, NÃO remover — reportar como DONE_WITH_CONCERNS listando o que falta.

- [ ] **Step 2: Remover o legado**

Se nada mais usa: deletar de `CommonComponents.kt` os composables não usados (`PillButton`, `BookCover`, `MemberAvatar`, `SectionHeader`, `StandardCard`). Se o arquivo ficar vazio, deletá-lo.

- [ ] **Step 3: Remover aliases de compatibilidade**

- Em `Color.kt`: remover `val VerdeMusgo = Oliva` se `grep -rn "VerdeMusgo" app/src/main/` não achar mais nada.
- Em `Type.kt`: remover `val FrauncesFontFamily = LiterataFontFamily` se `grep -rn "FrauncesFontFamily" app/src/main/` não achar mais nada.
- Se algum ainda for usado, mantê-lo e reportar.

- [ ] **Step 4: Limpar dark mode do ViewModel**

Em `MainViewModel.kt`: remover `themeMode`, `updateThemeMode` e o uso de `dataStoreManager.themeModeFlow`/`saveThemeMode` SE nenhuma tela os chama mais (a Task 7 removeu o seletor). Verificar com grep. Em `DataStoreManager.kt`, remover `THEME_MODE_KEY`/`themeModeFlow`/`saveThemeMode` se órfãos. Se houver dúvida, manter e reportar.

- [ ] **Step 5: Compilar e testar** — `./gradlew assembleDebug && ./gradlew testDebugUnitTest`. Expected: ambos BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**
```bash
git add -A
git commit -m "refactor: remove componentes legados e aliases de compatibilidade"
```

---

## Task 12: Verificação final da Fase 2

- [ ] **Step 1: Build limpo** — `./gradlew clean assembleDebug`. Expected: BUILD SUCCESSFUL, APK gerado.

- [ ] **Step 2: Todos os testes** — `./gradlew testDebugUnitTest`. Expected: todos PASS.

- [ ] **Step 3: Estado git** — `git log --oneline -15 && git status --short`. Working tree limpa.

A Fase 2 está completa quando: o app compila, gera APK, os testes passam, todas as telas usam o design system da Fase 1, e o legado foi removido.

---

## Notas

- Cada tarefa re-skina uma área e deixa o app navegável. Se uma tarefa quebrar a navegação, é regressão — corrigir antes de commitar.
- O `MainTabsScreen.kt` tem ~2000 linhas e várias tarefas o tocam (4, 5, 6, 7). Executar essas tarefas em ordem, uma de cada vez (são o mesmo arquivo — não paralelizar).
- Fidelidade ao protótipo é o alvo, mas idiomática Android. Onde o protótipo usa um truque web, adaptar para o equivalente Compose.
