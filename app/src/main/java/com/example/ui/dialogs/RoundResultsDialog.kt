package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
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
import com.example.model.ContractLevel
import com.example.model.Player
import com.example.ui.theme.*

@Composable
fun RoundResultsDialog(
    completedLevel: Int,
    players: List<Player>,
    winnerIndex: Int?,
    onViewTrophyClicked: (() -> Unit)? = null,
    onNextLevelClicked: () -> Unit
) {
    val winner = if (winnerIndex != null) players.getOrNull(winnerIndex) else null
    val currentContract = ContractLevel.fromLevelNumber(completedLevel)
    val isGameFinal = completedLevel >= 7

    Dialog(onDismissRequest = { }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(16.dp))
                .border(1.dp, GlassBorderEmerald, RoundedCornerShape(16.dp))
                .testTag("dialog_round_results"),
            color = ModalSurface,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Celebration,
                    contentDescription = null,
                    tint = EmeraldAccentLight,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isGameFinal) "GAME OVER: 7 LEVELS COMPLETED!" else "LEVEL $completedLevel COMPLETE!",
                    color = EmeraldAccentLight,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = currentContract.shortRequirement,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = Color(0x330D9488),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderEmerald)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${winner?.name ?: "Winner"} went OUT (0 pts)",
                            color = EmeraldAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "ROUND SCORES & STANDINGS",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val sorted = players.sortedBy { it.totalScore }
                    sorted.forEachIndexed { rank, player ->
                        val isWinner = player.id == winner?.id
                        val roundScore = player.scoresPerLevel.lastOrNull() ?: 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isWinner) Color(0x330D9488) else Color(0x1A000000),
                                    RoundedCornerShape(6.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isWinner) GlassBorderEmerald else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "#${rank + 1} ",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = player.name,
                                    color = if (player.isHuman) EmeraldAccentLight else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = if (player.isHuman) FontWeight.Black else FontWeight.Bold
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "+$roundScore pts",
                                    color = if (isWinner) EmeraldAccent else SuitRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Total: ${player.totalScore}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                if (isGameFinal && onViewTrophyClicked != null) {
                    Button(
                        onClick = onViewTrophyClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .shadow(6.dp, RoundedCornerShape(10.dp))
                            .testTag("btn_view_trophy"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD700),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "VIEW LEVEL 7 WINNING TROPHY",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = onNextLevelClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .shadow(6.dp, RoundedCornerShape(10.dp))
                        .testTag("btn_continue_next_level"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGameFinal) Color(0x330D9488) else EmeraldPrimary,
                        contentColor = if (isGameFinal) EmeraldAccentLight else Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (isGameFinal) "RETURN TO MENU" else "START LEVEL ${completedLevel + 1}",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
