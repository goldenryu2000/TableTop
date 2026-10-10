package dev.tabletop.solitaire.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.tabletop.cards.Card
import dev.tabletop.cards.ui.CardSlot
import dev.tabletop.cards.ui.CardStyle
import dev.tabletop.cards.ui.PlayingCard

/** One card in a tutorial illustration, offset from the center. A null [card] draws an empty slot. */
class Placed(val card: Card?, val dx: Dp, val dy: Dp, val faceUp: Boolean = true, val slot: String? = null)

/** A few cards laid out for a tutorial page. */
@Composable
fun TutorialCards(
    modifier: Modifier,
    slotColor: Color,
    vararg cards: Placed,
    style: CardStyle = CardStyle(),
    width: Dp = 62.dp,
) {
    val height = width * 1.4f
    Box(modifier, contentAlignment = Alignment.Center) {
        cards.forEach { p ->
            val m = Modifier.offset(p.dx, p.dy).size(width, height)
            if (p.card == null) {
                CardSlot(p.slot, slotColor, m)
            } else {
                PlayingCard(p.card, p.faceUp, style, m.shadow(3.dp, RoundedCornerShape(6.dp)))
            }
        }
    }
}
