package com.za.games.ui.tetris

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.za.games.tetris.TetrisState
import com.za.games.tetris.TetrisStatus
import com.za.games.tetris.gravityMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Motoru süren katman: yerçekimi döngüsünü işletir ve oyuncu girdilerini
 * değişmez [TetrisState] geçişlerine çevirir.
 */
class TetrisViewModel : ViewModel() {

    private val _state = MutableStateFlow(TetrisState.newGame())
    val state: StateFlow<TetrisState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                val current = _state.value
                if (current.status != TetrisStatus.RUNNING) {
                    // Duraklatma/oyun sonu: koşmaya başlayana dek bekle.
                    _state.first { it.status == TetrisStatus.RUNNING }
                    continue
                }
                delay(gravityMillis(current.level))
                _state.update { if (it.status == TetrisStatus.RUNNING) it.tick() else it }
            }
        }
    }

    fun moveLeft() = _state.update { it.moveLeft() }

    fun moveRight() = _state.update { it.moveRight() }

    fun rotateClockwise() = _state.update { it.rotate(clockwise = true) }

    fun rotateCounterClockwise() = _state.update { it.rotate(clockwise = false) }

    fun softDrop() = _state.update { it.softDrop() }

    fun hardDrop() = _state.update { it.hardDrop() }

    fun hold() = _state.update { it.holdPiece() }

    fun pause() = _state.update { it.pause() }

    fun togglePause() = _state.update { it.togglePause() }

    fun newGame() = _state.update { TetrisState.newGame() }
}
