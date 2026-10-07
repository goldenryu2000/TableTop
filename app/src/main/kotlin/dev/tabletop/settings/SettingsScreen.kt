package dev.tabletop.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.tabletop.game.GLOBAL_SETTINGS_KEY
import dev.tabletop.game.GlobalSettings
import dev.tabletop.game.KeyValueStore
import dev.tabletop.game.observeJson
import dev.tabletop.game.writeJson
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(store: KeyValueStore, onBack: () -> Unit) {
    val settings by remember { store.observeJson(GLOBAL_SETTINGS_KEY, GlobalSettings()) }.collectAsState(GlobalSettings())
    val scope = rememberCoroutineScope()
    fun update(f: (GlobalSettings) -> GlobalSettings) = scope.launch { store.writeJson(GLOBAL_SETTINGS_KEY, f(settings)) }
    val context = LocalContext.current
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            ListItem(
                headlineContent = { Text("Sound") },
                supportingContent = { Text("Soft card and table sounds") },
                trailingContent = { Switch(settings.sound, { on -> update { it.copy(sound = on) } }) },
            )
            ListItem(
                headlineContent = { Text("Haptics") },
                supportingContent = { Text("Gentle taps on pickup and placement") },
                trailingContent = { Switch(settings.haptics, { on -> update { it.copy(haptics = on) } }) },
            )
            Text(
                "About",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
            )
            ListItem(
                headlineContent = { Text("Tabletop $version") },
                supportingContent = { Text("Free and open source under GPL-3.0. No ads, no tracking.") },
            )
        }
    }
}
