package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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

                // Content
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, Color(0xFF5A2E0F), RoundedCornerShape(8.dp)),
                    color = Color(0xFF190C04),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(72.dp),
                        contentPadding = PaddingValues(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(player.hand, key = { it.id }) { card ->
                            val isPocketed = card.id in player.pocketCardIds
                            Box {
                                PlayingCardView(
                                    card = card,
                                    isSelected = selectedCardIds.contains(card.id),
                                    isHighlighted = newlyReceivedCardIds.contains(card.id),
                                    onClick = { onCardClicked(card.id) },
                                    cardWidth = 72.dp,
                                    cardHeight = 104.dp
                                )
                                if (isPocketed) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(2.dp),
                                        color = Color(0xFF0369A1),
                                        shape = RoundedCornerShape(3.dp),
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF38BDF8))
                                    ) {
                                        Text(
                                            text = "POCKET",
                                            color = Color.White,
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
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
