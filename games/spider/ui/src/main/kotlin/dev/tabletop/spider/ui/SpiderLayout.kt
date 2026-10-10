package dev.tabletop.spider.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import dev.tabletop.cards.ui.CARD_ASPECT
import dev.tabletop.solitaire.ui.CardPlacement
import dev.tabletop.solitaire.ui.CardTableLayout
import dev.tabletop.solitaire.ui.fanOffsets
import dev.tabletop.spider.engine.COLUMNS
import dev.tabletop.spider.engine.GameState

/** Spider's table. Piles are column indices; cards are keyed by their id. [stock] is the tap area for dealing. */
class SpiderLayout(
    cardSize: Size,
    val stock: Rect,
    val completedSlot: Rect,
    piles: Map<Int, Rect>,
    cards: List<CardPlacement<Int>>,
) : CardTableLayout<Int>(cardSize, piles, cards)

/**
 * Ten columns across. The top row holds finished runs fanned from the left (kings showing) and the
 * stock fanned from the right, one back per deal left.
 */
fun computeSpiderLayout(state: GameState, width: Float, height: Float, gap: Float): SpiderLayout {
    val byWidth = (width - gap * (COLUMNS + 1)) / COLUMNS
    val byHeight = (height - gap * 4) / 3f * CARD_ASPECT
    val cardW = minOf(byWidth, byHeight).coerceAtLeast(1f)
    val cardH = cardW / CARD_ASPECT
    val size = Size(cardW, cardH)
    val inset = (width - (cardW * COLUMNS + gap * (COLUMNS + 1))) / 2
    fun x(column: Int) = inset + gap + column * (cardW + gap)
    val top = gap
    val fan = cardW * 0.3f

    val cards = mutableListOf<CardPlacement<Int>>()
    var z = 0

    // Finished runs: each run stacked in one spot with its King on top.
    state.completed.forEachIndexed { k, run ->
        val at = Offset(x(0) + k * fan, top)
        run.asReversed().forEachIndexed { i, c -> cards += CardPlacement(c.id, c.card, true, null, i, at, z++) }
    }

    // Stock: deal group d holds stock[10d until 10d + 10]; group 0 deals next, so it sits in front.
    val deals = state.dealsLeft
    for (d in deals - 1 downTo 0) {
        val at = Offset(x(COLUMNS - 1) - d * fan, top)
        state.stock.subList(d * COLUMNS, (d + 1) * COLUMNS).forEachIndexed { i, c ->
            cards += CardPlacement(c.id, c.card, false, null, i, at, z++)
        }
    }
    val stockLeft = x(COLUMNS - 1) - (deals - 1).coerceAtLeast(0) * fan
    val stock = Rect(stockLeft, top, x(COLUMNS - 1) + cardW, top + cardH)

    val tableauTop = top + cardH + gap * 2
    val piles = mutableMapOf<Int, Rect>()
    state.tableau.forEachIndexed { column, pile ->
        val ys = fanOffsets(pile.map { it.faceUp }, tableauTop, height - gap, cardH, downStep = cardH * 0.10f, upStep = cardH * 0.28f)
        pile.forEachIndexed { i, c -> cards += CardPlacement(c.id, c.card, c.faceUp, column, i, Offset(x(column), ys[i]), z++) }
        piles[column] = Rect(x(column) - gap / 2, tableauTop, x(column) + cardW + gap / 2, height)
    }

    return SpiderLayout(size, stock, Rect(Offset(x(0), top), size), piles, cards)
}
