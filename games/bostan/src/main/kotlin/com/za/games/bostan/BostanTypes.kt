package com.za.games.bostan

enum class BostanStatus { RUNNING, WON, LOST }

enum class BostanDifficulty(val waves: Int, val budget: Float, val startWater: Int) {
    KOLAY(6, 4f, 150),
    ORTA(8, 6f, 125),
    ZOR(10, 8f, 100),
}

/**
 * Savunmalar. [cost] su, [hp] can, [cooldown] kartın yeniden dolma süresi (s).
 * Kuyu su üretir, fıskiye şeridine jet atar, korkuluk yolu keser, kovan
 * çevresine vurur, tuzak kurulduktan sonra üstüne basana patlar.
 */
enum class DefenderKind(val cost: Int, val hp: Float, val cooldown: Float) {
    KUYU(50, 6f, 5f),
    FISKIYE(100, 6f, 5f),
    KORKULUK(50, 24f, 12f),
    KOVAN(125, 6f, 10f),
    TUZAK(75, 4f, 15f),
}

/** Saldırganlar: [hp] can, [speed] hücre/s, [bite] savunmaya saniyede hasar, [points] puan, [cost] dalga bütçesindeki bedeli. */
enum class EnemyKind(val hp: Float, val speed: Float, val bite: Float, val points: Int, val cost: Float) {
    KARGA(5f, 0.18f, 1f, 10, 1f),
    TAVSAN(4f, 0.36f, 0.6f, 10, 1.5f),
    KECI(12f, 0.14f, 1.5f, 20, 3f),
    DOMUZ(24f, 0.11f, 2f, 40, 6f),
    AYI(60f, 0.08f, 3f, 100, 15f),
}

/** Dalga içinde bir doğum: dalga başlangıcından [at] saniye sonra [lane] şeridine [kind]. */
data class Spawn(val at: Float, val lane: Int, val kind: EnemyKind)

data class Wave(val spawns: List<Spawn>, val big: Boolean) {
    val duration: Float get() = spawns.maxOfOrNull { it.at } ?: 0f
}

/**
 * Üretilmiş seviye. [scale] üreticinin bütçeyi uzman kaybedince küçülttüğü
 * çarpan (1 = küçültülmedi); [expertLives] ve [expertScore] uzman
 * politikasının aynı seviyedeki sonucu.
 */
class BostanLevel(
    val seed: Long,
    val difficulty: BostanDifficulty,
    val waves: List<Wave>,
    val scale: Float,
    val expertLives: Int,
    val expertScore: Int,
) {
    val startWater: Int get() = difficulty.startWater
    val enemyCount: Int get() = waves.sumOf { it.spawns.size }
}

class Defender(val kind: DefenderKind, val lane: Int, val row: Int) {
    var hp = kind.hp
    var timer = 0f

    /** Tuzak kuruldu mu (3 s sonra). */
    var armed = false

    /** Vuruş parlaması (s). */
    var flash = 0f
    val alive: Boolean get() = hp > 0f
}

class Enemy(val id: Int, val kind: EnemyKind, val lane: Int) {
    /** Satır koordinatı: −1 doğum, [BostanState.ROWS] kulübe. */
    var y = -1f
    var hp = kind.hp
    var biting = false
    var flash = 0f
    val alive: Boolean get() = hp > 0f
    val front: Float get() = y + 0.45f
}

class Jet(val lane: Int, var y: Float)

class Drop(val id: Int, val lane: Int, val row: Int) {
    var ttl = BostanState.DROP_TTL
    val age: Float get() = BostanState.DROP_TTL - ttl
}

sealed interface BostanEvent {
    data class Placed(val kind: DefenderKind, val lane: Int, val row: Int) : BostanEvent
    data class Removed(val kind: DefenderKind, val lane: Int, val row: Int) : BostanEvent
    data class Water(val amount: Int, val lane: Int, val row: Int) : BostanEvent
    data class DropFell(val lane: Int, val row: Int) : BostanEvent
    data class EnemySpawned(val kind: EnemyKind, val lane: Int) : BostanEvent
    data class EnemyHit(val lane: Int, val y: Float) : BostanEvent
    data class EnemyDown(val kind: EnemyKind, val lane: Int, val y: Float, val points: Int) : BostanEvent
    data class DefenderDown(val kind: DefenderKind, val lane: Int, val row: Int) : BostanEvent
    data class TrapBlast(val lane: Int, val row: Int) : BostanEvent
    data class LifeLost(val lives: Int, val lane: Int) : BostanEvent
    data class WaveStart(val index: Int, val big: Boolean) : BostanEvent
    data class WaveClear(val index: Int) : BostanEvent
    data object Won : BostanEvent
    data object Lost : BostanEvent
}

/** Arayüz için değişmez özet. */
data class BostanHud(
    val water: Int,
    val lives: Int,
    /** Süren dalga (1 tabanlı); henüz başlamadıysa 0. */
    val wave: Int,
    val totalWaves: Int,
    /** Sürmekte olan dalganın doğum ilerlemesi (0..1). */
    val waveProgress: Float,
    /** Sıradaki dalgaya kalan süre (s); son dalgada ya da bitmişse 0. */
    val nextWaveIn: Float,
    val kills: Int,
    val score: Int,
    val status: BostanStatus,
    /** Kart bekleme payı (0 hazır .. 1 yeni basıldı), [DefenderKind] sırasıyla. */
    val cooldowns: List<Float>,
    val enemiesAlive: Int,
)
