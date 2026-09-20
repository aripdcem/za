package com.za.games.sozluk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Dil tablosunun değişmezleri. Tablo elle yazıldığı için asıl risk üç harf
 * dizisinin (alfabe, sözlük sırası, klavye) sessizce ayrışması: bir harf
 * klavyede yoksa oyuncu o kelimeyi hiç yazamaz ve bunu kimse fark etmez.
 */
class WordLangTest {

    @Test
    fun everyLanguageCoversTheSameLettersThreeTimes() {
        for (lang in WordLang.entries) {
            val alphabet = lang.letters.toSet()
            assertEquals("${lang.tag}: alfabede yinelenen harf",
                lang.letters.length, alphabet.size)
            assertEquals("${lang.tag}: sözlük sırası alfabeyle örtüşmüyor",
                alphabet, lang.order.toSet())
            assertEquals("${lang.tag}: sözlük sırasında yinelenen harf",
                lang.letters.length, lang.order.length)
            val keys = lang.keyRows.joinToString("")
            assertEquals("${lang.tag}: klavye alfabeyle örtüşmüyor",
                alphabet, keys.toSet())
            assertEquals("${lang.tag}: klavyede yinelenen tuş",
                lang.letters.length, keys.length)
        }
    }

    @Test
    fun keyboardsHaveThreeUsableRows() {
        for (lang in WordLang.entries) {
            assertEquals("${lang.tag}: klavye üç sıra olmalı", 3, lang.keyRows.size)
            for (row in lang.keyRows) {
                assertTrue("${lang.tag}: boş klavye sırası", row.isNotEmpty())
                assertTrue("${lang.tag}: '$row' sırası 12 tuştan uzun", row.length <= 12)
            }
        }
    }

    @Test
    fun dictionaryOrderIsNotUnicodeOrder() {
        // Türkçe: ı, i'den önce. Unicode tersini söyler.
        assertTrue(WordLang.TR.compare("ıslak", "islak") < 0)
        assertTrue("ıslak" > "islak")
        // Almanca: ä, a'dan hemen sonra, b'den önce.
        assertTrue(WordLang.DE.compare("ähnlich", "backen") < 0)
        // İsveççe: ä alfabenin sonunda, z'den sonra.
        assertTrue(WordLang.SV.compare("zebra", "ängel") < 0)
        // İspanyolca: ñ, n ile o arasında.
        assertTrue(WordLang.ES.compare("nube", "ñandu") < 0)
        assertTrue(WordLang.ES.compare("ñandu", "ocho") < 0)
    }

    @Test
    fun binarySearchFindsWordsAndInsertionPoints() {
        val words = listOf("bak", "bal", "bel", "bil", "bul").sortedWith(WordLang.TR::compare)
        for ((index, word) in words.withIndex()) {
            assertEquals(index, WordLang.TR.indexOf(words, word))
        }
        assertEquals("aradaki kelime ekleme noktasını vermeli",
            2, WordLang.TR.indexOf(words, "bec"))
        assertEquals("listenin sonrası boyut döner",
            words.size, WordLang.TR.indexOf(words, "zzz"))
    }

    @Test
    fun unsupportedLanguagesFallBackToEnglish() {
        assertNull(WordLang.of("ja"))
        assertEquals(WordLang.EN, WordLang.forTagOrDefault("ja"))
        assertEquals(WordLang.DE, WordLang.forTagOrDefault("de"))
        assertEquals(WordLang.EN, WordLang.DEFAULT)
    }
}
