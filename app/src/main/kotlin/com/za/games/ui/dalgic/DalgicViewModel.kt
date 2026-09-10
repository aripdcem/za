package com.za.games.ui.dalgic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.za.games.dalgic.DalgicEvent
import com.za.games.dalgic.DalgicHud
import com.za.games.dalgic.DalgicStatus
import com.za.games.dalgic.DalgicWorld
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import kotlin.random.Random

enum class DalgicPhase { MENU, PLAYING, PAUSED, OVER }

/**
 * Simülasyonu süren katman: kare döngüsü geçen süreyi [advance] ile verir,
 * sabit 1/60 s adımlara bölünür; parmak sürüklemesi [drag] ile denizaltının
 * hedefini kaydırır. Koşu bitince sicil yazılır.
 */
class DalgicViewModel(application: Application) : AndroidViewModel(application) {

    private val store = DalgicStore(application)

    private val _phase = MutableStateFlow(DalgicPhase.MENU)
    val phase: StateFlow<DalgicPhase> = _phase.asStateFlow()

    private val _dailyMode = MutableStateFlow(store.lastDaily())
    val dailyMode: StateFlow<Boolean> = _dailyMode.asStateFlow()

    private val _daily = MutableStateFlow(store.daily(todayEpoch()))
    val daily: StateFlow<DalgicDaily?> = _daily.asStateFlow()

    private val _freeBest = MutableStateFlow(store.freeBest())
    val freeBest: StateFlow<Int> = _freeBest.asStateFlow()

    private val _record = MutableStateFlow(false)
    val record: StateFlow<Boolean> = _record.asStateFlow()

    private val _runId = MutableStateFlow(0)
    val runId: StateFlow<Int> = _runId.asStateFlow()

    /** Çizim için doğrudan okunur; yalnızca ana iş parçacığında değişir. */
    var world: DalgicWorld = DalgicWorld(0L)
        private set

    private val _hud = MutableStateFlow(world.hud())
    val hud: StateFlow<DalgicHud> = _hud.asStateFlow()

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
        if (_phase.value != DalgicPhase.MENU) return
        _dailyMode.value = daily
        store.saveLastDaily(daily)
    }

    fun attemptsLeft(): Int = DAILY_ATTEMPTS - (store.daily(todayEpoch())?.attempts ?: 0)

    /** Parmak sürüklemesi: arena birimi cinsinden fark. */
    fun drag(dx: Float, dy: Float) {
        if (_phase.value == DalgicPhase.PLAYING) world.steerBy(dx, dy)
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
        world = DalgicWorld(if (daily) DalgicWorld.dailySeed(today) else Random.nextLong())
        settled = false
        _record.value = false
        accumulator = 0L
        _runId.value += 1
        _hud.value = world.hud()
        _frame.value += 1
        _phase.value = DalgicPhase.PLAYING
    }

    fun restart() {
        settle()
        if (runDaily && attemptsLeft() <= 0) _dailyMode.value = false
        _phase.value = DalgicPhase.MENU
        start()
    }

    fun advance(deltaNanos: Long): List<DalgicEvent> {
        if (_phase.value != DalgicPhase.PLAYING) return emptyList()
        accumulator += deltaNanos.coerceIn(0L, MAX_FRAME_NANOS)
        var steps = 0
        val out = ArrayList<DalgicEvent>()
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
            if (world.status == DalgicStatus.OVER) {
                settle()
                _phase.value = DalgicPhase.OVER
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
        if (_phase.value != DalgicPhase.PLAYING) return
        _phase.value = DalgicPhase.PAUSED
    }

    fun resume() {
        if (_phase.value != DalgicPhase.PAUSED) return
        accumulator = 0L
        _phase.value = DalgicPhase.PLAYING
    }

    fun togglePause() {
        when (_phase.value) {
            DalgicPhase.PLAYING -> pause()
            DalgicPhase.PAUSED -> resume()
            else -> Unit
        }
    }

    fun toMenu() {
        if (_phase.value == DalgicPhase.PLAYING || _phase.value == DalgicPhase.PAUSED) settle()
        _phase.value = DalgicPhase.MENU
        refresh()
    }

    companion object {
        const val DAILY_ATTEMPTS = 3
        private const val STEP_NANOS = 16_666_667L
        private const val MAX_FRAME_NANOS = 100_000_000L
        private const val MAX_STEPS = 4
    }
}
