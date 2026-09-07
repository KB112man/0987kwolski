package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
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
import com.example.model.ContractLevel
import com.example.ui.theme.*

@Composable
fun RulesDialog(onDismiss: () -> Unit) {
    val scrollState = rememberScrollState()
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f)
                .shadow(24.dp, RoundedCornerShape(16.dp))
                .border(1.dp, GlassBorderEmerald, RoundedCornerShape(16.dp))
                .testTag("dialog_rules"),
            color = ModalSurface,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "OFFICIAL RUMMAY RULES",
                            color = EmeraldAccent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RuleSection(
                        title = "1. THE 7 CONTRACT ROUNDS",
                        content = ContractLevel.entries.joinToString("\n") {
                            "• Level ${it.levelNumber}: ${it.shortRequirement} (Deal ${it.dealCount} cards)"
                        }
                    )
                    RuleSection(
                        title = "2. TURNS, DRAWS & BUYS",
                        content = "• On your turn: Draw 1 card from the stock pile OR take the face-up discard via 'TO YOU'.\n• Out of turn: When an opponent discards, you may BUY the discard. You receive that card plus 1 penalty draw card from the stock pile."
                    )
                    RuleSection(
                        title = "3. BOOKS & RUNS",
                        content = "• Book: 3 or more cards of the SAME rank (e.g. 7♠ 7♥ 7♦). Any suits allowed. Naturals must strictly exceed wild cards.\n• Run: 4 or more consecutive cards of the SAME suit (e.g. 4♥ 5♥ 6♥ 7♥). Naturals must exceed wild cards. Aces may be High or Low, but runs cannot wrap around."
                    )
                    RuleSection(
                        title = "4. GOING DOWN & PLAYING ON",
                        content = "• You must place your ENTIRE contract at once in order to go Down.\n• Once Down, you can 'Play On' by adding matching single cards to your or any opponent's table melds.\n• In Runs with a Joker, playing the natural card moves the Joker to the head or tail."
                    )
                    RuleSection(
                        title = "5. LEVEL 7 – 3 RUNS ('THAT DID IT!')",
                        content = "• Level 7 Win Condition: The round is won when ALL cards in hand can be partitioned into exactly 3 valid Runs (min 4 cards each, naturals > wilds).\n• Zero leftover cards outside those 3 Runs are allowed.\n• There is NO separate 'go down then play off leftovers' phase, and NO final discard.\n• When your entire hand fits into 3 Runs, tap 'THAT DID IT!' to reveal all 3 Runs and win immediately!\n• Pocket Feature: Use the 'POCKET' button on your rack or Expanded Hand to tuck cards privately for workspace organization."
                    )
                    RuleSection(
                        title = "6. RUMMAY! PENALTY",
                        content = "• If an active player discards a card that could legally be played onto any table meld, ANY player can call 'RUMMAY!'\n• The first caller gives 1 card from their hand to the offending discarder."
                    )
                    RuleSection(
                        title = "7. GOING OUT & SCORING",
                        content = "• A round ends immediately when a player empties their hand (Going Out) with 0 points.\n• Remaining cards in all opponents' hands are tallied as penalty points (Jokers: 20, Aces: 15, Faces/10s: 10, Number cards: 5).\n• Lowest score after 7 levels wins the Grand Championship trophy!"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("btn_close_rules"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("GOT IT", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun RuleSection(title: String, content: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF04140B), RoundedCornerShape(8.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Text(
            text = title,
            color = EmeraldAccentLight,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            color = TextPrimary,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}
