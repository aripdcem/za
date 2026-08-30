package com.za.games.besharf

import kotlin.random.Random

enum class LetterMark { CORRECT, PRESENT, ABSENT }

enum class BesHarfStatus { RUNNING, WON, LOST }

/**
 * Beş Harf motoru: Wordle kurallarıyla 5 harf, 6 deneme.
 *
 * Günlük mod, epoch gününden deterministik olarak kelime seçer (sunucu
 * gerekmez); serbest mod tohumla rastgeledir. İşaretleme, tekrarlı
 * harflerde klasik iki geçişli kuralı uygular: önce tam isabetler harf
 * stokunu düşer, kalanlar "var ama yanlış yerde" sayılır.
 */
data class BesHarfState(
    val answer: String,
    val guesses: List<String> = emptyList(),
    val marks: List<List<LetterMark>> = emptyList(),
    val current: String = "",
    val status: BesHarfStatus = BesHarfStatus.RUNNING,
    /** Geçersiz (listede olmayan/eksik) gönderim sayacı; arayüz sallama efekti için. */
    val invalidEvents: Int = 0,
    /** Günlük modda epoch günü; serbest modda null. */
    val dailyDay: Long? = null,
) {

    fun type(letter: Char): BesHarfState = when {
        status != BesHarfStatus.RUNNING -> this
        current.length >= WORD_LENGTH -> this
        letter !in ALPHABET -> this
        else -> copy(current = current + letter)
    }

    fun erase(): BesHarfState =
        if (status == BesHarfStatus.RUNNING && current.isNotEmpty()) {
            copy(current = current.dropLast(1))
        } else {
            this
        }

    fun submit(isAllowed: (String) -> Boolean): BesHarfState {
        if (status != BesHarfStatus.RUNNING) return this
        if (current.length < WORD_LENGTH || !isAllowed(current)) {
            return copy(invalidEvents = invalidEvents + 1)
        }
        val guess = current
        val newGuesses = guesses + guess
        val newMarks = marks + listOf(mark(answer, guess))
        val newStatus = when {
            guess == answer -> BesHarfStatus.WON
            newGuesses.size >= MAX_GUESSES -> BesHarfStatus.LOST
            else -> BesHarfStatus.RUNNING
        }
        return copy(guesses = newGuesses, marks = newMarks, current = "", status = newStatus)
    }

    /** Klavye boyama: her harf için bugüne dek görülen en iyi işaret. */
    fun keyMarks(): Map<Char, LetterMark> {
        val best = mutableMapOf<Char, LetterMark>()
        guesses.forEachIndexed { g, guess ->
            guess.forEachIndexed { i, letter ->
                val mark = marks[g][i]
                val old = best[letter]
                if (old == null || mark.ordinal < old.ordinal) best[letter] = mark
            }
        }
        return best
    }

    companion object {
        const val WORD_LENGTH = 5
        const val MAX_GUESSES = 6
        private const val DAILY_SEED = 0x5A_BE5_4A2FL

        /** Türk alfabesinin 29 harfi. */
        val ALPHABET: Set<Char> = "abcçdefgğhıijklmnoöprsştuüvyz".toSet()

        fun mark(answer: String, guess: String): List<LetterMark> {
            require(answer.length == WORD_LENGTH && guess.length == WORD_LENGTH)
            val result = arrayOfNulls<LetterMark>(WORD_LENGTH)
            val remaining = mutableMapOf<Char, Int>()

            for (i in 0 until WORD_LENGTH) {
                if (guess[i] == answer[i]) {
                    result[i] = LetterMark.CORRECT
                } else {
                    remaining.merge(answer[i], 1, Int::plus)
                }
            }
            for (i in 0 until WORD_LENGTH) {
                if (result[i] != null) continue
                val count = remaining[guess[i]] ?: 0
                result[i] = if (count > 0) {
                    remaining[guess[i]] = count - 1
                    LetterMark.PRESENT
                } else {
                    LetterMark.ABSENT
                }
            }
            return result.map { requireNotNull(it) }
        }

        /**
         * Günlük bulmaca: cevap listesi sabit tohumla bir kez karılır ve
         * epoch gününe göre sırayla gezilir — yakın günlerde tekrar olmaz.
         *
         * Karma, elle kodlanmış bir LCG ile yapılır: kotlin.random yalnızca
         * aynı Kotlin sürümü içinde tekrarlanabilirlik garantiler; bu sayede
         * günün kelimesi uygulama güncellemeleriyle değişmez.
         */
        fun daily(answers: List<String>, epochDay: Long): BesHarfState {
            val n = answers.size
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
            return BesHarfState(answer = answers[order[pos]], dailyDay = epochDay)
        }

        fun free(answers: List<String>, seed: Long = Random.nextLong()): BesHarfState =
            BesHarfState(answer = answers[Random(seed).nextInt(answers.size)])
    }
}
