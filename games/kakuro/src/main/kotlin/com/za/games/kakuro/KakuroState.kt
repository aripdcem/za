package com.za.games.kakuro

import kotlin.random.Random

/** Boy = ipucu kenarı dahil kare ızgara kenarı; kara oranı iç hücrelerin yaklaşık payı. */
enum class KakuroDifficulty(val size: Int, val blackRatio: Float) {
    EASY(8, 0.26f),
    MEDIUM(10, 0.28f),
    HARD(12, 0.30f),
}

enum class KakuroStatus { RUNNING, SOLVED }

/**
 * Hücre: [white] doldurulacak hücre; değilse kara. Kara hücrede [across] sağındaki
 * yatay koşunun, [down] altındaki dikey koşunun toplamı (0 = ipucu yok).
 */
data class Cell(val white: Boolean, val across: Int = 0, val down: Int = 0)

/** Koşu: ardışık beyaz hücreler ve toplam ipucu. */
data class Run(val sum: Int, val cells: List<Int>, val horizontal: Boolean)

/**
 * Kakuro motoru: değişmez durum makinesi. Bulmaca, tohumdan deterministik
 * üretilir ve tek çözümlüdür. Beyaz hücrelere 1..9 yazılır; her koşunun
 * rakamları farklı olmalı ve ipucuyla toplanmalıdır. Tablo çözümle birebir
 * eşleşince biter.
 */
data class KakuroState(
    val size: Int,
    val cells: List<Cell>,
    /** Beyaz hücrelerde 1..9, kara hücrelerde 0. */
    val solution: List<Int>,
    /** Mevcut tablo; 0 = boş (kara hücreler hep 0). */
    val values: List<Int>,
    val notes: List<Set<Int>>,
    val difficulty: KakuroDifficulty,
    val status: KakuroStatus,
    val seed: Long,
) {
    /** Koşular (yatay ve dikey); hücrelerden türetilir. */
    val runs: List<Run> by lazy { KakuroLogic.runs(size, cells) }

    /** Hücre başına yatay/dikey koşu dizini (yoksa -1). */
    val acrossOf: IntArray by lazy { runIndex(horizontal = true) }
    val downOf: IntArray by lazy { runIndex(horizontal = false) }

    private fun runIndex(horizontal: Boolean): IntArray {
        val out = IntArray(size * size) { -1 }
        runs.forEachIndexed { r, run ->
            if (run.horizontal == horizontal) run.cells.forEach { out[it] = r }
        }
        return out
    }

    val whiteCount: Int get() = cells.count { it.white }

    /**
     * Hatalı hücreler: koşu içinde yinelenen rakam; koşu dolmuşken toplam
     * ipucudan farklı; ya da dolmadan toplam ipucuyu aşmış (dolu hücreler işaretlenir).
     */
    val conflicts: Set<Int>
        get() {
            val bad = HashSet<Int>()
            for (run in runs) {
                val seen = HashMap<Int, Int>()
                var sum = 0
                var filled = 0
                for (c in run.cells) {
                    val v = values[c]
                    if (v == 0) continue
                    filled++
                    sum += v
                    val prev = seen[v]
                    if (prev != null) {
                        bad += prev
                        bad += c
                    } else {
                        seen[v] = c
                    }
                }
                val full = filled == run.cells.size
                if ((full && sum != run.sum) || (!full && sum >= run.sum)) {
                    run.cells.forEach { if (values[it] != 0) bad += it }
                }
            }
            return bad
        }

    /** Tamamlanmış ve doğru koşuların dizinleri (yeşil vurgu). */
    val solvedRuns: Set<Int>
        get() {
            val out = HashSet<Int>()
            runs.forEachIndexed { r, run ->
                var sum = 0
                val seen = HashSet<Int>()
                var ok = true
                for (c in run.cells) {
                    val v = values[c]
                    if (v == 0 || !seen.add(v)) {
                        ok = false
                        break
                    }
                    sum += v
                }
                if (ok && sum == run.sum) out += r
            }
            return out
        }

    fun setValue(index: Int, value: Int): KakuroState {
        if (status != KakuroStatus.RUNNING || !cells[index].white || value !in 1..9) return this
        if (values[index] == value) return clearCell(index) // aynı rakama tekrar basmak siler
        val newValues = values.toMutableList().also { it[index] = value }
        val newNotes = notes.toMutableList()
        newNotes[index] = emptySet()
        // Aynı koşulardaki hücrelerde bu rakamın notu düşer.
        for (r in intArrayOf(acrossOf[index], downOf[index])) {
            if (r < 0) continue
            for (peer in runs[r].cells) {
                if (peer != index && value in newNotes[peer]) newNotes[peer] = newNotes[peer] - value
            }
        }
        return copy(
            values = newValues,
            notes = newNotes,
            status = if (newValues == solution) KakuroStatus.SOLVED else KakuroStatus.RUNNING,
        )
    }

    fun toggleNote(index: Int, value: Int): KakuroState {
        if (status != KakuroStatus.RUNNING || !cells[index].white || value !in 1..9) return this
        if (values[index] != 0) return this
        val newNotes = notes.toMutableList()
        newNotes[index] = if (value in newNotes[index]) newNotes[index] - value else newNotes[index] + value
        return copy(notes = newNotes)
    }

    fun clearCell(index: Int): KakuroState {
        if (status != KakuroStatus.RUNNING || !cells[index].white) return this
        if (values[index] == 0 && notes[index].isEmpty()) return this
        val newValues = values.toMutableList().also { it[index] = 0 }
        val newNotes = notes.toMutableList().also { it[index] = emptySet() }
        return copy(values = newValues, notes = newNotes)
    }

    companion object {
        fun newGame(difficulty: KakuroDifficulty, seed: Long = Random.nextLong()): KakuroState =
            KakuroLogic.generate(difficulty, seed)

        /** Kayıttan geri yükleme: bulmaca + mevcut değerler. */
        fun restore(
            size: Int,
            cells: List<Cell>,
            solution: List<Int>,
            values: List<Int>,
            notes: List<Set<Int>>,
            difficulty: KakuroDifficulty,
            seed: Long,
        ): KakuroState = KakuroState(
            size = size,
            cells = cells,
            solution = solution,
            values = values,
            notes = notes,
            difficulty = difficulty,
            status = if (values == solution) KakuroStatus.SOLVED else KakuroStatus.RUNNING,
            seed = seed,
        )
    }
}
