package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Card
import com.example.model.Player
import com.example.ui.components.PlayingCardView
import com.example.ui.theme.*

@Composable
fun RummayCallDialog(
    discardedCard: Card,
    offender: Player,
    caller: Player?,
    isHumanCaller: Boolean,
    isHumanOffender: Boolean,
    humanPlayer: Player?,
    penaltyCard: Card? = null,
    isResolved: Boolean = false,
    onCallRummay: () -> Unit,
    onPassRummay: () -> Unit,
    onGiveCardSelected: (Card) -> Unit
) {
    var selectedCard by remember { mutableStateOf<Card?>(null) }

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
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .shadow(24.dp, RoundedCornerShape(16.dp))
                .border(2.dp, CrimsonBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = WoodDark
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF2C1014), Color(0xFF1E0A0D), WoodDark)
                        )
                    )
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Banner
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Rummay Alert",
                        tint = CrimsonBorder,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "RUMMAY!",
                        color = Color(0xFFFF4D4D),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }

                Text(
                    text = "${offender.name} discarded a Rummy Card (${discardedCard.displayName}) that could play on a table meld!",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                PlayingCardView(
                    card = discardedCard,
                    cardWidth = 86.dp,
                    cardHeight = 124.dp
                )

                if (isResolved && penaltyCard != null && caller != null) {
                    val catchText = if (isHumanOffender) "caught your" else "caught ${offender.name}'s"
                    val penaltyTargetText = if (isHumanOffender) "you" else offender.name
                    Text(
                        text = "${caller.name} $catchText ${discardedCard.displayName}.\nIt was played onto a table meld.\n\n${caller.name} gave $penaltyTargetText ${penaltyCard.displayName} as a penalty.",
                        color = Color(0xFFDCFCE7),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onPassRummay, // Maps to dismiss/acknowledge
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("acknowledge_rummay_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("CONTINUE", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else if (isHumanCaller && humanPlayer != null) {
                    Text(
                        text = "YOU CALLED RUMMAY!\nChoose 1 card from your hand to give to ${offender.name}:",
                        color = GoldPlaqueText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                    ) {
                        items(humanPlayer.hand) { card ->
                            val isSelected = selectedCard?.id == card.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) GoldPlaqueText else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedCard = card }
                            ) {
                                PlayingCardView(
                                    card = card,
                                    isSelected = isSelected,
                                    cardWidth = 72.dp,
                                    cardHeight = 104.dp
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            selectedCard?.let { onGiveCardSelected(it) }
                        },
                        enabled = selectedCard != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("confirm_give_rummay_card"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrimsonBorder,
                            disabledContainerColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (selectedCard != null) "GIVE ${selectedCard!!.displayName} TO ${offender.name.uppercase()}" else "SELECT A CARD TO GIVE",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (!isHumanOffender && caller == null) {
                    Text(
                        text = "Will you call RUMMAY? First caller gives 1 card from their hand to ${offender.name}!",
                        color = Color(0xFFFFD54F),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onPassRummay,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("pass_rummay_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                        ) {
                            Text("PASS", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onCallRummay,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("call_rummay_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("CALL RUMMAY!", color = Color.White, fontWeight = FontWeight.Black)
                        }
                    }
                } else if (isHumanOffender) {
                    Text(
                        text = "You were caught! An opponent will now call RUMMAY! and penalize you.",
                        color = Color(0xFFFFD54F),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onPassRummay,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("acknowledge_rummay_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("CONTINUE", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
