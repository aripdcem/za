package com.za.games.platform

import com.za.games.sozluk.WordLang
import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.za.games.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

/**
 * Dil altyapısının değişmezleri: desteklenen dil listesi tek kaynaktan gelir,
 * eşleşmeyen dil İngilizce'ye düşer, kelime oyunları Türkçe ya da İngilizce
 * kalır, sayılar her dilde Latin rakamla yazılır.
 */
@RunWith(AndroidJUnit4::class)
class ZaLocaleTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Verilen dile çözülmüş kaynak dizesi. */
    private fun stringIn(tag: String, id: Int): String {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag(tag))
        return context.createConfigurationContext(config).getString(id)
    }

    /** locales_config.xml içindeki dil etiketleri, dosyadaki sırayla. */
    private fun configuredTags(): List<String> {
        val parser = context.resources.getXml(R.xml.locales_config)
        val tags = mutableListOf<String>()
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                tags += parser.getAttributeValue(ANDROID_NS, "name")
            }
        }
        return tags
    }

    @Test
    fun supportedTagsMatchTheLocaleConfig() {
        // Sistem dil seçicisi locales_config.xml'i, uygulama içi seçici
        // ZaLocale.TAGS'i okur; ikisi ayrışırsa bir dil yalnız birinde görünür.
        assertEquals(configuredTags(), ZaLocale.TAGS)
    }

    @Test
    fun everySupportedLanguageHasAnEndonym() {
        for (tag in ZaLocale.TAGS) {
            val name = ZaLocale.endonym(tag)
            assertTrue("$tag için dil adı yok", name.isNotBlank() && name != tag)
        }
    }

    @Test
    fun unsupportedLanguagesFallBackToEnglishNotTurkish() {
        // Düzeltilen hata: Türkçe res/values altındaydı, desteklenmeyen her dil
        // uygulamayı Türkçe görüyordu. Japonca hiçbir zaman listeye girmeyeceği
        // için yedeğin İngilizce olduğunu güvenle gösterir.
        val english = stringIn("en", R.string.hub_tagline)
        assertEquals("varsayılan res/values İngilizce olmalı", "Zero ads. Pure play.", english)
        assertEquals("desteklenmeyen dil İngilizce\'ye düşmeli", english, stringIn("ja", R.string.hub_tagline))
        assertEquals("Sıfır reklam. Saf oyun.", stringIn("tr", R.string.hub_tagline))
    }

    @Test
    fun everySupportedLanguageHasItsOwnHubTagline() {
        // Her dil kendi metnini almalı; biri varsayılana düşüyorsa o dilin
        // klasörü ya eksik ya yanlış adlandırılmış.
        val english = stringIn("en", R.string.hub_tagline)
        for (tag in ZaLocale.TAGS - setOf("en")) {
            assertNotEquals("$tag kendi metnini almalı", english, stringIn(tag, R.string.hub_tagline))
        }
    }

    @Test
    fun wordGamesSpeakEveryLanguageWithAWordList() {
        // Kelime oyunları kendi listeleriyle her dilde oynanıyor, bu yüzden
        // metinleri de o dilde olmalı. Liste varken metin yoksa oyun açılır ama
        // arayüzü İngilizce görünür; testin yakaladığı şey bu.
        val english = stringIn("en", R.string.besharf_hint)
        for (lang in WordLang.entries) {
            val text = stringIn(lang.tag, R.string.besharf_hint)
            assertTrue("${lang.tag}: kelime oyunu metni boş", text.isNotBlank())
            if (lang != WordLang.EN) {
                assertNotEquals(
                    "${lang.tag}: kelime oyunu metni İngilizce'ye düşmüş " +
                        "(values-${lang.tag}/strings_words.xml eksik mi?)",
                    english,
                    text,
                )
            }
        }
    }

    @Test
    fun everyWordGameLanguageIsAlsoAnInterfaceLanguage() {
        // Tersi de tutmalı: kelime listesi olan dilin arayüz çevirisi de olmalı,
        // yoksa oyun o dilde oynanır ama uygulamanın kalanı İngilizce görünür.
        for (lang in WordLang.entries) {
            assertTrue(
                "${lang.tag}: kelime listesi var ama arayüz dili listesinde yok",
                lang.tag in ZaLocale.TAGS,
            )
        }
    }

    @Test
    fun sharedDailyModeStringsStayTranslatable() {
        // mode_daily kelime oyunlarının bloğunda duruyordu ama on üç oyun
        // kullanıyor: strings_words.xml'e taşınmadığını doğrula.
        assertEquals("Daily", stringIn("en", R.string.mode_daily))
        assertEquals("Günlük", stringIn("tr", R.string.mode_daily))
    }

    @Test
    fun normalizeStripsRegionsForEverySupportedLanguage() {
        // Desteklenen her dil için bölge eki düşer: de-DE → de, pt-BR → pt.
        // Liste büyüdükçe bu test kendiliğinden yeni dilleri de kapsar.
        for (tag in ZaLocale.TAGS) {
            assertEquals("$tag-XX etiketi $tag\'e inmeli", tag, ZaLocale.normalize(Locale.forLanguageTag("$tag-XX")))
        }
        assertEquals("tr", ZaLocale.normalize(Locale.forLanguageTag("tr-CY")))
    }

    @Test
    fun normalizeMapsLegacyNorwegianAndRejectsUnsupported() {
        // Locale("no") tarihsel olarak Norveççe'yi gösterir; kaynaklarımız nb altında.
        val legacyNorwegian = Locale.Builder().setLanguage("no").setRegion("NO").build()
        val expected = if ("nb" in ZaLocale.TAGS) "nb" else null
        assertEquals(expected, ZaLocale.normalize(legacyNorwegian))
        // Japonca desteklenmiyor ve desteklenmeyecek: null dönmeli.
        assertNull("desteklenmeyen dil null dönmeli", ZaLocale.normalize(Locale.forLanguageTag("ja")))
    }

    @Test
    fun numbersKeepLatinDigitsWithLocalSeparators() {
        assertEquals("1,234,567", ZaLocale.number(1_234_567L, Locale.forLanguageTag("en")))
        assertEquals("1.234.567", ZaLocale.number(1_234_567L, Locale.forLanguageTag("tr")))
        // Arapça varsayılanı ١٢٣٤٥٦٧ olurdu; skorlar tuvale sabit ölçüyle çizilir.
        val arabic = ZaLocale.number(1_234_567L, Locale.forLanguageTag("ar"))
        assertEquals("Arapça'da yedi Latin rakam olmalı: $arabic", 7, arabic.count { it in '0'..'9' })
        assertTrue("Arap-Hint rakamı kalmamalı: $arabic", arabic.none { it in '٠'..'٩' })
    }

    @Test
    fun decimalsKeepLatinDigitsWithLocalSeparators() {
        assertEquals("1.5", ZaLocale.decimal(1.5f, locale = Locale.forLanguageTag("en")))
        assertEquals("1,5", ZaLocale.decimal(1.5f, locale = Locale.forLanguageTag("tr")))
        assertEquals("1.50", ZaLocale.decimal(1.5f, digits = 2, locale = Locale.forLanguageTag("en")))
        val arabic = ZaLocale.decimal(1.5f, locale = Locale.forLanguageTag("ar"))
        assertEquals("Arapça'da iki Latin rakam olmalı: $arabic", 2, arabic.count { it in '0'..'9' })
    }

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
