package br.radio.fm.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF007A33),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7F2C5),
    onPrimaryContainer = Color(0xFF002109),
    secondary = Color(0xFF1F3A8A),
    tertiary = Color(0xFF8A6D00),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5BDB83),
    onPrimary = Color(0xFF003914),
    primaryContainer = Color(0xFF00531F),
    onPrimaryContainer = Color(0xFFB7F2C5),
    secondary = Color(0xFFB4C5FF),
    tertiary = Color(0xFFEBC248),
)

/** themeMode: 0 = sistema, 1 = claro, 2 = escuro */
@Composable
fun RadioTheme(themeMode: Int, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
