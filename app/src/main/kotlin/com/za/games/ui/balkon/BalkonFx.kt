package com.za.games.ui.balkon

import android.content.res.Resources
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.za.games.R
import com.za.games.balkon.BalkonEvent
import com.za.games.balkon.BalkonWorld
import com.za.games.balkon.TargetKind
import com.za.games.platform.Sfx
import com.za.games.platform.SoundPlayer
import kotlin.math.exp
import kotlin.random.Random

/** Dünya biriminde parçacık (x, y ekran genişliği/derinlik kesri); yalnızca çizim için. */
internal class BalkonParticle(
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

internal class BalkonText(val text: String, var x: Float, var y: Float, val color: Color, val big: Boolean = false) {
    val maxLife = if (big) 2.0f else 1.2f
    var life = maxLife
}

/** Yerde kalan iz (leke, kabuk yığını, su birikintisi); zamanla solar. */
internal class BalkonSplat(val x: Float, val y: Float, val mega: Boolean, val seed: Int) {
    val maxLife = 5f
    var life = maxLife
}

/** Motor olaylarını temaya göre ses, titreşim, parçacık, iz ve sarsıntıya çevirir. */
internal class BalkonFx {
    val particles = ArrayList<BalkonParticle>()
    val texts = ArrayList<BalkonText>()
    val splats = ArrayList<BalkonSplat>()
    var theme = BalkonTheme.CEKIRDEK
        private set
    var shakeX = 0f
        private set
    var shakeY = 0f
        private set
    private var shake = 0f
    private val rng = Random.Default

    val isBusy: Boolean
        get() = particles.isNotEmpty() || texts.isNotEmpty() || shake > 0.005f

    fun reset(theme: BalkonTheme) {
        this.theme = theme
        particles.clear()
        texts.clear()
        splats.clear()
        shake = 0f
        shakeX = 0f
        shakeY = 0f
    }

    /** Temanın sıçrama rengi. */
    fun splashColor(): Color = when (theme) {
        BalkonTheme.CEKIRDEK -> SHELL
        BalkonTheme.BALON -> WATER
        BalkonTheme.TUKURUK -> SLIME
    }

    fun onEvent(
        event: BalkonEvent,
        world: BalkonWorld,
        sound: SoundPlayer?,
        haptics: HapticFeedback,
        resources: Resources,
    ) {
        when (event) {
            is BalkonEvent.Throw -> {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                when (theme) {
                    BalkonTheme.CEKIRDEK -> sound?.play(Sfx.SHOT, volume = if (event.mega) 0.6f else 0.35f, rate = if (event.mega) 0.8f else 1.4f)
                    BalkonTheme.BALON -> sound?.play(Sfx.WHOOSH, volume = if (event.mega) 0.9f else 0.6f, rate = if (event.mega) 0.7f else 1f)
                    BalkonTheme.TUKURUK -> if (event.mega) sound?.play(Sfx.HOCK, volume = 0.9f) else sound?.play(Sfx.SPIT, volume = 0.6f)
                }
                if (event.mega) shake = maxOf(shake, 0.04f)
            }
            is BalkonEvent.Land -> {
                val count = if (event.mega) 22 else 8
                val speed = if (event.mega) 0.55f else 0.3f
                burst(event.x, event.y, count, splashColor(), speed)
                splats += BalkonSplat(event.x, event.y, event.mega, rng.nextInt())
                if (splats.size > 24) splats.removeAt(0)
                when (theme) {
                    BalkonTheme.CEKIRDEK -> sound?.play(if (event.mega) Sfx.STOMP else Sfx.DROP, volume = if (event.mega) 0.7f else 0.4f, rate = if (event.mega) 1.1f else 1.8f)
                    BalkonTheme.BALON -> sound?.play(Sfx.SPLASH, volume = if (event.mega) 1f else 0.7f, rate = if (event.mega) 0.7f else 1.05f)
                    BalkonTheme.TUKURUK -> sound?.play(Sfx.SPLAT, volume = if (event.mega) 1f else 0.7f, rate = if (event.mega) 0.75f else 1.1f)
                }
                if (event.mega) {
                    shake = maxOf(shake, 0.12f)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
            is BalkonEvent.Hit -> {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                val rate = when (event.kind) {
                    TargetKind.PIGEON -> 1.6f
                    TargetKind.CAT -> 1.3f
                    TargetKind.BALL -> 1.1f
                    TargetKind.CAR -> 0.7f
                    TargetKind.SIMIT -> 1.9f
                    else -> 1f
                }
                sound?.play(Sfx.POP, volume = 0.5f, rate = rate)
                texts += BalkonText("+${event.points}", event.x, event.y - 0.03f, POINTS)
            }
            BalkonEvent.Miss -> Unit
            is BalkonEvent.Forbidden -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                sound?.play(Sfx.HORN, volume = 0.7f, rate = 0.6f)
                texts += BalkonText(resources.getString(R.string.balkon_forbidden), event.x, event.y - 0.03f, DANGER)
                burst(event.x, event.y, 6, DANGER, 0.35f)
                shake = maxOf(shake, 0.15f)
            }
            BalkonEvent.MegaReady -> {
                sound?.play(Sfx.CLEAR, volume = 0.7f)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                texts += BalkonText(resources.getString(R.string.balkon_mega_ready_fmt, megaName(resources)), world.avatarX, 0.14f, MEGA)
            }
            is BalkonEvent.Bonus -> {
                sound?.play(Sfx.BIG, volume = 0.5f, rate = 1.3f)
                texts += BalkonText(resources.getString(R.string.balkon_bonus_fmt, event.seconds), event.x, event.y - 0.07f, BONUS)
            }
            is BalkonEvent.LevelClear -> {
                sound?.play(Sfx.BIG, volume = 0.8f)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                texts += BalkonText(resources.getString(R.string.balkon_level_clear_fmt, event.level, event.bonus), 0.5f, 0.5f, BONUS, big = true)
            }
            is BalkonEvent.LevelStart -> {
                sound?.play(Sfx.CLEAR, volume = 0.5f, rate = 0.9f)
                texts += BalkonText(resources.getString(R.string.balkon_level_fmt, event.level), 0.5f, 0.5f, POINTS, big = true)
            }
            BalkonEvent.Over -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                sound?.play(Sfx.OVER)
            }
        }
    }

    fun megaName(resources: Resources): String = resources.getString(
        when (theme) {
            BalkonTheme.CEKIRDEK -> R.string.balkon_mega_cekirdek
            BalkonTheme.BALON -> R.string.balkon_mega_balon
            BalkonTheme.TUKURUK -> R.string.balkon_mega_tukuruk
        },
    )

    private fun burst(x: Float, y: Float, count: Int, color: Color, speed: Float) {
        repeat(count) {
            particles += BalkonParticle(
                x = x,
                y = y,
                vx = (rng.nextFloat() * 2f - 1f) * speed,
                vy = -(rng.nextFloat() * speed * 0.8f + 0.05f),
                size = 0.006f + rng.nextFloat() * 0.01f,
                color = color,
                maxLife = 0.3f + rng.nextFloat() * 0.4f,
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
            q.vy += 1.4f * dt
            q.x += q.vx * dt
            q.y += q.vy * dt
        }
        val ti = texts.iterator()
        while (ti.hasNext()) {
            val t = ti.next()
            t.life -= dt
            if (!t.big) t.y -= 0.06f * dt
            if (t.life <= 0f) ti.remove()
        }
        val si = splats.iterator()
        while (si.hasNext()) {
            val s = si.next()
            s.life -= dt
            if (s.life <= 0f) si.remove()
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
        val SHELL = Color(0xFFE7D3A1)
        val WATER = Color(0xFF7DD3FC)
        val SLIME = Color(0xFF86EFAC)
        val POINTS = Color(0xFFFDE68A)
        val BONUS = Color(0xFF4ADE80)
        val DANGER = Color(0xFFF87171)
        val MEGA = Color(0xFF22D3EE)
    }
}
