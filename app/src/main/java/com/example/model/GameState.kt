package com.example.model

enum class TurnPhase {
    DRAW,
    DRAW_PENDING,
    TO_YOU_OFFER,
    PLAY_OR_DISCARD,
    BUY_PRIORITY_CHECK,
    RUMMAY_REACTION,
    ROUND_OVER,
    GAME_OVER
}

data class PendingBuyPriority(
    val discard: Card,
    val discarderId: String,
    val discarderName: String,
    val nextTurnPlayerId: String,
    val interestedBuyerIds: List<String> = emptyList()
) {
    val discardCard: Card get() = discard
    val eligibleContenderIds: List<String> get() = interestedBuyerIds
}

data class TurnTransition(
    val discarderIndex: Int,
    val nextPlayerIndex: Int,
    val otherPlayerIndex: Int,
    val nextPlayer: Player,
    val otherPlayer: Player,
    val pendingBuyPriority: PendingBuyPriority?
)

data class PendingRummayCall(
    val discardedCard: Card,
    val offenderId: String,
    val offenderName: String,
    val callerId: String? = null,
    val callerName: String? = null,
    val eligibleMelds: List<Meld> = emptyList()
)

data class PendingJokerMove(
    val meldId: String = "",
    val card: Card,
    val targetMeld: Meld,
    val options: List<List<Card>>
)

data class GameState(
    val currentLevel: Int = 1,
    val dealerIndex: Int = 0,
    val currentTurnPlayerIndex: Int = 0,
    val currentPhase: TurnPhase = TurnPhase.DRAW,
    val drawDeck: List<Card> = emptyList(),
    val discardPile: List<Card> = emptyList(),
    val players: List<Player> = emptyList(),
    val melds: List<Meld> = emptyList(),
    val allTableMelds: List<Meld> = emptyList(),
    val autoSortHand: Boolean = false,
    val sortMode: SortMode = SortMode.RANK,
    val pendingToYouOffer: Card? = null,
    val pendingBuyPriority: PendingBuyPriority? = null,
    val pendingRummay: PendingRummayCall? = null,
    val pendingJokerMove: PendingJokerMove? = null,
    val isRoundOver: Boolean = false,
    val isTournamentOver: Boolean = false,
    val roundWinnerIndex: Int? = null,
    val gameLogs: List<String> = emptyList(),
    val masterDeck: List<Card> = emptyList(),
    val statusMessage: String = ""
) {
    val contractLevel: ContractLevel
        get() = ContractLevel.fromLevelNumber(currentLevel)

    val currentContract: ContractLevel
        get() = contractLevel

    val currentTurnPlayer: Player
        get() = players.getOrElse(currentTurnPlayerIndex) {
            players.firstOrNull() ?: Player(name = "You", isHuman = true)
        }

    val currentPlayer: Player
        get() = currentTurnPlayer

    val currentTurnPhase: TurnPhase
        get() = currentPhase

    val nextPlayer: Player
        get() = if (players.isEmpty()) currentTurnPlayer else players[(currentTurnPlayerIndex + 1) % players.size]

    val turnPlayerIndex: Int
        get() = currentTurnPlayerIndex

    val humanPlayer: Player?
        get() = players.find { it.isHuman }

    val humanPlayerIndex: Int
        get() = players.indexOfFirst { it.isHuman }

    val isHumanTurn: Boolean
        get() = currentTurnPlayer.isHuman

    val topDiscard: Card?
        get() = discardPile.lastOrNull()

    val aiOpponents: List<Player>
        get() = players.filter { !it.isHuman }

    val topOpponent: Player?
        get() = aiOpponents.getOrNull(0)

    val leftOpponent: Player?
        get() = aiOpponents.getOrNull(1)

    val rightOpponent: Player?
        get() = null

    val allTrackedCards: List<Card>
        get() {
            val list = mutableListOf<Card>()
            list.addAll(drawDeck)
            list.addAll(discardPile)
            players.forEach { list.addAll(it.hand) }
            allTableMelds.forEach { list.addAll(it.cards) }
            return list
        }

    val isCardConservationValid: Boolean
        get() {
            val cards = allTrackedCards
            if (cards.size != 108) return false
            val uniqueIds = cards.map { it.id }.toSet()
            return uniqueIds.size == 108
        }
}
