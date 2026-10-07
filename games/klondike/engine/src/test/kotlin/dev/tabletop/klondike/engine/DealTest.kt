package dev.tabletop.klondike.engine

import dev.tabletop.cards.Suit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DealTest {
    @Test fun dealShape() {
        val s = GameState.deal(seed = 42, drawMode = DrawMode.ONE)
        assertEquals((1..7).toList(), s.tableau.map { it.size })
        s.tableau.forEach { pile ->
            assertTrue(pile.last().faceUp)
            assertTrue(pile.dropLast(1).none { it.faceUp })
        }
        assertEquals(24, s.stock.size)
        assertTrue(s.waste.isEmpty())
        assertTrue(s.foundations.all { it.isEmpty() })
        assertEquals(0, s.score)
        assertEquals(0, s.moves)
        assertFalse(s.isWon)
        s.assertValid()
    }

    @Test fun sameSeedSameDeal() {
        assertEquals(GameState.deal(7, DrawMode.ONE), GameState.deal(7, DrawMode.ONE))
    }

    @Test fun differentSeedDifferentDeal() {
        assertNotEquals(GameState.deal(1, DrawMode.ONE).tableau, GameState.deal(2, DrawMode.ONE).tableau)
    }

    @Test fun wonWhenAllFoundationsFull() {
        assertTrue(state(foundationTops = Suit.entries.associateWith { 13 }).isWon)
    }
}
