package com.za.games.turetme

import com.za.games.sozluk.WordFile
import com.za.games.sozluk.WordLang

/**
 * Gömülü kelime listeleri, dil başına bir küme (tools/gen_wordlists.py üretir;
 * kaynaklar ve lisansları tools/SOURCES.md'de). Tamamen çevrimdışıdır.
 */
class TuretmeWords private constructor(val lang: WordLang) {

    /** 3-7 harfli geçerli kelimeler (alt kelime doğrulaması). */
    val valid: Set<String> by lazy {
        WordFile.read(javaClass, "/turetme/${lang.tag}/valid.txt").toHashSet()
    }

    /** 6-7 harfli taban kelimeler; her biri en az 15 alt kelime garantili. */
    val bases: List<String> by lazy {
        WordFile.readPlain(javaClass, "/turetme/${lang.tag}/bases.txt")
    }

    companion object {
        private val cache = HashMap<WordLang, TuretmeWords>()

        @Synchronized
        fun of(lang: WordLang): TuretmeWords = cache.getOrPut(lang) { TuretmeWords(lang) }
    }
}
