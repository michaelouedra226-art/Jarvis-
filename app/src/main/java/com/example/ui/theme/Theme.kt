package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MyApplicationTheme(
    themeMode: String = "dark",
    accentColorIndex: Int = 0,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    // Resolve primary color based on index
    val primaryColor = when (accentColorIndex) {
        0 -> CyanPrimary
        1 -> OrangePrimary
        2 -> PurplePrimary
        3 -> GreenPrimary
        4 -> GoldPrimary
        else -> CyanPrimary
    }

    val accentColor = when (accentColorIndex) {
        0 -> CyanAccent
        1 -> OrangeAccent
        2 -> PurpleAccent
        3 -> GreenAccent
        4 -> GoldAccent
        else -> CyanAccent
    }

    val darkBg = when (accentColorIndex) {
        0 -> CyanDarkBg
        1 -> OrangeDarkBg
        2 -> PurpleDarkBg
        3 -> GreenDarkBg
        4 -> GoldDarkBg
        else -> CyanDarkBg
    }

    val darkCard = when (accentColorIndex) {
        0 -> CyanCardBg
        1 -> OrangeCardBg
        2 -> PurpleCardBg
        3 -> GreenCardBg
        4 -> GoldCardBg
        else -> CyanCardBg
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primaryColor,
            secondary = accentColor,
            background = darkBg,
            surface = darkCard,
            surfaceVariant = darkCard.copy(alpha = 0.8f),
            onPrimary = Color.Black,
            onSecondary = Color.Black,
            onBackground = TextLight,
            onSurface = TextLight,
            onSurfaceVariant = TextGray
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            secondary = accentColor,
            background = LightBg,
            surface = LightCardBg,
            surfaceVariant = Color(0xFFECEFF1),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = TextDark,
            onSurface = TextDark,
            onSurfaceVariant = Color(0xFF546E7A)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
