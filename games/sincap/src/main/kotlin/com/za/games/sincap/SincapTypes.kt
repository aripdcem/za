package com.za.games.sincap

/** Gövdenin solu ya da sağı; [x] işareti. */
enum class Side(val x: Float) { LEFT(-1f), RIGHT(1f) }

/**
 * Dal türü: [NONE] dal yok, [NORMAL] sağlam, [DRY] kuru (konduktan
 * [SincapWorld.DRY_HOLD] s sonra kırılır), [SNAKE] yılanlı (konan ölür),
 * [NUT] fındıklı, [GOLD] altın fındıklı.
 */
enum class BranchKind { NONE, NORMAL, DRY, SNAKE, NUT, GOLD }

val BranchKind.present: Boolean get() = this != BranchKind.NONE
val BranchKind.safe: Boolean get() = this != BranchKind.NONE && this != BranchKind.SNAKE

enum class SincapStatus { RUNNING, OVER }

enum class DeathCause { FALL, BROKE, CROW, SNAKE, CAT }

/** Bir yükseklik basamağı: iki yanda birer dal yuvası ve o yükseklikten karga geçip geçmeyeceği. */
class Level(var left: BranchKind, var right: BranchKind, val crow: Boolean) {
    fun at(side: Side): BranchKind = if (side == Side.LEFT) left else right

    fun set(side: Side, kind: BranchKind) {
        if (side == Side.LEFT) left = kind else right = kind
    }

    val safe: Boolean get() = left.safe || right.safe
}

class Crow(val id: Int, val level: Int, val dir: Int, var x: Float, val speed: Float)

sealed interface SincapEvent {
    data class Jumped(val from: Int, val to: Int, val side: Side) : SincapEvent
    data class Landed(val level: Int, val side: Side, val kind: BranchKind) : SincapEvent
    data class Nut(val level: Int, val side: Side, val points: Int, val golden: Boolean) : SincapEvent
    data class Cracking(val level: Int, val side: Side) : SincapEvent
    data class Broke(val level: Int, val side: Side) : SincapEvent
    data class CrowSpawned(val level: Int, val dir: Int) : SincapEvent
    data class Milestone(val height: Int) : SincapEvent
    data object CatClose : SincapEvent
    data class Over(val cause: DeathCause) : SincapEvent
}

/** Arayüz için değişmez özet. */
data class SincapHud(
    val height: Int,
    val nuts: Int,
    val score: Int,
    /** Kediyle aradaki yükseklik farkı (dal). */
    val catGap: Float,
    val status: SincapStatus,
    val cause: DeathCause?,
)
