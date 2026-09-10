package com.example.model

enum class CardZone {
    STOCK,
    DISCARD,
    PLAYER_HAND,
    TABLE_MELD
}

data class CardLocation(
    val cardId: String,
    val zone: CardZone,
    val playerId: String? = null,
    val playerName: String? = null,
    val meldId: String? = null,
    val pocketed: Boolean = false
)

data class CardIntegrityReport(
    val totalReferences: Int,
    val uniqueIds: Int,
    val missingIds: Set<String>,
    val duplicatedIds: Set<String>,
    val locationsByCardId: Map<String, List<CardLocation>>,
    val isValid: Boolean,
    val actionName: String = ""
) {
    fun formatErrorMessage(): String {
        val sb = StringBuilder()
        sb.appendLine("============================================================")
        sb.appendLine("CARD INTEGRITY FAILURE DETECTED")
        if (actionName.isNotEmpty()) {
            sb.appendLine("Triggering Action: $actionName")
        }
        sb.appendLine("Total Count: $totalReferences / 108")
        sb.appendLine("Unique IDs: $uniqueIds / 108")
        sb.appendLine("Missing IDs (${missingIds.size}): ${missingIds.joinToString(", ")}")
        sb.appendLine("Duplicated IDs (${duplicatedIds.size}): ${duplicatedIds.joinToString(", ")}")
        if (duplicatedIds.isNotEmpty()) {
            sb.appendLine("Duplicated Card Locations:")
            duplicatedIds.forEach { id ->
                val locs = locationsByCardId[id] ?: emptyList()
                sb.appendLine("  Card $id appears in ${locs.size} places: ${locs.map { "${it.zone}(player=${it.playerName ?: it.playerId}, meld=${it.meldId}, pocket=${it.pocketed})" }}")
            }
        }
        sb.appendLine("============================================================")
        return sb.toString()
    }
}
