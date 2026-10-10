package dev.tabletop.spider.ui

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
import dev.tabletop.cards.ui.PlayingCard
import dev.tabletop.game.GameEntry

object SpiderGame {
    val entry = GameEntry(
        id = SPIDER_ID,
        title = "Spider",
        tagline = "Two decks, eight runs, plenty of patience.",
        palette = SpiderPalette,
        illustration = { modifier ->
            val style = spiderCardStyle(fourColor = false)
            Box(modifier, contentAlignment = Alignment.Center) {
                listOf(Rank.KING, Rank.QUEEN, Rank.JACK).forEachIndexed { i, rank ->
                    PlayingCard(
                        Card(Suit.SPADES, rank),
                        true,
                        style,
                        Modifier
                            .offset(x = (-10 + i * 10).dp, y = (-22 + i * 22).dp)
                            .rotate(-6f + i * 3f)
                            .shadow(4.dp, RoundedCornerShape(5.dp))
                            .size(56.dp, 78.dp),
                    )
                }
            }
        },
        tutorial = SpiderTutorial,
        content = { services, onExit, onHowToPlay ->
            val vm = viewModel { SpiderViewModel(services) }
            SpiderScreen(vm, onExit, onHowToPlay)
        },
    )
}
