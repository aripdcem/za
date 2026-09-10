package com.za.games.sincap

import kotlin.math.abs

/**
 * Ölçüm pilotu: konduktan [Pilot.reaction] s sonra karar verir. Yılanlı dalı
 * asla seçmez; hedef basamaktan geçmekte olan bir karga varış anında dalın
 * üstünde olacaksa o yönü tehlikeli sayar ve bekler; kuru dal kırılmak
 * üzereyse ya da kedi yaklaştıysa beklemez (ama tepki süresini atlamaz: insan
 * gibi). Fındık ve iki basamaklık sıçrama tercih edilir.
 */
object SincapBots {

    class Pilot(val reaction: Float)

    class Run(val height: Int, val score: Int, val nuts: Int, val frames: Int, val cause: DeathCause?)

    private class Option(val side: Side, val level: Int, val kind: BranchKind)

    fun play(seed: Long, pilot: Pilot, maxFrames: Int = 60 * 300): Run {
        val w = SincapWorld(seed)
        var landedAt = -1f
        while (w.status == SincapStatus.RUNNING && w.frames < maxFrames) {
            w.step()
            if (w.status != SincapStatus.RUNNING) break
            if (w.jumping || w.falling) {
                landedAt = -1f
                continue
            }
            if (landedAt < 0f) landedAt = w.time
            if (w.time - landedAt >= pilot.reaction) {
                val s = decide(w)
                if (s != null) {
                    w.tap(s)
                    landedAt = -1f
                }
            }
        }
        return Run(w.height, w.score, w.nuts, w.frames, w.cause)
    }

    private fun urgent(w: SincapWorld): Boolean = (w.onDry && w.dryLeft < 0.3f) || w.catGap < 1.2f

    /** Seçilen yön; beklemek daha iyiyse null. */
    fun decide(w: SincapWorld): Side? {
        val options = ArrayList<Option>()
        for (s in Side.entries) {
            val j = w.target(s)
            if (j < 0) continue
            val k = w.levelAt(j).at(s)
            if (k == BranchKind.SNAKE) continue
            options += Option(s, j, k)
        }
        if (options.isEmpty()) return null
        val safe = options.filter { !danger(w, it) }
        val pool = if (safe.isNotEmpty()) safe else if (urgent(w)) options else return null
        return pool.maxByOrNull { value(it, w) }!!.side
    }

    private fun value(o: Option, w: SincapWorld): Float {
        var v = when (o.kind) {
            BranchKind.GOLD -> 100f
            BranchKind.NUT -> 20f
            BranchKind.NORMAL -> 5f
            else -> 0f
        }
        v += (o.level - w.level) * 3f
        return v
    }

    /** Varış anı ve sonraki 0,35 s içinde karga dalın üstünde olacak mı. */
    private fun danger(w: SincapWorld, o: Option): Boolean {
        val t0 = SincapWorld.JUMP_TIME * (1f + SincapWorld.JUMP_EXTRA * (o.level - w.level - 1))
        val t1 = t0 + 0.35f
        val sx = SincapWorld.sideX(o.side)
        val zone = SincapWorld.CROW_HIT + 0.05f
        for (c in w.crows) {
            if (c.level != o.level) continue
            val x0 = c.x + c.dir * c.speed * t0
            val x1 = c.x + c.dir * c.speed * t1
            if (abs(x0 - sx) < zone || abs(x1 - sx) < zone || (x0 - sx) * (x1 - sx) < 0f) return true
        }
        return false
    }
}
