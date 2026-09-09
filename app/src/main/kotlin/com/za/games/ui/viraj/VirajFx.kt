package com.za.games.ui.viraj

import android.content.res.Resources
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.za.games.R
import com.za.games.platform.Sfx
import com.za.games.platform.SoundPlayer
import com.za.games.viraj.BoxGift
import com.za.games.viraj.ItemKind
import com.za.games.viraj.VirajEvent
import com.za.games.viraj.VirajWorld
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.random.Random

/** Ekran kesrinde parçacık (x, y ∈ 0..1); yalnızca çizim için. */
internal class VirajParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val size: Float,
    val color: Color,
    val maxLife: Float,
) {
    var life = maxLife
}

internal class VirajText(val text: String, var x: Float, var y: Float, val color: Color, val big: Boolean = false) {
    val maxLife = if (big) 1.8f else 1.1f
    var life = maxLife
}

/** Motor olaylarını ses, titreşim, parçacık, uçan yazı ve sarsıntıya çevirir. */
internal class VirajFx {
    val particles = ArrayList<VirajParticle>()
    val texts = ArrayList<VirajText>()
    var shakeX = 0f
        private set
    var shakeY = 0f
        private set

    /** Çarpışmada kısa kırmızı parlama (0..1). */
    var flash = 0f
        private set

    /** Uzak tepelerin yanal kayması (ekran genişliği kesri). */
    var bgOffset = 0f
        private set

    private var shake = 0f
    private val rng = Random.Default

    val isBusy: Boolean
        get() = particles.isNotEmpty() || texts.isNotEmpty() || shake > 0.005f || flash > 0.01f

    fun reset() {
        particles.clear()
        texts.clear()
        shake = 0f
        shakeX = 0f
        shakeY = 0f
        flash = 0f
        bgOffset = 0f
    }

    fun onEvent(
        event: VirajEvent,
        world: VirajWorld,
        sound: SoundPlayer?,
        haptics: HapticFeedback,
        resources: Resources,
    ) {
        val px = playerScreenX(world)
        when (event) {
            is VirajEvent.Overtake -> {
                sound?.play(Sfx.WHOOSH, volume = 0.5f, rate = 1.3f)
                texts += VirajText(resources.getString(R.string.viraj_overtake_fmt, VirajWorld.OVERTAKE_POINTS.toInt()), px, 0.62f, POINTS)
            }
            VirajEvent.Crash -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                sound?.play(Sfx.SCREECH, volume = 0.8f, rate = 1.1f)
                sound?.play(Sfx.DROP, volume = 0.9f, rate = 0.6f)
                burst(px, 0.8f, 14, SPARK, 0.5f)
                shake = maxOf(shake, 0.06f)
                flash = 1f
            }
            VirajEvent.ShieldUsed -> {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                sound?.play(Sfx.CLEAR, volume = 0.7f, rate = 1.2f)
                texts += VirajText(resources.getString(R.string.viraj_shield_used), px, 0.6f, SHIELD)
                burst(px, 0.8f, 10, SHIELD, 0.35f)
            }
            is VirajEvent.Checkpoint -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                sound?.play(Sfx.BIG, volume = 0.8f)
                texts += VirajText(resources.getString(R.string.viraj_checkpoint_fmt, event.bonusSeconds), 0.5f, 0.4f, BONUS, big = true)
            }
            is VirajEvent.Pickup -> {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                when (event.kind) {
                    ItemKind.TURBO -> {
                        sound?.play(Sfx.WHOOSH, volume = 0.9f, rate = 0.7f)
                        texts += VirajText(resources.getString(R.string.viraj_turbo), px, 0.6f, TURBO)
                    }
                    ItemKind.BOX -> {
                        sound?.play(Sfx.POP, volume = 0.7f, rate = 1.2f)
                        val label = when (event.gift) {
                            BoxGift.TURBO -> resources.getString(R.string.viraj_turbo)
                            BoxGift.SHIELD -> resources.getString(R.string.viraj_shield)
                            BoxGift.TIME -> resources.getString(R.string.viraj_time_gift_fmt, VirajWorld.TIME_GIFT.toInt())
                            null -> ""
                        }
                        texts += VirajText(label, px, 0.6f, BONUS)
                        burst(px, 0.78f, 8, BONUS, 0.3f)
                    }
                    else -> Unit
                }
            }
            VirajEvent.Slip -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                sound?.play(Sfx.SCREECH, volume = 0.7f, rate = 1.4f)
                texts += VirajText(resources.getString(R.string.viraj_oil), px, 0.62f, DANGER)
            }
            VirajEvent.Cone -> {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                sound?.play(Sfx.DROP, volume = 0.6f, rate = 1.5f)
                texts += VirajText(resources.getString(R.string.viraj_cone), px, 0.62f, DANGER)
                burst(px, 0.8f, 6, CONE, 0.3f)
                shake = maxOf(shake, 0.02f)
            }
            is VirajEvent.Milestone -> {
                sound?.play(Sfx.POP, volume = 0.5f, rate = 0.9f)
                texts += VirajText(resources.getString(R.string.viraj_km_fmt, event.km), 0.5f, 0.3f, MILESTONE, big = true)
            }
            VirajEvent.Over -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                sound?.play(Sfx.OVER)
            }
        }
    }

    /** Oyuncu aracının ekrandaki yatay konumu (kesir); araç ortada, yol kayar. */
    private fun playerScreenX(world: VirajWorld): Float = 0.5f

    private fun burst(x: Float, y: Float, count: Int, color: Color, speed: Float) {
        repeat(count) {
            particles += VirajParticle(
                x = x,
                y = y,
                vx = (rng.nextFloat() * 2f - 1f) * speed,
                vy = -(rng.nextFloat() * speed + 0.1f),
                size = 0.006f + rng.nextFloat() * 0.01f,
                color = color,
                maxLife = 0.3f + rng.nextFloat() * 0.4f,
            )
        }
    }

    fun update(dt: Float, world: VirajWorld) {
        if (dt <= 0f) return
        val speedPct = world.speed / VirajWorld.MAX_SPEED
        val curve = world.track.segment(world.playerSegmentIndex).curve
        bgOffset -= curve * speedPct * dt * 0.05f
        // Yol dışında toz.
        if (abs(world.playerX) > 1f && speedPct > 0.05f && rng.nextFloat() < speedPct) {
            particles += VirajParticle(
                x = 0.5f + (rng.nextFloat() - 0.5f) * 0.16f,
                y = 0.88f,
                vx = (rng.nextFloat() - 0.5f) * 0.2f,
                vy = -0.15f - rng.nextFloat() * 0.2f,
                size = 0.008f + rng.nextFloat() * 0.012f,
                color = DUST,
                maxLife = 0.4f + rng.nextFloat() * 0.3f,
            )
        }
        val it = particles.iterator()
        while (it.hasNext()) {
            val q = it.next()
            q.life -= dt
            if (q.life <= 0f) {
                it.remove()
                continue
            }
            q.vy += 0.5f * dt
            q.x += q.vx * dt
            q.y += q.vy * dt
        }
        val ti = texts.iterator()
        while (ti.hasNext()) {
            val t = ti.next()
            t.life -= dt
            t.y -= 0.08f * dt
            if (t.life <= 0f) ti.remove()
        }
        flash *= exp(-6f * dt)
        if (flash < 0.01f) flash = 0f
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
        val POINTS = Color(0xFFFDE68A)
        val BONUS = Color(0xFF4ADE80)
        val TURBO = Color(0xFF4DE1FF)
        val SHIELD = Color(0xFF93C5FD)
        val DANGER = Color(0xFFF87171)
        val CONE = Color(0xFFFB923C)
        val SPARK = Color(0xFFFCD34D)
        val DUST = Color(0xFFD6D3D1)
        val MILESTONE = Color(0xFFE4EAF5)

        @Suppress("unused")
        private fun clamp(v: Float, lo: Float, hi: Float) = min(hi, maxOf(lo, v))
    }
}
