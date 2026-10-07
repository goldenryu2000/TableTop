package dev.tabletop.cards.ui

import dev.tabletop.cards.Suit
import org.junit.Assert.assertEquals
import org.junit.Test

class CardStyleTest {
    @Test fun twoColorSuits() {
        val s = CardStyle()
        assertEquals(s.red, s.colorOf(Suit.DIAMONDS))
        assertEquals(s.black, s.colorOf(Suit.CLUBS))
    }

    @Test fun fourColorSuits() {
        val s = CardStyle(fourColor = true)
        assertEquals(s.red, s.colorOf(Suit.HEARTS))
        assertEquals(s.black, s.colorOf(Suit.SPADES))
        assertEquals(s.diamonds, s.colorOf(Suit.DIAMONDS))
        assertEquals(s.clubs, s.colorOf(Suit.CLUBS))
    }

    @Test fun suitGlyphsForceTextPresentation() {
        Suit.entries.forEach { assertEquals(it.symbol + "\uFE0E", glyph(it)) }
    }
}
