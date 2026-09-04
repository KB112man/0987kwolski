package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.example.engine.MeldDetector
import com.example.engine.MeldSlotDefinition
import com.example.engine.MeldSlotValidationResult
import com.example.model.Card
import com.example.model.ContractLevel
import com.example.model.Meld
import com.example.model.MeldType
import com.example.model.Player
import com.example.ui.components.PlayingCardView
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun MeldBuilderDialog(
    player: Player,
    contractLevel: ContractLevel,
    onConfirmGoDown: (List<List<Card>>) -> Unit,
    onDismiss: () -> Unit
) {
    val slotDefinitions = remember(contractLevel) {
        MeldDetector.getContractSlotDefinitions(contractLevel)
    }

    val meldSlots = remember(contractLevel, player) {
        val list = mutableStateListOf<MutableList<Card>>()
        for (i in slotDefinitions.indices) {
            list.add(mutableStateListOf())
        }
        list
    }

    val unassignedCards = remember(player) {
        mutableStateListOf<Card>().apply { addAll(player.hand) }
    }

    var activeSlotIndex by remember { mutableIntStateOf(0) }
    var validationErrorMessage by remember { mutableStateOf<String?>(null) }
    var draggedCard by remember { mutableStateOf<Card?>(null) }
    var dragGlobalPosition by remember { mutableStateOf<Offset?>(null) }
    var hoveredSlotIndex by remember { mutableIntStateOf(-1) }
    val slotBounds = remember { mutableStateMapOf<Int, Rect>() }

    fun moveCardToSlot(card: Card, slotIdx: Int) {
        if (!unassignedCards.any { it.id == card.id }) return
        val cardToMove = unassignedCards.first { it.id == card.id }
        unassignedCards.remove(cardToMove)
        meldSlots[slotIdx].add(cardToMove)
        validationErrorMessage = null
    }

    fun removeCardFromSlot(slotIdx: Int, card: Card) {
        val removed = meldSlots[slotIdx].removeAll { it.id == card.id }
        if (removed) {
            unassignedCards.add(card)
            validationErrorMessage = null
        }
    }

    fun clearSlot(slotIdx: Int) {
        val cards = meldSlots[slotIdx].toList()
        meldSlots[slotIdx].clear()
        unassignedCards.addAll(cards)
        validationErrorMessage = null
    }

    fun resetAll() {
        for (slot in meldSlots) {
            unassignedCards.addAll(slot)
            slot.clear()
        }
        validationErrorMessage = null
    }

    fun autoFillContract() {
        resetAll()
        val foundMelds = MeldDetector.findValidContract(
            player.hand,
            contractLevel,
            player.id,
            player.name
        )
        if (foundMelds != null && foundMelds.size <= slotDefinitions.size) {
            foundMelds.forEachIndexed { idx, meld ->
                if (idx < meldSlots.size) {
                    val cardsToMove = meld.cards
                    unassignedCards.removeAll { c -> cardsToMove.any { it.id == c.id } }
                    meldSlots[idx].addAll(cardsToMove)
                }
            }
            validationErrorMessage = null
        } else {
            validationErrorMessage = "No auto-combination found. Build combinations manually using your cards below."
        }
    }

    fun sortUnassignedByRank() {
        val sorted = unassignedCards.sortedWith(
            compareBy<Card> { if (it.isWild) 100 else it.rank.value }
                .thenBy { it.suit.ordinal }
        )
        unassignedCards.clear()
        unassignedCards.addAll(sorted)
    }

    fun sortUnassignedBySuit() {
        val sorted = unassignedCards.sortedWith(
            compareBy<Card> { if (it.isWild) 100 else it.suit.ordinal }
                .thenBy { it.rank.value }
        )
        unassignedCards.clear()
        unassignedCards.addAll(sorted)
    }

    val hasValidContractAvailable = remember(player.hand, contractLevel) {
        MeldDetector.findValidContract(player.hand, contractLevel, player.id, player.name) != null
    }

    val slotValidations = remember(meldSlots.map { it.toList() }) {
        slotDefinitions.mapIndexed { idx, def ->
            MeldDetector.evaluateSingleSlot(
                cards = meldSlots[idx],
                targetType = def.type,
                minSize = def.minSize
            )
        }
    }

    val verifiedMelds = remember(meldSlots.map { it.toList() }) {
        MeldDetector.validateContract(
            groups = meldSlots.map { it.toList() },
            level = contractLevel,
            playerId = player.id,
            playerName = player.name
        )
    }
    val isContractComplete = verifiedMelds != null

    fun isLegalDestination(card: Card?, slotIdx: Int): Boolean {
        if (card == null) return false
        val def = slotDefinitions.getOrNull(slotIdx) ?: return false
        val existingCards = meldSlots[slotIdx]
        if (existingCards.isEmpty()) return true
        val testCards = existingCards + card
        val naturals = testCards.filter { !it.isWild }
        val wilds = testCards.filter { it.isWild }
        if (naturals.size <= wilds.size && wilds.isNotEmpty()) return false

        return when (def.type) {
            MeldType.BOOK -> {
                val bookRank = existingCards.find { !it.isWild }?.rank
                if (card.isWild) true
                else if (bookRank != null) card.rank == bookRank
                else true
            }
            MeldType.RUN -> {
                val runSuit = existingCards.find { !it.isWild }?.suit
                if (card.isWild) true
                else if (runSuit != null && card.suit != runSuit) false
                else {
                    if (testCards.size >= def.minSize) {
                        Meld.determineRunRange(testCards, def.minSize) != null
                    } else {
                        val naturalRanks = naturals.map { if (it.rank == com.example.model.Rank.ACE) 1 else it.rank.value }
                        val isUnique = naturalRanks.toSet().size == naturalRanks.size
                        val span = (naturalRanks.maxOrNull() ?: 0) - (naturalRanks.minOrNull() ?: 0) + 1
                        isUnique && span <= def.minSize + 2
                    }
                }
            }
        }
    }

    val workspaceScrollState = rememberScrollState()
    val density = LocalDensity.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .shadow(28.dp, RoundedCornerShape(14.dp))
                .border(
                    2.dp,
                    Brush.verticalGradient(
                        listOf(Color(0xFF8B4D20), Color(0xFF4A250E), Color(0xFF1E0A03))
                    ),
                    RoundedCornerShape(14.dp)
                )
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF0F3E22), Color(0xFF092916), Color(0xFF05180D))
                    ),
                    RoundedCornerShape(14.dp)
                )
                .testTag("dialog_meld_builder")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. TABLETOP HEADER
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(8.dp))
                        .border(1.dp, GoldPlaqueBorder.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                    color = Color(0xFF190C05),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF381B09), Color(0xFF220E04), Color(0xFF140702))
                                )
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DashboardCustomize,
                                    contentDescription = null,
                                    tint = GoldPlaqueText,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "BUILD YOUR MELDS",
                                    color = GoldPlaqueText,
                                    fontSize = 13.5.sp,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Text(
                                text = "Level ${contractLevel.levelNumber} – ${contractLevel.shortRequirement} (Deal ${contractLevel.dealCount} cards)",
                                color = Color(0xFFFFF7ED),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { autoFillContract() },
                                modifier = Modifier
                                    .height(32.dp)
                                    .shadow(if (hasValidContractAvailable) 4.dp else 1.dp, RoundedCornerShape(6.dp))
                                    .testTag("btn_auto_fill_melds"),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (hasValidContractAvailable) EmeraldPrimary else Color(0xFF153320),
                                    contentColor = if (hasValidContractAvailable) Color.Black else TextSecondary
                                ),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (hasValidContractAvailable) EmeraldAccentLight else Color(0xFF265937)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoFixHigh,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "AUTO-FILL",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.3.sp
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF2A1206), RoundedCornerShape(6.dp))
                                    .border(0.75.dp, Color(0xFF5A2E0F), RoundedCornerShape(6.dp))
                                    .testTag("btn_close_meld_builder")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color(0xFFCBD5E1),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 2. MELD WORKSPACE
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(workspaceScrollState)
                            .padding(vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            slotDefinitions.forEachIndexed { slotIdx, def ->
                                val cardsInSlot = meldSlots[slotIdx]
                                val validation = slotValidations[slotIdx]
                                val isActive = activeSlotIndex == slotIdx
                                val isHovered = hoveredSlotIndex == slotIdx
                                val isLegalForDragged = isLegalDestination(draggedCard, slotIdx)

                                MeldSlotTray(
                                    definition = def,
                                    cards = cardsInSlot,
                                    validation = validation,
                                    isActive = isActive,
                                    isHovered = isHovered,
                                    isLegalMatch = isLegalForDragged,
                                    onPositioned = { rect -> slotBounds[slotIdx] = rect },
                                    onCardClicked = { card -> removeCardFromSlot(slotIdx, card) },
                                    onClearSlotClicked = { clearSlot(slotIdx) },
                                    onSlotSelected = { activeSlotIndex = slotIdx },
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .widthIn(min = 220.dp)
                                )
                            }
                        }

                        if (validationErrorMessage != null) {
                            Surface(
                                color = Color(0xFF3B1214),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFFCA5A5),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = validationErrorMessage ?: "",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 3. PLAYER HAND / CARD POOL
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(10.dp))
                        .border(
                            1.5.dp,
                            Brush.verticalGradient(listOf(Color(0xFF7C3F1D), Color(0xFF2C1406))),
                            RoundedCornerShape(10.dp)
                        ),
                    color = Color(0xFF190C05),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF2C1408), Color(0xFF1C0C04), Color(0xFF120702))
                                )
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "YOUR HAND",
                                    color = GoldPlaqueText,
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                                Surface(
                                    color = Color(0xFF0F3B24),
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.75.dp, EmeraldAccentLight.copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = "${unassignedCards.size} cards available",
                                        color = EmeraldAccentLight,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                    )
                                }
                                val activeDef = slotDefinitions.getOrNull(activeSlotIndex)
                                if (activeDef != null) {
                                    Surface(
                                        color = Color(0xFF2A1506),
                                        shape = RoundedCornerShape(4.dp),
                                        border = androidx.compose.foundation.BorderStroke(0.75.dp, GoldPlaqueBorder)
                                    ) {
                                        Text(
                                            text = "Target: ${if (activeDef.type == MeldType.BOOK) "Book" else "Run"} ${activeDef.slotIndex + 1}",
                                            color = GoldPlaqueText,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { sortUnassignedByRank() },
                                    modifier = Modifier.height(26.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFF7ED)),
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPlaqueBorder.copy(alpha = 0.6f))
                                ) {
                                    Text("Sort Rank", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { sortUnassignedBySuit() },
                                    modifier = Modifier.height(26.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFF7ED)),
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPlaqueBorder.copy(alpha = 0.6f))
                                ) {
                                    Text("Sort Suit", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        if (unassignedCards.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                                    .background(Color(0x22000000), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0x335A2E0F), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "All cards placed into meld trays above. Tap 'CONFIRM / GO DOWN' when complete.",
                                    color = EmeraldAccentLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            val handScroll = rememberScrollState()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(76.dp)
                                    .background(Color(0x33000000), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0x335A2E0F), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                    .horizontalScroll(handScroll),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val cardW = 50.dp
                                val cardH = 70.dp
                                unassignedCards.forEach { card ->
                                    val isBeingDragged = draggedCard?.id == card.id
                                    var cardRootBounds by remember { mutableStateOf(Rect.Zero) }

                                    Box(
                                        modifier = Modifier
                                            .width(cardW)
                                            .height(cardH)
                                            .zIndex(if (isBeingDragged) 100f else 1f)
                                            .onGloballyPositioned { coords ->
                                                cardRootBounds = coords.boundsInRoot()
                                            }
                                            .graphicsLayer {
                                                alpha = if (isBeingDragged) 0.3f else 1f
                                            }
                                            .pointerInput(card.id) {
                                                awaitEachGesture {
                                                    val down = awaitFirstDown(requireUnconsumed = false)
                                                    var isDragging = false
                                                    var totalDrag = Offset.Zero
                                                    val initialTouch = if (cardRootBounds.width > 0) {
                                                        cardRootBounds.topLeft + down.position
                                                    } else {
                                                        down.position
                                                    }

                                                    while (true) {
                                                        val event = awaitPointerEvent()
                                                        val change = event.changes.firstOrNull { it.id == down.id }
                                                        if (change == null || !change.pressed) {
                                                            if (isDragging) {
                                                                val activeCard = draggedCard
                                                                val targetSlot = if (hoveredSlotIndex in meldSlots.indices) {
                                                                    hoveredSlotIndex
                                                                } else {
                                                                    activeSlotIndex
                                                                }
                                                                if (activeCard != null && targetSlot in meldSlots.indices) {
                                                                    moveCardToSlot(activeCard, targetSlot)
                                                                }
                                                            } else {
                                                                moveCardToSlot(card, activeSlotIndex)
                                                            }
                                                            draggedCard = null
                                                            dragGlobalPosition = null
                                                            hoveredSlotIndex = -1
                                                            break
                                                        }
                                                        val dragDelta = change.positionChange()
                                                        totalDrag += dragDelta
                                                        if (!isDragging && totalDrag.getDistance() > 8f) {
                                                            isDragging = true
                                                            draggedCard = card
                                                            dragGlobalPosition = initialTouch + totalDrag
                                                        }
                                                        if (isDragging) {
                                                            change.consume()
                                                            val currentPos = (dragGlobalPosition ?: initialTouch) + dragDelta
                                                            dragGlobalPosition = currentPos

                                                            var foundSlot = -1
                                                            for ((sIdx, sRect) in slotBounds) {
                                                                if (sRect.inflate(18f).contains(currentPos)) {
                                                                    foundSlot = sIdx
                                                                    break
                                                                }
                                                            }
                                                            hoveredSlotIndex = foundSlot
                                                        }
                                                    }
                                                }
                                            }
                                            .testTag("unassigned_card_${card.id}")
                                    ) {
                                        PlayingCardView(
                                            card = card,
                                            cardWidth = cardW,
                                            cardHeight = cardH,
                                            showPointsBadge = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4. BOTTOM ACTION BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .height(44.dp)
                            .weight(0.85f)
                            .testTag("btn_cancel_meld_builder"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1)),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF5A2E0F))
                    ) {
                        Text("CANCEL", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { resetAll() },
                        modifier = Modifier
                            .height(44.dp)
                            .weight(0.85f)
                            .testTag("btn_reset_meld_builder"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberAction),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, AmberAction.copy(alpha = 0.7f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RESET", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (isContractComplete) {
                                onConfirmGoDown(meldSlots.map { it.toList() })
                            } else {
                                val invalidSlot = slotValidations.indexOfFirst { !it.isValid }
                                validationErrorMessage = if (invalidSlot != -1) {
                                    val def = slotDefinitions[invalidSlot]
                                    val valResult = slotValidations[invalidSlot]
                                    "${def.title}: ${valResult.detailMessage}"
                                } else {
                                    "Combinations do not meet full contract (${contractLevel.shortRequirement}). Natural cards must strictly exceed wilds."
                                }
                            }
                        },
                        modifier = Modifier
                            .height(44.dp)
                            .weight(1.5f)
                            .shadow(if (isContractComplete) 8.dp else 2.dp, RoundedCornerShape(8.dp))
                            .testTag("btn_confirm_go_down"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isContractComplete) EmeraldPrimary else Color(0xFF163E27),
                            contentColor = if (isContractComplete) Color.Black else TextSecondary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.2.dp,
                            if (isContractComplete) Color(0xFF86EFAC) else Color(0xFF2D6A45)
                        )
                    ) {
                        Icon(
                            imageVector = if (isContractComplete) Icons.Default.CheckCircle else Icons.Default.Done,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isContractComplete) "CONFIRM / GO DOWN" else "VALIDATE & GO DOWN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            // 5. FLOATING DRAGGED CARD OVERLAY
            draggedCard?.let { card ->
                val pos = dragGlobalPosition
                if (pos != null) {
                    val cardWidthDp = 52.dp
                    val cardHeightDp = 74.dp
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
        }
    }
}

@Composable
private fun MeldSlotTray(
    definition: MeldSlotDefinition,
    cards: List<Card>,
    validation: MeldSlotValidationResult,
    isActive: Boolean,
    isHovered: Boolean,
    isLegalMatch: Boolean,
    onPositioned: (Rect) -> Unit,
    onCardClicked: (Card) -> Unit,
    onClearSlotClicked: () -> Unit,
    onSlotSelected: () -> Unit,
    modifier: Modifier = Modifier,
    RaidSlotTray: Boolean = false
) {
    val scrollState = rememberScrollState()
    val trayBorderColor = when {
        isHovered -> Color(0xFF22C55E)
        isActive -> Color(0xFFFFD700)
        isLegalMatch -> Color(0xFF34D399)
        validation.isValid -> Color(0xFF10B981)
        cards.isNotEmpty() -> Color(0xFFF59E0B)
        else -> Color(0xFF5A2E0F)
    }
    val trayBackground = when {
        isHovered -> Color(0xFF0F3B22)
        isActive -> Color(0xFF1F1106)
        validation.isValid -> Color(0xFF092916)
        cards.isNotEmpty() -> Color(0xFF1E1407)
        else -> Color(0xFF140903)
    }
    val typeLabel = if (definition.type == MeldType.BOOK) "BOOK" else "RUN"
    val slotNumber = definition.slotIndex + 1

    Surface(
        modifier = modifier
            .onGloballyPositioned { coords -> onPositioned(coords.boundsInRoot()) }
            .clickable { onSlotSelected() }
            .shadow(if (isActive || isHovered) 8.dp else 3.dp, RoundedCornerShape(8.dp))
            .border(
                if (isActive || isHovered) 2.dp else if (isLegalMatch || validation.isValid) 1.5.dp else 1.dp,
                trayBorderColor,
                RoundedCornerShape(8.dp)
            )
            .testTag("meld_slot_${definition.slotIndex}"),
        shape = RoundedCornerShape(8.dp),
        color = trayBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$typeLabel $slotNumber",
                        color = if (isActive) GoldPlaqueText else Color(0xFFFFF7ED),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Black
                    )
                    if (isActive) {
                        Surface(
                            color = Color(0xFFFFD700),
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                color = Color.Black,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    if (isLegalMatch && !isActive && cards.isNotEmpty()) {
                        Surface(
                            color = Color(0xFF15803D),
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Text(
                                text = "CAN ADD",
                                color = Color(0xFFDCFCE7),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "${cards.size} / ${definition.minSize}",
                    color = if (validation.isValid) Color(0xFF86EFAC) else TextSecondary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (cards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(Color(0x28000000), RoundedCornerShape(6.dp))
                        .border(
                            1.dp,
                            if (isHovered) Color(0xFF22C55E) else if (isActive) GoldPlaqueBorder.copy(alpha = 0.5f) else Color(0x225A2E0F),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isHovered) "DROP HERE" else "DROP CARDS HERE",
                        color = if (isHovered) EmeraldAccentLight else TextMuted,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .background(Color(0x22000000), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0x18000000), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(if (cards.size > 4) (-14).dp else (-8).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    cards.forEachIndexed { cIdx, card ->
                        Box(
                            modifier = Modifier
                                .clickable { onCardClicked(card) }
                                .zIndex(cIdx.toFloat())
                        ) {
                            PlayingCardView(
                                card = card,
                                cardWidth = 56.dp,
                                cardHeight = 80.dp,
                                showPointsBadge = false
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    validation.isValid -> {
                        Surface(
                            color = Color(0xFF15803D),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4ADE80))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFFDCFCE7),
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = "COMPLETE",
                                    color = Color(0xFFDCFCE7),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                    cards.isEmpty() -> {
                        Text(
                            text = "min ${definition.minSize} ${if (definition.type == MeldType.BOOK) "same rank" else "in run"}",
                            color = TextMuted,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    cards.size < definition.minSize -> {
                        Surface(
                            color = Color(0xFF451A03),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
                        ) {
                            Text(
                                text = "NEEDS ${definition.minSize - cards.size} MORE",
                                color = Color(0xFFFDE68A),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                    else -> {
                        Surface(
                            color = Color(0xFF7F1D1D),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444))
                        ) {
                            Text(
                                text = "INVALID $typeLabel",
                                color = Color(0xFFFEE2E2),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                }

                if (cards.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .clickable { onClearSlotClicked() }
                            .padding(start = 2.dp),
                        color = Color(0xFF2A1206),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(0.75.dp, Color(0xFF5A2E0F))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = TextSecondary,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "Clear",
                                color = TextSecondary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
