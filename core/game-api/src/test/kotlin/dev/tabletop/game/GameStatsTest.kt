package dev.tabletop.game

import org.junit.Assert.assertEquals
import org.junit.Test

class GameStatsTest {
    @Test fun winUpdatesBestsAndStreaks() {
        val s = GameStats().recordStart().recordWin(timeMs = 90_000, score = 500)
            .recordStart().recordWin(timeMs = 120_000, score = 700)
        assertEquals(2, s.played)
        assertEquals(2, s.won)
        assertEquals(90_000L, s.bestTimeMs)
        assertEquals(700, s.bestScore)
        assertEquals(2, s.currentStreak)
        assertEquals(2, s.bestStreak)
        assertEquals(1f, s.winRate)
    }

    @Test fun abandonResetsCurrentStreakOnly() {
        val s = GameStats().recordStart().recordWin(1, 1).recordStart().recordAbandon()
        assertEquals(0, s.currentStreak)
        assertEquals(1, s.bestStreak)
        assertEquals(0.5f, s.winRate)
    }

    @Test fun winRateWithNoGames() {
        assertEquals(0f, GameStats().winRate)
    }
}
