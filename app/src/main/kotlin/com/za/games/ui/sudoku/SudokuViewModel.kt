package com.za.games.ui.sudoku

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.za.games.sudoku.SudokuDifficulty
import com.za.games.sudoku.SudokuState
import com.za.games.sudoku.SudokuStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Sudoku sıra tabanlıdır; VM üretimi, süreyi ve kalıcılığı yönetir. */
class SudokuViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SudokuStore(application)

    /** null = henüz bulmaca yok; zorluk seçici gösterilir. */
    private val _state = MutableStateFlow<SudokuState?>(null)
    val state: StateFlow<SudokuState?> = _state.asStateFlow()

    private val _elapsed = MutableStateFlow(0)
    val elapsed: StateFlow<Int> = _elapsed.asStateFlow()

    private val history = ArrayDeque<SudokuState>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _timerPaused = MutableStateFlow(false)

    /** Zorluk seçicide en son oynanan zorluğu öne çıkarmak için; cihaz yeniden açılsa da kalır. */
    var lastDifficulty: SudokuDifficulty? = null
        private set

    init {
        // Yarım kalmış bulmaca varsa kaldığı yerden devam et.
        val restored = store.restore()
        if (restored != null) {
            val (saved, savedElapsed) = restored
            _state.value = saved
            _elapsed.value = savedElapsed
        }
        lastDifficulty = restored?.first?.difficulty ?: store.lastDifficulty()
        // Sayaç yalnızca oyun koşarken tikler; aksi hâlde askıda bekler —
        // duraklatılmışken veya menüdeyken saniyede bir uyanmaz.
        viewModelScope.launch {
            while (true) {
                combine(_state, _timerPaused) { state, paused ->
                    !paused && state?.status == SudokuStatus.RUNNING
                }.first { it }
                delay(1000L)
                val current = _state.value
                if (!_timerPaused.value && current?.status == SudokuStatus.RUNNING) {
                    _elapsed.update { it + 1 }
                }
            }
        }
    }

    fun newGame(difficulty: SudokuDifficulty) {
        lastDifficulty = difficulty
        viewModelScope.launch {
            val fresh = withContext(Dispatchers.Default) { SudokuState.newGame(difficulty) }
            clearHistory()
            _state.value = fresh
            _elapsed.value = 0
            store.save(fresh, 0)
        }
    }

    /** Aynı zorlukta yeni bulmaca. */
    fun retry() {
        lastDifficulty?.let { newGame(it) }
    }

    /** Zorluk seçicisine dön. */
    fun reset() {
        clearHistory()
        _state.value = null
        _elapsed.value = 0
        store.clear()
    }

    fun setPaused(paused: Boolean) {
        _timerPaused.value = paused
        // Arka plana geçerken süre de kaydedilir; süreç ölse bile korunur.
        if (paused) persist()
    }

    fun setValue(index: Int, value: Int) = mutate { it.setValue(index, value) }

    fun toggleNote(index: Int, value: Int) = mutate { it.toggleNote(index, value) }

    fun clearCell(index: Int) = mutate { it.clearCell(index) }

    fun undo() {
        val previous = history.removeLastOrNull() ?: return
        _state.value = previous
        _canUndo.value = history.isNotEmpty()
        persist()
    }

    /** Durumu değiştiren hamlelerde önceki durumu geri alma yığınına iter. */
    private fun mutate(transform: (SudokuState) -> SudokuState) {
        val before = _state.value ?: return
        val after = transform(before)
        if (after == before) return
        history.addLast(before)
        if (history.size > HISTORY_LIMIT) history.removeFirst()
        _state.value = after
        _canUndo.value = true
        persist()
    }

    private fun persist() {
        val current = _state.value ?: return
        if (current.status == SudokuStatus.SOLVED) store.clear() else store.save(current, _elapsed.value)
    }

    private fun clearHistory() {
        history.clear()
        _canUndo.value = false
    }

    private companion object {
        const val HISTORY_LIMIT = 100
    }
}
