package com.za.games.ui.snake

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.za.games.snake.SnakeDir
import com.za.games.snake.SnakeState
import com.za.games.snake.SnakeStatus
import com.za.games.snake.snakeSpeedMillis
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
