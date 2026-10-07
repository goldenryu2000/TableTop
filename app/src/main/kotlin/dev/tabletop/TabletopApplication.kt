package dev.tabletop

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import dev.tabletop.feedback.AndroidFeedback
import dev.tabletop.game.DataStoreKeyValueStore
import dev.tabletop.game.GLOBAL_SETTINGS_KEY
import dev.tabletop.game.GameServices
import dev.tabletop.game.GlobalSettings
import dev.tabletop.game.StatsRepository
import dev.tabletop.game.observeJson
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore("tabletop")

class TabletopApplication : Application() {
    lateinit var services: GameServices
        private set

    override fun onCreate() {
        super.onCreate()
        val store = DataStoreKeyValueStore(dataStore)
        val feedback = AndroidFeedback(this)
        services = GameServices(store, feedback, StatsRepository(store))
        MainScope().launch {
            store.observeJson(GLOBAL_SETTINGS_KEY, GlobalSettings()).collect {
                feedback.soundEnabled = it.sound
                feedback.hapticsEnabled = it.haptics
            }
        }
    }
}
