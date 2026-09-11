package com.za.games.ui.cici

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.za.games.cici.CiciEvent
import com.za.games.cici.CiciHud
import com.za.games.cici.CiciStatus
import com.za.games.cici.CiciWorld
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import kotlin.random.Random

enum class CiciPhase { MENU, PLAYING, PAUSED, OVER }

/**
 * Simülasyonu süren katman: kare döngüsü geçen süreyi [advance] ile verir,
 * sabit 1/60 s adımlara bölünür; parmak sürüklemesi [drag] ile Cici'nin
 * hedefini kaydırır. Koşu bitince sicil yazılır.
 */
class CiciViewModel(application: Application) : AndroidViewModel(application) {

    private val store = CiciStore(application)

    private val _phase = MutableStateFlow(CiciPhase.MENU)
    val phase: StateFlow<CiciPhase> = _phase.asStateFlow()

    private val _dailyMode = MutableStateFlow(store.lastDaily())
    val dailyMode: StateFlow<Boolean> = _dailyMode.asStateFlow()

    private val _daily = MutableStateFlow(store.daily(todayEpoch()))
    val daily: StateFlow<CiciDaily?> = _daily.asStateFlow()

    private val _freeBest = MutableStateFlow(store.freeBest())
    val freeBest: StateFlow<Int> = _freeBest.asStateFlow()

    private val _record = MutableStateFlow(false)
    val record: StateFlow<Boolean> = _record.asStateFlow()

    private val _runId = MutableStateFlow(0)
    val runId: StateFlow<Int> = _runId.asStateFlow()

    /** Çizim için doğrudan okunur; yalnızca ana iş parçacığında değişir. */
    var world: CiciWorld = CiciWorld(0L)
        private set

    private val _hud = MutableStateFlow(world.hud())
    val hud: StateFlow<CiciHud> = _hud.asStateFlow()

    private val _frame = MutableStateFlow(0L)
    val frame: StateFlow<Long> = _frame.asStateFlow()

    private var accumulator = 0L
    private var runDaily = false
    private var runDay = 0L
    private var settled = false

    private fun todayEpoch(): Long = LocalDate.now().toEpochDay()

    fun refresh() {
        _daily.value = store.daily(todayEpoch())
        _freeBest.value = store.freeBest()
    }

    fun setDailyMode(daily: Boolean) {
        if (_phase.value != CiciPhase.MENU) return
        _dailyMode.value = daily
        store.saveLastDaily(daily)
    }

    fun attemptsLeft(): Int = DAILY_ATTEMPTS - (store.daily(todayEpoch())?.attempts ?: 0)

    /** Parmak sürüklemesi: arena birimi cinsinden fark. */
    fun drag(dx: Float, dy: Float) {
        if (_phase.value == CiciPhase.PLAYING) world.steerBy(dx, dy)
    }

    fun start() {
        val today = todayEpoch()
        val daily = _dailyMode.value
        if (daily) {
            val used = store.daily(today)?.attempts ?: 0
            if (used >= DAILY_ATTEMPTS) {
                refresh()
                return
            }
            store.saveDaily(today, used + 1, store.daily(today)?.best ?: 0)
        }
        refresh()
        runDaily = daily
        runDay = today
        world = CiciWorld(if (daily) CiciWorld.dailySeed(today) else Random.nextLong())
        settled = false
        _record.value = false
        accumulator = 0L
        _runId.value += 1
        _hud.value = world.hud()
        _frame.value += 1
        _phase.value = CiciPhase.PLAYING
    }

    fun restart() {
        settle()
        if (runDaily && attemptsLeft() <= 0) _dailyMode.value = false
        _phase.value = CiciPhase.MENU
        start()
    }

    fun advance(deltaNanos: Long): List<CiciEvent> {
        if (_phase.value != CiciPhase.PLAYING) return emptyList()
        accumulator += deltaNanos.coerceIn(0L, MAX_FRAME_NANOS)
        var steps = 0
        val out = ArrayList<CiciEvent>()
        while (accumulator >= STEP_NANOS && steps < MAX_STEPS) {
            out += world.step()
            accumulator -= STEP_NANOS
            steps++
        }
        if (steps == MAX_STEPS) accumulator = 0L
        if (steps > 0) {
            _frame.value += 1
            val hud = world.hud()
            if (hud != _hud.value) _hud.value = hud
            if (world.status == CiciStatus.OVER) {
                settle()
                _phase.value = CiciPhase.OVER
            }
        }
        return out
    }

    private fun settle() {
        if (settled) return
        settled = true
        val score = world.score
        if (runDaily) {
            val current = store.daily(runDay)
            if (current != null && score > current.best) {
                store.saveDaily(runDay, current.attempts, score)
                _record.value = true
            }
        } else {
            _record.value = store.saveFreeBest(score)
        }
        refresh()
    }

    fun pause() {
        if (_phase.value != CiciPhase.PLAYING) return
        _phase.value = CiciPhase.PAUSED
    }

    fun resume() {
        if (_phase.value != CiciPhase.PAUSED) return
        accumulator = 0L
        _phase.value = CiciPhase.PLAYING
    }

    fun togglePause() {
        when (_phase.value) {
            CiciPhase.PLAYING -> pause()
            CiciPhase.PAUSED -> resume()
            else -> Unit
        }
    }

    fun toMenu() {
        if (_phase.value == CiciPhase.PLAYING || _phase.value == CiciPhase.PAUSED) settle()
        _phase.value = CiciPhase.MENU
        refresh()
    }

    companion object {
        const val DAILY_ATTEMPTS = 3
        private const val STEP_NANOS = 16_666_667L
        private const val MAX_FRAME_NANOS = 100_000_000L
        private const val MAX_STEPS = 4
    }
}
