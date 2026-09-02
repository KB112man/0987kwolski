package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Card
import com.example.ui.theme.*

@Composable
fun TableCenterView(
    stockCount: Int,
    topDiscard: Card?,
    discardPileCount: Int,
    isPlayerTurn: Boolean,
    isDrawPhase: Boolean,
    onStockClicked: () -> Unit,
    onDiscardClicked: () -> Unit,
    isDiscardHovered: Boolean = false,
    onRegisterStockBounds: (Rect) -> Unit = {},
    onRegisterDiscardBounds: (Rect) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // CENTER STOCK & DISCARD PILES
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // STOCK PILE
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .onGloballyPositioned { coords -> onRegisterStockBounds(coords.boundsInRoot()) }
                    .clickable(enabled = isPlayerTurn && isDrawPhase) { onStockClicked() }
            ) {
                Box(
                    modifier = Modifier
                        .shadow(if (isPlayerTurn && isDrawPhase) 10.dp else 4.dp, RoundedCornerShape(6.dp))
                        .border(
                            if (isPlayerTurn && isDrawPhase) 2.dp else 1.dp,
                            if (isPlayerTurn && isDrawPhase) Color(0xFF22C55E) else GoldPlaqueBorder.copy(alpha = 0.5f),
                            RoundedCornerShape(6.dp)
                        )
                ) {
                    CardBackView(
                        cardWidth = 58.dp,
                        cardHeight = 82.dp,
                        isGreenPattern = true,
                        count = null,
                        onClick = if (isPlayerTurn && isDrawPhase) onStockClicked else null
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "STOCK",
                    color = GoldPlaqueText,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(1.dp))
                Surface(
                    color = Color(0xFF140802),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPlaqueBorder.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "$stockCount",
                        color = Color.White,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp)
                    )
                }
            }

            // DISCARD PILE
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .onGloballyPositioned { coords -> onRegisterDiscardBounds(coords.boundsInRoot()) }
            ) {
                val discardBorderColor = when {
                    isDiscardHovered -> Color(0xFFF59E0B)
                    isPlayerTurn && isDrawPhase -> Color(0xFF22C55E)
                    else -> GoldPlaqueBorder.copy(alpha = 0.5f)
                }

                if (topDiscard != null) {
                    Box(
                        modifier = Modifier
                            .shadow(if (isDiscardHovered || (isPlayerTurn && isDrawPhase)) 10.dp else 4.dp, RoundedCornerShape(6.dp))
                            .border(
                                if (isDiscardHovered || (isPlayerTurn && isDrawPhase)) 2.dp else 1.dp,
                                discardBorderColor,
                                RoundedCornerShape(6.dp)
                            )
                    ) {
                        PlayingCardView(
                            card = topDiscard,
                            cardWidth = 58.dp,
                            cardHeight = 82.dp,
                            onClick = if (isPlayerTurn && isDrawPhase) onDiscardClicked else null,
                            showPointsBadge = false
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .width(58.dp)
                            .height(82.dp)
                            .border(1.dp, GoldPlaqueBorder.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .background(Color(0xFF120703), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Empty",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "DISCARD",
                    color = GoldPlaqueText,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(1.dp))
                Surface(
                    color = Color(0xFF140802),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPlaqueBorder.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "$discardPileCount",
                        color = Color.White,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}
