package com.za.games.ui.cici

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.za.games.cici.CiciEvent
import com.za.games.cici.CiciWorld
import com.za.games.cici.HazardKind
import com.za.games.cici.TreatKind
import com.za.games.platform.Sfx
import com.za.games.platform.SoundPlayer
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Görsel ve işitsel geri bildirim: yakalamada kıvılcım ve kalp, seri
 * büyüyünce büyük kutlama, vuruşta tüy saçılması, sarsıntı ve parlama;
 * sıkılınca ve puan giderken solgun yazılar. Sesler olaylara bağlı.
 */
class CiciFx {

    enum class Shape { DOT, HEART, FEATHER }

    class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float, val color: Color, val size: Float, val shape: Shape, val maxLife: Float) {
        var life = maxLife
        var spin = 0f
    }

    class Floating(val text: String, var x: Float, var y: Float, val color: Color, val big: Boolean, val maxLife: Float) {
        var life = maxLife
    }

    private val rng = Random(11)
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

    /** Sevinç dalgası: yakalamadan sonra kısa süre Cici'nin çevresinde halka. */
    var joyPulse = 0f
        private set
    private var shakeTimer = 0f

    /** Dile bağlı metinler ekrandan verilir. */
    var joyLabel = ""
    var boredLabel = ""
    var hitLabel: (HazardKind) -> String = { it.name }

    val isBusy: Boolean get() = particles.isNotEmpty() || texts.isNotEmpty() || shakeTimer > 0f || flash > 0f

    fun reset() {
        particles.clear()
        texts.clear()
        shakeTimer = 0f
        shakeX = 0f
        shakeY = 0f
        flash = 0f
        joyPulse = 0f
    }

    fun onEvent(event: CiciEvent, world: CiciWorld, sound: SoundPlayer?, haptics: HapticFeedback) {
        when (event) {
            is CiciEvent.Caught -> {
                val (rate, color) = when (event.kind) {
                    TreatKind.HONEY -> 1.4f to Color(0xFFFDE68A)
                    TreatKind.SEED -> 1.15f to Color(0xFFE9C39B)
                    TreatKind.WATER -> 0.9f to Color(0xFF7DD3FC)
                }
                sound?.play(Sfx.POP, volume = 0.55f, rate = rate)
                burst(event.x, event.y, color, 6, 0.5f, Shape.DOT)
                hearts(world.ciciX, world.ciciY - CiciWorld.CICI_R, if (event.streak >= 3) 4 else 2)
                joyPulse = 1f
                texts += Floating("+${event.points}", event.x, event.y - 0.05f, Color.White, big = false, maxLife = 0.9f)
                if (event.streak >= 3 && event.streak % 3 == 0) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    sound?.play(Sfx.CLEAR, volume = 0.6f, rate = 1.25f)
                    texts += Floating(joyLabel, world.ciciX, world.ciciY - 0.14f, Color(0xFFFDE68A), big = true, maxLife = 1.4f)
                    hearts(world.ciciX, world.ciciY - CiciWorld.CICI_R, 6)
                }
            }
            is CiciEvent.Hit -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                sound?.play(Sfx.SPLAT, volume = 0.9f, rate = 0.8f)
                shakeTimer = 0.4f
                flash = 1f
                flashColor = Color(0xFFEF4444)
                burst(world.ciciX, world.ciciY, Color(0xFFF8FAFC), 9, 0.9f, Shape.FEATHER)
                texts += Floating(hitLabel(event.by), world.ciciX, world.ciciY - 0.12f, Color(0xFFFCA5A5), big = true, maxLife = 1.4f)
            }
            CiciEvent.Bored -> {
                sound?.play(Sfx.POP, volume = 0.35f, rate = 0.5f)
                texts += Floating(boredLabel, world.ciciX, world.ciciY - 0.12f, Color(0xFFCBD5E1), big = false, maxLife = 1.6f)
            }
            is CiciEvent.PointLost -> {
                sound?.play(Sfx.DROP, volume = 0.3f, rate = 0.7f)
                texts += Floating("−1", world.ciciX + 0.06f, world.ciciY - 0.06f, Color(0xFFFCA5A5), big = false, maxLife = 0.9f)
            }
            CiciEvent.Over -> sound?.play(Sfx.OVER)
        }
    }

    private fun burst(x: Float, y: Float, color: Color, count: Int, life: Float, shape: Shape) {
        repeat(count) {
            val a = rng.nextFloat() * 6.283f
            val sp = 0.08f + rng.nextFloat() * 0.25f
            val size = if (shape == Shape.FEATHER) 0.012f + rng.nextFloat() * 0.01f else 0.006f + rng.nextFloat() * 0.008f
            particles += Particle(x, y, cos(a) * sp, sin(a) * sp, color, size, shape, life).also { it.spin = rng.nextFloat() * 6.283f }
        }
    }

    private fun hearts(x: Float, y: Float, count: Int) {
        repeat(count) {
            val vx = (rng.nextFloat() - 0.5f) * 0.16f
            particles += Particle(x + (rng.nextFloat() - 0.5f) * 0.06f, y, vx, -0.12f - rng.nextFloat() * 0.1f, Color(0xFFF472B6), 0.011f + rng.nextFloat() * 0.006f, Shape.HEART, 1.1f)
        }
    }

    fun update(dt: Float) {
        for (p in particles) {
            p.life -= dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            when (p.shape) {
                Shape.DOT -> {
                    p.vx *= 1f - 1.5f * dt
                    p.vy *= 1f - 1.5f * dt
                }
                Shape.HEART -> p.vx *= 1f - 2f * dt
                Shape.FEATHER -> {
                    p.vx *= 1f - 2f * dt
                    p.vy += (0.06f - p.vy) * 2f * dt
                    p.spin += 3f * dt
                }
            }
        }
        particles.removeAll { it.life <= 0f }
        for (t in texts) {
            t.life -= dt
            t.y -= 0.08f * dt
        }
        texts.removeAll { it.life <= 0f }
        if (joyPulse > 0f) joyPulse = (joyPulse - dt * 1.6f).coerceAtLeast(0f)
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
