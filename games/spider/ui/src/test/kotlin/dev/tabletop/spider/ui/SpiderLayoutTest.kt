package dev.tabletop.spider.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import dev.tabletop.spider.engine.GameState
import dev.tabletop.spider.engine.Move
import dev.tabletop.spider.engine.Suits
import dev.tabletop.spider.engine.apply
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpiderLayoutTest {
    private val deal = GameState.deal(4, Suits.TWO)

    @Test fun tenColumnsFitAndEveryCardIsPlaced() {
        val l = computeSpiderLayout(deal, width = 1080f, height = 1900f, gap = 10f)
        assertTrue(l.piles.values.maxOf { it.right } <= 1080f)
        assertEquals(104, l.cards.size)
        assertEquals(104, l.cards.map { it.key }.toSet().size)
    }

    @Test fun nextDealSitsOnTopOfTheStock() {
        val l = computeSpiderLayout(deal, 1080f, 1900f, 10f)
        val next = deal.stock.take(10).map { it.id }.toSet()
        val stock = l.cards.filter { it.pile == null }
        assertTrue(stock.filter { it.key in next }.minOf { it.z } > stock.filter { it.key !in next }.maxOf { it.z })
        assertTrue(l.stock.contains(stock.first { it.key in next }.topLeft + Offset(5f, 5f)))
    }

    @Test fun tallColumnsStayOnScreen() {
        var s = deal
        repeat(5) { s = s.apply(Move.Deal)!! }
        val height = 1200f
        val l = computeSpiderLayout(s, 1080f, height, 10f)
        assertTrue(l.cards.filter { it.pile != null }.all { it.topLeft.y + l.cardSize.height <= height + 0.5f })
    }

    @Test fun dropCandidatesFindTheColumnUnderTheCard() {
        val l = computeSpiderLayout(deal, 1080f, 1900f, 10f)
        val target = l.piles.getValue(6)
        val dragged = Rect(Offset(target.left + 6f, target.top + 200f), l.cardSize)
        assertEquals(6, l.dropCandidates(dragged).first())
    }
}
