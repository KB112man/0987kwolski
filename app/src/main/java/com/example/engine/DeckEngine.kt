package com.example.engine

import com.example.model.Card
import com.example.model.ContractLevel
import com.example.model.PendingBuyPriority
import com.example.model.Player
import com.example.model.Rank
import com.example.model.Suit
import com.example.model.Meld
import com.example.model.MeldType
import com.example.model.RACK_SLOTS_PER_ROW
import com.example.model.TurnTransition
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

    fun startNewGame(humanName: String = "You"): com.example.model.GameState {
        val opponentNames = NameGenerator.generateUniqueNames(2)
        val initialPlayers = listOf(
            Player(id = "player_human", name = humanName, isHuman = true, avatarIndex = 0),
            Player(id = "player_top", name = opponentNames[0], isHuman = false, avatarIndex = 1, personality = "Strategic"),
            Player(id = "player_left", name = opponentNames[1], isHuman = false, avatarIndex = 2, personality = "Aggressive")
        )
        val level1Contract = ContractLevel.fromLevelNumber(1)
        val deal = dealNewRound(initialPlayers, level1Contract)

        return com.example.model.GameState(
            currentLevel = 1,
            dealerIndex = 0,
            currentTurnPlayerIndex = 0,
            currentPhase = com.example.model.TurnPhase.DRAW,
            drawDeck = deal.drawDeck,
            discardPile = deal.discardPile,
            players = deal.players,
            allTableMelds = emptyList(),
            statusMessage = "Level 1: 2 Books. Your turn — Draw from Stock or Take Discard."
        )
    }

    fun startNextLevel(state: com.example.model.GameState, nextLevel: Int): com.example.model.GameState {
        val nextContract = ContractLevel.fromLevelNumber(nextLevel)
        val resetPlayers = state.players.map { player ->
            player.copy(
                laidMelds = emptyList(),
                initialDownMelds = emptyList(),
                isDown = false
            )
        }
        val deal = dealNewRound(resetPlayers, nextContract)
        val nextDealer = (state.dealerIndex + 1) % resetPlayers.size
        val nextTurnPlayer = (nextDealer + 1) % resetPlayers.size

        return state.copy(
            currentLevel = nextLevel,
            dealerIndex = nextDealer,
            currentTurnPlayerIndex = nextTurnPlayer,
            currentPhase = com.example.model.TurnPhase.DRAW,
            drawDeck = deal.drawDeck,
            discardPile = deal.discardPile,
            players = deal.players,
            allTableMelds = emptyList(),
            isRoundOver = false,
            pendingBuyPriority = null,
            pendingRummay = null,
            statusMessage = "Level $nextLevel: ${nextContract.shortRequirement}. ${deal.players[nextTurnPlayer].name}'s turn to draw."
        )
    }

    fun drawCard(state: com.example.model.GameState, playerId: String): Pair<com.example.model.GameState, Card?> {
        val player = state.players.find { it.id == playerId } ?: return Pair(state, null)
        val (newDeck, card, newDiscards) = drawCard(state.drawDeck, state.discardPile)
        if (card == null) return Pair(state, null)

        val updatedPlayer = player.withUpdatedHand(player.hand + card)
        val updatedPlayers = state.players.map { if (it.id == playerId) updatedPlayer else it }

        val newState = state.copy(
            drawDeck = newDeck,
            discardPile = newDiscards,
            players = updatedPlayers
        )
        return Pair(newState, card)
    }

    fun takeDiscard(state: com.example.model.GameState, playerId: String): Pair<com.example.model.GameState, Card?> {
        val player = state.players.find { it.id == playerId } ?: return Pair(state, null)
        if (state.discardPile.isEmpty()) return Pair(state, null)

        val topDiscard = state.discardPile.last()
        val remainingDiscards = state.discardPile.dropLast(1)
        val updatedPlayer = player.withUpdatedHand(player.hand + topDiscard)
        val updatedPlayers = state.players.map { if (it.id == playerId) updatedPlayer else it }

        val newState = state.copy(
            discardPile = remainingDiscards,
            players = updatedPlayers
        )
        return Pair(newState, topDiscard)
    }

    fun buyDiscard(state: com.example.model.GameState, buyerId: String): Pair<com.example.model.GameState, Card?> {
        val buyer = state.players.find { it.id == buyerId } ?: return Pair(state, null)
        if (state.discardPile.isEmpty()) return Pair(state, null)

        val boughtDiscard = state.discardPile.last()
        val remainingDiscards = state.discardPile.dropLast(1)

        // Draw 1 penalty card from stock
        val (deckAfterPenalty, penaltyCard, discardsAfterDraw) = drawCard(state.drawDeck, remainingDiscards)
        val newHand = if (penaltyCard != null) {
            buyer.hand + boughtDiscard + penaltyCard
        } else {
            buyer.hand + boughtDiscard
        }

        val updatedBuyer = buyer.withUpdatedHand(newHand)
        val updatedPlayers = state.players.map { if (it.id == buyerId) updatedBuyer else it }

        val newState = state.copy(
            drawDeck = deckAfterPenalty,
            discardPile = discardsAfterDraw,
            players = updatedPlayers,
            pendingBuyPriority = null
        )
        return Pair(newState, boughtDiscard)
    }

    fun discardCard(state: com.example.model.GameState, playerId: String, card: Card): Pair<com.example.model.GameState, Card> {
        val player = state.players.find { it.id == playerId } ?: return Pair(state, card)
        val updatedHand = player.hand.filter { it.id != card.id }
        val updatedPlayer = player.withUpdatedHand(updatedHand)
        val updatedPlayers = state.players.map { if (it.id == playerId) updatedPlayer else it }
        val updatedDiscards = state.discardPile + card

        val newState = state.copy(
            discardPile = updatedDiscards,
            players = updatedPlayers
        )
        return Pair(newState, card)
    }

    fun transferCardBetweenPlayers(
        state: com.example.model.GameState,
        fromId: String,
        toId: String,
        card: Card
    ): com.example.model.GameState {
        val fromPlayer = state.players.find { it.id == fromId } ?: return state
        val toPlayer = state.players.find { it.id == toId } ?: return state

        val updatedFrom = fromPlayer.withUpdatedHand(fromPlayer.hand.filter { it.id != card.id })
        val updatedTo = toPlayer.withUpdatedHand(toPlayer.hand + card)

        val updatedPlayers = state.players.map {
            when (it.id) {
                fromId -> updatedFrom
                toId -> updatedTo
                else -> it
            }
        }
        return state.copy(players = updatedPlayers)
    }

    fun goDown(
        state: com.example.model.GameState,
        playerId: String,
        validatedMelds: List<com.example.model.Meld>
    ): Pair<com.example.model.GameState, Boolean> {
        val player = state.players.find { it.id == playerId } ?: return Pair(state, false)
        val meldedCardIds = validatedMelds.flatMap { it.cards }.map { it.id }.toSet()
        val remainingHand = player.hand.filter { it.id !in meldedCardIds }

        val updatedPlayer = player.copy(
            isDown = true,
            laidMelds = player.laidMelds + validatedMelds,
            initialDownMelds = validatedMelds
        ).withUpdatedHand(remainingHand)

        val updatedPlayers = state.players.map { if (it.id == playerId) updatedPlayer else it }
        val updatedTableMelds = state.allTableMelds + validatedMelds

        val newState = state.copy(
            players = updatedPlayers,
            allTableMelds = updatedTableMelds
        )
        return Pair(newState, true)
    }

    fun playOnMeld(
        state: com.example.model.GameState,
        playerId: String,
        card: Card,
        meldId: String,
        shiftWildToHead: Boolean = false
    ): Pair<com.example.model.GameState, Boolean> {
        val player = state.players.find { it.id == playerId } ?: return Pair(state, false)
        val targetMeld = state.allTableMelds.find { it.id == meldId } ?: return Pair(state, false)

        if (!targetMeld.canAddCard(card)) return Pair(state, false)

        var returnedJoker: Card? = null
        var cardsToAlign = targetMeld.cards + card
        var specificConfig: List<Card>? = null

        if (targetMeld.type == MeldType.RUN && !card.isWild) {
            val missingRanks = targetMeld.getRepresentedRanksForJokers()
            if (missingRanks.contains(card.rank)) {
                val jokerToRemove = targetMeld.cards.first { it.isWild }
                
                val testCardsWithJoker = targetMeld.cards.filter { it.id != jokerToRemove.id } + card + jokerToRemove
                val possibleConfigs = Meld.determineAllValidRunConfigurations(testCardsWithJoker)
                
                if (possibleConfigs.isNotEmpty()) {
                    val headConfig = possibleConfigs.find { it.first().isWild }
                    val tailConfig = possibleConfigs.find { it.last().isWild }
                    
                    if (shiftWildToHead && headConfig != null) {
                        specificConfig = headConfig
                        cardsToAlign = testCardsWithJoker
                    } else if (!shiftWildToHead && tailConfig != null) {
                        specificConfig = tailConfig
                        cardsToAlign = testCardsWithJoker
                    } else {
                        specificConfig = possibleConfigs.first()
                        cardsToAlign = testCardsWithJoker
                    }
                } else {
                    returnedJoker = jokerToRemove
                    cardsToAlign = targetMeld.cards.filter { it.id != jokerToRemove.id } + card
                }
            }
        }

        val alignedCards = if (specificConfig != null) {
            specificConfig
        } else if (targetMeld.type == MeldType.RUN) {
            Meld.determineAllValidRunConfigurations(cardsToAlign).firstOrNull() ?: cardsToAlign
        } else {
            cardsToAlign
        }

        val updatedTargetMeld = targetMeld.copy(cards = alignedCards)
        
        val remainingHand = player.hand.filter { it.id != card.id }.toMutableList()
        if (returnedJoker != null) {
            remainingHand.add(returnedJoker)
        }
        val updatedPlayer = player.withUpdatedHand(remainingHand)

        val updatedTableMelds = state.allTableMelds.map {
            if (it.id == meldId) updatedTargetMeld else it
        }

        val updatedPlayers = state.players.map { p ->
            if (p.id == playerId) {
                updatedPlayer
            } else if (p.id == targetMeld.ownerId) {
                val updatedOwnerMelds = p.laidMelds.map { m ->
                    if (m.id == meldId) updatedTargetMeld else m
                }
                p.copy(laidMelds = updatedOwnerMelds)
            } else {
                p
            }
        }

        val newState = state.copy(
            players = updatedPlayers,
            allTableMelds = updatedTableMelds
        )
        return Pair(newState, true)
    }

    fun moveCardInRack(player: Player, fromRow: Int, fromSlot: Int, toRow: Int, toSlot: Int): Player {
        val currentSlotCard = player.rackRows.getOrNull(fromRow)?.getOrNull(fromSlot) ?: return player
        return player.moveCardInRack(currentSlotCard.id, toRow, toSlot)
    }

    fun sortHand(hand: List<Card>, mode: com.example.model.SortMode): List<Card> {
        return when (mode) {
            com.example.model.SortMode.RANK -> hand.sortedWith(
                compareBy({ it.isWild }, { it.rank.value }, { it.suit.ordinal })
            )
            com.example.model.SortMode.SUIT -> hand.sortedWith(
                compareBy({ it.isWild }, { it.suit.ordinal }, { it.rank.value })
            )
            com.example.model.SortMode.CUSTOM -> hand
        }
    }

    fun repackRackSlots(player: Player): Player {
        return player.reorganizeHandIntoRack()
    }

    fun revealLevel7Win(
        state: com.example.model.GameState,
        playerId: String,
        threeRuns: List<List<Card>>
    ): Pair<com.example.model.GameState, Player> {
        val player = state.players.find { it.id == playerId } ?: return Pair(state, state.players.first())
        val createdMelds = threeRuns.mapIndexed { idx, runCards ->
            Meld(
                id = "meld_${playerId}_l7_run_${idx + 1}_${System.currentTimeMillis()}",
                type = MeldType.RUN,
                cards = runCards,
                ownerId = playerId,
                ownerName = player.name
            )
        }
        val updatedPlayer = player.copy(
            hand = emptyList(),
            pocketCardIds = emptySet(),
            isDown = true,
            laidMelds = player.laidMelds + createdMelds,
            initialDownMelds = createdMelds,
            rackRows = listOf(
                List(RACK_SLOTS_PER_ROW) { null },
                List(RACK_SLOTS_PER_ROW) { null }
            )
        )
        val updatedPlayers = state.players.map { if (it.id == playerId) updatedPlayer else it }
        val updatedTableMelds = state.allTableMelds + createdMelds
        val stateAfterMelds = state.copy(
            players = updatedPlayers,
            allTableMelds = updatedTableMelds
        )
        val stateAfterScoring = calculateEndRoundScores(stateAfterMelds, playerId)
        val finalWinner = stateAfterScoring.players.find { it.id == playerId } ?: updatedPlayer
        return Pair(stateAfterScoring, finalWinner)
    }

    /**
     * Exact 3-player turn order and buy priority calculations:
     * discarderIndex = index of player who discarded
     * nextPlayerIndex = (discarderIndex + 1) % players.size
     * otherPlayerIndex = (discarderIndex + 2) % players.size
     *
     * The discard is reserved as TO YOU for nextPlayer.
     * Only otherPlayer receives the out-of-turn BUY opportunity.
     */
    fun calculateTurnAndBuyPriority(
        players: List<Player>,
        discarderId: String,
        lastDiscarded: Card?
    ): TurnTransition {
        val discarderIndex = players.indexOfFirst { it.id == discarderId }
            .takeIf { it >= 0 } ?: 0
        val numPlayers = players.size
        val nextPlayerIndex = (discarderIndex + 1) % numPlayers
        val previousPlayerIndex = (discarderIndex + numPlayers - 1) % numPlayers

        val nextPlayer = players[nextPlayerIndex]
        val previousPlayer = players[previousPlayerIndex]

        val pendingBuy = if (lastDiscarded != null) {
            PendingBuyPriority(
                discard = lastDiscarded,
                discarderId = players[discarderIndex].id,
                discarderName = players[discarderIndex].name,
                nextTurnPlayerId = nextPlayer.id,
                interestedBuyerIds = listOf(previousPlayer.id)
            )
        } else {
            null
        }

        return TurnTransition(
            discarderIndex = discarderIndex,
            nextPlayerIndex = nextPlayerIndex,
            otherPlayerIndex = previousPlayerIndex,
            nextPlayer = nextPlayer,
            otherPlayer = previousPlayer,
            pendingBuyPriority = pendingBuy
        )
    }

    fun nextTurn(state: com.example.model.GameState): com.example.model.GameState {
        val nextTurnIdx = (state.currentTurnPlayerIndex + 1) % state.players.size
        return state.copy(
            currentTurnPlayerIndex = nextTurnIdx,
            currentPhase = com.example.model.TurnPhase.DRAW,
            statusMessage = "${state.players[nextTurnIdx].name}'s turn — Draw from Stock or Take Discard."
        )
    }

    fun calculateEndRoundScores(state: com.example.model.GameState, winnerId: String): com.example.model.GameState {
        val updatedPlayers = state.players.map { p ->
            val penaltyPoints = if (p.id == winnerId) 0 else p.hand.sumOf { it.points }
            val newScoresPerLevel = p.scoresPerLevel + penaltyPoints
            p.copy(scoresPerLevel = newScoresPerLevel)
        }
        val winnerIndex = updatedPlayers.indexOfFirst { it.id == winnerId }.takeIf { it >= 0 }
        val isFinalRound = state.currentLevel >= 7
        return state.copy(
            players = updatedPlayers,
            isRoundOver = true,
            isTournamentOver = isFinalRound,
            roundWinnerIndex = winnerIndex,
            statusMessage = "${state.players.find { it.id == winnerId }?.name ?: "Someone"} went out and won Level ${state.currentLevel}!"
        )
    }

    fun createMatchHistoryEntry(state: com.example.model.GameState): com.example.model.MatchHistoryEntry {
        val sortedByScore = state.players.sortedBy { it.totalScore }
        val human = state.humanPlayer ?: state.players.first()
        val humanRank = sortedByScore.indexOfFirst { it.id == human.id } + 1
        val winner = sortedByScore.first()

        val standings = sortedByScore.mapIndexed { idx, p ->
            com.example.model.MatchPlayerStanding(
                playerId = p.id,
                name = p.name,
                isHuman = p.isHuman,
                rank = idx + 1,
                totalScore = p.totalScore,
                scoresPerLevel = p.scoresPerLevel
            )
        }

        val finalStandings = sortedByScore.map {
            com.example.model.FinalStandingItem(name = it.name, score = it.totalScore)
        }

        return com.example.model.MatchHistoryEntry(
            humanPlayerName = human.name,
            humanFinalRank = humanRank,
            humanFinalScore = human.totalScore,
            humanAchievement = if (humanRank == 1) "Grand Champion" else "Top Finish (Rank $humanRank)",
            isGrandChampion = humanRank == 1,
            winnerName = winner.name,
            winnerScore = winner.totalScore,
            humanScore = human.totalScore,
            humanWon = humanRank == 1,
            winnerIsHuman = winner.isHuman,
            highestLevelCompleted = state.currentLevel,
            standings = standings,
            finalStandings = finalStandings
        )
    }
}
