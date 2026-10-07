package dev.tabletop.klondike.engine

import org.junit.Test

class RandomPlayTest {
    @Test fun hintDrivenPlayKeepsInvariants() {
        for (mode in DrawMode.entries) for (seed in 0L until 150L) {
            var session = Session(GameState.deal(seed, mode))
            repeat(400) {
                val move = session.state.hint() ?: return@repeat
                session = session.play(move) ?: throw AssertionError("hint $move is illegal")
                session.state.assertValid()
            }
            if (session.state.canAutoComplete) {
                session.state.autoCompleteMoves().forEach { session = session.play(it)!! }
                check(session.state.isWon)
            }
        }
    }
}
