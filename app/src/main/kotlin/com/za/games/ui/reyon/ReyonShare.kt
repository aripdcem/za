package com.za.games.ui.reyon

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.za.games.platform.ShareDraw
import com.za.games.platform.drawCentered
import com.za.games.reyon.Brand
import com.za.games.reyon.ReyonState

/** Marka renkleri (ARGB); ekran ve paylaşım kartı aynı paleti kullanır. */
internal fun brandArgb(brand: Brand): Int = when (brand) {
    Brand.A -> 0xFF60A5FA.toInt()
    Brand.B -> 0xFFFBBF24.toInt()
    Brand.C -> 0xFF4ADE80.toInt()
    Brand.D -> 0xFFF472B6.toInt()
}

/** Paylaşım kartı ressamı: dizilmiş raf, ürün adları ve ★ işaretleri. */
internal fun reyonPainter(state: ReyonState, res: Resources): (Canvas, RectF) -> Unit = { canvas, rect ->
    val puzzle = state.puzzle
    val rows = puzzle.rows
    val cols = puzzle.cols
    val cw = rect.width() / cols
    val sh = rect.height() / rows
    val plank = sh * 0.12f
    val pad = cw * 0.05f
    canvas.drawRect(rect, ShareDraw.fill(0xFF0F1628.toInt()))
    val plankPaint = ShareDraw.fill(0xFF475569.toInt())
    for (r in 0 until rows) {
        val y = rect.top + (r + 1) * sh - plank
        canvas.drawRoundRect(RectF(rect.left, y, rect.right, y + plank), plank * 0.3f, plank * 0.3f, plankPaint)
    }
    val nameText = ShareDraw.text(sh * 0.2f, 0xFF0F172A.toInt())
    val starText = ShareDraw.text(sh * 0.2f, 0xFF0F172A.toInt(), bold = false)
    for (p in puzzle.products) {
        val pl = state.placement(p.id) ?: continue
        val x = rect.left + pl.col * cw + pad
        val y = rect.top + pl.row * sh + pad
        val w = p.facings * cw - 2 * pad
        val h = sh - plank - 2 * pad
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), cw * 0.12f, cw * 0.12f, ShareDraw.fill(brandArgb(p.brand)))
        val name = ReyonText.kind(res, p.kind)
        val paint = Paint(nameText)
        val maxWidth = w - cw * 0.16f
        val measured = paint.measureText(name)
        if (measured > maxWidth) paint.textSize = paint.textSize * maxWidth / measured
        canvas.drawCentered(name, x + w / 2f, y + h * 0.42f, paint)
        val marks = buildString {
            repeat(p.size) { append('•') }
            if (p.premium) append(" ★")
            if (p.heavy) append(" ▼")
        }
        canvas.drawCentered(marks, x + w / 2f, y + h * 0.76f, starText)
    }
}
