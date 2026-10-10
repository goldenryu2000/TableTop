package dev.tabletop.spider.ui

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
import dev.tabletop.cards.ui.CardSlot
import dev.tabletop.solitaire.ui.CardTable
import dev.tabletop.solitaire.ui.HintOutline
import dev.tabletop.solitaire.ui.WinCascade
import dev.tabletop.spider.engine.Move
import dev.tabletop.spider.engine.pickUp

@Composable
internal fun SpiderTable(ui: SpiderUiState, viewModel: SpiderViewModel, modifier: Modifier = Modifier) {
    val state = ui.state
    val style = remember(ui.settings.fourColor) { spiderCardStyle(ui.settings.fourColor) }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val gap = with(density) { 4.dp.toPx() }
        val layout = remember(state, width, height) { computeSpiderLayout(state, width, height, gap) }

        fun Rect.place() = with(density) { Modifier.offset(left.toDp(), top.toDp()).size(this@place.width.toDp(), this@place.height.toDp()) }

        // Empty pile markers: the stock spot once it's dealt out, and empty columns.
        val stockSlot = Rect(Offset(layout.stock.right - layout.cardSize.width, layout.stock.top), layout.cardSize)
        CardSlot(null, SpiderColors.ink, stockSlot.place().testTag("stock"))
        layout.piles.forEach { (_, rect) -> CardSlot(null, SpiderColors.ink, Rect(rect.topLeft + Offset(gap / 2, 0f), layout.cardSize).place()) }

        if (state.isWon) {
            val finished = remember(layout) { layout.cards.filter { it.pile == null && it.faceUp }.sortedByDescending { it.z } }
            WinCascade(finished, layout.cardSize, style, height)
        } else {
            CardTable(
                layout = layout,
                style = style,
                spawn = layout.stock.topLeft,
                fresh = state.moves == 0,
                seed = state.seed,
                acceptsInput = true,
                hinted = remember(ui.hint, state) { hintedCards(ui) },
                hintColor = SpiderColors.accent,
                onTap = { pos, card ->
                    when {
                        layout.stock.contains(pos) -> viewModel.onDeal()
                        card != null && card.faceUp && card.pile != null -> {
                            val column = card.pile!!
                            viewModel.onTapCard(column, state.tableau[column].size - card.index)
                        }
                    }
                },
                pickUp = { p ->
                    val column = p.pile ?: return@CardTable null
                    state.pickUp(column, state.tableau[column].size - p.index)?.map { it.id }
                },
                onPickup = viewModel::onPickup,
                onDrop = { lead, count, candidates -> lead.pile?.let { viewModel.onDrop(it, count, candidates) } },
                cardModifier = { Modifier.testTag("card-${it.key}") },
            )
            when (val hint = ui.hint) {
                Move.Deal -> HintOutline(SpiderColors.accent, layout.stock.place())
                is Move.Transfer -> {
                    val top = layout.cards.filter { it.pile == hint.to }.maxByOrNull { it.index }?.topLeft
                        ?: layout.piles.getValue(hint.to).topLeft + Offset(gap / 2, 0f)
                    HintOutline(SpiderColors.accent, Rect(top, layout.cardSize).place())
                }
                null -> Unit
            }
        }
    }
}

private fun hintedCards(ui: SpiderUiState): Set<Any> = when (val hint = ui.hint) {
    is Move.Transfer -> ui.state.pickUp(hint.from, hint.count)?.map { it.id }?.toSet().orEmpty()
    else -> emptySet()
}
