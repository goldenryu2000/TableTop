package dev.tabletop.klondike.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Lightbulb
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tabletop.design.LightSystemBarIcons
import dev.tabletop.design.StatLabel
import dev.tabletop.design.ToolbarAction
import dev.tabletop.game.GameStats
import dev.tabletop.klondike.engine.DrawMode
import dev.tabletop.klondike.engine.canAutoComplete
import kotlinx.coroutines.delay

@Composable
fun KlondikeScreen(viewModel: KlondikeViewModel, onExit: () -> Unit) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.setVisible(true)
        onPauseOrDispose { viewModel.setVisible(false) }
    }
    LightSystemBarIcons()
    var showSettings by remember { mutableStateOf(false) }
    var confirmNew by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(KlondikeColors.table, KlondikeColors.tableEdge),
                        center = center,
                        radius = size.maxDimension * 0.8f,
                    ),
                )
            },
    ) {
        val current = ui ?: return@Box
        val state = current.state
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            TopBar(current, onExit = onExit, onSettings = { showSettings = true })
            Table(current, viewModel, Modifier.weight(1f).padding(horizontal = 2.dp))
            BottomBar(
                current,
                onNew = { if (state.moves > 0 && !state.isWon) confirmNew = true else viewModel.onNewGame() },
                onUndo = viewModel::onUndo,
                onHint = viewModel::onHint,
                onAutoFinish = viewModel::onAutoFinish,
            )
        }
        if (state.isWon) WinPanel(current, stats, onNewGame = viewModel::onNewGame, onExit = onExit)
        if (showSettings) {
            SettingsSheet(current.settings, onChange = viewModel::updateSettings, onDismiss = { showSettings = false })
        }
        if (confirmNew) {
            AlertDialog(
                onDismissRequest = { confirmNew = false },
                title = { Text("Start a new game?") },
                text = { Text("This game will count as a loss.") },
                confirmButton = {
                    TextButton(onClick = { confirmNew = false; viewModel.onNewGame() }) { Text("New game") }
                },
                dismissButton = { TextButton(onClick = { confirmNew = false }) { Text("Keep playing") } },
            )
        }
    }
}

@Composable
private fun TopBar(ui: KlondikeUiState, onExit: () -> Unit, onSettings: () -> Unit) {
    val s = ui.state
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onExit) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = KlondikeColors.ink)
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatLabel("Time", formatTime(s.elapsedMs), KlondikeColors.ink)
            StatLabel("Moves", s.moves.toString(), KlondikeColors.ink, Modifier.testTag("moves"))
            StatLabel("Score", s.score.toString(), KlondikeColors.ink)
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Rounded.Tune, contentDescription = "Game settings", tint = KlondikeColors.ink)
        }
    }
}

@Composable
private fun BottomBar(
    ui: KlondikeUiState,
    onNew: () -> Unit,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onAutoFinish: () -> Unit,
) {
    val s = ui.state
    val idle = !ui.autoFinishing
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolbarAction(Icons.Rounded.Replay, "New", onNew, KlondikeColors.ink, enabled = idle, modifier = Modifier.testTag("new-game"))
        ToolbarAction(
            Icons.AutoMirrored.Rounded.Undo, "Undo", onUndo, KlondikeColors.ink,
            enabled = ui.session.canUndo && !s.isWon && idle, modifier = Modifier.testTag("undo"),
        )
        ToolbarAction(Icons.Rounded.Lightbulb, "Hint", onHint, KlondikeColors.ink, enabled = !s.isWon && idle, modifier = Modifier.testTag("hint"))
        AnimatedVisibility(s.canAutoComplete && idle, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            ToolbarAction(Icons.Rounded.AutoAwesome, "Finish", onAutoFinish, KlondikeColors.accent)
        }
    }
}

@Composable
private fun WinPanel(ui: KlondikeUiState, stats: GameStats, onNewGame: () -> Unit, onExit: () -> Unit) {
    var shown by remember(ui.state.seed) { mutableStateOf(false) }
    LaunchedEffect(ui.state.seed) {
        delay(1_200)
        shown = true
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(shown, enter = slideInVertically { it } + fadeIn()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .navigationBarsPadding()
                    .background(KlondikeColors.sheet, RoundedCornerShape(28.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Solved", style = MaterialTheme.typography.headlineMedium, color = KlondikeColors.ink)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatLabel("Time", formatTime(ui.state.elapsedMs), KlondikeColors.ink)
                    StatLabel("Moves", ui.state.moves.toString(), KlondikeColors.ink)
                    StatLabel("Score", ui.state.score.toString(), KlondikeColors.ink)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Best ${stats.bestTimeMs?.let(::formatTime) ?: "-"} · Streak ${stats.currentStreak}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KlondikeColors.ink.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onExit) { Text("Home", color = KlondikeColors.ink) }
                    Button(
                        onClick = onNewGame,
                        colors = ButtonDefaults.buttonColors(containerColor = KlondikeColors.accent, contentColor = KlondikeColors.tableEdge),
                    ) { Text("New game") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    settings: KlondikeSettings,
    onChange: ((KlondikeSettings) -> KlondikeSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = KlondikeColors.sheet, contentColor = KlondikeColors.ink) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text("Klondike", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            ToggleRow("Draw three", "Applies to your next game", settings.drawMode == DrawMode.THREE) { on ->
                onChange { it.copy(drawMode = if (on) DrawMode.THREE else DrawMode.ONE) }
            }
            ToggleRow("Tap to move", "Tap a card to send it to the best spot", settings.tapToMove) { on -> onChange { it.copy(tapToMove = on) } }
            ToggleRow("Four-color suits", "Blue diamonds, green clubs", settings.fourColor) { on -> onChange { it.copy(fourColor = on) } }
            ToggleRow("Left-handed", "Stock on the right", settings.leftHanded) { on -> onChange { it.copy(leftHanded = on) } }
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedTrackColor = KlondikeColors.accent, checkedThumbColor = KlondikeColors.tableEdge),
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = KlondikeColors.sheet,
            headlineColor = KlondikeColors.ink,
            supportingColor = KlondikeColors.ink.copy(alpha = 0.65f),
        ),
    )
}

internal fun formatTime(ms: Long): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
