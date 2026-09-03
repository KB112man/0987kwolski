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
import androidx.compose.material.icons.filled.Stars
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
import com.example.model.Player
import com.example.ui.theme.*

@Composable
fun WinningTrophyDialog(
    players: List<Player>,
    onPlayAgain: () -> Unit,
    onBackToMainMenu: () -> Unit
) {
    val sortedPlayers = players.sortedBy { it.totalScore }
    val winner = sortedPlayers.firstOrNull() ?: players.first()
    val humanPlayer = players.find { it.isHuman }
    val isHumanWinner = winner.isHuman

    val trophyGradient = Brush.verticalGradient(
        listOf(
            Color(0xFFFFDF00),
            Color(0xFFD4AF37),
            Color(0xFF996515)
        )
    )

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
                .shadow(32.dp, RoundedCornerShape(20.dp))
                .border(2.dp, GoldPlaqueBorder, RoundedCornerShape(20.dp))
                .testTag("dialog_winning_trophy"),
            color = Color(0xFF140802),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Trophy Icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(trophyGradient, CircleShape)
                        .shadow(12.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Grand Champion Trophy",
                        tint = Color.Black,
                        modifier = Modifier.size(46.dp)
                    )
                }

                Text(
                    text = "7-LEVEL TOURNAMENT CHAMPION",
                    color = GoldPlaqueText,
                    fontSize = 17.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isHumanWinner) "CONGRATULATIONS! YOU WON THE TOURNAMENT!" else "${winner.name} IS THE CHAMPION!",
                    color = if (isHumanWinner) EmeraldAccentLight else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1E0E05),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPlaqueBorder.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "FINAL TOURNAMENT STANDINGS",
                            color = GoldPlaqueSubText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        sortedPlayers.forEachIndexed { rank, p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (p.id == winner.id) Color(0x33D4AF37) else Color.Transparent,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (rank == 0) {
                                        Icon(
                                            imageVector = Icons.Default.Stars,
                                            contentDescription = null,
                                            tint = GoldPlaqueText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = "${rank + 1}. ${p.name}" + (if (p.isHuman) " (You)" else ""),
                                        color = if (p.isHuman) EmeraldAccentLight else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (rank == 0 || p.isHuman) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                Text(
                                    text = "${p.totalScore} total pts",
                                    color = if (rank == 0) GoldPlaqueText else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                if (humanPlayer != null) {
                    val humanRank = sortedPlayers.indexOfFirst { it.id == humanPlayer.id } + 1
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF0F2B1A),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldAccentLight.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Your Tournament Performance",
                                color = EmeraldAccentLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Finished #$humanRank of ${players.size} with ${humanPlayer.totalScore} points",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .shadow(4.dp, RoundedCornerShape(8.dp))
                        .testTag("btn_play_again"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPlaqueText,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "PLAY AGAIN (NEW TOURNAMENT)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                OutlinedButton(
                    onClick = onBackToMainMenu,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("btn_trophy_main_menu"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFFF7ED)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPlaqueBorder.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "MAIN MENU",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
