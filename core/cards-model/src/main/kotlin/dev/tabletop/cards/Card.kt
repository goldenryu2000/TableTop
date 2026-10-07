package dev.tabletop.cards

import kotlinx.serialization.Serializable

@Serializable
enum class Suit(val symbol: String, val isRed: Boolean) {
    SPADES("♠", false), HEARTS("♥", true), DIAMONDS("♦", true), CLUBS("♣", false)
}

@Serializable
enum class Rank(val value: Int, val label: String) {
    ACE(1, "A"), TWO(2, "2"), THREE(3, "3"), FOUR(4, "4"), FIVE(5, "5"), SIX(6, "6"), SEVEN(7, "7"),
    EIGHT(8, "8"), NINE(9, "9"), TEN(10, "10"), JACK(11, "J"), QUEEN(12, "Q"), KING(13, "K")
}

@Serializable
data class Card(val suit: Suit, val rank: Rank) {
    val isRed: Boolean get() = suit.isRed

    override fun toString() = rank.label + suit.symbol

    companion object {
        val deck: List<Card> = Suit.entries.flatMap { s -> Rank.entries.map { Card(s, it) } }
    }
}
