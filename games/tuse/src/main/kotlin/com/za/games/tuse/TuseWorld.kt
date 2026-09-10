package com.za.games.tuse

import kotlin.math.min
import kotlin.random.Random
import kotlin.math.floor

/**
 * Tuşe simülasyonu: dört şeritli tahta, her satırda bir karo. Sıradaki karo
 * en alttaki dokunulmamış satırdır; doğru şeride dokunmak karoyu vurur ve
 * parçanın sıradaki notasını çalar, yanlış şeride dokunmak koşuyu bitirir.
 * Yargı yalnızca şeride bakar (karonun ekrandaki yerine değil): şeritler
 * piyano tuşu gibidir.
 *
 * Klasik: [total] karo; süre ilk dokunuşla başlar, son vuruşla biter. Tahta
 * her vuruşta bir satır kayar ([SLIDE_SPEED], yalnızca görsel). Sonsuz: ilk
 * dokunuştan sonra tahta [speed] satır/s akar, vurulan karo sayısıyla
 * hızlanır; sıradaki karo dokunulmadan alttan çıkarsa koşu biter.
 *
 * Sabit 1/60 s adım; aynı tohum + aynı dokunuş dizisi = aynı koşu. Şeritler
 * tohumdan üretilir: art arda aynı şerit [REPEAT_CHANCE] olasılıkla.
 */
class TuseWorld(val seed: Long, val mode: TuseMode, val song: Song, val total: Int = CLASSIC_TILES) {

    companion object {
        const val STEP = 1f / 60f
        const val LANES = 4

        /** Ekranda görünen satır sayısı. */
        const val ROWS = 5
        const val CLASSIC_TILES = 50

        /** Klasik'te vuruştan sonra tahtanın kayma hızı (satır/s). */
        const val SLIDE_SPEED = 16f
        const val ARCADE_BASE = 3.2f

        /** Vurulan karo başına hız artışı (satır/s). */
        const val ARCADE_GAIN = 0.045f
        const val ARCADE_MAX = 11f
        const val REPEAT_CHANCE = 0.15f

        fun dailySeed(epochDay: Long): Long = mix(epochDay, 0x54, 0x55)

        fun mix(seed: Long, a: Int, b: Int = 0): Long {
            var z = seed xor (a.toLong() shl 32) xor b.toLong() xor -0x61C8864680B583EBL
            z = (z xor (z ushr 30)) * -0x40A7B892E31B1A47L
            z = (z xor (z ushr 27)) * -0x6B2FB644ECCEEE15L
            return z xor (z ushr 31)
        }
    }

    private val rng = Random(mix(seed, 0x54))
    private val lanes = ArrayList<Int>()

    var tapped = 0
        private set

    /** Tahtanın kaydığı satır sayısı (kesirli); karo i alt çizgiden (i − scroll) satır yukarıdadır. */
    var scroll = 0f
        private set
    var status = TuseStatus.RUNNING
        private set
    var started = false
        private set
    var missLane = -1
        private set
    var passedLane = -1
        private set
    var frames = 0
        private set
    private var startMs = 0L
    private var endMs = 0L

    val speed: Float get() = if (mode == TuseMode.ARCADE) min(ARCADE_MAX, ARCADE_BASE + ARCADE_GAIN * tapped) else 0f
    val isComplete: Boolean get() = status != TuseStatus.RUNNING

    /** [i]. karonun şeridi; gerektikçe üretilir. */
    fun lane(i: Int): Int {
        while (lanes.size <= i) {
            val prev = lanes.lastOrNull()
            val next = if (prev != null && rng.nextFloat() < REPEAT_CHANCE) {
                prev
            } else {
                var l = rng.nextInt(if (prev == null) LANES else LANES - 1)
                if (prev != null && l >= prev) l++
                l
            }
            lanes += next
        }
        return lanes[i]
    }

    /** [i]. karonun notası (MIDI); parça döner. */
    fun note(i: Int): Int = song.notes[i % song.notes.size]

    /** İlk dokunuştan bu yana geçen süre; bitince donar, başlamadıysa 0. */
    fun elapsedMs(nowMs: Long): Long = when {
        !started -> 0L
        status == TuseStatus.RUNNING -> nowMs - startMs
        else -> endMs - startMs
    }

    fun hud(nowMs: Long): TuseHud = TuseHud(
        tapped = tapped,
        total = if (mode == TuseMode.CLASSIC) total else -1,
        status = status,
        started = started,
        speed = speed,
        missLane = missLane,
        passedLane = passedLane,
        nextLane = lane(tapped),
        elapsedMs = elapsedMs(nowMs),
    )

    /** Şeride dokunuş; [nowMs] çağıranın saati (süre ölçümü için). */
    fun tap(lane: Int, nowMs: Long): List<TuseEvent> {
        val out = ArrayList<TuseEvent>(2)
        if (status != TuseStatus.RUNNING || lane !in 0 until LANES) return out
        if (!started) {
            started = true
            startMs = nowMs
            out += TuseEvent.Started
        }
        val expected = lane(tapped)
        if (lane == expected) {
            out += TuseEvent.Hit(tapped, lane, note(tapped))
            tapped++
            if (mode == TuseMode.CLASSIC && tapped >= total) {
                status = TuseStatus.DONE
                endMs = nowMs
                out += TuseEvent.Done
            }
        } else {
            status = TuseStatus.OVER
            missLane = lane
            endMs = nowMs
            out += TuseEvent.Miss(lane, expected)
        }
        return out
    }

    fun step(): List<TuseEvent> {
        val out = ArrayList<TuseEvent>(1)
        if (status != TuseStatus.RUNNING) return out
        frames++
        if (mode == TuseMode.CLASSIC) {
            if (scroll < tapped) scroll = min(tapped.toFloat(), scroll + SLIDE_SPEED * STEP)
        } else if (started) {
            scroll += speed * STEP
            if (scroll >= tapped + 1f) {
                status = TuseStatus.OVER
                passedLane = lane(tapped)
                endMs = startMs + frames * 1000L / 60L
                out += TuseEvent.Passed(passedLane)
            }
        }
        return out
    }

    /** Koşuyu yarıda bırakır (arka plana alınınca). */
    fun abandon(nowMs: Long) {
        if (status != TuseStatus.RUNNING) return
        status = TuseStatus.OVER
        endMs = nowMs
    }

    /** Görünür karoların ilk dizini (alt çizginin biraz altından). */
    fun firstVisible(): Int = maxOf(0, floor(scroll).toInt() - 1)
}
