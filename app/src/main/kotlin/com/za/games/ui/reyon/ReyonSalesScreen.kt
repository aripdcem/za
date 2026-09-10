package com.za.games.ui.reyon

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
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
import com.za.games.reyon.ReyonLevel
import com.za.games.reyon.ReyonSalesState
import com.za.games.reyon.SalesRule
import com.za.games.reyon.SalesScore
import com.za.games.ui.common.GameTopBar
import com.za.games.ui.common.OverlayCard
import com.za.games.ui.common.ScoreCard
import com.za.games.ui.common.ShareButton
import com.za.games.ui.common.formatScore
import com.za.games.ui.common.modeShareLabel

internal fun starsText(stars: Int): String = "★".repeat(stars) + "☆".repeat(3 - stars)

/** Satış modu: serbest diziliş, canlı puan, hedefe göre yıldız. */
@Composable
internal fun ReyonSalesContent(
    solved: Long,
    onCompleted: () -> Unit,
    onKind: (ReyonKind) -> Unit,
    onExit: () -> Unit,
    viewModel: ReyonSalesViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val version by viewModel.version.collectAsStateWithLifecycle()
    val generating by viewModel.generating.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()
    val review by viewModel.review.collectAsStateWithLifecycle()
    val level by viewModel.level.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val haptics = LocalZaHaptics.current
    val sound = LocalZaSound.current
    val res = LocalContext.current.resources

    var countedSeed by rememberSaveable { mutableLongStateOf(Long.MIN_VALUE) }
    val latestCompleted by rememberUpdatedState(onCompleted)
    LaunchedEffect(result) {
        val seed = state?.sales?.seed ?: return@LaunchedEffect
        if (result != null && seed != countedSeed) {
            countedSeed = seed
            latestCompleted()
            sound?.play(Sfx.BIG)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    LifecycleResumeEffect(Unit) {
        onPauseOrDispose { viewModel.persistNow() }
    }
    BackHandler { onExit() }

    val st = state
    @Suppress("UNUSED_VARIABLE")
    val tick = version
    val score = remember(st, version) { st?.score() }

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
                label = stringResource(R.string.reyon_sales_score_label),
                value = score?.total?.let { formatScore(it.toLong()) } ?: "—",
                modifier = Modifier.weight(1f),
                highlight = true,
            )
            ScoreCard(
                label = stringResource(R.string.reyon_sales_target_label),
                value = st?.sales?.target?.let { formatScore(it.toLong()) } ?: "—",
                modifier = Modifier.weight(1f),
            )
            ScoreCard(
                label = stringResource(R.string.solved_label),
                value = solved.toString(),
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
            if (st != null && score != null) {
                val showTarget = result != null && review == SalesReview.TARGET
                Column(modifier = Modifier.fillMaxSize()) {
                    SalesCanvas(
                        state = st,
                        score = score,
                        version = version,
                        selected = selected,
                        showTarget = showTarget,
                        onTap = { row, col ->
                            if (viewModel.tapSlot(row, col)) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                sound?.play(Sfx.POP, volume = 0.5f)
                            } else if (selected >= 0) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        onLongPress = { row, col ->
                            val occupant = st.occupant(row, col)
                            if (occupant >= 0 && !st.finished) {
                                viewModel.select(occupant)
                                if (viewModel.removeSelected()) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    sound?.play(Sfx.DROP, volume = 0.4f, rate = 1.4f)
                                }
                            }
                        },
                    )
                    if (result != null && review != SalesReview.RESULT) {
                        ReviewBar(review = review, target = st.sales.target, onReview = viewModel::setReview)
                    } else {
                        BreakdownLine(state = st, score = score, selected = selected)
                    }
                    RulesPanel(score = score, target = st.sales.target, modifier = Modifier.weight(1f, fill = false))
                    if (!st.finished) {
                        SalesTray(state = st, version = version, selected = selected) { id ->
                            viewModel.select(id)
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                }
            }
            when {
                st == null && generating -> OverlayCard {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.reyon_sales_generating))
                }
                st == null -> SalesMenuCard(
                    level = level,
                    mode = mode,
                    records = records,
                    bestPercent = viewModel.bestPercent(level),
                    onKind = onKind,
                    onLevel = viewModel::setLevel,
                    onMode = viewModel::setMode,
                    onStart = viewModel::newGame,
                    onExit = onExit,
                )
                result != null && review == SalesReview.RESULT -> SalesResultCard(
                    state = st,
                    result = result!!,
                    onReopen = viewModel::reopen,
                    onReview = viewModel::setReview,
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
                OutlinedButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.undo()
                    },
                    enabled = st.canUndo,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.undo))
                }
                OutlinedButton(
                    onClick = { if (viewModel.removeSelected()) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                    enabled = selected >= 0 && st.isPlaced(selected),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.reyon_remove))
                }
                Button(
                    onClick = {
                        if (viewModel.finish() != null) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    enabled = st.isComplete,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.reyon_sales_finish))
                }
            }
        }
    }
}

@Composable
private fun SalesCanvas(
    state: ReyonSalesState,
    score: SalesScore,
    version: Int,
    selected: Int,
    showTarget: Boolean,
    onTap: (Int, Int) -> Unit,
    onLongPress: (Int, Int) -> Unit,
) {
    val sales = state.sales
    val rows = sales.rows
    val cols = sales.cols
    val res = LocalContext.current.resources
    val currentTap by rememberUpdatedState(onTap)
    val currentLong by rememberUpdatedState(onLongPress)
    val textMeasurer = rememberTextMeasurer()
    val cache = remember { HashMap<String, TextLayoutResult>() }
    val labeler = remember(textMeasurer) { BlockLabeler(textMeasurer, cache) }
    val path = remember { Path() }
    val unplaced = sales.products.size - state.placedCount
    val desc = stringResource(R.string.reyon_sales_board_desc_fmt, rows, cols, unplaced, score.total)
    val targetScore = remember(sales) { com.za.games.reyon.SalesScorer.score(sales.board, sales.products, targetOf(sales)) }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(cols / (rows * 0.78f))
            .clip(RoundedCornerShape(12.dp))
            .background(ReyonPalette.BoardBg)
            .semantics { contentDescription = desc }
            .pointerInput(rows, cols) {
                detectTapGestures(
                    onTap = { pos ->
                        val col = (pos.x / (size.width / cols.toFloat())).toInt().coerceIn(0, cols - 1)
                        val row = (pos.y / (size.height / rows.toFloat())).toInt().coerceIn(0, rows - 1)
                        currentTap(row, col)
                    },
                    onLongPress = { pos ->
                        val col = (pos.x / (size.width / cols.toFloat())).toInt().coerceIn(0, cols - 1)
                        val row = (pos.y / (size.height / rows.toFloat())).toInt().coerceIn(0, rows - 1)
                        currentLong(row, col)
                    },
                )
            },
    ) {
        @Suppress("UNUSED_VARIABLE")
        val tick = version
        val g = ShelfGeom(size.width, size.height, rows, cols)
        drawShelfFrame(g)
        val at = if (showTarget) targetOf(sales) else null
        val contributions = if (showTarget) targetScore.byProduct else score.byProduct
        for (p in sales.products) {
            val row: Int
            val col: Int
            if (at != null) {
                row = sales.board.rowOf(at[p.id])
                col = sales.board.colOf(at[p.id])
            } else {
                val pl = state.placement(p.id) ?: continue
                row = pl.row
                col = pl.col
            }
            val ring = if (!showTarget && p.id == selected) ReyonPalette.SelectedRing else null
            drawBlock(g, path, labeler, ReyonText.kind(res, p.kind), p, p.facings, row, col, ring = ring, dim = showTarget)
            val badge = labeler.label("%+d".format(contributions[p.id]), g.cw, 9f, ReyonPalette.BlockText)
            val x = g.x(col) + g.w(p.facings) - badge.size.width - g.pad * 1.5f
            val y = g.y(row) + g.pad * 0.5f
            drawRoundRect(Color(0x66FFFFFF), topLeft = Offset(x - 2.dp.toPx(), y), size = Size(badge.size.width + 4.dp.toPx(), badge.size.height.toFloat()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx()))
            drawText(badge, topLeft = Offset(x, y))
        }
    }
}

/** Hedef dizilişin ürün→göz dizisi (iyileştiricinin bulduğu). */
private fun targetOf(sales: com.za.games.reyon.ReyonSales): IntArray {
    val st = ReyonSalesState(sales)
    st.applyTarget()
    return st.snapshot()
}

@Composable
private fun BreakdownLine(state: ReyonSalesState, score: SalesScore, selected: Int) {
    val res = LocalContext.current.resources
    val text = if (selected >= 0 && state.isPlaced(selected)) {
        val p = state.sales.products[selected]
        val single = com.za.games.reyon.SalesScorer.score(state.sales.board, state.sales.products, state.snapshot())
        val parts = ArrayList<String>()
        parts += stringResource(R.string.reyon_sales_part_fmt, stringResource(R.string.reyon_sales_rule_position), com.za.games.reyon.SalesRules.position(p, state.placement(selected)!!.row, state.sales.rows))
        val total = single.byProduct[selected]
        val rest = total - com.za.games.reyon.SalesRules.position(p, state.placement(selected)!!.row, state.sales.rows)
        if (rest != 0) parts += stringResource(R.string.reyon_sales_part_fmt, stringResource(R.string.reyon_sales_neighbors), rest)
        stringResource(R.string.reyon_sales_breakdown_fmt, ReyonText.kind(res, p.kind), parts.joinToString(" · "), total)
    } else if (selected >= 0) {
        val p = state.sales.products[selected]
        stringResource(R.string.reyon_sales_selected_fmt, ReyonText.kind(res, p.kind), p.demand)
    } else {
        stringResource(R.string.reyon_sales_pick_hint)
    }
    @Suppress("UNUSED_VARIABLE")
    val unused = score.total
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    )
}

@Composable
private fun ReviewBar(review: SalesReview, target: Int, onReview: (SalesReview) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Chip(stringResource(R.string.reyon_sales_review_mine), review == SalesReview.MINE, Modifier.weight(1f)) { onReview(SalesReview.MINE) }
        Chip(stringResource(R.string.reyon_sales_review_target_fmt, target), review == SalesReview.TARGET, Modifier.weight(1.2f)) { onReview(SalesReview.TARGET) }
        Chip(stringResource(R.string.reyon_sales_back_to_result), false, Modifier.weight(1f)) { onReview(SalesReview.RESULT) }
    }
}

@Composable
private fun RulesPanel(score: SalesScore, target: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.reyon_sales_rules_label),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
            )
            val fill = MaterialTheme.colorScheme.primary
            val track = MaterialTheme.colorScheme.surfaceVariant
            val fraction = if (target > 0) (score.total.toFloat() / target).coerceIn(0f, 1f) else 0f
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(CircleShape),
            ) {
                drawRect(track)
                drawRect(fill, size = Size(size.width * fraction, size.height))
            }
        }
        for (rule in SalesRule.entries) {
            val (name, desc) = when (rule) {
                SalesRule.POSITION -> R.string.reyon_sales_rule_position to R.string.reyon_sales_rule_position_desc
                SalesRule.COMPLEMENT -> R.string.reyon_sales_rule_complement to R.string.reyon_sales_rule_complement_desc
                SalesRule.CONFLICT -> R.string.reyon_sales_rule_conflict to R.string.reyon_sales_rule_conflict_desc
                SalesRule.CATEGORY -> R.string.reyon_sales_rule_category to R.string.reyon_sales_rule_category_desc
                SalesRule.BRAND -> R.string.reyon_sales_rule_brand to R.string.reyon_sales_rule_brand_desc
            }
            val value = score.of(rule)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(name), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = stringResource(desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                Text(
                    text = "%+d".format(value),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        value > 0 -> ReyonPalette.Satisfied
                        value < 0 -> ReyonPalette.Violated
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SalesTray(state: ReyonSalesState, version: Int, selected: Int, onSelect: (Int) -> Unit) {
    val res = LocalContext.current.resources
    @Suppress("UNUSED_VARIABLE")
    val tick = version
    val trayLabel = stringResource(R.string.reyon_tray_label)
    val pending = state.sales.products.filter { !state.isPlaced(it.id) }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Text(
            text = if (pending.isEmpty()) stringResource(R.string.reyon_tray_empty) else trayLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (p in pending) {
                val name = ReyonText.kind(res, p.kind)
                val desc = "$trayLabel: " + stringResource(R.string.reyon_tray_item_fmt, name, p.facings)
                val isSelected = p.id == selected
                Surface(
                    onClick = { onSelect(p.id) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(2.dp, if (isSelected) ReyonPalette.SelectedRing else Color.Transparent),
                    modifier = Modifier.semantics { contentDescription = desc },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(brandColor(p.brand)))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = buildString {
                                append("×${p.facings} ")
                                append(stringResource(R.string.reyon_sales_demand_fmt, p.demand))
                                if (p.premium) append(" ★")
                                if (p.heavy) append(" ▼")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SalesMenuCard(
    level: ReyonLevel,
    mode: ReyonMode,
    records: Map<ReyonLevel, ReyonStore.SalesRecord>,
    bestPercent: Int,
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
        KindChips(kind = ReyonKind.SALES, onKind = onKind)
        Text(
            text = stringResource(R.string.reyon_sales_intro),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
        ModeAndLevelChips(mode = mode, level = level, onMode = onMode, onLevel = onLevel)
        Text(
            text = stringResource(R.string.reyon_level_desc_fmt, level.rows, level.cols, level.products.first, level.products.last),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        val record = records[level]
        val info = when {
            mode == ReyonMode.DAILY && record != null -> {
                val stars = if (record.target > 0) when {
                    record.score >= record.target -> 3
                    record.score >= record.target * 0.9 -> 2
                    record.score >= record.target * 0.75 -> 1
                    else -> 0
                } else 0
                stringResource(R.string.reyon_sales_daily_done_fmt, record.score, record.target, starsText(stars))
            }
            mode == ReyonMode.DAILY -> stringResource(R.string.reyon_sales_daily_desc)
            bestPercent > 0 -> stringResource(R.string.reyon_sales_best_fmt, bestPercent)
            else -> stringResource(R.string.reyon_sales_free_desc)
        }
        Text(
            text = info,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (mode == ReyonMode.DAILY && record != null) R.string.reyon_play_again else R.string.reyon_sales_start))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

@Composable
private fun SalesResultCard(
    state: ReyonSalesState,
    result: SalesResult,
    onReopen: () -> Unit,
    onReview: (SalesReview) -> Unit,
    onRetry: () -> Unit,
    onMenu: () -> Unit,
    onExit: () -> Unit,
) {
    val res = LocalContext.current.resources
    val details = stringResource(R.string.reyon_sales_result_fmt, result.score, result.target, result.percent)
    val levelName = ReyonText.level(res, state.sales.level)
    OverlayCard {
        Text(
            text = stringResource(R.string.reyon_sales_done_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = starsText(result.stars),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
        )
        if (result.score > result.target) {
            Text(
                text = stringResource(R.string.reyon_sales_beat_target),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        } else if (result.record) {
            Text(
                text = stringResource(R.string.new_record),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Text(
            text = "$levelName · $details",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
        ShareButton(
            ShareContent(
                gameId = "reyon",
                headline = stringResource(R.string.share_score_fmt, formatScore(result.score.toLong())),
                details = listOf(levelName, details, starsText(result.stars), modeShareLabel(result.daily, result.day)),
                board = salesPainter(state, res),
            ),
        )
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onReopen, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.reyon_sales_reopen))
            }
            OutlinedButton(onClick = { onReview(SalesReview.TARGET) }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.reyon_sales_show_target))
            }
        }
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.same_difficulty))
        }
        TextButton(onClick = onMenu, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.reyon_to_menu))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}
