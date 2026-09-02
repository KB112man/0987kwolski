package com.example

import com.example.engine.DeckEngine
import com.example.engine.MeldDetector
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testFullDeckIntegrity() {
        val deck = DeckEngine.createFullDeck()
        assertEquals(108, deck.size)
        val jokers = deck.filter { it.isJoker }
        assertEquals(4, jokers.size)
        val uniqueIds = deck.map { it.id }.toSet()
        assertEquals(108, uniqueIds.size)
    }

    @Test
    fun testValidBookDetection() {
        val cards = listOf(
            Card("c1", Rank.EIGHT, Suit.HEARTS),
            Card("c2", Rank.EIGHT, Suit.DIAMONDS),
            Card("c3", Rank.EIGHT, Suit.SPADES)
        )
        assertTrue(Meld.isValidBook(cards, minSize = 3))
    }

    @Test
    fun testValidRunDetection() {
        val cards = listOf(
            Card("c1", Rank.FOUR, Suit.HEARTS),
            Card("c2", Rank.FIVE, Suit.HEARTS),
            Card("c3", Rank.SIX, Suit.HEARTS),
            Card("c4", Rank.SEVEN, Suit.HEARTS)
        )
        assertTrue(Meld.isValidRun(cards, minSize = 4))
    }
}
