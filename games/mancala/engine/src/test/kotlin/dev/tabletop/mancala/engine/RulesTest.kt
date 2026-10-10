package dev.tabletop.mancala.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Board literal in sowing order: South pits, South store, North pits, North store. */
fun board(south: List<Int>, southStore: Int, north: List<Int>, northStore: Int) = south + southStore + north + northStore

class RulesTest {
    private val start = GameState.start(Opponent.FRIEND)

    @Test fun startHasFourStonesPerPitAndEmptyStores() {
        assertEquals(board(List(6) { 4 }, 0, List(6) { 4 }, 0), start.board)
        assertEquals(Side.SOUTH, start.toMove)
        assertEquals(listOf(0, 1, 2, 3, 4, 5), start.legalPits)
    }

    @Test fun sowingDropsOneStonePerPitCounterClockwise() {
        val turn = start.sow(0)!!
        assertEquals(listOf(1, 2, 3, 4), turn.drops)
        assertEquals(board(listOf(0, 5, 5, 5, 5, 4), 0, List(6) { 4 }, 0), turn.state.board)
        assertEquals(Side.NORTH, turn.state.toMove)
        assertEquals(1, turn.state.moves)
    }

    @Test fun lastStoneInOwnStoreGivesAnotherTurn() {
        val turn = start.sow(2)!!
        assertEquals(listOf(3, 4, 5, 6), turn.drops)
        assertTrue(turn.extraTurn)
        assertEquals(Side.SOUTH, turn.state.toMove)
    }

    @Test fun northSowsIntoItsOwnStoreAndSkipsSouths() {
        val s = GameState(board(List(6) { 1 }, 0, listOf(0, 0, 0, 0, 0, 10), 0), Opponent.FRIEND, Side.NORTH)
        val turn = s.sow(12)!!
        assertEquals(listOf(13, 0, 1, 2, 3, 4, 5, 7, 8, 9), turn.drops)
        assertFalse(6 in turn.drops)
    }

    @Test fun southSkipsNorthsStore() {
        val s = GameState(board(listOf(0, 0, 0, 0, 0, 9), 0, List(6) { 1 }, 0), Opponent.FRIEND)
        val turn = s.sow(5)!!
        assertEquals(listOf(6, 7, 8, 9, 10, 11, 12, 0, 1), turn.drops)
    }

    @Test fun landingInOwnEmptyPitCapturesOpposite() {
        // Pit 0 has 1 stone; it lands in empty pit 1, which faces pit 11 with 5 stones.
        val s = GameState(board(listOf(1, 0, 3, 3, 3, 3), 0, listOf(4, 4, 4, 4, 5, 4), 0), Opponent.FRIEND)
        val turn = s.sow(0)!!
        assertEquals(Capture(pit = 1, opposite = 11, stones = 6), turn.capture)
        assertEquals(0, turn.state.board[1])
        assertEquals(0, turn.state.board[11])
        assertEquals(6, turn.state.store(Side.SOUTH))
    }

    @Test fun noCaptureWhenOppositeIsEmpty() {
        val s = GameState(board(listOf(1, 0, 3, 3, 3, 3), 0, listOf(4, 4, 4, 4, 0, 4), 0), Opponent.FRIEND)
        val turn = s.sow(0)!!
        assertNull(turn.capture)
        assertEquals(1, turn.state.board[1])
    }

    @Test fun noCaptureOnOpponentsSide() {
        // Pit 5 has 2 stones: store, then empty pit 7 on North's side.
        val s = GameState(board(listOf(1, 1, 1, 1, 1, 2), 0, listOf(0, 4, 4, 4, 4, 4), 0), Opponent.FRIEND)
        val turn = s.sow(5)!!
        assertNull(turn.capture)
        assertEquals(1, turn.state.board[7])
    }

    @Test fun fullLapBackIntoTheStartPitCaptures() {
        // 13 stones from pit 0 go all the way round and land in pit 0, which was emptied.
        val s = GameState(board(listOf(13, 0, 0, 0, 0, 1), 0, listOf(1, 1, 1, 1, 1, 1), 0), Opponent.FRIEND)
        val turn = s.sow(0)!!
        assertEquals(0, turn.drops.last())
        assertEquals(Capture(pit = 0, opposite = 12, stones = 3), turn.capture)
    }

    @Test fun emptyRowEndsGameAndSweepsTheRest() {
        // South's only stone goes to the store; North keeps 10 on its side.
        val s = GameState(board(listOf(0, 0, 0, 0, 0, 1), 20, listOf(2, 2, 2, 2, 1, 1), 10), Opponent.FRIEND)
        val turn = s.sow(5)!!
        assertTrue(turn.swept)
        assertFalse(turn.extraTurn)
        assertTrue(turn.state.isOver)
        assertEquals(21, turn.state.store(Side.SOUTH))
        assertEquals(20, turn.state.store(Side.NORTH))
        assertEquals(Side.SOUTH, turn.state.winner)
        assertTrue(turn.state.legalPits.isEmpty())
    }

    @Test fun equalStoresIsADraw() {
        val s = GameState(board(List(6) { 0 }, 24, List(6) { 0 }, 24), Opponent.FRIEND)
        assertTrue(s.isOver)
        assertTrue(s.isDraw)
        assertNull(s.winner)
    }

    @Test fun illegalMovesAreRejected() {
        assertNull(start.sow(7)) // North's pit on South's turn
        assertNull(start.sow(6)) // a store
        assertNull(start.sow(-1))
        val s = GameState(board(listOf(0, 4, 4, 4, 4, 4), 0, List(6) { 4 }, 0), Opponent.FRIEND)
        assertNull(s.sow(0)) // empty pit
    }

    @Test fun randomGamesConserveStonesAndEnd() {
        val random = kotlin.random.Random(7)
        repeat(300) {
            var s = GameState.start(Opponent.FRIEND)
            var turns = 0
            while (!s.isOver) {
                s = s.sow(s.legalPits.random(random))!!.state
                assertEquals(48, s.board.sum())
                assertTrue(s.board.all { it >= 0 })
                assertTrue(++turns < 500)
            }
            assertEquals(48, s.store(Side.SOUTH) + s.store(Side.NORTH))
        }
    }
}
