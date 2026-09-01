package com.za.games.turetme

import kotlin.random.Random

enum class TuretmeStatus { RUNNING, COMPLETED, GIVEN_UP }

enum class TuretmeInvalid { TOO_SHORT, NOT_WORD, ALREADY_FOUND }

/**
 * Kelime Türetme motoru: 6-7 harfli taban kelimenin harflerinden en az
 * 3 harfli alt kelimeleri bul.
 *
 * Harf seçimi indeksle yapılır; böylece tekrarlı harfler doğru çalışır
 * (aynı harf iki kez varsa ikisi de ayrı ayrı seçilebilir). Süre yoktur;
 * tur, tüm kelimeler bulununca tamamlanır ya da oyuncu pes eder ve kalan
 * kelimeler açıklanır. Puan: kelime uzunluğu × 10, taban kelime +50,
 * turu tamamlama +100. Günlük mod deterministiktir.
 */
data class TuretmeState(
    val base: String,
    /** Harflerin görüntü sırası (karılmış). */
    val letters: List<Char>,
    /** Bulunabilir tüm alt kelimeler; taban kelime dahil. */
    val targets: Set<String>,
    val found: Set<String> = emptySet(),
    /** Seçili harflerin [letters] içindeki indeksleri. */
    val usedIndices: List<Int> = emptyList(),
    val score: Long = 0L,
    val status: TuretmeStatus = TuretmeStatus.RUNNING,
    val invalidEvents: Int = 0,
    val lastInvalid: TuretmeInvalid? = null,
    /** Günlük modda epoch günü; serbest modda null. */
    val dailyDay: Long? = null,
) {

    /** Seçili harflerden oluşan kelime adayı. */
    val current: String
        get() = buildString { usedIndices.forEach { append(letters[it]) } }

    /** Henüz bulunmamış hedefler; pes edince oyuncuya gösterilir. */
    val missing: Set<String>
        get() = targets - found

    /** Turu bitirir ve kalan kelimelerin görülmesini sağlar. Puan korunur. */
    fun giveUp(): TuretmeState =
        if (status != TuretmeStatus.RUNNING) {
            this
        } else {
            copy(status = TuretmeStatus.GIVEN_UP, usedIndices = emptyList())
        }

    fun pick(index: Int): TuretmeState = when {
        status != TuretmeStatus.RUNNING -> this
        index !in letters.indices -> this
        index in usedIndices -> this
        else -> copy(usedIndices = usedIndices + index)
    }

    fun erase(): TuretmeState =
        if (status == TuretmeStatus.RUNNING && usedIndices.isNotEmpty()) {
            copy(usedIndices = usedIndices.dropLast(1))
        } else {
            this
        }

    fun clearCurrent(): TuretmeState =
        if (usedIndices.isEmpty()) this else copy(usedIndices = emptyList())

    /** Harf sırasını karıştırır; seçim temizlenir. Deterministik (tohumlu). */
    fun shuffle(seed: Long): TuretmeState =
        if (status != TuretmeStatus.RUNNING) {
            this
        } else {
            copy(letters = letters.shuffled(Random(seed)), usedIndices = emptyList())
        }

    fun submit(): TuretmeState {
        if (status != TuretmeStatus.RUNNING) return this
        val word = current
        return when {
            word.length < MIN_LENGTH ->
                copy(invalidEvents = invalidEvents + 1, lastInvalid = TuretmeInvalid.TOO_SHORT)
            word !in targets ->
                copy(invalidEvents = invalidEvents + 1, lastInvalid = TuretmeInvalid.NOT_WORD)
            word in found ->
                copy(invalidEvents = invalidEvents + 1, lastInvalid = TuretmeInvalid.ALREADY_FOUND)
            else -> accept(word).copy(usedIndices = emptyList())
        }
    }

    /** Günlük geri yükleme: kaydedilmiş bulunmuş kelimeyi yeniden uygular. */
    fun restoreFound(word: String): TuretmeState =
        if (status == TuretmeStatus.RUNNING && word in targets && word !in found) {
            accept(word)
        } else {
            this
        }

    private fun accept(word: String): TuretmeState {
        val newFound = found + word
        val completed = newFound.size == targets.size
        var gained = word.length * 10L
        if (word == base) gained += BASE_BONUS
        if (completed) gained += COMPLETION_BONUS
        return copy(
            found = newFound,
            score = score + gained,
            status = if (completed) TuretmeStatus.COMPLETED else TuretmeStatus.RUNNING,
        )
    }

    companion object {
        const val MIN_LENGTH = 3
        const val BASE_BONUS = 50L
        const val COMPLETION_BONUS = 100L
        private const val DAILY_SEED = 0x7E7E_AD20_26L

        /** Tabanın harf stoğuyla yazılabilen geçerli kelimeler (taban dahil). */
        fun targetsFor(base: String, valid: Iterable<String>): Set<String> {
            val stock = letterCounts(base)
            return valid.asSequence()
                .filter { it.length in MIN_LENGTH..base.length }
                .filter { fits(it, stock) }
                .toSet()
        }

        private fun letterCounts(word: String): Map<Char, Int> {
            val counts = mutableMapOf<Char, Int>()
            for (c in word) counts.merge(c, 1, Int::plus)
            return counts
        }

        private fun fits(word: String, stock: Map<Char, Int>): Boolean {
            val need = mutableMapOf<Char, Int>()
            for (c in word) {
                val n = need.merge(c, 1, Int::plus)!!
                if (n > (stock[c] ?: 0)) return false
            }
            return true
        }

        fun newGame(
            base: String,
            valid: Iterable<String>,
            shuffleSeed: Long,
            dailyDay: Long? = null,
        ): TuretmeState = TuretmeState(
            base = base,
            letters = base.toList().shuffled(Random(shuffleSeed)),
            targets = targetsFor(base, valid),
            dailyDay = dailyDay,
        )

        /** Günlük tur: diğer oyunlardaki sürümden bağımsız LCG karması. */
        fun daily(bases: List<String>, valid: Iterable<String>, epochDay: Long): TuretmeState {
            val n = bases.size
            val order = IntArray(n) { it }
            var s = DAILY_SEED
            for (i in n - 1 downTo 1) {
                s = s * 6364136223846793005L + 1442695040888963407L
                val j = ((s ushr 33) % (i + 1)).toInt()
                val tmp = order[i]
                order[i] = order[j]
                order[j] = tmp
            }
            val pos = (((epochDay % n) + n) % n).toInt()
            return newGame(
                base = bases[order[pos]],
                valid = valid,
                shuffleSeed = DAILY_SEED xor epochDay,
                dailyDay = epochDay,
            )
        }

        fun free(
            bases: List<String>,
            valid: Iterable<String>,
            seed: Long = Random.nextLong(),
        ): TuretmeState {
            val rng = Random(seed)
            return newGame(
                base = bases[rng.nextInt(bases.size)],
                valid = valid,
                shuffleSeed = rng.nextLong(),
            )
        }
    }
}
