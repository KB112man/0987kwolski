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

    @Test
    fun testLevel7ContiguousSameSuitRejection() {
        // Run 1: Hearts 2, 3, 4, 5
        val r1 = listOf(
            c(Suit.HEARTS, Rank.TWO, "h2"),
            c(Suit.HEARTS, Rank.THREE, "h3"),
            c(Suit.HEARTS, Rank.FOUR, "h4"),
            c(Suit.HEARTS, Rank.FIVE, "h5")
        )
        // Run 2: Hearts 6, 7, 8, 9 (directly continues Hearts 2-5 -> INVALID as separate runs)
        val r2 = listOf(
            c(Suit.HEARTS, Rank.SIX, "h6"),
            c(Suit.HEARTS, Rank.SEVEN, "h7"),
            c(Suit.HEARTS, Rank.EIGHT, "h8"),
            c(Suit.HEARTS, Rank.NINE, "h9")
        )
        // Run 3: Spades 4, 5, 6, 7
        val r3 = listOf(
            c(Suit.SPADES, Rank.FOUR, "s4"),
            c(Suit.SPADES, Rank.FIVE, "s5"),
            c(Suit.SPADES, Rank.SIX, "s6"),
            c(Suit.SPADES, Rank.SEVEN, "s7")
        )

        val totalHandCount = r1.size + r2.size + r3.size
        val result = MeldDetector.validateLevel7Reveal(
            run1Cards = r1,
            run2Cards = r2,
            run3Cards = r3,
            unassignedCards = emptyList(),
            totalHandCount = totalHandCount
        )

        assertFalse("Contiguous runs in the same suit must be rejected", result.isValid)
        assertTrue("Error message must mention contiguous same-suit runs",
            result.errorMessage!!.contains("contiguous in the same suit", ignoreCase = true))
    }

    @Test
    fun testLevel7BacktrackingSolver18CardsZeroLeftovers() {
        // Hand of 18 cards (e.g. after buys/draws):
        // Run 1: Clubs 3, 4, 5, 6, 7, 8 (6 cards)
        val r1 = listOf(
            c(Suit.CLUBS, Rank.THREE, "c3"),
            c(Suit.CLUBS, Rank.FOUR, "c4"),
            c(Suit.CLUBS, Rank.FIVE, "c5"),
            c(Suit.CLUBS, Rank.SIX, "c6"),
            c(Suit.CLUBS, Rank.SEVEN, "c7"),
            c(Suit.CLUBS, Rank.EIGHT, "c8")
        )
        // Run 2: Diamonds 8, 9, 10, J, Q, K (6 cards)
        val r2 = listOf(
            c(Suit.DIAMONDS, Rank.EIGHT, "d8"),
            c(Suit.DIAMONDS, Rank.NINE, "d9"),
            c(Suit.DIAMONDS, Rank.TEN, "d10"),
            c(Suit.DIAMONDS, Rank.JACK, "dj"),
            c(Suit.DIAMONDS, Rank.QUEEN, "dq"),
            c(Suit.DIAMONDS, Rank.KING, "dk")
        )
        // Run 3: Spades 2, 3, 4, Wild, 6, 7 (6 cards with Joker)
        val r3 = listOf(
            c(Suit.SPADES, Rank.TWO, "s2"),
            c(Suit.SPADES, Rank.THREE, "s3"),
            c(Suit.SPADES, Rank.FOUR, "s4"),
            wild("w1"),
            c(Suit.SPADES, Rank.SIX, "s6"),
            c(Suit.SPADES, Rank.SEVEN, "s7")
        )

        val fullHand18 = (r1 + r2 + r3).shuffled()
        assertEquals(18, fullHand18.size)

        val workspace = MeldDetector.solveLevel7Arrangement(fullHand18)
        assertEquals("Should find exactly 3 valid runs", 3, workspace.validRunCount)
        assertEquals("All 18 cards should be assigned", 18, workspace.assignedCount)
        assertTrue("Zero leftovers for complete 18-card win", workspace.leftoverIds.isEmpty())

        val winningRuns = MeldDetector.findLevel7WinningRuns(fullHand18)
        assertNotNull("Winning runs should be found", winningRuns)
        assertEquals(3, winningRuns!!.size)
    }

    @Test
    fun testLevel7PartialAutoArrangeReturnsBestSubsetWhenIncomplete() {
        // Hand of 14 cards:
        // Run 1: Clubs 3, 4, 5, 6 (4 cards)
        // Run 2: Diamonds 7, 8, 9, 10 (4 cards)
        // Leftovers: 6 mismatched cards (no 3rd run possible)
        val hand = listOf(
            c(Suit.CLUBS, Rank.THREE, "c3"),
            c(Suit.CLUBS, Rank.FOUR, "c4"),
            c(Suit.CLUBS, Rank.FIVE, "c5"),
            c(Suit.CLUBS, Rank.SIX, "c6"),
            c(Suit.DIAMONDS, Rank.SEVEN, "d7"),
            c(Suit.DIAMONDS, Rank.EIGHT, "d8"),
            c(Suit.DIAMONDS, Rank.NINE, "d9"),
            c(Suit.DIAMONDS, Rank.TEN, "d10"),
            c(Suit.HEARTS, Rank.TWO, "h2"),
            c(Suit.HEARTS, Rank.SEVEN, "h7"),
            c(Suit.SPADES, Rank.ACE, "sa"),
            c(Suit.SPADES, Rank.KING, "sk"),
            c(Suit.CLUBS, Rank.JACK, "cj"),
            c(Suit.DIAMONDS, Rank.TWO, "d2")
        )

        val workspace = MeldDetector.solveLevel7Arrangement(hand)
        assertEquals("Should identify best 2 runs", 2, workspace.validRunCount)
        assertEquals("Should assign 8 cards into the 2 runs", 8, workspace.assignedCount)
        assertEquals("Should leave 6 cards in leftovers", 6, workspace.leftoverIds.size)

        // Hand is not complete win yet
        assertNull("findLevel7WinningRuns should be null when win condition not met",
            MeldDetector.findLevel7WinningRuns(hand))
    }
}
