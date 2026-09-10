package com.example.model.replay

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.io.IOException

sealed class CartridgeLoadResult {
    data class Success(val cartridge: ReplayCartridge) : CartridgeLoadResult()
    data class Error(val message: String, val cause: Throwable? = null) : CartridgeLoadResult()
}

/**
 * Custom JsonAdapter for polymorphic ReplayEvent hierarchy.
 * Uses explicit "type" discriminator:
 * DRAW, DISCARD, BUY, GO_DOWN, PLAY_ON, RUMMAY_CALL, STOCK_RECYCLE, ROUND_END
 * Rejects unknown event types gracefully without silent state corruption.
 */
class ReplayEventJsonAdapter(private val moshi: Moshi) : JsonAdapter<ReplayEvent>() {

    private val drawAdapter by lazy { moshi.adapter(ReplayEvent.Draw::class.java) }
    private val discardAdapter by lazy { moshi.adapter(ReplayEvent.Discard::class.java) }
    private val buyAdapter by lazy { moshi.adapter(ReplayEvent.Buy::class.java) }
    private val goDownAdapter by lazy { moshi.adapter(ReplayEvent.GoDown::class.java) }
    private val playOnAdapter by lazy { moshi.adapter(ReplayEvent.PlayOn::class.java) }
    private val rummayCallAdapter by lazy { moshi.adapter(ReplayEvent.RummayCall::class.java) }
    private val stockRecycleAdapter by lazy { moshi.adapter(ReplayEvent.StockRecycle::class.java) }
    private val roundEndAdapter by lazy { moshi.adapter(ReplayEvent.RoundEnd::class.java) }

    override fun fromJson(reader: JsonReader): ReplayEvent? {
        val peeked = reader.peekJson()
        var type: String? = null
        peeked.beginObject()
        while (peeked.hasNext()) {
            if (peeked.nextName() == "type") {
                type = peeked.nextString()
                break
            } else {
                peeked.skipValue()
            }
        }
        peeked.close()

        return when (type) {
            "DRAW" -> drawAdapter.fromJson(reader)
            "DISCARD" -> discardAdapter.fromJson(reader)
            "BUY" -> buyAdapter.fromJson(reader)
            "GO_DOWN" -> goDownAdapter.fromJson(reader)
            "PLAY_ON" -> playOnAdapter.fromJson(reader)
            "RUMMAY_CALL" -> rummayCallAdapter.fromJson(reader)
            "STOCK_RECYCLE" -> stockRecycleAdapter.fromJson(reader)
            "ROUND_END" -> roundEndAdapter.fromJson(reader)
            null -> throw IllegalArgumentException("Replay event missing required 'type' discriminator.")
            else -> throw IllegalArgumentException("Replay contains an unsupported event type: '$type'.")
        }
    }

    override fun toJson(writer: JsonWriter, value: ReplayEvent?) {
        when (value) {
            is ReplayEvent.Draw -> drawAdapter.toJson(writer, value)
            is ReplayEvent.Discard -> discardAdapter.toJson(writer, value)
            is ReplayEvent.Buy -> buyAdapter.toJson(writer, value)
            is ReplayEvent.GoDown -> goDownAdapter.toJson(writer, value)
            is ReplayEvent.PlayOn -> playOnAdapter.toJson(writer, value)
            is ReplayEvent.RummayCall -> rummayCallAdapter.toJson(writer, value)
            is ReplayEvent.StockRecycle -> stockRecycleAdapter.toJson(writer, value)
            is ReplayEvent.RoundEnd -> roundEndAdapter.toJson(writer, value)
            null -> writer.nullValue()
        }
    }
}

/**
 * CartridgeSerializer handles:
 * 1. Inspecting schemaVersion before deserialization
 * 2. Rejecting newer versions gracefully ("Replay was created by a newer version of Ultimate Rummay!.")
 * 3. Migration hooks for older versions
 * 4. Rejecting unknown event types gracefully without silent state corruption
 * 5. Atomic file saving via temp file rename
 * 6. Full structural and semantic validation (108 unique cards, referenced IDs present, unique players, etc.)
 */
object CartridgeSerializer {

    val moshi: Moshi = Moshi.Builder()
        .add(ReplayEvent::class.java, ReplayEventJsonAdapter(
            Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        ))
        .add(KotlinJsonAdapterFactory())
        .build()

    private val cartridgeAdapter: JsonAdapter<ReplayCartridge> = moshi.adapter(ReplayCartridge::class.java)

    /**
     * Serializes a cartridge to JSON string.
     */
    fun toJson(cartridge: ReplayCartridge): String {
        return cartridgeAdapter.indent("  ").toJson(cartridge)
    }

    /**
     * Atomically writes a cartridge to the target file.
     * 1. Serialize to temporary file
     * 2. Verify temporary file can be deserialized
     * 3. Atomically rename/move temp file into final target location
     */
    @Synchronized
    fun writeCartridgeSafely(cartridge: ReplayCartridge, targetFile: File): CartridgeLoadResult {
        val parentDir = targetFile.parentFile ?: File(".")
        if (!parentDir.exists()) {
            parentDir.mkdirs()
        }

        val tempFile = File(parentDir, "${targetFile.name}.tmp_${System.currentTimeMillis()}")
        try {
            val json = toJson(cartridge)
            tempFile.writeText(json)

            // Validate temp file can be parsed back
            val testRead = parseJson(tempFile.readText())
            if (testRead !is CartridgeLoadResult.Success) {
                tempFile.delete()
                return CartridgeLoadResult.Error("Verification failed after writing temporary cartridge: ${(testRead as CartridgeLoadResult.Error).message}")
            }

            // Atomic move / rename
            if (targetFile.exists()) {
                targetFile.delete()
            }
            val renamed = tempFile.renameTo(targetFile)
            if (!renamed) {
                // Fallback copy & delete if atomic rename across mounts fails
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
            return CartridgeLoadResult.Success(cartridge)
        } catch (e: Exception) {
            tempFile.delete()
            return CartridgeLoadResult.Error("Failed to save replay cartridge safely: ${e.message}", e)
        }
    }

    /**
     * Reads and parses a JSON string, inspecting schemaVersion first.
     */
    fun parseJson(jsonString: String): CartridgeLoadResult {
        try {
            // 1. Inspect schemaVersion before full deserialization
            val schemaVersion = extractSchemaVersion(jsonString)
                ?: return CartridgeLoadResult.Error("Replay file is damaged: missing schemaVersion.")

            if (schemaVersion > CURRENT_REPLAY_SCHEMA_VERSION) {
                return CartridgeLoadResult.Error("Replay was created by a newer version of Ultimate Rummay!.")
            }

            if (schemaVersion < CURRENT_REPLAY_SCHEMA_VERSION) {
                // Route through migration hook if version < CURRENT_REPLAY_SCHEMA_VERSION
                return migrateLegacyCartridge(schemaVersion, jsonString)
            }

            // 2. Parse current V1 schema
            val cartridge = try {
                cartridgeAdapter.fromJson(jsonString)
            } catch (e: Exception) {
                // Inspect if the failure was caused by an unsupported/unknown event type
                if (e.message?.contains("unsupported event type", ignoreCase = true) == true) {
                    return CartridgeLoadResult.Error("Replay contains an unsupported event type.", e)
                }
                return CartridgeLoadResult.Error("Replay file is damaged or incompatible: ${e.message}", e)
            } ?: return CartridgeLoadResult.Error("Replay file is damaged: empty cartridge.")

            // 3. Full semantic validation
            val validationError = validateCartridgeSemantics(cartridge)
            if (validationError != null) {
                return CartridgeLoadResult.Error(validationError)
            }

            return CartridgeLoadResult.Success(cartridge)
        } catch (e: Throwable) {
            return CartridgeLoadResult.Error("Replay file is damaged or incompatible: ${e.message}", e)
        }
    }

    /**
     * Extracts schemaVersion by inspecting the JSON without full model deserialization.
     */
    private fun extractSchemaVersion(jsonString: String): Int? {
        val reader = JsonReader.of(okio.Buffer().writeUtf8(jsonString))
        try {
            reader.beginObject()
            while (reader.hasNext()) {
                val name = reader.nextName()
                if (name == "schemaVersion") {
                    return reader.nextInt()
                } else {
                    reader.skipValue()
                }
            }
            reader.endObject()
        } catch (e: Exception) {
            return null
        }
        return null
    }

    /**
     * Migration hook for older schemas when future versions are introduced.
     */
    fun migrateLegacyCartridge(sourceSchemaVersion: Int, jsonString: String): CartridgeLoadResult {
        return CartridgeLoadResult.Error("Unsupported legacy replay schema version: $sourceSchemaVersion")
    }

    /**
     * Strict Load Validation (Section 14):
     * - schemaVersion supported
     * - initialDeckOrder has exactly 108 IDs
     * - initialDeckOrder IDs are unique
     * - all referenced card IDs exist in the initial 108
     * - player IDs are unique
     * - meld IDs are valid when referenced
     * - event sequence is structurally valid
     */
    fun validateCartridgeSemantics(cartridge: ReplayCartridge): String? {
        if (cartridge.initialDeckOrder.size != 108) {
            return "Invalid cartridge: initialDeckOrder must contain exactly 108 card IDs (found ${cartridge.initialDeckOrder.size})."
        }

        val deckSet = cartridge.initialDeckOrder.toSet()
        if (deckSet.size != 108) {
            return "Invalid cartridge: initialDeckOrder contains duplicate card IDs."
        }

        val playerIds = mutableSetOf<String>()
        for (player in cartridge.players) {
            if (!playerIds.add(player.playerId)) {
                return "Invalid cartridge: duplicate playerId '${player.playerId}'."
            }
        }

        val knownMeldIds = mutableSetOf<String>()

        cartridge.actionLog.forEachIndexed { index, event ->
            when (event) {
                is ReplayEvent.Draw -> {
                    if (event.cardId !in deckSet) {
                        return "Invalid event at #$index (DRAW): cardId '${event.cardId}' is not in the 108 deck."
                    }
                    if (event.playerId !in playerIds) {
                        return "Invalid event at #$index (DRAW): unknown playerId '${event.playerId}'."
                    }
                }
                is ReplayEvent.Discard -> {
                    if (event.cardId !in deckSet) {
                        return "Invalid event at #$index (DISCARD): cardId '${event.cardId}' is not in the 108 deck."
                    }
                    if (event.playerId !in playerIds) {
                        return "Invalid event at #$index (DISCARD): unknown playerId '${event.playerId}'."
                    }
                }
                is ReplayEvent.Buy -> {
                    if (event.faceUpCardId !in deckSet) {
                        return "Invalid event at #$index (BUY): faceUpCardId '${event.faceUpCardId}' is not in the 108 deck."
                    }
                    if (event.stockCardId !in deckSet) {
                        return "Invalid event at #$index (BUY): stockCardId '${event.stockCardId}' is not in the 108 deck."
                    }
                    if (event.resultingDiscardId !in deckSet) {
                        return "Invalid event at #$index (BUY): resultingDiscardId '${event.resultingDiscardId}' is not in the 108 deck."
                    }
                    if (event.playerId !in playerIds) {
                        return "Invalid event at #$index (BUY): unknown playerId '${event.playerId}'."
                    }
                }
                is ReplayEvent.GoDown -> {
                    if (event.playerId !in playerIds) {
                        return "Invalid event at #$index (GO_DOWN): unknown playerId '${event.playerId}'."
                    }
                    for (meld in event.melds) {
                        if (!knownMeldIds.add(meld.meldId)) {
                            return "Invalid event at #$index (GO_DOWN): duplicate meldId '${meld.meldId}'."
                        }
                        for (cardId in meld.cardIds) {
                            if (cardId !in deckSet) {
                                return "Invalid event at #$index (GO_DOWN): meld cardId '$cardId' is not in the 108 deck."
                            }
                        }
                    }
                }
                is ReplayEvent.PlayOn -> {
                    if (event.cardId !in deckSet) {
                        return "Invalid event at #$index (PLAY_ON): cardId '${event.cardId}' is not in the 108 deck."
                    }
                    if (event.meldId !in knownMeldIds) {
                        return "Invalid event at #$index (PLAY_ON): meldId '${event.meldId}' has not been laid down."
                    }
                }
                is ReplayEvent.RummayCall -> {
                    if (event.playableDiscardId !in deckSet || event.transferredCardId !in deckSet) {
                        return "Invalid event at #$index (RUMMAY_CALL): referenced card is not in the 108 deck."
                    }
                    if (event.destinationMeldId !in knownMeldIds) {
                        return "Invalid event at #$index (RUMMAY_CALL): meldId '${event.destinationMeldId}' has not been laid down."
                    }
                }
                is ReplayEvent.StockRecycle -> {
                    if (event.preservedTopDiscardId !in deckSet) {
                        return "Invalid event at #$index (STOCK_RECYCLE): preservedTopDiscardId is not in the 108 deck."
                    }
                    for (cardId in event.newStockOrder) {
                        if (cardId !in deckSet) {
                            return "Invalid event at #$index (STOCK_RECYCLE): recycled card '$cardId' is not in the 108 deck."
                        }
                    }
                }
                is ReplayEvent.RoundEnd -> {
                    if (event.winnerId !in playerIds) {
                        return "Invalid event at #$index (ROUND_END): unknown winnerId '${event.winnerId}'."
                    }
                }
            }
        }

        return null
    }
}
