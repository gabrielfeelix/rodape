# Fase 3 — Telas Novas + Ajustes de Produção: Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Adicionar as telas que o protótipo tem e o app não (Detalhe do livro, Frases guardadas), incluindo a persistência de frases (entidade Room `SavedQuote`), e deixar o app pronto para publicar (applicationId, ícone, versionCode).

**Architecture:** A camada de dados ganha UMA entidade nova (`SavedQuote`) + DAO + repositório + flows no ViewModel. Duas telas novas (`BookDetailScreen`, `FrasesScreen`) entram no `NavHost`. A `EstanteTab` passa a navegar para o detalhe. Ajustes de produção tocam `build.gradle.kts`, `AndroidManifest.xml` e os recursos de ícone. O app compila e navega ao fim de cada tarefa.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Room, componentes da Fase 1.

---

## Contexto para quem nunca viu o projeto

- Tramabook é um app Android de clube de leitura. Fases 1 (design system) e 2 (re-skin das telas) já concluídas.
- Componentes da Fase 1 em `app/src/main/java/com/example/ui/components/`: `Cover(title, author, coverUrl, modifier, width, height)`, `Avatar(name, modifier, avatarUrl, size, ring)`, `TbButton(text, onClick, modifier, variant, size, enabled)` (`TbButtonVariant.{Primary,Terra,TerraSoft,Outline,Dark,OlivaSoft}`, `TbButtonSize.{Sm,Md,Lg}`), `Pill(text, modifier, variant)` (`PillVariant.{Default,Olive,OliveDeep,Terra,Mustard,Ink,Outline}`), `ProgressBar`, `TramabookCard`, `TbSectionHeader`.
- Tokens em `ui/theme/Color.kt`: `Terracota`, `TerracotaSoft`, `Oliva`, `OlivaDark`, `OlivaDeep`, `OlivaSoft`, `OlivaMid`, `Ink`, `InkSoft`, `Muted`, `Tertiary`, `Cream`, `CardSurface`, `CardSoft`, `Divider`, `DividerSoft`, `Paper`. Fontes `LiterataFontFamily`/`InterFontFamily`.
- Camada de dados: `data/model/Entities.kt` (13 entidades Room), `data/db/TramabookDao.kt`, `data/db/AppDatabase.kt` (version 1, `fallbackToDestructiveMigration`), `data/repository/TramabookRepository.kt`, `ui/viewmodel/MainViewModel.kt`.
- Navegação: `MainActivity.kt` tem um `NavHost` com rotas `welcome`, `login`, `create_club`, `join_club`, `main_tabs`, `notifications`, `discussion/{chapterId}/{title}`, `suggest_book`.
- O protótipo-alvo é `claude-design/`: a tela de detalhe está em `screens-book-detail.jsx` (`BookDetailScreen` com abas Resumo/Frases/Histórico, `QuoteCard`, `FrasesScreen`).
- Build: `cd ~/dev/tramabook && export JAVA_HOME=$HOME/.local/opt/jdk-21 ANDROID_HOME=$HOME/.local/opt/android-sdk && ./gradlew assembleDebug`.

## Decisões de escopo

- "Estante" já existe como sub-aba (`EstanteTab` em `NextTabScreen.kt`) — NÃO criar tela separada. Apenas fazer os livros da estante navegarem para o Detalhe.
- "Frases" tem persistência REAL via entidade `SavedQuote`.
- `AppDatabase` usa `fallbackToDestructiveMigration` — ao subir a `version` de 1 para 2, o banco local é recriado e o seed repovoa. Aceitável (app local sem backend).
- `applicationId` muda para `app.tramabook`.

---

## Task 1: Entidade `SavedQuote` + camada de dados

**Files:**
- Modificar: `app/src/main/java/com/example/data/model/Entities.kt`
- Modificar: `app/src/main/java/com/example/data/db/TramabookDao.kt`
- Modificar: `app/src/main/java/com/example/data/db/AppDatabase.kt`
- Modificar: `app/src/main/java/com/example/data/repository/TramabookRepository.kt`

- [ ] **Step 1: Adicionar a entidade `SavedQuote` em `Entities.kt`**

No fim de `app/src/main/java/com/example/data/model/Entities.kt`, adicionar:

```kotlin
@Entity(tableName = "saved_quotes")
data class SavedQuote(
    @PrimaryKey val id: String,
    val userId: String,
    val clubId: String,
    val bookId: String,
    val texto: String,
    val capituloRef: String, // ex.: "Cap. 7" ou nome do capítulo; livre
    val criadoEm: Long
)
```

- [ ] **Step 2: Adicionar os métodos de DAO em `TramabookDao.kt`**

No fim da interface `TramabookDao` (antes da última `}`), adicionar:

```kotlin
    // --- Saved Quotes ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedQuote(quote: SavedQuote)

    @Delete
    suspend fun deleteSavedQuote(quote: SavedQuote)

    @Query("SELECT * FROM saved_quotes WHERE userId = :userId ORDER BY criadoEm DESC")
    fun getSavedQuotesForUserFlow(userId: String): Flow<List<SavedQuote>>

    @Query("SELECT * FROM saved_quotes WHERE userId = :userId AND bookId = :bookId ORDER BY criadoEm DESC")
    fun getSavedQuotesForBookFlow(userId: String, bookId: String): Flow<List<SavedQuote>>
```

(`Insert`, `Delete`, `Query`, `OnConflictStrategy` e `Flow` já estão importados no arquivo — confirmar; se faltar algum, adicionar.)

- [ ] **Step 3: Registrar a entidade em `AppDatabase.kt`**

Em `app/src/main/java/com/example/data/db/AppDatabase.kt`:
- Adicionar `SavedQuote::class,` na lista `entities = [...]`.
- Mudar `version = 1` para `version = 2`.

(O `fallbackToDestructiveMigration()` já está no builder — não mexer nele.)

- [ ] **Step 4: Adicionar os métodos no repositório**

Em `app/src/main/java/com/example/data/repository/TramabookRepository.kt`, na seção apropriada, adicionar:

```kotlin
    // --- Saved Quotes ---
    suspend fun insertSavedQuote(quote: SavedQuote) = dao.insertSavedQuote(quote)
    suspend fun deleteSavedQuote(quote: SavedQuote) = dao.deleteSavedQuote(quote)
    fun getSavedQuotesForUserFlow(userId: String): Flow<List<SavedQuote>> = dao.getSavedQuotesForUserFlow(userId)
    fun getSavedQuotesForBookFlow(userId: String, bookId: String): Flow<List<SavedQuote>> = dao.getSavedQuotesForBookFlow(userId, bookId)
```

- [ ] **Step 5: Compilar** — `cd ~/dev/tramabook && export JAVA_HOME=$HOME/.local/opt/jdk-21 ANDROID_HOME=$HOME/.local/opt/android-sdk && ./gradlew compileDebugKotlin`. Expected: BUILD SUCCESSFUL (KSP gera o código do Room).

- [ ] **Step 6: Commit**
```bash
git add app/src/main/java/com/example/data/
git commit -m "feat(data): entidade SavedQuote para frases guardadas"
```

---

## Task 2: ViewModel — flows e ações de frases

**Files:**
- Modificar: `app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt`

- [ ] **Step 1: Adicionar os flows e ações de `SavedQuote` no `MainViewModel`**

Em `MainViewModel.kt`, adicionar (seguindo o padrão dos outros flows do arquivo — `stateIn` com `viewModelScope`):

- Um `StateFlow<List<SavedQuote>>` chamado `savedQuotes` que combina `currentUserId` com `repository.getSavedQuotesForUserFlow(userId)` (todas as frases do usuário). Quando `userId` é null → `emptyList()`.
- Uma função `getSavedQuotesForBook(bookId: String): Flow<List<SavedQuote>>` que retorna `repository.getSavedQuotesForBookFlow(currentUserId.value ?: "user_voce", bookId)`.
- Uma função `saveQuote(bookId: String, texto: String, capituloRef: String)` que, em `viewModelScope.launch`, monta um `SavedQuote` (id `"quote_${UUID.randomUUID()}"`, userId = `currentUserId.value ?: "user_voce"`, clubId = `activeClubId.value ?: ""`, criadoEm = `System.currentTimeMillis()`) e chama `repository.insertSavedQuote(...)`. Não fazer nada se `texto` estiver em branco.
- Uma função `deleteQuote(quote: SavedQuote)` que chama `repository.deleteSavedQuote(quote)` em `viewModelScope.launch`.

Seguir o estilo exato do arquivo (imports `UUID`, `kotlinx.coroutines.flow.*` já presentes). NÃO alterar nada que já existe no ViewModel.

- [ ] **Step 2: Compilar** — `./gradlew compileDebugKotlin`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt
git commit -m "feat(viewmodel): flows e ações de frases guardadas"
```

---

## Task 3: Seed de frases de exemplo

**Files:**
- Modificar: `app/src/main/java/com/example/data/repository/TramabookRepository.kt` (função `seedDatabase`)

- [ ] **Step 1: Adicionar frases de exemplo ao seed**

Na função `seedDatabase()` de `TramabookRepository.kt`, ao final (antes do fechamento da função), inserir ~4 `SavedQuote` de exemplo para `user_voce` no clube `club_mari`, usando os bookIds que o seed já cria (`book_metamorfose`, `fin_1`, `fin_2`, `fin_3`). Exemplo de conteúdo (use estes, são fiéis ao tom do app):

```kotlin
        // Frases guardadas de exemplo
        dao.insertSavedQuote(SavedQuote("quote_seed_1", "user_voce", "club_mari", "book_metamorfose",
            "A culpa é minha, dizia Macabéa sem saber bem de quê.", "Cap. 1", System.currentTimeMillis() - 6 * 24 * 3600 * 1000L))
        dao.insertSavedQuote(SavedQuote("quote_seed_2", "user_voce", "club_mari", "book_metamorfose",
            "Ela acreditava em anjos e, porque acreditava, eles existiam.", "Cap. 7", System.currentTimeMillis() - 3 * 24 * 3600 * 1000L))
        dao.insertSavedQuote(SavedQuote("quote_seed_3", "user_voce", "club_mari", "fin_3",
            "A terra é a nossa carta de alforria sem assinatura.", "Torto arado", System.currentTimeMillis() - 12 * 24 * 3600 * 1000L))
        dao.insertSavedQuote(SavedQuote("quote_seed_4", "user_voce", "club_mari", "fin_1",
            "O choro de minha mãe não desaguava em rio. Empoçava nos olhos.", "Olhos d'água", System.currentTimeMillis() - 20 * 24 * 3600 * 1000L))
```

O `seedDatabase()` só roda quando o banco está vazio; como a Task 1 subiu a `version` para 2, o `fallbackToDestructiveMigration` recria o banco e o seed roda de novo — as frases entram naturalmente.

- [ ] **Step 2: Compilar** — `./gradlew compileDebugKotlin`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/example/data/repository/TramabookRepository.kt
git commit -m "feat(data): frases de exemplo no seed"
```

---

## Task 4: Tela Detalhe do Livro (`BookDetailScreen`)

**Files:**
- Criar: `app/src/main/java/com/example/ui/screens/BookDetailScreen.kt`
- Modificar: `app/src/main/java/com/example/MainActivity.kt` (nova rota)
- Modificar: `app/src/main/java/com/example/ui/screens/NextTabScreen.kt` (`EstanteTab` navega para o detalhe)

- [ ] **Step 1: Criar `BookDetailScreen.kt`**

Protótipo: `BookDetailScreen` em `claude-design/screens-book-detail.jsx`. Uma tela com hero (capa + título + meta), banner de status de leitura, e abas internas Resumo / Frases / Histórico.

Criar `app/src/main/java/com/example/ui/screens/BookDetailScreen.kt` com:
- `@Composable fun BookDetailScreen(viewModel: MainViewModel, bookId: String, onNavigateBack: () -> Unit)`.
- Lê o livro: observar `viewModel.clubBooks` (StateFlow já existente) e achar o `Book` com `id == bookId`; se não achar, mostrar um estado vazio simples ("Livro não encontrado.") com botão voltar.
- Hero: fundo gradiente `OlivaSoft`→`Paper` (use `Brush.verticalGradient`), `Cover` da Fase 1 centralizada (~150x224dp), título em `MaterialTheme.typography.displaySmall` (Literata) centralizado, autor + ano abaixo em `Muted`. Botão voltar circular no topo.
- Banner de status: um card `Oliva` com texto `Cream` tipo "Tu leu este livro" (estático — o app não tem data de término por livro; usar texto genérico).
- **Abas internas** (estado local `var tab by remember`): "Resumo" / "Frases" / "Histórico", indicador `Terracota`.
  - **Resumo:** um parágrafo de resumo. O `Book` não tem campo de sinopse — usar um texto genérico fixo ("Um livro que o clube leu junto.") OU, se preferir, omitir e mostrar só "O que o clube achou" com um `TramabookCard` de texto placeholder. Mantenha simples.
  - **Frases:** observar `viewModel.getSavedQuotesForBook(bookId)` e listar cada `SavedQuote` num `QuoteCard` (ver Task 5 — `QuoteCard` é criado lá; esta Task 4 pode renderizar inline um card simples de citação SE a Task 5 ainda não existir — mas como o plano executa em ordem, a Task 5 vem DEPOIS; então nesta Task 4, na aba Frases, renderize uma lista simples de cards de citação inline; a Task 6 substitui pelo `QuoteCard` compartilhado). Para evitar retrabalho: nesta Task 4, na aba Frases, mostre cada frase num `TramabookCard` com o texto em `LiterataFontFamily` itálico + `capituloRef` embaixo. Botão "Salvar uma frase" → abre um diálogo simples com um `OutlinedTextField` e chama `viewModel.saveQuote(bookId, texto, "Cap.")`.
  - **Histórico:** uma timeline simples e estática (3-4 marcos: "Leitura começou", "Encontro do clube") — o app não rastreia isso; renderizar marcos fixos decorativos.
- Usar componentes da Fase 1 e tokens. Sem `Color(0x...)` hardcoded exceto documentados.

- [ ] **Step 2: Adicionar a rota no `MainActivity`**

Em `MainActivity.kt`, no `NavHost`, adicionar uma `composable` para `"book_detail/{bookId}"`:
```kotlin
                    composable("book_detail/{bookId}") { backStackEntry ->
                        val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
                        BookDetailScreen(
                            viewModel = viewModel,
                            bookId = bookId,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
```
E na `composable("main_tabs")`, o `MainTabsScreen` precisa de um novo callback `onNavigateToBookDetail: (String) -> Unit` que faz `navController.navigate("book_detail/$bookId")`. Adicionar esse parâmetro a `MainTabsScreen` e propagá-lo até `NextTabScreen` → `EstanteTab`.

- [ ] **Step 3: `EstanteTab` navega ao tocar num livro**

Em `NextTabScreen.kt`: `NextTabScreen`, `EstanteTab` recebem um novo parâmetro `onNavigateToBookDetail: (String) -> Unit`. Cada item da grade de livros finalizados fica `clickable { onNavigateToBookDetail(book.id) }`. Propagar o callback de `MainTabsScreen` → `NextTabScreen` → `EstanteTab`.

- [ ] **Step 4: Compilar** — `./gradlew assembleDebug`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/example/ui/screens/BookDetailScreen.kt app/src/main/java/com/example/MainActivity.kt app/src/main/java/com/example/ui/screens/NextTabScreen.kt app/src/main/java/com/example/ui/screens/MainTabsScreen.kt
git commit -m "feat(book): tela de Detalhe do Livro + navegação a partir da Estante"
```

---

## Task 5: Componente `QuoteCard` + tela `FrasesScreen`

**Files:**
- Criar: `app/src/main/java/com/example/ui/components/QuoteCard.kt`
- Criar: `app/src/main/java/com/example/ui/screens/FrasesScreen.kt`
- Modificar: `app/src/main/java/com/example/MainActivity.kt` (nova rota)

- [ ] **Step 1: Criar o componente `QuoteCard`**

Protótipo: `QuoteCard` em `claude-design/screens-book-detail.jsx`. Um card de citação: aspas grandes decorativas, texto da frase em serif itálico, e embaixo o autor/capítulo + um ícone de coração/excluir.

Criar `app/src/main/java/com/example/ui/components/QuoteCard.kt`:
- `@Composable fun QuoteCard(texto: String, ref: String, modifier: Modifier = Modifier, onDelete: (() -> Unit)? = null)`.
- Card `CardSurface` com borda `Divider`, raio 16dp.
- Uma aspa decorativa grande (`"` em `LiterataFontFamily`, ~54sp, cor `OlivaSoft`) no topo.
- O texto da frase em `LiterataFontFamily` itálico, ~16sp, cor `InkSoft`.
- Linha inferior: separador `DividerSoft`, o `ref` em `Muted`, e se `onDelete != null` um `IconButton` com ícone de lixeira/coração que chama `onDelete`.

- [ ] **Step 2: Criar `FrasesScreen.kt`**

Protótipo: `FrasesScreen` em `claude-design/screens-book-detail.jsx`. Lista TODAS as frases guardadas do usuário, agrupadas/rotuladas por livro.

Criar `app/src/main/java/com/example/ui/screens/FrasesScreen.kt`:
- `@Composable fun FrasesScreen(viewModel: MainViewModel, onNavigateBack: () -> Unit)`.
- `Scaffold` com `TopAppBar` "Frases" + botão voltar.
- Observar `viewModel.savedQuotes`. Para cada frase, mostrar o nome do livro como rótulo (resolver via `viewModel.clubBooks` — achar o `Book` por `bookId`, mostrar `book.title`; se não achar, usar o `capituloRef`) e renderizar um `QuoteCard`. Cada `QuoteCard` tem `onDelete = { viewModel.deleteQuote(quote) }`.
- Estado vazio: "Tu ainda não guardou nenhuma frase." centralizado.
- Subtítulo no topo: "As frases que tu guardou. N no total."

- [ ] **Step 3: Adicionar a rota no `MainActivity`**

Em `MainActivity.kt`, adicionar `composable("frases") { FrasesScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() }) }`. O `MainTabsScreen` precisa de um callback `onNavigateToFrases: () -> Unit` → `navController.navigate("frases")`.

- [ ] **Step 4: Compilar** — `./gradlew assembleDebug`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/example/ui/components/QuoteCard.kt app/src/main/java/com/example/ui/screens/FrasesScreen.kt app/src/main/java/com/example/MainActivity.kt app/src/main/java/com/example/ui/screens/MainTabsScreen.kt
git commit -m "feat(frases): componente QuoteCard e tela de Frases guardadas"
```

---

## Task 6: Ligar Frases ao Perfil e ao Detalhe do Livro

**Files:**
- Modificar: `app/src/main/java/com/example/ui/screens/MainTabsScreen.kt` (`ProfileScreenTab`)
- Modificar: `app/src/main/java/com/example/ui/screens/BookDetailScreen.kt`

- [ ] **Step 1: O card "frases guardadas" do Perfil abre a tela de Frases**

Em `ProfileScreenTab` (em `MainTabsScreen.kt`), o terceiro stat card ("frases guardadas", o de fundo `Oliva`): tornar `clickable { onNavigateToFrases() }`. O número exibido deve refletir `viewModel.savedQuotes.collectAsState().value.size` (substituir o número fixo "42" por essa contagem real). Garantir que `onNavigateToFrases` chega até `ProfileScreenTab` (via `MainTabsScreen` → `ProfileScreenTab`).

- [ ] **Step 2: A aba "Frases" do Detalhe do Livro usa `QuoteCard`**

Em `BookDetailScreen.kt`, na aba Frases, substituir os cards de citação inline (criados na Task 4) pelo componente `QuoteCard` da Task 5. Cada `QuoteCard` recebe `onDelete = { viewModel.deleteQuote(quote) }`.

- [ ] **Step 3: Compilar** — `./gradlew assembleDebug`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/example/ui/screens/MainTabsScreen.kt app/src/main/java/com/example/ui/screens/BookDetailScreen.kt
git commit -m "feat(frases): integra Frases ao Perfil e ao Detalhe do Livro"
```

---

## Task 7: applicationId e versionamento para produção

**Files:**
- Modificar: `app/build.gradle.kts`

- [ ] **Step 1: Trocar o `applicationId`**

Em `app/build.gradle.kts`, no bloco `defaultConfig`:
- Mudar `applicationId = "com.aistudio.tramabook.vrtpwb"` para `applicationId = "app.tramabook"`.
- Manter `versionCode = 1` e `versionName = "1.0"` (primeira publicação).
- NÃO mudar o `namespace = "com.example"` — mudar o namespace exigiria mover todos os pacotes Kotlin; o `applicationId` é o que importa para a Play Store e pode diferir do `namespace`.

- [ ] **Step 2: Compilar** — `./gradlew assembleDebug`. Expected: BUILD SUCCESSFUL. O APK terá o novo `applicationId`.

- [ ] **Step 3: Commit**
```bash
git add app/build.gradle.kts
git commit -m "chore(release): applicationId app.tramabook para publicação"
```

---

## Task 8: Ícone do app a partir de `logotramabook.png`

**Files:**
- Modificar: `app/src/main/res/drawable/ic_launcher_background.xml`, `ic_launcher_foreground.xml`
- O `logotramabook.png` está na raiz do projeto — é o balão de fala terracota.

- [ ] **Step 1: Definir o ícone adaptativo**

O ícone adaptativo Android tem duas camadas: background + foreground. O logo do Tramabook é o balão de fala terracota (`logotramabook.png` na raiz, e existe um vetor `app/src/main/res/drawable/ic_logo.xml` com o mesmo desenho).

Abordagem mais robusta e sem depender de ferramentas de bitmap: usar o **vetor** `ic_logo.xml` como base.
- `ic_launcher_background.xml`: um vetor de cor sólida — fundo creme `#FBFAF4` (token `Cream`) OU oliva claro. Usar um `<vector>` ou `<shape>` de cor sólida cobrindo 108x108dp.
- `ic_launcher_foreground.xml`: o balão de fala terracota centralizado. Copiar os `<path>` de `ic_logo.xml` para dentro de um vetor 108x108 com viewport 108x108, posicionando o desenho na "safe zone" central (~66dp dos 108dp). Pode-se envolver os paths num `<group>` com `android:scaleX/scaleY` ~0.62 e `android:translateX/translateY` para centralizar.

`ic_logo.xml` (referência, já no projeto) tem 2 paths terracota num viewport 108x108. O `ic_launcher_foreground.xml` deve conter esses paths escalados para a safe zone.

Os `mipmap-anydpi-v26/ic_launcher.xml` e `ic_launcher_round.xml` já referenciam `@drawable/ic_launcher_background` e `@drawable/ic_launcher_foreground` — não precisam mudar.

- [ ] **Step 2: Compilar** — `./gradlew assembleDebug`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Verificar visualmente (se possível)**

Extrair/inspecionar é difícil sem emulador. Garantir ao menos que os XMLs são `<vector>`/`<adaptive-icon>` válidos e que o build empacota sem erro de recurso. Se houver dúvida sobre o posicionamento, manter o desenho centralizado e conservador (não encostar nas bordas).

- [ ] **Step 4: Commit**
```bash
git add app/src/main/res/
git commit -m "chore(release): ícone adaptativo a partir do logo Tramabook"
```

---

## Task 9: Verificação final da Fase 3

- [ ] **Step 1: Build limpo** — `./gradlew clean assembleDebug`. Expected: BUILD SUCCESSFUL, APK gerado.

- [ ] **Step 2: Todos os testes** — `./gradlew testDebugUnitTest`. Expected: todos PASS.

- [ ] **Step 3: Verificar o applicationId do APK** — rodar `~/.local/opt/android-sdk/build-tools/36.0.0/aapt dump badging app/build/outputs/apk/debug/app-debug.apk | grep package` e confirmar que aparece `name='app.tramabook'`.

- [ ] **Step 4: Estado git** — `git log --oneline -12 && git status --short`. Working tree limpa.

A Fase 3 está completa quando: o app compila, gera APK com `applicationId = app.tramabook`, os testes passam, as telas de Detalhe do Livro e Frases existem e navegam, e as frases persistem no Room.

---

## Notas

- Tarefas 1-3 (dados) são pré-requisito das telas. Tarefas 4-6 (telas) dependem delas. Executar em ordem.
- `BookDetailScreen` na Task 4 renderiza frases inline; a Task 6 troca pelo `QuoteCard`. Isso é deliberado — evita dependência circular entre Task 4 e 5.
- O `MainActivity` é tocado por várias tarefas (4, 5) — executar em ordem, não paralelizar.
- Ícone: a abordagem por vetor (`ic_logo.xml`) evita depender de ferramentas de bitmap no ambiente. Se o resultado ficar ruim, é ajuste de `scale`/`translate` no `ic_launcher_foreground.xml`.
