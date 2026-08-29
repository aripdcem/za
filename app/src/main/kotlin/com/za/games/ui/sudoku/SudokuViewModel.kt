package com.za.games.ui.sudoku

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.za.games.sudoku.SudokuDifficulty
import com.za.games.sudoku.SudokuState
import com.za.games.sudoku.SudokuStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Sudoku sıra tabanlıdır; VM yalnızca üretimi ve süreyi yönetir. */
class SudokuViewModel : ViewModel() {

    /** null = henüz bulmaca yok; zorluk seçici gösterilir. */
    private val _state = MutableStateFlow<SudokuState?>(null)
    val state: StateFlow<SudokuState?> = _state.asStateFlow()

    private val _elapsed = MutableStateFlow(0)
    val elapsed: StateFlow<Int> = _elapsed.asStateFlow()

    private var timerPaused = false
    private var lastDifficulty: SudokuDifficulty? = null

    init {
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = _state.value
                if (!timerPaused && current?.status == SudokuStatus.RUNNING) {
                    _elapsed.update { it + 1 }
                }
            }
        }
    }

    fun newGame(difficulty: SudokuDifficulty) {
        lastDifficulty = difficulty
        viewModelScope.launch {
            val fresh = withContext(Dispatchers.Default) { SudokuState.newGame(difficulty) }
            _state.value = fresh
            _elapsed.value = 0
        }
    }

    /** Aynı zorlukta yeni bulmaca. */
    fun retry() {
        lastDifficulty?.let { newGame(it) }
    }

    /** Zorluk seçicisine dön. */
    fun reset() {
        _state.value = null
        _elapsed.value = 0
    }

    fun setPaused(paused: Boolean) {
        timerPaused = paused
    }

    fun setValue(index: Int, value: Int) = _state.update { it?.setValue(index, value) }

    fun toggleNote(index: Int, value: Int) = _state.update { it?.toggleNote(index, value) }

    fun clearCell(index: Int) = _state.update { it?.clearCell(index) }
}
