package com.za.games.ui.balkon

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.za.games.balkon.BalkonEvent
import com.za.games.balkon.BalkonHud
import com.za.games.balkon.BalkonStatus
import com.za.games.balkon.BalkonWorld
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

enum class BalkonPhase { MENU, PLAYING, PAUSED, OVER }

/**
 * Simülasyonu süren katman: ekranın kare döngüsü geçen süreyi [advance] ile
 * verir, burada sabit 1/60 s adımlara bölünür. Atışlar dokunuşla anında
 * motora geçer. [megaArmed] açıkken (ve şarj varken) sıradaki atış megadır.
 */
class BalkonViewModel(application: Application) : AndroidViewModel(application) {

    private val store = BalkonStore(application)

    private val _theme = MutableStateFlow(store.theme)
    val theme: StateFlow<BalkonTheme> = _theme.asStateFlow()

    private val _phase = MutableStateFlow(BalkonPhase.MENU)
    val phase: StateFlow<BalkonPhase> = _phase.asStateFlow()

    private val _runId = MutableStateFlow(0)
    val runId: StateFlow<Int> = _runId.asStateFlow()

    private val _megaArmed = MutableStateFlow(false)
    val megaArmed: StateFlow<Boolean> = _megaArmed.asStateFlow()

    private val _bestLevel = MutableStateFlow(store.bestLevel)
    val bestLevel: StateFlow<Int> = _bestLevel.asStateFlow()

    /** Çizim için doğrudan okunur; yalnızca ana iş parçacığında değişir. */
    var world: BalkonWorld = BalkonWorld(Random.nextLong())
        private set

    private val _hud = MutableStateFlow(world.hud())
    val hud: StateFlow<BalkonHud> = _hud.asStateFlow()

    private val _frame = MutableStateFlow(0L)
    val frame: StateFlow<Long> = _frame.asStateFlow()

    private var accumulator = 0L

    /** Bu koşunun teması (koşu boyunca sabit). */
    var runTheme: BalkonTheme = store.theme
        private set

    fun setTheme(theme: BalkonTheme) {
        store.theme = theme
        _theme.value = theme
    }

    fun start() {
        world = BalkonWorld(Random.nextLong())
        runTheme = _theme.value
        accumulator = 0L
        _megaArmed.value = false
        _runId.value += 1
        _hud.value = world.hud()
        _frame.value += 1
        _phase.value = BalkonPhase.PLAYING
    }

    fun restart() {
        _phase.value = BalkonPhase.MENU
        start()
    }

    /** Nişan alınan noktaya atış; [megaArmed] açıksa ve şarj varsa mega. */
    fun throwAt(x: Float, y: Float) {
        if (_phase.value != BalkonPhase.PLAYING) return
        val mega = _megaArmed.value && world.charges > 0
        if (world.throwAt(x, y, mega)) {
            if (mega) _megaArmed.value = false
            _frame.value += 1
        }
    }

    /** Uzun basış: şarj varsa doğrudan mega atış. */
    fun throwMegaAt(x: Float, y: Float) {
        if (_phase.value != BalkonPhase.PLAYING) return
        if (world.charges <= 0) {
            throwAt(x, y)
            return
        }
        if (world.throwAt(x, y, mega = true)) {
            _megaArmed.value = false
            _frame.value += 1
        }
    }

    fun toggleMega() {
        if (_phase.value != BalkonPhase.PLAYING) return
        _megaArmed.value = !_megaArmed.value && world.charges > 0
    }

    fun advance(deltaNanos: Long): List<BalkonEvent> {
        if (_phase.value != BalkonPhase.PLAYING) return emptyList()
        accumulator += deltaNanos.coerceIn(0L, MAX_FRAME_NANOS)
        var steps = 0
        val out = ArrayList<BalkonEvent>()
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
            if (world.charges <= 0 && _megaArmed.value) _megaArmed.value = false
            if (world.status == BalkonStatus.OVER) {
                _phase.value = BalkonPhase.OVER
                if (world.level > store.bestLevel) {
                    store.bestLevel = world.level
                    _bestLevel.value = world.level
                }
            }
        }
        return out
    }

    fun pause() {
        if (_phase.value != BalkonPhase.PLAYING) return
        _phase.value = BalkonPhase.PAUSED
    }

    fun resume() {
        if (_phase.value != BalkonPhase.PAUSED) return
        accumulator = 0L
        _phase.value = BalkonPhase.PLAYING
    }

    fun togglePause() {
        when (_phase.value) {
            BalkonPhase.PLAYING -> pause()
            BalkonPhase.PAUSED -> resume()
            else -> Unit
        }
    }

    /** Koşuyu bırakıp tema kartına döner. */
    fun toMenu() {
        _phase.value = BalkonPhase.MENU
    }

    companion object {
        private const val STEP_NANOS = 16_666_667L
        private const val MAX_FRAME_NANOS = 100_000_000L
        private const val MAX_STEPS = 4
    }
}
