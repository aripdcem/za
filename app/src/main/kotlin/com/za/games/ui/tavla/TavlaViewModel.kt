package com.za.games.ui.tavla

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.za.games.tavla.Move
import com.za.games.tavla.Phase
import com.za.games.tavla.TavlaAi
import com.za.games.tavla.TavlaMode
import com.za.games.tavla.TavlaRules
import com.za.games.tavla.TavlaState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

enum class TavlaPhase { SETUP, PLAYING }

/** Kurulum kartındaki seçimler; tercihler kalıcıdır. */
data class TavlaSetup(
    val mode: TavlaMode,
    val vsComputer: Boolean,
    val target: Int,
    val noPinInOpponentHome: Boolean,
    val cube: Boolean,
)

/**
 * Maçı sürer. İnsan her zaman oyuncu 0'dır; bilgisayar oyuncu 1. Bilgisayarın
 * sırası geldiğinde küçük gecikmelerle zar atar, katlama kararı verir ve
 * hamlelerini teker teker oynar; hesap arka planda yapılır.
 */
class TavlaViewModel(application: Application) : AndroidViewModel(application) {

    private val store = TavlaStore(application)

    private val _setup = MutableStateFlow(
        TavlaSetup(store.mode, store.vsComputer, store.target, store.noPinInOpponentHome, store.cube),
    )
    val setup: StateFlow<TavlaSetup> = _setup.asStateFlow()

    private val _phase = MutableStateFlow(TavlaPhase.SETUP)
    val phase: StateFlow<TavlaPhase> = _phase.asStateFlow()

    private val _state = MutableStateFlow<TavlaState?>(null)
    val state: StateFlow<TavlaState?> = _state.asStateFlow()

    private val _matchId = MutableStateFlow(0)
    val matchId: StateFlow<Int> = _matchId.asStateFlow()

    private val _thinking = MutableStateFlow(false)
    val thinking: StateFlow<Boolean> = _thinking.asStateFlow()

    private val _wins = MutableStateFlow(store.wins)
    val wins: StateFlow<Int> = _wins.asStateFlow()

    /** Son oynanan hamle (vurgulama için). */
    private val _lastMove = MutableStateFlow<Move?>(null)
    val lastMove: StateFlow<Move?> = _lastMove.asStateFlow()

    /** Bu maç bilgisayara karşı mı (kurulumdaki seçim maç boyunca sabit). */
    var matchVsComputer: Boolean = true
        private set

    private var aiJob: Job? = null

    fun setMode(mode: TavlaMode) = updateSetup { it.copy(mode = mode) }.also { store.mode = mode }

    fun setVsComputer(value: Boolean) = updateSetup { it.copy(vsComputer = value) }.also { store.vsComputer = value }

    fun setTarget(value: Int) = updateSetup { it.copy(target = value) }.also { store.target = value }

    fun setNoPin(value: Boolean) = updateSetup { it.copy(noPinInOpponentHome = value) }.also { store.noPinInOpponentHome = value }

    fun setCube(value: Boolean) = updateSetup { it.copy(cube = value) }.also { store.cube = value }

    private fun updateSetup(f: (TavlaSetup) -> TavlaSetup) {
        _setup.value = f(_setup.value)
    }

    /** İnsanın işlem yapabileceği an: kendi sırası ya da kendisine yapılmış küp teklifi. */
    fun humanCanAct(state: TavlaState): Boolean {
        if (!matchVsComputer) return true
        return when (state.phase) {
            Phase.DOUBLE_OFFERED -> state.responder != AI_PLAYER
            Phase.TO_ROLL, Phase.MOVING -> state.turn != AI_PLAYER
            else -> false
        }
    }

    fun start() {
        val s = _setup.value
        matchVsComputer = s.vsComputer
        val rules = TavlaRules(s.mode, s.target, s.noPinInOpponentHome, s.cube)
        aiJob?.cancel()
        _state.value = TavlaState.newMatch(rules, Random.nextLong()).openingRoll()
        _lastMove.value = null
        _matchId.value += 1
        _phase.value = TavlaPhase.PLAYING
        scheduleAi()
    }

    fun toSetup() {
        aiJob?.cancel()
        _thinking.value = false
        _phase.value = TavlaPhase.SETUP
        _state.value = null
    }

    fun roll() = act { it.roll() }

    fun move(from: Int, to: Int) = act { s ->
        s.move(from, to).also { if (it !== s) _lastMove.value = Move(from, to, 0) }
    }

    fun undo() = act { it.undo() }

    fun endTurn() = act { it.endTurn() }

    fun offerDouble() = act { it.offerDouble() }

    fun acceptDouble() = act { it.acceptDouble() }

    fun declineDouble() = act { it.declineDouble() }

    fun nextGame() {
        val cur = _state.value ?: return
        if (cur.phase != Phase.GAME_OVER) return
        aiJob?.cancel()
        _state.value = cur.nextGame().openingRoll()
        _lastMove.value = null
        scheduleAi()
    }

    private fun act(f: (TavlaState) -> TavlaState) {
        val cur = _state.value ?: return
        if (!humanCanAct(cur)) return
        val next = f(cur)
        if (next === cur) return
        _state.value = next
        onChanged(next)
        scheduleAi()
    }

    private fun onChanged(s: TavlaState) {
        if (s.phase == Phase.MATCH_OVER && matchVsComputer && s.winner == 0) {
            store.wins += 1
            _wins.value = store.wins
        }
    }

    private fun scheduleAi() {
        aiJob?.cancel()
        if (!matchVsComputer) return
        val s = _state.value ?: return
        val acts = when (s.phase) {
            Phase.TO_ROLL, Phase.MOVING -> s.turn == AI_PLAYER
            Phase.DOUBLE_OFFERED -> s.responder == AI_PLAYER
            else -> false
        }
        if (!acts) return
        aiJob = viewModelScope.launch { runAi() }
    }

    private suspend fun runAi() {
        _thinking.value = true
        try {
            while (true) {
                val s = _state.value ?: return
                when {
                    s.phase == Phase.DOUBLE_OFFERED && s.responder == AI_PLAYER -> {
                        delay(700L)
                        val accept = withContext(Dispatchers.Default) { TavlaAi.acceptsDouble(s) }
                        val next = if (accept) s.acceptDouble() else s.declineDouble()
                        _state.value = next
                        onChanged(next)
                        return // kabulde sıra teklif eden insandadır; peste oyun bitmiştir
                    }
                    s.phase == Phase.TO_ROLL && s.turn == AI_PLAYER -> {
                        delay(650L)
                        val wants = withContext(Dispatchers.Default) { TavlaAi.wantsDouble(s) }
                        if (wants) {
                            _state.value = s.offerDouble()
                            return // insan yanıtlayacak
                        }
                        _state.value = s.roll()
                    }
                    s.phase == Phase.MOVING && s.turn == AI_PLAYER -> {
                        if (!s.canMove) {
                            delay(1000L)
                            val next = s.endTurn()
                            _state.value = next
                            onChanged(next)
                            return
                        }
                        val turn = withContext(Dispatchers.Default) { TavlaAi.chooseTurn(s) }
                        var cur = s
                        for (m in turn) {
                            delay(420L)
                            val next = cur.move(m.from, m.to)
                            if (next === cur) break
                            cur = next
                            _state.value = cur
                            _lastMove.value = m
                            if (cur.phase != Phase.MOVING) break
                        }
                        if (cur.phase == Phase.MOVING) {
                            delay(300L)
                            cur = cur.endTurn()
                            _state.value = cur
                        }
                        onChanged(cur)
                        return
                    }
                    else -> return
                }
            }
        } finally {
            _thinking.value = false
        }
    }

    companion object {
        const val AI_PLAYER = 1
    }
}
