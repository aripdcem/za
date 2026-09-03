package com.za.games.kuyu

import kotlin.math.min
import kotlin.random.Random

enum class KuyuStatus { RUNNING, CHOOSING, OVER }

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

/** Bölge arası yükseltmeler; [stackable] olanlar birden çok kez alınabilir. */
enum class Upgrade(val stackable: Boolean) {
    /** +2 şarjör. */
    AMMO(true),

    /** +1 azami can, 1 can iyileşir. */
    HEART(true),

    /** Her atışta üç mermi. */
    SPREAD(false),

    /** Atış aralığı 6 → 4 kare: daha sık ateş, daha iyi askı. */
    RAPID(false),

    /** Mermi menzili 9 → 14 kare. */
    RANGE(false),

    /** Taş mıknatısı iki kat menzil. */
    MAGNET(false),

    /** Kombo bonusu iki kat. */
    COMBO(false),

    /** Her düşman +1 taş bırakır. */
    GREED(false),

    /** Her bölgede bir vuruşu engeller. */
    SHIELD(false),

    /** Zıplama yüzde 20 yüksek. */
    JUMP(false),
}

enum class ShopItem { HEAL, AMMO_UP, LIFE }

class ShopEntry(val item: ShopItem, val price: Int) {
    var bought = false
        internal set
}

/** Bölge başında sunulan seçim: bir yükseltme (ücretsiz) ve taş karşılığı dükkân. */
class KuyuOffer(val area: Int, val upgrades: List<Upgrade>, val shop: List<ShopEntry>) {
    var chosen: Upgrade? = null
        internal set
}

/** Koşu boyunca biriken kalıcı etkiler; oyun bitince sıfırlanır (yeni dünya). */
class KuyuPerks {
    var maxAmmo = KuyuWorld.AMMO
    var maxHp = KuyuWorld.MAX_HP
    var spread = false
    var shotInterval = KuyuWorld.SHOT_INTERVAL
    var bulletRange = KuyuWorld.BULLET_RANGE
    var magnetRange = KuyuWorld.MAGNET_RANGE
    var comboGems = KuyuWorld.COMBO_GEMS
    var greed = 0
    var shield = false
    var shieldReady = false
    var jumpMul = 1f
    val owned = LinkedHashSet<Upgrade>()

    fun apply(upgrade: Upgrade, player: Player) {
        when (upgrade) {
            Upgrade.AMMO -> maxAmmo = min(maxAmmo + 2, MAX_AMMO)
            Upgrade.HEART -> {
                maxHp = min(maxHp + 1, MAX_HP)
                player.hp = min(maxHp, player.hp + 1)
            }
            Upgrade.SPREAD -> spread = true
            Upgrade.RAPID -> shotInterval = 4
            Upgrade.RANGE -> bulletRange = 14f
            Upgrade.MAGNET -> magnetRange = KuyuWorld.MAGNET_RANGE * 2f
            Upgrade.COMBO -> comboGems = KuyuWorld.COMBO_GEMS * 2
            Upgrade.GREED -> greed = 1
            Upgrade.SHIELD -> {
                shield = true
                shieldReady = true
            }
            Upgrade.JUMP -> jumpMul = 1.2f
        }
        owned += upgrade
    }

    companion object {
        const val MAX_AMMO = 14
        const val MAX_HP = 8
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
    val maxHp: Int = KuyuWorld.MAX_HP,
    val maxAmmo: Int = KuyuWorld.AMMO,
    val wallet: Int = gems,
    val bossHp: Int? = null,
    val bossMaxHp: Int = 0,
    val shieldReady: Boolean = false,
)

/** Simülasyonun bir adımda ürettiği, arayüzün ses/titreşim/parçacık için dinlediği olaylar. */
sealed interface KuyuEvent {
    data object Jump : KuyuEvent
    data object Shot : KuyuEvent
    data object Stomp : KuyuEvent
    data object Hurt : KuyuEvent
    data object Over : KuyuEvent

    /** Kalkan bir vuruşu yuttu. */
    data object Shield : KuyuEvent

    /** Yere iniş; [fallRows] tepe noktasından inilen satır sayısı. */
    data class Land(val fallRows: Int) : KuyuEvent
    data class BlockBreak(val row: Int, val col: Int, val gems: Boolean) : KuyuEvent
    data class Chest(val row: Int, val col: Int, val gems: Int) : KuyuEvent
    data class Kill(val kind: EnemyKind, val x: Float, val y: Float, val stomp: Boolean) : KuyuEvent
    data class GateOpen(val chunk: Int) : KuyuEvent
    data class Gem(val x: Float, val y: Float, val total: Int) : KuyuEvent
    data class Combo(val count: Int, val bonus: Int) : KuyuEvent
    data class Area(val index: Int) : KuyuEvent

    /** Bölge başı seçim kartı açıldı; simülasyon [KuyuWorld.resumeFromOffer] çağrılana dek durur. */
    data class Offer(val area: Int) : KuyuEvent
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
    var maxHp = kind.hp
    var vy = 0f
    var t = 0f
    val baseY = y
    var alive = true
    var active = false

    /** Bekçinin çağırdığı yarasa; sayısı sınırlıdır. */
    var minion = false

    /** Bekçi: bir sonraki yarasaya kalan süre. */
    var spawnTimer = 0f

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
                EnemyKind.BOSS -> {
                    val area = KuyuGen.area(spawn.row / KuyuGen.CHUNK_ROWS)
                    Enemy(kind, spawn.col + 0.5f - kind.w / 2f, spawn.row + 0.4f, 1f + 0.15f * area, dir).also {
                        it.hp = 10 + 4 * area
                        it.maxHp = it.hp
                    }
                }
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
