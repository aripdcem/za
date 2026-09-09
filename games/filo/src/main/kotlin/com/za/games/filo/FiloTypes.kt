package com.za.games.filo

enum class FiloStatus { RUNNING, OVER }

/** Düşman türleri: can, yarıçap (oyun birimi) ve taban puan. */
enum class EnemyKind(val hp: Int, val radius: Float, val points: Int) {
    DRONE(1, 0.035f, 100),
    WASP(2, 0.036f, 150),
    TANK(4, 0.05f, 300),
    ASTEROID(3, 0.045f, 50),
    BOSS(1, 0.11f, 2500),
}

/** Hareket deseni. */
enum class Pattern { DIVE, SINE, SWEEP, RING, DRIFT, BOSS }

enum class PowerKind { WEAPON, SHIELD, BOMB, SCORE }

class Enemy(
    val id: Int,
    val kind: EnemyKind,
    val pattern: Pattern,
    /** Desenin merkezi ya da başlangıç sütunu. */
    val cx: Float,
    val amp: Float,
    val speed: Float,
    val phase: Float,
    /** Süpürmede yön: 1 soldan sağa, −1 sağdan sola. */
    val dir: Int,
    hp: Int,
    /** Ateş aralığı (s); 0 = ateş etmez. */
    val fireEvery: Float,
) {
    var hp = hp
    val maxHp = hp
    var x = cx
    var y = -0.1f
    var t = 0f
    var fireTimer = fireEvery * 0.6f
    var alive = true

    /** Desen aşaması (halka: iniş/çember/çıkış; patron: giriş/salınım). */
    var stage = 0

    /** Vuruş parlaması (s). */
    var flash = 0f
}

class Bullet(var x: Float, var y: Float, var vx: Float, var vy: Float, val radius: Float)

class Power(var x: Float, var y: Float, val kind: PowerKind)

sealed interface FiloEvent {
    data object Shot : FiloEvent
    data class EnemyHit(val x: Float, val y: Float) : FiloEvent
    data class EnemyDown(val kind: EnemyKind, val x: Float, val y: Float, val points: Long) : FiloEvent
    data class PlayerHit(val livesLeft: Int) : FiloEvent
    data object ShieldUsed : FiloEvent
    data class PowerUp(val kind: PowerKind) : FiloEvent
    data object Bomb : FiloEvent
    data class WaveStart(val wave: Int, val boss: Boolean) : FiloEvent
    data class WaveClear(val wave: Int, val bonus: Long) : FiloEvent
    data class BossDown(val points: Long) : FiloEvent
    data object Over : FiloEvent
}

/** Arayüz için değişmez özet. */
data class FiloHud(
    val score: Long,
    val lives: Int,
    val bombs: Int,
    val wave: Int,
    val chain: Int,
    val multiplier: Int,
    val weapon: Int,
    val shield: Boolean,
    val invulnerable: Boolean,
    /** Patron canı kesri; patron yoksa −1. */
    val bossHp: Float,
    /** Dalgada ilerleme (0..1). */
    val waveProgress: Float,
    val kills: Int,
    val status: FiloStatus,
)
