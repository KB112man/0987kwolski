package com.example.model

enum class Suit(val symbol: String, val displayName: String, val isRed: Boolean) {
    HEARTS("♥", "Hearts", true),
    DIAMONDS("♦", "Diamonds", true),
    CLUBS("♣", "Clubs", false),
    SPADES("♠", "Spades", false),
    NONE("★", "Wild", false)
}

enum class Rank(val value: Int, val points: Int, val shortName: String, val displayName: String) {
    TWO(2, 5, "2", "2"),
    THREE(3, 5, "3", "3"),
    FOUR(4, 5, "4", "4"),
    FIVE(5, 5, "5", "5"),
    SIX(6, 5, "6", "6"),
    SEVEN(7, 5, "7", "7"),
    EIGHT(8, 5, "8", "8"),
    NINE(9, 5, "9", "9"),
    TEN(10, 10, "10", "10"),
    JACK(11, 10, "J", "Jack"),
    QUEEN(12, 10, "Q", "Queen"),
    KING(13, 10, "K", "King"),
    ACE(14, 15, "A", "Ace"),
    JOKER(0, 20, "JKR", "Joker")
}

data class Card(
    val id: String,
    val rank: Rank,
    val suit: Suit,
    val deckNumber: Int = 1
) {
    val isJoker: Boolean
        get() = rank == Rank.JOKER || suit == Suit.NONE
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
