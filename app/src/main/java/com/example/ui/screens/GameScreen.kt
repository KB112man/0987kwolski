package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.model.Card
import com.example.model.GameState
import com.example.model.TurnPhase
import com.example.ui.GameViewModel
import com.example.ui.components.*
import com.example.ui.dialogs.*
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onNavigateToMainMenu: () -> Unit
) {
    val state by viewModel.gameState.collectAsState()
    val selectedCardIds by viewModel.selectedCardIds.collectAsState()
    val autoSortEnabled by viewModel.autoSortEnabled.collectAsState()

    // Dialog state collectors
    val showPauseDialog by viewModel.showPauseDialog.collectAsState()
    val showScoreboard by viewModel.showScoreboard.collectAsState()
    val showDeckStats by viewModel.showDeckStats.collectAsState()
    val showRules by viewModel.showRules.collectAsState()
    val showHistoryDialog by viewModel.showHistoryDialog.collectAsState()
    val matchHistory by viewModel.matchHistory.collectAsState()
    val showMeldBuilder by viewModel.showMeldBuilder.collectAsState()
    val layoffCard by viewModel.showLayoffDestinationDialog.collectAsState()
    val pendingJokerReplacement by viewModel.pendingJokerReplacement.collectAsState()
    val activeRummayCall by viewModel.activeRummayCall.collectAsState()
    val roundWinner by viewModel.roundWinner.collectAsState()
    val isTournamentFinished by viewModel.isTournamentFinished.collectAsState()

    val currentGameState = state ?: return

    // Drag and Drop Registry
    val dragDropRegistry = remember { DragDropRegistry() }
    var activeDraggedCard by remember { mutableStateOf<Card?>(null) }
    var dragOriginRow by remember { mutableIntStateOf(-1) }
    var dragOriginSlot by remember { mutableIntStateOf(-1) }
    var dragGlobalPosition by remember { mutableStateOf<Offset?>(null) }
    var hoveredTarget by remember { mutableStateOf<DragDropTarget?>(null) }

    val density = LocalDensity.current

    val woodBackground = Brush.radialGradient(
        listOf(
            Color(0xFF381B09),
            Color(0xFF220E04),
            Color(0xFF140702),
            Color(0xFF0A0301)
        )
    )

    val topOpponent = currentGameState.topOpponent
    val leftOpponent = currentGameState.leftOpponent
    val rightOpponent = currentGameState.rightOpponent
    val humanPlayer = currentGameState.humanPlayer

    val isHumanTurn = currentGameState.isHumanTurn
    val isPlayOrDiscardPhase = currentGameState.currentPhase == TurnPhase.PLAY_OR_DISCARD
    val isHumanDown = humanPlayer?.isDown == true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(woodBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("game_tabletop_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. TOP HEADER BANNER
            ContractBanner(
                currentContract = currentGameState.contractLevel,
                humanScore = humanPlayer?.totalScore ?: 0,
                currentRound = currentGameState.currentLevel,
                totalRounds = 7,
                turnPlayerName = currentGameState.currentPlayer.name,
                isHumanTurn = isHumanTurn,
                onScoreboardClicked = { viewModel.setScoreboardVisible(true) },
                onRulesClicked = { viewModel.setRulesVisible(true) },
                onMenuClicked = { viewModel.setPauseDialogVisible(true) },
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )

            // Status notification bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(vertical = 1.dp),
                color = Color(0x33000000),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(0.75.dp, Color(0x33FFFFFF))
            ) {
                Text(
                    text = currentGameState.statusMessage,
                    color = Color(0xFFF1F5F9),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    maxLines = 1
                )
            }

            // 2. TOP OPPONENT (e.g. Wicked Gremlin) + Melds directly below rack!
            if (topOpponent != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OpponentWoodenRackView(
                        player = topOpponent,
                        isCurrentTurn = currentGameState.currentPlayer.id == topOpponent.id,
                        position = OpponentPosition.TOP
                    )
                    // Melds directly below top opponent's rack
                    val hoveredMeldId = when (val h = hoveredTarget) {
                        is DragDropTarget.TableMeld -> h.meldId
                        is DragDropTarget.Meld -> h.meldId
                        else -> null
                    }
                    SpatialPlayerMeldsView(
                        melds = topOpponent.laidMelds,
                        isVerticalStack = false,
                        onMeldClicked = { meldId ->
                            val selectedCard = humanPlayer?.hand?.find { selectedCardIds.contains(it.id) }
                            if (selectedCard != null && isHumanDown) {
                                viewModel.onLayoffToMeld(meldId, selectedCard)
                            }
                        },
                        isHumanDown = isHumanDown,
                        hoveredMeldId = hoveredMeldId,
                        onRegisterMeldBounds = { meldId, rect ->
                            dragDropRegistry.registerMeld(meldId, rect)
                        }
                    )
                }
            }

            // 3. MIDDLE TABLE (Left Opponent + Melds, Center Stock/Discard, Right Opponent Melds + Rack)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val hoveredMeldId = when (val h = hoveredTarget) {
                    is DragDropTarget.TableMeld -> h.meldId
                    is DragDropTarget.Meld -> h.meldId
                    else -> null
                }

                // LEFT OPPONENT SEAT (Rack + Melds to the right)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (leftOpponent != null) {
                        OpponentWoodenRackView(
                            player = leftOpponent,
                            isCurrentTurn = currentGameState.currentPlayer.id == leftOpponent.id,
                            position = OpponentPosition.LEFT
                        )
                        SpatialPlayerMeldsView(
                            melds = leftOpponent.laidMelds,
                            isVerticalStack = true,
                            onMeldClicked = { meldId ->
                                val selectedCard = humanPlayer?.hand?.find { selectedCardIds.contains(it.id) }
                                if (selectedCard != null && isHumanDown) {
                                    viewModel.onLayoffToMeld(meldId, selectedCard)
                                }
                            },
                            isHumanDown = isHumanDown,
                            hoveredMeldId = hoveredMeldId,
                            onRegisterMeldBounds = { meldId, rect ->
                                dragDropRegistry.registerMeld(meldId, rect)
                            }
                        )
                    }
                }

                // CENTER TABLE (STOCK & DISCARD PILES)
                TableCenterView(
                    stockCount = currentGameState.drawDeck.size,
                    topDiscard = currentGameState.topDiscard,
                    discardPileCount = currentGameState.discardPile.size,
                    isPlayerTurn = isHumanTurn,
                    isDrawPhase = currentGameState.currentPhase == TurnPhase.DRAW,
                    onStockClicked = { viewModel.onHumanDrawFromStock() },
                    onDiscardClicked = {
                        if (isHumanTurn && currentGameState.currentPhase == TurnPhase.DRAW) {
                            viewModel.onHumanTakeDiscard()
                        } else if (currentGameState.pendingBuyPriority != null) {
                            viewModel.onHumanBuyDiscard()
                        }
                    },
                    isDiscardHovered = hoveredTarget is DragDropTarget.DiscardPile,
                    onRegisterStockBounds = { rect -> dragDropRegistry.registerStockPile(rect) },
                    onRegisterDiscardBounds = { rect -> dragDropRegistry.registerDiscardPile(rect) }
                )

                // RIGHT OPPONENT SEAT (Melds to the left + Rack)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (rightOpponent != null) {
                        SpatialPlayerMeldsView(
                            melds = rightOpponent.laidMelds,
                            isVerticalStack = true,
                            onMeldClicked = { meldId ->
                                val selectedCard = humanPlayer?.hand?.find { selectedCardIds.contains(it.id) }
                                if (selectedCard != null && isHumanDown) {
                                    viewModel.onLayoffToMeld(meldId, selectedCard)
                                }
                            },
                            isHumanDown = isHumanDown,
                            hoveredMeldId = hoveredMeldId,
                            onRegisterMeldBounds = { meldId, rect ->
                                dragDropRegistry.registerMeld(meldId, rect)
                            }
                        )
                        OpponentWoodenRackView(
                            player = rightOpponent,
                            isCurrentTurn = currentGameState.currentPlayer.id == rightOpponent.id,
                            position = OpponentPosition.RIGHT
                        )
                    }
                }
            }

            // 4. HUMAN PLAYER AREA
            if (humanPlayer != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Human player's melds directly in front (above) their wooden rack!
                    val hoveredMeldId = when (val h = hoveredTarget) {
                        is DragDropTarget.TableMeld -> h.meldId
                        is DragDropTarget.Meld -> h.meldId
                        else -> null
                    }
                    if (humanPlayer.laidMelds.isNotEmpty()) {
                        SpatialPlayerMeldsView(
                            melds = humanPlayer.laidMelds,
                            isVerticalStack = false,
                            onMeldClicked = { meldId ->
                                val selectedCard = humanPlayer.hand.find { selectedCardIds.contains(it.id) }
                                if (selectedCard != null && isHumanDown) {
                                    viewModel.onLayoffToMeld(meldId, selectedCard)
                                }
                            },
                            isHumanDown = isHumanDown,
                            hoveredMeldId = hoveredMeldId,
                            onRegisterMeldBounds = { meldId, rect ->
                                dragDropRegistry.registerMeld(meldId, rect)
                            },
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    // 2-TIER STEPPED WOODEN CARD RACK (10 slots each tier + 4 Action Buttons underneath)
                    PlayerWoodenCardRack(
                        player = humanPlayer,
                        selectedCardIds = selectedCardIds,
                        onCardClicked = { cardId -> viewModel.toggleCardSelection(cardId) },
                        onSortClicked = { mode -> viewModel.sortHumanHand(mode) },
                        onGoDownClicked = { viewModel.setMeldBuilderVisible(true) },
                        canGoDown = !humanPlayer.isDown && isHumanTurn && isPlayOrDiscardPhase,
                        onDiscardClicked = { viewModel.onHumanDiscardSelectedCard() },
                        canDiscard = selectedCardIds.size == 1,
                        onPlayOnClicked = { viewModel.onPlayOnSelectedCard() },
                        onClearSelection = { viewModel.clearSelection() },
                        isPlayerTurn = isHumanTurn,
                        isPlayOrDiscardPhase = isPlayOrDiscardPhase,
                        autoSortEnabled = autoSortEnabled,
                        onToggleAutoSort = { viewModel.toggleAutoSort() },
                        activeDraggedCardId = activeDraggedCard?.id,
                        hoveredTarget = hoveredTarget,
                        onCardDragStart = { card, row, slot, initialTouchPos ->
                            activeDraggedCard = card
                            dragOriginRow = row
                            dragOriginSlot = slot
                            dragGlobalPosition = initialTouchPos
                            hoveredTarget = null
                        },
                        onCardDragMove = { delta ->
                            val currentPos = (dragGlobalPosition ?: Offset.Zero) + delta
                            dragGlobalPosition = currentPos
                            hoveredTarget = dragDropRegistry.findTarget(currentPos)
                        },
                        onCardDragEnd = {
                            val card = activeDraggedCard
                            val target = hoveredTarget
                            if (card != null && target != null) {
                                when (target) {
                                    is DragDropTarget.DiscardPile -> {
                                        if (isHumanTurn && isPlayOrDiscardPhase) {
                                            viewModel.onHumanDiscardCard(card)
                                        }
                                    }
                                    is DragDropTarget.TableMeld -> {
                                        if (isHumanDown) {
                                            viewModel.onLayoffToMeld(target.meldId, card)
                                        }
                                    }
                                    is DragDropTarget.Meld -> {
                                        if (isHumanDown) {
                                            viewModel.onLayoffToMeld(target.meldId, card)
                                        }
                                    }
                                    is DragDropTarget.RackSlot -> {
                                        if (dragOriginRow != -1 && dragOriginSlot != -1) {
                                            viewModel.onMoveCardInRack(
                                                fromRow = dragOriginRow,
                                                fromSlot = dragOriginSlot,
                                                toRow = target.rowIndex,
                                                toSlot = target.slotIndex
                                            )
                                        }
                                    }
                                    else -> { /* No-op */ }
                                }
                            }
                            activeDraggedCard = null
                            dragOriginRow = -1
                            dragOriginSlot = -1
                            dragGlobalPosition = null
                            hoveredTarget = null
                        },
                        onCardDragCancel = {
                            activeDraggedCard = null
                            dragOriginRow = -1
                            dragOriginSlot = -1
                            dragGlobalPosition = null
                            hoveredTarget = null
                        },
                        onRegisterRowBounds = { rIdx, rect ->
                            dragDropRegistry.registerRow(rIdx, rect)
                        },
                        onRegisterSlotBounds = { rIdx, sIdx, rect ->
                            dragDropRegistry.registerSlot(rIdx, sIdx, rect)
                        }
                    )
                }
            }

            // 5. BOTTOM NAVIGATION BAR (SCORE, HISTORY, TABLE, 108 DECK, HELP/RULES)
            BottomTableNavBar(
                onScoreClicked = { viewModel.setScoreboardVisible(true) },
                onHistoryClicked = { viewModel.setHistoryDialogVisible(true) },
                onDeckStatsClicked = { viewModel.setDeckStatsVisible(true) },
                onRulesClicked = { viewModel.setRulesVisible(true) }
            )
        }

        // 6. FLOATING DRAGGED CARD OVERLAY
        activeDraggedCard?.let { card ->
            val pos = dragGlobalPosition
            if (pos != null) {
                val cardWidthDp = 50.dp
                val cardHeightDp = 70.dp
                val cardWidthPx = with(density) { cardWidthDp.toPx() }
                val cardHeightPx = with(density) { cardHeightDp.toPx() }

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (pos.x - cardWidthPx / 2f).roundToInt(),
                                y = (pos.y - cardHeightPx / 2f).roundToInt()
                            )
                        }
                        .zIndex(2000f)
                        .shadow(20.dp, RoundedCornerShape(4.dp))
                ) {
                    PlayingCardView(
                        card = card,
                        cardWidth = cardWidthDp,
                        cardHeight = cardHeightDp,
                        showPointsBadge = true
                    )
                }
            }
        }

        // 7. DIALOGS
        // Pause Dialog
        if (showPauseDialog) {
            PauseDialog(
                currentLevel = currentGameState.currentLevel,
                contract = currentGameState.contractLevel,
                onResumeGame = { viewModel.setPauseDialogVisible(false) },
                onSaveAndMainMenu = {
                    viewModel.setPauseDialogVisible(false)
                    onNavigateToMainMenu()
                },
                onQuitToMainMenu = {
                    viewModel.setPauseDialogVisible(false)
                    onNavigateToMainMenu()
                }
            )
        }

        // Scoreboard Dialog
        if (showScoreboard) {
            ScoreboardDialog(
                players = currentGameState.players,
                currentLevel = currentGameState.currentLevel,
                onDismiss = { viewModel.setScoreboardVisible(false) }
            )
        }

        // Deck Stats Dialog
        if (showDeckStats) {
            DeckStatsDialog(
                state = currentGameState,
                onDismiss = { viewModel.setDeckStatsVisible(false) }
            )
        }

        // Rules Dialog
        if (showRules) {
            RulesDialog(
                onDismiss = { viewModel.setRulesVisible(false) }
            )
        }

        // Match History Dialog
        if (showHistoryDialog) {
            MatchHistoryDialog(
                historyList = matchHistory,
                onDismiss = { viewModel.setHistoryDialogVisible(false) }
            )
        }

        // Buy Priority Offer Dialog (when out-of-turn opponent discards)
        val buyPriority = currentGameState.pendingBuyPriority
        if (buyPriority != null && buyPriority.eligibleContenderIds.contains(humanPlayer?.id)) {
            BuyPriorityDialog(
                discard = buyPriority.discardCard,
                discarderName = buyPriority.discarderName,
                player = humanPlayer,
                contractLevel = currentGameState.contractLevel,
                onBuyClicked = { viewModel.onHumanBuyDiscard() },
                onPassClicked = { viewModel.onHumanPassBuy() }
            )
        }

        // To You Offer Dialog (when human turn starts with discard available)
        if (isHumanTurn && currentGameState.currentPhase == TurnPhase.DRAW && currentGameState.topDiscard != null && buyPriority == null) {
            ToYouOfferDialog(
                discard = currentGameState.topDiscard!!,
                discarderName = "Discard Pile",
                player = humanPlayer,
                contractLevel = currentGameState.contractLevel,
                onTakeClicked = { viewModel.onHumanTakeDiscard() },
                onDrawFromStockClicked = { viewModel.onHumanDrawFromStock() }
            )
        }

        // Rummay! Penalty Dialog
        if (activeRummayCall != null) {
            val rummay = activeRummayCall!!
            RummayCallDialog(
                discardedCard = rummay.discardedCard,
                offender = rummay.offender,
                caller = rummay.caller,
                isHumanCaller = rummay.isHumanCaller,
                isHumanOffender = rummay.isHumanOffender,
                humanPlayer = rummay.humanPlayer,
                onCallRummay = { viewModel.onHumanCallRummay() },
                onPassRummay = { viewModel.onHumanPassRummay() },
                onGiveCardSelected = { penaltyCard ->
                    viewModel.onHumanGiveCardToRummayOffender(penaltyCard)
                }
            )
        }

        // Meld Builder Dialog (Going Down)
        if (showMeldBuilder && humanPlayer != null) {
            MeldBuilderDialog(
                player = humanPlayer,
                contractLevel = currentGameState.contractLevel,
                onConfirmGoDown = { melds -> viewModel.onConfirmGoDown(melds) },
                onDismiss = { viewModel.setMeldBuilderVisible(false) }
            )
        }

        // Layoff Destination Selection Dialog
        if (layoffCard != null && humanPlayer != null) {
            LayoffDestinationDialog(
                selectedCard = layoffCard!!,
                player = humanPlayer,
                allTableMelds = currentGameState.allTableMelds,
                onSelectDestination = { meldId ->
                    viewModel.onLayoffToMeld(meldId, layoffCard!!)
                },
                onDismiss = { viewModel.dismissLayoffDialog() }
            )
        }

        // Joker Destination Dialog (when playing natural card on run with Joker)
        if (pendingJokerReplacement != null) {
            val jokerState = pendingJokerReplacement!!
            JokerDestinationDialog(
                naturalCard = jokerState.naturalCard,
                meld = jokerState.meld,
                headRankName = jokerState.headRankName,
                tailRankName = jokerState.tailRankName,
                onSelectHead = { viewModel.onResolveJokerDestination(shiftToHead = true) },
                onSelectTail = { viewModel.onResolveJokerDestination(shiftToHead = false) }
            )
        }

        // Round Results Dialog
        if (roundWinner != null && !isTournamentFinished) {
            RoundResultsDialog(
                winner = roundWinner!!,
                players = currentGameState.players,
                completedLevel = currentGameState.currentLevel,
                isFinalLevel = currentGameState.currentLevel >= 7,
                onContinueNextLevel = { viewModel.onContinueNextLevel() }
            )
        }

        // Tournament Grand Trophy Dialog
        if (isTournamentFinished) {
            WinningTrophyDialog(
                players = currentGameState.players,
                onPlayAgain = { viewModel.startNewTournament() },
                onBackToMainMenu = { onNavigateToMainMenu() }
            )
        }
    }
}
