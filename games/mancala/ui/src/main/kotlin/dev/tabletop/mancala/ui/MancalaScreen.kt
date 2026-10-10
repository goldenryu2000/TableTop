package dev.tabletop.mancala.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tabletop.design.LightSystemBarIcons
import dev.tabletop.design.StatLabel
import dev.tabletop.design.ToolbarAction
import dev.tabletop.design.rememberReducedMotion
import dev.tabletop.game.GameStats
import dev.tabletop.mancala.engine.GameState
import dev.tabletop.mancala.engine.Opponent
import dev.tabletop.mancala.engine.Side
import kotlinx.coroutines.delay

/** Display and possessive names for each side, depending on who South is playing. */
private class Names(val south: String, val north: String, val southOf: String, val northOf: String) {
    fun of(side: Side) = if (side == Side.SOUTH) south else north

    companion object {
        fun forGame(state: GameState) = if (state.opponent.isComputer) {
            Names("You", "Computer", "Your", "Computer's")
        } else {
            Names("Player 1", "Player 2", "Player 1's", "Player 2's")
        }
    }
}

@Composable
fun MancalaScreen(viewModel: MancalaViewModel, onExit: () -> Unit, onHowToPlay: () -> Unit = {}) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.setVisible(true)
        onPauseOrDispose { viewModel.setVisible(false) }
    }
    LightSystemBarIcons()
    val reducedMotion = rememberReducedMotion()
    LaunchedEffect(reducedMotion) { viewModel.stepMs = if (reducedMotion) 0L else STEP_MS }
    var showSettings by remember { mutableStateOf(false) }
    var confirmNew by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(MancalaColors.table, MancalaColors.tableEdge),
                        center = center,
                        radius = size.maxDimension * 0.8f,
                    ),
                )
            },
    ) {
        val current = ui ?: return@Box
        val state = current.state
        val names = Names.forGame(state)
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            TopBar(status(current, names), onExit, onHowToPlay, onSettings = { showSettings = true })
            ScoreLine(names.north, current.board[Side.NORTH.store], active = !state.isOver && state.toMove == Side.NORTH, Modifier.testTag("score-north"))
            Board(
                current,
                names.southOf to names.northOf,
                reducedMotion,
                onPit = viewModel::onPit,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            )
            ScoreLine(names.south, current.board[Side.SOUTH.store], active = !state.isOver && state.toMove == Side.SOUTH, Modifier.testTag("score-south"))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ToolbarAction(
                    Icons.Rounded.Replay, "New", { if (state.moves > 0 && !state.isOver) confirmNew = true else viewModel.onNewGame() },
                    MancalaColors.ink, modifier = Modifier.testTag("new-game"),
                )
                ToolbarAction(
                    Icons.AutoMirrored.Rounded.Undo, "Undo", viewModel::onUndo, MancalaColors.ink,
                    enabled = current.session.canUndo && !current.busy && !state.isOver, modifier = Modifier.testTag("undo"),
                )
            }
        }
        if (state.isOver && !current.replaying) EndPanel(state, names, stats, onNewGame = viewModel::onNewGame, onExit = onExit)
        if (showSettings) {
            SettingsSheet(current.settings, gameStarted = state.moves > 0, onChange = viewModel::updateSettings, onDismiss = { showSettings = false })
        }
        if (confirmNew) {
            AlertDialog(
                onDismissRequest = { confirmNew = false },
                title = { Text("Start a new game?") },
                text = { Text(if (state.opponent.isComputer) "This game will count as a loss." else "This game will be lost.") },
                confirmButton = {
                    TextButton(onClick = { confirmNew = false; viewModel.onNewGame() }) { Text("New game") }
                },
                dismissButton = { TextButton(onClick = { confirmNew = false }) { Text("Keep playing") } },
            )
        }
    }
}

private fun status(ui: MancalaUiState, names: Names): String {
    val s = ui.state
    return when {
        s.isOver -> "Game over"
        ui.thinking -> "Thinking…"
        s.opponent.isComputer && s.toMove == Side.NORTH -> "Computer's turn"
        s.opponent.isComputer -> if (ui.goesAgain) "Go again" else "Your turn"
        else -> names.of(s.toMove) + if (ui.goesAgain) ", go again" else "'s turn"
    }
}

@Composable
private fun TopBar(status: String, onExit: () -> Unit, onHowToPlay: () -> Unit, onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onExit) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MancalaColors.ink)
        }
        Text(
            status,
            style = MaterialTheme.typography.titleMedium,
            color = MancalaColors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).testTag("status"),
        )
        IconButton(onClick = onHowToPlay) {
            Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = "How to play", tint = MancalaColors.ink)
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Rounded.Tune, contentDescription = "Game settings", tint = MancalaColors.ink)
        }
    }
}

/** A side's name and store count; the dot marks whose turn it is. */
@Composable
private fun ScoreLine(name: String, stones: Int, active: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 4.dp).semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(if (active) MancalaColors.accent else MancalaColors.ink.copy(alpha = 0.15f), CircleShape))
        Spacer(Modifier.width(10.dp))
        Text(name, style = MaterialTheme.typography.titleMedium, color = MancalaColors.ink.copy(alpha = if (active) 1f else 0.7f))
        Spacer(Modifier.width(12.dp))
        Text(
            stones.toString(),
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
            color = MancalaColors.ink,
        )
    }
}

@Composable
private fun EndPanel(state: GameState, names: Names, stats: GameStats, onNewGame: () -> Unit, onExit: () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(700)
        shown = true
    }
    val title = when (val w = state.winner) {
        null -> "A draw"
        Side.SOUTH -> if (state.opponent.isComputer) "You win" else "${names.south} wins"
        else -> "${names.of(w)} wins"
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(shown, enter = slideInVertically { it } + fadeIn()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .navigationBarsPadding()
                    .background(MancalaColors.sheet, RoundedCornerShape(28.dp))
                    .padding(24.dp)
                    .testTag("end-panel"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(title, style = MaterialTheme.typography.headlineMedium, color = MancalaColors.ink)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatLabel(names.south, state.store(Side.SOUTH).toString(), MancalaColors.ink)
                    StatLabel(names.north, state.store(Side.NORTH).toString(), MancalaColors.ink)
                }
                if (state.opponent.isComputer) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Best margin ${stats.bestScore ?: "-"} · Streak ${stats.currentStreak}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MancalaColors.ink.copy(alpha = 0.7f),
                    )
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onExit) { Text("Home", color = MancalaColors.ink) }
                    Button(
                        onClick = onNewGame,
                        colors = ButtonDefaults.buttonColors(containerColor = MancalaColors.accent, contentColor = MancalaColors.tableEdge),
                    ) { Text("New game") }
                }
            }
        }
    }
}

private val opponentLabels = mapOf(
    Opponent.EASY to ("Easy" to "Plays loosely; good for learning"),
    Opponent.MEDIUM to ("Medium" to "Thinks a few moves ahead"),
    Opponent.HARD to ("Hard" to "Looks deep and rarely slips"),
    Opponent.FRIEND to ("Two players" to "Take turns on this phone"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    settings: MancalaSettings,
    gameStarted: Boolean,
    onChange: ((MancalaSettings) -> MancalaSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MancalaColors.sheet, contentColor = MancalaColors.ink) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text("Opponent", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            if (gameStarted) {
                Text(
                    "Applies to your next game",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MancalaColors.ink.copy(alpha = 0.65f),
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
            Opponent.entries.forEach { o ->
                val (title, subtitle) = opponentLabels.getValue(o)
                val selected = settings.opponent == o
                ListItem(
                    headlineContent = { Text(title) },
                    supportingContent = { Text(subtitle) },
                    leadingContent = {
                        RadioButton(
                            selected = selected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = MancalaColors.accent, unselectedColor = MancalaColors.ink.copy(alpha = 0.6f)),
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MancalaColors.sheet,
                        headlineColor = MancalaColors.ink,
                        supportingColor = MancalaColors.ink.copy(alpha = 0.65f),
                    ),
                    modifier = Modifier
                        .selectable(selected, role = Role.RadioButton) { onChange { it.copy(opponent = o) } }
                        .testTag("opponent-${o.name.lowercase()}"),
                )
            }
        }
    }
}
