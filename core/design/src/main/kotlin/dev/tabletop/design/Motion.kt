package dev.tabletop.design

import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

object Motion {
    /** Card travel: quick, settles without visible bounce. */
    fun <T> card(): SpringSpec<T> = spring(dampingRatio = 0.85f, stiffness = 450f)

    /** Slow, soft movement for chrome and sheets. */
    fun <T> gentle(): SpringSpec<T> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
}

/** True when the system animator scale is off. */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
