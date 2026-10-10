package dev.tabletop.spider.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AssistTest {
    @Test fun tapPrefersSameSuitTarget() {
        val s = state(column("9H"), column("9S"), column("-2C", "8S"))
        assertEquals(Move.Transfer(2, 1, 1), s.bestMoveFor(2, 1))
    }

    @Test fun tapFallsBackToAnyCardThenEmptyColumn() {
        assertEquals(Move.Transfer(1, 0, 1), state(column("9H"), column("-2C", "8S")).bestMoveFor(1, 1))
        assertEquals(Move.Transfer(1, 0, 1), state(column(), column("-2C", "8S")).bestMoveFor(1, 1))
    }

    @Test fun tapOnAnUnmovableStackDoesNothing() {
        assertNull(state(column("9S", "8H"), column("10D")).bestMoveFor(0, 2))
    }

    @Test fun hintPrefersJoiningSuitOverRevealing() {
        val s = state(column("-3C", "7H"), column("8D"), column("-4C", "6S"), column("7S"))
        assertEquals(Move.Transfer(2, 3, 1), s.hint())
    }

    @Test fun hintRevealsHiddenCards() {
        val s = state(column("-3C", "7H"), column("8D"))
        assertEquals(Move.Transfer(0, 1, 1), s.hint())
    }

    @Test fun hintSkipsPointlessShuffles() {
        // 7♥ already sits on 8♠; moving it to 8♦ changes nothing. With no stock there's nothing to do.
        val s = state(column("8S", "7H"), column("8D"))
        assertNull(s.hint())
    }

    @Test fun hintDealsWhenNothingElseHelps() {
        val s = state(column("8S", "7H"), stock = List(10) { c("-2C") })
        assertEquals(Move.Deal, s.hint())
    }

    @Test fun hintNeverEmptiesAColumnWhileCardsRemainToDeal() {
        // Moving the lone 7♠ onto 8♠ would empty its column, which must be refilled before dealing.
        val s = state(column("8S"), column("7S"), stock = List(10) { c("-2C") })
        assertEquals(Move.Deal, s.hint())
        val noStock = state(column("8S"), column("7S"))
        assertEquals(Move.Transfer(1, 0, 1), noStock.hint())
    }

    @Test fun hintFillsAnEmptyColumnSoItCanDeal() {
        val s = state(column(), column("QD", "5S"), stock = List(10) { c("-2C") })
        val hint = s.hint()
        assertEquals(Move.Transfer(1, 0, 1), hint)
        assertTrue(s.apply(hint!!)!!.canDeal)
    }

    @Test fun hintsAreAlwaysLegal() {
        val random = Random(5)
        repeat(30) {
            var s = GameState.deal(random.nextLong(), Suits.entries.random(random))
            repeat(60) {
                val h = s.hint() ?: return@repeat
                s = s.apply(h) ?: throw AssertionError("illegal hint $h")
            }
        }
    }
}
