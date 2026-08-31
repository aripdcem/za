package com.za.games

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.za.games.platform.GameRegistry
import com.za.games.platform.LocalZaHaptics
import com.za.games.platform.LocalZaSound
import com.za.games.platform.ScoreStore
import com.za.games.platform.SettingsStore
import com.za.games.platform.SoundPlayer
import com.za.games.platform.gatedBy
import com.za.games.ui.hub.HubScreen

/**
 * Uygulama kökü: ana menü ile oyunlar arasında geçişi, rekor akışını ve
 * paylaşılan ses çaları yönetir.
 */
@Composable
fun ZaApp() {
    val context = LocalContext.current
    val scoreStore = remember { ScoreStore(context) }
    val settings = remember { SettingsStore(context) }
    val highScores = remember {
        mutableStateMapOf<String, Long>().apply {
            GameRegistry.games.forEach { put(it.id, scoreStore.highScore(it.id)) }
        }
    }

    var soundOn by remember { mutableStateOf(settings.soundEnabled) }
    val soundPlayer = remember { SoundPlayer(context) { soundOn } }
    DisposableEffect(Unit) {
        onDispose { soundPlayer.release() }
    }

    var hapticsOn by remember { mutableStateOf(settings.hapticsEnabled) }
    val systemHaptics = LocalHapticFeedback.current
    val gatedHaptics = remember(systemHaptics) { systemHaptics.gatedBy { hapticsOn } }

    var currentGameId by rememberSaveable { mutableStateOf<String?>(null) }
    val currentGame = GameRegistry.games.firstOrNull { it.id == currentGameId }

    BackHandler(enabled = currentGame != null) { currentGameId = null }

    CompositionLocalProvider(LocalZaSound provides soundPlayer, LocalZaHaptics provides gatedHaptics) {
        if (currentGame == null) {
            HubScreen(
                games = GameRegistry.games,
                highScores = highScores,
                onPlay = { currentGameId = it.id },
                soundOn = soundOn,
                onToggleSound = {
                    soundOn = !soundOn
                    settings.soundEnabled = soundOn
                },
                hapticsOn = hapticsOn,
                onToggleHaptics = {
                    hapticsOn = !hapticsOn
                    settings.hapticsEnabled = hapticsOn
                },
            )
        } else {
            currentGame.screen(
                highScores[currentGame.id] ?: 0L,
                { score ->
                    scoreStore.submit(currentGame.id, score)
                    if (score > (highScores[currentGame.id] ?: 0L)) {
                        highScores[currentGame.id] = score
                    }
                },
                { currentGameId = null },
            )
        }
    }
}
