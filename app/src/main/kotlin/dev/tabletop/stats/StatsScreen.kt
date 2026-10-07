package dev.tabletop.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tabletop.game.GameEntry
import dev.tabletop.game.GameStats
import dev.tabletop.game.StatsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(games: List<GameEntry>, stats: StatsRepository, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(games, key = { it.id }) { game ->
                val s by remember(game.id) { stats.observe(game.id) }.collectAsState(GameStats())
                StatsCard(game, s, onReset = { stats.reset(game.id) })
            }
        }
    }
}

@Composable
private fun StatsCard(game: GameEntry, s: GameStats, onReset: suspend () -> Unit) {
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = game.palette.background, contentColor = game.palette.onBackground),
    ) {
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            Text(game.title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            StatRow("Played" to s.played.toString(), "Won" to s.won.toString(), "Win rate" to "${(s.winRate * 100).toInt()}%")
            Spacer(Modifier.height(12.dp))
            StatRow(
                "Best time" to (s.bestTimeMs?.let(::formatTime) ?: "-"),
                "Best score" to (s.bestScore?.toString() ?: "-"),
                "Streak" to "${s.currentStreak} / ${s.bestStreak}",
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { confirm = true }) { Text("Reset", color = game.palette.onBackground.copy(alpha = 0.7f)) }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Reset ${game.title} statistics?") },
            confirmButton = { TextButton(onClick = { confirm = false; scope.launch { onReset() } }) { Text("Reset") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun StatRow(vararg items: Pair<String, String>) {
    Row(Modifier.fillMaxWidth()) {
        items.forEach { (label, value) ->
            Column(Modifier.weight(1f)) {
                Text(value, style = MaterialTheme.typography.titleLarge)
                Text(label, style = MaterialTheme.typography.labelMedium, color = LocalContentColor.current.copy(alpha = 0.65f))
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
