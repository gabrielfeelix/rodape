# Fase 1 — Fundação do Design System: Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Substituir a camada de tema do Tramabook (cores, tipografia, componentes base) pelos tokens do protótipo `claude-design/`, deixando o app pronto para o re-skin das telas.

**Architecture:** Reescrever `ui/theme/` (Color, Type, Theme) com os tokens de `tokens.jsx`; criar um objeto `TramabookTokens` para valores que não cabem no `ColorScheme` do Material; expandir `ui/components/` com `Cover`, `Avatar`, `PillButton`, `Pill`, `Progress`, `Card` espelhando o protótipo. Nenhuma tela é alterada nesta fase — só a fundação. O app continua compilando e navegável a cada tarefa.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Google Fonts (Literata + Inter), Roborazzi (screenshot tests).

---

## Contexto para quem nunca viu o projeto

- O Tramabook é um app Android de clube de leitura. O código está em `app/src/main/java/com/example/`.
- A UI alvo é o protótipo React em `claude-design/` (abrir `Tramabook.html` num browser para ver). Os tokens estão em `claude-design/tokens.jsx`.
- O spec completo está em `docs/superpowers/specs/2026-05-21-tramabook-redesign-design.md` — **leia a seção 3 (tokens) e a seção 5 (regras de UI) antes de começar.**
- **Mudança central:** o verde-oliva vira a cor herói; a terracota é só acento. No Material isso vira `secondary`=oliva, `primary`=terracota.
- **Só tema claro.** O dark mode é removido nesta fase.
- Build: `cd ~/dev/tramabook && ./gradlew assembleDebug`. As variáveis de ambiente (`JAVA_HOME`, `ANDROID_HOME`) já estão no `~/.bashrc`.
- Testes de screenshot: `./gradlew testDebugUnitTest`. O projeto usa Roborazzi; `GreetingScreenshotTest.kt` é o exemplo existente.

## Arquivos desta fase

| Arquivo | Responsabilidade | Ação |
|---|---|---|
| `app/src/main/java/com/example/ui/theme/Color.kt` | Todos os tokens de cor do protótipo | Reescrever |
| `app/src/main/java/com/example/ui/theme/Type.kt` | Fontes (Literata serif + Inter sans) e `Typography` | Reescrever |
| `app/src/main/java/com/example/ui/theme/Theme.kt` | `ColorScheme` claro + `MyApplicationTheme` | Reescrever |
| `app/src/main/java/com/example/ui/theme/Tokens.kt` | Objeto `TramabookTokens` (cores fora do `ColorScheme`) + cores de clube | Criar |
| `app/src/main/java/com/example/ui/components/Cover.kt` | Capa de livro (imagem ou bloco gerado por hash) | Criar |
| `app/src/main/java/com/example/ui/components/Avatar.kt` | Avatar de iniciais por hash | Criar |
| `app/src/main/java/com/example/ui/components/Buttons.kt` | `PillButton` com variantes | Criar |
| `app/src/main/java/com/example/ui/components/Chips.kt` | `Pill`/chip + `ProgressBar` | Criar |
| `app/src/main/java/com/example/ui/components/Cards.kt` | `TramabookCard` + `SectionHeader` | Criar |
| `app/src/main/java/com/example/ui/components/CommonComponents.kt` | Componentes legados (`BookCover`, `MemberAvatar` etc.) | Manter intacto nesta fase |
| `app/src/test/java/com/example/theme/FoundationScreenshotTest.kt` | Screenshot test que renderiza os componentes novos | Criar |

**Por que manter `CommonComponents.kt` intacto:** as telas atuais ainda o usam. Trocá-las pelos componentes novos é trabalho das fases seguintes. Remover ou alterar `CommonComponents.kt` agora quebraria a compilação das telas.

---

## Task 1: Tokens de cor

**Files:**
- Reescrever: `app/src/main/java/com/example/ui/theme/Color.kt`
- Criar: `app/src/main/java/com/example/ui/theme/Tokens.kt`

- [ ] **Step 1: Reescrever `Color.kt` com os tokens do protótipo**

Substituir todo o conteúdo de `app/src/main/java/com/example/ui/theme/Color.kt` por:

```kotlin
package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ── Tokens de cor — origem: claude-design/tokens.jsx ──
// Oliva é a cor herói; terracota é só acento.

// Acento / Primário (terracota) — usar com parcimônia
val Terracota = Color(0xFFB85838)
val TerracotaDark = Color(0xFF8E3F25)
val TerracotaSoft = Color(0xFFFBE5DA)

// Herói / Secundário (oliva)
val Oliva = Color(0xFF5C7349)
val OlivaDark = Color(0xFF3E5230)
val OlivaDeep = Color(0xFF293820)
val OlivaSoft = Color(0xFFE5EBDA)
val OlivaMid = Color(0xFF92A57F)

// Neutro
val Tertiary = Color(0xFF5B5B53)
val TertiarySoft = Color(0xFFD9D9CF)
val Ink = Color(0xFF1B1F1A)
val InkSoft = Color(0xFF383C36)
val Muted = Color(0xFF8A8A80)

// Superfícies
val Paper = Color(0xFFF7F5EE)
val PaperDeep = Color(0xFFF0EEE5)
val CardSurface = Color(0xFFFFFFFF)
val CardSoft = Color(0xFFF9F8F2)
val Cream = Color(0xFFFBFAF4)
val Divider = Color(0xFFE9E7DD)
val DividerSoft = Color(0xFFF1EFE6)

// Compat: nomes antigos ainda referenciados pelas telas atuais.
// Serão removidos quando as telas forem re-skinadas nas fases seguintes.
val VerdeMusgo = Oliva
```

Nota: `VerdeMusgo` é mantido como alias de `Oliva` porque `MainTabsScreen.kt`, `NextTabScreen.kt` etc. ainda o importam. Sem ele a compilação quebra. Será removido na fase de re-skin de cada tela.

- [ ] **Step 2: Criar `Tokens.kt` com cores fora do ColorScheme e cores de clube**

Criar `app/src/main/java/com/example/ui/theme/Tokens.kt`:

```kotlin
package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Tokens que não cabem no ColorScheme do Material 3.
 * Acesse via `TramabookTokens` direto (são estáticos, tema único claro).
 */
object TramabookTokens {
    val terracotaDark = TerracotaDark
    val terracotaSoft = TerracotaSoft
    val olivaDark = OlivaDark
    val olivaDeep = OlivaDeep
    val olivaSoft = OlivaSoft
    val olivaMid = OlivaMid
    val tertiary = Tertiary
    val tertiarySoft = TertiarySoft
    val inkSoft = InkSoft
    val paperDeep = PaperDeep
    val cardSoft = CardSoft
    val cream = Cream
    val dividerSoft = DividerSoft
}

/** Cor de clube — presets do protótipo (CreateClubScreen). */
data class ClubColor(val id: String, val bg: Color, val soft: Color, val ink: Color)

/** Os 5 presets na ordem do protótipo. Índice "0".."4" é o que fica em Club.cor. */
val ClubColors = listOf(
    ClubColor("olive", Color(0xFF4F653F), Color(0xFFE1E7D7), Color.White),
    ClubColor("terracotta", Color(0xFF934528), Color(0xFFF3DCD0), Color.White),
    ClubColor("plum", Color(0xFF6E3A52), Color(0xFFEBDCE4), Color.White),
    ClubColor("mustard", Color(0xFFA6802B), Color(0xFFF1E3BE), Color.White),
    ClubColor("ink", Color(0xFF2E3A47), Color(0xFFD7DCE2), Color.White),
)

/** Resolve a cor do clube a partir do valor salvo em Club.cor (índice ou hex). */
fun clubColorFor(cor: String): ClubColor {
    cor.toIntOrNull()?.let { idx ->
        if (idx in ClubColors.indices) return ClubColors[idx]
    }
    return runCatching { Color(android.graphics.Color.parseColor(cor)) }
        .map { ClubColor("custom", it, OlivaSoft, Color.White) }
        .getOrDefault(ClubColors[0])
}
```

- [ ] **Step 3: Compilar para validar**

Run: `cd ~/dev/tramabook && ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL. (As telas ainda usam `Terracota` e `VerdeMusgo`, ambos definidos — compila.)

- [ ] **Step 4: Commit**

```bash
cd ~/dev/tramabook
git add app/src/main/java/com/example/ui/theme/Color.kt app/src/main/java/com/example/ui/theme/Tokens.kt
git commit -m "feat(theme): tokens de cor do protótipo + cores de clube"
```

---

## Task 2: Tipografia (Literata + Inter)

**Files:**
- Reescrever: `app/src/main/java/com/example/ui/theme/Type.kt`

- [ ] **Step 1: Reescrever `Type.kt` trocando Fraunces por Literata**

A fonte serif passa de Fraunces para Literata (via Google Fonts). Inter permanece. Substituir todo o conteúdo de `app/src/main/java/com/example/ui/theme/Type.kt` por:

```kotlin
package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.R

val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// Serif editorial — origem: claude-design usa Literata.
val LiterataFont = GoogleFont("Literata")
// Sans para UI.
val InterFont = GoogleFont("Inter")

val LiterataFontFamily = FontFamily(
    Font(googleFont = LiterataFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = LiterataFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = LiterataFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = LiterataFont, fontProvider = fontProvider, weight = FontWeight.Bold),
    Font(googleFont = LiterataFont, fontProvider = fontProvider, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(googleFont = LiterataFont, fontProvider = fontProvider, weight = FontWeight.SemiBold, style = FontStyle.Italic),
)

val InterFontFamily = FontFamily(
    Font(googleFont = InterFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = InterFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = InterFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = InterFont, fontProvider = fontProvider, weight = FontWeight.Bold),
)

// Alias de compatibilidade: telas atuais importam FrauncesFontFamily.
// Aponta para Literata para não quebrar a compilação; removido no re-skin.
val FrauncesFontFamily = LiterataFontFamily

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = LiterataFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.8).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = LiterataFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.7).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = LiterataFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.4).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = LiterataFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = LiterataFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp, lineHeight = 24.sp, letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = LiterataFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.3).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = LiterataFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = (-0.2).sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 19.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp, lineHeight = 17.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 1.sp,
    ),
)
```

Notas:
- `FrauncesFontFamily` vira alias de `LiterataFontFamily` — `MainTabsScreen.kt` e outras telas importam esse nome. Sem o alias, quebra.
- `labelSmall` agora é o estilo de overline (10sp, bold, uppercase espaçado). As telas atuais usam `labelSmall` em alguns lugares como texto de 13sp; isso muda a aparência levemente até o re-skin, mas não quebra build.

- [ ] **Step 2: Compilar para validar**

Run: `cd ~/dev/tramabook && ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
cd ~/dev/tramabook
git add app/src/main/java/com/example/ui/theme/Type.kt
git commit -m "feat(theme): troca fonte serif para Literata e ajusta escala tipográfica"
```

---

## Task 3: Tema claro único (remover dark mode)

**Files:**
- Reescrever: `app/src/main/java/com/example/ui/theme/Theme.kt`
- Modificar: `app/src/main/java/com/example/MainActivity.kt:30-37`

- [ ] **Step 1: Reescrever `Theme.kt` com apenas o esquema claro**

Substituir todo o conteúdo de `app/src/main/java/com/example/ui/theme/Theme.kt` por:

```kotlin
package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Tema único claro. Oliva é herói (secondary), terracota é acento (primary).
private val TramabookColorScheme = lightColorScheme(
    primary = Terracota,
    onPrimary = Color.White,
    primaryContainer = TerracotaSoft,
    onPrimaryContainer = TerracotaDark,
    secondary = Oliva,
    onSecondary = Color.White,
    secondaryContainer = OlivaSoft,
    onSecondaryContainer = OlivaDark,
    background = Paper,
    onBackground = Ink,
    surface = CardSurface,
    onSurface = Ink,
    surfaceVariant = CardSoft,
    onSurfaceVariant = Muted,
    outline = Divider,
    outlineVariant = DividerSoft,
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = TramabookColorScheme,
        typography = Typography,
        content = content,
    )
}
```

Nota: `MyApplicationTheme` perde o parâmetro `darkTheme`. O `MainActivity` precisa parar de passá-lo (próximo step).

- [ ] **Step 2: Remover a lógica de dark mode do `MainActivity`**

Em `app/src/main/java/com/example/MainActivity.kt`, o bloco atual (linhas ~30-41) é:

```kotlin
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> false // Padrão reconhecido como claro por padrão, não escuro
            }

            MyApplicationTheme(darkTheme = darkTheme) {
```

Substituir esse trecho por:

```kotlin
        setContent {
            MyApplicationTheme {
```

Remover também o import agora não usado, se o linter reclamar: `import androidx.compose.foundation.isSystemInDarkTheme` (linha 8). Manter `collectAsState` e `getValue` — ainda são usados por `currentUserId`.

- [ ] **Step 3: Compilar para validar**

Run: `cd ~/dev/tramabook && ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

Se houver erro `unresolved reference: themeMode` em outra tela (ex.: `ProfileScreenTab` tem um seletor de tema), envolver o uso com TODO ou deixar — `viewModel.themeMode` e `updateThemeMode` ainda existem no `MainViewModel`, então só o `MainActivity` muda. Não apague nada do ViewModel nesta fase.

- [ ] **Step 4: Build completo de APK**

Run: `cd ~/dev/tramabook && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL, APK em `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 5: Commit**

```bash
cd ~/dev/tramabook
git add app/src/main/java/com/example/ui/theme/Theme.kt app/src/main/java/com/example/MainActivity.kt
git commit -m "feat(theme): tema claro único com paleta oliva/terracota, remove dark mode"
```

---

## Task 4: Componente `Cover` (capa de livro)

**Files:**
- Criar: `app/src/main/java/com/example/ui/components/Cover.kt`

- [ ] **Step 1: Criar `Cover.kt`**

Capa de livro: usa `AsyncImage` quando há URL; senão renderiza um bloco colorido determinístico (paleta escolhida por hash do título), com o título em serif. Espelha o `Cover` de `tokens.jsx`.

Criar `app/src/main/java/com/example/ui/components/Cover.kt`:

```kotlin
package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.LiterataFontFamily

private data class CoverPalette(val bg: Color, val fg: Color)

// 8 paletas — origem: claude-design/tokens.jsx (Cover).
private val coverPalettes = listOf(
    CoverPalette(Color(0xFF3E5230), Color(0xFFF0EEE5)),
    CoverPalette(Color(0xFFB85838), Color(0xFFFBFAF4)),
    CoverPalette(Color(0xFF1F2A1A), Color(0xFFE5EBDA)),
    CoverPalette(Color(0xFF7A4F2B), Color(0xFFFBFAF4)),
    CoverPalette(Color(0xFFD8C9B0), Color(0xFF3E5230)),
    CoverPalette(Color(0xFF5C7349), Color(0xFFFBFAF4)),
    CoverPalette(Color(0xFFE5EBDA), Color(0xFF3E5230)),
    CoverPalette(Color(0xFFFBE5DA), Color(0xFF8E3F25)),
)

private fun hashOf(text: String): Int = text.sumOf { it.code }

@Composable
fun Cover(
    title: String,
    author: String,
    coverUrl: String = "",
    modifier: Modifier = Modifier,
    width: Dp = 92.dp,
    height: Dp = 138.dp,
) {
    val shape = RoundedCornerShape(4.dp)
    val box = modifier
        .width(width)
        .height(height)
        .shadow(elevation = 8.dp, shape = shape, clip = false)
        .clip(shape)

    if (coverUrl.isNotEmpty()) {
        AsyncImage(
            model = coverUrl,
            contentDescription = "Capa de $title",
            modifier = box,
            contentScale = ContentScale.Crop,
        )
        return
    }

    val palette = coverPalettes[hashOf(title).mod(coverPalettes.size)]
    Box(modifier = box.background(palette.bg)) {
        Column(modifier = Modifier.fillMaxSize().padding(width * 0.09f)) {
            Text(
                text = title,
                color = palette.fg,
                fontFamily = LiterataFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = (width.value / maxOf(8, title.length) * 1.4f)
                    .coerceIn(9f, 15f).sp,
                lineHeight = (width.value / maxOf(8, title.length) * 1.5f)
                    .coerceIn(11f, 17f).sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            )
            Box(Modifier.weight(1f))
            Text(
                text = author.uppercase(),
                color = palette.fg.copy(alpha = 0.75f),
                fontSize = 7.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
```

Nota: ornamentos do protótipo (círculos/linhas decorativas) foram omitidos de propósito — adicionam complexidade sem valor funcional. O bloco colorido com título serif já entrega o efeito. Manter simples (YAGNI).

- [ ] **Step 2: Compilar para validar**

Run: `cd ~/dev/tramabook && ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
cd ~/dev/tramabook
git add app/src/main/java/com/example/ui/components/Cover.kt
git commit -m "feat(components): Cover com fallback de bloco gerado por hash"
```

---

## Task 5: Componente `Avatar`

**Files:**
- Criar: `app/src/main/java/com/example/ui/components/Avatar.kt`

- [ ] **Step 1: Criar `Avatar.kt`**

Avatar circular: usa `AsyncImage` quando há URL; senão iniciais sobre cor por hash do nome. Espelha `Avatar` de `tokens.jsx`. Suporta anel opcional.

Criar `app/src/main/java/com/example/ui/components/Avatar.kt`:

```kotlin
package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// 8 cores — origem: claude-design/tokens.jsx (Avatar).
private val avatarColors = listOf(
    Color(0xFF5C7349), Color(0xFFB85838), Color(0xFF3E5230), Color(0xFF7A4F2B),
    Color(0xFF1F2A1A), Color(0xFF92A57F), Color(0xFF8E3F25), Color(0xFF5B5B53),
)

private fun initialsOf(name: String): String =
    name.trim().split(" ").filter { it.isNotEmpty() }
        .take(2).joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }

@Composable
fun Avatar(
    name: String,
    modifier: Modifier = Modifier,
    avatarUrl: String = "",
    size: Dp = 40.dp,
    ring: Color? = null,
) {
    val base = modifier.size(size).clip(CircleShape)
    val ringed = if (ring != null) base.border(2.dp, ring, CircleShape) else base

    if (avatarUrl.isNotEmpty()) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = "Avatar de $name",
            modifier = ringed,
            contentScale = ContentScale.Crop,
        )
        return
    }

    val bg = avatarColors[name.sumOf { it.code }.mod(avatarColors.size)]
    Box(
        modifier = ringed.background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initialsOf(name),
            color = Color(0xFFFBF6EC),
            fontSize = (size.value * 0.4f).sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
```

- [ ] **Step 2: Compilar para validar**

Run: `cd ~/dev/tramabook && ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
cd ~/dev/tramabook
git add app/src/main/java/com/example/ui/components/Avatar.kt
git commit -m "feat(components): Avatar de iniciais por hash"
```

---

## Task 6: Componente `PillButton`

**Files:**
- Criar: `app/src/main/java/com/example/ui/components/Buttons.kt`

- [ ] **Step 1: Criar `Buttons.kt`**

`PillButton` é um botão em forma de pílula com variantes. Espelha `PillButton` de `tokens.jsx`. Nome distinto de `PillButton` legado (em `CommonComponents.kt`): este vive em `com.example.ui.components` no arquivo `Buttons.kt` mas tem assinatura diferente — para evitar colisão de overload ambíguo, este chama-se `TbButton`.

Criar `app/src/main/java/com/example/ui/components/Buttons.kt`:

```kotlin
package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Cream
import com.example.ui.theme.Divider
import com.example.ui.theme.Ink
import com.example.ui.theme.Oliva
import com.example.ui.theme.OlivaDark
import com.example.ui.theme.OlivaSoft
import com.example.ui.theme.Terracota
import com.example.ui.theme.TerracotaDark
import com.example.ui.theme.TerracotaSoft

enum class TbButtonVariant { Primary, Terra, Outline, Soft, Dark, OliveSoft }
enum class TbButtonSize { Sm, Md, Lg }

private data class ButtonStyle(val bg: Color, val fg: Color, val border: BorderStroke?)

@Composable
fun TbButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: TbButtonVariant = TbButtonVariant.Primary,
    size: TbButtonSize = TbButtonSize.Md,
    enabled: Boolean = true,
) {
    val style = when (variant) {
        TbButtonVariant.Primary -> ButtonStyle(Oliva, Cream, null)
        TbButtonVariant.Terra -> ButtonStyle(Terracota, Cream, null)
        TbButtonVariant.Outline -> ButtonStyle(Color.White, Ink, BorderStroke(1.dp, Divider))
        TbButtonVariant.Soft -> ButtonStyle(TerracotaSoft, TerracotaDark, null)
        TbButtonVariant.Dark -> ButtonStyle(Ink, Cream, null)
        TbButtonVariant.OliveSoft -> ButtonStyle(OlivaSoft, OlivaDark, null)
    }
    val height = when (size) {
        TbButtonSize.Sm -> 32.dp
        TbButtonSize.Md -> 46.dp
        TbButtonSize.Lg -> 54.dp
    }
    val fontSize = when (size) {
        TbButtonSize.Sm -> 13
        TbButtonSize.Md -> 15
        TbButtonSize.Lg -> 16
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(999.dp),
        border = style.border,
        contentPadding = PaddingValues(horizontal = 22.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = style.bg,
            contentColor = style.fg,
            disabledContainerColor = Color(0xFFD8C9B8),
            disabledContentColor = Cream,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = fontSize.sp,
            ),
        )
    }
}
```

Adicionar o import faltante no topo se o compilador pedir: `import androidx.compose.ui.unit.sp`.

- [ ] **Step 2: Compilar para validar**

Run: `cd ~/dev/tramabook && ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
cd ~/dev/tramabook
git add app/src/main/java/com/example/ui/components/Buttons.kt
git commit -m "feat(components): TbButton com variantes em pílula"
```

---

## Task 7: Componentes `Pill` (chip) e `ProgressBar`

**Files:**
- Criar: `app/src/main/java/com/example/ui/components/Chips.kt`

- [ ] **Step 1: Criar `Chips.kt`**

`Pill` é um chip/tag pequeno uppercase. `ProgressBar` é uma barra fina arredondada. Espelham `Pill` e `Progress` de `tokens.jsx`.

Criar `app/src/main/java/com/example/ui/components/Chips.kt`:

```kotlin
package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardSoft
import com.example.ui.theme.Cream
import com.example.ui.theme.Divider
import com.example.ui.theme.DividerSoft
import com.example.ui.theme.Ink
import com.example.ui.theme.Oliva
import com.example.ui.theme.OlivaDark
import com.example.ui.theme.OlivaSoft
import com.example.ui.theme.Tertiary
import com.example.ui.theme.TerracotaDark
import com.example.ui.theme.TerracotaSoft

enum class PillVariant { Default, Olive, OliveDeep, Terra, Mustard, Ink, Outline }

private data class PillStyle(val bg: Color, val fg: Color, val border: Color?)

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    variant: PillVariant = PillVariant.Default,
) {
    val style = when (variant) {
        PillVariant.Default -> PillStyle(CardSoft, Tertiary, Divider)
        PillVariant.Olive -> PillStyle(OlivaSoft, OlivaDark, null)
        PillVariant.OliveDeep -> PillStyle(Oliva, Cream, null)
        PillVariant.Terra -> PillStyle(TerracotaSoft, TerracotaDark, null)
        PillVariant.Mustard -> PillStyle(Color(0xFFF1E3BE), Color(0xFF6E5316), null)
        PillVariant.Ink -> PillStyle(Ink, Cream, null)
        PillVariant.Outline -> PillStyle(Color.Transparent, Tertiary, Divider)
    }
    val base = modifier.clip(RoundedCornerShape(999.dp)).background(style.bg)
    val bordered = if (style.border != null) {
        base.border(1.dp, style.border, RoundedCornerShape(999.dp))
    } else base

    Box(bordered.padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(
            text = text.uppercase(),
            color = style.fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp,
        )
    }
}

@Composable
fun ProgressBar(
    value: Float,
    modifier: Modifier = Modifier,
    color: Color = Oliva,
    track: Color = DividerSoft,
    height: Dp = 6.dp,
) {
    val clamped = value.coerceIn(0f, 1f)
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(999.dp))
            .background(track),
    ) {
        Box(
            Modifier
                .fillMaxWidth(clamped)
                .height(height)
                .clip(RoundedCornerShape(999.dp))
                .background(color),
        )
    }
}
```

- [ ] **Step 2: Compilar para validar**

Run: `cd ~/dev/tramabook && ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
cd ~/dev/tramabook
git add app/src/main/java/com/example/ui/components/Chips.kt
git commit -m "feat(components): Pill (chip) e ProgressBar"
```

---

## Task 8: Componentes `TramabookCard` e `SectionHeader`

**Files:**
- Criar: `app/src/main/java/com/example/ui/components/Cards.kt`

- [ ] **Step 1: Criar `Cards.kt`**

`TramabookCard` é a superfície de card padrão (raio 20, borda fina, sombra suave). `SectionHeader` é título de seção serif com ação opcional. Espelham `Card` e `SectionHeader` de `shell.jsx`.

Criar `app/src/main/java/com/example/ui/components/Cards.kt`:

```kotlin
package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CardSurface
import com.example.ui.theme.Divider
import com.example.ui.theme.Ink

@Composable
fun TramabookCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardSurface,
            contentColor = Ink,
        ),
        border = BorderStroke(0.5.dp, Divider),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
        )
        action?.invoke()
    }
}
```

Nota: `SectionHeader` colide em nome com o `SectionHeader` legado de `CommonComponents.kt`. Os dois ficam no mesmo package `com.example.ui.components` → **erro de redeclaração**. Resolver renomeando o novo para `TbSectionHeader`. Atualizar o `fun SectionHeader` acima para `fun TbSectionHeader`.

- [ ] **Step 2: Compilar para validar**

Run: `cd ~/dev/tramabook && ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL. Se aparecer `conflicting declarations: SectionHeader`, confirme que renomeou para `TbSectionHeader` no step 1.

- [ ] **Step 3: Commit**

```bash
cd ~/dev/tramabook
git add app/src/main/java/com/example/ui/components/Cards.kt
git commit -m "feat(components): TramabookCard e TbSectionHeader"
```

---

## Task 9: Screenshot test da fundação

**Files:**
- Criar: `app/src/test/java/com/example/theme/FoundationScreenshotTest.kt`

- [ ] **Step 1: Criar o screenshot test**

Renderiza os componentes novos juntos sob `MyApplicationTheme` e captura uma imagem. Serve como verificação visual e como prova de que nada crasha ao compor.

Criar `app/src/test/java/com/example/theme/FoundationScreenshotTest.kt`:

```kotlin
package com.example.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.ui.components.Avatar
import com.example.ui.components.Cover
import com.example.ui.components.Pill
import com.example.ui.components.PillVariant
import com.example.ui.components.ProgressBar
import com.example.ui.components.TbButton
import com.example.ui.components.TbButtonVariant
import com.example.ui.components.TbSectionHeader
import com.example.ui.components.TramabookCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import androidx.compose.ui.Modifier

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class FoundationScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun foundation_components_render() {
        composeTestRule.setContent {
            MyApplicationTheme {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TbSectionHeader(title = "Componentes")
                    TbButton(text = "Criar um clube", onClick = {}, variant = TbButtonVariant.Primary)
                    TbButton(text = "Sugerir livro", onClick = {}, variant = TbButtonVariant.Outline)
                    Pill(text = "Atual", variant = PillVariant.Terra)
                    ProgressBar(value = 0.62f)
                    Avatar(name = "Beatriz Almeida", size = 56.dp)
                    Cover(title = "A Hora da Estrela", author = "Clarice Lispector")
                    TramabookCard {
                        Pill(text = "olive", variant = PillVariant.Olive)
                    }
                }
            }
        }
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "src/test/screenshots/foundation.png")
    }
}
```

- [ ] **Step 2: Rodar o teste**

Run: `cd ~/dev/tramabook && ./gradlew testDebugUnitTest --tests "com.example.theme.FoundationScreenshotTest"`
Expected: PASS. Gera `app/src/test/screenshots/foundation.png`.

Se falhar por fonte do Google Fonts não resolver no ambiente Robolectric, o teste ainda deve renderizar com fonte fallback — o objetivo é provar que compõe sem crash. Se crashar por outro motivo, corrigir o componente apontado no stacktrace antes de prosseguir.

- [ ] **Step 3: Inspecionar o screenshot gerado**

Abrir `app/src/test/screenshots/foundation.png` e conferir visualmente: botão oliva, botão outline, pill terracota, barra de progresso ~62%, avatar com iniciais "BA", capa "A Hora da Estrela" como bloco colorido. Se algo estiver claramente errado (cor invertida, texto cortado), corrigir o componente.

- [ ] **Step 4: Commit**

```bash
cd ~/dev/tramabook
git add app/src/test/java/com/example/theme/FoundationScreenshotTest.kt app/src/test/screenshots/foundation.png
git commit -m "test(theme): screenshot test dos componentes da fundação"
```

---

## Task 10: Verificação final da fase

- [ ] **Step 1: Build completo limpo**

Run: `cd ~/dev/tramabook && ./gradlew clean assembleDebug`
Expected: BUILD SUCCESSFUL, APK gerado.

- [ ] **Step 2: Rodar todos os testes unitários**

Run: `cd ~/dev/tramabook && ./gradlew testDebugUnitTest`
Expected: todos PASS (inclui `GreetingScreenshotTest`, `FoundationScreenshotTest`, `ExampleUnitTest`, `ExampleRobolectricTest`).

- [ ] **Step 3: Confirmar estado do git**

Run: `cd ~/dev/tramabook && git log --oneline -10 && git status --short`
Expected: ~9 commits novos da fase, working tree limpa.

A Fase 1 está completa quando: o app compila, gera APK, os testes passam, e os componentes base existem em `ui/components/`. As telas ainda não usam os componentes novos — isso é a Fase 2.

---

## Notas de continuidade para as próximas fases

- Os aliases de compatibilidade (`VerdeMusgo` em `Color.kt`, `FrauncesFontFamily` em `Type.kt`) existem só para não quebrar as telas atuais. Cada fase de re-skin de tela deve trocar o uso pelo token correto e, ao final, esses aliases podem ser removidos.
- `CommonComponents.kt` (com `BookCover`, `MemberAvatar`, `PillButton` legado, `SectionHeader` legado, `StandardCard`) permanece. Conforme as telas migrarem para `Cover`/`Avatar`/`TbButton`/`TbSectionHeader`/`TramabookCard`, o arquivo legado encolhe até poder ser deletado.
- `MainViewModel` ainda tem `themeMode`/`updateThemeMode` sem uso após esta fase. Não remover agora — a Fase de re-skin do Perfil decide se o seletor de tema sai da UI; aí sim limpa o ViewModel.
