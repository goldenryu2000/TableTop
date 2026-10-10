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

/** Key that is present once a game's tutorial has been shown. */
fun tutorialSeenKey(gameId: String) = "tutorial_seen_$gameId"

/** One page of a game's "How to play" guide. */
class TutorialPage(
    val title: String,
    val body: String,
    val illustration: @Composable (Modifier) -> Unit,
)

/**
 * How a game plugs into the shell. [content] gets `onHowToPlay` for its help button.
 * [scoreLabel] names `GameStats.bestScore` on the stats screen; [timed] games also show a best time.
 */
class GameEntry(
    val id: String,
    val title: String,
    val tagline: String,
    val palette: GamePalette,
    val illustration: @Composable (Modifier) -> Unit,
    val tutorial: List<TutorialPage>,
    val scoreLabel: String = "Best score",
    val timed: Boolean = true,
    val content: @Composable (services: GameServices, onExit: () -> Unit, onHowToPlay: () -> Unit) -> Unit,
)
