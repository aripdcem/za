package com.za.games.ui.kakuro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.za.games.kakuro.KakuroDifficulty
import com.za.games.kakuro.KakuroState
import com.za.games.kakuro.KakuroStatus
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

/** Kakuro sıra tabanlıdır; VM üretimi (arka planda), süreyi ve kalıcılığı yönetir. */
class KakuroViewModel(application: Application) : AndroidViewModel(application) {

    private val store = KakuroStore(application)

    /** null = henüz bulmaca yok; zorluk seçici gösterilir. */
    private val _state = MutableStateFlow<KakuroState?>(null)
    val state: StateFlow<KakuroState?> = _state.asStateFlow()

    private val _generating = MutableStateFlow(false)
    val generating: StateFlow<Boolean> = _generating.asStateFlow()

    private val _elapsed = MutableStateFlow(0)
    val elapsed: StateFlow<Int> = _elapsed.asStateFlow()

    private val history = ArrayDeque<KakuroState>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _timerPaused = MutableStateFlow(false)

    var lastDifficulty: KakuroDifficulty? = null
        private set

    private var generation = 0

    init {
        val restored = store.restore()
        if (restored != null) {
            val (saved, savedElapsed) = restored
            _state.value = saved
            _elapsed.value = savedElapsed
        }
        lastDifficulty = restored?.first?.difficulty ?: store.lastDifficulty()
        viewModelScope.launch {
            while (true) {
                combine(_state, _timerPaused) { state, paused ->
                    !paused && state?.status == KakuroStatus.RUNNING
                }.first { it }
                delay(1000L)
                val current = _state.value
                if (!_timerPaused.value && current?.status == KakuroStatus.RUNNING) {
                    _elapsed.update { it + 1 }
                }
            }
        }
    }

    fun newGame(difficulty: KakuroDifficulty) {
        lastDifficulty = difficulty
        val token = ++generation
        _generating.value = true
        viewModelScope.launch {
            val fresh = withContext(Dispatchers.Default) { KakuroState.newGame(difficulty) }
            if (token != generation) return@launch
            clearHistory()
            _state.value = fresh
            _elapsed.value = 0
            _generating.value = false
            store.save(fresh, 0)
        }
    }

    fun retry() {
        lastDifficulty?.let { newGame(it) }
    }

    /** Zorluk seçicisine dön. */
    fun reset() {
        generation++
        _generating.value = false
        clearHistory()
        _state.value = null
        _elapsed.value = 0
        store.clear()
    }

    fun setPaused(paused: Boolean) {
        _timerPaused.value = paused
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

    private fun mutate(transform: (KakuroState) -> KakuroState) {
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
        if (current.status == KakuroStatus.SOLVED) store.clear() else store.save(current, _elapsed.value)
    }

    private fun clearHistory() {
        history.clear()
        _canUndo.value = false
    }

    private companion object {
        const val HISTORY_LIMIT = 100
    }
}
