package com.za.games.cici

import kotlin.math.hypot
import kotlin.math.max
import kotlin.random.Random

/**
 * Ölçüm pilotu: tepki süresinde bir en "değerli" ikrama gider (puan / uzaklık,
 * 0,4 s sonraki yerine nişan); [DANGER] içinde *yaklaşan* bir kedi ya da top
 * varsa yoluna dik kaçar (insan gibi: tersine değil yana), ikram yoksa
 * sıkılmamak için ortada dolanır.
 */
object CiciBots {

    const val DANGER = 0.26f

    class Pilot(val reaction: Float, seed: Long = 7L) {
        private val rng = Random(seed)
        private var timer = 0f
        private var tx = CiciWorld.WIDTH / 2f
        private var ty = CiciWorld.HEIGHT * 0.55f
        private var fleeing = false

        fun drive(w: CiciWorld) {
            timer -= CiciWorld.STEP
            if (timer <= 0f) {
                timer = reaction
                val cx = w.ciciX
                val cy = w.ciciY
                var tdx = 0f
                var tdy = 0f
                var tvx = 0f
                var tvy = 0f
                var tdist = 9f
                fun consider(x: Float, y: Float, vx: Float, vy: Float) {
                    val d = hypot(x - cx, y - cy)
                    // Yalnız yaklaşan tehdit: hız, Cici'ye doğru bir bileşen taşımalı.
                    val approaching = (cx - x) * vx + (cy - y) * vy > 0f
                    if (d < tdist && (approaching || d < DANGER * 0.5f)) {
                        tdist = d
                        tdx = x - cx
                        tdy = y - cy
                        tvx = vx
                        tvy = vy
                    }
                }
                for (c in w.cats) consider(c.x, c.y, c.vx, c.vy)
                for (b in w.balls) consider(b.x, b.y, b.vx, b.vy)
                // Kaçış başladıysa biraz daha uzağa dek sürer (tehdit ile av arasında salınmamak için).
                if (tdist < (if (fleeing) DANGER * 1.4f else DANGER)) {
                    fleeing = true
                    // Tehdidin yoluna dik kaç; iki yandan tehditten uzaklaşanı seç.
                    val sp = max(hypot(tvx, tvy), 1e-3f)
                    var px = -tvy / sp
                    var py = tvx / sp
                    if (px * -tdx + py * -tdy < 0f) {
                        px = -px
                        py = -py
                    }
                    val n = max(tdist, 1e-3f)
                    tx = (cx + px * 0.3f - tdx / n * 0.12f).coerceIn(0.08f, CiciWorld.WIDTH - 0.08f)
                    ty = (cy + py * 0.3f - tdy / n * 0.12f).coerceIn(0.08f, CiciWorld.HEIGHT - 0.08f)
                } else {
                    fleeing = false
                    // Kedi ya da topun dibindeki ikram avlanmaz.
                    fun clear(x: Float, y: Float): Boolean =
                        w.cats.none { hypot(it.x - x, it.y - y) < 0.3f } && w.balls.none { hypot(it.x - x, it.y - y) < 0.3f }
                    val treat = w.treats
                        .filter { it.alive && it.x in 0.03f..CiciWorld.WIDTH - 0.03f && it.y in 0.03f..CiciWorld.HEIGHT - 0.03f && clear(it.x, it.y) }
                        .maxByOrNull { it.kind.points / (hypot(it.x - cx, it.y - cy) + 0.2f) }
                    if (treat != null) {
                        tx = (treat.x + treat.vx * 0.4f).coerceIn(0.05f, CiciWorld.WIDTH - 0.05f)
                        ty = (treat.y + treat.vy * 0.4f).coerceIn(0.05f, CiciWorld.HEIGHT - 0.05f)
                    } else {
                        tx = 0.3f + rng.nextFloat() * 0.4f
                        ty = 0.5f + rng.nextFloat() * 0.6f
                    }
                }
            }
            w.steerTo(tx, ty)
        }
    }

    data class Flight(
        val score: Int,
        val seconds: Float,
        val over: Boolean,
        val hits: List<HazardKind>,
        val lostPoints: Int,
        val caught: Map<TreatKind, Int>,
        val bestStreak: Int,
    )

    fun play(seed: Long, pilot: Pilot, maxFrames: Int = 60 * 300): Flight {
        val w = CiciWorld(seed)
        var frames = 0
        val hits = ArrayList<HazardKind>()
        while (w.status == CiciStatus.RUNNING && frames < maxFrames) {
            pilot.drive(w)
            for (e in w.step()) if (e is CiciEvent.Hit) hits += e.by
            frames++
        }
        return Flight(
            score = w.score,
            seconds = frames / 60f,
            over = w.status == CiciStatus.OVER,
            hits = hits,
            lostPoints = w.lostPoints,
            caught = TreatKind.entries.associateWith { w.caughtOf(it) },
            bestStreak = w.bestStreak,
        )
    }
}
