package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.engine.MeldDetector
import com.example.model.*
import com.example.ui.GameViewModel
import com.example.ui.components.*
import com.example.ui.dialogs.*
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun GameTableScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.gameState.collectAsState()
    val selectedCardIds by viewModel.selectedCardIds.collectAsState()
    val showScoreboard by viewModel.showScoreboard.collectAsState()
    val showDeckStats by viewModel.showDeckStats.collectAsState()
    val showRules by viewModel.showRules.collectAsState()
    val showMeldBuilder by viewModel.showMeldBuilder.collectAsState()
    val showLayoffDialog by viewModel.showLayoffDialog.collectAsState()
    val pendingJokerMove by viewModel.pendingJokerMove.collectAsState()
    val showWinningTrophy by viewModel.showWinningTrophy.collectAsState()
    val showPauseDialog by viewModel.showPauseDialog.collectAsState()
    val showMatchHistory by viewModel.showMatchHistory.collectAsState()
    val matchHistoryList by viewModel.matchHistoryList.collectAsState()

    val activeDraggedCard by viewModel.activeDraggedCard.collectAsState()
    val dragGlobalPos by viewModel.dragGlobalPosition.collectAsState()
    val hoveredTarget by viewModel.hoveredTarget.collectAsState()

    val human = state.humanPlayer ?: Player(name = "You", isHuman = true)
    val opponents = state.players.filter { !it.isHuman }
    val isHumanTurn = state.currentTurnPlayer.isHuman
    val isDrawPhase = isHumanTurn && state.currentPhase == TurnPhase.DRAW
    val isPlayOrDiscardPhase = isHumanTurn && state.currentPhase == TurnPhase.PLAY_OR_DISCARD

    val canHumanGoDown = remember(human.hand, state.contractLevel, human.isDown) {
        !human.isDown && MeldDetector.findValidContract(
            human.hand,
            state.contractLevel,
            human.id,
            human.name
        ) != null
    }

    val density = LocalDensity.current

    val feltBackground = Brush.radialGradient(
        listOf(
            FeltGreenLight,
            FeltGreen,
            FeltGreenDark,
            Color(0xFF031008)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(feltBackground)
            .testTag("game_table_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. TOP CONTRACT HEADER
            ContractBanner(
                currentContract = state.contractLevel,
                humanScore = human.totalScore,
                currentRound = state.currentLevel,
                totalRounds = 7,
                turnPlayerName = state.currentTurnPlayer.name,
                isHumanTurn = isHumanTurn,
                onScoreboardClicked = { viewModel.openScoreboard() },
                onRulesClicked = { viewModel.openRules() },
                onMenuClicked = { viewModel.openPauseDialog() }
            )

            // 2. MAIN FELT TABLETOP AREA
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // TOP OPPONENTS ROW (3 AI opponents seated around table)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top
                ) {
                    opponents.forEachIndexed { index, opp ->
                        val isTurn = state.currentTurnPlayer.id == opp.id
                        OpponentWoodenRackView(
                            player = opp,
                            isCurrentTurn = isTurn,
                            position = when (index) {
                                0 -> OpponentPosition.TOP
                                1 -> OpponentPosition.LEFT
                                else -> OpponentPosition.RIGHT
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // TABLE MELDS VIEW
                val hoveredMeldId = (hoveredTarget as? DragDropTarget.Meld)?.meldId
                TableMeldsView(
                    melds = state.allTableMelds,
                    onMeldClicked = { meldId ->
                        val singleSelectedCardId = selectedCardIds.firstOrNull()
                        val singleCard = human.hand.find { it.id == singleSelectedCardId }
                        if (human.isDown && singleCard != null) {
                            viewModel.onPlayOnSingleCard(singleCard, meldId)
                        }
                    },
                    isHumanDown = human.isDown,
                    hasSelectedCards = selectedCardIds.isNotEmpty(),
                    hoveredMeldId = hoveredMeldId,
                    onRegisterMeldBounds = { meldId, rect ->
                        viewModel.dragRegistry.registerTarget(DragDropTarget.Meld(meldId), rect)
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // CENTER TABLE (Stock Pile & Discard Pile)
                val isDiscardHovered = hoveredTarget is DragDropTarget.DiscardPile
                TableCenterView(
                    stockCount = state.drawDeck.size,
                    topDiscard = state.discardPile.lastOrNull(),
                    discardPileCount = state.discardPile.size,
                    isPlayerTurn = isHumanTurn,
                    isDrawPhase = isDrawPhase,
                    onStockClicked = {
                        if (isDrawPhase) {
                            viewModel.onDrawFromStock()
                        }
                    },
                    onDiscardClicked = {
                        if (isDrawPhase && state.pendingToYouOffer != null) {
                            viewModel.onTakeDiscardToYou()
                        }
                    },
                    isDiscardHovered = isDiscardHovered,
                    onRegisterStockBounds = { rect ->
                        viewModel.dragRegistry.registerTarget(DragDropTarget.StockPile, rect)
                    },
                    onRegisterDiscardBounds = { rect ->
                        viewModel.dragRegistry.registerTarget(DragDropTarget.DiscardPile, rect)
                    }
                )

                Spacer(modifier = Modifier.height(2.dp))
            }

            // 3. HUMAN 2-TIER WOODEN CARD RACK & ACTION BUTTONS
            PlayerWoodenCardRack(
                player = human,
                selectedCardIds = selectedCardIds,
                onCardClicked = { cardId ->
                    viewModel.toggleCardSelection(cardId)
                },
                onSortClicked = { mode -> viewModel.sortHand(mode) },
                onGoDownClicked = { viewModel.openMeldBuilder() },
                canGoDown = canHumanGoDown,
                onDiscardClicked = { viewModel.onDiscardCard() },
                canDiscard = selectedCardIds.isNotEmpty(),
                onBuyClicked = { viewModel.onHumanBuyDiscard() },
                canBuy = state.pendingBuyPriority != null,
                onPlayOnClicked = {
                    val singleSelectedCardId = selectedCardIds.firstOrNull()
                    val card = human.hand.find { it.id == singleSelectedCardId }
                    if (card != null) {
                        viewModel.openLayoffDialog(card)
                    } else {
                        val firstPlayable = human.hand.firstOrNull { c ->
                            state.allTableMelds.any { m -> m.canAddCard(c) }
                        }
                        if (firstPlayable != null) {
                            viewModel.openLayoffDialog(firstPlayable)
                        }
                    }
                },
                onClearSelection = { viewModel.clearSelection() },
                isPlayerTurn = isHumanTurn,
                isPlayOrDiscardPhase = isPlayOrDiscardPhase,
                autoSortEnabled = state.autoSortHand,
                onToggleAutoSort = { viewModel.toggleAutoSort() },
                activeDraggedCardId = activeDraggedCard?.id,
                hoveredTarget = hoveredTarget,
                onCardDragStart = { card, row, slot, globalPos ->
                    viewModel.onCardDragStart(card, row, slot, globalPos)
                },
                onCardDragMove = { delta -> viewModel.onCardDragMove(delta) },
                onCardDragEnd = { viewModel.onCardDragEnd() },
                onCardDragCancel = { viewModel.onCardDragCancel() },
                onRegisterSlotBounds = { rIdx, sIdx, rect ->
                    viewModel.dragRegistry.registerTarget(DragDropTarget.RackSlot(rIdx, sIdx), rect)
                }
            )

            // 4. BOTTOM WOODEN TAB NAVIGATION BAR
            BottomTableNavBar(
                onScoreClicked = { viewModel.openScoreboard() },
                onHistoryClicked = { viewModel.openMatchHistory() },
                onDeckStatsClicked = { viewModel.openDeckStats() },
                onRulesClicked = { viewModel.openRules() }
            )
        }

        // FLOATING DRAGGED CARD OVERLAY
        activeDraggedCard?.let { card ->
            val pos = dragGlobalPos
            if (pos != null) {
                val cardWidthDp = 54.dp
                val cardHeightDp = 76.dp
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
                        .shadow(16.dp, RoundedCornerShape(4.dp))
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

        // DIALOG OVERLAYS
        if (state.pendingToYouOffer != null && isHumanTurn && isDrawPhase) {
            val discard = state.pendingToYouOffer!!
            val discarder = state.players[(state.currentTurnPlayerIndex + state.players.size - 1) % state.players.size]
            ToYouOfferDialog(
                discard = discard,
                discarderName = discarder.name,
                player = human,
                contractLevel = state.contractLevel,
                onTakeClicked = { viewModel.onTakeDiscardToYou() },
                onDrawFromStockClicked = { viewModel.onDrawFromStock() }
            )
        }

        if (state.pendingBuyPriority != null && !isHumanTurn) {
            val buyOffer = state.pendingBuyPriority!!
            BuyPriorityDialog(
                discard = buyOffer.discard,
                discarderName = buyOffer.discarderName,
                player = human,
                contractLevel = state.contractLevel,
                onBuyClicked = { viewModel.onHumanBuyDiscard() },
                onPassClicked = { viewModel.onHumanPassBuy() }
            )
        }

        if (state.pendingRummay != null) {
            val rummay = state.pendingRummay!!
            val offender = state.players.find { it.id == rummay.offenderId } ?: human
            val caller = state.players.find { it.id == rummay.callerId }
            RummayCallDialog(
                discardedCard = rummay.discardedCard,
                offender = offender,
                caller = caller,
                isHumanCaller = rummay.callerId == human.id,
                isHumanOffender = rummay.offenderId == human.id,
                humanPlayer = human,
                onCallRummay = { viewModel.onHumanCallRummay() },
                onPassRummay = { viewModel.onHumanPassRummay() },
                onGiveCardSelected = { card -> viewModel.onHumanGiveRummayCard(card) }
            )
        }

        if (showMeldBuilder) {
            MeldBuilderDialog(
                player = human,
                contractLevel = state.contractLevel,
                onConfirmGoDown = { groups -> viewModel.onConfirmGoDown(groups) },
                onDismiss = { viewModel.closeMeldBuilder() }
            )
        }

        if (showLayoffDialog != null) {
            LayoffDestinationDialog(
                selectedCard = showLayoffDialog!!,
                player = human,
                allTableMelds = state.allTableMelds,
                onSelectDestination = { meldId ->
                    viewModel.onPlayOnSingleCard(showLayoffDialog!!, meldId)
                },
                onDismiss = { viewModel.closeLayoffDialog() }
            )
        }

        if (pendingJokerMove != null) {
            JokerDestinationDialog(
                pendingMove = pendingJokerMove!!,
                onSelectConfiguration = { config -> viewModel.onConfirmJokerMove(config) },
                onCancel = { viewModel.cancelJokerMove() }
            )
        }

        if (state.isRoundOver) {
            RoundResultsDialog(
                completedLevel = state.currentLevel,
                players = state.players,
                winnerIndex = state.roundWinnerIndex,
                onViewTrophyClicked = if (state.currentLevel >= 7) {
                    { viewModel.openScoreboard() }
                } else null,
                onNextLevelClicked = {
                    if (state.currentLevel >= 7) {
                        viewModel.returnToMainMenu()
                    } else {
                        viewModel.onNextLevel()
                    }
                }
            )
        }

        if (showScoreboard) {
            ScoreboardDialog(
                players = state.players,
                currentLevel = state.currentLevel,
                onDismiss = { viewModel.closeScoreboard() }
            )
        }

        if (showDeckStats) {
            DeckStatsDialog(
                state = state,
                onDismiss = { viewModel.closeDeckStats() }
            )
        }

        if (showRules) {
            RulesDialog(
                onDismiss = { viewModel.closeRules() }
            )
        }

        if (showPauseDialog) {
            PauseDialog(
                currentLevel = state.currentLevel,
                contract = state.contractLevel,
                onResumeGame = { viewModel.resumeFromPause() },
                onSaveAndMainMenu = { viewModel.saveAndReturnToMenu() },
                onQuitToMainMenu = { viewModel.quitToMainMenu() }
            )
        }

        if (showWinningTrophy) {
            WinningTrophyDialog(
                players = state.players,
                onReturnToMenu = {
                    viewModel.closeWinningTrophy()
                    viewModel.returnToMainMenu()
                }
            )
        }

        if (showMatchHistory) {
            MatchHistoryDialog(
                historyList = matchHistoryList,
                onDismiss = { viewModel.closeMatchHistory() }
            )
        }
    }
}
