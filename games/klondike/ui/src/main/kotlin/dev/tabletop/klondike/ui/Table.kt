package dev.tabletop.klondike.ui

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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.tabletop.cards.Card
import dev.tabletop.cards.ui.CardSlot
import dev.tabletop.cards.ui.CardStyle
import dev.tabletop.cards.ui.PlayingCard
import dev.tabletop.cards.ui.glyph
import dev.tabletop.design.Motion
import dev.tabletop.design.rememberReducedMotion
import dev.tabletop.klondike.engine.Move
import dev.tabletop.klondike.engine.Pile
import dev.tabletop.klondike.engine.pickUp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private data class Drag(val from: Pile, val count: Int, val cards: Set<Card>, val lead: Card, val offset: Offset)

@Composable
internal fun Table(ui: KlondikeUiState, viewModel: KlondikeViewModel, modifier: Modifier = Modifier) {
    val state = ui.state
    val style = remember(ui.settings.fourColor) { CardStyle(fourColor = ui.settings.fourColor) }
    val reducedMotion = rememberReducedMotion()
    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val gap = with(density) { 6.dp.toPx() }
        val layout = remember(state, width, height, ui.settings.leftHanded) {
            computeLayout(state, width, height, gap, ui.settings.leftHanded)
        }
        val cardSize = with(density) { DpSize(layout.cardSize.width.toDp(), layout.cardSize.height.toDp()) }
        var drag by remember { mutableStateOf<Drag?>(null) }
        val currentLayout by rememberUpdatedState(layout)
        val currentUi by rememberUpdatedState(ui)

        fun Rect.place() = with(density) { Modifier.offset(left.toDp(), top.toDp()).size(this@place.width.toDp(), this@place.height.toDp()) }

        // Empty pile markers.
        CardSlot(if (state.waste.isNotEmpty()) "↻" else null, KlondikeColors.ink, layout.stock.place().testTag("stock"))
        layout.piles.forEach { (pile, rect) ->
            when (pile) {
                is Pile.Foundation -> CardSlot(glyph(pile.suit), KlondikeColors.ink, rect.place())
                is Pile.Tableau -> CardSlot("K", KlondikeColors.ink, Rect(rect.topLeft + Offset(gap / 2, 0f), layout.cardSize).place())
                Pile.Waste -> Unit
            }
        }

        if (state.isWon) {
            WinCascade(layout, style, height)
        } else {
            val hinted = remember(ui.hint, state) { hintedCards(ui.hint, ui) }
            layout.cards.forEach { p ->
                key(p.card) {
                    MovingCard(
                        placement = p,
                        spawn = layout.stock.topLeft,
                        fresh = state.moves == 0,
                        seed = state.seed,
                        dragOffset = drag?.takeIf { p.card in it.cards }?.offset,
                        reducedMotion = reducedMotion,
                    ) { mod ->
                        PlayingCard(
                            p.card,
                            p.faceUp,
                            style,
                            mod.size(cardSize)
                                .testTag("card-${p.card}")
                                .then(if (p.card in hinted) Modifier.hintGlow(cardSize.width) else Modifier),
                        )
                    }
                }
            }
            ui.hint?.let { HintTarget(it, layout) { rect -> rect.place() } }
        }

        // One gesture layer above everything; hit testing uses the layout, not composables.
        Box(
            Modifier
                .fillMaxSize()
                .zIndex(10_000f)
                .pointerInput(Unit) {
                    detectTapGestures { pos ->
                        if (!currentUi.acceptsInput) return@detectTapGestures
                        val l = currentLayout
                        if (l.stock.contains(pos)) {
                            viewModel.onDraw()
                            return@detectTapGestures
                        }
                        val p = l.cardAt(pos)?.takeIf { it.faceUp } ?: return@detectTapGestures
                        val pile = p.pile ?: return@detectTapGestures
                        val count = pickCount(currentUi, pile, p.index) ?: return@detectTapGestures
                        viewModel.onTapCard(pile, count)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { pos ->
                            if (!currentUi.acceptsInput) return@detectDragGestures
                            val p = currentLayout.cardAt(pos)?.takeIf { it.faceUp } ?: return@detectDragGestures
                            val pile = p.pile ?: return@detectDragGestures
                            val count = pickCount(currentUi, pile, p.index) ?: return@detectDragGestures
                            val cards = currentUi.state.pickUp(pile, count) ?: return@detectDragGestures
                            drag = Drag(pile, count, cards.toSet(), cards.first(), Offset.Zero)
                            viewModel.onPickup()
                        },
                        onDrag = { change, delta ->
                            val d = drag ?: return@detectDragGestures
                            change.consume()
                            drag = d.copy(offset = d.offset + delta)
                        },
                        onDragEnd = {
                            val d = drag ?: return@detectDragGestures
                            val l = currentLayout
                            l.placementOf(d.lead)?.let { lead ->
                                val rect = Rect(lead.topLeft + d.offset, l.cardSize)
                                viewModel.onDrop(d.from, d.count, l.dropCandidates(rect).filter { it != d.from })
                            }
                            drag = null
                        },
                        onDragCancel = { drag = null },
                    )
                },
        )
    }
}

private val KlondikeUiState.acceptsInput: Boolean get() = !autoFinishing && !state.isWon

/** How many cards a touch at [index] of [pile] picks up, or null if that card cannot be picked up. */
private fun pickCount(ui: KlondikeUiState, pile: Pile, index: Int): Int? = when (pile) {
    is Pile.Tableau -> ui.state.tableau[pile.index].size - index
    Pile.Waste -> 1.takeIf { index == ui.state.waste.lastIndex }
    is Pile.Foundation -> 1.takeIf { index == ui.state.foundation(pile.suit).lastIndex }
}

private fun hintedCards(hint: Move?, ui: KlondikeUiState): Set<Card> = when (hint) {
    is Move.Transfer -> ui.state.pickUp(hint.from, hint.count)?.toSet().orEmpty()
    else -> emptySet()
}

/** Animates a card toward its slot; snaps while dragged; staggers the opening deal. */
@Composable
private fun MovingCard(
    placement: CardPlacement,
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

private fun Modifier.hintGlow(width: Dp): Modifier =
    border(2.dp, KlondikeColors.accent, RoundedCornerShape(width * 0.09f))

@Composable
private fun HintTarget(hint: Move, layout: TableLayout, place: (Rect) -> Modifier) {
    val rect = when (hint) {
        Move.Draw -> layout.stock
        is Move.Transfer -> {
            val area = layout.piles[hint.to] ?: return
            if (hint.to is Pile.Tableau) {
                val top = layout.cards.filter { it.pile == hint.to }.maxByOrNull { it.index }?.topLeft
                    ?: Offset(area.left + (area.width - layout.cardSize.width) / 2, area.top)
                Rect(top, layout.cardSize)
            } else {
                area
            }
        }
    }
    val pulse by rememberInfiniteTransition(label = "hint").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "hintPulse",
    )
    Box(
        place(rect)
            .zIndex(5_000f)
            .border(3.dp, KlondikeColors.accent.copy(alpha = pulse), RoundedCornerShape(8.dp)),
    )
}
