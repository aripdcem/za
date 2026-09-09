package com.za.games.ui.viraj

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.za.games.platform.SettingsStore
import com.za.games.viraj.VirajEvent
import com.za.games.viraj.VirajHud
import com.za.games.viraj.VirajStatus
import com.za.games.viraj.VirajWorld
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import kotlin.random.Random

enum class VirajMode { DAILY, FREE }

enum class VirajPhase { MENU, PLAYING, PAUSED, OVER }

/**
 * Simülasyonu süren katman: ekranın kare döngüsü geçen süreyi [advance] ile
 * verir, burada sabit 1/60 s adımlara bölünür. Direksiyon ve fren basılı
 * tutma durumlarıdır; her adımda motora aynen geçer.
 */
class VirajViewModel(application: Application) : AndroidViewModel(application) {

    private val store = VirajStore(application)
    private val settings = SettingsStore(application)

    private val _leftHanded = MutableStateFlow(settings.leftHanded)
    val leftHanded: StateFlow<Boolean> = _leftHanded.asStateFlow()

    private val _phase = MutableStateFlow(VirajPhase.MENU)
    val phase: StateFlow<VirajPhase> = _phase.asStateFlow()

    private val _mode = MutableStateFlow(VirajMode.DAILY)
    val mode: StateFlow<VirajMode> = _mode.asStateFlow()

    private val _daily = MutableStateFlow(store.daily(todayEpoch()))
    val daily: StateFlow<VirajDaily?> = _daily.asStateFlow()

    private val _runId = MutableStateFlow(0)
    val runId: StateFlow<Int> = _runId.asStateFlow()

    /** Çizim için doğrudan okunur; yalnızca ana iş parçacığında değişir. */
    var world: VirajWorld = VirajWorld(VirajWorld.dailySeed(todayEpoch()))
        private set

    private val _hud = MutableStateFlow(world.hud())
    val hud: StateFlow<VirajHud> = _hud.asStateFlow()

    private val _frame = MutableStateFlow(0L)
    val frame: StateFlow<Long> = _frame.asStateFlow()

    private var left = false
    private var right = false
    private var brake = false
    private var accumulator = 0L
    private var runMode = VirajMode.FREE
    private var runDay = 0L

    private fun todayEpoch(): Long = LocalDate.now().toEpochDay()

    fun refreshDaily() {
        _daily.value = store.daily(todayEpoch())
        _leftHanded.value = settings.leftHanded
    }

    fun setLeftHanded(value: Boolean) {
        settings.leftHanded = value
        _leftHanded.value = value
    }

    fun setMode(mode: VirajMode) {
        if (_phase.value == VirajPhase.MENU) _mode.value = mode
    }

    fun attemptsLeft(): Int = DAILY_ATTEMPTS - (store.daily(todayEpoch())?.attempts ?: 0)

    fun pressLeft(pressed: Boolean) {
        left = pressed
        applyInput()
    }

    fun pressRight(pressed: Boolean) {
        right = pressed
        applyInput()
    }

    fun pressBrake(pressed: Boolean) {
        brake = pressed
        applyInput()
    }

    private fun applyInput() {
        world.steer = (if (right) 1 else 0) - (if (left) 1 else 0)
        world.brake = brake
    }

    /** Seçili modda yeni koşu; günlük hak bittiyse başlamaz. */
    fun start() {
        val today = todayEpoch()
        val mode = _mode.value
        if (mode == VirajMode.DAILY) {
            val used = store.daily(today)?.attempts ?: 0
            if (used >= DAILY_ATTEMPTS) {
                refreshDaily()
                return
            }
            store.saveDaily(today, used + 1, store.daily(today)?.best ?: 0L)
            refreshDaily()
            world = VirajWorld(VirajWorld.dailySeed(today))
        } else {
            world = VirajWorld(Random.nextLong())
        }
        runMode = mode
        runDay = today
        accumulator = 0L
        applyInput()
        _runId.value += 1
        _hud.value = world.hud()
        _frame.value += 1
        _phase.value = VirajPhase.PLAYING
    }

    /** Bitiş/duraklatma kartından: hak varsa aynı modda, yoksa serbest koşu. */
    fun restart() {
        settleDaily()
        if (runMode == VirajMode.DAILY && attemptsLeft() <= 0) _mode.value = VirajMode.FREE
        _phase.value = VirajPhase.MENU
        start()
    }

    fun advance(deltaNanos: Long): List<VirajEvent> {
        if (_phase.value != VirajPhase.PLAYING) return emptyList()
        accumulator += deltaNanos.coerceIn(0L, MAX_FRAME_NANOS)
        var steps = 0
        val out = ArrayList<VirajEvent>()
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
            if (world.status == VirajStatus.OVER) {
                _phase.value = VirajPhase.OVER
                settleDaily()
            }
        }
        return out
    }

    private fun settleDaily() {
        if (runMode != VirajMode.DAILY) return
        val current = store.daily(runDay) ?: return
        if (world.score > current.best) store.saveDaily(runDay, current.attempts, world.score)
        refreshDaily()
    }

    fun pause() {
        if (_phase.value != VirajPhase.PLAYING) return
        _phase.value = VirajPhase.PAUSED
    }

    fun resume() {
        if (_phase.value != VirajPhase.PAUSED) return
        accumulator = 0L
        _phase.value = VirajPhase.PLAYING
    }

    fun togglePause() {
        when (_phase.value) {
            VirajPhase.PLAYING -> pause()
            VirajPhase.PAUSED -> resume()
            else -> Unit
        }
    }

    fun toMenu() {
        if (_phase.value == VirajPhase.PLAYING || _phase.value == VirajPhase.PAUSED) settleDaily()
        _phase.value = VirajPhase.MENU
        refreshDaily()
    }

    companion object {
        const val DAILY_ATTEMPTS = 3
        private const val STEP_NANOS = 16_666_667L
        private const val MAX_FRAME_NANOS = 100_000_000L
        private const val MAX_STEPS = 4
    }
}
