package dev.tabletop.klondike.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.tabletop.cards.Card
import dev.tabletop.cards.Rank
import dev.tabletop.cards.Suit
import dev.tabletop.cards.ui.CardSlot
import dev.tabletop.cards.ui.CardStyle
import dev.tabletop.cards.ui.PlayingCard
import dev.tabletop.game.TutorialPage

private val cardW = 62.dp
private val cardH = 87.dp

/** One card at an offset from the center of the illustration. */
private class Placed(val card: Card?, val dx: Dp, val dy: Dp, val faceUp: Boolean = true, val slot: String? = null)

@Composable
private fun Cards(modifier: Modifier, vararg cards: Placed) {
    val style = CardStyle()
    Box(modifier, contentAlignment = Alignment.Center) {
        cards.forEach { p ->
            val m = Modifier.offset(p.dx, p.dy).size(cardW, cardH)
            if (p.card == null) {
                CardSlot(p.slot, KlondikeColors.ink, m)
            } else {
                PlayingCard(p.card, p.faceUp, style, m.shadow(3.dp, RoundedCornerShape(6.dp)))
            }
        }
    }
}

private fun c(rank: Rank, suit: Suit) = Card(suit, rank)

internal val KlondikeTutorial = listOf(
    TutorialPage(
        "Build each suit up",
        "Move every card to the four foundations at the top, one suit each, from Ace up to King.",
    ) { m ->
        Cards(
            m,
            Placed(c(Rank.ACE, Suit.HEARTS), (-36).dp, 0.dp),
            Placed(c(Rank.TWO, Suit.HEARTS), 0.dp, 0.dp),
            Placed(c(Rank.THREE, Suit.HEARTS), 36.dp, 0.dp),
        )
    },
    TutorialPage(
        "Stack down, alternating colors",
        "On the table, put a card on one rank higher of the other color: a red 9 on a black 10. Drag a card to move it with everything on top, or tap it to send it to the best spot.",
    ) { m ->
        Cards(
            m,
            Placed(c(Rank.TEN, Suit.SPADES), 0.dp, (-36).dp),
            Placed(c(Rank.NINE, Suit.HEARTS), 0.dp, 0.dp),
            Placed(c(Rank.EIGHT, Suit.CLUBS), 0.dp, 36.dp),
        )
    },
    TutorialPage(
        "Draw from the stock",
        "Tap the stock to turn over new cards. When it runs out, tap again to turn the pile back over. Hidden cards on the table flip up once they're uncovered.",
    ) { m ->
        Cards(
            m,
            Placed(c(Rank.FIVE, Suit.CLUBS), (-40).dp, 0.dp, faceUp = false),
            Placed(c(Rank.QUEEN, Suit.DIAMONDS), 40.dp, 0.dp),
        )
    },
    TutorialPage(
        "Kings start new columns",
        "Only a King can go into an empty column. Stuck? Undo is unlimited, Hint shows a move, and Finish appears once the rest plays itself.",
    ) { m ->
        Cards(
            m,
            Placed(null, (-40).dp, 0.dp, slot = "K"),
            Placed(c(Rank.KING, Suit.SPADES), 40.dp, 0.dp),
        )
    },
)
