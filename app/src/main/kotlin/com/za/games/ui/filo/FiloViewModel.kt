package com.za.games.ui.filo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.za.games.filo.FiloEvent
import com.za.games.filo.FiloHud
import com.za.games.filo.FiloStatus
import com.za.games.filo.FiloWorld
import com.za.games.platform.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import kotlin.random.Random

enum class FiloMode { DAILY, FREE }

enum class FiloPhase { MENU, PLAYING, PAUSED, OVER }

/**
 * Simülasyonu süren katman: ekranın kare döngüsü geçen süreyi [advance] ile
 * verir, burada sabit 1/60 s adımlara bölünür. Gemi parmak sürüklemesiyle
 * ([drag]) yer değiştirir; bomba tek dokunuştur.
 */
class FiloViewModel(application: Application) : AndroidViewModel(application) {

    private val store = FiloStore(application)
    private val settings = SettingsStore(application)

    private val _leftHanded = MutableStateFlow(settings.leftHanded)
    val leftHanded: StateFlow<Boolean> = _leftHanded.asStateFlow()

    private val _phase = MutableStateFlow(FiloPhase.MENU)
    val phase: StateFlow<FiloPhase> = _phase.asStateFlow()

    private val _mode = MutableStateFlow(FiloMode.DAILY)
    val mode: StateFlow<FiloMode> = _mode.asStateFlow()

    private val _daily = MutableStateFlow(store.daily(todayEpoch()))
    val daily: StateFlow<FiloDaily?> = _daily.asStateFlow()

    private val _runId = MutableStateFlow(0)
    val runId: StateFlow<Int> = _runId.asStateFlow()

    /** Çizim için doğrudan okunur; yalnızca ana iş parçacığında değişir. */
    var world: FiloWorld = FiloWorld(FiloWorld.dailySeed(todayEpoch()))
        private set

    private val _hud = MutableStateFlow(world.hud())
    val hud: StateFlow<FiloHud> = _hud.asStateFlow()

    private val _frame = MutableStateFlow(0L)
    val frame: StateFlow<Long> = _frame.asStateFlow()

    private var accumulator = 0L
    private var runMode = FiloMode.FREE
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

    fun setMode(mode: FiloMode) {
        if (_phase.value == FiloPhase.MENU) _mode.value = mode
    }

    fun attemptsLeft(): Int = DAILY_ATTEMPTS - (store.daily(todayEpoch())?.attempts ?: 0)

    /** Parmak sürüklemesi: oyun alanı birimi cinsinden yatay ve dikey fark ([dy] pozitifse aşağı). */
    fun drag(dx: Float, dy: Float = 0f) {
        if (_phase.value == FiloPhase.PLAYING) world.steerBy(dx, dy)
    }

    /** Bomba; hak yoksa ya da oyun sürmüyorsa etkisiz. */
    fun bomb(): Boolean {
        if (_phase.value != FiloPhase.PLAYING) return false
        val ok = world.bomb()
        if (ok) _hud.value = world.hud()
        return ok
    }

    /** Seçili modda yeni koşu; günlük hak bittiyse başlamaz. */
    fun start() {
        val today = todayEpoch()
        val mode = _mode.value
        if (mode == FiloMode.DAILY) {
            val used = store.daily(today)?.attempts ?: 0
            if (used >= DAILY_ATTEMPTS) {
                refreshDaily()
                return
            }
            store.saveDaily(today, used + 1, store.daily(today)?.best ?: 0L)
            refreshDaily()
            world = FiloWorld(FiloWorld.dailySeed(today))
        } else {
            world = FiloWorld(Random.nextLong())
        }
        runMode = mode
        runDay = today
        accumulator = 0L
        _runId.value += 1
        _hud.value = world.hud()
        _frame.value += 1
        _phase.value = FiloPhase.PLAYING
    }

    /** Bitiş/duraklatma kartından: hak varsa aynı modda, yoksa serbest koşu. */
    fun restart() {
        settleDaily()
        if (runMode == FiloMode.DAILY && attemptsLeft() <= 0) _mode.value = FiloMode.FREE
        _phase.value = FiloPhase.MENU
        start()
    }

    fun advance(deltaNanos: Long): List<FiloEvent> {
        if (_phase.value != FiloPhase.PLAYING) return emptyList()
        accumulator += deltaNanos.coerceIn(0L, MAX_FRAME_NANOS)
        var steps = 0
        val out = ArrayList<FiloEvent>()
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
            if (world.status == FiloStatus.OVER) {
                _phase.value = FiloPhase.OVER
                settleDaily()
            }
        }
        return out
    }

    private fun settleDaily() {
        if (runMode != FiloMode.DAILY) return
        val current = store.daily(runDay) ?: return
        if (world.score > current.best) store.saveDaily(runDay, current.attempts, world.score)
        refreshDaily()
    }

    fun pause() {
        if (_phase.value != FiloPhase.PLAYING) return
        _phase.value = FiloPhase.PAUSED
    }

    fun resume() {
        if (_phase.value != FiloPhase.PAUSED) return
        accumulator = 0L
        _phase.value = FiloPhase.PLAYING
    }

    fun togglePause() {
        when (_phase.value) {
            FiloPhase.PLAYING -> pause()
            FiloPhase.PAUSED -> resume()
            else -> Unit
        }
    }

    fun toMenu() {
        if (_phase.value == FiloPhase.PLAYING || _phase.value == FiloPhase.PAUSED) settleDaily()
        _phase.value = FiloPhase.MENU
        refreshDaily()
    }

    companion object {
        const val DAILY_ATTEMPTS = 3
        private const val STEP_NANOS = 16_666_667L
        private const val MAX_FRAME_NANOS = 100_000_000L
        private const val MAX_STEPS = 4
    }
}
