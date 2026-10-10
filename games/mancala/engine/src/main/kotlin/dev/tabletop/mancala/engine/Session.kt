package dev.tabletop.mancala.engine

import kotlinx.serialization.Serializable

/** A game in progress with its undo history. */
@Serializable
data class Session(val state: GameState, val history: List<GameState> = emptyList()) {
    /** Against the computer, undo goes back to the player's previous turn, past the computer's replies. */
    private val undoIndex: Int
        get() = if (state.opponent.isComputer) history.indexOfLast { it.toMove == Side.SOUTH } else history.lastIndex

    val canUndo: Boolean get() = undoIndex >= 0

    fun play(pit: Int): Pair<Session, Turn>? {
        val turn = state.sow(pit) ?: return null
        return Session(turn.state, (history + state).takeLast(MAX_HISTORY)) to turn
    }

    fun undo(): Session? {
        val i = undoIndex.takeIf { it >= 0 } ?: return null
        return Session(history[i], history.take(i))
    }

    companion object {
        const val MAX_HISTORY = 500
    }
}
