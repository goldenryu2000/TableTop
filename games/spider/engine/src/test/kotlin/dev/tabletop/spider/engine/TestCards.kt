package dev.tabletop.spider.engine

import dev.tabletop.cards.Card
import dev.tabletop.cards.Rank
import dev.tabletop.cards.Suit

private var nextId = 1000

/** "9S" is the nine of spades, face up; a leading "-" makes it face down. Ids are unique per call. */
fun c(spec: String): SpiderCard {
    val down = spec.startsWith("-")
    val s = spec.removePrefix("-")
    val rank = Rank.entries.first { it.label == s.dropLast(1) }
    val suit = when (s.last()) {
        'S' -> Suit.SPADES
        'H' -> Suit.HEARTS
        'D' -> Suit.DIAMONDS
        else -> Suit.CLUBS
    }
    return SpiderCard(nextId++, Card(suit, rank), faceUp = !down)
}

fun column(vararg specs: String): List<SpiderCard> = specs.map(::c)

/** A full King-to-Ace run of [suit], face up. */
fun run(suit: String): List<SpiderCard> = Rank.entries.reversed().map { c(it.label + suit) }

/** A state with the given columns (padded to ten) and stock. */
fun state(vararg columns: List<SpiderCard>, stock: List<SpiderCard> = emptyList(), completed: Int = 0): GameState {
    val tableau = columns.toList() + List(COLUMNS - columns.size) { column("KD") }
    return GameState(
        seed = 0,
        suits = Suits.FOUR,
        stock = stock,
        tableau = tableau,
        completed = List(completed) { run("C") },
    )
}
