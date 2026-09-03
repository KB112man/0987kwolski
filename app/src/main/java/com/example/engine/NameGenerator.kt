package com.example.engine

import kotlin.random.Random

object NameGenerator {
    private val ADJECTIVES = listOf(
        "Sneaky", "Wobbly", "Grumpy", "Shady", "Rowdy", "Slick", "Chaotic", "Flirty",
        "Naughty", "Greasy", "Trashy", "Cheeky", "Bossy", "Funky", "Spicy", "Wild",
        "Thirsty", "Salty", "Clumsy", "Zesty", "Sassy", "Saucy", "Goofy", "Snarky",
        "Snooty", "Cranky", "Chunky", "Greedy", "Jumpy", "Dizzy", "Moody", "Rusty",
        "Sloppy", "Silly", "Cocky", "Feisty", "Gassy", "Loopy", "Quirky", "Smelly",
        "Crusty", "Peppy", "Scrappy", "Bubbly", "Wired", "Pudgy", "Nutty", "Breezy",
        "Crabby", "Grimy", "Prickly", "Bouncy", "Sleepy", "Jazzy", "Tipsy", "Spunky",
        "Snoopy", "Bawdy", "Cuddly", "Dopey", "Fizzy", "Frisky", "Giddy", "Groovy",
        "Hasty", "Icy", "Jolly", "Lumpy", "Mellow", "Nippy", "Plucky", "Punky",
        "Shaky", "Smug", "Sticky", "Stinky", "Touchy", "Tricky", "Twitchy", "Vain",
        "Wicked", "Wonky", "Zippy", "Crafty", "Foxy", "Kooky", "Squeaky"
    )

    private val NOUNS = listOf(
        "Pickle", "Pants", "Biscuit", "Hamster", "Noodle", "Weasel", "Sausage", "Goblin",
        "Meatball", "Goose", "Donut", "Badger", "Taco", "Waffle", "Monkey", "Bandit",
        "Potato", "Chicken", "Llama", "Gremlin", "Muffin", "Turnip", "Nugget", "Baboon",
        "Wombat", "Walrus", "Possum", "Otter", "Duck", "Pelican", "Pigeon", "Toad",
        "Mushroom", "Burrito", "Meatloaf", "Dumpling", "Cheeseball", "Trombone", "Kazoo",
        "Penguin", "Cactus", "Platypus", "Banjo", "Spatula", "Rutabaga", "Critter",
        "Rascal", "Troublemaker", "Bandicoot", "Ferret", "Pangolin", "Armadillo", "Skunk",
        "Porcupine", "Chipmunk", "Hedgehog", "Capybara", "Chinchilla", "Meerkat", "Lemur",
        "Chimp", "Gorilla", "Flamingo", "Vulture", "Ostrich", "Turkey", "Rooster",
        "Bullfrog", "Salamander", "Gecko", "Iguana", "Chameleon", "Bagel", "Pretzel",
        "Cupcake", "Brownie", "Cannoli", "Nacho", "Churro", "Twinkie", "Meathead",
        "Knucklehead", "Goofball", "Dingbat", "Bozo", "Scoundrel", "Whippersnapper"
    )

    fun generateName(): String {
        val adj = ADJECTIVES.random(Random.Default)
        val noun = NOUNS.random(Random.Default)
        return "$adj $noun"
    }

    fun generateUniqueNames(count: Int, existingNames: Set<String> = emptySet()): List<String> {
        val used = existingNames.toMutableSet()
        val result = mutableListOf<String>()
        val maxCombinations = ADJECTIVES.size * NOUNS.size
        var attempts = 0
        val maxAttempts = maxCombinations * 2
        while (result.size < count && attempts < maxAttempts) {
            attempts++
            val name = generateName()
            if (name !in used) {
                used.add(name)
                result.add(name)
            }
        }
        var fallbackIdx = 1
        while (result.size < count) {
            val fallbackName = "${generateName()} $fallbackIdx"
            if (fallbackName !in used) {
                used.add(fallbackName)
                result.add(fallbackName)
            }
            fallbackIdx++
        }
        return result
    }

    fun generateOpponents(count: Int): List<String> {
        return generateUniqueNames(count)
    }

    val totalPossibleCombinations: Int
        get() = ADJECTIVES.size * NOUNS.size
}
