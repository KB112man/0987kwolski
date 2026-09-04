package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.example.model.Meld

@Composable
fun SpatialPlayerMeldsView(
    melds: List<Meld>,
    isVerticalStack: Boolean = false,
    onMeldClicked: (String) -> Unit,
    isHumanDown: Boolean,
    hoveredMeldId: String? = null,
    onRegisterMeldBounds: (String, Rect) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    if (melds.isEmpty()) return

    if (isVerticalStack) {
        Column(
            modifier = modifier.padding(horizontal = 2.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            melds.forEach { meld ->
                val isHovered = hoveredMeldId == meld.id
                SpatialMeldCard(
                    meld = meld,
                    onMeldClicked = { onMeldClicked(meld.id) },
                    isInteractive = isHumanDown,
                    isHovered = isHovered,
                    onBoundsMeasured = { rect -> onRegisterMeldBounds(meld.id, rect) }
                )
            }
        }
    } else {
        Row(
            modifier = modifier
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            melds.forEach { meld ->
                val isHovered = hoveredMeldId == meld.id
                SpatialMeldCard(
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
fun SpatialMeldCard(
    meld: Meld,
    onMeldClicked: () -> Unit,
    isInteractive: Boolean,
    isHovered: Boolean,
    onBoundsMeasured: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val trayBorderColor = when {
        isHovered -> Color(0xFF22C55E)
        isInteractive -> Color(0xFFFFD700).copy(alpha = 0.7f)
        else -> Color(0xFF5A2E0F).copy(alpha = 0.5f)
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coords -> onBoundsMeasured(coords.boundsInRoot()) }
            .shadow(if (isHovered) 8.dp else 4.dp, RoundedCornerShape(6.dp))
            .border(
                if (isHovered) 2.5.dp else if (isInteractive) 1.2.dp else 0.75.dp,
                trayBorderColor,
                RoundedCornerShape(6.dp)
            )
            .background(
                if (isHovered) Color(0xFF0F3B20) else Color(0xFF140803).copy(alpha = 0.85f),
                RoundedCornerShape(6.dp)
            )
            .clickable(enabled = isInteractive) { onMeldClicked() }
            .padding(horizontal = 4.dp, vertical = 3.dp)
            .testTag("spatial_meld_${meld.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((-32).dp)
        ) {
            meld.cards.forEach { card ->
                PlayingCardView(
                    card = card,
                    cardWidth = 64.dp,
                    cardHeight = 90.dp,
                    showPointsBadge = false
                )
            }
        }
    }
}
