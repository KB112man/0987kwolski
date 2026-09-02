package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun MainMenuScreen(
    hasSavedGame: Boolean,
    onResumeGame: () -> Unit,
    onStartNewTournament: () -> Unit,
    onMatchHistory: () -> Unit,
    onHowToPlay: () -> Unit,
    onDeckStats: () -> Unit
) {
    val tableFeltGradient = Brush.radialGradient(
        colors = listOf(
            FeltGreenLight,
            FeltGreen,
            FeltGreenDark,
            Color(0xFF031008)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tableFeltGradient)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("main_menu_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .widthIn(max = 440.dp)
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Title Plaque
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(16.dp))
                    .border(
                        2.dp,
                        Brush.verticalGradient(
                            listOf(Color(0xFFFFDF00), Color(0xFFD4AF37), Color(0xFF8C6212))
                        ),
                        RoundedCornerShape(16.dp)
                    ),
                color = Color(0xFF1B0C04),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF381B09), Color(0xFF220E04), Color(0xFF140702))
                            )
                        )
                        .padding(vertical = 18.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ULTIMATE RUMMAY!",
                        color = GoldPlaqueText,
                        fontSize = 26.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "7-Level Contract Rummy Tournament Edition",
                        color = Color(0xFFFFF7ED),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "108-Card Double Deck • AI Opponents • Authentic Rules",
                        color = EmeraldAccentLight,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Menu Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. RESUME GAME (If saved game exists)
                if (hasSavedGame) {
                    MainMenuButton(
                        icon = Icons.Default.PlayArrow,
                        title = "RESUME GAME",
                        subtitle = "Continue your active tournament",
                        isPrimary = true,
                        onClick = onResumeGame,
                        testTag = "btn_menu_resume"
                    )
                }

                // 2. START NEW TOURNAMENT
                MainMenuButton(
                    icon = Icons.Default.EmojiEvents,
                    title = "START NEW TOURNAMENT",
                    subtitle = "Begin Level 1 with 3 AI Opponents",
                    isPrimary = !hasSavedGame,
                    onClick = onStartNewTournament,
                    testTag = "btn_menu_new_tournament"
                )

                // 3. MATCH HISTORY
                MainMenuButton(
                    icon = Icons.Default.History,
                    title = "MATCH HISTORY",
                    subtitle = "View past tournament results & champions",
                    isPrimary = false,
                    onClick = onMatchHistory,
                    testTag = "btn_menu_history"
                )

                // 4. HOW TO PLAY / RULES
                MainMenuButton(
                    icon = Icons.Default.MenuBook,
                    title = "HOW TO PLAY & RULES",
                    subtitle = "7 Contracts, Buy Priority & Rummay rules",
                    isPrimary = false,
                    onClick = onHowToPlay,
                    testTag = "btn_menu_rules"
                )

                // 5. 108-CARD DECK & POINT VALUES
                MainMenuButton(
                    icon = Icons.Default.Layers,
                    title = "108-CARD DECK STATS",
                    subtitle = "2 Decks + 4 Jokers card point breakdown",
                    isPrimary = false,
                    onClick = onDeckStats,
                    testTag = "btn_menu_deck_stats"
                )
            }
        }
    }
}

@Composable
private fun MainMenuButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val borderColor = if (isPrimary) EmeraldAccentLight else GoldPlaqueBorder.copy(alpha = 0.7f)
    val bgColor = if (isPrimary) Color(0xFF092916) else Color(0xFF1E0D05)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isPrimary) 8.dp else 4.dp, RoundedCornerShape(12.dp))
            .border(if (isPrimary) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(testTag),
        color = bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isPrimary) EmeraldPrimary else Color(0xFF2C1408),
                        CircleShape
                    )
                    .border(
                        1.dp,
                        if (isPrimary) EmeraldAccentLight else GoldPlaqueBorder,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isPrimary) Color.Black else GoldPlaqueText,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isPrimary) EmeraldAccentLight else Color(0xFFFFF7ED),
                    fontSize = 13.5.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = subtitle,
                    color = if (isPrimary) Color(0xFF86EFAC) else TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (isPrimary) EmeraldAccentLight else GoldPlaqueText.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
