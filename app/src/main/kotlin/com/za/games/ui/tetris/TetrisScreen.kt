package com.za.games.ui.tetris

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.za.games.R
import com.za.games.platform.LocalZaHaptics
import com.za.games.platform.LocalZaSound
import com.za.games.platform.Sfx
import com.za.games.tetris.TetrisState
import com.za.games.tetris.TetrisStatus
import com.za.games.ui.common.GameOverOverlay
import com.za.games.ui.common.GameTopBar
import com.za.games.ui.common.PadButton
import com.za.games.ui.common.PausedOverlay
import com.za.games.ui.common.formatScore
import java.util.Locale

@Composable
fun TetrisScreen(
    highScore: Long,
    onScore: (Long) -> Unit,
    onExit: () -> Unit,
    viewModel: TetrisViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = LocalZaHaptics.current
    val sound = LocalZaSound.current
    val latestScore by rememberUpdatedState(state.score)
    val latestOnScore by rememberUpdatedState(onScore)

    // Skoru oyun sonunda ve ekrandan ayrılırken platforma bildir. Bitiş
    // sesi yalnızca canlı geçişte çalar (ekrana geri girişte tekrar etmez).
    var overHeard by remember { mutableStateOf(state.status == TetrisStatus.OVER) }
    LaunchedEffect(state.status) {
        if (state.status == TetrisStatus.OVER) {
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

    // Satır temizleme: parlamalı animasyon + ses + titreşim.
    val clearFlash = remember { Animatable(0f) }
    var flashRows by remember { mutableStateOf<List<Int>>(emptyList()) }
    LaunchedEffect(state.clearEvents) {
        if (state.clearEvents > 0) {
            sound?.play(if (state.lastClear >= 4) Sfx.BIG else Sfx.CLEAR)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            flashRows = state.lastClearedRows
            clearFlash.snapTo(1f)
            clearFlash.animateTo(0f, animationSpec = tween(durationMillis = 320))
            flashRows = emptyList()
        }
    }
    // Temizlik olmayan kilitlenmelerde tok bir vuruş sesi.
    LaunchedEffect(state.locks) {
        if (state.locks > 0 && state.lastClear == 0) sound?.play(Sfx.DROP, volume = 0.6f)
    }

    // Uygulama arka plana geçince otomatik duraklat.
    LifecycleResumeEffect(Unit) {
        onPauseOrDispose { viewModel.pause() }
    }
    // Geri tuşu: önce duraklat, tekrar basınca menüye dön.
    BackHandler {
        if (state.status == TetrisStatus.RUNNING) viewModel.pause() else onExit()
    }

    // "Yeni rekor" rozetini bu oturumun başındaki rekora göre belirle.
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
        GameTopBar(title = stringResource(R.string.game_tetris), onExit = onExit) {
            TextButton(
                onClick = viewModel::togglePause,
                enabled = state.status != TetrisStatus.OVER,
            ) {
                Text(
                    text = stringResource(
                        if (state.status == TetrisStatus.PAUSED) R.string.resume else R.string.pause,
                    ),
                )
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                BoardCanvas(
                    state = state,
                    onMove = { if (it > 0) viewModel.moveRight() else viewModel.moveLeft() },
                    onSoftDrop = viewModel::softDrop,
                    onRotate = viewModel::rotateClockwise,
                    modifier = Modifier.aspectRatio(state.width / state.height.toFloat()),
                    flashRows = flashRows,
                    flashAlpha = clearFlash.value,
                )
                when (state.status) {
                    TetrisStatus.PAUSED -> PausedOverlay(
                        onResume = viewModel::togglePause,
                        onRestart = restart,
                        onExit = onExit,
                    )
                    TetrisStatus.OVER -> GameOverOverlay(
                        score = state.score,
                        isRecord = state.score > previousBest.longValue,
                        onRestart = restart,
                        onExit = onExit,
                    )
                    TetrisStatus.RUNNING -> Unit
                }
            }

            SidePanel(
                state = state,
                highScore = maxOf(highScore, state.score),
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight(),
            )
        }

        Text(
            text = stringResource(R.string.tetris_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 4.dp, end = 16.dp),
        )

        ControlsPad(
            onLeft = viewModel::moveLeft,
            onRight = viewModel::moveRight,
            onDown = viewModel::softDrop,
            onRotateCw = viewModel::rotateClockwise,
            onRotateCcw = viewModel::rotateCounterClockwise,
            onHardDrop = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.hardDrop()
            },
            onHold = viewModel::hold,
        )
    }
}

@Composable
private fun SidePanel(state: TetrisState, highScore: Long, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PanelBox(label = stringResource(R.string.hold_label)) {
            PiecePreview(type = state.hold, dimmed = state.holdUsed)
        }
        PanelBox(label = stringResource(R.string.next_label)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(TetrisState.VISIBLE_NEXT) { index ->
                    PiecePreview(type = state.next.getOrNull(index))
                }
            }
        }
        StatBlock(stringResource(R.string.score), formatScore(state.score), highlight = true)
        StatBlock(stringResource(R.string.high_score), formatScore(highScore))
        StatBlock(stringResource(R.string.level), state.level.toString())
        StatBlock(stringResource(R.string.lines), state.lines.toString())
    }
}

@Composable
private fun PanelBox(label: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        )
        Spacer(Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0F1628),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(Modifier.padding(6.dp)) { content() }
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String, highlight: Boolean = false) {
    Column {
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (highlight) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onBackground
            },
        )
    }
}

@Composable
private fun ControlsPad(
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onDown: () -> Unit,
    onRotateCw: () -> Unit,
    onRotateCcw: () -> Unit,
    onHardDrop: () -> Unit,
    onHold: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PadButton(
                label = "↺",
                description = stringResource(R.string.ctrl_rotate_ccw),
                modifier = Modifier.weight(1f).height(52.dp),
                onAction = onRotateCcw,
            )
            PadButton(
                label = "↻",
                description = stringResource(R.string.ctrl_rotate_cw),
                modifier = Modifier.weight(1f).height(52.dp),
                onAction = onRotateCw,
            )
            PadButton(
                label = "⇄",
                description = stringResource(R.string.ctrl_hold),
                modifier = Modifier.weight(1f).height(52.dp),
                onAction = onHold,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PadButton(
                label = "◀",
                description = stringResource(R.string.ctrl_left),
                modifier = Modifier.weight(1f).height(68.dp),
                repeatIntervalMs = 110L,
                onAction = onLeft,
            )
            PadButton(
                label = "▼",
                description = stringResource(R.string.ctrl_down),
                modifier = Modifier.weight(1f).height(68.dp),
                repeatIntervalMs = 50L,
                onAction = onDown,
            )
            PadButton(
                label = "▶",
                description = stringResource(R.string.ctrl_right),
                modifier = Modifier.weight(1f).height(68.dp),
                repeatIntervalMs = 110L,
                onAction = onRight,
            )
            PadButton(
                label = "⇓",
                description = stringResource(R.string.ctrl_hard_drop),
                modifier = Modifier.weight(1f).height(68.dp),
                accent = true,
                onAction = onHardDrop,
            )
        }
    }
}
