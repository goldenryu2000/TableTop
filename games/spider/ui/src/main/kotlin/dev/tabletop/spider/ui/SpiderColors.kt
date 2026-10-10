package dev.tabletop.spider.ui

import androidx.compose.ui.graphics.Color
import dev.tabletop.cards.ui.CardStyle
import dev.tabletop.game.GamePalette

internal object SpiderColors {
    val table = Color(0xFF1E2A45)
    val tableEdge = Color(0xFF0F1626)
    val ink = Color(0xFFE6EAF4)
    val accent = Color(0xFFA9BCFF)
    val sheet = Color(0xFF233152)
    val back = Color(0xFF34477A)
}

internal fun spiderCardStyle(fourColor: Boolean) = CardStyle(fourColor = fourColor, back = SpiderColors.back)

val SpiderPalette = GamePalette(
    background = SpiderColors.table,
    surface = SpiderColors.sheet,
    accent = SpiderColors.accent,
    onBackground = SpiderColors.ink,
)
