package com.za.games.platform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.za.games.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

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

    @Test
    fun theKiskacHintShowsSinglePercentSigns() {
        val hint = context.getString(R.string.kiskac_hint)
        assertTrue("ipucu yüzde işareti içerir", hint.contains("%"))
        assertFalse("argümansız okunan metinde %% ekrana gelir", hint.contains("%%"))
    }
}
