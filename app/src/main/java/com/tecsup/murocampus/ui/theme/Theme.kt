package com.tecsup.murocampus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MuroCampusColors = lightColorScheme(
    primary = Color(0xFF176B9B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8EBF7),
    onPrimaryContainer = Color(0xFF104967),
    surfaceVariant = Color(0xFFEDF2F7)
)

@Composable
fun MuroCampusTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MuroCampusColors, content = content)
}
