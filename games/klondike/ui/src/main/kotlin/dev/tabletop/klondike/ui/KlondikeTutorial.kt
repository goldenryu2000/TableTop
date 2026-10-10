package dev.tabletop.klondike.ui

import androidx.compose.ui.unit.dp
import dev.tabletop.cards.Card
import dev.tabletop.cards.Rank
import dev.tabletop.cards.Suit
import dev.tabletop.game.TutorialPage
import dev.tabletop.solitaire.ui.Placed
import dev.tabletop.solitaire.ui.TutorialCards

private fun c(rank: Rank, suit: Suit) = Card(suit, rank)

internal val KlondikeTutorial = listOf(
    TutorialPage(
        "Build each suit up",
        "Move every card to the four foundations at the top, one suit each, from Ace up to King.",
    ) { m ->
        TutorialCards(
            m,
            KlondikeColors.ink,
            Placed(c(Rank.ACE, Suit.HEARTS), (-36).dp, 0.dp),
            Placed(c(Rank.TWO, Suit.HEARTS), 0.dp, 0.dp),
            Placed(c(Rank.THREE, Suit.HEARTS), 36.dp, 0.dp),
        )
    },
    TutorialPage(
        "Stack down, alternating colors",
        "On the table, put a card on one rank higher of the other color: a red 9 on a black 10. Drag a card to move it with everything on top, or tap it to send it to the best spot.",
    ) { m ->
        TutorialCards(
            m,
            KlondikeColors.ink,
            Placed(c(Rank.TEN, Suit.SPADES), 0.dp, (-36).dp),
            Placed(c(Rank.NINE, Suit.HEARTS), 0.dp, 0.dp),
            Placed(c(Rank.EIGHT, Suit.CLUBS), 0.dp, 36.dp),
        )
    },
    TutorialPage(
        "Draw from the stock",
        "Tap the stock to turn over new cards. When it runs out, tap again to turn the pile back over. Hidden cards on the table flip up once they're uncovered.",
    ) { m ->
        TutorialCards(
            m,
            KlondikeColors.ink,
            Placed(c(Rank.FIVE, Suit.CLUBS), (-40).dp, 0.dp, faceUp = false),
            Placed(c(Rank.QUEEN, Suit.DIAMONDS), 40.dp, 0.dp),
        )
    },
    TutorialPage(
        "Kings start new columns",
        "Only a King can go into an empty column. Stuck? Undo is unlimited, Hint shows a move, and Finish appears once the rest plays itself.",
    ) { m ->
        TutorialCards(
            m,
            KlondikeColors.ink,
            Placed(null, (-40).dp, 0.dp, slot = "K"),
            Placed(c(Rank.KING, Suit.SPADES), 40.dp, 0.dp),
        )
    },
)
