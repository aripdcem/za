package com.za.games.ui.filo

import android.content.res.Resources
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.za.games.R
import com.za.games.filo.EnemyKind
import com.za.games.filo.FiloEvent
import com.za.games.filo.FiloStatus
import com.za.games.filo.FiloWorld
import com.za.games.filo.PowerKind
import com.za.games.platform.Sfx
import com.za.games.platform.SoundPlayer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** Oyun alanı biriminde parçacık (x ∈ 0..1, y ∈ 0..1.6); yalnızca çizim için. */
internal class FiloParticle(
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

internal class FiloText(val text: String, var x: Float, var y: Float, val color: Color, val big: Boolean = false) {
    val maxLife = if (big) 1.8f else 1.1f
    var life = maxLife
}

/** Arka plan yıldızı; ekran kesrinde (0..1 × 0..1) tutulur, aşağı akar. */
internal class Star(var x: Float, var y: Float, val size: Float, val speed: Float, val alpha: Float)

/** Motor olaylarını ses, titreşim, parçacık, uçan yazı, parlama ve sarsıntıya çevirir. */
internal class FiloFx {
    val particles = ArrayList<FiloParticle>()
    val texts = ArrayList<FiloText>()
    val stars: List<Star>
    var shakeX = 0f
        private set
    var shakeY = 0f
        private set

    /** Kısa tam ekran parlama (0..1) ve rengi. */
    var flash = 0f
        private set
    var flashColor = Color.White
        private set

    private var shake = 0f
    private val rng = Random.Default

    init {
        val r = Random(7)
        stars = List(70) {
            val depth = r.nextFloat()
            Star(
                x = r.nextFloat(),
                y = r.nextFloat(),
                size = 0.004f + depth * 0.008f,
                speed = 0.05f + depth * 0.22f,
                alpha = 0.25f + depth * 0.6f,
            )
        }
    }

    val isBusy: Boolean
        get() = particles.isNotEmpty() || texts.isNotEmpty() || shake > 0.005f || flash > 0.01f

    fun reset() {
        particles.clear()
        texts.clear()
        shake = 0f
        shakeX = 0f
        shakeY = 0f
        flash = 0f
    }

    fun onEvent(
        event: FiloEvent,
        world: FiloWorld,
        sound: SoundPlayer?,
        haptics: HapticFeedback,
        resources: Resources,
    ) {
        val px = world.playerX
        val py = world.playerY
        when (event) {
            FiloEvent.Shot -> sound?.play(Sfx.SHOT, volume = 0.16f, rate = 1.7f)
            is FiloEvent.EnemyHit -> {
                sound?.play(Sfx.POP, volume = 0.3f, rate = 1.6f)
                burst(event.x, event.y, 3, SPARK, 0.25f, 0.2f)
            }
            is FiloEvent.EnemyDown -> {
                when (event.kind) {
                    EnemyKind.BOSS -> {
                        burst(event.x, event.y, 46, BOSS_FIRE, 0.7f, 0.9f)
                        burst(event.x, event.y, 20, SPARK, 0.5f, 0.6f)
                    }
                    EnemyKind.TANK, EnemyKind.ASTEROID -> {
                        sound?.play(Sfx.STOMP, volume = 0.8f, rate = 0.9f)
                        burst(event.x, event.y, 16, if (event.kind == EnemyKind.TANK) TANK_FIRE else ROCK, 0.45f, 0.5f)
                        shake = maxOf(shake, 0.012f)
                    }
                    else -> {
                        sound?.play(Sfx.DROP, volume = 0.55f, rate = 1.3f)
                        burst(event.x, event.y, 10, if (event.kind == EnemyKind.WASP) WASP_FIRE else DRONE_FIRE, 0.35f, 0.4f)
                    }
                }
                texts += FiloText(resources.getString(R.string.filo_points_fmt, event.points), event.x, event.y - 0.03f, if (world.multiplier > 1) MULTI else POINTS)
            }
            is FiloEvent.PlayerHit -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                sound?.play(Sfx.SCREECH, volume = 0.8f, rate = 0.8f)
                burst(px, py, 18, DANGER, 0.5f, 0.5f)
                shake = maxOf(shake, 0.05f)
                flash = 1f
                flashColor = DANGER
                texts += FiloText(resources.getString(R.string.filo_hit), px, py - 0.12f, DANGER)
            }
            FiloEvent.ShieldUsed -> {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                sound?.play(Sfx.CLEAR, volume = 0.7f, rate = 1.2f)
                burst(px, py, 12, SHIELD, 0.4f, 0.4f)
                texts += FiloText(resources.getString(R.string.filo_shield_used), px, py - 0.12f, SHIELD)
            }
            is FiloEvent.PowerUp -> {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                sound?.play(Sfx.CLEAR, volume = 0.7f, rate = if (event.kind == PowerKind.SCORE) 1.4f else 1f)
                val (label, color) = when (event.kind) {
                    PowerKind.WEAPON -> resources.getString(R.string.filo_weapon_up) to WEAPON
                    PowerKind.SHIELD -> resources.getString(R.string.filo_shield) to SHIELD
                    PowerKind.BOMB -> resources.getString(R.string.filo_bomb_pickup) to BOMB
                    PowerKind.SCORE -> resources.getString(R.string.filo_points_fmt, FiloWorld.SCORE_GIFT * world.multiplier) to POINTS
                }
                texts += FiloText(label, px, py - 0.12f, color)
                burst(px, py, 8, color, 0.3f, 0.35f)
            }
            FiloEvent.Bomb -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                sound?.play(Sfx.BIG, volume = 0.9f, rate = 0.5f)
                ring(px, py, 28, BOMB, 1.1f)
                shake = maxOf(shake, 0.06f)
                flash = 0.8f
                flashColor = Color.White
            }
            is FiloEvent.WaveStart -> {
                if (event.boss) {
                    sound?.play(Sfx.HORN, volume = 0.8f, rate = 0.7f)
                    texts += FiloText(resources.getString(R.string.filo_boss_incoming), 0.5f, 0.55f, DANGER, big = true)
                } else {
                    sound?.play(Sfx.POP, volume = 0.5f, rate = 0.8f)
                    texts += FiloText(resources.getString(R.string.filo_wave_start_fmt, event.wave), 0.5f, 0.55f, TITLE, big = true)
                }
            }
            is FiloEvent.WaveClear -> {
                sound?.play(Sfx.BIG, volume = 0.6f, rate = 1.2f)
                texts += FiloText(resources.getString(R.string.filo_wave_clear_fmt, event.bonus), 0.5f, 0.65f, BONUS, big = true)
            }
            is FiloEvent.BossDown -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                sound?.play(Sfx.BIG, volume = 1f, rate = 0.7f)
                shake = maxOf(shake, 0.08f)
                flash = 0.6f
                flashColor = Color.White
                texts += FiloText(resources.getString(R.string.filo_boss_down), 0.5f, 0.5f, BONUS, big = true)
            }
            FiloEvent.Over -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                sound?.play(Sfx.OVER)
            }
        }
    }

    private fun burst(x: Float, y: Float, count: Int, color: Color, speed: Float, life: Float) {
        repeat(count) {
            val a = rng.nextFloat() * 2f * PI.toFloat()
            val v = (0.3f + rng.nextFloat() * 0.7f) * speed
            particles += FiloParticle(
                x = x,
                y = y,
                vx = cos(a) * v,
                vy = sin(a) * v,
                size = 0.005f + rng.nextFloat() * 0.01f,
                color = color,
                maxLife = life * (0.5f + rng.nextFloat() * 0.5f),
            )
        }
    }

    /** Eşit aralıklı halka: bomba dalgası. */
    private fun ring(x: Float, y: Float, count: Int, color: Color, speed: Float) {
        for (i in 0 until count) {
            val a = i * 2f * PI.toFloat() / count
            particles += FiloParticle(x, y, cos(a) * speed, sin(a) * speed, 0.01f, color, 0.6f)
        }
    }

    fun update(dt: Float, world: FiloWorld) {
        if (dt <= 0f) return
        for (s in stars) {
            s.y += s.speed * dt
            if (s.y > 1f) {
                s.y -= 1f
                s.x = rng.nextFloat()
            }
        }
        // Motor alevi kıvılcımları.
        if (world.status == FiloStatus.RUNNING && rng.nextFloat() < 0.5f) {
            particles += FiloParticle(
                x = world.playerX + (rng.nextFloat() - 0.5f) * 0.012f,
                y = world.playerY + FiloWorld.PLAYER_RADIUS * 0.9f,
                vx = (rng.nextFloat() - 0.5f) * 0.05f,
                vy = 0.25f + rng.nextFloat() * 0.2f,
                size = 0.004f + rng.nextFloat() * 0.005f,
                color = if (rng.nextBoolean()) FLAME else FLAME_CORE,
                maxLife = 0.18f + rng.nextFloat() * 0.12f,
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
            q.vx *= exp(-2f * dt)
            q.vy *= exp(-2f * dt)
            q.x += q.vx * dt
            q.y += q.vy * dt
        }
        val ti = texts.iterator()
        while (ti.hasNext()) {
            val t = ti.next()
            t.life -= dt
            t.y -= 0.06f * dt
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
        val MULTI = Color(0xFFFB923C)
        val BONUS = Color(0xFF4ADE80)
        val TITLE = Color(0xFFE4EAF5)
        val WEAPON = Color(0xFF22D3EE)
        val SHIELD = Color(0xFF93C5FD)
        val BOMB = Color(0xFFFB923C)
        val DANGER = Color(0xFFF87171)
        val SPARK = Color(0xFFFEF3C7)
        val DRONE_FIRE = Color(0xFFF87171)
        val WASP_FIRE = Color(0xFFFBBF24)
        val TANK_FIRE = Color(0xFF94A3B8)
        val ROCK = Color(0xFFA8A29E)
        val BOSS_FIRE = Color(0xFFC084FC)
        val FLAME = Color(0xFFFB923C)
        val FLAME_CORE = Color(0xFFFDE68A)
    }
}
