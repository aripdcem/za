package com.za.games

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.test.core.app.ApplicationProvider
import com.za.games.platform.GameEntry
import com.za.games.platform.GameRegistry
import com.za.games.platform.LocalZaHaptics
import com.za.games.platform.LocalZaSound
import com.za.games.ui.theme.ZaTheme
import java.util.Locale

/** Oyun ekranlarının beklediği tema ve yerel sağlayıcılarla içerik kurar (ses yok). */
fun ComposeContentTestRule.setZaContent(content: @Composable () -> Unit) {
    setContent {
        ZaTheme {
            val haptics = LocalHapticFeedback.current
            CompositionLocalProvider(LocalZaHaptics provides haptics, LocalZaSound provides null) {
                content()
            }
        }
    }
}

/** Uygulama kaynaklarından dize; testler yerel ayardan bağımsız kalır. */
fun str(@StringRes id: Int, vararg args: Any): String =
    ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

/**
 * GameTopBar başlıkları büyük harfle çizilir. Üretimdeki appLocale() gibi
 * yapılandırmanın yerelini kullanır: qualifiers = "tr" ile koşan bir test
 * Locale.getDefault()'a bakarsa "İ" yerine "I" bekler ve boşa düşer.
 */
fun titleOf(@StringRes id: Int): String = str(id).uppercase(testLocale())

/** Testin koştuğu yapılandırmanın yereli. */
fun testLocale(): Locale =
    ApplicationProvider.getApplicationContext<Context>().resources.configuration.locales[0]

fun game(id: String): GameEntry = GameRegistry.games.first { it.id == id }
