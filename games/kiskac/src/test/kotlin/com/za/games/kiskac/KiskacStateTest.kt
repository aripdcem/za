package com.za.games.kiskac

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KiskacStateTest {

    private val acceptAll: (String) -> Boolean = { true }

    private fun typed(state: KiskacState, word: String): KiskacState =
        word.fold(state) { s, c -> s.type(c) }

    private fun guessed(state: KiskacState, word: String): KiskacState =
        typed(state, word).submit(acceptAll)

    // --- Türk alfabesi sıralaması ---

    @Test
    fun `turkish collation orders the 29 letters correctly`() {
        // Unicode sırasının yanlış yapacağı çiftler:
        assertTrue(TurkishOrder.compare("aaaca", "aaaça") < 0)
        assertTrue(TurkishOrder.compare("aaaga", "aaağa") < 0)
        assertTrue(TurkishOrder.compare("aaaha", "aaaıa") < 0)
        assertTrue(TurkishOrder.compare("aaaıa", "aaaia") < 0)
        assertTrue(TurkishOrder.compare("aaaoa", "aaaöa") < 0)
        assertTrue(TurkishOrder.compare("aaasa", "aaaşa") < 0)
        assertTrue(TurkishOrder.compare("aaaua", "aaaüa") < 0)
        assertTrue(TurkishOrder.compare("aaaza", "aaaaa") > 0)
        assertEquals(0, TurkishOrder.compare("kalem", "kalem"))
        assertEquals(28, TurkishOrder.rankOf('z'))
        assertEquals(29, TurkishOrder.LETTERS.length)
    }

    // --- Geri bildirim ve sınırlar ---

    @Test
    fun `guesses report whether the hidden word comes after and bounds narrow`() {
        var state = KiskacState(answer = "kalem")
        state = guessed(state, "cacık")
        assertTrue(state.guesses.last().hiddenIsAfter) // kalem, cacık'tan sonra
        state = guessed(state, "yazma")
        assertTrue(!state.guesses.last().hiddenIsAfter) // kalem, yazma'dan önce
        state = guessed(state, "elmas")
        state = guessed(state, "masal")

        assertEquals("elmas", state.lowerBound) // alt sınırların en büyüğü
        assertEquals("masal", state.upperBound) // üst sınırların en küçüğü
        assertEquals(KiskacStatus.RUNNING, state.status)
    }

    @Test
    fun `guessing the answer wins`() {
        val state = guessed(KiskacState(answer = "kalem"), "kalem")
        assertEquals(KiskacStatus.WON, state.status)
        assertEquals("", state.current)
        assertEquals(state, state.type('a')) // bitince giriş işlemez
    }

    @Test
    fun `twelve wrong guesses lose the game`() {
        var state = KiskacState(answer = "kalem")
        // Hepsi farklı, hepsi yanlış 12 tahmin:
        val words = listOf(
            "aaaaa", "bbbbb", "ccccc", "ddddd", "eeeee", "fffff",
            "ggggg", "hhhhh", "jjjjj", "lllll", "mmmmm", "nnnnn",
        )
        for (w in words) state = guessed(state, w)
        assertEquals(KiskacStatus.LOST, state.status)
        assertEquals(KiskacState.MAX_GUESSES, state.guesses.size)
        assertEquals(state, typed(state, "kalem").submit(acceptAll))
    }

    // --- Geçersiz gönderimler ---

    @Test
    fun `short unlisted and repeated words are invalid and consume no guess`() {
        var state = typed(KiskacState(answer = "kalem"), "ka").submit(acceptAll)
        assertEquals(1, state.invalidEvents)
        assertEquals(KiskacInvalid.NOT_IN_LIST, state.lastInvalid)
        assertTrue(state.guesses.isEmpty())

        state = typed(KiskacState(answer = "kalem"), "zzzzz").submit { false }
        assertEquals(KiskacInvalid.NOT_IN_LIST, state.lastInvalid)

        state = guessed(KiskacState(answer = "kalem"), "elmas")
        state = typed(state, "elmas").submit(acceptAll)
        assertEquals(KiskacInvalid.ALREADY_TRIED, state.lastInvalid)
        assertEquals(1, state.guesses.size)
    }

    @Test
    fun `typing respects the turkish alphabet and length limit`() {
        var state = typed(KiskacState(answer = "kalem"), "çğıöş")
        assertEquals("çğıöş", state.current)
        assertEquals(state, state.type('x'))
        assertEquals(state, state.type('a')) // dolu
        assertEquals("çğıö", state.erase().current)
    }

    // --- Klavye soluklaştırma ---

    @Test
    fun `possible first letters shrink with the bounds`() {
        var state = KiskacState(answer = "kalem")
        assertEquals(29, state.possibleFirstLetters().size)

        state = guessed(state, "elmas") // alt sınır e...
        val afterLower = state.possibleFirstLetters()
        assertTrue('a' !in afterLower && 'd' !in afterLower)
        assertTrue('e' in afterLower && 'z' in afterLower)

        state = guessed(state, "masal") // üst sınır m...
        val squeezed = state.possibleFirstLetters()
        assertTrue('e' in squeezed && 'k' in squeezed && 'm' in squeezed)
        assertTrue('n' !in squeezed && 'z' !in squeezed)
    }

    // --- Günlük ve serbest ---

    @Test
    fun `daily puzzles are deterministic per day and differ between days`() {
        val answers = listOf("araba", "bebek", "cadde", "kalem", "masal", "yazma")
        assertEquals(
            KiskacState.daily(answers, 20_000L).answer,
            KiskacState.daily(answers, 20_000L).answer,
        )
        assertEquals(20_000L, KiskacState.daily(answers, 20_000L).dailyDay)
        assertNotEquals(
            KiskacState.daily(answers, 20_000L).answer,
            KiskacState.daily(answers, 20_001L).answer,
        )
        assertNull(KiskacState.free(answers, 7L).dailyDay)
        assertEquals(
            KiskacState.free(answers, 7L).answer,
            KiskacState.free(answers, 7L).answer,
        )
    }
}
