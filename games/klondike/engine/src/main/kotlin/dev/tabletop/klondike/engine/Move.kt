package dev.tabletop.klondike.engine

import dev.tabletop.cards.Suit

sealed interface Pile {
    data object Waste : Pile
    data class Foundation(val suit: Suit) : Pile
    data class Tableau(val index: Int) : Pile
}

sealed interface Move {
    /** Deal from the stock, or recycle the waste when the stock is empty. */
    data object Draw : Move

    /** Move the top [count] cards of [from] onto [to]. */
    data class Transfer(val from: Pile, val to: Pile, val count: Int = 1) : Move
}
