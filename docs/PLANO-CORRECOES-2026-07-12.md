# Plano profissional de correção, melhorias e sugestões — Rodapé

**Data:** 2026-07-12 · **Base:** código atual (`master` @ 8528460, v1.0.4) · **Método:**
6 auditorias paralelas do código real (performance, telas vazias/UX, bugs de runtime,
camada de dados/sync, auth/navegação) + verificação direta dos achados P0 no código e no
schema Supabase versionado (`supabase/schema/`).

> Este plano **não** reproduz os docs antigos. A maioria dos fixes de `AUDITORIA-2026-07.md`
> e `REVISAO-2026-07-12-impecavel.md` foi verificada como **realmente presente e correta**
> (ver §6). O que segue é o que **ainda está aberto** no código de hoje, cruzado contra o que
> os docs alegam. Build compila e `testDebugUnitTest` passa — os problemas são de
> **runtime, estado, sync e UX**, não de compilação.

---

## 1. Diagnóstico honesto (mapeando as 3 dores relatadas)

| Sua reclamação | Veredito da auditoria |
|---|---|
| **"Muito bug"** | Real. 2 bugs de **perda/corrupção de dados** (P0), 1 bug que quebra a **2ª votação**, edição de capítulos que remaneja comentários, RPCs de admin com "sucesso falso", back stack quebrado ao entrar em clube. |
| **"Muita lentidão"** | Real, mas **não há um único congelamento** — é um **cluster** de causas somadas: refetch de rede em loop na discussão, listas sem `key`, N+1 de rede para contadores, canais Realtime vazando e duplo-refetch por ação. A maioria dos ganhos é de esforço **FÁCIL**. |
| **"Telas vazias, sem ação, sem nada"** | Real, porém **localizado** — não é "a maioria das telas". É o **primeiro uso**: num clube recém-criado (sem livro/sem encontro), **3 das 5 abas inferiores dão beco** sem botão e sem explicação. Fora isso, não há nenhum `onClick` morto e nenhum mock exposto (isso já foi limpo). O problema é **falta de CTA contextual por papel** (admin × membro), não telas em branco por toda parte. |

**A manchete:** o app está mais próximo de "bom" do que a sensação sugere — mas tem **1 bug
P0 que quebra o recurso central (discussão por capítulo) entre dispositivos** e que passa
despercebido em teste de aparelho único. Corrigir os P0 + o cluster de lentidão FÁCIL +
os CTAs de primeiro uso muda completamente a percepção.

---

## 2. 🔴 P0 — Perda / corrupção de dados (fazer PRIMEIRO)

### P0-1 · Capítulos não sincronizam e comentários são perdidos entre dispositivos
- **Onde:** `RemoteRepository.kt:1803` (`Chapter(id = "ch_${bookId}_$numero")`), `:221`
  (`ChapterDto(id, …)` envia o id), `:1812` (`upsert`), `insertComment` `:1898-1907`
  (`chapter_id = "ch_<uuid>_N"`). Schema: `supabase/schema/tables.sql:67`
  (`chapters.id uuid`), `:123` (`comments.chapter_id uuid`).
- **O que acontece:** o cliente monta ids de capítulo **sintéticos de texto**
  (`ch_<uuid>_1`) e os envia para uma coluna **`uuid`**. O Postgres rejeita
  (`22P02 invalid input syntax for type uuid`, HTTP 400). O `runCatching` em `:1811-1822`
  **engole o erro e só loga** → capítulos **nunca chegam ao servidor**. Comentários usam
  o mesmo `chapter_id` de texto → insert rejeitado → na fila offline `isPermanentError`
  casa `400` → **dead-letter imediato** → comentário **perdido**.
- **Por que passou batido:** num aparelho só, o **Room é a fonte de leitura** — capítulos e
  comentários locais aparecem normalmente. Só **entre dispositivos** (o ponto de um clube de
  leitura!) é que nada propaga. O teste "no device real" (commit 1.0.4) não pega isso.
- **Relação com os docs:** `REVISAO-…-impecavel.md` (P0 #3) apresenta o `saveChapters` DIFF
  como *correção* P0. Ele resolveu o CASCADE local, mas o esquema de id que introduziu
  **quebrou o sync remoto**. É uma **regressão de dados mascarada**.
- **Fix (MÉDIO):** identidade de capítulo compatível com `uuid` — ou (a) uuid **determinístico
  v5** a partir de `(bookId, numero)`, ou (b) deixar o servidor gerar o id e usar
  `onConflict = "book_id,numero"` no upsert (existe `UNIQUE(book_id, numero)` no schema),
  mapeando `numero → id` para os comentários. Alinhar `comments.chapter_id`.
- ⚠️ **Toca schema/sync → validar em 2 dispositivos** antes de dar por fechado.

### P0-2 · Criação offline é podada no próximo sync (perda silenciosa)
- **Onde:** `insertBook` `:1586-1589`, `insertClubBook` `:1619-1622`, `insertBookSuggestion`
  `:2253-2268`, `insertMeeting` `:2324-2362` — todos **local-first SEM enfileirar**. Poda:
  `syncClubBooks` → `replaceClubBooksInClub` (DAO), idem sugestões/encontros.
- **O que acontece:** criar livro/sugestão/encontro **offline** (ou quando o remoto responde
  429/5xx) grava no Room mas **não cria `PendingMutation`**. No próximo trigger de sync
  (TTL expira, evento Realtime de outro membro, pull-to-refresh) o `replace…Except`
  **deleta a linha local-only** → o item **some de vez**, sem aviso.
- **Relação com os docs:** `AUDITORIA` (Críticos #2) mandou converter escritas para
  local-first+fila. Progresso/rating foram convertidos; **estas 4 criações viraram
  local-first mas sem fila** — com a poda-no-sync, ainda perdem.
- **Fix (MÉDIO):** rotear por `tryRemoteOrEnqueue` com handlers dedicados **ou** não prunar
  linhas que tenham `PendingMutation` associada.

---

## 3. 🟠 P1 — Bugs de comportamento visível

| ID | Bug | Onde | Esforço |
|---|---|---|---|
| **B1** | **Votação usa votos do CLUBE INTEIRO, não da rodada ativa.** Na **2ª votação**, todos os botões travam em "Limite de votos atingido" (mesmo com 0 votos na rodada nova); % ficam diluídos; tocar num card marcado "Teu voto" (voto de rodada antiga) **cria voto novo** em vez de desfazer. Modelo de voto ainda diverge cliente↔servidor (PK local `(round,book,user)` × PK servidor `(round,user)`; `nLivros` = votos-por-usuário no app × nº de vencedores no servidor). | `NextTabScreen.kt:703,711,777-781`; `getVotesForClubFlow` `RemoteRepository.kt:2123`; `voteForBook` `MainViewModel.kt:598-619`; `tables.sql:291` | MÉDIO |
| **B2** | **Editar/reordenar capítulos remaneja os comentários pro capítulo errado.** `mapIndexed` renumera por posição; comentários ficam presos ao número antigo. Apagar/subir/descer um capítulo faz a discussão "pular" de capítulo. (Some ao P0-1: resolver a identidade de capítulo resolve os dois.) | `ManageChaptersScreen.kt:222`; `saveChapters` `RemoteRepository.kt:1801-1808` | MÉDIO |
| **B3** | **`join_club` empilha `main_tabs` duplicado** — `popUpTo("welcome")` é no-op (welcome já foi removida no login), sem `launchSingleTop`. Voltar após entrar num clube reabre a tela de código. O `create_club` já corrige isso e **comenta o bug** — o `join_club` ficou pra trás. | `MainActivity.kt:248-249` | FÁCIL |
| **B4** | **RPCs de admin engolem exceção → "sucesso falso".** Promover/rebaixar/transferir super-admin/fechar votação com falha (rede/RLS/invariante) reporta sucesso e não muda nada; `closeVotingRound` ainda chama 3 `notifyLocalMutation` como se tivesse promovido o vencedor. | `RemoteRepository.kt:1371-1411`, `:2239-2247` | FÁCIL |
| **B5** | **Updates/deletes offline são no-op silencioso.** Arquivar clube, mudar status de livro, concluir encontro, salvar resumo/ata, deletar sugestão/frase: offline não escrevem local **nem** enfileiram — some sem erro. | `RemoteRepository.kt:1693-1755, 2432-2447, 1533-1559, 2794-2807, 2611-2651` | MÉDIO-GRANDE |

---

## 4. 🟠 P1 — Lentidão (o cluster que explica o "engasga")

> Sequência recomendada = maior ganho de "parece mais rápido" por hora de trabalho.

| ID | Causa | Onde | Esforço |
|---|---|---|---|
| **L1** | **Refetch de rede em LOOP na discussão.** `getCommentsForChapter`/`getReactionsForChapter` chamados **sem `remember`** no corpo do composable → cada recomposição (todo comentário, todo toggle de reação) recria o Flow e dispara 2 GETs; as reações assinam Realtime **sem TTL e sem filtro**. **Maior ofensor isolado.** | `DiscussionScreen.kt:62,66`; `RemoteRepository.kt:2066-2083` | FÁCIL |
| **L2** | **Listas sem `key` estável** (comentários, sugestões de voto) → inserir/remover recompõe todos os itens e perde scroll. | `DiscussionScreen.kt:273`; `NextTabScreen.kt:776` | FÁCIL |
| **L3** | **`collectAsState` DENTRO de `items{}`** (`isCurrentUserAdmin`): N comentários = N coletores; cada emissão recompõe tudo. | `DiscussionScreen.kt:278` | FÁCIL |
| **L4** | **`reactions.filter` / `members.find` por item, sem `remember`** → reagir a 1 comentário reprocessa a lista toda (O(N×R)). | `DiscussionScreen.kt:274,276,363` | MÉDIO |
| **L5** | **N+1 de rede para contadores**: 1 Flow (GET + canal) **por capítulo** (contagem de comentários na aba Livro) e **por livro** (média na Estante). Abas "acordam" devagar, rede em rajada. | `MainTabsScreen.kt:1671-1674`; `ShelfTabScreen.kt:40-45` | MÉDIO |
| **L6** | **Vazamento de canais Realtime + duplo refetch por mutação.** Canais keyed por id nunca são removidos (`close()` cancela corrotina mas **não desregistra o canal** do cliente singleton); cada mutação faz `notifyLocalMutation` **e** o evento Realtime dispara reload = 2 fetches por escrita. Degrada com o tempo. | `RemoteRepository.kt:1083-1113,1067-1071,1118-1122` | MÉDIO |
| **L7** | **`SubcomposeAsyncImage`** (mais caro que `AsyncImage`) em capas/avatares dentro de `LazyRow`/grid → jank ao rolar. | `Cover.kt:87`; `Avatar.kt:162` | MÉDIO |
| **L8** | **Votação: scans O(livros×votos) e O(rsvps×membros) por recomposição**, sem memo; `userVotesInRound` recalculado por item. | `NextTabScreen.kt:777-782,436-438,522-525` | FÁCIL-MÉDIO |
| **L9** | **Rajada de ~15 GETs paralelos no cold start / troca de clube** (getters sem `syncOnce`/TTL uniforme). | `RemoteRepository.kt:1478,1889,2376,2391,2483,2295,2724,2769` | MÉDIO |
| **L10** | **BookDetail: `forEach` dentro de `verticalScroll`** (sem virtualização) + `sortedByDescending`/`associateBy` inline sem `remember`. | `BookDetailScreen.kt:242-247,459,525,647` | FÁCIL (sort) / GRANDE (LazyColumn) |

---

## 5. 🟠 P1 — Telas vazias / falta de ação (mata a queixa do "1º uso")

> Padrão do fix: empty state = **ícone + microcopy explicando o porquê + CTA que varia por papel**.

| ID | Tela | Hoje | CTA que falta | Esforço |
|---|---|---|---|---|
| **U1** | **Aba "Livro" sem livro** (`MainTabsScreen.kt:1348-1358`) | só texto "Nenhum livro atualmente em leitura." | Admin: **"Escolher livro" / "Abrir votação"**; Membro: **"Sugerir um livro" / "Ver votação"** | MÉDIO |
| **U2** | **Aba "Estante" vazia + bug de copy** (`ShelfTabScreen.kt:75-84,47-49`) | sem CTA; filtro "Favoritos" vazio mostra "Nenhum livro lido ainda" (mentira) | separar copy de Favoritos; CTA **"Sugerir uma leitura"** | FÁCIL (copy) / MÉDIO (CTA) |
| **U3** | **"Próximo › Encontro" e card de encontro na Home, sem encontro** (`NextTabScreen.kt:155-171`; `MainTabsScreen.kt:879-893`) | "Nenhum próximo encontro" sem botão — **nem para admin** | Admin: **"Agendar encontro"**; Membro: microcopy de espera | MÉDIO |
| **U4** | **Aba Livro, "sem capítulos"** (`MainTabsScreen.kt:1629-1658`) | manda **o próprio admin** "pedir pro admin" | Admin: **"Cadastrar capítulos"** (navega direto) | MÉDIO |
| **U5** | **Frases (standalone)** (`FrasesScreen.kt:147-154`) | "Você ainda não guardou nenhuma frase." sem saída | microcopy do fluxo + **"Ir para o livro atual"** | MÉDIO |
| **U6** | **ManageChapters sem livro** (`ManageChaptersScreen.kt:94-101`) | beco: "Sem livro atual" | CTA/orientação p/ definir livro atual | FÁCIL |
| **U7** | **Votação aberta sem sugestões** (`NextTabScreen.kt:760-773`) | "Nenhum livro sugerido" — botão existe longe | CTA inline **"Sugerir o primeiro livro"** | FÁCIL |
| **U8** | **Histórico do livro (Chat) vazio** (`BookDetailScreen.kt:511-522`) | sem aviso de read-only no ramo vazio | mostrar "(somente leitura — comente em Livro atual)" | FÁCIL |

---

## 6. 🟡 P2 — Estado / robustez / edge

| ID | Item | Onde | Esforço |
|---|---|---|---|
| R1 | **Rotação perde formulários inteiros** (sem `rememberSaveable`, Activity recriada): AddBookManual (form todo), Suggest (`query`), BookDetail (tab), NextTab (subTab), Discussion (texto), MeetingDetail (rascunhos). | `AddBookManualScreen.kt:63-73` et al. | FÁCIL |
| R2 | **Chat não rola pro comentário recém-enviado** — envia e não vê a própria mensagem. | `DiscussionScreen.kt:266,617` | FÁCIL |
| R3 | **Barreira de spoiler vaza numa corrida de load** — `chapterNum` cai em `1` se `chapters` ainda não carregou (deep-link/cold start) → gate pulado, mostra debate à frente. Erra pro lado inseguro. | `DiscussionScreen.kt:68-72,114` | FÁCIL |
| R4 | **`getAllProgressForClubFlow` só retorna o próprio progresso** (RLS `select self`) → indicador "no ritmo/atrasado" dos **outros** membros vem vazio/errado. | `RemoteRepository.kt:1880-1892`; `policies.sql:93` | MÉDIO (RPC/view server-side) |
| R5 | **Flash de Onboarding/Welcome no cold start** — decisão antes do DataStore/sessão resolver; pisca avatar-picker/welcome pra quem já está logado. | `MainViewModel.kt:99-106`; `MainActivity.kt:82-121` | MÉDIO |
| R6 | **Mutex de drain é por-instância** (Worker × ViewModel = instâncias distintas) → drains concorrentes incrementam `attempts` 2×/ciclo → **dead-letter prematuro** (< 5 tentativas reais). | `RemoteRepository.kt:809` | MÉDIO (mutex em singleton) |
| R7 | **Signed URL de capa expira em 365d, sem regeneração** (o comentário promete, o código não faz) → capas 404 em ~1 ano. | `RemoteRepository.kt:1603-1615` | MÉDIO |
| R8 | `selectedDoc` obsoleto no Suggest permite adicionar livro fora da lista. | `SuggestScreen.kt:60,110-124` | FÁCIL |
| R9 | Aviso "permissão de câmera negada" **pisca por trás do diálogo do sistema** no 1º pedido. | `AddBookManualScreen.kt:82,219` | FÁCIL |
| R10 | `AuthErrors` deixa **erro cru em inglês** passar no fallback. | `AuthErrors.kt:70-73` | FÁCIL |
| R11 | `MainActivity` sem `launchMode="singleTop"` → deep link de auth cria Activity duplicada em vez de `onNewIntent`. | `AndroidManifest.xml:20-24` | FÁCIL |

---

## 7. 🟢 P3 / higiene / código morto

- **Código morto a remover:** aba "link" do JoinClub + parser de convite (`WelcomeScreen.kt:777,944-1026` — inalcançável), `insertNotification` (sem caller e barrado por RLS), `clearLocalCache` wrapper.
- **Reloaders com chave sem filtro** (`table:*=*`) → alternar clubes pode substituir o reload de um pelo do outro (staleness). `RemoteRepository.kt:1890,2081`.
- **`LoadingGate` é timer fixo de 2.5s** (`hasData=false` hardcoded) — em rede lenta (>2.5s) quem TEM clube ainda vê o empty-state piscar. `LoadingGate.kt:31-38`.
- **`race check-then-act` no `ensureRealtime`** pode vazar 1 canal. `RemoteRepository.kt:1093-1112` — FÁCIL (`putIfAbsent`).

---

## 8. 💡 Sugestões / melhorias (além de bugs)

1. **Tela de Configurações real** — o Onboarding promete "muda nas configurações" e o
   `AuthErrors` promete "vincular Google nas configurações", mas **não existe a tela**
   (settings hoje espalhados no Perfil). Curto prazo: trocar o texto para "no seu perfil".
2. **Feedback de sucesso padronizado** — SnackbarHost global ligado a `pendingMutationsCount`
   ("salvo — sincronizando…"). Hoje votar/RSVP/avaliar/salvar frase não dão retorno.
3. **Pull-to-refresh** nas listas principais.
4. **Deep link de convite** — decidir: implementar de verdade (exige domínio próprio +
   App Links + `autoVerify`) **ou** remover o código morto e a opção "clube aberto a quem tem
   link" (hoje inconsequente — não há descoberta de clube público, só join por código).
5. **Reenviar email de confirmação** no hint pós-signup (hoje dead-end se o email não chega).
6. **Testes da camada de dados** — hoje **0 cobertura** no código mais arriscado. Alvos:
   DAO in-memory, ciclo enqueue→drain da fila, mapeadores DTO↔domínio, e um teste que teria
   pego o **P0-1** (id de capítulo vs `uuid`).
7. **Atualizar dependências** — Compose BOM `2024.09.00` (≈2 anos, destoa de Kotlin 2.2 / AGP
   9.1), OkHttp/logging `4.10.0` (2022), Coil 2.x → 3.x, supabase-kt 3.6.
8. **Refatorar os dois "god files"** — `MainTabsScreen.kt` (3153 linhas) e `MainViewModel.kt`
   (1468) já custam manutenção. Decisão consciente documentada, mas o tamanho já dói;
   fatiar por feature reduz risco de regressão (como a do P0-1).

---

## 9. Plano de execução por sprints (sequência recomendada)

> Ordenado por **impacto percebido ÷ risco**. Quick wins de lentidão e os P0 primeiro.

**Sprint 1 — "Parar de perder dados" + quick wins de velocidade** _(alto impacto, baixo risco)_
- P0-1, P0-2 (⚠️ verificar em 2 dispositivos — toca sync/schema).
- L1, L2, L3 (discussão: `remember` + TTL + keys + içar `collectAsState`). Mata o "engasga".
- B3 (back stack do join), B4 (RPCs propagarem erro).

**Sprint 2 — Bugs de comportamento**
- B1 (votação por rodada + modelo de voto), B2 (identidade de capítulo — casa com P0-1).
- R1 (`rememberSaveable` nos formulários), R2 (scroll do chat), R3 (spoiler na corrida).

**Sprint 3 — Telas vazias / CTAs por papel** _(mata a queixa "sem ação")_
- U1–U8, priorizando U1/U2/U3 (o beco do primeiro uso).

**Sprint 4 — Robustez de dados/sync/realtime + loading**
- B5 (updates/deletes na fila), L5/L6/L9 (N+1, canais, cold start), R4 (progresso dos outros),
  R5 (flash cold start), R6 (mutex singleton), R7 (capas on-read).

**Sprint 5 — Produto + higiene**
- Configurações, feedback de sucesso, pull-to-refresh, decisão do deep link, reenviar email.
- Testes da camada de dados, atualização de deps, remoção de código morto.
- (Opcional/maior) fatiar `MainTabsScreen`/`MainViewModel`.

---

## 10. O que já está bom (verificado — não mexer)

Confirmado presente e **correto** no código atual (evitar retrabalho):
`MIGRATION_1_2`/`MIGRATION_2_3` (Room v3, fallback destrutivo só em DEBUG); fila offline com
`maxDrainAttempts=5`, distinção 4xx×408/429, dead-letter, parse por `jsonPrimitive.content`,
drain-antes-do-signOut; **forjar notificação bloqueado** (sem INSERT policy no cliente; tudo
por trigger/RPC SECURITY DEFINER); `GEMINI_API_KEY` e credenciais de admin barradas do
BuildConfig; `close()` chamado em `onCleared`/worker; schema **versionado** em
`supabase/schema/` + `migrations/`; RPCs (`create_club`, `join_club_with_code`, `leave_club`,
`delete_own_account`, etc.) existem server-side; **fontes com fallback embarcado** (Literata/
Inter em `res/font/`); mock "Estatísticas"/lorem e botão `⋮` morto **removidos**; timezone de
encontros centralizado (`MeetingTime`) com teste; recorrência de encontros com teste;
`rememberSaveable`/live-regions/IME/BackHandler no fluxo de auth; política de senha do Reset
já alinhada ao SignUp. Build e `testDebugUnitTest` **verdes**.

---

## 11. Riscos / o que exige verificação em runtime

- **P0-1 e P0-2, B1, B2** tocam **sync/schema/dados** — corretos por código, mas o efeito
  final (capítulos aparecendo entre 2 dispositivos, votação da 2ª rodada, criação offline
  sobrevivendo ao sync) **só se confirma rodando** com ≥2 contas/aparelhos. Parar para
  checkpoint visual antes de considerar fechados.
- **L6/L9** (ciclo de vida de Realtime) — refactor de lifecycle com risco de quebrar o
  realtime silenciosamente; validar canais sem leak e sem reload redundante em sessão real.
