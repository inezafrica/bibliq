package com.yourname.bibliq.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BibliqPurple = Color(0xFF6200EE)
private val BibliqTeal = Color(0xFF03DAC5)
private val CorrectGreen = Color(0xFF2ECC71)
private val IncorrectRed = Color(0xFFE74C3C)

private val LightColors = lightColorScheme(
    primary = BibliqPurple,
    secondary = BibliqTeal
)

private val DarkColors = darkColorScheme(
    primary = BibliqPurple,
    secondary = BibliqTeal
)

object BibliqFeedbackColors {
    val Correct = CorrectGreen
    val Incorrect = IncorrectRed
}

@Composable
fun BibliQTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colorScheme, content = content)
}
