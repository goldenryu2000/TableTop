package dev.tabletop.mancala.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTest {
    @Test fun undoAgainstComputerSkipsBackOverItsReplies() {
        val start = Session(GameState.start(Opponent.MEDIUM))
        val (afterSouth, _) = start.play(0)!!
        val (afterNorth, _) = afterSouth.play(7)!!
        assertEquals(Side.SOUTH, afterNorth.state.toMove)
        val undone = afterNorth.undo()!!
        assertEquals(start.state, undone.state)
        assertFalse(undone.canUndo)
    }

    @Test fun undoAgainstComputerKeepsExtraTurnPositions() {
        // Pit 2 earns an extra turn; undo from the follow-up position returns to it, not the start.
        val (extra, _) = Session(GameState.start(Opponent.EASY)).play(2)!!
        val (afterSecond, _) = extra.play(0)!!
        assertEquals(extra.state, afterSecond.undo()!!.state)
    }

    @Test fun undoWithFriendGoesBackOneMove() {
        val (a, _) = Session(GameState.start(Opponent.FRIEND)).play(0)!!
        val (b, _) = a.play(7)!!
        assertEquals(a.state, b.undo()!!.state)
    }

    @Test fun freshSessionCannotUndo() {
        val s = Session(GameState.start(Opponent.HARD))
        assertFalse(s.canUndo)
        assertNull(s.undo())
    }

    @Test fun illegalPlayReturnsNull() {
        assertNull(Session(GameState.start(Opponent.FRIEND)).play(9))
    }

    @Test fun historyIsCapped() {
        var s = Session(GameState.start(Opponent.FRIEND))
        repeat(Session.MAX_HISTORY + 20) { s = s.copy(history = s.history + s.state) }
        val (next, _) = s.play(0)!!
        assertTrue(next.history.size <= Session.MAX_HISTORY)
    }
}
