package dev.tabletop.cards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardTest {
    @Test fun deckHas52UniqueCards() {
        assertEquals(52, Card.deck.size)
        assertEquals(52, Card.deck.toSet().size)
    }

    @Test fun colors() {
        assertTrue(Card(Suit.HEARTS, Rank.ACE).isRed)
        assertTrue(Card(Suit.DIAMONDS, Rank.TEN).isRed)
        assertFalse(Card(Suit.SPADES, Rank.KING).isRed)
        assertFalse(Card(Suit.CLUBS, Rank.TWO).isRed)
    }

    @Test fun label() {
        assertEquals("10♥", Card(Suit.HEARTS, Rank.TEN).toString())
    }
}
