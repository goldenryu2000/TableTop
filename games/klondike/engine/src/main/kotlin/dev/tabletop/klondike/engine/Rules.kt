package dev.tabletop.klondike.engine

import dev.tabletop.cards.Card
import dev.tabletop.cards.Rank

/** Windows-style standard scoring. */
object Scoring {
    const val WASTE_TO_TABLEAU = 5
    const val WASTE_TO_FOUNDATION = 10
    const val TABLEAU_TO_FOUNDATION = 10
    const val TURN_OVER = 5
    const val FOUNDATION_TO_TABLEAU = -15
    const val RECYCLE_DRAW_ONE = -100
    const val UNDO = -2
}

fun GameState.apply(move: Move): GameState? = when (move) {
    Move.Draw -> draw()
    is Move.Transfer -> transfer(move)
}

fun GameState.topCard(pile: Pile): Card? = when (pile) {
    Pile.Waste -> waste.lastOrNull()
    is Pile.Foundation -> foundation(pile.suit).lastOrNull()
    is Pile.Tableau -> tableau.getOrNull(pile.index)?.lastOrNull()?.card
}

/** Cards that would leave [from], bottom-first, or null if they cannot be picked up together. */
fun GameState.pickUp(from: Pile, count: Int): List<Card>? {
    if (count < 1) return null
    return when (from) {
        Pile.Waste, is Pile.Foundation -> topCard(from)?.takeIf { count == 1 }?.let(::listOf)
        is Pile.Tableau -> {
            val pile = tableau.getOrNull(from.index) ?: return null
            if (count > pile.size) return null
            val run = pile.takeLast(count)
            if (!run.all { it.faceUp }) return null
            val cards = run.map { it.card }
            cards.takeIf { it.zipWithNext().all { (below, above) -> stacks(above, onto = below) } }
        }
    }
}

fun GameState.accepts(to: Pile, cards: List<Card>): Boolean {
    val first = cards.firstOrNull() ?: return false
    return when (to) {
        Pile.Waste -> false
        is Pile.Foundation ->
            cards.size == 1 && first.suit == to.suit && first.rank.value == foundation(to.suit).size + 1
        is Pile.Tableau -> {
            val pile = tableau.getOrNull(to.index) ?: return false
            val top = pile.lastOrNull()
            if (top == null) first.rank == Rank.KING else top.faceUp && stacks(first, onto = top.card)
        }
    }
}

private fun stacks(card: Card, onto: Card) = card.isRed != onto.isRed && card.rank.value == onto.rank.value - 1

private fun GameState.draw(): GameState? = when {
    stock.isNotEmpty() -> {
        val n = minOf(drawMode.count, stock.size)
        copy(stock = stock.dropLast(n), waste = waste + stock.takeLast(n).reversed(), moves = moves + 1)
    }
    waste.isNotEmpty() -> {
        val penalty = if (drawMode == DrawMode.ONE) Scoring.RECYCLE_DRAW_ONE else 0
        copy(
            stock = waste.reversed(),
            waste = emptyList(),
            moves = moves + 1,
            score = (score + penalty).coerceAtLeast(0),
        )
    }
    else -> null
}

private fun GameState.transfer(m: Move.Transfer): GameState? {
    if (m.from == m.to) return null
    val cards = pickUp(m.from, m.count) ?: return null
    if (!accepts(m.to, cards)) return null

    var gained = transferScore(m.from, m.to)
    var next = when (val from = m.from) {
        Pile.Waste -> copy(waste = waste.dropLast(1))
        is Pile.Foundation -> copy(foundations = foundations.replace(from.suit.ordinal) { it.dropLast(1) })
        is Pile.Tableau -> {
            var remaining = tableau[from.index].dropLast(m.count)
            val newTop = remaining.lastOrNull()
            if (newTop != null && !newTop.faceUp) {
                remaining = remaining.dropLast(1) + newTop.copy(faceUp = true)
                gained += Scoring.TURN_OVER
            }
            copy(tableau = tableau.replace(from.index) { remaining })
        }
    }
    next = when (val to = m.to) {
        is Pile.Foundation -> next.copy(foundations = next.foundations.replace(to.suit.ordinal) { it + cards })
        is Pile.Tableau -> next.copy(tableau = next.tableau.replace(to.index) { it + cards.map { c -> TableauCard(c, true) } })
        Pile.Waste -> return null
    }
    return next.copy(moves = moves + 1, score = (score + gained).coerceAtLeast(0))
}

private fun transferScore(from: Pile, to: Pile): Int = when {
    from == Pile.Waste && to is Pile.Tableau -> Scoring.WASTE_TO_TABLEAU
    from == Pile.Waste && to is Pile.Foundation -> Scoring.WASTE_TO_FOUNDATION
    from is Pile.Tableau && to is Pile.Foundation -> Scoring.TABLEAU_TO_FOUNDATION
    from is Pile.Foundation && to is Pile.Tableau -> Scoring.FOUNDATION_TO_TABLEAU
    else -> 0
}

private inline fun <T> List<T>.replace(index: Int, f: (T) -> T): List<T> =
    mapIndexed { i, v -> if (i == index) f(v) else v }
