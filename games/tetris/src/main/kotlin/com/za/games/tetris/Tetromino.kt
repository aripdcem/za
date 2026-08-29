package com.za.games.tetris

/** Tahta üzerindeki bir hücre konumu. Satırlar yukarıdan aşağı artar. */
data class Cell(val row: Int, val col: Int)

/**
 * Yedi standart tetromino. Her taşın "spawn" (doğuş) hücreleri, SRS'in
 * kabul ettiği kutu (bounding box) içinde tanımlanır; dört rotasyon durumu
 * kutu içinde saat yönünde döndürülerek önceden hesaplanır.
 */
enum class Tetromino(val boxSize: Int, spawnCells: List<Pair<Int, Int>>) {
    I(4, listOf(1 to 0, 1 to 1, 1 to 2, 1 to 3)),
    J(3, listOf(0 to 0, 1 to 0, 1 to 1, 1 to 2)),
    L(3, listOf(0 to 2, 1 to 0, 1 to 1, 1 to 2)),
    O(2, listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1)),
    S(3, listOf(0 to 1, 0 to 2, 1 to 0, 1 to 1)),
    T(3, listOf(0 to 1, 1 to 0, 1 to 1, 1 to 2)),
    Z(3, listOf(0 to 0, 0 to 1, 1 to 1, 1 to 2));

    /** rotations[r] = r kez saat yönünde döndürülmüş hücreler (kutu koordinatında). */
    val rotations: List<List<Cell>> = buildList {
        var cells = spawnCells.map { (r, c) -> Cell(r, c) }
        repeat(4) {
            add(cells)
            cells = cells.map { Cell(it.col, boxSize - 1 - it.row) }
        }
    }
}

/** Tahtada düşmekte olan aktif taş. (row, col) kutunun sol üst köşesidir. */
data class ActivePiece(
    val type: Tetromino,
    val rotation: Int,
    val row: Int,
    val col: Int,
) {
    /** Taşın tahta koordinatındaki mutlak hücreleri. */
    val cells: List<Cell>
        get() = type.rotations[rotation].map { Cell(row + it.row, col + it.col) }

    fun moved(dRow: Int, dCol: Int): ActivePiece = copy(row = row + dRow, col = col + dCol)

    fun rotated(clockwise: Boolean): ActivePiece =
        copy(rotation = (rotation + if (clockwise) 1 else 3) % 4)
}
