package com.example

import com.example.engine.DeckEngine
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class CardIntegrityAssertionTest {

    @Test
    fun testStartGameIntegrityReport() {
        val state = DeckEngine.startNewGame("Kwush")
        val report = DeckEngine.generateIntegrityReport(state, "testStartGame")

        assertTrue(report.formatErrorMessage(), report.isValid)
        assertEquals(108, report.totalReferences)
        assertEquals(108, report.uniqueIds)
        assertTrue(report.missingIds.isEmpty())
        assertTrue(report.duplicatedIds.isEmpty())
    }

    @Test
    fun testPocketAndReorderRegressionConservation() {
        var state = DeckEngine.startNewGame("Kwush")
        val human = state.humanPlayer!!

        // Human has 10 cards initially in Level 1.
        // Move 5 cards into the Pocket
        val cardsToPocket = human.hand.take(5).map { it.id }.toSet()
        val pocketedHuman = human.withPocketedCards(cardsToPocket)

        var updatedPlayers = state.players.map { if (it.id == human.id) pocketedHuman else it }
        state = state.copy(players = updatedPlayers)

        var report = DeckEngine.generateIntegrityReport(state, "afterPocketing")
        assertTrue("State must be valid after pocketing cards: ${report.formatErrorMessage()}", report.isValid)
        assertEquals(5, state.humanPlayer!!.pocketCardIds.size)
        assertEquals(10, state.humanPlayer!!.hand.size)

        // Now simulate repeatedly reordering rack slots (the exact code path that caused BUG-002)
        var currentHuman = state.humanPlayer!!
        val activeCardToMove = currentHuman.activeCards.first().id
        for (i in 0 until 10) {
            val movedHuman = currentHuman.moveCardInRack(
                cardId = activeCardToMove,
                targetRowIndex = i % 2,
                targetSlotIndex = i % RACK_SLOTS_PER_ROW
            )
            currentHuman = movedHuman
            updatedPlayers = state.players.map { if (it.id == human.id) currentHuman else it }
            state = state.copy(players = updatedPlayers)

            report = DeckEngine.generateIntegrityReport(state, "moveCardInRack_iteration_$i")
            assertTrue("State must conserve all 108 cards during rack reorders: ${report.formatErrorMessage()}", report.isValid)
            assertEquals("Pocket count must remain unchanged", 5, currentHuman.pocketCardIds.size)
            assertEquals("Human hand total must remain unchanged", 10, currentHuman.hand.size)
            assertEquals(108, report.totalReferences)
            assertEquals(108, report.uniqueIds)
        }
    }

    @Test
    fun testPocketDrawRackMoveDiscardLifecycle() {
        var state = DeckEngine.startNewGame("Kwush")
        val human = state.humanPlayer!!

        // 1. Pocket 4 cards
        val pocketIds = human.hand.take(4).map { it.id }.toSet()
        val pocketedHuman = human.withPocketedCards(pocketIds)
        state = state.copy(players = state.players.map { if (it.id == human.id) pocketedHuman else it })

        var report = DeckEngine.generateIntegrityReport(state, "1_pocket")
        assertTrue(report.isValid)

        // 2. Human draws from Stock
        val (stateAfterDraw, drawnCard) = DeckEngine.drawCard(state, human.id)
        assertNotNull(drawnCard)
        state = stateAfterDraw
        report = DeckEngine.generateIntegrityReport(state, "2_drawStock")
        assertTrue(report.isValid)

        // 3. Move card in rack
        val humanAfterDraw = state.humanPlayer!!
        val cardIdToMove = humanAfterDraw.activeCards.first().id
        val rackMovedHuman = humanAfterDraw.moveCardInRack(cardIdToMove, 1, 5)
        state = state.copy(players = state.players.map { if (it.id == human.id) rackMovedHuman else it })
        report = DeckEngine.generateIntegrityReport(state, "3_rackMove")
        assertTrue(report.isValid)

        // 4. Discard an active card
        val discardTarget = state.humanPlayer!!.activeCards.first()
        val (stateAfterDiscard, _) = DeckEngine.discardCard(state, human.id, discardTarget)
        state = stateAfterDiscard
        report = DeckEngine.generateIntegrityReport(state, "4_discard")
        assertTrue(report.isValid)
        assertEquals(4, state.humanPlayer!!.pocketCardIds.size)
        assertEquals(108, report.totalReferences)
    }

    @Test
    fun testPocketBuyAndRackMoveLifecycle() {
        var state = DeckEngine.startNewGame("Kwush")
        val human = state.humanPlayer!!

        // Pocket 3 cards
        val pocketIds = human.hand.take(3).map { it.id }.toSet()
        state = state.copy(players = state.players.map { if (it.id == human.id) human.withPocketedCards(pocketIds) else it })

        // Execute BUY for Human via DeckEngine.buyDiscard
        val (stateAfterBuy, boughtCard) = DeckEngine.buyDiscard(state, human.id)
        assertNotNull(boughtCard)
        state = stateAfterBuy

        val report = DeckEngine.generateIntegrityReport(state, "afterBuy")
        assertTrue(report.isValid)
        assertEquals(3, state.humanPlayer!!.pocketCardIds.size)

        // Reorder rack slots
        val activeCard = state.humanPlayer!!.activeCards.first().id
        val rackMoved = state.humanPlayer!!.moveCardInRack(activeCard, 1, 2)
        state = state.copy(players = state.players.map { if (it.id == human.id) rackMoved else it })

        val reportAfterMove = DeckEngine.generateIntegrityReport(state, "afterRackMovePostBuy")
        assertTrue(reportAfterMove.isValid)
        assertEquals(3, state.humanPlayer!!.pocketCardIds.size)
        assertEquals(108, reportAfterMove.totalReferences)
    }

    @Test
    fun testStockRecycleIntegrity() {
        var state = DeckEngine.startNewGame("Kwush")

        // Force drawDeck to have 1 card and discard pile to have 30 cards
        val allAvailable = state.drawDeck + state.discardPile
        val smallDraw = listOf(allAvailable.first())
        val largeDiscard = allAvailable.drop(1)

        state = state.copy(
            drawDeck = smallDraw,
            discardPile = largeDiscard
        )

        var report = DeckEngine.generateIntegrityReport(state, "beforeRecycle")
        assertTrue(report.isValid)

        // Draw card 1 (drawDeck now empty)
        val (state1, card1) = DeckEngine.drawCard(state, state.humanPlayer!!.id)
        assertNotNull(card1)
        assertTrue("drawDeck should now be empty after drawing the last card", state1.drawDeck.isEmpty())
        assertTrue("discardPile still has 30 cards", state1.discardPile.size > 1)

        // Draw card 2 (this triggers recycle of discard pile into drawDeck)
        val (state2, card2) = DeckEngine.drawCard(state1, state1.humanPlayer!!.id)
        assertNotNull(card2)
        assertTrue("drawDeck should now be populated from recycled discards", state2.drawDeck.isNotEmpty())
        assertEquals("discardPile should now have only top card", 1, state2.discardPile.size)

        val reportRecycled = DeckEngine.generateIntegrityReport(state2, "afterDrawAndRecycle")
        assertTrue("Conservation must hold after stock recycle: ${reportRecycled.formatErrorMessage()}", reportRecycled.isValid)
        assertEquals(108, reportRecycled.totalReferences)
        assertEquals(108, reportRecycled.uniqueIds)
    }

    @Test
    fun testPocketWithExpandedHandReorder() {
        var state = DeckEngine.startNewGame("Kwush")
        val human = state.humanPlayer!!

        // Pocket 5 cards
        val pocketIds = human.hand.take(5).map { it.id }.toSet()
        val pocketedHuman = human.withPocketedCards(pocketIds)
        state = state.copy(players = state.players.map { if (it.id == human.id) pocketedHuman else it })

        val initialHuman = state.humanPlayer!!
        val card1 = initialHuman.hand[0]
        val card2 = initialHuman.hand[5]

        // Swap cards in hand (simulating ExpandedHand reorder)
        val hand = initialHuman.hand.toMutableList()
        val idx1 = hand.indexOfFirst { it.id == card1.id }
        val idx2 = hand.indexOfFirst { it.id == card2.id }
        val temp = hand[idx1]
        hand[idx1] = hand[idx2]
        hand[idx2] = temp
        val swappedHuman = initialHuman.copy(hand = hand).reorganizeHandIntoRack()

        state = state.copy(players = state.players.map { if (it.id == human.id) swappedHuman else it })

        val report = DeckEngine.generateIntegrityReport(state, "expandedHandSwap")
        assertTrue(report.isValid)
        assertEquals(5, state.humanPlayer!!.pocketCardIds.size)
        assertEquals(108, report.totalReferences)
        assertEquals(108, report.uniqueIds)
    }

    @Test
    fun testUniversalDragAndDropInsertAndShift() {
        var state = DeckEngine.startNewGame("Kwush")
        val human = state.humanPlayer!!

        // Pocket 3 cards out of 10
        val pocketIds = human.hand.take(3).map { it.id }.toSet()
        var currentHuman = human.withPocketedCards(pocketIds)
        assertEquals(7, currentHuman.activeCards.size)
        assertEquals(3, currentHuman.pocketCards.size)

        // Perform insertAndShift from slot 0 to slot 5
        val firstActiveCard = currentHuman.activeCards[0]
        val secondActiveCard = currentHuman.activeCards[1]
        currentHuman = currentHuman.insertAndShiftCardInHand(firstActiveCard.id, 5)

        // The card that was second should now be first
        assertEquals(secondActiveCard.id, currentHuman.activeCards[0].id)
        assertEquals(firstActiveCard.id, currentHuman.activeCards[5].id)
        assertEquals(7, currentHuman.activeCards.size)
        assertEquals(3, currentHuman.pocketCards.size)
        assertEquals(10, currentHuman.hand.size)

        // Verify with full state integrity audit
        state = state.copy(players = state.players.map { if (it.id == human.id) currentHuman else it })
        val report = DeckEngine.generateIntegrityReport(state, "testUniversalDragAndDropInsertAndShift")
        assertTrue("Conservation must hold after insertAndShift: ${report.formatErrorMessage()}", report.isValid)
        assertEquals(108, report.totalReferences)
        assertEquals(108, report.uniqueIds)
    }

    @Test
    fun testDragCardDirectlyToPocket() {
        var state = DeckEngine.startNewGame("Kwush")
        val human = state.humanPlayer!!

        val cardToDropInPocket = human.activeCards[0]
        val updatedHuman = human.withPocketedCards(setOf(cardToDropInPocket.id))

        assertEquals(1, updatedHuman.pocketCardIds.size)
        assertTrue(updatedHuman.pocketCardIds.contains(cardToDropInPocket.id))
        assertEquals(human.hand.size - 1, updatedHuman.activeCards.size)
        assertEquals(human.hand.size, updatedHuman.hand.size)

        state = state.copy(players = state.players.map { if (it.id == human.id) updatedHuman else it })
        val report = DeckEngine.generateIntegrityReport(state, "testDragCardDirectlyToPocket")
        assertTrue("Conservation must hold after dragging card to Pocket: ${report.formatErrorMessage()}", report.isValid)
        assertEquals(108, report.totalReferences)
        assertEquals(108, report.uniqueIds)
    }
}
