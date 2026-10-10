package dev.tabletop.mancala.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.tabletop.game.GameEntry

object MancalaGame {
    val entry = GameEntry(
        id = MANCALA_ID,
        title = "Mancala",
        tagline = "Sow the stones, outwit the board.",
        palette = MancalaPalette,
        illustration = { modifier ->
            MiniBoardView(
                MiniBoard(
                    listOf(4, 4, 4, 2, 5, 3, 7, 4, 4, 4, 4, 4, 4, 0),
                    rows = 3..5,
                    northStore = false,
                ),
                modifier.rotate(-12f),
            )
        },
        tutorial = MancalaTutorial,
        scoreLabel = "Best margin",
        timed = false,
        content = { services, onExit, onHowToPlay ->
            val vm = viewModel { MancalaViewModel(services) }
            MancalaScreen(vm, onExit, onHowToPlay)
        },
    )
}
