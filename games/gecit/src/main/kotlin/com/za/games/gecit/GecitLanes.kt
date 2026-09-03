package com.za.games.gecit

enum class LaneKind { GRASS, ROAD, RAIL, RIVER }

/**
 * Şerit boyunca akan nesne: araç (yol) ya da kütük (nehir). [start] parkur
 * konumu, [len] hücre uzunluğu, [style] çizim çeşidi.
 */
class Mover(val start: Float, val len: Int, val style: Int)

enum class RailPhase { IDLE, WARNING, TRAIN }

/**
 * Tek şerit. Araç ve kütükler [Lane.TRACK] uzunluğunda döngüsel bir parkurda
 * akar; ekran hücresi = parkur konumu − [Lane.PAD]. Ray şeridinde tren
 * [period] döngüsüyle gelir: sessizlik → uyarı ışığı → tren geçişi.
 */
class Lane(
    val row: Int,
    val kind: LaneKind,
    val dir: Int = 1,
    val speed: Float = 0f,
    val movers: List<Mover> = emptyList(),
    val trees: BooleanArray = BooleanArray(GecitGen.WIDTH),
    val period: Float = 0f,
    initialTimer: Float = 0f,
) {
    var phase = 0f
        private set
    var timer = initialTimer
        private set

    /** Bu şeritte oyuncunun (yürüyerek) bulunabileceği sütunlar; üreteç doldurur. */
    var reach: BooleanArray = BooleanArray(GecitGen.WIDTH) { true }

    val railPhase: RailPhase
        get() = when {
            kind != LaneKind.RAIL -> RailPhase.IDLE
            timer >= period - TRAIN_TIME -> RailPhase.TRAIN
            timer >= period - TRAIN_TIME - WARNING_TIME -> RailPhase.WARNING
            else -> RailPhase.IDLE
        }

    /** Bir adım ilerletir; ray evresi değiştiyse yeni evreyi döndürür. */
    fun update(dt: Float): RailPhase? {
        phase += dir * speed * dt
        if (phase >= TRACK || phase <= -TRACK) phase = ((phase % TRACK) + TRACK) % TRACK
        if (kind != LaneKind.RAIL) return null
        val before = railPhase
        timer += dt
        if (timer >= period) timer -= period
        val after = railPhase
        return if (after != before) after else null
    }

    /** Nesnenin sol kenarının ekran hücresi. */
    fun moverX(m: Mover): Float = (((m.start + phase) % TRACK) + TRACK) % TRACK - PAD

    /** Trenin sol kenarı (ekran hücresi); tren yoksa null. */
    fun trainX(): Float? {
        if (railPhase != RailPhase.TRAIN) return null
        val t = timer - (period - TRAIN_TIME)
        return if (dir > 0) -TRAIN_LEN + TRAIN_SPEED * t else GecitGen.WIDTH - TRAIN_SPEED * t
    }

    /** Sol kenarı [x] olan oyuncu kutusu (0,25–0,75) bir araca ya da trene değiyor mu? */
    fun hits(x: Float): Boolean {
        val left = x + 0.25f
        val right = x + 0.75f
        return when (kind) {
            LaneKind.ROAD -> movers.any { m ->
                val mx = moverX(m)
                mx < right && mx + m.len > left
            }
            LaneKind.RAIL -> trainX()?.let { tx -> tx < right && tx + TRAIN_LEN > left } ?: false
            else -> false
        }
    }

    /** Nehirde [centerX] altındaki kütük; yoksa null (su). */
    fun logUnder(centerX: Float): Mover? {
        if (kind != LaneKind.RIVER) return null
        return movers.firstOrNull { m ->
            val mx = moverX(m)
            centerX >= mx - 0.1f && centerX <= mx + m.len + 0.1f
        }
    }

    companion object {
        const val TRACK = 24f
        const val PAD = 4f
        const val TRAIN_LEN = 14f
        const val TRAIN_SPEED = 28f
        const val WARNING_TIME = 1.2f
        val TRAIN_TIME: Float = (GecitGen.WIDTH + TRAIN_LEN) / TRAIN_SPEED
    }
}
