package dev.tabletop.spider.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SessionTest {
    @Test fun undoRestoresAndCountsAsAMove() {
        val start = Session(GameState.deal(1, Suits.ONE))
        val dealt = start.play(Move.Deal)!!.tick(5_000)
        val undone = dealt.undo()!!
        assertEquals(start.state.tableau, undone.state.tableau)
        assertEquals(start.state.stock, undone.state.stock)
        assertEquals(2, undone.state.moves)
        assertEquals(Scoring.START - 1, undone.state.score)
        assertEquals(5_000L, undone.state.elapsedMs)
        assertFalse(undone.canUndo)
    }

    @Test fun illegalPlayAndEmptyUndoReturnNull() {
        val s = Session(state(column(), stock = List(10) { c("-AS") }))
        assertNull(s.play(Move.Deal))
        assertNull(s.undo())
    }
}
