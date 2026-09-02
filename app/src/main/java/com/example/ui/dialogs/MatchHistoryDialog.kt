package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.MatchHistoryEntry
import com.example.ui.theme.*

@Composable
fun MatchHistoryDialog(
    historyList: List<MatchHistoryEntry>,
    onDismiss: () -> Unit
) {
    var selectedEntry by remember { mutableStateOf<MatchHistoryEntry?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.88f)
                .shadow(24.dp, RoundedCornerShape(16.dp))
                .border(1.2.dp, GoldPlaqueBorder, RoundedCornerShape(16.dp))
                .testTag("dialog_match_history"),
            color = ModalSurface,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = GoldPlaqueText,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedEntry != null) "MATCH SUMMARY" else "TOURNAMENT HISTORY",
                            color = GoldPlaqueText,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                    IconButton(
                        onClick = {
                            if (selectedEntry != null) {
                                selectedEntry = null
                            } else {
                                onDismiss()
                            }
                        },
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
                    text = if (selectedEntry != null) "Complete 7-Contract Tournament Breakdown" else "Past 7-level tournament results & champions",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedEntry != null) {
                    val entry = selectedEntry!!
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFF140802), RoundedCornerShape(8.dp))
                            .border(1.dp, GoldPlaqueBorder.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Date: ${entry.formattedDate}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = if (entry.humanWon) "🏆 GRAND CHAMPION" else "Rank #${entry.humanFinalRank}",
                                color = if (entry.humanWon) GoldPlaqueText else Color(0xFF60A5FA),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Divider(color = Color(0xFF381B09))

                        Text(
                            text = "Players:",
                            color = GoldPlaqueText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Column(
                            modifier = Modifier.padding(start = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            entry.finalStandings.forEach { item ->
                                Text(
                                    text = "• ${item.name}",
                                    color = if (item.name == entry.humanPlayerName || item.name == "You") EmeraldAccentLight else Color(0xFFFFF7ED),
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        Divider(color = Color(0xFF381B09))

                        Text(
                            text = "Final Standings:",
                            color = GoldPlaqueText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Column(
                            modifier = Modifier.padding(start = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            entry.finalStandings.forEachIndexed { index, item ->
                                val isUser = item.name == entry.humanPlayerName || item.name == "You"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${index + 1}. ${item.name}",
                                        color = if (isUser) EmeraldAccentLight else Color(0xFFFFF7ED),
                                        fontSize = 12.sp,
                                        fontWeight = if (index == 0) FontWeight.Black else FontWeight.Medium
                                    )
                                    Text(
                                        text = "${item.score} pts",
                                        color = if (index == 0) GoldPlaqueText else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Divider(color = Color(0xFF381B09))

                        val achievementText = when (entry.humanFinalRank) {
                            1 -> "GRAND CHAMPION — Winner of the 7-Contract Championship"
                            2 -> "CHAMPIONSHIP CONTENDER — Runner-Up Commendation"
                            3 -> "CONTRACT MASTER — 7 Contracts Conquered"
                            else -> "RUMMAY! CHALLENGER — Tournament Completion"
                        }
                        Surface(
                            color = Color(0xFF1E0E05),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldPlaqueBorder.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Achievement Awarded:",
                                    color = GoldPlaqueSubText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = achievementText,
                                    color = GoldPlaqueText,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                } else if (historyList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFF140802), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF381B09), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No completed tournaments yet!",
                                color = Color(0xFFFFF7ED),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Play through all 7 Contract Rummy levels to record your tournament results here.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(historyList) { entry ->
                            MatchHistoryCard(
                                entry = entry,
                                onClick = { selectedEntry = entry }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (selectedEntry != null) {
                            selectedEntry = null
                        } else {
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("btn_close_history"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPlaqueText,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        if (selectedEntry != null) "BACK TO LIST" else "CLOSE",
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchHistoryCard(
    entry: MatchHistoryEntry,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .border(
                1.dp,
                if (entry.humanWon) EmeraldAccentLight else GoldPlaqueBorder.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        color = Color(0xFF1C0D05),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = if (entry.humanWon) EmeraldAccentLight else GoldPlaqueText,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Winner: ${entry.winnerName}",
                        color = if (entry.humanWon) EmeraldAccentLight else Color(0xFFFFF7ED),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    text = entry.formattedDate,
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }

            Text(
                text = "Your Final Score: ${entry.humanScore} pts (${if (entry.humanWon) "🏆 CHAMPION" else "Rank #${entry.humanFinalRank}"})",
                color = if (entry.humanWon) EmeraldAccentLight else Color(0xFFCBD5E1),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                entry.finalStandings.take(4).forEachIndexed { index, item ->
                    Text(
                        text = "#${index + 1} ${item.name}: ${item.score}p",
                        color = TextSecondary,
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
