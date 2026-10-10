package dev.tabletop.klondike.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.tabletop.cards.Card
import dev.tabletop.cards.ui.CardSlot
import dev.tabletop.cards.ui.CardStyle
import dev.tabletop.cards.ui.glyph
import dev.tabletop.klondike.engine.Move
import dev.tabletop.klondike.engine.Pile
import dev.tabletop.klondike.engine.pickUp
import dev.tabletop.solitaire.ui.CardTable
import dev.tabletop.solitaire.ui.HintOutline
import dev.tabletop.solitaire.ui.WinCascade

@Composable
internal fun Table(ui: KlondikeUiState, viewModel: KlondikeViewModel, modifier: Modifier = Modifier) {
    val state = ui.state
    val style = remember(ui.settings.fourColor) { CardStyle(fourColor = ui.settings.fourColor) }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val gap = with(density) { 6.dp.toPx() }
        val layout = remember(state, width, height, ui.settings.leftHanded) {
            computeLayout(state, width, height, gap, ui.settings.leftHanded)
        }

        fun Rect.place() = with(density) { Modifier.offset(left.toDp(), top.toDp()).size(this@place.width.toDp(), this@place.height.toDp()) }

        // Empty pile markers.
        CardSlot(if (state.waste.isNotEmpty()) "↻" else null, KlondikeColors.ink, layout.stock.place().testTag("stock"))
        layout.piles.forEach { (pile, rect) ->
            when (pile) {
                is Pile.Foundation -> CardSlot(glyph(pile.suit), KlondikeColors.ink, rect.place())
                is Pile.Tableau -> CardSlot("K", KlondikeColors.ink, Rect(rect.topLeft + Offset(gap / 2, 0f), layout.cardSize).place())
                Pile.Waste -> Unit
            }
        }

        if (state.isWon) {
            val foundation = remember(layout) {
                layout.cards.filter { it.pile is Pile.Foundation }
                    .sortedWith(compareByDescending<CardPlacement> { it.index }.thenBy { it.card.suit })
            }
            WinCascade(foundation, layout.cardSize, style, height)
        } else {
            CardTable(
                layout = layout,
                style = style,
                spawn = layout.stock.topLeft,
                fresh = state.moves == 0,
                seed = state.seed,
                acceptsInput = ui.acceptsInput,
                hinted = remember(ui.hint, state) { hintedCards(ui.hint, ui) },
                hintColor = KlondikeColors.accent,
                onTap = { pos, card ->
                    if (layout.stock.contains(pos)) {
                        viewModel.onDraw()
                    } else {
                        val p = card?.takeIf { it.faceUp }
                        val pile = p?.pile
                        val count = pile?.let { pickCount(ui, it, p.index) }
                        if (count != null) viewModel.onTapCard(pile, count)
                    }
                },
                pickUp = { p ->
                    val pile = p.pile ?: return@CardTable null
                    val count = pickCount(ui, pile, p.index) ?: return@CardTable null
                    ui.state.pickUp(pile, count)
                },
                onPickup = viewModel::onPickup,
                onDrop = { lead, count, candidates -> lead.pile?.let { viewModel.onDrop(it, count, candidates) } },
                cardModifier = { Modifier.testTag("card-${it.card}") },
            )
            ui.hint?.let { hint -> hintTarget(hint, layout)?.let { HintOutline(KlondikeColors.accent, it.place()) } }
        }
    }
}

private val KlondikeUiState.acceptsInput: Boolean get() = !autoFinishing && !state.isWon

/** How many cards a touch at [index] of [pile] picks up, or null if that card cannot be picked up. */
private fun pickCount(ui: KlondikeUiState, pile: Pile, index: Int): Int? = when (pile) {
    is Pile.Tableau -> ui.state.tableau[pile.index].size - index
    Pile.Waste -> 1.takeIf { index == ui.state.waste.lastIndex }
    is Pile.Foundation -> 1.takeIf { index == ui.state.foundation(pile.suit).lastIndex }
}

private fun hintedCards(hint: Move?, ui: KlondikeUiState): Set<Card> = when (hint) {
    is Move.Transfer -> ui.state.pickUp(hint.from, hint.count)?.toSet().orEmpty()
    else -> emptySet()
}

/** Where a hinted move lands: the stock for a draw, else the target pile's top card or slot. */
private fun hintTarget(hint: Move, layout: TableLayout): Rect? = when (hint) {
    Move.Draw -> layout.stock
    is Move.Transfer -> {
        val area = layout.piles[hint.to]
        when {
            area == null -> null
            hint.to is Pile.Tableau -> {
                val top = layout.cards.filter { it.pile == hint.to }.maxByOrNull { it.index }?.topLeft
                    ?: Offset(area.left + (area.width - layout.cardSize.width) / 2, area.top)
                Rect(top, layout.cardSize)
            }
            else -> area
        }
    }
}
