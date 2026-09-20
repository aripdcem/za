package com.aripd.zagames.besharf

import com.aripd.zagames.sozluk.WordFile
import com.aripd.zagames.sozluk.WordLang

/**
 * Gömülü kelime listeleri, dil başına bir küme.
 *
 * Listeler tools/gen_wordlists.py ile yazım sözlüklerinden ve FrequencyWords
 * sıklık verisinden türetilir (kaynaklar ve lisansları tools/SOURCES.md'de);
 * tamamen çevrimdışıdır. Kıskaç da bu listeleri kullanır.
 *
 * Bir dilin listeleri ilk istendiğinde okunur ve bellekte tutulur: oyuncu dil
 * değiştirip geri döndüğünde dosya yeniden ayrıştırılmaz.
 */
class BesHarfWords private constructor(val lang: WordLang) {

    /** Cevap havuzu: yaygın, elden geçirilmiş 5 harfli kelimeler. */
    val answers: List<String> by lazy {
        WordFile.readPlain(javaClass, "/besharf/${lang.tag}/answers.txt")
    }

    /** Geçerli tahminler: cevaplar dahil geniş küme, dilin sözlük sırasında. */
    val allowed: List<String> by lazy {
        WordFile.read(javaClass, "/besharf/${lang.tag}/allowed.txt")
    }

    private val allowedSet: Set<String> by lazy { (allowed + answers).toHashSet() }

    fun isAllowed(word: String): Boolean = word in allowedSet

    companion object {
        private val cache = HashMap<WordLang, BesHarfWords>()

        @Synchronized
        fun of(lang: WordLang): BesHarfWords = cache.getOrPut(lang) { BesHarfWords(lang) }
    }
}
