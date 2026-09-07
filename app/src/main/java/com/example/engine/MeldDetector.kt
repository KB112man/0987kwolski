package com.example.engine

import com.example.model.Card
import com.example.model.ContractLevel
import com.example.model.Meld
import com.example.model.MeldType
import com.example.model.Rank
import com.example.model.Suit

data class MeldSlotDefinition(
    val slotIndex: Int,
    val type: MeldType,
    val minSize: Int,
    val title: String,
    val subtitle: String
)

data class MeldSlotValidationResult(
    val isValid: Boolean,
    val statusText: String,
    val detailMessage: String
)

data class Level7ValidationResult(
    val isValid: Boolean,
    val run1Result: MeldSlotValidationResult,
    val run2Result: MeldSlotValidationResult,
    val run3Result: MeldSlotValidationResult,
    val leftoverCount: Int,
    val errorMessage: String? = null
)

object MeldDetector {
    fun getContractSlotDefinitions(level: ContractLevel): List<MeldSlotDefinition> {
        val list = mutableListOf<MeldSlotDefinition>()
        var slotIdx = 0
        var bookIdx = 0
        var runIdx = 0
        for (bookSize in level.requiredBookSizes) {
            slotIdx++
            bookIdx++
            list.add(
                MeldSlotDefinition(
                    slotIndex = slotIdx - 1,
                    type = MeldType.BOOK,
                    minSize = bookSize,
                    title = "Book $bookIdx: Book of $bookSize+",
                    subtitle = "Min $bookSize cards of same rank (naturals > wilds)"
                )
            )
        }
        for (runSize in level.requiredRunSizes) {
            slotIdx++
            runIdx++
            list.add(
                MeldSlotDefinition(
                    slotIndex = slotIdx - 1,
                    type = MeldType.RUN,
                    minSize = runSize,
                    title = "Run $runIdx: Run of $runSize+",
                    subtitle = "Min $runSize cards in sequence, same suit (naturals > wilds)"
                )
            )
        }
        return list
    }

    fun evaluateSingleSlot(cards: List<Card>, targetType: MeldType, minSize: Int): MeldSlotValidationResult {
        if (cards.isEmpty()) {
            return MeldSlotValidationResult(
                isValid = false,
                statusText = "Empty (0 cards)",
                detailMessage = "Tap cards from your hand to assign them to this meld."
            )
        }
        val naturals = cards.filter { !it.isWild }
        val wilds = cards.filter { it.isWild }
        if (cards.size < minSize) {
            return MeldSlotValidationResult(
                isValid = false,
                statusText = "Incomplete (${cards.size}/$minSize cards)",
                detailMessage = "Need at least $minSize cards (currently ${cards.size})."
            )
        }
        if (naturals.isEmpty()) {
            return MeldSlotValidationResult(
                isValid = false,
                statusText = "Invalid: Only wild cards",
                detailMessage = "A meld cannot be made purely of wild cards. Natural cards must strictly outnumber wilds."
            )
        }
        if (naturals.size <= wilds.size) {
            return MeldSlotValidationResult(
                isValid = false,
                statusText = "Invalid: Wilds (${wilds.size}) >= Naturals (${naturals.size})",
                detailMessage = "Rule: Natural cards must strictly outnumber wild cards (${naturals.size} natural, ${wilds.size} wild)."
            )
        }
        return when (targetType) {
            MeldType.BOOK -> {
                val firstRank = naturals.first().rank
                val allSameRank = naturals.all { it.rank == firstRank }
                if (!allSameRank) {
                    val foundRanks = naturals.map { it.rank.shortName }.distinct().joinToString(", ")
                    MeldSlotValidationResult(
                        isValid = false,
                        statusText = "Invalid: Mixed ranks ($foundRanks)",
                        detailMessage = "All natural cards in a Book must have the same rank."
                    )
                } else {
                    val wildInfo = if (wilds.isNotEmpty()) " + ${wilds.size} wild" else ""
                    MeldSlotValidationResult(
                        isValid = true,
                        statusText = "✓ Valid Book of ${firstRank.shortName}s (${cards.size} cards)",
                        detailMessage = "${naturals.size} natural ${firstRank.shortName}s$wildInfo"
                    )
                }
            }
            MeldType.RUN -> {
                val firstSuit = naturals.first().suit
                val allSameSuit = naturals.all { it.suit == firstSuit }
                if (!allSameSuit) {
                    val foundSuits = naturals.map { it.suit.symbol }.distinct().joinToString(" ")
                    MeldSlotValidationResult(
                        isValid = false,
                        statusText = "Invalid: Mixed suits ($foundSuits)",
                        detailMessage = "All natural cards in a Run must be of the same suit."
                    )
                } else {
                    val runRange = Meld.determineRunRange(cards, minSize)
                    if (runRange == null) {
                        MeldSlotValidationResult(
                            isValid = false,
                            statusText = "Invalid: Non-consecutive sequence",
                            detailMessage = "Cards do not form a consecutive run in ${firstSuit.displayName}."
                        )
                    } else {
                        val wildInfo = if (wilds.isNotEmpty()) " + ${wilds.size} wild" else ""
                        MeldSlotValidationResult(
                            isValid = true,
                            statusText = "✓ Valid Run in ${firstSuit.symbol} (${cards.size} cards)",
                            detailMessage = "Sequential run in ${firstSuit.displayName} (${naturals.size} naturals$wildInfo)"
                        )
                    }
                }
            }
        }
    }

    fun validateContract(
        groups: List<List<Card>>,
        level: ContractLevel,
        playerId: String,
        playerName: String
    ): List<Meld>? {
        val totalExpectedMelds = level.requiredBooks + level.requiredRuns
        if (groups.size != totalExpectedMelds) return null
        val createdMelds = mutableListOf<Meld>()
        val remainingBookSizes = level.requiredBookSizes.toMutableList()
        val remainingRunSizes = level.requiredRunSizes.toMutableList()

        for (group in groups) {
            val naturals = group.filter { !it.isWild }
            val wilds = group.filter { it.isWild }
            if (naturals.size <= wilds.size) return null
            if (naturals.isEmpty()) return null

            val matchingBookSize = remainingBookSizes.firstOrNull { size ->
                group.size >= size && Meld.isValidBook(group, minSize = size)
            }
            if (matchingBookSize != null) {
                remainingBookSizes.remove(matchingBookSize)
                createdMelds.add(
                    Meld(
                        id = "meld_${playerId}_book_${createdMelds.size}_${System.currentTimeMillis()}",
                        type = MeldType.BOOK,
                        cards = group,
                        ownerId = playerId,
                        ownerName = playerName
                    )
                )
                continue
            }

            val matchingRunSize = remainingRunSizes.firstOrNull { size ->
                group.size >= size && Meld.isValidRun(group, minSize = size)
            }
            if (matchingRunSize != null) {
                remainingRunSizes.remove(matchingRunSize)
                createdMelds.add(
                    Meld(
                        id = "meld_${playerId}_run_${createdMelds.size}_${System.currentTimeMillis()}",
                        type = MeldType.RUN,
                        cards = group,
                        ownerId = playerId,
                        ownerName = playerName
                    )
                )
                continue
            }

            return null
        }

        if (remainingBookSizes.isNotEmpty() || remainingRunSizes.isNotEmpty()) {
            return null
        }
        return createdMelds
    }

    fun findValidContract(
        hand: List<Card>,
        level: ContractLevel,
        playerId: String,
        playerName: String
    ): List<Meld>? {
        if (level.levelNumber == 7) {
            val threeRuns = findLevel7WinningRuns(hand) ?: return null
            return threeRuns.mapIndexed { idx, runCards ->
                Meld(
                    id = "meld_${playerId}_l7_run_${idx + 1}_${System.currentTimeMillis()}",
                    type = MeldType.RUN,
                    cards = runCards,
                    ownerId = playerId,
                    ownerName = playerName
                )
            }
        }
        if (level.requiredBooks == 0 && level.requiredRuns == 0) return emptyList()
        val candidateBooks = mutableListOf<List<Card>>()
        for (bookSize in level.requiredBookSizes.distinct()) {
            candidateBooks.addAll(findAllBooks(hand, bookSize))
        }
        val candidateRuns = mutableListOf<List<Card>>()
        for (runSize in level.requiredRunSizes.distinct()) {
            candidateRuns.addAll(findAllRuns(hand, runSize))
        }
        val combinations = searchMeldCombinations(
            hand = hand,
            availableBooks = candidateBooks,
            availableRuns = candidateRuns,
            requiredBookSizes = level.requiredBookSizes,
            requiredRunSizes = level.requiredRunSizes
        )
        for (groups in combinations) {
            val valid = validateContract(groups, level, playerId, playerName)
            if (valid != null) {
                return valid
            }
        }
        return null
    }

    fun getLegalJokerDestinations(targetMeld: Meld, card: Card): List<List<Card>> {
        if (targetMeld.type != MeldType.RUN) {
            return if (targetMeld.canAddCard(card)) listOf(targetMeld.cards + card) else emptyList()
        }
        return targetMeld.getLegalRunConfigurationsWithCard(card)
    }

    fun findAllBooks(hand: List<Card>, targetSize: Int): List<List<Card>> {
        val results = mutableListOf<List<Card>>()
        val wildCards = hand.filter { it.isWild }
        val naturalsByRank = hand.filter { !it.isWild }.groupBy { it.rank }

        for ((_, naturals) in naturalsByRank) {
            val naturalCount = naturals.size
            val minNaturals = (targetSize / 2) + 1
            for (n in minNaturals..naturalCount.coerceAtMost(targetSize)) {
                val neededWilds = targetSize - n
                if (neededWilds >= 0 && neededWilds <= wildCards.size && n > neededWilds) {
                    val naturalCombos = combinations(naturals, n)
                    val wildCombos = combinations(wildCards, neededWilds)
                    for (nCombo in naturalCombos) {
                        for (wCombo in wildCombos) {
                            val candidate = nCombo + wCombo
                            if (Meld.isValidBook(candidate, minSize = targetSize)) {
                                results.add(candidate)
                            }
                        }
                    }
                }
            }
        }
        return results
    }

    fun findAllRuns(hand: List<Card>, targetSize: Int): List<List<Card>> {
        val results = mutableListOf<List<Card>>()
        val wildCards = hand.filter { it.isWild }
        val naturalsBySuit = hand.filter { !it.isWild }.groupBy { it.suit }
        val minNaturals = (targetSize / 2) + 1

        for ((suit, naturals) in naturalsBySuit) {
            if (suit == Suit.NONE) continue
            for (start in 1..(15 - targetSize)) {
                val end = start + targetSize - 1
                val neededRanks = (start..end).toList()
                val naturalsForRank = mutableMapOf<Int, MutableList<Card>>()
                for (card in naturals) {
                    val v = if (card.rank == Rank.ACE) {
                        if (start == 1 && 1 in neededRanks) 1
                        else if (end == 14 && 14 in neededRanks) 14
                        else -1
                    } else {
                        card.rank.value
                    }
                    if (v in neededRanks) {
                        naturalsForRank.getOrPut(v) { mutableListOf() }.add(card)
                    }
                }
                val availableDistinctRanks = naturalsForRank.keys.toList()
                if (availableDistinctRanks.size < minNaturals) continue
                for (k in minNaturals..availableDistinctRanks.size.coerceAtMost(targetSize)) {
                    val neededWilds = targetSize - k
                    if (neededWilds >= 0 && neededWilds <= wildCards.size && k > neededWilds) {
                        val rankCombos = combinations(availableDistinctRanks, k)
                        val wildCombos = combinations(wildCards, neededWilds)
                        for (rCombo in rankCombos) {
                            val naturalPicksList = generateCartesianPicks(rCombo.map { naturalsForRank[it]!! })
                            for (naturalPick in naturalPicksList) {
                                for (wCombo in wildCombos) {
                                    val candidate = naturalPick + wCombo
                                    if (Meld.isValidRun(candidate, minSize = targetSize)) {
                                        results.add(candidate)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return results
    }

    private fun generateCartesianPicks(lists: List<List<Card>>): List<List<Card>> {
        if (lists.isEmpty()) return listOf(emptyList())
        val head = lists.first()
        val tailPicks = generateCartesianPicks(lists.drop(1))
        val result = mutableListOf<List<Card>>()
        for (card in head) {
            for (tail in tailPicks) {
                result.add(listOf(card) + tail)
            }
        }
        return result
    }

    private fun searchMeldCombinations(
        hand: List<Card>,
        availableBooks: List<List<Card>>,
        availableRuns: List<List<Card>>,
        requiredBookSizes: List<Int>,
        requiredRunSizes: List<Int>
    ): List<List<List<Card>>> {
        val results = mutableListOf<List<List<Card>>>()

        fun backtrackRuns(
            currentGroups: List<List<Card>>,
            usedCardIds: Set<String>,
            runIndex: Int
        ) {
            if (runIndex == requiredRunSizes.size) {
                results.add(currentGroups)
                return
            }
            val targetSize = requiredRunSizes[runIndex]
            val candidateRuns = availableRuns.filter { it.size >= targetSize }
            for (candidate in candidateRuns) {
                val candidateIds = candidate.map { it.id }.toSet()
                if (candidateIds.none { it in usedCardIds }) {
                    backtrackRuns(
                        currentGroups = currentGroups + listOf(candidate),
                        usedCardIds = usedCardIds + candidateIds,
                        runIndex = runIndex + 1
                    )
                    if (results.size > 20) return
                }
            }
        }

        fun backtrackBooks(
            currentGroups: List<List<Card>>,
            usedCardIds: Set<String>,
            bookIndex: Int
        ) {
            if (bookIndex == requiredBookSizes.size) {
                backtrackRuns(currentGroups, usedCardIds, 0)
                return
            }
            val targetSize = requiredBookSizes[bookIndex]
            val candidateBooks = availableBooks.filter { it.size >= targetSize }
            for (candidate in candidateBooks) {
                val candidateIds = candidate.map { it.id }.toSet()
                if (candidateIds.none { it in usedCardIds }) {
                    backtrackBooks(
                        currentGroups = currentGroups + listOf(candidate),
                        usedCardIds = usedCardIds + candidateIds,
                        bookIndex = bookIndex + 1
                    )
                    if (results.size > 20) return
                }
            }
        }

        backtrackBooks(emptyList(), emptySet(), 0)
        return results
    }

    fun findValidMeldForCard(card: Card, tableMelds: List<Meld>): Meld? {
        return tableMelds.firstOrNull { it.canAddCard(card) }
    }

    fun isRummyCard(card: Card, tableMelds: List<Meld>): Boolean {
        if (tableMelds.isEmpty()) return false
        return tableMelds.any { it.canAddCard(card) }
    }

    fun validateLevel7Reveal(
        run1Cards: List<Card>,
        run2Cards: List<Card>,
        run3Cards: List<Card>,
        unassignedCards: List<Card>,
        totalHandCount: Int
    ): Level7ValidationResult {
        val r1 = evaluateSingleSlot(run1Cards, MeldType.RUN, minSize = 4)
        val r2 = evaluateSingleSlot(run2Cards, MeldType.RUN, minSize = 4)
        val r3 = evaluateSingleSlot(run3Cards, MeldType.RUN, minSize = 4)
        val leftovers = unassignedCards.size

        val error = when {
            leftovers > 0 -> "All cards in your hand must fit into your 3 Runs to win Level 7. Leftover cards remaining: $leftovers"
            !r1.isValid -> "Run 1: ${r1.detailMessage}"
            !r2.isValid -> "Run 2: ${r2.detailMessage}"
            !r3.isValid -> "Run 3: ${r3.detailMessage}"
            (run1Cards.size + run2Cards.size + run3Cards.size) != totalHandCount -> "Total cards in runs (${run1Cards.size + run2Cards.size + run3Cards.size}) does not match total hand count ($totalHandCount)."
            else -> null
        }

        return Level7ValidationResult(
            isValid = error == null,
            run1Result = r1,
            run2Result = r2,
            run3Result = r3,
            leftoverCount = leftovers,
            errorMessage = error
        )
    }

    fun findLevel7WinningRuns(hand: List<Card>): List<List<Card>>? {
        if (hand.size < 12) return null
        val allRuns = findAllValidRunsForHand(hand, minSize = 4)
        if (allRuns.size < 3) return null

        for (r1 in allRuns) {
            val r1Ids = r1.map { it.id }.toSet()
            val remainingAfterR1 = hand.filter { it.id !in r1Ids }
            if (remainingAfterR1.size < 8) continue

            val runsForR2 = findAllValidRunsForHand(remainingAfterR1, minSize = 4)
            for (r2 in runsForR2) {
                val r2Ids = r2.map { it.id }.toSet()
                val remainingAfterR2 = remainingAfterR1.filter { it.id !in r2Ids }
                if (remainingAfterR2.size < 4) continue

                val r3Configs = Meld.determineAllValidRunConfigurations(remainingAfterR2, minSize = 4)
                if (r3Configs.isNotEmpty()) {
                    val r1Config = Meld.determineAllValidRunConfigurations(r1, 4).firstOrNull() ?: r1
                    val r2Config = Meld.determineAllValidRunConfigurations(r2, 4).firstOrNull() ?: r2
                    val r3Config = r3Configs.first()
                    return listOf(r1Config, r2Config, r3Config)
                }
            }
        }
        return null
    }

    fun findAllValidRunsForHand(cards: List<Card>, minSize: Int = 4): List<List<Card>> {
        val results = mutableListOf<List<Card>>()
        val wildCards = cards.filter { it.isWild }
        val naturalsBySuit = cards.filter { !it.isWild }.groupBy { it.suit }

        for ((suit, naturals) in naturalsBySuit) {
            if (suit == com.example.model.Suit.NONE) continue
            val maxSize = cards.size.coerceAtMost(14)
            for (targetSize in minSize..maxSize) {
                val minNaturals = (targetSize / 2) + 1
                for (start in 1..(15 - targetSize)) {
                    val end = start + targetSize - 1
                    val neededRanks = (start..end).toList()
                    val naturalsForRank = mutableMapOf<Int, MutableList<Card>>()
                    for (card in naturals) {
                        val v = if (card.rank == com.example.model.Rank.ACE) {
                            if (start == 1 && 1 in neededRanks) 1
                            else if (end == 14 && 14 in neededRanks) 14
                            else -1
                        } else {
                            card.rank.value
                        }
                        if (v in neededRanks) {
                            naturalsForRank.getOrPut(v) { mutableListOf() }.add(card)
                        }
                    }
                    val availableDistinctRanks = naturalsForRank.keys.toSet()
                    val missingRanks = neededRanks.filter { it !in availableDistinctRanks }
                    val neededWilds = missingRanks.size
                    if (neededWilds > wildCards.size) continue
                    if (targetSize - neededWilds < minNaturals) continue
                    if (targetSize - neededWilds <= neededWilds) continue

                    var hasAdjacentWilds = false
                    for (i in 0 until missingRanks.size - 1) {
                        if (missingRanks[i + 1] == missingRanks[i] + 1) {
                            hasAdjacentWilds = true
                            break
                        }
                    }
                    if (hasAdjacentWilds) continue

                    val naturalPicksList = generateCartesianPicks(
                        neededRanks.filter { it in availableDistinctRanks }.map { naturalsForRank[it]!! }
                    )
                    val wildCombos = combinations(wildCards, neededWilds)
                    for (natPick in naturalPicksList) {
                        for (wCombo in wildCombos) {
                            val candidate = natPick + wCombo
                            if (Meld.isValidRun(candidate, minSize = targetSize)) {
                                results.add(candidate)
                            }
                        }
                    }
                }
            }
        }
        return results
    }

    private fun <T> combinations(list: List<T>, k: Int): List<List<T>> {
        if (k == 0) return listOf(emptyList())
        if (list.isEmpty() || k > list.size) return emptyList()
        val head = list.first()
        val tail = list.drop(1)
        val withHead = combinations(tail, k - 1).map { listOf(head) + it }
        val withoutHead = combinations(tail, k)
        return withHead + withoutHead
    }
}
