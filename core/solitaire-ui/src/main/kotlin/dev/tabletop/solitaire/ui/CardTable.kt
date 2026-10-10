package dev.tabletop.solitaire.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.tabletop.cards.ui.CardStyle
import dev.tabletop.cards.ui.PlayingCard
import dev.tabletop.design.Motion
import dev.tabletop.design.rememberReducedMotion
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private class Drag<P>(val lead: CardPlacement<P>, val keys: Set<Any>, val offset: Offset)

/**
 * Every card of a solitaire table plus one gesture layer above them. Emits its nodes straight into
 * the caller's Box, which should fill the table area.
 *
 * - [fresh] with [seed]: cards fly in from [spawn] one by one, for a new deal.
 * - [onTap] gets the tap position and the topmost card under it, if any.
 * - [pickUp] returns the keys of the cards a drag from that face-up card lifts, lead first, or null.
 * - [onDrop] gets the lifted lead card, how many cards moved with it, and the piles under it.
 */
@Composable
fun <P> CardTable(
    layout: CardTableLayout<P>,
    style: CardStyle,
    spawn: Offset,
    fresh: Boolean,
    seed: Long,
    acceptsInput: Boolean,
    hinted: Set<Any>,
    hintColor: Color,
    onTap: (Offset, CardPlacement<P>?) -> Unit,
    pickUp: (CardPlacement<P>) -> List<Any>?,
    onPickup: () -> Unit,
    onDrop: (lead: CardPlacement<P>, count: Int, candidates: List<P>) -> Unit,
    cardModifier: (CardPlacement<P>) -> Modifier = { Modifier },
) {
    val density = LocalDensity.current
    val reducedMotion = rememberReducedMotion()
    val cardSize = with(density) { DpSize(layout.cardSize.width.toDp(), layout.cardSize.height.toDp()) }
    var drag by remember { mutableStateOf<Drag<P>?>(null) }
    val currentLayout by rememberUpdatedState(layout)
    val accepts by rememberUpdatedState(acceptsInput)
    val tap by rememberUpdatedState(onTap)
    val lift by rememberUpdatedState(pickUp)
    val pickup by rememberUpdatedState(onPickup)
    val drop by rememberUpdatedState(onDrop)

    layout.cards.forEach { p ->
        key(p.key) {
            MovingCard(
                placement = p,
                spawn = spawn,
                fresh = fresh,
                seed = seed,
                dragOffset = drag?.takeIf { p.key in it.keys }?.offset,
                reducedMotion = reducedMotion,
            ) { mod ->
                PlayingCard(
                    p.card,
                    p.faceUp,
                    style,
                    mod.size(cardSize)
                        .then(cardModifier(p))
                        .then(if (p.key in hinted) Modifier.hintGlow(cardSize.width, hintColor) else Modifier),
                )
            }
        }
    }

    // One gesture layer above everything; hit testing uses the layout, not composables.
    Box(
        Modifier
            .fillMaxSize()
            .zIndex(10_000f)
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    if (accepts) tap(pos, currentLayout.cardAt(pos))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { pos ->
                        if (!accepts) return@detectDragGestures
                        val p = currentLayout.cardAt(pos)?.takeIf { it.faceUp && it.pile != null } ?: return@detectDragGestures
                        val keys = lift(p) ?: return@detectDragGestures
                        drag = Drag(p, keys.toSet(), Offset.Zero)
                        pickup()
                    },
                    onDrag = { change, delta ->
                        val d = drag ?: return@detectDragGestures
                        change.consume()
                        drag = Drag(d.lead, d.keys, d.offset + delta)
                    },
                    onDragEnd = {
                        val d = drag ?: return@detectDragGestures
                        val l = currentLayout
                        l.placementOf(d.lead.key)?.let { lead ->
                            val rect = Rect(lead.topLeft + d.offset, l.cardSize)
                            drop(lead, d.keys.size, l.dropCandidates(rect).filter { it != lead.pile })
                        }
                        drag = null
                    },
                    onDragCancel = { drag = null },
                )
            },
    )
}

/** Animates a card toward its slot; snaps while dragged; staggers the opening deal. */
@Composable
private fun <P> MovingCard(
    placement: CardPlacement<P>,
    spawn: Offset,
    fresh: Boolean,
    seed: Long,
    dragOffset: Offset?,
    reducedMotion: Boolean,
    content: @Composable (Modifier) -> Unit,
) {
    val target = placement.topLeft + (dragOffset ?: Offset.Zero)
    val anim = remember { Animatable(if (fresh && !reducedMotion) spawn else target, Offset.VectorConverter) }
    val dealing = remember(seed) { mutableStateOf(fresh) }
    LaunchedEffect(target, dragOffset != null) {
        when {
            dragOffset != null || reducedMotion -> anim.snapTo(target)
            dealing.value -> {
                delay(placement.z * 14L)
                dealing.value = false
                anim.animateTo(target, Motion.card())
            }
            else -> anim.animateTo(target, Motion.card())
        }
    }
    val lifted = dragOffset != null || anim.isRunning
    content(
        Modifier
            .zIndex(placement.z + if (lifted) 1_000f else 0f)
            .offset { IntOffset(anim.value.x.roundToInt(), anim.value.y.roundToInt()) }
            .shadow(if (dragOffset != null) 10.dp else 1.dp, RoundedCornerShape(6.dp), clip = false),
    )
}

fun Modifier.hintGlow(width: Dp, color: Color): Modifier =
    border(2.dp, color, RoundedCornerShape(width * 0.09f))

/** A pulsing outline around where a hinted move would land. [modifier] places and sizes it. */
@Composable
fun HintOutline(color: Color, modifier: Modifier) {
    val pulse by rememberInfiniteTransition(label = "hint").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "hintPulse",
    )
    Box(
        modifier
            .zIndex(5_000f)
            .border(3.dp, color.copy(alpha = pulse), RoundedCornerShape(8.dp)),
    )
}
