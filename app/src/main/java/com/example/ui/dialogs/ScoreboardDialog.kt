package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Player
import com.example.ui.theme.*

@Composable
fun ScoreboardDialog(
    players: List<Player>,
    currentLevel: Int,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.85f)
                .shadow(24.dp, RoundedCornerShape(16.dp))
                .border(1.dp, GlassBorderEmerald, RoundedCornerShape(16.dp))
                .testTag("dialog_scoreboard"),
            color = ModalSurface,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TOURNAMENT SCORECARD",
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
                    text = "Lowest total points after Level 7 wins the tournament",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF04140B), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PLAYER",
                            color = EmeraldAccentLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.weight(2.4f)
                        )
                        for (lvl in 1..7) {
                            Text(
                                text = "L$lvl",
                                color = if (lvl == currentLevel) AmberAction else TextSecondary,
                                fontSize = 9.5.sp,
                                fontWeight = if (lvl == currentLevel) FontWeight.Black else FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(0.85f)
                            )
                        }
                        Text(
                            text = "TOTAL",
                            color = EmeraldAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    // Player Score Rows
                    val sortedPlayers = players.sortedBy { it.totalScore }
                    sortedPlayers.forEachIndexed { rank, player ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (player.isHuman) Color(0x330D9488) else Color(0x1A000000),
                                    RoundedCornerShape(6.dp)
                                )
                                .border(
                                    1.dp,
                                    if (player.isHuman) GlassBorderEmerald else Color(0x0DFFFFFF),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(2.4f)) {
                                Text(
                                    text = "${rank + 1}. ${player.name}",
                                    color = if (player.isHuman) EmeraldAccentLight else TextPrimary,
                                    fontSize = 10.5.sp,
                                    lineHeight = 12.5.sp,
                                    fontWeight = if (player.isHuman) FontWeight.Black else FontWeight.Bold,
                                    maxLines = 2,
                                    softWrap = true
                                )
                                if (player.isDown) {
                                    Text(
                                        text = "DOWN",
                                        color = EmeraldAccent,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            for (lvl in 1..7) {
                                val score = player.scoresPerLevel.getOrNull(lvl - 1)
                                Text(
                                    text = if (score != null) "$score" else "-",
                                    color = if (score == 0) EmeraldAccent else if (score != null) TextPrimary else TextSecondary.copy(alpha = 0.4f),
                                    fontSize = 10.sp,
                                    fontWeight = if (score == 0) FontWeight.Black else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(0.85f)
                                )
                            }
                            Text(
                                text = "${player.totalScore}",
                                color = if (player.isHuman) EmeraldAccentLight else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("btn_close_scoreboard"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("CLOSE", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
