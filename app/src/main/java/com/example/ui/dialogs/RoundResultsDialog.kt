package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Stars
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
import androidx.compose.ui.window.DialogProperties
import com.example.model.Card
import com.example.model.ContractLevel
import com.example.model.Player
import com.example.ui.components.PlayingCardView
import com.example.ui.theme.*

@Composable
fun RoundResultsDialog(
    winner: Player,
    players: List<Player>,
    completedLevel: Int,
    isFinalLevel: Boolean,
    onContinueNextLevel: () -> Unit
) {
    val scrollState = rememberScrollState()
    val contract = ContractLevel.entries.firstOrNull { it.levelNumber == completedLevel }
        ?: ContractLevel.LEVEL_1

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .shadow(28.dp, RoundedCornerShape(16.dp))
                .border(1.5.dp, GlassBorderEmerald, RoundedCornerShape(16.dp))
                .testTag("dialog_round_results"),
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
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = AmberAction,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LEVEL $completedLevel COMPLETE",
                        color = EmeraldAccent,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = "${winner.name} WENT OUT! (0 penalty points)",
                    color = AmberAction,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Contract: ${contract.shortRequirement}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sortedByRoundScore = players.sortedBy {
                        it.scoresPerLevel.getOrNull(completedLevel - 1) ?: 0
                    }

                    sortedByRoundScore.forEach { p ->
                        val roundPoints = p.scoresPerLevel.getOrNull(completedLevel - 1) ?: 0
                        val isRoundWinner = p.id == winner.id
                        PlayerRoundSummaryCard(
                            player = p,
                            roundPoints = roundPoints,
                            isWinner = isRoundWinner
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF04140B),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderEmerald)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "OVERALL STANDINGS (LOWEST POINTS WINS)",
                                color = EmeraldAccentLight,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val sortedOverall = players.sortedBy { it.totalScore }
                            sortedOverall.forEachIndexed { rank, p ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${rank + 1}. ${p.name}" + (if (p.isHuman) " (You)" else ""),
                                        color = if (p.isHuman) EmeraldAccentLight else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = if (p.isHuman) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = "${p.totalScore} pts",
                                        color = if (rank == 0) AmberAction else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onContinueNextLevel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_continue_next_level"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isFinalLevel) "VIEW CHAMPIONSHIP RESULTS" else "CONTINUE TO LEVEL ${completedLevel + 1}",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerRoundSummaryCard(
    player: Player,
    roundPoints: Int,
    isWinner: Boolean
) {
    val handScrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isWinner) Color(0xFF0F3B24) else Color(0xFF140802),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isWinner) GlassBorderEmerald else GlassBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isWinner) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = null,
                            tint = AmberAction,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = player.name + (if (player.isHuman) " (You)" else ""),
                        color = if (isWinner) EmeraldAccentLight else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = if (isWinner) "0 pts (Went Out)" else "+$roundPoints pts",
                    color = if (isWinner) EmeraldAccent else AmberAction,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }

            if (!isWinner && player.hand.isNotEmpty()) {
                Text(
                    text = "Remaining cards (${player.cardCount}):",
                    color = TextSecondary,
                    fontSize = 9.5.sp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(handScrollState),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    player.hand.forEach { c ->
                        PlayingCardView(
                            card = c,
                            cardWidth = 36.dp,
                            cardHeight = 52.dp,
                            showPointsBadge = true
                        )
                    }
                }
            }
        }
    }
}
