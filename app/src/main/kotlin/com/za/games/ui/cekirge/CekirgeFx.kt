package com.za.games.ui.cekirge

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.za.games.cekirge.BugKind
import com.za.games.cekirge.CekirgeEvent
import com.za.games.cekirge.CekirgeWorld
import com.za.games.platform.Sfx
import com.za.games.platform.SoundPlayer
import kotlin.random.Random

/**
 * Görsel ve işitsel geri bildirim: çekirge ve saman parçacıkları, uçan
 * yazılar, can kaybında sarsıntı ve parlama; olaylara bağlı sesler.
 * Koordinatlar tarla birimi (x 0..1, y 0..1,35 aşağı).
 */
class CekirgeFx {

    class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float, val color: Color, val size: Float, val maxLife: Float) {
        var life = maxLife
    }
    class Floating(val text: String, var x: Float, var y: Float, val color: Color, val big: Boolean, val maxLife: Float) {
        var life = maxLife
    }

    private val rng = Random(31)
    val particles = ArrayList<Particle>()
    val texts = ArrayList<Floating>()
    var shakeX = 0f
        private set
    var shakeY = 0f
        private set
    var flash = 0f
        private set
    private var shakeTimer = 0f

    /** Dile bağlı metinler ekrandan verilir. */
    var waveLabel: (Int) -> String = { "$it" }
    var clearedLabel = ""
    var lifeLabel = ""
    var invadedLabel = ""
    var lostLabel = ""

    val isBusy: Boolean get() = particles.isNotEmpty() || texts.isNotEmpty() || shakeTimer > 0f || flash > 0f

    fun reset() {
        particles.clear()
        texts.clear()
        shakeTimer = 0f
        shakeX = 0f
        shakeY = 0f
        flash = 0f
    }

    fun onEvent(event: CekirgeEvent, world: CekirgeWorld, sound: SoundPlayer?, haptics: HapticFeedback) {
        when (event) {
            CekirgeEvent.Fired -> sound?.play(Sfx.SPIT, volume = 0.35f, rate = 1.4f)
            is CekirgeEvent.BugHit -> {
                sound?.play(Sfx.SPLAT, volume = 0.6f, rate = 1.5f - 0.2f * event.kind.ordinal)
                burst(event.x, event.y, bugColor(event.kind), 7, 0.45f, 0.5f)
                texts += Floating("+${event.points}", event.x, event.y - 0.03f, Color.White, big = false, maxLife = 0.8f)
            }
            is CekirgeEvent.SpitHit -> {
                sound?.play(Sfx.POP, volume = 0.5f, rate = 1.3f)
                burst(event.x, event.y, Color(0xFF86EFAC), 5, 0.3f, 0.4f)
            }
            is CekirgeEvent.BaleHit -> {
                sound?.play(Sfx.DROP, volume = 0.3f, rate = 1.7f)
                burst(event.x, event.y, Color(0xFFFDE68A), 6, 0.4f, 0.35f)
            }
            is CekirgeEvent.FarmerHit -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                sound?.play(Sfx.HORN, volume = 0.9f, rate = 0.8f)
                shakeTimer = 0.4f
                flash = 1f
                burst(world.farmerX, CekirgeWorld.FARMER_Y, Color(0xFFFCA5A5), 10, 0.6f, 0.6f)
                texts += Floating(lifeLabel, world.farmerX, CekirgeWorld.FARMER_Y - 0.1f, Color(0xFFFCA5A5), big = true, maxLife = 1.4f)
            }
            is CekirgeEvent.QueenSpawned -> sound?.play(Sfx.WHOOSH, volume = 0.5f, rate = 0.6f)
            is CekirgeEvent.QueenHit -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                sound?.play(Sfx.BIG, volume = 0.9f, rate = 1.1f)
                burst(event.x, CekirgeWorld.QUEEN_Y, Color(0xFFFACC15), 14, 0.7f, 0.7f)
                texts += Floating("+${event.points}", event.x, CekirgeWorld.QUEEN_Y + 0.06f, Color(0xFFFDE68A), big = true, maxLife = 1.4f)
            }
            is CekirgeEvent.WaveCleared -> {
                sound?.play(Sfx.CLEAR, volume = 0.7f, rate = 1.1f)
                texts += Floating("$clearedLabel +${event.bonus}", 0.5f, 0.55f, Color(0xFFFDE68A), big = true, maxLife = 1.5f)
            }
            is CekirgeEvent.WaveStart -> {
                if (event.wave > 1) sound?.play(Sfx.HORN, volume = 0.5f, rate = 1.2f)
                texts += Floating(waveLabel(event.wave), 0.5f, 0.62f, Color.White, big = true, maxLife = 1.4f)
            }
            is CekirgeEvent.Over -> {
                sound?.play(Sfx.OVER)
                shakeTimer = 0.5f
                flash = 1f
                texts += Floating(if (event.invaded) invadedLabel else lostLabel, 0.5f, 0.7f, Color(0xFFFCA5A5), big = true, maxLife = 2f)
            }
        }
    }

    /** Uçan fıskırtma varken sıkma denemesi: sessiz bir "tık". */
    fun onFireBlocked(sound: SoundPlayer?) {
        sound?.play(Sfx.HOCK, volume = 0.15f, rate = 1.6f)
    }

    private fun bugColor(kind: BugKind): Color = when (kind) {
        BugKind.KARA -> Color(0xFF374151)
        BugKind.YESIL -> Color(0xFF65A30D)
        BugKind.KAHVE -> Color(0xFFCA8A04)
    }

    private fun burst(x: Float, y: Float, color: Color, count: Int, life: Float, speed: Float) {
        repeat(count) {
            val a = rng.nextFloat() * 6.283f
            val sp = speed * (0.4f + rng.nextFloat() * 0.6f)
            particles += Particle(x, y, kotlin.math.cos(a) * sp, kotlin.math.sin(a) * sp - 0.2f, color, 0.006f + rng.nextFloat() * 0.008f, life)
        }
    }

    fun update(dt: Float) {
        for (p in particles) {
            p.life -= dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vy += 0.8f * dt
        }
        particles.removeAll { it.life <= 0f }
        for (t in texts) {
            t.life -= dt
            t.y -= 0.08f * dt
        }
        texts.removeAll { it.life <= 0f }
        if (shakeTimer > 0f) {
            shakeTimer -= dt
            val k = (shakeTimer / 0.4f).coerceIn(0f, 1f) * 0.015f
            shakeX = (rng.nextFloat() * 2f - 1f) * k
            shakeY = (rng.nextFloat() * 2f - 1f) * k
        } else {
            shakeX = 0f
            shakeY = 0f
        }
        if (flash > 0f) flash = (flash - dt * 2.5f).coerceAtLeast(0f)
    }
}
