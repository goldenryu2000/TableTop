package dev.tabletop.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.tabletop.game.GameEntry
import dev.tabletop.game.KeyValueStore
import dev.tabletop.game.saveKey
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    games: List<GameEntry>,
    store: KeyValueStore,
    onPlay: (GameEntry) -> Unit,
    onSettings: () -> Unit,
    onStats: () -> Unit,
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Tabletop") },
                actions = {
                    IconButton(onClick = onStats) { Icon(Icons.Rounded.BarChart, contentDescription = "Statistics") }
                    IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, contentDescription = "Settings") }
                },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(games, key = { it.id }) { game ->
                val hasSave by remember(game.id) { store.observe(saveKey(game.id)).map { it != null } }.collectAsState(false)
                GameTile(game, hasSave, onClick = { onPlay(game) })
            }
            item { ComingSoonTile() }
        }
    }
}

@Composable
private fun GameTile(game: GameEntry, hasSave: Boolean, onClick: () -> Unit) {
    val p = game.palette
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = p.background, contentColor = p.onBackground),
        modifier = Modifier.fillMaxWidth().height(184.dp),
    ) {
        Row(Modifier.fillMaxSize().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(game.title, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        game.tagline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = p.onBackground.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    if (hasSave) "Continue" else "Play",
                    style = MaterialTheme.typography.labelLarge,
                    color = p.background,
                    modifier = Modifier.background(p.accent, RoundedCornerShape(50)).padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            game.illustration(Modifier.size(120.dp))
        }
    }
}

@Composable
private fun ComingSoonTile() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(96.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text("More games on the way", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
