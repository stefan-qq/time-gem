package com.dragonpi.timegem.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.ExperimentalTextApi
import com.dragonpi.timegem.R

@OptIn(ExperimentalTextApi::class)
private val GoogleSansFlex = FontFamily(
    Font(R.font.google_sans_flex, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.google_sans_flex, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.google_sans_flex, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.google_sans_flex, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

private val defaults = Typography()
val Typography = Typography(
    displayLarge = defaults.displayLarge.copy(fontFamily = GoogleSansFlex),
    displayMedium = defaults.displayMedium.copy(fontFamily = GoogleSansFlex),
    displaySmall = defaults.displaySmall.copy(fontFamily = GoogleSansFlex),
    headlineLarge = defaults.headlineLarge.copy(fontFamily = GoogleSansFlex),
    headlineMedium = defaults.headlineMedium.copy(fontFamily = GoogleSansFlex),
    headlineSmall = defaults.headlineSmall.copy(fontFamily = GoogleSansFlex),
    titleLarge = defaults.titleLarge.copy(fontFamily = GoogleSansFlex),
    titleMedium = defaults.titleMedium.copy(fontFamily = GoogleSansFlex),
    titleSmall = defaults.titleSmall.copy(fontFamily = GoogleSansFlex),
    bodyLarge = defaults.bodyLarge.copy(fontFamily = GoogleSansFlex),
    bodyMedium = defaults.bodyMedium.copy(fontFamily = GoogleSansFlex),
    bodySmall = defaults.bodySmall.copy(fontFamily = GoogleSansFlex),
    labelLarge = defaults.labelLarge.copy(fontFamily = GoogleSansFlex),
    labelMedium = defaults.labelMedium.copy(fontFamily = GoogleSansFlex),
    labelSmall = defaults.labelSmall.copy(fontFamily = GoogleSansFlex),
)
