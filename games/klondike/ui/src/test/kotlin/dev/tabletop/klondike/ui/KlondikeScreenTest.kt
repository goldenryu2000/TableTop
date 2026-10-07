package dev.tabletop.klondike.ui

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.tabletop.cards.Card
import dev.tabletop.cards.Rank
import dev.tabletop.cards.Suit
import dev.tabletop.game.GameServices
import dev.tabletop.game.InMemoryKeyValueStore
import dev.tabletop.game.StatsRepository
import dev.tabletop.game.TabletopJson
import dev.tabletop.game.saveKey
import dev.tabletop.klondike.engine.DrawMode
import dev.tabletop.klondike.engine.GameState
import dev.tabletop.klondike.engine.Session
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class KlondikeScreenTest {
    @get:Rule val rule = createComposeRule()

    private fun launch(store: InMemoryKeyValueStore = InMemoryKeyValueStore()): KlondikeViewModel {
        val vm = KlondikeViewModel(GameServices(store, RecordingFeedback(), StatsRepository(store)), newSeed = { 5L })
        rule.setContent { KlondikeScreen(vm, onExit = {}) }
        rule.waitForIdle()
        return vm
    }

    @Test fun tappingStockDraws() {
        val vm = launch()
        rule.onNodeWithTag("stock").performClick()
        rule.waitForIdle()
        assertEquals(1, vm.ui.value!!.state.waste.size)
        rule.onNodeWithTag("moves").assertTextEquals("MOVES", "1")
    }

    @Test fun undoRevertsDraw() {
        val vm = launch()
        rule.onNodeWithTag("stock").performClick()
        rule.onNodeWithTag("undo").performClick()
        rule.waitForIdle()
        assertEquals(0, vm.ui.value!!.state.waste.size)
    }

    @Test fun tappingAceSendsItHome() {
        val ace = Card(Suit.SPADES, Rank.ACE)
        val base = GameState.deal(5L, DrawMode.ONE)
        val tableau = base.tableau.map { pile ->
            pile.filter { it.card != ace }.let { p -> if (p.isNotEmpty() && !p.last().faceUp) p.dropLast(1) + p.last().copy(faceUp = true) else p }
        }
        val state = base.copy(stock = base.stock - ace, tableau = tableau, waste = listOf(ace), moves = 1)
        val store = InMemoryKeyValueStore()
        runBlocking { store.write(saveKey(KLONDIKE_ID), TabletopJson.encodeToString(SavedGame.serializer(), SavedGame(Session(state), true))) }
        val vm = launch(store)
        rule.onNodeWithTag("card-A♠").performClick()
        rule.waitForIdle()
        assertEquals(listOf(ace), vm.ui.value!!.state.foundation(Suit.SPADES))
    }
}
