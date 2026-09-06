package com.za.games.ui.mines

import android.graphics.Canvas
import android.graphics.RectF
import com.za.games.mines.MinesState
import com.za.games.mines.MinesStatus
import com.za.games.platform.ShareDraw
import com.za.games.platform.drawCentered
import kotlin.math.min

private val NUMBER_ARGB = intArrayOf(
    0,
    0xFF60A5FA.toInt(), 0xFF4ADE80.toInt(), 0xFFF87171.toInt(), 0xFFA78BFA.toInt(),
    0xFFFB923C.toInt(), 0xFF22D3EE.toInt(), 0xFFE4EAF5.toInt(), 0xFFAAB4C8.toInt(),
)

/** Paylaşım kartı ressamı: oyun sonundaki tahta (mayınlar, bayraklar, patlayan hücre). */
internal fun minesPainter(state: MinesState): (Canvas, RectF) -> Unit = { canvas, rect ->
    canvas.drawRect(rect, ShareDraw.fill(0xFF0F1628.toInt()))
    val cell = min(rect.width() / state.width, rect.height() / state.height)
    val left = rect.left + (rect.width() - cell * state.width) / 2f
    val top = rect.top + (rect.height() - cell * state.height) / 2f
    val pad = cell * 0.05f
    val corner = cell * 0.16f
    val revealedFill = ShareDraw.fill(0x08FFFFFF)
    val hiddenFill = ShareDraw.fill(0xFF223049.toInt())
    val explodedFill = ShareDraw.fill(0xFF7F1D1D.toInt())
    val emoji = ShareDraw.text(cell * 0.5f, 0xFFFFFFFF.toInt())
    val numbers = NUMBER_ARGB.map { ShareDraw.text(cell * 0.5f, it) }
    val lost = state.status == MinesStatus.LOST
    val won = state.status == MinesStatus.WON
    val box = RectF()

    for (index in 0 until state.cellCount) {
        val r = index / state.width
        val c = index % state.width
        box.set(left + c * cell + pad, top + r * cell + pad, left + (c + 1) * cell - pad, top + (r + 1) * cell - pad)
        val cx = box.centerX()
        val cy = box.centerY()
        if (index in state.revealed) {
            canvas.drawRoundRect(box, corner, corner, revealedFill)
            val number = state.adjacentMines(index)
            if (number > 0) canvas.drawCentered(number.toString(), cx, cy, numbers[number])
        } else {
            val exploded = lost && index == state.exploded
            canvas.drawRoundRect(box, corner, corner, if (exploded) explodedFill else hiddenFill)
            when {
                lost && index in state.mines -> canvas.drawCentered("💣", cx, cy, emoji)
                won && index in state.mines -> canvas.drawCentered("🚩", cx, cy, emoji)
                index in state.flagged -> canvas.drawCentered("🚩", cx, cy, emoji)
            }
        }
    }
}
