package dev.tabletop.game

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

/** String values by key; games store JSON documents here. */
interface KeyValueStore {
    fun observe(key: String): Flow<String?>
    suspend fun write(key: String, value: String?)
}

class DataStoreKeyValueStore(private val dataStore: DataStore<Preferences>) : KeyValueStore {
    override fun observe(key: String): Flow<String?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[stringPreferencesKey(key)] }
        .distinctUntilChanged()

    override suspend fun write(key: String, value: String?) {
        dataStore.edit { prefs ->
            val k = stringPreferencesKey(key)
            if (value == null) prefs.remove(k) else prefs[k] = value
        }
    }
}

class InMemoryKeyValueStore(initial: Map<String, String> = emptyMap()) : KeyValueStore {
    private val values = MutableStateFlow(initial)

    override fun observe(key: String): Flow<String?> = values.map { it[key] }.distinctUntilChanged()

    override suspend fun write(key: String, value: String?) {
        values.update { if (value == null) it - key else it + (key to value) }
    }
}

val TabletopJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

inline fun <reified T> KeyValueStore.observeJson(key: String, default: T): Flow<T> = observe(key).map { raw ->
    raw?.let { runCatching { TabletopJson.decodeFromString<T>(it) }.getOrNull() } ?: default
}

suspend inline fun <reified T> KeyValueStore.readJson(key: String, default: T): T = observeJson(key, default).first()

suspend inline fun <reified T> KeyValueStore.writeJson(key: String, value: T?) {
    write(key, value?.let { TabletopJson.encodeToString(it) })
}
