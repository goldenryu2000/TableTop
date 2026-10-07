package dev.tabletop.cards.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.tabletop.cards.Card
import dev.tabletop.cards.Suit
import dev.tabletop.design.Motion

const val CARD_ASPECT = 5f / 7f

@Immutable
data class CardStyle(
    val face: Color = Color(0xFFFBF8F1),
    val edge: Color = Color(0x22000000),
    val black: Color = Color(0xFF1E2329),
    val red: Color = Color(0xFFC8423B),
    val fourColor: Boolean = false,
    val diamonds: Color = Color(0xFF2F6FB5),
    val clubs: Color = Color(0xFF2E7D4F),
    val back: Color = Color(0xFF2D5A4C),
    val backPattern: Color = Color(0x26FFFFFF),
) {
    fun colorOf(suit: Suit): Color = when {
        fourColor && suit == Suit.DIAMONDS -> diamonds
        fourColor && suit == Suit.CLUBS -> clubs
        suit.isRed -> red
        else -> black
    }
}

private fun cornerFor(width: Dp) = RoundedCornerShape(width * 0.09f)

/** A card that flips with a 3D turn when [faceUp] changes. */
@Composable
fun PlayingCard(card: Card, faceUp: Boolean, style: CardStyle, modifier: Modifier = Modifier) {
    val rotation by animateFloatAsState(if (faceUp) 0f else 180f, Motion.card(), label = "flip")
    BoxWithConstraints(
        modifier.graphicsLayer {
            rotationY = rotation
            cameraDistance = 14f * density
        },
    ) {
        if (rotation <= 90f) {
            CardFace(card, style, maxWidth)
        } else {
            CardBack(style, maxWidth, Modifier.graphicsLayer { rotationY = 180f })
        }
    }
}

@Composable
private fun CardFace(card: Card, style: CardStyle, width: Dp) {
    val shape = cornerFor(width)
    val color = style.colorOf(card.suit)
    val isFace = card.rank.value > 10
    val (rankSize, suitSize, centerSize, tracking) = with(LocalDensity.current) {
        val w = width.toPx()
        listOf(w * 0.30f, w * 0.24f, w * if (isFace) 0.46f else 0.52f, -w * 0.02f).map { it.toSp() }
    }
    Box(
        Modifier
            .fillMaxSize()
            .clip(shape)
            .background(style.face)
            .border(1.dp, style.edge, shape),
    ) {
        Row(
            Modifier.padding(start = width * 0.08f, top = width * 0.04f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                card.rank.label,
                style = TextStyle(color = color, fontSize = rankSize, fontWeight = FontWeight.SemiBold, letterSpacing = tracking),
            )
            Text(card.suit.symbol, style = TextStyle(color = color, fontSize = suitSize))
        }
        Text(
            if (isFace) card.rank.label else card.suit.symbol,
            modifier = Modifier.align(Alignment.Center).padding(top = width * 0.28f),
            style = TextStyle(
                color = color.copy(alpha = if (isFace) 0.9f else 1f),
                fontSize = centerSize,
                fontWeight = if (isFace) FontWeight.Light else FontWeight.Normal,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@Composable
private fun CardBack(style: CardStyle, width: Dp, modifier: Modifier = Modifier) {
    val shape = cornerFor(width)
    Box(
        modifier
            .fillMaxSize()
            .clip(shape)
            .background(style.back)
            .border(1.dp, style.edge, shape)
            .padding(width * 0.07f),
    ) {
        Canvas(Modifier.fillMaxSize().clip(cornerFor(width * 0.7f))) {
            val step = size.width / 5f
            var x = -size.height
            while (x < size.width) {
                drawLine(style.backPattern, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = step * 0.22f)
                x += step
            }
            drawRoundRect(style.backPattern, cornerRadius = CornerRadius(size.width * 0.08f), style = Stroke(width = 1.dp.toPx()))
        }
    }
}

/** Outline marking an empty pile, with an optional centered glyph. */
@Composable
fun CardSlot(label: String?, color: Color, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val shape = cornerFor(maxWidth)
        val glyph = with(LocalDensity.current) { (maxWidth.toPx() * 0.4f).toSp() }
        Box(
            Modifier.fillMaxSize().clip(shape).background(color.copy(alpha = 0.08f)).border(1.5.dp, color.copy(alpha = 0.25f), shape),
            contentAlignment = Alignment.Center,
        ) {
            if (label != null) Text(label, style = TextStyle(color = color.copy(alpha = 0.35f), fontSize = glyph))
        }
    }
}
