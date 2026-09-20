package com.aripd.zagames.dizgi

import com.aripd.zagames.sozluk.WordFile
import com.aripd.zagames.sozluk.WordLang

/**
 * Dizgi harf seti: torbadaki adetler ve harf puanları, dil başına.
 *
 * Tablolar tools/gen_wordlists.py ile o dilin sözlük derleminin ağırlıklı harf
 * sıklığından türetilir (sık harf = çok taş, az puan) ve kaynak dosyasında
 * dondurulur: kelime listeleri yenilense de oyunun dengesi kaymaz. Resmî
 * Scrabble dağılımı değildir; Dizgi'ye özgüdür.
 *
 * Dosya biçimi, satır başına "harf adet puan".
 */
class DizgiLetters private constructor(val lang: WordLang) {

    private data class Kind(val count: Int, val points: Int)

    private val kinds: Map<Char, Kind> by lazy {
        WordFile.readPlain(javaClass, "/dizgi/${lang.tag}/letters.txt")
            .associate { line ->
                val (letter, count, points) = line.split(" ")
                letter.single() to Kind(count.toInt(), points.toInt())
            }
    }

    val letters: List<Char> get() = kinds.keys.toList()

    fun isLetter(c: Char): Boolean = c in kinds

    fun pointsOf(letter: Char): Int = kinds[letter]?.points ?: 0

    /** Torba içeriği (karılmamış): 98 harf + 2 joker = 100 taş. */
    fun bag(): List<DizgiTile> = buildList {
        for ((letter, kind) in kinds) {
            repeat(kind.count) { add(DizgiTile(letter)) }
        }
        repeat(JOKER_COUNT) { add(DizgiTile(JOKER, isJoker = true)) }
    }

    companion object {
        const val JOKER = '*'
        const val JOKER_COUNT = 2

        private val cache = HashMap<WordLang, DizgiLetters>()

        @Synchronized
        fun of(lang: WordLang): DizgiLetters = cache.getOrPut(lang) { DizgiLetters(lang) }
    }
}
