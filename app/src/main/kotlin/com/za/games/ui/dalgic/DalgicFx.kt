package com.za.games.ui.dalgic

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.za.games.dalgic.DalgicEvent
import com.za.games.dalgic.DalgicWorld
import com.za.games.dalgic.FoeKind
import com.za.games.dalgic.LifeCause
import com.za.games.platform.Sfx
import com.za.games.platform.SoundPlayer
import kotlin.random.Random

/**
 * Görsel ve işitsel geri bildirim: kabarcıklar, patlama parçacıkları, uçan
 * yazılar, can kaybında sarsıntı ve parlama; olaylara bağlı sesler.
 */
class DalgicFx {

    class Bubble(var x: Float, var y: Float, val r: Float, val vy: Float)
    class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float, val color: Color, val size: Float, val maxLife: Float) {
        var life = maxLife
    }
    class Floating(val text: String, var x: Float, var y: Float, val color: Color, val big: Boolean, val maxLife: Float) {
        var life = maxLife
    }

    private val rng = Random(11)
    val bubbles = ArrayList<Bubble>()
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
    private var bubbleTimer = 0f

    /** Dile bağlı metinler ekrandan verilir. */
    var fullLabel = ""
    var oxygenLabel = ""
    var lifeLabel: (LifeCause) -> String = { it.name }

    val isBusy: Boolean get() = particles.isNotEmpty() || texts.isNotEmpty() || shakeTimer > 0f || flash > 0f

    fun reset() {
        bubbles.clear()
        particles.clear()
        texts.clear()
        shakeTimer = 0f
        shakeX = 0f
        shakeY = 0f
        flash = 0f
    }

    fun onEvent(event: DalgicEvent, world: DalgicWorld, sound: SoundPlayer?, haptics: HapticFeedback) {
        when (event) {
            is DalgicEvent.DiverRescued -> {
                sound?.play(Sfx.POP, volume = 0.5f, rate = 1.2f + 0.05f * event.count)
                burst(event.x, event.y, Color(0xFF7DD3FC), 5, 0.4f)
            }
            DalgicEvent.Full -> {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                sound?.play(Sfx.CLEAR, volume = 0.6f, rate = 1.2f)
                texts += Floating(fullLabel, world.subX, world.subY - 0.1f, Color(0xFFFDE68A), big = true, maxLife = 1.4f)
            }
            is DalgicEvent.Delivered -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                sound?.play(Sfx.BIG, volume = 0.9f, rate = if (event.divers >= DalgicWorld.CAPACITY) 1.2f else 1f)
                texts += Floating("+${event.points}", world.subX, 0.3f, Color(0xFFFDE68A), big = true, maxLife = 1.5f)
            }
            DalgicEvent.Shot -> sound?.play(Sfx.SHOT, volume = 0.12f, rate = 0.8f)
            DalgicEvent.EnemyShot -> sound?.play(Sfx.SHOT, volume = 0.2f, rate = 0.55f)
            is DalgicEvent.FoeDown -> {
                sound?.play(if (event.kind == FoeKind.MINE) Sfx.STOMP else Sfx.SPLAT, volume = 0.7f, rate = if (event.kind == FoeKind.MINE) 0.7f else 1f)
                burst(event.x, event.y, if (event.kind == FoeKind.MINE) Color(0xFFFB923C) else Color(0xFFE2E8F0), 8, 0.5f)
                texts += Floating("+${event.points}", event.x, event.y - 0.06f, Color(0xFFFFFFFF), big = false, maxLife = 0.9f)
            }
            is DalgicEvent.LifeLost -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                sound?.play(if (event.cause == LifeCause.OXYGEN || event.cause == LifeCause.EMPTY_SURFACE) Sfx.HORN else Sfx.SPLAT, volume = 0.9f, rate = 0.8f)
                shakeTimer = 0.4f
                flash = 1f
                flashColor = if (event.cause == LifeCause.OXYGEN) Color(0xFF1E3A8A) else Color(0xFFEF4444)
                texts += Floating(lifeLabel(event.cause), 0.5f, 0.7f, Color(0xFFFCA5A5), big = true, maxLife = 1.6f)
            }
            DalgicEvent.OxygenLow -> {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                sound?.play(Sfx.HORN, volume = 0.6f, rate = 1.3f)
                texts += Floating(oxygenLabel, world.subX, world.subY - 0.1f, Color(0xFFFCA5A5), big = false, maxLife = 1.2f)
            }
            DalgicEvent.Surfaced -> sound?.play(Sfx.SPLASH, volume = 0.6f, rate = 1f)
            DalgicEvent.Over -> sound?.play(Sfx.OVER)
        }
    }

    private fun burst(x: Float, y: Float, color: Color, count: Int, life: Float) {
        repeat(count) {
            val a = rng.nextFloat() * 6.283f
            val sp = 0.1f + rng.nextFloat() * 0.3f
            particles += Particle(x, y, kotlin.math.cos(a) * sp, kotlin.math.sin(a) * sp, color, 0.006f + rng.nextFloat() * 0.008f, life)
        }
    }

    fun update(dt: Float, world: DalgicWorld) {
        bubbleTimer -= dt
        if (bubbleTimer <= 0f && !world.atSurface) {
            bubbleTimer = 0.12f
            bubbles += Bubble(world.subX - world.facing * DalgicWorld.SUB_R * 1.2f, world.subY, 0.004f + rng.nextFloat() * 0.006f, 0.15f + rng.nextFloat() * 0.1f)
        }
        for (b in bubbles) {
            b.y -= b.vy * dt
            b.x += (rng.nextFloat() - 0.5f) * 0.01f * dt
        }
        bubbles.removeAll { it.y < DalgicWorld.SURFACE_Y }
        for (p in particles) {
            p.life -= dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vy -= 0.2f * dt
        }
        particles.removeAll { it.life <= 0f }
        for (t in texts) {
            t.life -= dt
            t.y -= 0.1f * dt
        }
        texts.removeAll { it.life <= 0f }
        if (shakeTimer > 0f) {
            shakeTimer -= dt
            val k = (shakeTimer / 0.4f).coerceIn(0f, 1f) * 0.02f
            shakeX = (rng.nextFloat() * 2f - 1f) * k
            shakeY = (rng.nextFloat() * 2f - 1f) * k
        } else {
            shakeX = 0f
            shakeY = 0f
        }
        if (flash > 0f) flash = (flash - dt * 2.5f).coerceAtLeast(0f)
    }
}
