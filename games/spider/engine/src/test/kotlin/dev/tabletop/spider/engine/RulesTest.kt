package dev.tabletop.spider.engine

import dev.tabletop.cards.Rank
import dev.tabletop.cards.Suit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesTest {
    @Test fun dealShapeAndUniqueIdsInEveryMode() {
        for (suits in Suits.entries) {
            val s = GameState.deal(42, suits)
            assertEquals(listOf(6, 6, 6, 6, 5, 5, 5, 5, 5, 5), s.tableau.map { it.size })
            assertEquals(50, s.stock.size)
            assertEquals(5, s.dealsLeft)
            val all = s.stock + s.tableau.flatten()
            assertEquals(104, all.map { it.id }.toSet().size)
            assertEquals(suits.suits.toSet(), all.map { it.suit }.toSet())
            assertTrue(s.tableau.all { pile -> pile.last().faceUp && pile.dropLast(1).none { it.faceUp } })
            assertTrue(s.stock.none { it.faceUp })
        }
    }

    @Test fun dealIsReproducible() {
        assertEquals(GameState.deal(7, Suits.TWO), GameState.deal(7, Suits.TWO))
    }

    @Test fun deckHasEightOfEachRank() {
        assertEquals(8, GameState.deck(Suits.FOUR).count { it.rank == Rank.ACE })
        assertTrue(GameState.deck(Suits.ONE).all { it.suit == Suit.SPADES })
    }

    @Test fun anyCardOneRankLowerMayGoOnAnySuit() {
        val s = state(column("9S"), column("8H"))
        val next = s.apply(Move.Transfer(1, 0))!!
        assertEquals(listOf("9♠", "8♥"), next.tableau[0].map { it.card.toString() })
        assertTrue(next.tableau[1].isEmpty())
        assertEquals(1, next.moves)
        assertEquals(Scoring.START - 1, next.score)
    }

    @Test fun wrongRankIsRejected() {
        assertNull(state(column("9S"), column("7H")).apply(Move.Transfer(1, 0)))
        assertNull(state(column("9S"), column("9H")).apply(Move.Transfer(1, 0)))
    }

    @Test fun onlySameSuitRunsMoveTogether() {
        val s = state(column("-2C", "9S", "8H"), column("10D"))
        assertNull(s.pickUp(0, 2))
        assertNull(s.apply(Move.Transfer(0, 1, 2)))
        val sameSuit = state(column("-2C", "9S", "8S"), column("10D"))
        assertEquals(2, sameSuit.movableCount(0))
        val next = sameSuit.apply(Move.Transfer(0, 1, 2))!!
        assertEquals(3, next.tableau[1].size)
    }

    @Test fun faceDownCardsNeverMove() {
        val s = state(column("-9S", "8S"), column("10S"))
        assertNull(s.pickUp(0, 2))
        assertEquals(1, s.movableCount(0))
    }

    @Test fun movingUncoversAndFlipsTheNextCard() {
        val s = state(column("-4C", "9S"), column("10D"))
        val next = s.apply(Move.Transfer(0, 1))!!
        assertTrue(next.tableau[0].single().faceUp)
    }

    @Test fun anythingGoesIntoAnEmptyColumn() {
        val s = state(column(), column("-2C", "7H", "6H"))
        val next = s.apply(Move.Transfer(1, 0, 2))!!
        assertEquals(2, next.tableau[0].size)
    }

    @Test fun dealPutsOneFaceUpCardOnEveryColumn() {
        val s = GameState.deal(3, Suits.ONE)
        val next = s.apply(Move.Deal)!!
        assertEquals(40, next.stock.size)
        assertTrue(next.tableau.indices.all { next.tableau[it].size == s.tableau[it].size + 1 && next.tableau[it].last().faceUp })
        assertEquals(s.stock.take(10).map { it.id }, next.tableau.map { it.last().id })
    }

    @Test fun cannotDealWithAnEmptyColumnOrEmptyStock() {
        val stock = List(10) { c("-AS") }
        assertNull(state(column(), stock = stock).apply(Move.Deal))
        assertNull(state(column("5S")).apply(Move.Deal))
        assertTrue(state(column("5S"), stock = stock).canDeal)
    }

    @Test fun finishingARunClearsItAndScores() {
        val kingToTwo = run("H").dropLast(1)
        val s = state(column("-3C") + kingToTwo, column("AH"))
        val next = s.apply(Move.Transfer(1, 0))!!
        assertEquals(1, next.completed.size)
        assertEquals(Rank.KING, next.completed.single().first().rank)
        assertEquals(listOf("3♣"), next.tableau[0].map { it.card.toString() })
        assertTrue(next.tableau[0].single().faceUp)
        assertEquals(Scoring.START - 1 + Scoring.RUN, next.score)
    }

    @Test fun mixedSuitRunDoesNotComplete() {
        val mixed = run("H").dropLast(1) + c("AS")
        val s = state(mixed.dropLast(1), column("AS"))
        assertTrue(s.apply(Move.Transfer(1, 0))!!.completed.isEmpty())
    }

    @Test fun dealCanCompleteARun() {
        val stock = listOf(c("-AH")) + List(9) { c("-5C") }
        val s = state(run("H").dropLast(1), stock = stock)
        assertEquals(1, s.apply(Move.Deal)!!.completed.size)
    }

    @Test fun eighthRunWins() {
        val s = state(run("S").dropLast(1), column("AS"), completed = 7)
        assertFalse(s.isWon)
        assertTrue(s.apply(Move.Transfer(1, 0))!!.isWon)
    }

    @Test fun scoreNeverGoesNegative() {
        val s = state(column("9S"), column("8H")).copy(score = 0)
        assertEquals(0, s.apply(Move.Transfer(1, 0))!!.score)
    }

    @Test fun moveToSameColumnOrOutOfRangeIsRejected() {
        val s = state(column("9S"), column("8H"))
        assertNull(s.apply(Move.Transfer(0, 0)))
        assertNull(s.apply(Move.Transfer(1, 10)))
        assertNull(s.apply(Move.Transfer(1, 0, 0)))
    }
}
