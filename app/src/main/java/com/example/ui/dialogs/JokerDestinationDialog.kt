package com.example.ui.dialogs

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
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
import com.example.model.Meld
import com.example.ui.components.PlayingCardView
import com.example.ui.theme.*

@Composable
fun JokerDestinationDialog(
    naturalCard: Card,
    meld: Meld,
    headRankName: String,
    tailRankName: String,
    onSelectHead: () -> Unit,
    onSelectTail: () -> Unit
) {
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
                .fillMaxWidth(0.90f)
                .wrapContentHeight()
                .shadow(24.dp, RoundedCornerShape(16.dp))
                .border(1.5.dp, GoldenPlaqueBorder, RoundedCornerShape(16.dp))
                .testTag("dialog_joker_destination"),
            color = ModalSurface,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = GoldPlaqueText,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "MOVE REPLACED JOKER",
                        color = GoldPlaqueText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = "Playing ${naturalCard.displayName} replaces the wild Joker in ${meld.ownerName}'s Run!",
                    color = Color.White,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                PlayingCardView(
                    card = naturalCard,
                    cardWidth = 48.dp,
                    cardHeight = 68.dp,
                    showPointsBadge = true
                )

                Text(
                    text = "Where would you like to shift the wild Joker?",
                    color = GoldPlaqueSubText,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onSelectHead,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_joker_move_head"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MOVE TO HEAD", fontSize = 11.sp, fontWeight = FontWeight.Black)
                            Text("(As $headRankName)", fontSize = 9.sp, color = Color(0xFFDCFCE7))
                        }
                    }

                    Button(
                        onClick = onSelectTail,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_joker_move_tail"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MOVE TO TAIL", fontSize = 11.sp, fontWeight = FontWeight.Black)
                            Text("(As $tailRankName)", fontSize = 9.sp, color = Color(0xFFDCFCE7))
                        }
                    }
                }
            }
        }
    }
}
