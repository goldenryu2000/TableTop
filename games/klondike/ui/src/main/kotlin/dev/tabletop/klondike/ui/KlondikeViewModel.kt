package dev.tabletop.klondike.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.tabletop.feedback.FeedbackEvent
import dev.tabletop.game.GameServices
import dev.tabletop.game.GameStats
import dev.tabletop.game.readJson
import dev.tabletop.game.saveKey
import dev.tabletop.game.TabletopJson
import dev.tabletop.game.writeJson
import dev.tabletop.klondike.engine.DrawMode
import dev.tabletop.klondike.engine.GameState
import dev.tabletop.klondike.engine.Move
import dev.tabletop.klondike.engine.Pile
import dev.tabletop.klondike.engine.Session
import dev.tabletop.klondike.engine.autoCompleteMoves
import dev.tabletop.klondike.engine.bestMoveFor
import dev.tabletop.klondike.engine.hint
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

const val KLONDIKE_ID = "klondike"
const val KLONDIKE_SETTINGS_KEY = "klondike_settings"
const val SAVE_DEBOUNCE_MS = 300L
private const val AUTO_FINISH_STEP_MS = 90L

@Serializable
data class KlondikeSettings(
    val drawMode: DrawMode = DrawMode.ONE,
    val tapToMove: Boolean = true,
    val fourColor: Boolean = false,
    val leftHanded: Boolean = false,
)

/** [counted] is true once this game has been added to "played". */
@Serializable
data class SavedGame(val session: Session, val counted: Boolean)

data class KlondikeUiState(
    val session: Session,
    val settings: KlondikeSettings,
    val hint: Move? = null,
    val autoFinishing: Boolean = false,
) {
    val state: GameState get() = session.state
}

class KlondikeViewModel(
    private val services: GameServices,
    private val newSeed: () -> Long = { Random.nextLong() },
    private val encodeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _ui = MutableStateFlow<KlondikeUiState?>(null)
    val ui: StateFlow<KlondikeUiState?> = _ui.asStateFlow()

    val stats: StateFlow<GameStats> = services.stats.observe(KLONDIKE_ID)
        .stateIn(viewModelScope, SharingStarted.Eagerly, GameStats())

    private var counted = false
    private var visible = false
    private var timerJob: Job? = null
    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            val settings = services.store.readJson(KLONDIKE_SETTINGS_KEY, KlondikeSettings())
            val saved = services.store.readJson<SavedGame?>(saveKey(KLONDIKE_ID), null)
            counted = saved?.counted ?: false
            _ui.value = KlondikeUiState(saved?.session ?: Session(GameState.deal(newSeed(), settings.drawMode)), settings)
            if (saved == null) services.feedback.play(FeedbackEvent.Deal)
            updateTimer()
        }
    }

    /** Current state, or null while loading or auto-finishing. */
    private val ready: KlondikeUiState? get() = _ui.value?.takeIf { !it.autoFinishing }

    /** Like [ready], but also null once the game is won: the table no longer takes moves. */
    private val playable: KlondikeUiState? get() = ready?.takeIf { !it.state.isWon }

    fun onDraw() {
        if (play(Move.Draw)) services.feedback.play(FeedbackEvent.Draw)
    }

    fun onTapCard(from: Pile, count: Int) {
        val ui = playable ?: return
        if (!ui.settings.tapToMove) return
        val move = ui.state.bestMoveFor(from, count)
        if (move == null) services.feedback.play(FeedbackEvent.Invalid) else play(move)
    }

    fun onPickup() {
        if (playable != null) services.feedback.play(FeedbackEvent.Pickup)
    }

    /** Tries each candidate in order; returns whether the cards moved. */
    fun onDrop(from: Pile, count: Int, candidates: List<Pile>): Boolean {
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
        _ui.value = ui.copy(hint = ui.state.hint())
    }

    fun onNewGame() {
        val ui = ready ?: return
        val state = ui.state
        if (state.moves > 0 && !state.isWon) {
            viewModelScope.launch { services.stats.update(KLONDIKE_ID) { it.recordAbandon() } }
        }
        counted = false
        commit(ui.copy(session = Session(GameState.deal(newSeed(), ui.settings.drawMode)), hint = null))
        services.feedback.play(FeedbackEvent.Deal)
        updateTimer()
    }

    fun onAutoFinish() {
        val ui = playable ?: return
        val moves = ui.state.autoCompleteMoves()
        if (moves.isEmpty()) return
        _ui.value = ui.copy(autoFinishing = true, hint = null)
        viewModelScope.launch {
            for (m in moves) {
                applyMove(m)
                delay(AUTO_FINISH_STEP_MS)
            }
            _ui.update { it?.copy(autoFinishing = false) }
        }
    }

    fun updateSettings(f: (KlondikeSettings) -> KlondikeSettings) {
        val ui = _ui.value ?: return
        val settings = f(ui.settings)
        _ui.value = ui.copy(settings = settings)
        viewModelScope.launch { services.store.writeJson(KLONDIKE_SETTINGS_KEY, settings) }
    }

    fun setVisible(visible: Boolean) {
        this.visible = visible
        updateTimer()
        if (!visible) {
            saveJob?.cancel()
            viewModelScope.launch { save() }
        }
    }

    private fun play(move: Move): Boolean = playable != null && applyMove(move)

    /** Applies [move] even during auto-finish; plays feedback and handles stats and the win. */
    private fun applyMove(move: Move): Boolean {
        val ui = _ui.value ?: return false
        val next = ui.session.play(move) ?: return false
        commit(ui.copy(session = next, hint = null))
        if (move is Move.Transfer) {
            services.feedback.play(if (move.to is Pile.Foundation) FeedbackEvent.Foundation else FeedbackEvent.Place)
        }
        if (!counted) {
            counted = true
            viewModelScope.launch { services.stats.update(KLONDIKE_ID) { it.recordStart() } }
            updateTimer()
        }
        if (next.state.isWon) onWin(next.state)
        return true
    }

    private fun onWin(state: GameState) {
        services.feedback.play(FeedbackEvent.Win)
        updateTimer()
        viewModelScope.launch { services.stats.update(KLONDIKE_ID) { it.recordWin(state.elapsedMs, state.score) } }
    }

    private fun commit(ui: KlondikeUiState) {
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
        services.store.write(saveKey(KLONDIKE_ID), json)
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
