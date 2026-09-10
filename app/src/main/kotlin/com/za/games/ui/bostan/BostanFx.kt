package com.za.games.ui.bostan

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.za.games.bostan.BostanEvent
import com.za.games.bostan.BostanState
import com.za.games.bostan.EnemyKind
import com.za.games.platform.Sfx
import com.za.games.platform.SoundPlayer
import kotlin.random.Random

/**
 * Görsel ve işitsel geri bildirim: tüy/kürk parçacıkları, uçan yazılar, can
 * kaybında sarsıntı ve parlama, dalga duyuruları; olaylara bağlı sesler.
 * Koordinatlar tarla birimi: x şerit (0..5), y satır (−1..7).
 */
class BostanFx {

    class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float, val color: Color, val size: Float, val maxLife: Float) {
        var life = maxLife
    }
    class Floating(val text: String, var x: Float, var y: Float, val color: Color, val big: Boolean, val maxLife: Float) {
        var life = maxLife
    }

    private val rng = Random(17)
    val particles = ArrayList<Particle>()
    val texts = ArrayList<Floating>()
    var shakeX = 0f
        private set
    var shakeY = 0f
        private set
    var flash = 0f
        private set
    var flashColor = Color.White
        private set
    private var shakeTimer = 0f

    /** Dile bağlı metinler ekrandan verilir. */
    var waveLabel: (Int) -> String = { "$it" }
    var bigWaveLabel = ""
    var lifeLabel = ""
    var wonLabel = ""
    var lostLabel = ""
    var noWaterLabel = ""
    var cooldownLabel = ""
    var occupiedLabel = ""

    val isBusy: Boolean get() = particles.isNotEmpty() || texts.isNotEmpty() || shakeTimer > 0f || flash > 0f

    fun reset() {
        particles.clear()
        texts.clear()
        shakeTimer = 0f
        shakeX = 0f
        shakeY = 0f
        flash = 0f
    }

    fun onEvent(event: BostanEvent, sound: SoundPlayer?, haptics: HapticFeedback) {
        when (event) {
            is BostanEvent.EnemyHit -> burst(event.lane + 0.5f, event.y, Color(0xFF7DD3FC), 2, 0.25f, 0.6f)
            is BostanEvent.EnemyDown -> {
                val big = event.kind == EnemyKind.DOMUZ || event.kind == EnemyKind.AYI
                if (big) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                sound?.play(Sfx.SPLAT, volume = if (big) 0.9f else 0.6f, rate = 1.3f - 0.15f * event.kind.ordinal)
                burst(event.lane + 0.5f, event.y, furColor(event.kind), if (big) 12 else 7, 0.6f, 1.4f)
                texts += Floating("+${event.points}", event.lane + 0.5f, event.y - 0.2f, Color(0xFFFFFFFF), big = big, maxLife = 0.9f)
            }
            is BostanEvent.DefenderDown -> {
                sound?.play(Sfx.DROP, volume = 0.6f, rate = 0.7f)
                burst(event.lane + 0.5f, event.row.toFloat(), Color(0xFFA16207), 8, 0.5f, 1.2f)
            }
            is BostanEvent.TrapBlast -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                sound?.play(Sfx.STOMP, volume = 1f, rate = 0.8f)
                burst(event.lane + 0.5f, event.row.toFloat(), Color(0xFFFB923C), 16, 0.6f, 2.2f)
                shakeTimer = 0.2f
            }
            is BostanEvent.LifeLost -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                sound?.play(Sfx.HORN, volume = 0.9f, rate = 0.8f)
                shakeTimer = 0.4f
                flash = 1f
                flashColor = Color(0xFFEF4444)
                texts += Floating(lifeLabel, event.lane + 0.5f, BostanState.HUT_Y - 0.6f, Color(0xFFFCA5A5), big = true, maxLife = 1.6f)
            }
            is BostanEvent.WaveStart -> {
                sound?.play(if (event.big) Sfx.HORN else Sfx.WHOOSH, volume = 0.7f, rate = if (event.big) 1.1f else 1f)
                texts += Floating(if (event.big) bigWaveLabel else waveLabel(event.index + 1), BostanState.COLS / 2f, 1.2f, if (event.big) Color(0xFFFCA5A5) else Color(0xFFFDE68A), big = true, maxLife = 1.8f)
            }
            is BostanEvent.WaveClear -> {
                sound?.play(Sfx.CLEAR, volume = 0.6f, rate = 1.2f)
                texts += Floating("+${BostanState.WAVE_BONUS}", BostanState.COLS / 2f, 2.5f, Color(0xFFFDE68A), big = true, maxLife = 1.2f)
            }
            is BostanEvent.Water -> {
                sound?.play(Sfx.POP, volume = 0.35f, rate = 1.4f)
                texts += Floating("+${event.amount}", event.lane + 0.5f, event.row - 0.2f, Color(0xFF7DD3FC), big = false, maxLife = 0.9f)
            }
            is BostanEvent.DropFell -> sound?.play(Sfx.SPLASH, volume = 0.25f, rate = 1.6f)
            BostanEvent.Won -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                sound?.play(Sfx.BIG, volume = 1f)
                texts += Floating(wonLabel, BostanState.COLS / 2f, 2.5f, Color(0xFFFDE68A), big = true, maxLife = 2f)
            }
            BostanEvent.Lost -> {
                sound?.play(Sfx.OVER)
                texts += Floating(lostLabel, BostanState.COLS / 2f, 2.5f, Color(0xFFFCA5A5), big = true, maxLife = 2f)
            }
            is BostanEvent.Placed, is BostanEvent.Removed, is BostanEvent.EnemySpawned -> Unit
        }
    }

    /** Hücre dokunuşunun geri bildirimi (dönüş değerinden; olay listesine düşmez). */
    fun onTap(outcome: TapOutcome, lane: Int, row: Int, sound: SoundPlayer?, haptics: HapticFeedback) {
        val x = lane + 0.5f
        val y = row.toFloat()
        when (outcome) {
            TapOutcome.DROP -> {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                sound?.play(Sfx.POP, volume = 0.6f, rate = 1.6f)
                burst(x, y, Color(0xFF7DD3FC), 6, 0.35f, 1.2f)
                texts += Floating("+${BostanState.DROP_WATER}", x, y - 0.25f, Color(0xFF7DD3FC), big = false, maxLife = 0.9f)
            }
            TapOutcome.PLACED -> {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                sound?.play(Sfx.DROP, volume = 0.5f, rate = 1.2f)
                burst(x, y + 0.2f, Color(0xFFA16207), 6, 0.3f, 0.8f)
            }
            TapOutcome.REMOVED -> {
                sound?.play(Sfx.WHOOSH, volume = 0.5f, rate = 0.9f)
                burst(x, y, Color(0xFFA16207), 6, 0.4f, 1f)
            }
            TapOutcome.NO_WATER -> {
                sound?.play(Sfx.HOCK, volume = 0.4f, rate = 0.8f)
                texts += Floating(noWaterLabel, x, y - 0.2f, Color(0xFFFCA5A5), big = false, maxLife = 0.8f)
            }
            TapOutcome.COOLDOWN -> {
                sound?.play(Sfx.HOCK, volume = 0.4f, rate = 1f)
                texts += Floating(cooldownLabel, x, y - 0.2f, Color(0xFFFCA5A5), big = false, maxLife = 0.8f)
            }
            TapOutcome.OCCUPIED -> {
                sound?.play(Sfx.HOCK, volume = 0.3f, rate = 1.2f)
                texts += Floating(occupiedLabel, x, y - 0.2f, Color(0xFFFCA5A5), big = false, maxLife = 0.8f)
            }
            TapOutcome.NO_SELECTION, TapOutcome.EMPTY, TapOutcome.IGNORED -> Unit
        }
    }

    private fun furColor(kind: EnemyKind): Color = when (kind) {
        EnemyKind.KARGA -> Color(0xFF1F2937)
        EnemyKind.TAVSAN -> Color(0xFFE5E7EB)
        EnemyKind.KECI -> Color(0xFFD4D4D8)
        EnemyKind.DOMUZ -> Color(0xFF57534E)
        EnemyKind.AYI -> Color(0xFF9A3412)
    }

    private fun burst(x: Float, y: Float, color: Color, count: Int, life: Float, speed: Float) {
        repeat(count) {
            val a = rng.nextFloat() * 6.283f
            val sp = speed * (0.4f + rng.nextFloat() * 0.6f)
            particles += Particle(x, y, kotlin.math.cos(a) * sp, kotlin.math.sin(a) * sp - 0.3f, color, 0.04f + rng.nextFloat() * 0.05f, life)
        }
    }

    fun update(dt: Float) {
        for (p in particles) {
            p.life -= dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vy += 1.5f * dt
        }
        particles.removeAll { it.life <= 0f }
        for (t in texts) {
            t.life -= dt
            t.y -= 0.5f * dt
        }
        texts.removeAll { it.life <= 0f }
        if (shakeTimer > 0f) {
            shakeTimer -= dt
            val k = (shakeTimer / 0.4f).coerceIn(0f, 1f) * 0.08f
            shakeX = (rng.nextFloat() * 2f - 1f) * k
            shakeY = (rng.nextFloat() * 2f - 1f) * k
        } else {
            shakeX = 0f
            shakeY = 0f
        }
        if (flash > 0f) flash = (flash - dt * 2.5f).coerceAtLeast(0f)
    }
}
