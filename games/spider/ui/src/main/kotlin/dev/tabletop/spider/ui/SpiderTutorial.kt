package dev.tabletop.spider.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.tabletop.cards.Card
import dev.tabletop.cards.Rank
import dev.tabletop.cards.Suit
import dev.tabletop.game.TutorialPage
import dev.tabletop.solitaire.ui.Placed
import dev.tabletop.solitaire.ui.TutorialCards

private fun c(rank: Rank, suit: Suit) = Card(suit, rank)

/** A column fanned downward from [top], centered at [dx]. */
private fun fanned(dx: Dp, top: Dp, vararg cards: Card, step: Dp = 22.dp) =
    cards.mapIndexed { i, card -> Placed(card, dx, top + step * i) }.toTypedArray()

private val style = spiderCardStyle(fourColor = false)

internal val SpiderTutorial = listOf(
    TutorialPage(
        "Build runs, King to Ace",
        "Arrange each suit in order from King down to Ace in a column. A finished run leaves the table. Clear eight runs to win.",
    ) { m ->
        TutorialCards(
            m,
            SpiderColors.ink,
            *fanned(0.dp, (-44).dp, c(Rank.KING, Suit.SPADES), c(Rank.QUEEN, Suit.SPADES), c(Rank.JACK, Suit.SPADES), c(Rank.TEN, Suit.SPADES), c(Rank.NINE, Suit.SPADES)),
            style = style,
        )
    },
    TutorialPage(
        "Any suit stacks, one suit moves",
        "Place a card on any card one rank higher, whatever its suit. But only cards in a single suit move together, so build in one suit where you can.",
    ) { m ->
        TutorialCards(
            m,
            SpiderColors.ink,
            *fanned((-44).dp, (-30).dp, c(Rank.NINE, Suit.SPADES), c(Rank.EIGHT, Suit.HEARTS)),
            *fanned(44.dp, (-30).dp, c(Rank.NINE, Suit.HEARTS), c(Rank.EIGHT, Suit.HEARTS), c(Rank.SEVEN, Suit.HEARTS)),
            style = style,
        )
    },
    TutorialPage(
        "Deal when you're stuck",
        "Tap the stock at the top right to deal one card onto every column. Every column needs at least one card first, and there are five deals.",
    ) { m ->
        TutorialCards(
            m,
            SpiderColors.ink,
            Placed(c(Rank.TWO, Suit.CLUBS), (-60).dp, 0.dp, faceUp = false),
            Placed(c(Rank.TWO, Suit.CLUBS), (-48).dp, 0.dp, faceUp = false),
            Placed(c(Rank.TWO, Suit.CLUBS), (-36).dp, 0.dp, faceUp = false),
            Placed(c(Rank.SIX, Suit.SPADES), 36.dp, (-12).dp),
            Placed(c(Rank.JACK, Suit.HEARTS), 60.dp, 12.dp),
            style = style,
        )
    },
    TutorialPage(
        "Empty columns are gold",
        "Any card or run can go into an empty column. Undo is unlimited and Hint suggests a move. Choose one, two or four suits in settings.",
    ) { m ->
        TutorialCards(
            m,
            SpiderColors.ink,
            Placed(null, (-40).dp, 0.dp),
            Placed(c(Rank.SEVEN, Suit.DIAMONDS), 40.dp, 0.dp),
            style = style,
        )
    },
)
