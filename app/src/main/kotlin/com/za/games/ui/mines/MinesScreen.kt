package com.za.games.ui.mines

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.za.games.R
import com.za.games.mines.MinesDifficulty
import com.za.games.mines.MinesState
import com.za.games.mines.MinesStatus
import com.za.games.platform.LocalZaHaptics
import com.za.games.platform.LocalZaSound
import com.za.games.platform.Sfx
import com.za.games.ui.common.DifficultyOverlay
import com.za.games.ui.common.GameTopBar
import com.za.games.ui.common.OverlayCard
import com.za.games.ui.common.PadButton
import com.za.games.ui.common.ScoreCard
import com.za.games.ui.common.formatTime

@Composable
fun MinesScreen(
    highScore: Long,
    onScore: (Long) -> Unit,
    onExit: () -> Unit,
    viewModel: MinesViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val elapsed by viewModel.elapsed.collectAsStateWithLifecycle()
    val haptics = LocalZaHaptics.current
    val sound = LocalZaSound.current

    var flagMode by rememberSaveable { mutableStateOf(false) }

    // Rekor = toplam kazanılan oyun sayısı. Sayaç, ekrana bitmiş bir
    // tahtayla girildiğinde o oyunu yeniden saymasın/ses çalmasın diye
    // mevcut tohumla başlatılır.
    val baseline = remember { highScore }
    var winsSession by remember { mutableIntStateOf(0) }
    var countedSeed by remember {
        mutableLongStateOf(
            state?.takeIf {
                it.status == MinesStatus.WON || it.status == MinesStatus.LOST
            }?.seed ?: Long.MIN_VALUE,
        )
    }
    val latestOnScore by rememberUpdatedState(onScore)

    LaunchedEffect(state?.status, state?.seed) {
        val s = state ?: return@LaunchedEffect
        when {
            s.status == MinesStatus.WON && s.seed != countedSeed -> {
                countedSeed = s.seed
                winsSession++
                latestOnScore(baseline + winsSession)
                sound?.play(Sfx.BIG)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            s.status == MinesStatus.LOST && s.seed != countedSeed -> {
                countedSeed = s.seed
                sound?.play(Sfx.OVER)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }
    LifecycleResumeEffect(Unit) {
        viewModel.setPaused(false)
        onPauseOrDispose { viewModel.setPaused(true) }
    }
    BackHandler { onExit() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        GameTopBar(title = stringResource(R.string.game_mines), onExit = onExit) {
            TextButton(onClick = viewModel::reset) {
                Text(stringResource(R.string.new_game))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ScoreCard(
                label = stringResource(R.string.mines_left_label),
                value = state?.let { (it.mineCount - it.flagged.size).toString() } ?: "—",
                modifier = Modifier.weight(1f),
                highlight = true,
            )
            ScoreCard(
                label = stringResource(R.string.time_label),
                value = formatTime(elapsed),
                modifier = Modifier.weight(1f),
            )
            ScoreCard(
                label = stringResource(R.string.wins_label),
                value = (baseline + winsSession).toString(),
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
            state?.let { board ->
                MinesBoard(
                    state = board,
                    onCellTap = { index ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (flagMode && index !in board.revealed) {
                            viewModel.toggleFlag(index)
                            sound?.play(Sfx.POP, volume = 0.5f)
                        } else if (index in board.revealed) {
                            viewModel.chord(index)
                        } else {
                            viewModel.reveal(index)
                            sound?.play(Sfx.DROP, volume = 0.5f)
                        }
                    },
                    onCellLongPress = { index ->
                        if (index !in board.revealed) {
                            viewModel.toggleFlag(index)
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            sound?.play(Sfx.POP, volume = 0.5f)
                        }
                    },
                    modifier = Modifier.aspectRatio(board.width / board.height.toFloat()),
                )
            }
            when (state?.status) {
                null -> DifficultyOverlay(
                    descriptions = MinesDifficulty.entries.map {
                        stringResource(R.string.mines_difficulty_desc_fmt, it.width, it.height, it.mineCount)
                    },
                ) { index ->
                    viewModel.newGame(MinesDifficulty.entries[index])
                }
                MinesStatus.WON -> ResultOverlay(
                    title = stringResource(R.string.mines_won),
                    time = formatTime(elapsed),
                    onRetry = viewModel::retry,
                    onPickDifficulty = viewModel::reset,
                    onExit = onExit,
                )
                MinesStatus.LOST -> ResultOverlay(
                    title = stringResource(R.string.mines_lost),
                    time = formatTime(elapsed),
                    onRetry = viewModel::retry,
                    onPickDifficulty = viewModel::reset,
                    onExit = onExit,
                )
                else -> Unit
            }
        }

        Text(
            text = stringResource(R.string.mines_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            PadButton(
                label = if (flagMode) "🚩" else "⛏️",
                description = stringResource(R.string.flag_mode),
                modifier = Modifier.width(150.dp).height(52.dp),
                accent = flagMode,
            ) { flagMode = !flagMode }
        }
    }
}

private val NUMBER_COLORS = listOf(
    Color.Transparent,
    Color(0xFF60A5FA), Color(0xFF4ADE80), Color(0xFFF87171), Color(0xFFA78BFA),
    Color(0xFFFB923C), Color(0xFF22D3EE), Color(0xFFE4EAF5), Color(0xFFAAB4C8),
)

@Composable
private fun MinesBoard(
    state: MinesState,
    onCellTap: (Int) -> Unit,
    onCellLongPress: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val currentTap by rememberUpdatedState(onCellTap)
    val currentLongPress by rememberUpdatedState(onCellLongPress)

    Canvas(
        modifier = modifier.pointerInput(state.width, state.height) {
            val toIndex = { offset: Offset ->
                val cell = size.width / state.width.toFloat()
                val col = (offset.x / cell).toInt().coerceIn(0, state.width - 1)
                val row = (offset.y / cell).toInt().coerceIn(0, state.height - 1)
                row * state.width + col
            }
            detectTapGestures(
                onTap = { offset -> currentTap(toIndex(offset)) },
                onLongPress = { offset -> currentLongPress(toIndex(offset)) },
            )
        },
    ) {
        val cell = size.width / state.width
        val corner = CornerRadius(cell * 0.16f, cell * 0.16f)
        val pad = cell * 0.05f
        val lost = state.status == MinesStatus.LOST
        val won = state.status == MinesStatus.WON

        fun textAt(text: String, index: Int, color: Color, scale: Float = 0.5f) {
            val layout = textMeasurer.measure(
                AnnotatedString(text),
                style = TextStyle(
                    fontSize = with(this) { (cell * scale).toSp() },
                    fontWeight = FontWeight.Bold,
                    color = color,
                ),
            )
            val r = index / state.width
            val c = index % state.width
            drawText(
                layout,
                topLeft = Offset(
                    c * cell + (cell - layout.size.width) / 2f,
                    r * cell + (cell - layout.size.height) / 2f,
                ),
            )
        }

        drawRoundRect(Color(0xFF0F1628), cornerRadius = CornerRadius(16f, 16f))

        for (index in 0 until state.cellCount) {
            val r = index / state.width
            val c = index % state.width
            val topLeft = Offset(c * cell + pad, r * cell + pad)
            val boxSize = Size(cell - 2 * pad, cell - 2 * pad)

            if (index in state.revealed) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.03f),
                    topLeft = topLeft,
                    size = boxSize,
                    cornerRadius = corner,
                )
                val number = state.adjacentMines(index)
                if (number > 0) textAt(number.toString(), index, NUMBER_COLORS[number])
            } else {
                val exploded = lost && index == state.exploded
                drawRoundRect(
                    color = if (exploded) Color(0xFF7F1D1D) else Color(0xFF223049),
                    topLeft = topLeft,
                    size = boxSize,
                    cornerRadius = corner,
                )
                when {
                    lost && index in state.mines -> textAt("💣", index, Color.White)
                    won && index in state.mines -> textAt("🚩", index, Color.White)
                    index in state.flagged -> textAt("🚩", index, Color.White)
                }
            }
        }
    }
}

@Composable
private fun ResultOverlay(
    title: String,
    time: String,
    onRetry: () -> Unit,
    onPickDifficulty: () -> Unit,
    onExit: () -> Unit,
) {
    OverlayCard {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.time_fmt, time),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.try_again))
        }
        OutlinedButton(onClick = onPickDifficulty, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.pick_difficulty))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}
