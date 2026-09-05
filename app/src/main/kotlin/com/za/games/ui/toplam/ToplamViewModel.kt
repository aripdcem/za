package com.za.games.ui.toplam

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.za.games.sayi.ToplamAi
import com.za.games.sayi.ToplamState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

enum class ToplamPhase { SETUP, PLAYING }

data class ToplamSetup(val vsComputer: Boolean, val perfect: Boolean)

/** Oturum sayacı: oyuncu 0 galibiyeti, oyuncu 1 galibiyeti, beraberlik. */
data class ToplamTally(val p0: Int = 0, val p1: Int = 0, val draws: Int = 0)

/**
 * Toplam Kapma'yı sürer: insan oyuncu 0, bilgisayar oyuncu 1. Başlayan oyuncu
 * her oyunda değişir. Bilgisayar kısa bir gecikmeyle oynar.
 */
class ToplamViewModel(application: Application) : AndroidViewModel(application) {

    private val store = ToplamStore(application)
    private val rng = Random.Default

    private val _setup = MutableStateFlow(ToplamSetup(store.vsComputer, store.perfect))
    val setup: StateFlow<ToplamSetup> = _setup.asStateFlow()

    private val _phase = MutableStateFlow(ToplamPhase.SETUP)
    val phase: StateFlow<ToplamPhase> = _phase.asStateFlow()

    private val _state = MutableStateFlow(ToplamState())
    val state: StateFlow<ToplamState> = _state.asStateFlow()

    private val _thinking = MutableStateFlow(false)
    val thinking: StateFlow<Boolean> = _thinking.asStateFlow()

    private val _tally = MutableStateFlow(ToplamTally())
    val tally: StateFlow<ToplamTally> = _tally.asStateFlow()

    private val _wins = MutableStateFlow(store.wins)
    val wins: StateFlow<Int> = _wins.asStateFlow()

    private val _gameId = MutableStateFlow(0)
    val gameId: StateFlow<Int> = _gameId.asStateFlow()

    var matchVsComputer: Boolean = true
        private set
    var matchPerfect: Boolean = false
        private set

    private var humanStarts = true
    private var aiJob: Job? = null

    fun setVsComputer(value: Boolean) {
        store.vsComputer = value
        _setup.value = _setup.value.copy(vsComputer = value)
    }

    fun setPerfect(value: Boolean) {
        store.perfect = value
        _setup.value = _setup.value.copy(perfect = value)
    }

    /** Kurulumdan yeni maç: sayaç sıfırlanır, insan başlar. */
    fun start() {
        matchVsComputer = _setup.value.vsComputer
        matchPerfect = _setup.value.perfect
        _tally.value = ToplamTally()
        humanStarts = true
        _phase.value = ToplamPhase.PLAYING
        newGame()
    }

    /** Sonraki oyun: başlayan değişir, sayaç korunur. */
    fun nextGame() {
        humanStarts = !humanStarts
        newGame()
    }

    private fun newGame() {
        aiJob?.cancel()
        _thinking.value = false
        _state.value = ToplamState(turn = if (matchVsComputer && !humanStarts) 1 else 0)
        _gameId.value += 1
        scheduleAi()
    }

    fun humanCanAct(): Boolean {
        val s = _state.value
        if (s.over) return false
        return !matchVsComputer || s.turn != AI_PLAYER
    }

    fun pick(x: Int) {
        if (_phase.value != ToplamPhase.PLAYING || !humanCanAct()) return
        val s = _state.value
        val next = s.pick(x)
        if (next === s) return
        _state.value = next
        onChanged(next)
        scheduleAi()
    }

    private fun onChanged(s: ToplamState) {
        if (!s.over) return
        val t = _tally.value
        _tally.value = when (s.winner) {
            0 -> t.copy(p0 = t.p0 + 1)
            1 -> t.copy(p1 = t.p1 + 1)
            else -> t.copy(draws = t.draws + 1)
        }
        if (matchVsComputer && s.winner == 0) {
            store.wins += 1
            _wins.value = store.wins
        }
    }

    private fun scheduleAi() {
        aiJob?.cancel()
        if (!matchVsComputer) return
        val s = _state.value
        if (s.over || s.turn != AI_PLAYER) return
        aiJob = viewModelScope.launch {
            _thinking.value = true
            try {
                delay(650L)
                val current = _state.value
                if (current.over || current.turn != AI_PLAYER) return@launch
                val move = withContext(Dispatchers.Default) { ToplamAi.choose(current, matchPerfect, rng) }
                val next = current.pick(move)
                _state.value = next
                onChanged(next)
            } finally {
                _thinking.value = false
            }
        }
    }

    fun toSetup() {
        aiJob?.cancel()
        _thinking.value = false
        _phase.value = ToplamPhase.SETUP
    }

    companion object {
        const val AI_PLAYER = 1
    }
}
