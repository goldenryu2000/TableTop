package dev.tabletop.mancala.ui

import dev.tabletop.feedback.FeedbackEvent
import dev.tabletop.game.GameServices
import dev.tabletop.game.InMemoryKeyValueStore
import dev.tabletop.game.StatsRepository
import dev.tabletop.game.TabletopJson
import dev.tabletop.game.saveKey
import dev.tabletop.mancala.engine.GameState
import dev.tabletop.mancala.engine.Opponent
import dev.tabletop.mancala.engine.Session
import dev.tabletop.mancala.engine.Side
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class MancalaViewModelTest {
    private val store = InMemoryKeyValueStore()
    private val feedback = RecordingFeedback()
    private val stats = StatsRepository(store)
    private fun vm() = MancalaViewModel(GameServices(store, feedback, stats), Random(1), computerDispatcher = Dispatchers.Main)
    private suspend fun savedRaw() = store.observe(saveKey(MANCALA_ID)).first()
    private suspend fun stats() = stats.observe(MANCALA_ID).first()
    private val MancalaViewModel.state get() = ui.value!!.state

    private suspend fun save(state: GameState, counted: Boolean = true) {
        store.write(saveKey(MANCALA_ID), TabletopJson.encodeToString(SavedGame.serializer(), SavedGame(Session(state), counted)))
    }

    private fun board(south: List<Int>, southStore: Int, north: List<Int>, northStore: Int) = south + southStore + north + northStore

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun freshStartIsAnUnsavedMediumGame() = runTest {
        val vm = vm()
        assertEquals(GameState.start(Opponent.MEDIUM), vm.state)
        assertTrue(vm.ui.value!!.acceptsInput)
        assertNull(savedRaw())
    }

    @Test fun moveReplaysStoneByStoneThenComputerReplies() = runTest {
        val vm = vm()
        vm.onPit(0)
        val ui = vm.ui.value!!
        assertTrue(ui.replaying)
        assertEquals(1, ui.state.moves)
        assertEquals(Side.NORTH, ui.state.toMove)
        advanceUntilIdle()
        val after = vm.ui.value!!
        assertFalse(after.busy)
        assertNull(after.shown)
        assertEquals(Side.SOUTH, after.state.toMove)
        assertTrue(after.state.moves >= 2)
        assertTrue(feedback.events.count { it == FeedbackEvent.Stone } >= 8)
        assertEquals(1, stats().played)
        assertNotNull(savedRaw())
    }

    @Test fun tapsAreIgnoredWhileBusy() = runTest {
        val vm = vm()
        vm.onPit(0)
        vm.onPit(1)
        assertEquals(1, vm.state.moves)
    }

    @Test fun illegalTapPlaysInvalid() = runTest {
        val vm = vm()
        feedback.events.clear()
        vm.onPit(8)
        assertEquals(listOf(FeedbackEvent.Invalid), feedback.events)
        assertEquals(0, vm.state.moves)
    }

    @Test fun resumedGameWaitingOnComputerContinues() = runTest {
        save(GameState.start(Opponent.EASY).sow(0)!!.state)
        val vm = vm()
        assertTrue(vm.ui.value!!.thinking)
        advanceUntilIdle()
        assertEquals(Side.SOUTH, vm.state.toMove)
    }

    @Test fun undoGoesBackPastTheComputersReply() = runTest {
        val vm = vm()
        vm.onPit(0)
        advanceUntilIdle()
        vm.onUndo()
        assertEquals(GameState.start(Opponent.MEDIUM).board, vm.state.board)
        assertEquals(Side.SOUTH, vm.state.toMove)
    }

    @Test fun winningRecordsTheMarginAndClearsTheSave() = runTest {
        save(GameState(board(listOf(0, 0, 0, 0, 0, 1), 30, listOf(1, 1, 1, 1, 1, 2), 10), Opponent.HARD))
        val vm = vm()
        vm.onPit(5)
        advanceUntilIdle()
        assertTrue(vm.state.isOver)
        val s = stats()
        assertEquals(1, s.won)
        assertEquals(14, s.bestScore)
        assertNull(s.bestTimeMs)
        assertNull(savedRaw())
        assertEquals(FeedbackEvent.Win, feedback.events.last())
    }

    @Test fun losingResetsTheStreak() = runTest {
        stats.update(MANCALA_ID) { it.recordStart().recordWin(null, 5) }
        save(GameState(board(listOf(0, 0, 0, 0, 0, 1), 5, listOf(4, 4, 4, 4, 2, 2), 22), Opponent.EASY))
        val vm = vm()
        vm.onPit(5)
        advanceUntilIdle()
        assertEquals(Side.NORTH, vm.state.winner)
        val s = stats()
        assertEquals(0, s.currentStreak)
        assertEquals(1, s.won)
        assertFalse(FeedbackEvent.Win in feedback.events)
    }

    @Test fun twoPlayerGamesAreNotCounted() = runTest {
        val vm = vm()
        vm.updateSettings { it.copy(opponent = Opponent.FRIEND) }
        assertEquals(Opponent.FRIEND, vm.state.opponent)
        vm.onPit(0)
        advanceUntilIdle()
        assertEquals(Side.NORTH, vm.state.toMove)
        assertTrue(vm.ui.value!!.acceptsInput)
        vm.onPit(7)
        advanceUntilIdle()
        assertEquals(2, vm.state.moves)
        assertEquals(0, stats().played)
    }

    @Test fun opponentChangeMidGameAppliesToTheNextGame() = runTest {
        val vm = vm()
        vm.onPit(0)
        advanceUntilIdle()
        vm.updateSettings { it.copy(opponent = Opponent.HARD) }
        assertEquals(Opponent.MEDIUM, vm.state.opponent)
        vm.onNewGame()
        assertEquals(GameState.start(Opponent.HARD), vm.state)
    }

    @Test fun abandoningAGameBreaksTheStreak() = runTest {
        stats.update(MANCALA_ID) { it.recordStart().recordWin(null, 5) }
        val vm = vm()
        vm.onPit(0)
        advanceUntilIdle()
        vm.onNewGame()
        advanceUntilIdle()
        assertEquals(0, stats().currentStreak)
        assertNull(savedRaw())
    }

    @Test fun corruptSaveStartsFresh() = runTest {
        store.write(saveKey(MANCALA_ID), "{garbage")
        assertEquals(GameState.start(Opponent.MEDIUM), vm().state)
    }
}
