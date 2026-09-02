package com.za.games.ui.sudoku

import android.content.Context
import com.za.games.sudoku.SudokuDifficulty
import com.za.games.sudoku.SudokuState
import com.za.games.sudoku.SudokuStatus

/**
 * Devam eden Sudoku bulmacasını (tahta + notlar + süre) cihazda saklar;
 * uygulama kapatılsa da bulmaca kaldığı yerden sürer. Yalnızca devam eden
 * bulmacalar kaydedilir — çözülünce kayıt silinir.
 */
class SudokuStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("za_sudoku", Context.MODE_PRIVATE)

    fun save(state: SudokuState, elapsedSeconds: Int) {
        prefs.edit()
            .putString(KEY_DIFFICULTY, state.difficulty.name)
            .putLong(KEY_SEED, state.seed)
            .putString(KEY_SOLUTION, state.solution.joinToString(""))
            .putString(KEY_GIVEN, state.given.joinToString("") { if (it) "1" else "0" })
            .putString(KEY_VALUES, state.values.joinToString(""))
            .putString(KEY_NOTES, state.notes.joinToString(",") { set -> set.sorted().joinToString("") })
            .putInt(KEY_ELAPSED, elapsedSeconds)
            .apply()
    }

    /** Bulmacayı siler; en son oynanan zorluk (zorluk seçicide öne çıkarmak için) kalır. */
    fun clear() {
        prefs.edit()
            .remove(KEY_SEED)
            .remove(KEY_SOLUTION)
            .remove(KEY_GIVEN)
            .remove(KEY_VALUES)
            .remove(KEY_NOTES)
            .remove(KEY_ELAPSED)
            .apply()
    }

    /** En son oynanan zorluk; bulmaca bitmiş/silinmiş olsa bile kalıcıdır. */
    fun lastDifficulty(): SudokuDifficulty? =
        prefs.getString(KEY_DIFFICULTY, null)
            ?.let { name -> SudokuDifficulty.entries.firstOrNull { it.name == name } }

    /** Kayıtlı bulmaca ve geçen süre; yoksa ya da veri bozuksa null. */
    fun restore(): Pair<SudokuState, Int>? {
        val difficulty = lastDifficulty() ?: return null
        val solution = prefs.getString(KEY_SOLUTION, null)?.toDigits(1..9) ?: return null
        val values = prefs.getString(KEY_VALUES, null)?.toDigits(0..9) ?: return null
        val givenRaw = prefs.getString(KEY_GIVEN, null) ?: return null
        if (givenRaw.length != 81 || givenRaw.any { it != '0' && it != '1' }) return null
        val given = givenRaw.map { it == '1' }

        // İpucu hücreleri çözümle eşleşmeli; çözülmüş tahta geri yüklenmez.
        for (i in 0 until 81) {
            if (given[i] && values[i] != solution[i]) return null
        }
        if (values == solution) return null

        val notesRaw = prefs.getString(KEY_NOTES, null) ?: return null
        val groups = notesRaw.split(',')
        if (groups.size != 81) return null
        val notes = groups.map { group ->
            if (group.any { it !in '1'..'9' }) return null
            group.map { it - '0' }.toSet()
        }

        val state = SudokuState(
            solution = solution,
            given = given,
            values = values,
            notes = notes,
            difficulty = difficulty,
            status = SudokuStatus.RUNNING,
            seed = prefs.getLong(KEY_SEED, 0L),
        )
        return state to prefs.getInt(KEY_ELAPSED, 0).coerceAtLeast(0)
    }

    private fun String.toDigits(range: IntRange): List<Int>? {
        if (length != 81) return null
        val digits = map { it - '0' }
        return if (digits.all { it in range }) digits else null
    }

    private companion object {
        const val KEY_DIFFICULTY = "difficulty"
        const val KEY_SEED = "seed"
        const val KEY_SOLUTION = "solution"
        const val KEY_GIVEN = "given"
        const val KEY_VALUES = "values"
        const val KEY_NOTES = "notes"
        const val KEY_ELAPSED = "elapsed"
    }
}
