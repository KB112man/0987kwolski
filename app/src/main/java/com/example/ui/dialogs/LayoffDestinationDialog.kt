package com.example.ui.dialogs

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.model.Meld
import com.example.model.MeldType
import com.example.model.Player
import com.example.ui.components.PlayingCardView
import com.example.ui.theme.*

@Composable
fun LayoffDestinationDialog(
    selectedCard: Card?,
    player: Player,
    allTableMelds: List<Meld>,
    onSelectDestination: (meldId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var showNotDownNotice by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val myMelds = allTableMelds.filter { it.ownerId == player.id }
    val opponentMelds = allTableMelds.filter { it.ownerId != player.id }
    val legalMelds = if (selectedCard != null) allTableMelds.filter { it.canAddCard(selectedCard) } else emptyList()

    if (showNotDownNotice) {
        AlertDialog(
            onDismissRequest = { showNotDownNotice = false },
            title = {
                Text(text = "YOU MUST GO DOWN FIRST", color = Color(0xFFEF4444), fontWeight = FontWeight.Black)
            },
            text = {
                Text(
                    text = "You can view table melds, but you cannot Play On until you complete your contract.",
                    color = Color.White
                )
            },
            confirmButton = {
                TextButton(onClick = { showNotDownNotice = false }) {
                    Text("OK", color = GoldPlaqueText, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = ModalSurface,
            titleContentColor = Color(0xFFEF4444),
            textContentColor = Color.White
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.88f)
                .shadow(24.dp, RoundedCornerShape(16.dp))
                .border(1.5.dp, GoldenPlaqueBorder, RoundedCornerShape(16.dp))
                .testTag("dialog_layoff_destination"),
            color = ModalSurface,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = null,
                            tint = GoldPlaqueText,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SELECT PLAY ON DESTINATION",
                            color = GoldPlaqueText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp).testTag("btn_close_play_on_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Card preview
                if (selectedCard != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(8.dp)),
                        color = Color(0xFF140802),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPlaqueBorder.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PlayingCardView(
                                card = selectedCard,
                                cardWidth = 48.dp,
                                cardHeight = 68.dp,
                                showPointsBadge = true
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Selected Card: ${selectedCard.displayName}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Select the specific Book or Run below to play this card onto.",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                                if (legalMelds.isNotEmpty()) {
                                    Text(
                                        text = "✓ ${legalMelds.size} legal destination(s) available on table",
                                        color = EmeraldAccentLight,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        color = Color(0xFF181310),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3D2111))
                    ) {
                        Text(
                            text = "No card selected. You are in View Mode.",
                            color = Color(0xFFFFB4B4),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (allTableMelds.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        color = Color(0xFF1E0E05),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3D1E0B))
                    ) {
                        Text(
                            text = "No Books or Runs have been placed on the table yet by any player.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                if (allTableMelds.isNotEmpty() && legalMelds.isEmpty() && selectedCard != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        color = Color(0xFF2E1205),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6B290C))
                    ) {
                        Text(
                            text = "No legal destinations available on the table for ${selectedCard.displayName}.",
                            color = Color(0xFFFF9E80),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // MY BOOKS & RUNS
                if (myMelds.isNotEmpty()) {
                    MeldDestinationSection(
                        title = "YOUR TABLE COMBINATIONS (${myMelds.size})",
                        melds = myMelds,
                        selectedCard = selectedCard,
                        onSelectDestination = { meldId ->
                            if (!player.isDown) {
                                showNotDownNotice = true
                            } else {
                                onSelectDestination(meldId)
                                onDismiss()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // OPPONENTS' BOOKS & RUNS
                if (opponentMelds.isNotEmpty()) {
                    MeldDestinationSection(
                        title = "OPPONENTS' TABLE COMBINATIONS (${opponentMelds.size})",
                        melds = opponentMelds,
                        selectedCard = selectedCard,
                        onSelectDestination = { meldId ->
                            if (!player.isDown) {
                                showNotDownNotice = true
                            } else {
                                onSelectDestination(meldId)
                                onDismiss()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("btn_cancel_layoff"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPlaqueBorder.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("CANCEL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MeldDestinationSection(
    title: String,
    melds: List<Meld>,
    selectedCard: Card?,
    onSelectDestination: (meldId: String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            color = GoldPlaqueText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )

        melds.forEach { meld ->
            val isLegal = selectedCard != null && meld.canAddCard(selectedCard)
            val rejectionReason = if (selectedCard == null) "Select a card to play" else meld.getPlayOnRejectionReason(selectedCard)
            val typeTitle = if (meld.type == MeldType.BOOK) "Book" else "Run"

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, RoundedCornerShape(8.dp))
                    .testTag("layoff_meld_card_${meld.id}"),
                color = if (isLegal) Color(0xFF072414) else Color(0xFF190C05),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isLegal) 1.5.dp else 0.75.dp,
                    color = if (isLegal) EmeraldAccent else Color(0xFF3B1E0A)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${meld.ownerName}'s $typeTitle",
                                color = if (isLegal) EmeraldAccentLight else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (meld.type == MeldType.BOOK) Color(0xFF1E3A8A) else Color(0xFF831843),
                                shape = RoundedCornerShape(3.dp)
                            ) {
                                Text(
                                    text = if (meld.type == MeldType.BOOK) "BOOK (${meld.cards.size})" else "RUN (${meld.cards.size})",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        if (isLegal) {
                            Surface(
                                color = Color(0xFF0F5132),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = EmeraldAccentLight,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "LEGAL",
                                        color = EmeraldAccentLight,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        } else {
                            Surface(
                                color = Color(0xFF2E1005),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "NOT LEGAL",
                                    color = TextMuted,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        meld.cards.forEach { c ->
                            PlayingCardView(
                                card = c,
                                cardWidth = 36.dp,
                                cardHeight = 50.dp,
                                showPointsBadge = false
                            )
                        }
                        if (isLegal) {
                            Text(
                                text = "+",
                                color = EmeraldAccentLight,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .border(1.dp, EmeraldAccentLight, RoundedCornerShape(4.dp))
                                    .clip(RoundedCornerShape(4.dp))
                            ) {
                                PlayingCardView(
                                    card = selectedCard,
                                    cardWidth = 36.dp,
                                    cardHeight = 50.dp,
                                    showPointsBadge = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (isLegal) {
                        Button(
                            onClick = { onSelectDestination(meld.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .testTag("btn_layoff_destination_${meld.id}"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0F5132),
                                contentColor = EmeraldAccentLight
                            ),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldAccentLight)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Upload,
                                    contentDescription = null,
                                    tint = EmeraldAccentLight,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "PLAY ON THIS ${typeTitle.uppercase()}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    } else if (rejectionReason != null) {
                        Text(
                            text = "Cannot play on: $rejectionReason",
                            color = TextMuted,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(start = 2.dp, top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
