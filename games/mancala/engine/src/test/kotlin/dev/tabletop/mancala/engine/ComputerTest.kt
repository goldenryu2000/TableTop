package dev.tabletop.mancala.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ComputerTest {
    @Test fun takesTheBigCapture() {
        // North to move. Pit 7 (1 stone) lands in empty pit 8, facing 9 stones in pit 4.
        val s = GameState(board(listOf(1, 1, 1, 1, 9, 1), 0, listOf(1, 0, 3, 3, 3, 3), 0), Opponent.MEDIUM, Side.NORTH)
        assertEquals(7, Computer.bestPit(s, depth = 1))
    }

    @Test fun takesTheExtraTurnWhenNothingElseMatters() {
        val s = GameState.start(Opponent.MEDIUM).copy(toMove = Side.NORTH)
        // Pit 9 has exactly 4 stones to reach North's store at 13.
        val pick = Computer.bestPit(s, depth = 1)
        assertEquals(9, pick)
    }

    @Test fun alwaysPicksALegalPit() {
        val random = Random(3)
        repeat(50) {
            var s = GameState.start(Opponent.EASY)
            while (!s.isOver) {
                val pit = if (s.toMove == Side.NORTH) Computer.choose(s, random) else s.legalPits.random(random)
                assertTrue(pit in s.legalPits)
                s = s.sow(pit)!!.state
            }
        }
    }

    @Test fun deeperSearchBeatsEasy() {
        val random = Random(11)
        var wins = 0
        val games = 20
        repeat(games) {
            var s = GameState.start(Opponent.EASY)
            while (!s.isOver) {
                // South plays at depth 6; North is the Easy computer.
                val pit = if (s.toMove == Side.SOUTH) Computer.bestPit(s, depth = 6) else Computer.choose(s, random)
                s = s.sow(pit)!!.state
            }
            if (s.winner == Side.SOUTH) wins++
        }
        assertTrue("won $wins of $games", wins >= games * 3 / 4)
    }

    @Test fun hardAnswersWithinItsBudget() {
        val s = GameState.start(Opponent.HARD).sow(0)!!.state
        val t0 = System.nanoTime()
        val pit = Computer.choose(s, Random(1))
        val ms = (System.nanoTime() - t0) / 1_000_000
        assertTrue(pit in s.legalPits)
        assertTrue("took $ms ms", ms < Computer.HARD_BUDGET_MS + 300)
    }
}
