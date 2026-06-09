package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = AegisEmerald,
    secondary = AegisEmeraldDark,
    tertiary = AegisSlateBlue,
    background = AegisBackground,
    surface = AegisZinc900,
    onBackground = AegisSilver,
    onSurface = AegisSilver,
    surfaceVariant = AegisZinc800,
    onSurfaceVariant = AegisMuted
  )

private val LightColorScheme = DarkColorScheme // Force sophisticated dark for gaming systems appeal

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme
  dynamicColor: Boolean = false, // Disable dynamic colors as per custom system style
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme // Consistently use DarkColorScheme to match the "Sophisticated Dark" aesthetic

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
