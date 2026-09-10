package com.za.games.ui.sincap

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.za.games.platform.Sfx
import com.za.games.platform.SoundPlayer
import com.za.games.sincap.DeathCause
import com.za.games.sincap.SincapEvent
import com.za.games.sincap.SincapWorld
import kotlin.random.Random

/**
 * Görsel ve işitsel geri bildirim: yaprak ve kabuk parçacıkları, uçan
 * yazılar, ölümde sarsıntı ve parlama; olaylara bağlı sesler. Koordinatlar
 * dünya birimi: x −1..1, y basamak.
 */
class SincapFx {

    class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float, val color: Color, val size: Float, val maxLife: Float) {
        var life = maxLife
    }
    class Floating(val text: String, var x: Float, var y: Float, val color: Color, val big: Boolean, val maxLife: Float) {
        var life = maxLife
    }

    private val rng = Random(23)
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
    var crackingLabel = ""
    var catLabel = ""
    var milestoneLabel: (Int) -> String = { "$it" }
    var overLabel: (DeathCause) -> String = { it.name }

    val isBusy: Boolean get() = particles.isNotEmpty() || texts.isNotEmpty() || shakeTimer > 0f || flash > 0f

    fun reset() {
        particles.clear()
        texts.clear()
        shakeTimer = 0f
        shakeX = 0f
        shakeY = 0f
        flash = 0f
    }

    fun onEvent(event: SincapEvent, world: SincapWorld, sound: SoundPlayer?, haptics: HapticFeedback) {
        when (event) {
            is SincapEvent.Jumped -> {
                sound?.play(Sfx.HOP, volume = 0.5f, rate = if (event.to - event.from > 1) 0.9f else 1.1f)
                burst(world.x, world.y, Color(0xFF65A30D), 3, 0.4f, 0.8f)
            }
            is SincapEvent.Landed -> burst(SincapWorld.sideX(event.side), event.level.toFloat(), Color(0xFF84CC16), 4, 0.35f, 0.9f)
            is SincapEvent.Nut -> {
                if (event.golden) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    sound?.play(Sfx.BIG, volume = 0.8f, rate = 1.2f)
                    burst(SincapWorld.sideX(event.side), event.level.toFloat(), Color(0xFFFDE68A), 12, 0.7f, 1.6f)
                } else {
                    sound?.play(Sfx.POP, volume = 0.6f, rate = 1.4f)
                    burst(SincapWorld.sideX(event.side), event.level.toFloat(), Color(0xFFD97706), 5, 0.4f, 1f)
                }
                texts += Floating("+${event.points}", SincapWorld.sideX(event.side), event.level + 0.4f, if (event.golden) Color(0xFFFDE68A) else Color(0xFFFFFFFF), big = event.golden, maxLife = 0.9f)
            }
            is SincapEvent.Cracking -> {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                sound?.play(Sfx.HOCK, volume = 0.5f, rate = 0.7f)
                texts += Floating(crackingLabel, SincapWorld.sideX(event.side), event.level + 0.5f, Color(0xFFFCA5A5), big = false, maxLife = 0.9f)
            }
            is SincapEvent.Broke -> {
                sound?.play(Sfx.DROP, volume = 0.7f, rate = 0.6f)
                burst(SincapWorld.sideX(event.side), event.level.toFloat(), Color(0xFF92400E), 10, 0.6f, 1.2f)
            }
            is SincapEvent.CrowSpawned -> sound?.play(Sfx.SCREECH, volume = 0.45f, rate = 1.1f)
            is SincapEvent.Milestone -> {
                sound?.play(Sfx.CLEAR, volume = 0.6f, rate = 1.1f)
                texts += Floating(milestoneLabel(event.height), 0f, world.y + 1.2f, Color(0xFFFDE68A), big = true, maxLife = 1.4f)
            }
            SincapEvent.CatClose -> {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                sound?.play(Sfx.HORN, volume = 0.6f, rate = 0.9f)
                texts += Floating(catLabel, 0f, world.y - 0.8f, Color(0xFFFCA5A5), big = false, maxLife = 1.3f)
            }
            is SincapEvent.Over -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                sound?.play(if (event.cause == DeathCause.CROW) Sfx.SCREECH else if (event.cause == DeathCause.CAT) Sfx.HORN else Sfx.SPLAT, volume = 0.9f, rate = 0.8f)
                sound?.play(Sfx.OVER)
                shakeTimer = 0.45f
                flash = 1f
                texts += Floating(overLabel(event.cause), 0f, world.y + 0.6f, Color(0xFFFCA5A5), big = true, maxLife = 1.8f)
                burst(world.x, world.y, Color(0xFFB45309), 10, 0.7f, 1.4f)
            }
        }
    }

    private fun burst(x: Float, y: Float, color: Color, count: Int, life: Float, speed: Float) {
        repeat(count) {
            val a = rng.nextFloat() * 6.283f
            val sp = speed * (0.4f + rng.nextFloat() * 0.6f)
            particles += Particle(x, y, kotlin.math.cos(a) * sp, kotlin.math.sin(a) * sp + 0.6f, color, 0.025f + rng.nextFloat() * 0.03f, life)
        }
    }

    fun update(dt: Float) {
        for (p in particles) {
            p.life -= dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vy -= 3f * dt
        }
        particles.removeAll { it.life <= 0f }
        for (t in texts) {
            t.life -= dt
            t.y += 0.4f * dt
        }
        texts.removeAll { it.life <= 0f }
        if (shakeTimer > 0f) {
            shakeTimer -= dt
            val k = (shakeTimer / 0.45f).coerceIn(0f, 1f) * 0.04f
            shakeX = (rng.nextFloat() * 2f - 1f) * k
            shakeY = (rng.nextFloat() * 2f - 1f) * k
        } else {
            shakeX = 0f
            shakeY = 0f
        }
        if (flash > 0f) flash = (flash - dt * 2.5f).coerceAtLeast(0f)
    }
}
