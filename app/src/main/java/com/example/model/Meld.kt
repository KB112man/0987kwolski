package com.example.model

enum class MeldType {
    BOOK,
    RUN
}

data class Meld(
    val id: String,
    val type: MeldType,
    val cards: List<Card>,
    val ownerId: String,
    val ownerName: String
) {
    val naturalCards: List<Card>
        get() = cards.filter { !it.isWild }

    val wildCards: List<Card>
        get() = cards.filter { it.isWild }

    val naturalCount: Int
        get() = naturalCards.size

    val wildCount: Int
        get() = wildCards.size

    val hasNaturalMajority: Boolean
        get() = naturalCount > wildCount

    val bookRank: Rank?
        get() = if (type == MeldType.BOOK) {
            naturalCards.firstOrNull()?.rank
        } else null

    val runSuit: Suit?
        get() = if (type == MeldType.RUN) {
            naturalCards.firstOrNull()?.suit
        } else null

    fun getRunRange(): Pair<Int, Int>? {
        if (type != MeldType.RUN) return null
        return determineRunRange(cards)
    }

    fun canAddCard(card: Card): Boolean {
        val testCards = cards + card
        return when (type) {
            MeldType.BOOK -> isValidBook(testCards, minSize = 3)
            MeldType.RUN -> isValidRun(testCards, minSize = 4)
        }
    }

    fun addCard(card: Card): Meld? {
        return if (canAddCard(card)) withCardAdded(card) else null
    }

    fun getPlayOnRejectionReason(card: Card): String? {
        if (canAddCard(card)) return null
        val testNaturals = (cards + card).filter { !it.isWild }
        val testWilds = (cards + card).filter { it.isWild }
        if (testNaturals.size <= testWilds.size) {
            return "Natural cards must strictly outnumber Jokers"
        }
        if (type == MeldType.BOOK) {
            val rank = bookRank
            if (!card.isWild && rank != null && card.rank != rank) {
                return "Rank ${card.rank.shortName} does not match Book of ${rank.shortName}s"
            }
        } else {
            val suit = runSuit
            if (!card.isWild && suit != null && card.suit != suit) {
                return "Suit ${card.suit.symbol} does not match ${suit.displayName} Run"
            }
            if (!card.isWild && cards.filter { !it.isWild }.any { it.rank == card.rank }) {
                return "Duplicate rank ${card.rank.shortName} in Run"
            }
            return "Rank ${card.rank.shortName} does not connect sequentially"
        }
        return "Invalid combination"
    }

    fun withCardAdded(card: Card): Meld {
        if (!canAddCard(card)) return this
        if (type == MeldType.BOOK) {
            return copy(cards = cards + card)
        }
        val allCards = cards + card
        val alignedCards = alignRunCards(allCards)
        return copy(cards = alignedCards ?: allCards)
    }

    fun getRepresentedRanksForJokers(): List<Rank> {
        if (type != MeldType.RUN) return emptyList()
        val range = getRunRange() ?: return emptyList()
        val (start, end) = range
        val isAceHigh = end == 14 && cards.any { it.rank == Rank.ACE && !it.isWild }
        val naturalRanks = cards.filter { !it.isWild }.map { c ->
            if (c.rank == Rank.ACE && isAceHigh) 14 else c.rank.value
        }.toSet()
        val missingRanks = mutableListOf<Rank>()
        for (v in start..end) {
            if (v !in naturalRanks) {
                val rank = if (v == 14) Rank.ACE else Rank.entries.firstOrNull { it.value == v }
                if (rank != null) missingRanks.add(rank)
            }
        }
        return missingRanks
    }

    fun getLegalRunConfigurationsWithCard(card: Card): List<List<Card>> {
        if (type != MeldType.RUN || !canAddCard(card)) return emptyList()
        val allCards = cards + card
        return determineAllValidRunConfigurations(allCards)
    }

    private fun alignRunCards(candidateCards: List<Card>): List<Card>? {
        val configs = determineAllValidRunConfigurations(candidateCards)
        return configs.firstOrNull()
    }

    companion object {
        fun isValidBook(cards: List<Card>, minSize: Int = 3): Boolean {
            if (cards.size < minSize) return false
            val naturals = cards.filter { !it.isWild }
            val wilds = cards.filter { it.isWild }
            if (naturals.size <= wilds.size) return false
            if (naturals.isEmpty()) return false
            val firstRank = naturals.first().rank
            return naturals.all { it.rank == firstRank }
        }

        fun determineAllValidRunConfigurations(candidateCards: List<Card>, minSize: Int = 4): List<List<Card>> {
            if (candidateCards.size < minSize) return emptyList()
            val naturals = candidateCards.filter { !it.isWild }
            val wilds = candidateCards.filter { it.isWild }
            if (naturals.size <= wilds.size || naturals.isEmpty()) return emptyList()
            val suit = naturals.first().suit
            if (suit == Suit.NONE || naturals.any { it.suit != suit }) return emptyList()
            val length = candidateCards.size
            val validSpans = mutableListOf<Triple<Int, Int, Boolean>>()
            val spansAceLow = findAllRunSpans(candidateCards, length, isAceHigh = false)
            for (pair in spansAceLow) {
                validSpans.add(Triple(pair.first, pair.second, false))
            }
            val spansAceHigh = findAllRunSpans(candidateCards, length, isAceHigh = true)
            for (pair in spansAceHigh) {
                val containsAce = naturals.any { it.rank == Rank.ACE }
                if (containsAce || validSpans.none { it.first == pair.first && it.second == pair.second }) {
                    validSpans.add(Triple(pair.first, pair.second, true))
                }
            }
            val results = mutableListOf<List<Card>>()
            val seenSignatures = mutableSetOf<String>()
            for ((start, end, isAceHigh) in validSpans) {
                val arranged = alignRunCardsForSpan(candidateCards, start, end, isAceHigh)
                if (arranged != null) {
                    val signature = arranged.joinToString("-") { it.id }
                    if (seenSignatures.add(signature)) {
                        results.add(arranged)
                    }
                }
            }
            return results
        }

        private fun findAllRunSpans(cards: List<Card>, length: Int, isAceHigh: Boolean): List<Pair<Int, Int>> {
            val naturals = cards.filter { !it.isWild }
            val wildCount = cards.count { it.isWild }
            val values = naturals.map { card ->
                if (card.rank == Rank.ACE) {
                    if (isAceHigh) 14 else 1
                } else {
                    card.rank.value
                }
            }
            if (values.toSet().size != values.size) return emptyList()
            val minVal = values.minOrNull() ?: return emptyList()
            val maxVal = values.maxOrNull() ?: return emptyList()
            if (maxVal - minVal + 1 > length) return emptyList()
            val minPossibleStart = (maxVal - length + 1).coerceAtLeast(1)
            val maxPossibleStart = minVal.coerceAtMost(14 - length + 1)
            val spans = mutableListOf<Pair<Int, Int>>()
            for (start in minPossibleStart..maxPossibleStart) {
                val end = start + length - 1
                if (end > 14) continue
                if (values.all { it in start..end }) {
                    val missingInSpan = length - values.size
                    if (missingInSpan == wildCount) {
                        spans.add(Pair(start, end))
                    }
                }
            }
            return spans
        }

        fun alignRunCardsForSpan(
            candidateCards: List<Card>,
            start: Int,
            end: Int,
            isAceHigh: Boolean
        ): List<Card>? {
            val naturals = candidateCards.filter { !it.isWild }.toMutableList()
            val jokers = candidateCards.filter { it.isWild }.toMutableList()
            val result = mutableListOf<Card>()
            for (v in start..end) {
                val naturalMatch = naturals.firstOrNull { c ->
                    val cv = if (c.rank == Rank.ACE && isAceHigh) 14 else c.rank.value
                    cv == v
                }
                if (naturalMatch != null) {
                    naturals.remove(naturalMatch)
                    result.add(naturalMatch)
                } else if (jokers.isNotEmpty()) {
                    result.add(jokers.removeAt(0))
                } else {
                    return null
                }
            }
            // Check for adjacent jokers
            for (i in 0 until result.size - 1) {
                if (result[i].isWild && result[i+1].isWild) return null
            }
            result.addAll(naturals)
            result.addAll(jokers)
            return result
        }

        fun determineRunRange(cards: List<Card>, minSize: Int = 4): Pair<Int, Int>? {
            if (cards.size < minSize) return null
            val naturals = cards.filter { !it.isWild }
            val wilds = cards.filter { it.isWild }
            if (naturals.size <= wilds.size) return null
            if (naturals.isEmpty()) return null
            val suit = naturals.first().suit
            if (suit == Suit.NONE || naturals.any { it.suit != suit }) return null
            val length = cards.size
            val aceLowResult = testRunSpan(cards, length, isAceHigh = false)
            if (aceLowResult != null) return aceLowResult
            val aceHighResult = testRunSpan(cards, length, isAceHigh = true)
            if (aceHighResult != null) return aceHighResult
            return null
        }

        private fun testRunSpan(cards: List<Card>, length: Int, isAceHigh: Boolean): Pair<Int, Int>? {
            val naturals = cards.filter { !it.isWild }
            val wildCount = cards.count { it.isWild }
            val values = naturals.map { card ->
                if (card.rank == Rank.ACE) {
                    if (isAceHigh) 14 else 1
                } else {
                    card.rank.value
                }
            }
            if (values.toSet().size != values.size) return null
            val minVal = values.minOrNull() ?: return null
            val maxVal = values.maxOrNull() ?: return null
            if (maxVal - minVal + 1 > length) return null
            val minPossibleStart = (maxVal - length + 1).coerceAtLeast(1)
            val maxPossibleStart = minVal.coerceAtMost(14 - length + 1)
            for (start in minPossibleStart..maxPossibleStart) {
                val end = start + length - 1
                if (end > 14) continue
                if (values.all { it in start..end }) {
                    val missingInSpan = length - values.size
                    if (missingInSpan == wildCount) {
                        return Pair(start, end)
                    }
                }
            }
            return null
        }

        fun isValidRun(cards: List<Card>, minSize: Int = 4): Boolean {
            return determineRunRange(cards, minSize) != null
        }
    }
}
