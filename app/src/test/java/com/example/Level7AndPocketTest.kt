package com.example

import com.example.engine.DeckEngine
import com.example.engine.MeldDetector
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class Level7AndPocketTest {

    private fun c(suit: Suit, rank: Rank, id: String): Card {
        return Card(
            id = id,
            rank = rank,
            suit = suit
        )
    }

    private fun wild(id: String): Card {
        return Card(
            id = id,
            rank = Rank.JOKER,
            suit = Suit.NONE
        )
    }

    @Test
    fun testLevel7ValidationSuccessZeroLeftovers() {
        // Run 1: 4 cards (Diamonds 4, 5, 6, 7)
        val r1 = listOf(
            c(Suit.DIAMONDS, Rank.FOUR, "d4"),
            c(Suit.DIAMONDS, Rank.FIVE, "d5"),
            c(Suit.DIAMONDS, Rank.SIX, "d6"),
            c(Suit.DIAMONDS, Rank.SEVEN, "d7")
        )
        // Run 2: 4 cards (Spades 9, 10, J, Q)
        val r2 = listOf(
            c(Suit.SPADES, Rank.NINE, "s9"),
            c(Suit.SPADES, Rank.TEN, "s10"),
            c(Suit.SPADES, Rank.JACK, "sj"),
            c(Suit.SPADES, Rank.QUEEN, "sq")
        )
        // Run 3: 4 cards (Hearts Wild (Joker), 4, 5, 6)
        val r3 = listOf(
            wild("w2"),
            c(Suit.HEARTS, Rank.FOUR, "h4"),
            c(Suit.HEARTS, Rank.FIVE, "h5"),
            c(Suit.HEARTS, Rank.SIX, "h6")
        )

        val totalHandCount = r1.size + r2.size + r3.size
        val result = MeldDetector.validateLevel7Reveal(
            run1Cards = r1,
            run2Cards = r2,
            run3Cards = r3,
            unassignedCards = emptyList(),
            totalHandCount = totalHandCount
        )

        assertTrue("Validation must succeed when all cards are in 3 valid runs", result.isValid)
        assertEquals(0, result.leftoverCount)
    }

    @Test
    fun testLevel7ValidationFailsWithLeftoverCards() {
        val r1 = listOf(
            c(Suit.DIAMONDS, Rank.FOUR, "d4"),
            c(Suit.DIAMONDS, Rank.FIVE, "d5"),
            c(Suit.DIAMONDS, Rank.SIX, "d6"),
            c(Suit.DIAMONDS, Rank.SEVEN, "d7")
        )
        val r2 = listOf(
            c(Suit.SPADES, Rank.NINE, "s9"),
            c(Suit.SPADES, Rank.TEN, "s10"),
            c(Suit.SPADES, Rank.JACK, "sj"),
            c(Suit.SPADES, Rank.QUEEN, "sq")
        )
        val r3 = listOf(
            c(Suit.HEARTS, Rank.THREE, "h3"),
            c(Suit.HEARTS, Rank.FOUR, "h4"),
            c(Suit.HEARTS, Rank.FIVE, "h5"),
            c(Suit.HEARTS, Rank.SIX, "h6")
        )
        val leftover = listOf(c(Suit.CLUBS, Rank.ACE, "ca"))

        val totalHandCount = r1.size + r2.size + r3.size + leftover.size
        val result = MeldDetector.validateLevel7Reveal(
            run1Cards = r1,
            run2Cards = r2,
            run3Cards = r3,
            unassignedCards = leftover,
            totalHandCount = totalHandCount
        )

        assertFalse("Level 7 must fail if there is any leftover card outside the 3 runs", result.isValid)
        assertEquals(1, result.leftoverCount)
        assertNotNull("Error message should be present", result.errorMessage)
        assertTrue("Message must mention leftover cards", result.errorMessage!!.contains("leftover", ignoreCase = true))
    }

    @Test
    fun testLevel7ValidationFailsWhenWildsExceedNaturals() {
        // 2 wilds, 2 naturals -> naturals must strictly exceed wilds
        val r1 = listOf(
            wild("w1"),
            wild("w2"),
            c(Suit.DIAMONDS, Rank.SIX, "d6"),
            c(Suit.DIAMONDS, Rank.SEVEN, "d7")
        )
        val r2 = listOf(
            c(Suit.SPADES, Rank.NINE, "s9"),
            c(Suit.SPADES, Rank.TEN, "s10"),
            c(Suit.SPADES, Rank.JACK, "sj"),
            c(Suit.SPADES, Rank.QUEEN, "sq")
        )
        val r3 = listOf(
            c(Suit.HEARTS, Rank.THREE, "h3"),
            c(Suit.HEARTS, Rank.FOUR, "h4"),
            c(Suit.HEARTS, Rank.FIVE, "h5"),
            c(Suit.HEARTS, Rank.SIX, "h6")
        )

        val totalHandCount = r1.size + r2.size + r3.size
        val result = MeldDetector.validateLevel7Reveal(
            run1Cards = r1,
            run2Cards = r2,
            run3Cards = r3,
            unassignedCards = emptyList(),
            totalHandCount = totalHandCount
        )

        assertFalse("Run must have more naturals than wilds", result.isValid)
    }

    @Test
    fun testPocketCardOperationsConserveHand() {
        val hand = listOf(
            c(Suit.DIAMONDS, Rank.FOUR, "d4"),
            c(Suit.DIAMONDS, Rank.FIVE, "d5"),
            c(Suit.SPADES, Rank.TEN, "s10")
        )
        val player = Player(id = "p1", name = "Test", hand = hand)

        // Pocket d4
        val pocketed = player.withPocketedCards(setOf("d4"))
        assertEquals("Total hand size must remain conserved", 3, pocketed.hand.size)
        assertEquals("Active cards must exclude pocketed card", 2, pocketed.activeCards.size)
        assertEquals("Pocket cards must contain d4", setOf("d4"), pocketed.pocketCardIds)
        assertEquals("Points in hand must still count all 3 cards", hand.sumOf { it.points }, pocketed.pointsInHand)

        // Unpocket d4
        val unpocketed = pocketed.withUnpocketedCards(setOf("d4"))
        assertEquals(3, unpocketed.activeCards.size)
        assertTrue(unpocketed.pocketCardIds.isEmpty())

        // Empty pocket
        val emptyPocketPlayer = pocketed.withEmptyPocket()
        assertEquals(3, emptyPocketPlayer.activeCards.size)
        assertTrue(emptyPocketPlayer.pocketCardIds.isEmpty())
    }

    @Test
    fun testDeckEngineRevealLevel7WinFlow() {
        val game = DeckEngine.startNewGame("You")
        val l7Game = game.copy(currentLevel = 7)

        val r1 = listOf(
            c(Suit.DIAMONDS, Rank.FOUR, "d4"),
            c(Suit.DIAMONDS, Rank.FIVE, "d5"),
            c(Suit.DIAMONDS, Rank.SIX, "d6"),
            c(Suit.DIAMONDS, Rank.SEVEN, "d7")
        )
        val r2 = listOf(
            c(Suit.SPADES, Rank.NINE, "s9"),
            c(Suit.SPADES, Rank.TEN, "s10"),
            c(Suit.SPADES, Rank.JACK, "sj"),
            c(Suit.SPADES, Rank.QUEEN, "sq")
        )
        val r3 = listOf(
            c(Suit.HEARTS, Rank.THREE, "h3"),
            c(Suit.HEARTS, Rank.FOUR, "h4"),
            c(Suit.HEARTS, Rank.FIVE, "h5"),
            c(Suit.HEARTS, Rank.SIX, "h6")
        )

        val human = l7Game.humanPlayer!!
        val (stateAfterWin, winner) = DeckEngine.revealLevel7Win(l7Game, human.id, listOf(r1, r2, r3))

        assertEquals("Winner must be human", human.id, winner.id)
        assertTrue("Round must be over", stateAfterWin.isRoundOver)
        assertTrue("Tournament must be over after Level 7 win", stateAfterWin.isTournamentOver)
        assertEquals("Winner must have empty hand", 0, stateAfterWin.players.find { it.id == human.id }!!.hand.size)
        assertEquals("Winner penalty points for round must be 0", 0, stateAfterWin.players.find { it.id == human.id }!!.scoresPerLevel.last())
    }
}
