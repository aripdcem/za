package com.za.games.dalgic

enum class DalgicStatus { RUNNING, OVER }

/** Can kaybının nedeni. */
enum class LifeCause { SHARK, ENEMY_SUB, TORPEDO, MINE, OXYGEN, EMPTY_SURFACE }

enum class FoeKind(val radius: Float, val points: Int) {
    SHARK(0.045f, 20),
    ENEMY_SUB(0.045f, 30),
    MINE(0.035f, 10),
}

class Diver(val id: Int, var x: Float, var y: Float, val dir: Int, val speed: Float) {
    var alive = true
    var t = 0f
}

/**
 * Düşman: köpekbalığı ve düşman denizaltı [dir] yönünde yüzer, mayın yerinde
 * salınır ([baseY] çevresinde [wobble] genlikle). Düşman denizaltı
 * [DalgicWorld.ENEMY_FIRE_EVERY] saniyede bir yönüne torpido atar.
 */
class Foe(val id: Int, val kind: FoeKind, var x: Float, var y: Float, val dir: Int, val speed: Float, val wobble: Float, val phase: Float) {
    val baseY = y
    var alive = true
    var t = 0f
    var fireTimer = 1.4f

    /** Vuruş parlaması (s). */
    var flash = 0f
}

class Torpedo(var x: Float, var y: Float, val vx: Float, val friendly: Boolean)

/** Akıntı bandı: [y] merkezli, ±[half] yükseklikte, [dir] yönünde [speed] birim/s. */
class Current(val y: Float, val half: Float, val dir: Int, val speed: Float)

sealed interface DalgicEvent {
    data class DiverRescued(val count: Int, val x: Float, val y: Float) : DalgicEvent

    /** Kapasite doldu; yüzeye çıkma zamanı. */
    data object Full : DalgicEvent

    data class Delivered(val divers: Int, val points: Int, val wave: Int) : DalgicEvent
    data object Shot : DalgicEvent
    data object EnemyShot : DalgicEvent
    data class FoeDown(val kind: FoeKind, val x: Float, val y: Float, val points: Int) : DalgicEvent
    data class LifeLost(val cause: LifeCause, val lives: Int) : DalgicEvent
    data object OxygenLow : DalgicEvent
    data object Surfaced : DalgicEvent
    data object Over : DalgicEvent
}

/** Arayüz için değişmez özet. */
data class DalgicHud(
    val score: Int,
    val lives: Int,
    val divers: Int,
    /** Oksijen payı (0..1). */
    val oxygen: Float,
    val wave: Int,
    /** Teslim edilen toplam dalgıç. */
    val rescued: Int,
    val status: DalgicStatus,
    val facing: Int,
    val atSurface: Boolean,
    val invulnerable: Boolean,
)
