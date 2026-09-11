package com.za.games.cici

enum class CiciStatus { RUNNING, OVER }

/** Bölümler: her bölüm bir ortam ve tehlike seti. Şimdilik yalnız uzay (Bölüm 1). */
enum class CiciChapter(val number: Int) { SPACE(1) }

/** İkramlar ve puanları: ballı yem 7, kuş yemi 5, su 2. */
enum class TreatKind(val points: Int, val radius: Float) {
    SEED(5, 0.03f),
    WATER(2, 0.028f),
    HONEY(7, 0.032f),
}

/** Can götüren tehlikeler. */
enum class HazardKind { CAT, BALL }

/** Cici'nin ruh hâli: yeni puan aldıysa mutlu, uzun süre kıpırdamadıysa sıkkın, yoksa sakin. */
enum class Mood { HAPPY, CALM, BORED }

/** Uzayda süzülen ikram: [vx],[vy] birim/s; yoluna dik küçük bir salınım ([wobble], [phase]). */
class Treat(val id: Int, val kind: TreatKind, var x: Float, var y: Float, val vx: Float, val vy: Float, val wobble: Float, val phase: Float) {
    var alive = true
    var t = 0f
}

/** Uzay kedisi: yandan girer, [vx],[vy] ile süzülür; [homing] > 0 ise Cici'nin hizasına kıvrılır. [tint] çizim rengi (0..2). */
class Cat(val id: Int, var x: Float, var y: Float, var vx: Float, var vy: Float, val homing: Float, val tint: Int) {
    var alive = true
    var t = 0f
}

/** Kırmızı top: arenanın kenarlarından seker, hızı zamanla artar. */
class Ball(var x: Float, var y: Float, var vx: Float, var vy: Float) {
    var t = 0f
}

sealed interface CiciEvent {
    /** İkram yakalandı; [streak] son [CiciWorld.JOY_WINDOW] saniye içindeki ardışık yakalama sayısı. */
    data class Caught(val kind: TreatKind, val points: Int, val streak: Int, val x: Float, val y: Float) : CiciEvent

    data class Hit(val by: HazardKind, val lives: Int) : CiciEvent

    /** Cici [CiciWorld.IDLE_WARN] saniyedir kıpırdamadı; puan cezası yaklaşıyor. */
    data object Bored : CiciEvent

    /** Hareketsizlik cezası: bir puan gitti (kalan [score]). */
    data class PointLost(val score: Int) : CiciEvent

    data object Over : CiciEvent
}

/** Arayüz için değişmez özet. */
data class CiciHud(
    val score: Int,
    val lives: Int,
    val mood: Mood,
    val streak: Int,
    val bestStreak: Int,
    /** Geçen süre (s). */
    val seconds: Int,
    /** Yakalanan toplam ikram. */
    val caught: Int,
    /** Son hareketten bu yana geçen süre (s). */
    val idleSeconds: Float,
    val invulnerable: Boolean,
    val status: CiciStatus,
    val facing: Int,
)
