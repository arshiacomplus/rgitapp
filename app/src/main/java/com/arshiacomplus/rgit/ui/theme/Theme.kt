package com.arshiacomplus.rgit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// ! RGit does not support light theme by design.
private val RGitColorScheme = darkColorScheme(
    background = GhBackground,
    surface = GhSurface,
    onBackground = GhTextPrimary,
    onSurface = GhTextPrimary,
    primary = GhButtonPrimary,
    secondary = GhButtonBlue,
    outline = GhBorder
)

@Composable
fun RGitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RGitColorScheme,
        content = content
    )
}