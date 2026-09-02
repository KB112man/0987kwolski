package com.example.model

enum class ContractLevel(
    val levelNumber: Int,
    val dealCount: Int,
    val requiredBookSizes: List<Int>,
    val requiredRunSizes: List<Int>,
    val title: String,
    val shortRequirement: String,
    val description: String
) {
    LEVEL_1(
        levelNumber = 1,
        dealCount = 10,
        requiredBookSizes = listOf(3, 3),
        requiredRunSizes = emptyList(),
        title = "Level 1: 2 Books (min 3 each)",
        shortRequirement = "2 Books",
        description = "Deal 10 cards. Contract: 2 Books, minimum 3 cards each (same rank, natural > wild)."
    ),
    LEVEL_2(
        levelNumber = 2,
        dealCount = 10,
        requiredBookSizes = listOf(3),
        requiredRunSizes = listOf(4),
        title = "Level 2: 1 Book (min 3) + 1 Run (min 4)",
        shortRequirement = "1 Book + 1 Run",
        description = "Deal 10 cards. Contract: 1 Book (min 3) + 1 Run (min 4, same suit consecutive, natural > wild)."
    ),
    LEVEL_3(
        levelNumber = 3,
        dealCount = 10,
        requiredBookSizes = emptyList(),
        requiredRunSizes = listOf(4, 4),
        title = "Level 3: 2 Runs (min 4 each)",
        shortRequirement = "2 Runs",
        description = "Deal 10 cards. Contract: 2 Runs, minimum 4 cards each (same suit consecutive, natural > wild)."
    ),
    LEVEL_4(
        levelNumber = 4,
        dealCount = 10,
        requiredBookSizes = listOf(3, 3, 3),
        requiredRunSizes = emptyList(),
        title = "Level 4: 3 Books (min 3 each)",
        shortRequirement = "3 Books",
        description = "Deal 10 cards. Contract: 3 Books, minimum 3 cards each (same rank, natural > wild)."
    ),
    LEVEL_5(
        levelNumber = 5,
        dealCount = 13,
        requiredBookSizes = listOf(3, 3),
        requiredRunSizes = listOf(4),
        title = "Level 5: 2 Books (min 3 each) + 1 Run (min 4)",
        shortRequirement = "2 Books + 1 Run",
        description = "Deal 13 cards. Contract: 2 Books (min 3 each) + 1 Run (min 4, same suit consecutive, natural > wild)."
    ),
    LEVEL_6(
        levelNumber = 6,
        dealCount = 13,
        requiredBookSizes = listOf(3),
        requiredRunSizes = listOf(4, 4),
        title = "Level 6: 1 Book (min 3) + 2 Runs (min 4 each)",
        shortRequirement = "1 Book + 2 Runs",
        description = "Deal 13 cards. Contract: 1 Book (min 3) + 2 Runs (min 4 each, same suit consecutive, natural > wild)."
    ),
    LEVEL_7(
        levelNumber = 7,
        dealCount = 15,
        requiredBookSizes = emptyList(),
        requiredRunSizes = listOf(4, 4, 4),
        title = "Level 7: 3 Runs (min 4 each) – No Discard",
        shortRequirement = "3 Runs (No Discard)",
        description = "Deal 15 cards. Contract: 3 Runs (min 4 each, natural > wild). Winning Trophy Level – No Discard."
    );

    val noDiscard: Boolean
        get() = this == LEVEL_7

    val requiredBooks: Int
        get() = requiredBookSizes.size

    val requiredRuns: Int
        get() = requiredRunSizes.size

    companion object {
        fun fromLevelNumber(num: Int): ContractLevel {
            return entries.find { it.levelNumber == num } ?: LEVEL_1
        }
    }
}
