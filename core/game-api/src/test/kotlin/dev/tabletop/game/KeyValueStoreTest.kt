package dev.tabletop.game

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyValueStoreTest {
    @Test fun jsonRoundTrip() = runTest {
        val store = InMemoryKeyValueStore()
        store.writeJson("g", GlobalSettings(sound = false))
        assertEquals(GlobalSettings(sound = false), store.readJson("g", GlobalSettings()))
    }

    @Test fun corruptJsonFallsBackToDefault() = runTest {
        val store = InMemoryKeyValueStore(mapOf("g" to "{not json"))
        assertEquals(GlobalSettings(), store.observeJson("g", GlobalSettings()).first())
    }

    @Test fun writingNullRemoves() = runTest {
        val store = InMemoryKeyValueStore(mapOf("k" to "v"))
        store.write("k", null)
        assertNull(store.observe("k").first())
    }

    @Test fun statsRepositoryUpdates() = runTest {
        val repo = StatsRepository(InMemoryKeyValueStore())
        repo.update("klondike") { it.recordStart() }
        assertEquals(1, repo.observe("klondike").first().played)
        repo.reset("klondike")
        assertEquals(GameStats(), repo.observe("klondike").first())
    }
}
