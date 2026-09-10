package com.za.games.ui.bostan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.za.games.bostan.BostanDifficulty
import com.za.games.bostan.BostanEvent
import com.za.games.bostan.BostanGenerator
import com.za.games.bostan.BostanHud
import com.za.games.bostan.BostanLevel
import com.za.games.bostan.BostanState
import com.za.games.bostan.BostanStatus
import com.za.games.bostan.DefenderKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.floor
import kotlin.random.Random

enum class BostanPhase { MENU, LOADING, PLAYING, PAUSED, OVER }

/** Hücreye dokunmanın sonucu; ekran geri bildirimini buradan verir. */
enum class TapOutcome { DROP, PLACED, REMOVED, NO_SELECTION, NO_WATER, COOLDOWN, OCCUPIED, EMPTY, IGNORED }

/** [tapField] sonucu: ne oldu ve hangi hücrede (damla toplandıysa damlanın hücresi). */
class TapResult(val outcome: TapOutcome, val lane: Int, val row: Int)

/**
 * Simülasyonu süren katman: seviye arka planda üretilir ([BostanPhase.LOADING]),
 * kare döngüsü geçen süreyi [advance] ile verir, sabit 1/60 s adımlara
 * bölünür. Kart seçimi ([select], [selectShovel]) ve hücre dokunuşu
 * ([tapCell]) buradan geçer. Koşu bitince sicil yazılır.
 */
class BostanViewModel @JvmOverloads constructor(
    application: Application,
    private val generate: (Long, BostanDifficulty) -> BostanLevel = BostanGenerator::generate,
) : AndroidViewModel(application) {

    private val store = BostanStore(application)

    private val _phase = MutableStateFlow(BostanPhase.MENU)
    val phase: StateFlow<BostanPhase> = _phase.asStateFlow()

    private val _dailyMode = MutableStateFlow(store.lastDaily())
    val dailyMode: StateFlow<Boolean> = _dailyMode.asStateFlow()

    private val _difficulty = MutableStateFlow(store.lastDifficulty())
    val difficulty: StateFlow<BostanDifficulty> = _difficulty.asStateFlow()

    private val _daily = MutableStateFlow(store.daily(todayEpoch()))
    val daily: StateFlow<BostanDaily?> = _daily.asStateFlow()

    private val _freeBest = MutableStateFlow(store.freeBest())
    val freeBest: StateFlow<List<Int>> = _freeBest.asStateFlow()

    private val _record = MutableStateFlow(false)
    val record: StateFlow<Boolean> = _record.asStateFlow()

    private val _runId = MutableStateFlow(0)
    val runId: StateFlow<Int> = _runId.asStateFlow()

    private val _selected = MutableStateFlow<DefenderKind?>(null)
    val selected: StateFlow<DefenderKind?> = _selected.asStateFlow()

    private val _shovel = MutableStateFlow(false)
    val shovel: StateFlow<Boolean> = _shovel.asStateFlow()

    /** Çizim için doğrudan okunur; yalnızca ana iş parçacığında değişir. */
    var state: BostanState = BostanState(BostanLevel(0L, BostanDifficulty.KOLAY, emptyList(), 1f, 0, 0))
        private set

    private val _hud = MutableStateFlow(state.hud())
    val hud: StateFlow<BostanHud> = _hud.asStateFlow()

    private val _frame = MutableStateFlow(0L)
    val frame: StateFlow<Long> = _frame.asStateFlow()

    private var accumulator = 0L
    private var runDaily = false
    private var runDay = 0L
    private var runDifficulty = BostanDifficulty.KOLAY
    private var settled = false
    private var loadToken = 0

    private fun todayEpoch(): Long = LocalDate.now().toEpochDay()

    fun refresh() {
        _daily.value = store.daily(todayEpoch())
        _freeBest.value = store.freeBest()
    }

    fun setDailyMode(daily: Boolean) {
        if (_phase.value != BostanPhase.MENU) return
        _dailyMode.value = daily
        store.saveLastDaily(daily)
    }

    fun setDifficulty(d: BostanDifficulty) {
        if (_phase.value != BostanPhase.MENU) return
        _difficulty.value = d
        store.saveLastDifficulty(d)
    }

    fun attemptsLeft(): Int = DAILY_ATTEMPTS - (store.daily(todayEpoch())?.attempts ?: 0)

    fun select(kind: DefenderKind) {
        if (_phase.value != BostanPhase.PLAYING) return
        _shovel.value = false
        _selected.value = if (_selected.value == kind) null else kind
    }

    fun selectShovel() {
        if (_phase.value != BostanPhase.PLAYING) return
        _selected.value = null
        _shovel.value = !_shovel.value
    }

    /**
     * Sürekli tarla koordinatında dokunuş: [DROP_REACH] hücre yakınındaki damla
     * her şeyden önce toplanır (kart seçili olsa da, komşu hücreden de), sonra
     * dokunulan hücreye [tapCell] kuralı.
     */
    fun tapField(x: Float, y: Float): TapResult {
        if (_phase.value != BostanPhase.PLAYING) return TapResult(TapOutcome.IGNORED, -1, -1)
        val drop = state.nearestDrop(x, y, DROP_REACH)
        if (drop != null && state.collectDrop(drop.lane, drop.row)) {
            publish()
            return TapResult(TapOutcome.DROP, drop.lane, drop.row)
        }
        val lane = floor(x).toInt()
        val row = floor(y + 0.5f).toInt()
        if (lane !in 0 until BostanState.COLS || row !in 0 until BostanState.ROWS) return TapResult(TapOutcome.IGNORED, lane, row)
        return TapResult(tapCell(lane, row), lane, row)
    }

    /** Hücre dokunuşu: önce damla, sonra kürek, sonra seçili kart. */
    fun tapCell(lane: Int, row: Int): TapOutcome {
        if (_phase.value != BostanPhase.PLAYING) return TapOutcome.IGNORED
        if (state.collectDrop(lane, row)) {
            publish()
            return TapOutcome.DROP
        }
        if (_shovel.value) {
            val removed = state.remove(lane, row)
            if (removed) {
                _shovel.value = false
                publish()
                return TapOutcome.REMOVED
            }
            return TapOutcome.EMPTY
        }
        val kind = _selected.value ?: return TapOutcome.NO_SELECTION
        if (state.defenderAt(lane, row) != null) return TapOutcome.OCCUPIED
        if (!state.affordable(kind)) return TapOutcome.NO_WATER
        if (!state.ready(kind)) return TapOutcome.COOLDOWN
        if (!state.place(kind, lane, row)) return TapOutcome.IGNORED
        _selected.value = null
        publish()
        return TapOutcome.PLACED
    }

    fun start() {
        if (_phase.value == BostanPhase.LOADING) return
        val today = todayEpoch()
        val daily = _dailyMode.value
        if (daily) {
            val used = store.daily(today)?.attempts ?: 0
            if (used >= DAILY_ATTEMPTS) {
                refresh()
                return
            }
            store.saveDaily(today, used + 1, store.daily(today)?.best ?: List(BostanDifficulty.entries.size) { 0 })
        }
        refresh()
        runDaily = daily
        runDay = today
        runDifficulty = _difficulty.value
        val seed = if (daily) BostanState.dailySeed(today) else Random.nextLong()
        val token = ++loadToken
        _phase.value = BostanPhase.LOADING
        viewModelScope.launch {
            val level = withContext(Dispatchers.Default) { generate(seed, runDifficulty) }
            if (token != loadToken || _phase.value != BostanPhase.LOADING) return@launch
            begin(level)
        }
    }

    private fun begin(level: BostanLevel) {
        state = BostanState(level)
        settled = false
        _record.value = false
        _selected.value = null
        _shovel.value = false
        accumulator = 0L
        _runId.value += 1
        _hud.value = state.hud()
        _frame.value += 1
        _phase.value = BostanPhase.PLAYING
    }

    fun restart() {
        settle()
        if (runDaily && attemptsLeft() <= 0) _dailyMode.value = false
        _phase.value = BostanPhase.MENU
        start()
    }

    fun advance(deltaNanos: Long): List<BostanEvent> {
        if (_phase.value != BostanPhase.PLAYING) return emptyList()
        accumulator += deltaNanos.coerceIn(0L, MAX_FRAME_NANOS)
        var steps = 0
        val out = ArrayList<BostanEvent>()
        while (accumulator >= STEP_NANOS && steps < MAX_STEPS) {
            state.step()
            out += state.events
            accumulator -= STEP_NANOS
            steps++
        }
        if (steps == MAX_STEPS) accumulator = 0L
        if (steps > 0) {
            publish()
            if (state.status != BostanStatus.RUNNING) {
                settle()
                _selected.value = null
                _shovel.value = false
                _phase.value = BostanPhase.OVER
            }
        }
        return out
    }

    private fun publish() {
        _frame.value += 1
        val hud = state.hud()
        if (hud != _hud.value) _hud.value = hud
    }

    private fun settle() {
        if (settled) return
        settled = true
        val score = state.score
        if (runDaily) {
            val current = store.daily(runDay)
            if (current != null && score > current.bestOf(runDifficulty)) {
                val best = current.best.toMutableList()
                while (best.size < BostanDifficulty.entries.size) best += 0
                best[runDifficulty.ordinal] = score
                store.saveDaily(runDay, current.attempts, best)
                _record.value = true
            }
        } else {
            _record.value = store.saveFreeBest(runDifficulty, score)
        }
        refresh()
    }

    fun pause() {
        if (_phase.value != BostanPhase.PLAYING) return
        _phase.value = BostanPhase.PAUSED
    }

    fun resume() {
        if (_phase.value != BostanPhase.PAUSED) return
        accumulator = 0L
        _phase.value = BostanPhase.PLAYING
    }

    fun togglePause() {
        when (_phase.value) {
            BostanPhase.PLAYING -> pause()
            BostanPhase.PAUSED -> resume()
            else -> Unit
        }
    }

    fun toMenu() {
        if (_phase.value == BostanPhase.PLAYING || _phase.value == BostanPhase.PAUSED) settle()
        loadToken++
        _phase.value = BostanPhase.MENU
        refresh()
    }

    companion object {
        const val DAILY_ATTEMPTS = 3

        /** Damla toplama toleransı (hücre): dokunuşa bu kadar yakın damla önce toplanır. */
        const val DROP_REACH = 0.75f
        private const val STEP_NANOS = 16_666_667L
        private const val MAX_FRAME_NANOS = 100_000_000L
        private const val MAX_STEPS = 4
    }
}
