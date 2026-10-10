package dev.tabletop.spider.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.tabletop.feedback.FeedbackEvent
import dev.tabletop.game.GameServices
import dev.tabletop.game.GameStats
import dev.tabletop.game.TabletopJson
import dev.tabletop.game.readJson
import dev.tabletop.game.saveKey
import dev.tabletop.game.writeJson
import dev.tabletop.spider.engine.GameState
import dev.tabletop.spider.engine.Move
import dev.tabletop.spider.engine.Session
import dev.tabletop.spider.engine.Suits
import dev.tabletop.spider.engine.bestMoveFor
import dev.tabletop.spider.engine.canDeal
import dev.tabletop.spider.engine.hint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlin.random.Random

const val SPIDER_ID = "spider"
const val SPIDER_SETTINGS_KEY = "spider_settings"
const val SAVE_DEBOUNCE_MS = 300L
const val NOTICE_MS = 2_500L

@Serializable
data class SpiderSettings(
    val suits: Suits = Suits.ONE,
    val tapToMove: Boolean = true,
    val fourColor: Boolean = false,
)

/** [counted] is true once this game has been added to "played". */
@Serializable
data class SavedGame(val session: Session, val counted: Boolean)

/** [notice] is a short message, like why a deal was refused. */
data class SpiderUiState(
    val session: Session,
    val settings: SpiderSettings,
    val hint: Move? = null,
    val notice: String? = null,
) {
    val state: GameState get() = session.state
}

class SpiderViewModel(
    private val services: GameServices,
    private val newSeed: () -> Long = { Random.nextLong() },
    private val encodeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _ui = MutableStateFlow<SpiderUiState?>(null)
    val ui: StateFlow<SpiderUiState?> = _ui.asStateFlow()

    val stats: StateFlow<GameStats> = services.stats.observe(SPIDER_ID)
        .stateIn(viewModelScope, SharingStarted.Eagerly, GameStats())

    private var counted = false
    private var visible = false
    private var timerJob: Job? = null
    private var saveJob: Job? = null
    private var noticeJob: Job? = null

    init {
        viewModelScope.launch {
            val settings = services.store.readJson(SPIDER_SETTINGS_KEY, SpiderSettings())
            val saved = services.store.readJson<SavedGame?>(saveKey(SPIDER_ID), null)
            counted = saved?.counted ?: false
            _ui.value = SpiderUiState(saved?.session ?: Session(GameState.deal(newSeed(), settings.suits)), settings)
            if (saved == null) services.feedback.play(FeedbackEvent.Deal)
            updateTimer()
        }
    }

    /** Current state, or null while loading or once the game is won. */
    private val playable: SpiderUiState? get() = _ui.value?.takeIf { !it.state.isWon }

    fun onDeal() {
        val ui = playable ?: return
        val state = ui.state
        when {
            state.stock.isEmpty() -> return
            !state.canDeal -> {
                services.feedback.play(FeedbackEvent.Invalid)
                showNotice("Fill every column before dealing")
            }
            play(Move.Deal) -> services.feedback.play(FeedbackEvent.Draw)
        }
    }

    fun onTapCard(column: Int, count: Int) {
        val ui = playable ?: return
        if (!ui.settings.tapToMove) return
        val move = ui.state.bestMoveFor(column, count)
        if (move == null) services.feedback.play(FeedbackEvent.Invalid) else play(move)
    }

    fun onPickup() {
        if (playable != null) services.feedback.play(FeedbackEvent.Pickup)
    }

    /** Tries each candidate column in order; returns whether the cards moved. */
    fun onDrop(from: Int, count: Int, candidates: List<Int>): Boolean {
        if (playable == null || candidates.isEmpty()) return false
        val moved = candidates.any { to -> play(Move.Transfer(from, to, count)) }
        if (!moved) services.feedback.play(FeedbackEvent.Invalid)
        return moved
    }

    fun onUndo() {
        val ui = playable ?: return
        val previous = ui.session.undo() ?: return
        commit(ui.copy(session = previous, hint = null))
        services.feedback.play(FeedbackEvent.Place)
    }

    fun onHint() {
        val ui = playable ?: return
        val hint = ui.state.hint()
        _ui.value = ui.copy(hint = hint)
        if (hint == null) showNotice("No helpful moves left. Try Undo or a new game")
    }

    fun onNewGame() {
        val ui = _ui.value ?: return
        val state = ui.state
        if (state.moves > 0 && !state.isWon) {
            viewModelScope.launch { services.stats.update(SPIDER_ID) { it.recordAbandon() } }
        }
        counted = false
        commit(ui.copy(session = Session(GameState.deal(newSeed(), ui.settings.suits)), hint = null, notice = null))
        services.feedback.play(FeedbackEvent.Deal)
        updateTimer()
    }

    fun updateSettings(f: (SpiderSettings) -> SpiderSettings) {
        val ui = _ui.value ?: return
        val settings = f(ui.settings)
        _ui.value = ui.copy(settings = settings)
        viewModelScope.launch { services.store.writeJson(SPIDER_SETTINGS_KEY, settings) }
    }

    fun setVisible(visible: Boolean) {
        this.visible = visible
        updateTimer()
        if (!visible) {
            saveJob?.cancel()
            viewModelScope.launch { save() }
        }
    }

    /** Applies [move]; plays feedback for finished runs and handles stats and the win. */
    private fun play(move: Move): Boolean {
        val ui = playable ?: return false
        val next = ui.session.play(move) ?: return false
        commit(ui.copy(session = next, hint = null))
        val finishedRun = next.state.completed.size > ui.state.completed.size
        when {
            finishedRun -> services.feedback.play(FeedbackEvent.Foundation)
            move is Move.Transfer -> services.feedback.play(FeedbackEvent.Place)
        }
        if (!counted) {
            counted = true
            viewModelScope.launch { services.stats.update(SPIDER_ID) { it.recordStart() } }
            updateTimer()
        }
        if (next.state.isWon) onWin(next.state)
        return true
    }

    private fun onWin(state: GameState) {
        services.feedback.play(FeedbackEvent.Win)
        updateTimer()
        viewModelScope.launch { services.stats.update(SPIDER_ID) { it.recordWin(state.elapsedMs, state.score) } }
    }

    private fun showNotice(text: String) {
        _ui.update { it?.copy(notice = text) }
        noticeJob?.cancel()
        noticeJob = viewModelScope.launch {
            delay(NOTICE_MS)
            _ui.update { it?.copy(notice = null) }
        }
    }

    private fun commit(ui: SpiderUiState) {
        _ui.value = ui
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            save()
        }
    }

    private suspend fun save() {
        val ui = _ui.value ?: return
        val keep = ui.state.moves > 0 && !ui.state.isWon
        val saved = if (keep) SavedGame(ui.session, counted) else null
        // The undo history makes this large; keep the encoding off the main thread.
        val json = saved?.let { withContext(encodeDispatcher) { TabletopJson.encodeToString(SavedGame.serializer(), it) } }
        services.store.write(saveKey(SPIDER_ID), json)
    }

    private fun updateTimer() {
        timerJob?.cancel()
        val state = _ui.value?.state ?: return
        if (!visible || state.moves == 0 || state.isWon) return
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                _ui.update { it?.copy(session = it.session.tick(1_000)) }
            }
        }
    }
}
