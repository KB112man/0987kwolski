package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.engine.DeckEngine
import com.example.model.GameState
import com.example.ui.theme.*

@Composable
fun DeckStatsDialog(
    state: GameState,
    onDismiss: () -> Unit
) {
    val report = remember(state) {
        DeckEngine.generateIntegrityReport(state, "DeckStatsDialog")
    }

    val humanPlayer = state.humanPlayer
    val aiOpponents = state.aiOpponents

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .shadow(24.dp, RoundedCornerShape(16.dp))
                .border(
                    1.dp,
                    if (report.isValid) GlassBorderEmerald else Color(0xFFEF4444),
                    RoundedCornerShape(16.dp)
                )
                .testTag("dialog_deck_stats"),
            color = ModalSurface,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = if (report.isValid) EmeraldAccent else Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "108-CARD INVENTORY AUDIT",
                            color = if (report.isValid) EmeraldAccent else Color(0xFFEF4444),
                            fontSize = 14.5.sp,
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

                // Integrity Banner
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (report.isValid) Color(0xFF072414) else Color(0xFF330909),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (report.isValid) EmeraldAccentLight.copy(alpha = 0.5f) else Color(0xFFEF4444).copy(alpha = 0.6f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (report.isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (report.isValid) EmeraldAccentLight else Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (report.isValid) "108 / 108 — CARD STATE VALID" else "CARD ACCOUNTING ERROR",
                            color = if (report.isValid) EmeraldAccentLight else Color(0xFFFCA5A5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.4.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Authoritative Zone Breakdown
                Text(
                    text = "AUTHORITATIVE LOCATIONS",
                    color = EmeraldAccentLight,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF04140B), RoundedCornerShape(8.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StatRow("Stock / Draw Pile", "${state.drawDeck.size} cards", EmeraldAccentLight)
                    StatRow("Discard Pile", "${state.discardPile.size} cards", AmberAction)

                    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

                    // Human
                    if (humanPlayer != null) {
                        StatRow("Human (${humanPlayer.name}) Total", "${humanPlayer.cardCount} cards", TextPrimary)
                        StatRow("   └ Active Rack", "${humanPlayer.activeCards.size} cards", TextSecondary)
                        StatRow("   └ Pocket", "${humanPlayer.pocketCards.size} cards", EmeraldAccent)
                    }

                    // CPUs
                    aiOpponents.forEachIndexed { index, cpu ->
                        StatRow("CPU ${index + 1} (${cpu.name})", "${cpu.cardCount} cards", TextSecondary)
                    }

                    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

                    // Table Melds
                    StatRow("Table Melds (${state.allTableMelds.size} groups)", "${state.allTableMelds.sumOf { it.cards.size }} cards", GoldPlaqueText)

                    HorizontalDivider(color = GlassBorder, thickness = 0.75.dp)

                    // Audit Totals
                    StatRow("Total References", "${report.totalReferences} / 108", if (report.totalReferences == 108) Color.White else Color(0xFFEF4444))
                    StatRow("Unique IDs", "${report.uniqueIds} / 108", if (report.uniqueIds == 108) Color.White else Color(0xFFEF4444))
                    StatRow("Missing IDs", "${report.missingIds.size}", if (report.missingIds.isEmpty()) EmeraldAccentLight else Color(0xFFEF4444))
                    StatRow("Duplicated IDs", "${report.duplicatedIds.size}", if (report.duplicatedIds.isEmpty()) EmeraldAccentLight else Color(0xFFEF4444))
                }

                // If missing or duplicate details exist, show diagnostic breakdown
                if (report.missingIds.isNotEmpty() || report.duplicatedIds.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x33330909), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF7F1D1D), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        if (report.missingIds.isNotEmpty()) {
                            Text(
                                text = "Missing: ${report.missingIds.take(5).joinToString(", ")}${if (report.missingIds.size > 5) "..." else ""}",
                                color = Color(0xFFFCA5A5),
                                fontSize = 9.sp
                            )
                        }
                        if (report.duplicatedIds.isNotEmpty()) {
                            Text(
                                text = "Duplicated: ${report.duplicatedIds.take(5).joinToString(", ")}${if (report.duplicatedIds.size > 5) "..." else ""}",
                                color = Color(0xFFFCA5A5),
                                fontSize = 9.sp
                            )
                        }
                    }
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
