package dev.tabletop.spider.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Plays many seeded games, mostly following hints, and checks invariants after every move. */
class RandomPlayTest {
    @Test fun invariantsHoldAcrossManyGames() {
        val random = Random(2024)
        repeat(60) { game ->
            var session = Session(GameState.deal(random.nextLong(), Suits.entries[game % 3]))
            var steps = 0
            while (!session.state.isWon && steps < 600) {
                val s = session.state
                val move = if (random.nextInt(10) < 8) s.hint() else randomMove(s, random)
                session = (move?.let { session.play(it) } ?: break)
                if (random.nextInt(25) == 0) session = session.undo() ?: session
                check(session.state)
                steps++
            }
        }
    }

    @Test fun followingHintsNeverGoesInCircles() {
        for (suits in Suits.entries) repeat(40) { g ->
            var s = GameState.deal(g * 7919L, suits)
            val seen = HashSet<GameState>()
            while (!s.isWon) {
                assertTrue("$suits game $g repeats a position", seen.add(s.copy(moves = 0, score = 0)))
                s = s.apply(s.hint() ?: break)!!
            }
        }
    }

    private fun randomMove(s: GameState, random: Random): Move? {
        val moves = buildList {
            if (s.canDeal) add(Move.Deal)
            for (from in s.tableau.indices) for (count in 1..s.movableCount(from)) for (to in s.tableau.indices) {
                val m = Move.Transfer(from, to, count)
                if (s.apply(m) != null) add(m)
            }
        }
        return moves.randomOrNull(random)
    }

    private fun check(s: GameState) {
        val all = s.stock + s.tableau.flatten() + s.completed.flatten()
        assertEquals(104, all.size)
        assertEquals(104, all.map { it.id }.toSet().size)
        for (pile in s.tableau) {
            // Face-down cards only ever sit below face-up ones, and a non-empty column shows its top card.
            val firstUp = pile.indexOfFirst { it.faceUp }
            if (pile.isNotEmpty()) assertTrue(pile.last().faceUp)
            if (firstUp >= 0) assertTrue(pile.drop(firstUp).all { it.faceUp })
        }
        assertTrue(s.completed.all { it.size == 13 && it.map { c -> c.suit }.toSet().size == 1 })
        assertTrue(s.score >= 0)
    }
}
