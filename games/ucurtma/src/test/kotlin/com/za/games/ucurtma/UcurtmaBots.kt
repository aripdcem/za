package com.za.games.ucurtma

/** Test ve ölçüm ortak yardımcıları: sütun geçilebilirliği ve basit bir pilot. */
object UcurtmaBots {

    /** [x] dünya sütununda uçurtma merkezinin giremeyeceği y aralıkları (rakipler hariç). */
    fun blockedAt(w: UcurtmaWorld, x: Float): List<Pair<Float, Float>> {
        val r = UcurtmaWorld.KITE_R
        val out = ArrayList<Pair<Float, Float>>()
        for (b in w.buildings) {
            if (x < b.x - r || x > b.right + r) continue
            out += (b.top - r) to UcurtmaWorld.GROUND
            if (b.chimneyX >= 0f && x >= b.chimneyX - r && x <= b.chimneyX + UcurtmaWorld.CHIMNEY_W + r) out += (b.chimneyTop - r) to UcurtmaWorld.GROUND
        }
        for (wr in w.wires) {
            if (x < wr.x1 - r || x > wr.x2 + r) continue
            out += (wr.y - r) to (wr.y + r)
        }
        return out
    }

    /** [lo, hi] içinde engellenmemiş dikey aralıklar: (başlangıç, uzunluk). */
    fun gaps(lo: Float, hi: Float, blocked: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
        val sorted = blocked.sortedBy { it.first }
        val out = ArrayList<Pair<Float, Float>>()
        var cursor = lo
        for ((a, b) in sorted) {
            if (a > cursor) out += cursor to (minOf(a, hi) - cursor)
            if (b > cursor) cursor = b
            if (cursor >= hi) break
        }
        if (hi > cursor) out += cursor to (hi - cursor)
        return out.filter { it.second > 0f }
    }

    /** [lo, hi] içinde engellenmemiş en büyük dikey aralık: (başlangıç, uzunluk). */
    fun largestGap(lo: Float, hi: Float, blocked: List<Pair<Float, Float>>): Pair<Float, Float> =
        gaps(lo, hi, blocked).maxByOrNull { it.second } ?: (lo to 0f)

    /**
     * İnsan benzeri pilot: [reaction] saniyede bir tüm ekrana bakar, ilerideki
     * ilk engel kümesinin (0,35 birim) boşluklarından uçurtmaya en yakın
     * yeterli olanı hedefler; yakın rakibin üstüne çıkar; engel yoksa orta
     * yüksekliğe süzülür. Sönümlü kontrol: 0,25 s sonraki konuma bakar.
     */
    class Pilot(val reaction: Float) {
        private var timer = 0f
        private var target = 0.6f

        fun drive(w: UcurtmaWorld) {
            timer -= UcurtmaWorld.STEP
            if (timer <= 0f) {
                timer = reaction
                val lo = UcurtmaWorld.CEIL + UcurtmaWorld.KITE_R
                val hi = UcurtmaWorld.GROUND - UcurtmaWorld.KITE_R
                var x = w.distance + UcurtmaWorld.KITE_X + 0.06f
                val end = w.distance + UcurtmaWorld.WIDTH + 0.05f
                var found = -1f
                while (x < end) {
                    if (blockedAt(w, x).isNotEmpty()) {
                        found = x
                        break
                    }
                    x += 0.03f
                }
                val rival = w.rivals.firstOrNull { it.alive && w.screenX(it.x) > UcurtmaWorld.KITE_X - 0.05f && w.screenX(it.x) < UcurtmaWorld.WIDTH + 0.1f }
                if (found < 0f && rival == null) {
                    target = 0.6f
                } else {
                    val blocked = ArrayList<Pair<Float, Float>>()
                    if (found >= 0f) {
                        var cx = found
                        while (cx < found + 0.35f) {
                            blocked += blockedAt(w, cx)
                            cx += 0.03f
                        }
                    }
                    if (rival != null) blocked += (rival.baseY - rival.amp - 0.14f) to hi
                    val all = gaps(lo, hi, blocked)
                    val wide = all.filter { it.second >= 0.2f }
                    val pick = wide.minByOrNull { kotlin.math.abs(it.first + it.second / 2f - w.kiteY) } ?: all.maxByOrNull { it.second }
                    target = if (pick != null) pick.first + pick.second / 2f else lo + 0.05f
                }
            }
            w.hold(w.kiteY + w.kiteVy * 0.25f > target)
        }
    }

    data class Flight(val meters: Int, val score: Int, val crash: CrashKind?, val ribbons: Int, val cuts: Int)

    fun fly(seed: Long, pilot: Pilot, maxFrames: Int = 60 * 300, gadget: Gadget? = null): Flight {
        val w = UcurtmaWorld(seed, gadget)
        var frames = 0
        while (w.status == UcurtmaStatus.RUNNING && frames < maxFrames) {
            pilot.drive(w)
            w.step()
            frames++
        }
        return Flight(w.meters, w.score, w.crash, w.ribbons, w.cuts)
    }
}
