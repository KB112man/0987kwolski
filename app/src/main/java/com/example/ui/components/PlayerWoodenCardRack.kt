package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.model.Card
import com.example.model.Player
import com.example.model.RACK_ROW_COUNT
import com.example.model.RACK_SLOTS_PER_ROW
import com.example.model.SortMode
import com.example.ui.theme.*

@Composable
fun PlayerWoodenCardRack(
    player: Player,
    selectedCardIds: Set<String>,
    onCardClicked: (String) -> Unit,
    onSortClicked: (SortMode) -> Unit,
    onGoDownClicked: () -> Unit,
    canGoDown: Boolean,
    onDiscardClicked: () -> Unit,
    canDiscard: Boolean,
    onBuyClicked: (() -> Unit)? = null,
    canBuy: Boolean = false,
    onPlayOnClicked: (() -> Unit)? = null,
    onClearSelection: () -> Unit,
    isPlayerTurn: Boolean,
    isPlayOrDiscardPhase: Boolean,
    autoSortEnabled: Boolean = false,
    onToggleAutoSort: (() -> Unit)? = null,
    activeDraggedCardId: String? = null,
    hoveredTarget: DragDropTarget? = null,
    onCardDragStart: (Card, Int, Int, Offset) -> Unit = { _, _, _, _ -> },
    onCardDragMove: (Offset) -> Unit = {},
    onCardDragEnd: () -> Unit = {},
    onCardDragCancel: () -> Unit = {},
    onRegisterRowBounds: (Int, Rect) -> Unit = { _, _ -> },
    onRegisterSlotBounds: (Int, Int, Rect) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val rackRows = remember(player.hand, player.rackRows) {
        val rows = (0 until RACK_ROW_COUNT).map { rIdx ->
            val row = player.rackRows.getOrNull(rIdx) ?: emptyList()
            val mRow = row.toMutableList()
            while (mRow.size < RACK_SLOTS_PER_ROW) mRow.add(null)
            mRow
        }
        rows
    }

    val woodRackGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF5E3013),
            Color(0xFF3B1C0A),
            Color(0xFF220E04),
            Color(0xFF120601)
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(20.dp, RoundedCornerShape(10.dp))
            .background(woodRackGradient, RoundedCornerShape(10.dp))
            .border(
                1.5.dp,
                Brush.verticalGradient(listOf(Color(0xFF8B4D20), Color(0xFF2A1204))),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .testTag("player_wooden_card_rack")
    ) {
        // 1. 2-TIER STEPPED WOODEN CARD RACK (10-column slot layout)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF5A2E0F), RoundedCornerShape(8.dp)),
            color = Color(0xFF190C04),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp, horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                rackRows.forEachIndexed { rowIndex, rowSlots ->
                    val isRowHovered = (hoveredTarget as? DragDropTarget.RackSlot)?.rowIndex == rowIndex
                    val targetSlotIndex = if (isRowHovered) (hoveredTarget as DragDropTarget.RackSlot).slotIndex else -1
                    TieredWoodenShelfRow(
                        tierNumber = rowIndex + 1,
                        slots = rowSlots,
                        selectedCardIds = selectedCardIds,
                        isHovered = isRowHovered,
                        targetSlotIndex = targetSlotIndex,
                        activeDraggedCardId = activeDraggedCardId,
                        onCardClicked = onCardClicked,
                        onDragStart = { card, slotIndex, globalOffset ->
                            onCardDragStart(card, rowIndex, slotIndex, globalOffset)
                        },
                        onDragMove = onCardDragMove,
                        onDragEnd = onCardDragEnd,
                        onDragCancel = onCardDragCancel,
                        onRowBoundsMeasured = { rect -> onRegisterRowBounds(rowIndex, rect) },
                        onSlotBoundsMeasured = { slotIndex, rect -> onRegisterSlotBounds(rowIndex, slotIndex, rect) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 2. ACTION BUTTONS BAR (Matches reference layout)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. AUTO SORT
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .shadow(3.dp, RoundedCornerShape(6.dp))
                    .border(
                        1.dp,
                        if (autoSortEnabled) EmeraldAccentLight else GoldPlaqueBorder.copy(alpha = 0.5f),
                        RoundedCornerShape(6.dp)
                    )
                    .clickable {
                        onToggleAutoSort?.invoke() ?: onSortClicked(SortMode.RANK)
                    }
                    .testTag("btn_auto_sort"),
                color = if (autoSortEnabled) Color(0xFF092E16) else Color(0xFF1C0D05),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .background(
                                if (autoSortEnabled) EmeraldPrimary else Color.Transparent,
                                RoundedCornerShape(3.dp)
                            )
                            .border(
                                1.dp,
                                if (autoSortEnabled) EmeraldAccentLight else GoldPlaqueBorder,
                                RoundedCornerShape(3.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (autoSortEnabled) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Auto Sort",
                        color = if (autoSortEnabled) EmeraldAccentLight else GoldPlaqueText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            // 2. PLAY ON
            val canPlayOn = isPlayerTurn && isPlayOrDiscardPhase
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .shadow(if (canPlayOn) 6.dp else 2.dp, RoundedCornerShape(6.dp))
                    .border(
                        1.dp,
                        if (canPlayOn) Color(0xFF4ADE80) else Color(0xFF2E2620),
                        RoundedCornerShape(6.dp)
                    )
                    .clickable {
                        onPlayOnClicked?.invoke()
                    }
                    .testTag("btn_action_play_on"),
                color = if (canPlayOn) Color(0xFF15803D) else Color(0xFF181310),
                shape = RoundedCornerShape(6.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "PLAY ON",
                        color = if (canPlayOn) Color.White else TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = when {
                            !player.isDown -> "Down required"
                            selectedCardIds.size == 1 -> "Tap to layoff"
                            else -> "Select 1 card"
                        },
                        color = if (canPlayOn) Color(0xFFDCFCE7) else Color(0xFF64748B),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            // 3. DISCARD
            val isDiscardActive = isPlayerTurn && isPlayOrDiscardPhase && canDiscard
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .shadow(if (isDiscardActive) 6.dp else 2.dp, RoundedCornerShape(6.dp))
                    .border(
                        1.dp,
                        if (isDiscardActive) Color(0xFFFBBF24) else Color(0xFF452408),
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { onDiscardClicked() }
                    .testTag("btn_action_discard"),
                color = if (isDiscardActive) Color(0xFFB45309) else Color(0xFF2A1505),
                shape = RoundedCornerShape(6.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "DISCARD",
                        color = if (isDiscardActive) Color.White else TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (selectedCardIds.size == 1) "Discard card" else "Select 1 card",
                        color = if (isDiscardActive) Color(0xFFFEF3C7) else Color(0xFF785434),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            // 4. GOING DOWN (Or BUY if priority is active)
            if (canBuy && onBuyClicked != null) {
                Surface(
                    modifier = Modifier
                        .weight(1.05f)
                        .height(46.dp)
                        .shadow(6.dp, RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF4ADE80), RoundedCornerShape(6.dp))
                        .clickable { onBuyClicked() }
                        .testTag("btn_action_buy"),
                    color = Color(0xFF15803D),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "BUY",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Buy discard + 1",
                            color = Color(0xFFDCFCE7),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            } else {
                val isGoDownEnabled = !player.isDown && isPlayerTurn && isPlayOrDiscardPhase
                Surface(
                    modifier = Modifier
                        .weight(1.05f)
                        .height(46.dp)
                        .shadow(if (canGoDown) 6.dp else 2.dp, RoundedCornerShape(6.dp))
                        .border(
                            1.dp,
                            if (canGoDown) Color(0xFFEF4444) else Color(0xFF451812),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable(enabled = isGoDownEnabled || !player.isDown) {
                            onGoDownClicked()
                        }
                        .testTag("btn_action_going_down"),
                    color = if (canGoDown) Color(0xFF7F1D1D) else Color(0xFF280E0B),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "GOING DOWN",
                            color = if (canGoDown) Color.White else Color(0xFFD18A8A),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            text = if (canGoDown) "Ready to Meld!" else "Go down / Meld",
                            color = if (canGoDown) Color(0xFFFEE2E2) else Color(0xFF8A5555),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TieredWoodenShelfRow(
    tierNumber: Int,
    slots: List<Card?>,
    selectedCardIds: Set<String>,
    isHovered: Boolean,
    targetSlotIndex: Int,
    activeDraggedCardId: String?,
    onCardClicked: (String) -> Unit,
    onDragStart: (Card, Int, Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onRowBoundsMeasured: (Rect) -> Unit,
    onSlotBoundsMeasured: (Int, Rect) -> Unit
) {
    val shelfBgGradient = Brush.verticalGradient(
        colors = listOf(
            if (isHovered) Color(0xFF0D331C) else Color(0xFF261006),
            if (isHovered) Color(0xFF071F11) else Color(0xFF180A04),
            if (isHovered) Color(0xFF04140B) else Color(0xFF0E0502)
        )
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .onGloballyPositioned { coords -> onRowBoundsMeasured(coords.boundsInRoot()) }
            .border(
                if (isHovered) 1.5.dp else 0.75.dp,
                if (isHovered) Color(0xFF22C55E) else Color(0xFF4A250B),
                RoundedCornerShape(6.dp)
            ),
        color = Color(0xFF120702),
        shape = RoundedCornerShape(6.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(shelfBgGradient)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val availableWidth = maxWidth
            val maxSlots = RACK_SLOTS_PER_ROW
            val cardWidth = 72.dp
            // Calculate step size so that maxSlots fit within availableWidth
            val step = if (maxSlots > 1) (availableWidth - cardWidth) / (maxSlots - 1).toFloat() else 0.dp

            slots.take(RACK_SLOTS_PER_ROW).forEachIndexed { slotIndex, card ->
                val isSlotHovered = isHovered && targetSlotIndex == slotIndex
                var slotRootBounds by remember { mutableStateOf(Rect.Zero) }

                Box(
                    modifier = Modifier
                        .offset(x = step * slotIndex)
                        .width(cardWidth)
                        .fillMaxHeight()
                        .onGloballyPositioned { coords ->
                            val bounds = coords.boundsInRoot()
                            slotRootBounds = bounds
                            onSlotBoundsMeasured(slotIndex, bounds)
                        }
                        .zIndex(if (selectedCardIds.contains(card?.id)) 50f else (slotIndex + 1).toFloat()),
                    contentAlignment = Alignment.Center
                ) {
                    if (card != null) {
                        val isSelected = selectedCardIds.contains(card.id)
                        val isBeingDragged = activeDraggedCardId == card.id
                        val elevationOffset by animateDpAsState(
                            targetValue = if (isSelected) (-8).dp else 0.dp,
                            label = "card_elevation"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .offset(y = elevationOffset)
                                .graphicsLayer {
                                    alpha = if (isBeingDragged) 0.25f else 1f
                                }
                                .border(
                                    if (isSelected) 3.dp else if (isSlotHovered) 2.dp else 0.dp,
                                    if (isSelected) Color(0xFFFFD700) else if (isSlotHovered) Color(0xFF22C55E) else Color.Transparent,
                                    RoundedCornerShape(4.dp)
                                )
                                .pointerInput(card.id) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        var isDragging = false
                                        var totalDrag = Offset.Zero
                                        val initialTouchPos = if (slotRootBounds.width > 0) {
                                            slotRootBounds.topLeft + down.position
                                        } else {
                                            down.position
                                        }

                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == down.id }
                                            if (change == null || !change.pressed) {
                                                if (isDragging) {
                                                    onDragEnd()
                                                } else {
                                                    onCardClicked(card.id)
                                                }
                                                break
                                            }
                                            val dragDelta = change.positionChange()
                                            totalDrag += dragDelta
                                            if (!isDragging && totalDrag.getDistance() > 8f) {
                                                isDragging = true
                                                onDragStart(card, slotIndex, initialTouchPos + totalDrag)
                                            }
                                            if (isDragging) {
                                                change.consume()
                                                onDragMove(dragDelta)
                                            }
                                        }
                                    }
                                }
                                .testTag("player_card_${card.id}")
                        ) {
                            PlayingCardView(
                                card = card,
                                isSelected = isSelected,
                                modifier = Modifier.fillMaxSize(),
                                showPointsBadge = false,
                                cardWidth = 72.dp,
                                cardHeight = 104.dp
                            )
                        }
                    } else {
                        // Empty Shelf Slot Groove
                        Box(
                            modifier = Modifier
                                .width(cardWidth * 0.7f)
                                .height(104.dp * 0.8f)
                                .background(
                                    if (isSlotHovered) Color(0x3322C55E) else Color(0x11000000),
                                    RoundedCornerShape(4.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSlotHovered) Color(0xFF22C55E) else Color(0x225A2E0F),
                                    RoundedCornerShape(4.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSlotHovered) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(Color(0xFF22C55E), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Drop here",
                                        tint = Color.Black,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
