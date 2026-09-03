package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.GameState
import com.example.ui.theme.*

@Composable
fun MainMenuScreen(
    savedGameState: GameState?,
    hasHistory: Boolean,
    onStartNewTournament: () -> Unit,
    onResumeGame: () -> Unit,
    onViewHistory: () -> Unit,
    onViewRules: () -> Unit
) {
    val woodBackground = Brush.radialGradient(
        listOf(
            Color(0xFF381B09),
            Color(0xFF220E04),
            Color(0xFF140702),
            Color(0xFF0A0301)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(woodBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("screen_main_menu"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 440.dp)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // App Hero / Logo
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(16.dp))
                    .border(2.dp, GoldPlaqueBorder, RoundedCornerShape(16.dp)),
                color = Color(0xFF1C0D05),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF4A250E),
                                    Color(0xFF2E1306),
                                    Color(0xFF140702)
                                )
                            )
                        )
                        .padding(vertical = 20.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "RUMMAY!",
                        color = GoldPlaqueText,
                        fontSize = 38.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp
                    )
                    Text(
                        text = "The 7-Contract Rummy Card Game",
                        color = Color(0xFFFFF7ED),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "108-Card Double Deck • Wild Jokers • Books & Runs",
                        color = GoldPlaqueSubText,
                        fontSize = 10.sp,
                        letterSpacing = 0.2.sp
                    )
                }
            }

            // Resume Game Card (if saved game exists)
            if (savedGameState != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(12.dp))
                        .border(1.5.dp, EmeraldAccentLight, RoundedCornerShape(12.dp)),
                    color = Color(0xFF0F3B20),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircleFilled,
                                    contentDescription = null,
                                    tint = EmeraldAccentLight,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SAVED MATCH IN PROGRESS",
                                    color = EmeraldAccentLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Surface(
                                color = EmeraldPrimary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Level ${savedGameState.currentLevel} of 7",
                                    color = Color.Black,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Contract: ${savedGameState.contractLevel.shortRequirement}",
                            color = Color(0xFFDCFCE7),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = "Players: ${savedGameState.players.joinToString(", ") { it.name }}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )

                        Button(
                            onClick = onResumeGame,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .shadow(6.dp, RoundedCornerShape(8.dp))
                                .testTag("btn_resume_match"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "RESUME MATCH",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            // Main Menu Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MenuActionButton(
                    icon = Icons.Default.AddCircleOutline,
                    title = "NEW TOURNAMENT",
                    subtitle = "Start fresh from Level 1 (2 Books)",
                    containerColor = GoldPlaqueText,
                    contentColor = Color.Black,
                    onClick = onStartNewTournament,
                    testTag = "btn_new_tournament"
                )

                MenuActionButton(
                    icon = Icons.Default.History,
                    title = "MATCH HISTORY",
                    subtitle = "Past championship results & standings",
                    containerColor = Color(0xFF261205),
                    contentColor = GoldPlaqueText,
                    borderColor = GoldPlaqueBorder.copy(alpha = 0.6f),
                    onClick = onViewHistory,
                    testTag = "btn_menu_history"
                )

                MenuActionButton(
                    icon = Icons.Default.MenuBook,
                    title = "OFFICIAL RULES",
                    subtitle = "Contracts, Buys, Going Down, Rummay!",
                    containerColor = Color(0xFF1E0E05),
                    contentColor = Color(0xFFFFF7ED),
                    borderColor = Color(0xFF5A2E0F),
                    onClick = onViewRules,
                    testTag = "btn_menu_rules"
                )
            }

            // 7 Contract levels summary
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                color = Color(0xFF140702),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF381B09))
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "THE 7 TOURNAMENT CONTRACTS",
                        color = GoldPlaqueSubText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text("• Level 1: 2 Books (Deal 10)", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                    Text("• Level 2: 1 Book & 1 Run (Deal 10)", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                    Text("• Level 3: 2 Runs (Deal 10)", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                    Text("• Level 4: 3 Books (Deal 10)", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                    Text("• Level 5: 2 Books & 1 Run (Deal 12)", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                    Text("• Level 6: 1 Book & 2 Runs (Deal 12)", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                    Text("• Level 7: 3 Runs / No Discard (Deal 12)", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun MenuActionButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color? = null,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(4.dp, RoundedCornerShape(10.dp))
            .border(
                1.dp,
                borderColor ?: Color.Transparent,
                RoundedCornerShape(10.dp)
            ),
        color = containerColor,
        shape = RoundedCornerShape(10.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxSize()
                .testTag(testTag),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = contentColor
            ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = contentColor,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = subtitle,
                        color = contentColor.copy(alpha = 0.75f),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
