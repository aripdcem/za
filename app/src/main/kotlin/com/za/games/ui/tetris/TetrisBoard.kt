package com.za.games.ui.tetris

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import com.za.games.tetris.Tetromino
import com.za.games.tetris.TetrisState
import com.za.games.tetris.TetrisStatus
import kotlin.math.abs
import kotlin.math.min

/** Taş renkleri: koyu zeminde okunaklı, klasik paletten uyarlanmış tonlar. */
fun Tetromino.color(): Color = when (this) {
    Tetromino.I -> Color(0xFF22D3EE)
    Tetromino.O -> Color(0xFFFACC15)
    Tetromino.T -> Color(0xFFA78BFA)
    Tetromino.S -> Color(0xFF4ADE80)
    Tetromino.Z -> Color(0xFFF87171)
    Tetromino.J -> Color(0xFF60A5FA)
    Tetromino.L -> Color(0xFFFB923C)
}

/**
 * Oyun tahtası. Dokunma desteği: tahtaya tek dokunuş döndürür, yatay
 * sürükleme taşı hücre hücre kaydırır, aşağı sürükleme yumuşak düşüş yapar.
 */
@Composable
fun BoardCanvas(
    state: TetrisState,
    onMove: (Int) -> Unit,
    onSoftDrop: () -> Unit,
    onRotate: () -> Unit,
    modifier: Modifier = Modifier,
    flashRows: List<Int> = emptyList(),
    flashAlpha: Float = 0f,
) {
    val boardWidth = state.width
    Canvas(
        modifier = modifier
            // Döndürme (tek dokunuş) ve taşıma/yumuşak düşüş (sürükleme) ayrı jest
            // algılayıcılarındaydı ve aynı pointer akışını tüketiyorlardı; birleştirip
            // gerçek sürükleme başlayana kadar dokunuş adayı olarak tutuyoruz.
            .pointerInput(Unit) {
                val touchSlop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown()
                    var accumulatedX = 0f
                    var accumulatedY = 0f
                    var dragging = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.changedToUpIgnoreConsumed()) {
                            if (!dragging) onRotate()
                            change.consume()
                            break
                        }
                        val delta = change.positionChange()
                        accumulatedX += delta.x
                        accumulatedY += delta.y
                        if (!dragging &&
                            (abs(accumulatedX) > touchSlop || abs(accumulatedY) > touchSlop)
                        ) {
                            dragging = true
                        }
                        if (dragging) {
                            val cell = size.width / boardWidth.toFloat()
                            while (accumulatedX >= cell) {
                                onMove(1)
                                accumulatedX -= cell
                            }
                            while (accumulatedX <= -cell) {
                                onMove(-1)
                                accumulatedX += cell
                            }
                            while (accumulatedY >= cell) {
                                onSoftDrop()
                                accumulatedY -= cell
                            }
                            if (accumulatedY < 0f) accumulatedY = 0f
                        }
                        change.consume()
                    }
                }
            },
    ) {
        drawTetrisBoard(state, flashRows, flashAlpha)
    }
}

private fun DrawScope.drawTetrisBoard(
    state: TetrisState,
    flashRows: List<Int>,
    flashAlpha: Float,
) {
    val cell = size.width / state.width

    drawRoundRect(
        color = Color(0xFF0F1628),
        cornerRadius = CornerRadius(16f, 16f),
    )

    val gridColor = Color.White.copy(alpha = 0.05f)
    for (c in 1 until state.width) {
        drawLine(gridColor, Offset(c * cell, 0f), Offset(c * cell, size.height), strokeWidth = 1f)
    }
    for (r in 1 until state.height) {
        drawLine(gridColor, Offset(0f, r * cell), Offset(size.width, r * cell), strokeWidth = 1f)
    }

    val pad = cell * 0.07f
    val corner = CornerRadius(cell * 0.18f, cell * 0.18f)

    fun drawBlock(row: Int, col: Int, color: Color) {
        if (row < 0) return
        val topLeft = Offset(col * cell + pad, row * cell + pad)
        val blockSize = Size(cell - 2 * pad, cell - 2 * pad)
        drawRoundRect(color = color, topLeft = topLeft, size = blockSize, cornerRadius = corner)
        drawRoundRect(
            color = Color.White.copy(alpha = 0.16f),
            topLeft = topLeft,
            size = Size(blockSize.width, blockSize.height * 0.42f),
            cornerRadius = corner,
        )
    }

    state.board.forEachIndexed { r, row ->
        row.forEachIndexed { c, piece ->
            if (piece != null) drawBlock(r, c, piece.color())
        }
    }

    if (state.status != TetrisStatus.OVER) {
        val ghostColor = state.active.type.color().copy(alpha = 0.35f)
        state.ghost.cells.forEach { (r, c) ->
            if (r >= 0) {
                drawRoundRect(
                    color = ghostColor,
                    topLeft = Offset(c * cell + pad, r * cell + pad),
                    size = Size(cell - 2 * pad, cell - 2 * pad),
                    cornerRadius = corner,
                    style = Stroke(width = cell * 0.08f),
                )
            }
        }
    }

    state.active.cells.forEach { (r, c) -> drawBlock(r, c, state.active.type.color()) }

    // Satır temizleme parlaması: temizlenen satırlar kısaca beyaz yanıp söner.
    if (flashAlpha > 0f) {
        flashRows.forEach { r ->
            drawRoundRect(
                color = Color.White.copy(alpha = 0.85f * flashAlpha),
                topLeft = Offset(0f, r * cell),
                size = Size(size.width, cell),
                cornerRadius = corner,
            )
        }
    }
}

/** Hold ve sıradaki taşlar için küçük önizleme. */
@Composable
fun PiecePreview(type: Tetromino?, modifier: Modifier = Modifier, dimmed: Boolean = false) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp),
    ) {
        if (type == null) return@Canvas
        val cells = type.rotations[0]
        val minRow = cells.minOf { it.row }
        val maxRow = cells.maxOf { it.row }
        val minCol = cells.minOf { it.col }
        val maxCol = cells.maxOf { it.col }
        val pieceWidth = maxCol - minCol + 1
        val pieceHeight = maxRow - minRow + 1

        val cell = min(size.width / 4.5f, size.height / 2.2f)
        val originX = (size.width - pieceWidth * cell) / 2f
        val originY = (size.height - pieceHeight * cell) / 2f
        val color = type.color().copy(alpha = if (dimmed) 0.35f else 1f)
        val corner = CornerRadius(cell * 0.22f, cell * 0.22f)

        cells.forEach { (r, c) ->
            drawRoundRect(
                color = color,
                topLeft = Offset(originX + (c - minCol) * cell + 1f, originY + (r - minRow) * cell + 1f),
                size = Size(cell - 2f, cell - 2f),
                cornerRadius = corner,
            )
        }
    }
}
