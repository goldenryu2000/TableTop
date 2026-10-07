package dev.tabletop.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.tabletop.feedback.Feedback
import kotlinx.serialization.Serializable

@Serializable
data class GlobalSettings(val sound: Boolean = true, val haptics: Boolean = true)

const val GLOBAL_SETTINGS_KEY = "global_settings"

/** Key under which a game keeps its in-progress save; present means "Continue" on home. */
fun saveKey(gameId: String) = "save_$gameId"

@Immutable
data class GamePalette(val background: Color, val surface: Color, val accent: Color, val onBackground: Color)

class GameServices(val store: KeyValueStore, val feedback: Feedback, val stats: StatsRepository)

/** How a game plugs into the shell. */
class GameEntry(
    val id: String,
    val title: String,
    val tagline: String,
    val palette: GamePalette,
    val illustration: @Composable (Modifier) -> Unit,
    val content: @Composable (services: GameServices, onExit: () -> Unit) -> Unit,
)
