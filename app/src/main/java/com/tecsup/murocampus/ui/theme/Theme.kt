package com.tecsup.murocampus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MuroCampusColors = lightColorScheme(
    primary = Color(0xFF176B9B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF176B9B),
    onPrimaryContainer = Color.White,
    surfaceVariant = Color(0xFFF4F6F8),
    onSurfaceVariant = Color(0xFF4A5568),
    errorContainer = Color(0xFFFDE8E8),
    onErrorContainer = Color(0xFF9B1C1C),
    outline = Color(0xFFCBD5E0)
)

@Composable
fun MuroCampusTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MuroCampusColors, content = content)
}
