package com.za.games.ui.tuse

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.za.games.tuse.NoteSynth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Tuşe notaları: [NoteSynth] ile üretilen kısa piyano tonları uygulamanın
 * önbellek dizinine WAV olarak yazılır ve SoundPool'a yüklenir; uygulamaya
 * ses dosyası eklenmez, izin gerekmez. [enabled] her çalmada okunur (ana
 * menüdeki ses düğmesiyle aynı kapı, bkz. SoundPlayer).
 */
class NotePlayer(context: Context, private val enabled: () -> Boolean) {

    private val dir = File(context.applicationContext.cacheDir, "tuse")

    private val pool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
        )
        .build()

    private val ids = ConcurrentHashMap<Int, Int>()
    private val loaded: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    @Volatile
    private var released = false

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status -> if (status == 0) loaded += sampleId }
    }

    /** Parçanın notalarını hazırlar: eksik WAV'ları yazar, havuza yükler. IO'da çalışır. */
    suspend fun prepare(notes: IntArray) = withContext(Dispatchers.IO) {
        if (released) return@withContext
        dir.mkdirs()
        for (midi in notes.distinct().sorted()) {
            if (ids.containsKey(midi)) continue
            val file = File(dir, "note_$midi.wav")
            if (!file.exists() || file.length() == 0L) {
                val tmp = File(dir, "note_$midi.tmp")
                tmp.writeBytes(NoteSynth.wav(NoteSynth.pcm(midi)))
                if (!tmp.renameTo(file)) {
                    file.writeBytes(tmp.readBytes())
                    tmp.delete()
                }
            }
            if (released) return@withContext
            val id = pool.load(file.path, 1)
            if (id != 0) ids[midi] = id
        }
    }

    /** Notayı çalar; hazır değilse ya da ses kapalıysa sessiz. */
    fun play(midi: Int) {
        if (released || !enabled()) return
        val id = ids[midi] ?: return
        if (id !in loaded) return
        pool.play(id, 0.85f, 0.85f, 1, 0, 1f)
    }

    fun release() {
        released = true
        pool.release()
    }
}
