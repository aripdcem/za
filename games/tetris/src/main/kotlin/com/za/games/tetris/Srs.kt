package com.za.games.tetris

/**
 * Super Rotation System (SRS) duvar tekmesi (wall kick) tabloları.
 *
 * Tablolar Tetris Guideline'daki (dx, dy) çiftleriyle yazılmıştır; dy yukarı
 * yönde pozitiftir. Tahta koordinatına çevirirken dCol = dx, dRow = -dy olur.
 */
object Srs {

    private val JLSTZ_KICKS: Map<Pair<Int, Int>, List<Pair<Int, Int>>> = mapOf(
        (0 to 1) to listOf(0 to 0, -1 to 0, -1 to 1, 0 to -2, -1 to -2),
        (1 to 0) to listOf(0 to 0, 1 to 0, 1 to -1, 0 to 2, 1 to 2),
        (1 to 2) to listOf(0 to 0, 1 to 0, 1 to -1, 0 to 2, 1 to 2),
        (2 to 1) to listOf(0 to 0, -1 to 0, -1 to 1, 0 to -2, -1 to -2),
        (2 to 3) to listOf(0 to 0, 1 to 0, 1 to 1, 0 to -2, 1 to -2),
        (3 to 2) to listOf(0 to 0, -1 to 0, -1 to -1, 0 to 2, -1 to 2),
        (3 to 0) to listOf(0 to 0, -1 to 0, -1 to -1, 0 to 2, -1 to 2),
        (0 to 3) to listOf(0 to 0, 1 to 0, 1 to 1, 0 to -2, 1 to -2),
    )

    private val I_KICKS: Map<Pair<Int, Int>, List<Pair<Int, Int>>> = mapOf(
        (0 to 1) to listOf(0 to 0, -2 to 0, 1 to 0, -2 to -1, 1 to 2),
        (1 to 0) to listOf(0 to 0, 2 to 0, -1 to 0, 2 to 1, -1 to -2),
        (1 to 2) to listOf(0 to 0, -1 to 0, 2 to 0, -1 to 2, 2 to -1),
        (2 to 1) to listOf(0 to 0, 1 to 0, -2 to 0, 1 to -2, -2 to 1),
        (2 to 3) to listOf(0 to 0, 2 to 0, -1 to 0, 2 to 1, -1 to -2),
        (3 to 2) to listOf(0 to 0, -2 to 0, 1 to 0, -2 to -1, 1 to 2),
        (3 to 0) to listOf(0 to 0, 1 to 0, -2 to 0, 1 to -2, -2 to 1),
        (0 to 3) to listOf(0 to 0, -1 to 0, 2 to 0, -1 to 2, 2 to -1),
    )

    /** Denenecek (dRow, dCol) ofsetleri, sırayla. İlk sığan kazanır. */
    fun kicks(type: Tetromino, from: Int, to: Int): List<Pair<Int, Int>> {
        val table = when (type) {
            Tetromino.O -> return listOf(0 to 0)
            Tetromino.I -> I_KICKS
            else -> JLSTZ_KICKS
        }
        return table.getValue(from to to).map { (dx, dy) -> -dy to dx }
    }
}
