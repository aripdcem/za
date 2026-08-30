package com.za.games.ui.sudoku

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.za.games.R
import com.za.games.platform.LocalZaHaptics
import com.za.games.platform.LocalZaSound
import com.za.games.platform.Sfx
import com.za.games.sudoku.SudokuDifficulty
import com.za.games.sudoku.SudokuState
import com.za.games.sudoku.SudokuStatus
import com.za.games.ui.common.DifficultyOverlay
import com.za.games.ui.common.GameTopBar
import com.za.games.ui.common.OverlayCard
import com.za.games.ui.common.ScoreCard
import com.za.games.ui.common.formatTime

@Composable
fun difficultyLabel(difficulty: SudokuDifficulty): String = stringResource(
    when (difficulty) {
        SudokuDifficulty.EASY -> R.string.difficulty_easy
        SudokuDifficulty.MEDIUM -> R.string.difficulty_medium
        SudokuDifficulty.HARD -> R.string.difficulty_hard
    },
)

@Composable
fun SudokuScreen(
    highScore: Long,
    onScore: (Long) -> Unit,
    onExit: () -> Unit,
    viewModel: SudokuViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val elapsed by viewModel.elapsed.collectAsStateWithLifecycle()
    val haptics = LocalZaHaptics.current
    val sound = LocalZaSound.current

    var selected by rememberSaveable { mutableIntStateOf(-1) }
    var notesMode by rememberSaveable { mutableStateOf(false) }

    // Rekor = toplam çözülen bulmaca sayısı. Sayaç, ekrana bitmiş bir
    // tahtayla girildiğinde o oyunu yeniden saymasın diye mevcut tohumla
    // başlatılır (yoksa her geri giriş hayalet +1 üretirdi).
    val baseline = remember { highScore }
    var solvedSession by remember { mutableIntStateOf(0) }
    var countedSeed by remember {
        mutableLongStateOf(
            state?.takeIf { it.status == SudokuStatus.SOLVED }?.seed ?: Long.MIN_VALUE,
        )
    }
    val latestOnScore by rememberUpdatedState(onScore)

    LaunchedEffect(state?.status, state?.seed) {
        val s = state ?: return@LaunchedEffect
        if (s.status == SudokuStatus.SOLVED && s.seed != countedSeed) {
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
        GameTopBar(title = stringResource(R.string.game_sudoku), onExit = onExit) {
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
                SudokuBoard(
                    state = puzzle,
                    selected = selected,
                    onSelect = { selected = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            when {
                state == null -> DifficultyOverlay(
                    descriptions = SudokuDifficulty.entries.map {
                        stringResource(R.string.sudoku_difficulty_desc_fmt, it.targetClues)
                    },
                ) { index ->
                    viewModel.newGame(SudokuDifficulty.entries[index])
                }
                state?.status == SudokuStatus.SOLVED -> SolvedOverlay(
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

        Text(
            text = stringResource(R.string.sudoku_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        state?.let { puzzle ->
            SudokuPad(
                state = puzzle,
                notesMode = notesMode,
                onDigit = { digit ->
                    if (selected >= 0) {
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
                onToggleNotes = { notesMode = !notesMode },
            )
        }
    }
}

@Composable
private fun SudokuBoard(
    state: SudokuState,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val conflicts = remember(state.values) { state.conflicts }
    val selectedValue = if (selected in 0 until 81) state.values[selected] else 0

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F1628)),
    ) {
        Column(Modifier.fillMaxSize()) {
            repeat(9) { r ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    repeat(9) { c ->
                        val index = r * 9 + c
                        SudokuCell(
                            row = r,
                            col = c,
                            value = state.values[index],
                            notes = state.notes[index],
                            isGiven = state.given[index],
                            isSelected = index == selected,
                            isPeerOfSelected = selected >= 0 && index != selected &&
                                sharesUnit(index, selected),
                            hasSameValue = selectedValue != 0 && index != selected &&
                                state.values[index] == selectedValue,
                            isConflict = index in conflicts,
                            onClick = { onSelect(index) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }
        // 3x3 kutu ayraçları ve ince ızgara.
        Canvas(Modifier.matchParentSize()) {
            val cell = size.width / 9f
            val thin = Color.White.copy(alpha = 0.07f)
            val thick = Color.White.copy(alpha = 0.30f)
            for (i in 1..8) {
                val heavy = i % 3 == 0
                val color = if (heavy) thick else thin
                val stroke = if (heavy) 3f else 1f
                drawLine(color, Offset(i * cell, 0f), Offset(i * cell, size.height), stroke)
                drawLine(color, Offset(0f, i * cell), Offset(size.width, i * cell), stroke)
            }
        }
    }
}

private fun sharesUnit(a: Int, b: Int): Boolean {
    val ra = a / 9
    val ca = a % 9
    val rb = b / 9
    val cb = b % 9
    return ra == rb || ca == cb || (ra / 3 == rb / 3 && ca / 3 == cb / 3)
}

@Composable
private fun SudokuCell(
    row: Int,
    col: Int,
    value: Int,
    notes: Set<Int>,
    isGiven: Boolean,
    isSelected: Boolean,
    isPeerOfSelected: Boolean,
    hasSameValue: Boolean,
    isConflict: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
        hasSameValue -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        isPeerOfSelected -> Color.White.copy(alpha = 0.045f)
        else -> Color.Transparent
    }
    val content = if (value > 0) value.toString() else stringResource(R.string.sudoku_cell_empty)
    val note = when {
        isConflict -> " (${stringResource(R.string.sudoku_cell_conflict_note)})"
        isGiven -> " (${stringResource(R.string.sudoku_cell_given_note)})"
        else -> ""
    }
    val description = stringResource(R.string.sudoku_cell_desc_fmt, row + 1, col + 1, content + note)
    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .background(background)
            .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (value > 0) {
            Text(
                text = value.toString(),
                fontSize = 19.sp,
                fontWeight = if (isGiven) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    isConflict -> MaterialTheme.colorScheme.error
                    isGiven -> MaterialTheme.colorScheme.onBackground
                    else -> MaterialTheme.colorScheme.primary
                },
            )
        } else if (notes.isNotEmpty()) {
            Text(
                text = buildString {
                    for (row in 0..2) {
                        for (col in 0..2) {
                            val digit = row * 3 + col + 1
                            append(if (digit in notes) digit.toString() else " ")
                            if (col < 2) append(' ')
                        }
                        if (row < 2) append('\n')
                    }
                },
                fontSize = 10.sp,
                lineHeight = 11.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun SudokuPad(
    state: SudokuState,
    notesMode: Boolean,
    onDigit: (Int) -> Unit,
    onErase: () -> Unit,
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
                DigitButton(digit, state, Modifier.weight(1f)) { onDigit(digit) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (digit in 6..9) {
                DigitButton(digit, state, Modifier.weight(1f)) { onDigit(digit) }
            }
            PadActionButton(
                label = "⌫",
                description = stringResource(R.string.erase),
                modifier = Modifier.weight(1f),
                onClick = onErase,
            )
            PadActionButton(
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
private fun DigitButton(
    digit: Int,
    state: SudokuState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val remaining = 9 - state.values.count { it == digit }
    val description = stringResource(R.string.sudoku_digit_desc_fmt, digit, remaining.coerceAtLeast(0))
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .height(56.dp)
            .semantics { contentDescription = description },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = digit.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (remaining <= 0) 0.3f else 1f,
                ),
            )
            Text(
                text = remaining.coerceAtLeast(0).toString(),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun PadActionButton(
    label: String,
    description: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (active) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = modifier
            .height(56.dp)
            .semantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                fontSize = 20.sp,
                color = if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
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
