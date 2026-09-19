package com.za.games.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Sürüm notlarının değişmezleri. Notlar sürüm başına elle yazıldığı için asıl
 * risk sıralamanın ya da dil seçiminin sessizce bozulması: bir sürüm yanlış
 * yere girerse ana menüdeki "Yenilikler" kartı eski sürümü gösterir.
 */
class ChangelogTest {

    @Test
    fun entriesAreOrderedNewestFirst() {
        val codes = Changelog.entries.map { it.code }
        assertEquals("sürümler en yeniden eskiye sıralı olmalı", codes.sortedDescending(), codes)
        assertEquals("aynı sürüm iki kez girilmiş", codes.size, codes.distinct().size)
    }

    @Test
    fun versionCodeMatchesTheGradleRule() {
        // build.gradle.kts: major*10000 + minor*100 + patch.
        assertEquals(4_000, Changelog.versionCode("0.40.0"))
        assertEquals(3_902, Changelog.versionCode("0.39.2"))
        assertEquals(10_000, Changelog.versionCode("1.0.0"))
    }

    @Test
    fun turkishAndEnglishNotesExistForEveryVersion() {
        for (note in Changelog.entries) {
            val tr = note.notes(Locale.forLanguageTag("tr"))
            val en = note.notes(Locale.forLanguageTag("en"))
            assertTrue("${note.version}: Türkçe not yok", tr.isNotEmpty())
            assertTrue("${note.version}: İngilizce not yok", en.isNotEmpty())
            assertNotEquals("${note.version}: iki dil aynı metni veriyor", tr, en)
        }
    }

    @Test
    fun untranslatedLanguagesFallBackToEnglishNotTurkish() {
        // Japonca hiç desteklenmiyor; Almanca notu olmayan eski sürümler de aynı yola girer.
        val note = Changelog.entries.last()
        val english = note.notes(Locale.forLanguageTag("en"))
        assertEquals(english, note.notes(Locale.forLanguageTag("ja")))
        assertEquals(english, note.notes(Locale.forLanguageTag("de")))
    }

    @Test
    fun theLanguageReleaseIsWrittenInEverySupportedLanguage() {
        // Kullanıcının kendi dilini duyuran sürüm, o dilde görünmeli: ana menüdeki
        // "Yenilikler" kartı yalnız en yeni sürümü gösterir.
        val note = Changelog.entries.first { it.version == "0.40.0" }
        val english = note.notes(Locale.forLanguageTag("en"))
        for (tag in ZaLocale.TAGS) {
            val lines = note.notes(Locale.forLanguageTag(tag))
            assertTrue("$tag: not boş", lines.isNotEmpty())
            if (tag != "en") {
                assertNotEquals("$tag: İngilizce metne düşmüş", english, lines)
            }
        }
    }

    @Test
    fun regionsAndLegacyCodesReachTheSameNotes() {
        val note = Changelog.entries.first { it.version == "0.40.0" }
        assertEquals(note.notes(Locale.forLanguageTag("de")), note.notes(Locale.forLanguageTag("de-AT")))
        assertEquals(note.notes(Locale.forLanguageTag("pt")), note.notes(Locale.forLanguageTag("pt-BR")))
        // Norveççe'nin eski "no" kodu da nb notlarına varmalı.
        val legacyNorwegian = Locale.Builder().setLanguage("no").setRegion("NO").build()
        assertEquals(note.notes(Locale.forLanguageTag("nb")), note.notes(legacyNorwegian))
    }
}
