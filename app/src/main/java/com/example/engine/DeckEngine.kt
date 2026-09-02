package com.example.engine

import com.example.model.Card
import com.example.model.ContractLevel
import com.example.model.Player
import com.example.model.Rank
import com.example.model.Suit
import kotlin.random.Random

data class DealResult(
    val players: List<Player>,
    val drawDeck: List<Card>,
    val discardPile: List<Card>
)

object DeckEngine {
    /**
     * Builds the complete 108-card deck:
     * 2 standard 52-card decks + 4 Jokers.
     */
    fun createFullDeck(): List<Card> {
        val cards = mutableListOf<Card>()
        for (deckNum in 1..2) {
            for (suit in listOf(Suit.HEARTS, Suit.DIAMONDS, Suit.CLUBS, Suit.SPADES)) {
                for (rank in listOf(
                    Rank.TWO, Rank.THREE, Rank.FOUR, Rank.FIVE, Rank.SIX,
                    Rank.SEVEN, Rank.EIGHT, Rank.NINE, Rank.TEN,
                    Rank.JACK, Rank.QUEEN, Rank.KING, Rank.ACE
                )) {
                    cards.add(
                        Card(
                            id = "card_${deckNum}_${suit.name}_${rank.name}",
                            rank = rank,
                            suit = suit,
                            deckNumber = deckNum
                        )
                    )
                }
            }
        }
        for (jokerIdx in 1..4) {
            cards.add(
                Card(
                    id = "joker_$jokerIdx",
                    rank = Rank.JOKER,
                    suit = Suit.NONE,
                    deckNumber = if (jokerIdx <= 2) 1 else 2
                )
            )
        }
        return cards
    }

    /**
     * Shuffles the deck.
     */
    fun shuffleDeck(deck: List<Card>): List<Card> {
        return deck.shuffled(Random.Default)
    }

    fun dealRound(
        playerCount: Int,
        cardsPerPlayer: Int,
        deck: List<Card>
    ): Triple<List<List<Card>>, List<Card>, List<Card>> {
        val workingDeck = deck.toMutableList()
        val playerHands = List(playerCount) { mutableListOf<Card>() }
        for (round in 0 until cardsPerPlayer) {
            for (p in 0 until playerCount) {
                if (workingDeck.isNotEmpty()) {
                    playerHands[p].add(workingDeck.removeAt(0))
                }
            }
        }
        val discardPile = mutableListOf<Card>()
        if (workingDeck.isNotEmpty()) {
            discardPile.add(workingDeck.removeAt(0))
        }
        return Triple(playerHands, workingDeck, discardPile)
    }

    fun dealNewRound(players: List<Player>, contract: ContractLevel): DealResult {
        val fullDeck = shuffleDeck(createFullDeck())
        val (hands, remainingStock, discards) = dealRound(players.size, contract.dealCount, fullDeck)
        val updatedPlayers = players.mapIndexed { index, player ->
            val hand = hands[index]
            player.copy(hand = hand).reorganizeHandIntoRack()
        }
        return DealResult(
            players = updatedPlayers,
            drawDeck = remainingStock,
            discardPile = discards
        )
    }

    fun drawCard(
        drawDeck: List<Card>,
        discardPile: List<Card>
    ): Triple<List<Card>, Card?, List<Card>> {
        if (drawDeck.isNotEmpty()) {
            val card = drawDeck.first()
            val newDeck = drawDeck.drop(1)
            return Triple(newDeck, card, discardPile)
        }

        // Recycle discard pile if draw deck is empty
        if (discardPile.size > 1) {
            val (recycledDeck, newDiscards) = recycleDiscardPile(discardPile)
            if (recycledDeck.isNotEmpty()) {
                val card = recycledDeck.first()
                val newDeck = recycledDeck.drop(1)
                return Triple(newDeck, card, newDiscards)
            }
        }

        return Triple(emptyList(), null, discardPile)
    }

    fun recycleDiscardPile(discardPile: List<Card>): Pair<List<Card>, List<Card>> {
        if (discardPile.size <= 1) {
            return Pair(emptyList(), discardPile)
        }
        val topDiscard = discardPile.last()
        val otherCards = discardPile.subList(0, discardPile.size - 1)
        val newDrawDeck = otherCards.shuffled(Random.Default)
        val newDiscardPile = listOf(topDiscard)
        return Pair(newDrawDeck, newDiscardPile)
    }

    fun gatherAndVerifyLevelCards(gameState: com.example.model.GameState): List<Card> {
        val gathered = gameState.allTrackedCards
        check(gathered.size == 108) {
            "Level end card gathering error: Expected 108 cards, but found ${gathered.size} cards."
        }
        val uniqueIds = gathered.map { it.id }.toSet()
        check(uniqueIds.size == 108) {
            "Level end card integrity error: Expected 108 unique card IDs, but found ${uniqueIds.size} unique IDs."
        }
        return gathered
    }
}
