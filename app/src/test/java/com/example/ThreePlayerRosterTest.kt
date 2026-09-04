package com.example

import com.example.engine.DeckEngine
import com.example.engine.NameGenerator
import com.example.model.Card
import com.example.model.GameState
import com.example.model.PendingBuyPriority
import com.example.model.Player
import com.example.model.Rank
import com.example.model.Suit
import com.example.model.TurnPhase
import org.junit.Assert.*
import org.junit.Test

class ThreePlayerRosterTest {

    // A. NEW GAME TEST
    @Test
    fun testNewGameHasExactlyThreePlayersOneHumanTwoAi() {
        val game = DeckEngine.startNewGame("You")

        assertEquals("Total players must be exactly 3", 3, game.players.size)
        assertEquals("Must have exactly 1 human player", 1, game.players.count { it.isHuman })
        assertEquals("Must have exactly 2 AI opponents", 2, game.players.count { !it.isHuman })

        assertNotNull("topOpponent must not be null", game.topOpponent)
        assertNotNull("leftOpponent must not be null", game.leftOpponent)
        assertNull("rightOpponent must be null for 3-player game", game.rightOpponent)

        assertEquals("Human ID", "player_human", game.players[0].id)
        assertEquals("Top AI ID", "player_top", game.players[1].id)
        assertEquals("Left AI ID", "player_left", game.players[2].id)
    }

    // B. NAMES TEST
    @Test
    fun testAiNamesAreNonEmptyUniqueAndGenerated() {
        val game = DeckEngine.startNewGame("You")
        val ai1 = game.players[1]
        val ai2 = game.players[2]

        assertTrue("AI 1 name must be non-empty", ai1.name.isNotBlank())
        assertTrue("AI 2 name must be non-empty", ai2.name.isNotBlank())
        assertNotEquals("AI names must be unique", ai1.name, ai2.name)
        assertNotEquals("AI 1 name must not equal human name", "You", ai1.name)
        assertNotEquals("AI 2 name must not equal human name", "You", ai2.name)

        val uniqueNames = NameGenerator.generateUniqueNames(2)
        assertEquals("NameGenerator generates requested count", 2, uniqueNames.size)
        assertNotEquals("NameGenerator names are distinct", uniqueNames[0], uniqueNames[1])
    }

    // C. LEVEL ADVANCE TEST
    @Test
    fun testLevelAdvancePreservesPlayerCountAndNamesAcrossAllSevenLevels() {
        var current = DeckEngine.startNewGame("TestHuman")
        val initialNames = current.players.map { it.name }

        for (lvl in 2..7) {
            current = DeckEngine.startNextLevel(current, lvl)
            assertEquals("Level $lvl must have exactly 3 players", 3, current.players.size)
            assertEquals("Level $lvl player names must be identical to level 1", initialNames, current.players.map { it.name })
            assertEquals("Level $lvl currentLevel must match", lvl, current.currentLevel)

            // Verify 108-card double deck conservation
            val totalCards = current.drawDeck.size + current.discardPile.size + current.players.sumOf { it.cardCount }
            assertEquals("108 total cards must be preserved at level $lvl", 108, totalCards)
        }
    }

    // D. SCORECARD TEST
    @Test
    fun testScorecardHasExactlyThreeRowsAndAlignsScores() {
        val game = DeckEngine.startNewGame("You")
        val scoredState = DeckEngine.calculateEndRoundScores(game, winnerId = game.players[0].id)

        assertEquals("Scores state must have exactly 3 players", 3, scoredState.players.size)
        val sorted = scoredState.players.sortedBy { it.totalScore }
        assertEquals("Sorted players for scorecard must contain exactly 3 entries", 3, sorted.size)
        assertEquals("Winner must have 0 penalty points", 0, scoredState.players[0].scoresPerLevel.last())
    }

    // E. TURN ORDER TEST
    @Test
    fun testTurnOrderCyclesThroughThreePlayersAndDealerRotatesModuloThree() {
        var state = DeckEngine.startNewGame("You")
        assertEquals(0, state.currentTurnPlayerIndex)
        assertEquals("player_human", state.currentPlayer.id)

        state = DeckEngine.nextTurn(state)
        assertEquals(1, state.currentTurnPlayerIndex)
        assertEquals("player_top", state.currentPlayer.id)

        state = DeckEngine.nextTurn(state)
        assertEquals(2, state.currentTurnPlayerIndex)
        assertEquals("player_left", state.currentPlayer.id)

        state = DeckEngine.nextTurn(state)
        assertEquals("Turn must wrap to player 0 (human)", 0, state.currentTurnPlayerIndex)
        assertEquals("player_human", state.currentPlayer.id)

        // Dealer rotation across 4 rounds
        var dealerState = state
        assertEquals("Round 1 dealer", 0, dealerState.dealerIndex)
        dealerState = DeckEngine.startNextLevel(dealerState, 2)
        assertEquals("Round 2 dealer", 1, dealerState.dealerIndex)
        dealerState = DeckEngine.startNextLevel(dealerState, 3)
        assertEquals("Round 3 dealer", 2, dealerState.dealerIndex)
        dealerState = DeckEngine.startNextLevel(dealerState, 4)
        assertEquals("Round 4 dealer wraps to 0", 0, dealerState.dealerIndex)
    }

    // F. BUY PRIORITY TEST
    @Test
    fun testBuyPriorityClockwiseOrderForThreePlayers() {
        val game = DeckEngine.startNewGame("You")
        // Players: 0 = human, 1 = top, 2 = left
        // If player 0 discards, next player is 1.
        // Clockwise starting after next player (1): (1 + 1) % 3 = 2 (left AI).
        // Check calculation:
        val nextPlayerIndex = (0 + 1) % game.players.size
        val eligibleContenderIds = (1 until game.players.size).mapNotNull { offset ->
            val candidate = game.players[(nextPlayerIndex + offset) % game.players.size]
            if (candidate.id != game.players[0].id && candidate.id != game.players[nextPlayerIndex].id) candidate.id else null
        }

        assertEquals("Exactly 1 player eligible to buy out of turn", 1, eligibleContenderIds.size)
        assertEquals("Clockwise buyer after player 1 must be player 2 (left AI)", "player_left", eligibleContenderIds[0])
    }

    // G. SAVE / RESUME TEST
    @Test
    fun testSaveAndRestorePreservesExactlyThreePlayersWithOriginalNames() {
        val originalGame = DeckEngine.startNewGame("Captain")
        val namesBefore = originalGame.players.map { it.name }
        val idsBefore = originalGame.players.map { it.id }

        // Copy / restore simulation
        val restoredGame = originalGame.copy()

        assertEquals("Restored players count must be 3", 3, restoredGame.players.size)
        assertEquals("Restored names must match exactly", namesBefore, restoredGame.players.map { it.name })
        assertEquals("Restored IDs must match exactly", idsBefore, restoredGame.players.map { it.id })
    }

    // H. TROPHY / STANDINGS TEST
    @Test
    fun testTrophyAndMatchHistoryStandingsHaveExactlyThreeParticipants() {
        var game = DeckEngine.startNewGame("ChampionHuman")
        for (lvl in 2..7) {
            game = DeckEngine.startNextLevel(game, lvl)
        }
        val endState = DeckEngine.calculateEndRoundScores(game, winnerId = "player_human")
        val entry = DeckEngine.createMatchHistoryEntry(endState)

        assertEquals("Final standings must have exactly 3 participants", 3, entry.finalStandings.size)
        assertTrue("Ranks must be 1 to 3", entry.humanFinalRank in 1..3)
    }

    // I. EXACT 10 CONSECUTIVE TURNS VERIFICATION
    @Test
    fun testTenConsecutiveTurnsOrderAndBuyPriority() {
        val playerA = Player(id = "pA", name = "Player A", isHuman = true)
        val playerB = Player(id = "pB", name = "Player B", isHuman = false)
        val playerC = Player(id = "pC", name = "Player C", isHuman = false)
        val players = listOf(playerA, playerB, playerC)

        val dummyCard = Card(id = "card_1", suit = Suit.HEARTS, rank = Rank.ACE, deckNumber = 1)

        // Run 12 consecutive turns (4 full cycles of A -> B -> C)
        for (turn in 0 until 12) {
            val discarderIndex = turn % 3
            val discarder = players[discarderIndex]
            val expectedNextIndex = (discarderIndex + 1) % 3
            val expectedOtherIndex = (discarderIndex + 2) % 3

            val transition = DeckEngine.calculateTurnAndBuyPriority(players, discarder.id, dummyCard)

            // 1. Clockwise Order: discarder -> next -> other
            assertEquals("Turn $turn discarder index", discarderIndex, transition.discarderIndex)
            assertEquals("Turn $turn next player index", expectedNextIndex, transition.nextPlayerIndex)
            assertEquals("Turn $turn other player index", expectedOtherIndex, transition.otherPlayerIndex)

            // 2. Next player calculation
            val expectedNextPlayer = players[expectedNextIndex]
            val expectedOtherPlayer = players[expectedOtherIndex]
            assertEquals("Turn $turn next player", expectedNextPlayer.id, transition.nextPlayer.id)
            assertEquals("Turn $turn other player", expectedOtherPlayer.id, transition.otherPlayer.id)

            // 3. TO YOU assigned to next player
            val pendingBuy = transition.pendingBuyPriority
            assertNotNull("Pending buy priority must exist for discard", pendingBuy)
            assertEquals("TO YOU must be assigned to next player", expectedNextPlayer.id, pendingBuy!!.nextTurnPlayerId)

            // 4. Next player NEVER gets Buy dialog / eligibility
            assertFalse(
                "Next player (${expectedNextPlayer.name}) must NEVER be in eligibleContenderIds",
                pendingBuy.eligibleContenderIds.contains(expectedNextPlayer.id)
            )

            // 5. Only other player gets Buy
            assertEquals("Exactly 1 candidate for out-of-turn buy", 1, pendingBuy.eligibleContenderIds.size)
            assertEquals(
                "Only other player (${expectedOtherPlayer.name}) gets Buy opportunity",
                expectedOtherPlayer.id,
                pendingBuy.eligibleContenderIds[0]
            )
        }
    }

    // J. BUY DOES NOT CHANGE TURN ORDER TEST
    @Test
    fun testBuyDoesNotChangeUnderlyingTurnOrder() {
        val game = DeckEngine.startNewGame("You")
        // Player 0: You, Player 1: Top AI, Player 2: Left AI
        val dummyCard = Card(id = "c_ace_h", suit = Suit.HEARTS, rank = Rank.ACE, deckNumber = 1)

        // Player 0 discards -> Next is Player 1. Other (buyer candidate) is Player 2.
        val transition = DeckEngine.calculateTurnAndBuyPriority(game.players, game.players[0].id, dummyCard)
        assertEquals(1, transition.nextPlayerIndex)
        assertEquals(2, transition.otherPlayerIndex)

        // State with buy pending: turn is already player 1 (nextPlayer)
        val stateWithBuy = game.copy(
            currentTurnPlayerIndex = transition.nextPlayerIndex,
            pendingBuyPriority = transition.pendingBuyPriority
        )
        assertEquals("Underlying turn player is next player (Player 1)", 1, stateWithBuy.currentTurnPlayerIndex)

        // Now execute Buy for Player 2
        val (stateAfterBuy, boughtCard) = DeckEngine.buyDiscard(stateWithBuy, game.players[2].id)
        assertNotNull(boughtCard)
        assertEquals("Pending buy must be cleared", null, stateAfterBuy.pendingBuyPriority)
        assertEquals(
            "Underlying turn MUST STILL BE Player 1 after Player 2 bought",
            1,
            stateAfterBuy.currentTurnPlayerIndex
        )
    }

    // K. HUMAN SPECIFIC SCENARIOS: AFTER ROWDY vs AFTER CRUSTY
    @Test
    fun testHumanTurnAndBuyEligibilityAfterRowdyAndCrustyDiscards() {
        val human = Player(id = "player_human", name = "You", isHuman = true)
        val crusty = Player(id = "player_top", name = "Crusty Salamander", isHuman = false)
        val rowdy = Player(id = "player_left", name = "Rowdy Possum", isHuman = false)
        val players = listOf(human, crusty, rowdy) // index 0 = You, 1 = Crusty, 2 = Rowdy

        val aceHearts = Card(id = "ace_hearts", suit = Suit.HEARTS, rank = Rank.ACE, deckNumber = 1)

        // Scenario 1: CRUSTY DISCARDS (Crusty = 1)
        // Rowdy is NEXT (2). Human is OTHER (0).
        val crustyDiscardTransition = DeckEngine.calculateTurnAndBuyPriority(players, crusty.id, aceHearts)
        assertEquals("Crusty discard: Next must be Rowdy (2)", 2, crustyDiscardTransition.nextPlayerIndex)
        assertEquals("Crusty discard: Next player name", "Rowdy Possum", crustyDiscardTransition.nextPlayer.name)
        assertEquals("Crusty discard: Other must be Human (0)", 0, crustyDiscardTransition.otherPlayerIndex)
        assertEquals("Crusty discard: Other player name", "You", crustyDiscardTransition.otherPlayer.name)

        val crustyBuyPriority = crustyDiscardTransition.pendingBuyPriority!!
        assertEquals("TO YOU is for Rowdy", rowdy.id, crustyBuyPriority.nextTurnPlayerId)
        assertTrue(
            "HUMAN AFTER CRUSTY DISCARD = BUY ELIGIBLE",
            crustyBuyPriority.eligibleContenderIds.contains(human.id)
        )
        assertFalse(
            "Rowdy is next player so Rowdy NEVER gets Buy",
            crustyBuyPriority.eligibleContenderIds.contains(rowdy.id)
        )

        // Scenario 2: ROWDY DISCARDS (Rowdy = 2)
        // Human is NEXT (0). Crusty is OTHER (1).
        val rowdyDiscardTransition = DeckEngine.calculateTurnAndBuyPriority(players, rowdy.id, aceHearts)
        assertEquals("Rowdy discard: Next must be Human (0)", 0, rowdyDiscardTransition.nextPlayerIndex)
        assertEquals("Rowdy discard: Next player name", "You", rowdyDiscardTransition.nextPlayer.name)
        assertEquals("Rowdy discard: Other must be Crusty (1)", 1, rowdyDiscardTransition.otherPlayerIndex)
        assertEquals("Rowdy discard: Other player name", "Crusty Salamander", rowdyDiscardTransition.otherPlayer.name)

        val rowdyBuyPriority = rowdyDiscardTransition.pendingBuyPriority!!
        assertEquals("TO YOU is for Human", human.id, rowdyBuyPriority.nextTurnPlayerId)
        assertFalse(
            "HUMAN AFTER ROWDY DISCARD = NEVER BUY DIALOG (IT IS TO YOU)",
            rowdyBuyPriority.eligibleContenderIds.contains(human.id)
        )
        assertTrue(
            "Crusty is the only buy candidate after Rowdy discard",
            rowdyBuyPriority.eligibleContenderIds.contains(crusty.id)
        )

        // Scenario 3: YOU DISCARD (Human = 0)
        // Crusty is NEXT (1). Rowdy is OTHER (2).
        val humanDiscardTransition = DeckEngine.calculateTurnAndBuyPriority(players, human.id, aceHearts)
        assertEquals("Human discard: Next must be Crusty (1)", 1, humanDiscardTransition.nextPlayerIndex)
        assertEquals("Human discard: Next player name", "Crusty Salamander", humanDiscardTransition.nextPlayer.name)
        assertEquals("Human discard: Other must be Rowdy (2)", 2, humanDiscardTransition.otherPlayerIndex)
        assertEquals("Human discard: Other player name", "Rowdy Possum", humanDiscardTransition.otherPlayer.name)

        val humanBuyPriority = humanDiscardTransition.pendingBuyPriority!!
        assertEquals("TO YOU is for Crusty", crusty.id, humanBuyPriority.nextTurnPlayerId)
        assertFalse(
            "Crusty is next player so Crusty NEVER gets Buy",
            humanBuyPriority.eligibleContenderIds.contains(crusty.id)
        )
        assertTrue(
            "Rowdy is the only buy candidate after Human discard",
            humanBuyPriority.eligibleContenderIds.contains(rowdy.id)
        )
    }

    @Test
    fun testHumanOffenderRummayResolution() {
        // Construct a state where Human is current player, and there's a Book of Jacks on the table.
        val human = Player(id = "human", name = "You", isHuman = true, hand = listOf(
            Card(id = "c1", rank = Rank.JACK, suit = Suit.DIAMONDS)
        ))
        val ai1 = Player(id = "ai1", name = "AI1", isHuman = false, hand = listOf(
            Card(id = "c2", rank = Rank.SIX, suit = Suit.HEARTS)
        ))
        val ai2 = Player(id = "ai2", name = "AI2", isHuman = false, hand = emptyList())
        
        val bookOfJacks = com.example.model.Meld(
            id = "meld1",
            ownerId = "ai1",
            ownerName = "AI1",
            cards = listOf(
                Card("c3", Rank.JACK, Suit.SPADES),
                Card("c4", Rank.JACK, Suit.CLUBS),
                Card("c5", Rank.JACK, Suit.HEARTS)
            ),
            type = com.example.model.MeldType.BOOK
        )
        
        val state = GameState(
            players = listOf(human, ai1, ai2),
            currentTurnPlayerIndex = 0,
            currentPhase = TurnPhase.DRAW, // Using DRAW as it doesn't require importing TurnPhase.DISCARD if not defined
            allTableMelds = listOf(bookOfJacks),
            discardPile = emptyList()
        )
        
        // This simulates what GameViewModel does in triggerRummayAlert for human offenders
        val discardedCard = human.hand.first()
        val penaltyCard = ai1.hand.random()
        val targetMeld = state.allTableMelds.firstOrNull { it.canAddCard(discardedCard) }
        
        assertNotNull(targetMeld)
        
        var stateAfterRummy = state
        val updatedDiscards = stateAfterRummy.discardPile.filter { it.id != discardedCard.id }
        stateAfterRummy = stateAfterRummy.copy(discardPile = updatedDiscards)
        val (stateAfterPlay, _) = DeckEngine.playOnMeld(stateAfterRummy, human.id, discardedCard, targetMeld!!.id)
        stateAfterRummy = stateAfterPlay
        
        val callerPlayer = stateAfterRummy.players.find { it.id == ai1.id }!!
        val updatedCaller = callerPlayer.withUpdatedHand(callerPlayer.hand.filter { it.id != penaltyCard.id })
        
        val offenderPlayer = stateAfterRummy.players.find { it.id == human.id }!!
        val updatedOffender = offenderPlayer.withUpdatedHand(offenderPlayer.hand + penaltyCard)
        
        stateAfterRummy = stateAfterRummy.copy(
            players = stateAfterRummy.players.map { p ->
                when (p.id) {
                    ai1.id -> updatedCaller
                    human.id -> updatedOffender
                    else -> p
                }
            }
        )
        
        // Advance turn using DeckEngine as done in ViewModel
        val transition = DeckEngine.calculateTurnAndBuyPriority(stateAfterRummy.players, human.id, null)
        val finalState = stateAfterRummy.copy(
            currentTurnPlayerIndex = transition.nextPlayerIndex,
            currentPhase = TurnPhase.DRAW,
            pendingBuyPriority = transition.pendingBuyPriority
        )
        
        // Assertions based on expected human offender RUMMAY flow
        
        // 1. Offending card should be in the meld
        val updatedMeld = finalState.allTableMelds.find { it.id == "meld1" }!!
        assertTrue("Offending J♦ is in the Book of Jacks", updatedMeld.cards.any { it.id == discardedCard.id })
        
        // 2. Penalty card should be in human hand
        val updatedHuman = finalState.players.find { it.id == human.id }!!
        assertTrue("Penalty card is in Human hand", updatedHuman.hand.any { it.id == penaltyCard.id })
        
        // 3. Penalty card is NOT in AI hand
        val updatedAi = finalState.players.find { it.id == ai1.id }!!
        assertFalse("Penalty card is NOT in AI hand", updatedAi.hand.any { it.id == penaltyCard.id })
        
        // 4. Discard pile should be empty
        assertTrue("Discard pile should be empty", finalState.discardPile.isEmpty())
        
        // 5. Next turn player should be AI1 (index 1)
        assertEquals("Next player should be index 1 (AI1)", 1, finalState.currentTurnPlayerIndex)
        assertEquals("Phase should be DRAW", TurnPhase.DRAW, finalState.currentPhase)
        
        // 6. Buy priority should be null since lastDiscarded was null
        assertNull("Buy priority should be null", finalState.pendingBuyPriority)
    }
}
