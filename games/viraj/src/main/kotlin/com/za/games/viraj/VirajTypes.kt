package com.za.games.viraj

enum class VirajStatus { RUNNING, OVER }

/** Yol üstündeki eşyalar. */
enum class ItemKind { TURBO, OIL, CONE, BOX }

/** Sarı kutudan çıkan armağan. */
enum class BoxGift { TURBO, SHIELD, TIME }

/** Yol kenarı nesneleri; [width] yol genişliği biriminde çarpışma genişliği. */
enum class SpriteKind(val width: Float) {
    TREE(0.55f), BUSH(0.35f), BOULDER(0.45f), SIGN(0.3f), POLE(0.12f)
}

class Sprite(val kind: SpriteKind, val x: Float)

class Item(val kind: ItemKind, val x: Float) {
    var taken = false
}

/**
 * Yol parçası. [curve] parçanın yanal kayması (çizimde parça başına
 * birikir), [y] parçanın uzak ucunun yüksekliği (dünya birimi). [z] parçanın
 * yakın ucu.
 */
class Segment(val index: Int, val curve: Float, val y: Float) {
    val z: Float get() = index * VirajWorld.SEGMENT_LENGTH
    val sprites = ArrayList<Sprite>(2)
    var item: Item? = null
    var checkpoint = false
}

/** Rakip araç: [z] dünya konumu, [x] yol genişliği kesri (−1..1 yol), [style] çizim çeşidi. */
class Car(var z: Float, var x: Float, var speed: Float, val style: Int) {
    /** Oyuncu bu aracı geçti mi (yeniden öne düşünce sıfırlanır). */
    var passed = false
}

sealed interface VirajEvent {
    data class Overtake(val total: Int) : VirajEvent
    data object Crash : VirajEvent
    data object ShieldUsed : VirajEvent
    data class Checkpoint(val index: Int, val bonusSeconds: Int) : VirajEvent
    data class Pickup(val kind: ItemKind, val gift: BoxGift?) : VirajEvent
    data object Slip : VirajEvent
    data object Cone : VirajEvent
    data class Milestone(val km: Int) : VirajEvent
    data object Over : VirajEvent
}

/** Arayüz için değişmez özet. */
data class VirajHud(
    val score: Long,
    val kmh: Int,
    val timeLeft: Float,
    val meters: Int,
    val overtakes: Int,
    val checkpoints: Int,
    val shield: Boolean,
    /** Turbo kalan süresinin kesri (0 = yok). */
    val turbo: Float,
    /** Kayma kalan süresinin kesri (0 = yok). */
    val slip: Float,
    /** Sıradaki kontrol noktasına ilerleme (0..1). */
    val checkpointProgress: Float,
    val status: VirajStatus,
)
