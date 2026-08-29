package com.za.games.tetris

import kotlin.math.pow
import kotlin.random.Random

enum class TetrisStatus { RUNNING, PAUSED, OVER }

/**
 * Oyunun tamamı: değişmez (immutable) bir durum makinesi.
 *
 * Her hamle (`tick`, `moveLeft`, `hardDrop`...) yeni bir [TetrisState] döndürür.
 * Rastgelelik `bagSeed` üzerinden deterministiktir: aynı tohumla başlayan iki
 * oyun, aynı hamlelerle birebir aynı sonucu üretir. Bu, motoru test edilebilir
 * ve ileride "yeniden oynatma" (replay) gibi özelliklere hazır kılar.
 *
 * Kurallar: 10x20 tahta, 7'li torba (7-bag) rastgeleliği, SRS duvar tekmeleri,
 * hold + hayalet taş, Guideline skorlaması (100/300/500/800 x seviye,
 * yumuşak düşüş +1/hücre, sert düşüş +2/hücre), her 10 satırda seviye atlama.
 */
data class TetrisState(
    val width: Int,
    val height: Int,
    /** Kilitlenmiş hücreler; board[satır][sütun], üst satır 0. Boş hücre null. */
    val board: List<List<Tetromino?>>,
    val active: ActivePiece,
    /** Sıradaki taşlar; en az [VISIBLE_NEXT] eleman garanti edilir. */
    val next: List<Tetromino>,
    val hold: Tetromino?,
    /** Bu taş düşerken hold hakkı kullanıldı mı? Kilitlenince sıfırlanır. */
    val holdUsed: Boolean,
    val score: Long,
    val lines: Int,
    val status: TetrisStatus,
    /** Torba yenilemede kullanılacak RNG tohumu. */
    val bagSeed: Long,
    /** Son kilitlenmede temizlenen satır sayısı (arayüz efektleri için). */
    val lastClear: Int = 0,
) {

    /** Seviye: her 10 satırda bir artar, 1'den başlar. */
    val level: Int get() = lines / 10 + 1

    /** Aktif taşın bırakıldığında ineceği konum (hayalet taş). */
    val ghost: ActivePiece
        get() {
            var drop = 0
            while (fits(active.moved(drop + 1, 0))) drop++
            return active.moved(drop, 0)
        }

    private fun fits(piece: ActivePiece): Boolean = piece.cells.all { (r, c) ->
        c in 0 until width && r < height && (r < 0 || board[r][c] == null)
    }

    fun moveLeft(): TetrisState = shifted(0, -1)

    fun moveRight(): TetrisState = shifted(0, 1)

    private fun shifted(dRow: Int, dCol: Int): TetrisState {
        if (status != TetrisStatus.RUNNING) return this
        val moved = active.moved(dRow, dCol)
        return if (fits(moved)) copy(active = moved) else this
    }

    fun rotate(clockwise: Boolean = true): TetrisState {
        if (status != TetrisStatus.RUNNING) return this
        val turned = active.rotated(clockwise)
        for ((dRow, dCol) in Srs.kicks(active.type, active.rotation, turned.rotation)) {
            val candidate = turned.moved(dRow, dCol)
            if (fits(candidate)) return copy(active = candidate)
        }
        return this
    }

    /** Yerçekimi adımı: bir satır in; inemiyorsa taşı kilitle. */
    fun tick(): TetrisState = when {
        status != TetrisStatus.RUNNING -> this
        fits(active.moved(1, 0)) -> copy(active = active.moved(1, 0))
        else -> locked()
    }

    /** Oyuncunun hızlandırdığı düşüş: hücre başına +1 puan; zeminde ise kilitler. */
    fun softDrop(): TetrisState = when {
        status != TetrisStatus.RUNNING -> this
        fits(active.moved(1, 0)) -> copy(active = active.moved(1, 0), score = score + 1)
        else -> locked()
    }

    /** Sert düşüş: taş dibe iner ve anında kilitlenir; hücre başına +2 puan. */
    fun hardDrop(): TetrisState {
        if (status != TetrisStatus.RUNNING) return this
        var drop = 0
        while (fits(active.moved(drop + 1, 0))) drop++
        return copy(active = active.moved(drop, 0), score = score + 2L * drop).locked()
    }

    /** Aktif taşı beklemeye al; taş başına bir kez kullanılabilir. */
    fun holdPiece(): TetrisState {
        if (status != TetrisStatus.RUNNING || holdUsed) return this
        val stored = active.type
        val held = hold
        return if (held == null) {
            val (queue, seed) = refilled(next, bagSeed)
            copy(
                active = spawnPiece(queue.first(), width),
                next = queue.drop(1),
                bagSeed = seed,
                hold = stored,
                holdUsed = true,
            )
        } else {
            copy(active = spawnPiece(held, width), hold = stored, holdUsed = true)
        }
    }

    fun pause(): TetrisState =
        if (status == TetrisStatus.RUNNING) copy(status = TetrisStatus.PAUSED) else this

    fun togglePause(): TetrisState = when (status) {
        TetrisStatus.RUNNING -> copy(status = TetrisStatus.PAUSED)
        TetrisStatus.PAUSED -> copy(status = TetrisStatus.RUNNING)
        TetrisStatus.OVER -> this
    }

    private fun locked(): TetrisState {
        val cells = active.cells
        // Tahtanın üstünde kilitlenme = tavana dayanma = oyun sonu.
        if (cells.any { it.row < 0 }) return copy(status = TetrisStatus.OVER)

        val grid = board.map { it.toMutableList() }
        for ((r, c) in cells) grid[r][c] = active.type

        val remaining = grid.filterNot { row -> row.all { it != null } }
        val cleared = height - remaining.size
        val emptyRows = List(cleared) { List<Tetromino?>(width) { null } }
        val newBoard = emptyRows + remaining.map { it.toList() }

        val gained = when (cleared) {
            1 -> 100L
            2 -> 300L
            3 -> 500L
            4 -> 800L
            else -> 0L
        } * level

        val (queue, seed) = refilled(next, bagSeed)
        val spawned = spawnPiece(queue.first(), width)
        val state = copy(
            board = newBoard,
            active = spawned,
            next = queue.drop(1),
            holdUsed = false,
            score = score + gained,
            lines = lines + cleared,
            bagSeed = seed,
            lastClear = cleared,
        )
        return if (state.fits(spawned)) state else state.copy(status = TetrisStatus.OVER)
    }

    companion object {
        const val WIDTH = 10
        const val HEIGHT = 20
        const val VISIBLE_NEXT = 3

        /** Taşlar tahtanın hemen üstünde, alt sırası görünür şekilde doğar. */
        private const val SPAWN_ROW = -1

        fun newGame(seed: Long = Random.nextLong()): TetrisState {
            val rng = Random(seed)
            val queue = Tetromino.entries.shuffled(rng) + Tetromino.entries.shuffled(rng)
            return TetrisState(
                width = WIDTH,
                height = HEIGHT,
                board = List(HEIGHT) { List(WIDTH) { null } },
                active = spawnPiece(queue.first(), WIDTH),
                next = queue.drop(1),
                hold = null,
                holdUsed = false,
                score = 0L,
                lines = 0,
                status = TetrisStatus.RUNNING,
                bagSeed = rng.nextLong(),
            )
        }

        private fun spawnPiece(type: Tetromino, width: Int): ActivePiece =
            ActivePiece(type = type, rotation = 0, row = SPAWN_ROW, col = (width - type.boxSize) / 2)

        /** Kuyruk kısalınca yeni bir 7'li torba ekler; tohumu ilerletir. */
        private fun refilled(queue: List<Tetromino>, seed: Long): Pair<List<Tetromino>, Long> {
            if (queue.size > VISIBLE_NEXT + 1) return queue to seed
            val rng = Random(seed)
            return (queue + Tetromino.entries.shuffled(rng)) to rng.nextLong()
        }
    }
}

/**
 * Guideline yerçekimi eğrisi: (0.8 - (seviye-1) * 0.007)^(seviye-1) saniye.
 * Seviye 1'de 1000 ms; yüksek seviyelerde 50 ms tabanına iner.
 */
fun gravityMillis(level: Int): Long {
    val step = level - 1
    val seconds = (0.8 - step * 0.007).pow(step)
    return (seconds * 1000).toLong().coerceAtLeast(50L)
}
