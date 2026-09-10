package com.za.games.tuse

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Piyano benzeri kısa nota: beş harmonik, üsttekiler daha hızlı sönen üstel
 * zarf, 4 ms atak, sonda tık önleyen kısa iniş. Saf hesap; uygulama sonucu
 * WAV olarak önbelleğe yazıp SoundPool ile çalar (bkz. NotePlayer).
 */
object NoteSynth {

    const val SAMPLE_RATE = 22_050
    const val DURATION = 0.5f
    const val MIN_MIDI = 48
    const val MAX_MIDI = 96

    private val HARMONICS = doubleArrayOf(1.0, 0.5, 0.25, 0.12, 0.06)

    fun frequency(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)

    fun pcm(midi: Int): ShortArray {
        require(midi in MIN_MIDI..MAX_MIDI) { "nota aralık dışı: $midi" }
        val n = (SAMPLE_RATE * DURATION).toInt()
        val f = frequency(midi)
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            var s = 0.0
            for ((k, a) in HARMONICS.withIndex()) {
                s += a * sin(2.0 * PI * f * (k + 1) * t) * exp(-t * (3.5 + 2.0 * k))
            }
            val attack = min(1.0, t / 0.004)
            val release = min(1.0, (DURATION - t) / 0.03)
            val v = s * attack * release * 0.42
            out[i] = (v.coerceIn(-1.0, 1.0) * 32_767).toInt().toShort()
        }
        return out
    }

    /** 16 bit tek kanallı WAV: 44 baytlık başlık + küçük sonlu örnekler. */
    fun wav(pcm: ShortArray): ByteArray {
        val dataBytes = pcm.size * 2
        val out = ByteArray(44 + dataBytes)
        fun str(at: Int, s: String) { for (i in s.indices) out[at + i] = s[i].code.toByte() }
        fun int32(at: Int, v: Int) { for (i in 0 until 4) out[at + i] = (v ushr (8 * i)).toByte() }
        fun int16(at: Int, v: Int) { out[at] = v.toByte(); out[at + 1] = (v ushr 8).toByte() }
        str(0, "RIFF"); int32(4, 36 + dataBytes); str(8, "WAVE")
        str(12, "fmt "); int32(16, 16); int16(20, 1); int16(22, 1)
        int32(24, SAMPLE_RATE); int32(28, SAMPLE_RATE * 2); int16(32, 2); int16(34, 16)
        str(36, "data"); int32(40, dataBytes)
        var p = 44
        for (s in pcm) {
            out[p] = s.toByte()
            out[p + 1] = (s.toInt() ushr 8).toByte()
            p += 2
        }
        return out
    }
}
