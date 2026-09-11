package com.za.games.cekirge

import kotlin.math.abs

/**
 * Ölçüm pilotu: [Pilot.reaction] s'de bir en yakın sütunun alt çekirgesini
 * hedefler (fıskırtmanın varış anındaki x'i tahmin eder), başparmak hızıyla
 * ([Pilot.speed] birim/s) oraya kayar, hizaya gelince ve fıskırtma boşsa
 * sıkar. Yaklaşan tükürükten kaçış hedeflemeyi bastırır. Kraliçeyi
 * hedeflemez (rastgele vurabilir).
 */
object CekirgeBots {

    class Pilot(val reaction: Float, val speed: Float, val aim: Float)

    class Run(val score: Int, val wave: Int, val kills: Int, val frames: Int, val invaded: Boolean, val over: Boolean)

    fun play(seed: Long, pilot: Pilot, maxFrames: Int = 60 * 240): Run {
        val w = CekirgeWorld(seed)
        var decideAt = 0f
        var targetX = w.farmerX
        var kills = 0
        while (w.status == CekirgeStatus.RUNNING && w.frames < maxFrames) {
            if (w.time >= decideAt) {
                decideAt = w.time + pilot.reaction
                targetX = aim(w)
            }
            val goal = safeGoal(w, targetX)
            val dx = (goal - w.farmerX).coerceIn(-pilot.speed * CekirgeWorld.STEP, pilot.speed * CekirgeWorld.STEP)
            w.moveBy(dx)
            if (goal == targetX && abs(w.farmerX - targetX) <= pilot.aim && w.shotReady) w.fire()
            val ev = w.step()
            for (e in ev) if (e is CekirgeEvent.BugHit) kills++
        }
        return Run(w.score, w.wave, kills, w.frames, w.invaded, w.status == CekirgeStatus.OVER)
    }

    /** x, 1 s içinde çiftçiye varacak bir tükürüğün yolunda mı. */
    fun threatened(w: CekirgeWorld, x: Float): Boolean {
        for (sp in w.spits) {
            val t = (CekirgeWorld.FARMER_Y - sp.y) / sp.speed
            if (t < 0f || t > 1f) continue
            if (abs(sp.x - x) < CekirgeWorld.FARMER_HALF_W + CekirgeWorld.SPIT_HALF + 0.03f) return true
        }
        return false
    }

    /**
     * Gidilecek x: hedef güvenliyse hedef; çiftçinin bulunduğu yer tehlikedeyse en
     * yakın güvenli yer; hedef tehlikede ama duruş güvenliyse yerinde kal.
     */
    fun safeGoal(w: CekirgeWorld, targetX: Float): Float {
        if (threatened(w, w.farmerX)) {
            var d = 0.03f
            while (d < 0.5f) {
                val l = w.farmerX - d
                val r = w.farmerX + d
                val lOk = l >= CekirgeWorld.FARMER_MIN_X && !threatened(w, l)
                val rOk = r <= CekirgeWorld.FARMER_MAX_X && !threatened(w, r)
                if (lOk && rOk) return if (abs(l - targetX) <= abs(r - targetX)) l else r
                if (lOk) return l
                if (rOk) return r
                d += 0.03f
            }
            return w.farmerX
        }
        return if (threatened(w, targetX)) w.farmerX else targetX
    }

    /** Hedef x: en yakın sütunun alt çekirgesinin fıskırtma varış anındaki yeri (yön değişimi yok sayılır). */
    fun aim(w: CekirgeWorld): Float {
        val alive = w.bugs.filter { it.alive }
        if (alive.isEmpty()) return w.farmerX
        val bottoms = alive.groupBy { it.col }.values.map { col -> col.maxBy { it.row } }
        val target = bottoms.minBy { abs(w.bugX(it) - w.farmerX) }
        val t = (CekirgeWorld.FARMER_Y - w.bugY(target)) / CekirgeWorld.SHOT_SPEED
        return (w.bugX(target) + w.swarmDir * w.swarmSpeed() * t).coerceIn(CekirgeWorld.FARMER_MIN_X, CekirgeWorld.FARMER_MAX_X)
    }
}
