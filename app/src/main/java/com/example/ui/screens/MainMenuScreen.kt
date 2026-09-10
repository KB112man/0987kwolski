package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ContractLevel
import com.example.model.GameState
import com.example.ui.theme.*

@Composable
fun MainMenuScreen(
    savedGameState: GameState?,
    hasHistory: Boolean,
    onStartNewTournament: () -> Unit,
    onResumeGame: () -> Unit,
    onViewHistory: () -> Unit,
    onViewRules: () -> Unit,
    onOpenDeckReplay: () -> Unit = {}
) {
    var showAbandonDialog by remember { mutableStateOf(false) }

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
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ==================================================
            // 1. TOP SECTION: Logo + Resume Tournament (if saved exists)
            // ==================================================
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

            // Resume Game Card directly below logo, only when a saved tournament exists
            if (savedGameState != null) {
                Spacer(modifier = Modifier.height(12.dp))
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
                            text = "Contract: ${savedGameState.contractLevel.shortRequirement} (Deal ${savedGameState.contractLevel.dealCount})",
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
                                text = "RESUME TOURNAMENT",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==================================================
            // 2. CENTER SECTION: Trophy Case, Deck Replay, Match History, Rules, Contract Preview
            // ==================================================
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Trophy Case (Meta feature placeholder)
                MenuActionButton(
                    icon = Icons.Default.EmojiEvents,
                    title = "TROPHY CASE",
                    subtitle = "Tournament championship accolades & feats (Coming Soon)",
                    containerColor = Color(0xFF221106),
                    contentColor = GoldPlaqueSubText,
                    borderColor = Color(0xFF4A250E),
                    enabled = false,
                    onClick = { /* Meta feature: disabled */ },
                    testTag = "btn_menu_trophy_case"
                )

                // Deck Replay / Cartridge
                MenuActionButton(
                    icon = Icons.Default.Replay,
                    title = "DECK REPLAY / CARTRIDGE",
                    subtitle = "Inspect seeded deck orders & public table replay",
                    containerColor = Color(0xFF261205),
                    contentColor = GoldPlaqueText,
                    borderColor = GoldPlaqueBorder.copy(alpha = 0.6f),
                    enabled = true,
                    onClick = onOpenDeckReplay,
                    testTag = "btn_menu_deck_replay"
                )

                // Match History
                MenuActionButton(
                    icon = Icons.Default.History,
                    title = "MATCH HISTORY",
                    subtitle = if (hasHistory) "View past championship results & standings" else "No completed matches yet",
                    containerColor = Color(0xFF261205),
                    contentColor = GoldPlaqueText,
                    borderColor = GoldPlaqueBorder.copy(alpha = 0.6f),
                    enabled = true,
                    onClick = onViewHistory,
                    testTag = "btn_menu_history"
                )

                // Rules
                MenuActionButton(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = "OFFICIAL RULES",
                    subtitle = "Contracts, Buys, Going Down, Rummay!",
                    containerColor = Color(0xFF1E0E05),
                    contentColor = Color(0xFFFFF7ED),
                    borderColor = Color(0xFF5A2E0F),
                    enabled = true,
                    onClick = onViewRules,
                    testTag = "btn_menu_rules"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Authoritative Contract / Level Preview derived dynamically from ContractLevel.entries
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("preview_contract_levels"),
                color = Color(0xFF140702),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF381B09))
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "THE 7 TOURNAMENT CONTRACTS",
                        color = GoldPlaqueSubText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    ContractLevel.entries.forEach { level ->
                        val suffix = if (level.noDiscard) " - No Discard" else ""
                        Text(
                            text = "• Level ${level.levelNumber}: ${level.shortRequirement}$suffix (Deal ${level.dealCount})",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // ==================================================
            // PHYSICAL SEPARATION: Spacer(weight = 1f)
            // Separates destructive action from Resume / Navigation
            // ==================================================
            Spacer(modifier = Modifier.weight(1f).defaultMinSize(minHeight = 24.dp))

            // ==================================================
            // 3. BOTTOM SECTION: New Tournament (with Safety Lock)
            // ==================================================
            MenuActionButton(
                icon = Icons.Default.AddCircleOutline,
                title = "NEW TOURNAMENT",
                subtitle = "Start fresh from Level 1 (2 Books)",
                containerColor = GoldPlaqueText,
                contentColor = Color.Black,
                enabled = true,
                onClick = {
                    if (savedGameState != null) {
                        showAbandonDialog = true
                    } else {
                        onStartNewTournament()
                    }
                },
                testTag = "btn_new_tournament"
            )
        }
    }

    // Safety Lock Dialog: Abandon Confirmation
    if (showAbandonDialog) {
        AlertDialog(
            onDismissRequest = { showAbandonDialog = false },
            containerColor = Color(0xFF220E04),
            titleContentColor = Color(0xFFFDE047),
            textContentColor = Color(0xFFFEE2E2),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Abandon current tournament?",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Text(
                    text = "Starting a new tournament will erase the current tournament progress.",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAbandonDialog = false
                        onStartNewTournament()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.testTag("btn_confirm_abandon_tournament")
                ) {
                    Text(
                        text = "START NEW TOURNAMENT",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAbandonDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1)),
                    modifier = Modifier.testTag("btn_cancel_abandon_tournament")
                ) {
                    Text(
                        text = "CANCEL",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        )
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
    enabled: Boolean = true,
    onClick: () -> Unit,
    testTag: String
) {
    val actualContainerColor = if (enabled) containerColor else containerColor.copy(alpha = 0.5f)
    val actualContentColor = if (enabled) contentColor else contentColor.copy(alpha = 0.5f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(if (enabled) 4.dp else 0.dp, RoundedCornerShape(10.dp))
            .border(
                1.dp,
                (borderColor ?: Color.Transparent).let { if (enabled) it else it.copy(alpha = 0.3f) },
                RoundedCornerShape(10.dp)
            ),
        color = actualContainerColor,
        shape = RoundedCornerShape(10.dp)
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxSize()
                .testTag(testTag),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = actualContentColor,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = actualContentColor
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
                    tint = actualContentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = actualContentColor,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = subtitle,
                        color = actualContentColor.copy(alpha = 0.75f),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (enabled) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = actualContentColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
