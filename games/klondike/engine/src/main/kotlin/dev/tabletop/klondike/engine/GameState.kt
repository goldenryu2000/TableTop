package dev.tabletop.klondike.engine

import dev.tabletop.cards.Card
import dev.tabletop.cards.Suit
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
enum class DrawMode(val count: Int) { ONE(1), THREE(3) }

@Serializable
data class TableauCard(val card: Card, val faceUp: Boolean)

/** Full Klondike position. Every list is bottom-first, so `last()` is the top card. */
@Serializable
data class GameState(
    val seed: Long,
    val drawMode: DrawMode,
    val stock: List<Card>,
    val waste: List<Card>,
    val foundations: List<List<Card>>,
    val tableau: List<List<TableauCard>>,
    val score: Int = 0,
    val moves: Int = 0,
    val elapsedMs: Long = 0,
) {
    fun foundation(suit: Suit): List<Card> = foundations[suit.ordinal]

    val isWon: Boolean get() = foundations.all { it.size == 13 }

    companion object {
        fun deal(seed: Long, drawMode: DrawMode): GameState {
            val cards = Card.deck.shuffled(Random(seed))
            var next = 0
            val tableau = List(7) { pile ->
                List(pile + 1) { i -> TableauCard(cards[next++], faceUp = i == pile) }
            }
            return GameState(
                seed = seed,
                drawMode = drawMode,
                stock = cards.drop(next),
                waste = emptyList(),
                foundations = List(4) { emptyList() },
                tableau = tableau,
            )
        }
    }
}
