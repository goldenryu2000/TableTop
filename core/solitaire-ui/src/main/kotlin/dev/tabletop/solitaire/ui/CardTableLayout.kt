package dev.tabletop.solitaire.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import dev.tabletop.cards.Card

/**
 * Where one card sits. [key] names the physical card (a game with two decks needs more than the
 * [Card] itself). [pile] is null for cards that can't be picked up from, like a stock.
 */
data class CardPlacement<P>(
    val key: Any,
    val card: Card,
    val faceUp: Boolean,
    val pile: P?,
    val index: Int,
    val topLeft: Offset,
    val z: Int,
)

/** Card positions plus the areas each pile accepts drops in. */
open class CardTableLayout<P>(
    val cardSize: Size,
    val piles: Map<P, Rect>,
    val cards: List<CardPlacement<P>>,
) {
    private val byKey = cards.associateBy { it.key }

    fun placementOf(key: Any): CardPlacement<P>? = byKey[key]

    fun cardAt(point: Offset): CardPlacement<P>? =
        cards.filter { Rect(it.topLeft, cardSize).contains(point) }.maxByOrNull { it.z }

    /** Piles the dragged card overlaps, biggest overlap first. */
    open fun dropCandidates(dragged: Rect): List<P> = piles
        .mapNotNull { (pile, area) ->
            val i = area.intersect(dragged)
            if (i.width > 0f && i.height > 0f) pile to i.width * i.height else null
        }
        .sortedByDescending { it.second }
        .map { it.first }
}

/**
 * Top offsets for a fanned column starting at [top]. Face-down cards step by [downStep] and face-up
 * cards by [upStep]; when the column would pass [bottom], every step shrinks evenly to fit.
 */
fun fanOffsets(faceUp: List<Boolean>, top: Float, bottom: Float, cardHeight: Float, downStep: Float, upStep: Float): List<Float> {
    val natural = faceUp.dropLast(1).sumOf { (if (it) upStep else downStep).toDouble() }.toFloat()
    val available = bottom - top
    val scale = if (natural > 0f && natural + cardHeight > available) ((available - cardHeight) / natural).coerceIn(0f, 1f) else 1f
    var y = top
    return faceUp.map { up ->
        y.also { y += (if (up) upStep else downStep) * scale }
    }
}
