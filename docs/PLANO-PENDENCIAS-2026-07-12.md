# Plano de resolução das pendências — Rodapé

**Data:** 2026-07-12 · **Base:** build 1.1.2 (`f0ba9f1`) · **Origem:** achados do
estudo de 15 personas (`docs/PERSONAS-2026-07-12.md`) + resíduos dos 4 mapeamentos
de tela.

Este documento faz três coisas:
1. **Reconcilia** o que a 1.1.2 já resolveu (checklist guiado + busca no "Trocar livro").
2. Lista, num quadro só, **cada problema → a melhor solução profissional**.
3. Dá um **plano de implementação por item** (solução, arquivos, esforço, critério de
   pronto) e um **roadmap em ondas**.

**Escala de esforço:** `P` pequeno (≤ meio dia) · `M` médio (1–2 dias) · `G` grande
(3+ dias ou exige decisão de produto).
**Prioridade:** `P0` destrava perfil crítico / acessibilidade · `P1` fricção séria ·
`P2` polimento.

---

## 0. Reconciliação — o que a 1.1.2 já entregou

| Item | Status | Avaliação |
|------|--------|-----------|
| **Checklist guiado de clube novo** (`09c85ad`) | ✅ Entregue, **refinar** | Stepper "Primeiros passos" (3 passos: livro → capítulos → encontro), some quando o clube está rodando, passo 1 leva **direto pra sub-aba de votação** (isso já ataca a "votação escondida" no setup). **Gaps:** (a) mostra os **mesmos 3 passos pro membro comum**, mas cadastrar capítulos/agendar encontro são **admin-only** → o membro vê tarefa que não pode fazer; (b) não guia a ação de **encerrar a votação** (o passo "livro" só vira ✓ quando existe livro atual, mas o que falta no meio é fechar a rodada); (c) só cobre o **setup**, não o resto do ciclo (ler → concluir → próximo). Ver **A1**. |
| **Buscar/adicionar livro no "Trocar livro"** (`09c85ad`) | ✅ Entregue | Campo de busca (debounce 400ms) no diálogo, cria e define a leitura atual (`setSearchedBookAsCurrent`). Fecha o beco "sugira primeiro na aba Votação". **Considero resolvido**; resta só unificar a sinalização de "qual caminho usar" (ver **A3**). |

**Conclusão:** dos 2 itens do antigo `PROXIMOS-PASSOS.md`, o segundo está pronto e o
primeiro está **80% lá** — falta adaptar por papel e cobrir o "encerrar votação".

---

## 1. Lista consolidada — problema → melhor solução profissional

> Ordenada por prioridade. IDs (A1, B2…) referenciam o plano detalhado na seção 2.

| ID | Problema (persona) | Melhor solução profissional | Esf. | Prio |
|----|--------------------|-----------------------------|:----:|:----:|
| **A1** | Membro vê passos de admin no checklist; falta "encerrar votação" | Checklist **por papel** + passo/sub-estado "Encerrar a votação" | M | P0 |
| **A2** | "Votação" escondida na aba **"Encontros"** | Renomear aba p/ **"Clube"** ou **"Encontros e votação"** + badge de rodada aberta | M | P0 |
| **C1** | Ações sociais **não são faladas** (leitor de tela) | `liveRegion`/`announceForAccessibility` central em toda mudança de estado + snackbars | M | P0 |
| **F1** | **Sem push** → membro perde encontro presencial | FCM (token table + Edge Function) plugado no cron de lembrete que **já existe** | G | P0 |
| **B1** | Convidado só cola **código depois de criar conta** | 3º caminho **"Tenho um convite"** na Welcome, aceitando código antes/junto do cadastro | M | P1 |
| **D1** | Progresso só **+1 por toque** | "Li até o capítulo ___" (picker) além do +1 | P | P1 |
| **B2** | Reset de senha **sem checklist** | Reaproveitar o `PasswordChecklist` do cadastro na tela de reset | P | P1 |
| **C2** | Alvo de **reação = bolha inteira** (abre emoji sem querer) | Alvo dedicado: **long-press** p/ reagir + toque simples só seleciona/expande | M | P1 |
| **G1** | Pill "aguardando conexão" **parece travada** | Estado final explícito **"Tudo salvo ✓"** que aparece e some | P | P1 |
| **C3** | Fonte A++ **trunca** rótulos (abas, cards) | `ScrollableTabRow` / remover `maxLines=1` fixo + testes de fonte 1.3× | M | P1 |
| **B3** | Salvar perfil **falha em silêncio** offline | Enfileirar a escrita do perfil (não `runCatching` mudo) + confirmação | M | P1 |
| **B4** | Criar-clube/Entrar **sem timeout** | Timeout + retry visível no callback dessas ações | P | P1 |
| **C4** | CTAs da Welcome **sem `Role.Button`** | `Role.Button` + `minimumInteractiveComponentSize()` nos 2 CTAs | P | P1 |
| **C5** | Alvos < 48dp (send 44dp, emoji, "Sair do clube") | Padronizar `minimumInteractiveComponentSize()` nesses pontos | P | P1 |
| **I1** | Jargão: **"ata", "spoiler", "moderação"** | Plain-language + microdica de 1 linha | P | P1 |
| **E1** | **Sem responder/citar** comentário | Threads leves (reply-to) na discussão | G | P2 |
| **E2** | Só **5 emojis** de reação | Ampliar paleta / emoji livre | P | P2 |
| **H1** | Busca **some livro sem capa** (Open Library) | Remover o filtro `coverI != null` do serviço | P | P2 |
| **J1** | **Sem dark mode** | Paleta dark de tokens + toggle (exige decisão de produto) | G | P2 |
| **B5** | Login aceita **6**, cadastro exige **8 forte** | Unificar a regra (subir login p/ 8-forte ou explicar) | P | P2 |
| **G2** | Exclusão de conta **a 1 toque** | Confirmação por texto ("digite EXCLUIR") + export opcional | P | P2 |
| **C6** | Avatares **sem opção neutra**; sem pronome | 1–2 avatares neutros/abstratos + campo de pronome opcional | M | P2 |
| **A4** | Nome de **1 caractere** trava salvar avatar | Desacoplar salvar-avatar da validação de nome | P | P2 |
| **I2** | Fallbacks de nome divergentes ("Leitor(a)" vs "Você") | Um único helper de nome-de-exibição | P | P2 |
| **C7** | TalkBack lê aba **2×** ("Início, Início") | Merge/clear de semântica no item da bottom bar | P | P2 |
| **F2** | Deep-link de notificação por **string slicing frágil** | Extrair `chapterId` do JSON já parseado | P | P2 |
| **B6** | `JoinClubScreen` tem **branch morto** (colar link) | Remover código morto ou religar a feature | P | P2 |

---

## 2. Plano detalhado por item

### Tema A — Onboarding do ciclo do clube (organizadora)

#### A1 · Checklist por papel + "encerrar votação" — `M`, `P0`
**Problema.** O checklist mostra os 3 passos (livro/capítulos/encontro) igual pra
todos, mas capítulos e encontro são admin-only; e não sinaliza a ação de **fechar a
rodada** (o gargalo real entre "votaram" e "tem livro atual").
**Solução profissional.** Checklist **derivado do papel + do sub-estado da rodada**:
- **Admin:** `Abrir votação → (rodada aberta?) Encerrar votação → Cadastrar capítulos
  → Agendar encontro`. O passo 1 vira dois sub-estados: "Abrir votação" e, quando
  `activeVotingRound != null`, "Encerrar a votação (N votos)".
- **Membro:** `Sugerir um livro → Votar → Ler e marcar progresso`. Nada de capítulos/
  encontro (fora do alcance dele).
**Implementação.** `MainTabsScreen.kt:813 ClubSetupChecklist` — passar
`activeVotingRound`, `hasSuggestions`, `hasUserVoted` (já existem no `MainViewModel`)
e ramificar `steps` por `isAdmin`. Passo "Encerrar votação" navega
`next/votacao` e realça o botão de encerrar.
**Critério de pronto.** Admin e membro veem listas de passos **diferentes e
executáveis por eles**; com rodada aberta, o próximo passo do admin é "Encerrar".

#### A2 · Aba de votação descoberta — `M`, `P0`
**Problema.** Vota-se dentro da aba rotulada **"Encontros"** — o nome não sugere isso.
**Solução profissional.** Renomear a aba inferior de "Encontros" para **"Clube"**
(hub que contém Encontro + Votação) **ou** "Encontros e votação"; e adicionar um
**badge/ponto** no ícone quando há **rodada de votação aberta** ("tem coisa pra você
votar"). Manter os deep-links atuais.
**Implementação.** `MainTabsScreen.kt:709-714` (label da aba `next`), badge em
`BottomBarItem` (`:732`) alimentado por `activeVotingRound != null`. Os sub-tabs
internos ("Encontro"/"Votação") continuam.
**Critério de pronto.** Usuário que quer votar acha a votação **sem depender de
deep-link**; rodada aberta acende um indicador na aba.

#### A3 · Caminho único de "definir leitura atual" — `P`, `P2`
**Problema.** Dois caminhos divergentes (encerrar votação × trocar manual) sem dizer
qual usar. A busca no "Trocar livro" (1.1.2) já mitigou.
**Solução.** Rotular claramente: no diálogo, separar **"Definir leitura atual"**
(busca/manual) de **"Escolher pela votação"** com uma linha de ajuda. Sem nova infra.
**Arquivos.** `ManageClubScreen.kt` (~760).
**Critério.** O admin entende, no ponto da decisão, qual caminho usar.

---

### Tema B — Entrada, auth e resiliência

#### B1 · "Tenho um convite" na Welcome — `M`, `P1`
**Problema.** O convidado (caso mais comum de entrada num clube privado) precisa
**criar conta** antes de poder colar o código.
**Solução profissional.** Terceiro CTA na Welcome — **"Tenho um convite"** — que abre
a entrada de código **antes** do cadastro. Fluxo: valida o código → mostra "Você foi
convidado pro clube X" → aí sim pede login/cadastro (ou Google) → entra já no clube.
Guarda o código em estado até concluir a auth (deep-link `rodape://join?code=` como
bônus).
**Implementação.** `WelcomeScreen.kt:190-241` (add CTA), reusar `JoinClubScreen`
(`:896`) desacoplando a checagem de sessão; `MainActivity.kt:295` (rota acessível
pré-auth); `joinClubWithCode` chamado pós-auth com o código retido.
**Critério de pronto.** Fluxo "recebi código → instalo → colo código → crio/entro →
já estou no clube" sem caçar menus.

#### B2 · Checklist de senha no reset — `P`, `P1`
**Problema.** A tela de reset só tem a linha de apoio + botão cinza; o cadastro tem
checklist ao vivo.
**Solução.** Reaproveitar **o mesmo componente** de checklist do cadastro.
**Implementação.** Extrair o checklist de `SignUpScreen.kt:302-311` para um
`PasswordChecklist` compartilhado e usá-lo em `ResetPasswordScreen.kt:106`.
**Critério.** Reset mostra ✓/○ por regra; o usuário sabe o que falta.

#### B3 · Salvar perfil não falha em silêncio — `M`, `P1`
**Problema.** `completeOnboarding` faz `runCatching{ insertUser }` sem tratar → falha
remota some; nome/avatar podem não persistir.
**Solução profissional.** Escrita de perfil **local-first + fila offline** (como o
resto do app): grava local, enfileira a mutação remota, e o drenador confirma. Se
falhar de vez, marcar "perfil não sincronizado" e reintentar.
**Implementação.** `MainViewModel.kt:114 completeOnboarding` — enfileirar via o mesmo
mecanismo de `pending_mutations`; adicionar handler de replay de perfil (já existe
handler de perfil na fila, reusar).
**Critério.** Desligar a rede no onboarding e reconectar → nome/avatar aparecem no
servidor sem ação do usuário.

#### B4 · Timeout em criar-clube/entrar — `P`, `P1`
**Problema.** "Criando…"/"Entrando…" dependem de um callback que pode nunca vir → trava.
**Solução.** Timeout (ex.: 15s) no `viewModelScope` dessas ações → em estouro, limpar
o estado e mostrar **"Demorou demais. Verifique a conexão e tente de novo."** com retry.
**Implementação.** `MainViewModel.createClub`/`joinClubWithCode` (`:487`,`:515`) —
`withTimeoutOrNull`; UI em `WelcomeScreen.kt:869-887`/`1107-1149`.
**Critério.** Em sinal morto, o botão **volta** com mensagem, sem force-close.

#### B5 · Regra de senha consistente — `P`, `P2`
**Problema.** Login aceita 6; cadastro/reset exigem 8-forte.
**Solução.** Alinhar: **login também valida 8-forte** (ou, no mínimo, a mesma
mensagem). Sem regra dupla.
**Arquivos.** `WelcomeScreen.kt:292`.
**Critério.** Mesma política em login/cadastro/reset.

#### B6 · Remover branch morto do JoinClub — `P`, `P2`
**Problema.** `activeTabIsCode` init `true` e nunca muda → a UI de "colar link" é
inalcançável.
**Solução.** Remover o código morto (ou religar a feature de link, se desejada).
**Arquivos.** `WelcomeScreen.kt:900, 1062-1103`.
**Critério.** Sem código inalcançável; ou a colagem de link funciona.

---

### Tema C — Acessibilidade (a maior dívida, e barata em grande parte)

#### C1 · Live regions nas ações — `M`, `P0`
**Problema.** Votar, comentar, RSVP, "Código copiado" não são anunciados a leitor de
tela → quem depende dele "age às cegas".
**Solução profissional.** Um **anunciador central**: helper que emite
`announceForAccessibility` (ou `semantics { liveRegion = Polite }`) em toda mudança de
estado relevante e em **todo snackbar**. Padronizar: snackbars viram live regions por
padrão.
**Implementação.** Wrapper no host de snackbar (usado no app todo) + `liveRegion` nos
selos otimistas de voto/progresso/RSVP em `NextTabScreen.kt`, `DiscussionScreen.kt`,
`MeetingDetailScreen.kt`.
**Critério.** Com TalkBack, cada ação social produz **fala de confirmação**.

#### C2 · Alvo de reação dedicado — `M`, `P1`
**Problema.** Tocar a bolha inteira abre o seletor de emoji (reler = abrir sem querer).
**Solução profissional.** Padrão consagrado: **long-press** na bolha abre reações;
toque simples não reage (no máximo expande). O ícone de reagir vira um **alvo real**
(não decorativo) pra quem prefere toque.
**Implementação.** `DiscussionScreen.kt:402` (trocar `clickable` por
`combinedClickable` com `onLongClick`), tornar o `AddReaction` (`:427`) clicável com
`contentDescription`.
**Critério.** Ler um comentário nunca abre o emoji; reagir é long-press ou ícone.

#### C3 · Sem truncar na fonte grande — `M`, `P1`
**Problema.** A++ (1.3×) + escala do sistema trunca "Avaliações"→"Avaliaçõ…" e cards
da Estante.
**Solução profissional.** Abas do detalhe do livro em **`ScrollableTabRow`** (ou
ícone+texto que quebra) em vez de 5 slots `weight(1f)` com `maxLines=1`; nos cards da
Estante, permitir 2 linhas / `minimumFontScale`. Teste com fonte 1.3×.
**Implementação.** `BookDetailScreen.kt:195-250`, `ShelfTabScreen.kt:215-233`.
**Critério.** Em A++ + fonte do sistema grande, nenhum rótulo essencial corta.

#### C4 · `Role.Button` nos CTAs da Welcome — `P`, `P1`
**Solução.** Adicionar `semantics { role = Role.Button }` +
`minimumInteractiveComponentSize()` nos dois `Box.clickable`.
**Arquivos.** `WelcomeScreen.kt:190-241`.
**Critério.** TalkBack anuncia "Criar conta, botão".

#### C5 · Alvos ≥ 48dp — `P`, `P1`
**Solução.** `minimumInteractiveComponentSize()` no botão enviar (44dp,
`DiscussionScreen.kt:713`), nos alvos do seletor de emoji (`:768`) e no link "Sair do
clube" (`MainTabsScreen.kt:2635`).
**Critério.** Nenhum alvo interativo < 48dp.

#### C6 · Avatares neutros + pronome — `M`, `P2`
**Solução profissional.** Adicionar **1–2 avatares neutros/abstratos** (símbolo,
inicial, ilustração sem gênero) ao conjunto e um **campo de pronome opcional** no
perfil (ele/ela/elu/—). Nada obrigatório.
**Implementação.** `Avatar.kt:65-108` (novos presets), `MainTabsScreen.kt` EditProfile
(campo pronome), coluna opcional no perfil.
**Critério.** Existe avatar sem gênero; pronome é escolha, não imposição.

#### C7 · TalkBack não lê a aba 2× — `P`, `P2`
**Solução.** No item da bottom bar, `clearAndSetSemantics` no filho ou
`contentDescription = null` no ícone quando há `Text(label)` irmão.
**Arquivos.** `MainTabsScreen.kt:751-782`.
**Critério.** TalkBack lê "Início, aba" uma vez só.

---

### Tema D — Leitura e progresso

#### D1 · "Li até o capítulo X" — `P`, `P1`
**Problema.** Só dá pra avançar +1 por toque; binge/atrasado sofre.
**Solução profissional.** Manter o botão **"Marcar progresso" (+1)** como caminho
rápido e adicionar **"Li até o capítulo ___"** — um picker (lista de capítulos ou
number picker) que chama `updateBookProgress` com índice arbitrário. Avançar o próprio
progresso não tem risco de spoiler (é a leitura da pessoa).
**Implementação.** `MainTabsScreen.kt:1662-1707` (add link/botão que abre o picker),
`MainViewModel.updateBookProgress` (`:545`) já aceita índice.
**Critério.** Dá pra pular pro capítulo 12 num gesto; o +1 continua existindo.

---

### Tema E — Discussão social

#### E1 · Responder/citar comentário — `G`, `P2`
**Solução profissional.** Threads **leves** (1 nível): `parentCommentId` no comentário,
"Responder" na bolha, render aninhado raso. Notificação "Fulano respondeu você".
**Implementação.** Coluna `parent_comment_id` (migração), UI em `DiscussionScreen.kt`,
trigger de notificação. **Precisa de migração de banco.**
**Critério.** Dá pra responder um comentário e o autor é avisado.

#### E2 · Paleta de reações maior — `P`, `P2`
**Solução.** Ampliar o set (`❤🤯💀🍷👍` → +alguns) ou permitir emoji livre via teclado.
**Arquivos.** `DiscussionScreen.kt:754`.
**Critério.** Mais opções de reação sem inflar a UI.

---

### Tema F — Notificações e push

#### F1 · Push notifications (FCM) — `G`, `P0`
**Problema.** O app só avisa **por dentro**; num clube **presencial**, isso faz perder
encontro. **O gatilho já existe** — o cron `rodape-meeting-reminders` (de hora em hora)
cria a notificação de "encontro em 24h". Falta **canal de entrega**.
**Solução profissional (arquitetura completa):**
1. **Cliente:** SDK do Firebase Messaging, registro de **device token** por usuário.
2. **Banco:** tabela `device_tokens (user_id, token, platform, updated_at)` com RLS.
3. **Entrega:** **Supabase Edge Function** `send-push` que, ao inserir em
   `notifications` (ou no cron), busca os tokens do destinatário e chama a **FCM HTTP
   v1 API**. Trigger `AFTER INSERT ON notifications` → chama a function.
4. **Permissão:** pedido de permissão de notificação (Android 13+) no momento certo
   (após o 1º encontro agendado, não no onboarding).
**Implementação.** `app/build.gradle.kts` (firebase-messaging), `AndroidManifest`
(service), nova migração (`device_tokens`), Edge Function em `supabase/functions/`,
trigger. **Precisa de conta Firebase + segredo FCM no Supabase.**
**Critério.** Encontro marcado → 24h antes o celular **apita** mesmo com o app fechado.
**Nota:** é o item de maior esforço; ver "decisões de produto".

#### F2 · Deep-link de notificação robusto — `P`, `P2`
**Problema.** `chapterId` é extraído por `substringAfter("\"chapterId\":\"")` — frágil.
**Solução.** Usar o `JsonObject` **já parseado** (o texto já é parseado com segurança).
**Arquivos.** `NotificationsScreen.kt:358-366`.
**Critério.** Deep-link continua certo mesmo com o JSON reordenado/espaçado.

---

### Tema G — Confiança e feedback

#### G1 · Estado final da pill de sync — `P`, `P1`
**Problema.** "X alterações aguardando conexão" com rodinha **parece travado** pra
leigo; nunca há "acabou".
**Solução profissional.** Máquina de 3 estados: **sincronizando** (rodinha) →
**"Tudo salvo ✓"** (2s, verde) → **some**. Offline persistente: "Sem conexão — salvo
no aparelho".
**Implementação.** `MainTabsScreen.kt:274-299` — derivar do tamanho da fila +
transição pós-drain.
**Critério.** Depois de sincronizar, o usuário vê "Tudo salvo ✓" e a pill some.

#### G2 · Atrito na exclusão de conta — `P`, `P2`
**Problema.** Ação **irreversível a 1 toque** dentro do diálogo.
**Solução profissional.** Confirmação **por digitação** ("digite EXCLUIR para
confirmar") habilitando o botão; opcional: **"Baixar meus dados"** antes. Mantém o
"Pedir por email".
**Arquivos.** `MainTabsScreen.kt:2710-2748`.
**Critério.** Não dá pra excluir sem uma ação deliberada de digitação.

---

### Tema H — Busca e dados

#### H1 · Não sumir livro sem capa — `P`, `P2`
**Problema.** `BookSearchService.kt:45` filtra `it.coverI != null` e derruba resultados
do Open Library sem capa — inconsistente com o resto (capa não é obrigatória).
**Solução.** Remover o filtro; usar **placeholder de capa** (já existe no app) quando
faltar.
**Critério.** Livro sem capa aparece na busca com capa-placeholder.

---

### Tema I — Copy / linguagem simples

#### I1 · Plain language do jargão — `P`, `P1`
**Problema.** "ata", "spoiler", "moderação" travam letramento textual baixo.
**Solução profissional.** Trocar/explicar: **"ata"** → "resumo do encontro";
**"spoiler"** → manter mas com dica "(não conta o final)"; **"moderação"** →
"esconder comentário". Uma microdica de 1 linha onde o termo aparece primeiro.
**Arquivos.** `MeetingDetailScreen.kt` (ata), `DiscussionScreen.kt` (spoiler/moderação).
**Critério.** Nenhum termo técnico aparece sem tradução em linguagem do dia a dia.

#### I2 · Um helper de nome-de-exibição — `P`, `P2`
**Problema.** Fallbacks divergentes: "Leitor(a)" (saudação) vs "Você" (header).
**Solução.** Um único `displayName(user)` com fallback consistente, usado em todo lugar.
**Arquivos.** `MainTabsScreen.kt:199, 1372`.
**Critério.** Usuário anônimo é chamado do mesmo jeito em toda a tela.

---

### Tema J — Design

#### J1 · Dark mode — `G`, `P2` (exige decisão de produto)
**Problema.** Sem modo escuro — dói em leitor noturno (Marina) e no olho de designer
(Fernanda). Hoje é **decisão de design** consciente.
**Solução profissional.** Definir uma **paleta dark de tokens** (o app tem design
system próprio em `ui/theme/` — `Color.kt`, `Tokens.kt` — então dá pra fazer certo, não
"inverter cores"): derivar do `claude-design/` uma versão escura, mapear todos os
tokens semânticos (superfícies, Ink→claro, terracota/oliva ajustados p/ contraste
AA), e um **toggle** (Sistema/Claro/Escuro) em APARÊNCIA, persistido no DataStore.
**Implementação.** `ui/theme/Color.kt` + `Theme.kt` (esquema dark), `DataStoreManager`
(preferência), seletor no Perfil. Auditar contraste.
**Critério.** App inteiro legível no escuro, contraste AA, sem "buraco branco".
**Nota:** confirmar com você se quer reverter a decisão de "sem dark mode".

---

## 3. Roadmap sugerido (ondas)

**Onda 1 — "Destrava quem importa" (P0, ~1 sprint).**
`A1` checklist por papel · `A2` aba de votação descoberta · `C1` live regions ·
`D1` "li até o capítulo X" · `G1` pill "Tudo salvo ✓". *(Tudo M/P; impacto altíssimo,
sem dependência externa.)*

**Onda 2 — "Acessibilidade e entrada" (P1, ~1 sprint).**
`C3` truncamento · `C4` Role.Button · `C5` alvos 48dp · `B1` código na Welcome ·
`B2` checklist no reset · `B3` perfil na fila · `B4` timeout · `C2` reação por
long-press · `I1` plain language.

**Onda 3 — "Push" (P0 de valor, esforço G, com dependência externa).**
`F1` FCM ponta-a-ponta (Firebase + Edge Function + trigger). Tratada à parte por exigir
conta Firebase e segredo no Supabase.

**Onda 4 — "Polimento e produto" (P2).**
`E1` replies · `E2` emojis · `H1` busca sem capa · `B5` senha consistente ·
`G2` atrito na exclusão · `C6` avatar neutro + pronome · `A4`/`I2`/`C7`/`F2`/`B6`
(quick wins) · `J1` dark mode *(se aprovado)*.

---

## 4. Itens que exigem decisão de produto (antes de codar)

1. **`F1` Push:** topo? Exige criar projeto Firebase + configurar segredo FCM no
   Supabase. É a coisa que mais muda a percepção num app **presencial**.
2. **`J1` Dark mode:** reverter a decisão de "sem dark mode"? É pedido por 2 perfis
   opostos (adolescente e designer).
3. **`A2` Nome da aba:** "Clube" vs "Encontros e votação" — escolha de nomenclatura.
4. **`G2` Exclusão por digitação:** quão rígido você quer o atrito numa ação
   irreversível (Play Store aceita ambos).
5. **`E1` Replies:** vale a migração de banco agora ou fica pra um marco de "discussão
   2.0"?

> **Resumo:** a 1.1.2 já matou o pior gargalo (checklist) e o beco do "trocar livro".
> O que falta se divide em **3 baratos-e-altíssimo-impacto** (checklist por papel, aba
> de votação, live regions), **1 caro-e-decisivo** (push), e um monte de **quick wins**
> de acessibilidade/copy. Nenhum é regressão — são refinamentos e features novas.
