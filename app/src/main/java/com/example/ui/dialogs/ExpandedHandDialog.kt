package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Player
import com.example.model.SortMode
import com.example.ui.components.PlayingCardView
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldPlaqueBorder

@Composable
fun ExpandedHandDialog(
    player: Player,
    selectedCardIds: Set<String>,
    newlyReceivedCardIds: Set<String>,
    onCardClicked: (String) -> Unit,
    onSortClicked: (SortMode) -> Unit,
    onMoveCardsToPocket: (Set<String>) -> Unit,
    onMoveCardsToHand: (Set<String>) -> Unit,
    onSwapCards: (String, String) -> Unit,
    onEmptyPocket: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val selectedPocketCards = selectedCardIds.filter { it in player.pocketCardIds }.toSet()
    val selectedActiveCards = selectedCardIds.filter { it !in player.pocketCardIds }.toSet()

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            color = Color(0xFF140702),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "EXPANDED HAND WORKSPACE",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Total Hand: ${player.hand.size} cards • ${player.activeCards.size} Active • ${player.pocketCards.size} Pocket",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp
                        )
                    }
                    Button(
                        onClick = onDismissRequest,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("CLOSE", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Toolbar: Sort buttons (moved from main table) + Pocket management actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sort Rank
                    OutlinedButton(
                        onClick = { onSortClicked(SortMode.RANK) },
                        modifier = Modifier.height(36.dp).testTag("btn_expanded_sort_rank"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFF7ED)),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPlaqueBorder.copy(alpha = 0.6f))
                    ) {
                        Text("SORT RANK", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }

                    // Sort Suit
                    OutlinedButton(
                        onClick = { onSortClicked(SortMode.SUIT) },
                        modifier = Modifier.height(36.dp).testTag("btn_expanded_sort_suit"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFF7ED)),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPlaqueBorder.copy(alpha = 0.6f))
                    ) {
                        Text("SORT SUIT", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Move to Pocket
                    if (selectedActiveCards.isNotEmpty()) {
                        Button(
                            onClick = { onMoveCardsToPocket(selectedActiveCards) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(36.dp).testTag("btn_expanded_to_pocket")
                        ) {
                            Text("POCKET (${selectedActiveCards.size})", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Swap Cards
                    if (selectedActiveCards.size == 2) {
                        Button(
                            onClick = {
                                val list = selectedActiveCards.toList()
                                onSwapCards(list[0], list[1])
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(36.dp).testTag("btn_expanded_swap")
                        ) {
                            Text("SWAP (2)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Move to Active Hand
                    if (selectedPocketCards.isNotEmpty()) {
                        Button(
                            onClick = { onMoveCardsToHand(selectedPocketCards) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(36.dp).testTag("btn_expanded_to_hand")
                        ) {
                            Text("UNPOCKET (${selectedPocketCards.size})", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Empty Pocket
                    if (player.pocketCards.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onEmptyPocket,
                            modifier = Modifier.height(36.dp).testTag("btn_expanded_empty_pocket"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCA5A5)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("UNPOCKET ALL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Pocket Tray Section (if player has pocketed cards)
                if (player.pocketCards.isNotEmpty()) {
                    RegionContainer(
                        title = "POCKET TRAY (${player.pocketCards.size} cards stored)",
                        color = Color(0xFF03223F),
                        borderColor = Color(0xFF38BDF8),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            player.pocketCards.forEach { card ->
                                PlayingCardView(
                                    card = card,
                                    isSelected = selectedCardIds.contains(card.id),
                                    isHighlighted = newlyReceivedCardIds.contains(card.id),
                                    onClick = { onCardClicked(card.id) },
                                    cardWidth = 72.dp,
                                    cardHeight = 104.dp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Content
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, Color(0xFF5A2E0F), RoundedCornerShape(8.dp)),
                    color = Color(0xFF190C04),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    ActiveHandGrid(
                        activeCards = player.activeCards,
                        selectedCardIds = selectedCardIds,
                        newlyReceivedCardIds = newlyReceivedCardIds,
                        onCardClicked = onCardClicked
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Hand: ${player.hand.size} Cards (${player.activeCards.size} Active, ${player.pocketCards.size} Pocket)",
                        color = Color(0xFFDCFCE7),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Scoring: ${player.pointsInHand} pts",
                        color = Color(0xFFFDE68A),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveHandGrid(
    activeCards: List<com.example.model.Card>,
    selectedCardIds: Set<String>,
    newlyReceivedCardIds: Set<String>,
    onCardClicked: (String) -> Unit
) {
    val cardWidth = 72.dp
    val cardHeight = 104.dp
    val hSpacing = 8.dp
    val vSpacing = 10.dp

    val rackBg = Color(0xFF0F172A) // Dark Slate
    val rackBorder = Color(0xFF38BDF8) // Light Blue
    
    val overflowBg = Color(0xFF2C1515) // Dark Red/Brown
    val overflowBorder = Color(0xFFF87171) // Light Red

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(vSpacing)
    ) {
        val totalCards = activeCards.size
        // Always show at least 4 rows to clearly demonstrate Table Rack and Overflow regions
        val rowCount = maxOf(4, (totalCards + 7) / 8)

        for (rowIndex in 0 until rowCount) {
            val startIndex = rowIndex * 8
            val rowCards = if (startIndex < totalCards) activeCards.subList(startIndex, minOf(startIndex + 8, totalCards)) else emptyList()

            when (rowIndex) {
                0, 1 -> {
                    // Full Table Rack Row
                    RegionContainer(
                        title = if (rowIndex == 0) "TABLE RACK (Compact Table Slots 1-20)" else null,
                        color = rackBg,
                        borderColor = rackBorder
                    ) {
                        CardRow(rowCards, 8, cardWidth, cardHeight, hSpacing, selectedCardIds, newlyReceivedCardIds, onCardClicked)
                    }
                }
                2 -> {
                    // Row 3: Split Table Rack (4) and Overflow (4)
                    val rackCards = rowCards.take(4)
                    val overflowCards = rowCards.drop(4)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(0.dp) // The padding trick perfectly spaces them 8dp apart
                    ) {
                        RegionContainer(
                            title = "(Table Rack cont.)",
                            color = rackBg,
                            borderColor = rackBorder,
                            contentPadding = PaddingValues(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
                        ) {
                            CardRow(rackCards, 4, cardWidth, cardHeight, hSpacing, selectedCardIds, newlyReceivedCardIds, onCardClicked)
                        }

                        RegionContainer(
                            title = "OVERFLOW (Active Hand 21+)",
                            color = overflowBg,
                            borderColor = overflowBorder,
                            contentPadding = PaddingValues(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                        ) {
                            CardRow(overflowCards, 4, cardWidth, cardHeight, hSpacing, selectedCardIds, newlyReceivedCardIds, onCardClicked)
                        }
                    }
                }
                else -> {
                    // Full Overflow Row
                    RegionContainer(
                        title = if (rowIndex == 3) null else null, // Title is already on Row 3 right half
                        color = overflowBg,
                        borderColor = overflowBorder
                    ) {
                        CardRow(rowCards, 8, cardWidth, cardHeight, hSpacing, selectedCardIds, newlyReceivedCardIds, onCardClicked)
                    }
                }
            }
        }
    }
}

@Composable
fun RegionContainer(
    title: String?,
    color: Color,
    borderColor: Color,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = color,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(contentPadding)) {
            if (title != null) {
                Text(
                    text = title,
                    color = borderColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(20.dp)) // Maintain vertical alignment with titled rows
            }
            content()
        }
    }
}

@Composable
fun CardRow(
    cards: List<com.example.model.Card>,
    maxSlots: Int,
    cardWidth: androidx.compose.ui.unit.Dp,
    cardHeight: androidx.compose.ui.unit.Dp,
    hSpacing: androidx.compose.ui.unit.Dp,
    selectedCardIds: Set<String>,
    newlyReceivedCardIds: Set<String>,
    onCardClicked: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(hSpacing)) {
        cards.forEach { card ->
            Box(contentAlignment = Alignment.Center) {
                PlayingCardView(
                    card = card,
                    isSelected = selectedCardIds.contains(card.id),
                    isHighlighted = newlyReceivedCardIds.contains(card.id),
                    onClick = { onCardClicked(card.id) },
                    cardWidth = cardWidth,
                    cardHeight = cardHeight
                )
            }
        }
        repeat(maxSlots - cards.size) {
            Box(
                modifier = Modifier
                    .width(cardWidth)
                    .height(cardHeight)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            )
        }
    }
}
