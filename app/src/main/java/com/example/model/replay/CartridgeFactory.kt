package com.example.model.replay

import com.example.engine.DeckEngine
import com.example.model.ContractLevel
import java.util.UUID

/**
 * Provides demo/sample cartridges or builders for verified cartridges.
 */
object CartridgeFactory {

    /**
     * Builds a realistic V1 demonstration cartridge for replay inspection.
     * Contains all 108 permanent card IDs, realistic initial deal for Level 1,
     * human and AI players, and chronological public events:
     * - Stock draws
     * - Discards
     * - Discard pickup
     * - Go Down with Books
     * - Play On table meld
     * - Round End
     */
    fun createSampleCartridge(
        matchId: String = UUID.randomUUID().toString(),
        humanName: String = "Kwush"
    ): ReplayCartridge {
        val canonical108 = DeckEngine.createFullDeck().map { it.id }

        val players = listOf(
            ReplayPlayer(
                playerId = "p_human",
                displayName = humanName,
                seatIndex = 0,
                isHuman = true
            ),
            ReplayPlayer(
                playerId = "p_cpu1",
                displayName = "Rothgar",
                seatIndex = 1,
                isHuman = false
            ),
            ReplayPlayer(
                playerId = "p_cpu2",
                displayName = "Xander",
                seatIndex = 2,
                isHuman = false
            )
        )

        // Level 1 Deal: 10 cards to each of 3 players:
        // p_human gets indices: 0, 3, 6, 9, 12, 15, 18, 21, 24, 27 (10 cards)
        // p_cpu1 gets indices:  1, 4, 7, 10, 13, 16, 19, 22, 25, 28 (10 cards)
        // p_cpu2 gets indices:  2, 5, 8, 11, 14, 17, 20, 23, 26, 29 (10 cards)
        // Discard pile gets index 30 (1 card)
        // Stock begins at index 31 (77 cards)

        val stockCard1 = canonical108[31]
        val stockCard2 = canonical108[32]
        val stockCard3 = canonical108[33]
        val stockCard4 = canonical108[34]

        val events = listOf(
            // Turn 1: Human draws stock, discards card 0
            ReplayEvent.Draw(
                playerId = "p_human",
                cardId = stockCard1,
                source = DrawSource.STOCK
            ),
            ReplayEvent.Discard(
                playerId = "p_human",
                cardId = canonical108[0]
            ),

            // Turn 2: CPU1 draws stock, discards card 1
            ReplayEvent.Draw(
                playerId = "p_cpu1",
                cardId = stockCard2,
                source = DrawSource.STOCK
            ),
            ReplayEvent.Discard(
                playerId = "p_cpu1",
                cardId = canonical108[1]
            ),

            // Turn 3: CPU2 draws stock, discards card 2
            ReplayEvent.Draw(
                playerId = "p_cpu2",
                cardId = stockCard3,
                source = DrawSource.STOCK
            ),
            ReplayEvent.Discard(
                playerId = "p_cpu2",
                cardId = canonical108[2]
            ),

            // Turn 4: Human draws stockCard4, goes down with 2 Books, discards
            ReplayEvent.Draw(
                playerId = "p_human",
                cardId = stockCard4,
                source = DrawSource.STOCK
            ),
            ReplayEvent.GoDown(
                playerId = "p_human",
                melds = listOf(
                    ReplayMeld(
                        meldId = "meld_h1",
                        ownerId = "p_human",
                        meldType = ReplayMeldType.BOOK,
                        cardIds = listOf(canonical108[3], canonical108[6], canonical108[9])
                    ),
                    ReplayMeld(
                        meldId = "meld_h2",
                        ownerId = "p_human",
                        meldType = ReplayMeldType.BOOK,
                        cardIds = listOf(canonical108[12], canonical108[15], canonical108[18])
                    )
                )
            ),
            ReplayEvent.Discard(
                playerId = "p_human",
                cardId = canonical108[21]
            ),

            // Turn 5: CPU1 picks up human discard canonical108[21]
            ReplayEvent.Draw(
                playerId = "p_cpu1",
                cardId = canonical108[21],
                source = DrawSource.DISCARD
            ),
            ReplayEvent.Discard(
                playerId = "p_cpu1",
                cardId = canonical108[4]
            ),

            // Turn 6: CPU2 plays on meld_h1 and discards
            ReplayEvent.PlayOn(
                playerId = "p_human",
                meldId = "meld_h1",
                cardId = canonical108[24]
            ),
            ReplayEvent.Discard(
                playerId = "p_human",
                cardId = canonical108[27]
            ),

            // Round End
            ReplayEvent.RoundEnd(
                winnerId = "p_human",
                levelNumber = 1
            )
        )

        return ReplayCartridge(
            schemaVersion = 1,
            matchId = matchId,
            tournamentId = "t_demo_championship",
            createdAtUtc = "2026-09-10T01:45:00Z",
            buildVersion = CURRENT_BUILD_VERSION,
            rulesVersion = CURRENT_REPLAY_RULES_VERSION,
            levelNumber = 1,
            players = players,
            initialDeckOrder = canonical108,
            actionLog = events
        )
    }
}
