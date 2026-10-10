package dev.tabletop.mancala.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.tabletop.game.GameServices
import dev.tabletop.game.InMemoryKeyValueStore
import dev.tabletop.game.StatsRepository
import dev.tabletop.mancala.engine.Opponent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class MancalaScreenTest {
    @get:Rule val rule = createComposeRule()

    private fun launch(): MancalaViewModel {
        val store = InMemoryKeyValueStore()
        val vm = MancalaViewModel(GameServices(store, RecordingFeedback(), StatsRepository(store)))
        rule.setContent { MancalaScreen(vm, onExit = {}) }
        rule.waitForIdle()
        return vm
    }

    @Test fun startsOnYourTurn() {
        launch()
        rule.onNodeWithTag("status").assertTextEquals("Your turn")
        rule.onAllNodesWithContentDescription("Your pit, 4 stones").assertCountEquals(6)
        rule.onAllNodesWithContentDescription("Computer's pit, 4 stones").assertCountEquals(6)
    }

    @Test fun tappingYourPitPlaysIt() {
        val vm = launch()
        rule.onNodeWithTag("pit-2").performClick()
        rule.waitForIdle()
        assertEquals(1, vm.ui.value!!.state.moves)
    }

    @Test fun choosingAnOpponentBeforeMovingRestarts() {
        val vm = launch()
        rule.onNodeWithContentDescription("Game settings").performClick()
        rule.onNodeWithTag("opponent-easy").performClick()
        rule.waitForIdle()
        assertEquals(Opponent.EASY, vm.ui.value!!.state.opponent)
    }
}
