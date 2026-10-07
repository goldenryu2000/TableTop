package dev.tabletop.klondike.engine

import dev.tabletop.cards.Suit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistTest {
    private val nearlyDone = state(
        foundationTops = mapOf(Suit.SPADES to 11, Suit.HEARTS to 12, Suit.DIAMONDS to 13, Suit.CLUBS to 12),
        tableau = listOf(listOf(up("KS"), up("QS")), listOf(up("KH")), listOf(up("KC"))),
    )

    @Test fun autoCompleteFinishesTheGame() {
        assertTrue(nearlyDone.canAutoComplete)
        var s = nearlyDone
        nearlyDone.autoCompleteMoves().forEach { s = s.apply(it)!! }
        assertTrue(s.isWon)
    }

    @Test fun noAutoCompleteWithFaceDownCards() {
        val st = state(tableau = listOf(listOf(down("2S"), up("AS"))))
        assertFalse(st.canAutoComplete)
        assertTrue(st.autoCompleteMoves().isEmpty())
    }

    @Test fun noAutoCompleteWithStockLeft() {
        assertFalse(nearlyDone.copy(stock = listOf(c("2S"))).canAutoComplete)
    }

    @Test fun bestMovePrefersFoundation() {
        val st = state(waste = listOf("AH"), tableau = listOf(listOf(up("2S"))))
        assertEquals(Move.Transfer(Pile.Waste, Pile.Foundation(Suit.HEARTS)), st.bestMoveFor(Pile.Waste))
    }

    @Test fun bestMovePrefersNonEmptyTableau() {
        val st = state(waste = listOf("QH"), tableau = listOf(emptyList(), listOf(up("KS"))))
        assertEquals(Move.Transfer(Pile.Waste, Pile.Tableau(1)), st.bestMoveFor(Pile.Waste))
    }

    @Test fun bestMoveSkipsKingShuffleBetweenEmptyColumns() {
        val st = state(tableau = listOf(listOf(up("KS")), emptyList()))
        assertNull(st.bestMoveFor(Pile.Tableau(0)))
    }

    @Test fun bestMoveMovesRuns() {
        val st = state(tableau = listOf(listOf(down("2C"), up("9H"), up("8S")), listOf(up("10C"))))
        assertEquals(Move.Transfer(Pile.Tableau(0), Pile.Tableau(1), 2), st.bestMoveFor(Pile.Tableau(0), 2))
    }

    @Test fun hintPrefersFoundation() {
        val st = state(stock = listOf("5D"), waste = listOf("AH"), tableau = listOf(listOf(up("2S"))))
        assertEquals(Move.Transfer(Pile.Waste, Pile.Foundation(Suit.HEARTS)), st.hint())
    }

    @Test fun hintRevealsHiddenCards() {
        val st = state(stock = listOf("5D"), tableau = listOf(listOf(down("2C"), up("9H")), listOf(up("10C"))))
        assertEquals(Move.Transfer(Pile.Tableau(0), Pile.Tableau(1), 1), st.hint())
    }

    @Test fun hintFallsBackToDraw() {
        assertEquals(Move.Draw, state(stock = listOf("5D"), tableau = listOf(listOf(up("KS")))).hint())
    }

    @Test fun hintIsNullWhenStuck() {
        assertNull(state(tableau = listOf(listOf(up("KS")), listOf(up("KH")))).hint())
    }
}
