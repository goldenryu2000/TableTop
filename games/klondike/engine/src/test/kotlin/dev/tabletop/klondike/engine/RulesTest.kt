package dev.tabletop.klondike.engine

import dev.tabletop.cards.Suit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesTest {
    private fun tableau(vararg piles: List<TableauCard>) = piles.toList()

    @Test fun drawOneMovesTopOfStockToWaste() {
        val s = state(stock = listOf("2H", "3H", "4H")).apply(Move.Draw)!!
        assertEquals(listOf(c("2H"), c("3H")), s.stock)
        assertEquals(listOf(c("4H")), s.waste)
        assertEquals(1, s.moves)
    }

    @Test fun drawThreeDealsThreeWithLastDrawnOnTop() {
        val s = state(stock = listOf("AH", "2H", "3H", "4H"), drawMode = DrawMode.THREE).apply(Move.Draw)!!
        assertEquals(listOf(c("AH")), s.stock)
        assertEquals(listOf(c("4H"), c("3H"), c("2H")), s.waste)
    }

    @Test fun drawThreeWithTwoLeftDrawsTwo() {
        val s = state(stock = listOf("AH", "2H"), drawMode = DrawMode.THREE).apply(Move.Draw)!!
        assertTrue(s.stock.isEmpty())
        assertEquals(listOf(c("2H"), c("AH")), s.waste)
    }

    @Test fun recycleRestoresStockOrderWithPenaltyInDrawOne() {
        val start = state(stock = listOf("2H", "3H"), score = 150)
        val drawn = start.apply(Move.Draw)!!.apply(Move.Draw)!!
        val recycled = drawn.apply(Move.Draw)!!
        assertEquals(start.stock, recycled.stock)
        assertTrue(recycled.waste.isEmpty())
        assertEquals(50, recycled.score)
    }

    @Test fun recyclePenaltyFloorsAtZero() {
        assertEquals(0, state(waste = listOf("2H")).apply(Move.Draw)!!.score)
    }

    @Test fun recycleInDrawThreeHasNoPenalty() {
        assertEquals(30, state(waste = listOf("2H"), drawMode = DrawMode.THREE, score = 30).apply(Move.Draw)!!.score)
    }

    @Test fun drawWithNothingLeftIsIllegal() {
        assertNull(state().apply(Move.Draw))
    }

    @Test fun aceFromWasteToFoundation() {
        val s = state(waste = listOf("AS")).apply(Move.Transfer(Pile.Waste, Pile.Foundation(Suit.SPADES)))!!
        assertEquals(listOf(c("AS")), s.foundation(Suit.SPADES))
        assertEquals(10, s.score)
    }

    @Test fun foundationRejectsWrongRankOrSuit() {
        assertNull(state(waste = listOf("2S")).apply(Move.Transfer(Pile.Waste, Pile.Foundation(Suit.SPADES))))
        assertNull(state(waste = listOf("AH")).apply(Move.Transfer(Pile.Waste, Pile.Foundation(Suit.SPADES))))
        val ok = state(waste = listOf("2S"), foundationTops = mapOf(Suit.SPADES to 1))
        assertEquals(2, ok.apply(Move.Transfer(Pile.Waste, Pile.Foundation(Suit.SPADES)))!!.foundation(Suit.SPADES).size)
    }

    @Test fun wasteToTableauAlternatingDescending() {
        val s = state(waste = listOf("9H"), tableau = tableau(listOf(up("10S"))))
            .apply(Move.Transfer(Pile.Waste, Pile.Tableau(0)))!!
        assertEquals(listOf(up("10S"), up("9H")), s.tableau[0])
        assertEquals(5, s.score)
    }

    @Test fun tableauRejectsSameColorOrWrongRank() {
        val base = tableau(listOf(up("10S")))
        assertNull(state(waste = listOf("9C"), tableau = base).apply(Move.Transfer(Pile.Waste, Pile.Tableau(0))))
        assertNull(state(waste = listOf("8H"), tableau = base).apply(Move.Transfer(Pile.Waste, Pile.Tableau(0))))
    }

    @Test fun onlyKingsGoToEmptyTableau() {
        assertNull(state(waste = listOf("QH")).apply(Move.Transfer(Pile.Waste, Pile.Tableau(0))))
        val s = state(waste = listOf("KH")).apply(Move.Transfer(Pile.Waste, Pile.Tableau(0)))!!
        assertEquals(listOf(up("KH")), s.tableau[0])
    }

    @Test fun moveRunAndFlipRevealedCard() {
        val s = state(
            tableau = tableau(
                listOf(down("2C"), up("9H"), up("8S"), up("7H")),
                listOf(up("10C")),
            ),
        ).apply(Move.Transfer(Pile.Tableau(0), Pile.Tableau(1), count = 3))!!
        assertEquals(listOf(up("2C")), s.tableau[0])
        assertEquals(listOf(up("10C"), up("9H"), up("8S"), up("7H")), s.tableau[1])
        assertEquals(5, s.score)
    }

    @Test fun cannotPickUpFaceDownCards() {
        val st = state(tableau = tableau(listOf(down("KC"), up("QH")), emptyList()))
        assertNull(st.apply(Move.Transfer(Pile.Tableau(0), Pile.Tableau(1), count = 2)))
    }

    @Test fun foundationToTableauCostsPoints() {
        val s = state(
            foundationTops = mapOf(Suit.HEARTS to 9),
            tableau = tableau(listOf(up("10S"))),
            score = 10,
        ).apply(Move.Transfer(Pile.Foundation(Suit.HEARTS), Pile.Tableau(0)))!!
        assertEquals(listOf(up("10S"), up("9H")), s.tableau[0])
        assertEquals(0, s.score)
    }

    @Test fun sameSourceAndTargetIsIllegal() {
        val st = state(tableau = tableau(listOf(up("KC"))))
        assertNull(st.apply(Move.Transfer(Pile.Tableau(0), Pile.Tableau(0))))
    }

    @Test fun cannotTransferToWaste() {
        val st = state(tableau = tableau(listOf(up("KC"))))
        assertNull(st.apply(Move.Transfer(Pile.Tableau(0), Pile.Waste)))
    }

    @Test fun multiCardToFoundationIsIllegal() {
        val st = state(tableau = tableau(listOf(up("2S"), up("AH"))), foundationTops = mapOf(Suit.HEARTS to 0))
        assertNull(st.apply(Move.Transfer(Pile.Tableau(0), Pile.Foundation(Suit.HEARTS), count = 2)))
    }

    @Test fun countOutOfRangeIsIllegal() {
        val st = state(tableau = tableau(listOf(up("KC")), emptyList()))
        assertNull(st.apply(Move.Transfer(Pile.Tableau(0), Pile.Tableau(1), count = 2)))
        assertNull(st.apply(Move.Transfer(Pile.Tableau(0), Pile.Tableau(1), count = 0)))
        assertNull(st.apply(Move.Transfer(Pile.Tableau(9), Pile.Tableau(1))))
    }
}
