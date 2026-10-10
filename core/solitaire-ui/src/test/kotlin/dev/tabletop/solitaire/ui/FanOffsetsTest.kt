package dev.tabletop.solitaire.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FanOffsetsTest {
    @Test fun shortColumnUsesNaturalSteps() {
        val ys = fanOffsets(listOf(false, false, true, true), top = 100f, bottom = 1000f, cardHeight = 140f, downStep = 10f, upStep = 30f)
        assertEquals(listOf(100f, 110f, 120f, 150f), ys)
    }

    @Test fun tallColumnCompressesToFit() {
        val faceUp = List(10) { false } + List(30) { true }
        val ys = fanOffsets(faceUp, top = 0f, bottom = 800f, cardHeight = 140f, downStep = 10f, upStep = 40f)
        assertTrue(ys.last() + 140f <= 800.01f)
        assertTrue(ys.zipWithNext().all { (a, b) -> b > a })
    }

    @Test fun emptyColumn() {
        assertEquals(emptyList<Float>(), fanOffsets(emptyList(), 0f, 100f, 50f, 5f, 10f))
    }
}
