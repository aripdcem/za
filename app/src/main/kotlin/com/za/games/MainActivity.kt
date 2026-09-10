package com.za.games

import android.os.Bundle
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.za.games.ui.theme.ZaTheme

class MainActivity : ComponentActivity() {

    /** Dokunarak keşif açılıp kapanınca çubuk kararı yenilenir. */
    private val touchExplorationListener = AccessibilityManager.TouchExplorationStateChangeListener { hideSystemBars() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        getSystemService(AccessibilityManager::class.java)?.addTouchExplorationStateChangeListener(touchExplorationListener)
        hideSystemBars()
        setContent {
            ZaTheme {
                ZaApp()
            }
        }
    }

    override fun onDestroy() {
        getSystemService(AccessibilityManager::class.java)?.removeTouchExplorationStateChangeListener(touchExplorationListener)
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    /**
     * Tam ekran: gezinme ve durum çubukları gizlenir; kenardan kaydırınca
     * geçici olarak görünüp kendiliğinden kaybolurlar. Ekranlar
     * safeDrawingPadding kullandığından çentik payı korunur.
     *
     * Dokunarak keşif (TalkBack) açıkken çubuklar gizlenmez: gizli çubuğun
     * bölgesine çizilen düğmelerin erişilebilirlik sınırı bazı cihazlarda
     * sıfırlanıyor ve ekran okuyucu oraya inemiyor (docs/oyun-testi.md,
     * Reyon Sipariş bulgu 3). Çubuklar görünürken içerik onların üstünde kalır.
     */
    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        val exploring = getSystemService(AccessibilityManager::class.java)?.isTouchExplorationEnabled == true
        if (exploring) {
            controller.show(WindowInsetsCompat.Type.systemBars())
            return
        }
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}
