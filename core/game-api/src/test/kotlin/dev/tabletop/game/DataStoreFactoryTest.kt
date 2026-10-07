package dev.tabletop.game

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreFactoryTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun corruptFileIsReplacedInsteadOfCrashing() = runTest {
        val file = tmp.newFile("tabletop.preferences_pb").apply { writeBytes(byteArrayOf(0x7f, 0x01, 0x02, 0x03, 0x04)) }
        val store = DataStoreKeyValueStore(tabletopDataStore(backgroundScope) { file })
        store.write("k", "v")
        assertEquals("v", store.observe("k").first())
    }
}
