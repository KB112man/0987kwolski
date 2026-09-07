package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.model.*
import com.example.ui.GameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ExpandedHandTest {
    
    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    private fun c(suit: Suit, rank: Rank, id: String): Card {
        return Card(id, rank, suit, 0)
    }

    private fun makeCards(count: Int): List<Card> {
        val cards = mutableListOf<Card>()
        val suits = listOf(Suit.HEARTS, Suit.SPADES, Suit.DIAMONDS, Suit.CLUBS)
        val validRanks = Rank.values().filter { it != Rank.JOKER }
        for (i in 0 until count) {
            val suit = suits[i % 4]
            val rank = validRanks[i % validRanks.size]
            cards.add(c(suit, rank, "c$i"))
        }
        return cards
    }

    @Test
    fun testTableRackAndOverflowCounts() {
        val cards = makeCards(21)
        val player = Player(id = "human", name = "You", isHuman = true, hand = cards)
        
        // 20 active cards => 20 TABLE RACK, 0 OVERFLOW
        val player20 = player.copy(hand = cards.take(20))
        assertEquals(20, player20.activeCards.take(20).size)
        assertEquals(0, player20.activeCards.drop(20).size)

        // 21 active cards => 20 TABLE RACK, 1 OVERFLOW
        assertEquals(20, player.activeCards.take(20).size)
        assertEquals(1, player.activeCards.drop(20).size)
    }

    @Test
    fun testManualReorderUpdatesMembership() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val cards = makeCards(21)
        val viewModel = GameViewModel(app)
        
        val initialHuman = Player(id = "human", name = "You", isHuman = true, hand = cards)
        val gameState = GameState(
            players = listOf(initialHuman, Player(id = "ai", name = "AI", hand = emptyList())),
            currentTurnPlayerIndex = 0
        )
        // Access via reflection or public methods to set initial state if needed, 
        // but here we can just use setGameState if it exists, or start a new game and overwrite.
        // GameViewModel has a method `startNewGame` which sets up a game state. 
        // We will call startNewGame, then use reflection to set _gameState to avoid compilation errors on private.
        
        val field = GameViewModel::class.java.getDeclaredField("_gameState")
        field.isAccessible = true
        val stateFlow = field.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<GameState?>
        stateFlow.value = gameState
        
        val cardAtPos21 = cards[20]
        val cardAtPos1 = cards[0]
        
        assertTrue(viewModel.gameState.value?.humanPlayer?.activeCards?.drop(20)?.contains(cardAtPos21) == true)
        
        viewModel.swapCardsInHand(cardAtPos21.id, cardAtPos1.id)
        
        val updatedHuman = viewModel.gameState.value?.humanPlayer!!
        val newTableRack = updatedHuman.activeCards.take(20)
        val newOverflow = updatedHuman.activeCards.drop(20)
        
        assertTrue(newTableRack.contains(cardAtPos21))
        assertTrue(newOverflow.contains(cardAtPos1))
        
        assertEquals(21, updatedHuman.hand.size)
        assertEquals(21, updatedHuman.activeCards.size)
    }

    @Test
    fun testPocketingPromotesOverflow() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val cards = makeCards(22)
        val viewModel = GameViewModel(app)
        
        val initialHuman = Player(id = "human", name = "You", isHuman = true, hand = cards)
        val gameState = GameState(
            players = listOf(initialHuman, Player(id = "ai", name = "AI", hand = emptyList())),
            currentTurnPlayerIndex = 0
        )
        val field = GameViewModel::class.java.getDeclaredField("_gameState")
        field.isAccessible = true
        val stateFlow = field.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<GameState?>
        stateFlow.value = gameState
        
        val cardToPocket = cards[5]
        val expectedPromotedCard = cards[20]
        val overflowCard2 = cards[21]
        
        viewModel.moveCardsToPocket(setOf(cardToPocket.id))
        
        val updatedHuman = viewModel.gameState.value?.humanPlayer!!
        val newTableRack = updatedHuman.activeCards.take(20)
        val newOverflow = updatedHuman.activeCards.drop(20)
        
        assertEquals(21, updatedHuman.activeCards.size)
        assertEquals(1, updatedHuman.pocketCards.size)
        
        assertTrue(newTableRack.contains(expectedPromotedCard))
        assertTrue(newOverflow.contains(overflowCard2))
        
        assertEquals(22, updatedHuman.hand.size)
    }

    @Test
    fun testReturningPocketRecalculatesPositions() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val cards = makeCards(21)
        val viewModel = GameViewModel(app)
        
        val initialHuman = Player(id = "human", name = "You", isHuman = true, hand = cards, pocketCardIds = setOf(cards[20].id))
        val gameState = GameState(
            players = listOf(initialHuman, Player(id = "ai", name = "AI", hand = emptyList())),
            currentTurnPlayerIndex = 0
        )
        val field = GameViewModel::class.java.getDeclaredField("_gameState")
        field.isAccessible = true
        val stateFlow = field.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<GameState?>
        stateFlow.value = gameState
        
        assertEquals(20, initialHuman.activeCards.size)
        assertEquals(0, initialHuman.activeCards.drop(20).size)
        
        viewModel.moveCardsToHand(setOf(cards[20].id))
        
        val updatedHuman = viewModel.gameState.value?.humanPlayer!!
        val newTableRack = updatedHuman.activeCards.take(20)
        val newOverflow = updatedHuman.activeCards.drop(20)
        
        assertEquals(21, updatedHuman.activeCards.size)
        assertEquals(1, newOverflow.size)
        assertEquals(cards[20], newOverflow[0])
    }

    @Test
    fun testAutoSortRecalculates() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val cards = makeCards(21).shuffled()
        val viewModel = GameViewModel(app)
        
        val initialHuman = Player(id = "human", name = "You", isHuman = true, hand = cards)
        val gameState = GameState(
            players = listOf(initialHuman, Player(id = "ai", name = "AI", hand = emptyList())),
            currentTurnPlayerIndex = 0
        )
        val field = GameViewModel::class.java.getDeclaredField("_gameState")
        field.isAccessible = true
        val stateFlow = field.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<GameState?>
        stateFlow.value = gameState
        
        viewModel.sortHumanHand(SortMode.RANK)
        
        val updatedHuman = viewModel.gameState.value?.humanPlayer!!
        val sortedCards = updatedHuman.activeCards
        
        val newTableRack = sortedCards.take(20)
        val newOverflow = sortedCards.drop(20)
        
        assertEquals(20, newTableRack.size)
        assertEquals(1, newOverflow.size)
        assertEquals(21, updatedHuman.hand.size)
    }
}
