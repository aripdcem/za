package com.za.games.ui.ucurtma

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.za.games.ucurtma.Gadget
import com.za.games.ucurtma.Mission
import com.za.games.ucurtma.Missions
import com.za.games.ucurtma.UcurtmaEvent
import com.za.games.ucurtma.UcurtmaHud
import com.za.games.ucurtma.UcurtmaStatus
import com.za.games.ucurtma.UcurtmaWorld
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import kotlin.random.Random

enum class UcurtmaPhase { MENU, PLAYING, PAUSED, OVER }

/**
 * Simülasyonu süren katman: ekranın kare döngüsü geçen süreyi [advance] ile
 * verir, sabit 1/60 s adımlara bölünür; parmak basılıyken [hold] true.
 * Koşu bitince sicil ve görevler yazılır: tamamlanan görevin yerine dizideki
 * sıradaki gelir, tamamlanan sayısı ekipmanı açar.
 */
class UcurtmaViewModel(application: Application) : AndroidViewModel(application) {

    private val store = UcurtmaStore(application)

    private val _phase = MutableStateFlow(UcurtmaPhase.MENU)
    val phase: StateFlow<UcurtmaPhase> = _phase.asStateFlow()

    private val _dailyMode = MutableStateFlow(store.lastDaily())
    val dailyMode: StateFlow<Boolean> = _dailyMode.asStateFlow()

    private val _daily = MutableStateFlow(store.daily(todayEpoch()))
    val daily: StateFlow<UcurtmaDaily?> = _daily.asStateFlow()

    private val _freeBest = MutableStateFlow(store.freeBest())
    val freeBest: StateFlow<Int> = _freeBest.asStateFlow()

    private val _missions = MutableStateFlow(store.activeMissions().map { Missions.at(it) })
    val missions: StateFlow<List<Mission>> = _missions.asStateFlow()

    private val _completed = MutableStateFlow(store.completedMissions())
    val completed: StateFlow<Int> = _completed.asStateFlow()

    private val _gadget = MutableStateFlow(store.gadget())
    val gadget: StateFlow<Gadget?> = _gadget.asStateFlow()

    /** Son koşuda tamamlanan görevler ve açılan ekipman (bitiş kartı için). */
    private val _lastDone = MutableStateFlow<List<Mission>>(emptyList())
    val lastDone: StateFlow<List<Mission>> = _lastDone.asStateFlow()

    private val _lastUnlocked = MutableStateFlow<List<Gadget>>(emptyList())
    val lastUnlocked: StateFlow<List<Gadget>> = _lastUnlocked.asStateFlow()

    private val _record = MutableStateFlow(false)
    val record: StateFlow<Boolean> = _record.asStateFlow()

    private val _runId = MutableStateFlow(0)
    val runId: StateFlow<Int> = _runId.asStateFlow()

    /** Çizim için doğrudan okunur; yalnızca ana iş parçacığında değişir. */
    var world: UcurtmaWorld = UcurtmaWorld(0L)
        private set

    private val _hud = MutableStateFlow(world.hud())
    val hud: StateFlow<UcurtmaHud> = _hud.asStateFlow()

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
        _missions.value = store.activeMissions().map { Missions.at(it) }
        _completed.value = store.completedMissions()
        _gadget.value = store.gadget()
    }

    fun setDailyMode(daily: Boolean) {
        if (_phase.value != UcurtmaPhase.MENU) return
        _dailyMode.value = daily
        store.saveLastDaily(daily)
    }

    fun unlocked(): List<Gadget> = Missions.unlocked(_completed.value)

    fun setGadget(gadget: Gadget?) {
        if (_phase.value != UcurtmaPhase.MENU) return
        if (gadget != null && gadget !in unlocked()) return
        _gadget.value = gadget
        store.saveGadget(gadget)
    }

    fun attemptsLeft(): Int = DAILY_ATTEMPTS - (store.daily(todayEpoch())?.attempts ?: 0)

    fun hold(pressed: Boolean) {
        if (_phase.value == UcurtmaPhase.PLAYING) world.hold(pressed)
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
        world = UcurtmaWorld(if (daily) UcurtmaWorld.dailySeed(today) else Random.nextLong(), _gadget.value, _missions.value)
        settled = false
        _record.value = false
        _lastDone.value = emptyList()
        _lastUnlocked.value = emptyList()
        accumulator = 0L
        _runId.value += 1
        _hud.value = world.hud()
        _frame.value += 1
        _phase.value = UcurtmaPhase.PLAYING
    }

    /** Bitiş/duraklatma kartından: hak varsa aynı modda, yoksa serbest. */
    fun restart() {
        settle()
        if (runDaily && attemptsLeft() <= 0) _dailyMode.value = false
        _phase.value = UcurtmaPhase.MENU
        start()
    }

    fun advance(deltaNanos: Long): List<UcurtmaEvent> {
        if (_phase.value != UcurtmaPhase.PLAYING) return emptyList()
        accumulator += deltaNanos.coerceIn(0L, MAX_FRAME_NANOS)
        var steps = 0
        val out = ArrayList<UcurtmaEvent>()
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
            if (world.status == UcurtmaStatus.OVER) {
                settle()
                _phase.value = UcurtmaPhase.OVER
            }
        }
        return out
    }

    /** Koşunun sonucunu sicile yazar; görevleri ilerletir (bir kez). */
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
        val hud = world.hud()
        val active = store.activeMissions().toMutableList()
        var next = store.nextMission()
        var completed = store.completedMissions()
        val done = ArrayList<Mission>()
        val before = Missions.unlocked(completed)
        for (i in world.missions.indices) {
            if (i < hud.done.size && hud.done[i] && i < active.size) {
                done += world.missions[i]
                active[i] = next++
                completed++
            }
        }
        if (done.isNotEmpty()) store.saveMissions(active, next, completed)
        _lastDone.value = done
        _lastUnlocked.value = Missions.unlocked(completed).filter { it !in before }
        refresh()
    }

    fun pause() {
        if (_phase.value != UcurtmaPhase.PLAYING) return
        world.hold(false)
        _phase.value = UcurtmaPhase.PAUSED
    }

    fun resume() {
        if (_phase.value != UcurtmaPhase.PAUSED) return
        accumulator = 0L
        _phase.value = UcurtmaPhase.PLAYING
    }

    fun togglePause() {
        when (_phase.value) {
            UcurtmaPhase.PLAYING -> pause()
            UcurtmaPhase.PAUSED -> resume()
            else -> Unit
        }
    }

    fun toMenu() {
        if (_phase.value == UcurtmaPhase.PLAYING || _phase.value == UcurtmaPhase.PAUSED) settle()
        _phase.value = UcurtmaPhase.MENU
        refresh()
    }

    companion object {
        const val DAILY_ATTEMPTS = 3
        private const val STEP_NANOS = 16_666_667L
        private const val MAX_FRAME_NANOS = 100_000_000L
        private const val MAX_STEPS = 4
    }
}
