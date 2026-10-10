package dev.tabletop.spider.engine

import kotlinx.serialization.Serializable

/** A game in progress with its undo history. */
@Serializable
data class Session(val state: GameState, val history: List<GameState> = emptyList()) {
    val canUndo: Boolean get() = history.isNotEmpty()

    fun play(move: Move): Session? {
        val next = state.apply(move) ?: return null
        return Session(next, (history + state).takeLast(MAX_HISTORY))
    }

    /** Restores the previous position; time keeps running and the undo counts as a move. */
    fun undo(): Session? {
        val previous = history.lastOrNull() ?: return null
        val restored = previous.copy(
            elapsedMs = state.elapsedMs,
            moves = state.moves + 1,
            score = (previous.score + Scoring.MOVE).coerceAtLeast(0),
        )
        return Session(restored, history.dropLast(1))
    }

    fun tick(ms: Long): Session = copy(state = state.copy(elapsedMs = state.elapsedMs + ms))

    companion object {
        const val MAX_HISTORY = 500
    }
}
