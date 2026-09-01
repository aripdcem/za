package com.za.games.dizgi

/**
 * Dizgi harf seti: torbadaki adetler ve harf puanları.
 *
 * Tablo tools/gen_dizgi.py ile sözlük derleminin ağırlıklı harf
 * sıklığından türetilmiş ve burada dondurulmuştur (sık harf = çok taş,
 * az puan). Resmî Scrabble dağılımı değildir; Dizgi'ye özgüdür.
 */
object DizgiLetters {

    const val JOKER = '*'
    const val JOKER_COUNT = 2

    private data class Kind(val count: Int, val points: Int)

    private val kinds: Map<Char, Kind> = mapOf(
        'a' to Kind(11, 1),
        'b' to Kind(5, 3),
        'c' to Kind(1, 6),
        'ç' to Kind(2, 4),
        'd' to Kind(4, 3),
        'e' to Kind(11, 1),
        'f' to Kind(1, 7),
        'g' to Kind(1, 5),
        'ğ' to Kind(1, 6),
        'h' to Kind(2, 4),
        'ı' to Kind(2, 4),
        'i' to Kind(9, 2),
        'j' to Kind(1, 10),
        'k' to Kind(5, 2),
        'l' to Kind(4, 3),
        'm' to Kind(4, 3),
        'n' to Kind(6, 2),
        'o' to Kind(2, 4),
        'ö' to Kind(1, 6),
        'p' to Kind(1, 6),
        'r' to Kind(6, 2),
        's' to Kind(3, 4),
        'ş' to Kind(1, 5),
        't' to Kind(3, 3),
        'u' to Kind(2, 4),
        'ü' to Kind(2, 5),
        'v' to Kind(2, 5),
        'y' to Kind(3, 3),
        'z' to Kind(2, 5),
    )

    val letters: List<Char> = kinds.keys.toList()

    fun isLetter(c: Char): Boolean = c in kinds

    fun pointsOf(letter: Char): Int = kinds[letter]?.points ?: 0

    /** Torba içeriği (karılmamış): 98 harf + 2 joker = 100 taş. */
    fun bag(): List<DizgiTile> = buildList {
        for ((letter, kind) in kinds) {
            repeat(kind.count) { add(DizgiTile(letter)) }
        }
        repeat(JOKER_COUNT) { add(DizgiTile(JOKER, isJoker = true)) }
    }
}
