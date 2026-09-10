package com.za.games.raket

/** Oyun türü: bilgisayara karşı, aynı telefonda iki kişi, duvara karşı ralli. */
enum class RaketMode { SOLO, DUO, WALL }

/**
 * Raketin kenarı. Alt raket her zaman (ilk) oyuncunundur; üst raket SOLO'da
 * bilgisayarın, DUO'da ikinci oyuncunundur, WALL'da yoktur (arka duvar).
 */
enum class Side { BOTTOM, TOP }

enum class RaketStatus { SERVING, RALLY, OVER }

class Ball {
    var x = RaketWorld.WIDTH / 2f
        internal set
    var y = RaketWorld.HEIGHT / 2f
        internal set
    var vx = 0f
        internal set
    var vy = 0f
        internal set
}

/** Yatay raket; [y] merkez çizgisidir ve değişmez. */
class Paddle(val side: Side, val y: Float) {
    var x = RaketWorld.WIDTH / 2f
        internal set
    var width = RaketWorld.PADDLE_W
        internal set

    /** Yumuşatılmış yatay hız (birim/s); falso buradan gelir. */
    var vel = 0f
        internal set

    /** Vuruş parlaması için kalan süre (s). */
    var flash = 0f
        internal set

    internal var prevX = x

    val left: Float get() = x - width / 2f
    val right: Float get() = x + width / 2f
}

sealed interface RaketEvent {
    data class Serve(val toward: Side) : RaketEvent

    /** [angle] dikeyden sapma (radyan, sağa pozitif), [speed] yeni top hızı. */
    data class PaddleHit(val side: Side, val speed: Float, val angle: Float) : RaketEvent

    data object SideWall : RaketEvent

    /** Duvar modunda arka duvar. */
    data object BackWall : RaketEvent

    data class Point(val scorer: Side, val bottom: Int, val top: Int) : RaketEvent

    /** [winner] duvar modunda null. */
    data class Over(val winner: Side?) : RaketEvent
}

/** Arayüz için değişmez özet. */
data class RaketHud(
    val bottom: Int,
    val top: Int,
    /** Süren rallideki vuruş sayısı (duvar modunda koşunun tamamı). */
    val rally: Int,
    val bestRally: Int,
    /** Top hızının taban–tavan aralığındaki payı (0..1). */
    val speed: Float,
    /** Servisin gideceği taraf; ralli sürerken null. */
    val serving: Side?,
    val status: RaketStatus,
    val winner: Side?,
)
