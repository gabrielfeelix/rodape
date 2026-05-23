# Fase 4 — Estante na navbar, Votação evoluída e polimentos

**Status:** proposto  
**Data:** 2026-05-23  
**Autor:** Gabriel (com Claude)  
**Substitui parcialmente:** [Spec do redesign Tramabook](2026-05-21-tramabook-redesign-design.md) — esta fase adiciona/altera, não revisa o que já está pronto.  
**Precondições:** Fases 1, 2 e 3 concluídas e mergeadas na `master`. App offline-only, light theme, Room v2, `applicationId = app.tramabook`.

---

## 1. Objetivo

Evoluir três fluxos do Tramabook:

1. **Bugs visuais reportados:** OTP de 6 dígitos cortando o último na tela "Entrar num clube"; rótulo "DOMINGO" cortado no card de próximo encontro da Home.
2. **Votação do próximo livro:** hoje é estática (texto fixo, sem prazo, sem critério de encerramento, sem fila futura, justificativa sumindo). Precisa virar uma votação real com prazo, encerramento, N livros configurável e exibição da justificativa.
3. **Estante:** hoje é só uma sub-tab dentro de "Próximo" mostrando uma grade fria. Sobe pra tab principal da navbar e o detalhe do livro se torna um espaço rico com Resumo wiki, Frases, Chat retrospectivo, Avaliações e Histórico — porque é onde o clube revisita os livros que já leu.

## 2. Princípios não-negociáveis

- **Preservar o visual.** O usuário gostou do design atual. Esta fase ADICIONA elementos e CORRIGE bugs — não rasga UI. Mudanças visuais existem só onde há um problema concreto (bug, lacuna funcional, pedido explícito).
- **Offline-first, sem backend.** Tudo continua em Room. Sem API nova, sem autenticação real, sem permissões além do `papel = "admin"` já existente em `ClubMember`.
- **YAGNI sobre permissões e roles.** Não há sistema novo de permissões. O único bit usado é `ClubMember.papel == "admin"`, que libera: (a) abrir/encerrar votação, (b) marcar a data do encontro de um livro. Edição de resumo e adição de frases/avaliações é livre pra qualquer membro.
- **Reaproveitar entidades existentes.** `Comment` (já tem `chapterId`, `clubId`, `userId`, `texto`, `criadoEm`) é a base do Chat retrospectivo. `SavedQuote` (já tem `bookId`) é a base das Frases por livro. Não duplicar.
- **YAGNI sobre i18n e configurabilidade.** Strings em português direto no código (padrão atual do projeto). Sem `strings.xml` separados nesta fase.

## 3. Bugs visuais (correções cirúrgicas)

### 3.1 OTP de 6 dígitos na JoinClubScreen

**Arquivo:** [WelcomeScreen.kt:950-1028](../../app/src/main/java/com/example/ui/screens/WelcomeScreen.kt#L950-L1028)

**Problema:** os 6 `OutlinedTextField` têm `.width(44.dp)` fixo + um separador `-` no meio. Em telas estreitas (~360dp width comum em Android baixo/médio), o conjunto não cabe e o último campo é clipped pelo padding lateral de 24dp do `LazyColumn`.

**Fix:**
- Wrappar os 6 campos em um único `Row` com `horizontalArrangement = Arrangement.spacedBy(6.dp)`.
- Trocar `Modifier.width(44.dp)` por `Modifier.weight(1f)` em cada `OutlinedTextField`.
- Substituir o separador `Text("-")` por um `Spacer(Modifier.width(8.dp))` entre o 3º e 4º campo (sem texto, só respiro visual).
- Reduzir `textStyle` de `titleLarge` para `titleMedium` se ainda não couber em width 360dp (validar visualmente).
- Reduzir o padding horizontal do card que contém o OTP de 24dp para 16dp.

**Aceite:** em dispositivo/emulador 360×640 (Pixel 4a-ish), todos os 6 campos visíveis e clicáveis. Em telas maiores (411dp+) o layout continua idêntico ao atual.

### 3.2 Card "Próximo Encontro" da Home — "DOMINGO" cortado

**Arquivo:** [MainTabsScreen.kt:556-613](../../app/src/main/java/com/example/ui/screens/MainTabsScreen.kt#L556-L613)

**Problema:** `dayNameStr` faz `take(3)` (→ "DOM") mas o `Text` não declara `maxLines = 1` nem `overflow = Ellipsis`. A coluna de data tem `width(72.dp)` fixo, e se o `Calendar.getInstance().get(DAY_OF_WEEK)` (em algum momento futuro) gerar string maior, quebra a linha e empurra o "24" pra baixo.

**Fix:**
- Manter `take(3)` (já reduz a 3 letras maiúsculas — DOM/SEG/TER/etc).
- Adicionar `maxLines = 1, overflow = TextOverflow.Ellipsis` no `Text(dayNameStr)`.
- Aumentar `Column.width` de `72.dp` para `80.dp`.
- Adicionar `maxLines = 1, overflow = TextOverflow.Ellipsis` no `Text(finalMonthLabel)` (mesma justificativa).

**Aceite:** card renderiza idêntico ao atual em conteúdos típicos; nunca quebra em telas estreitas.

## 4. Reorganização da navbar

### 4.1 Nova estrutura

A navbar passa de **4 → 5 tabs**:

```
Início · Livro atual · Próximo · Estante · Perfil
```

**Mudanças:**
- Tab "Estante" é nova.
- Sub-tab "Estante" dentro de "Próximo" é **removida**. A tab "Próximo" passa a ter só 2 sub-tabs: Encontro e Votação.
- Ícone da Estante: `Icons.Outlined.MenuBook` (não-selecionado) / `Icons.Filled.MenuBook` (selecionado). Label: "Estante".

### 4.2 Layout da bottom bar

Hoje a [CustomBottomBar](../../app/src/main/java/com/example/ui/screens/MainTabsScreen.kt#L335-L387) usa `Arrangement.SpaceAround` num `Row` dentro de um `Surface` pill (`OlivaDeep`, `RoundedCornerShape(999.dp)`). O selecionado mostra ícone+label; os outros, só ícone.

**Verificação necessária:** com 5 tabs num pill horizontal, o item selecionado (ícone+label) tem ~120-150dp; os 4 não-selecionados têm ~44dp cada. Total ≈ 300-330dp + padding. Em width 360dp (descontando 32dp de padding lateral), sobra ~328dp. **Aperta mas cabe.** Mitigações se não couber:
- Reduzir padding interno do item selecionado de `horizontal = 16.dp` para `12.dp`.
- Reduzir padding lateral do `Box` envolvente de `horizontal = 16.dp` para `12.dp`.

### 4.3 Roteamento

Adicionar `"shelf"` ao `selectedTab` state em `MainTabsScreen`. Novo composable `ShelfTabScreen` substitui o `EstanteTab` que existe hoje em `NextTabScreen.kt` (esse `EstanteTab` é removido junto com sua referência no `when (subTab)` de `NextTabScreen`).

## 5. Tab Estante (nova)

### 5.1 Comportamento

Lista **livros já lidos pelo clube** (status `finished`), em grade 2 colunas (igual layout atual da Estante sub-tab — preservar o visual).

Cada card mostra:
- Cover (100×150dp)
- Título (1 linha, ellipsis)
- Autor (1 linha, ellipsis)
- **NOVO:** "Encontro em DD/MMM" (label small, cor Muted) — data extraída de `ClubBook.dataEncontro` (novo campo, ver §9).
- Estrelas (1–5), média de `BookRating` daquele livro (ver §6.4). Hoje é mockada por id — passa a ser computada de verdade.

Filtros (chips no topo, igual hoje): `Todos / Favoritos`. "Favoritos" = média ≥ 4.5 estrelas.

Clique → abre nova tela `BookDetailScreen` enriquecida (ver §6).

### 5.2 Estado vazio

"Nenhum livro lido ainda pelo clube. Quando vocês terminarem um livro, ele aparece aqui." (centralizado, `Muted`).

## 6. Detalhe do Livro enriquecido

### 6.1 Arquivo e abas

Refatorar [BookDetailScreen.kt](../../app/src/main/java/com/example/ui/screens/BookDetailScreen.kt). As abas internas passam de **3 → 5**:

```
Resumo · Frases · Chat · Avaliações · Histórico
```

Layout do TabRow segue o padrão atual (Literata, underline terracota no selecionado, divider soft no resto). Com 5 abas, cada uma fica `weight(1f)` num `Row` horizontal. Se o label "Avaliações" estourar em telas 360dp, reduzir `fontSize` da label de tab de `titleMedium` para `bodyMedium`.

### 6.2 Header (banner)

Hero gradient (`OlivaSoft → Paper`), cover 150×224dp e título/autor centralizados — **idênticos ao atual**.

O banner abaixo do hero muda de:
> "Este livro está na estante do clube."

para algo informativo:
> "Lido pelo clube · Encontro em DD/MMM/AAAA"

(se `dataEncontro` não existe, fallback: "Lido pelo clube"). Mantém background `Oliva`, texto `Cream`, mesma forma e padding.

### 6.3 Aba Resumo

Wiki coletivo do clube sobre o livro.

**Persistência:** nova entidade `BookSummary` (ver §9).

**Comportamento:**
- Se não existe `BookSummary` para o par `(bookId, clubId)`: mostra estado vazio com texto cinza "Ninguém escreveu o resumo ainda. Que tal começar?" e botão `Escrever resumo` (`TbButton Outline`).
- Se existe: renderiza o texto em `TramabookCard` (multi-linha, `lineHeight 22.sp`), e abaixo do card um rodapé pequeno "editado por {nome} · há {tempo}". Botão `Editar resumo` (`TbButton Outline`, ícone `Edit`) abre o editor.
- Editor: `AlertDialog` cheio (ou `ModalBottomSheet` em fullscreen) com um `OutlinedTextField` multilinha (min 8 linhas), botões Cancelar / Salvar. Salvar grava `BookSummary(bookId, clubId, texto, lastEditorId = currentUserId, updatedAt = now)`.

**Quem edita:** qualquer membro do clube. Sem confirmação de "tem certeza que quer sobrescrever?" — wiki simples mesmo. Histórico de edições fora de escopo.

### 6.4 Aba Frases

Lista de `SavedQuote` filtradas por `bookId` (já existe via `getSavedQuotesForBook`).

**Diferenças do atual:**
- Cada card de frase passa a mostrar **avatar + nome de quem salvou** (campo `userId` já existe na entidade; basta resolver via `clubMembers`).
- Mantém: texto da frase, capítulo (chip), botão deletar (só na frase do usuário logado).
- Botão "Salvar uma frase" continua igual.

### 6.5 Aba Chat (NOVA)

Feed retrospectivo unificado de **todos os `Comment` do livro** (de todos os capítulos), em ordem cronológica ascendente.

**Layout:**
- `LazyColumn` com items intercalados:
  - Header de capítulo: divisor horizontal sutil + label `"── CAPÍTULO {n} · {titulo} ──"` (style `labelSmall`, `OlivaMid`, letterSpacing). Aparece **uma vez por capítulo** sempre que o próximo comentário muda de capítulo. (Note que comentários do mesmo capítulo podem aparecer agrupados no feed cronológico se forem postados em sequência; se intercalados, o header se repete — comportamento OK.)
  - Card de comentário (mesma estética do `DiscussionScreen`): avatar, nome, timestamp relativo, texto.

**Read-only.** Sem input. Quem quer comentar continua indo em "Livro atual" → discussão do capítulo. Justificativa: a Estante é histórico. Chat aqui é pra rever, não pra adicionar nova mensagem num livro já lido.

**Estado vazio:** "Esse livro não rendeu conversa por aqui." (`Muted`, centralizado).

**Implementação:** novo método no `MainViewModel`: `getAllCommentsForBookFlow(bookId, clubId): Flow<List<Comment>>`. Faz JOIN com `chapters` pra ordenar por `chapter.numero` ASC, depois `Comment.criadoEm` ASC. Retorna `List<Pair<Chapter, List<Comment>>>` ou um `List<ChatItem>` selado (`Header(chapter) | Message(comment)`).

### 6.6 Aba Avaliações (NOVA)

**Persistência:** nova entidade `BookRating` (ver §9).

**Layout:**
- Topo: bloco grande centralizado com estrelas (5 estrelas grandes, gold `#E6BF6B` igual ao usado hoje na Estante grid), label "{media} de 5 · {n} avaliações".
- Botão `Avaliar este livro` (ou `Editar minha avaliação` se já avaliou). `TbButton Terra`, full-width.
- Lista de cards, um por avaliação:
  - Avatar + nome + estrelas (small)
  - Texto do comentário (se houver), `bodyMedium`
  - Timestamp relativo
- Rodapé sutil: "{n} membros ainda não avaliaram" (se houver).

**Dialog de avaliação:** `AlertDialog` com 5 estrelas tocáveis (rating de 1 a 5), `OutlinedTextField` opcional pra comentário (placeholder: "Conta o que tu achou (opcional)"), Cancelar / Salvar.

**Cada usuário tem 1 rating por livro** (PK composta `(bookId, userId)` em `BookRating`). Re-salvar substitui.

**Média:** soma de stars / n ratings, arredondado a 1 casa decimal pra exibição, mas armazenado raw.

### 6.7 Aba Histórico

Mantém a timeline atual mas com datas reais quando disponíveis:
- "Sugerido por {nome} em {data}" — usa `BookSuggestion.criadoEm` se existir, senão fallback "Sugerido".
- "Leitura começou em {data}" — derivada do primeiro `UserProgress` para o livro no clube (mais antigo `criadoEm`... mas `UserProgress` hoje não tem `criadoEm`. Vou usar fallback "Leitura começou" sem data até adicionar timestamp em fase futura).
- "Encontro do clube em {data}" — usa `ClubBook.dataEncontro` se existir.

Se nenhuma data existir, mantém timeline genérica atual.

## 7. Votação evoluída

### 7.1 Princípio

O usuário foi explícito: **preserve o visual atual da tab Votação**, só adicione informação e overlays. Sem refazer cards.

### 7.2 Modelo de dados (novas entidades)

- `VotingRound(id, clubId, criadoPor, abertaEm, fechaEm, n_livros, cadencia, status, vencedoresJson)` — uma rodada de votação. Ver §9.
- `BookSuggestion(clubBookRowId, suggestedByUserId, justificativa, criadoEm)` — guarda a justificativa por (clubId+bookId). Ver §9.
- `Vote` ganha campo `votingRoundId` (FK opcional na migração, NOT NULL pra dados novos).

### 7.3 Fluxo: abrir uma votação (admin only)

Quando a tab Votação não tem rodada com `status = "aberta"`:

- **Para membros não-admin:** estado vazio: "Não há votação aberta no momento. Quando o admin abrir, vocês votam aqui."
- **Para admin:** mesmo estado vazio + botão `Abrir nova votação` (`TbButton Terra`).

Clique → abre `ModalBottomSheet`:

```
[ Abrir votação do clube ]

Quantos livros vamos escolher?
  [ - ] [ 1 ] [ + ]      ← input numérico, default 1, range 1-12

Quanto tempo a votação fica aberta?
  ( ) 3 dias   ( ) 7 dias   ( ) 14 dias   ( ) outro
  Se "outro": date picker, máx 60 dias

Qual a cadência da escolha?
  ( ) Única   ( ) Semanal   ( ) Quinzenal   ( ) Mensal   ( ) Anual
  (só metadado pro texto descritivo no card; não afeta lógica)

[ Cancelar ]   [ Abrir votação ]
```

Salvar → cria `VotingRound(status = "aberta", abertaEm = now, fechaEm = now + duração, n_livros = N, cadencia = ...)`. Os livros com status `suggested` no clube já existem; novos sugeridos durante a rodada são automaticamente incluídos.

### 7.4 Fluxo: votar (membros)

**Mudanças visuais MÍNIMAS no card de cada livro sugerido:**

1. **Header da tab Votação** ganha uma 2ª linha abaixo do título:
   > "Aberta até 27 mai · Escolham o próximo livro" (N=1)  
   > "Aberta até 27 mai · Escolham os próximos {N} livros" (N>1)
   
   O texto fixo gigante atual ("Votem no livro que vocês gostariam de ler...") é **substituído** por essa 2 linhas. Mantém `TramabookCard` envolvente — só o conteúdo do card muda.

2. **Cards de sugestão** ganham 1 ícone `Icons.AutoMirrored.Outlined.Comment` (ou `ChatBubbleOutline`) ao lado do título, **apenas se houver `BookSuggestion.justificativa` não-vazia**. Tamanho 16dp, cor `OlivaMid`, com `Modifier.clickable`. Clique abre `ModalBottomSheet`:
   ```
   [Avatar] {nome} sugeriu

   "{justificativa}"

   [ Fechar ]
   ```

3. **Cada usuário pode votar em até N livros** (em vez de 1). UI: o `Pill "Teu voto"` aparece em cada livro votado. Botão `Votar nesse` continua, mas trava se o usuário já tem N votos (texto muda pra `Limite de votos atingido`, desabilitado). Para mudar voto: clica num livro já votado → desfaz o voto.

4. **Fila de próximos livros** (status `next`): bloco compacto abaixo da lista de sugestões, antes do botão "Sugerir livro":
   ```
   [pill terracota] FILA DO CLUBE
   [mini-cover] {Título 1}
   [mini-cover] {Título 2}
   ...
   ```
   Só aparece se houver livros em status `next`. Clique no item → abre BookDetailScreen.

5. **Botão "Encerrar votação agora"** (admin only) no fim da lista de sugestões: `TbButton Outline Terra`, full-width, label "Encerrar votação agora". Confirma via dialog: "Encerrar agora vai escolher os {N} livros mais votados. Sem volta. Confirma?"

### 7.5 Fluxo: encerrar votação

Duas formas:
- **Automática (prazo):** quando o app abre/foca e `now >= round.fechaEm` e `status == "aberta"`, dispara o encerramento.
- **Manual (admin):** botão acima.

Encerramento:
1. Calcula os top-N livros por contagem de votos. Em caso de empate, desempata por `criadoEm` do `ClubBook` (livros sugeridos antes ganham).
2. Marca `round.status = "fechada"`, grava `vencedoresJson = [bookId1, bookId2, ...]`.
3. **Promove os vencedores:**
   - Se já existe um livro `current` no clube e o usuário admin não interveio, marca o `current` antigo como `finished` (com `dataEncontro = now` por default; admin pode editar depois). Sem confirmação adicional — encerrar a votação já significa "começamos o próximo livro".
   - O 1º vencedor vira `current`.
   - Os demais (se N>1) viram `next`, ordenados pelo ranking.
4. Cria notificação local pra cada membro do clube: tipo `voting_closed`, payload `{titulos: [...], n: N}`. Texto: "O clube escolheu: {Título 1}" (ou "O clube escolheu {N} livros: {Título 1}, ...").
5. Demais livros que ficaram com votos mas não venceram permanecem como `suggested` pra próxima rodada.

### 7.6 Sugestão de livro (correção)

**Bug atual:** `createBookSuggestion` em [MainViewModel.kt:407-434](../../app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt#L407-L434) recebe `justification` mas só o comenta no código ("Keep justification as first comment...") sem persistir. **Fix:** criar `BookSuggestion(clubBookRowId, suggestedByUserId = currentUserId, justificativa, criadoEm)` na mesma transação. Esse registro é o que alimenta o ícone de comentário no card de votação.

## 8. Data do encontro de livros finalizados (admin)

Pra dar significado à frase "Encontro em DD/MMM" na Estante, o admin precisa poder informar a data. Não vou criar uma tela nova — basta um campo opcional acessível **no Detalhe do Livro, aba Histórico**, visível só pro admin: linha "Encontro em: [date picker]". Salvar → atualiza `ClubBook.dataEncontro`.

Se nenhum admin marcar, o campo fica null e o app omite a data graciosamente em todos os lugares.

No fluxo de encerrar votação (§7.5), o livro `current` antigo (ao virar `finished`) recebe `dataEncontro = now` automaticamente, como assumption razoável ("se vocês escolheram o próximo livro, é porque o encontro do atual rolou ou tá pra rolar"). Admin pode editar.

## 9. Migração de schema (Room v2 → v3)

### 9.1 Novas tabelas

```kotlin
@Entity(tableName = "book_summaries", primaryKeys = ["bookId", "clubId"])
data class BookSummary(
    val bookId: String,
    val clubId: String,
    val texto: String,
    val lastEditorId: String,
    val updatedAt: Long
)

@Entity(tableName = "book_ratings", primaryKeys = ["bookId", "clubId", "userId"])
data class BookRating(
    val bookId: String,
    val clubId: String,
    val userId: String,
    val stars: Int,         // 1..5
    val comment: String,    // pode ser ""
    val updatedAt: Long
)

@Entity(tableName = "book_suggestions")
data class BookSuggestion(
    @PrimaryKey val id: String,            // uuid
    val clubId: String,
    val bookId: String,
    val suggestedByUserId: String,
    val justificativa: String,
    val criadoEm: Long
)

@Entity(tableName = "voting_rounds")
data class VotingRound(
    @PrimaryKey val id: String,
    val clubId: String,
    val criadoPor: String,
    val abertaEm: Long,
    val fechaEm: Long,
    val nLivros: Int,
    val cadencia: String,           // "unica" | "semanal" | "quinzenal" | "mensal" | "anual"
    val status: String,             // "aberta" | "fechada"
    val vencedoresJson: String      // "[]" enquanto aberta; "[bookId1,bookId2,...]" depois
)
```

### 9.2 Alterações em tabelas existentes

```kotlin
// Vote: ganha votingRoundId. Nullable na migração pra não destruir dados antigos.
@Entity(tableName = "votes", primaryKeys = ["clubBookId", "userId"])
data class Vote(
    val clubBookId: String,
    val userId: String,
    val votedAt: Long,
    val votingRoundId: String?      // NOVO, nullable
)

// ClubBook: ganha dataEncontro opcional.
@Entity(tableName = "club_books", primaryKeys = ["clubId", "bookId"])
data class ClubBook(
    val clubId: String,
    val bookId: String,
    val status: String,
    val ordem: Int,
    val dataEncontro: Long?         // NOVO, nullable
)
```

### 9.3 Estratégia de migração

O projeto já usa Room sem `Migration` formal (a v1→v2 da Fase 3 usou `fallbackToDestructiveMigration`). Como o app ainda não está em produção real (publicação na Play Store é o próximo passo, não aconteceu), **manter `fallbackToDestructiveMigration()` na v3**. Ao subir, o seed recria o estado inicial do clube demo.

**Risco aceito:** usuários que testaram localmente perdem dados na atualização. Aceitável porque (a) ainda é beta interno, (b) o seed restaura a experiência completa.

### 9.4 Seed (TramabookRepository.seedDatabase)

Adicionar ao seed do `club_mari`:
- 1 `VotingRound` aberto: `id = "round_mari_1"`, `criadoPor = "user_mari"` (admin do clube demo), `abertaEm = now`, `fechaEm = now + 7 dias`, `nLivros = 1`, `cadencia = "unica"`, `status = "aberta"`, `vencedoresJson = "[]"`.
- Para cada `Book` com status `suggested` no seed atual, criar um `BookSuggestion` correspondente com `suggestedByUserId` variando entre membros do clube e `justificativa` de exemplo curta (1–2 frases naturais em português).
- Para 3 dos `Book` com status `finished`: `BookSummary` semi-preenchido (1 parágrafo) + 2-3 `BookRating` de membros distintos com `stars` 4-5 e algum comentário.
- Para os `Book` com status `finished`: `ClubBook.dataEncontro` preenchida (datas no passado, espaçadas mensalmente pra parecer realista).

## 10. Mapa de arquivos

### Arquivos novos
- `app/src/main/java/com/example/ui/screens/ShelfTabScreen.kt` — composable da tab Estante (extrai a lógica do antigo `EstanteTab` e adiciona data do encontro + ratings reais).
- `app/src/main/java/com/example/ui/components/RatingStars.kt` — componente reutilizável de estrelas (tocável e display).
- `app/src/main/java/com/example/ui/components/ChapterDivider.kt` — header de capítulo no Chat retrospectivo.
- `app/src/main/java/com/example/data/migration/SeedV3.kt` (opcional, se ficar grande demais pra inline no Repository).

### Arquivos alterados
- `app/src/main/java/com/example/data/model/Entities.kt` — novas data classes + alteração de `Vote` e `ClubBook`.
- `app/src/main/java/com/example/data/db/AppDatabase.kt` — version 2 → 3, novas entities, `fallbackToDestructiveMigration` permanece.
- `app/src/main/java/com/example/data/db/TramabookDao.kt` — DAOs novos (rounds, ratings, summaries, suggestions) e alteração nos DAOs de votes.
- `app/src/main/java/com/example/data/repository/TramabookRepository.kt` — novos métodos + seed expandido.
- `app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt` — novos flows (votingRound atual, ratings por livro, summary por livro, comments por livro agregados) e novas ações (`openVotingRound`, `closeVotingRound`, `saveRating`, `saveSummary`, `setBookMeetingDate`, `createBookSuggestion` corrigido).
- `app/src/main/java/com/example/ui/screens/MainTabsScreen.kt` — bottom bar com 5 tabs, novo case `"shelf"`, ajustes responsivos no card de Próximo Encontro.
- `app/src/main/java/com/example/ui/screens/NextTabScreen.kt` — remove `EstanteTab`, tab interna passa a ter 2 sub-tabs; refaz `VotacaoTab` com a nova lógica de rodadas.
- `app/src/main/java/com/example/ui/screens/BookDetailScreen.kt` — 5 abas, novo conteúdo de cada uma, banner enriquecido.
- `app/src/main/java/com/example/ui/screens/WelcomeScreen.kt` — fix do OTP responsivo (§3.1).

## 11. Critérios de aceite (UAT)

1. **OTP responsivo:** em emulador 360dp width, todos os 6 campos visíveis na JoinClubScreen, sem clipping.
2. **DOMINGO fix:** card de Próximo Encontro nunca quebra `dayNameStr` em 2 linhas, em qualquer width.
3. **Navbar 5 tabs:** tocar em "Estante" abre a grade de finished books com data do encontro. Tocar em "Próximo" mostra só 2 sub-tabs (Encontro, Votação).
4. **Estante grid:** cada card mostra data do encontro (quando preenchida), e estrelas baseadas em rating médio real.
5. **BookDetail 5 abas:** Resumo, Frases, Chat, Avaliações, Histórico — todas renderizam sem crash, mesmo com dados vazios.
6. **Resumo wiki:** abrir BookDetail de um livro com seed populado mostra resumo + "editado por X há Y". Editar e salvar persiste após reabrir o app.
7. **Chat retrospectivo:** abrir BookDetail de um livro lido mostra feed unificado com divisores de capítulo separando blocos de comentários.
8. **Avaliações:** avaliar um livro (1-5 estrelas + comentário opcional) cria um card. Reavaliar substitui o anterior. Média no topo se atualiza.
9. **Sugestão com justificativa:** sugerir um livro com justificativa salva → na tab Votação, o card desse livro mostra o ícone de comentário; clique abre sheet com texto + autor.
10. **Abrir votação (admin):** logar como admin do clube demo (`user_mari`... o seed atual usa `user_voce` como demo, então criar uma forma de testar — ver §12), abrir tab Votação sem rodada ativa, criar nova com 7 dias / N=3 / Mensal. Rodada aparece pros demais membros.
11. **Votar em N:** com rodada N=3 aberta, votar em 3 livros → 4º clique mostra "Limite de votos atingido". Desfazer um voto libera vaga.
12. **Fila de próximos livros:** após encerrar uma rodada N=3, a tab Votação mostra "Fila do clube" com os 2 livros que viraram `next` (1º virou `current`).
13. **Encerrar manual (admin):** admin clica em "Encerrar votação agora" → confirma → rodada vira fechada, vencedores promovidos, notificação criada.
14. **Encerrar automático:** alterar `fechaEm` no banco pra now-1, reabrir o app → rodada encerra automaticamente.

## 12. Pontos abertos e assumptions

- **Usuário admin para testar:** o seed atual loga como `user_voce` mas o admin do `club_mari` é `user_mari`. Pra testar fluxos de admin sem refatorar o sistema de auth, vou adicionar `user_voce` como `papel = "admin"` em `club_mari` no seed (e marcar `user_mari` também como admin). Assumption: nesta fase, "admin" significa simplesmente "tem o flag no `ClubMember` desse clube".
- **Notificações novas:** já existe `DbNotification` com `tipo` e `payloadJson` livre. Adicionar tipos `voting_closed`, `voting_opened`, `rating_added` sem mudança de schema. Renderização na tela de Notificações: fora de escopo desta fase; o item aparece como notificação genérica se a UI não souber renderizar.
- **Timestamps relativos** ("há 2 dias", "há 1 hora"): usar helper simples baseado em `(now - timestamp) / unit`. Sem biblioteca nova.
- **Fora de escopo desta fase:** sistema de permissões real, edit history de resumo, threading de comentários no Chat retrospectivo, push notifications, sync online, paginação infinita.

## 13. Riscos

- **5 tabs apertando a navbar em telas pequenas.** Mitigação prevista em §4.2. Se mesmo assim ficar feio em 360dp, fallback é deixar o item selecionado mostrando só ícone também (uniforme). Decidir em validação visual.
- **Aba "Chat" pode confundir** com a discussão em "Livro atual" → capítulo. Mitigação: subtítulo discreto na aba: "(somente leitura)". Se persistir confusão em uso real, renomear pra "Conversas" em fase futura.
- **Promoção automática de current → finished** ao encerrar votação pode surpreender. Mitigação: o dialog de confirmação ("Encerrar agora vai escolher os {N} livros...") deixa explícito que o atual termina. Se for incômodo, fase futura adiciona checkbox "manter o atual ainda lendo".
- **Migração destrutiva** apaga dados locais de testes. Mitigação: documentar em CHANGELOG da fase. Aceito porque o app ainda não foi publicado.

## 14. Próximos passos após esta fase (não fazer agora)

- Tela de Notificações renderizando `voting_closed` / `voting_opened` com CTA "Ver fila".
- Marcar livro como `current` manualmente (sem precisar de votação).
- Comentários no Chat retrospectivo (input liberado).
- Histórico de edições do Resumo.
- Backup local exportável (pré-Play Store).
