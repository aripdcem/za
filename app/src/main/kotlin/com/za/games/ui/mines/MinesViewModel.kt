package com.za.games.ui.mines

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.za.games.mines.MinesDifficulty
import com.za.games.mines.MinesState
import com.za.games.mines.MinesStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Mayın Tarlası sıra tabanlıdır; VM üretimi, süreyi ve kalıcılığı yönetir. */
class MinesViewModel(application: Application) : AndroidViewModel(application) {

    private val store = MinesStore(application)

    /** null = henüz tahta yok; zorluk seçici gösterilir. */
    private val _state = MutableStateFlow<MinesState?>(null)
    val state: StateFlow<MinesState?> = _state.asStateFlow()

    private val _elapsed = MutableStateFlow(0)
    val elapsed: StateFlow<Int> = _elapsed.asStateFlow()

    private var timerPaused = false
    private var lastDifficulty: MinesDifficulty? = null

    init {
        // Yarım kalmış tahta varsa kaldığı yerden devam et.
        store.restore()?.let { (saved, savedElapsed) ->
            _state.value = saved
            _elapsed.value = savedElapsed
            lastDifficulty = MinesDifficulty.entries.first {
                it.width == saved.width && it.height == saved.height
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = _state.value
                if (!timerPaused && current?.status == MinesStatus.RUNNING) {
                    _elapsed.update { it + 1 }
                }
            }
        }
    }

    fun newGame(difficulty: MinesDifficulty) {
        lastDifficulty = difficulty
        _state.value = MinesState.newGame(difficulty)
        _elapsed.value = 0
        store.clear()
    }

    /** Aynı zorlukta yeni tahta. */
    fun retry() {
        lastDifficulty?.let { newGame(it) }
    }

    /** Zorluk seçicisine dön. */
    fun reset() {
        _state.value = null
        _elapsed.value = 0
        store.clear()
    }

    fun setPaused(paused: Boolean) {
        timerPaused = paused
        // Arka plana geçerken süre de kaydedilir; süreç ölse bile korunur.
        if (paused) persist()
    }

    fun reveal(index: Int) = mutate { it?.reveal(index) }

    fun toggleFlag(index: Int) = mutate { it?.toggleFlag(index) }

    fun chord(index: Int) = mutate { it?.chord(index) }

    private fun mutate(transform: (MinesState?) -> MinesState?) {
        _state.update { transform(it) }
        persist()
    }

    private fun persist() {
        val current = _state.value ?: return
        if (current.status == MinesStatus.RUNNING) store.save(current, _elapsed.value) else store.clear()
    }
}
