package dev.tabletop.klondike.engine

import dev.tabletop.cards.Card
import dev.tabletop.cards.Rank
import dev.tabletop.cards.Suit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/** Parses "AS", "10H", "QD", "KC". */
fun c(code: String): Card {
    val suit = when (code.last()) {
        'S' -> Suit.SPADES; 'H' -> Suit.HEARTS; 'D' -> Suit.DIAMONDS; 'C' -> Suit.CLUBS
        else -> error("bad suit in $code")
    }
    val rank = Rank.entries.first { it.label == code.dropLast(1).replace("T", "10") }
    return Card(suit, rank)
}

fun up(code: String) = TableauCard(c(code), faceUp = true)
fun down(code: String) = TableauCard(c(code), faceUp = false)

/** A state with only the given piles; foundations given as top rank per suit (0 = empty). */
fun state(
    stock: List<String> = emptyList(),
    waste: List<String> = emptyList(),
    foundationTops: Map<Suit, Int> = emptyMap(),
    tableau: List<List<TableauCard>> = List(7) { emptyList() },
    drawMode: DrawMode = DrawMode.ONE,
    score: Int = 0,
) = GameState(
    seed = 0,
    drawMode = drawMode,
    stock = stock.map(::c),
    waste = waste.map(::c),
    foundations = Suit.entries.map { s -> Rank.entries.take(foundationTops[s] ?: 0).map { Card(s, it) } },
    tableau = tableau + List(7 - tableau.size) { emptyList() },
    score = score,
)

fun GameState.allCards() = stock + waste + foundations.flatten() + tableau.flatten().map { it.card }

/** Structural invariants that must hold for any state reachable from a deal. */
fun GameState.assertValid() {
    val all = allCards()
    assertEquals(52, all.size)
    assertEquals(52, all.toSet().size)
    foundations.forEachIndexed { i, pile ->
        pile.forEachIndexed { r, card ->
            assertEquals(Suit.entries[i], card.suit)
            assertEquals(r + 1, card.rank.value)
        }
    }
    tableau.forEach { pile ->
        if (pile.isNotEmpty()) {
            assertTrue("top must be face up", pile.last().faceUp)
            val firstUp = pile.indexOfFirst { it.faceUp }
            assertTrue("face-down above face-up", pile.drop(firstUp).all { it.faceUp })
        }
    }
}
