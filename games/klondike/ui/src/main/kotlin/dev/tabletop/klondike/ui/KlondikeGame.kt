package dev.tabletop.klondike.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.tabletop.cards.Card
import dev.tabletop.cards.Rank
import dev.tabletop.cards.Suit
import dev.tabletop.cards.ui.CardStyle
import dev.tabletop.cards.ui.PlayingCard
import dev.tabletop.game.GameEntry

object KlondikeGame {
    val entry = GameEntry(
        id = KLONDIKE_ID,
        title = "Klondike",
        tagline = "The classic patience, unhurried.",
        palette = KlondikePalette,
        illustration = { modifier ->
            val style = CardStyle()
            Box(modifier, contentAlignment = Alignment.Center) {
                listOf(
                    Triple(Card(Suit.HEARTS, Rank.KING), -14f, -22),
                    Triple(Card(Suit.CLUBS, Rank.QUEEN), 0f, 0),
                    Triple(Card(Suit.SPADES, Rank.ACE), 14f, 22),
                ).forEach { (card, angle, dx) ->
                    PlayingCard(
                        card,
                        true,
                        style,
                        Modifier
                            .offset(x = dx.dp)
                            .rotate(angle)
                            .shadow(4.dp, RoundedCornerShape(5.dp))
                            .size(56.dp, 78.dp),
                    )
                }
            }
        },
        tutorial = KlondikeTutorial,
        content = { services, onExit, onHowToPlay ->
            val vm = viewModel { KlondikeViewModel(services) }
            KlondikeScreen(vm, onExit, onHowToPlay)
        },
    )
}
