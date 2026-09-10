package com.example.engine.replay

import com.example.engine.DeckEngine
import com.example.model.ContractLevel
import com.example.model.replay.*
import org.junit.Assert.*
import org.junit.Test

class ReplayEngineTest {

    @Test
    fun testReplayEngineDeterministicExecution() {
        val canonical108 = DeckEngine.createFullDeck().map { it.id }
        assertEquals(108, canonical108.size)

        // Deal for Level 1 (dealCount = 10 cards per player):
        // 2 players:
        // p_human gets cards at round 0..9 -> indices 0, 2, 4, 6, 8, 10, 12, 14, 16, 18 (10 cards)
        // p_cpu1 gets cards at round 0..9  -> indices 1, 3, 5, 7, 9, 11, 13, 15, 17, 19 (10 cards)
        // Discard pile gets index 20 (1 card)
        // Stock begins at index 21 (87 cards remaining)

        val firstStockCardId = canonical108[21]
        val humanFirstCardId = canonical108[0]

        val cartridge = ReplayCartridge(
            schemaVersion = 1,
            matchId = "deterministic-engine-test",
            createdAtUtc = "2026-09-10T01:45:00Z",
            levelNumber = 1,
            players = listOf(
                ReplayPlayer(playerId = "p_human", displayName = "Kwush", seatIndex = 0, isHuman = true),
                ReplayPlayer(playerId = "p_cpu1", displayName = "Bot Alpha", seatIndex = 1, isHuman = false)
            ),
            initialDeckOrder = canonical108,
            actionLog = listOf(
                ReplayEvent.Draw(
                    playerId = "p_human",
                    cardId = firstStockCardId,
                    source = DrawSource.STOCK
                ),
                ReplayEvent.Discard(
                    playerId = "p_human",
                    cardId = humanFirstCardId
                )
            )
        )

        val engine = ReplayEngine(cartridge)
        val initialState = engine.getPublicState()

        assertEquals("Initial step index is 0", 0, initialState.currentStepIndex)
        assertEquals("Level 1", 1, initialState.levelNumber)
        assertEquals("Stock count initially 87", 87, initialState.stockCount)
        assertEquals("Discard count initially 1", 1, initialState.discardPileCount)
        assertEquals("Both players have 10 cards initially", 10, initialState.seats[0].cardCount)
        assertEquals("Both players have 10 cards initially", 10, initialState.seats[1].cardCount)

        // Step 1: Draw
        assertTrue("Step forward 1 succeeds", engine.stepForward())
        val afterDrawState = engine.getPublicState()
        assertEquals(1, afterDrawState.currentStepIndex)
        assertEquals("Stock decremented to 86", 86, afterDrawState.stockCount)
        assertEquals("Human has 11 cards after draw", 11, afterDrawState.seats[0].cardCount)

        // Step 2: Discard
        assertTrue("Step forward 2 succeeds", engine.stepForward())
        val afterDiscardState = engine.getPublicState()
        assertEquals(2, afterDiscardState.currentStepIndex)
        assertEquals("Discard count incremented to 2", 2, afterDiscardState.discardPileCount)
        assertEquals("Human has 10 cards after discard", 10, afterDiscardState.seats[0].cardCount)
        assertEquals("Top discard matches human discarded card", humanFirstCardId, afterDiscardState.topDiscard?.id)

        // Step Backward
        assertTrue("Step backward succeeds", engine.stepBackward())
        val rewindState = engine.getPublicState()
        assertEquals("Rewound to step 1", 1, rewindState.currentStepIndex)
        assertEquals(11, rewindState.seats[0].cardCount)
        assertEquals(1, rewindState.discardPileCount)
    }

    @Test
    fun testReplayEngineDivergenceThrowsException() {
        val canonical108 = DeckEngine.createFullDeck().map { it.id }
        // Intentionally mismatched card ID in DRAW event
        val wrongStockCardId = "card_1_SPADES_ACE" // Not the actual top of stock

        val cartridge = ReplayCartridge(
            schemaVersion = 1,
            matchId = "divergence-test",
            createdAtUtc = "2026-09-10T01:45:00Z",
            levelNumber = 1,
            players = listOf(
                ReplayPlayer("p_human", "Human", 0, true)
            ),
            initialDeckOrder = canonical108,
            actionLog = listOf(
                ReplayEvent.Draw(
                    playerId = "p_human",
                    cardId = wrongStockCardId,
                    source = DrawSource.STOCK
                )
            )
        )

        val engine = ReplayEngine(cartridge)
        try {
            engine.stepForward()
            fail("Expected divergence check to throw IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue("Exception message mentions divergence", e.message!!.contains("Divergence", ignoreCase = true))
        }
    }
}
