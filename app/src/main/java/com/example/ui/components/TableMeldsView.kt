package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Meld
import com.example.model.MeldType
import com.example.ui.theme.*

@Composable
fun TableMeldsView(
    melds: List<Meld>,
    onMeldClicked: (String) -> Unit,
    isHumanDown: Boolean,
    hasSelectedCards: Boolean,
    hoveredMeldId: String? = null,
    onRegisterMeldBounds: (String, Rect) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    if (melds.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .height(48.dp)
                .background(Color(0x33000000), RoundedCornerShape(8.dp))
                .border(0.75.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Table Melds Area (Books & Runs appear here when players Go Down)",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            melds.forEach { meld ->
                val isHovered = hoveredMeldId == meld.id
                PublicMeldDisplay(
                    meld = meld,
                    onMeldClicked = { onMeldClicked(meld.id) },
                    isInteractive = isHumanDown,
                    isHovered = isHovered,
                    onBoundsMeasured = { rect -> onRegisterMeldBounds(meld.id, rect) }
                )
            }
        }
    }
}

@Composable
fun PublicMeldDisplay(
    meld: Meld,
    onMeldClicked: () -> Unit,
    isInteractive: Boolean,
    isHovered: Boolean,
    onBoundsMeasured: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val typeLabel = if (meld.type == MeldType.BOOK) "Book" else "Run"
    val suitOrRankSymbol = if (meld.type == MeldType.BOOK) {
        meld.bookRank?.shortName ?: ""
    } else {
        meld.runSuit?.symbol ?: ""
    }
    val suitColor = if (meld.type == MeldType.RUN && meld.runSuit?.isRed == true) SuitRed else EmeraldAccentLight
    val trayBorderColor = when {
        isHovered -> Color(0xFF22C55E)
        isInteractive -> Color(0xFFFFD700)
        else -> Color(0xFF6B3A1C).copy(alpha = 0.6f)
    }

    Surface(
        modifier = modifier
            .onGloballyPositioned { coords -> onBoundsMeasured(coords.boundsInRoot()) }
            .shadow(if (isHovered) 8.dp else 4.dp, RoundedCornerShape(6.dp))
            .clickable(enabled = isInteractive) { onMeldClicked() }
            .testTag("meld_${meld.id}"),
        color = if (isHovered) Color(0xFF0D3B20) else Color(0xFF180A04),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(
            if (isHovered) 2.dp else if (isInteractive) 1.2.dp else 0.75.dp,
            trayBorderColor
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Text(
                    text = meld.ownerName,
                    color = EmeraldAccentLight,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "•",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 8.sp
                )
                Text(
                    text = "$typeLabel $suitOrRankSymbol",
                    color = if (suitColor == SuitRed) Color(0xFFF87171) else Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((-52).dp)
            ) {
                meld.cards.forEach { card ->
                    PlayingCardView(
                        card = card,
                        cardWidth = 86.dp,
                        cardHeight = 124.dp,
                        showPointsBadge = false
                    )
                }
            }
        }
    }
}
