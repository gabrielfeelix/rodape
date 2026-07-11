package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.R
import androidx.compose.ui.text.googlefonts.Font as GFont

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// Serif editorial — origem: claude-design usa Literata.
private val LiterataFont = GoogleFont("Literata")
// Sans para UI.
private val InterFont = GoogleFont("Inter")

// Fallback embarcado: fontes variáveis em res/font garantem que Literata/Inter
// renderizam mesmo sem GMS/rede (sem elas o Compose caía silenciosamente em
// Roboto). Em API 24-25 a variação de peso é ignorada (instância padrão).
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun literataLocal(weight: FontWeight, style: FontStyle = FontStyle.Normal) = Font(
    resId = if (style == FontStyle.Italic) R.font.literata_italic else R.font.literata,
    weight = weight,
    style = style,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun interLocal(weight: FontWeight) = Font(
    resId = R.font.inter,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val LiterataFontFamily = FontFamily(
    GFont(googleFont = LiterataFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    literataLocal(FontWeight.Normal),
    GFont(googleFont = LiterataFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    literataLocal(FontWeight.Medium),
    GFont(googleFont = LiterataFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    literataLocal(FontWeight.SemiBold),
    GFont(googleFont = LiterataFont, fontProvider = fontProvider, weight = FontWeight.Bold),
    literataLocal(FontWeight.Bold),
    GFont(googleFont = LiterataFont, fontProvider = fontProvider, weight = FontWeight.Normal, style = FontStyle.Italic),
    literataLocal(FontWeight.Normal, FontStyle.Italic),
    GFont(googleFont = LiterataFont, fontProvider = fontProvider, weight = FontWeight.Medium, style = FontStyle.Italic),
    literataLocal(FontWeight.Medium, FontStyle.Italic),
    GFont(googleFont = LiterataFont, fontProvider = fontProvider, weight = FontWeight.SemiBold, style = FontStyle.Italic),
    literataLocal(FontWeight.SemiBold, FontStyle.Italic),
)

val InterFontFamily = FontFamily(
    GFont(googleFont = InterFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    interLocal(FontWeight.Normal),
    GFont(googleFont = InterFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    interLocal(FontWeight.Medium),
    GFont(googleFont = InterFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    interLocal(FontWeight.SemiBold),
    GFont(googleFont = InterFont, fontProvider = fontProvider, weight = FontWeight.Bold),
    interLocal(FontWeight.Bold),
)

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
    headlineSmall = TextStyle(
        fontFamily = LiterataFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = LiterataFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.3).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = LiterataFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = (-0.2).sp,
    ),
    titleSmall = TextStyle(
        fontFamily = LiterataFontFamily, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = (-0.1).sp,
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
