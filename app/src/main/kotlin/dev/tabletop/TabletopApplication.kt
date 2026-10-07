package dev.tabletop

import android.app.Application
import androidx.datastore.preferences.preferencesDataStoreFile
import dev.tabletop.feedback.AndroidFeedback
import dev.tabletop.game.DataStoreKeyValueStore
import dev.tabletop.game.GLOBAL_SETTINGS_KEY
import dev.tabletop.game.GameServices
import dev.tabletop.game.GlobalSettings
import dev.tabletop.game.StatsRepository
import dev.tabletop.game.observeJson
import dev.tabletop.game.tabletopDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class TabletopApplication : Application() {
    lateinit var services: GameServices
        private set

    override fun onCreate() {
        super.onCreate()
        val dataStore = tabletopDataStore(CoroutineScope(Dispatchers.IO + SupervisorJob())) { preferencesDataStoreFile("tabletop") }
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
