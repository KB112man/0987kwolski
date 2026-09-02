package com.example.model

data class FinalStandingItem(
    val name: String = "",
    val score: Int = 0
)

data class MatchPlayerStanding(
    val playerId: String = "",
    val name: String = "",
    val isHuman: Boolean = false,
    val rank: Int = 1,
    val totalScore: Int = 0,
    val scoresPerLevel: List<Int> = emptyList()
)

data class MatchHistoryEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val formattedDate: String = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
    val humanPlayerName: String = "You",
    val humanFinalRank: Int = 1,
    val humanFinalScore: Int = 0,
    val humanAchievement: String = "Tournament Completed",
    val isGrandChampion: Boolean = false,
    val winnerName: String = "",
    val winnerScore: Int = 0,
    val humanScore: Int = 0,
    val humanWon: Boolean = false,
    val winnerIsHuman: Boolean = false,
    val highestLevelCompleted: Int = 7,
    val standings: List<MatchPlayerStanding> = emptyList(),
    val finalStandings: List<FinalStandingItem> = emptyList()
)
