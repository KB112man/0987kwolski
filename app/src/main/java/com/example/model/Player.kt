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
    val pocketCardIds: Set<String> = emptySet(),
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
    val melds: List<Meld>
        get() = laidMelds

    val totalScore: Int
        get() = scoresPerLevel.sum()

    val cardCount: Int
        get() = hand.size

    val pointsInHand: Int
        get() = hand.sumOf { it.points }

    val activeCards: List<Card>
        get() = hand.filter { it.id !in pocketCardIds }

    val pocketCards: List<Card>
        get() = hand.filter { it.id in pocketCardIds }

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
        val workingCards = if (isHuman) activeCards else hand
        val cardsPerRow = (workingCards.size + RACK_ROW_COUNT - 1).coerceAtLeast(1) / RACK_ROW_COUNT
        val newRows = mutableListOf<List<Card?>>()
        for (i in 0 until RACK_ROW_COUNT) {
            val start = i * cardsPerRow
            val end = (start + cardsPerRow).coerceAtMost(workingCards.size)
            val rowCards = if (start < workingCards.size) workingCards.subList(start, end) else emptyList()
            val fullRow = mutableListOf<Card?>()
            rowCards.forEach { card ->
                fullRow.add(card)
            }
            while (fullRow.size < RACK_SLOTS_PER_ROW) {
                fullRow.add(null)
            }
            newRows.add(fullRow)
        }
        return copy(rackRows = newRows)
    }

    fun withUpdatedHand(newHand: List<Card>): Player {
        val validIds = newHand.map { it.id }.toSet()
        val cleanedPocketIds = pocketCardIds.filter { it in validIds }.toSet()
        val workingCards = (if (isHuman) newHand.filter { it.id !in cleanedPocketIds } else newHand).toMutableList()
        val currentRows = if (rackRows.size == RACK_ROW_COUNT) rackRows else listOf(emptyList(), emptyList())
        val updatedRows = mutableListOf<MutableList<Card?>>()
        for (rIdx in 0 until RACK_ROW_COUNT) {
            val sourceRow = currentRows.getOrNull(rIdx) ?: emptyList()
            val preservedRow = mutableListOf<Card?>()
            for (slotIdx in 0 until RACK_SLOTS_PER_ROW) {
                val card = sourceRow.getOrNull(slotIdx)
                if (card != null) {
                    val match = workingCards.find { it.id == card.id }
                    if (match != null) {
                        preservedRow.add(match)
                        workingCards.remove(match)
                    } else {
                        preservedRow.add(null)
                    }
                } else {
                    preservedRow.add(null)
                }
            }
            updatedRows.add(preservedRow)
        }
        for (cardToAdd in workingCards) {
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
            pocketCardIds = cleanedPocketIds,
            rackRows = updatedRows
        )
    }

    fun withPocketedCards(cardIds: Set<String>): Player {
        val validIds = hand.map { it.id }.toSet()
        val newPocketIds = (pocketCardIds + cardIds).filter { it in validIds }.toSet()
        return copy(pocketCardIds = newPocketIds).reorganizeHandIntoRack()
    }

    fun withUnpocketedCards(cardIds: Set<String>): Player {
        val newPocketIds = pocketCardIds - cardIds
        return copy(pocketCardIds = newPocketIds).reorganizeHandIntoRack()
    }

    fun withEmptyPocket(): Player {
        return copy(pocketCardIds = emptySet()).reorganizeHandIntoRack()
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
            hand = rows.flatten().filterNotNull() + pocketCards
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
            val fullRow = mutableListOf<Card?>()
            rowCards.forEach { card ->
                fullRow.add(card)
            }
            while (fullRow.size < RACK_SLOTS_PER_ROW) {
                fullRow.add(null)
            }
            newRows.add(fullRow)
        }
        return copy(rackRows = newRows)
    }

    fun insertAndShiftCardInHand(cardId: String, targetActiveIndex: Int): Player {
        val activeList = activeCards.toMutableList()
        val currentIndex = activeList.indexOfFirst { it.id == cardId }
        if (currentIndex == -1) return this

        val clampedTarget = targetActiveIndex.coerceIn(0, (activeList.size - 1).coerceAtLeast(0))
        if (currentIndex == clampedTarget) return this

        val cardToMove = activeList.removeAt(currentIndex)
        activeList.add(clampedTarget, cardToMove)

        // Maintain continuous hand ordering: ordered active cards followed by pocket cards
        val newHand = activeList + pocketCards
        return copy(hand = newHand).reorganizeHandIntoRack()
    }
}
