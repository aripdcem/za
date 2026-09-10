package com.za.games.ui.reyon

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.za.games.R
import com.za.games.platform.LocalZaHaptics
import com.za.games.platform.LocalZaSound
import com.za.games.platform.Sfx
import com.za.games.platform.ShareContent
import com.za.games.reyon.AuditTap
import com.za.games.reyon.ReyonAuditGenerator
import com.za.games.reyon.ReyonAuditState
import com.za.games.reyon.ReyonLevel
import com.za.games.ui.common.GameTopBar
import com.za.games.ui.common.OverlayCard
import com.za.games.ui.common.ScoreCard
import com.za.games.ui.common.ShareButton
import com.za.games.ui.common.formatTime
import com.za.games.ui.common.modeShareLabel
import kotlinx.coroutines.delay

/** Denetim modu: üstte plan, altta sapmalı gerçek raf; sapmalara dokunulur. */
@Composable
internal fun ReyonAuditContent(
    solved: Long,
    onCompleted: () -> Unit,
    onKind: (ReyonKind) -> Unit,
    onExit: () -> Unit,
    viewModel: ReyonAuditViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val version by viewModel.version.collectAsStateWithLifecycle()
    val generating by viewModel.generating.collectAsStateWithLifecycle()
    val elapsed by viewModel.elapsed.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()
    val missSlot by viewModel.missSlot.collectAsStateWithLifecycle()
    val level by viewModel.level.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val haptics = LocalZaHaptics.current
    val sound = LocalZaSound.current
    val res = LocalContext.current.resources

    var countedSeed by rememberSaveable { mutableLongStateOf(Long.MIN_VALUE) }
    val latestCompleted by rememberUpdatedState(onCompleted)
    LaunchedEffect(result) {
        val seed = state?.audit?.seed ?: return@LaunchedEffect
        if (result != null && seed != countedSeed) {
            countedSeed = seed
            latestCompleted()
            sound?.play(Sfx.BIG)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    LaunchedEffect(missSlot) {
        if (missSlot >= 0) {
            delay(700L)
            viewModel.clearMiss()
        }
    }
    LifecycleResumeEffect(Unit) {
        viewModel.setPaused(false)
        onPauseOrDispose { viewModel.setPaused(true) }
    }
    BackHandler { onExit() }

    val st = state
    @Suppress("UNUSED_VARIABLE")
    val tick = version

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        GameTopBar(title = stringResource(R.string.game_reyon), onExit = onExit) {
            if (st != null) {
                TextButton(onClick = viewModel::toMenu) {
                    Text(stringResource(R.string.reyon_to_menu))
                }
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
                label = stringResource(R.string.reyon_audit_found_label),
                value = st?.let { "${it.foundCount}/${it.audit.deviations.size}" } ?: "—",
                modifier = Modifier.weight(1f),
            )
            ScoreCard(
                label = stringResource(R.string.reyon_audit_mistakes_label),
                value = st?.mistakes?.toString() ?: "—",
                modifier = Modifier.weight(1f),
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (st != null) {
                Column(modifier = Modifier.fillMaxSize()) {
                    SectionLabel(stringResource(R.string.reyon_audit_plan_label))
                    PlanCanvas(state = st)
                    SectionLabel(stringResource(R.string.reyon_audit_shelf_label))
                    AuditCanvas(
                        state = st,
                        version = version,
                        missSlot = missSlot,
                        onTap = { row, col ->
                            when (viewModel.tap(row, col)) {
                                is AuditTap.Found -> {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    sound?.play(Sfx.POP, volume = 0.7f, rate = 1.2f)
                                }
                                AuditTap.Miss -> {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    sound?.play(Sfx.DROP, volume = 0.5f, rate = 0.8f)
                                }
                                AuditTap.Already -> Unit
                            }
                        },
                    )
                    FoundList(state = st, version = version, modifier = Modifier.weight(1f, fill = false))
                }
            }
            when {
                st == null && generating -> OverlayCard {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.reyon_audit_generating))
                }
                st == null -> AuditMenuCard(
                    level = level,
                    mode = mode,
                    records = records,
                    bestTime = viewModel.bestTime(level),
                    onKind = onKind,
                    onLevel = viewModel::setLevel,
                    onMode = viewModel::setMode,
                    onStart = viewModel::newGame,
                    onExit = onExit,
                )
                result != null -> AuditDoneCard(
                    state = st,
                    result = result!!,
                    onRetry = viewModel::retry,
                    onMenu = viewModel::toMenu,
                    onExit = onExit,
                )
            }
        }

        if (st != null && result == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.reyon_audit_remaining_fmt, st.audit.deviations.size - st.foundCount),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically),
                )
                Button(
                    onClick = {
                        if (viewModel.hint() != null) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            sound?.play(Sfx.CLEAR, volume = 0.6f)
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.reyon_hint))
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun PlanCanvas(state: ReyonAuditState) {
    val audit = state.audit
    val res = LocalContext.current.resources
    val textMeasurer = rememberTextMeasurer()
    val cache = remember { HashMap<String, TextLayoutResult>() }
    val labeler = remember(textMeasurer) { BlockLabeler(textMeasurer, cache) }
    val path = remember { Path() }
    val desc = stringResource(R.string.reyon_audit_plan_desc_fmt, audit.rows, audit.cols)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(audit.cols / (audit.rows * 0.62f))
            .clip(RoundedCornerShape(12.dp))
            .background(ReyonPalette.BoardBg)
            .semantics { contentDescription = desc },
    ) {
        val g = ShelfGeom(size.width, size.height, audit.rows, audit.cols)
        drawShelfFrame(g)
        for (it in audit.planItems) drawBlock(g, path, labeler, ReyonText.kind(res, it.product.kind), it.product, it.facings, it.row, it.col)
    }
}

@Composable
private fun AuditCanvas(state: ReyonAuditState, version: Int, missSlot: Int, onTap: (Int, Int) -> Unit) {
    val audit = state.audit
    val rows = audit.rows
    val cols = audit.cols
    val res = LocalContext.current.resources
    val currentTap by rememberUpdatedState(onTap)
    val textMeasurer = rememberTextMeasurer()
    val cache = remember { HashMap<String, TextLayoutResult>() }
    val labeler = remember(textMeasurer) { BlockLabeler(textMeasurer, cache) }
    val path = remember { Path() }
    val desc = stringResource(R.string.reyon_audit_board_desc_fmt, rows, cols, state.foundCount, audit.deviations.size)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(cols / (rows * 0.78f))
            .clip(RoundedCornerShape(12.dp))
            .background(ReyonPalette.BoardBg)
            .semantics { contentDescription = desc }
            .pointerInput(rows, cols) {
                detectTapGestures { pos ->
                    val col = (pos.x / (size.width / cols.toFloat())).toInt().coerceIn(0, cols - 1)
                    val row = (pos.y / (size.height / rows.toFloat())).toInt().coerceIn(0, rows - 1)
                    currentTap(row, col)
                }
            },
    ) {
        @Suppress("UNUSED_VARIABLE")
        val tick = version
        val g = ShelfGeom(size.width, size.height, rows, cols)
        drawShelfFrame(g)
        for (it in audit.items) drawBlock(g, path, labeler, ReyonText.kind(res, it.product.kind), it.product, it.facings, it.row, it.col)
        for ((i, d) in audit.deviations.withIndex()) {
            if (state.isFound(i)) drawMaskRing(g, d.slotMask, ReyonPalette.FoundRing)
        }
        if (missSlot >= 0) drawMaskRing(g, 1 shl missSlot, ReyonPalette.MissRing)
    }
}

@Composable
private fun FoundList(state: ReyonAuditState, version: Int, modifier: Modifier = Modifier) {
    val res = LocalContext.current.resources
    @Suppress("UNUSED_VARIABLE")
    val tick = version
    val audit = state.audit
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        for ((i, d) in audit.deviations.withIndex()) {
            if (!state.isFound(i)) continue
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "✓", color = ReyonPalette.Satisfied, fontWeight = FontWeight.Bold, modifier = Modifier.width(18.dp))
                Text(
                    text = ReyonText.deviation(res, audit, d),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun AuditMenuCard(
    level: ReyonLevel,
    mode: ReyonMode,
    records: Map<ReyonLevel, ReyonStore.AuditRecord>,
    bestTime: Int,
    onKind: (ReyonKind) -> Unit,
    onLevel: (ReyonLevel) -> Unit,
    onMode: (ReyonMode) -> Unit,
    onStart: () -> Unit,
    onExit: () -> Unit,
) {
    OverlayCard {
        Text(
            text = stringResource(R.string.game_reyon),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        KindChips(kind = ReyonKind.AUDIT, onKind = onKind)
        Text(
            text = stringResource(R.string.reyon_audit_intro),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
        ModeAndLevelChips(mode = mode, level = level, onMode = onMode, onLevel = onLevel)
        Text(
            text = stringResource(R.string.reyon_audit_level_desc_fmt, level.rows, level.cols, ReyonAuditGenerator.count(level)),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        val record = records[level]
        val info = when {
            mode == ReyonMode.DAILY && record != null ->
                stringResource(R.string.reyon_audit_daily_done_fmt, formatTime(record.time), record.mistakes)
            mode == ReyonMode.DAILY -> stringResource(R.string.reyon_audit_daily_desc)
            bestTime > 0 -> stringResource(R.string.reyon_best_fmt, formatTime(bestTime))
            else -> stringResource(R.string.reyon_audit_free_desc)
        }
        Text(
            text = info,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (mode == ReyonMode.DAILY && record != null) R.string.reyon_play_again else R.string.reyon_audit_start))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

@Composable
private fun AuditDoneCard(
    state: ReyonAuditState,
    result: AuditResult,
    onRetry: () -> Unit,
    onMenu: () -> Unit,
    onExit: () -> Unit,
) {
    val res = LocalContext.current.resources
    val time = formatTime(result.time)
    val details = stringResource(R.string.reyon_audit_result_fmt, ReyonText.level(res, state.audit.level), result.mistakes, result.hints)
    OverlayCard {
        Text(
            text = stringResource(R.string.reyon_audit_done_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = time,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
        )
        if (result.record) {
            Text(
                text = stringResource(R.string.new_record),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Text(
            text = details,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
        ShareButton(
            ShareContent(
                gameId = "reyon",
                headline = stringResource(R.string.reyon_audit_done_title),
                details = listOf(details, stringResource(R.string.time_fmt, time), modeShareLabel(result.daily, result.day)),
                board = auditPainter(state, res),
            ),
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.same_difficulty))
        }
        OutlinedButton(onClick = onMenu, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.reyon_to_menu))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}
