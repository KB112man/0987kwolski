package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Card
import com.example.model.Rank
import com.example.ui.theme.*

@Composable
fun PlayingCardView(
    card: Card,
    isSelected: Boolean = false,
    isHighlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 72.dp,
    cardHeight: Dp = 104.dp,
    showPointsBadge: Boolean = false
) {
    val offsetY by animateDpAsState(
        targetValue = if (isSelected) (-8).dp else 0.dp,
        label = "cardElevation"
    )

    val suitColor = when {
        card.isJoker -> WildText
        card.suit.isRed -> SuitRed
        else -> SuitBlack
    }
    val backgroundColor = if (card.isWild) WildBackground else CardWhite
    val borderColor = if (isHighlighted) Color(0xFF22C55E) else if (isSelected) Color(0xFFFFD700) else if (card.isWild) WildBorder else CardBorder
    val borderWidth = if (isHighlighted) 3.dp else if (isSelected) 2.5.dp else 1.dp

    Card(
        modifier = modifier
            .width(cardWidth)
            .height(cardHeight)
            .offset(y = offsetY)
            .shadow(
                elevation = if (isSelected) 10.dp else 3.dp,
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(4.dp)
            )
            .clip(RoundedCornerShape(4.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .testTag("card_${card.id}"),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.5.dp)
        ) {
            if (card.isJoker) {
                // Joker Card
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "★",
                            color = WildText,
                            fontSize = (cardWidth.value * 0.22f).sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (showPointsBadge) {
                            Text(
                                text = "20p",
                                color = WildText,
                                fontSize = (cardWidth.value * 0.16f).sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "★",
                            color = EmeraldPrimary,
                            fontSize = (cardWidth.value * 0.36f).sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "JOKER",
                            color = WildText,
                            fontSize = (cardWidth.value * 0.18f).sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        text = "★",
                        color = WildText,
                        fontSize = (cardWidth.value * 0.22f).sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            } else {
                // Standard Card
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top-Left Index (Rank + Suit)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = card.rank.shortName,
                                color = suitColor,
                                fontSize = (cardWidth.value * 0.30f).sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = (cardWidth.value * 0.30f).sp
                            )
                            Text(
                                text = card.suit.symbol,
                                color = suitColor,
                                fontSize = (cardWidth.value * 0.24f).sp,
                                lineHeight = (cardWidth.value * 0.24f).sp
                            )
                        }
                        if (showPointsBadge) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (card.suit.isRed) Color(0xFFFEE2E2) else Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(3.dp)
                                    )
                                    .padding(horizontal = 2.dp, vertical = 0.5.dp)
                            ) {
                                Text(
                                    text = "${card.pointValue}p",
                                    color = if (card.suit.isRed) SuitRed else SuitBlack,
                                    fontSize = (cardWidth.value * 0.14f).sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Center Big Suit Symbol or Court figure
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (card.rank in listOf(Rank.JACK, Rank.QUEEN, Rank.KING)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = card.suit.symbol,
                                    color = suitColor.copy(alpha = 0.85f),
                                    fontSize = (cardWidth.value * 0.38f).sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        } else {
                            Text(
                                text = card.suit.symbol,
                                color = suitColor.copy(alpha = 0.95f),
                                fontSize = (cardWidth.value * 0.44f).sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Bottom-Right Inverted Index
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = card.suit.symbol,
                                color = suitColor,
                                fontSize = (cardWidth.value * 0.22f).sp,
                                lineHeight = (cardWidth.value * 0.22f).sp
                            )
                            Text(
                                text = card.rank.shortName,
                                color = suitColor,
                                fontSize = (cardWidth.value * 0.28f).sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = (cardWidth.value * 0.28f).sp
                            )
                        }
                    }
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x22FFD700))
                )
            }
        }
    }
}

@Composable
fun CardBackView(
    modifier: Modifier = Modifier,
    cardWidth: Dp = 72.dp,
    cardHeight: Dp = 104.dp,
    isGreenPattern: Boolean = false,
    isBurgundyPattern: Boolean = false,
    count: Int? = null,
    onClick: (() -> Unit)? = null
) {
    val baseColor = when {
        isGreenPattern -> Color(0xFF0F3B20)
        isBurgundyPattern -> Color(0xFF5B1019)
        else -> Color(0xFF1E3A8A)
    }
    val patternColor = when {
        isGreenPattern -> Color(0xFF34D399)
        isBurgundyPattern -> Color(0xFFB91C1C)
        else -> Color(0xFF60A5FA)
    }

    Card(
        modifier = modifier
            .width(cardWidth)
            .height(cardHeight)
            .shadow(elevation = 5.dp, shape = RoundedCornerShape(4.dp))
            .border(width = 1.dp, color = Color(0xFFE2E8F0), shape = RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .testTag("deck_pile_card"),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.5.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(3.dp))
                    .background(baseColor)
                    .border(1.dp, patternColor.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    // Filigree Border
                    drawRect(
                        color = patternColor.copy(alpha = 0.5f),
                        topLeft = Offset(w * 0.08f, h * 0.08f),
                        size = Size(w * 0.84f, h * 0.84f),
                        style = Stroke(width = 1.5f)
                    )
                    // Inner diamond pattern
                    val diamondPath = Path().apply {
                        moveTo(w * 0.5f, h * 0.18f)
                        lineTo(w * 0.82f, h * 0.5f)
                        lineTo(w * 0.5f, h * 0.82f)
                        lineTo(w * 0.18f, h * 0.5f)
                        close()
                    }
                    drawPath(
                        path = diamondPath,
                        color = patternColor.copy(alpha = 0.4f),
                        style = Stroke(width = 1f)
                    )
                    // Rosette / Oval Center
                    drawCircle(
                        color = patternColor.copy(alpha = 0.6f),
                        radius = w * 0.18f,
                        center = Offset(w * 0.5f, h * 0.5f),
                        style = Stroke(width = 1.5f)
                    )
                }

                if (isGreenPattern) {
                    // "RUMMY!" center emblem for Stock
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "RUMMY!",
                            color = Color(0xFFFFD700),
                            fontSize = (cardWidth.value * 0.16f).sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            if (count != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(3.dp))
                        .border(0.5.dp, Color(0xFFFFD700), RoundedCornerShape(3.dp))
                        .padding(horizontal = 3.dp, vertical = 0.5.dp)
                ) {
                    Text(
                        text = "$count",
                        color = Color(0xFFFFD700),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
