package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
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
fun BuyPriorityDialog(
    discard: Card,
    discarderName: String,
    player: Player? = null,
    contractLevel: ContractLevel? = null,
    onBuyClicked: () -> Unit,
    onPassClicked: () -> Unit
) {
    val handScrollState = rememberScrollState()

    Dialog(
        onDismissRequest = { /* Must respond */ },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .widthIn(max = 420.dp)
                .wrapContentHeight()
                .shadow(20.dp, RoundedCornerShape(14.dp))
                .border(1.25.dp, GlassBorderAmber, RoundedCornerShape(14.dp))
                .testTag("dialog_buy_priority"),
            color = Color(0xFF160B04),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = AmberAction,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BUY OUT-OF-TURN",
                        color = AmberAction,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF221107), RoundedCornerShape(8.dp))
                        .border(0.75.dp, Color(0xFF4A250B), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayingCardView(
                        card = discard,
                        cardWidth = 50.dp,
                        cardHeight = 70.dp,
                        showPointsBadge = true
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$discarderName discarded:",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = discard.displayName,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (contractLevel != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Target: ${contractLevel.shortRequirement}",
                                color = EmeraldAccentLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = Color(0x28F59E0B),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(0.75.dp, GlassBorderAmber),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Buying gives you this card + 1 draw penalty from Stock (2 cards total).",
                        color = AmberAction,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }

                if (player != null && player.hand.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.40f), RoundedCornerShape(8.dp))
                            .border(0.75.dp, Color(0xFF331808), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "YOUR HAND (${player.cardCount} cards)",
                                color = EmeraldAccentLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.3.sp
                            )
                            val matchesRank = player.hand.count { !it.isWild && it.rank == discard.rank }
                            val wildCount = player.hand.count { it.isWild }
                            if (matchesRank > 0 || wildCount > 0) {
                                Text(
                                    text = "$matchesRank match" + (if (wildCount > 0) " + $wildCount wild" else ""),
                                    color = AmberAction,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(handScrollState),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            player.hand.forEach { handCard ->
                                val isMatch = (!handCard.isWild && handCard.rank == discard.rank) ||
                                        (!handCard.isWild && handCard.suit == discard.suit && Math.abs(handCard.rank.value - discard.rank.value) == 1) ||
                                        handCard.isWild
                                Box(
                                    modifier = if (isMatch) {
                                        Modifier.border(1.5.dp, AmberAction, RoundedCornerShape(4.dp))
                                    } else {
                                        Modifier
                                    }
                                ) {
                                    PlayingCardView(
                                        card = handCard,
                                        cardWidth = 42.dp,
                                        cardHeight = 58.dp,
                                        showPointsBadge = false
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onPassClicked,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_buy_pass"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4A250B))
                    ) {
                        Text("PASS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onBuyClicked,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp)
                            .shadow(4.dp, RoundedCornerShape(8.dp))
                            .testTag("btn_buy_confirm"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberAction,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("BUY (2 CARDS)", fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
