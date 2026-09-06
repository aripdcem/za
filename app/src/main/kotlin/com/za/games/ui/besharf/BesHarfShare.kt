package com.za.games.ui.besharf

import android.graphics.Canvas
import android.graphics.RectF
import com.za.games.besharf.BesHarfState
import com.za.games.besharf.LetterMark
import com.za.games.platform.ShareDraw
import kotlin.math.min

/**
 * Paylaşım kartı ressamı: harfsiz renkli kutular (günlük kelimeyi ele vermez),
 * oynanmamış satırlar boş çerçeve.
 */
internal fun besHarfPainter(state: BesHarfState): (Canvas, RectF) -> Unit = { canvas, rect ->
    canvas.drawRect(rect, ShareDraw.fill(0xFF0F1628.toInt()))
    val cols = BesHarfState.WORD_LENGTH
    val rows = BesHarfState.MAX_GUESSES
    val unit = min(rect.width() / cols, rect.height() / rows) * 0.9f
    val tile = unit * 0.86f
    val left = rect.centerX() - cols * unit / 2f
    val top = rect.centerY() - rows * unit / 2f
    val corner = tile * 0.14f
    val fills = mapOf(
        LetterMark.CORRECT to ShareDraw.fill(0xFF4ADE80.toInt()),
        LetterMark.PRESENT to ShareDraw.fill(0xFFFACC15.toInt()),
        LetterMark.ABSENT to ShareDraw.fill(0xFF313A4E.toInt()),
    )
    val empty = ShareDraw.stroke(0xFF313A4E.toInt(), tile * 0.06f)
    val box = RectF()
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val x = left + c * unit + (unit - tile) / 2f
            val y = top + r * unit + (unit - tile) / 2f
            box.set(x, y, x + tile, y + tile)
            val mark = state.marks.getOrNull(r)?.getOrNull(c)
            canvas.drawRoundRect(box, corner, corner, if (mark != null) fills.getValue(mark) else empty)
        }
    }
}

/** Düz metin paylaşımı için emoji ızgarası (Wordle geleneği). */
internal fun besHarfEmojiGrid(state: BesHarfState): String = state.marks.joinToString("\n") { row ->
    row.joinToString("") { mark ->
        when (mark) {
            LetterMark.CORRECT -> "🟩"
            LetterMark.PRESENT -> "🟨"
            LetterMark.ABSENT -> "⬛"
        }
    }
}
