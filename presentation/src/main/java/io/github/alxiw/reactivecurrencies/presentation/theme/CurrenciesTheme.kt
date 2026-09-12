package io.github.alxiw.reactivecurrencies.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF24B4D3),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4AC2CB),
    onPrimaryContainer = Color.Black,
    secondary = Color(0xFFFADD5B),
    onSecondary = Color.Black,
    background = Color.White,
    onBackground = Color(0xFF000000),
    surface = Color.White,
    onSurface = Color(0xFF000000),
    onSurfaceVariant = Color(0xFF808080),
)

@Composable
fun CurrenciesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content,
    )
}
