package dev.tabletop.klondike.engine

import dev.tabletop.cards.Suit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTest {
    @Test fun playRecordsHistory() {
        val s = Session(state(stock = listOf("2H"))).play(Move.Draw)!!
        assertTrue(s.canUndo)
        assertEquals(1, s.history.size)
    }

    @Test fun illegalPlayReturnsNull() {
        assertNull(Session(state()).play(Move.Draw))
    }

    @Test fun undoRestoresPilesWithPenaltyAndKeepsTime() {
        val start = state(waste = listOf("AS"), score = 20)
        val played = Session(start).play(Move.Transfer(Pile.Waste, Pile.Foundation(Suit.SPADES)))!!
            .tick(5_000)
        val undone = played.undo()!!
        assertEquals(start.waste, undone.state.waste)
        assertEquals(start.foundations, undone.state.foundations)
        assertEquals(18, undone.state.score)
        assertEquals(5_000, undone.state.elapsedMs)
        assertEquals(2, undone.state.moves)
        assertFalse(undone.canUndo)
    }

    @Test fun undoWithNoHistoryIsNull() {
        assertNull(Session(state()).undo())
    }

    @Test fun historyIsCapped() {
        var s = Session(state(stock = listOf("2H", "3H")))
        repeat(Session.MAX_HISTORY + 50) { s = s.play(Move.Draw)!! }
        assertEquals(Session.MAX_HISTORY, s.history.size)
    }
}
