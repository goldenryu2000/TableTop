package dev.tabletop.klondike.ui

import androidx.compose.ui.graphics.Color
import dev.tabletop.game.GamePalette

internal object KlondikeColors {
    val table = Color(0xFF1F3B33)
    val tableEdge = Color(0xFF12241F)
    val ink = Color(0xFFE6EEE9)
    val accent = Color(0xFFE8C07A)
    val sheet = Color(0xFF203F36)
}

val KlondikePalette = GamePalette(
    background = KlondikeColors.table,
    surface = KlondikeColors.sheet,
    accent = KlondikeColors.accent,
    onBackground = KlondikeColors.ink,
)
