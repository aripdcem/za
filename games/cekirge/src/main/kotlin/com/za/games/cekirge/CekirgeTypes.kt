package com.za.games.cekirge

enum class CekirgeStatus { RUNNING, OVER }

/** Çekirge türü satıra göre: en üst sıra kara (30), ortadaki iki sıra yeşil (20), alttaki iki sıra kahverengi (10). */
enum class BugKind(val points: Int) { KARA(30), YESIL(20), KAHVE(10) }

class Bug(val col: Int, val row: Int, val kind: BugKind) {
    var alive = true
}

/** Çiftçinin ilaç fıskırtması; aynı anda yalnızca bir tane uçar. */
class Shot(var x: Float, var y: Float)

/** Sürünün tükürüğü: aşağı iner, balyayı aşındırır, çiftçiye çarpar. */
class Spit(val id: Int, var x: Float, var y: Float, val speed: Float)

/**
 * Saman balyası: [CekirgeWorld.BALE_CX] × [CekirgeWorld.BALE_CY] hücre; vuruş
 * ve sürü teması hücreleri siler. [x] merkez, [top] üst kenar.
 */
class Bale(val index: Int, val x: Float, val top: Float) {
    val cells: Array<BooleanArray> = Array(CekirgeWorld.BALE_CY) { BooleanArray(CekirgeWorld.BALE_CX) { true } }
    val intact: Int get() = cells.sumOf { r -> r.count { it } }
    val left: Float get() = x - CekirgeWorld.BALE_W / 2f
    val cellW: Float get() = CekirgeWorld.BALE_W / CekirgeWorld.BALE_CX
    val cellH: Float get() = CekirgeWorld.BALE_H / CekirgeWorld.BALE_CY
}

/** Kraliçe: üstten geçer, vurulunca bonus. [dir] +1 sağa. */
class Queen(val dir: Int, var x: Float, val points: Int)

sealed interface CekirgeEvent {
    data object Fired : CekirgeEvent
    data class BugHit(val kind: BugKind, val x: Float, val y: Float, val points: Int) : CekirgeEvent
    data class SpitHit(val x: Float, val y: Float) : CekirgeEvent
    data class BaleHit(val x: Float, val y: Float) : CekirgeEvent
    data class FarmerHit(val lives: Int) : CekirgeEvent
    data class QueenSpawned(val dir: Int) : CekirgeEvent
    data class QueenHit(val points: Int, val x: Float) : CekirgeEvent
    data class WaveCleared(val wave: Int, val bonus: Int) : CekirgeEvent
    data class WaveStart(val wave: Int) : CekirgeEvent
    data class Over(val invaded: Boolean) : CekirgeEvent
}

/** Arayüz için değişmez özet. */
data class CekirgeHud(
    val score: Int,
    val lives: Int,
    val wave: Int,
    val alive: Int,
    val total: Int,
    /** Koşu boyunca vurulan çekirge. */
    val kills: Int,
    val status: CekirgeStatus,
    /** Fıskırtma hazır mı (uçan mermi yok). */
    val shotReady: Boolean,
    val invaded: Boolean,
)
