package com.za.games.ui.kakuro

import android.graphics.Canvas
import android.graphics.RectF
import com.za.games.kakuro.KakuroState
import com.za.games.kakuro.KakuroStatus
import com.za.games.platform.ShareDraw
import com.za.games.platform.drawCentered

/** Paylaşım kartı ressamı: ekrandaki renklerle, çözülmüş tahta. */
internal fun kakuroPainter(state: KakuroState): (Canvas, RectF) -> Unit = { canvas, rect ->
    val n = state.size
    val cell = rect.width() / n
    val solved = state.status == KakuroStatus.SOLVED
    val clueFill = ShareDraw.fill(0xFF1E293B.toInt())
    val whiteFill = ShareDraw.fill(if (solved) 0xFFBBF7D0.toInt() else 0xFFE8ECF3.toInt())
    val clueLine = ShareDraw.stroke(0x59FFFFFF, cell * 0.02f)
    val grid = ShareDraw.stroke(0x33000000, cell * 0.02f)
    val clueText = ShareDraw.text(cell * 0.30f, 0xFFE2E8F0.toInt())
    val digitText = ShareDraw.text(cell * 0.52f, 0xFF0F172A.toInt())

    canvas.drawRect(rect, ShareDraw.fill(0xFF0F1628.toInt()))
    for (r in 0 until n) {
        for (c in 0 until n) {
            val i = r * n + c
            val x = rect.left + c * cell
            val y = rect.top + r * cell
            val info = state.cells[i]
            if (!info.white) {
                canvas.drawRect(x, y, x + cell, y + cell, clueFill)
                if (info.across > 0 || info.down > 0) {
                    canvas.drawLine(x, y, x + cell, y + cell, clueLine)
                    if (info.across > 0) {
                        canvas.drawCentered(info.across.toString(), x + cell * 0.68f, y + cell * 0.30f, clueText)
                    }
                    if (info.down > 0) {
                        canvas.drawCentered(info.down.toString(), x + cell * 0.32f, y + cell * 0.70f, clueText)
                    }
                }
            } else {
                canvas.drawRect(x, y, x + cell, y + cell, whiteFill)
                val v = state.values[i]
                if (v > 0) canvas.drawCentered(v.toString(), x + cell / 2f, y + cell / 2f, digitText)
            }
        }
    }
    for (k in 0..n) {
        canvas.drawLine(rect.left + k * cell, rect.top, rect.left + k * cell, rect.bottom, grid)
        canvas.drawLine(rect.left, rect.top + k * cell, rect.right, rect.top + k * cell, grid)
    }
}
