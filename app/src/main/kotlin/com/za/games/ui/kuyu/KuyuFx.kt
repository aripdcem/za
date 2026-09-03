package com.za.games.ui.kuyu

import android.content.res.Resources
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.za.games.R
import com.za.games.kuyu.EnemyKind
import com.za.games.kuyu.KuyuEvent
import com.za.games.kuyu.KuyuWorld
import com.za.games.platform.Sfx
import com.za.games.platform.SoundPlayer
import kotlin.math.exp
import kotlin.random.Random

/** Kare biriminde konum/hız; yalnızca çizim için, simülasyonu etkilemez. */
internal class Particle(
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

internal class FloatingText(val text: String, var x: Float, var y: Float, val color: Color) {
    val maxLife = 1.2f
    var life = maxLife
}

/**
 * Motor olaylarını ses, titreşim, parçacık, sarsıntı ve uçan yazıya çevirir.
 * Rastgelelik salt görseldir; determinizm gereksinimi yoktur.
 */
internal class KuyuFx {
    val particles = ArrayList<Particle>()
    val texts = ArrayList<FloatingText>()
    var shakeX = 0f
        private set
    var shakeY = 0f
        private set
    var flash = 0f
        private set
    private var shake = 0f
    private var gemSoundCooldown = 0f
    private val rng = Random.Default

    val isBusy: Boolean get() = particles.isNotEmpty() || texts.isNotEmpty() || shake > 0.005f || flash > 0f

    fun reset() {
        particles.clear()
        texts.clear()
        shake = 0f
        shakeX = 0f
        shakeY = 0f
        flash = 0f
    }

    fun onEvent(
        event: KuyuEvent,
        world: KuyuWorld,
        sound: SoundPlayer?,
        haptics: HapticFeedback,
        resources: Resources,
    ) {
        val p = world.player
        when (event) {
            KuyuEvent.Jump -> burst(p.centerX, p.bottom, 3, DUST, 1.5f, up = true)
            KuyuEvent.Shot -> {
                sound?.play(Sfx.SHOT, volume = 0.45f, rate = 0.9f + rng.nextFloat() * 0.2f)
                burst(p.centerX, p.bottom + 0.1f, 2, MUZZLE, 2.5f, up = false)
                shake = maxOf(shake, 0.03f)
            }
            is KuyuEvent.Land -> if (event.fallRows >= 3) {
                sound?.play(Sfx.DROP, volume = 0.3f, rate = 1.2f)
                burst(p.centerX, p.bottom, 5, DUST, 2f, up = true)
                shake = maxOf(shake, 0.05f)
            }
            KuyuEvent.Stomp -> {
                sound?.play(Sfx.STOMP)
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                shake = maxOf(shake, 0.12f)
            }
            is KuyuEvent.Kill -> {
                if (event.kind == EnemyKind.BOSS) {
                    sound?.play(Sfx.BIG)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    burst(event.x, event.y, 24, ENEMY, 6f, up = true)
                    shake = maxOf(shake, 0.3f)
                } else {
                    if (!event.stomp) sound?.play(Sfx.POP, volume = 0.7f, rate = 0.8f)
                    burst(event.x, event.y, 10, ENEMY, 5f, up = true)
                }
            }
            is KuyuEvent.Chest -> {
                sound?.play(Sfx.BIG, volume = 0.8f)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                burst(event.col + 0.5f, event.row + 0.5f, 12, GEM, 4f, up = true)
                texts += FloatingText(
                    resources.getString(R.string.kuyu_chest, event.gems),
                    event.col + 0.5f,
                    event.row - 0.5f,
                    GEM,
                )
            }
            is KuyuEvent.GateOpen -> {
                sound?.play(Sfx.CLEAR)
                val gateRow = event.chunk * KuyuWorld.CHUNK_ROWS + KuyuWorld.CHUNK_ROWS - 0.5f
                burst(KuyuWorld.WIDTH / 2f, gateRow, 16, BLOCK, 4f, up = true)
                texts += FloatingText(resources.getString(R.string.kuyu_gate_open), p.centerX, p.y - 0.8f, PLAYER)
            }
            KuyuEvent.Shield -> {
                sound?.play(Sfx.POP, volume = 0.6f, rate = 0.5f)
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                texts += FloatingText(resources.getString(R.string.kuyu_shield_used), p.centerX, p.y - 0.6f, GEM)
            }
            is KuyuEvent.Offer -> Unit
            is KuyuEvent.BlockBreak -> {
                sound?.play(Sfx.POP, volume = 0.35f, rate = 0.6f)
                burst(event.col + 0.5f, event.row + 0.5f, 6, BLOCK, 3f, up = true)
            }
            is KuyuEvent.Gem -> {
                if (gemSoundCooldown <= 0f) {
                    sound?.play(Sfx.POP, volume = 0.35f, rate = 1.4f + (event.total % 8) * 0.05f)
                    gemSoundCooldown = 0.08f
                }
                burst(event.x, event.y, 2, GEM, 1.5f, up = true)
            }
            is KuyuEvent.Combo -> {
                sound?.play(Sfx.CLEAR, volume = 0.8f)
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                texts += FloatingText(
                    resources.getString(R.string.kuyu_combo_bonus_fmt, event.count, event.bonus),
                    p.centerX,
                    p.y - 0.6f,
                    GEM,
                )
            }
            KuyuEvent.Hurt -> {
                sound?.play(Sfx.DROP, volume = 0.8f, rate = 0.6f)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                flash = 1f
                shake = maxOf(shake, 0.25f)
            }
            KuyuEvent.Over -> {
                sound?.play(Sfx.OVER)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                shake = maxOf(shake, 0.3f)
                burst(p.centerX, p.centerY, 14, PLAYER, 5f, up = true)
            }
            is KuyuEvent.Area -> {
                sound?.play(Sfx.BIG, volume = 0.6f)
                texts += FloatingText(
                    resources.getString(R.string.kuyu_area_fmt, event.index + 1),
                    p.centerX,
                    p.y - 0.8f,
                    PLAYER,
                )
            }
        }
    }

    private fun burst(x: Float, y: Float, count: Int, color: Color, speed: Float, up: Boolean) {
        repeat(count) {
            val vx = (rng.nextFloat() * 2f - 1f) * speed
            val vy = if (up) -(rng.nextFloat() * speed + speed * 0.3f) else rng.nextFloat() * speed
            particles += Particle(x, y, vx, vy, 0.08f + rng.nextFloat() * 0.12f, color, 0.35f + rng.nextFloat() * 0.35f)
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
            q.vy += 22f * dt
            q.x += q.vx * dt
            q.y += q.vy * dt
        }
        val ti = texts.iterator()
        while (ti.hasNext()) {
            val t = ti.next()
            t.life -= dt
            t.y -= 0.9f * dt
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
        if (flash > 0f) flash = maxOf(0f, flash - 3f * dt)
        if (gemSoundCooldown > 0f) gemSoundCooldown -= dt
    }

    companion object {
        val PLAYER = Color(0xFFF1F5F9)
        val ENEMY = Color(0xFFF87171)
        val GEM = Color(0xFF4DE1FF)
        val DUST = Color(0xFF94A3B8)
        val BLOCK = Color(0xFF64748B)
        val MUZZLE = Color(0xFFFDE68A)
    }
}
