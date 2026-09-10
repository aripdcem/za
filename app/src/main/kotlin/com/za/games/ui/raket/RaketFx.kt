package com.za.games.ui.raket

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.za.games.platform.Sfx
import com.za.games.platform.SoundPlayer
import com.za.games.raket.RaketEvent
import com.za.games.raket.RaketMode
import com.za.games.raket.RaketStatus
import com.za.games.raket.RaketWorld
import com.za.games.raket.Side

/**
 * Görsel ve işitsel geri bildirim: top izi, sayı sonrası yarı alan parlaması,
 * ilk servisteki ipucu ve olaylara bağlı sesler. Simülasyona yazmaz.
 */
class RaketFx {

    /** Son [TRAIL] kare için topun yeri (x, y çiftleri, halka tampon). */
    val trail = FloatArray(TRAIL * 2)
    var trailCount = 0
        private set
    private var trailHead = 0

    /** Sayıyı kaybeden taraf kısa süre kızarır. */
    var flashSide: Side? = null
        private set
    var flashTimer = 0f
        private set

    /** İlk servis ipucu (0..1). */
    var hintAlpha = 1f
        private set
    private var hintTime = 0f

    val isBusy: Boolean get() = flashTimer > 0f

    fun reset() {
        trailCount = 0
        trailHead = 0
        flashSide = null
        flashTimer = 0f
        hintAlpha = 1f
        hintTime = 0f
    }

    fun trailX(i: Int): Float = trail[((trailHead - 1 - i + TRAIL * 2) % TRAIL) * 2]
    fun trailY(i: Int): Float = trail[((trailHead - 1 - i + TRAIL * 2) % TRAIL) * 2 + 1]

    fun onEvent(event: RaketEvent, world: RaketWorld, sound: SoundPlayer?, haptics: HapticFeedback) {
        when (event) {
            is RaketEvent.Serve -> sound?.play(Sfx.POP, volume = 0.35f, rate = 0.7f)
            is RaketEvent.PaddleHit -> {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                val speed = ((event.speed - RaketWorld.BASE_SPEED) / (RaketWorld.MAX_SPEED - RaketWorld.BASE_SPEED)).coerceIn(0f, 1f)
                sound?.play(Sfx.POP, volume = 0.6f, rate = 0.9f + 0.6f * speed)
            }
            RaketEvent.SideWall -> sound?.play(Sfx.DROP, volume = 0.25f, rate = 1.5f)
            RaketEvent.BackWall -> sound?.play(Sfx.DROP, volume = 0.4f, rate = 1.1f)
            is RaketEvent.Point -> {
                flashSide = if (event.scorer == Side.BOTTOM) Side.TOP else Side.BOTTOM
                flashTimer = FLASH_TIME
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                sound?.play(Sfx.CLEAR, volume = 0.5f, rate = if (event.scorer == Side.BOTTOM) 1.2f else 0.8f)
                trailCount = 0
            }
            is RaketEvent.Over -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                val won = when (world.mode) {
                    RaketMode.SOLO -> event.winner == Side.BOTTOM
                    RaketMode.DUO -> true
                    RaketMode.WALL -> false
                }
                if (won) sound?.play(Sfx.BIG, volume = 0.9f, rate = 0.9f) else sound?.play(Sfx.OVER)
                if (world.mode == RaketMode.WALL) {
                    flashSide = Side.BOTTOM
                    flashTimer = FLASH_TIME
                }
            }
        }
    }

    fun update(dt: Float, world: RaketWorld) {
        if (flashTimer > 0f) flashTimer = (flashTimer - dt).coerceAtLeast(0f)
        if (world.status == RaketStatus.RALLY) {
            trail[trailHead * 2] = world.ball.x
            trail[trailHead * 2 + 1] = world.ball.y
            trailHead = (trailHead + 1) % TRAIL
            if (trailCount < TRAIL) trailCount++
        } else if (world.status == RaketStatus.SERVING) {
            trailCount = 0
        }
        if (world.hits > 0 || hintTime > HINT_TIME) {
            hintAlpha = (hintAlpha - dt * 2f).coerceAtLeast(0f)
        } else {
            hintTime += dt
        }
    }

    companion object {
        const val TRAIL = 8
        const val FLASH_TIME = 0.35f
        const val HINT_TIME = 3f
    }
}
