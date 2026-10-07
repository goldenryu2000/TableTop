package dev.tabletop.klondike.engine

import dev.tabletop.cards.Rank

val GameState.canAutoComplete: Boolean
    get() = !isWon && stock.isEmpty() && waste.isEmpty() && tableau.all { pile -> pile.all { it.faceUp } }

/** Foundation moves that finish the game, lowest cards first. Empty unless [canAutoComplete]. */
fun GameState.autoCompleteMoves(): List<Move> {
    if (!canAutoComplete) return emptyList()
    val moves = mutableListOf<Move>()
    var s = this
    while (!s.isWon) {
        val step = s.tableau.indices
            .mapNotNull { i -> s.tableau[i].lastOrNull()?.let { i to it.card } }
            .sortedBy { (_, card) -> card.rank.value }
            .firstNotNullOfOrNull { (i, card) ->
                val m = Move.Transfer(Pile.Tableau(i), Pile.Foundation(card.suit))
                s.apply(m)?.let { m to it }
            } ?: return emptyList()
        moves += step.first
        s = step.second
    }
    return moves
}

/** Where a tap should send the top [count] cards of [from]: foundation first, then a non-empty column. */
fun GameState.bestMoveFor(from: Pile, count: Int = 1): Move.Transfer? {
    val cards = pickUp(from, count) ?: return null
    val targets = buildList {
        if (count == 1 && from !is Pile.Foundation) add(Pile.Foundation(cards.first().suit))
        tableau.indices.sortedBy { if (tableau[it].isEmpty()) 1 else 0 }.forEach { add(Pile.Tableau(it)) }
    }
    return targets.asSequence()
        .filter { it != from }
        .map { Move.Transfer(from, it, count) }
        .filterNot { isKingShuffle(it) }
        .firstOrNull { apply(it) != null }
}

/** A useful next move, or null when nothing is left to try. */
fun GameState.hint(): Move? {
    val sources = listOf(Pile.Waste) + tableau.indices.map { Pile.Tableau(it) }
    for (from in sources) {
        val card = topCard(from) ?: continue
        val m = Move.Transfer(from, Pile.Foundation(card.suit))
        if (apply(m) != null) return m
    }
    for (i in tableau.indices) {
        val pile = tableau[i]
        val firstUp = pile.indexOfFirst { it.faceUp }
        if (firstUp < 0 || (firstUp == 0 && pile[0].card.rank == Rank.KING)) continue
        val count = pile.size - firstUp
        for (j in tableau.indices) {
            if (j == i) continue
            val m = Move.Transfer(Pile.Tableau(i), Pile.Tableau(j), count)
            if (apply(m) != null) return m
        }
    }
    if (waste.isNotEmpty()) {
        for (j in tableau.indices) {
            val m = Move.Transfer(Pile.Waste, Pile.Tableau(j))
            if (apply(m) != null) return m
        }
    }
    return if (stock.isNotEmpty() || waste.isNotEmpty()) Move.Draw else null
}

/** Moving a whole column onto an empty column changes nothing. */
private fun GameState.isKingShuffle(m: Move.Transfer): Boolean {
    val from = m.from as? Pile.Tableau ?: return false
    val to = m.to as? Pile.Tableau ?: return false
    return tableau[to.index].isEmpty() && m.count == tableau[from.index].size
}
