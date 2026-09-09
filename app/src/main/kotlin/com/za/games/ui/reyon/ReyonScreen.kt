package com.za.games.ui.reyon

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.za.games.R
import com.za.games.platform.LocalZaHaptics
import com.za.games.platform.LocalZaSound
import com.za.games.platform.Sfx
import com.za.games.platform.ShareContent
import com.za.games.reyon.Brand
import com.za.games.reyon.ClueStatus
import com.za.games.reyon.Product
import com.za.games.reyon.ReyonHint
import com.za.games.reyon.ReyonLevel
import com.za.games.reyon.ReyonState
import com.za.games.reyon.Rules
import com.za.games.ui.common.GameTopBar
import com.za.games.ui.common.OverlayCard
import com.za.games.ui.common.ScoreCard
import com.za.games.ui.common.ShareButton
import com.za.games.ui.common.formatTime
import com.za.games.ui.common.modeShareLabel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val BoardBg = Color(0xFF0F1628)
private val Plank = Color(0xFF475569)
private val PlankEdge = Color(0xFF64748B)
private val SlotLine = Color(0x22FFFFFF)
private val BlockText = Color(0xFF0F172A)
private val Lock = Color(0x99000000)
private val SelectedRing = Color(0xFFF8FAFC)
private val HighlightRing = Color(0xFF4DE1FF)
private val ViolatedRing = Color(0xFFF87171)
private val HintRing = Color(0xFFFDE68A)
private val Satisfied = Color(0xFF4ADE80)
private val Violated = Color(0xFFF87171)

internal fun brandColor(brand: Brand): Color = Color(brandArgb(brand))

@Composable
fun ReyonScreen(
    highScore: Long,
    onScore: (Long) -> Unit,
    onExit: () -> Unit,
    viewModel: ReyonViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val version by viewModel.version.collectAsStateWithLifecycle()
    val generating by viewModel.generating.collectAsStateWithLifecycle()
    val elapsed by viewModel.elapsed.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val highlightClue by viewModel.highlightClue.collectAsStateWithLifecycle()
    val lastHint by viewModel.lastHint.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()
    val level by viewModel.level.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val haptics = LocalZaHaptics.current
    val sound = LocalZaSound.current
    val res = LocalContext.current.resources

    // Rekor = çözülen bulmaca sayısı (Kakuro ile aynı kural).
    val baseline = remember { highScore }
    var solvedSession by remember { mutableIntStateOf(0) }
    // Aynı çözüm ekran yeniden kurulunca (döndürme) ikinci kez sayılmasın.
    var countedSeed by rememberSaveable { mutableLongStateOf(Long.MIN_VALUE) }
    val latestOnScore by rememberUpdatedState(onScore)
    LaunchedEffect(result) {
        val seed = state?.puzzle?.seed ?: return@LaunchedEffect
        if (result != null && seed != countedSeed) {
            countedSeed = seed
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
                label = stringResource(R.string.difficulty_label),
                value = st?.let { ReyonText.level(res, it.puzzle.level) } ?: "—",
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
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (st != null) {
                val highlighted = remember(st, highlightClue, version) {
                    if (highlightClue in st.puzzle.brief.indices) Rules.involved(st.puzzle.brief[highlightClue], st.puzzle.products).toSet() else emptySet()
                }
                val violated = remember(st, version) {
                    st.puzzle.brief.filter { st.status(it) == ClueStatus.VIOLATED }
                        .flatMap { Rules.involved(it, st.puzzle.products).toList() }.toSet()
                }
                val hinted = (lastHint as? ReyonHint.Place)?.product ?: -1
                val wrongHint = (lastHint as? ReyonHint.Wrong)?.product ?: -1
                Column(modifier = Modifier.fillMaxSize()) {
                    ShelfCanvas(
                        state = st,
                        version = version,
                        selected = selected,
                        highlighted = highlighted,
                        violated = violated,
                        hinted = hinted,
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
                            if (occupant >= 0 && !st.isLocked(occupant)) {
                                viewModel.select(occupant)
                                if (viewModel.removeSelected()) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    sound?.play(Sfx.DROP, volume = 0.4f, rate = 1.4f)
                                }
                            }
                        },
                    )
                    HintLine(state = st, hint = lastHint)
                    Brief(
                        state = st,
                        version = version,
                        highlightClue = highlightClue,
                        onToggle = viewModel::toggleHighlight,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Tray(
                        state = st,
                        version = version,
                        selected = selected,
                        highlighted = highlighted,
                        wrongHint = wrongHint,
                        onSelect = { id ->
                            viewModel.select(id)
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                    )
                }
            }
            when {
                st == null && generating -> OverlayCard {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.reyon_generating))
                }
                st == null -> MenuCard(
                    level = level,
                    mode = mode,
                    records = records,
                    bestTime = viewModel.bestTime(level),
                    onLevel = viewModel::setLevel,
                    onMode = viewModel::setMode,
                    onStart = viewModel::newGame,
                    onExit = onExit,
                )
                result != null -> SolvedCard(
                    state = st,
                    result = result!!,
                    onRetry = viewModel::retry,
                    onMenu = viewModel::toMenu,
                    onExit = onExit,
                )
            }
        }

        if (st != null && result == null) {
            Controls(
                canUndo = st.canUndo,
                canRemove = selected >= 0 && st.isPlaced(selected),
                onUndo = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.undo()
                },
                onRemove = {
                    if (viewModel.removeSelected()) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                onHint = {
                    val h = viewModel.hint()
                    if (h != null) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        sound?.play(Sfx.CLEAR, volume = 0.6f)
                    }
                },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Raf
// ---------------------------------------------------------------------------

@Composable
private fun ShelfCanvas(
    state: ReyonState,
    version: Int,
    selected: Int,
    highlighted: Set<Int>,
    violated: Set<Int>,
    hinted: Int,
    onTap: (Int, Int) -> Unit,
    onLongPress: (Int, Int) -> Unit,
) {
    val puzzle = state.puzzle
    val rows = puzzle.rows
    val cols = puzzle.cols
    val res = LocalContext.current.resources
    val currentTap by rememberUpdatedState(onTap)
    val currentLong by rememberUpdatedState(onLongPress)
    val textMeasurer = rememberTextMeasurer()
    val cache = remember { HashMap<String, TextLayoutResult>() }
    val path = remember { Path() }
    val unplaced = puzzle.products.size - state.placedCount
    val desc = stringResource(R.string.reyon_board_desc_fmt, rows, cols, unplaced)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(cols / (rows * 0.78f))
            .clip(RoundedCornerShape(12.dp))
            .background(BoardBg)
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
        val cw = size.width / cols
        val sh = size.height / rows
        val plank = sh * 0.12f
        val pad = cw * 0.05f

        // Raflar ve göz çizgileri.
        for (r in 0 until rows) {
            val y = (r + 1) * sh - plank
            drawRoundRect(Plank, topLeft = Offset(0f, y), size = Size(size.width, plank), cornerRadius = CornerRadius(plank * 0.3f, plank * 0.3f))
            drawRect(PlankEdge, topLeft = Offset(0f, y), size = Size(size.width, plank * 0.25f))
            for (c in 1 until cols) {
                drawLine(SlotLine, Offset(c * cw, r * sh + pad), Offset(c * cw, y - pad), strokeWidth = 1.dp.toPx())
            }
        }

        fun label(text: String, maxWidth: Float, sizeSp: Float, color: Color): TextLayoutResult =
            cache.getOrPut("$text|${maxWidth.toInt()}|${sizeSp.toInt()}|${color.value}") {
                textMeasurer.measure(
                    AnnotatedString(text),
                    style = TextStyle(fontSize = sizeSp.sp, fontWeight = FontWeight.Bold, color = color, textAlign = TextAlign.Center),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                    constraints = Constraints(maxWidth = maxWidth.toInt().coerceAtLeast(1)),
                )
            }

        for (p in puzzle.products) {
            val pl = state.placement(p.id) ?: continue
            val x = pl.col * cw + pad
            val y = pl.row * sh + pad
            val w = p.facings * cw - 2 * pad
            val h = sh - plank - 2 * pad
            val locked = state.isLocked(p.id)
            val fill = brandColor(p.brand).let { if (locked) it.copy(alpha = 0.8f) else it }
            drawRoundRect(fill, topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(cw * 0.12f, cw * 0.12f))
            val nameSp = (h * 0.22f / density).coerceIn(9f, 14f)
            val layout = label(ReyonText.kind(res, p.kind), w - cw * 0.16f, nameSp, BlockText)
            drawText(layout, topLeft = Offset(x + (w - layout.size.width) / 2f, y + h * 0.38f - layout.size.height / 2f))
            drawMarks(path, p, x, y, w, h)
            if (locked) drawLock(x + w - h * 0.22f, y + h * 0.16f, h * 0.12f)
            val ring = when {
                p.id == selected -> SelectedRing
                p.id == hinted -> HintRing
                p.id in violated -> ViolatedRing
                p.id in highlighted -> HighlightRing
                else -> null
            }
            if (ring != null) {
                drawRoundRect(ring, topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(cw * 0.12f, cw * 0.12f), style = Stroke(width = 3.dp.toPx()))
            }
        }
    }
}

/** Boy noktaları, ★ (yüksek marj) ve ağırlık işareti; bloğun alt şeridinde. */
private fun DrawScope.drawMarks(path: Path, p: Product, x: Float, y: Float, w: Float, h: Float) {
    val r = h * 0.055f
    val cy = y + h * 0.78f
    var cx = x + w * 0.5f - (p.size - 1) * r * 1.6f
    if (p.premium) cx -= r * 2.2f
    if (p.heavy) cx -= r * 2.2f
    repeat(p.size) {
        drawCircle(BlockText, radius = r, center = Offset(cx, cy))
        cx += r * 3.2f
    }
    if (p.premium) {
        cx += r * 1.2f
        drawStar(path, cx, cy, r * 2.2f)
        cx += r * 4.4f
    }
    if (p.heavy) {
        cx += r * 0.6f
        drawRoundRect(BlockText, topLeft = Offset(cx - r * 1.8f, cy - r * 0.6f), size = Size(r * 3.6f, r * 2.2f), cornerRadius = CornerRadius(r * 0.6f, r * 0.6f))
        drawRect(BlockText, topLeft = Offset(cx - r * 0.7f, cy - r * 1.7f), size = Size(r * 1.4f, r * 1.2f))
    }
}

private fun DrawScope.drawStar(path: Path, cx: Float, cy: Float, r: Float) {
    path.reset()
    for (i in 0 until 10) {
        val a = -PI.toFloat() / 2f + i * PI.toFloat() / 5f
        val rr = if (i % 2 == 0) r else r * 0.45f
        val px = cx + rr * cos(a)
        val py = cy + rr * sin(a)
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, BlockText)
}

private fun DrawScope.drawLock(cx: Float, cy: Float, r: Float) {
    drawRoundRect(Lock, topLeft = Offset(cx - r, cy), size = Size(2 * r, r * 1.6f), cornerRadius = CornerRadius(r * 0.3f, r * 0.3f))
    drawCircle(Lock, radius = r * 0.7f, center = Offset(cx, cy), style = Stroke(width = r * 0.35f))
}

// ---------------------------------------------------------------------------
// Brif, tepsi, ipucu satırı, kontroller
// ---------------------------------------------------------------------------

@Composable
private fun Brief(
    state: ReyonState,
    version: Int,
    highlightClue: Int,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val res = LocalContext.current.resources
    @Suppress("UNUSED_VARIABLE")
    val tick = version
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(R.string.reyon_brief_label),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
        )
        state.puzzle.brief.forEachIndexed { index, clue ->
            val status = state.status(clue)
            val (glyph, color) = when (status) {
                ClueStatus.PENDING -> "○" to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                ClueStatus.SATISFIED -> "✓" to Satisfied
                ClueStatus.VIOLATED -> "✗" to Violated
            }
            val text = ReyonText.clue(res, state.puzzle, clue)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (index == highlightClue) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { onToggle(index) }
                    .padding(horizontal = 6.dp, vertical = 3.dp)
                    .semantics { contentDescription = "$glyph $text" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = glyph, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.width(18.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status == ClueStatus.VIOLATED) Violated else MaterialTheme.colorScheme.onSurface.copy(alpha = if (status == ClueStatus.SATISFIED) 0.6f else 0.9f),
                )
            }
        }
    }
}

@Composable
private fun HintLine(state: ReyonState, hint: ReyonHint?) {
    if (hint == null) return
    val res = LocalContext.current.resources
    val text = when (hint) {
        is ReyonHint.Wrong -> stringResource(R.string.reyon_hint_wrong_fmt, ReyonText.kind(res, state.puzzle.products[hint.product].kind))
        is ReyonHint.Place -> stringResource(
            R.string.reyon_hint_place_fmt,
            ReyonText.kind(res, state.puzzle.products[hint.product].kind),
            ReyonText.shelfAt(res, hint.row, state.puzzle.rows),
        )
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = HintRing,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Tray(
    state: ReyonState,
    version: Int,
    selected: Int,
    highlighted: Set<Int>,
    wrongHint: Int,
    onSelect: (Int) -> Unit,
) {
    val res = LocalContext.current.resources
    @Suppress("UNUSED_VARIABLE")
    val tick = version
    val trayLabel = stringResource(R.string.reyon_tray_label)
    val pending = state.puzzle.products.filter { !state.isPlaced(it.id) }
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
                val ring = when {
                    isSelected -> SelectedRing
                    p.id == wrongHint -> HintRing
                    p.id in highlighted -> HighlightRing
                    else -> Color.Transparent
                }
                Surface(
                    onClick = { onSelect(p.id) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(2.dp, ring),
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
                                repeat(p.size) { append('•') }
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
private fun Controls(canUndo: Boolean, canRemove: Boolean, onUndo: () -> Unit, onRemove: () -> Unit, onHint: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.undo))
        }
        OutlinedButton(onClick = onRemove, enabled = canRemove, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.reyon_remove))
        }
        Button(onClick = onHint, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.reyon_hint))
        }
    }
}

// ---------------------------------------------------------------------------
// Kartlar
// ---------------------------------------------------------------------------

@Composable
private fun Chip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun MenuCard(
    level: ReyonLevel,
    mode: ReyonMode,
    records: Map<ReyonLevel, ReyonStore.Record>,
    bestTime: Int,
    onLevel: (ReyonLevel) -> Unit,
    onMode: (ReyonMode) -> Unit,
    onStart: () -> Unit,
    onExit: () -> Unit,
) {
    val res = LocalContext.current.resources
    OverlayCard {
        Text(
            text = stringResource(R.string.game_reyon),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.reyon_intro),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(stringResource(R.string.mode_daily), mode == ReyonMode.DAILY, Modifier.weight(1f)) { onMode(ReyonMode.DAILY) }
            Chip(stringResource(R.string.mode_free), mode == ReyonMode.FREE, Modifier.weight(1f)) { onMode(ReyonMode.FREE) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (l in ReyonLevel.entries) {
                Chip(ReyonText.level(res, l), level == l, Modifier.weight(1f)) { onLevel(l) }
            }
        }
        Text(
            text = stringResource(R.string.reyon_level_desc_fmt, level.rows, level.cols, level.products.first, level.products.last),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        val record = records[level]
        val info = when {
            mode == ReyonMode.DAILY && record != null ->
                stringResource(R.string.reyon_daily_done_fmt, formatTime(record.time), record.hints)
            mode == ReyonMode.DAILY -> stringResource(R.string.reyon_daily_desc)
            bestTime > 0 -> stringResource(R.string.reyon_best_fmt, formatTime(bestTime))
            else -> stringResource(R.string.reyon_free_desc)
        }
        Text(
            text = info,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (mode == ReyonMode.DAILY && record != null) R.string.reyon_play_again else R.string.reyon_start))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

@Composable
private fun SolvedCard(
    state: ReyonState,
    result: ReyonResult,
    onRetry: () -> Unit,
    onMenu: () -> Unit,
    onExit: () -> Unit,
) {
    val res = LocalContext.current.resources
    val time = formatTime(result.time)
    val details = stringResource(R.string.reyon_result_fmt, ReyonText.level(res, state.puzzle.level), result.hints)
    OverlayCard {
        Text(
            text = stringResource(R.string.congrats),
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
                headline = stringResource(R.string.share_solved),
                details = listOf(details, stringResource(R.string.time_fmt, time), modeShareLabel(result.daily, result.day)),
                board = reyonPainter(state, res),
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

