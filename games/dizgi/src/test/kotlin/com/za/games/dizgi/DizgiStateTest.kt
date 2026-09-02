package com.za.games.dizgi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DizgiStateTest {

    private fun cell(row: Int, col: Int) = row * DizgiBoard.SIZE + col

    private fun tiles(s: String) = s.map { DizgiTile(it, isJoker = it == DizgiLetters.JOKER) }

    /** Denetlenebilir durum: eller ve torba elle verilir, tahta boş başlar. */
    private fun state(
        rack0: String,
        rack1: String = "eeeeeee",
        bag: String = "",
        board: Map<Int, DizgiTile> = emptyMap(),
    ) = DizgiState(
        players = listOf(DizgiPlayer(tiles(rack0)), DizgiPlayer(tiles(rack1))),
        bag = tiles(bag),
        seed = 42L,
        board = board,
    )

    /** "ev" kelimesi (7,7)-(7,8) hücrelerinde kesinleşmiş tahta. */
    private fun boardWithEv(): Map<Int, DizgiTile> = mapOf(
        cell(7, 7) to DizgiTile('e'),
        cell(7, 8) to DizgiTile('v'),
    )

    private fun dict(vararg words: String): (String) -> Boolean = words.toSet()::contains

    // --- Kurulum ---

    @Test
    fun `bag holds 100 tiles with 2 jokers and dealing is deterministic`() {
        val bag = DizgiLetters.bag()
        assertEquals(100, bag.size)
        assertEquals(2, bag.count { it.isJoker })
        assertTrue(bag.filterNot { it.isJoker }.all { it.points >= 1 })

        val a = DizgiState.new(2, seed = 9L)
        val b = DizgiState.new(2, seed = 9L)
        assertEquals(a, b)
        assertNotEquals(a.players, DizgiState.new(2, seed = 10L).players)
        assertTrue(a.players.all { it.rack.size == DizgiState.RACK_SIZE })
        assertEquals(86, a.bag.size)

        assertEquals(DizgiState.MAX_PLAYERS, DizgiState.new(4, 1L).players.size)
    }

    @Test
    fun `premium layout is symmetric with expected counts`() {
        val all = (0 until DizgiBoard.CELLS).map { DizgiBoard.premium(it) }
        assertEquals(Premium.DW, DizgiBoard.premium(DizgiBoard.CENTER))
        assertEquals(4, all.count { it == Premium.TW })
        assertEquals(21, all.count { it == Premium.DW }) // 20 + merkez
        assertEquals(12, all.count { it == Premium.TL })
        assertEquals(24, all.count { it == Premium.DL })
        for (r in 0 until 15) for (c in 0 until 15) {
            assertEquals(DizgiBoard.premium(cell(r, c)), DizgiBoard.premium(cell(14 - r, c)))
            assertEquals(DizgiBoard.premium(cell(r, c)), DizgiBoard.premium(cell(r, 14 - c)))
            assertEquals(DizgiBoard.premium(cell(r, c)), DizgiBoard.premium(cell(c, r)))
        }
    }

    // --- İlk hamle ve yerleştirme kuralları ---

    @Test
    fun `first word must cross the center star and center doubles it`() {
        var s = state(rack0 = "evaaaaa", bag = "kk")
        val missed = s.place(cell(7, 4), 0).place(cell(7, 5), 1).submit(dict("ev"))
        assertEquals(DizgiInvalid.CENTER_REQUIRED, missed.lastInvalid)
        assertTrue(missed.board.isEmpty())

        s = s.place(cell(7, 7), 0).place(cell(7, 8), 1).submit(dict("ev"))
        assertNull(s.lastInvalid)
        // e=1 + v=5, merkez ÇK -> 12.
        assertEquals(12, s.players[0].score)
        assertEquals(listOf("ev"), s.lastMove!!.words)
        assertEquals(2, s.board.size)
        assertEquals(1, s.current)
        assertEquals(DizgiState.RACK_SIZE, s.players[0].rack.size) // torbadan tamamlandı
        assertEquals(0, s.bag.size)
    }

    @Test
    fun `placement validation catches line, gap, connection and short words`() {
        val s = state(rack0 = "evaaaaa")
        assertEquals(
            DizgiInvalid.NOT_LINE,
            s.place(cell(7, 7), 0).place(cell(8, 8), 1).submit(dict("ev")).lastInvalid,
        )
        assertEquals(
            DizgiInvalid.GAP,
            s.place(cell(7, 7), 0).place(cell(7, 9), 1).submit(dict("ev")).lastInvalid,
        )
        assertEquals(
            DizgiInvalid.SHORT_WORD,
            s.place(cell(7, 7), 0).submit(dict("e")).lastInvalid,
        )
        assertEquals(DizgiInvalid.EMPTY, s.submit(dict("ev")).lastInvalid)

        val far = state(rack0 = "ataaaaa", board = boardWithEv())
            .place(cell(0, 0), 0).place(cell(0, 1), 1).submit(dict("at"))
        assertEquals(DizgiInvalid.NOT_CONNECTED, far.lastInvalid)
    }

    @Test
    fun `invalid words are reported and nothing is committed`() {
        val s = state(rack0 = "ataaaaa", board = boardWithEv())
            .place(cell(8, 7), 0).place(cell(8, 8), 1)
            .submit(dict("at", "ea")) // "vt" sözlükte yok
        assertEquals(DizgiInvalid.INVALID_WORD, s.lastInvalid)
        assertEquals(listOf("vt"), s.invalidWords)
        assertEquals(2, s.pending.size) // taşlar geri alınmadı, tahta değişmedi
        assertEquals(2, s.board.size)
        assertEquals(0, s.players[0].score)
    }

    // --- Çapraz kelimeler ve puanlama ---

    @Test
    fun `a move scores the main word and every cross word it creates`() {
        val s = state(rack0 = "ataaaaa", board = boardWithEv())
            .place(cell(8, 7), 0).place(cell(8, 8), 1)
            .submit(dict("at", "ea", "vt"))
        assertNull(s.lastInvalid)
        // at: a1 + t3×2(ÇH d) = 7; ea: 1+1 = 2; vt: 5 + 6 = 11.
        assertEquals(20, s.players[0].score)
        assertEquals(setOf("at", "ea", "vt"), s.lastMove!!.words.toSet())
    }

    @Test
    fun `a single tile can complete a cross word and premiums count once per move`() {
        val s = state(rack0 = "aaaaaaa", board = boardWithEv())
            .place(cell(6, 8), 0)
            .submit(dict("av"))
        assertNull(s.lastInvalid)
        // (6,8) ÇH: a=1×2 + v=5 -> 7.
        assertEquals(7, s.players[0].score)

        // Eski taşın altındaki premium yeniden işlemez: "ev"i "eve" yap.
        val extend = state(rack0 = "eaaaaaa", board = boardWithEv())
            .place(cell(7, 9), 0)
            .submit(dict("eve"))
        // e1 + v5 + e1 = 7; merkez ÇK eski taşa ait, katlanmaz.
        assertEquals(7, extend.players[0].score)
    }

    @Test
    fun `using all seven tiles earns the bingo bonus`() {
        val s = state(rack0 = "aaaaaaa", bag = "kkkkkkk")
        var play = s
        for ((i, c) in (4..10).withIndex()) play = play.place(cell(7, c), i)
        play = play.submit(dict("aaaaaaa"))
        assertNull(play.lastInvalid)
        // 7×1 ×2 (merkez) + 50 bingo = 64.
        assertEquals(64, play.players[0].score)
        assertTrue(play.lastMove!!.bingo)
    }

    @Test
    fun `joker plays as a chosen letter worth zero points`() {
        val s = state(rack0 = "e*aaaaa")
        val noChoice = s.place(cell(7, 8), 1)
        assertEquals(s, noChoice) // harf seçilmeden joker konamaz

        val played = s.place(cell(7, 7), 0)
            .place(cell(7, 8), 1, jokerAs = 'v')
            .submit(dict("ev"))
        assertNull(played.lastInvalid)
        assertEquals(2, played.players[0].score) // (e1 + joker0) × 2
        assertTrue(played.board.getValue(cell(7, 8)).isJoker)
        assertEquals('v', played.board.getValue(cell(7, 8)).letter)
    }

    @Test
    fun `recall returns pending tiles to the rack`() {
        val s = state(rack0 = "evaaaaa")
        val placed = s.place(cell(7, 7), 0).place(cell(7, 8), 1)
        assertEquals(listOf(2, 3, 4, 5, 6), placed.availableRack)
        assertEquals(6, placed.recall(cell(7, 8)).availableRack.size)
        assertEquals(7, placed.recallAll().availableRack.size)
        assertTrue(placed.recallAll().pending.isEmpty())
    }

    // --- Pas, değişim, bitiş ---

    @Test
    fun `two scoreless rounds end the game and racks are deducted`() {
        var s = state(rack0 = "ev", rack1 = "ab")
        repeat(3) {
            s = s.pass()
            assertEquals(DizgiStatus.RUNNING, s.status)
        }
        s = s.pass()
        assertEquals(DizgiStatus.FINISHED, s.status)
        assertEquals(-6, s.players[0].score) // e1+v5
        assertEquals(-4, s.players[1].score) // a1+b3
    }

    @Test
    fun `exchange needs a full bag draw and is deterministic`() {
        val s = state(rack0 = "abcdefg", bag = "kkkkkkk")
        val done = s.exchange(listOf(0, 1))
        assertNull(done.lastInvalid)
        assertEquals(2, done.players[0].rack.count { it.letter == 'k' })
        assertEquals(7, done.bag.size)
        assertEquals(1, done.current)
        assertEquals(1, done.scorelessTurns)
        assertEquals(done, s.exchange(listOf(0, 1))) // aynı tohum, aynı sonuç

        val small = state(rack0 = "abcdefg", bag = "kkkkkk")
        assertEquals(DizgiInvalid.EXCHANGE_UNAVAILABLE, small.exchange(listOf(0)).lastInvalid)
    }

    @Test
    fun `emptying your rack with an empty bag ends the game with rack bonuses`() {
        val s = state(rack0 = "ev", rack1 = "ab")
            .place(cell(7, 7), 0).place(cell(7, 8), 1)
            .submit(dict("ev"))
        assertEquals(DizgiStatus.FINISHED, s.status)
        // 12 + rakibin taşları (a1+b3=4) = 16; rakip -4.
        assertEquals(16, s.players[0].score)
        assertEquals(-4, s.players[1].score)
        assertEquals(16, s.lastMove!!.gained)
    }

    @Test
    fun `two scoreless rounds can end the game in a tie`() {
        var s = state(rack0 = "ab", rack1 = "ab")
        repeat(3) { s = s.pass() }
        s = s.pass()
        assertEquals(DizgiStatus.FINISHED, s.status)
        assertEquals(s.players[0].score, s.players[1].score)
        assertEquals(-4, s.players[0].score) // a1+b3, ikisi de eşit kaybeder
    }

    @Test
    fun `no moves are accepted after the game finishes`() {
        var s = state(rack0 = "ev", rack1 = "ab")
        repeat(4) { s = s.pass() }
        assertEquals(s, s.pass())
        assertEquals(s, s.place(cell(7, 7), 0))
        assertEquals(s, s.exchange(listOf(0)))
        assertEquals(s, s.submit(dict("ev")))
    }

    // --- Gömülü sözlük ---

    @Test
    fun `bundled list is broad and well formed`() {
        val valid = DizgiWords.valid
        assertTrue(valid.size > 20_000)
        assertTrue(valid.all { it.length in 2..15 })
        assertTrue(valid.all { w -> w.all { DizgiLetters.isLetter(it) } })
        // Şapkalı kaynak girdileri düzleştirilmiş olmalı (belâ -> bela, kâğıt -> kağıt).
        for (w in listOf("ev", "at", "su", "kalem", "kitap", "deniz", "bela", "kağıt", "hikaye")) {
            assertTrue(w, w in valid)
        }
    }
}
