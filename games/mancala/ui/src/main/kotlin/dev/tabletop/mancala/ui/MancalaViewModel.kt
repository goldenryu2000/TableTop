package dev.tabletop.mancala.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.tabletop.feedback.FeedbackEvent
import dev.tabletop.game.GameServices
import dev.tabletop.game.GameStats
import dev.tabletop.game.readJson
import dev.tabletop.game.saveKey
import dev.tabletop.game.writeJson
import dev.tabletop.mancala.engine.Computer
import dev.tabletop.mancala.engine.GameState
import dev.tabletop.mancala.engine.Opponent
import dev.tabletop.mancala.engine.Session
import dev.tabletop.mancala.engine.Side
import dev.tabletop.mancala.engine.Turn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.random.Random

const val MANCALA_ID = "mancala"
const val MANCALA_SETTINGS_KEY = "mancala_settings"
const val SAVE_DEBOUNCE_MS = 300L

/** Time between stones while a move replays. */
const val STEP_MS = 140L

/** The computer never answers faster than this, so its move doesn't feel abrupt. */
const val COMPUTER_MIN_MS = 600L

@Serializable
data class MancalaSettings(val opponent: Opponent = Opponent.MEDIUM)

/** [counted] is true once this game has been added to "played". */
@Serializable
data class SavedGame(val session: Session, val counted: Boolean)

/**
 * [shown] is the board while a move replays stone by stone; the session already holds the result.
 * [lastPit] is where the latest stone landed.
 */
data class MancalaUiState(
    val session: Session,
    val settings: MancalaSettings,
    val shown: List<Int>? = null,
    val lastPit: Int? = null,
    val replaying: Boolean = false,
    val thinking: Boolean = false,
) {
    val state: GameState get() = session.state
    val board: List<Int> get() = shown ?: state.board
    val busy: Boolean get() = replaying || thinking

    /** True when the side to move is a person who may tap a pit now. */
    val acceptsInput: Boolean
        get() = !busy && !state.isOver && (state.toMove == Side.SOUTH || !state.opponent.isComputer)

    /** The side to move also moved last: their last stone landed in their store. */
    val goesAgain: Boolean get() = session.history.lastOrNull()?.toMove == state.toMove
}

class MancalaViewModel(
    private val services: GameServices,
    private val random: Random = Random.Default,
    private val computerDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _ui = MutableStateFlow<MancalaUiState?>(null)
    val ui: StateFlow<MancalaUiState?> = _ui.asStateFlow()

    val stats: StateFlow<GameStats> = services.stats.observe(MANCALA_ID)
        .stateIn(viewModelScope, SharingStarted.Eagerly, GameStats())

    /** Zero skips the stone-by-stone replay, for the system "remove animations" setting. */
    var stepMs = STEP_MS

    private var counted = false
    private var saveJob: Job? = null
    private var moveJob: Job? = null

    init {
        viewModelScope.launch {
            val settings = services.store.readJson(MANCALA_SETTINGS_KEY, MancalaSettings())
            val saved = services.store.readJson<SavedGame?>(saveKey(MANCALA_ID), null)
            counted = saved?.counted ?: false
            _ui.value = MancalaUiState(saved?.session ?: Session(GameState.start(settings.opponent)), settings)
            computerTurn()
        }
    }

    fun onPit(pit: Int) {
        val ui = _ui.value ?: return
        if (!ui.acceptsInput) return
        if (!play(pit)) services.feedback.play(FeedbackEvent.Invalid)
    }

    fun onUndo() {
        val ui = _ui.value?.takeIf { !it.busy && !it.state.isOver } ?: return
        val previous = ui.session.undo() ?: return
        commit(ui.copy(session = previous, lastPit = null))
        services.feedback.play(FeedbackEvent.Pickup)
    }

    fun onNewGame() {
        val ui = _ui.value ?: return
        moveJob?.cancel()
        val state = ui.state
        if (counted && !state.isOver) {
            viewModelScope.launch { services.stats.update(MANCALA_ID) { it.recordAbandon() } }
        }
        counted = false
        commit(MancalaUiState(Session(GameState.start(ui.settings.opponent)), ui.settings))
        services.feedback.play(FeedbackEvent.Stone)
    }

    /** A new opponent applies at once if the game hasn't started, otherwise from the next game. */
    fun updateSettings(f: (MancalaSettings) -> MancalaSettings) {
        val ui = _ui.value ?: return
        val settings = f(ui.settings)
        viewModelScope.launch { services.store.writeJson(MANCALA_SETTINGS_KEY, settings) }
        if (ui.state.moves == 0 && !ui.busy) {
            commit(ui.copy(session = Session(GameState.start(settings.opponent)), settings = settings))
        } else {
            _ui.value = ui.copy(settings = settings)
        }
    }

    fun setVisible(visible: Boolean) {
        if (!visible) {
            saveJob?.cancel()
            viewModelScope.launch { save() }
        }
    }

    /** Commits the move straight away, so saves are correct, then replays it stone by stone. */
    private fun play(pit: Int): Boolean {
        val ui = _ui.value ?: return false
        val (next, turn) = ui.session.play(pit) ?: return false
        commit(ui.copy(session = next, shown = ui.state.board, replaying = true, thinking = false))
        if (!counted && ui.state.opponent.isComputer) {
            counted = true
            viewModelScope.launch { services.stats.update(MANCALA_ID) { it.recordStart() } }
        }
        if (next.state.isOver && next.state.opponent.isComputer) recordResult(next.state)
        moveJob = viewModelScope.launch {
            replay(ui.state.board, turn)
            _ui.update { it?.copy(shown = null, replaying = false) }
            when {
                !next.state.isOver -> computerTurn()
                // No fanfare when the computer wins.
                next.state.winner != Side.NORTH || !next.state.opponent.isComputer -> services.feedback.play(FeedbackEvent.Win)
            }
        }
        return true
    }

    private suspend fun replay(before: List<Int>, turn: Turn) {
        val b = before.toMutableList()
        fun show(at: Int?) = _ui.update { it?.copy(shown = b.toList(), lastPit = at) }
        b[turn.pit] = 0
        show(null)
        services.feedback.play(FeedbackEvent.Pickup)
        for (i in turn.drops) {
            delay(stepMs)
            b[i]++
            show(i)
            services.feedback.play(FeedbackEvent.Stone)
        }
        turn.capture?.let { c ->
            delay(stepMs * 2)
            b[c.pit] = 0
            b[c.opposite] = 0
            b[mover(turn).store] += c.stones
            show(mover(turn).store)
            services.feedback.play(FeedbackEvent.Foundation)
        }
        if (turn.extraTurn) services.feedback.play(FeedbackEvent.Foundation)
        if (turn.swept) {
            delay(stepMs * 2)
            b.indices.forEach { b[it] = turn.state.board[it] }
            show(null)
        }
        delay(stepMs)
    }

    /** The side that made [turn]. */
    private fun mover(turn: Turn): Side = if (turn.pit in Side.SOUTH.pits) Side.SOUTH else Side.NORTH

    private fun computerTurn() {
        val ui = _ui.value ?: return
        val state = ui.state
        if (state.isOver || !state.opponent.isComputer || state.toMove != Side.NORTH) return
        _ui.value = ui.copy(thinking = true)
        moveJob = viewModelScope.launch {
            val pit = coroutineScope {
                val choice = async(computerDispatcher) { Computer.choose(state, random) }
                delay(COMPUTER_MIN_MS)
                choice.await()
            }
            play(pit)
        }
    }

    private fun recordResult(state: GameState) {
        val margin = state.store(Side.SOUTH) - state.store(Side.NORTH)
        viewModelScope.launch {
            services.stats.update(MANCALA_ID) {
                when (state.winner) {
                    Side.SOUTH -> it.recordWin(timeMs = null, score = margin)
                    Side.NORTH -> it.recordAbandon()
                    null -> it
                }
            }
        }
    }

    private fun commit(ui: MancalaUiState) {
        _ui.value = ui
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            save()
        }
    }

    private suspend fun save() {
        val ui = _ui.value ?: return
        val keep = ui.state.moves > 0 && !ui.state.isOver
        services.store.writeJson(saveKey(MANCALA_ID), if (keep) SavedGame(ui.session, counted) else null)
    }
}
