package com.aripd.zagames.kiskac

import com.aripd.zagames.sozluk.WordLang
import com.aripd.zagames.besharf.BesHarfWords
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KiskacStateTest {

    private val words = BesHarfWords.of(WordLang.TR)

    private val acceptAll: (String) -> Boolean = { true }

    private fun typed(state: KiskacState, word: String): KiskacState =
        word.fold(state) { s, c -> s.type(c) }

    private fun guessed(state: KiskacState, word: String): KiskacState =
        typed(state, word).submit(acceptAll)

    // --- Türk alfabesi sıralaması ---

    @Test
    fun `turkish collation orders the 29 letters correctly`() {
        // Unicode sırasının yanlış yapacağı çiftler:
        assertTrue(WordLang.TR.compare("aaaca", "aaaça") < 0)
        assertTrue(WordLang.TR.compare("aaaga", "aaağa") < 0)
        assertTrue(WordLang.TR.compare("aaaha", "aaaıa") < 0)
        assertTrue(WordLang.TR.compare("aaaıa", "aaaia") < 0)
        assertTrue(WordLang.TR.compare("aaaoa", "aaaöa") < 0)
        assertTrue(WordLang.TR.compare("aaasa", "aaaşa") < 0)
        assertTrue(WordLang.TR.compare("aaaua", "aaaüa") < 0)
        assertTrue(WordLang.TR.compare("aaaza", "aaaaa") > 0)
        assertEquals(0, WordLang.TR.compare("kalem", "kalem"))
        assertEquals(28, WordLang.TR.rankOf('z'))
        assertEquals(29, WordLang.TR.letters.length)
    }

    // --- Geri bildirim ve sınırlar ---

    @Test
    fun `guesses report whether the hidden word comes after and bounds narrow`() {
        var state = KiskacState(answer = "kalem", lang = WordLang.TR)
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
        val state = guessed(KiskacState(answer = "kalem", lang = WordLang.TR), "kalem")
        assertEquals(KiskacStatus.WON, state.status)
        assertEquals("", state.current)
        assertEquals(state, state.type('a')) // bitince giriş işlemez
    }

    @Test
    fun `using every guess loses the game`() {
        var state = KiskacState(answer = "kalem", lang = WordLang.TR)
        // Hepsi farklı, hepsi yanlış: hak sayısı kadar tahmin (sabitten türetilir).
        val harfler = "abcdfghjlmnprs"
        val words = (0 until KiskacState.MAX_GUESSES).map { harfler[it].toString().repeat(5) }
        for (w in words) state = guessed(state, w)
        assertEquals(KiskacStatus.LOST, state.status)
        assertEquals(KiskacState.MAX_GUESSES, state.guesses.size)
        assertEquals(state, typed(state, "kalem").submit(acceptAll))
    }

    // --- Geçersiz gönderimler ---

    @Test
    fun `short unlisted and repeated words are invalid and consume no guess`() {
        var state = typed(KiskacState(answer = "kalem", lang = WordLang.TR), "ka").submit(acceptAll)
        assertEquals(1, state.invalidEvents)
        assertEquals(KiskacInvalid.NOT_IN_LIST, state.lastInvalid)
        assertTrue(state.guesses.isEmpty())

        state = typed(KiskacState(answer = "kalem", lang = WordLang.TR), "zzzzz").submit { false }
        assertEquals(KiskacInvalid.NOT_IN_LIST, state.lastInvalid)

        state = guessed(KiskacState(answer = "kalem", lang = WordLang.TR), "elmas")
        state = typed(state, "elmas").submit(acceptAll)
        assertEquals(KiskacInvalid.ALREADY_TRIED, state.lastInvalid)
        assertEquals(1, state.guesses.size)
    }

    @Test
    fun `typing respects the turkish alphabet and length limit`() {
        var state = typed(KiskacState(answer = "kalem", lang = WordLang.TR), "çğıöş")
        assertEquals("çğıöş", state.current)
        assertEquals(state, state.type('x'))
        assertEquals(state, state.type('a')) // dolu
        assertEquals("çğıö", state.erase().current)
    }

    // --- Klavye soluklaştırma ---

    @Test
    fun `possible first letters shrink with the bounds`() {
        var state = KiskacState(answer = "kalem", lang = WordLang.TR)
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
            KiskacState.daily(WordLang.TR, answers, 20_000L).answer,
            KiskacState.daily(WordLang.TR, answers, 20_000L).answer,
        )
        assertEquals(20_000L, KiskacState.daily(WordLang.TR, answers, 20_000L).dailyDay)
        assertNotEquals(
            KiskacState.daily(WordLang.TR, answers, 20_000L).answer,
            KiskacState.daily(WordLang.TR, answers, 20_001L).answer,
        )
        assertNull(KiskacState.free(WordLang.TR, answers, 7L).dailyDay)
        assertEquals(
            KiskacState.free(WordLang.TR, answers, 7L).answer,
            KiskacState.free(WordLang.TR, answers, 7L).answer,
        )
    }

    // --- Uzaklık ipucu ---

    private val sorted = listOf("abaca", "bakır", "çamur", "dalga", "elmas", "fasıl")

    @Test
    fun `distance uses list ends when there are no bounds`() {
        val d = KiskacState(answer = "çamur", lang = WordLang.TR).distance(sorted)
        assertEquals(2, d.answerIndex)
        assertEquals(-1, d.lowerIndex)
        assertEquals(6, d.upperIndex)
        assertEquals(3, d.fromLower)
        assertEquals(4, d.toUpper)
        assertEquals(3f / 7f, d.fraction, 1e-6f)
    }

    @Test
    fun `distance narrows with bounds`() {
        var state = KiskacState(answer = "çamur", lang = WordLang.TR)
        state = guessed(state, "abaca")
        var d = state.distance(sorted)
        assertEquals(0, d.lowerIndex)
        assertEquals(2, d.fromLower)
        state = guessed(state, "elmas")
        d = state.distance(sorted)
        assertEquals(4, d.upperIndex)
        assertEquals(2, d.toUpper)
        assertEquals(0.5f, d.fraction, 1e-6f)
        state = guessed(state, "dalga")
        d = state.distance(sorted)
        assertEquals(3, d.upperIndex)
        assertEquals(1, d.toUpper)
        assertEquals(2f / 3f, d.fraction, 1e-6f)
        state = guessed(state, "bakır")
        d = state.distance(sorted)
        assertEquals(1, d.fromLower)
        assertEquals(1, d.toUpper)
        assertEquals(0.5f, d.fraction, 1e-6f)
    }

    @Test
    fun `index search finds words and insertion points in turkish order`() {
        assertEquals(2, WordLang.TR.indexOf(sorted, "çamur"))
        assertEquals(0, WordLang.TR.indexOf(sorted, "aaaaa"))
        assertEquals(6, WordLang.TR.indexOf(sorted, "zzzzz"))
        assertEquals(2, WordLang.TR.indexOf(sorted, "cccca")) // c < ç
        assertTrue(WordLang.TR.compare("abcde", "abxde") < 0) // tablo dışı harf çökmez
    }

    /**
     * Hak, oyuncunun arayabildiği uzayın ikili arama derinliğini karşılamalı.
     * Kıskaç ikili aramadır ve ekranda sıralı olarak geçerli tahminlerin
     * tamamı gösterilir; hak ⌈log2(n+1)⌉'in altına düşerse cevapların bir
     * kısmı kusursuz oynayan biri için bile ulaşılmaz olur. Kelime listesi
     * büyüdüğünde bu test uyarır.
     */
    @Test
    fun `guess budget covers binary search over the guessable list`() {
        val uzay = words.allowed.size
        val gereken = kotlin.math.ceil(kotlin.math.ln(uzay + 1.0) / kotlin.math.ln(2.0)).toInt()
        assertTrue(
            "arama uzayı $uzay kelime → $gereken tahmin gerekiyor, hak ${KiskacState.MAX_GUESSES}",
            KiskacState.MAX_GUESSES >= gereken,
        )
    }
}
