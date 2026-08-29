package com.za.games.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.annotation.RawRes
import androidx.compose.runtime.staticCompositionLocalOf
import com.za.games.R

/** Platformdaki ortak ses efektleri; tüm oyunlar aynı paleti kullanır. */
enum class Sfx(@RawRes val res: Int) {
    /** Küçük olumlu an: yem yeme, taş birleştirme. */
    POP(R.raw.sfx_pop),

    /** Satır temizleme. */
    CLEAR(R.raw.sfx_clear),

    /** Büyük an: Tetris (4 satır), 2048'e ulaşma. */
    BIG(R.raw.sfx_big),

    /** Taş kilitlenmesi / sert düşüş. */
    DROP(R.raw.sfx_drop),

    /** Oyun sonu. */
    OVER(R.raw.sfx_over),
}

/**
 * SoundPool tabanlı hafif ses çalar. Efektler prosedürel üretilmiş küçük
 * WAV'lardır (bkz. tools/gen_sfx.py). [enabled] her çalmada okunur; ana
 * menüdeki ses düğmesi böylece anında etki eder.
 */
class SoundPlayer(context: Context, private val enabled: () -> Boolean) {

    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val ids: Map<Sfx, Int> =
        Sfx.entries.associateWith { pool.load(context, it.res, 1) }

    fun play(sfx: Sfx, volume: Float = 1f, rate: Float = 1f) {
        if (!enabled()) return
        val id = ids[sfx] ?: return
        pool.play(id, volume, volume, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    fun release() = pool.release()
}

/** Oyun ekranlarının ses çalara eriştiği yer; sağlanmadıysa sessizlik. */
val LocalZaSound = staticCompositionLocalOf<SoundPlayer?> { null }
