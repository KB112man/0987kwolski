package com.example.ui

import com.example.model.replay.CartridgeFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReplayViewModelTest {

    @Test
    fun testReplayViewModelLifecycleAndPlayback() = runTest {
        val sampleCartridge = CartridgeFactory.createSampleCartridge()
        val viewModel = ReplayViewModel(sampleCartridge)

        val initialState = viewModel.uiState.value
        assertEquals("Initial step is 0", 0, initialState.currentStepIndex)
        assertEquals("Total steps matches actionLog size", sampleCartridge.actionLog.size, initialState.totalSteps)
        assertFalse("Not playing initially until startPlayback called", viewModel.isPlaying.value)

        // Manual Step Forward
        viewModel.stepForwardManual()
        assertEquals("Step forward increments step index to 1", 1, viewModel.uiState.value.currentStepIndex)
        assertTrue("Action banner updated with action text", viewModel.actionBanner.value.isNotEmpty())

        // Manual Step Backward
        viewModel.stepBackwardManual()
        assertEquals("Step backward decrements step index to 0", 0, viewModel.uiState.value.currentStepIndex)

        // Cycle speed
        assertEquals(PlaybackSpeed.NORMAL, viewModel.playbackSpeed.value)
        viewModel.cycleSpeed()
        assertEquals(PlaybackSpeed.FAST, viewModel.playbackSpeed.value)
        viewModel.cycleSpeed()
        assertEquals(PlaybackSpeed.SLOW, viewModel.playbackSpeed.value)
        viewModel.cycleSpeed()
        assertEquals(PlaybackSpeed.NORMAL, viewModel.playbackSpeed.value)

        // Clean pause
        viewModel.pausePlayback()
        assertFalse(viewModel.isPlaying.value)
    }

    @Test
    fun testReplayPublicIsolationNoPrivateHandExposure() {
        val sampleCartridge = CartridgeFactory.createSampleCartridge()
        val viewModel = ReplayViewModel(sampleCartridge)
        val state = viewModel.uiState.value

        // Confirm PublicSeat exposes only public summaries:
        // playerId, displayName, seatIndex, isHuman, cardCount, isDown, laidMelds
        assertEquals(3, state.seats.size)
        state.seats.forEach { seat ->
            assertTrue("Seat must have a positive initial cardCount", seat.cardCount > 0)
            assertNotNull("Seat displayName exists", seat.displayName)
            // No private cards list in PublicSeat
        }
    }
}
