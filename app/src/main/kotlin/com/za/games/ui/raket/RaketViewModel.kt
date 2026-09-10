package com.za.games.ui.raket

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.za.games.raket.RaketEvent
import com.za.games.raket.RaketHud
import com.za.games.raket.RaketMode
import com.za.games.raket.RaketStatus
import com.za.games.raket.RaketWorld
import com.za.games.raket.Side
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import kotlin.random.Random

enum class RaketPhase { MENU, PLAYING, PAUSED, OVER }

/**
 * Simülasyonu süren katman: ekranın kare döngüsü geçen süreyi [advance] ile
 * verir, burada sabit 1/60 s adımlara bölünür. Raketler parmak sürüklemesiyle
 * ([move]) yer değiştirir. Maç bitince sicil yazılır; duvar koşusu yarıda
 * bırakılsa da o ana kadarki ralli sayılır.
 */
class RaketViewModel(application: Application) : AndroidViewModel(application) {

    private val store = RaketStore(application)

    private val _phase = MutableStateFlow(RaketPhase.MENU)
    val phase: StateFlow<RaketPhase> = _phase.asStateFlow()

    private val _mode = MutableStateFlow(store.lastMode())
    val mode: StateFlow<RaketMode> = _mode.asStateFlow()

    private val _level = MutableStateFlow(store.lastLevel())
    val level: StateFlow<Int> = _level.asStateFlow()

    private val _wallDaily = MutableStateFlow(store.lastWallDaily())
    val wallDaily: StateFlow<Boolean> = _wallDaily.asStateFlow()

    private val _daily = MutableStateFlow(store.daily(todayEpoch()))
    val daily: StateFlow<RaketDaily?> = _daily.asStateFlow()

    private val _records = MutableStateFlow(loadRecords())
    val records: StateFlow<List<RaketRecord>> = _records.asStateFlow()

    private val _wallBest = MutableStateFlow(store.wallBest())
    val wallBest: StateFlow<Int> = _wallBest.asStateFlow()

    private val _runId = MutableStateFlow(0)
    val runId: StateFlow<Int> = _runId.asStateFlow()

    /** Çizim için doğrudan okunur; yalnızca ana iş parçacığında değişir. */
    var world: RaketWorld = RaketWorld(0L, RaketMode.SOLO, 1)
        private set

    private val _hud = MutableStateFlow(world.hud())
    val hud: StateFlow<RaketHud> = _hud.asStateFlow()

    private val _frame = MutableStateFlow(0L)
    val frame: StateFlow<Long> = _frame.asStateFlow()

    /** Koşunun sonucu sicile yazıldı mı (yeni rekor bilgisiyle). */
    private val _record = MutableStateFlow(false)
    val record: StateFlow<Boolean> = _record.asStateFlow()

    private var accumulator = 0L
    private var runMode = RaketMode.SOLO
    private var runLevel = 1
    private var runDaily = false
    private var runDay = 0L
    private var settled = false

    private fun todayEpoch(): Long = LocalDate.now().toEpochDay()

    private fun loadRecords(): List<RaketRecord> = (0..RaketWorld.MAX_LEVEL).map { store.record(it) }

    fun refresh() {
        _daily.value = store.daily(todayEpoch())
        _records.value = loadRecords()
        _wallBest.value = store.wallBest()
    }

    fun setMode(mode: RaketMode) {
        if (_phase.value != RaketPhase.MENU) return
        _mode.value = mode
        saveLast()
    }

    fun setLevel(level: Int) {
        if (_phase.value != RaketPhase.MENU) return
        _level.value = level.coerceIn(0, RaketWorld.MAX_LEVEL)
        saveLast()
    }

    fun setWallDaily(daily: Boolean) {
        if (_phase.value != RaketPhase.MENU) return
        _wallDaily.value = daily
        saveLast()
    }

    private fun saveLast() = store.saveLast(_mode.value, _level.value, _wallDaily.value)

    /** Parmak sürüklemesi: kort genişliği cinsinden yatay fark. */
    fun move(side: Side, dx: Float) {
        if (_phase.value == RaketPhase.PLAYING) world.move(side, dx)
    }

    fun start() {
        val mode = _mode.value
        val today = todayEpoch()
        runMode = mode
        runLevel = _level.value
        runDaily = mode == RaketMode.WALL && _wallDaily.value
        runDay = today
        world = when (mode) {
            RaketMode.SOLO -> RaketWorld(Random.nextLong(), RaketMode.SOLO, runLevel)
            RaketMode.DUO -> RaketWorld(Random.nextLong(), RaketMode.DUO)
            RaketMode.WALL -> RaketWorld(if (runDaily) RaketWorld.dailySeed(today) else Random.nextLong(), RaketMode.WALL)
        }
        settled = false
        _record.value = false
        accumulator = 0L
        _runId.value += 1
        _hud.value = world.hud()
        _frame.value += 1
        _phase.value = RaketPhase.PLAYING
    }

    fun restart() {
        settle()
        _phase.value = RaketPhase.MENU
        start()
    }

    fun advance(deltaNanos: Long): List<RaketEvent> {
        if (_phase.value != RaketPhase.PLAYING) return emptyList()
        accumulator += deltaNanos.coerceIn(0L, MAX_FRAME_NANOS)
        var steps = 0
        val out = ArrayList<RaketEvent>()
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
            if (world.status == RaketStatus.OVER) {
                settle()
                _phase.value = RaketPhase.OVER
            }
        }
        return out
    }

    /** Koşunun sonucunu sicile yazar (bir kez). */
    private fun settle() {
        if (settled) return
        settled = true
        when (runMode) {
            RaketMode.SOLO -> if (world.status == RaketStatus.OVER) store.addResult(runLevel, world.winner == Side.BOTTOM)
            RaketMode.DUO -> Unit
            RaketMode.WALL -> {
                val rally = world.hits
                if (rally > 0) {
                    _record.value = if (runDaily) store.saveDaily(runDay, rally) else store.saveWallBest(rally)
                }
            }
        }
        refresh()
    }

    fun pause() {
        if (_phase.value != RaketPhase.PLAYING) return
        _phase.value = RaketPhase.PAUSED
    }

    fun resume() {
        if (_phase.value != RaketPhase.PAUSED) return
        accumulator = 0L
        _phase.value = RaketPhase.PLAYING
    }

    fun togglePause() {
        when (_phase.value) {
            RaketPhase.PLAYING -> pause()
            RaketPhase.PAUSED -> resume()
            else -> Unit
        }
    }

    fun toMenu() {
        if (_phase.value == RaketPhase.PLAYING || _phase.value == RaketPhase.PAUSED) settle()
        _phase.value = RaketPhase.MENU
        refresh()
    }

    companion object {
        private const val STEP_NANOS = 16_666_667L
        private const val MAX_FRAME_NANOS = 100_000_000L
        private const val MAX_STEPS = 4
    }
}
