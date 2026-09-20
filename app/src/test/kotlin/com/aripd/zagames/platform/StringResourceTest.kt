package com.aripd.zagames.platform

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aripd.zagames.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * Derlenmiş metinlerin değişmezleri.
 *
 * aapt2 iki şeyi sessizce yapar ve ikisi de derlemeyi kırmaz, yalnızca
 * kullanıcı görür: kaçışsız çift tırnağı tırnak aç/kapa sayıp atar, %% ikilisini
 * olduğu gibi bırakır (yüzdeyi tekleştiren şey String.format'tır, argümansız
 * getString onu çalıştırmaz). Kaynak tarafını tools/check_strings.py denetler;
 * burada okunan, aapt2'nin gerçekten ne ürettiğidir.
 */
@RunWith(AndroidJUnit4::class)
class StringResourceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun theLicenseNoteKeepsItsQuotes() {
        assertTrue(
            "marka adı tırnak içinde kalmalı",
            context.getString(R.string.about_app_license).contains("\"ZA Games\""),
        )
    }

    /**
     * Arapça'da sayı taşıyan metinler Latin rakamla mı yazılıyor?
     *
     * `getString(id, sayı)` cihazın yerel ayarıyla biçimler ve Arapça'da
     * `%d`'yi Hint-Arap rakamına çevirir: Mayın'ın zorluk kartı `٩×١٢`
     * görünüyordu (v0.43.0 cihaz koşumu, F2). [zaText] Locale.ROOT ile
     * biçimlediği için rakamlar Latin kalır. Test ham okumanın hâlâ
     * Hint-Arap bastığını da doğrular: kural gerçekten gerekli.
     */
    @Test
    fun numbersStayLatinInArabic() {
        val config = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("ar"))
        }
        val arabic = context.createConfigurationContext(config).resources
        val id = R.string.mines_difficulty_desc_fmt

        val safe = zaText(arabic, id, 9, 12, 14)
        assertTrue("metin Arapça kaynaktan gelmeli", safe.contains("لغماً"))
        assertTrue("rakamlar Latin olmalı: $safe", safe.contains("9") && safe.contains("12"))
        assertFalse("Hint-Arap rakamı kalmamalı: $safe", safe.any { it in '\u0660'..'\u0669' })

        val raw = arabic.getString(id, 9, 12, 14)
        assertTrue("ham okuma Hint-Arap basıyor, kural bu yüzden var: $raw",
            raw.any { it in '\u0660'..'\u0669' })
    }

    @Test
    fun theKiskacHintShowsSinglePercentSigns() {
        val hint = context.getString(R.string.kiskac_hint)
        assertTrue("ipucu yüzde işareti içerir", hint.contains("%"))
        assertFalse("argümansız okunan metinde %% ekrana gelir", hint.contains("%%"))
    }
}
