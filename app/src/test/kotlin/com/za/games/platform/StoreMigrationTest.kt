package com.za.games.platform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/**
 * v0.39.0'da Blok'un kimliği değişti. Cihazda duran rekor ve "son oynananlar"
 * kaydı yeni kimliğe taşınmalı, eski anahtar da silinmeli.
 */
@RunWith(AndroidJUnit4::class)
class StoreMigrationTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun scores() = context.getSharedPreferences("za_scores", Context.MODE_PRIVATE)
    private fun settings() = context.getSharedPreferences("za_settings", Context.MODE_PRIVATE)

    @Test
    fun blokRecordMovesToTheNewId() {
        scores().edit().clear().putLong("tetris", 4200L).commit()
        assertEquals(4200L, ScoreStore(context).highScore("blok"))
        assertFalse("eski anahtar silinir", scores().contains("tetris"))
    }

    @Test
    fun theHigherRecordWinsAndNothingIsLost() {
        scores().edit().clear().putLong("tetris", 900L).putLong("blok", 1500L).commit()
        assertEquals(1500L, ScoreStore(context).highScore("blok"))
        scores().edit().clear().putLong("tetris", 1800L).putLong("blok", 1200L).commit()
        assertEquals(1800L, ScoreStore(context).highScore("blok"))
    }

    @Test
    fun aFreshInstallIsUntouched() {
        scores().edit().clear().commit()
        val store = ScoreStore(context)
        assertEquals(0L, store.highScore("blok"))
        store.submit("blok", 7L)
        assertEquals(7L, store.highScore("blok"))
    }

    @Test
    fun recentlyPlayedEntryMoves() {
        settings().edit().clear().putLong("played_tetris", 1_700_000_000_000L).commit()
        assertEquals(1_700_000_000_000L, SettingsStore(context).lastPlayed("blok"))
        assertFalse("eski anahtar silinir", settings().contains("played_tetris"))
    }
}
