package com.za.games.ucurtma

enum class UcurtmaStatus { RUNNING, OVER }

/** Koşuyu bitiren çarpma. */
enum class CrashKind { ROOF, WIRE, STRING }

/** Ekipman: görev tamamlayınca açılır, biri takılır. */
enum class Gadget { TAIL, REEL, GLASS }

/** Bina: dünya x'inde [x, x + width], üstü [top] (y aşağı büyür, zemin [UcurtmaWorld.GROUND]). Baca [chimneyX] ≥ 0 ise vardır. */
class Building(val x: Float, val width: Float, val top: Float, val chimneyX: Float, val chimneyTop: Float) {
    /** Sıyırma sayıldı mı. */
    var graded = false

    /** Üstünden geçerken görülen en küçük boşluk (üst kenar − uçurtma altı). */
    var minClear = Float.MAX_VALUE
    val right: Float get() = x + width
}

/** Elektrik teli: direkler [x1] ve [x2]'de, tel [y] yüksekliğinde. */
class Wire(val x1: Float, val x2: Float, val y: Float) {
    var graded = false

    /** Uçurtma tele girerken altında mıydı (null: henüz girmedi). */
    var below: Boolean? = null
}

/**
 * Rakip uçurtma: [x] dünya konumu, akışa ek olarak [speed] birim/s sola gider,
 * [baseY] çevresinde [amp] genlikle salınır. İpi uçurtmadan sol-alta,
 * (x − [UcurtmaWorld.RIVAL_STRING_DX], zemin altı) noktasına iner.
 */
class Rival(val id: Int, var x: Float, val baseY: Float, val amp: Float, val phase: Float, val speed: Float) {
    var y = baseY
    var t = 0f
    var alive = true

    /** Kesildikten sonra düşüş hızı (görsel). */
    var fall = 0f
}

class Ribbon(val x: Float, val y: Float) {
    var taken = false
}

enum class MissionKind { DISTANCE, RIBBONS, UNDER_WIRE, NEAR_MISS, CUTS }

/** Görev: bir koşuda [target] kadar [kind]. [index] dizideki sırası. */
data class Mission(val index: Int, val kind: MissionKind, val target: Int)

/**
 * Görev dizisi deterministiktir: tür sırayla döner, hedef her turda büyür.
 * Üç görev aynı anda açıktır; biri tamamlanınca dizideki sıradaki gelir.
 */
object Missions {
    private val ORDER = listOf(MissionKind.DISTANCE, MissionKind.RIBBONS, MissionKind.UNDER_WIRE, MissionKind.NEAR_MISS, MissionKind.CUTS)

    fun at(index: Int): Mission {
        val i = index.coerceAtLeast(0)
        val tier = i / ORDER.size
        val kind = ORDER[i % ORDER.size]
        val target = when (kind) {
            MissionKind.DISTANCE -> 150 + 150 * tier
            MissionKind.RIBBONS -> 6 + 4 * tier
            MissionKind.UNDER_WIRE -> 2 + 2 * tier
            MissionKind.NEAR_MISS -> 2 + tier
            MissionKind.CUTS -> 1 + tier
        }
        return Mission(i, kind, target)
    }

    /** Ekipmanın açıldığı tamamlanmış görev sayısı. */
    fun unlockAt(gadget: Gadget): Int = when (gadget) {
        Gadget.TAIL -> 2
        Gadget.REEL -> 5
        Gadget.GLASS -> 9
    }

    fun unlocked(completed: Int): List<Gadget> = Gadget.entries.filter { completed >= unlockAt(it) }
}

sealed interface UcurtmaEvent {
    data class RibbonTaken(val x: Float, val y: Float, val count: Int) : UcurtmaEvent
    data class Cut(val x: Float, val y: Float, val bonus: Int) : UcurtmaEvent
    data class NearMiss(val x: Float, val y: Float) : UcurtmaEvent
    data class UnderWire(val count: Int) : UcurtmaEvent
    data class Milestone(val meters: Int) : UcurtmaEvent
    data class MissionDone(val mission: Mission) : UcurtmaEvent
    data class Crash(val kind: CrashKind) : UcurtmaEvent
    data object Over : UcurtmaEvent
}

/** Arayüz için değişmez özet. */
data class UcurtmaHud(
    val meters: Int,
    val ribbons: Int,
    val cuts: Int,
    val underWires: Int,
    val nearMisses: Int,
    val score: Int,
    /** Akış hızı (birim/s). */
    val speed: Float,
    val status: UcurtmaStatus,
    val crash: CrashKind?,
    /** Açık görevlerin ilerlemesi (sırayla). */
    val progress: List<Int>,
    val done: List<Boolean>,
)
