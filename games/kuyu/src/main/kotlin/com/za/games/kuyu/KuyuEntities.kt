package com.za.games.kuyu

import kotlin.random.Random

enum class KuyuStatus { RUNNING, OVER }

/** Bir adımlık oyuncu girdisi; tuşlar basılı tutulur. */
data class KuyuInput(
    val left: Boolean = false,
    val right: Boolean = false,
    val fire: Boolean = false,
) {
    companion object {
        val NONE = KuyuInput()
    }
}

/** Arayüz için değişmez özet; yalnızca değer değişince yayınlanır. */
data class KuyuHud(
    val score: Long,
    val depth: Int,
    val gems: Int,
    val hp: Int,
    val ammo: Int,
    val combo: Int,
    val bestCombo: Int,
    val area: Int,
    val status: KuyuStatus,
)

/** Simülasyonun bir adımda ürettiği, arayüzün ses/titreşim/parçacık için dinlediği olaylar. */
sealed interface KuyuEvent {
    data object Jump : KuyuEvent
    data object Shot : KuyuEvent
    data object Stomp : KuyuEvent
    data object Hurt : KuyuEvent
    data object Over : KuyuEvent

    /** Yere iniş; [fallRows] tepe noktasından inilen satır sayısı. */
    data class Land(val fallRows: Int) : KuyuEvent
    data class BlockBreak(val row: Int, val col: Int, val gems: Boolean) : KuyuEvent
    data class Kill(val kind: EnemyKind, val x: Float, val y: Float, val stomp: Boolean) : KuyuEvent
    data class Gem(val x: Float, val y: Float, val total: Int) : KuyuEvent
    data class Combo(val count: Int, val bonus: Int) : KuyuEvent
    data class Area(val index: Int) : KuyuEvent
}

/** Oyuncu; [x],[y] sol üst köşe, boyutlar [KuyuWorld.PLAYER_W]×[KuyuWorld.PLAYER_H]. */
class Player(var x: Float, var y: Float) {
    var vx = 0f
    var vy = 0f
    var hp = KuyuWorld.MAX_HP
    var ammo = KuyuWorld.AMMO
    var grounded = false
    var invincible = 0
    var shotCooldown = 0
    var coyote = 0
    var knock = 0
    var facing = 1
    var fireHeld = false
    var spread = 1

    /** Havadayken ulaşılan en yüksek nokta; iniş yüksekliği bundan ölçülür. */
    var apexY = y

    val bottom: Float get() = y + KuyuWorld.PLAYER_H
    val centerX: Float get() = x + KuyuWorld.PLAYER_W / 2f
    val centerY: Float get() = y + KuyuWorld.PLAYER_H / 2f
}

class Enemy(
    val kind: EnemyKind,
    var x: Float,
    var y: Float,
    val speedMul: Float,
    var dir: Int = 1,
    val minRow: Int = Int.MIN_VALUE,
    val maxRow: Int = Int.MAX_VALUE,
) {
    var hp = kind.hp
    var vy = 0f
    var t = 0f
    val baseY = y
    var alive = true
    var active = false

    /** Vurulunca birkaç kare parlar (yalnızca çizim için). */
    var hitFlash = 0

    val w: Float get() = kind.w
    val h: Float get() = kind.h
    val centerX: Float get() = x + w / 2f
    val centerY: Float get() = y + h / 2f

    companion object {
        fun from(spawn: KuyuSpawn, speedMul: Float, rng: Random): Enemy {
            val kind = spawn.kind
            val dir = if (rng.nextBoolean()) 1 else -1
            return when (kind) {
                EnemyKind.BLOB, EnemyKind.SPIKY ->
                    Enemy(kind, spawn.col + (1f - kind.w) / 2f, spawn.row + 1f - kind.h, speedMul, dir)
                EnemyKind.BAT ->
                    Enemy(kind, spawn.col + (1f - kind.w) / 2f, spawn.row + (1f - kind.h) / 2f, speedMul, dir)
                        .also { it.t = rng.nextFloat() * 6.2832f }
                EnemyKind.CRAWLER ->
                    Enemy(
                        kind,
                        spawn.col + (1f - kind.w) / 2f,
                        spawn.row + (1f - kind.h) / 2f,
                        speedMul,
                        dir,
                        minRow = spawn.row - 4,
                        maxRow = spawn.row + 4,
                    )
            }
        }
    }
}

/** Aşağı giden bot mermisi; [x],[y] nokta. */
class Bullet(var x: Float, var y: Float) {
    var traveled = 0f
}

/** Toplanabilir taş; [x],[y] merkez. */
class Gem(var x: Float, var y: Float, var vx: Float, var vy: Float) {
    var resting = false
}
