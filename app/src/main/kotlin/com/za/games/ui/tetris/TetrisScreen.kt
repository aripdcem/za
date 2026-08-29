package com.za.games.ui.tetris

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.za.games.R
import com.za.games.tetris.TetrisState
import com.za.games.tetris.TetrisStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

private fun formatScore(value: Long): String =
    String.format(Locale.getDefault(), "%,d", value)

@Composable
fun TetrisScreen(
    highScore: Long,
    onScore: (Long) -> Unit,
    onExit: () -> Unit,
    viewModel: TetrisViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    // Oyun bitince skoru platforma bildir.
    LaunchedEffect(state.status) {
        if (state.status == TetrisStatus.OVER) onScore(state.score)
    }
    // Satır temizlenince küçük bir titreşim.
    LaunchedEffect(state.lines) {
        if (state.lines > 0) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        TetrisTopBar(
            status = state.status,
            onExit = onExit,
            onTogglePause = viewModel::togglePause,
        )

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
                )
                when (state.status) {
                    TetrisStatus.PAUSED -> PausedOverlay(
                        onResume = viewModel::togglePause,
                        onRestart = {
                            previousBest.longValue = maxOf(previousBest.longValue, state.score)
                            viewModel.newGame()
                        },
                        onExit = onExit,
                    )
                    TetrisStatus.OVER -> GameOverOverlay(
                        score = state.score,
                        isRecord = state.score > previousBest.longValue,
                        onRestart = {
                            previousBest.longValue = maxOf(previousBest.longValue, state.score)
                            viewModel.newGame()
                        },
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
private fun TetrisTopBar(
    status: TetrisStatus,
    onExit: () -> Unit,
    onTogglePause: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onExit) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
            )
        }
        Text(
            text = stringResource(R.string.game_tetris).uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            letterSpacing = 3.sp,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onTogglePause, enabled = status != TetrisStatus.OVER) {
            Text(
                text = stringResource(
                    if (status == TetrisStatus.PAUSED) R.string.resume else R.string.pause,
                ),
            )
        }
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

/**
 * Oyun tuşu. [repeatIntervalMs] verilirse basılı tutuldukça tekrarlar
 * (ilk tekrar 220 ms sonra başlar).
 */
@Composable
private fun PadButton(
    label: String,
    description: String,
    modifier: Modifier = Modifier,
    repeatIntervalMs: Long = 0L,
    accent: Boolean = false,
    onAction: () -> Unit,
) {
    val currentAction by rememberUpdatedState(onAction)
    val interactionSource = remember { MutableInteractionSource() }
    val repeatable = repeatIntervalMs > 0L

    if (repeatable) {
        val pressed by interactionSource.collectIsPressedAsState()
        LaunchedEffect(pressed) {
            if (pressed) {
                currentAction()
                delay(220L)
                while (isActive) {
                    currentAction()
                    delay(repeatIntervalMs)
                }
            }
        }
    }

    Surface(
        onClick = { if (!repeatable) currentAction() },
        modifier = modifier.semantics { contentDescription = description },
        shape = RoundedCornerShape(16.dp),
        color = if (accent) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        interactionSource = interactionSource,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 22.sp,
                color = if (accent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun OverlayCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        modifier = Modifier.padding(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun PausedOverlay(onResume: () -> Unit, onRestart: () -> Unit, onExit: () -> Unit) {
    OverlayCard {
        Text(
            text = stringResource(R.string.paused),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onResume, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.resume))
        }
        OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.restart))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

@Composable
private fun GameOverOverlay(
    score: Long,
    isRecord: Boolean,
    onRestart: () -> Unit,
    onExit: () -> Unit,
) {
    OverlayCard {
        Text(
            text = stringResource(R.string.game_over),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = formatScore(score),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
        )
        if (isRecord) {
            Text(
                text = stringResource(R.string.new_record),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Spacer(Modifier.height(4.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.restart))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}
