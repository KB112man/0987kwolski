package com.example.model

enum class Suit(val symbol: String, val displayName: String, val isRed: Boolean) {
    HEARTS("♥", "Hearts", true),
    DIAMONDS("♦", "Diamonds", true),
    CLUBS("♣", "Clubs", false),
    SPADES("♠", "Spades", false),
    NONE("★", "Wild", false)
}

enum class Rank(val value: Int, val points: Int, val shortName: String, val displayName: String) {
    TWO(2, 5, "2", "2"),        // 2-9 = 5 points
    THREE(3, 5, "3", "3"),       // 2-9 = 5 points
    FOUR(4, 5, "4", "4"),        // 2-9 = 5 points
    FIVE(5, 5, "5", "5"),        // 2-9 = 5 points
    SIX(6, 5, "6", "6"),         // 2-9 = 5 points
    SEVEN(7, 5, "7", "7"),       // 2-9 = 5 points
    EIGHT(8, 5, "8", "8"),       // 2-9 = 5 points
    NINE(9, 5, "9", "9"),        // 2-9 = 5 points
    TEN(10, 10, "10", "10"),     // 10-K = 10 points
    JACK(11, 10, "J", "Jack"),   // 10-K = 10 points
    QUEEN(12, 10, "Q", "Queen"), // 10-K = 10 points
    KING(13, 10, "K", "King"),   // 10-K = 10 points
    ACE(14, 15, "A", "Ace"),     // Ace = 15 points
    JOKER(0, 20, "JKR", "Joker") // Joker = 20 points (Wild)
}

data class Card(
    val id: String,
    val rank: Rank,
    val suit: Suit,
    val deckNumber: Int = 1
) {
    val isJoker: Boolean
        get() = rank == Rank.JOKER || suit == Suit.NONE

    /**
     * ONLY Jokers are wild cards. 2s are natural.
     */
    val isWild: Boolean
        get() = isJoker

    val pointValue: Int
        get() = rank.points

    val points: Int
        get() = rank.points

    val displayName: String
        get() = if (isJoker) "Joker (Wild)" else "${rank.displayName} of ${suit.displayName}"

    val shortLabel: String
        get() = if (isJoker) "JKR" else "${rank.shortName}${suit.symbol}"

    val isRed: Boolean
        get() = suit.isRed
}
