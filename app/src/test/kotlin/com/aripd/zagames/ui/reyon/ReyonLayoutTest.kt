package com.aripd.zagames.ui.reyon

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Panel ile tepsinin kalan yüksekliği paylaşma kuralı.
 *
 * Kural saf bir işlev olduğu için burada cihaz da Robolectric de gerekmiyor;
 * ölçülen sayılar cihazdan geliyor (`docs/oyun-testi.md`), sınanan onların
 * aritmetiği. Asıl korunan şey: uzun ekranda panele [PANEL_WANT] bırakılıyor
 * ama bu kısa ekranın yerleşimini **hiç** değiştirmiyor.
 */
class ReyonLayoutTest {

    /** Kısa ekran: pay [PANEL_MIN]'de kalıyor, yani tepsi eskisi gibi ölçülüyor. */
    @Test
    fun shortScreenKeepsTheOldShare() {
        // 360×640 dp'de ölçülen kalan; panel 140 dp tabanını alıyordu.
        assertEquals(86.dp, trayHeight(226.dp, panelWant = PANEL_WANT))
        assertEquals(trayHeight(226.dp), trayHeight(226.dp, panelWant = PANEL_WANT))
    }

    /** Uzun ekran: tepsinin tavanı panele [PANEL_WANT] bırakacak kadar iniyor. */
    @Test
    fun tallScreenLeavesTheWantedShareToThePanel() {
        // 411 dp'de ölçülen kalan: tepsi doğal boyunda 219 dp istiyordu, panele
        // 208 dp kalıyor ve beşinci kural kırpılıyordu.
        assertEquals(426.dp - PANEL_WANT, trayHeight(426.dp, panelWant = PANEL_WANT))
    }

    /** Ayrım noktası: tepsiye [TRAY_KEEP] kalmıyorsa pay tabana düşüyor. */
    @Test
    fun theWantedShareOnlyAppliesWhenTheTrayStillGetsItsTwoRows() {
        assertEquals(PANEL_MIN, 308.dp - trayHeight(308.dp, panelWant = PANEL_WANT))
        assertEquals(PANEL_MIN + 1.dp, 309.dp - trayHeight(309.dp, panelWant = PANEL_WANT))
    }

    /** Payı yükseltmek tepsiyi tabanının altına indirmiyor. */
    @Test
    fun theTrayNeverDropsBelowItsFloor() {
        assertEquals(TRAY_MIN, trayHeight(100.dp, panelWant = PANEL_WANT))
    }

    /** Varsayılan çağrı (Diziliş, Sipariş) değişmedi. */
    @Test
    fun theDefaultShareIsUnchanged() {
        for (rest in listOf(100, 226, 308, 426, 700)) {
            assertEquals("kalan $rest dp", (rest.dp - PANEL_MIN).coerceAtLeast(TRAY_MIN), trayHeight(rest.dp))
        }
    }
}
