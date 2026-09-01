package com.za.games.dizgi

enum class Premium { NONE, DL, TL, DW, TW }

/**
 * 15×15 tahtanın premium kare dizilişi. Dizgi'ye özgü, dört eksende ve
 * köşegende simetrik bir tasarımdır (klasik Scrabble dizilişi değildir):
 * köşeler ÇK (çift kelime), kenar ortaları ÜK (üç kelime), köşegenler ÇK.
 * Ortadaki yıldız ilk kelimenin geçmek zorunda olduğu ÇK karesidir.
 */
object DizgiBoard {

    const val SIZE = 15
    const val CELLS = SIZE * SIZE
    const val CENTER = CELLS / 2 // (7,7)

    // d = çift harf, t = üç harf, D = çift kelime, T = üç kelime, * = merkez.
    private val rows = listOf(
        "D..d...T...d..D",
        ".D...t...t...D.",
        "..D...d.d...D..",
        "d..D...d...D..d",
        "....D.....D....",
        ".t...t...t...t.",
        "..d...d.d...d..",
        "T..d...*...d..T",
        "..d...d.d...d..",
        ".t...t...t...t.",
        "....D.....D....",
        "d..D...d...D..d",
        "..D...d.d...D..",
        ".D...t...t...D.",
        "D..d...T...d..D",
    )

    fun premium(cell: Int): Premium = when (rows[cell / SIZE][cell % SIZE]) {
        'd' -> Premium.DL
        't' -> Premium.TL
        'D', '*' -> Premium.DW
        'T' -> Premium.TW
        else -> Premium.NONE
    }
}
