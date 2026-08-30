package com.za.games.snake

import kotlin.random.Random

/** Tahta hücresi; satırlar yukarıdan aşağı artar. */
data class Cell(val row: Int, val col: Int)

enum class SnakeDir(val dRow: Int, val dCol: Int) {
    UP(-1, 0), DOWN(1, 0), LEFT(0, -1), RIGHT(0, 1);

    val opposite: SnakeDir
        get() = when (this) {
            UP -> DOWN
            DOWN -> UP
            LEFT -> RIGHT
            RIGHT -> LEFT
        }
}

enum class SnakeStatus { RUNNING, PAUSED, OVER }

/**
 * Yılan motoru: değişmez durum makinesi.
 *
 * Klasik kurallar: yılan her tikte bir hücre ilerler; duvara ya da kendine
 * çarpmak oyunu bitirir (kuyruğun o tikte boşalttığı hücre serbesttir).
 * Yem +10 puan ve +1 uzunluk kazandırır; her yemle oyun bir miktar hızlanır.
 * Yem konumu [seed] üzerinden deterministiktir.
 */
data class SnakeState(
    val width: Int,
    val height: Int,
    /** Baş ilk elemandır. */
    val body: List<Cell>,
    /** Son tikte uygulanan yön. */
    val dir: SnakeDir,
    /** Bir sonraki tikte uygulanacak (tamponlanmış) yön. */
    val pending: SnakeDir,
    /** Ondan sonraki tik için kuyruğa alınan ikinci dönüş (hızlı köşeler). */
    val queued: SnakeDir? = null,
    val food: Cell,
    val score: Long,
    /** Yenen yem sayısı; hız bundan türetilir. */
    val foods: Int,
    val status: SnakeStatus,
    val seed: Long,
) {

    /**
     * Yön girişi. En fazla iki dönüş tamponlanır: biri sonraki tik için
     * ([pending]), biri onun ardından ([queued]) — hızlı köşe dönüşlerinde
     * ikinci giriş kaybolmaz. Sıradaki son yöne göre geri dönüş engellenir.
     */
    fun turn(newDir: SnakeDir): SnakeState {
        if (status != SnakeStatus.RUNNING) return this
        val last = queued ?: pending
        if (newDir == last || newDir == last.opposite) return this
        return when {
            pending == dir -> copy(pending = newDir) // tampon boş
            queued == null -> copy(queued = newDir) // ikinci dönüşü sıraya al
            else -> this // tampon dolu
        }
    }

    fun tick(): SnakeState {
        if (status != SnakeStatus.RUNNING) return this
        val d = pending
        val nextPending = queued ?: d
        val head = body.first()
        val next = Cell(head.row + d.dRow, head.col + d.dCol)

        if (next.row !in 0 until height || next.col !in 0 until width) {
            return copy(status = SnakeStatus.OVER, dir = d)
        }

        val ate = next == food
        // Yemiyorsak kuyruk bu tikte boşalır; o hücreye girmek serbesttir.
        val obstacles = if (ate) body else body.dropLast(1)
        if (next in obstacles) return copy(status = SnakeStatus.OVER, dir = d)

        val newBody = listOf(next) + if (ate) body else body.dropLast(1)
        if (!ate) return copy(body = newBody, dir = d, pending = nextPending, queued = null)

        val rng = Random(seed)
        val empties = buildList {
            for (r in 0 until height) {
                for (c in 0 until width) {
                    val cell = Cell(r, c)
                    if (cell !in newBody) add(cell)
                }
            }
        }
        val grown = copy(
            body = newBody,
            dir = d,
            pending = nextPending,
            queued = null,
            score = score + 10,
            foods = foods + 1,
            seed = rng.nextLong(),
        )
        // Tahta tamamen dolduysa oyun (zaferle) biter.
        if (empties.isEmpty()) return grown.copy(status = SnakeStatus.OVER)
        return grown.copy(food = empties[rng.nextInt(empties.size)])
    }

    fun pause(): SnakeState =
        if (status == SnakeStatus.RUNNING) copy(status = SnakeStatus.PAUSED) else this

    fun togglePause(): SnakeState = when (status) {
        SnakeStatus.RUNNING -> copy(status = SnakeStatus.PAUSED)
        SnakeStatus.PAUSED -> copy(status = SnakeStatus.RUNNING)
        SnakeStatus.OVER -> this
    }

    companion object {
        const val WIDTH = 15
        const val HEIGHT = 20

        fun newGame(seed: Long = Random.nextLong()): SnakeState {
            val rng = Random(seed)
            val row = HEIGHT / 2
            val col = WIDTH / 2
            val body = listOf(Cell(row, col), Cell(row, col - 1), Cell(row, col - 2))
            val empties = buildList {
                for (r in 0 until HEIGHT) {
                    for (c in 0 until WIDTH) {
                        val cell = Cell(r, c)
                        if (cell !in body) add(cell)
                    }
                }
            }
            return SnakeState(
                width = WIDTH,
                height = HEIGHT,
                body = body,
                dir = SnakeDir.RIGHT,
                pending = SnakeDir.RIGHT,
                food = empties[rng.nextInt(empties.size)],
                score = 0L,
                foods = 0,
                status = SnakeStatus.RUNNING,
                seed = rng.nextLong(),
            )
        }
    }
}

/** Tik aralığı: her yemle 3 ms kısalır, 70 ms tabanında durur. */
fun snakeSpeedMillis(foods: Int): Long = (160L - foods * 3L).coerceAtLeast(70L)
