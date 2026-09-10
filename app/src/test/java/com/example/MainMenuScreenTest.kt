package com.example

import com.example.model.ContractLevel
import org.junit.Assert.*
import org.junit.Test

class MainMenuScreenTest {

    @Test
    fun testContractLevelDealCountsMatchAuthoritativeSpecification() {
        // Authoritative values specified by Rothgar:
        // L1 = 10, L2 = 10, L3 = 10, L4 = 10, L5 = 13, L6 = 13, L7 = 15
        assertEquals("Level 1 must deal 10 cards", 10, ContractLevel.LEVEL_1.dealCount)
        assertEquals("Level 2 must deal 10 cards", 10, ContractLevel.LEVEL_2.dealCount)
        assertEquals("Level 3 must deal 10 cards", 10, ContractLevel.LEVEL_3.dealCount)
        assertEquals("Level 4 must deal 10 cards", 10, ContractLevel.LEVEL_4.dealCount)
        assertEquals("Level 5 must deal 13 cards", 13, ContractLevel.LEVEL_5.dealCount)
        assertEquals("Level 6 must deal 13 cards", 13, ContractLevel.LEVEL_6.dealCount)
        assertEquals("Level 7 must deal 15 cards", 15, ContractLevel.LEVEL_7.dealCount)
    }

    @Test
    fun testContractLevelDescriptionsDerivedFromAuthoritativeModel() {
        assertEquals("3 Runs (No Discard)", ContractLevel.LEVEL_7.shortRequirement)
        assertTrue("Level 7 is no discard", ContractLevel.LEVEL_7.noDiscard)
        assertFalse("Level 1 is not no discard", ContractLevel.LEVEL_1.noDiscard)
        assertFalse("Level 6 is not no discard", ContractLevel.LEVEL_6.noDiscard)

        assertEquals(2, ContractLevel.LEVEL_1.requiredBooks)
        assertEquals(0, ContractLevel.LEVEL_1.requiredRuns)

        assertEquals(1, ContractLevel.LEVEL_2.requiredBooks)
        assertEquals(1, ContractLevel.LEVEL_2.requiredRuns)

        assertEquals(0, ContractLevel.LEVEL_3.requiredBooks)
        assertEquals(2, ContractLevel.LEVEL_3.requiredRuns)

        assertEquals(3, ContractLevel.LEVEL_4.requiredBooks)
        assertEquals(0, ContractLevel.LEVEL_4.requiredRuns)

        assertEquals(2, ContractLevel.LEVEL_5.requiredBooks)
        assertEquals(1, ContractLevel.LEVEL_5.requiredRuns)

        assertEquals(1, ContractLevel.LEVEL_6.requiredBooks)
        assertEquals(2, ContractLevel.LEVEL_6.requiredRuns)

        assertEquals(0, ContractLevel.LEVEL_7.requiredBooks)
        assertEquals(3, ContractLevel.LEVEL_7.requiredRuns)
    }
}
