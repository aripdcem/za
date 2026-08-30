package com.za.games.ui.snake

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.za.games.R
import com.za.games.platform.LocalZaHaptics
import com.za.games.platform.LocalZaSound
import com.za.games.platform.Sfx
import com.za.games.snake.SnakeDir
import com.za.games.snake.SnakeState
import com.za.games.snake.SnakeStatus
import com.za.games.ui.common.GameOverOverlay
import com.za.games.ui.common.GameTopBar
import com.za.games.ui.common.PausedOverlay
import com.za.games.ui.common.ScoreCard
import com.za.games.ui.common.formatScore
import kotlin.math.abs

@Composable
fun SnakeScreen(
    highScore: Long,
    onScore: (Long) -> Unit,
    onExit: () -> Unit,
    viewModel: SnakeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = LocalZaHaptics.current
    val sound = LocalZaSound.current
    val latestScore by rememberUpdatedState(state.score)
    val latestOnScore by rememberUpdatedState(onScore)

    // Bitiş efekti yalnızca canlı geçişte (ekrana geri girişte tekrar etmez).
    var overHeard by remember { mutableStateOf(state.status == SnakeStatus.OVER) }
    LaunchedEffect(state.status) {
        if (state.status == SnakeStatus.OVER) {
            latestOnScore(state.score)
            if (!overHeard) {
                overHeard = true
                sound?.play(Sfx.OVER)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        } else {
            overHeard = false
        }
    }
    DisposableEffect(Unit) {
        onDispose { latestOnScore(latestScore) }
    }
    // Yem sesi: yalnızca canlı yenen yemlerde, yılan uzadıkça hafifçe tizleşir.
    var seenFoods by remember { mutableIntStateOf(state.foods) }
    LaunchedEffect(state.foods) {
        val previous = seenFoods
        seenFoods = state.foods
        if (state.foods > previous) {
            sound?.play(Sfx.POP, rate = 1f + (state.foods % 12) * 0.03f)
        }
    }
    LifecycleResumeEffect(Unit) {
        onPauseOrDispose { viewModel.pause() }
    }
    BackHandler {
        if (state.status == SnakeStatus.RUNNING) viewModel.pause() else onExit()
    }

    val previousBest = remember { mutableLongStateOf(highScore) }
    val restart = {
        previousBest.longValue = maxOf(previousBest.longValue, state.score)
        viewModel.newGame()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        GameTopBar(title = stringResource(R.string.game_snake), onExit = onExit) {
            TextButton(
                onClick = viewModel::togglePause,
                enabled = state.status != SnakeStatus.OVER,
            ) {
                Text(
                    text = stringResource(
                        if (state.status == SnakeStatus.PAUSED) R.string.resume else R.string.pause,
                    ),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ScoreCard(
                label = stringResource(R.string.score),
                value = formatScore(state.score),
                modifier = Modifier.weight(1f),
                highlight = true,
            )
            ScoreCard(
                label = stringResource(R.string.high_score),
                value = formatScore(maxOf(highScore, state.score)),
                modifier = Modifier.weight(1f),
            )
            ScoreCard(
                label = stringResource(R.string.length_label),
                value = state.body.size.toString(),
                modifier = Modifier.weight(1f),
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            SnakeBoard(
                state = state,
                onTurn = viewModel::turn,
                onTapCell = { row, col ->
                    // Dokunulan tarafa yönlen: yatay giderken üst/alt,
                    // dikey giderken sol/sağ seçilir — her an tek anlamlı seçim.
                    if (state.status == SnakeStatus.RUNNING) {
                        val head = state.body.first()
                        val heading = state.queued ?: state.pending
                        val turn = if (heading.dRow == 0) {
                            when {
                                row < head.row -> SnakeDir.UP
                                row > head.row -> SnakeDir.DOWN
                                else -> null
                            }
                        } else {
                            when {
                                col < head.col -> SnakeDir.LEFT
                                col > head.col -> SnakeDir.RIGHT
                                else -> null
                            }
                        }
                        turn?.let(viewModel::turn)
                    }
                },
                modifier = Modifier.aspectRatio(state.width / state.height.toFloat()),
            )
            when (state.status) {
                SnakeStatus.PAUSED -> PausedOverlay(
                    onResume = viewModel::togglePause,
                    onRestart = restart,
                    onExit = onExit,
                )
                SnakeStatus.OVER -> GameOverOverlay(
                    score = state.score,
                    isRecord = state.score > previousBest.longValue,
                    onRestart = restart,
                    onExit = onExit,
                )
                SnakeStatus.RUNNING -> Unit
            }
        }

        Text(
            text = stringResource(R.string.snake_tap_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun SnakeBoard(
    state: SnakeState,
    onTurn: (SnakeDir) -> Unit,
    onTapCell: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentTurn by rememberUpdatedState(onTurn)
    val currentTapCell by rememberUpdatedState(onTapCell)
    val pulse by rememberInfiniteTransition(label = "food")
        .animateFloat(
            initialValue = 0.8f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
            label = "foodPulse",
        )

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val cell = size.width / state.width.toFloat()
                    val col = (offset.x / cell).toInt().coerceIn(0, state.width - 1)
                    val row = (offset.y / cell).toInt().coerceIn(0, state.height - 1)
                    currentTapCell(row, col)
                }
            }
            .pointerInput(Unit) {
            var dx = 0f
            var dy = 0f
            var fired = false
            detectDragGestures(
                onDragStart = {
                    dx = 0f
                    dy = 0f
                    fired = false
                },
                onDrag = { change, amount ->
                    change.consume()
                    dx += amount.x
                    dy += amount.y
                    if (!fired) {
                        val threshold = 24.dp.toPx()
                        if (abs(dx) > abs(dy) && abs(dx) > threshold) {
                            currentTurn(if (dx > 0) SnakeDir.RIGHT else SnakeDir.LEFT)
                            fired = true
                        } else if (abs(dy) > threshold) {
                            currentTurn(if (dy > 0) SnakeDir.DOWN else SnakeDir.UP)
                            fired = true
                        }
                    }
                },
            )
        },
    ) {
        val cell = size.width / state.width

        drawRoundRect(color = Color(0xFF0F1628), cornerRadius = CornerRadius(16f, 16f))

        val gridColor = Color.White.copy(alpha = 0.04f)
        for (c in 1 until state.width) {
            drawLine(gridColor, Offset(c * cell, 0f), Offset(c * cell, size.height), 1f)
        }
        for (r in 1 until state.height) {
            drawLine(gridColor, Offset(0f, r * cell), Offset(size.width, r * cell), 1f)
        }

        // Yem: nabız gibi atan kırmızı nokta.
        val foodCenter = Offset(
            (state.food.col + 0.5f) * cell,
            (state.food.row + 0.5f) * cell,
        )
        drawCircle(Color(0xFFF87171), radius = cell * 0.34f * pulse, center = foodCenter)

        // Gövde: baştan kuyruğa doğru sönen yeşil.
        val last = state.body.size - 1
        state.body.forEachIndexed { i, segment ->
            val t = if (last == 0) 0f else i / last.toFloat()
            val pad = cell * (0.08f + 0.06f * t)
            drawRoundRect(
                color = Color(0xFF4ADE80).copy(alpha = 1f - 0.45f * t),
                topLeft = Offset(segment.col * cell + pad, segment.row * cell + pad),
                size = Size(cell - 2 * pad, cell - 2 * pad),
                cornerRadius = CornerRadius(cell * 0.3f, cell * 0.3f),
            )
        }

        // Baş üzerine göz çifti.
        val head = state.body.first()
        val eyeOffset = cell * 0.18f
        val center = Offset((head.col + 0.5f) * cell, (head.row + 0.5f) * cell)
        val (ex, ey) = when (state.dir) {
            SnakeDir.UP, SnakeDir.DOWN -> eyeOffset to 0f
            SnakeDir.LEFT, SnakeDir.RIGHT -> 0f to eyeOffset
        }
        val forward = Offset(state.dir.dCol * cell * 0.12f, state.dir.dRow * cell * 0.12f)
        drawCircle(Color(0xFF06121D), radius = cell * 0.08f, center = center + forward + Offset(ex, ey))
        drawCircle(Color(0xFF06121D), radius = cell * 0.08f, center = center + forward - Offset(ex, ey))
    }
}
