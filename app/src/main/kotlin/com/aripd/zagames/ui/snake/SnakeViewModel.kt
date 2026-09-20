package com.aripd.zagames.ui.snake

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aripd.zagames.snake.SnakeDir
import com.aripd.zagames.snake.SnakeState
import com.aripd.zagames.snake.SnakeStatus
import com.aripd.zagames.snake.snakeSpeedMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SnakeViewModel : ViewModel() {

    private val _state = MutableStateFlow(SnakeState.newGame())
    val state: StateFlow<SnakeState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                val current = _state.value
                if (current.status != SnakeStatus.RUNNING) {
                    _state.first { it.status == SnakeStatus.RUNNING }
                    continue
                }
                delay(snakeSpeedMillis(current.foods))
                _state.update { if (it.status == SnakeStatus.RUNNING) it.tick() else it }
            }
        }
    }

    fun turn(dir: SnakeDir) = _state.update { it.turn(dir) }

    fun pause() = _state.update { it.pause() }

    fun togglePause() = _state.update { it.togglePause() }

    fun newGame() = _state.update { SnakeState.newGame() }
}
