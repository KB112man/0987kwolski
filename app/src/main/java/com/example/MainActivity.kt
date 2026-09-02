package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.GameViewModel
import com.example.ui.dialogs.DeckStatsDialog
import com.example.ui.dialogs.MatchHistoryDialog
import com.example.ui.dialogs.RulesDialog
import com.example.ui.screens.GameTableScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val isInMainMenu by viewModel.isInMainMenu.collectAsState()
                    val hasSavedGame by viewModel.hasSavedGame.collectAsState()
                    val showDeckStats by viewModel.showDeckStats.collectAsState()
                    val showRules by viewModel.showRules.collectAsState()
                    val showMatchHistory by viewModel.showMatchHistory.collectAsState()
                    val matchHistoryList by viewModel.matchHistoryList.collectAsState()
                    val gameState by viewModel.gameState.collectAsState()

                    if (isInMainMenu) {
                        MainMenuScreen(
                            hasSavedGame = hasSavedGame,
                            onResumeGame = { viewModel.resumeSavedGame() },
                            onStartNewTournament = { viewModel.startNewTournament() },
                            onMatchHistory = { viewModel.openMatchHistory() },
                            onHowToPlay = { viewModel.openRules() },
                            onDeckStats = { viewModel.openDeckStats() }
                        )

                        if (showDeckStats) {
                            DeckStatsDialog(
                                state = gameState,
                                onDismiss = { viewModel.closeDeckStats() }
                            )
                        }

                        if (showRules) {
                            RulesDialog(
                                onDismiss = { viewModel.closeRules() }
                            )
                        }

                        if (showMatchHistory) {
                            MatchHistoryDialog(
                                historyList = matchHistoryList,
                                onDismiss = { viewModel.closeMatchHistory() }
                            )
                        }
                    } else {
                        GameTableScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
