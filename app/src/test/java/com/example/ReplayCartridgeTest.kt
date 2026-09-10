package com.example.model.replay

import com.example.engine.DeckEngine
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ReplayCartridgeTest {

    private fun createValidCanonical108(): List<String> {
        return DeckEngine.createFullDeck().map { it.id }
    }

    @Test
    fun testCartridgeSerializationV1RoundTrip() {
        val deck108 = createValidCanonical108()
        assertEquals(108, deck108.size)

        val cartridge = ReplayCartridge(
            schemaVersion = 1,
            matchId = "test-match-12345",
            tournamentId = "tourney-001",
            createdAtUtc = "2026-09-10T01:45:00Z",
            buildVersion = "1.0.8",
            rulesVersion = "2026-09",
            levelNumber = 1,
            players = listOf(
                ReplayPlayer(playerId = "p_human", displayName = "Kwush", seatIndex = 0, isHuman = true),
                ReplayPlayer(playerId = "p_cpu1", displayName = "Bot Alpha", seatIndex = 1, isHuman = false)
            ),
            initialDeckOrder = deck108,
            actionLog = listOf(
                ReplayEvent.Draw(
                    playerId = "p_human",
                    cardId = deck108[21], // 10 cards to p1, 10 cards to p2, 1 card to discard = index 21 is first stock card
                    source = DrawSource.STOCK
                ),
                ReplayEvent.Discard(
                    playerId = "p_human",
                    cardId = deck108[0] // Card dealt to p_human
                )
            )
        )

        val json = CartridgeSerializer.toJson(cartridge)
        assertTrue("Serialized JSON must contain explicit DRAW discriminator", json.contains("\"type\": \"DRAW\""))
        assertTrue("Serialized JSON must contain explicit DISCARD discriminator", json.contains("\"type\": \"DISCARD\""))
        assertTrue("Serialized JSON must contain schemaVersion 1", json.contains("\"schemaVersion\": 1"))

        val result = CartridgeSerializer.parseJson(json)
        assertTrue("Parsing valid cartridge must succeed", result is CartridgeLoadResult.Success)
        val loaded = (result as CartridgeLoadResult.Success).cartridge

        assertEquals(1, loaded.schemaVersion)
        assertEquals("test-match-12345", loaded.matchId)
        assertEquals(108, loaded.initialDeckOrder.size)
        assertEquals(2, loaded.actionLog.size)
        assertTrue("First event is Draw", loaded.actionLog[0] is ReplayEvent.Draw)
        assertTrue("Second event is Discard", loaded.actionLog[1] is ReplayEvent.Discard)
    }

    @Test
    fun testRejectsNewerSchemaVersionGracefully() {
        val deck108 = createValidCanonical108()
        val jsonFuture = """
            {
              "schemaVersion": 2,
              "matchId": "future-match",
              "createdAtUtc": "2026-09-10T01:45:00Z",
              "buildVersion": "2.0.0",
              "rulesVersion": "2027-01",
              "levelNumber": 1,
              "players": [],
              "initialDeckOrder": [],
              "actionLog": []
            }
        """.trimIndent()

        val result = CartridgeSerializer.parseJson(jsonFuture)
        assertTrue("Must return Error for future schemaVersion", result is CartridgeLoadResult.Error)
        val error = (result as CartridgeLoadResult.Error).message
        assertEquals("Replay was created by a newer version of Ultimate Rummay!.", error)
    }

    @Test
    fun testRejectsUnknownEventTypesGracefully() {
        val deck108 = createValidCanonical108()
        val deckJson = deck108.joinToString(",") { "\"$it\"" }
        val jsonWithUnknownEvent = """
            {
              "schemaVersion": 1,
              "matchId": "unknown-event-match",
              "createdAtUtc": "2026-09-10T01:45:00Z",
              "buildVersion": "1.0.8",
              "rulesVersion": "2026-09",
              "levelNumber": 1,
              "players": [
                {
                  "playerId": "p_human",
                  "displayName": "Player",
                  "seatIndex": 0,
                  "isHuman": true
                }
              ],
              "initialDeckOrder": [$deckJson],
              "actionLog": [
                {
                  "type": "FUTURE_SUPER_MELD",
                  "playerId": "p_human"
                }
              ]
            }
        """.trimIndent()

        val result = CartridgeSerializer.parseJson(jsonWithUnknownEvent)
        assertTrue("Must return Error for unknown event type", result is CartridgeLoadResult.Error)
        val error = (result as CartridgeLoadResult.Error).message
        assertTrue("Error message must state unsupported event type",
            error.contains("unsupported event type", ignoreCase = true) || error.contains("damaged or incompatible", ignoreCase = true))
    }

    @Test
    fun testValidationEnforcesExact108Cards() {
        val incompleteDeck = createValidCanonical108().take(107)
        val cartridge = ReplayCartridge(
            schemaVersion = 1,
            matchId = "invalid-deck-size",
            createdAtUtc = "2026-09-10T01:45:00Z",
            levelNumber = 1,
            players = listOf(ReplayPlayer("p1", "P1", 0, true)),
            initialDeckOrder = incompleteDeck,
            actionLog = emptyList()
        )

        val json = CartridgeSerializer.toJson(cartridge)
        val result = CartridgeSerializer.parseJson(json)
        assertTrue("Must fail validation when deck does not have 108 cards", result is CartridgeLoadResult.Error)
        val error = (result as CartridgeLoadResult.Error).message
        assertTrue("Error must mention 108 card IDs", error.contains("108 card IDs"))
    }

    @Test
    fun testAtomicFileSaveOperation() {
        val deck108 = createValidCanonical108()
        val cartridge = ReplayCartridge(
            schemaVersion = 1,
            matchId = "atomic-save-test",
            createdAtUtc = "2026-09-10T01:45:00Z",
            levelNumber = 1,
            players = listOf(ReplayPlayer("p1", "P1", 0, true)),
            initialDeckOrder = deck108,
            actionLog = emptyList()
        )

        val tempTarget = File.createTempFile("cartridge_test", ".json")
        try {
            val saveResult = CartridgeSerializer.writeCartridgeSafely(cartridge, tempTarget)
            assertTrue("Safe write must succeed", saveResult is CartridgeLoadResult.Success)
            assertTrue("Target file must exist", tempTarget.exists())

            val readResult = CartridgeSerializer.parseJson(tempTarget.readText())
            assertTrue("Reading saved file must succeed", readResult is CartridgeLoadResult.Success)
            assertEquals("atomic-save-test", (readResult as CartridgeLoadResult.Success).cartridge.matchId)
        } finally {
            tempTarget.delete()
        }
    }
}
