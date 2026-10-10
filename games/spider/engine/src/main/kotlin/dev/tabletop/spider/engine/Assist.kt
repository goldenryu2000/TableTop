package dev.tabletop.spider.engine

/** Where a tap should send the top [count] cards of [from]: a same-suit card, then any card, then an empty column. */
fun GameState.bestMoveFor(from: Int, count: Int): Move.Transfer? {
    val lead = pickUp(from, count)?.first() ?: return null
    return tableau.indices
        .filter { it != from && accepts(it, lead) }
        .sortedBy { to ->
            val top = tableau[to].lastOrNull()
            when {
                top == null -> 2
                top.suit == lead.suit -> 0
                else -> 1
            }
        }
        .firstOrNull()
        ?.let { Move.Transfer(from, it, count) }
}

/**
 * A useful next move: joining a same-suit run, uncovering a card or emptying a column, best first.
 * Falls back to dealing, or to filling an empty column so dealing becomes possible.
 */
fun GameState.hint(): Move? {
    if (isWon) return null
    var best: Move.Transfer? = null
    var bestScore = 0
    for (from in tableau.indices) {
        val pile = tableau[from]
        val movable = movableCount(from)
        for (count in 1..movable) {
            val lead = pile[pile.size - count]
            val below = pile.getOrNull(pile.size - count - 1)
            for (to in tableau.indices) {
                if (to == from || !accepts(to, lead)) continue
                val score = scoreMove(lead, below, tableau[to].lastOrNull(), count == movable, stock.isEmpty())
                if (score > bestScore) {
                    bestScore = score
                    best = Move.Transfer(from, to, count)
                }
            }
        }
    }
    best?.let { return it }
    if (canDeal) return Move.Deal
    val empty = tableau.indexOfFirst { it.isEmpty() }
    if (stock.isNotEmpty() && empty >= 0) {
        // An empty column blocks the deal: fill it, leaving at least one card behind. Moving a whole
        // run off hidden or unrelated cards beats splitting one.
        val from = tableau.indices
            .filter { tableau[it].size > 1 && movableCount(it) > 0 }
            .maxByOrNull { if (tableau[it].size > movableCount(it)) 1 else 0 }
        if (from != null) return Move.Transfer(from, empty, minOf(movableCount(from), tableau[from].size - 1))
    }
    return null
}

/** Zero for moves that don't help; otherwise higher is better. */
private fun scoreMove(lead: SpiderCard, below: SpiderCard?, target: SpiderCard?, wholeRun: Boolean, stockEmpty: Boolean): Int {
    val sameSuit = target != null && target.suit == lead.suit
    // Splitting a run only pays off when the piece joins its own suit.
    if (!wholeRun && !sameSuit) return 0
    val belowFits = below != null && below.faceUp && below.rank.value == lead.rank.value + 1
    // Already sitting on a fitting card: moving only helps if it trades an off-suit spot for a same-suit one.
    if (belowFits && (below!!.suit == lead.suit || !sameSuit)) return 0
    val reveals = below != null && !below.faceUp
    val empties = below == null
    // While cards remain to deal, an emptied column must be refilled before the next deal, so
    // suggesting it only leads in circles.
    if (empties && !stockEmpty) return 0
    // Moving a whole column into another empty column achieves nothing.
    if (target == null && (empties || !reveals)) return 0
    var score = 0
    if (sameSuit) score += 100
    if (reveals) score += 60
    if (empties) score += 50
    if (target == null) score -= 30
    return if (score > 0) score + lead.rank.value else 0
}
