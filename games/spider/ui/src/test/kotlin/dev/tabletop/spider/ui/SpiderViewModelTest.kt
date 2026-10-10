package dev.tabletop.spider.ui

import dev.tabletop.cards.Card
import dev.tabletop.cards.Rank
import dev.tabletop.cards.Suit
import dev.tabletop.feedback.FeedbackEvent
import dev.tabletop.game.GameServices
import dev.tabletop.game.InMemoryKeyValueStore
import dev.tabletop.game.StatsRepository
import dev.tabletop.game.TabletopJson
import dev.tabletop.game.saveKey
import dev.tabletop.spider.engine.GameState
import dev.tabletop.spider.engine.Session
import dev.tabletop.spider.engine.SpiderCard
import dev.tabletop.spider.engine.Suits
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpiderViewModelTest {
    private val store = InMemoryKeyValueStore()
    private val feedback = RecordingFeedback()
    private val stats = StatsRepository(store)
    private fun vm() = SpiderViewModel(GameServices(store, feedback, stats), newSeed = { 9L }, encodeDispatcher = Dispatchers.Main)
    private suspend fun savedRaw() = store.observe(saveKey(SPIDER_ID)).first()
    private val SpiderViewModel.state get() = ui.value!!.state

    private suspend fun save(state: GameState) {
        store.write(saveKey(SPIDER_ID), TabletopJson.encodeToString(SavedGame.serializer(), SavedGame(Session(state), counted = true)))
    }

    /** Seven runs done; column 0 holds K♠ to 2♠ and column 1 the A♠. */
    private fun oneMoveFromWin(): GameState {
        var id = 0
        fun card(rank: Rank, faceUp: Boolean = true) = SpiderCard(id++, Card(Suit.SPADES, rank), faceUp)
        val completed = List(7) { Rank.entries.reversed().map { card(it) } }
        val kingToTwo = Rank.entries.reversed().dropLast(1).map { card(it) }
        val tableau = listOf(kingToTwo, listOf(card(Rank.ACE))) + List(8) { emptyList() }
        return GameState(seed = 1, suits = Suits.ONE, stock = emptyList(), tableau = tableau, completed = completed, moves = 90, elapsedMs = 120_000)
    }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun freshStartDealsOneSuitByDefault() = runTest {
        val vm = vm()
        assertEquals(GameState.deal(9L, Suits.ONE), vm.state)
        assertNull(savedRaw())
        assertEquals(listOf(FeedbackEvent.Deal), feedback.events)
    }

    @Test fun dealCountsTheGameAndSaves() = runTest {
        val vm = vm()
        vm.onDeal()
        assertEquals(40, vm.state.stock.size)
        advanceTimeBy(SAVE_DEBOUNCE_MS + 1); runCurrent()
        assertNotNull(savedRaw())
        assertEquals(1, stats.observe(SPIDER_ID).first().played)
        assertEquals(FeedbackEvent.Draw, feedback.events.last())
    }

    @Test fun dealWithAnEmptyColumnIsRefusedWithANotice() = runTest {
        val start = GameState.deal(9L, Suits.ONE)
        save(start.copy(tableau = start.tableau.toMutableList().also { it[3] = emptyList() }, moves = 1))
        val vm = vm()
        feedback.events.clear()
        vm.onDeal()
        assertEquals(50, vm.state.stock.size)
        assertEquals(listOf(FeedbackEvent.Invalid), feedback.events)
        assertNotNull(vm.ui.value!!.notice)
        advanceTimeBy(NOTICE_MS + 1); runCurrent()
        assertNull(vm.ui.value!!.notice)
    }

    @Test fun resumesSavedGameWithUndo() = runTest {
        val first = vm()
        first.onDeal()
        first.setVisible(false); runCurrent()
        val resumed = vm()
        assertEquals(first.ui.value!!.session, resumed.ui.value!!.session)
        assertTrue(resumed.ui.value!!.session.canUndo)
    }

    @Test fun corruptSaveStartsFresh() = runTest {
        store.write(saveKey(SPIDER_ID), "{garbage")
        assertEquals(GameState.deal(9L, Suits.ONE), vm().state)
    }

    @Test fun winningByTapRecordsStatsAndClearsSave() = runTest {
        save(oneMoveFromWin())
        val vm = vm()
        vm.onTapCard(column = 1, count = 1)
        advanceTimeBy(SAVE_DEBOUNCE_MS + 1); runCurrent()
        assertTrue(vm.state.isWon)
        val s = stats.observe(SPIDER_ID).first()
        assertEquals(1, s.won)
        assertEquals(120_000L, s.bestTimeMs)
        assertNull(savedRaw())
        assertEquals(FeedbackEvent.Win, feedback.events.last())
        assertTrue(FeedbackEvent.Foundation in feedback.events)
    }

    @Test fun suitsSettingAppliesToNextGame() = runTest {
        val vm = vm()
        vm.updateSettings { it.copy(suits = Suits.FOUR) }
        assertEquals(Suits.ONE, vm.state.suits)
        vm.onNewGame()
        assertEquals(Suits.FOUR, vm.state.suits)
    }

    @Test fun newGameAfterMovesBreaksStreak() = runTest {
        stats.update(SPIDER_ID) { it.recordStart().recordWin(1, 1) }
        val vm = vm()
        vm.onDeal()
        vm.onNewGame()
        advanceTimeBy(SAVE_DEBOUNCE_MS + 1); runCurrent()
        assertEquals(0, stats.observe(SPIDER_ID).first().currentStreak)
        assertNull(savedRaw())
    }

    @Test fun hintWithNothingToDoShowsANotice() = runTest {
        save(GameState(seed = 1, suits = Suits.ONE, stock = emptyList(), tableau = List(10) { emptyList() }, moves = 3))
        val vm = vm()
        vm.onHint()
        assertNull(vm.ui.value!!.hint)
        assertNotNull(vm.ui.value!!.notice)
    }

    @Test fun illegalDropPlaysInvalid() = runTest {
        val nine = SpiderCard(0, Card(Suit.SPADES, Rank.NINE), faceUp = true)
        val five = SpiderCard(1, Card(Suit.SPADES, Rank.FIVE), faceUp = true)
        save(GameState(seed = 1, suits = Suits.ONE, stock = emptyList(), tableau = listOf(listOf(nine), listOf(five)) + List(8) { emptyList() }, moves = 1))
        val vm = vm()
        feedback.events.clear()
        assertEquals(false, vm.onDrop(from = 1, count = 1, candidates = listOf(0)))
        assertEquals(listOf(FeedbackEvent.Invalid), feedback.events)
    }
}
