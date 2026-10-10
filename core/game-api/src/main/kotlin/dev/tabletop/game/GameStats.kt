package dev.tabletop.game

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class GameStats(
    val played: Int = 0,
    val won: Int = 0,
    val bestTimeMs: Long? = null,
    val bestScore: Int? = null,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
) {
    val winRate: Float get() = if (played == 0) 0f else won.toFloat() / played

    fun recordStart() = copy(played = played + 1)

    /** [timeMs] is null for games without a clock. */
    fun recordWin(timeMs: Long?, score: Int): GameStats {
        val streak = currentStreak + 1
        return copy(
            won = won + 1,
            bestTimeMs = if (timeMs == null) bestTimeMs else minOf(bestTimeMs ?: timeMs, timeMs),
            bestScore = maxOf(bestScore ?: score, score),
            currentStreak = streak,
            bestStreak = maxOf(bestStreak, streak),
        )
    }

    fun recordAbandon() = copy(currentStreak = 0)
}

class StatsRepository(private val store: KeyValueStore) {
    fun observe(gameId: String): Flow<GameStats> = store.observeJson(key(gameId), GameStats())

    suspend fun update(gameId: String, f: (GameStats) -> GameStats) {
        store.writeJson(key(gameId), f(store.readJson(key(gameId), GameStats())))
    }

    suspend fun reset(gameId: String) = store.write(key(gameId), null)

    private fun key(gameId: String) = "stats_$gameId"
}
