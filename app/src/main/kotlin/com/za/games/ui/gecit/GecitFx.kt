package com.za.games.ui.gecit

import android.content.res.Resources
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.za.games.R
import com.za.games.gecit.DeathCause
import com.za.games.gecit.GecitEvent
import com.za.games.gecit.GecitWorld
import com.za.games.platform.Sfx
import com.za.games.platform.SoundPlayer
import kotlin.math.exp
import kotlin.random.Random

/** Hücre biriminde parçacık; yalnızca çizim için. */
internal class GecitParticle(
    var x: Float,
    var row: Float,
    var vx: Float,
    var vRow: Float,
    val size: Float,
    val color: Color,
    val maxLife: Float,
) {
    var life = maxLife
}

internal class GecitText(val text: String, var x: Float, var row: Float) {
    val maxLife = 1.3f
    var life = maxLife
}

/** Motor olaylarını ses, titreşim, parçacık ve sarsıntıya çevirir. Rastgelelik salt görseldir. */
internal class GecitFx {
    val particles = ArrayList<GecitParticle>()
    val texts = ArrayList<GecitText>()
    var shakeX = 0f
        private set
    var shakeY = 0f
        private set
    private var shake = 0f
    private val rng = Random.Default

    val isBusy: Boolean get() = particles.isNotEmpty() || texts.isNotEmpty() || shake > 0.005f

    fun reset() {
        particles.clear()
        texts.clear()
        shake = 0f
        shakeX = 0f
        shakeY = 0f
    }

    fun onEvent(
        event: GecitEvent,
        world: GecitWorld,
        sound: SoundPlayer?,
        haptics: HapticFeedback,
        resources: Resources,
    ) {
        val p = world.player
        when (event) {
            GecitEvent.Hop -> {
                sound?.play(Sfx.HOP, volume = 0.35f, rate = 0.92f + rng.nextFloat() * 0.16f)
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                burst(p.fromX + 0.5f, p.fromRow.toFloat(), 3, DUST, 1.2f)
            }
            GecitEvent.Bump -> sound?.play(Sfx.DROP, volume = 0.2f, rate = 1.6f)
            is GecitEvent.Warning -> sound?.play(Sfx.HORN, volume = 0.5f)
            is GecitEvent.Train -> {
                sound?.play(Sfx.DROP, volume = 0.5f, rate = 0.4f)
                shake = maxOf(shake, 0.08f)
            }
            is GecitEvent.Milestone -> {
                sound?.play(Sfx.CLEAR, volume = 0.7f)
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                texts += GecitText(resources.getString(R.string.gecit_milestone_fmt, event.row), p.centerX, p.row + 1.2f)
            }
            is GecitEvent.Over -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                when (event.cause) {
                    DeathCause.CAR, DeathCause.TRAIN -> {
                        sound?.play(Sfx.DROP, volume = 0.9f, rate = 0.5f)
                        burst(p.centerX, p.row.toFloat(), 12, CRASH, 4f)
                        shake = maxOf(shake, 0.3f)
                    }
                    DeathCause.WATER, DeathCause.CARRIED -> {
                        sound?.play(Sfx.DROP, volume = 0.7f, rate = 1.2f)
                        burst(p.centerX, p.row.toFloat(), 10, WATER, 2.5f)
                    }
                    DeathCause.EAGLE -> shake = maxOf(shake, 0.15f)
                    DeathCause.CAMERA -> Unit
                }
                sound?.play(Sfx.OVER)
            }
        }
    }

    private fun burst(x: Float, row: Float, count: Int, color: Color, speed: Float) {
        repeat(count) {
            particles += GecitParticle(
                x = x,
                row = row + 0.5f,
                vx = (rng.nextFloat() * 2f - 1f) * speed,
                vRow = rng.nextFloat() * speed * 0.8f + 0.2f,
                size = 0.08f + rng.nextFloat() * 0.12f,
                color = color,
                maxLife = 0.3f + rng.nextFloat() * 0.35f,
            )
        }
    }

    fun update(dt: Float) {
        if (dt <= 0f) return
        val it = particles.iterator()
        while (it.hasNext()) {
            val q = it.next()
            q.life -= dt
            if (q.life <= 0f) {
                it.remove()
                continue
            }
            q.vRow -= 9f * dt
            q.x += q.vx * dt
            q.row += q.vRow * dt
        }
        val ti = texts.iterator()
        while (ti.hasNext()) {
            val t = ti.next()
            t.life -= dt
            t.row += 0.8f * dt
            if (t.life <= 0f) ti.remove()
        }
        shake *= exp(-8f * dt)
        if (shake < 0.005f) {
            shake = 0f
            shakeX = 0f
            shakeY = 0f
        } else {
            shakeX = (rng.nextFloat() * 2f - 1f) * shake
            shakeY = (rng.nextFloat() * 2f - 1f) * shake
        }
    }

    companion object {
        val DUST = Color(0xFFD9F99D)
        val CRASH = Color(0xFFF87171)
        val WATER = Color(0xFF93C5FD)
    }
}
