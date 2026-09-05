package com.za.games.ui.vergici

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.za.games.sayi.VergiciSolver
import com.za.games.sayi.VergiciState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class VergiciPhase { SETUP, PLAYING, OVER }

/** Hedef puan: küçük aralıklarda tam arama (kesin), büyüklerde açgözlü sezgisel (yaklaşık). */
data class VergiciTarget(val score: Int, val exact: Boolean)

/**
 * Vergici sıra tabanlıdır. Seçim iki adımlıdır: sayıya dokununca vergicinin
 * alacağı bölenler gösterilir, tekrar dokununca ya da "Al" ile onaylanır.
 */
class VergiciViewModel(application: Application) : AndroidViewModel(application) {

    private val store = VergiciStore(application)

    private val _size = MutableStateFlow(store.size)
    val size: StateFlow<Int> = _size.asStateFlow()

    private val _phase = MutableStateFlow(VergiciPhase.SETUP)
    val phase: StateFlow<VergiciPhase> = _phase.asStateFlow()

    private val _state = MutableStateFlow<VergiciState?>(null)
    val state: StateFlow<VergiciState?> = _state.asStateFlow()

    private val _selected = MutableStateFlow<Int?>(null)
    val selected: StateFlow<Int?> = _selected.asStateFlow()

    private val _target = MutableStateFlow<VergiciTarget?>(null)
    val target: StateFlow<VergiciTarget?> = _target.asStateFlow()

    private val _wins = MutableStateFlow(store.wins)
    val wins: StateFlow<Int> = _wins.asStateFlow()

    private val _best = MutableStateFlow(store.best(store.size))
    val best: StateFlow<Int> = _best.asStateFlow()

    private var runId = 0

    fun setSize(n: Int) {
        if (n !in VergiciState.SIZES) return
        store.size = n
        _size.value = n
        _best.value = store.best(n)
    }

    fun start() {
        val n = _size.value
        val token = ++runId
        _state.value = VergiciState.start(n)
        _selected.value = null
        _target.value = null
        _phase.value = VergiciPhase.PLAYING
        viewModelScope.launch {
            val exact = withContext(Dispatchers.Default) { VergiciSolver.optimal(n) }
            val target = if (exact != null) {
                VergiciTarget(exact, exact = true)
            } else {
                VergiciTarget(withContext(Dispatchers.Default) { VergiciSolver.greedyScore(n) }, exact = false)
            }
            if (token == runId) _target.value = target
        }
    }

    /** Dokunma: alınabilir sayıyı seçer; seçiliye tekrar dokunmak onaylar. */
    fun select(x: Int) {
        val s = _state.value ?: return
        if (_phase.value != VergiciPhase.PLAYING) return
        if (!s.canTake(x)) {
            _selected.value = null
            return
        }
        if (_selected.value == x) confirm() else _selected.value = x
    }

    fun confirm() {
        val s = _state.value ?: return
        val x = _selected.value ?: return
        if (_phase.value != VergiciPhase.PLAYING) return
        val next = s.take(x)
        if (next === s) return
        _state.value = next
        _selected.value = null
        if (next.over) finish(next)
    }

    private fun finish(s: VergiciState) {
        _phase.value = VergiciPhase.OVER
        if (s.player > s.taxman) {
            store.wins += 1
            _wins.value = store.wins
        }
        if (s.player > store.best(s.n)) {
            store.setBest(s.n, s.player)
            _best.value = s.player
        }
    }

    fun retry() = start()

    fun toSetup() {
        runId++
        _phase.value = VergiciPhase.SETUP
        _state.value = null
        _selected.value = null
    }
}
