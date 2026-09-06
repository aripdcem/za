package com.za.games.ui.sudoku

import android.graphics.Canvas
import android.graphics.RectF
import com.za.games.platform.ShareDraw
import com.za.games.platform.drawCentered
import com.za.games.sudoku.SudokuState

/** Paylaşım kartı ressamı: ipuçları kalın, oyuncunun yazdıkları vurgu renginde. */
internal fun sudokuPainter(state: SudokuState): (Canvas, RectF) -> Unit = { canvas, rect ->
    val cell = rect.width() / 9f
    val given = ShareDraw.text(cell * 0.55f, 0xFFE4EAF5.toInt(), bold = true)
    val entered = ShareDraw.text(cell * 0.55f, 0xFF4DE1FF.toInt(), bold = false)
    val thin = ShareDraw.stroke(0x12FFFFFF, cell * 0.02f)
    val thick = ShareDraw.stroke(0x4DFFFFFF, cell * 0.05f)

    canvas.drawRect(rect, ShareDraw.fill(0xFF0F1628.toInt()))
    for (i in 0 until 81) {
        val v = state.values[i]
        if (v == 0) continue
        val r = i / 9
        val c = i % 9
        canvas.drawCentered(
            v.toString(),
            rect.left + (c + 0.5f) * cell,
            rect.top + (r + 0.5f) * cell,
            if (state.given[i]) given else entered,
        )
    }
    for (k in 1..8) {
        val paint = if (k % 3 == 0) thick else thin
        canvas.drawLine(rect.left + k * cell, rect.top, rect.left + k * cell, rect.bottom, paint)
        canvas.drawLine(rect.left, rect.top + k * cell, rect.right, rect.top + k * cell, paint)
    }
}
