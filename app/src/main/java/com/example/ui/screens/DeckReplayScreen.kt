package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.engine.replay.PublicReplayState
import com.example.engine.replay.PublicSeat
import com.example.model.replay.ReplayMeld
import com.example.model.replay.ReplayMeldType
import com.example.ui.PlaybackSpeed
import com.example.ui.ReplayViewModel
import com.example.ui.components.CardBackView
import com.example.ui.components.PlayingCardView
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckReplayScreen(
    viewModel: ReplayViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val bannerText by viewModel.actionBanner.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val speed by viewModel.playbackSpeed.collectAsState()

    // Enforce Lifecycle cleanup: start playback automatically, cancel on dispose
    DisposableEffect(viewModel) {
        viewModel.startPlayback()
        onDispose {
            viewModel.pausePlayback()
        }
    }

    val feltBackground = Brush.radialGradient(
        listOf(
            Color(0xFF1B4D2E),
            Color(0xFF0F3B20),
            Color(0xFF0A2614),
            Color(0xFF05150A)
        )
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen_deck_replay"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "DECK REPLAY & CARTRIDGE",
                            color = GoldPlaqueText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Level ${state.levelNumber} • Step ${state.currentStepIndex} of ${state.totalSteps}",
                            color = Color(0xFFDCFCE7),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_replay_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Main Menu",
                            tint = GoldPlaqueText
                        )
                    }
                },
                actions = {
                    // Speed toggle button (0.5x, 1x, 2.5x)
                    TextButton(
                        onClick = { viewModel.cycleSpeed() },
                        modifier = Modifier.testTag("btn_replay_speed")
                    ) {
                        Surface(
                            color = Color(0xFF2E1306),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldPlaqueBorder)
                        ) {
                            Text(
                                text = speed.label,
                                color = GoldPlaqueText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF140702),
                    titleContentColor = GoldPlaqueText
                )
            )
        },
        bottomBar = {
            ReplayControlsBar(
                isPlaying = isPlaying,
                currentStep = state.currentStepIndex,
                totalSteps = state.totalSteps,
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onStepForward = { viewModel.stepForwardManual() },
                onStepBackward = { viewModel.stepBackwardManual() },
                onRestart = { viewModel.restart() }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(feltBackground)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // ==========================================
                // 1. ACTION BANNER (Information Context)
                // ==========================================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(8.dp))
                        .testTag("replay_action_banner"),
                    color = Color(0xFF140702),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldPlaqueBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = GoldPlaqueText,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = bannerText,
                            color = Color(0xFFFFF7ED),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ==========================================
                // 2. PLAYER RING (Avatars around table - NO PRIVATE RACKS)
                // ==========================================
                PlayerRing(seats = state.seats)

                Spacer(modifier = Modifier.height(10.dp))

                // ==========================================
                // 3. TABLE CENTER (Public Stock & Top Discard)
                // ==========================================
                ReplayTableCenter(
                    stockCount = state.stockCount,
                    topDiscard = state.topDiscard,
                    discardPileCount = state.discardPileCount
                )

                Spacer(modifier = Modifier.height(10.dp))

                // ==========================================
                // 4. PUBLIC TABLE MELDS ONLY
                // ==========================================
                ReplayTableMelds(
                    melds = state.tableMelds,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Public-Only Player Ring:
 * Renders all players around the perimeter as avatars with pure card counts and Down status badges.
 * Strictly avoids instantiating PlayerWoodenCardRack (no private hand bleed).
 */
@Composable
private fun PlayerRing(seats: List<PublicSeat>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("replay_player_ring"),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        seats.forEach { seat ->
            PublicPlayerAvatar(seat = seat)
        }
    }
}

@Composable
private fun PublicPlayerAvatar(seat: PublicSeat) {
    Surface(
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(10.dp))
            .border(
                1.5.dp,
                if (seat.isDown) EmeraldAccentLight else GoldPlaqueBorder.copy(alpha = 0.7f),
                RoundedCornerShape(10.dp)
            ),
        color = Color(0xFF1A0C04),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (seat.isHuman) Icons.Default.Person else Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = if (seat.isHuman) GoldPlaqueText else Color(0xFF93C5FD),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = seat.displayName,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            // Card Count Badge
            Surface(
                color = Color(0xFF2E1306),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF5A2E0F))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CARDS: ",
                        color = GoldPlaqueSubText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${seat.cardCount}",
                        color = GoldPlaqueText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Going Down Status
            if (seat.isDown) {
                Surface(
                    color = EmeraldPrimary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "DOWN (${seat.laidMelds.size})",
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            } else {
                Text(
                    text = "In Hand",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 9.sp
                )
            }
        }
    }
}

/**
 * Public Table Center: Stock count and face-up discard.
 */
@Composable
private fun ReplayTableCenter(
    stockCount: Int,
    topDiscard: com.example.model.Card?,
    discardPileCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("replay_table_center"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // STOCK PILE
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .shadow(6.dp, RoundedCornerShape(6.dp))
                    .border(1.2.dp, GoldPlaqueBorder.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            ) {
                CardBackView(
                    cardWidth = 80.dp,
                    cardHeight = 116.dp,
                    isGreenPattern = true
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "STOCK ($stockCount)",
                color = GoldPlaqueSubText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(28.dp))

        // DISCARD PILE
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (topDiscard != null) {
                PlayingCardView(
                    card = topDiscard,
                    cardWidth = 80.dp,
                    cardHeight = 116.dp,
                    isSelected = false
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(width = 80.dp, height = 116.dp)
                        .background(Color(0x33000000), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "EMPTY",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "DISCARD ($discardPileCount)",
                color = GoldPlaqueSubText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Public Melds View: Shows laid Books and Runs on table.
 */
@Composable
private fun ReplayTableMelds(
    melds: List<ReplayMeld>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .testTag("replay_table_melds"),
        color = Color(0xFF100703).copy(alpha = 0.85f),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPlaqueBorder.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Text(
                text = "PUBLIC TABLE MELDS (${melds.size})",
                color = GoldPlaqueText,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (melds.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x22000000), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No melds laid down yet. Table melds appear when players Go Down.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    melds.forEach { meld ->
                        PublicMeldCard(meld = meld)
                    }
                }
            }
        }
    }
}

@Composable
private fun PublicMeldCard(meld: ReplayMeld) {
    Surface(
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .border(1.dp, GoldPlaqueBorder, RoundedCornerShape(8.dp)),
        color = Color(0xFF1E0E05),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                color = if (meld.meldType == ReplayMeldType.BOOK) Color(0xFFB45309) else Color(0xFF047857),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = if (meld.meldType == ReplayMeldType.BOOK) "BOOK (${meld.cardIds.size})" else "RUN (${meld.cardIds.size})",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }

            // Miniature Card Pill IDs
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                meld.cardIds.forEach { id ->
                    val cleanLabel = formatCardIdForMiniDisplay(id)
                    Surface(
                        color = Color(0xFF2E1306),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            text = cleanLabel,
                            color = Color(0xFFFFF7ED),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatCardIdForMiniDisplay(cardId: String): String {
    return if (cardId.startsWith("joker")) {
        "★ Joker"
    } else {
        val parts = cardId.split("_")
        if (parts.size >= 4) {
            val suit = parts[2].take(1)
            val rank = parts[3]
            "$rank $suit"
        } else {
            cardId
        }
    }
}

/**
 * Replay Control Bar: Play/Pause, Step Backward, Step Forward, Restart.
 */
@Composable
private fun ReplayControlsBar(
    isPlaying: Boolean,
    currentStep: Int,
    totalSteps: Int,
    onTogglePlayPause: () -> Unit,
    onStepForward: () -> Unit,
    onStepBackward: () -> Unit,
    onRestart: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp),
        color = Color(0xFF140702),
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPlaqueBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Restart
            IconButton(
                onClick = onRestart,
                modifier = Modifier.testTag("btn_replay_restart")
            ) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = "Restart Replay",
                    tint = GoldPlaqueText
                )
            }

            // Step Backward
            IconButton(
                onClick = onStepBackward,
                enabled = currentStep > 0,
                modifier = Modifier.testTag("btn_replay_step_back")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Step Backward",
                    tint = if (currentStep > 0) GoldPlaqueText else GoldPlaqueText.copy(alpha = 0.3f)
                )
            }

            // Play / Pause Primary Button
            Button(
                onClick = onTogglePlayPause,
                modifier = Modifier
                    .height(44.dp)
                    .testTag("btn_replay_play_pause"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPlaqueText,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isPlaying) "PAUSE" else "PLAY",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }

            // Step Forward
            IconButton(
                onClick = onStepForward,
                enabled = currentStep < totalSteps,
                modifier = Modifier.testTag("btn_replay_step_forward")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Step Forward",
                    tint = if (currentStep < totalSteps) GoldPlaqueText else GoldPlaqueText.copy(alpha = 0.3f)
                )
            }

            // Step Counter
            Text(
                text = "$currentStep/$totalSteps",
                color = GoldPlaqueSubText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
