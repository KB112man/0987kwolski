package com.example.model

enum class SortMode {
    RANK,
    SUIT,
    CUSTOM
}

data class LevelHistoryEntry(
    val levelNumber: Int,
    val wentDown: Boolean,
    val downCombinations: List<Meld> = emptyList(),
    val penaltyPoints: Int = 0,
    val wentOut: Boolean = false
)

const val RACK_SLOTS_PER_ROW = 10
const val RACK_ROW_COUNT = 2

data class Player(
    val id: String = "player_human",
    val name: String = "You",
    val isHuman: Boolean = true,
    val avatarIndex: Int = 0,
    val hand: List<Card> = emptyList(),
    val rackRows: List<List<Card?>> = listOf(
        List(RACK_SLOTS_PER_ROW) { null },
        List(RACK_SLOTS_PER_ROW) { null }
    ),
    val laidMelds: List<Meld> = emptyList(),
    val initialDownMelds: List<Meld> = emptyList(),
    val isDown: Boolean = false,
    val scoresPerLevel: List<Int> = emptyList(),
    val levelHistory: List<LevelHistoryEntry> = emptyList(),
    val personality: String = "Balanced"
) {
    val totalScore: Int
        get() = scoresPerLevel.sum()

    val cardCount: Int
        get() = hand.size

    val pointsInHand: Int
        get() = hand.sumOf { it.points }

    fun sortedByRank(): Player {
        val sorted = hand.sortedWith(
            compareBy<Card> { if (it.isWild) 100 else it.rank.value }
                .thenBy { it.suit.ordinal }
        )
        return copy(hand = sorted)
    }

    fun sortedBySuit(): Player {
        val sorted = hand.sortedWith(
            compareBy<Card> { if (it.isWild) 100 else it.suit.ordinal }
                .thenBy { it.rank.value }
        )
        return copy(hand = sorted)
    }

    fun reorganizeHandIntoRack(): Player {
        val cardsPerRow = (hand.size + RACK_ROW_COUNT - 1).coerceAtLeast(1) / RACK_ROW_COUNT
        val newRows = mutableListOf<List<Card?>>()
        for (i in 0 until RACK_ROW_COUNT) {
            val start = i * cardsPerRow
            val end = (start + cardsPerRow).coerceAtMost(hand.size)
            val rowCards = if (start < hand.size) hand.subList(start, end) else emptyList()
            val fullRow = MutableList<Card?>(RACK_SLOTS_PER_ROW) { null }
            rowCards.forEachIndexed { idx, card ->
                if (idx < RACK_SLOTS_PER_ROW) {
                    fullRow[idx] = card
                }
            }
            newRows.add(fullRow)
        }
        return copy(rackRows = newRows)
    }

    fun withUpdatedHand(newHand: List<Card>): Player {
        val newCards = newHand.toMutableList()
        val currentRows = if (rackRows.size == RACK_ROW_COUNT) rackRows else listOf(emptyList(), emptyList())
        val updatedRows = mutableListOf<MutableList<Card?>>()

        for (rIdx in 0 until RACK_ROW_COUNT) {
            val sourceRow = currentRows.getOrNull(rIdx) ?: emptyList()
            val preservedRow = mutableListOf<Card?>()
            for (slotIdx in 0 until RACK_SLOTS_PER_ROW) {
                val card = sourceRow.getOrNull(slotIdx)
                if (card != null) {
                    val match = newCards.find { it.id == card.id }
                    if (match != null) {
                        preservedRow.add(match)
                        newCards.remove(match)
                    } else {
                        preservedRow.add(null)
                    }
                } else {
                    preservedRow.add(null)
                }
            }
            updatedRows.add(preservedRow)
        }

        for (cardToAdd in newCards) {
            var placed = false
            for (row in updatedRows) {
                val emptyIdx = row.indexOfFirst { it == null }
                if (emptyIdx != -1) {
                    row[emptyIdx] = cardToAdd
                    placed = true
                    break
                }
            }
            if (!placed) {
                updatedRows[0].add(cardToAdd)
            }
        }

        return copy(
            hand = newHand,
            rackRows = updatedRows
        )
    }

    fun moveCardInRack(cardId: String, targetRowIndex: Int, targetSlotIndex: Int? = null): Player {
        if (targetRowIndex !in 0 until RACK_ROW_COUNT) return this
        val currentCard = hand.find { it.id == cardId } ?: return this

        val rows = (0 until RACK_ROW_COUNT).map { rIdx ->
            val row = rackRows.getOrNull(rIdx) ?: emptyList()
            val mRow = row.toMutableList()
            while (mRow.size < RACK_SLOTS_PER_ROW) mRow.add(null)
            mRow
        }.toMutableList()

        var sourceRowIdx = -1
        var sourceSlotIdx = -1
        for (r in rows.indices) {
            for (s in rows[r].indices) {
                if (rows[r][s]?.id == cardId) {
                    sourceRowIdx = r
                    sourceSlotIdx = s
                    break
                }
            }
        }

        val targetSlot = (targetSlotIndex ?: 0).coerceIn(0, RACK_SLOTS_PER_ROW - 1)
        val targetRow = rows[targetRowIndex]
        val existingCardAtTarget = targetRow[targetSlot]

        if (sourceRowIdx != -1 && sourceSlotIdx != -1) {
            if (sourceRowIdx == targetRowIndex && sourceSlotIdx == targetSlot) {
                return this
            }
            if (existingCardAtTarget != null && existingCardAtTarget.id != cardId) {
                rows[sourceRowIdx][sourceSlotIdx] = existingCardAtTarget
                rows[targetRowIndex][targetSlot] = currentCard
            } else {
                rows[sourceRowIdx][sourceSlotIdx] = null
                rows[targetRowIndex][targetSlot] = currentCard
            }
        } else {
            rows[targetRowIndex][targetSlot] = currentCard
        }

        return copy(
            rackRows = rows,
            hand = rows.flatten().filterNotNull()
        )
    }

    fun autoOrganizeRack(sortBy: SortMode): Player {
        val sortedCards = when (sortBy) {
            SortMode.RANK -> hand.sortedWith(
                compareBy({ it.isWild }, { it.rank.value }, { it.suit.ordinal })
            )
            SortMode.SUIT -> hand.sortedWith(
                compareBy({ it.isWild }, { it.suit.ordinal }, { it.rank.value })
            )
            SortMode.CUSTOM -> hand
        }

        val cardsPerRow = (sortedCards.size + RACK_ROW_COUNT - 1).coerceAtLeast(1) / RACK_ROW_COUNT
        val newRows = mutableListOf<List<Card?>>()
        for (i in 0 until RACK_ROW_COUNT) {
            val start = i * cardsPerRow
            val end = (start + cardsPerRow).coerceAtMost(sortedCards.size)
            val rowCards = if (start < sortedCards.size) sortedCards.subList(start, end) else emptyList()
            val fullRow = MutableList<Card?>(RACK_SLOTS_PER_ROW) { null }
            rowCards.forEachIndexed { idx, card ->
                if (idx < RACK_SLOTS_PER_ROW) {
                    fullRow[idx] = card
                }
            }
            newRows.add(fullRow)
        }

        return copy(rackRows = newRows)
    }
}
