package com.example.ui

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.*
import com.example.model.*
import com.example.storage.GamePreferences
import com.example.ui.components.DragDropRegistry
import com.example.ui.components.DragDropTarget
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = GamePreferences(application)

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _selectedCardIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedCardIds: StateFlow<Set<String>> = _selectedCardIds.asStateFlow()

    // Dialog state
    private val _showScoreboard = MutableStateFlow(false)
    val showScoreboard: StateFlow<Boolean> = _showScoreboard.asStateFlow()

    private val _showDeckStats = MutableStateFlow(false)
    val showDeckStats: StateFlow<Boolean> = _showDeckStats.asStateFlow()

    private val _showRules = MutableStateFlow(false)
    val showRules: StateFlow<Boolean> = _showRules.asStateFlow()

    private val _showMeldBuilder = MutableStateFlow(false)
    val showMeldBuilder: StateFlow<Boolean> = _showMeldBuilder.asStateFlow()

    private val _showLayoffDialog = MutableStateFlow<Card?>(null)
    val showLayoffDialog: StateFlow<Card?> = _showLayoffDialog.asStateFlow()

    private val _pendingJokerMove = MutableStateFlow<PendingJokerMove?>(null)
    val pendingJokerMove: StateFlow<PendingJokerMove?> = _pendingJokerMove.asStateFlow()

    private val _showWinningTrophy = MutableStateFlow(false)
    val showWinningTrophy: StateFlow<Boolean> = _showWinningTrophy.asStateFlow()

    private val _showPauseDialog = MutableStateFlow(false)
    val showPauseDialog: StateFlow<Boolean> = _showPauseDialog.asStateFlow()

    private val _showMatchHistory = MutableStateFlow(false)
    val showMatchHistory: StateFlow<Boolean> = _showMatchHistory.asStateFlow()

    private val _matchHistoryList = MutableStateFlow<List<MatchHistoryEntry>>(emptyList())
    val matchHistoryList: StateFlow<List<MatchHistoryEntry>> = _matchHistoryList.asStateFlow()

    private val _hasSavedGame = MutableStateFlow(false)
    val hasSavedGame: StateFlow<Boolean> = _hasSavedGame.asStateFlow()

    private val _isInMainMenu = MutableStateFlow(true)
    val isInMainMenu: StateFlow<Boolean> = _isInMainMenu.asStateFlow()

    // Drag-and-drop state
    val dragRegistry = DragDropRegistry()
    private val _activeDraggedCard = MutableStateFlow<Card?>(null)
    val activeDraggedCard: StateFlow<Card?> = _activeDraggedCard.asStateFlow()

    private val _dragGlobalPosition = MutableStateFlow<Offset?>(null)
    val dragGlobalPosition: StateFlow<Offset?> = _dragGlobalPosition.asStateFlow()

    private val _hoveredTarget = MutableStateFlow<DragDropTarget?>(null)
    val hoveredTarget: StateFlow<DragDropTarget?> = _hoveredTarget.asStateFlow()

    private var aiTurnJob: Job? = null

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        val saved = preferences.loadGameState()
        val history = preferences.loadMatchHistory()
        _matchHistoryList.value = history
        if (saved != null) {
            _hasSavedGame.value = true
        }
    }

    fun startNewTournament() {
        aiTurnJob?.cancel()
        val aiNames = NameGenerator.generateOpponents(3)
        val players = listOf(
            Player(id = "player_human", name = "You", isHuman = true),
            Player(id = "player_ai_1", name = aiNames[0], isHuman = false),
            Player(id = "player_ai_2", name = aiNames[1], isHuman = false),
            Player(id = "player_ai_3", name = aiNames[2], isHuman = false)
        )

        val newState = startLevelInternal(level = 1, players = players, dealerIndex = 0)
        _gameState.value = newState
        _isInMainMenu.value = false
        _hasSavedGame.value = true
        _selectedCardIds.value = emptySet()
        saveCurrentState()
        checkAndTriggerAITurn()
    }

    fun resumeSavedGame() {
        val saved = preferences.loadGameState()
        if (saved != null) {
            _gameState.value = saved
            _isInMainMenu.value = false
            _hasSavedGame.value = true
            _selectedCardIds.value = emptySet()
            checkAndTriggerAITurn()
        }
    }

    fun openPauseDialog() {
        aiTurnJob?.cancel()
        saveCurrentState()
        _showPauseDialog.value = true
    }

    fun resumeFromPause() {
        _showPauseDialog.value = false
        checkAndTriggerAITurn()
    }

    fun saveAndReturnToMenu() {
        _showPauseDialog.value = false
        aiTurnJob?.cancel()
        saveCurrentState()
        _isInMainMenu.value = true
        _hasSavedGame.value = preferences.hasSavedGame()
    }

    fun quitToMainMenu() {
        _showPauseDialog.value = false
        aiTurnJob?.cancel()
        preferences.clearSavedGame()
        _hasSavedGame.value = false
        _isInMainMenu.value = true
    }

    fun returnToMainMenu() {
        aiTurnJob?.cancel()
        saveCurrentState()
        _isInMainMenu.value = true
        _hasSavedGame.value = preferences.hasSavedGame()
    }

    private fun startLevelInternal(level: Int, players: List<Player>, dealerIndex: Int): GameState {
        val contract = ContractLevel.fromLevelNumber(level)
        val resetPlayers = players.map { it.copy(hand = emptyList(), isDown = false, rackRows = emptyList()) }
        val dealResult = DeckEngine.dealNewRound(resetPlayers, contract)

        // Find starting player (left of dealer)
        val startingPlayerIndex = (dealerIndex + 1) % resetPlayers.size

        return GameState(
            currentLevel = level,
            dealerIndex = dealerIndex,
            currentTurnPlayerIndex = startingPlayerIndex,
            currentPhase = TurnPhase.DRAW,
            drawDeck = dealResult.drawDeck,
            discardPile = dealResult.discardPile,
            players = dealResult.players,
            melds = emptyList(),
            allTableMelds = emptyList(),
            gameLogs = listOf("Starting Level $level: ${contract.shortRequirement} (Dealer: ${resetPlayers[dealerIndex].name})")
        )
    }

    private fun saveCurrentState() {
        val current = _gameState.value
        if (current.players.isNotEmpty()) {
            preferences.saveGameState(current)
            _hasSavedGame.value = true
        }
    }

    // UI Click Actions
    fun toggleCardSelection(cardId: String) {
        _selectedCardIds.update { current ->
            if (current.contains(cardId)) current - cardId else current + cardId
        }
    }

    fun clearSelection() {
        _selectedCardIds.value = emptySet()
    }

    fun toggleAutoSort() {
        val currentAuto = _gameState.value.autoSortHand
        val newAuto = !currentAuto
        _gameState.update { it.copy(autoSortHand = newAuto) }
        if (newAuto) {
            sortHand(SortMode.RANK)
        }
    }

    fun sortHand(mode: SortMode) {
        _gameState.update { state ->
            val updatedPlayers = state.players.map { player ->
                if (player.isHuman) {
                    val sortedHand = when (mode) {
                        SortMode.RANK -> player.hand.sortedWith(
                            compareBy<Card> { if (it.isWild) 100 else it.rank.value }
                                .thenBy { it.suit.ordinal }
                        )
                        SortMode.SUIT -> player.hand.sortedWith(
                            compareBy<Card> { if (it.isWild) 100 else it.suit.ordinal }
                                .thenBy { it.rank.value }
                        )
                        SortMode.CUSTOM -> player.hand
                    }
                    player.copy(hand = sortedHand).reorganizeHandIntoRack()
                } else {
                    player
                }
            }
            state.copy(players = updatedPlayers, sortMode = mode)
        }
    }

    // DRAW PHASE ACTIONS
    fun onDrawFromStock() {
        val state = _gameState.value
        val human = state.humanPlayer ?: return
        if (state.currentTurnPlayer.id != human.id || state.currentPhase != TurnPhase.DRAW) return

        val (newDeck, drawnCard, newDiscards) = DeckEngine.drawCard(state.drawDeck, state.discardPile)
        if (drawnCard == null) return

        val newHand = human.hand + drawnCard
        val updatedHuman = if (state.autoSortHand) {
            human.copy(hand = newHand).sortedByRank().reorganizeHandIntoRack()
        } else {
            human.withUpdatedHand(newHand)
        }

        val updatedPlayers = state.players.map { if (it.id == human.id) updatedHuman else it }
        val updatedState = state.copy(
            drawDeck = newDeck,
            discardPile = newDiscards,
            players = updatedPlayers,
            currentPhase = TurnPhase.PLAY_OR_DISCARD,
            pendingToYouOffer = null,
            gameLogs = state.gameLogs + "You drew 1 card from the stock pile."
        )
        _gameState.value = updatedState
        saveCurrentState()
    }

    fun onTakeDiscardToYou() {
        val state = _gameState.value
        val human = state.humanPlayer ?: return
        if (state.currentTurnPlayer.id != human.id || state.currentPhase != TurnPhase.DRAW) return
        if (state.discardPile.isEmpty()) return

        val discard = state.discardPile.last()
        val newDiscards = state.discardPile.dropLast(1)
        val newHand = human.hand + discard
        val updatedHuman = if (state.autoSortHand) {
            human.copy(hand = newHand).sortedByRank().reorganizeHandIntoRack()
        } else {
            human.withUpdatedHand(newHand)
        }

        val updatedPlayers = state.players.map { if (it.id == human.id) updatedHuman else it }
        val updatedState = state.copy(
            discardPile = newDiscards,
            players = updatedPlayers,
            currentPhase = TurnPhase.PLAY_OR_DISCARD,
            pendingToYouOffer = null,
            gameLogs = state.gameLogs + "You took ${discard.displayName} from the discard pile."
        )
        _gameState.value = updatedState
        saveCurrentState()
    }

    // DISCARD ACTION
    fun onDiscardCard(cardId: String? = null) {
        val state = _gameState.value
        val human = state.humanPlayer ?: return
        if (state.currentTurnPlayer.id != human.id || state.currentPhase != TurnPhase.PLAY_OR_DISCARD) return

        val targetCardId = cardId ?: _selectedCardIds.value.firstOrNull() ?: return
        val cardToDiscard = human.hand.find { it.id == targetCardId } ?: return

        val newHand = human.hand.filter { it.id != targetCardId }
        val updatedHuman = human.withUpdatedHand(newHand)
        val updatedPlayers = state.players.map { if (it.id == human.id) updatedHuman else it }
        val newDiscards = state.discardPile + cardToDiscard

        _selectedCardIds.value = emptySet()

        // Check if human went OUT
        if (newHand.isEmpty()) {
            handlePlayerWentOut(human.id, updatedPlayers, newDiscards, state.allTableMelds)
            return
        }

        // Check Rummay possibility on this discard
        val isRummayEligible = state.allTableMelds.any { it.canAddCard(cardToDiscard) }

        if (isRummayEligible) {
            // Trigger Rummay window
            val rummayOffer = PendingRummayCall(
                discardedCard = cardToDiscard,
                offenderId = human.id,
                offenderName = human.name,
                eligibleMelds = state.allTableMelds.filter { it.canAddCard(cardToDiscard) }
            )
            _gameState.value = state.copy(
                players = updatedPlayers,
                discardPile = newDiscards,
                pendingRummay = rummayOffer,
                gameLogs = state.gameLogs + "You discarded ${cardToDiscard.displayName} (Rummay Opportunity!)."
            )
            saveCurrentState()
            evaluateAiRummayCall(rummayOffer)
            return
        }

        // Check Out-of-turn Buy priority from AI opponents
        val buyCandidates = state.players.filter { it.id != human.id }
        val interestedAi = buyCandidates.firstOrNull { ai ->
            AiPlayerEngine.shouldAiBuyDiscard(ai, cardToDiscard, state.contractLevel, state.allTableMelds)
        }

        if (interestedAi != null) {
            val pendingBuy = PendingBuyPriority(
                discard = cardToDiscard,
                discarderId = human.id,
                discarderName = human.name,
                nextTurnPlayerId = state.players[(state.currentTurnPlayerIndex + 1) % state.players.size].id,
                interestedBuyerIds = listOf(interestedAi.id)
            )
            _gameState.value = state.copy(
                players = updatedPlayers,
                discardPile = newDiscards,
                pendingBuyPriority = pendingBuy,
                gameLogs = state.gameLogs + "You discarded ${cardToDiscard.displayName}."
            )
            saveCurrentState()
            executeAiBuy(interestedAi.id, cardToDiscard)
            return
        }

        // Normal turn advancement
        advanceToNextPlayer(updatedPlayers, newDiscards, state.allTableMelds, "You discarded ${cardToDiscard.displayName}.")
    }

    // BUY OUT-OF-TURN ACTION (HUMAN)
    fun onHumanBuyDiscard() {
        val state = _gameState.value
        val human = state.humanPlayer ?: return
        val pendingBuy = state.pendingBuyPriority ?: return
        val discard = pendingBuy.discard

        val (newDeck, penaltyCard, _) = DeckEngine.drawCard(state.drawDeck, state.discardPile)
        val newDiscards = state.discardPile.dropLast(1)
        val newCards = listOfNotNull(discard, penaltyCard)
        val newHand = human.hand + newCards
        val updatedHuman = if (state.autoSortHand) {
            human.copy(hand = newHand).sortedByRank().reorganizeHandIntoRack()
        } else {
            human.withUpdatedHand(newHand)
        }

        val updatedPlayers = state.players.map { if (it.id == human.id) updatedHuman else it }
        val nextTurnIdx = state.players.indexOfFirst { it.id == pendingBuy.nextTurnPlayerId }

        val updatedState = state.copy(
            drawDeck = newDeck,
            discardPile = newDiscards,
            players = updatedPlayers,
            pendingBuyPriority = null,
            currentTurnPlayerIndex = if (nextTurnIdx != -1) nextTurnIdx else (state.currentTurnPlayerIndex + 1) % state.players.size,
            currentPhase = TurnPhase.DRAW,
            gameLogs = state.gameLogs + "You BOUGHT ${discard.displayName} (+1 penalty card from stock)."
        )
        _gameState.value = updatedState
        saveCurrentState()
        checkAndTriggerAITurn()
    }

    fun onHumanPassBuy() {
        val state = _gameState.value
        val pendingBuy = state.pendingBuyPriority ?: return
        val nextTurnIdx = state.players.indexOfFirst { it.id == pendingBuy.nextTurnPlayerId }

        val updatedState = state.copy(
            pendingBuyPriority = null,
            currentTurnPlayerIndex = if (nextTurnIdx != -1) nextTurnIdx else (state.currentTurnPlayerIndex + 1) % state.players.size,
            currentPhase = TurnPhase.DRAW
        )
        _gameState.value = updatedState
        saveCurrentState()
        checkAndTriggerAITurn()
    }

    // GO DOWN (MELD CONFIRMATION)
    fun onConfirmGoDown(groups: List<List<Card>>) {
        val state = _gameState.value
        val human = state.humanPlayer ?: return

        val validatedMelds = MeldDetector.validateContract(
            groups = groups,
            level = state.contractLevel,
            playerId = human.id,
            playerName = human.name
        ) ?: return

        val usedCardIds = groups.flatten().map { it.id }.toSet()
        val remainingHand = human.hand.filter { !usedCardIds.contains(it.id) }
        val updatedHuman = human.withUpdatedHand(remainingHand).copy(isDown = true)

        val updatedPlayers = state.players.map { if (it.id == human.id) updatedHuman else it }
        val updatedAllMelds = state.allTableMelds + validatedMelds

        _showMeldBuilder.value = false
        _selectedCardIds.value = emptySet()

        // Check if level 7 or hand empty (went out)
        if (remainingHand.isEmpty()) {
            handlePlayerWentOut(human.id, updatedPlayers, state.discardPile, updatedAllMelds)
            return
        }

        val updatedState = state.copy(
            players = updatedPlayers,
            allTableMelds = updatedAllMelds,
            gameLogs = state.gameLogs + "You successfully went DOWN with ${state.contractLevel.shortRequirement}!"
        )
        _gameState.value = updatedState
        saveCurrentState()
    }

    // PLAY ON / LAYOFF ACTION
    fun onPlayOnSingleCard(card: Card, targetMeldId: String) {
        val state = _gameState.value
        val human = state.humanPlayer ?: return
        if (!human.isDown) return

        val targetMeld = state.allTableMelds.find { it.id == targetMeldId } ?: return

        // Check if run joker repositioning is required
        val jokerDestinations = MeldDetector.getLegalJokerDestinations(targetMeld, card)
        if (jokerDestinations.size > 1) {
            _pendingJokerMove.value = PendingJokerMove(
                card = card,
                targetMeld = targetMeld,
                options = jokerDestinations
            )
            return
        }

        val updatedMeld = targetMeld.addCard(card) ?: return
        val newHand = human.hand.filter { it.id != card.id }
        val updatedHuman = human.withUpdatedHand(newHand)
        val updatedPlayers = state.players.map { if (it.id == human.id) updatedHuman else it }
        val updatedAllMelds = state.allTableMelds.map { if (it.id == targetMeldId) updatedMeld else it }

        _selectedCardIds.value = emptySet()
        _showLayoffDialog.value = null

        if (newHand.isEmpty()) {
            handlePlayerWentOut(human.id, updatedPlayers, state.discardPile, updatedAllMelds)
            return
        }

        val updatedState = state.copy(
            players = updatedPlayers,
            allTableMelds = updatedAllMelds,
            gameLogs = state.gameLogs + "You played ${card.displayName} on ${targetMeld.ownerName}'s ${if (targetMeld.type == MeldType.BOOK) "Book" else "Run"}."
        )
        _gameState.value = updatedState
        saveCurrentState()
    }

    fun onConfirmJokerMove(newCards: List<Card>) {
        val move = _pendingJokerMove.value ?: return
        val state = _gameState.value
        val human = state.humanPlayer ?: return

        val updatedMeld = move.targetMeld.copy(cards = newCards)
        val newHand = human.hand.filter { it.id != move.card.id }
        val updatedHuman = human.copy(hand = newHand).reorganizeHandIntoRack()
        val updatedPlayers = state.players.map { if (it.id == human.id) updatedHuman else it }
        val updatedAllMelds = state.allTableMelds.map { if (it.id == move.targetMeld.id) updatedMeld else it }

        _pendingJokerMove.value = null
        _selectedCardIds.value = emptySet()
        _showLayoffDialog.value = null

        if (newHand.isEmpty()) {
            handlePlayerWentOut(human.id, updatedPlayers, state.discardPile, updatedAllMelds)
            return
        }

        val updatedState = state.copy(
            players = updatedPlayers,
            allTableMelds = updatedAllMelds,
            gameLogs = state.gameLogs + "You played ${move.card.displayName} and repositioned the Joker."
        )
        _gameState.value = updatedState
        saveCurrentState()
    }

    fun cancelJokerMove() {
        _pendingJokerMove.value = null
    }

    // RUMMAY CALL HANDLING
    fun onHumanCallRummay() {
        val state = _gameState.value
        val human = state.humanPlayer ?: return
        val rummay = state.pendingRummay ?: return
        if (rummay.offenderId == human.id) return

        // Mark human as caller
        _gameState.update {
            it.copy(
                pendingRummay = rummay.copy(callerId = human.id, callerName = human.name)
            )
        }
    }

    fun onHumanGiveRummayCard(cardToGive: Card) {
        val state = _gameState.value
        val human = state.humanPlayer ?: return
        val rummay = state.pendingRummay ?: return
        val offender = state.players.find { it.id == rummay.offenderId } ?: return

        val newHumanHand = human.hand.filter { it.id != cardToGive.id }
        val updatedHuman = human.copy(hand = newHumanHand).reorganizeHandIntoRack()
        val newOffenderHand = offender.hand + cardToGive
        val updatedOffender = offender.copy(hand = newOffenderHand).reorganizeHandIntoRack()

        val updatedPlayers = state.players.map {
            when (it.id) {
                human.id -> updatedHuman
                offender.id -> updatedOffender
                else -> it
            }
        }

        _gameState.value = state.copy(
            players = updatedPlayers,
            pendingRummay = null,
            gameLogs = state.gameLogs + "You called RUMMAY on ${offender.name} and gave them ${cardToGive.displayName}!"
        )
        saveCurrentState()

        // Advance to next turn
        val nextIdx = (state.currentTurnPlayerIndex + 1) % state.players.size
        _gameState.update { it.copy(currentTurnPlayerIndex = nextIdx, currentPhase = TurnPhase.DRAW) }
        checkAndTriggerAITurn()
    }

    fun onHumanPassRummay() {
        val state = _gameState.value
        val rummay = state.pendingRummay ?: return

        // Advance turn
        val nextIdx = (state.currentTurnPlayerIndex + 1) % state.players.size
        _gameState.update {
            it.copy(
                pendingRummay = null,
                currentTurnPlayerIndex = nextIdx,
                currentPhase = TurnPhase.DRAW
            )
        }
        saveCurrentState()
        checkAndTriggerAITurn()
    }

    private fun evaluateAiRummayCall(rummay: PendingRummayCall) {
        viewModelScope.launch {
            delay(1200)
            val state = _gameState.value
            if (state.pendingRummay == null || state.pendingRummay?.callerId != null) return@launch

            val aiEligible = state.players.filter { it.id != rummay.offenderId && !it.isHuman }
            val callingAi = aiEligible.firstOrNull { ai ->
                ai.hand.isNotEmpty()
            }

            if (callingAi != null) {
                val cardToGive = callingAi.hand.maxByOrNull { it.points } ?: callingAi.hand.first()
                val offender = state.players.find { it.id == rummay.offenderId } ?: return@launch

                val newAiHand = callingAi.hand.filter { it.id != cardToGive.id }
                val updatedAi = callingAi.copy(hand = newAiHand).reorganizeHandIntoRack()
                val newOffenderHand = offender.hand + cardToGive
                val updatedOffender = offender.copy(hand = newOffenderHand).reorganizeHandIntoRack()

                val updatedPlayers = state.players.map {
                    when (it.id) {
                        callingAi.id -> updatedAi
                        offender.id -> updatedOffender
                        else -> it
                    }
                }

                _gameState.value = state.copy(
                    players = updatedPlayers,
                    pendingRummay = null,
                    gameLogs = state.gameLogs + "${callingAi.name} CALLED RUMMAY on ${offender.name}!"
                )
                saveCurrentState()

                val nextIdx = (state.currentTurnPlayerIndex + 1) % state.players.size
                _gameState.update { it.copy(currentTurnPlayerIndex = nextIdx, currentPhase = TurnPhase.DRAW) }
                checkAndTriggerAITurn()
            } else {
                _gameState.update { it.copy(pendingRummay = null) }
                val nextIdx = (state.currentTurnPlayerIndex + 1) % state.players.size
                _gameState.update { it.copy(currentTurnPlayerIndex = nextIdx, currentPhase = TurnPhase.DRAW) }
                checkAndTriggerAITurn()
            }
        }
    }

    private fun executeAiBuy(aiId: String, discard: Card) {
        viewModelScope.launch {
            delay(1000)
            val state = _gameState.value
            val ai = state.players.find { it.id == aiId } ?: return@launch
            val pendingBuy = state.pendingBuyPriority ?: return@launch

            val (newDeck, penaltyCard, _) = DeckEngine.drawCard(state.drawDeck, state.discardPile)
            val newDiscards = state.discardPile.dropLast(1)
            val newHand = ai.hand + listOfNotNull(discard, penaltyCard)
            val updatedAi = ai.copy(hand = newHand).reorganizeHandIntoRack()

            val updatedPlayers = state.players.map { if (it.id == ai.id) updatedAi else it }
            val nextTurnIdx = state.players.indexOfFirst { it.id == pendingBuy.nextTurnPlayerId }

            _gameState.value = state.copy(
                drawDeck = newDeck,
                discardPile = newDiscards,
                players = updatedPlayers,
                pendingBuyPriority = null,
                currentTurnPlayerIndex = if (nextTurnIdx != -1) nextTurnIdx else (state.currentTurnPlayerIndex + 1) % state.players.size,
                currentPhase = TurnPhase.DRAW,
                gameLogs = state.gameLogs + "${ai.name} BOUGHT ${discard.displayName} out-of-turn (+1 penalty card)."
            )
            saveCurrentState()
            checkAndTriggerAITurn()
        }
    }

    private fun advanceToNextPlayer(
        updatedPlayers: List<Player>,
        newDiscards: List<Card>,
        allMelds: List<Meld>,
        logMsg: String
    ) {
        val state = _gameState.value
        val nextIdx = (state.currentTurnPlayerIndex + 1) % updatedPlayers.size
        val nextPlayer = updatedPlayers[nextIdx]

        // Check if next player is human and top discard is available
        val topDiscard = newDiscards.lastOrNull()
        val toYouOffer = if (nextPlayer.isHuman && topDiscard != null) topDiscard else null

        val updatedState = state.copy(
            players = updatedPlayers,
            discardPile = newDiscards,
            allTableMelds = allMelds,
            currentTurnPlayerIndex = nextIdx,
            currentPhase = TurnPhase.DRAW,
            pendingToYouOffer = toYouOffer,
            gameLogs = state.gameLogs + logMsg
        )
        _gameState.value = updatedState
        saveCurrentState()
        checkAndTriggerAITurn()
    }

    private fun handlePlayerWentOut(
        winnerId: String,
        currentPlayers: List<Player>,
        currentDiscards: List<Card>,
        allMelds: List<Meld>
    ) {
        val state = _gameState.value
        val winner = currentPlayers.find { it.id == winnerId } ?: currentPlayers.first()

        // Calculate penalty points for each player
        val playersWithScores = currentPlayers.map { player ->
            val pointsInHand = if (player.id == winnerId) 0 else player.hand.sumOf { it.points }
            val updatedScores = player.scoresPerLevel + pointsInHand
            player.copy(scoresPerLevel = updatedScores)
        }

        val completedLevel = state.currentLevel
        val isFinalTournament = completedLevel >= 7

        val updatedState = state.copy(
            players = playersWithScores,
            discardPile = currentDiscards,
            allTableMelds = allMelds,
            roundWinnerIndex = playersWithScores.indexOfFirst { it.id == winnerId },
            isRoundOver = true,
            isTournamentOver = isFinalTournament,
            gameLogs = state.gameLogs + "${winner.name} WENT OUT! (0 pts). Level $completedLevel Completed."
        )
        _gameState.value = updatedState

        if (isFinalTournament) {
            recordTournamentHistory(playersWithScores)
            preferences.clearSavedGameState()
            _hasSavedGame.value = false
        } else {
            saveCurrentState()
        }
    }

    private fun recordTournamentHistory(finalPlayers: List<Player>) {
        val sorted = finalPlayers.sortedBy { it.totalScore }
        val winner = sorted.firstOrNull() ?: finalPlayers.first()
        val human = finalPlayers.find { it.isHuman } ?: finalPlayers.first()

        val entry = MatchHistoryEntry(
            winnerName = winner.name,
            winnerScore = winner.totalScore,
            humanScore = human.totalScore,
            humanWon = winner.isHuman,
            finalStandings = sorted.map { FinalStandingItem(name = it.name, score = it.totalScore) }
        )
        preferences.saveMatchHistoryEntry(entry)
        _matchHistoryList.value = preferences.loadMatchHistory()
    }

    fun onNextLevel() {
        val state = _gameState.value
        if (state.currentLevel >= 7) {
            _showWinningTrophy.value = true
            return
        }

        val nextLevel = state.currentLevel + 1
        val nextDealerIndex = (state.dealerIndex + 1) % state.players.size
        val newState = startLevelInternal(nextLevel, state.players, nextDealerIndex)

        _gameState.value = newState
        _selectedCardIds.value = emptySet()
        saveCurrentState()
        checkAndTriggerAITurn()
    }

    // AI TURN ORCHESTRATION
    private fun checkAndTriggerAITurn() {
        val state = _gameState.value
        if (state.isRoundOver || state.isTournamentOver) return
        val current = state.currentTurnPlayer
        if (current.isHuman) return

        aiTurnJob?.cancel()
        aiTurnJob = viewModelScope.launch {
            delay(800)
            executeAiTurn(current.id)
        }
    }

    private suspend fun executeAiTurn(aiId: String) {
        val state = _gameState.value
        val ai = state.players.find { it.id == aiId } ?: return
        if (ai.isHuman || state.isRoundOver) return

        // 1. DRAW PHASE
        val shouldTakeDiscard = state.discardPile.isNotEmpty() &&
                AiPlayerEngine.shouldAiTakeDiscard(ai, state.discardPile.last(), state.contractLevel, state.allTableMelds)

        val afterDrawState: GameState
        val drawnCard: Card
        if (shouldTakeDiscard) {
            val discard = state.discardPile.last()
            val newDiscards = state.discardPile.dropLast(1)
            val newHand = ai.hand + discard
            val updatedAi = ai.copy(hand = newHand).reorganizeHandIntoRack()
            val updatedPlayers = state.players.map { if (it.id == ai.id) updatedAi else it }
            afterDrawState = state.copy(
                discardPile = newDiscards,
                players = updatedPlayers,
                currentPhase = TurnPhase.PLAY_OR_DISCARD,
                gameLogs = state.gameLogs + "${ai.name} took ${discard.displayName} from discard."
            )
            drawnCard = discard
        } else {
            val (newDeck, card, newDiscards) = DeckEngine.drawCard(state.drawDeck, state.discardPile)
            if (card == null) return
            val newHand = ai.hand + card
            val updatedAi = ai.copy(hand = newHand).reorganizeHandIntoRack()
            val updatedPlayers = state.players.map { if (it.id == ai.id) updatedAi else it }
            afterDrawState = state.copy(
                drawDeck = newDeck,
                discardPile = newDiscards,
                players = updatedPlayers,
                currentPhase = TurnPhase.PLAY_OR_DISCARD,
                gameLogs = state.gameLogs + "${ai.name} drew 1 card from stock."
            )
            drawnCard = card
        }

        _gameState.value = afterDrawState
        saveCurrentState()
        delay(900)

        // 2. PLAY PHASE (Check Go Down)
        var currentAi = afterDrawState.players.find { it.id == aiId } ?: return
        var currentAllMelds = afterDrawState.allTableMelds
        var currentPlayers = afterDrawState.players

        if (!currentAi.isDown) {
            val possibleMelds = MeldDetector.findValidContract(
                currentAi.hand,
                afterDrawState.contractLevel,
                currentAi.id,
                currentAi.name
            )
            if (possibleMelds != null) {
                val usedCardIds = possibleMelds.flatMap { it.cards }.map { it.id }.toSet()
                val remainingHand = currentAi.hand.filter { !usedCardIds.contains(it.id) }
                currentAi = currentAi.copy(hand = remainingHand, isDown = true).reorganizeHandIntoRack()
                currentAllMelds = currentAllMelds + possibleMelds
                currentPlayers = currentPlayers.map { if (it.id == ai.id) currentAi else it }

                _gameState.value = afterDrawState.copy(
                    players = currentPlayers,
                    allTableMelds = currentAllMelds,
                    gameLogs = afterDrawState.gameLogs + "${ai.name} went DOWN with ${afterDrawState.contractLevel.shortRequirement}!"
                )
                saveCurrentState()
                delay(800)

                if (remainingHand.isEmpty()) {
                    handlePlayerWentOut(ai.id, currentPlayers, afterDrawState.discardPile, currentAllMelds)
                    return
                }
            }
        }

        // 3. PLAY ON / LAYOFFS (If AI is down)
        if (currentAi.isDown && currentAi.hand.isNotEmpty()) {
            val playableMoves = AiPlayerEngine.findAiPlayOnMoves(currentAi, currentAllMelds)
            for (move in playableMoves) {
                if (currentAi.hand.none { it.id == move.card.id }) continue
                val targetMeld = currentAllMelds.find { it.id == move.targetMeldId } ?: continue
                val updatedMeld = targetMeld.addCard(move.card) ?: continue
                val newHand = currentAi.hand.filter { it.id != move.card.id }
                currentAi = currentAi.copy(hand = newHand).reorganizeHandIntoRack()
                currentAllMelds = currentAllMelds.map { if (it.id == move.targetMeldId) updatedMeld else it }
                currentPlayers = currentPlayers.map { if (it.id == ai.id) currentAi else it }

                _gameState.value = _gameState.value.copy(
                    players = currentPlayers,
                    allTableMelds = currentAllMelds,
                    gameLogs = _gameState.value.gameLogs + "${ai.name} played ${move.card.displayName} on a meld."
                )
                saveCurrentState()
                delay(600)

                if (newHand.isEmpty()) {
                    handlePlayerWentOut(ai.id, currentPlayers, afterDrawState.discardPile, currentAllMelds)
                    return
                }
            }
        }

        // 4. DISCARD PHASE
        val discardCard = AiPlayerEngine.selectAiDiscard(currentAi, afterDrawState.contractLevel, currentAllMelds)
        val finalHand = currentAi.hand.filter { it.id != discardCard.id }
        currentAi = currentAi.copy(hand = finalHand).reorganizeHandIntoRack()
        currentPlayers = currentPlayers.map { if (it.id == ai.id) currentAi else it }
        val newDiscards = afterDrawState.discardPile + discardCard

        if (finalHand.isEmpty()) {
            handlePlayerWentOut(ai.id, currentPlayers, newDiscards, currentAllMelds)
            return
        }

        // Check if Rummay eligible
        val isRummay = currentAllMelds.any { it.canAddCard(discardCard) }
        if (isRummay) {
            val rummayOffer = PendingRummayCall(
                discardedCard = discardCard,
                offenderId = ai.id,
                offenderName = ai.name,
                eligibleMelds = currentAllMelds.filter { it.canAddCard(discardCard) }
            )
            _gameState.value = _gameState.value.copy(
                players = currentPlayers,
                discardPile = newDiscards,
                pendingRummay = rummayOffer,
                gameLogs = _gameState.value.gameLogs + "${ai.name} discarded ${discardCard.displayName} (Rummay Opportunity!)."
            )
            saveCurrentState()
            evaluateAiRummayCall(rummayOffer)
            return
        }

        // Check if Human can buy out-of-turn
        val human = currentPlayers.find { it.isHuman }
        val nextTurnPlayer = currentPlayers[(afterDrawState.currentTurnPlayerIndex + 1) % currentPlayers.size]
        if (human != null && nextTurnPlayer.id != human.id) {
            val pendingBuy = PendingBuyPriority(
                discard = discardCard,
                discarderId = ai.id,
                discarderName = ai.name,
                nextTurnPlayerId = nextTurnPlayer.id,
                interestedBuyerIds = emptyList()
            )
            _gameState.value = _gameState.value.copy(
                players = currentPlayers,
                discardPile = newDiscards,
                pendingBuyPriority = pendingBuy,
                gameLogs = _gameState.value.gameLogs + "${ai.name} discarded ${discardCard.displayName}."
            )
            saveCurrentState()
            return
        }

        // Turn advance
        advanceToNextPlayer(currentPlayers, newDiscards, currentAllMelds, "${ai.name} discarded ${discardCard.displayName}.")
    }

    // Drag and Drop Handlers
    fun onCardDragStart(card: Card, rowIndex: Int, slotIndex: Int, initialGlobalPos: Offset) {
        _activeDraggedCard.value = card
        _dragGlobalPosition.value = initialGlobalPos
    }

    fun onCardDragMove(delta: Offset) {
        val current = _dragGlobalPosition.value ?: return
        val newPos = current + delta
        _dragGlobalPosition.value = newPos
        _hoveredTarget.value = dragRegistry.findTarget(newPos)
    }

    fun onCardDragEnd() {
        val card = _activeDraggedCard.value
        val target = _hoveredTarget.value
        val state = _gameState.value
        val human = state.humanPlayer

        if (card != null && target != null && human != null) {
            when (target) {
                is DragDropTarget.DiscardPile -> {
                    if (state.currentTurnPlayer.id == human.id && state.currentPhase == TurnPhase.PLAY_OR_DISCARD) {
                        onDiscardCard(card.id)
                    }
                }
                is DragDropTarget.Meld -> {
                    if (human.isDown) {
                        onPlayOnSingleCard(card, target.meldId)
                    }
                }
                is DragDropTarget.TableMeld -> {
                    if (human.isDown) {
                        onPlayOnSingleCard(card, target.meldId)
                    }
                }
                is DragDropTarget.RackSlot -> {
                    reorderCardInRack(card, target.rowIndex, target.slotIndex)
                }
                is DragDropTarget.MeldWorkspaceSlot -> {
                    // Handled within MeldBuilderDialog
                }
                is DragDropTarget.StockPile -> { /* Not a drop target */ }
            }
        }

        _activeDraggedCard.value = null
        _dragGlobalPosition.value = null
        _hoveredTarget.value = null
    }

    fun onCardDragCancel() {
        _activeDraggedCard.value = null
        _dragGlobalPosition.value = null
        _hoveredTarget.value = null
    }

    private fun reorderCardInRack(card: Card, targetRow: Int, targetSlot: Int) {
        _gameState.update { state ->
            val human = state.humanPlayer ?: return@update state
            val updatedHuman = human.moveCardInRack(card.id, targetRow, targetSlot)
            val updatedPlayers = state.players.map { if (it.id == human.id) updatedHuman else it }
            state.copy(players = updatedPlayers, sortMode = SortMode.CUSTOM)
        }
    }

    // Modal Toggles
    fun openScoreboard() { _showScoreboard.value = true }
    fun closeScoreboard() { _showScoreboard.value = false }
    fun openDeckStats() { _showDeckStats.value = true }
    fun closeDeckStats() { _showDeckStats.value = false }
    fun openRules() { _showRules.value = true }
    fun closeRules() { _showRules.value = false }
    fun openMeldBuilder() { _showMeldBuilder.value = true }
    fun closeMeldBuilder() { _showMeldBuilder.value = false }
    fun openLayoffDialog(card: Card) { _showLayoffDialog.value = card }
    fun closeLayoffDialog() { _showLayoffDialog.value = null }
    fun openMatchHistory() { _showMatchHistory.value = true }
    fun closeMatchHistory() { _showMatchHistory.value = false }
    fun closeWinningTrophy() { _showWinningTrophy.value = false }
}
