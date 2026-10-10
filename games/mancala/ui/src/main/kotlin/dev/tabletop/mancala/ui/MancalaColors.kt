package dev.tabletop.mancala.ui

import androidx.compose.ui.graphics.Color
import dev.tabletop.game.GamePalette

internal object MancalaColors {
    val table = Color(0xFF3A2A21)
    val tableEdge = Color(0xFF1F1610)
    val wood = Color(0xFF9A6A45)
    val woodDark = Color(0xFF6E4629)
    val pit = Color(0xFF4A2D1B)
    val pitRim = Color(0xFFB48459)
    val ink = Color(0xFFF4EADF)
    val accent = Color(0xFFEBB173)
    val sheet = Color(0xFF47352A)

    /** Sea-glass pebbles. */
    val stones = listOf(
        Color(0xFF9CCBC2),
        Color(0xFFE9DCBC),
        Color(0xFFB9A8D8),
        Color(0xFFE8ABA2),
        Color(0xFF93B6DB),
    )
}

val MancalaPalette = GamePalette(
    background = MancalaColors.table,
    surface = MancalaColors.sheet,
    accent = MancalaColors.accent,
    onBackground = MancalaColors.ink,
)
