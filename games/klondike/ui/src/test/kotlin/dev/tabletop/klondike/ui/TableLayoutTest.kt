package dev.tabletop.klondike.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import dev.tabletop.cards.Card
import dev.tabletop.cards.Rank
import dev.tabletop.cards.Suit
import dev.tabletop.klondike.engine.DrawMode
import dev.tabletop.klondike.engine.GameState
import dev.tabletop.klondike.engine.Pile
import dev.tabletop.klondike.engine.TableauCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TableLayoutTest {
    private val deal = GameState.deal(3, DrawMode.ONE)

    @Test fun sevenColumnsFitTheWidth() {
        val l = computeLayout(deal, width = 1080f, height = 1800f, gap = 12f, leftHanded = false)
        val right = (0 until 7).maxOf { l.piles.getValue(Pile.Tableau(it)).right }
        assertTrue(right <= 1080f)
        assertEquals(52, l.cards.size)
    }

    @Test fun tableauCardsStepDown() {
        val l = computeLayout(deal, 1080f, 1800f, 12f, false)
        val column = l.cards.filter { it.pile == Pile.Tableau(6) }.sortedBy { it.index }
        assertTrue(column.zipWithNext().all { (a, b) -> b.topLeft.y > a.topLeft.y })
    }

    @Test fun tallColumnsStayOnScreen() {
        val big = deal.tableau[6].take(6).map { it.copy(faceUp = false) } +
            Rank.entries.reversed().mapIndexed { i, r -> TableauCard(Card(if (i % 2 == 0) Suit.SPADES else Suit.HEARTS, r), true) }
        val s = deal.copy(tableau = deal.tableau.toMutableList().also { it[6] = big })
        val height = 1400f
        val l = computeLayout(s, 1080f, height, 12f, false)
        val lowest = l.cards.filter { it.pile == Pile.Tableau(6) }.maxOf { it.topLeft.y + l.cardSize.height }
        assertTrue("bottom $lowest exceeds $height", lowest <= height)
    }

    @Test fun leftHandedMirrorsStock() {
        val right = computeLayout(deal, 1080f, 1800f, 12f, false)
        val left = computeLayout(deal, 1080f, 1800f, 12f, true)
        assertTrue(right.stock.left < 540f)
        assertTrue(left.stock.left > 540f)
    }

    @Test fun drawThreeFansTopThreeWasteCards() {
        val s = deal.copy(drawMode = DrawMode.THREE, waste = deal.stock.take(5), stock = deal.stock.drop(5))
        val l = computeLayout(s, 1080f, 1800f, 12f, false)
        val xs = l.cards.filter { it.pile == Pile.Waste }.sortedBy { it.index }.map { it.topLeft.x }
        assertEquals(xs[0], xs[2])
        assertTrue(xs[3] > xs[2] && xs[4] > xs[3])
    }

    @Test fun cardAtReturnsTopmost() {
        val l = computeLayout(deal, 1080f, 1800f, 12f, false)
        val top = l.cards.filter { it.pile == Pile.Tableau(6) }.maxBy { it.index }
        val hit = l.cardAt(top.topLeft + Offset(5f, 5f))
        assertEquals(top.card, hit?.card)
    }

    @Test fun dropCandidatesOrderedByOverlap() {
        val l = computeLayout(deal, 1080f, 1800f, 12f, false)
        val col2 = l.piles.getValue(Pile.Tableau(2))
        val dragged = Rect(Offset(col2.left + l.cardSize.width * 0.3f, col2.top + 50f), l.cardSize)
        assertEquals(listOf(Pile.Tableau(2), Pile.Tableau(3)), l.dropCandidates(dragged))
    }
}
