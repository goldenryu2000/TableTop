package dev.tabletop.klondike.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import dev.tabletop.cards.Card
import dev.tabletop.cards.Suit
import dev.tabletop.cards.ui.CARD_ASPECT
import dev.tabletop.klondike.engine.DrawMode
import dev.tabletop.klondike.engine.GameState
import dev.tabletop.klondike.engine.Pile

/** Where one card sits. [pile] is null for the stock. */
data class CardPlacement(
    val card: Card,
    val faceUp: Boolean,
    val pile: Pile?,
    val index: Int,
    val topLeft: Offset,
    val z: Int,
)

class TableLayout(
    val cardSize: Size,
    val stock: Rect,
    val piles: Map<Pile, Rect>,
    val cards: List<CardPlacement>,
) {
    private val byCard = cards.associateBy { it.card }

    fun placementOf(card: Card): CardPlacement? = byCard[card]

    fun cardAt(point: Offset): CardPlacement? =
        cards.filter { Rect(it.topLeft, cardSize).contains(point) }.maxByOrNull { it.z }

    /** Piles the dragged card overlaps, biggest overlap first. Tableau areas span the full column. */
    fun dropCandidates(dragged: Rect): List<Pile> = piles
        .filterKeys { it != Pile.Waste }
        .mapNotNull { (pile, area) ->
            val i = area.intersect(dragged)
            if (i.width > 0f && i.height > 0f) pile to i.width * i.height else null
        }
        .sortedByDescending { it.second }
        .map { it.first }
}

fun computeLayout(state: GameState, width: Float, height: Float, gap: Float, leftHanded: Boolean): TableLayout {
    // Fit seven columns across, but on short tables (landscape) let height decide so a column always fits.
    val byWidth = (width - gap * 8) / 7
    val byHeight = (height - gap * 4.5f) / 2.6f * CARD_ASPECT
    val cardW = minOf(byWidth, byHeight).coerceAtLeast(1f)
    val cardH = cardW / CARD_ASPECT
    val size = Size(cardW, cardH)
    val inset = (width - (cardW * 7 + gap * 8)) / 2
    fun x(column: Int): Float {
        val left = inset + gap + column * (cardW + gap)
        return if (leftHanded) width - left - cardW else left
    }
    val top = gap

    val stock = Rect(Offset(x(0), top), size)
    val piles = mutableMapOf<Pile, Rect>(Pile.Waste to Rect(Offset(x(1), top), size))
    Suit.entries.forEachIndexed { i, s -> piles[Pile.Foundation(s)] = Rect(Offset(x(3 + i), top), size) }

    val cards = mutableListOf<CardPlacement>()
    var z = 0
    state.stock.forEachIndexed { i, c -> cards += CardPlacement(c, false, null, i, stock.topLeft, z++) }

    val fanStep = cardW * 0.34f * if (leftHanded) -1 else 1
    val visible = minOf(if (state.drawMode == DrawMode.THREE) 3 else 1, state.waste.size)
    state.waste.forEachIndexed { i, c ->
        val fromTop = state.waste.size - 1 - i
        val slot = if (fromTop >= visible) 0 else visible - 1 - fromTop
        cards += CardPlacement(c, true, Pile.Waste, i, Offset(x(1) + slot * fanStep, top), z++)
    }

    Suit.entries.forEach { s ->
        val at = piles.getValue(Pile.Foundation(s)).topLeft
        state.foundation(s).forEachIndexed { i, c -> cards += CardPlacement(c, true, Pile.Foundation(s), i, at, z++) }
    }

    val tableauTop = top + cardH + gap * 2.5f
    val available = height - tableauTop - gap
    val downStep = cardH * 0.12f
    val upStep = cardH * 0.27f
    state.tableau.forEachIndexed { p, pile ->
        val natural = pile.dropLast(1).sumOf { (if (it.faceUp) upStep else downStep).toDouble() }.toFloat()
        val scale = if (natural > 0f && natural + cardH > available) ((available - cardH) / natural).coerceIn(0f, 1f) else 1f
        var y = tableauTop
        pile.forEachIndexed { i, tc ->
            cards += CardPlacement(tc.card, tc.faceUp, Pile.Tableau(p), i, Offset(x(p), y), z++)
            y += (if (tc.faceUp) upStep else downStep) * scale
        }
        piles[Pile.Tableau(p)] = Rect(x(p) - gap / 2, tableauTop, x(p) + cardW + gap / 2, height)
    }

    return TableLayout(size, stock, piles, cards)
}
