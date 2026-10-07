package dev.tabletop

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.tabletop.game.GameServices
import dev.tabletop.home.HomeScreen
import dev.tabletop.settings.SettingsScreen
import dev.tabletop.stats.StatsScreen

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
            game.content(services) { nav.popBackStack() }
        }
        composable("settings") { SettingsScreen(services.store, onBack = { nav.popBackStack() }) }
        composable("stats") { StatsScreen(games, services.stats, onBack = { nav.popBackStack() }) }
    }
}
