package com.za.games.ui.g2048

import android.graphics.Canvas
import android.graphics.RectF
import com.za.games.g2048.G2048State
import com.za.games.platform.ShareDraw
import com.za.games.platform.drawCentered

/** Paylaşım kartı ressamı: son tahta, ekrandaki taş renkleriyle. */
internal fun g2048Painter(state: G2048State): (Canvas, RectF) -> Unit = { canvas, rect ->
    canvas.drawRect(rect, ShareDraw.fill(0xFF0F1628.toInt()))
    val n = state.size
    val margin = rect.width() * 0.04f
    val unit = (rect.width() - 2 * margin) / n
    val tile = unit * 0.90f
    val corner = tile * 0.12f
    val emptyFill = ShareDraw.fill(0x0DFFFFFF)
    val darkInk = 0xFF776E65.toInt()
    val lightInk = 0xFFFFFFFF.toInt()
    val box = RectF()
    for (i in 0 until n * n) {
        val r = i / n
        val c = i % n
        val x = rect.left + margin + c * unit + (unit - tile) / 2f
        val y = rect.top + margin + r * unit + (unit - tile) / 2f
        box.set(x, y, x + tile, y + tile)
        val value = state.cells[i]
        if (value == 0) {
            canvas.drawRoundRect(box, corner, corner, emptyFill)
            continue
        }
        canvas.drawRoundRect(box, corner, corner, ShareDraw.fill(tileArgb(value)))
        val scale = when {
            value < 100 -> 0.50f
            value < 1000 -> 0.42f
            value < 10000 -> 0.34f
            else -> 0.28f
        }
        canvas.drawCentered(value.toString(), box.centerX(), box.centerY(), ShareDraw.text(tile * scale, if (value <= 4) darkInk else lightInk))
    }
}

private fun tileArgb(value: Int): Int = when (value) {
    2 -> 0xFFEEE4DA
    4 -> 0xFFEDE0C8
    8 -> 0xFFF2B179
    16 -> 0xFFF59563
    32 -> 0xFFF67C5F
    64 -> 0xFFF65E3B
    128 -> 0xFFEDCF72
    256 -> 0xFFEDCC61
    512 -> 0xFFEDC850
    1024 -> 0xFFEDC53F
    2048 -> 0xFFEDC22E
    else -> 0xFF3C3A32
}.toInt()
