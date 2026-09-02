package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun BottomTableNavBar(
    onScoreClicked: () -> Unit,
    onHistoryClicked: () -> Unit,
    onDeckStatsClicked: () -> Unit,
    onRulesClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF381B09),
            Color(0xFF220E04),
            Color(0xFF140702)
        )
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp),
        color = Color(0xFF140702),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5A2E0F))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(navGradient)
                .navigationBarsPadding()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. SCORE
            NavPlaqueTab(
                icon = Icons.Default.EmojiEvents,
                label = "SCORE",
                isActive = false,
                onClick = onScoreClicked,
                testTag = "nav_tab_score",
                modifier = Modifier.weight(1f)
            )

            // 2. HISTORY
            NavPlaqueTab(
                icon = Icons.Default.History,
                label = "HISTORY",
                isActive = false,
                onClick = onHistoryClicked,
                testTag = "nav_tab_history",
                modifier = Modifier.weight(1f)
            )

            // 3. TABLE (Active Highlighted Tab)
            NavPlaqueTab(
                icon = Icons.Default.Dashboard,
                label = "TABLE",
                isActive = true,
                onClick = { },
                testTag = "nav_tab_table",
                modifier = Modifier.weight(1.05f)
            )

            // 4. 108 DECK
            NavPlaqueTab(
                icon = Icons.Default.Layers,
                label = "108 DECK",
                isActive = false,
                onClick = onDeckStatsClicked,
                testTag = "nav_tab_deck",
                modifier = Modifier.weight(1.05f)
            )

            // 5. HELP / RULES
            NavPlaqueTab(
                icon = Icons.Default.HelpOutline,
                label = "HELP / RULES",
                isActive = false,
                onClick = onRulesClicked,
                testTag = "nav_tab_rules",
                modifier = Modifier.weight(1.15f)
            )
        }
    }
}

@Composable
private fun NavPlaqueTab(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(46.dp)
            .padding(horizontal = 2.dp)
            .shadow(if (isActive) 6.dp else 1.dp, RoundedCornerShape(8.dp))
            .border(
                if (isActive) 1.5.dp else 0.75.dp,
                if (isActive) GoldPlaqueBorder else Color(0xFF381B09),
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .testTag(testTag),
        color = if (isActive) Color(0xFF261205) else Color(0xFF140803),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) GoldPlaqueText else Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = if (isActive) GoldPlaqueText else Color(0xFFCBD5E1),
                fontSize = 9.sp,
                fontWeight = if (isActive) FontWeight.Black else FontWeight.Bold,
                letterSpacing = 0.4.sp,
                maxLines = 1
            )
        }
    }
}
