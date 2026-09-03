package com.za.games.ui.gecit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.za.games.gecit.GecitEvent
import com.za.games.gecit.GecitHud
import com.za.games.gecit.GecitStatus
import com.za.games.gecit.GecitWorld
import com.za.games.gecit.Move
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import kotlin.random.Random

enum class GecitMode { DAILY, FREE }

enum class GecitPhase { MENU, PLAYING, PAUSED, OVER }

/**
 * Simülasyonu süren katman: ekranın kare döngüsü geçen süreyi [advance] ile
 * verir, burada sabit 1/60 s adımlara bölünür. Hamleler [move] ile sıraya
 * alınır ve bir sonraki adımda motora geçer. Ekrandan çıkınca döngü durur.
 */
class GecitViewModel(application: Application) : AndroidViewModel(application) {

    private val store = GecitStore(application)

    private val _phase = MutableStateFlow(GecitPhase.MENU)
    val phase: StateFlow<GecitPhase> = _phase.asStateFlow()

    private val _mode = MutableStateFlow(GecitMode.DAILY)
    val mode: StateFlow<GecitMode> = _mode.asStateFlow()

    private val _daily = MutableStateFlow(store.daily(todayEpoch()))
    val daily: StateFlow<GecitDaily?> = _daily.asStateFlow()

    private val _runId = MutableStateFlow(0)
    val runId: StateFlow<Int> = _runId.asStateFlow()

    /** Çizim için doğrudan okunur; yalnızca ana iş parçacığında değişir. */
    var world: GecitWorld = GecitWorld(GecitWorld.dailySeed(todayEpoch()))
        private set

    private val _hud = MutableStateFlow(world.hud())
    val hud: StateFlow<GecitHud> = _hud.asStateFlow()

    private val _frame = MutableStateFlow(0L)
    val frame: StateFlow<Long> = _frame.asStateFlow()

    private var queued: Move? = null
    private var accumulator = 0L
    private var runMode = GecitMode.FREE
    private var runDay = 0L

    private fun todayEpoch(): Long = LocalDate.now().toEpochDay()

    fun refreshDaily() {
        _daily.value = store.daily(todayEpoch())
    }

    fun setMode(mode: GecitMode) {
        if (_phase.value == GecitPhase.MENU) _mode.value = mode
    }

    /** Günün kalan deneme hakkı. */
    fun attemptsLeft(): Int = DAILY_ATTEMPTS - (store.daily(todayEpoch())?.attempts ?: 0)

    /** Seçili modda yeni koşu; günlük hak bittiyse başlamaz. */
    fun start() {
        val today = todayEpoch()
        val mode = _mode.value
        if (mode == GecitMode.DAILY) {
            val used = store.daily(today)?.attempts ?: 0
            if (used >= DAILY_ATTEMPTS) {
                refreshDaily()
                return
            }
            store.saveDaily(today, used + 1, store.daily(today)?.best ?: 0L)
            refreshDaily()
            world = GecitWorld(GecitWorld.dailySeed(today))
        } else {
            world = GecitWorld(Random.nextLong())
        }
        runMode = mode
        runDay = today
        queued = null
        accumulator = 0L
        _runId.value += 1
        _hud.value = world.hud()
        _frame.value += 1
        _phase.value = GecitPhase.PLAYING
    }

    /** Bitiş/duraklatma kartından: hak varsa aynı modda, yoksa serbest koşu. */
    fun restart() {
        settleDaily()
        if (runMode == GecitMode.DAILY && attemptsLeft() <= 0) _mode.value = GecitMode.FREE
        _phase.value = GecitPhase.MENU
        start()
    }

    fun move(move: Move) {
        if (_phase.value == GecitPhase.PLAYING) queued = move
    }

    fun advance(deltaNanos: Long): List<GecitEvent> {
        if (_phase.value != GecitPhase.PLAYING) return emptyList()
        accumulator += deltaNanos.coerceIn(0L, MAX_FRAME_NANOS)
        var steps = 0
        val out = ArrayList<GecitEvent>()
        while (accumulator >= STEP_NANOS && steps < MAX_STEPS) {
            val move = queued
            queued = null
            out += world.step(move)
            accumulator -= STEP_NANOS
            steps++
        }
        if (steps == MAX_STEPS) accumulator = 0L
        if (steps > 0) {
            _frame.value += 1
            val hud = world.hud()
            if (hud != _hud.value) _hud.value = hud
            if (world.status == GecitStatus.OVER) {
                _phase.value = GecitPhase.OVER
                settleDaily()
            }
        }
        return out
    }

    /** Günlük koşunun skorunu günün en iyisiyle karşılaştırıp kaydeder. */
    private fun settleDaily() {
        if (runMode != GecitMode.DAILY) return
        val current = store.daily(runDay) ?: return
        if (world.score > current.best) store.saveDaily(runDay, current.attempts, world.score)
        refreshDaily()
    }

    fun pause() {
        if (_phase.value != GecitPhase.PLAYING) return
        _phase.value = GecitPhase.PAUSED
        queued = null
    }

    fun resume() {
        if (_phase.value != GecitPhase.PAUSED) return
        accumulator = 0L
        _phase.value = GecitPhase.PLAYING
    }

    fun togglePause() {
        when (_phase.value) {
            GecitPhase.PLAYING -> pause()
            GecitPhase.PAUSED -> resume()
            else -> Unit
        }
    }

    /** Koşuyu bırakıp kurulum kartına döner. */
    fun toMenu() {
        if (_phase.value == GecitPhase.PLAYING || _phase.value == GecitPhase.PAUSED) settleDaily()
        queued = null
        _phase.value = GecitPhase.MENU
        refreshDaily()
    }

    companion object {
        const val DAILY_ATTEMPTS = 3
        private const val STEP_NANOS = 16_666_667L
        private const val MAX_FRAME_NANOS = 100_000_000L
        private const val MAX_STEPS = 4
    }
}
