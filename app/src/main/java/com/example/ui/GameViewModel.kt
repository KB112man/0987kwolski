package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.AiPlayerEngine
import com.example.engine.DeckEngine
import com.example.engine.MeldDetector
import com.example.model.*
import com.example.storage.GamePreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = GamePreferences(application)

    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    private val _savedMatchState = MutableStateFlow<GameState?>(null)
    val savedMatchState: StateFlow<GameState?> = _savedMatchState.asStateFlow()

    private val _matchHistory = MutableStateFlow<List<MatchHistoryEntry>>(emptyList())
    val matchHistory: StateFlow<List<MatchHistoryEntry>> = _matchHistory.asStateFlow()

    private val _selectedCardIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedCardIds: StateFlow<Set<String>> = _selectedCardIds.asStateFlow()

    private val _newlyReceivedCardIds = MutableStateFlow<Set<String>>(emptySet())
    val newlyReceivedCardIds: StateFlow<Set<String>> = _newlyReceivedCardIds.asStateFlow()

    // Dialog & UI flows
    private val _showPauseDialog = MutableStateFlow(false)
    val showPauseDialog: StateFlow<Boolean> = _showPauseDialog.asStateFlow()

    private val _showScoreboard = MutableStateFlow(false)
    val showScoreboard: StateFlow<Boolean> = _showScoreboard.asStateFlow()

    private val _showDeckStats = MutableStateFlow(false)
    val showDeckStats: StateFlow<Boolean> = _showDeckStats.asStateFlow()

    private val _showRules = MutableStateFlow(false)
    val showRules: StateFlow<Boolean> = _showRules.asStateFlow()

    private val _showHistoryDialog = MutableStateFlow(false)
    val showHistoryDialog: StateFlow<Boolean> = _showHistoryDialog.asStateFlow()

    private val _showExpandedHandDialog = MutableStateFlow(false)
    val showExpandedHandDialog: StateFlow<Boolean> = _showExpandedHandDialog.asStateFlow()

    private val _showMeldBuilder = MutableStateFlow(false)
    val showMeldBuilder: StateFlow<Boolean> = _showMeldBuilder.asStateFlow()

    private val _showPocketDialog = MutableStateFlow(false)
    val showPocketDialog: StateFlow<Boolean> = _showPocketDialog.asStateFlow()

    private val _showLevel7Reveal = MutableStateFlow(false)
    val showLevel7Reveal: StateFlow<Boolean> = _showLevel7Reveal.asStateFlow()

    private val _isLayoffDialogOpen = MutableStateFlow(false)
    val isLayoffDialogOpen: StateFlow<Boolean> = _isLayoffDialogOpen.asStateFlow()

    private val _pendingJokerReplacement = MutableStateFlow<JokerReplacementState?>(null)
    val pendingJokerReplacement: StateFlow<JokerReplacementState?> = _pendingJokerReplacement.asStateFlow()

    private val _activeRummayCall = MutableStateFlow<RummayCallState?>(null)
    val activeRummayCall: StateFlow<RummayCallState?> = _activeRummayCall.asStateFlow()

    private val _roundWinner = MutableStateFlow<Player?>(null)
    val roundWinner: StateFlow<Player?> = _roundWinner.asStateFlow()

    private val _isTournamentFinished = MutableStateFlow(false)
    val isTournamentFinished: StateFlow<Boolean> = _isTournamentFinished.asStateFlow()

    private val _autoSortEnabled = MutableStateFlow(false)
    val autoSortEnabled: StateFlow<Boolean> = _autoSortEnabled.asStateFlow()

    private var aiLoopJob: Job? = null

    init {
        loadPersistedData()
    }

    private fun loadPersistedData() {
        val saved = preferences.loadActiveGame()
        _savedMatchState.value = saved
        _matchHistory.value = preferences.loadMatchHistory()
    }

    fun startNewTournament(humanName: String = "You") {
        aiLoopJob?.cancel()
        val newState = DeckEngine.startNewGame(humanName = humanName)
        _gameState.value = newState
        _selectedCardIds.value = emptySet()
        _roundWinner.value = null
        _isTournamentFinished.value = false
        preferences.saveActiveGame(newState)
        _savedMatchState.value = newState

        checkTurnState()
    }

    fun resumeSavedTournament() {
        val saved = preferences.loadActiveGame()
        if (saved != null) {
            aiLoopJob?.cancel()
            _gameState.value = saved
            _selectedCardIds.value = emptySet()
            _roundWinner.value = null
            _isTournamentFinished.value = false
            checkTurnState()
        }
    }

    fun toggleAutoSort() {
        _autoSortEnabled.value = !_autoSortEnabled.value
        if (_autoSortEnabled.value) {
            sortHumanHand(SortMode.RANK)
        }
    }

    fun sortHumanHand(mode: SortMode) {
        val state = _gameState.value ?: return
        val human = state.humanPlayer ?: return
        val updatedHand = DeckEngine.sortHand(human.hand, mode)
        val updatedHuman = human.copy(hand = updatedHand)
        val finalHuman = DeckEngine.repackRackSlots(updatedHuman)

        val updatedPlayers = state.players.map { if (it.id == human.id) finalHuman else it }
        val newState = state.copy(players = updatedPlayers, sortMode = mode)
        _gameState.value = newState
        persistState(newState)
    }

    fun toggleCardSelection(cardId: String) {
        val current = _selectedCardIds.value.toMutableSet()
        if (current.contains(cardId)) {
            current.remove(cardId)
        } else {
            current.add(cardId)
        }
        _selectedCardIds.value = current
        _newlyReceivedCardIds.value = emptySet()
    }

    fun clearSelection() {
        _selectedCardIds.value = emptySet()
        _newlyReceivedCardIds.value = emptySet()
    }

    // DRAW ACTIONS
    fun onHumanDrawFromStock() {
        val state = _gameState.value ?: return
        if (!state.isHumanTurn || state.currentPhase != TurnPhase.DRAW) return

        val (updatedState, drawnCard) = DeckEngine.drawCard(state, state.humanPlayer?.id ?: return)
        if (drawnCard != null) {
            val stateAfterDraw = updatedState.copy(
                currentPhase = TurnPhase.PLAY_OR_DISCARD,
                statusMessage = "You drew ${drawnCard.displayName} from Stock. Meld, Play On, or Discard."
            )
            _newlyReceivedCardIds.value = setOf(drawnCard.id)
            _gameState.value = stateAfterDraw
            persistState(stateAfterDraw)
        }
    }

    fun onHumanTakeDiscard() {
        val state = _gameState.value ?: return
        if (!state.isHumanTurn) return

        val (updatedState, takenCard) = DeckEngine.takeDiscard(state, state.humanPlayer?.id ?: return)
        if (takenCard != null) {
            val stateAfterTake = updatedState.copy(
                currentPhase = TurnPhase.PLAY_OR_DISCARD,
                statusMessage = "You took ${takenCard.displayName} from Discard. Meld, Play On, or Discard."
            )
            _newlyReceivedCardIds.value = setOf(takenCard.id)
            _gameState.value = stateAfterTake
            persistState(stateAfterTake)
        }
    }

    // BUY DISCARD
    fun onHumanBuyDiscard() {
        val state = _gameState.value ?: return
        val currentBuyState = state.pendingBuyPriority ?: return
        val humanId = state.humanPlayer?.id ?: return
        if (!currentBuyState.eligibleContenderIds.contains(humanId)) return

        val oldHand = state.humanPlayer?.hand?.map { it.id }?.toSet() ?: emptySet()
        val (updatedState, boughtCard) = DeckEngine.buyDiscard(state, humanId)
        if (boughtCard != null) {
            val nextTurnPlayer = updatedState.players.find { it.id == currentBuyState.nextTurnPlayerId }
                ?: updatedState.currentPlayer
            val finalState = updatedState.copy(
                pendingBuyPriority = null,
                statusMessage = if (nextTurnPlayer.isHuman) {
                    "You bought ${boughtCard.displayName} (+1 draw penalty from Stock). Your turn."
                } else {
                    "You bought ${boughtCard.displayName} (+1 draw penalty from Stock). ${nextTurnPlayer.name}'s turn."
                }
            )
            val newHand = finalState.humanPlayer?.hand?.map { it.id }?.toSet() ?: emptySet()
            _newlyReceivedCardIds.value = newHand.subtract(oldHand)
            _gameState.value = finalState
            persistState(finalState)
            checkTurnState()
        }
    }

    fun onHumanPassBuy() {
        val state = _gameState.value ?: return
        val currentBuyState = state.pendingBuyPriority ?: return
        val humanId = state.humanPlayer?.id ?: return
        if (!currentBuyState.eligibleContenderIds.contains(humanId)) return

        val nextTurnPlayer = state.players.find { it.id == currentBuyState.nextTurnPlayerId }
            ?: state.currentPlayer
        val afterPass = state.copy(
            pendingBuyPriority = null,
            statusMessage = if (nextTurnPlayer.isHuman) {
                "Your turn — Draw from Stock or Take Discard."
            } else {
                "${nextTurnPlayer.name}'s turn — Draw from Stock or Take Discard."
            }
        )
        _gameState.value = afterPass
        persistState(afterPass)
        checkTurnState()
    }

    private fun processBuyPrioritySequence() {
        val state = _gameState.value ?: return
        val currentBuyState = state.pendingBuyPriority ?: return

        val candidateBuyerId = currentBuyState.eligibleContenderIds.firstOrNull()
        if (candidateBuyerId == null) {
            val nextTurnPlayer = state.players.find { it.id == currentBuyState.nextTurnPlayerId } ?: state.currentPlayer
            val afterPass = state.copy(
                pendingBuyPriority = null,
                statusMessage = if (nextTurnPlayer.isHuman) {
                    "Your turn — Draw from Stock or Take Discard."
                } else {
                    "${nextTurnPlayer.name}'s turn — Draw from Stock or Take Discard."
                }
            )
            _gameState.value = afterPass
            persistState(afterPass)
            checkTurnState()
            return
        }

        val candidateBuyer = state.players.find { it.id == candidateBuyerId } ?: return

        if (candidateBuyer.isHuman) {
            // WAIT INDEFINITELY for human to press Buy or Pass.
            val waitState = state.copy(statusMessage = "BUY PRIORITY — Your decision")
            _gameState.value = waitState
            return
        }

        // It is an AI's turn to decide on the Buy
        val waitAiState = state.copy(statusMessage = "Checking if ${candidateBuyer.name} wants to Buy...")
        _gameState.value = waitAiState

        viewModelScope.launch {
            delay(1000)
            val currentState = _gameState.value ?: return@launch
            val currentPriority = currentState.pendingBuyPriority ?: return@launch
            if (currentPriority.eligibleContenderIds.firstOrNull() != candidateBuyerId) return@launch

            val wantsBuy = AiPlayerEngine.shouldAiBuyDiscard(
                candidateBuyer,
                currentPriority.discardCard,
                currentState.contractLevel,
                currentState.allTableMelds
            )
            val nextTurnPlayer = currentState.players.find { it.id == currentPriority.nextTurnPlayerId }
                ?: currentState.currentPlayer

            if (wantsBuy) {
                val (boughtState, _) = DeckEngine.buyDiscard(currentState, candidateBuyerId)
                val afterBuy = boughtState.copy(
                    pendingBuyPriority = null,
                    statusMessage = if (nextTurnPlayer.isHuman) {
                        "${candidateBuyer.name} bought ${currentPriority.discardCard.displayName}. Your turn — Draw from Stock."
                    } else {
                        "${candidateBuyer.name} bought ${currentPriority.discardCard.displayName}. ${nextTurnPlayer.name}'s turn."
                    }
                )
                _gameState.value = afterBuy
                persistState(afterBuy)
                checkTurnState()
            } else {
                val afterPass = currentState.copy(
                    pendingBuyPriority = null,
                    statusMessage = if (nextTurnPlayer.isHuman) {
                        "${candidateBuyer.name} passed. Your turn — Draw from Stock or Take Discard."
                    } else {
                        "${candidateBuyer.name} passed. ${nextTurnPlayer.name}'s turn."
                    }
                )
                _gameState.value = afterPass
                persistState(afterPass)
                checkTurnState()
            }
        }
    }

    // DISCARD ACTION
    fun onHumanDiscardSelectedCard() {
        val state = _gameState.value ?: return
        if (!state.isHumanTurn || state.currentPhase != TurnPhase.PLAY_OR_DISCARD) {
            _gameState.value = state.copy(statusMessage = "You can only discard after drawing during your turn!")
            return
        }

        val selectedCardId = _selectedCardIds.value.firstOrNull()
        if (selectedCardId == null) {
            _gameState.value = state.copy(statusMessage = "Select 1 card from your rack to discard.")
            return
        }
        val human = state.humanPlayer ?: return
        if (state.contractLevel.noDiscard && human.hand.size == 1) {
            _gameState.value = state.copy(statusMessage = "NO DISCARD: You must play all cards to win. You cannot discard your last card!")
            return
        }
        
        val card = human.hand.find { it.id == selectedCardId } ?: return

        executeDiscard(state, human, card)
    }

    fun onHumanDiscardCard(card: Card) {
        val state = _gameState.value ?: return
        if (!state.isHumanTurn || state.currentPhase != TurnPhase.PLAY_OR_DISCARD) return

        val human = state.humanPlayer ?: return
        if (state.contractLevel.noDiscard && human.hand.size == 1) {
            _gameState.value = state.copy(statusMessage = "NO DISCARD: You must play all cards to win. You cannot discard your last card!")
            return
        }
        
        executeDiscard(state, human, card)
    }

    private fun executeDiscard(state: GameState, player: Player, card: Card) {
        _selectedCardIds.value = emptySet()

        // Check for Rummay! (Offending discard)
        val canPlayOnAnyMeld = state.allTableMelds.any { it.canAddCard(card) }
        val (stateAfterDiscard, _) = DeckEngine.discardCard(state, player.id, card)

        // Check if player went out
        val updatedPlayer = stateAfterDiscard.players.find { it.id == player.id }
        if (updatedPlayer != null && updatedPlayer.hand.isEmpty()) {
            if (state.contractLevel.noDiscard) {
                _gameState.value = state.copy(statusMessage = "Level 7: You must play all cards to go out. You cannot discard your last card!")
                return
            }
            handleRoundWon(stateAfterDiscard, updatedPlayer)
            return
        }

        if (canPlayOnAnyMeld && stateAfterDiscard.allTableMelds.isNotEmpty()) {
            triggerRummayAlert(stateAfterDiscard, player, card)
            return
        }

        advanceTurn(stateAfterDiscard, card, player)
    }

    private fun triggerRummayAlert(state: GameState, offender: Player, discardedCard: Card) {
        val isHumanOffender = offender.isHuman
        val human = state.humanPlayer

        if (isHumanOffender) {
            val aiCallers = state.players.filter { !it.isHuman && it.id != offender.id && it.hand.isNotEmpty() }
            val aiCaller = aiCallers.firstOrNull()

            if (aiCaller != null) {
                val penaltyCard = aiCaller.hand.random()
                val targetMeld = state.allTableMelds.firstOrNull { it.canAddCard(discardedCard) }

                var stateAfterRummy = state
                if (targetMeld != null) {
                    val updatedDiscards = stateAfterRummy.discardPile.filter { it.id != discardedCard.id }
                    stateAfterRummy = stateAfterRummy.copy(discardPile = updatedDiscards)
                    val (stateAfterPlay, _) = DeckEngine.playOnMeld(stateAfterRummy, offender.id, discardedCard, targetMeld.id)
                    stateAfterRummy = stateAfterPlay
                }

                val callerPlayer = stateAfterRummy.players.find { it.id == aiCaller.id }!!
                val updatedCaller = callerPlayer.withUpdatedHand(callerPlayer.hand.filter { it.id != penaltyCard.id })

                val offenderPlayer = stateAfterRummy.players.find { it.id == offender.id }!!
                val updatedOffender = offenderPlayer.withUpdatedHand(offenderPlayer.hand + penaltyCard)

                stateAfterRummy = stateAfterRummy.copy(
                    players = stateAfterRummy.players.map { p ->
                        when (p.id) {
                            aiCaller.id -> updatedCaller
                            offender.id -> updatedOffender
                            else -> p
                        }
                    }
                )

                _newlyReceivedCardIds.value = setOf(penaltyCard.id)

                val updatedState = stateAfterRummy.copy(
                    statusMessage = "${aiCaller.name} called RUMMAY! and gave ${penaltyCard.displayName} to you."
                )

                _activeRummayCall.value = RummayCallState(
                    discardedCard = discardedCard,
                    offender = offender,
                    caller = aiCaller,
                    isHumanCaller = false,
                    isHumanOffender = true,
                    humanPlayer = human,
                    penaltyCard = penaltyCard,
                    isResolved = true
                )
                _gameState.value = updatedState
                persistState(updatedState)
                // advanceTurn will be called in onHumanDismissRummay
                return

            } else {
                advanceTurn(state, discardedCard, offender)
                return
            }
        }

        _activeRummayCall.value = RummayCallState(
            discardedCard = discardedCard,
            offender = offender,
            caller = null,
            isHumanCaller = false,
            isHumanOffender = isHumanOffender,
            humanPlayer = human,
            isResolved = false
        )
    }

    fun onHumanCallRummay() {
        val currentCall = _activeRummayCall.value ?: return
        val state = _gameState.value ?: return
        val human = state.humanPlayer ?: return

        _activeRummayCall.value = currentCall.copy(
            caller = human,
            isHumanCaller = true
        )
    }

    fun onHumanPassRummay() {
        val currentCall = _activeRummayCall.value ?: return
        val state = _gameState.value ?: return

        // AI might call Rummay
        val aiCallers = state.players.filter { !it.isHuman && it.id != currentCall.offender.id && it.hand.isNotEmpty() }
        val aiCaller = aiCallers.firstOrNull()

        if (aiCaller != null) {
            val penaltyCard = aiCaller.hand.random()
            
            val offendingCard = currentCall.discardedCard
            val targetMeld = state.allTableMelds.firstOrNull { it.canAddCard(offendingCard) }
            
            var stateAfterRummy = state
            if (targetMeld != null) {
                val updatedDiscards = stateAfterRummy.discardPile.filter { it.id != offendingCard.id }
                stateAfterRummy = stateAfterRummy.copy(discardPile = updatedDiscards)
                val (stateAfterPlay, _) = DeckEngine.playOnMeld(stateAfterRummy, currentCall.offender.id, offendingCard, targetMeld.id)
                stateAfterRummy = stateAfterPlay
            }

            val callerPlayer = stateAfterRummy.players.find { it.id == aiCaller.id }!!
            val updatedCaller = callerPlayer.withUpdatedHand(callerPlayer.hand.filter { it.id != penaltyCard.id })

            val offenderPlayer = stateAfterRummy.players.find { it.id == currentCall.offender.id }!!
            val updatedOffender = offenderPlayer.withUpdatedHand(offenderPlayer.hand + penaltyCard)

            stateAfterRummy = stateAfterRummy.copy(
                players = stateAfterRummy.players.map { p ->
                    when (p.id) {
                        aiCaller.id -> updatedCaller
                        currentCall.offender.id -> updatedOffender
                        else -> p
                    }
                }
            )

            val updatedState = stateAfterRummy.copy(
                statusMessage = "${aiCaller.name} called RUMMAY! and gave ${penaltyCard.displayName} to ${currentCall.offender.name}."
            )
            
            // Highlight if Human is the offender
            val human = state.humanPlayer
            if (currentCall.offender.id == human?.id) {
                _newlyReceivedCardIds.value = setOf(penaltyCard.id)
            }
            
            _gameState.value = updatedState
            persistState(updatedState)
            // advanceTurn will be called in onHumanDismissRummay
            
            _activeRummayCall.value = currentCall.copy(
                caller = aiCaller,
                isHumanCaller = false,
                isHumanOffender = false,
                penaltyCard = penaltyCard,
                isResolved = true
            )
        } else {
            _activeRummayCall.value = null
            advanceTurn(state, currentCall.discardedCard, currentCall.offender)
        }
    }

    fun onHumanDismissRummay() {
        val currentCall = _activeRummayCall.value ?: return
        val state = _gameState.value ?: return
        _activeRummayCall.value = null
        advanceTurn(state, null, currentCall.offender)
    }


    fun onHumanGiveCardToRummayOffender(penaltyCard: Card) {
        val currentCall = _activeRummayCall.value ?: return
        val state = _gameState.value ?: return
        val human = state.humanPlayer ?: return

        val offendingCard = currentCall.discardedCard
        val targetMeld = state.allTableMelds.firstOrNull { it.canAddCard(offendingCard) }
        
        var stateAfterRummy = state
        if (targetMeld != null) {
            val updatedDiscards = stateAfterRummy.discardPile.filter { it.id != offendingCard.id }
            stateAfterRummy = stateAfterRummy.copy(discardPile = updatedDiscards)
            val (stateAfterPlay, _) = DeckEngine.playOnMeld(stateAfterRummy, currentCall.offender.id, offendingCard, targetMeld.id)
            stateAfterRummy = stateAfterPlay
        }

        val callerPlayer = stateAfterRummy.players.find { it.id == human.id }!!
        val updatedCaller = callerPlayer.withUpdatedHand(callerPlayer.hand.filter { it.id != penaltyCard.id })

        val offenderPlayer = stateAfterRummy.players.find { it.id == currentCall.offender.id }!!
        val updatedOffender = offenderPlayer.withUpdatedHand(offenderPlayer.hand + penaltyCard)
        
        stateAfterRummy = stateAfterRummy.copy(
            players = stateAfterRummy.players.map { p ->
                when (p.id) {
                    human.id -> updatedCaller
                    currentCall.offender.id -> updatedOffender
                    else -> p
                }
            }
        )

        val updatedState = stateAfterRummy.copy(
            statusMessage = "You called RUMMAY! and gave ${penaltyCard.displayName} to ${currentCall.offender.name}."
        )
        _gameState.value = updatedState
        persistState(updatedState)
        // advanceTurn will be called in onHumanDismissRummay

        _activeRummayCall.value = currentCall.copy(
            caller = human,
            isHumanCaller = true,
            isHumanOffender = false,
            penaltyCard = penaltyCard,
            isResolved = true
        )
    }

    private fun advanceTurn(state: GameState, lastDiscarded: Card?, discarder: Player) {
        val transition = DeckEngine.calculateTurnAndBuyPriority(state.players, discarder.id, lastDiscarded)
        val nextPlayer = transition.nextPlayer
        val otherPlayer = transition.otherPlayer

        if (transition.pendingBuyPriority != null) {
            val status = if (otherPlayer.isHuman) {
                "BUY PRIORITY — Your decision"
            } else {
                "Checking if ${otherPlayer.name} wants to Buy..."
            }

            val stateWithBuy = state.copy(
                currentTurnPlayerIndex = transition.nextPlayerIndex,
                currentPhase = TurnPhase.DRAW,
                pendingBuyPriority = transition.pendingBuyPriority,
                statusMessage = status
            )
            _gameState.value = stateWithBuy
            persistState(stateWithBuy)
            processBuyPrioritySequence()
        } else {
            val stateWithoutBuy = state.copy(
                currentTurnPlayerIndex = transition.nextPlayerIndex,
                currentPhase = TurnPhase.DRAW,
                pendingBuyPriority = null,
                statusMessage = if (nextPlayer.isHuman) {
                    "Your turn — Draw from Stock."
                } else {
                    "${nextPlayer.name}'s turn — Draw from Stock."
                }
            )
            _gameState.value = stateWithoutBuy
            persistState(stateWithoutBuy)
            checkTurnState()
        }
    }

    // GOING DOWN / CONFIRM MELDS
    fun onConfirmGoDown(meldGroups: List<List<Card>>) {
        val state = _gameState.value ?: return
        val human = state.humanPlayer ?: return

        val validatedMelds = MeldDetector.validateContract(
            groups = meldGroups,
            level = state.contractLevel,
            playerId = human.id,
            playerName = human.name
        ) ?: return

        val (stateAfterMeld, _) = DeckEngine.goDown(state, human.id, validatedMelds)
        _showMeldBuilder.value = false
        _selectedCardIds.value = emptySet()

        val updatedHuman = stateAfterMeld.players.find { it.id == human.id }
        if (updatedHuman != null && updatedHuman.hand.isEmpty()) {
            handleRoundWon(stateAfterMeld, updatedHuman)
            return
        }

        val finalState = stateAfterMeld.copy(
            statusMessage = "You Went Down! Now you can Play On to any table meld or Discard."
        )
        _gameState.value = finalState
        persistState(finalState)
    }

    // LEVEL 7 "THAT DID IT!" REVEAL & WIN
    fun canHumanWinLevel7(): Boolean {
        val current = _gameState.value ?: return false
        if (current.currentLevel != 7) return false
        val human = current.humanPlayer ?: return false
        return MeldDetector.findLevel7WinningRuns(human.hand) != null
    }

    fun onConfirmLevel7Win(winningRuns: List<List<Card>>) {
        val current = _gameState.value ?: return
        val human = current.humanPlayer ?: return
        _showLevel7Reveal.value = false
        _selectedCardIds.value = emptySet()

        val (stateAfterWin, winner) = DeckEngine.revealLevel7Win(current, human.id, winningRuns)
        _gameState.value = stateAfterWin
        _roundWinner.value = winner
        if (stateAfterWin.isTournamentOver) {
            _isTournamentFinished.value = true
        }
        persistState(stateAfterWin)
    }

    // POCKET CARD MANAGEMENT (Private hand-organization tray)
    fun moveCardsToPocket(cardIds: Set<String>) {
        val current = _gameState.value ?: return
        val human = current.humanPlayer ?: return
        val updatedHuman = human.withPocketedCards(cardIds)
        val updatedPlayers = current.players.map { if (it.id == human.id) updatedHuman else it }
        val updatedState = current.copy(players = updatedPlayers)
        _gameState.value = updatedState
        persistState(updatedState)
        _selectedCardIds.value = _selectedCardIds.value - cardIds
    }

    fun moveCardsToHand(cardIds: Set<String>) {
        val current = _gameState.value ?: return
        val human = current.humanPlayer ?: return
        val updatedHuman = human.withUnpocketedCards(cardIds)
        val updatedPlayers = current.players.map { if (it.id == human.id) updatedHuman else it }
        val updatedState = current.copy(players = updatedPlayers)
        _gameState.value = updatedState
        persistState(updatedState)
        _selectedCardIds.value = _selectedCardIds.value - cardIds
    }

    fun emptyPocket() {
        val current = _gameState.value ?: return
        val human = current.humanPlayer ?: return
        val updatedHuman = human.withEmptyPocket()
        val updatedPlayers = current.players.map { if (it.id == human.id) updatedHuman else it }
        val updatedState = current.copy(players = updatedPlayers)
        _gameState.value = updatedState
        persistState(updatedState)
    }

    fun onPocketButtonClicked() {
        val current = _gameState.value ?: return
        val human = current.humanPlayer ?: return
        val selected = _selectedCardIds.value
        val activeSelected = selected.filter { it !in human.pocketCardIds }.toSet()
        if (activeSelected.isNotEmpty()) {
            moveCardsToPocket(activeSelected)
        } else {
            _showPocketDialog.value = true
        }
    }

    // PLAY ON (LAYOFF)
    fun onPlayOnSelectedCard() {
        val state = _gameState.value ?: return
        val human = state.humanPlayer ?: return

        if (!state.isHumanTurn || state.currentPhase != TurnPhase.PLAY_OR_DISCARD) {
            _gameState.value = state.copy(statusMessage = "You can only Play On during your play phase!")
            return
        }

        _isLayoffDialogOpen.value = true
    }

    fun onLayoffToMeld(meldId: String, card: Card) {
        val state = _gameState.value ?: return
        val human = state.humanPlayer ?: return

        val targetMeld = state.allTableMelds.find { it.id == meldId } ?: return

        // Check if playing natural card on a run that contains a joker AND replaces it
        if (targetMeld.type == MeldType.RUN && !card.isWild) {
            val missingRanks = targetMeld.getRepresentedRanksForJokers()
            if (missingRanks.contains(card.rank)) {
                val range = targetMeld.getRunRange() ?: Pair(2, 14)
                val headRank = Rank.entries.firstOrNull { it.value == range.first - 1 }?.displayName ?: "Low"
                val tailRank = Rank.entries.firstOrNull { it.value == range.second + 1 }?.displayName ?: "High"
    
                _pendingJokerReplacement.value = JokerReplacementState(
                    naturalCard = card,
                    meld = targetMeld,
                    headRankName = headRank,
                    tailRankName = tailRank
                )
                return
            }
        }

        val oldHand = state.humanPlayer?.hand?.map { it.id }?.toSet() ?: emptySet()
        val (stateAfterPlayOn, success) = DeckEngine.playOnMeld(state, human.id, card, meldId)
        if (success) {
            _selectedCardIds.value = emptySet()
            _isLayoffDialogOpen.value = false

            val updatedHuman = stateAfterPlayOn.players.find { it.id == human.id }
            if (updatedHuman != null) {
                val newHand = updatedHuman.hand.map { it.id }.toSet()
                _newlyReceivedCardIds.value = newHand.subtract(oldHand)
            }

            if (updatedHuman != null && updatedHuman.hand.isEmpty()) {
                handleRoundWon(stateAfterPlayOn, updatedHuman)
                return
            }

            val finalState = stateAfterPlayOn.copy(
                statusMessage = "Played ${card.displayName} on ${targetMeld.ownerName}'s ${targetMeld.type.name}."
            )
            _gameState.value = finalState
            persistState(finalState)
        }
    }

    fun onResolveJokerDestination(shiftToHead: Boolean) {
        val replacementState = _pendingJokerReplacement.value ?: return
        val state = _gameState.value ?: return
        val human = state.humanPlayer ?: return

        val oldHand = state.humanPlayer?.hand?.map { it.id }?.toSet() ?: emptySet()
        val (stateAfterPlayOn, success) = DeckEngine.playOnMeld(
            state,
            human.id,
            replacementState.naturalCard,
            replacementState.meld.id,
            shiftWildToHead = shiftToHead
        )
        _pendingJokerReplacement.value = null
        _isLayoffDialogOpen.value = false
        _selectedCardIds.value = emptySet()

        if (success) {
            val updatedHuman = stateAfterPlayOn.players.find { it.id == human.id }
            if (updatedHuman != null) {
                val newHand = updatedHuman.hand.map { it.id }.toSet()
                _newlyReceivedCardIds.value = newHand.subtract(oldHand)
            }

            if (updatedHuman != null && updatedHuman.hand.isEmpty()) {
                handleRoundWon(stateAfterPlayOn, updatedHuman)
                return
            }
            val finalState = stateAfterPlayOn.copy(
                statusMessage = "Replaced Joker in ${replacementState.meld.ownerName}'s Run with ${replacementState.naturalCard.displayName}."
            )
            _gameState.value = finalState
            persistState(finalState)
        }
    }

    // SWAP RACK SLOTS
    fun onMoveCardInRack(fromRow: Int, fromSlot: Int, toRow: Int, toSlot: Int) {
        val state = _gameState.value ?: return
        val human = state.humanPlayer ?: return
        val updatedHuman = DeckEngine.moveCardInRack(human, fromRow, fromSlot, toRow, toSlot)
        val updatedPlayers = state.players.map { if (it.id == human.id) updatedHuman else it }
        val newState = state.copy(players = updatedPlayers)
        _gameState.value = newState
        persistState(newState)
    }

    // ROUND WON / TRANSITION
    private fun handleRoundWon(state: GameState, winner: Player) {
        val stateAfterScoring = DeckEngine.calculateEndRoundScores(state, winner.id)
        _roundWinner.value = winner
        _gameState.value = stateAfterScoring
        persistState(stateAfterScoring)

        if (stateAfterScoring.currentLevel >= 7) {
            _isTournamentFinished.value = true
            val historyEntry = DeckEngine.createMatchHistoryEntry(stateAfterScoring)
            preferences.saveMatchHistoryEntry(historyEntry)
            preferences.clearActiveGame()
            _matchHistory.value = preferences.loadMatchHistory()
            _savedMatchState.value = null
        }
    }

    fun onContinueNextLevel() {
        val state = _gameState.value ?: return
        if (state.currentLevel >= 7) {
            _roundWinner.value = null
            _isTournamentFinished.value = true
            return
        }

        val nextLevel = state.currentLevel + 1
        val nextRoundState = DeckEngine.startNextLevel(state, nextLevel)
        _roundWinner.value = null
        _gameState.value = nextRoundState
        persistState(nextRoundState)
        checkTurnState()
    }

    // AI TURN EXECUTION LOOP
    private fun checkTurnState() {
        val state = _gameState.value ?: return
        if (_roundWinner.value != null || _isTournamentFinished.value) return
        if (state.pendingBuyPriority != null) return

        if (!state.isHumanTurn) {
            runAiTurn()
        }
    }

    private fun runAiTurn() {
        aiLoopJob?.cancel()
        aiLoopJob = viewModelScope.launch {
            val initialState = _gameState.value ?: return@launch
            if (initialState.isHumanTurn) return@launch
            
            val aiPlayer = initialState.currentPlayer
            _gameState.value = initialState.copy(statusMessage = "${aiPlayer.name} is thinking...")
            delay(800)
            
            var currentState = _gameState.value ?: return@launch
            if (currentState.topDiscard != null) {
                _gameState.value = currentState.copy(statusMessage = "${aiPlayer.name} is checking the discard...")
                delay(600)
                currentState = _gameState.value ?: return@launch
            }

            // 1. DRAW PHASE
            val shouldTakeDiscard = if (currentState.topDiscard != null) {
                AiPlayerEngine.shouldAiTakeDiscard(aiPlayer, currentState.topDiscard!!, currentState.contractLevel, currentState.allTableMelds)
            } else false

            if (shouldTakeDiscard && currentState.topDiscard != null) {
                val (stateAfterTake, card) = DeckEngine.takeDiscard(currentState, aiPlayer.id)
                currentState = stateAfterTake.copy(
                    currentPhase = TurnPhase.PLAY_OR_DISCARD,
                    statusMessage = "${aiPlayer.name} took ${card?.displayName} from Discard."
                )
            } else {
                _gameState.value = currentState.copy(statusMessage = "${aiPlayer.name} is drawing from Stock...")
                delay(600)
                currentState = _gameState.value ?: return@launch
                
                val (stateAfterDraw, _) = DeckEngine.drawCard(currentState, aiPlayer.id)
                currentState = stateAfterDraw.copy(
                    currentPhase = TurnPhase.PLAY_OR_DISCARD,
                    statusMessage = "${aiPlayer.name} drew a card from Stock."
                )
            }
            _gameState.value = currentState
            delay(800)
            currentState = _gameState.value ?: return@launch

            // 2. CHECK GO DOWN (For Level 7, check full-hand 3-run win condition)
            var updatedAi = currentState.players.find { it.id == aiPlayer.id } ?: return@launch
            if (!updatedAi.isDown) {
                if (currentState.currentLevel == 7) {
                    val winningRuns = MeldDetector.findLevel7WinningRuns(updatedAi.hand)
                    if (winningRuns != null) {
                        _gameState.value = currentState.copy(statusMessage = "${updatedAi.name} reveals 3 RUNS — THAT DID IT!")
                        delay(1000)
                        val (stateAfterWin, winner) = DeckEngine.revealLevel7Win(currentState, updatedAi.id, winningRuns)
                        _gameState.value = stateAfterWin
                        _roundWinner.value = winner
                        if (stateAfterWin.isTournamentOver) {
                            _isTournamentFinished.value = true
                        }
                        persistState(stateAfterWin)
                        return@launch
                    }
                } else {
                    _gameState.value = currentState.copy(statusMessage = "${aiPlayer.name} is checking melds...")
                    delay(600)
                    currentState = _gameState.value ?: return@launch

                    val candidateMelds = MeldDetector.findValidContract(
                        updatedAi.hand,
                        currentState.contractLevel,
                        updatedAi.id,
                        updatedAi.name
                    )
                    if (candidateMelds != null) {
                        val (stateAfterDown, _) = DeckEngine.goDown(currentState, updatedAi.id, candidateMelds)
                        currentState = stateAfterDown.copy(
                            statusMessage = "${updatedAi.name} WENT DOWN!"
                        )
                        _gameState.value = currentState
                        updatedAi = currentState.players.find { it.id == aiPlayer.id } ?: return@launch
                        delay(800)
                    }
                }
            }

            // 3. CHECK PLAY ON
            if (updatedAi.isDown) {
                var moves = AiPlayerEngine.findAiPlayOnMoves(updatedAi, currentState.allTableMelds)
                if (currentState.contractLevel.noDiscard && updatedAi.hand.size - moves.size == 1) {
                    moves = moves.dropLast(1)
                }
                
                if (moves.isNotEmpty()) {
                    _gameState.value = currentState.copy(statusMessage = "${updatedAi.name} is playing...")
                    delay(600)
                    currentState = _gameState.value ?: return@launch
                }
                
                for (move in moves) {
                    val (stateAfterLayoff, success) = DeckEngine.playOnMeld(currentState, updatedAi.id, move.card, move.targetMeldId)
                    if (success) {
                        currentState = stateAfterLayoff.copy(
                            statusMessage = "${updatedAi.name} played ${move.card.displayName} on a meld."
                        )
                        _gameState.value = currentState
                        updatedAi = currentState.players.find { it.id == aiPlayer.id } ?: break
                        if (updatedAi.hand.isEmpty()) {
                            handleRoundWon(currentState, updatedAi)
                            return@launch
                        }
                        delay(600)
                    }
                }
            }

            // 4. DISCARD PHASE
            _gameState.value = currentState.copy(statusMessage = "${updatedAi.name} is discarding...")
            delay(600)
            currentState = _gameState.value ?: return@launch
            
            val discardCard = AiPlayerEngine.chooseDiscard(updatedAi, currentState.contractLevel, currentState.allTableMelds)
            if (discardCard != null) {
                executeDiscard(currentState, updatedAi, discardCard)
            } else {
                _gameState.value = currentState.copy(statusMessage = "DEADLOCK: ${updatedAi.name} has only Jokers and cannot discard!")
            }
        }
    }

    private fun persistState(state: GameState) {
        preferences.saveActiveGame(state)
        _savedMatchState.value = state
    }

    // DIALOG VISIBILITY CONTROLS
    fun setPauseDialogVisible(visible: Boolean) { _showPauseDialog.value = visible }
    fun setScoreboardVisible(visible: Boolean) { _showScoreboard.value = visible }
    fun setDeckStatsVisible(visible: Boolean) { _showDeckStats.value = visible }
    fun setRulesVisible(visible: Boolean) { _showRules.value = visible }
    fun setHistoryDialogVisible(visible: Boolean) { _showHistoryDialog.value = visible }
    fun setExpandedHandVisible(visible: Boolean) { _showExpandedHandDialog.value = visible }
    fun setMeldBuilderVisible(visible: Boolean) { _showMeldBuilder.value = visible }
    fun setPocketDialogVisible(visible: Boolean) { _showPocketDialog.value = visible }
    fun setLevel7RevealVisible(visible: Boolean) { _showLevel7Reveal.value = visible }
    fun dismissLayoffDialog() { _isLayoffDialogOpen.value = false }
    fun dismissJokerDialog() { _pendingJokerReplacement.value = null }
}

data class JokerReplacementState(
    val naturalCard: Card,
    val meld: Meld,
    val headRankName: String,
    val tailRankName: String
)

data class RummayCallState(
    val discardedCard: Card,
    val offender: Player,
    val caller: Player?,
    val isHumanCaller: Boolean,
    val isHumanOffender: Boolean,
    val humanPlayer: Player?,
    val penaltyCard: Card? = null,
    val isResolved: Boolean = false
)
