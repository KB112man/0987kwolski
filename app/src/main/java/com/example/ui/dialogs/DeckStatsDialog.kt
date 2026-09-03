package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.GameState
import com.example.ui.theme.*

@Composable
fun DeckStatsDialog(
    state: GameState,
    onDismiss: () -> Unit
) {
    val totalInPlay = state.players.sumOf { it.cardCount } +
            state.drawDeck.size +
            state.discardPile.size +
            state.allTableMelds.sumOf { it.cards.size }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .shadow(24.dp, RoundedCornerShape(16.dp))
                .border(1.dp, GlassBorderEmerald, RoundedCornerShape(16.dp))
                .testTag("dialog_deck_stats"),
            color = ModalSurface,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "108-CARD DOUBLE DECK",
                            color = EmeraldAccent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }
                Text(
                    text = "Standard 2 Decks (104) + 4 Jokers = 108 Cards",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF04140B), RoundedCornerShape(8.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatRow("Stock / Draw Pile", "${state.drawDeck.size} cards", EmeraldAccentLight)
                    StatRow("Discard Pile", "${state.discardPile.size} cards", AmberAction)
                    StatRow("Cards in Players' Hands", "${state.players.sumOf { it.cardCount }} cards", TextPrimary)
                    StatRow("Cards on Table Melds", "${state.allTableMelds.sumOf { it.cards.size }} cards", EmeraldAccent)
                    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)
                    StatRow("Total Accounted For", "$totalInPlay / 108 cards", Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "CARD POINT VALUES",
                    color = EmeraldAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x1A000000), RoundedCornerShape(6.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PointRow("Jokers (Wild)", "20 pts each")
                    PointRow("Aces", "15 pts each")
                    PointRow("Face Cards (K, Q, J) & 10s", "10 pts each")
                    PointRow("Number Cards (2 - 9)", "5 pts each")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F3B24),
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderEmerald)
                ) {
                    Text("CLOSE", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
        Text(text = value, color = valueColor, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PointRow(label: String, pts: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextPrimary, fontSize = 10.5.sp)
        Text(text = pts, color = AmberAction, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
    }
}
