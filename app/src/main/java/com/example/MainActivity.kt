package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.GameViewModel
import com.example.ui.dialogs.MatchHistoryDialog
import com.example.ui.dialogs.RulesDialog
import com.example.ui.screens.GameScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.theme.MyApplicationTheme

enum class AppScreen {
    MAIN_MENU,
    GAME
}

class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(darkTheme = true, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F0602)
                ) {
                    var currentScreen by remember { mutableStateOf(AppScreen.MAIN_MENU) }
                    val savedGame by gameViewModel.savedMatchState.collectAsState()
                    val matchHistory by gameViewModel.matchHistory.collectAsState()

                    var showMainMenuHistory by remember { mutableStateOf(false) }
                    var showMainMenuRules by remember { mutableStateOf(false) }

                    when (currentScreen) {
                        AppScreen.MAIN_MENU -> {
                            MainMenuScreen(
                                savedGameState = savedGame,
                                hasHistory = matchHistory.isNotEmpty(),
                                onStartNewTournament = {
                                    gameViewModel.startNewTournament("You")
                                    currentScreen = AppScreen.GAME
                                },
                                onResumeGame = {
                                    gameViewModel.resumeSavedTournament()
                                    currentScreen = AppScreen.GAME
                                },
                                onViewHistory = {
                                    showMainMenuHistory = true
                                },
                                onViewRules = {
                                    showMainMenuRules = true
                                }
                            )

                            if (showMainMenuHistory) {
                                MatchHistoryDialog(
                                    historyList = matchHistory,
                                    onDismiss = { showMainMenuHistory = false }
                                )
                            }

                            if (showMainMenuRules) {
                                RulesDialog(
                                    onDismiss = { showMainMenuRules = false }
                                )
                            }
                        }
                        AppScreen.GAME -> {
                            BackHandler {
                                gameViewModel.setPauseDialogVisible(true)
                            }

                            GameScreen(
                                viewModel = gameViewModel,
                                onNavigateToMainMenu = {
                                    currentScreen = AppScreen.MAIN_MENU
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
