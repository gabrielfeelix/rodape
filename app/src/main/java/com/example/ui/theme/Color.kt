package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ── Tokens de cor — origem: claude-design/tokens.jsx ──
// Oliva é a cor herói; terracota é só acento.
//
// Duas paletas: CLARA (original) e ESCURA (dark mode). Os call sites NÃO usam
// estas constantes cruas direto pra cor de UI — usam `RodapeTheme.colors.<token>`
// (ver RodapeColors.kt), que resolve por tema. Estas constantes são só a FONTE
// dos dois RodapeColors (claro/escuro) e de casos fixos de marca (ClubColors).

// ════════════════════════════════════════════════════════════════════════════
// PALETA CLARA (tema padrão)
// ════════════════════════════════════════════════════════════════════════════

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

// Dourado — estrelas de rating e dots de destaque (tokens.jsx usa #E6BF6B inline)
val Dourado = Color(0xFFE6BF6B)

// Neutro
val Tertiary = Color(0xFF5B5B53)
val TertiarySoft = Color(0xFFD9D9CF)
val Ink = Color(0xFF1B1F1A)
val InkSoft = Color(0xFF383C36)
// Escurecido de #8A8A80 pra passar WCAG AA (≈4.6:1 sobre Paper). Usado em quase
// todo texto secundário/legenda/placeholder — era o pior problema de contraste.
val Muted = Color(0xFF6E6E64)

// Superfícies
val Paper = Color(0xFFF7F5EE)
val PaperDeep = Color(0xFFF0EEE5)
val CardSurface = Color(0xFFFFFFFF)
val CardSoft = Color(0xFFF9F8F2)
val Cream = Color(0xFFFBFAF4)
val Divider = Color(0xFFE9E7DD)
val DividerSoft = Color(0xFFF1EFE6)
val DisabledSurface = Color(0xFFD8C9B8)

// ════════════════════════════════════════════════════════════════════════════
// PALETA ESCURA (dark mode)
// ════════════════════════════════════════════════════════════════════════════
//
// Filosofia: NÃO é "inverter cores". É uma paleta escura própria, quente e
// oliva-tingida (carvão morno, não azul-preto), pra manter a alma literária da
// marca à noite. Regras aplicadas:
//  - Superfícies: carvão-oliva em degraus (fundo → card elevado).
//  - Ink/Cream INVERTEM de papel: no escuro, `ink` é quase-branco quente e
//    `cream` vira uma superfície escura. Isso conserta de graça o padrão do
//    código "background(Terracota) + text=Cream" → texto escuro sobre acento
//    claro (contraste AA), sem tocar em cada call site.
//  - Tokens "*Dark"/"*Deep" (que no claro são o tom mais ESCURO do acento, usado
//    como texto sobre fill claro) INVERTEM pro tom mais CLARO no escuro — porque
//    no escuro eles caem sobre `*Soft` que virou escuro.
//  - Acentos base (oliva/terracota) ficam num tom médio-claro: legível como texto
//    sobre fundo escuro E aceita texto `cream`(escuro) por cima nos fills.
//    Alvos de contraste conferidos no papel; refino fino fica pro passe em emulador.

// Acento / Primário (terracota)
val TerracotaD = Color(0xFFC97A56)      // base — vívido o bastante pra texto no escuro
val TerracotaDarkD = Color(0xFFE0A488)  // "TerracotaDark" no escuro = tom claro (ênfase)
val TerracotaSoftD = Color(0xFF3A2318)  // fill suave → terracota bem escuro

// Herói / Secundário (oliva)
val OlivaD = Color(0xFF7C9A62)      // base herói — pop no escuro
val OlivaDarkD = Color(0xFFA7C08D)  // "OlivaDark" no escuro = tom claro
val OlivaDeepD = Color(0xFFC3D6AE)  // "OlivaDeep" no escuro = o mais claro (ênfase máx.)
val OlivaSoftD = Color(0xFF2A3420)  // fill suave → oliva bem escuro
val OlivaMidD = Color(0xFF889E71)

// Dourado — estrelas: o mesmo tom já lê bem sobre escuro.
val DouradoD = Color(0xFFE6BF6B)

// Neutro
val TertiaryD = Color(0xFFBABAAC)
val TertiarySoftD = Color(0xFF3A3D33)
val InkD = Color(0xFFECEADF)      // texto principal: quase-branco QUENTE
val InkSoftD = Color(0xFFD0CFC2)
val MutedD = Color(0xFF9E9E90)    // secundário — alvo AA sobre PaperD

// Superfícies (carvão-oliva morno, em degraus)
val PaperD = Color(0xFF14160F)          // fundo
val PaperDeepD = Color(0xFF1A1D14)
val CardSurfaceD = Color(0xFF1E2117)    // card elevado
val CardSoftD = Color(0xFF23261B)
val CreamD = Color(0xFF23261B)          // "cream" no escuro = superfície escura (inversão de papel)
val DividerD = Color(0xFF2E3226)
val DividerSoftD = Color(0xFF23261B)
val DisabledSurfaceD = Color(0xFF3B3C31)
