package dev.tabletop.design

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val LightFallback = lightColorScheme(
    primary = Color(0xFF2F6B5A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7EBD7),
    onPrimaryContainer = Color(0xFF002118),
    secondary = Color(0xFF4C6359),
    background = Color(0xFFF6F8F5),
    surface = Color(0xFFF6F8F5),
    surfaceContainer = Color(0xFFEAEFEB),
)

private val DarkFallback = darkColorScheme(
    primary = Color(0xFF9BD0BC),
    onPrimary = Color(0xFF00382B),
    primaryContainer = Color(0xFF14513F),
    onPrimaryContainer = Color(0xFFB7EBD7),
    secondary = Color(0xFFB3CCC0),
    background = Color(0xFF0F1513),
    surface = Color(0xFF0F1513),
    surfaceContainer = Color(0xFF1B2220),
)

private val base = Typography()

private val TabletopTypography = base.copy(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.25).sp),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

private val TabletopShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun TabletopTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkFallback
        else -> LightFallback
    }
    MaterialTheme(colorScheme = scheme, typography = TabletopTypography, shapes = TabletopShapes, content = content)
}

/** Light status and navigation bar icons while composed, for dark game tables. */
@Composable
fun LightSystemBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window ?: return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, view)
        val status = controller.isAppearanceLightStatusBars
        val nav = controller.isAppearanceLightNavigationBars
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        onDispose {
            controller.isAppearanceLightStatusBars = status
            controller.isAppearanceLightNavigationBars = nav
        }
    }
}
