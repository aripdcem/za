package com.za.games.ui.mines

import android.content.Context
import com.za.games.mines.MinesDifficulty
import com.za.games.mines.MinesState
import com.za.games.mines.MinesStatus

/**
 * Devam eden Mayın Tarlası tahtasını cihazda saklar; uygulama kapatılsa da
 * tahta kaldığı yerden sürer. Yalnızca sürmekte olan tahtalar kaydedilir —
 * kazanınca/kaybedince kayıt silinir, dönüşte zorluk seçici gelir.
 */
class MinesStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("za_mines", Context.MODE_PRIVATE)

    fun save(state: MinesState, elapsedSeconds: Int) {
        prefs.edit()
            .putString(KEY_DIFFICULTY, difficultyFor(state).name)
            .putLong(KEY_SEED, state.seed)
            .putString(KEY_MINES, state.mines.joinToString(","))
            .putString(KEY_REVEALED, state.revealed.joinToString(","))
            .putString(KEY_FLAGGED, state.flagged.joinToString(","))
            .putInt(KEY_ELAPSED, elapsedSeconds)
            .apply()
    }

    /** Yeni tahta kurulurken zorluk hemen kalıcı olsun — ilk hamleyi beklemeden. */
    fun saveDifficulty(difficulty: MinesDifficulty) {
        prefs.edit().putString(KEY_DIFFICULTY, difficulty.name).apply()
    }

    /** Tahtayı siler; en son oynanan zorluk (zorluk seçicide öne çıkarmak için) kalır. */
    fun clear() {
        prefs.edit()
            .remove(KEY_SEED)
            .remove(KEY_MINES)
            .remove(KEY_REVEALED)
            .remove(KEY_FLAGGED)
            .remove(KEY_ELAPSED)
            .apply()
    }

    /** En son oynanan zorluk; tahta bitmiş/silinmiş olsa bile kalıcıdır. */
    fun lastDifficulty(): MinesDifficulty? =
        prefs.getString(KEY_DIFFICULTY, null)
            ?.let { name -> MinesDifficulty.entries.firstOrNull { it.name == name } }

    /** Kayıtlı tahta ve geçen süre; yoksa ya da veri bozuksa null. */
    fun restore(): Pair<MinesState, Int>? {
        val difficulty = lastDifficulty() ?: return null
        val cellCount = difficulty.width * difficulty.height

        val mines = prefs.getString(KEY_MINES, null)?.toIndexSet(cellCount) ?: return null
        if (mines.size != difficulty.mineCount) return null

        val revealed = prefs.getString(KEY_REVEALED, null)?.toIndexSet(cellCount) ?: return null
        if (revealed.any { it in mines }) return null
        if (revealed.size >= cellCount - difficulty.mineCount) return null

        val flagged = prefs.getString(KEY_FLAGGED, null)?.toIndexSet(cellCount) ?: return null
        if (flagged.any { it in revealed }) return null

        val state = MinesState(
            width = difficulty.width,
            height = difficulty.height,
            mineCount = difficulty.mineCount,
            mines = mines,
            revealed = revealed,
            flagged = flagged,
            status = MinesStatus.RUNNING,
            seed = prefs.getLong(KEY_SEED, 0L),
        )
        return state to prefs.getInt(KEY_ELAPSED, 0).coerceAtLeast(0)
    }

    private fun difficultyFor(state: MinesState): MinesDifficulty =
        MinesDifficulty.entries.first { it.width == state.width && it.height == state.height }

    private fun String.toIndexSet(cellCount: Int): Set<Int>? {
        if (isEmpty()) return emptySet()
        val indices = split(',').mapNotNull { it.toIntOrNull() }
        if (indices.size != split(',').size) return null
        if (indices.any { it < 0 || it >= cellCount }) return null
        return indices.toSet()
    }

    private companion object {
        const val KEY_DIFFICULTY = "difficulty"
        const val KEY_SEED = "seed"
        const val KEY_MINES = "mines"
        const val KEY_REVEALED = "revealed"
        const val KEY_FLAGGED = "flagged"
        const val KEY_ELAPSED = "elapsed"
    }
}
