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

    private val _showMeldBuilder = MutableStateFlow(false)
    val showMeldBuilder: StateFlow<Boolean> = _showMeldBuilder.asStateFlow()

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
    }

    fun clearSelection() {
        _selectedCardIds.value = emptySet()
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
            _gameState.value = stateAfterTake
            persistState(stateAfterTake)
        }
    }

    // BUY DISCARD
    fun onHumanBuyDiscard() {
        val state = _gameState.value ?: return
        val humanId = state.humanPlayer?.id ?: return
        val (updatedState, boughtCard) = DeckEngine.buyDiscard(state, humanId)
        if (boughtCard != null) {
            val finalState = updatedState.copy(
                pendingBuyPriority = null,
                statusMessage = "You bought ${boughtCard.displayName} (+1 draw penalty from Stock)."
            )
            _gameState.value = finalState
            persistState(finalState)
            checkTurnState()
        }
    }

    fun onHumanPassBuy() {
        val state = _gameState.value ?: return
        val currentBuyState = state.pendingBuyPriority ?: return
        
        if (currentBuyState.eligibleContenderIds.firstOrNull() == state.humanPlayer?.id) {
            val afterPass = state.copy(
                pendingBuyPriority = currentBuyState.copy(
                    interestedBuyerIds = currentBuyState.eligibleContenderIds.drop(1)
                )
            )
            _gameState.value = afterPass
            persistState(afterPass)
            processBuyPrioritySequence()
        }
    }

    private fun processBuyPrioritySequence() {
        val state = _gameState.value ?: return
        val currentBuyState = state.pendingBuyPriority ?: return

        if (currentBuyState.eligibleContenderIds.isEmpty()) {
            val afterPass = state.copy(
                pendingBuyPriority = null,
                statusMessage = "${state.players.find { it.id == currentBuyState.nextTurnPlayerId }?.name}'s turn."
            )
            _gameState.value = afterPass
            persistState(afterPass)
            checkTurnState()
            return
        }

        val nextContenderId = currentBuyState.eligibleContenderIds.first()
        val nextContender = state.players.find { it.id == nextContenderId } ?: return

        if (nextContender.isHuman) {
            // WAIT INDEFINITELY for human to press Buy or Pass.
            val waitState = state.copy(statusMessage = "Waiting for your Buy/Pass decision...")
            _gameState.value = waitState
            return
        }

        // It is an AI's turn to decide on the Buy
        val waitAiState = state.copy(statusMessage = "Checking if ${nextContender.name} wants to Buy...")
        _gameState.value = waitAiState

        viewModelScope.launch {
            delay(1000)
            val currentState = _gameState.value ?: return@launch
            val currentPriority = currentState.pendingBuyPriority ?: return@launch

            val wantsBuy = AiPlayerEngine.shouldAiBuyDiscard(nextContender, currentPriority.discardCard, currentState.contractLevel, currentState.allTableMelds)
            if (wantsBuy) {
                val (boughtState, _) = DeckEngine.buyDiscard(currentState, nextContenderId)
                val afterBuy = boughtState.copy(
                    pendingBuyPriority = null,
                    statusMessage = "${nextContender.name} bought ${currentPriority.discardCard.displayName}."
                )
                _gameState.value = afterBuy
                persistState(afterBuy)
                checkTurnState()
            } else {
                val afterPass = currentState.copy(
                    pendingBuyPriority = currentPriority.copy(
                        interestedBuyerIds = currentPriority.eligibleContenderIds.drop(1)
                    )
                )
                _gameState.value = afterPass
                processBuyPrioritySequence()
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
        
        if (state.contractLevel.noDiscard && player.hand.size == 1) {
            // In Level 7, AI might try to discard its last card. 
            // We just skip the discard and advance turn.
            advanceTurn(state, null)
            return
        }

        // Check for Rummay! (Offending discard)
        val canPlayOnAnyMeld = state.allTableMelds.any { it.canAddCard(card) }
        val (stateAfterDiscard, _) = DeckEngine.discardCard(state, player.id, card)

        // Check if player went out
        val updatedPlayer = stateAfterDiscard.players.find { it.id == player.id }
        if (updatedPlayer != null && updatedPlayer.hand.isEmpty()) {
            handleRoundWon(stateAfterDiscard, updatedPlayer)
            return
        }

        if (canPlayOnAnyMeld && stateAfterDiscard.allTableMelds.isNotEmpty()) {
            triggerRummayAlert(stateAfterDiscard, player, card)
            return
        }

        advanceTurn(stateAfterDiscard, card)
    }

    private fun triggerRummayAlert(state: GameState, offender: Player, discardedCard: Card) {
        val isHumanOffender = offender.isHuman
        val human = state.humanPlayer

        _activeRummayCall.value = RummayCallState(
            discardedCard = discardedCard,
            offender = offender,
            caller = null,
            isHumanCaller = false,
            isHumanOffender = isHumanOffender,
            humanPlayer = human
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
            val penaltyCard = aiCaller.hand.maxByOrNull { it.points } ?: aiCaller.hand.first()
            val stateAfterTransfer = DeckEngine.transferCardBetweenPlayers(state, aiCaller.id, currentCall.offender.id, penaltyCard)
            _activeRummayCall.value = null

            val updatedState = stateAfterTransfer.copy(
                statusMessage = "${aiCaller.name} called RUMMAY! and gave a card to ${currentCall.offender.name}."
            )
            _gameState.value = updatedState
            persistState(updatedState)
            advanceTurn(updatedState, currentCall.discardedCard)
        } else {
            _activeRummayCall.value = null
            advanceTurn(state, currentCall.discardedCard)
        }
    }

    fun onHumanGiveCardToRummayOffender(penaltyCard: Card) {
        val currentCall = _activeRummayCall.value ?: return
        val state = _gameState.value ?: return
        val human = state.humanPlayer ?: return

        val stateAfterTransfer = DeckEngine.transferCardBetweenPlayers(state, human.id, currentCall.offender.id, penaltyCard)
        _activeRummayCall.value = null

        val updatedState = stateAfterTransfer.copy(
            statusMessage = "You called RUMMAY! and gave ${penaltyCard.displayName} to ${currentCall.offender.name}."
        )
        _gameState.value = updatedState
        persistState(updatedState)
        advanceTurn(updatedState, currentCall.discardedCard)
    }

    private fun advanceTurn(state: GameState, lastDiscarded: Card?) {
        val nextPlayer = state.nextPlayer

        val potentialBuyers = state.players.filter { it.id != nextPlayer.id && it.id != state.currentPlayer.id }
        val eligibleContenderIds = potentialBuyers.map { it.id }

        if (lastDiscarded != null && eligibleContenderIds.isNotEmpty()) {
            val stateWithBuy = state.copy(
                pendingBuyPriority = PendingBuyPriority(
                    discard = lastDiscarded,
                    discarderId = state.currentPlayer.id,
                    discarderName = state.currentPlayer.name,
                    nextTurnPlayerId = nextPlayer.id,
                    interestedBuyerIds = eligibleContenderIds
                )
            )
            val finalAdvancedState = DeckEngine.nextTurn(stateWithBuy)
            _gameState.value = finalAdvancedState
            persistState(finalAdvancedState)
            processBuyPrioritySequence()
        } else {
            val finalAdvancedState = DeckEngine.nextTurn(state)
            _gameState.value = finalAdvancedState
            persistState(finalAdvancedState)
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

        // Check if playing natural card on a run that contains a joker
        if (targetMeld.type == MeldType.RUN && !card.isWild && targetMeld.cards.any { it.isWild }) {
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

        val (stateAfterPlayOn, success) = DeckEngine.playOnMeld(state, human.id, card, meldId)
        if (success) {
            _selectedCardIds.value = emptySet()
            _isLayoffDialogOpen.value = false

            val updatedHuman = stateAfterPlayOn.players.find { it.id == human.id }
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

            // 2. CHECK GO DOWN
            var updatedAi = currentState.players.find { it.id == aiPlayer.id } ?: return@launch
            if (!updatedAi.isDown) {
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

            // 3. CHECK PLAY ON
            if (updatedAi.isDown) {
                val moves = AiPlayerEngine.findAiPlayOnMoves(updatedAi, currentState.allTableMelds)
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
            executeDiscard(currentState, updatedAi, discardCard)
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
    fun setMeldBuilderVisible(visible: Boolean) { _showMeldBuilder.value = visible }
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
    val humanPlayer: Player?
)
