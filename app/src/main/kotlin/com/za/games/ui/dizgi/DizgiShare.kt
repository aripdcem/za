package com.za.games.ui.dizgi

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.za.games.dizgi.DizgiBoard
import com.za.games.dizgi.DizgiState
import com.za.games.dizgi.Premium
import com.za.games.platform.ShareDraw
import com.za.games.platform.drawCentered
import java.util.Locale

private val ShareTrLocale: Locale = Locale.forLanguageTag("tr")

/** Paylaşım kartı ressamı: bitmiş tahta, premium kareler ve taşlar. */
internal fun dizgiPainter(state: DizgiState): (Canvas, RectF) -> Unit = { canvas, rect ->
    canvas.drawRect(rect, ShareDraw.fill(0xFF0F1628.toInt()))
    val n = DizgiBoard.SIZE
    val cell = rect.width() / n
    val pad = cell * 0.05f
    val corner = cell * 0.16f
    val face = ShareDraw.fill(0xFFEADFC8.toInt())
    val ink = ShareDraw.text(cell * 0.58f, 0xFF1F2937.toInt())
    val jokerInk = ShareDraw.text(cell * 0.58f, 0xFF7C3AED.toInt())
    val points = ShareDraw.text(cell * 0.24f, 0xFF1F2937.toInt(), bold = false).apply { textAlign = Paint.Align.RIGHT }
    val label = ShareDraw.text(cell * 0.30f, 0x8CFFFFFF.toInt())
    val premiumFill = mapOf(
        Premium.NONE to ShareDraw.fill(0x0BFFFFFF),
        Premium.DL to ShareDraw.fill(ShareDraw.withAlpha(0xFF3B82F6.toInt(), 0x4D)),
        Premium.TL to ShareDraw.fill(ShareDraw.withAlpha(0xFF2563EB.toInt(), 0x99)),
        Premium.DW to ShareDraw.fill(ShareDraw.withAlpha(0xFFFB923C.toInt(), 0x47)),
        Premium.TW to ShareDraw.fill(ShareDraw.withAlpha(0xFFF87171.toInt(), 0x8C)),
    )
    val box = RectF()
    for (index in 0 until DizgiBoard.CELLS) {
        val r = index / n
        val c = index % n
        box.set(rect.left + c * cell + pad, rect.top + r * cell + pad, rect.left + (c + 1) * cell - pad, rect.top + (r + 1) * cell - pad)
        val tile = state.board[index]
        if (tile != null) {
            canvas.drawRoundRect(box, corner, corner, face)
            canvas.drawCentered(
                tile.letter.toString().uppercase(ShareTrLocale),
                box.centerX() - cell * 0.04f,
                box.centerY(),
                if (tile.isJoker) jokerInk else ink,
            )
            if (!tile.isJoker) {
                canvas.drawText(tile.points.toString(), box.right - cell * 0.06f, box.bottom - cell * 0.08f, points)
            }
        } else {
            val premium = DizgiBoard.premium(index)
            canvas.drawRoundRect(box, corner, corner, premiumFill.getValue(premium))
            val text = when {
                index == DizgiBoard.CENTER -> "★"
                premium == Premium.DL -> "2H"
                premium == Premium.TL -> "3H"
                premium == Premium.DW -> "2K"
                premium == Premium.TW -> "3K"
                else -> null
            }
            if (text != null) canvas.drawCentered(text, box.centerX(), box.centerY(), label)
        }
    }
}
