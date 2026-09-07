package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
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
import com.example.model.Card
import com.example.model.Player
import com.example.ui.components.PlayingCardView
import com.example.ui.theme.*

@Composable
fun PocketDialog(
    player: Player,
    onMoveCardsToHand: (Set<String>) -> Unit,
    onMoveCardsToPocket: (Set<String>) -> Unit,
    onEmptyPocket: () -> Unit,
    onDismissRequest: () -> Unit
) {
    var selectedPocketCardIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedActiveCardIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val pocketCards = player.pocketCards
    val activeCards = player.activeCards

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .shadow(24.dp, RoundedCornerShape(14.dp))
                .border(
                    2.dp,
                    Brush.verticalGradient(
                        listOf(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF0C4A6E))
                    ),
                    RoundedCornerShape(14.dp)
                )
                .testTag("dialog_pocket"),
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // 1. Header
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
                                text = "POCKET WORKSPACE",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Surface(
                                color = Color(0xFF0369A1),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                            ) {
                                Text(
                                    text = "${pocketCards.size} POCKETED",
                                    color = Color(0xFFE0F2FE),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Private organization tray • Cards count toward hand total, card conservation & scoring",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.5.sp
                        )
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("btn_close_pocket")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFCBD5E1)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Action Bar for Pocket
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Move selected pocket cards to active hand
                    Button(
                        onClick = {
                            if (selectedPocketCardIds.isNotEmpty()) {
                                onMoveCardsToHand(selectedPocketCardIds)
                                selectedPocketCardIds = emptySet()
                            }
                        },
                        enabled = selectedPocketCardIds.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0284C7),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF1E293B),
                            disabledContentColor = Color(0xFF64748B)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("btn_pocket_move_to_hand")
                    ) {
                        Text(
                            text = if (selectedPocketCardIds.isNotEmpty()) "MOVE TO ACTIVE HAND (${selectedPocketCardIds.size})" else "SELECT TO MOVE TO HAND",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Move selected active cards into pocket
                    if (selectedActiveCardIds.isNotEmpty()) {
                        Button(
                            onClick = {
                                onMoveCardsToPocket(selectedActiveCardIds)
                                selectedActiveCardIds = emptySet()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF059669),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("btn_move_active_to_pocket")
                        ) {
                            Text(
                                text = "MOVE TO POCKET (${selectedActiveCardIds.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Empty Pocket / Unpocket All
                    OutlinedButton(
                        onClick = {
                            onEmptyPocket()
                            selectedPocketCardIds = emptySet()
                        },
                        enabled = pocketCards.isNotEmpty(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFF87171),
                            disabledContentColor = Color(0xFF475569)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (pocketCards.isNotEmpty()) Color(0xFFDC2626) else Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("btn_empty_pocket")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "UNPOCKET ALL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Pocketed Cards Section
                Text(
                    text = "POCKETED CARDS (${pocketCards.size})",
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.1f)
                        .border(1.dp, Color(0xFF0369A1), RoundedCornerShape(8.dp)),
                    color = Color(0xFF0B1329),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (pocketCards.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Your Pocket is currently empty.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Tuck cards here from your active hand below to organize your workspace.",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(68.dp),
                            contentPadding = PaddingValues(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(pocketCards, key = { it.id }) { card ->
                                val isSelected = selectedPocketCardIds.contains(card.id)
                                PlayingCardView(
                                    card = card,
                                    isSelected = isSelected,
                                    onClick = {
                                        selectedPocketCardIds = if (isSelected) {
                                            selectedPocketCardIds - card.id
                                        } else {
                                            selectedPocketCardIds + card.id
                                        }
                                    },
                                    cardWidth = 68.dp,
                                    cardHeight = 98.dp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Active Hand Cards Section (Quick tuck to Pocket)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE WORKING HAND (${activeCards.size})",
                        color = Color(0xFF34D399),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Tap cards to select & tuck to pocket",
                        color = Color(0xFF64748B),
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.9f)
                        .border(1.dp, Color(0xFF064E3B), RoundedCornerShape(8.dp)),
                    color = Color(0xFF062118),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (activeCards.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "All cards are currently in your Pocket.",
                                color = Color(0xFF6EE7B7),
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(64.dp),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(activeCards, key = { it.id }) { card ->
                                val isSelected = selectedActiveCardIds.contains(card.id)
                                PlayingCardView(
                                    card = card,
                                    isSelected = isSelected,
                                    onClick = {
                                        selectedActiveCardIds = if (isSelected) {
                                            selectedActiveCardIds - card.id
                                        } else {
                                            selectedActiveCardIds + card.id
                                        }
                                    },
                                    cardWidth = 64.dp,
                                    cardHeight = 92.dp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 5. Footer Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL HAND: ${player.hand.size} cards (${activeCards.size} Active + ${pocketCards.size} Pocket)",
                        color = Color(0xFFE2E8F0),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Scoring Points: ${player.pointsInHand}",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.5.sp
                    )
                }
            }
        }
    }
}
