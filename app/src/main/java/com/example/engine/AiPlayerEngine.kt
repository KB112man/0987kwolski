package com.example.engine

import com.example.model.Card
import com.example.model.ContractLevel
import com.example.model.Meld
import com.example.model.Player
import kotlin.random.Random

data class AiPlayOnMove(
    val card: Card,
    val targetMeldId: String
)

object AiPlayerEngine {
    fun shouldTakeDiscard(
        ai: Player,
        discard: Card,
        level: ContractLevel,
        tableMelds: List<Meld>
    ): Boolean {
        if (discard.isWild) return true
        if (ai.isDown && tableMelds.any { it.canAddCard(discard) }) {
            return true
        }
        val testHand = ai.hand + discard
        val contractWithCard = MeldDetector.findValidContract(testHand, level, ai.id, ai.name)
        if (contractWithCard != null) {
            return true
        }

        val acceptChance = when (ai.personality) {
            "Aggressive" -> 0.35
            "Strategic" -> 0.25
            "Cautious" -> 0.15
            else -> 0.20
        }

        if (level.requiredBooks > 0) {
            val naturalRankMatches = ai.hand.count { !it.isWild && it.rank == discard.rank }
            val wildCount = ai.hand.count { it.isWild }
            if (naturalRankMatches >= 2 || (naturalRankMatches == 1 && wildCount >= 1)) {
                return true
            }
            if (naturalRankMatches == 1 && Random.nextDouble() < acceptChance) {
                return true
            }
        }

        if (level.requiredRuns > 0) {
            val sameSuitRanks = ai.hand.filter { !it.isWild && it.suit == discard.suit }.map { it.rank.value }.toSet()
            val hasTwoNeighbors = (sameSuitRanks.contains(discard.rank.value - 1) && sameSuitRanks.contains(discard.rank.value - 2)) ||
                    (sameSuitRanks.contains(discard.rank.value + 1) && sameSuitRanks.contains(discard.rank.value + 2)) ||
                    (sameSuitRanks.contains(discard.rank.value - 1) && sameSuitRanks.contains(discard.rank.value + 1))
            if (hasTwoNeighbors) {
                return true
            }
            val hasOneNeighbor = sameSuitRanks.contains(discard.rank.value - 1) || sameSuitRanks.contains(discard.rank.value + 1)
            if (hasOneNeighbor && Random.nextDouble() < acceptChance) {
                return true
            }
        }
        return false
    }

    fun shouldAiTakeDiscard(
        ai: Player,
        discard: Card,
        level: ContractLevel,
        tableMelds: List<Meld>
    ): Boolean = shouldTakeDiscard(ai, discard, level, tableMelds)

    fun shouldBuyCard(
        ai: Player,
        discard: Card,
        level: ContractLevel,
        tableMelds: List<Meld>
    ): Boolean {
        if (discard.isWild) return true
        val testHand = ai.hand + discard
        if (MeldDetector.findValidContract(testHand, level, ai.id, ai.name) != null) {
            return true
        }
        if (ai.isDown && tableMelds.any { it.canAddCard(discard) }) {
            return ai.hand.size < 10
        }
        val buyThreshold = when (ai.personality) {
            "Aggressive" -> 0.70
            "Strategic" -> 0.55
            "Cautious" -> 0.30
            else -> 0.45
        }
        val rankMatches = ai.hand.count { !it.isWild && it.rank == discard.rank }
        if (level.requiredBooks > 0 && rankMatches >= 2) {
            return Random.nextDouble() < buyThreshold
        }
        val suitNeighbors = ai.hand.filter {
            !it.isWild && it.suit == discard.suit && Math.abs(it.rank.value - discard.rank.value) == 1
        }
        if (level.requiredRuns > 0 && suitNeighbors.size >= 2) {
            return Random.nextDouble() < buyThreshold
        }
        return false
    }

    fun shouldAiBuyDiscard(
        ai: Player,
        discard: Card,
        level: ContractLevel,
        tableMelds: List<Meld>
    ): Boolean = shouldBuyCard(ai, discard, level, tableMelds)

    fun chooseDiscard(
        ai: Player,
        level: ContractLevel,
        tableMelds: List<Meld>
    ): Card? {
        val nonWilds = ai.hand.filter { !it.isWild }
        val nonRummyCandidates = nonWilds.filter { !MeldDetector.isRummyCard(it, tableMelds) }
        val candidates = when {
            nonRummyCandidates.isNotEmpty() -> nonRummyCandidates
            nonWilds.isNotEmpty() -> nonWilds
            else -> emptyList()
        }
        if (candidates.isEmpty()) return null
        return candidates.maxByOrNull { card ->
            var discardScore = card.pointValue * 2
            val sameRankCount = ai.hand.count { !it.isWild && it.id != card.id && it.rank == card.rank }
            discardScore -= sameRankCount * 8
            val adjacentSuitCount = ai.hand.count {
                !it.isWild && it.id != card.id && it.suit == card.suit && Math.abs(it.rank.value - card.rank.value) == 1
            }
            discardScore -= adjacentSuitCount * 6
            discardScore
        } ?: ai.hand.first()
    }

    fun selectAiDiscard(
        ai: Player,
        level: ContractLevel,
        tableMelds: List<Meld>
    ): Card? = chooseDiscard(ai, level, tableMelds)

    fun findAiPlayOnMoves(ai: Player, tableMelds: List<Meld>): List<AiPlayOnMove> {
        val moves = mutableListOf<AiPlayOnMove>()
        for (card in ai.hand) {
            val meld = tableMelds.firstOrNull { it.canAddCard(card) }
            if (meld != null) {
                moves.add(AiPlayOnMove(card = card, targetMeldId = meld.id))
            }
        }
        return moves
    }

    fun choosePenaltyCardToGive(ai: Player): Card {
        val nonWilds = ai.hand.filter { !it.isWild }
        if (nonWilds.isEmpty()) return ai.hand.first()
        return nonWilds.maxByOrNull { it.pointValue } ?: nonWilds.first()
    }
}
