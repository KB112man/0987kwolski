package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Player
import com.example.ui.theme.*

enum class OpponentPosition {
    TOP,
    LEFT,
    RIGHT
}

@Composable
fun OpponentWoodenRackView(
    player: Player,
    isCurrentTurn: Boolean,
    position: OpponentPosition = OpponentPosition.TOP,
    modifier: Modifier = Modifier
) {
    val woodRackGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF6E3A1A),
            Color(0xFF4A250E),
            Color(0xFF2C1406),
            Color(0xFF140702)
        )
    )

    when (position) {
        OpponentPosition.TOP -> {
            Column(
                modifier = modifier.testTag("opponent_seat_${player.id}"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OpponentPillTag(player = player, isCurrentTurn = isCurrentTurn)
                Spacer(modifier = Modifier.height(3.dp))
                Surface(
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(4.dp))
                        .border(1.2.dp, Color(0xFF7C3F1D), RoundedCornerShape(4.dp)),
                    color = Color(0xFF1C0D05),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .background(woodRackGradient)
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            for (slotIdx in 0 until 4) {
                                CardBackView(
                                    cardWidth = 26.dp,
                                    cardHeight = 38.dp,
                                    isBurgundyPattern = true
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(116.dp)
                                .height(4.dp)
                                .background(Color(0xFF7C3F1D), RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        }
        OpponentPosition.LEFT -> {
            Row(
                modifier = modifier.testTag("opponent_seat_${player.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .shadow(4.dp, RoundedCornerShape(6.dp))
                        .border(
                            1.dp,
                            if (isCurrentTurn) EmeraldAccentLight else GoldPlaqueBorder.copy(alpha = 0.6f),
                            RoundedCornerShape(6.dp)
                        ),
                    color = Color(0xFF1C0D05),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = player.name,
                            color = if (isCurrentTurn) EmeraldAccentLight else Color(0xFFFFF7ED),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = GoldPlaqueText,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${player.cardCount}",
                            color = GoldPlaqueText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (player.isDown) {
                            Surface(
                                color = EmeraldPrimary,
                                shape = RoundedCornerShape(3.dp)
                            ) {
                                Text(
                                    text = "DN",
                                    color = Color.Black,
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 0.5.dp)
                                )
                            }
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(4.dp))
                        .border(1.2.dp, Color(0xFF7C3F1D), RoundedCornerShape(4.dp)),
                    color = Color(0xFF1C0D05),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .background(woodRackGradient)
                            .padding(horizontal = 3.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy((-18).dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        for (i in 0 until 4) {
                            CardBackView(
                                cardWidth = 32.dp,
                                cardHeight = 44.dp,
                                isBurgundyPattern = true
                            )
                        }
                    }
                }
            }
        }
        OpponentPosition.RIGHT -> {
            Row(
                modifier = modifier.testTag("opponent_seat_${player.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(4.dp))
                        .border(1.2.dp, Color(0xFF7C3F1D), RoundedCornerShape(4.dp)),
                    color = Color(0xFF1C0D05),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .background(woodRackGradient)
                            .padding(horizontal = 3.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy((-18).dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        for (i in 0 until 4) {
                            CardBackView(
                                cardWidth = 32.dp,
                                cardHeight = 44.dp,
                                isBurgundyPattern = true
                            )
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .shadow(4.dp, RoundedCornerShape(6.dp))
                        .border(
                            1.dp,
                            if (isCurrentTurn) EmeraldAccentLight else GoldPlaqueBorder.copy(alpha = 0.6f),
                            RoundedCornerShape(6.dp)
                        ),
                    color = Color(0xFF1C0D05),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = GoldPlaqueText,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = player.name,
                            color = if (isCurrentTurn) EmeraldAccentLight else Color(0xFFFFF7ED),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "${player.cardCount}",
                            color = GoldPlaqueText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (player.isDown) {
                            Surface(
                                color = EmeraldPrimary,
                                shape = RoundedCornerShape(3.dp)
                            ) {
                                Text(
                                    text = "DN",
                                    color = Color.Black,
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 0.5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OpponentPillTag(
    player: Player,
    isCurrentTurn: Boolean
) {
    Surface(
        color = Color(0xFF1C0D05),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCurrentTurn) EmeraldAccentLight else GoldPlaqueBorder.copy(alpha = 0.7f)
        ),
        modifier = Modifier.shadow(4.dp, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = GoldPlaqueText,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = player.name,
                color = if (isCurrentTurn) EmeraldAccentLight else Color(0xFFFFF7ED),
                fontSize = 11.5.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = "${player.cardCount}",
                color = GoldPlaqueText,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
            if (player.isDown) {
                Surface(
                    color = EmeraldPrimary,
                    shape = RoundedCornerShape(3.dp)
                ) {
                    Text(
                        text = "DOWN",
                        color = Color.Black,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}
