package com.za.games.tuse

/** Klasik: sabit karo sayısı, en kısa süre. Sonsuz: sürekli akan, hızlanan karolar. */
enum class TuseMode { CLASSIC, ARCADE }

enum class TuseStatus { RUNNING, DONE, OVER }

/** Telifsiz bir ezgi: [notes] MIDI numaraları (60 = do4), sırayla çalınır. */
class Song(val id: String, val notes: IntArray)

sealed interface TuseEvent {
    /** İlk dokunuş: süre başladı. */
    data object Started : TuseEvent

    data class Hit(val index: Int, val lane: Int, val note: Int) : TuseEvent

    /** Yanlış şeride dokunuldu; [expected] doğru şerit. */
    data class Miss(val lane: Int, val expected: Int) : TuseEvent

    /** Sonsuz: sıradaki karo dokunulmadan alttan çıktı. */
    data class Passed(val lane: Int) : TuseEvent

    /** Klasik: son karo da vuruldu. */
    data object Done : TuseEvent
}

/** Arayüz için değişmez özet. */
data class TuseHud(
    val tapped: Int,
    /** Klasik'te karo sayısı; Sonsuz'da −1. */
    val total: Int,
    val status: TuseStatus,
    val started: Boolean,
    /** Sonsuz akış hızı (satır/s); Klasik'te 0. */
    val speed: Float,
    /** Yanlış dokunulan şerit; yoksa −1. */
    val missLane: Int,
    /** Kaçan karonun şeridi; yoksa −1. */
    val passedLane: Int,
    /** Sıradaki karonun şeridi. */
    val nextLane: Int,
    /** İlk dokunuştan bu yana geçen süre (ms); bitince donar. */
    val elapsedMs: Long,
)
