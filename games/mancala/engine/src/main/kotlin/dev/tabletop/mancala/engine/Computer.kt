package dev.tabletop.mancala.engine

import kotlin.random.Random

/** Negamax with alpha-beta pruning. Scores are the mover's store minus the other store. */
object Computer {
    private const val EASY_DEPTH = 2
    private const val EASY_BLUNDER = 0.3
    private const val MEDIUM_DEPTH = 6
    private const val HARD_MAX_DEPTH = 14
    const val HARD_BUDGET_MS = 400L

    /** Picks a pit for the side to move, at the strength of the state's opponent. */
    fun choose(state: GameState, random: Random, budgetMs: Long = HARD_BUDGET_MS): Int {
        val legal = state.legalPits
        require(legal.isNotEmpty()) { "No legal move" }
        return when (state.opponent) {
            Opponent.EASY -> if (random.nextDouble() < EASY_BLUNDER) legal.random(random) else bestPit(state, EASY_DEPTH)
            Opponent.MEDIUM, Opponent.FRIEND -> bestPit(state, MEDIUM_DEPTH)
            Opponent.HARD -> deepest(state, budgetMs)
        }
    }

    /** Best pit searching [depth] moves ahead; ties go to the first pit in search order. */
    fun bestPit(state: GameState, depth: Int, deadline: Long = Long.MAX_VALUE): Int {
        val b = state.board.toIntArray()
        val side = state.toMove
        var best = -1
        var bestScore = Int.MIN_VALUE
        var alpha = -INF
        for (pit in ordered(b, side)) {
            val score = child(b, side, pit, depth, alpha, INF, deadline)
            if (score > bestScore) {
                bestScore = score
                best = pit
            }
            alpha = maxOf(alpha, score)
        }
        return best
    }

    /** Iterative deepening: keeps the result of the deepest search that finished within [budgetMs]. */
    private fun deepest(state: GameState, budgetMs: Long): Int {
        val deadline = System.nanoTime() + budgetMs * 1_000_000
        var best = bestPit(state, 1)
        for (depth in 2..HARD_MAX_DEPTH) {
            best = try {
                bestPit(state, depth, deadline)
            } catch (_: OutOfTime) {
                break
            }
        }
        return best
    }

    private fun child(b: IntArray, side: Side, pit: Int, depth: Int, alpha: Int, beta: Int, deadline: Long): Int {
        val c = b.copyOf()
        val last = Kalah.sow(c, side, pit)
        Kalah.capture(c, side, last)
        Kalah.sweepIfOver(c)
        return if (last == side.store) {
            search(c, side, depth - 1, alpha, beta, deadline)
        } else {
            -search(c, side.other, depth - 1, -beta, -alpha, deadline)
        }
    }

    private fun search(b: IntArray, side: Side, depth: Int, alpha0: Int, beta: Int, deadline: Long): Int {
        val diff = b[side.store] - b[side.other.store]
        if (side.pits.all { b[it] == 0 } || side.other.pits.all { b[it] == 0 }) return diff
        if (depth <= 0) return diff
        if (System.nanoTime() > deadline) throw OutOfTime
        var alpha = alpha0
        var best = -INF
        for (pit in ordered(b, side)) {
            val score = child(b, side, pit, depth, alpha, beta, deadline)
            if (score > best) best = score
            if (best > alpha) alpha = best
            if (alpha >= beta) break
        }
        return best
    }

    /** Legal pits, with moves that end in the mover's own store (an extra turn) first. */
    private fun ordered(b: IntArray, side: Side): List<Int> {
        val legal = side.pits.filter { b[it] > 0 }
        return legal.sortedByDescending { b[it] == side.store - it }
    }

    private const val INF = 1_000

    private object OutOfTime : RuntimeException() {
        private fun readResolve(): Any = OutOfTime
        override fun fillInStackTrace(): Throwable = this
    }
}
