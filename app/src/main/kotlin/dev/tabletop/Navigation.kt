package dev.tabletop

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.tabletop.game.GameEntry
import dev.tabletop.game.GameServices
import dev.tabletop.game.tutorialSeenKey
import dev.tabletop.home.HomeScreen
import dev.tabletop.settings.SettingsScreen
import dev.tabletop.stats.StatsScreen
import dev.tabletop.tutorial.TutorialSheet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun TabletopNavHost(services: GameServices) {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = "home",
        enterTransition = { fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(180)) },
    ) {
        composable("home") {
            HomeScreen(
                games = games,
                store = services.store,
                onPlay = { nav.navigate("game/${it.id}") },
                onSettings = { nav.navigate("settings") },
                onStats = { nav.navigate("stats") },
            )
        }
        composable("game/{id}") { entry ->
            val game = games.first { it.id == entry.arguments?.getString("id") }
            GameHost(game, services, onExit = { nav.popBackStack() })
        }
        composable("settings") { SettingsScreen(services.store, onBack = { nav.popBackStack() }) }
        composable("stats") { StatsScreen(games, services.stats, onBack = { nav.popBackStack() }) }
    }
}

/** Shows the game, plus its tutorial on a first visit or when the game asks for it. */
@Composable
private fun GameHost(game: GameEntry, services: GameServices, onExit: () -> Unit) {
    var showTutorial by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(game.id) {
        val seen = services.store.observe(tutorialSeenKey(game.id)).first() != null
        // Players who already know the game from an earlier version aren't interrupted.
        val played = services.stats.observe(game.id).first().played > 0
        if (!seen && !played) showTutorial = true
    }
    game.content(services, onExit) { showTutorial = true }
    if (showTutorial) {
        TutorialSheet(game) {
            showTutorial = false
            scope.launch { services.store.write(tutorialSeenKey(game.id), "1") }
        }
    }
}
