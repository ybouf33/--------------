package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Immutable
data class SyndicSemanticColors(
  val paidText: Color,
  val paidContainer: Color,
  val pendingText: Color,
  val pendingContainer: Color,
  val overdueText: Color,
  val overdueContainer: Color,
  val goldAccent: Color,
  val heroStart: Color,
  val heroEnd: Color,
  val heroBorder: Color,
  val heroContent: Color,
  val heroSubtleContent: Color,
  val heroChipBg: Color,
  val heroButtonBg: Color,
  val heroButtonContent: Color,
  val hairline: Color,
  val isLight: Boolean,
)

val LocalSyndicColors = staticCompositionLocalOf {
  SyndicSemanticColors(
    paidText = StatusPaidGreen,
    paidContainer = StatusPaidBg,
    pendingText = StatusPendingAmber,
    pendingContainer = StatusPendingBg,
    overdueText = StatusOverdueRed,
    overdueContainer = StatusOverdueBg,
    goldAccent = AmberTertiary,
    heroStart = HeroLightStart,
    heroEnd = HeroLightEnd,
    heroBorder = HeroLightBorder,
    heroContent = HeroLightContent,
    heroSubtleContent = HeroLightSubtle,
    heroChipBg = Color.White.copy(alpha = 0.85f),
    heroButtonBg = CobaltPrimary,
    heroButtonContent = Color.White,
    hairline = SlateHairline,
    isLight = true,
  )
}

private val LightColorScheme =
  lightColorScheme(
    primary = CobaltPrimary,
    onPrimary = Color.White,
    primaryContainer = CobaltContainer,
    onPrimaryContainer = OnCobaltContainer,
    secondary = TealSecondary,
    onSecondary = Color.White,
    secondaryContainer = TealContainer,
    onSecondaryContainer = OnTealContainer,
    tertiary = AmberTertiary,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = OnAmberContainer,
    background = CanvasSlate,
    onBackground = MidnightNavy,
    surface = PureWhite,
    onSurface = MidnightNavy,
    surfaceVariant = SlateVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = SlateOutline,
    outlineVariant = SlateHairline,
    error = StatusOverdueRed,
    onError = Color.White,
    errorContainer = StatusOverdueBg,
    onErrorContainer = Color(0xFF881337),
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF0A2463),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = CobaltContainer,
    secondary = Color(0xFF22D3EE),
    onSecondary = Color(0xFF083344),
    secondaryContainer = Color(0xFF155E75),
    onSecondaryContainer = TealContainer,
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = AmberContainer,
    background = NightBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = NightSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = NightOutline,
    outlineVariant = Color(0xFF1E293B),
    error = Color(0xFFFDA4AF),
    onError = Color(0xFF881337),
    errorContainer = Color(0xFF881337),
    onErrorContainer = StatusOverdueBg,
  )

private val SyndicShapes =
  Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
  )

@Composable
fun SyndicTheme(
  darkTheme: Boolean = false,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val semanticColors =
    if (darkTheme) {
      SyndicSemanticColors(
        paidText = Color(0xFF34D399),
        paidContainer = Color(0xFF064E3B),
        pendingText = Color(0xFFFBBF24),
        pendingContainer = Color(0xFF451A03),
        overdueText = Color(0xFFFDA4AF),
        overdueContainer = Color(0xFF4C0519),
        goldAccent = GoldAccent,
        heroStart = Color(0xFF020617),
        heroEnd = Color(0xFF1E3A8A),
        heroBorder = Color(0xFF1E3A8A),
        heroContent = Color.White,
        heroSubtleContent = Color.White.copy(alpha = 0.78f),
        heroChipBg = Color.White.copy(alpha = 0.13f),
        heroButtonBg = Color.White,
        heroButtonContent = MidnightNavy,
        hairline = Color(0xFF1E293B),
        isLight = false,
      )
    } else {
      SyndicSemanticColors(
        paidText = StatusPaidGreen,
        paidContainer = StatusPaidBg,
        pendingText = StatusPendingAmber,
        pendingContainer = StatusPendingBg,
        overdueText = StatusOverdueRed,
        overdueContainer = StatusOverdueBg,
        goldAccent = AmberTertiary,
        heroStart = HeroLightStart,
        heroEnd = HeroLightEnd,
        heroBorder = HeroLightBorder,
        heroContent = HeroLightContent,
        heroSubtleContent = HeroLightSubtle,
        heroChipBg = Color.White.copy(alpha = 0.85f),
        heroButtonBg = CobaltPrimary,
        heroButtonContent = Color.White,
        hairline = SlateHairline,
        isLight = true,
      )
    }

  CompositionLocalProvider(LocalSyndicColors provides semanticColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = SyndicTypography,
      shapes = SyndicShapes,
      content = content,
    )
  }
}
