package dev.tabletop

import dev.tabletop.klondike.ui.KlondikeGame
import dev.tabletop.mancala.ui.MancalaGame
import dev.tabletop.spider.ui.SpiderGame

/** Every game in the app, in home-screen order. */
val games = listOf(KlondikeGame.entry, SpiderGame.entry, MancalaGame.entry)
