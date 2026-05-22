# Tramabook — Redesign de UI + Preparação para Publicação

> **Documento de design / fonte de verdade.** Consulte este arquivo sempre que precisar
> lembrar um token, uma regra de UI ou uma decisão de escopo. Ele reflete o protótipo
> em `claude-design/` adaptado para Android nativo.

Data: 2026-05-21
Status: aprovado para implementação

---

## 1. Contexto e objetivo

Tramabook é um **clube de leitura privado**: grupos coordenam o que leem, discutem
capítulo a capítulo (sem spoiler para quem está atrasado), votam no próximo livro e
marcam encontros presenciais.

O app foi gerado pelo Google AI Studio e é **funcional**, mas a UI está aquém. O
protótipo em `claude-design/` (React/JSX) define a UI alvo — mais editorial, "cozy
reading nook". Este trabalho aplica esse visual ao app Android **sem reescrever a
lógica**, adiciona as telas que faltam e prepara o app para publicação na Play Store.

### Escopo desta versão (decisões fechadas)

| Tema | Decisão |
|---|---|
| Foco | Re-skin visual + telas faltantes + ajustes de produção |
| Backend | **Publica offline agora** (Room local + seed). Backend real fica para versão futura — não introduzir dependências de rede além da Open Library já existente |
| Fidelidade | Adaptação **fiel ao protótipo, mas idiomática Android** (Material 3 nativo, ripple, comportamento de teclado nativo) |
| Tema | **Apenas tema claro.** Remover dark mode — o protótipo só tem claro |
| applicationId | Trocar `com.aistudio.tramabook.vrtpwb` → **`app.tramabook`** |
| Ícone | Gerar ícone adaptativo a partir de `logotramabook.png` (raiz do projeto) |
| Seed | **Manter o clube demo** na primeira abertura (app single-device, evita tela vazia) |

### O que NÃO fazer

- Não criar backend, autenticação real ou sincronização.
- Não reescrever `MainViewModel`, Room, ou a navegação. A lógica fica.
- Não introduzir dark mode.
- Não adicionar abstrações/refactors fora do necessário para a UI.

---

## 2. Stack atual (referência)

- Kotlin + Jetpack Compose + Material 3 · `minSdk 24` / `targetSdk 36`
- Room (13 entidades) — banco local, sem backend
- DataStore — sessão e tema
- Retrofit + Moshi — apenas busca na Open Library (sugestão de livros)
- `MainViewModel` (`AndroidViewModel`) — ViewModel único com `StateFlow` para tudo
- Navegação: `NavHost` (onboarding + telas de detalhe) + abas internas via `selectedTab: String` em `MainTabsScreen`
- `seedDatabase()` popula o clube demo e auto-loga `user_voce`

---

## 3. Sistema de design (tokens)

Origem: `claude-design/tokens.jsx`. Estes valores são **a fonte de verdade**. Vivem em
`ui/theme/Color.kt` e são consumidos via `MaterialTheme` + objetos de token.

### 3.1 Cores

**Inversão de hierarquia (mudança central):** o verde-oliva é a cor herói; a terracota
é só acento.

```
Herói / Secundário (oliva)
  secondary       #5C7349   cor herói, RSVP confirmado, progresso de membro
  secondaryDark   #3E5230
  secondaryDeep   #293820   bottom nav, ticket de encontro, hero do Livro atual
  secondarySoft   #E5EBDA
  secondaryMid    #92A57F

Acento / Primário (terracota) — usar com parcimônia
  primary         #B85838   botões de ação, badge "Atual", barra de progresso do usuário
  primaryDark     #8E3F25
  primarySoft     #FBE5DA

Neutro
  tertiary        #5B5B53   texto/ink-gray
  tertiarySoft    #D9D9CF
  ink             #1B1F1A   texto principal
  inkSoft         #383C36
  muted           #8A8A80   texto secundário

Superfícies (claras, frescas)
  paper           #F7F5EE   fundo do app
  paperDeep       #F0EEE5   fundo alternativo (segmented control)
  card            #FFFFFF   superfície de card
  cardSoft        #F9F8F2
  cream           #FBFAF4   ivory (inputs, cards alternativos)
  divider         #E9E7DD
  dividerSoft     #F1EFE6
```

**Mapeamento Material `lightColorScheme`:**
`primary`=terracota · `secondary`=oliva · `background`=paper · `surface`=card ·
`onBackground`/`onSurface`=ink · `onSurfaceVariant`=**tertiary** (`#5B5B53`) · `outline`=divider.

> Nota: `onSurfaceVariant` usa `tertiary`, não `muted`. `muted` (`#8A8A80`) sobre
> `surfaceVariant` falha WCAG AA (3.27:1); `tertiary` dá 6.44:1. Para texto secundário,
> prefira `onSurfaceVariant` (= tertiary). `muted` continua disponível para uso
> decorativo onde contraste de texto não se aplica.

**Cores de clube** (5 presets — `CreateClubScreen`):
```
olive       bg #4F653F   soft #E1E7D7
terracotta  bg #934528   soft #F3DCD0
plum        bg #6E3A52   soft #EBDCE4
mustard     bg #A6802B   soft #F1E3BE
ink         bg #2E3A47   soft #D7DCE2
```
Persistido em `Club.cor` como índice "0".."4". Manter compatibilidade com o mapeamento
antigo já existente em `MainTabsScreen`.

> **Atenção (conflito conhecido — resolver na fase de re-skin de clubes):** o app legado
> em `WelcomeScreen.kt`/`CreateClubScreen` salva `Club.cor` como **hex** (ex.: `"#8C4027"`),
> não como índice, e o seed em `TramabookRepository.kt` usa `"0"`. O helper `clubColorFor()`
> (criado na Fase 1) resolve índice **ou** hex, mas o índice "0" mapeia para oliva
> (`#4F653F`), enquanto o mapeamento legado de `MainTabsScreen` tratava "0" como terracota.
> A fase que re-skina `CreateClubScreen`/`MainTabsScreen` deve unificar: passar a salvar
> sempre o índice "0".."4" e usar `clubColorFor()` em todos os pontos de leitura.

### 3.2 Tipografia

- **Serif: Literata** (era Fraunces). Editorial, combina com leitura. Via Google Fonts.
- **Sans: Inter** (mantém).
- Hierarquia: hero 28sp · títulos de seção 19-22sp · título de card 15-17sp ·
  corpo 13-15sp · overline/label 10-12sp uppercase com letter-spacing.
- Overlines são `Inter` 700 uppercase; títulos editoriais são `Literata` 600.

### 3.3 Forma e elevação

- Raios: 8 / 12 / 14 / 16 / 20 / 24 / 999 (pílula).
- Card padrão: raio 20, borda `0.5dp divider`, sombra suave dupla
  (`0 1px 2px` + `0 4px 14px` em tom quente).
- Botões: pílula (raio 999), altura 46 (md) / 54 (lg).

### 3.4 Componentes base (`ui/components/`)

| Componente | Comportamento |
|---|---|
| `Cover` | Capa de livro. Se `coverUrl` existe → `AsyncImage`. Senão → **bloco colorido gerado por hash do título** (8 paletas + ornamento), com título serif. Espelha `Cover` do `tokens.jsx` |
| `Avatar` | Iniciais sobre cor por hash; suporta anel (ring) |
| `PillButton` | Variantes: `primary`(oliva), `terra`, `outline`, `soft`, `dark`, `oliveSoft`. Pílula |
| `Pill` / chip | Variantes: `default`, `olive`, `terra`, `mustard`, `ink`, `outline`. Uppercase |
| `Progress` | Barra fina arredondada |
| `Card` | Superfície com a sombra/borda padrão |
| `SectionHeader` | Título serif + ação opcional à direita |

---

## 4. Mapa de telas

Lógica (ViewModel/Room/navegação) **permanece**. Só a camada Compose muda.

### 4.1 Telas existentes — re-skin

| Tela | Arquivo | Mudança visual principal |
|---|---|---|
| Welcome | `WelcomeScreen.kt` | Hero "Leituras *juntas*."; seção oliva curvada com ilustração de livros |
| Login/Signup | `WelcomeScreen.kt` | Segmented control Entrar/Criar; campos creme; botão Google |
| Criar clube | `WelcomeScreen.kt` | Picker das 5 cores; seletor de privacidade |
| Entrar clube | `WelcomeScreen.kt` | Code-input de 6 caracteres |
| **Início** | `MainTabsScreen.kt` | Hero = **ticket de encontro perfurado**; strip de leitura compacto; "Onde a galera tá" (avatares + status pill); "No clube hoje" |
| **Livro atual** | `MainTabsScreen.kt` | Hero oliva curvado com capa flutuante; card de progresso sobreposto com anel cônico; lista de capítulos |
| Discussão | `DiscussionScreen.kt` | Bolhas de comentário (próprio = terracota soft); pill "tu já passou daqui"; input arredondado |
| **Próximo** | `NextTabScreen.kt` | 3 sub-abas: Encontro · Votação · Estante |
| Perfil | `MainTabsScreen.kt` | 3 stat cards (1 oliva); lista de clubes; sair |
| Notificações | `NotificationsScreen.kt` | Agrupado HOJE / ONTEM / ESTA SEMANA |
| Sugerir livro | `SuggestScreen.kt` | Busca; modal "por que esse livro?" |
| Bottom nav | `MainTabsScreen.kt` | **Pílula oliva-escura flutuante**; label só no item ativo |

### 4.2 Telas novas (faltam no app)

| Tela | Origem no design | Dados |
|---|---|---|
| **Estante** | `ShelfTab` em `screens-next.jsx` | Grid 2 col de capas + filtros. Consome `finishedBooks` (já existe no ViewModel) |
| **Detalhe do livro** | `screens-book-detail.jsx` | Abre ao tocar num livro da estante. Resumo / Frases / Histórico |
| **Frases guardadas** | `FrasesScreen` no design | Citações salvas pelo usuário |

### 4.3 Mudança de dados necessária

"Frases guardadas" **não existe** no Room hoje. Requer entidade nova:

```
SavedQuote(id, userId, bookId, clubId, texto, capituloRef, criadoEm)
```
+ DAO + métodos no repositório + `StateFlow` no ViewModel. Incrementar a `version` do
`AppDatabase` (já usa `fallbackToDestructiveMigration` — aceitável para app local sem
backend; dados de seed serão repovoados).

---

## 5. Regras de UI (checklist permanente)

Aplicar em **toda** tela nova ou re-skinada:

1. **Oliva é herói, terracota é acento.** Terracota só em: botão de ação primário,
   badge "Atual", barra de progresso do usuário, número de notificações. Nunca como
   cor de fundo dominante.
2. **Serif (Literata) para conteúdo editorial** (títulos de livro, nomes de clube,
   citações, números de destaque). **Sans (Inter) para UI** (labels, botões, corpo).
3. **Overlines** sempre Inter 700 uppercase com letter-spacing ~1.
4. **Cards** usam o raio 20 + borda 0.5dp + sombra quente suave. Nada de elevação dura.
5. **Botões** são pílulas. Ação primária = oliva ou terracota conforme contexto.
6. **Capas de livro** usam `Cover`: imagem quando há URL, bloco gerado por hash quando
   não há. Nunca mostrar placeholder "No cover" cru.
7. **Idiomático Android:** usar `ripple`, `ModalBottomSheet`, `Scaffold`, insets de
   sistema (`windowInsetsPadding`). Reproduzir a aparência do protótipo sem copiar
   comportamentos web.
8. **Só tema claro.** Não referenciar `isSystemInDarkTheme()` para alternar esquema.
9. **Textos em pt-BR**, tom informal/intimista do protótipo ("tu", "a galera",
   "Onde tu tá?").
10. **Espaçamentos** seguem o protótipo: padding horizontal de tela ~22dp; gap entre
    seções ~18-20dp.

---

## 6. Ordem de implementação sugerida

1. **Fundação** — tokens (`Color.kt`, `Theme.kt`), tipografia (`Type.kt` → Literata),
   componentes base (`Cover`, `Avatar`, `PillButton`, `Pill`, `Card`). Remover dark mode.
2. **Onboarding** — Welcome, Login, Criar/Entrar clube.
3. **Fluxo principal** — Início, Livro atual, bottom nav, Perfil.
4. **Discussão e Próximo** — Discussão de capítulo, sub-abas Encontro/Votação.
5. **Telas novas** — entidade `SavedQuote`; Estante, Detalhe do livro, Frases.
6. **Produção** — `applicationId` `app.tramabook`, ícone adaptativo de
   `logotramabook.png`, `versionCode`/nome, revisar `signingConfig`, limpar warnings.

Cada etapa deve deixar o app compilando e navegável.

---

## 7. Referências

- Protótipo alvo: `claude-design/` (abrir `Tramabook.html` num browser para ver)
- Tokens: `claude-design/tokens.jsx`
- Telas: `claude-design/screens-*.jsx`, `shell.jsx`
- Logo: `logotramabook.png` (raiz)
- App atual: `app/src/main/java/com/example/`
