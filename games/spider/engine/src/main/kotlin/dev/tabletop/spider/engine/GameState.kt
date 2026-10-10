package dev.tabletop.spider.engine

import dev.tabletop.cards.Card
import dev.tabletop.cards.Rank
import dev.tabletop.cards.Suit
import kotlinx.serialization.Serializable
import kotlin.random.Random

const val COLUMNS = 10
const val RUNS_TO_WIN = 8

/** How many suits the two decks use: fewer suits make same-suit runs far easier. */
@Serializable
enum class Suits(val count: Int, val suits: List<Suit>) {
    ONE(1, listOf(Suit.SPADES)),
    TWO(2, listOf(Suit.SPADES, Suit.HEARTS)),
    FOUR(4, Suit.entries),
}

/** One physical card. Two decks hold duplicates, so [id] (0-103) tells them apart. */
@Serializable
data class SpiderCard(val id: Int, val card: Card, val faceUp: Boolean = false) {
    val rank: Rank get() = card.rank
    val suit: Suit get() = card.suit
}

/**
 * Full Spider position. Every list is bottom-first, so `last()` is the top card. [completed] holds
 * finished runs, each King first.
 */
@Serializable
data class GameState(
    val seed: Long,
    val suits: Suits,
    val stock: List<SpiderCard>,
    val tableau: List<List<SpiderCard>>,
    val completed: List<List<SpiderCard>> = emptyList(),
    val score: Int = Scoring.START,
    val moves: Int = 0,
    val elapsedMs: Long = 0,
) {
    val isWon: Boolean get() = completed.size == RUNS_TO_WIN

    /** Deals left in the stock, ten cards each. */
    val dealsLeft: Int get() = stock.size / COLUMNS

    companion object {
        /** 104 cards: each of [suits]' suits repeated until there are eight full suits. */
        fun deck(suits: Suits): List<Card> =
            List(RUNS_TO_WIN / suits.count) { suits.suits }.flatten().flatMap { s -> Rank.entries.map { Card(s, it) } }

        fun deal(seed: Long, suits: Suits): GameState {
            val cards = deck(suits).mapIndexed { i, c -> SpiderCard(i, c) }.shuffled(Random(seed))
            var next = 0
            val tableau = List(COLUMNS) { column ->
                val size = if (column < 4) 6 else 5
                List(size) { i -> cards[next++].copy(faceUp = i == size - 1) }
            }
            return GameState(seed, suits, stock = cards.drop(next), tableau = tableau)
        }
    }
}

object Scoring {
    const val START = 500
    const val MOVE = -1
    const val RUN = 100
}

sealed interface Move {
    /** One face-up card from the stock onto every column. */
    data object Deal : Move

    /** The top [count] cards of column [from] onto column [to]. */
    data class Transfer(val from: Int, val to: Int, val count: Int = 1) : Move
}
