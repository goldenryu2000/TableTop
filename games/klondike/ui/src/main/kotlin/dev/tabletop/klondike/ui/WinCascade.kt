package dev.tabletop.klondike.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import dev.tabletop.cards.ui.CardStyle
import dev.tabletop.cards.ui.PlayingCard
import dev.tabletop.klondike.engine.Pile
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

/** Foundation cards drift down off the table one by one, kings first. */
@Composable
internal fun WinCascade(layout: TableLayout, style: CardStyle, height: Float) {
    val density = LocalDensity.current
    val size = with(density) { DpSize(layout.cardSize.width.toDp(), layout.cardSize.height.toDp()) }
    val cards = remember(layout) {
        layout.cards.filter { it.pile is Pile.Foundation }
            .sortedWith(compareByDescending<CardPlacement> { it.index }.thenBy { it.card.suit })
    }
    cards.forEachIndexed { i, p ->
        key(p.card) {
            val progress = remember { Animatable(0f) }
            val rnd = remember { Random(p.card.hashCode()) }
            val drift = remember { (rnd.nextFloat() - 0.5f) * layout.cardSize.width * 4f }
            val spin = remember { (rnd.nextFloat() - 0.5f) * 50f }
            LaunchedEffect(Unit) {
                delay(i * 45L)
                progress.animateTo(1f, tween(1500, easing = FastOutLinearInEasing))
            }
            val fall = height + layout.cardSize.height - p.topLeft.y
            PlayingCard(
                p.card,
                true,
                style,
                Modifier
                    .zIndex(1_000f - i)
                    .offset {
                        val t = progress.value
                        IntOffset((p.topLeft.x + drift * t).roundToInt(), (p.topLeft.y + fall * t * t).roundToInt())
                    }
                    .size(size)
                    .graphicsLayer { rotationZ = spin * progress.value },
            )
        }
    }
}
