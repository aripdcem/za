package com.aripd.zagames.ui.common

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aripd.zagames.platform.WordLangs
import com.aripd.zagames.sozluk.WordLang
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Kelime dili seçimi ekranın derinine ulaşıyor mu?
 *
 * v0.43.0'ta ulaşmıyordu: seçici kaydı yazıyor, oyunun görünüm modeli yeni
 * dile geçiyor, ama klavyeyi besleyen [LocalWordLang] eski dilde kalıyordu —
 * Almanca arayüzde Türkçe seçilince Ç Ğ İ Ş yazılamıyordu. Ekran testleri bunu
 * göremezdi, çünkü sağlayıcıyı kendileri kuruyor; bu yüzden test doğrudan
 * [rememberWordLang]'e bakar, yani ZaApp'in kullandığı bağlantının kendisine.
 */
@RunWith(AndroidJUnit4::class)
class WordLangProviderTest {

    @get:Rule
    val rule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun pickingAWordLanguageUpdatesTheProvidedValue() {
        WordLangs.choose(context, WordLang.EN)
        var seen: WordLang? = null
        rule.setContent { seen = rememberWordLang() }
        rule.runOnIdle { assertEquals(WordLang.EN, seen) }

        rule.runOnIdle { WordLangs.choose(context, WordLang.TR) }
        rule.waitForIdle()
        assertEquals("seçim değişince klavyeyi besleyen dil de değişmeli", WordLang.TR, seen)
        assertEquals("klavye yeni dilin sırasında olmalı", WordLang.TR.keyRows, seen?.keyRows)
    }

    @Test
    fun clearingTheChoiceFallsBackToTheInterfaceLanguage() {
        WordLangs.choose(context, WordLang.TR)
        var seen: WordLang? = null
        rule.setContent { seen = rememberWordLang() }
        rule.runOnIdle { assertEquals(WordLang.TR, seen) }

        rule.runOnIdle { WordLangs.choose(context, null) }
        rule.waitForIdle()
        assertEquals("seçim kalkınca arayüzün diline dönülür", WordLangs.followed(context), seen)
    }
}
