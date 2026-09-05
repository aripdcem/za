package com.za.games.ui.vergici

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.za.games.R
import com.za.games.platform.LocalZaHaptics
import com.za.games.platform.LocalZaSound
import com.za.games.platform.Sfx
import com.za.games.sayi.VergiciState
import com.za.games.ui.common.GameTopBar
import com.za.games.ui.common.OverlayCard
import com.za.games.ui.common.PadButton
import com.za.games.ui.common.ScoreCard

private val TaxColor = Color(0xFFF87171)
private val CoinColor = Color(0xFFFBBF24)

@Composable
fun VergiciScreen(
    highScore: Long,
    onScore: (Long) -> Unit,
    onExit: () -> Unit,
    viewModel: VergiciViewModel = viewModel(),
) {
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val size by viewModel.size.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val target by viewModel.target.collectAsStateWithLifecycle()
    val wins by viewModel.wins.collectAsStateWithLifecycle()
    val best by viewModel.best.collectAsStateWithLifecycle()
    val haptics = LocalZaHaptics.current
    val sound = LocalZaSound.current
    val latestOnScore by rememberUpdatedState(onScore)

    // Rekor = vergiciyi yenme sayısı.
    LaunchedEffect(wins) {
        if (wins > 0) latestOnScore(wins.toLong())
    }
    LaunchedEffect(phase) {
        val s = state ?: return@LaunchedEffect
        if (phase == VergiciPhase.OVER) {
            sound?.play(if (s.player > s.taxman) Sfx.BIG else Sfx.OVER)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    BackHandler {
        if (phase == VergiciPhase.PLAYING) viewModel.toSetup() else onExit()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        GameTopBar(title = stringResource(R.string.game_vergici), onExit = onExit) {
            Text(
                text = "🏆 ${maxOf(wins.toLong(), highScore)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 14.dp),
            )
        }

        val current = state
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ScoreCard(
                label = stringResource(R.string.vergici_you),
                value = (current?.player ?: 0).toString(),
                modifier = Modifier.weight(1f),
                highlight = true,
            )
            ScoreCard(
                label = stringResource(R.string.vergici_taxman),
                value = (current?.taxman ?: 0).toString(),
                modifier = Modifier.weight(1f),
            )
            ScoreCard(
                label = stringResource(R.string.vergici_target),
                value = target?.let { if (it.exact) it.score.toString() else "~${it.score}" } ?: "…",
                modifier = Modifier.weight(1f),
            )
        }

        InfoLine(state = current, selected = selected)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (current != null) {
                NumberBoard(
                    state = current,
                    selected = selected,
                    onTap = { x ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val wasSelected = selected == x
                        viewModel.select(x)
                        if (wasSelected) sound?.play(Sfx.POP, volume = 0.6f)
                    },
                )
            }
            when (phase) {
                VergiciPhase.SETUP -> SetupCard(
                    size = size,
                    wins = wins,
                    best = best,
                    onSize = viewModel::setSize,
                    onStart = viewModel::start,
                    onExit = onExit,
                )
                VergiciPhase.OVER -> current?.let { s ->
                    OverCard(
                        state = s,
                        target = target,
                        onRetry = viewModel::retry,
                        onSetup = viewModel::toSetup,
                        onExit = onExit,
                    )
                }
                VergiciPhase.PLAYING -> Unit
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (phase == VergiciPhase.PLAYING && selected != null) {
                PadButton(
                    label = stringResource(R.string.vergici_take_fmt, selected ?: 0),
                    description = stringResource(R.string.vergici_take_fmt, selected ?: 0),
                    accent = true,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onAction = {
                        sound?.play(Sfx.POP, volume = 0.6f)
                        viewModel.confirm()
                    },
                )
            } else {
                Text(
                    text = if (phase == VergiciPhase.PLAYING) stringResource(R.string.vergici_hint_pick) else " ",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
                )
            }
        }
    }
}

/** Seçim önizlemesi ya da son hamle özeti. */
@Composable
private fun InfoLine(state: VergiciState?, selected: Int?) {
    val text = when {
        state == null -> stringResource(R.string.vergici_rules)
        selected != null -> {
            val taxed = state.divisorsOf(selected)
            stringResource(R.string.vergici_preview_fmt, selected, taxed.joinToString(", "), taxed.sum())
        }
        state.over && state.leftovers.isNotEmpty() ->
            stringResource(R.string.vergici_leftover_fmt, state.leftovers.joinToString(", "))
        state.lastTaken > 0 ->
            stringResource(R.string.vergici_last_fmt, state.lastTaken, state.lastTaxed.joinToString(", "))
        else -> stringResource(R.string.vergici_rules)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        maxLines = 2,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun NumberBoard(state: VergiciState, selected: Int?, onTap: (Int) -> Unit) {
    val n = state.n
    val cols = when {
        n <= 12 -> 4
        n <= 20 -> 5
        n <= 30 -> 6
        else -> 8
    }
    val rows = (n + cols - 1) / cols
    val preview = selected?.let { state.divisorsOf(it) }.orEmpty().toSet()
    val takeable = state.takeable.toSet()
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (r in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (c in 0 until cols) {
                    val x = r * cols + c + 1
                    if (x > n) {
                        Spacer(Modifier.weight(1f))
                        continue
                    }
                    NumberCell(
                        x = x,
                        onBoard = state.remaining[x],
                        takeable = x in takeable,
                        selected = x == selected,
                        taxed = x in preview,
                        onTap = { onTap(x) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberCell(
    x: Int,
    onBoard: Boolean,
    takeable: Boolean,
    selected: Boolean,
    taxed: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val desc = when {
        !onBoard -> stringResource(R.string.vergici_cell_gone_fmt, x)
        taxed -> stringResource(R.string.vergici_cell_taxed_fmt, x)
        takeable -> stringResource(R.string.vergici_cell_takeable_fmt, x)
        else -> stringResource(R.string.vergici_cell_locked_fmt, x)
    }
    val bg = when {
        !onBoard -> Color.Transparent
        selected -> CoinColor.copy(alpha = 0.45f)
        taxed -> TaxColor.copy(alpha = 0.30f)
        takeable -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }
    val border = when {
        selected -> CoinColor
        taxed -> TaxColor
        else -> Color.Transparent
    }
    Surface(
        onClick = onTap,
        enabled = onBoard,
        shape = RoundedCornerShape(12.dp),
        color = bg,
        modifier = modifier
            .aspectRatio(1f)
            .border(2.dp, border, RoundedCornerShape(12.dp))
            .semantics { contentDescription = desc },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (onBoard) {
                Text(
                    text = x.toString(),
                    fontSize = 18.sp,
                    fontWeight = if (takeable || taxed) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        selected -> Color(0xFF1C1917)
                        taxed -> TaxColor
                        takeable -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    },
                )
            }
        }
    }
}

@Composable
private fun SetupCard(
    size: Int,
    wins: Int,
    best: Int,
    onSize: (Int) -> Unit,
    onStart: () -> Unit,
    onExit: () -> Unit,
) {
    OverlayCard {
        Text(
            text = stringResource(R.string.vergici_rules),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        )
        Text(
            text = stringResource(R.string.vergici_size_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VergiciState.SIZES.forEach { n ->
                SizeChip(label = stringResource(R.string.vergici_size_fmt, n), selected = n == size, modifier = Modifier.weight(1f)) { onSize(n) }
            }
        }
        Text(
            text = stringResource(R.string.vergici_wins_fmt, wins) + if (best > 0) " · " + stringResource(R.string.vergici_best_fmt, size, best) else "",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(2.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.vergici_start))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

@Composable
private fun SizeChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.height(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OverCard(
    state: VergiciState,
    target: VergiciTarget?,
    onRetry: () -> Unit,
    onSetup: () -> Unit,
    onExit: () -> Unit,
) {
    OverlayCard {
        Text(
            text = stringResource(
                when {
                    state.player > state.taxman -> R.string.vergici_won
                    state.player < state.taxman -> R.string.vergici_lost
                    else -> R.string.vergici_draw
                },
            ),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.vergici_result_fmt, state.player, state.taxman),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        if (target != null) {
            Text(
                text = stringResource(if (target.exact) R.string.vergici_target_fmt else R.string.vergici_good_fmt, target.score),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            )
        }
        Spacer(Modifier.height(4.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.vergici_again))
        }
        OutlinedButton(onClick = onSetup, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.vergici_to_setup))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}
