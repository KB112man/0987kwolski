package com.example.ui.dialogs

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.ui.window.DialogProperties
import com.example.model.Card
import com.example.model.PendingJokerMove
import com.example.ui.components.PlayingCardView
import com.example.ui.theme.*

@Composable
fun JokerDestinationDialog(
    pendingMove: PendingJokerMove,
    onSelectConfiguration: (List<Card>) -> Unit,
    onCancel: () -> Unit
) {
    val verticalScroll = rememberScrollState()

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.85f)
                .shadow(24.dp, RoundedCornerShape(16.dp))
                .border(1.5.dp, GoldPlaqueBorder, RoundedCornerShape(16.dp))
                .testTag("dialog_joker_destination"),
            color = ModalSurface,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScroll)
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = GoldPlaqueText,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MOVE JOKER",
                            color = GoldPlaqueText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("btn_close_joker_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF140802),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "You played ${pendingMove.card.displayName}. Where do you want the Joker to move?",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 16.sp
                        )
                        Text(
                            text = "Select one of the legal Run arrangements below. The Joker remains in this Run in the chosen position.",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Current Run Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "CURRENT RUN (${pendingMove.targetMeld.ownerName}'s):",
                        color = GoldPlaqueText,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF0F1E13),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1B4D2E))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (card in pendingMove.targetMeld.cards) {
                                PlayingCardView(
                                    card = card,
                                    cardWidth = 40.dp,
                                    cardHeight = 56.dp,
                                    showPointsBadge = false
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "LEGAL RESULTING ARRANGEMENTS:",
                    color = EmeraldAccentLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 2.dp, bottom = 6.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pendingMove.options.forEachIndexed { index, optionCards ->
                        val isJokerAtHead = optionCards.firstOrNull()?.isWild == true
                        val isJokerAtTail = optionCards.lastOrNull()?.isWild == true
                        val positionLabel = when {
                            isJokerAtHead -> "OPTION ${index + 1}: JOKER MOVES DOWN (HEAD)"
                            isJokerAtTail -> "OPTION ${index + 1}: JOKER MOVES UP (TAIL)"
                            else -> "OPTION ${index + 1}: JOKER AT POSITION ${optionCards.indexOfFirst { it.isWild } + 1}"
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(4.dp, RoundedCornerShape(8.dp))
                                .border(1.5.dp, GoldPlaqueBorder, RoundedCornerShape(8.dp))
                                .clickable { onSelectConfiguration(optionCards) }
                                .testTag("joker_destination_option_$index"),
                            color = Color(0xFF1E1006),
                            shape = RoundedCornerShape(8.dp)
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
                                    Text(
                                        text = positionLabel,
                                        color = GoldPlaqueText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Button(
                                        onClick = { onSelectConfiguration(optionCards) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF1B4D2E),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldAccentLight),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier
                                            .height(30.dp)
                                            .testTag("btn_select_joker_option_$index")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "SELECT",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .background(Color(0xFF120803), RoundedCornerShape(6.dp))
                                        .border(0.75.dp, CardBorder.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .padding(6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (card in optionCards) {
                                        PlayingCardView(
                                            card = card,
                                            cardWidth = 38.dp,
                                            cardHeight = 54.dp,
                                            showPointsBadge = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2A1006),
                        contentColor = TextMuted
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5C260E)),
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(38.dp)
                        .testTag("btn_cancel_joker_destination")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "CANCEL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
