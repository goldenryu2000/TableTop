package dev.tabletop.spider.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Undo
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tabletop.design.LightSystemBarIcons
import dev.tabletop.design.StatLabel
import dev.tabletop.design.ToolbarAction
import dev.tabletop.game.GameStats
import dev.tabletop.spider.engine.Suits
import kotlinx.coroutines.delay

@Composable
fun SpiderScreen(viewModel: SpiderViewModel, onExit: () -> Unit, onHowToPlay: () -> Unit = {}) {
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
                        listOf(SpiderColors.table, SpiderColors.tableEdge),
                        center = center,
                        radius = size.maxDimension * 0.8f,
                    ),
                )
            },
    ) {
        val current = ui ?: return@Box
        val state = current.state
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            TopBar(current, onExit, onHowToPlay, onSettings = { showSettings = true })
            SpiderTable(current, viewModel, Modifier.weight(1f).padding(horizontal = 2.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ToolbarAction(
                    Icons.Rounded.Replay, "New", { if (state.moves > 0 && !state.isWon) confirmNew = true else viewModel.onNewGame() },
                    SpiderColors.ink, modifier = Modifier.testTag("new-game"),
                )
                ToolbarAction(
                    Icons.AutoMirrored.Rounded.Undo, "Undo", viewModel::onUndo, SpiderColors.ink,
                    enabled = current.session.canUndo && !state.isWon, modifier = Modifier.testTag("undo"),
                )
                ToolbarAction(Icons.Rounded.Lightbulb, "Hint", viewModel::onHint, SpiderColors.ink, enabled = !state.isWon, modifier = Modifier.testTag("hint"))
            }
        }
        Notice(current.notice)
        if (state.isWon) WinPanel(current, stats, onNewGame = viewModel::onNewGame, onExit = onExit)
        if (showSettings) SettingsSheet(current.settings, onChange = viewModel::updateSettings, onDismiss = { showSettings = false })
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
private fun TopBar(ui: SpiderUiState, onExit: () -> Unit, onHowToPlay: () -> Unit, onSettings: () -> Unit) {
    val s = ui.state
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onExit) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = SpiderColors.ink)
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatLabel("Time", formatTime(s.elapsedMs), SpiderColors.ink)
            StatLabel("Moves", s.moves.toString(), SpiderColors.ink, Modifier.testTag("moves"))
            StatLabel("Score", s.score.toString(), SpiderColors.ink)
        }
        IconButton(onClick = onHowToPlay) {
            Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = "How to play", tint = SpiderColors.ink)
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Rounded.Tune, contentDescription = "Game settings", tint = SpiderColors.ink)
        }
    }
}

/** A short message floating above the bottom bar. */
@Composable
private fun Notice(text: String?) {
    Box(Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = 88.dp), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(text != null, enter = fadeIn(), exit = fadeOut()) {
            Text(
                text.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = SpiderColors.ink,
                modifier = Modifier
                    .background(SpiderColors.sheet, RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                    .testTag("notice"),
            )
        }
    }
}

@Composable
private fun WinPanel(ui: SpiderUiState, stats: GameStats, onNewGame: () -> Unit, onExit: () -> Unit) {
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
                    .background(SpiderColors.sheet, RoundedCornerShape(28.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Solved", style = MaterialTheme.typography.headlineMedium, color = SpiderColors.ink)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatLabel("Time", formatTime(ui.state.elapsedMs), SpiderColors.ink)
                    StatLabel("Moves", ui.state.moves.toString(), SpiderColors.ink)
                    StatLabel("Score", ui.state.score.toString(), SpiderColors.ink)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Best ${stats.bestTimeMs?.let(::formatTime) ?: "-"} · Streak ${stats.currentStreak}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpiderColors.ink.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onExit) { Text("Home", color = SpiderColors.ink) }
                    Button(
                        onClick = onNewGame,
                        colors = ButtonDefaults.buttonColors(containerColor = SpiderColors.accent, contentColor = SpiderColors.tableEdge),
                    ) { Text("New game") }
                }
            }
        }
    }
}

private val suitLabels = mapOf(
    Suits.ONE to ("One suit" to "Relaxed; most games can be won"),
    Suits.TWO to ("Two suits" to "The classic challenge"),
    Suits.FOUR to ("Four suits" to "For experts; few games come out"),
)

private val listColors @Composable get() = ListItemDefaults.colors(
    containerColor = SpiderColors.sheet,
    headlineColor = SpiderColors.ink,
    supportingColor = SpiderColors.ink.copy(alpha = 0.65f),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    settings: SpiderSettings,
    onChange: ((SpiderSettings) -> SpiderSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SpiderColors.sheet, contentColor = SpiderColors.ink) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text("Spider", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            Text(
                "Suits apply to your next game",
                style = MaterialTheme.typography.bodyMedium,
                color = SpiderColors.ink.copy(alpha = 0.65f),
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Suits.entries.forEach { suits ->
                val (title, subtitle) = suitLabels.getValue(suits)
                val selected = settings.suits == suits
                ListItem(
                    headlineContent = { Text(title) },
                    supportingContent = { Text(subtitle) },
                    leadingContent = {
                        RadioButton(
                            selected = selected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = SpiderColors.accent, unselectedColor = SpiderColors.ink.copy(alpha = 0.6f)),
                        )
                    },
                    colors = listColors,
                    modifier = Modifier
                        .selectable(selected, role = Role.RadioButton) { onChange { it.copy(suits = suits) } }
                        .testTag("suits-${suits.count}"),
                )
            }
            ToggleRow("Tap to move", "Tap a card to send it to the best spot", settings.tapToMove) { on -> onChange { it.copy(tapToMove = on) } }
            ToggleRow("Four-color suits", "Blue diamonds, green clubs", settings.fourColor) { on -> onChange { it.copy(fourColor = on) } }
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
                colors = SwitchDefaults.colors(checkedTrackColor = SpiderColors.accent, checkedThumbColor = SpiderColors.tableEdge),
            )
        },
        colors = listColors,
    )
}

internal fun formatTime(ms: Long): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
