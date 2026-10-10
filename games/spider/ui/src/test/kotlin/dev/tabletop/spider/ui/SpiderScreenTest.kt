package dev.tabletop.spider.ui

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.tabletop.game.GameServices
import dev.tabletop.game.InMemoryKeyValueStore
import dev.tabletop.game.StatsRepository
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class SpiderScreenTest {
    @get:Rule val rule = createComposeRule()

    private fun launch(): SpiderViewModel {
        val store = InMemoryKeyValueStore()
        val vm = SpiderViewModel(GameServices(store, RecordingFeedback(), StatsRepository(store)), newSeed = { 5L })
        rule.setContent { SpiderScreen(vm, onExit = {}) }
        rule.waitForIdle()
        return vm
    }

    @Test fun tappingTheStockDeals() {
        val vm = launch()
        rule.onNodeWithTag("stock").performClick()
        rule.waitForIdle()
        assertEquals(40, vm.ui.value!!.state.stock.size)
        rule.onNodeWithTag("moves").assertTextEquals("MOVES", "1")
    }

    @Test fun undoRevertsTheDeal() {
        val vm = launch()
        rule.onNodeWithTag("stock").performClick()
        rule.onNodeWithTag("undo").performClick()
        rule.waitForIdle()
        assertEquals(50, vm.ui.value!!.state.stock.size)
    }
}
