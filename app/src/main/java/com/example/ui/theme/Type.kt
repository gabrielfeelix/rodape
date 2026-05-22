package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
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

val FrauncesFont = GoogleFont("Fraunces")
val InterFont = GoogleFont("Inter")

val FrauncesFontFamily = FontFamily(
    Font(googleFont = FrauncesFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = FrauncesFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = FrauncesFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = FrauncesFont, fontProvider = fontProvider, weight = FontWeight.Bold)
)

val InterFontFamily = FontFamily(
    Font(googleFont = InterFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = InterFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = InterFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = InterFont, fontProvider = fontProvider, weight = FontWeight.Bold)
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FrauncesFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp
    ),
    displayMedium = TextStyle(
        fontFamily = FrauncesFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FrauncesFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FrauncesFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FrauncesFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 17.sp
    )
)
