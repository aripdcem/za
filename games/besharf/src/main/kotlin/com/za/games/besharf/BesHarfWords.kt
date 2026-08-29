package com.za.games.besharf

/**
 * Gömülü kelime listeleri. Kaynak dosyalar tools/gen_words.py ile
 * Zemberek (Apache-2.0) ve FrequencyWords (CC-BY-SA-4.0) verisinden
 * türetilir; tamamen çevrimdışıdır.
 */
object BesHarfWords {

    /** Cevap havuzu: yaygın, elden geçirilmiş 5 harfli kelimeler. */
    val answers: List<String> by lazy { load("answers.txt") }

    /** Geçerli tahminler: cevaplar dahil geniş küme. */
    val allowed: Set<String> by lazy { (load("allowed.txt") + answers).toHashSet() }

    fun isAllowed(word: String): Boolean = word in allowed

    private fun load(name: String): List<String> =
        requireNotNull(BesHarfWords::class.java.getResourceAsStream("/besharf/$name")) {
            "Kelime listesi bulunamadı: $name"
        }.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.map { it.trim() }.filter { it.isNotEmpty() }.toList()
        }
}
