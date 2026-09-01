package com.za.games.dizgi

import kotlin.random.Random

enum class DizgiStatus { RUNNING, FINISHED }

enum class DizgiInvalid {
    EMPTY, NOT_LINE, GAP, NOT_CONNECTED, CENTER_REQUIRED, SHORT_WORD,
    INVALID_WORD, EXCHANGE_UNAVAILABLE,
}

enum class DizgiMoveKind { PLACE, PASS, EXCHANGE }

/** Tahtadaki ya da eldeki bir taş. Joker yerleştirilince seçilen harfi taşır, 0 puandır. */
data class DizgiTile(val letter: Char, val isJoker: Boolean = false) {
    val points: Int
        get() = if (isJoker) 0 else DizgiLetters.pointsOf(letter)
}

data class DizgiPlayer(val rack: List<DizgiTile>, val score: Int = 0)

data class DizgiMove(
    val player: Int,
    val kind: DizgiMoveKind,
    val words: List<String> = emptyList(),
    val gained: Int = 0,
    val bingo: Boolean = false,
)

/**
 * Dizgi motoru: aynı cihazda elden ele oynanan kelime tahtası.
 *
 * Kurallar: ilk kelime merkezden geçer; sonraki her hamle mevcut taşlara
 * değer; hamledeki taşlar tek satır/sütunda ve boşluksuz dizilir; oluşan
 * ana kelime ve tüm çapraz kelimeler sözlükte olmalıdır. Puan: harf/kelime
 * katları yalnızca o hamlede konan taşlar için sayılır; 7 taşı birden
 * kullanmak +50. Oyun, torba boşken bir oyuncunun elini bitirmesiyle
 * (kalanların taş puanlarını alır, kalanlar kendi taşlarını düşer) ya da
 * üst üste 2 tur puansız hamleyle (herkes kendi taşlarını düşer) biter.
 *
 * Sözlük dışarıdan verilir ([submit] `isWord` alır); motor determinsttir:
 * aynı tohum ve hamle dizisi aynı sonucu üretir.
 */
data class DizgiState(
    val players: List<DizgiPlayer>,
    val bag: List<DizgiTile>,
    val seed: Long,
    val board: Map<Int, DizgiTile> = emptyMap(),
    /** Bu turda konulmuş, henüz onaylanmamış taşlar: hücre -> taş. */
    val pending: Map<Int, DizgiTile> = emptyMap(),
    /** Bekleyen taşların eldeki kaynağı: hücre -> rack indeksi. */
    val pendingRack: Map<Int, Int> = emptyMap(),
    val current: Int = 0,
    val status: DizgiStatus = DizgiStatus.RUNNING,
    /** Üst üste puansız hamle sayısı (pas + değişim). */
    val scorelessTurns: Int = 0,
    val moveCount: Int = 0,
    val lastMove: DizgiMove? = null,
    val invalidEvents: Int = 0,
    val lastInvalid: DizgiInvalid? = null,
    val invalidWords: List<String> = emptyList(),
) {

    /** Eldeki taşlardan bu turda tahtaya konmamış olanların indeksleri. */
    val availableRack: List<Int>
        get() = players[current].rack.indices.filter { it !in pendingRack.values }

    private fun occupied(cell: Int): DizgiTile? = pending[cell] ?: board[cell]

    // --- Taş koyma / geri alma ---

    fun place(cell: Int, rackIndex: Int, jokerAs: Char? = null): DizgiState {
        if (status != DizgiStatus.RUNNING) return this
        if (cell !in 0 until DizgiBoard.CELLS || occupied(cell) != null) return this
        val rack = players[current].rack
        if (rackIndex !in rack.indices || rackIndex in pendingRack.values) return this
        val tile = rack[rackIndex]
        val letter = if (tile.isJoker) {
            jokerAs?.takeIf { DizgiLetters.isLetter(it) } ?: return this
        } else {
            tile.letter
        }
        return copy(
            pending = pending + (cell to DizgiTile(letter, tile.isJoker)),
            pendingRack = pendingRack + (cell to rackIndex),
        )
    }

    fun recall(cell: Int): DizgiState =
        if (cell in pending) copy(pending = pending - cell, pendingRack = pendingRack - cell) else this

    fun recallAll(): DizgiState =
        if (pending.isEmpty()) this else copy(pending = emptyMap(), pendingRack = emptyMap())

    // --- Hamle onayı ---

    fun submit(isWord: (String) -> Boolean): DizgiState {
        if (status != DizgiStatus.RUNNING) return this
        if (pending.isEmpty()) return invalid(DizgiInvalid.EMPTY)

        val cells = pending.keys
        val sameRow = cells.map { it / DizgiBoard.SIZE }.distinct().size == 1
        val sameCol = cells.map { it % DizgiBoard.SIZE }.distinct().size == 1
        if (!sameRow && !sameCol) return invalid(DizgiInvalid.NOT_LINE)

        // Boşluk denetimi: dizinin uçları arasındaki her hücre dolu olmalı.
        if (cells.size > 1) {
            val step = if (sameRow) 1 else DizgiBoard.SIZE
            val lo = cells.min()
            val hi = cells.max()
            var c = lo
            while (c <= hi) {
                if (occupied(c) == null) return invalid(DizgiInvalid.GAP)
                c += step
            }
        }

        if (board.isEmpty()) {
            if (DizgiBoard.CENTER !in cells) return invalid(DizgiInvalid.CENTER_REQUIRED)
        } else {
            val touches = cells.any { cell -> neighbors(cell).any { it in board } }
            if (!touches) return invalid(DizgiInvalid.NOT_CONNECTED)
        }

        // Oluşan kelimeler: ana kelime + her yeni taşın dikey/yatay çaprazı.
        val words = formedWords(mainHorizontal = sameRow)
        if (words.isEmpty()) return invalid(DizgiInvalid.SHORT_WORD)
        val bad = words.map { it.first }.filterNot(isWord).distinct()
        if (bad.isNotEmpty()) return invalid(DizgiInvalid.INVALID_WORD, bad)

        var gained = 0
        for ((_, span) in words) gained += scoreWord(span)
        val bingo = pending.size == RACK_SIZE
        if (bingo) gained += BINGO_BONUS

        // Taahhüt: taşlar tahtaya, el tamamlanır, sıra geçer.
        val used = pendingRack.values.toSet()
        val keptRack = players[current].rack.filterIndexed { i, _ -> i !in used }
        val drawn = bag.takeLast(RACK_SIZE - keptRack.size)
        val newRack = keptRack + drawn
        val newBag = bag.dropLast(drawn.size)

        var newPlayers = players.mapIndexed { i, p ->
            if (i == current) p.copy(rack = newRack, score = p.score + gained) else p
        }
        var newStatus = DizgiStatus.RUNNING
        var finalGained = gained
        if (newRack.isEmpty() && newBag.isEmpty()) {
            // Elini bitiren, kalanların taş puanlarını alır; onlar kendi taşlarını düşer.
            val remaining = newPlayers.map { p -> p.rack.sumOf { it.points } }
            finalGained += remaining.sum()
            newPlayers = newPlayers.mapIndexed { i, p ->
                if (i == current) p.copy(score = p.score + remaining.sum())
                else p.copy(score = p.score - remaining[i])
            }
            newStatus = DizgiStatus.FINISHED
        }

        return copy(
            board = board + pending,
            pending = emptyMap(),
            pendingRack = emptyMap(),
            players = newPlayers,
            bag = newBag,
            current = if (newStatus == DizgiStatus.RUNNING) next() else current,
            status = newStatus,
            scorelessTurns = 0,
            moveCount = moveCount + 1,
            lastMove = DizgiMove(current, DizgiMoveKind.PLACE, words.map { it.first }, finalGained, bingo),
            lastInvalid = null,
            invalidWords = emptyList(),
        )
    }

    // --- Pas ve taş değişimi ---

    fun pass(): DizgiState {
        if (status != DizgiStatus.RUNNING) return this
        return withScorelessTurn(DizgiMove(current, DizgiMoveKind.PASS))
    }

    /** Seçilen taşları torbayla değiştirir; torbada en az [RACK_SIZE] taş gerekir. */
    fun exchange(rackIndices: List<Int>): DizgiState {
        if (status != DizgiStatus.RUNNING) return this
        val rack = players[current].rack
        val indices = rackIndices.distinct().filter { it in rack.indices }
        if (indices.isEmpty()) return this
        if (bag.size < RACK_SIZE) return invalid(DizgiInvalid.EXCHANGE_UNAVAILABLE)

        // Önce çekilir, iade edilen taşlar sonra torbaya karıştırılır.
        val rng = Random(seed)
        val returned = indices.map { rack[it].asBagTile() }
        val kept = rack.filterIndexed { i, _ -> i !in indices.toSet() }
        val drawn = bag.takeLast(indices.size)
        val newBag = (bag.dropLast(indices.size) + returned).shuffled(rng)
        val newPlayers = players.mapIndexed { i, p ->
            if (i == current) p.copy(rack = kept + drawn) else p
        }
        return withScorelessTurn(
            DizgiMove(current, DizgiMoveKind.EXCHANGE),
            base = copy(players = newPlayers, bag = newBag, seed = rng.nextLong()),
        )
    }

    /** Puansız hamle: pas/değişim. Üst üste 2 tur olursa oyun biter. */
    private fun withScorelessTurn(move: DizgiMove, base: DizgiState = this): DizgiState {
        val turns = scorelessTurns + 1
        val ending = turns >= 2 * players.size
        val newPlayers = if (ending) {
            base.players.map { p -> p.copy(score = p.score - p.rack.sumOf { it.points }) }
        } else {
            base.players
        }
        return base.copy(
            pending = emptyMap(),
            pendingRack = emptyMap(),
            players = newPlayers,
            current = if (ending) current else next(),
            status = if (ending) DizgiStatus.FINISHED else DizgiStatus.RUNNING,
            scorelessTurns = turns,
            moveCount = moveCount + 1,
            lastMove = move,
            lastInvalid = null,
            invalidWords = emptyList(),
        )
    }

    // --- Kelime kurma ve puanlama ---

    /** Ana + çapraz kelimeler: (kelime, hücre dizisi) listesi. En az 2 harfliler. */
    private fun formedWords(mainHorizontal: Boolean): List<Pair<String, List<Int>>> {
        val words = mutableListOf<Pair<String, List<Int>>>()
        if (pending.size == 1) {
            // Tek taş: her iki eksendeki şerit de kelime sayılır.
            val cell = pending.keys.first()
            val h = span(cell, horizontal = true)
            val v = span(cell, horizontal = false)
            if (h.size > 1) words += h.toWord()
            if (v.size > 1) words += v.toWord()
        } else {
            val main = span(pending.keys.first(), horizontal = mainHorizontal)
            if (main.size > 1) words += main.toWord()
            for (cell in pending.keys.sorted()) {
                val cross = span(cell, horizontal = !mainHorizontal)
                if (cross.size > 1) words += cross.toWord()
            }
        }
        return words
    }

    /** [cell] üzerinden geçen yatay/dikey dolu hücre şeridi. */
    private fun span(cell: Int, horizontal: Boolean): List<Int> {
        val step = if (horizontal) 1 else DizgiBoard.SIZE
        val row = cell / DizgiBoard.SIZE
        val col = cell % DizgiBoard.SIZE
        var start = cell
        while (true) {
            val prev = start - step
            if (prev < 0 || occupied(prev) == null) break
            if (horizontal && prev / DizgiBoard.SIZE != row) break
            if (!horizontal && prev % DizgiBoard.SIZE != col) break
            start = prev
        }
        val cells = mutableListOf<Int>()
        var c = start
        while (c < DizgiBoard.CELLS && occupied(c) != null) {
            if (horizontal && c / DizgiBoard.SIZE != row) break
            if (!horizontal && c % DizgiBoard.SIZE != col) break
            cells += c
            c += step
        }
        return cells
    }

    private fun List<Int>.toWord(): Pair<String, List<Int>> =
        buildString { this@toWord.forEach { append(occupied(it)!!.letter) } } to this

    /** Kelime puanı: kat karları yalnızca bu hamlede konan taşlara işler. */
    private fun scoreWord(span: List<Int>): Int {
        var sum = 0
        var wordMultiplier = 1
        for (cell in span) {
            val tile = occupied(cell)!!
            var letterPoints = tile.points
            if (cell in pending) {
                when (DizgiBoard.premium(cell)) {
                    Premium.DL -> letterPoints *= 2
                    Premium.TL -> letterPoints *= 3
                    Premium.DW -> wordMultiplier *= 2
                    Premium.TW -> wordMultiplier *= 3
                    Premium.NONE -> {}
                }
            }
            sum += letterPoints
        }
        return sum * wordMultiplier
    }

    // --- Yardımcılar ---

    private fun next(): Int = (current + 1) % players.size

    private fun neighbors(cell: Int): List<Int> {
        val row = cell / DizgiBoard.SIZE
        val col = cell % DizgiBoard.SIZE
        return buildList {
            if (col > 0) add(cell - 1)
            if (col < DizgiBoard.SIZE - 1) add(cell + 1)
            if (row > 0) add(cell - DizgiBoard.SIZE)
            if (row < DizgiBoard.SIZE - 1) add(cell + DizgiBoard.SIZE)
        }
    }

    private fun invalid(reason: DizgiInvalid, words: List<String> = emptyList()): DizgiState =
        copy(invalidEvents = invalidEvents + 1, lastInvalid = reason, invalidWords = words)

    private fun DizgiTile.asBagTile(): DizgiTile =
        if (isJoker) DizgiTile(DizgiLetters.JOKER, isJoker = true) else this

    companion object {
        const val RACK_SIZE = 7
        const val BINGO_BONUS = 50
        const val MIN_PLAYERS = 2
        const val MAX_PLAYERS = 4

        fun new(playerCount: Int, seed: Long): DizgiState {
            require(playerCount in MIN_PLAYERS..MAX_PLAYERS) { "oyuncu: $playerCount" }
            val rng = Random(seed)
            var bag = DizgiLetters.bag().shuffled(rng)
            val players = List(playerCount) {
                val rack = bag.takeLast(RACK_SIZE)
                bag = bag.dropLast(RACK_SIZE)
                DizgiPlayer(rack)
            }
            return DizgiState(players = players, bag = bag, seed = rng.nextLong())
        }
    }
}
