package com.example.model.replay

import com.squareup.moshi.JsonClass

/**
 * Replay Cartridge V1 Specification
 *
 * Exact Initial Condition (108 card IDs) + Chronological Authoritative Events.
 * Completely decoupled from transient GameState/Player UI domains.
 */

const val CURRENT_REPLAY_SCHEMA_VERSION = 1
const val CURRENT_REPLAY_RULES_VERSION = "2026-09"
const val CURRENT_BUILD_VERSION = "1.0.8"

@JsonClass(generateAdapter = true)
data class ReplayCartridge(
    val schemaVersion: Int = CURRENT_REPLAY_SCHEMA_VERSION,
    val matchId: String,
    val tournamentId: String? = null,
    val createdAtUtc: String,
    val buildVersion: String = CURRENT_BUILD_VERSION,
    val rulesVersion: String = CURRENT_REPLAY_RULES_VERSION,
    val levelNumber: Int,
    val players: List<ReplayPlayer>,
    val initialDeckOrder: List<String>,
    val actionLog: List<ReplayEvent>
)

@JsonClass(generateAdapter = true)
data class ReplayPlayer(
    val playerId: String,
    val displayName: String,
    val seatIndex: Int,
    val isHuman: Boolean
)

enum class ReplayMeldType {
    BOOK,
    RUN
}

@JsonClass(generateAdapter = true)
data class ReplayMeld(
    val meldId: String,
    val ownerId: String,
    val meldType: ReplayMeldType,
    val cardIds: List<String>
)

enum class DrawSource {
    STOCK,
    DISCARD
}

/**
 * Polymorphic Event Base Contract.
 * Serializes with an explicit "type" discriminator:
 * DRAW, DISCARD, BUY, GO_DOWN, PLAY_ON, RUMMAY_CALL, STOCK_RECYCLE, ROUND_END
 */
sealed interface ReplayEvent {
    val type: String

    @JsonClass(generateAdapter = true)
    data class Draw(
        val playerId: String,
        val cardId: String,
        val source: DrawSource,
        override val type: String = "DRAW"
    ) : ReplayEvent

    @JsonClass(generateAdapter = true)
    data class Discard(
        val playerId: String,
        val cardId: String,
        override val type: String = "DISCARD"
    ) : ReplayEvent

    @JsonClass(generateAdapter = true)
    data class Buy(
        val playerId: String,
        val faceUpCardId: String,
        val stockCardId: String,
        val resultingDiscardId: String,
        override val type: String = "BUY"
    ) : ReplayEvent

    @JsonClass(generateAdapter = true)
    data class GoDown(
        val playerId: String,
        val melds: List<ReplayMeld>,
        override val type: String = "GO_DOWN"
    ) : ReplayEvent

    @JsonClass(generateAdapter = true)
    data class PlayOn(
        val playerId: String,
        val meldId: String,
        val cardId: String,
        override val type: String = "PLAY_ON"
    ) : ReplayEvent

    @JsonClass(generateAdapter = true)
    data class RummayCall(
        val callerId: String,
        val offenderId: String,
        val playableDiscardId: String,
        val destinationMeldId: String,
        val transferredCardId: String,
        override val type: String = "RUMMAY_CALL"
    ) : ReplayEvent

    @JsonClass(generateAdapter = true)
    data class StockRecycle(
        val preservedTopDiscardId: String,
        val newStockOrder: List<String>,
        override val type: String = "STOCK_RECYCLE"
    ) : ReplayEvent

    @JsonClass(generateAdapter = true)
    data class RoundEnd(
        val winnerId: String,
        val levelNumber: Int,
        override val type: String = "ROUND_END"
    ) : ReplayEvent
}
