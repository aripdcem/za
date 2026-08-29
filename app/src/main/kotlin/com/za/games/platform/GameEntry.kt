package com.za.games.platform

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Platformdaki bir oyunun tanımı.
 *
 * Yeni bir oyun eklemek için bir [GameEntry] oluşturup [GameRegistry.games]
 * listesine eklemek yeterlidir; ana menü ve skor takibi otomatik çalışır.
 */
data class GameEntry(
    /** Kalıcı kimlik; skor deposunda anahtar olarak kullanılır. Değiştirmeyin. */
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val taglineRes: Int,
    /** Menü kartında kullanılan vurgu rengi. */
    val accent: Color,
    /** Menü kartındaki küçük görsel. */
    val art: @Composable (Modifier) -> Unit,
    /** Oyunun tam ekran arayüzü. */
    val screen: @Composable (
        highScore: Long,
        onScore: (Long) -> Unit,
        onExit: () -> Unit,
    ) -> Unit,
)
