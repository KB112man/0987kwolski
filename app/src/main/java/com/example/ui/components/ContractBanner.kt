package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ContractLevel
import com.example.ui.theme.*

@Composable
fun ContractBanner(
    currentContract: ContractLevel,
    humanScore: Int = 0,
    currentRound: Int = 1,
    totalRounds: Int = 7,
    turnPlayerName: String = "You",
    isHumanTurn: Boolean = true,
    onScoreboardClicked: () -> Unit,
    onRulesClicked: () -> Unit,
    onMenuClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val woodHeaderGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF381B09),
            Color(0xFF220E04),
            Color(0xFF140702)
        )
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp),
        color = Color(0xFF140702),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5A2E0F))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(woodHeaderGradient)
                .statusBarsPadding()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. PAUSE / MAIN MENU BUTTON
            Surface(
                modifier = Modifier
                    .size(width = 44.dp, height = 50.dp)
                    .shadow(4.dp, RoundedCornerShape(6.dp))
                    .border(1.dp, GoldPlaqueBorder.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                    .clickable { onMenuClicked() }
                    .testTag("btn_top_menu"),
                color = Color(0xFF1C0D05),
                shape = RoundedCornerShape(6.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = GoldPlaqueText,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 2. PANEL 1: LEVEL / CONTRACT
            Surface(
                modifier = Modifier
                    .weight(1.35f)
                    .height(50.dp)
                    .shadow(4.dp, RoundedCornerShape(6.dp))
                    .border(1.dp, GoldPlaqueBorder.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                    .clickable { onRulesClicked() }
                    .testTag("plaque_contract"),
                color = Color(0xFF1C0D05),
                shape = RoundedCornerShape(6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "LEVEL / CONTRACT",
                        color = GoldPlaqueText,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.4.sp,
                        maxLines = 1
                    )
                    Text(
                        text = "Level ${currentContract.levelNumber}",
                        color = Color(0xFFFFF7ED),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = currentContract.shortRequirement,
                        color = GoldPlaqueSubText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            // 3. PANEL 2: YOU / SCORE
            Surface(
                modifier = Modifier
                    .weight(0.9f)
                    .height(50.dp)
                    .shadow(4.dp, RoundedCornerShape(6.dp))
                    .border(1.dp, GoldPlaqueBorder.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                    .clickable { onScoreboardClicked() }
                    .testTag("plaque_you_score"),
                color = Color(0xFF1C0D05),
                shape = RoundedCornerShape(6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "YOU",
                        color = GoldPlaqueText,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.4.sp
                    )
                    Text(
                        text = "$humanScore",
                        color = Color(0xFFFFF7ED),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Score",
                        color = GoldPlaqueSubText,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            // 4. PANEL 3: ROUND
            Surface(
                modifier = Modifier
                    .weight(0.9f)
                    .height(50.dp)
                    .shadow(4.dp, RoundedCornerShape(6.dp))
                    .border(1.dp, GoldPlaqueBorder.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                    .clickable { onScoreboardClicked() }
                    .testTag("plaque_round"),
                color = Color(0xFF1C0D05),
                shape = RoundedCornerShape(6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "ROUND",
                        color = GoldPlaqueText,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.4.sp
                    )
                    Text(
                        text = "$currentRound / $totalRounds",
                        color = Color(0xFFFFF7ED),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Round",
                        color = GoldPlaqueSubText,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            // 5. PANEL 4: TURN
            Surface(
                modifier = Modifier
                    .weight(1.0f)
                    .height(50.dp)
                    .shadow(4.dp, RoundedCornerShape(6.dp))
                    .border(
                        1.dp,
                        if (isHumanTurn) EmeraldAccentLight else GoldPlaqueBorder.copy(alpha = 0.8f),
                        RoundedCornerShape(6.dp)
                    )
                    .testTag("plaque_turn"),
                color = if (isHumanTurn) Color(0xFF072412) else Color(0xFF1C0D05),
                shape = RoundedCornerShape(6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "TURN",
                        color = GoldPlaqueText,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.4.sp
                    )
                    Text(
                        text = turnPlayerName,
                        color = if (isHumanTurn) EmeraldAccentLight else Color(0xFFFFF7ED),
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isHumanTurn) Color(0xFF22C55E) else Color(0xFFEAB308))
                        )
                        Text(
                            text = if (isHumanTurn) "Your Turn" else "Waiting",
                            color = if (isHumanTurn) Color(0xFF86EFAC) else Color(0xFFCBD5E1),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
