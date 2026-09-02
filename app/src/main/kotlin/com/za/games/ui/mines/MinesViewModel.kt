package com.za.games.ui.mines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.za.games.mines.MinesDifficulty
import com.za.games.mines.MinesState
import com.za.games.mines.MinesStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MinesViewModel : ViewModel() {

    /** null = henüz tahta yok; zorluk seçici gösterilir. */
    private val _state = MutableStateFlow<MinesState?>(null)
    val state: StateFlow<MinesState?> = _state.asStateFlow()

    private val _elapsed = MutableStateFlow(0)
    val elapsed: StateFlow<Int> = _elapsed.asStateFlow()

    private val _timerPaused = MutableStateFlow(false)
    private var lastDifficulty: MinesDifficulty? = null

    init {
        // Sayaç yalnızca oyun koşarken tikler; aksi hâlde askıda bekler —
        // duraklatılmışken veya menüdeyken saniyede bir uyanmaz.
        viewModelScope.launch {
            while (true) {
                combine(_state, _timerPaused) { state, paused ->
                    !paused && state?.status == MinesStatus.RUNNING
                }.first { it }
                delay(1000L)
                val current = _state.value
                if (!_timerPaused.value && current?.status == MinesStatus.RUNNING) {
                    _elapsed.update { it + 1 }
                }
            }
        }
    }

    fun newGame(difficulty: MinesDifficulty) {
        lastDifficulty = difficulty
        _state.value = MinesState.newGame(difficulty)
        _elapsed.value = 0
    }

    /** Aynı zorlukta yeni tahta. */
    fun retry() {
        lastDifficulty?.let { newGame(it) }
    }

    /** Zorluk seçicisine dön. */
    fun reset() {
        _state.value = null
        _elapsed.value = 0
    }

    fun setPaused(paused: Boolean) {
        _timerPaused.value = paused
    }

    fun reveal(index: Int) = _state.update { it?.reveal(index) }

    fun toggleFlag(index: Int) = _state.update { it?.toggleFlag(index) }

    fun chord(index: Int) = _state.update { it?.chord(index) }
}
