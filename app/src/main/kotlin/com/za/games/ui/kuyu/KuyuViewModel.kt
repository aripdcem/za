package com.za.games.ui.kuyu

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.za.games.kuyu.KuyuEvent
import com.za.games.kuyu.KuyuHud
import com.za.games.kuyu.KuyuInput
import com.za.games.kuyu.KuyuStatus
import com.za.games.kuyu.KuyuWorld
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import kotlin.random.Random

enum class KuyuMode { DAILY, FREE }

/** Ekran evresi: kurulum kartı → oyun ⇄ duraklatma → bitiş kartı. */
enum class KuyuPhase { MENU, PLAYING, PAUSED, OVER }

/**
 * Simülasyonu süren katman. Kare döngüsü ekranda ([androidx.compose.runtime.withFrameNanos])
 * koşar ve geçen süreyi [advance] ile verir; burada sabit 1/60 s adımlara
 * bölünür. Ekrandan çıkınca döngü durur, dünya duraklatılmış olarak bekler.
 */
class KuyuViewModel(application: Application) : AndroidViewModel(application) {

    private val store = KuyuStore(application)

    private val _phase = MutableStateFlow(KuyuPhase.MENU)
    val phase: StateFlow<KuyuPhase> = _phase.asStateFlow()

    private val _mode = MutableStateFlow(KuyuMode.DAILY)
    val mode: StateFlow<KuyuMode> = _mode.asStateFlow()

    private val _leftHanded = MutableStateFlow(store.leftHanded)
    val leftHanded: StateFlow<Boolean> = _leftHanded.asStateFlow()

    private val _daily = MutableStateFlow(store.daily(todayEpoch()))
    val daily: StateFlow<KuyuDaily?> = _daily.asStateFlow()

    /** Koşu sayacı: ekran efekt korumaları ve parçacık sıfırlama anahtarı. */
    private val _runId = MutableStateFlow(0)
    val runId: StateFlow<Int> = _runId.asStateFlow()

    /** Çizim için doğrudan okunur; yalnızca ana iş parçacığında değişir. */
    var world: KuyuWorld = KuyuWorld(KuyuWorld.dailySeed(todayEpoch()))
        private set

    private val _hud = MutableStateFlow(world.hud())
    val hud: StateFlow<KuyuHud> = _hud.asStateFlow()

    /** Her simülasyon karesinde artar; tuval bunu okuyarak yeniden çizilir. */
    private val _frame = MutableStateFlow(0L)
    val frame: StateFlow<Long> = _frame.asStateFlow()

    private var left = false
    private var right = false
    private var fire = false
    private var accumulator = 0L
    private var runMode = KuyuMode.FREE
    private var runDay = 0L

    private fun todayEpoch(): Long = LocalDate.now().toEpochDay()

    /** Gün değişmiş olabilir; ekrana girişte çağrılır. */
    fun refreshDaily() {
        _daily.value = store.daily(todayEpoch())
    }

    fun setMode(mode: KuyuMode) {
        if (_phase.value == KuyuPhase.MENU) _mode.value = mode
    }

    fun setLeftHanded(value: Boolean) {
        store.leftHanded = value
        _leftHanded.value = value
    }

    /** Seçili modda yeni koşu; günün koşusu zaten yapıldıysa başlamaz. */
    fun start() {
        val today = todayEpoch()
        val mode = _mode.value
        if (mode == KuyuMode.DAILY && store.daily(today) != null) {
            refreshDaily()
            return
        }
        val seed = if (mode == KuyuMode.DAILY) KuyuWorld.dailySeed(today) else Random.nextLong()
        world = KuyuWorld(seed)
        runMode = mode
        runDay = today
        if (mode == KuyuMode.DAILY) {
            store.saveDaily(today, 0L, 0, done = false)
            refreshDaily()
        }
        left = false
        right = false
        fire = false
        accumulator = 0L
        _runId.value += 1
        _hud.value = world.hud()
        _frame.value += 1
        _phase.value = KuyuPhase.PLAYING
    }

    /** Bitiş/duraklatma kartından: serbest modda aynı, günlükte serbest koşu. */
    fun restart() {
        if (_phase.value == KuyuPhase.PLAYING || _phase.value == KuyuPhase.PAUSED) {
            saveDailyProgress(done = true)
        }
        if (runMode == KuyuMode.DAILY) _mode.value = KuyuMode.FREE
        _phase.value = KuyuPhase.MENU
        start()
    }

    fun pressLeft(pressed: Boolean) {
        left = pressed
    }

    fun pressRight(pressed: Boolean) {
        right = pressed
    }

    fun pressFire(pressed: Boolean) {
        fire = pressed
    }

    /**
     * Kare döngüsünden çağrılır: geçen süre kadar sabit adım koşturur, üretilen
     * olayları döndürür. Uzun takılmalarda en fazla [MAX_STEPS] adım işlenir,
     * kalan süre atılır — oyun "yetişmek" için ışınlanmaz.
     */
    fun advance(deltaNanos: Long): List<KuyuEvent> {
        if (_phase.value != KuyuPhase.PLAYING) return emptyList()
        accumulator += deltaNanos.coerceIn(0L, MAX_FRAME_NANOS)
        var steps = 0
        val out = ArrayList<KuyuEvent>()
        while (accumulator >= STEP_NANOS && steps < MAX_STEPS) {
            out += world.step(KuyuInput(left = left, right = right, fire = fire))
            accumulator -= STEP_NANOS
            steps++
        }
        if (steps == MAX_STEPS) accumulator = 0L
        if (steps > 0) {
            _frame.value += 1
            val hud = world.hud()
            if (hud != _hud.value) _hud.value = hud
            if (world.status == KuyuStatus.OVER) {
                _phase.value = KuyuPhase.OVER
                saveDailyProgress(done = true)
            }
        }
        return out
    }

    private fun saveDailyProgress(done: Boolean) {
        if (runMode != KuyuMode.DAILY) return
        store.saveDaily(runDay, world.score, world.depth, done)
        refreshDaily()
    }

    fun pause() {
        if (_phase.value != KuyuPhase.PLAYING) return
        _phase.value = KuyuPhase.PAUSED
        left = false
        right = false
        fire = false
        saveDailyProgress(done = false)
    }

    fun resume() {
        if (_phase.value != KuyuPhase.PAUSED) return
        accumulator = 0L
        _phase.value = KuyuPhase.PLAYING
    }

    fun togglePause() {
        when (_phase.value) {
            KuyuPhase.PLAYING -> pause()
            KuyuPhase.PAUSED -> resume()
            else -> Unit
        }
    }

    /** Koşuyu bırakıp kurulum kartına döner; günlük koşunun o ana kadarki sonucu kalır. */
    fun toMenu() {
        if (_phase.value == KuyuPhase.PLAYING || _phase.value == KuyuPhase.PAUSED) {
            saveDailyProgress(done = true)
        }
        left = false
        right = false
        fire = false
        _phase.value = KuyuPhase.MENU
        refreshDaily()
    }

    private companion object {
        const val STEP_NANOS = 16_666_667L
        const val MAX_FRAME_NANOS = 100_000_000L
        const val MAX_STEPS = 4
    }
}
