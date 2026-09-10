package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.replay.PublicReplayState
import com.example.engine.replay.ReplayEngine
import com.example.model.replay.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PlaybackSpeed(val speedMs: Long, val label: String) {
    NORMAL(750L, "1x"),
    FAST(300L, "2.5x"),
    SLOW(1200L, "0.5x")
}

/**
 * ReplayViewModel:
 * Scoped to ViewModel lifecycle to prevent coroutine leaks.
 * Drives public-only table state via ReplayEngine.
 * Does NOT touch GamePreferences or live GameViewModel tournament state.
 */
class ReplayViewModel(
    val cartridge: ReplayCartridge = CartridgeFactory.createSampleCartridge()
) : ViewModel() {

    private val engine = ReplayEngine(cartridge)

    private val _uiState = MutableStateFlow<PublicReplayState>(engine.getPublicState())
    val uiState: StateFlow<PublicReplayState> = _uiState.asStateFlow()

    private val _actionBanner = MutableStateFlow("Starting Replay...")
    val actionBanner: StateFlow<String> = _actionBanner.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(PlaybackSpeed.NORMAL)
    val playbackSpeed: StateFlow<PlaybackSpeed> = _playbackSpeed.asStateFlow()

    private var playbackJob: Job? = null

    init {
        updateStateFromEngine()
    }

    private fun updateStateFromEngine() {
        val state = engine.getPublicState()
        _uiState.value = state
        _actionBanner.value = state.lastActionDescription
    }

    fun startPlayback(speedMs: Long = _playbackSpeed.value.speedMs) {
        playbackJob?.cancel()
        _isPlaying.value = true

        playbackJob = viewModelScope.launch {
            try {
                while (_isPlaying.value) {
                    val advanced = engine.stepForward()
                    updateStateFromEngine()

                    if (!advanced || _uiState.value.isRoundComplete) {
                        _isPlaying.value = false
                        _actionBanner.value = "Replay Complete"
                        break
                    }
                    delay(speedMs)
                }
            } catch (e: Exception) {
                _isPlaying.value = false
                _actionBanner.value = "Playback stopped: ${e.message ?: "Divergence"}"
            }
        }
    }

    fun pausePlayback() {
        playbackJob?.cancel()
        _isPlaying.value = false
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pausePlayback()
        } else {
            // If already at end, restart from beginning
            if (_uiState.value.currentStepIndex >= _uiState.value.totalSteps) {
                restart()
            }
            startPlayback()
        }
    }

    fun stepForwardManual() {
        pausePlayback()
        if (engine.stepForward()) {
            updateStateFromEngine()
        }
    }

    fun stepBackwardManual() {
        pausePlayback()
        if (engine.stepBackward()) {
            updateStateFromEngine()
        }
    }

    fun restart() {
        pausePlayback()
        engine.resetToInitialDeal()
        updateStateFromEngine()
    }

    fun cycleSpeed() {
        val next = when (_playbackSpeed.value) {
            PlaybackSpeed.NORMAL -> PlaybackSpeed.FAST
            PlaybackSpeed.FAST -> PlaybackSpeed.SLOW
            PlaybackSpeed.SLOW -> PlaybackSpeed.NORMAL
        }
        _playbackSpeed.value = next
        if (_isPlaying.value) {
            startPlayback(next.speedMs)
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel() // Enforce coroutine leak safety
    }
}
