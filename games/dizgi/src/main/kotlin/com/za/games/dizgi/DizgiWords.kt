package com.za.games.dizgi

import com.za.games.sozluk.WordFile
import com.za.games.sozluk.WordLang

/** Gömülü sözlük: 2-15 harfli geçerli kelimeler (tools/gen_wordlists.py üretir). */
class DizgiWords private constructor(val lang: WordLang) {

    val valid: Set<String> by lazy {
        WordFile.read(javaClass, "/dizgi/${lang.tag}/valid.txt").toHashSet()
    }

    companion object {
        private val cache = HashMap<WordLang, DizgiWords>()

        @Synchronized
        fun of(lang: WordLang): DizgiWords = cache.getOrPut(lang) { DizgiWords(lang) }
    }
}
