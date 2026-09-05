package com.za.games.ui.kakuro

import android.content.Context
import com.za.games.kakuro.Cell
import com.za.games.kakuro.KakuroDifficulty
import com.za.games.kakuro.KakuroState
import com.za.games.kakuro.KakuroStatus

/**
 * Devam eden Kakuro bulmacasını (düzen + ipuçları + değerler + notlar + süre)
 * cihazda saklar; uygulama kapatılsa da kaldığı yerden sürer. Çözülünce kayıt silinir.
 */
class KakuroStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("za_kakuro", Context.MODE_PRIVATE)

    fun save(state: KakuroState, elapsedSeconds: Int) {
        prefs.edit()
            .putString(KEY_DIFFICULTY, state.difficulty.name)
            .putLong(KEY_SEED, state.seed)
            .putInt(KEY_SIZE, state.size)
            .putString(KEY_CELLS, encodeCells(state.cells))
            .putString(KEY_SOLUTION, state.solution.joinToString(""))
            .putString(KEY_VALUES, state.values.joinToString(""))
            .putString(KEY_NOTES, state.notes.joinToString(",") { set -> set.sorted().joinToString("") })
            .putInt(KEY_ELAPSED, elapsedSeconds)
            .apply()
    }

    /** Bulmacayı siler; en son oynanan zorluk kalır. */
    fun clear() {
        prefs.edit()
            .remove(KEY_SEED)
            .remove(KEY_SIZE)
            .remove(KEY_CELLS)
            .remove(KEY_SOLUTION)
            .remove(KEY_VALUES)
            .remove(KEY_NOTES)
            .remove(KEY_ELAPSED)
            .apply()
    }

    fun lastDifficulty(): KakuroDifficulty? =
        prefs.getString(KEY_DIFFICULTY, null)
            ?.let { name -> KakuroDifficulty.entries.firstOrNull { it.name == name } }

    /** Kayıtlı bulmaca ve geçen süre; yoksa ya da veri bozuksa null. */
    fun restore(): Pair<KakuroState, Int>? {
        val difficulty = lastDifficulty() ?: return null
        val size = prefs.getInt(KEY_SIZE, 0)
        if (size !in 4..20) return null
        val total = size * size
        val cells = prefs.getString(KEY_CELLS, null)?.let { decodeCells(it) } ?: return null
        if (cells.size != total) return null
        val solution = prefs.getString(KEY_SOLUTION, null)?.toDigits(total) ?: return null
        val values = prefs.getString(KEY_VALUES, null)?.toDigits(total) ?: return null
        for (i in 0 until total) {
            if (!cells[i].white && (solution[i] != 0 || values[i] != 0)) return null
            if (cells[i].white && solution[i] !in 1..9) return null
        }
        if (values == solution) return null
        val groups = (prefs.getString(KEY_NOTES, null) ?: return null).split(',')
        if (groups.size != total) return null
        val notes = groups.map { group ->
            if (group.any { it !in '1'..'9' }) return null
            group.map { it - '0' }.toSet()
        }
        val state = KakuroState.restore(size, cells, solution, values, notes, difficulty, prefs.getLong(KEY_SEED, 0L))
        if (state.status == KakuroStatus.SOLVED) return null
        return state to prefs.getInt(KEY_ELAPSED, 0).coerceAtLeast(0)
    }

    private fun String.toDigits(total: Int): List<Int>? {
        if (length != total || any { it !in '0'..'9' }) return null
        return map { it - '0' }
    }

    private fun encodeCells(cells: List<Cell>): String = cells.joinToString(",") { c ->
        when {
            c.white -> "w"
            c.across == 0 && c.down == 0 -> "b"
            else -> "a${c.across}d${c.down}"
        }
    }

    private fun decodeCells(raw: String): List<Cell>? {
        val pattern = Regex("^a(\\d+)d(\\d+)$")
        return raw.split(',').map { token ->
            when (token) {
                "w" -> Cell(white = true)
                "b" -> Cell(white = false)
                else -> {
                    val m = pattern.matchEntire(token) ?: return null
                    Cell(white = false, across = m.groupValues[1].toInt(), down = m.groupValues[2].toInt())
                }
            }
        }
    }

    private companion object {
        const val KEY_DIFFICULTY = "difficulty"
        const val KEY_SEED = "seed"
        const val KEY_SIZE = "size"
        const val KEY_CELLS = "cells"
        const val KEY_SOLUTION = "solution"
        const val KEY_VALUES = "values"
        const val KEY_NOTES = "notes"
        const val KEY_ELAPSED = "elapsed"
    }
}
