package com.za.games.turetme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TuretmeStateTest {

    private val tinyValid = listOf("kal", "kel", "elma", "kale", "mele", "kalem", "masal", "lam")

    private fun game(base: String = "kalem", valid: Iterable<String> = tinyValid): TuretmeState =
        TuretmeState.newGame(base, valid, shuffleSeed = 1L)

    /** Görüntü sırası karışık olduğundan kelimeyi harf arayarak seçer. */
    private fun select(state: TuretmeState, word: String): TuretmeState {
        var s = state.clearCurrent()
        for (c in word) {
            val index = s.letters.indices.first { it !in s.usedIndices && s.letters[it] == c }
            s = s.pick(index)
        }
        return s
    }

    private fun play(state: TuretmeState, word: String): TuretmeState =
        select(state, word).submit()

    // --- Hedef kümesi ---

    @Test
    fun `targets contain only words writable from the letter stock`() {
        val targets = TuretmeState.targetsFor("kalem", tinyValid)
        assertEquals(setOf("kal", "kel", "elma", "kale", "kalem", "lam"), targets)
        // "mele" iki e ister, "masal" iki a ve s ister: dışarıda kalırlar.
    }

    @Test
    fun `duplicate letters in the base extend the stock`() {
        val targets = TuretmeState.targetsFor("meleke", listOf("mele", "elek", "kel", "kkk"))
        assertEquals(setOf("mele", "elek", "kel"), targets)
    }

    // --- Seçim ---

    @Test
    fun `picking uses indices so duplicate letters work`() {
        val state = game(base = "elele", valid = listOf("ele"))
        val once = state.pick(0)
        assertEquals(once, once.pick(0)) // aynı indeks ikinci kez seçilemez
        val twice = once.pick(1).pick(2)
        assertEquals(3, twice.usedIndices.size)
        assertEquals(3, twice.current.length)
        assertEquals(2, twice.erase().usedIndices.size)
        assertTrue(twice.clearCurrent().usedIndices.isEmpty())
    }

    // --- Gönderim ve puan ---

    @Test
    fun `valid words score by length and are collected`() {
        var state = game()
        state = play(state, "kal")
        assertEquals(30L, state.score)
        assertTrue("kal" in state.found)
        state = play(state, "elma")
        assertEquals(30L + 40L, state.score)
        assertTrue(state.usedIndices.isEmpty()) // gönderim seçimi temizler
    }

    @Test
    fun `invalid submissions consume nothing and carry a reason`() {
        var state = play(game(), "ka") // 2 harf
        assertEquals(TuretmeInvalid.TOO_SHORT, state.lastInvalid)
        assertEquals(1, state.invalidEvents)
        assertTrue(state.found.isEmpty())

        state = play(game(), "mek") // listede yok
        assertEquals(TuretmeInvalid.NOT_WORD, state.lastInvalid)

        state = play(play(game(), "kal"), "kal")
        assertEquals(TuretmeInvalid.ALREADY_FOUND, state.lastInvalid)
        assertEquals(1, state.found.size)
    }

    @Test
    fun `finding everything completes the round with bonuses`() {
        var state = game(base = "kalem", valid = listOf("kal", "kalem"))
        assertEquals(setOf("kal", "kalem"), state.targets)
        state = play(state, "kal")
        assertEquals(TuretmeStatus.RUNNING, state.status)
        state = play(state, "kalem")
        assertEquals(TuretmeStatus.COMPLETED, state.status)
        // 30 (kal) + 50 (kalem) + 50 taban bonusu + 100 tamamlama.
        assertEquals(230L, state.score)
        assertEquals(state, state.pick(0)) // tur bitince seçim yapılamaz
        assertEquals(state, state.submit())
    }

    @Test
    fun `restore replays found words to the same score regardless of order`() {
        val played = play(play(play(game(), "kal"), "elma"), "kale")
        val restored = game().restoreFound("kale").restoreFound("elma").restoreFound("kal")
        assertEquals(played.found, restored.found)
        assertEquals(played.score, restored.score)
        // Bilinmeyen veya tekrar kelime geri yüklemede sessizce atlanır.
        assertEquals(restored, restored.restoreFound("kale").restoreFound("zzz"))
    }

    // --- Pes etme ---

    @Test
    fun `giving up freezes play, keeps the score and reveals the missing words`() {
        var state = play(game(), "kal") // 30 puan
        state = select(state, "kale").giveUp()
        assertEquals(TuretmeStatus.GIVEN_UP, state.status)
        assertEquals(30L, state.score)
        assertTrue(state.usedIndices.isEmpty()) // yarım seçim temizlenir
        assertEquals(setOf("kel", "elma", "kale", "kalem", "lam"), state.missing)
        // Pes edildikten sonra hiçbir hamle işlemez.
        assertEquals(state, state.pick(0))
        assertEquals(state, state.submit())
        assertEquals(state, state.shuffle(3L))
        assertEquals(state, state.restoreFound("kale"))
        assertEquals(state, state.giveUp())
    }

    @Test
    fun `giving up after completion changes nothing`() {
        val done = play(play(game(base = "kalem", valid = listOf("kal", "kalem")), "kal"), "kalem")
        assertEquals(TuretmeStatus.COMPLETED, done.status)
        assertEquals(done, done.giveUp())
        assertTrue(done.missing.isEmpty())
    }

    // --- Karıştırma ---

    @Test
    fun `shuffle keeps the same letters deterministically and clears selection`() {
        val state = game().pick(0).pick(1)
        val shuffled = state.shuffle(9L)
        assertTrue(shuffled.usedIndices.isEmpty())
        assertEquals(state.letters.sorted(), shuffled.letters.sorted())
        assertEquals(shuffled.letters, state.shuffle(9L).letters)
    }

    // --- Günlük ve serbest ---

    @Test
    fun `daily rounds are deterministic per day and differ between days`() {
        val bases = listOf("kalem", "masal", "meleke", "elele", "araba", "bebek")
        val valid = tinyValid
        assertEquals(
            TuretmeState.daily(bases, valid, 20_000L),
            TuretmeState.daily(bases, valid, 20_000L),
        )
        assertNotEquals(
            TuretmeState.daily(bases, valid, 20_000L).base,
            TuretmeState.daily(bases, valid, 20_001L).base,
        )
        assertEquals(20_000L, TuretmeState.daily(bases, valid, 20_000L).dailyDay)
        assertEquals(
            TuretmeState.free(bases, valid, 7L).base,
            TuretmeState.free(bases, valid, 7L).base,
        )
    }

    // --- Gömülü listeler ---

    @Test
    fun `bundled lists are well formed and bases keep their guarantees`() {
        assertTrue(TuretmeWords.valid.size > 10_000)
        assertTrue(TuretmeWords.bases.size >= 1000)
        assertTrue(TuretmeWords.bases.all { it.length in 6..7 })
        assertTrue(TuretmeWords.bases.all { it in TuretmeWords.valid })
        // Şapkalı kaynak yazımları düzleştirilmiş olmalı (belâ -> bela).
        assertTrue(listOf("bela", "kağıt", "hikaye").all { it in TuretmeWords.valid })

        for (base in listOf(TuretmeWords.bases.first(), TuretmeWords.bases.last())) {
            val count = TuretmeState.targetsFor(base, TuretmeWords.valid).size
            // Üretici tabanları şapkasız listeye göre 15-60 alt kelimeyle seçer;
            // düzleştirilmiş sözlükle sayı biraz artabilir (en fazla ~66).
            assertTrue("$base: $count", count in 15..70)
        }
    }
}
