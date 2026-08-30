package com.za.games.ui.g2048

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.za.games.R
import com.za.games.g2048.G2048State
import com.za.games.g2048.G2048Status
import com.za.games.g2048.MoveDir
import com.za.games.platform.LocalZaHaptics
import com.za.games.platform.LocalZaSound
import com.za.games.platform.Sfx
import com.za.games.ui.common.GameOverOverlay
import com.za.games.ui.common.GameTopBar
import com.za.games.ui.common.OverlayCard
import com.za.games.ui.common.ScoreCard
import com.za.games.ui.common.formatScore
import kotlin.math.abs

@Composable
fun G2048Screen(
    highScore: Long,
    onScore: (Long) -> Unit,
    onExit: () -> Unit,
    viewModel: G2048ViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = LocalZaHaptics.current
    val sound = LocalZaSound.current
    val latestScore by rememberUpdatedState(state.score)
    val latestOnScore by rememberUpdatedState(onScore)

    // Bitiş efekti yalnızca canlı geçişte (ekrana geri girişte tekrar etmez).
    var overHeard by remember { mutableStateOf(state.status == G2048Status.OVER) }
    LaunchedEffect(state.status) {
        if (state.status == G2048Status.OVER) {
            latestOnScore(state.score)
            if (!overHeard) {
                overHeard = true
                sound?.play(Sfx.OVER)
            }
        } else {
            overHeard = false
        }
    }
    DisposableEffect(Unit) {
        onDispose { latestOnScore(latestScore) }
    }
    // Birleşme sesi: yalnızca canlı hamlelerde; büyüyen taşla hafifçe tizleşir.
    var seenMoves by remember { mutableIntStateOf(state.moves) }
    LaunchedEffect(state.moves) {
        val previous = seenMoves
        seenMoves = state.moves
        if (state.moves > previous && state.lastMerged.isNotEmpty()) {
            val biggest = state.lastMerged.maxOf { state.cells[it] }
            sound?.play(Sfx.POP, rate = 0.9f + (Integer.numberOfTrailingZeros(biggest) * 0.04f))
            haptics.performHapticFeedback(
                if (biggest >= 512) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove,
            )
        }
    }
    // 2048 kutlaması ve katmanı: zaten kutlanmış bir oyuna geri girişte tekrar etmez.
    var wonShown by remember { mutableStateOf(state.reached2048) }
    var celebrated2048 by remember { mutableStateOf(state.reached2048) }
    LaunchedEffect(state.reached2048) {
        if (state.reached2048 && !celebrated2048) {
            celebrated2048 = true
            sound?.play(Sfx.BIG)
        } else if (!state.reached2048) {
            celebrated2048 = false
        }
    }
    BackHandler { onExit() }

    val previousBest = remember { mutableLongStateOf(highScore) }
    val restart = {
        previousBest.longValue = maxOf(previousBest.longValue, state.score)
        wonShown = false
        viewModel.newGame()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        GameTopBar(title = stringResource(R.string.game_2048), onExit = onExit) {
            TextButton(onClick = restart) {
                Text(stringResource(R.string.restart))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
            }

            Spacer(Modifier.height(16.dp))

            Box(contentAlignment = Alignment.Center) {
                BoardGrid(
                    state = state,
                    onMove = viewModel::move,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.status == G2048Status.OVER) {
                    GameOverOverlay(
                        score = state.score,
                        isRecord = state.score > previousBest.longValue,
                        onRestart = restart,
                        onExit = onExit,
                    )
                } else if (state.reached2048 && !wonShown) {
                    WonOverlay(
                        onKeepGoing = { wonShown = true },
                        onRestart = restart,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.swipe_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
            )
        }
    }
}

@Composable
private fun BoardGrid(
    state: G2048State,
    onMove: (MoveDir) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentMove by rememberUpdatedState(onMove)
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F1628))
            .pointerInput(Unit) {
                var dx = 0f
                var dy = 0f
                detectDragGestures(
                    onDragStart = {
                        dx = 0f
                        dy = 0f
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        dx += amount.x
                        dy += amount.y
                    },
                    onDragEnd = {
                        val threshold = 40.dp.toPx()
                        if (abs(dx) > abs(dy) && abs(dx) > threshold) {
                            currentMove(if (dx > 0) MoveDir.RIGHT else MoveDir.LEFT)
                        } else if (abs(dy) > threshold) {
                            currentMove(if (dy > 0) MoveDir.DOWN else MoveDir.UP)
                        }
                    },
                )
            }
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(state.size) { r ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(state.size) { c ->
                    val index = r * state.size + c
                    TileCell(
                        value = state.cells[index],
                        popped = index == state.lastSpawn || index in state.lastMerged,
                        moveCount = state.moves,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun TileCell(
    value: Int,
    popped: Boolean,
    moveCount: Int,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        // Boş yuva her zaman çizilir.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.05f)),
        )
        if (value > 0) {
            val scale = remember(moveCount, value) { Animatable(if (popped) 0.45f else 1f) }
            LaunchedEffect(moveCount, value) {
                if (scale.value < 1f) {
                    scale.animateTo(
                        1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
                    .clip(RoundedCornerShape(10.dp))
                    .background(tileColor(value)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = value.toString(),
                    fontWeight = FontWeight.Black,
                    fontSize = when {
                        value < 100 -> 26.sp
                        value < 1000 -> 22.sp
                        value < 10000 -> 18.sp
                        else -> 15.sp
                    },
                    color = if (value <= 4) Color(0xFF776E65) else Color.White,
                )
            }
        }
    }
}

private fun tileColor(value: Int): Color = when (value) {
    2 -> Color(0xFFEEE4DA)
    4 -> Color(0xFFEDE0C8)
    8 -> Color(0xFFF2B179)
    16 -> Color(0xFFF59563)
    32 -> Color(0xFFF67C5F)
    64 -> Color(0xFFF65E3B)
    128 -> Color(0xFFEDCF72)
    256 -> Color(0xFFEDCC61)
    512 -> Color(0xFFEDC850)
    1024 -> Color(0xFFEDC53F)
    2048 -> Color(0xFFEDC22E)
    else -> Color(0xFF3C3A32)
}

@Composable
private fun WonOverlay(onKeepGoing: () -> Unit, onRestart: () -> Unit) {
    OverlayCard {
        Text(
            text = stringResource(R.string.won_2048_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.won_2048_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onKeepGoing, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.keep_going))
        }
        OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.restart))
        }
    }
}
