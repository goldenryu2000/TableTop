package dev.tabletop.klondike.ui

import dev.tabletop.cards.Card
import dev.tabletop.cards.Rank
import dev.tabletop.cards.Suit
import dev.tabletop.feedback.FeedbackEvent
import dev.tabletop.game.GameServices
import dev.tabletop.game.InMemoryKeyValueStore
import dev.tabletop.game.StatsRepository
import dev.tabletop.game.TabletopJson
import dev.tabletop.game.saveKey
import dev.tabletop.klondike.engine.DrawMode
import dev.tabletop.klondike.engine.GameState
import dev.tabletop.klondike.engine.Pile
import dev.tabletop.klondike.engine.Session
import dev.tabletop.klondike.engine.TableauCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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

@OptIn(ExperimentalCoroutinesApi::class)
class KlondikeViewModelTest {
    private val store = InMemoryKeyValueStore()
    private val feedback = RecordingFeedback()
    private val stats = StatsRepository(store)
    private fun vm() = KlondikeViewModel(GameServices(store, feedback, stats), newSeed = { 11L }, encodeDispatcher = Dispatchers.Main)
    private suspend fun savedRaw() = store.observe(saveKey(KLONDIKE_ID)).first()

    /** Everything on foundations except K♠, which sits alone in column 0. */
    private val oneMoveFromWin = GameState(
        seed = 1, drawMode = DrawMode.ONE, stock = emptyList(), waste = emptyList(),
        foundations = Suit.entries.map { s -> Rank.entries.filter { s != Suit.SPADES || it != Rank.KING }.map { Card(s, it) } },
        tableau = listOf(listOf(TableauCard(Card(Suit.SPADES, Rank.KING), true))) + List(6) { emptyList() },
        moves = 99, score = 400, elapsedMs = 60_000,
    )

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun freshStartDealsSeededGameWithoutSaving() = runTest {
        val vm = vm()
        assertEquals(GameState.deal(11L, DrawMode.ONE), vm.ui.value!!.state)
        assertNull(savedRaw())
        assertEquals(listOf(FeedbackEvent.Deal), feedback.events)
    }

    @Test fun firstMoveCountsGameAndSavesAfterDebounce() = runTest {
        val vm = vm()
        vm.onDraw()
        assertNull(savedRaw())
        advanceTimeBy(SAVE_DEBOUNCE_MS + 1); runCurrent()
        assertNotNull(savedRaw())
        assertEquals(1, stats.observe(KLONDIKE_ID).first().played)
        vm.onDraw()
        assertEquals(1, stats.observe(KLONDIKE_ID).first().played)
    }

    @Test fun pausingSavesImmediately() = runTest {
        val vm = vm()
        vm.onDraw()
        vm.setVisible(true)
        vm.setVisible(false)
        runCurrent()
        assertNotNull(savedRaw())
    }

    @Test fun resumesSavedGameWithUndo() = runTest {
        val first = vm()
        first.onDraw()
        first.setVisible(false); runCurrent()
        val resumed = vm()
        assertEquals(first.ui.value!!.session, resumed.ui.value!!.session)
        assertTrue(resumed.ui.value!!.session.canUndo)
    }

    @Test fun corruptSaveStartsFresh() = runTest {
        store.write(saveKey(KLONDIKE_ID), "{garbage")
        assertEquals(GameState.deal(11L, DrawMode.ONE), vm().ui.value!!.state)
    }

    @Test fun illegalDropPlaysInvalid() = runTest {
        val vm = vm()
        feedback.events.clear()
        assertFalse(vm.onDrop(Pile.Tableau(0), 1, listOf(Pile.Waste)))
        assertEquals(listOf(FeedbackEvent.Invalid), feedback.events)
    }

    @Test fun dropWithNoCandidatesIsSilent() = runTest {
        val vm = vm()
        feedback.events.clear()
        assertFalse(vm.onDrop(Pile.Tableau(0), 1, emptyList()))
        assertTrue(feedback.events.isEmpty())
    }

    @Test fun undoRestores() = runTest {
        val vm = vm()
        val before = vm.ui.value!!.state
        vm.onDraw()
        vm.onUndo()
        assertEquals(before.stock, vm.ui.value!!.state.stock)
    }

    @Test fun newGameAfterMovesBreaksStreakAndClearsSave() = runTest {
        stats.update(KLONDIKE_ID) { it.recordStart().recordWin(1, 1) }
        val vm = vm()
        vm.onDraw()
        advanceTimeBy(SAVE_DEBOUNCE_MS + 1); runCurrent()
        vm.onNewGame()
        advanceTimeBy(SAVE_DEBOUNCE_MS + 1); runCurrent()
        assertEquals(0, stats.observe(KLONDIKE_ID).first().currentStreak)
        assertNull(savedRaw())
        assertEquals(0, vm.ui.value!!.state.moves)
    }

    @Test fun winningRecordsStatsAndClearsSave() = runTest {
        store.write(saveKey(KLONDIKE_ID), TabletopJson.encodeToString(SavedGame.serializer(), SavedGame(Session(oneMoveFromWin), counted = true)))
        val vm = vm()
        vm.onTapCard(Pile.Tableau(0), 1)
        advanceTimeBy(SAVE_DEBOUNCE_MS + 1); runCurrent()
        assertTrue(vm.ui.value!!.state.isWon)
        val s = stats.observe(KLONDIKE_ID).first()
        assertEquals(1, s.won)
        assertEquals(60_000L, s.bestTimeMs)
        assertNull(savedRaw())
        assertEquals(FeedbackEvent.Win, feedback.events.last())
    }

    @Test fun inputIgnoredWhileAutoFinishing() = runTest {
        val almost = oneMoveFromWin.copy(
            foundations = oneMoveFromWin.foundations.toMutableList().also { it[Suit.HEARTS.ordinal] = it[Suit.HEARTS.ordinal].dropLast(1) },
            tableau = oneMoveFromWin.tableau.toMutableList().also { it[1] = listOf(TableauCard(Card(Suit.HEARTS, Rank.KING), true)) },
        )
        store.write(saveKey(KLONDIKE_ID), TabletopJson.encodeToString(SavedGame.serializer(), SavedGame(Session(almost), counted = true)))
        val vm = vm()
        vm.onAutoFinish()
        assertTrue(vm.ui.value!!.autoFinishing)
        val movesDuring = vm.ui.value!!.state.moves
        vm.onUndo(); vm.onNewGame(); vm.onDraw()
        assertEquals(movesDuring, vm.ui.value!!.state.moves)
        advanceTimeBy(5_000); runCurrent()
        assertTrue(vm.ui.value!!.state.isWon)
        assertFalse(vm.ui.value!!.autoFinishing)
    }

    @Test fun drawModeSettingAppliesToNextGame() = runTest {
        val vm = vm()
        vm.updateSettings { it.copy(drawMode = DrawMode.THREE) }
        assertEquals(DrawMode.ONE, vm.ui.value!!.state.drawMode)
        vm.onNewGame()
        assertEquals(DrawMode.THREE, vm.ui.value!!.state.drawMode)
    }

    @Test fun inputAfterWinIsIgnored() = runTest {
        store.write(saveKey(KLONDIKE_ID), TabletopJson.encodeToString(SavedGame.serializer(), SavedGame(Session(oneMoveFromWin), counted = true)))
        val vm = vm()
        vm.onTapCard(Pile.Tableau(0), 1)
        advanceTimeBy(SAVE_DEBOUNCE_MS + 1); runCurrent()
        vm.onTapCard(Pile.Foundation(Suit.SPADES), 1)
        assertFalse(vm.onDrop(Pile.Foundation(Suit.HEARTS), 1, listOf(Pile.Tableau(1))))
        vm.onUndo()
        advanceTimeBy(SAVE_DEBOUNCE_MS + 1); runCurrent()
        assertTrue(vm.ui.value!!.state.isWon)
        assertEquals(1, stats.observe(KLONDIKE_ID).first().won)
    }
}
