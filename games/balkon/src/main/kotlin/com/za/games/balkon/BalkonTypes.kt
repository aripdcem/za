package com.za.games.balkon

enum class BalkonStatus { RUNNING, CLEARED, OVER }

/**
 * Sokak şeritleri: [depth] 0 balkon kenarı (üst), 1 ekranın altı. Yol
 * şeritlerinde trafik yönü sabittir; kaldırımlarda yürüyenler iki yöne gider.
 */
enum class Lane(val depth: Float, val trafficDir: Int) {
    NEAR_WALK(0.30f, 0),
    ROAD_A(0.50f, 1),
    ROAD_B(0.64f, -1),
    FAR_WALK(0.84f, 0),
    ;

    val isRoad: Boolean get() = trafficDir != 0
}

/**
 * Hedef türleri. [points] isabet puanı (yasaklarda ceza olarak eksi), [radius]
 * ekran genişliği biriminde gövde yarıçapı, hızlar ekran genişliği/s.
 * [forbidden] hedefler vurulunca ceza ve kısa bir donma getirir; [bonusTime]
 * simitçi gibi ödül hedeflerinde saniye cinsinden ek süre.
 */
enum class TargetKind(
    val points: Int,
    val radius: Float,
    val minSpeed: Float,
    val maxSpeed: Float,
    val forbidden: Boolean = false,
    val vehicle: Boolean = false,
    val bonusTime: Int = 0,
) {
    PIGEON(15, 0.030f, 0.06f, 0.12f),
    CAT(20, 0.034f, 0.09f, 0.16f),
    BALL(10, 0.026f, 0.16f, 0.26f),
    BIKE(25, 0.038f, 0.20f, 0.30f, vehicle = true),
    SCOOTER(40, 0.038f, 0.32f, 0.46f, vehicle = true),
    CAR(30, 0.070f, 0.14f, 0.24f, vehicle = true),
    SIMIT(100, 0.044f, 0.05f, 0.08f, bonusTime = 5),
    JANITOR(-50, 0.044f, 0.05f, 0.09f, forbidden = true),
    NEIGHBOR(-50, 0.044f, 0.04f, 0.08f, forbidden = true),
}

/** Sokaktaki hedef; [x] merkez, [dir] +1 sağa, [y] şerit derinliği artı küçük salınım. */
class Target(
    val id: Int,
    val kind: TargetKind,
    val lane: Lane,
    var x: Float,
    val dir: Int,
    val speed: Float,
    val phase: Float,
) {
    var y: Float = lane.depth
    var age = 0f

    /** Vuruldu mu; vurulan hedef kısa bir tepki animasyonundan sonra silinir. */
    var hit = false
    var hitAge = 0f

    /** Duraklama süresi (kediler arada durur). */
    var pause = 0f

    val alive: Boolean get() = !hit
}

/**
 * Havadaki atış: [fromX] balkondaki çıkış noktası, ([toX], [toY]) nişan
 * alınan yer, [drift] rüzgârın uçuş boyunca eklediği kayma. [t] 0..1 ilerler.
 */
class Shot(
    val id: Int,
    val fromX: Float,
    val toX: Float,
    val toY: Float,
    val duration: Float,
    val mega: Boolean,
    val drift: Float,
) {
    var t = 0f

    /** Gerçek iniş noktası: nişan artı rüzgâr kayması. */
    val landX: Float get() = toX + drift

    /** Yerdeki iz (gölge) konumu: çıkıştan inişe doğru düz çizgi. */
    val groundX: Float get() = fromX + (landX - fromX) * t
    val groundY: Float get() = toY * t
}

sealed interface BalkonEvent {
    data class Throw(val mega: Boolean) : BalkonEvent
    data class Land(val x: Float, val y: Float, val mega: Boolean, val hits: Int) : BalkonEvent
    data class Hit(val kind: TargetKind, val x: Float, val y: Float, val points: Int) : BalkonEvent
    data object Miss : BalkonEvent
    data class Forbidden(val kind: TargetKind, val x: Float, val y: Float) : BalkonEvent
    data object MegaReady : BalkonEvent
    data class Bonus(val seconds: Int, val x: Float, val y: Float) : BalkonEvent
    data class LevelClear(val level: Int, val bonus: Int) : BalkonEvent
    data class LevelStart(val level: Int) : BalkonEvent
    data object Over : BalkonEvent
}

/** Arayüz için değişmez özet. */
data class BalkonHud(
    val score: Long,
    val level: Int,
    val hits: Int,
    val required: Int,
    val seconds: Int,
    val timeFraction: Float,
    val wind: Float,
    val combo: Int,
    val charges: Int,
    val stunned: Boolean,
    val status: BalkonStatus,
)

/** splitmix64 tabanlı deterministik üreteç. */
class Rng(seed: Long) {
    private var state = seed

    fun nextLong(): Long {
        state += -0x61C8864680B583EBL
        var z = state
        z = (z xor (z ushr 30)) * -0x40A7B892E31B1A47L
        z = (z xor (z ushr 27)) * -0x6B2FB644ECCEEE15L
        return z xor (z ushr 31)
    }

    /** [0, 1) aralığında. */
    fun nextFloat(): Float = (nextLong() ushr 40).toFloat() / (1L shl 24).toFloat()

    fun nextInt(bound: Int): Int = ((nextLong() ushr 33) % bound).toInt()

    fun range(lo: Float, hi: Float): Float = lo + (hi - lo) * nextFloat()

    fun chance(p: Float): Boolean = nextFloat() < p

    companion object {
        fun mix(a: Long, b: Long, c: Long): Long {
            var z = a * -0x61C8864680B583EBL + b * 0x9E3779B97F4A7C15uL.toLong() + c
            z = (z xor (z ushr 30)) * -0x40A7B892E31B1A47L
            z = (z xor (z ushr 27)) * -0x6B2FB644ECCEEE15L
            return z xor (z ushr 31)
        }
    }
}
