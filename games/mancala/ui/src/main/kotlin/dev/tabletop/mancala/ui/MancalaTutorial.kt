package dev.tabletop.mancala.ui

import dev.tabletop.game.TutorialPage
import dev.tabletop.mancala.engine.BOARD_SIZE
import dev.tabletop.mancala.engine.Side

/** A board with four stones in every pit, changed at the given indices. */
private fun counts(vararg changes: Pair<Int, Int>): List<Int> {
    val b = MutableList(BOARD_SIZE) { if (it == Side.SOUTH.store || it == Side.NORTH.store) 0 else 4 }
    changes.forEach { (i, n) -> b[i] = n }
    return b
}

internal val MancalaTutorial = listOf(
    TutorialPage(
        "Your side",
        "You own the left column of pits and the store at the bottom. The other player owns the right column and the store at the top.",
    ) { m ->
        MiniBoardView(MiniBoard(counts(), ring = Side.SOUTH.pits.toSet(), dim = (Side.NORTH.pits + Side.NORTH.store).toSet()), m)
    },
    TutorialPage(
        "Sow the stones",
        "Tap one of your pits. Its stones go one per pit counter-clockwise: down your side, into your store, then up the other side. You never drop a stone in your opponent's store.",
    ) { m ->
        MiniBoardView(
            MiniBoard(counts(3 to 0, 4 to 5, 5 to 5, 6 to 1, 7 to 5, 8 to 5), rows = 3..5, northStore = false, ring = setOf(3), path = listOf(3, 4, 5, 6, 7, 8)),
            m,
        )
    },
    TutorialPage(
        "Land in your store, go again",
        "If your last stone drops into your own store, you take another turn. Pits that hold exactly enough stones to reach your store are worth spotting.",
    ) { m ->
        MiniBoardView(
            MiniBoard(counts(4 to 0, 5 to 5, 6 to 1), rows = 3..5, northStore = false, ring = setOf(4, 6), path = listOf(4, 5, 6)),
            m,
        )
    },
    TutorialPage(
        "Capture",
        "If your last stone lands in an empty pit on your side, you take it and every stone in the pit beside it on the other side. They all go into your store.",
    ) { m ->
        MiniBoardView(
            MiniBoard(counts(1 to 1, 2 to 0, 10 to 7), rows = 1..3, northStore = false, southStore = false, ring = setOf(2, 10), path = listOf(1, 2)),
            m,
        )
    },
    TutorialPage(
        "The end",
        "When either side has no stones left in its pits, the game ends. The other player keeps whatever is still on their side. Whoever has more stones in their store wins.",
    ) { m ->
        MiniBoardView(
            MiniBoard(
                listOf(0, 0, 0, 0, 0, 0, 25, 2, 0, 3, 1, 0, 2, 15),
                ring = Side.NORTH.pits.filter { it != 8 && it != 11 }.toSet(),
                dim = Side.SOUTH.pits.toSet(),
            ),
            m,
        )
    },
)
