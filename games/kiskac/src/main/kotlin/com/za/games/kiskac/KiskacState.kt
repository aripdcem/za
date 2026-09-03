package com.za.games.kiskac

import kotlin.random.Random

/**
 * Türk alfabesi sıralaması (29 harf): c<ç, g<ğ, h<ı<i, o<ö, s<ş, u<ü.
 * Unicode sırası Türkçe için yanlış sonuç verir; karşılaştırma bu tabloyla yapılır.
 */
object TurkishOrder {
    const val LETTERS = "abcçdefgğhıijklmnoöprsştuüvyz"

    private val ranks: Map<Char, Int> =
        LETTERS.withIndex().associate { (index, letter) -> letter to index }

    /** Tablo dışı bir karakter (olmamalı) alfabenin sonuna sıralanır; çökmez. */
    fun rankOf(letter: Char): Int = ranks[letter] ?: (LETTERS.length + letter.code)

    /** [sorted] bu sıralamayla dizilmiş olmalı; bulunamazsa ekleme noktası döner. */
    fun indexOf(sorted: List<String>, word: String): Int {
        var low = 0
        var high = sorted.size - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val c = compare(sorted[mid], word)
            when {
                c < 0 -> low = mid + 1
                c > 0 -> high = mid - 1
                else -> return mid
            }
        }
        return low
    }

    fun compare(a: String, b: String): Int {
        val n = minOf(a.length, b.length)
        for (i in 0 until n) {
            val d = rankOf(a[i]) - rankOf(b[i])
            if (d != 0) return d
        }
        return a.length - b.length
    }
}

enum class KiskacStatus { RUNNING, WON, LOST }

/**
 * Sınırlar ile gizli kelime arasındaki uzaklık, sözlük sırasındaki konumlarla
 * ölçülür. Sınır yoksa listenin başı/sonu sınır sayılır.
 */
data class KiskacDistance(
    val answerIndex: Int,
    /** Alt sınırın dizini; sınır yoksa -1. */
    val lowerIndex: Int,
    /** Üst sınırın dizini; sınır yoksa liste boyu. */
    val upperIndex: Int,
    val size: Int,
) {
    /** Alt sınırdan (ya da A'dan) gizli kelimeye kaç kelime sonra. */
    val fromLower: Int get() = answerIndex - lowerIndex

    /** Gizli kelimeden üst sınıra (ya da Z'ye) kaç kelime önce. */
    val toUpper: Int get() = upperIndex - answerIndex

    /** Gizli kelimenin iki sınır arasındaki konumu, 0 (alt) ile 1 (üst) arasında. */
    val fraction: Float
        get() {
            val span = upperIndex - lowerIndex
            return if (span <= 0) 0.5f else fromLower.toFloat() / span
        }
}

enum class KiskacInvalid { NOT_IN_LIST, ALREADY_TRIED }

/** Bir tahmin ve sonucu: gizli kelime bu tahminden alfabetik olarak sonra mı? */
data class KiskacGuess(val word: String, val hiddenIsAfter: Boolean)

/**
 * Kıskaç motoru: gizli 5 harfli kelimeyi alfabetik aralık daraltarak bul.
 *
 * Her geçerli tahmin tek bir bilgi verir: gizli kelime, tahminden sözlük
 * sırasına göre önce mi sonra mı geliyor. Oyuncu alt ve üst sınırı
 * daraltarak kelimeyi "sıkıştırır". Günlük mod epoch gününden
 * deterministiktir; serbest mod tohumla rastgeledir.
 */
data class KiskacState(
    val answer: String,
    val guesses: List<KiskacGuess> = emptyList(),
    val current: String = "",
    val status: KiskacStatus = KiskacStatus.RUNNING,
    /** Geçersiz gönderim sayacı; arayüz uyarı/titreşim için izler. */
    val invalidEvents: Int = 0,
    /** Son geçersiz gönderimin nedeni (mesaj seçimi için). */
    val lastInvalid: KiskacInvalid? = null,
    /** Günlük modda epoch günü; serbest modda null. */
    val dailyDay: Long? = null,
) {

    /** Gizli kelimenin SONRASINDA geldiği en büyük tahmin (alt sınır). */
    val lowerBound: String?
        get() = guesses.asSequence()
            .filter { it.hiddenIsAfter }
            .map { it.word }
            .maxWithOrNull { a, b -> TurkishOrder.compare(a, b) }

    /** Gizli kelimenin ÖNCESİNDE geldiği en küçük tahmin (üst sınır). */
    val upperBound: String?
        get() = guesses.asSequence()
            .filter { !it.hiddenIsAfter }
            .map { it.word }
            .minWithOrNull { a, b -> TurkishOrder.compare(a, b) }

    /**
     * Uzaklık ipucu: [sorted], oyuncunun tahmin edebileceği tüm kelimelerin
     * [TurkishOrder] ile dizilmiş listesi olmalı.
     */
    fun distance(sorted: List<String>): KiskacDistance = KiskacDistance(
        answerIndex = TurkishOrder.indexOf(sorted, answer),
        lowerIndex = lowerBound?.let { TurkishOrder.indexOf(sorted, it) } ?: -1,
        upperIndex = upperBound?.let { TurkishOrder.indexOf(sorted, it) } ?: sorted.size,
        size = sorted.size,
    )

    /** Sınırlara göre hâlâ mümkün olan baş harfler (klavye soluklaştırma). */
    fun possibleFirstLetters(): Set<Char> {
        val lowRank = lowerBound?.let { TurkishOrder.rankOf(it.first()) }
        val highRank = upperBound?.let { TurkishOrder.rankOf(it.first()) }
        return TurkishOrder.LETTERS.filter { letter ->
            val r = TurkishOrder.rankOf(letter)
            (lowRank == null || r >= lowRank) && (highRank == null || r <= highRank)
        }.toSet()
    }

    fun type(letter: Char): KiskacState = when {
        status != KiskacStatus.RUNNING -> this
        current.length >= WORD_LENGTH -> this
        letter !in ALPHABET -> this
        else -> copy(current = current + letter)
    }

    fun erase(): KiskacState =
        if (status == KiskacStatus.RUNNING && current.isNotEmpty()) {
            copy(current = current.dropLast(1))
        } else {
            this
        }

    fun submit(isAllowed: (String) -> Boolean): KiskacState {
        if (status != KiskacStatus.RUNNING) return this
        if (current.length < WORD_LENGTH || !isAllowed(current)) {
            return copy(invalidEvents = invalidEvents + 1, lastInvalid = KiskacInvalid.NOT_IN_LIST)
        }
        if (guesses.any { it.word == current }) {
            return copy(invalidEvents = invalidEvents + 1, lastInvalid = KiskacInvalid.ALREADY_TRIED)
        }

        val cmp = TurkishOrder.compare(answer, current)
        val newGuesses = guesses + KiskacGuess(word = current, hiddenIsAfter = cmp > 0)
        val newStatus = when {
            cmp == 0 -> KiskacStatus.WON
            newGuesses.size >= MAX_GUESSES -> KiskacStatus.LOST
            else -> KiskacStatus.RUNNING
        }
        return copy(guesses = newGuesses, current = "", status = newStatus)
    }

    companion object {
        const val WORD_LENGTH = 5
        const val MAX_GUESSES = 12
        private const val DAILY_SEED = 0x4B15_4AC0_77E1L

        val ALPHABET: Set<Char> = TurkishOrder.LETTERS.toSet()

        /**
         * Günlük bulmaca: Beş Harf'teki gibi elle kodlanmış LCG karması
         * (Kotlin sürümünden bağımsız); tohum farklı olduğundan aynı günün
         * Kıskaç kelimesi Beş Harf'inkiyle çakışmaz.
         */
        fun daily(answers: List<String>, epochDay: Long): KiskacState {
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
            return KiskacState(answer = answers[order[pos]], dailyDay = epochDay)
        }

        fun free(answers: List<String>, seed: Long = Random.nextLong()): KiskacState =
            KiskacState(answer = answers[Random(seed).nextInt(answers.size)])
    }
}
