package com.example.engine.replay

import com.example.model.Card
import com.example.model.ContractLevel
import com.example.model.Rank
import com.example.model.Suit
import com.example.model.replay.*

/**
 * PublicReplayState represents the entertainment table view:
 * - Public information ONLY (Stock count, top Discard, public Books & Runs, PLAY ON events)
 * - Private hand faces, Pockets, Racks, and hidden draws remain private until played publicly.
 */
data class PublicSeat(
    val playerId: String,
    val displayName: String,
    val seatIndex: Int,
    val isHuman: Boolean,
    val cardCount: Int,
    val isDown: Boolean,
    val laidMelds: List<ReplayMeld>
)

data class PublicReplayState(
    val currentStepIndex: Int,
    val totalSteps: Int,
    val levelNumber: Int,
    val stockCount: Int,
    val topDiscard: Card?,
    val discardPileCount: Int,
    val seats: List<PublicSeat>,
    val tableMelds: List<ReplayMeld>,
    val lastActionDescription: String,
    val isRoundComplete: Boolean = false,
    val winnerId: String? = null
)

/**
 * ReplayEngine:
 * Reconstructs the exact game state deterministically step-by-step from the initial condition and action log.
 * Validates expected card states against logged events.
 * Emits PublicReplayState for UI display.
 * COMPLETELY ISOLATED from live tournament save state (GamePreferences / GameViewModel).
 */
class ReplayEngine(val cartridge: ReplayCartridge) {

    private val masterCardMap: Map<String, Card> = buildMasterCardMap()

    // Internal reconstructed full state
    private val fullDeckOrder: List<String> = cartridge.initialDeckOrder
    private val players: List<ReplayPlayer> = cartridge.players
    private val playerHands = mutableMapOf<String, MutableList<String>>()
    private val stock = mutableListOf<String>()
    private val discardPile = mutableListOf<String>()
    private val tableMelds = mutableListOf<ReplayMeld>()

    private var currentEventIndex = 0
    private var isInitialized = false
    private var lastDescription = "Tournament initial deal complete."
    private var roundWinnerId: String? = null

    init {
        resetToInitialDeal()
    }

    private fun buildMasterCardMap(): Map<String, Card> {
        val map = mutableMapOf<String, Card>()
        for (deckNum in 1..2) {
            for (suit in listOf(Suit.HEARTS, Suit.DIAMONDS, Suit.CLUBS, Suit.SPADES)) {
                for (rank in listOf(
                    Rank.TWO, Rank.THREE, Rank.FOUR, Rank.FIVE, Rank.SIX,
                    Rank.SEVEN, Rank.EIGHT, Rank.NINE, Rank.TEN,
                    Rank.JACK, Rank.QUEEN, Rank.KING, Rank.ACE
                )) {
                    val id = "card_${deckNum}_${suit.name}_${rank.name}"
                    map[id] = Card(id = id, rank = rank, suit = suit, deckNumber = deckNum)
                }
            }
        }
        for (jokerIdx in 1..4) {
            val id = "joker_$jokerIdx"
            map[id] = Card(id = id, rank = Rank.JOKER, suit = Suit.NONE, deckNumber = if (jokerIdx <= 2) 1 else 2)
        }
        return map
    }

    fun resetToInitialDeal() {
        playerHands.clear()
        stock.clear()
        discardPile.clear()
        tableMelds.clear()
        currentEventIndex = 0
        roundWinnerId = null

        val contract = ContractLevel.fromLevelNumber(cartridge.levelNumber)
        val dealCount = contract.dealCount
        val workingDeck = fullDeckOrder.toMutableList()

        players.forEach { player ->
            playerHands[player.playerId] = mutableListOf()
        }

        // Deal cards in order
        for (round in 0 until dealCount) {
            for (player in players) {
                if (workingDeck.isNotEmpty()) {
                    playerHands[player.playerId]?.add(workingDeck.removeAt(0))
                }
            }
        }

        // First discard
        if (workingDeck.isNotEmpty()) {
            discardPile.add(workingDeck.removeAt(0))
        }

        // Remaining becomes Stock
        stock.addAll(workingDeck)
        lastDescription = "Dealt Level ${cartridge.levelNumber} (${contract.shortRequirement}, Deal $dealCount)."
        isInitialized = true
    }

    /**
     * Executes the next event in the action log.
     * Returns true if advanced, false if at end or divergence.
     */
    fun stepForward(): Boolean {
        if (currentEventIndex >= cartridge.actionLog.size) return false

        val event = cartridge.actionLog[currentEventIndex]
        val playerName = players.find { it.playerId == (when (event) {
            is ReplayEvent.Draw -> event.playerId
            is ReplayEvent.Discard -> event.playerId
            is ReplayEvent.Buy -> event.playerId
            is ReplayEvent.GoDown -> event.playerId
            is ReplayEvent.PlayOn -> event.playerId
            is ReplayEvent.RummayCall -> event.callerId
            is ReplayEvent.RoundEnd -> event.winnerId
            is ReplayEvent.StockRecycle -> "Dealer"
        }) }?.displayName ?: "Player"

        when (event) {
            is ReplayEvent.Draw -> {
                when (event.source) {
                    DrawSource.STOCK -> {
                        if (stock.isEmpty()) {
                            throw IllegalStateException("Divergence: Draw from empty Stock at step #$currentEventIndex")
                        }
                        val drawn = stock.removeAt(0)
                        if (drawn != event.cardId) {
                            throw IllegalStateException("Divergence: Expected stock card '${event.cardId}', got '$drawn' at step #$currentEventIndex")
                        }
                        playerHands[event.playerId]?.add(drawn)
                        lastDescription = "$playerName drew from Stock."
                    }
                    DrawSource.DISCARD -> {
                        if (discardPile.isEmpty()) {
                            throw IllegalStateException("Divergence: Draw from empty Discard at step #$currentEventIndex")
                        }
                        val drawn = discardPile.removeAt(discardPile.size - 1)
                        if (drawn != event.cardId) {
                            throw IllegalStateException("Divergence: Expected discard '${event.cardId}', got '$drawn' at step #$currentEventIndex")
                        }
                        playerHands[event.playerId]?.add(drawn)
                        val cardName = masterCardMap[drawn]?.displayName ?: drawn
                        lastDescription = "$playerName picked up $cardName from Discard."
                    }
                }
            }
            is ReplayEvent.Discard -> {
                val hand = playerHands[event.playerId]
                if (hand == null || !hand.remove(event.cardId)) {
                    throw IllegalStateException("Divergence: Player '${event.playerId}' discarded '${event.cardId}' not in hand at step #$currentEventIndex")
                }
                discardPile.add(event.cardId)
                val cardName = masterCardMap[event.cardId]?.displayName ?: event.cardId
                lastDescription = "$playerName discarded $cardName."
            }
            is ReplayEvent.Buy -> {
                val hand = playerHands[event.playerId]
                    ?: throw IllegalStateException("Unknown buyer '${event.playerId}'")
                // Take face up discard
                if (discardPile.isEmpty() || discardPile.last() != event.faceUpCardId) {
                    throw IllegalStateException("Divergence: Face up buy mismatch at step #$currentEventIndex")
                }
                discardPile.removeAt(discardPile.size - 1)
                hand.add(event.faceUpCardId)

                // Take penalty stock card
                if (stock.isEmpty() || stock.first() != event.stockCardId) {
                    throw IllegalStateException("Divergence: Stock buy card mismatch at step #$currentEventIndex")
                }
                stock.removeAt(0)
                hand.add(event.stockCardId)

                // Resulting discard should match
                if (discardPile.isNotEmpty() && discardPile.last() != event.resultingDiscardId) {
                    // Previous discard beneath
                }
                val boughtName = masterCardMap[event.faceUpCardId]?.displayName ?: event.faceUpCardId
                lastDescription = "$playerName BOUGHT $boughtName (+ 1 penalty card)."
            }
            is ReplayEvent.GoDown -> {
                val hand = playerHands[event.playerId]
                    ?: throw IllegalStateException("Unknown player '${event.playerId}'")
                for (meld in event.melds) {
                    for (cardId in meld.cardIds) {
                        if (!hand.remove(cardId)) {
                            throw IllegalStateException("Divergence: Card '$cardId' laid down not in player's hand at step #$currentEventIndex")
                        }
                    }
                    tableMelds.add(meld)
                }
                lastDescription = "$playerName WENT DOWN with ${event.melds.size} melds!"
            }
            is ReplayEvent.PlayOn -> {
                val hand = playerHands[event.playerId]
                    ?: throw IllegalStateException("Unknown player '${event.playerId}'")
                if (!hand.remove(event.cardId)) {
                    throw IllegalStateException("Divergence: Play-on card '${event.cardId}' not in player hand at step #$currentEventIndex")
                }
                val targetIndex = tableMelds.indexOfFirst { it.meldId == event.meldId }
                if (targetIndex >= 0) {
                    val existing = tableMelds[targetIndex]
                    tableMelds[targetIndex] = existing.copy(cardIds = existing.cardIds + event.cardId)
                }
                val cardName = masterCardMap[event.cardId]?.displayName ?: event.cardId
                lastDescription = "$playerName played $cardName on a table meld."
            }
            is ReplayEvent.RummayCall -> {
                val offenderHand = playerHands[event.offenderId]
                if (offenderHand != null && offenderHand.contains(event.transferredCardId)) {
                    offenderHand.remove(event.transferredCardId)
                    playerHands[event.callerId]?.add(event.transferredCardId)
                }
                val callerName = players.find { it.playerId == event.callerId }?.displayName ?: "Player"
                val offenderName = players.find { it.playerId == event.offenderId }?.displayName ?: "Opponent"
                lastDescription = "$callerName called RUMMAY on $offenderName!"
            }
            is ReplayEvent.StockRecycle -> {
                stock.clear()
                stock.addAll(event.newStockOrder)
                lastDescription = "Stock exhausted. Discards shuffled into new Stock."
            }
            is ReplayEvent.RoundEnd -> {
                roundWinnerId = event.winnerId
                lastDescription = "$playerName WENT OUT! Round Complete."
            }
        }

        currentEventIndex++
        return true
    }

    /**
     * Steps backward to previous event by replaying from initial deal to currentEventIndex - 1.
     */
    fun stepBackward(): Boolean {
        if (currentEventIndex <= 0) return false
        val targetIndex = currentEventIndex - 1
        resetToInitialDeal()
        while (currentEventIndex < targetIndex) {
            stepForward()
        }
        return true
    }

    /**
     * Builds the public state for table-only visualization.
     */
    fun getPublicState(): PublicReplayState {
        val seats = players.map { player ->
            val count = playerHands[player.playerId]?.size ?: 0
            val playerMelds = tableMelds.filter { it.ownerId == player.playerId }
            PublicSeat(
                playerId = player.playerId,
                displayName = player.displayName,
                seatIndex = player.seatIndex,
                isHuman = player.isHuman,
                cardCount = count,
                isDown = playerMelds.isNotEmpty(),
                laidMelds = playerMelds
            )
        }

        val topDiscardCard = discardPile.lastOrNull()?.let { masterCardMap[it] }

        return PublicReplayState(
            currentStepIndex = currentEventIndex,
            totalSteps = cartridge.actionLog.size,
            levelNumber = cartridge.levelNumber,
            stockCount = stock.size,
            topDiscard = topDiscardCard,
            discardPileCount = discardPile.size,
            seats = seats,
            tableMelds = tableMelds.toList(),
            lastActionDescription = lastDescription,
            isRoundComplete = roundWinnerId != null || currentEventIndex >= cartridge.actionLog.size,
            winnerId = roundWinnerId
        )
    }
}
