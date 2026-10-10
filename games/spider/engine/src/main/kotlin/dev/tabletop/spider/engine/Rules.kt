package dev.tabletop.spider.engine

import dev.tabletop.cards.Rank

/** True when [upper] can sit directly under [lower] in a moving run: same suit, one rank higher. */
private fun runs(upper: SpiderCard, lower: SpiderCard) =
    upper.faceUp && lower.faceUp && upper.suit == lower.suit && upper.rank.value == lower.rank.value + 1

/** The top [count] cards of [column] if they're face up and form one descending same-suit run. */
fun GameState.pickUp(column: Int, count: Int): List<SpiderCard>? {
    val pile = tableau.getOrNull(column) ?: return null
    if (count !in 1..pile.size) return null
    val cards = pile.takeLast(count)
    if (!cards.first().faceUp || cards.zipWithNext().any { (a, b) -> !runs(a, b) }) return null
    return cards
}

/** How many cards from the top of [column] move together: the longest same-suit run. */
fun GameState.movableCount(column: Int): Int {
    val pile = tableau[column]
    if (pile.isEmpty() || !pile.last().faceUp) return 0
    var n = 1
    while (n < pile.size && runs(pile[pile.size - n - 1], pile[pile.size - n])) n++
    return n
}

/** Whether a run led by [lead] may go onto [column]: any card one rank higher, or an empty column. */
fun GameState.accepts(column: Int, lead: SpiderCard): Boolean {
    val top = tableau[column].lastOrNull() ?: return true
    return top.faceUp && top.rank.value == lead.rank.value + 1
}

val GameState.canDeal: Boolean get() = stock.isNotEmpty() && tableau.none { it.isEmpty() }

/** Returns the new state, or null if [move] is illegal. */
fun GameState.apply(move: Move): GameState? = when (move) {
    Move.Deal -> deal()
    is Move.Transfer -> transfer(move)
}

private fun GameState.deal(): GameState? {
    if (!canDeal) return null
    val dealt = stock.take(COLUMNS)
    val columns = tableau.mapIndexed { i, pile -> pile + dealt[i].copy(faceUp = true) }
    return copy(stock = stock.drop(COLUMNS), tableau = columns).afterMove(columns.indices)
}

private fun GameState.transfer(m: Move.Transfer): GameState? {
    if (m.from == m.to || m.to !in tableau.indices) return null
    val cards = pickUp(m.from, m.count) ?: return null
    if (!accepts(m.to, cards.first())) return null
    val columns = tableau.toMutableList()
    columns[m.from] = flipTop(tableau[m.from].dropLast(m.count))
    columns[m.to] = tableau[m.to] + cards
    return copy(tableau = columns).afterMove(listOf(m.to))
}

/** Counts the move, then clears any finished run from [touched] columns. */
private fun GameState.afterMove(touched: Iterable<Int>): GameState {
    var s = copy(moves = moves + 1, score = (score + Scoring.MOVE).coerceAtLeast(0))
    for (column in touched) s = s.completeRun(column)
    return s
}

/** Moves a face-up King-to-Ace run of one suit from the top of [column] to [GameState.completed]. */
private fun GameState.completeRun(column: Int): GameState {
    val pile = tableau[column]
    if (pile.size < Rank.entries.size) return this
    val run = pile.takeLast(Rank.entries.size)
    if (run.first().rank != Rank.KING || run.zipWithNext().any { (a, b) -> !runs(a, b) }) return this
    val columns = tableau.toMutableList()
    columns[column] = flipTop(pile.dropLast(run.size))
    return copy(tableau = columns, completed = completed + listOf(run), score = score + Scoring.RUN)
}

private fun flipTop(pile: List<SpiderCard>): List<SpiderCard> =
    if (pile.isNotEmpty() && !pile.last().faceUp) pile.dropLast(1) + pile.last().copy(faceUp = true) else pile
