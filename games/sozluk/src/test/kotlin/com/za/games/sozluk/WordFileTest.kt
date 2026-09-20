package com.za.games.sozluk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Ön-kodlu biçimin çözülmesi: sıra korunur, bozuk dosya sessizce yutulmaz. */
class WordFileTest {

    @Test
    fun decodesSharedPrefixes() {
        val lines = listOf("0kalem", "5lik", "5tıraş", "0masa", "4l")
        assertEquals(
            listOf("kalem", "kalemlik", "kalemtıraş", "masa", "masal"),
            WordFile.decode(lines.asSequence()),
        )
    }

    @Test
    fun emptyLinesAreSkipped() {
        assertEquals(listOf("at", "ata"), WordFile.decode(sequenceOf("0at", "", "2a", "")))
    }

    @Test
    fun prefixLongerThanThePreviousWordIsRejected() {
        // Dosya bozulursa sessizce yanlış kelime üretmek yerine patlamalı.
        val error = assertThrows(IllegalArgumentException::class.java) {
            WordFile.decode(sequenceOf("0at", "9lar"))
        }
        assertEquals(true, error.message!!.contains("Bozuk kelime listesi"))
    }
}
