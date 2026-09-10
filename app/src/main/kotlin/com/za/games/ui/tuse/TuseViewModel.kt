package com.za.games.ui.tuse

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import com.za.games.tuse.Song
import com.za.games.tuse.Songs
import com.za.games.tuse.TuseEvent
import com.za.games.tuse.TuseHud
import com.za.games.tuse.TuseMode
import com.za.games.tuse.TuseStatus
import com.za.games.tuse.TuseWorld
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import kotlin.random.Random

/** Menüdeki seçim: Klasik, Sonsuz ya da Günlük (günün parçasıyla Klasik). */
enum class TuseMenuMode { CLASSIC, ARCADE, DAILY }

enum class TusePhase { MENU, PLAYING, OVER }

/**
 * Simülasyonu süren katman: dokunuşlar [tap] ile hemen işlenir (nota o anda
 * çalınsın diye olaylar çağırana döner), kare döngüsü [advance] ile sabit
 * 1/60 s adımlara bölünür (kayma ve Sonsuz akışı). Koşu bitince sicil yazılır.
 */
class TuseViewModel(application: Application) : AndroidViewModel(application) {

    private val store = TuseStore(application)

    private val _phase = MutableStateFlow(TusePhase.MENU)
    val phase: StateFlow<TusePhase> = _phase.asStateFlow()

    private val _menu = MutableStateFlow(store.lastMenu())
    val menu: StateFlow<TuseMenuMode> = _menu.asStateFlow()

    private val _songId = MutableStateFlow(Songs.byId(store.lastSong()).id)
    val songId: StateFlow<String> = _songId.asStateFlow()

    private val _daily = MutableStateFlow(store.daily(todayEpoch()))
    val daily: StateFlow<TuseDaily?> = _daily.asStateFlow()

    private val _classicBest = MutableStateFlow(0L)
    val classicBest: StateFlow<Long> = _classicBest.asStateFlow()

    private val _arcadeBest = MutableStateFlow(0)
    val arcadeBest: StateFlow<Int> = _arcadeBest.asStateFlow()

    private val _runId = MutableStateFlow(0)
    val runId: StateFlow<Int> = _runId.asStateFlow()

    /** Çizim için doğrudan okunur; yalnızca ana iş parçacığında değişir. */
    var world: TuseWorld = TuseWorld(0L, TuseMode.CLASSIC, Songs.ODE)
        private set

    private val _hud = MutableStateFlow(world.hud(0L))
    val hud: StateFlow<TuseHud> = _hud.asStateFlow()

    private val _frame = MutableStateFlow(0L)
    val frame: StateFlow<Long> = _frame.asStateFlow()

    /** Biten koşu yeni rekor mu. */
    private val _record = MutableStateFlow(false)
    val record: StateFlow<Boolean> = _record.asStateFlow()

    private var accumulator = 0L
    private var settled = false

    /** Süren ya da son biten koşunun seçimi. */
    var runMenu: TuseMenuMode = TuseMenuMode.CLASSIC
        private set
    var runSong: Song = Songs.ODE
        private set
    private var runDay = 0L

    init {
        refresh()
    }

    private fun todayEpoch(): Long = LocalDate.now().toEpochDay()

    private fun now(): Long = SystemClock.uptimeMillis()

    /** Menüde başlatılacak parça: Günlük'te günün parçası. */
    fun songForMenu(): Song = if (_menu.value == TuseMenuMode.DAILY) Songs.ofDay(todayEpoch()) else Songs.byId(_songId.value)

    fun refresh() {
        _daily.value = store.daily(todayEpoch())
        val song = songForMenu()
        _classicBest.value = store.classicBest(song.id)
        _arcadeBest.value = store.arcadeBest(song.id)
    }

    fun setMenu(menu: TuseMenuMode) {
        if (_phase.value != TusePhase.MENU) return
        _menu.value = menu
        store.saveLast(menu, _songId.value)
        refresh()
    }

    /** Parça seçici: [delta] +1 sonraki, −1 önceki. */
    fun shiftSong(delta: Int) {
        if (_phase.value != TusePhase.MENU) return
        val all = Songs.ALL
        val i = all.indexOfFirst { it.id == _songId.value }.coerceAtLeast(0)
        _songId.value = all[((i + delta) % all.size + all.size) % all.size].id
        store.saveLast(_menu.value, _songId.value)
        refresh()
    }

    fun attemptsLeft(): Int = DAILY_ATTEMPTS - (store.daily(todayEpoch())?.attempts ?: 0)

    fun start() {
        val menu = _menu.value
        val today = todayEpoch()
        val song: Song
        val seed: Long
        if (menu == TuseMenuMode.DAILY) {
            val current = store.daily(today)
            val used = current?.attempts ?: 0
            if (used >= DAILY_ATTEMPTS) {
                refresh()
                return
            }
            store.saveDaily(today, used + 1, current?.bestMs ?: 0L)
            song = Songs.ofDay(today)
            seed = TuseWorld.dailySeed(today)
        } else {
            song = Songs.byId(_songId.value)
            seed = Random.nextLong()
        }
        runMenu = menu
        runSong = song
        runDay = today
        world = TuseWorld(seed, if (menu == TuseMenuMode.ARCADE) TuseMode.ARCADE else TuseMode.CLASSIC, song)
        settled = false
        _record.value = false
        accumulator = 0L
        _runId.value += 1
        _hud.value = world.hud(now())
        _frame.value += 1
        _phase.value = TusePhase.PLAYING
        refresh()
    }

    /** Bitiş kartından: Günlük'te hak varsa aynı modda, yoksa Klasik. */
    fun restart() {
        if (runMenu == TuseMenuMode.DAILY && attemptsLeft() <= 0) _menu.value = TuseMenuMode.CLASSIC
        _phase.value = TusePhase.MENU
        start()
    }

    /** Şeride dokunuş; olaylar hemen döner (nota, titreşim). */
    fun tap(lane: Int): List<TuseEvent> {
        if (_phase.value != TusePhase.PLAYING) return emptyList()
        val ev = world.tap(lane, now())
        _hud.value = world.hud(now())
        _frame.value += 1
        if (world.isComplete) finish()
        return ev
    }

    fun advance(deltaNanos: Long): List<TuseEvent> {
        if (_phase.value != TusePhase.PLAYING) return emptyList()
        accumulator += deltaNanos.coerceIn(0L, MAX_FRAME_NANOS)
        var steps = 0
        val out = ArrayList<TuseEvent>()
        while (accumulator >= STEP_NANOS && steps < MAX_STEPS) {
            out += world.step()
            accumulator -= STEP_NANOS
            steps++
        }
        if (steps == MAX_STEPS) accumulator = 0L
        _frame.value += 1
        val hud = world.hud(now())
        if (hud != _hud.value) _hud.value = hud
        if (world.isComplete) finish()
        return out
    }

    /** Arka plana alınınca: süren koşu yarıda kalır ve biter. */
    fun abandon() {
        if (_phase.value != TusePhase.PLAYING) return
        world.abandon(now())
        _hud.value = world.hud(now())
        finish()
    }

    private fun finish() {
        if (!settled) {
            settled = true
            val song = runSong
            when (runMenu) {
                TuseMenuMode.CLASSIC -> if (world.status == TuseStatus.DONE) {
                    _record.value = store.saveClassicBest(song.id, world.elapsedMs(now()))
                }
                TuseMenuMode.DAILY -> if (world.status == TuseStatus.DONE) {
                    val current = store.daily(runDay)
                    val ms = world.elapsedMs(now())
                    val best = current?.bestMs ?: 0L
                    if (best == 0L || ms < best) {
                        store.saveDaily(runDay, current?.attempts ?: 1, ms)
                        _record.value = true
                    }
                }
                TuseMenuMode.ARCADE -> _record.value = store.saveArcadeBest(song.id, world.tapped)
            }
            refresh()
        }
        _phase.value = TusePhase.OVER
    }

    fun toMenu() {
        if (_phase.value == TusePhase.PLAYING) abandon()
        _phase.value = TusePhase.MENU
        refresh()
    }

    companion object {
        const val DAILY_ATTEMPTS = 3
        private const val STEP_NANOS = 16_666_667L
        private const val MAX_FRAME_NANOS = 100_000_000L
        private const val MAX_STEPS = 4
    }
}
