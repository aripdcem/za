package com.za.games.dalgic

import kotlin.math.abs
import kotlin.math.hypot

/** Ölçüm pilotu: en yakın dalgıca gider, oksijen azalınca ya da yük dolunca yüzeye çıkar, önündeki düşmandan derinlik değiştirerek kaçar. */
object DalgicBots {

    class Pilot(val reaction: Float) {
        private var timer = 0f
        private var tx = 0.5f
        private var ty = DalgicWorld.SURFACE_LEVEL

        fun drive(w: DalgicWorld) {
            timer -= DalgicWorld.STEP
            if (timer <= 0f) {
                timer = reaction
                // Dalgıçsız yüzeye çıkmak da can götürür: dalgıç yoksa sonuna dek aranır.
                val surfaceNeeded = w.divers >= DalgicWorld.CAPACITY || (w.oxygen < 9f && w.divers > 0)
                if (surfaceNeeded) {
                    tx = w.subX
                    ty = DalgicWorld.SURFACE_LEVEL
                } else {
                    val diver = w.diverList.filter { it.alive && it.x in 0.02f..0.98f }.minByOrNull { hypot(it.x - w.subX, it.y - w.subY) }
                    if (diver != null) {
                        tx = diver.x + diver.dir * 0.08f
                        ty = diver.y
                    } else {
                        tx = 0.5f
                        ty = if (w.atSurface) DalgicWorld.laneY(1) else w.subY
                    }
                }
                // Kaçınma: aynı derinlikte yaklaşan düşman ya da torpido varsa bir şerit kay.
                val threat = w.foes.any { it.alive && abs(it.y - w.subY) < 0.09f && abs(it.x - w.subX) < 0.28f && (it.kind == FoeKind.MINE || (it.x - w.subX) * it.dir < 0f) } ||
                    w.torpedoes.any { !it.friendly && abs(it.y - w.subY) < 0.05f && abs(it.x - w.subX) < 0.3f && (it.x - w.subX) * it.vx < 0f }
                if (threat && !surfaceNeeded) {
                    val up = w.subY > DalgicWorld.laneY(1)
                    ty = (w.subY + if (up) -0.2f else 0.2f).coerceIn(DalgicWorld.SURFACE_LEVEL + 0.1f, DalgicWorld.FLOOR_Y - 0.06f)
                }
            }
            w.steerTo(tx, ty)
        }
    }

    data class Dive(val score: Int, val wave: Int, val rescued: Int, val frames: Int, val over: Boolean, val causes: List<LifeCause>)

    fun play(seed: Long, pilot: Pilot, maxFrames: Int = 60 * 240): Dive {
        val w = DalgicWorld(seed)
        var frames = 0
        val causes = ArrayList<LifeCause>()
        while (w.status == DalgicStatus.RUNNING && frames < maxFrames) {
            pilot.drive(w)
            for (e in w.step()) if (e is DalgicEvent.LifeLost) causes += e.cause
            frames++
        }
        return Dive(w.score, w.wave, w.rescued, frames, w.status == DalgicStatus.OVER, causes)
    }
}
