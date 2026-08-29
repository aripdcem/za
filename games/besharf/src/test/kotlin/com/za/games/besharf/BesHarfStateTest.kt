package com.za.games.besharf

import com.za.games.besharf.LetterMark.ABSENT
import com.za.games.besharf.LetterMark.CORRECT
import com.za.games.besharf.LetterMark.PRESENT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BesHarfStateTest {

    private val acceptAll: (String) -> Boolean = { true }

    private fun typed(state: BesHarfState, word: String): BesHarfState =
        word.fold(state) { s, c -> s.type(c) }

    // --- İşaretleme ---

    @Test
    fun `exact guess marks everything correct and wins`() {
        assertEquals(List(5) { CORRECT }, BesHarfState.mark("kalem", "kalem"))
        val state = typed(BesHarfState(answer = "kalem"), "kalem").submit(acceptAll)
        assertEquals(BesHarfStatus.WON, state.status)
        assertEquals("", state.current)
    }

    @Test
    fun `present letters are marked in wrong positions`() {
        assertEquals(
            listOf(PRESENT, PRESENT, PRESENT, PRESENT, ABSENT),
            BesHarfState.mark("kalem", "elmas"),
        )
    }

    @Test
    fun `duplicate guess letters consume the answer stock only once`() {
        // k-a-l-e-m'e karşı k-e-k-e-k: yalnızca tam isabetler kalır.
        assertEquals(
            listOf(CORRECT, ABSENT, ABSENT, CORRECT, ABSENT),
            BesHarfState.mark("kalem", "kekek"),
        )
        // Tek a'lı cevaba beş a: yalnızca doğru konumdaki boyanır.
        assertEquals(
            listOf(ABSENT, CORRECT, ABSENT, ABSENT, ABSENT),
            BesHarfState.mark("kalem", "aaaaa"),
        )
    }

    @Test
    fun `exact matches take priority over earlier present letters`() {
        // s-a-z-a-n'a karşı a-z-r-a-k: 4. konumdaki a tam isabet,
        // 1. konumdaki a kalan stoktan "var" alır.
        assertEquals(
            listOf(PRESENT, PRESENT, ABSENT, CORRECT, ABSENT),
            BesHarfState.mark("sazan", "azrak"),
        )
    }

    // --- Yazma ve gönderme ---

    @Test
    fun `typing respects length limit and the turkish alphabet`() {
        var state = typed(BesHarfState(answer = "kalem"), "çğıöş")
        assertEquals("çğıöş", state.current)
        assertEquals(state, state.type('a')) // 5 harf dolu
        state = state.erase().type('q').type('w').type('x').type('1')
        assertEquals("çğıö", state.current) // alfabe dışı karakterler yok sayılır
        assertEquals("", BesHarfState(answer = "kalem").erase().current)
    }

    @Test
    fun `submitting a short or unlisted word counts as invalid`() {
        val state = typed(BesHarfState(answer = "kalem"), "kal")
        val short = state.submit(acceptAll)
        assertEquals(1, short.invalidEvents)
        assertTrue(short.guesses.isEmpty())
        assertEquals("kal", short.current) // yazılan korunur

        val unlisted = typed(BesHarfState(answer = "kalem"), "zzzzz").submit { false }
        assertEquals(1, unlisted.invalidEvents)
        assertTrue(unlisted.guesses.isEmpty())
    }

    @Test
    fun `six wrong guesses lose the game and further input is ignored`() {
        var state = BesHarfState(answer = "kalem")
        repeat(6) { state = typed(state, "sazan").submit(acceptAll) }
        assertEquals(BesHarfStatus.LOST, state.status)
        assertEquals(6, state.guesses.size)
        assertEquals(state, state.type('a'))
        assertEquals(state, typed(state, "kalem").submit(acceptAll))
    }

    @Test
    fun `key marks keep the best information per letter`() {
        var state = typed(BesHarfState(answer = "kalem"), "elmas").submit(acceptAll)
        assertEquals(PRESENT, state.keyMarks()['e'])
        state = typed(state, "kelam").submit(acceptAll)
        assertEquals(CORRECT, state.keyMarks()['k'])
        assertEquals(CORRECT, state.keyMarks()['l'])
        assertNotEquals(ABSENT, state.keyMarks()['e']) // en iyi bilgi korunur
    }

    // --- Günlük ve serbest ---

    @Test
    fun `daily puzzles are deterministic per day and differ between days`() {
        val answers = BesHarfWords.answers
        assertEquals(
            BesHarfState.daily(answers, 20_000L).answer,
            BesHarfState.daily(answers, 20_000L).answer,
        )
        assertNotEquals(
            BesHarfState.daily(answers, 20_000L).answer,
            BesHarfState.daily(answers, 20_001L).answer,
        )
        assertEquals(20_000L, BesHarfState.daily(answers, 20_000L).dailyDay)
    }

    @Test
    fun `free games are deterministic per seed`() {
        val answers = BesHarfWords.answers
        assertEquals(
            BesHarfState.free(answers, 7L).answer,
            BesHarfState.free(answers, 7L).answer,
        )
        assertTrue(BesHarfState.free(answers, 7L).dailyDay == null)
    }

    // --- Kelime listeleri ---

    @Test
    fun `word lists are loaded and well formed`() {
        val alphabet = BesHarfState.ALPHABET
        assertTrue(BesHarfWords.answers.size > 1000)
        assertTrue(BesHarfWords.allowed.size > BesHarfWords.answers.size)
        assertTrue(BesHarfWords.answers.all { it.length == 5 && it.all { c -> c in alphabet } })
        assertTrue(BesHarfWords.answers.all { BesHarfWords.isAllowed(it) })
        assertFalse(BesHarfWords.isAllowed("qqqqq"))
        assertEquals(BesHarfWords.answers.size, BesHarfWords.answers.toSet().size)
    }
}
