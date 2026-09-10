package com.za.games.ui.ucurtma

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.za.games.platform.Sfx
import com.za.games.platform.SoundPlayer
import com.za.games.ucurtma.CrashKind
import com.za.games.ucurtma.MissionKind
import com.za.games.ucurtma.UcurtmaEvent
import com.za.games.ucurtma.UcurtmaWorld
import kotlin.random.Random

/**
 * Görsel ve işitsel geri bildirim: bulut paralaksı, parçacıklar, uçan
 * yazılar, çarpmada sarsıntı ve parlama; olaylara bağlı sesler. Simülasyona
 * yazmaz; kendi rastgelesi vardır (görsel, tohuma bağlı değil).
 */
class UcurtmaFx {

    class Cloud(var x: Float, var y: Float, val size: Float, val alpha: Float)
    class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float, val color: Color, val size: Float, val maxLife: Float) {
        var life = maxLife
    }
    class Floating(val text: String, var x: Float, var y: Float, val color: Color, val big: Boolean, val maxLife: Float) {
        var life = maxLife
    }

    private val rng = Random(7)
    val clouds = ArrayList<Cloud>()
    val particles = ArrayList<Particle>()
    val texts = ArrayList<Floating>()
    var shakeX = 0f
        private set
    var shakeY = 0f
        private set
    var flash = 0f
        private set
    private var shakeTimer = 0f

    /** Görev metinleri ekrandan gelir (dile bağlı). */
    var missionLabel: (MissionKind) -> String = { it.name }
    var milestoneLabel: (Int) -> String = { "$it m" }
    var missionDoneLabel: String = ""
    var nearMissLabel: String = ""

    val isBusy: Boolean get() = particles.isNotEmpty() || texts.isNotEmpty() || shakeTimer > 0f || flash > 0f

    init {
        reset()
    }

    fun reset() {
        clouds.clear()
        repeat(5) { i ->
            clouds += Cloud(rng.nextFloat() * 1.2f, 0.12f + rng.nextFloat() * 0.5f, 0.05f + rng.nextFloat() * 0.05f, 0.5f + 0.4f * rng.nextFloat())
        }
        particles.clear()
        texts.clear()
        shakeX = 0f
        shakeY = 0f
        shakeTimer = 0f
        flash = 0f
    }

    fun onEvent(event: UcurtmaEvent, sound: SoundPlayer?, haptics: HapticFeedback) {
        when (event) {
            is UcurtmaEvent.RibbonTaken -> {
                sound?.play(Sfx.POP, volume = 0.45f, rate = 1.3f + 0.02f * (event.count % 10))
                burst(event.x, event.y, RibbonColors[event.count % RibbonColors.size], 6, 0.35f)
            }
            is UcurtmaEvent.Cut -> {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                sound?.play(Sfx.CLEAR, volume = 0.7f, rate = 1.1f)
                burst(event.x, event.y, Color(0xFFFFFFFF), 8, 0.5f)
                texts += Floating("+${event.bonus}", event.x, event.y - 0.08f, Color(0xFFFDE68A), big = true, maxLife = 1.1f)
            }
            is UcurtmaEvent.NearMiss -> {
                sound?.play(Sfx.WHOOSH, volume = 0.35f, rate = 1.4f)
                texts += Floating(nearMissLabel, event.x, event.y - 0.12f, Color(0xFFE2E8F0), big = false, maxLife = 0.8f)
            }
            is UcurtmaEvent.UnderWire -> sound?.play(Sfx.DROP, volume = 0.25f, rate = 1.6f)
            is UcurtmaEvent.Milestone -> {
                sound?.play(Sfx.BIG, volume = 0.35f, rate = 1.3f)
                texts += Floating(milestoneLabel(event.meters), UcurtmaWorld.KITE_X + 0.3f, 0.35f, Color(0xFFFFFFFF), big = true, maxLife = 1.2f)
            }
            is UcurtmaEvent.MissionDone -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                sound?.play(Sfx.BIG, volume = 0.8f, rate = 1f)
                texts += Floating(missionDoneLabel, UcurtmaWorld.KITE_X + 0.3f, 0.5f, Color(0xFF86EFAC), big = true, maxLife = 1.6f)
            }
            is UcurtmaEvent.Crash -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                sound?.play(if (event.kind == CrashKind.STRING) Sfx.SCREECH else Sfx.SPLAT, volume = 0.9f, rate = if (event.kind == CrashKind.STRING) 0.7f else 1f)
                shakeTimer = 0.35f
                flash = 1f
                burst(UcurtmaWorld.KITE_X, 0f, Color(0xFFEF4444), 10, 0.6f)
            }
            UcurtmaEvent.Over -> sound?.play(Sfx.OVER)
        }
    }

    private fun burst(x: Float, y: Float, color: Color, count: Int, life: Float) {
        repeat(count) {
            val a = rng.nextFloat() * 6.283f
            val sp = 0.15f + rng.nextFloat() * 0.35f
            particles += Particle(x, y, kotlin.math.cos(a) * sp, kotlin.math.sin(a) * sp - 0.1f, color, 0.006f + rng.nextFloat() * 0.008f, life)
        }
    }

    fun update(dt: Float, world: UcurtmaWorld) {
        val scroll = if (world.status == com.za.games.ucurtma.UcurtmaStatus.RUNNING) world.speed else 0f
        for (c in clouds) {
            c.x -= scroll * 0.15f * dt
            if (c.x < -0.3f) {
                c.x = UcurtmaWorld.WIDTH + 0.2f + rng.nextFloat() * 0.3f
                c.y = 0.12f + rng.nextFloat() * 0.5f
            }
        }
        // Çarpma parçacıkları uçurtmanın yerinden çıkar (y olaydan sonra bilinir).
        for (p in particles) {
            if (p.y == 0f && p.life == p.maxLife) p.y = world.kiteY
            p.life -= dt
            p.x += (p.vx - scroll) * dt
            p.y += p.vy * dt
            p.vy += 0.6f * dt
        }
        particles.removeAll { it.life <= 0f }
        for (t in texts) {
            t.life -= dt
            t.y -= 0.12f * dt
        }
        texts.removeAll { it.life <= 0f }
        if (shakeTimer > 0f) {
            shakeTimer -= dt
            val k = (shakeTimer / 0.35f).coerceIn(0f, 1f) * 0.02f
            shakeX = (rng.nextFloat() * 2f - 1f) * k
            shakeY = (rng.nextFloat() * 2f - 1f) * k
        } else {
            shakeX = 0f
            shakeY = 0f
        }
        if (flash > 0f) flash = (flash - dt * 2.5f).coerceAtLeast(0f)
    }

    companion object {
        val RibbonColors = listOf(Color(0xFFF472B6), Color(0xFFFDE047), Color(0xFF34D399), Color(0xFF60A5FA), Color(0xFFFB923C))
    }
}
