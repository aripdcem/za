package com.aripd.zagames.ui.vergici

import androidx.compose.ui.graphics.Color
import com.aripd.zagames.ui.theme.ZaColors
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Vergici tahtasında seçili sayı: koyu mürekkep, saydam sarı zemin üzerine
 * çizilir. Zemin uygulama arka planıyla karışır; saydamlık düşürülürse rakam
 * okunmaz olur (0,45'te 2,8:1 ölçülmüştü). WCAG AA metin için 4,5:1 ister.
 */
class VergiciCellContrastTest {

    private fun luminance(color: Color): Double {
        fun kanal(v: Float): Double {
            val c = v.toDouble()
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * kanal(color.red) + 0.7152 * kanal(color.green) + 0.0722 * kanal(color.blue)
    }

    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /** Saydam ön rengi arka plan üzerine bindirir (alfa karışımı). */
    private fun over(fg: Color, alpha: Float, bg: Color): Color = Color(
        red = fg.red * alpha + bg.red * (1f - alpha),
        green = fg.green * alpha + bg.green * (1f - alpha),
        blue = fg.blue * alpha + bg.blue * (1f - alpha),
    )

    @Test
    fun `selected number ink is readable on its blended fill`() {
        val fill = over(CoinColor, SelectedFillAlpha, ZaColors.background)
        val oran = contrast(SelectedInk, fill)
        assertTrue(
            "seçili sayı kontrastı ${"%.2f".format(oran)}:1; WCAG AA en az 4,5:1 ister",
            oran >= 4.5,
        )
    }
}
