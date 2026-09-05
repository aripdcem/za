package com.za.games.ui.kakuro

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.za.games.R
import com.za.games.kakuro.KakuroDifficulty
import com.za.games.kakuro.KakuroState
import com.za.games.kakuro.KakuroStatus
import com.za.games.platform.LocalZaHaptics
import com.za.games.platform.LocalZaSound
import com.za.games.platform.Sfx
import com.za.games.ui.common.DifficultyOverlay
import com.za.games.ui.common.GameTopBar
import com.za.games.ui.common.OverlayCard
import com.za.games.ui.common.ScoreCard
import com.za.games.ui.common.formatTime

private val BoardBg = Color(0xFF0F1628)
private val ClueCell = Color(0xFF1E293B)
private val ClueLine = Color(0x59FFFFFF)
private val ClueText = Color(0xFFE2E8F0)
private val WhiteCell = Color(0xFFE8ECF3)
private val PeerCell = Color(0xFFC7DAFB)
private val SelectedCell = Color(0xFF93C5FD)
private val SolvedCell = Color(0xFFBBF7D0)
private val Digit = Color(0xFF0F172A)
private val ConflictDigit = Color(0xFFDC2626)
private val NoteText = Color(0xFF475569)
private val GridLine = Color(0x33000000)

@Composable
private fun difficultyLabel(difficulty: KakuroDifficulty): String = stringResource(
    when (difficulty) {
        KakuroDifficulty.EASY -> R.string.difficulty_easy
        KakuroDifficulty.MEDIUM -> R.string.difficulty_medium
        KakuroDifficulty.HARD -> R.string.difficulty_hard
    },
)

@Composable
fun KakuroScreen(
    highScore: Long,
    onScore: (Long) -> Unit,
    onExit: () -> Unit,
    viewModel: KakuroViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val generating by viewModel.generating.collectAsStateWithLifecycle()
    val elapsed by viewModel.elapsed.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val haptics = LocalZaHaptics.current
    val sound = LocalZaSound.current

    var selected by rememberSaveable { mutableIntStateOf(-1) }
    var notesMode by rememberSaveable { mutableStateOf(false) }

    // Rekor = çözülen bulmaca sayısı; ekrana çözülmüş tahtayla girilince yeniden sayılmaz.
    val baseline = remember { highScore }
    var solvedSession by remember { mutableIntStateOf(0) }
    var countedSeed by remember {
        mutableLongStateOf(state?.takeIf { it.status == KakuroStatus.SOLVED }?.seed ?: Long.MIN_VALUE)
    }
    val latestOnScore by rememberUpdatedState(onScore)

    LaunchedEffect(state?.status, state?.seed) {
        val s = state ?: return@LaunchedEffect
        if (s.status == KakuroStatus.SOLVED && s.seed != countedSeed) {
            countedSeed = s.seed
            solvedSession++
            latestOnScore(baseline + solvedSession)
            sound?.play(Sfx.BIG)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
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
        GameTopBar(title = stringResource(R.string.game_kakuro), onExit = onExit) {
            TextButton(onClick = {
                selected = -1
                viewModel.reset()
            }) {
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
                label = stringResource(R.string.time_label),
                value = formatTime(elapsed),
                modifier = Modifier.weight(1f),
                highlight = true,
            )
            ScoreCard(
                label = stringResource(R.string.difficulty_label),
                value = state?.let { difficultyLabel(it.difficulty) } ?: "—",
                modifier = Modifier.weight(1f),
            )
            ScoreCard(
                label = stringResource(R.string.solved_label),
                value = (baseline + solvedSession).toString(),
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
            state?.let { puzzle ->
                KakuroBoard(
                    state = puzzle,
                    selected = selected,
                    onSelect = { index ->
                        selected = if (index >= 0 && puzzle.cells[index].white) index else -1
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            when {
                state == null && generating -> OverlayCard {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.kakuro_generating))
                }
                state == null -> DifficultyOverlay(
                    descriptions = KakuroDifficulty.entries.map {
                        stringResource(R.string.kakuro_difficulty_desc_fmt, it.size - 1, it.size - 1)
                    },
                    lastPicked = viewModel.lastDifficulty?.let { KakuroDifficulty.entries.indexOf(it) },
                ) { index ->
                    selected = -1
                    viewModel.newGame(KakuroDifficulty.entries[index])
                }
                state?.status == KakuroStatus.SOLVED -> SolvedOverlay(
                    time = formatTime(elapsed),
                    onSameDifficulty = {
                        selected = -1
                        viewModel.retry()
                    },
                    onPickDifficulty = {
                        selected = -1
                        viewModel.reset()
                    },
                    onExit = onExit,
                )
            }
        }

        RunInfo(state = state, selected = selected)

        state?.let { puzzle ->
            KakuroPad(
                notesMode = notesMode,
                canUndo = canUndo,
                onDigit = { digit ->
                    if (selected >= 0 && puzzle.cells[selected].white) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (notesMode) {
                            viewModel.toggleNote(selected, digit)
                        } else {
                            viewModel.setValue(selected, digit)
                            sound?.play(Sfx.POP, volume = 0.6f)
                        }
                    }
                },
                onErase = {
                    if (selected >= 0) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.clearCell(selected)
                    }
                },
                onUndo = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.undo()
                },
                onToggleNotes = { notesMode = !notesMode },
            )
        }
    }
}

/** Seçili hücrenin koşuları: ipucu, kalan toplam ve boş hücre sayısı. */
@Composable
private fun RunInfo(state: KakuroState?, selected: Int) {
    val text = if (state == null) {
        stringResource(R.string.kakuro_rules)
    } else if (selected < 0 || !state.cells[selected].white) {
        stringResource(R.string.kakuro_select_hint)
    } else {
        val parts = ArrayList<String>()
        for ((runIndex, horizontal) in listOf(state.acrossOf[selected] to true, state.downOf[selected] to false)) {
            if (runIndex < 0) continue
            val run = state.runs[runIndex]
            val filled = run.cells.sumOf { state.values[it] }
            val empty = run.cells.count { state.values[it] == 0 }
            parts += stringResource(
                if (horizontal) R.string.kakuro_run_across_fmt else R.string.kakuro_run_down_fmt,
                run.sum,
                run.sum - filled,
                empty,
            )
        }
        parts.joinToString("   ·   ")
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        textAlign = TextAlign.Center,
        maxLines = 2,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )
}

@Composable
private fun KakuroBoard(
    state: KakuroState,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val n = state.size
    val conflicts = remember(state.values, state.cells) { state.conflicts }
    val solvedRuns = remember(state.values, state.cells) { state.solvedRuns }
    val currentSelect by rememberUpdatedState(onSelect)
    val textMeasurer = rememberTextMeasurer()
    val cache = remember { HashMap<String, TextLayoutResult>() }
    val empty = state.cells.indices.count { state.cells[it].white && state.values[it] == 0 }
    val desc = stringResource(R.string.kakuro_board_desc_fmt, n - 1, n - 1, empty)
    val peerAcross = if (selected >= 0) state.acrossOf[selected] else -1
    val peerDown = if (selected >= 0) state.downOf[selected] else -1
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(BoardBg)
            .semantics { contentDescription = desc }
            .pointerInput(n) {
                detectTapGestures { pos ->
                    val cell = size.width / n.toFloat()
                    val col = (pos.x / cell).toInt().coerceIn(0, n - 1)
                    val row = (pos.y / cell).toInt().coerceIn(0, n - 1)
                    currentSelect(row * n + col)
                }
            },
    ) {
        val cell = size.width / n
        val clueSize = (cell * 0.30f).toSp()
        val digitSize = (cell * 0.52f).toSp()
        val noteSize = (cell * 0.22f).toSp()

        fun text(key: String, value: String, sizeSp: TextUnit, color: Color, bold: Boolean): TextLayoutResult =
            cache.getOrPut("$key|$value|${cell.toInt()}|${color.value}") {
                textMeasurer.measure(
                    AnnotatedString(value),
                    style = TextStyle(fontSize = sizeSp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium, color = color),
                )
            }

        fun drawCentered(layout: TextLayoutResult, cx: Float, cy: Float) {
            drawText(layout, topLeft = Offset(cx - layout.size.width / 2f, cy - layout.size.height / 2f))
        }

        for (r in 0 until n) {
            for (c in 0 until n) {
                val i = r * n + c
                val x = c * cell
                val y = r * cell
                val info = state.cells[i]
                if (!info.white) {
                    drawRect(ClueCell, topLeft = Offset(x, y), size = Size(cell, cell))
                    if (info.across > 0 || info.down > 0) {
                        drawLine(ClueLine, Offset(x, y), Offset(x + cell, y + cell), strokeWidth = 1.5f)
                        if (info.across > 0) {
                            drawCentered(text("c", info.across.toString(), clueSize, ClueText, true), x + cell * 0.68f, y + cell * 0.30f)
                        }
                        if (info.down > 0) {
                            drawCentered(text("c", info.down.toString(), clueSize, ClueText, true), x + cell * 0.32f, y + cell * 0.70f)
                        }
                    }
                    continue
                }
                val inSolved = (state.acrossOf[i] in solvedRuns) || (state.downOf[i] in solvedRuns)
                val fill = when {
                    i == selected -> SelectedCell
                    peerAcross >= 0 && (state.acrossOf[i] == peerAcross || state.downOf[i] == peerDown) -> PeerCell
                    inSolved -> SolvedCell
                    else -> WhiteCell
                }
                drawRect(fill, topLeft = Offset(x, y), size = Size(cell, cell))
                val v = state.values[i]
                if (v > 0) {
                    val color = if (i in conflicts) ConflictDigit else Digit
                    drawCentered(text("d", v.toString(), digitSize, color, true), x + cell / 2f, y + cell / 2f)
                } else if (state.notes[i].isNotEmpty()) {
                    for (d in 1..9) {
                        if (d !in state.notes[i]) continue
                        val nc = (d - 1) % 3
                        val nr = (d - 1) / 3
                        drawCentered(text("n", d.toString(), noteSize, NoteText, false), x + cell * (0.2f + nc * 0.3f), y + cell * (0.22f + nr * 0.28f))
                    }
                }
            }
        }
        for (k in 0..n) {
            drawLine(GridLine, Offset(k * cell, 0f), Offset(k * cell, size.height), 1.5f)
            drawLine(GridLine, Offset(0f, k * cell), Offset(size.width, k * cell), 1.5f)
        }
    }
}

@Composable
private fun KakuroPad(
    notesMode: Boolean,
    canUndo: Boolean,
    onDigit: (Int) -> Unit,
    onErase: () -> Unit,
    onUndo: () -> Unit,
    onToggleNotes: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (digit in 1..5) {
                KakuroKey(label = digit.toString(), description = digit.toString(), modifier = Modifier.weight(1f)) { onDigit(digit) }
            }
            KakuroKey(
                label = "↶",
                description = stringResource(R.string.undo),
                modifier = Modifier.weight(1f),
                enabled = canUndo,
                onClick = onUndo,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (digit in 6..9) {
                KakuroKey(label = digit.toString(), description = digit.toString(), modifier = Modifier.weight(1f)) { onDigit(digit) }
            }
            KakuroKey(
                label = "⌫",
                description = stringResource(R.string.erase),
                modifier = Modifier.weight(1f),
                onClick = onErase,
            )
            KakuroKey(
                label = "✎",
                description = stringResource(R.string.notes_toggle),
                modifier = Modifier.weight(1f),
                active = notesMode,
                onClick = onToggleNotes,
            )
        }
    }
}

@Composable
private fun KakuroKey(
    label: String,
    description: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = if (active) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = modifier
            .height(52.dp)
            .semantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    active -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun SolvedOverlay(
    time: String,
    onSameDifficulty: () -> Unit,
    onPickDifficulty: () -> Unit,
    onExit: () -> Unit,
) {
    OverlayCard {
        Text(
            text = stringResource(R.string.congrats),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.time_fmt, time),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onSameDifficulty, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.same_difficulty))
        }
        OutlinedButton(onClick = onPickDifficulty, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.pick_difficulty))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}
