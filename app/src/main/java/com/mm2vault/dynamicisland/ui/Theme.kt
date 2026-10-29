package com.mm2vault.dynamicisland.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Palette {
    val Background = Color(0xFF08060F)
    val Surface = Color(0xFF14101E)
    val Surface2 = Color(0xFF1C1529)
    val Neon = Color(0xFFA970FF)
    val Lavender = Color(0xFFD5B8FF)
    val Violet = Color(0xFF7C4DFF)
    val Text = Color(0xFFF7F2FF)
    val TextDim = Color(0xFFA69BB8)
    val Success = Color(0xFF8BE5B0)
    val Warning = Color(0xFFFFC477)
    val Hairline = Color(0x33A970FF)
    val Screen = Brush.verticalGradient(listOf(Color(0xFF1A0F2E), Background, Color(0xFF06040B)))
}

private val scheme = darkColorScheme(
    primary = Palette.Neon, onPrimary = Color(0xFF1A0B33),
    secondary = Palette.Violet, background = Palette.Background,
    surface = Palette.Surface, onSurface = Palette.Text,
    surfaceVariant = Palette.Surface2, onSurfaceVariant = Palette.TextDim,
    outline = Palette.Hairline,
)

private val type = Typography(
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp, color = Palette.Text),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, color = Palette.Text),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp, color = Palette.Text),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 17.sp, color = Palette.TextDim),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.4.sp, color = Palette.TextDim),
)

private val shapes = Shapes(
    small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp),
)

@Composable
fun IslandTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = type, shapes = shapes, content = content)
}
