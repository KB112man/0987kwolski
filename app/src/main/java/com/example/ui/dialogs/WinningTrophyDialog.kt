package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
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
fun WinningTrophyDialog(
    players: List<Player>,
    tournamentName: String = "Tournament Cup",
    winningRuns: List<List<Card>> = emptyList(),
    onReturnToMenu: () -> Unit
) {
    val sorted = players.sortedBy { it.totalScore }
    val winner = sorted.firstOrNull() ?: players.first()
    val isHumanWinner = winner.isHuman
    val humanIndex = sorted.indexOfFirst { it.isHuman }
    val humanRank = if (humanIndex >= 0) humanIndex + 1 else 4
    val humanPlayer = players.firstOrNull { it.isHuman } ?: Player(name = "You", isHuman = true)

    val (title, subtitle) = when (humanRank) {
        1 -> "GRAND CHAMPION" to "Winner of the 7-Contract Championship"
        2 -> "CHAMPIONSHIP CONTENDER" to "Runner-Up Commendation"
        3 -> "CONTRACT MASTER" to "7 Contracts Conquered"
        else -> "RUMMAY! CHALLENGER" to "Tournament Completion"
    }

    val trophyGradient = if (isHumanWinner) {
        Brush.verticalGradient(
            listOf(
                Color(0xFFFFDF00),
                Color(0xFFD4AF37),
                Color(0xFF996515),
                Color(0xFF5C3A08)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFCBD5E1),
                Color(0xFF94A3B8),
                Color(0xFF475569),
                Color(0xFF1E293B)
            )
        )
    }

    Dialog(
        onDismissRequest = { /* Must click button */ },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .shadow(28.dp, RoundedCornerShape(16.dp))
                .border(2.dp, GoldPlaqueBorder, RoundedCornerShape(16.dp))
                .testTag("dialog_winning_trophy"),
            color = Color(0xFF140802),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Foreground Championship Trophy Icon Container
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .shadow(12.dp, CircleShape)
                        .background(trophyGradient, CircleShape)
                        .border(2.dp, if (isHumanWinner) Color(0xFFFFF7ED) else Color(0xFFE2E8F0), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isHumanWinner) Icons.Default.EmojiEvents else Icons.Default.MilitaryTech,
                        contentDescription = "Trophy",
                        tint = if (isHumanWinner) Color(0xFF2C1406) else Color(0xFF0F172A),
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = title,
                    color = GoldPlaqueText,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = subtitle,
                    color = Color(0xFFFFF7ED),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // SEVEN-CONTRACT JOURNEY PRESENTATION
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GoldPlaqueBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    color = Color(0xFF1A0C05),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "7-CONTRACT JOURNEY COMPLETED",
                            color = GoldPlaqueText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ContractLevel.entries.forEach { level ->
                                val isL7 = level.levelNumber == 7
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        color = if (isL7) Color(0xFFB45309) else Color(0xFF0F2E1B),
                                        border = androidx.compose.foundation.BorderStroke(
                                            0.75.dp,
                                            if (isL7) GoldPlaqueBorder else Color(0xFF22C55E)
                                        ),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "L${level.levelNumber}",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = level.shortRequirement.take(6),
                                        color = TextMuted,
                                        fontSize = 7.5.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // LEVEL 7 THREE RUNS SHOWCASE IF AVAILABLE
                if (winningRuns.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                        color = Color(0xFF200F05),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "LEVEL 7 WINNING 3 RUNS (NO DISCARD)",
                                color = GoldPlaqueText,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            winningRuns.forEachIndexed { idx, runCards ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Run ${idx + 1}: ",
                                        color = GoldPlaqueBorder,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    runCards.forEach { c ->
                                        PlayingCardView(
                                            card = c,
                                            cardWidth = 24.dp,
                                            cardHeight = 34.dp,
                                            showPointsBadge = false
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Plaque of Standings
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(10.dp))
                        .border(1.2.dp, GoldPlaqueBorder.copy(alpha = 0.8f), RoundedCornerShape(10.dp)),
                    color = Color(0xFF1C0D05),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "FINAL TOURNAMENT STANDINGS",
                            color = GoldPlaqueText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        sorted.forEachIndexed { rank, player ->
                            val isWinnerRank = rank == 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isWinnerRank) Color(0x33D4AF37) else Color(0x18000000),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        0.75.dp,
                                        if (isWinnerRank) GoldPlaqueBorder else Color(0x225A2E0F),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isWinnerRank) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "1st Place",
                                            tint = GoldPlaqueText,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "#${rank + 1}",
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = player.name,
                                        color = if (player.isHuman) EmeraldAccentLight else Color(0xFFFFF7ED),
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isWinnerRank) FontWeight.Black else FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "${player.totalScore} pts",
                                    color = if (isWinnerRank) GoldPlaqueText else Color(0xFFCBD5E1),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onReturnToMenu,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .shadow(8.dp, RoundedCornerShape(10.dp))
                        .testTag("btn_return_to_main_menu"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPlaqueText,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "RETURN TO MAIN MENU",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
