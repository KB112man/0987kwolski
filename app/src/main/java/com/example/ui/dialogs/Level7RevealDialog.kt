package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine.MeldDetector
import com.example.model.*
import com.example.ui.components.PlayingCardView
import com.example.ui.theme.*

@Composable
fun Level7RevealDialog(
    player: Player,
    onConfirmWin: (List<List<Card>>) -> Unit,
    onDismiss: () -> Unit
) {
    val totalHandCards = remember(player.hand) { player.hand }
    val run1Cards = remember { mutableStateListOf<Card>() }
    val run2Cards = remember { mutableStateListOf<Card>() }
    val run3Cards = remember { mutableStateListOf<Card>() }
    val unassignedCards = remember { mutableStateListOf<Card>().apply { addAll(totalHandCards) } }

    var selectedRunSlot by remember { mutableIntStateOf(0) }
    var validationErrorMessage by remember { mutableStateOf<String?>(null) }
    var arrangementMessage by remember { mutableStateOf<String?>(null) }

    val validation = remember(run1Cards.toList(), run2Cards.toList(), run3Cards.toList(), unassignedCards.toList()) {
        MeldDetector.validateLevel7Reveal(
            run1Cards = run1Cards.toList(),
            run2Cards = run2Cards.toList(),
            run3Cards = run3Cards.toList(),
            unassignedCards = unassignedCards.toList(),
            totalHandCount = totalHandCards.size
        )
    }

    fun resetAll() {
        run1Cards.clear()
        run2Cards.clear()
        run3Cards.clear()
        unassignedCards.clear()
        unassignedCards.addAll(totalHandCards)
        validationErrorMessage = null
        arrangementMessage = null
    }

    fun autoArrange3Runs() {
        val workspace = MeldDetector.solveLevel7Arrangement(totalHandCards)
        val cardMap = totalHandCards.associateBy { it.id }

        run1Cards.clear()
        run1Cards.addAll(workspace.run1Ids.mapNotNull { cardMap[it] })

        run2Cards.clear()
        run2Cards.addAll(workspace.run2Ids.mapNotNull { cardMap[it] })

        run3Cards.clear()
        run3Cards.addAll(workspace.run3Ids.mapNotNull { cardMap[it] })

        unassignedCards.clear()
        unassignedCards.addAll(workspace.leftoverIds.mapNotNull { cardMap[it] })

        validationErrorMessage = null
        arrangementMessage = workspace.summaryMessage
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

    fun addCardToSlot(card: Card, slotIdx: Int) {
        unassignedCards.remove(card)
        when (slotIdx) {
            0 -> run1Cards.add(card)
            1 -> run2Cards.add(card)
            2 -> run3Cards.add(card)
        }
    }

    fun returnCardToUnassigned(card: Card, slotIdx: Int) {
        when (slotIdx) {
            0 -> run1Cards.remove(card)
            1 -> run2Cards.remove(card)
            2 -> run3Cards.remove(card)
        }
        unassignedCards.add(card)
    }

    fun clearSlot(slotIdx: Int) {
        when (slotIdx) {
            0 -> {
                unassignedCards.addAll(run1Cards)
                run1Cards.clear()
            }
            1 -> {
                unassignedCards.addAll(run2Cards)
                run2Cards.clear()
            }
            2 -> {
                unassignedCards.addAll(run3Cards)
                run3Cards.clear()
            }
        }
    }

    // Try auto-arranging initially if a valid winning state is already present!
    LaunchedEffect(Unit) {
        val initialWinningRuns = MeldDetector.findLevel7WinningRuns(totalHandCards)
        if (initialWinningRuns != null && initialWinningRuns.size == 3) {
            run1Cards.addAll(initialWinningRuns[0])
            run2Cards.addAll(initialWinningRuns[1])
            run3Cards.addAll(initialWinningRuns[2])
            unassignedCards.clear()
        }
    }

    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.97f)
                .fillMaxHeight(0.95f)
                .shadow(28.dp, RoundedCornerShape(14.dp))
                .border(
                    2.dp,
                    Brush.verticalGradient(
                        listOf(Color(0xFFF59E0B), Color(0xFFB45309), Color(0xFF451A03))
                    ),
                    RoundedCornerShape(14.dp)
                )
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1E0A02), Color(0xFF120501), Color(0xFF0A0301))
                    ),
                    RoundedCornerShape(14.dp)
                )
                .testTag("dialog_level7_reveal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // 1. Header Banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "THAT DID IT! — LEVEL 7 REVEAL",
                                color = Color(0xFFFDE68A),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Surface(
                                color = if (validation.isValid) Color(0xFF15803D) else Color(0xFF7F1D1D),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (validation.isValid) Color(0xFF86EFAC) else Color(0xFFF87171)
                                )
                            ) {
                                Text(
                                    text = if (validation.isValid) "READY TO WIN!" else "LEFTOVERS: ${validation.leftoverCount}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Level 7 Rule: Partition your entire hand into exactly 3 Runs • 0 leftover cards allowed • No final discard",
                            color = Color(0xFFFBBF24),
                            fontSize = 10.5.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { autoArrange3Runs() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF92400E)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp).testTag("btn_auto_arrange_level7")
                        ) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFFFDE68A))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AUTO-ARRANGE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFEF3C7))
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp).testTag("btn_close_level7_reveal")
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFE2E8F0))
                        }
                    }
                }

                if (validationErrorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = Color(0xFF7F1D1D),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = validationErrorMessage ?: "",
                            color = Color(0xFFFEE2E2),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else if (arrangementMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = if (validation.isValid) Color(0xFF064E3B) else Color(0xFF3B1C0A),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (validation.isValid) Color(0xFF10B981) else Color(0xFFD97706)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = arrangementMessage ?: "",
                            color = if (validation.isValid) Color(0xFFD1FAE5) else Color(0xFFFEF3C7),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Scrollable 3-Run Trays & Unassigned Cards
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Run Trays 1, 2, 3
                    Level7RunTray(
                        title = "RUN 1 (Min 4 Cards)",
                        cards = run1Cards,
                        slotIndex = 0,
                        isSelected = selectedRunSlot == 0,
                        validation = validation.run1Result,
                        onSelectSlot = { selectedRunSlot = 0 },
                        onCardClicked = { card -> returnCardToUnassigned(card, 0) },
                        onClearSlot = { clearSlot(0) }
                    )

                    Level7RunTray(
                        title = "RUN 2 (Min 4 Cards)",
                        cards = run2Cards,
                        slotIndex = 1,
                        isSelected = selectedRunSlot == 1,
                        validation = validation.run2Result,
                        onSelectSlot = { selectedRunSlot = 1 },
                        onCardClicked = { card -> returnCardToUnassigned(card, 1) },
                        onClearSlot = { clearSlot(1) }
                    )

                    Level7RunTray(
                        title = "RUN 3 (Min 4 Cards)",
                        cards = run3Cards,
                        slotIndex = 2,
                        isSelected = selectedRunSlot == 2,
                        validation = validation.run3Result,
                        onSelectSlot = { selectedRunSlot = 2 },
                        onCardClicked = { card -> returnCardToUnassigned(card, 2) },
                        onClearSlot = { clearSlot(2) }
                    )

                    // Remaining / Unassigned Cards Section
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF5A2E0F), RoundedCornerShape(8.dp)),
                        color = Color(0xFF140803),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
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
                                        text = "UNASSIGNED LEFTOVERS (${unassignedCards.size})",
                                        color = if (unassignedCards.isEmpty()) Color(0xFF86EFAC) else Color(0xFFFCA5A5),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    if (unassignedCards.isEmpty()) {
                                        Text("✓ Zero Leftovers", color = Color(0xFF86EFAC), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Text("• Tap card to add to Run ${selectedRunSlot + 1}", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedButton(
                                        onClick = { sortUnassignedByRank() },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Text("Rank", fontSize = 9.sp, color = Color(0xFFFDE68A))
                                    }
                                    OutlinedButton(
                                        onClick = { sortUnassignedBySuit() },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Text("Suit", fontSize = 9.sp, color = Color(0xFFFDE68A))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            if (unassignedCards.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .background(Color(0xFF092E16), RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "All cards in hand placed into 3 Runs! Tap 'THAT DID IT!' below to win.",
                                        color = Color(0xFF86EFAC),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    unassignedCards.forEach { card ->
                                        PlayingCardView(
                                            card = card,
                                            onClick = { addCardToSlot(card, selectedRunSlot) },
                                            cardWidth = 56.dp,
                                            cardHeight = 80.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Bottom Action Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(0.7f)
                            .height(44.dp)
                            .testTag("btn_cancel_level7_reveal"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("CANCEL", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { resetAll() },
                        modifier = Modifier
                            .weight(0.7f)
                            .height(44.dp)
                            .testTag("btn_reset_level7_reveal"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFBBF24)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RESET", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (validation.isValid) {
                                onConfirmWin(listOf(run1Cards.toList(), run2Cards.toList(), run3Cards.toList()))
                            } else {
                                validationErrorMessage = validation.errorMessage ?: "Full hand must be partitioned into 3 valid Runs with zero leftovers."
                            }
                        },
                        modifier = Modifier
                            .weight(1.6f)
                            .height(44.dp)
                            .shadow(if (validation.isValid) 8.dp else 2.dp, RoundedCornerShape(8.dp))
                            .testTag("btn_confirm_level7_win"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (validation.isValid) Color(0xFFB45309) else Color(0xFF3B1C0A),
                            contentColor = if (validation.isValid) Color(0xFFFEF3C7) else Color(0xFF785434)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.2.dp,
                            if (validation.isValid) Color(0xFFFDE68A) else Color(0xFF5A2E0F)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = if (validation.isValid) Icons.Default.CheckCircle else Icons.Default.Done,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (validation.isValid) "THAT DID IT! (WIN ROUND)" else "VALIDATE THAT DID IT!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Level7RunTray(
    title: String,
    cards: List<Card>,
    slotIndex: Int,
    isSelected: Boolean,
    validation: com.example.engine.MeldSlotValidationResult,
    onSelectSlot: () -> Unit,
    onCardClicked: (Card) -> Unit,
    onClearSlot: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                if (isSelected) 1.5.dp else 1.dp,
                if (isSelected) Color(0xFFFBBF24) else Color(0xFF452408),
                RoundedCornerShape(8.dp)
            )
            .clickable { onSelectSlot() },
        color = if (isSelected) Color(0xFF221105) else Color(0xFF190C04),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = onSelectSlot,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFFFBBF24),
                            unselectedColor = Color(0xFF785434)
                        ),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = title,
                        color = if (isSelected) Color(0xFFFDE68A) else Color(0xFFCBD5E1),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "(${cards.size} cards)",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (cards.isEmpty()) {
                        Surface(
                            color = Color(0xFF2E2620),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "EMPTY TRAY",
                                color = Color(0xFF94A3B8),
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    } else if (validation.isValid) {
                        Surface(
                            color = Color(0xFF092E16),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4ADE80))
                        ) {
                            Text(
                                text = "✓ VALID RUN",
                                color = Color(0xFF86EFAC),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    } else {
                        Surface(
                            color = Color(0xFF7F1D1D),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444))
                        ) {
                            Text(
                                text = "INVALID (${validation.statusText})",
                                color = Color(0xFFFEE2E2),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }

                    if (cards.isNotEmpty()) {
                        IconButton(
                            onClick = onClearSlot,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (cards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(Color(0x33000000), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSelected) "Active tray: Tap cards below to place here" else "Tap tray to select as target",
                        color = Color(0xFF64748B),
                        fontSize = 10.5.sp
                    )
                }
            } else {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    cards.forEach { card ->
                        PlayingCardView(
                            card = card,
                            onClick = { onCardClicked(card) },
                            cardWidth = 56.dp,
                            cardHeight = 80.dp
                        )
                    }
                }
            }
        }
    }
}
