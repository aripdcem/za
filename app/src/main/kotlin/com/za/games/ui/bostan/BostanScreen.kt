package com.za.games.ui.bostan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.za.games.R
import com.za.games.bostan.BostanDifficulty
import com.za.games.bostan.BostanHud
import com.za.games.bostan.BostanLevel
import com.za.games.bostan.BostanState
import com.za.games.bostan.BostanStatus
import com.za.games.bostan.Defender
import com.za.games.bostan.DefenderKind
import com.za.games.bostan.Enemy
import com.za.games.bostan.EnemyKind
import com.za.games.platform.LocalZaHaptics
import com.za.games.platform.LocalZaSound
import com.za.games.platform.ShareContent
import com.za.games.ui.common.GameTopBar
import com.za.games.ui.common.OverlayCard
import com.za.games.ui.common.ScoreCard
import com.za.games.ui.common.ShareButton
import com.za.games.ui.common.formatScore
import com.za.games.ui.common.modeShareLabel
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.isActive

private val Grass = Color(0xFF4D7C0F)
private val GrassDark = Color(0xFF3F6212)
private val Soil = Color(0xFF7C4A1E)
private val SoilAlt = Color(0xFF8A5A2B)
private val Sprout = Color(0xFF84CC16)
private val Forest = Color(0xFF14532D)
private val Tree = Color(0xFF166534)
private val TreeLight = Color(0xFF22C55E)
private val Fence = Color(0xFFE7C58A)
private val HutWall = Color(0xFFB45309)
private val HutRoof = Color(0xFF7F1D1D)
private val HutDoor = Color(0xFF451A03)
private val Stone = Color(0xFFA1A1AA)
private val StoneDark = Color(0xFF52525B)
private val WaterBlue = Color(0xFF38BDF8)
private val WaterDark = Color(0xFF0369A1)
private val WaterLight = Color(0xFFBAE6FD)
private val Straw = Color(0xFFFACC15)
private val Sack = Color(0xFFF5D0A9)
private val Wood = Color(0xFF92400E)
private val Pipe = Color(0xFF1E3A8A)
private val HiveYellow = Color(0xFFF59E0B)
private val HiveDark = Color(0xFF78350F)
private val Bee = Color(0xFF111827)
private val TrapMetal = Color(0xFF64748B)
private val TrapRed = Color(0xFFEF4444)
private val CrowBody = Color(0xFF111827)
private val Beak = Color(0xFFF97316)
private val RabbitBody = Color(0xFFD1D5DB)
private val RabbitEar = Color(0xFFFBCFE8)
private val GoatBody = Color(0xFFE5E7EB)
private val Horn = Color(0xFF57534E)
private val BoarBody = Color(0xFF44403C)
private val Snout = Color(0xFFF9A8D4)
private val BearBody = Color(0xFF7C2D12)
private val BearMuzzle = Color(0xFFD6B27A)
private val HpBack = Color(0x99000000)
private val HpGood = Color(0xFF4ADE80)
private val HpBad = Color(0xFFF87171)
private val Highlight = Color(0x55FFFFFF)
private val Shadow = Color(0x33000000)

/** Tarla birimi: üstte orman şeridi (−1,3..−0,5), hücreler (−0,5..6,5), altta kulübe şeridi. */
private const val TOP_Y = -1.3f
private const val BOTTOM_Y = BostanState.ROWS + 0.2f
private const val TOTAL_H = BOTTOM_Y - TOP_Y

internal class FieldGeometry(val cs: Float, val ox: Float, val oy: Float) {
    fun px(x: Float): Float = ox + x * cs
    fun py(y: Float): Float = oy + (y - TOP_Y) * cs
}

internal fun fieldGeometry(width: Float, height: Float): FieldGeometry {
    val cs = min(width / BostanState.COLS, height / TOTAL_H)
    return FieldGeometry(cs, (width - cs * BostanState.COLS) / 2f, (height - cs * TOTAL_H) / 2f)
}

/** Hücre merkezinin tuval pikseli (testler de kullanır). */
internal fun bostanCellCenter(width: Float, height: Float, lane: Int, row: Int): Offset {
    val g = fieldGeometry(width, height)
    return Offset(g.px(lane + 0.5f), g.py(row.toFloat()))
}

internal fun bostanCellAt(width: Float, height: Float, x: Float, y: Float): Pair<Int, Int>? {
    val g = fieldGeometry(width, height)
    if (g.cs <= 0f) return null
    val lane = floor((x - g.ox) / g.cs).toInt()
    val row = floor((y - g.oy) / g.cs + TOP_Y + 0.5f).toInt()
    if (lane !in 0 until BostanState.COLS || row !in 0 until BostanState.ROWS) return null
    return lane to row
}

@Composable
private fun defenderName(kind: DefenderKind): String = stringResource(
    when (kind) {
        DefenderKind.KUYU -> R.string.bostan_def_kuyu
        DefenderKind.FISKIYE -> R.string.bostan_def_fiskiye
        DefenderKind.KORKULUK -> R.string.bostan_def_korkuluk
        DefenderKind.KOVAN -> R.string.bostan_def_kovan
        DefenderKind.TUZAK -> R.string.bostan_def_tuzak
    },
)

@Composable
private fun difficultyName(d: BostanDifficulty): String = stringResource(
    when (d) {
        BostanDifficulty.KOLAY -> R.string.difficulty_easy
        BostanDifficulty.ORTA -> R.string.difficulty_medium
        BostanDifficulty.ZOR -> R.string.difficulty_hard
    },
)

@Composable
fun BostanScreen(
    highScore: Long,
    onScore: (Long) -> Unit,
    onExit: () -> Unit,
    viewModel: BostanViewModel = viewModel(),
) {
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val dailyMode by viewModel.dailyMode.collectAsStateWithLifecycle()
    val difficulty by viewModel.difficulty.collectAsStateWithLifecycle()
    val daily by viewModel.daily.collectAsStateWithLifecycle()
    val freeBest by viewModel.freeBest.collectAsStateWithLifecycle()
    val record by viewModel.record.collectAsStateWithLifecycle()
    val hud by viewModel.hud.collectAsStateWithLifecycle()
    val runId by viewModel.runId.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val shovel by viewModel.shovel.collectAsStateWithLifecycle()
    val haptics = LocalZaHaptics.current
    val sound = LocalZaSound.current
    val fx = remember { BostanFx() }
    val fxTick = remember { mutableLongStateOf(0L) }
    val waveFmt = stringResource(R.string.bostan_wave_fmt)
    fx.waveLabel = { String.format(waveFmt, it) }
    fx.bigWaveLabel = stringResource(R.string.bostan_big_wave)
    fx.lifeLabel = stringResource(R.string.bostan_life_lost)
    fx.wonLabel = stringResource(R.string.bostan_won_title)
    fx.lostLabel = stringResource(R.string.bostan_lost_title)
    fx.noWaterLabel = stringResource(R.string.bostan_no_water)
    fx.cooldownLabel = stringResource(R.string.bostan_cooldown)
    fx.occupiedLabel = stringResource(R.string.bostan_occupied)

    val latestScore by rememberUpdatedState(hud.score.toLong())
    val latestOnScore by rememberUpdatedState(onScore)
    DisposableEffect(Unit) {
        onDispose { latestOnScore(latestScore) }
    }
    LaunchedEffect(phase) {
        if (phase == BostanPhase.OVER) latestOnScore(hud.score.toLong())
    }
    LaunchedEffect(runId) { fx.reset() }
    LaunchedEffect(Unit) { viewModel.refresh() }
    LifecycleResumeEffect(Unit) {
        onPauseOrDispose { viewModel.pause() }
    }
    BackHandler {
        if (phase == BostanPhase.PLAYING) viewModel.pause() else onExit()
    }

    LaunchedEffect(phase, runId) {
        if (phase != BostanPhase.PLAYING && phase != BostanPhase.OVER) return@LaunchedEffect
        var last = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = now - last
                    for (event in viewModel.advance(dt)) fx.onEvent(event, sound, haptics)
                    fx.update(dt / 1_000_000_000f)
                    fxTick.longValue += 1
                }
                last = now
            }
            if (phase == BostanPhase.OVER && !fx.isBusy) break
        }
    }

    var previousBest by remember { mutableLongStateOf(highScore) }
    val startRun = {
        previousBest = maxOf(previousBest, hud.score.toLong())
        viewModel.start()
    }
    val restartRun = {
        previousBest = maxOf(previousBest, hud.score.toLong())
        viewModel.restart()
    }
    val attemptsLeft = BostanViewModel.DAILY_ATTEMPTS - (daily?.attempts ?: 0)
    val best = if (dailyMode) daily?.bestOf(difficulty) ?: 0 else freeBest.getOrElse(difficulty.ordinal) { 0 }
    val inRun = phase == BostanPhase.PLAYING || phase == BostanPhase.PAUSED || phase == BostanPhase.OVER

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        GameTopBar(title = stringResource(R.string.game_bostan), onExit = onExit) {
            if (phase == BostanPhase.PLAYING || phase == BostanPhase.PAUSED) {
                TextButton(onClick = viewModel::togglePause) {
                    Text(stringResource(if (phase == BostanPhase.PAUSED) R.string.resume else R.string.pause))
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ScoreCard(label = stringResource(R.string.score), value = formatScore(hud.score.toLong()), modifier = Modifier.weight(1.1f), highlight = true)
            ScoreCard(label = stringResource(R.string.bostan_wave), value = if (hud.totalWaves > 0) "${hud.wave}/${hud.totalWaves}" else "–", modifier = Modifier.weight(0.8f))
            ScoreCard(label = stringResource(R.string.bostan_lives), value = if (inRun && hud.lives > 0) "♥".repeat(hud.lives) else "–", modifier = Modifier.weight(0.8f))
            ScoreCard(label = stringResource(R.string.bostan_best), value = formatScore(maxOf(best, if (inRun) hud.score else 0).toLong()), modifier = Modifier.weight(1f))
        }

        StatusBar(hud = hud, visible = inRun)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            BostanCanvas(viewModel = viewModel, fx = fx, fxTick = fxTick, hud = hud, selected = selected, shovel = shovel, modifier = Modifier.fillMaxSize())
            when (phase) {
                BostanPhase.MENU -> StartCard(
                    dailyMode = dailyMode,
                    difficulty = difficulty,
                    daily = daily,
                    freeBest = freeBest,
                    onMode = viewModel::setDailyMode,
                    onDifficulty = viewModel::setDifficulty,
                    onStart = startRun,
                    onExit = onExit,
                )
                BostanPhase.LOADING -> OverlayCard(scrollable = false) {
                    Text(
                        text = stringResource(R.string.bostan_loading),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
                BostanPhase.PAUSED -> PauseCard(
                    onResume = viewModel::resume,
                    onRestart = restartRun,
                    onMenu = viewModel::toMenu,
                    onExit = onExit,
                )
                BostanPhase.OVER -> OverCard(
                    hud = hud,
                    level = viewModel.state.level,
                    difficulty = difficulty,
                    daily = dailyMode,
                    attemptsLeft = attemptsLeft,
                    isRecord = record || hud.score > previousBest,
                    onRestart = restartRun,
                    onMenu = viewModel::toMenu,
                    onExit = onExit,
                )
                BostanPhase.PLAYING -> Unit
            }
        }

        CardBar(
            hud = hud,
            selected = selected,
            shovel = shovel,
            enabled = phase == BostanPhase.PLAYING,
            onSelect = viewModel::select,
            onShovel = viewModel::selectShovel,
        )
    }
}

/** Su sayacı, dalga doğum çubuğu ve sıradaki dalga geri sayımı. */
@Composable
private fun StatusBar(hud: BostanHud, visible: Boolean) {
    val alpha = if (visible) 1f else 0f
    val trailing = when {
        !visible -> ""
        hud.nextWaveIn > 0f && (hud.wave == 0 || hud.waveProgress >= 1f) -> stringResource(R.string.bostan_next_wave_fmt, ceil(hud.nextWaveIn).toInt())
        hud.enemiesAlive > 0 -> stringResource(R.string.bostan_alive_fmt, hud.enemiesAlive)
        else -> ""
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(24.dp)
            .alpha(alpha),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Canvas(modifier = Modifier.size(14.dp)) { drawDrop(center.x, center.y, size.minDimension * 0.42f, 1f) }
        Text(
            text = hud.water.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val track = MaterialTheme.colorScheme.surfaceVariant
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(CircleShape),
        ) {
            drawRect(track)
            drawRect(HpBad.copy(alpha = 0.9f), size = Size(size.width * hud.waveProgress.coerceIn(0f, 1f), size.height))
        }
        Text(
            text = trailing,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
    }
}

// ---------------------------------------------------------------------------
// Kart çubuğu
// ---------------------------------------------------------------------------

@Composable
private fun CardBar(
    hud: BostanHud,
    selected: DefenderKind?,
    shovel: Boolean,
    enabled: Boolean,
    onSelect: (DefenderKind) -> Unit,
    onShovel: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .alpha(if (enabled) 1f else 0.45f),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (kind in DefenderKind.entries) {
            val name = defenderName(kind)
            ToolCard(
                desc = stringResource(R.string.bostan_card_desc_fmt, name, kind.cost),
                label = kind.cost.toString(),
                selected = selected == kind,
                dim = hud.water < kind.cost,
                cooldown = hud.cooldowns.getOrElse(kind.ordinal) { 0f },
                enabled = enabled,
                onClick = { onSelect(kind) },
                modifier = Modifier.weight(1f),
            ) { drawDefender(kind, center.x, center.y + size.height * 0.05f, size.minDimension * 1.15f, 0f, false, 0f) }
        }
        val shovelName = stringResource(R.string.bostan_shovel)
        ToolCard(
            desc = shovelName,
            label = shovelName,
            selected = shovel,
            dim = false,
            cooldown = 0f,
            enabled = enabled,
            onClick = onShovel,
            modifier = Modifier.weight(1f),
        ) { drawShovel(center.x, center.y, size.minDimension) }
    }
}

@Composable
private fun ToolCard(
    desc: String,
    label: String,
    selected: Boolean,
    dim: Boolean,
    cooldown: Float,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: DrawScope.() -> Unit,
) {
    val border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surfaceVariant,
        border = border,
        modifier = modifier
            .aspectRatio(0.82f)
            .semantics { contentDescription = desc },
    ) {
        val overlay = MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .alpha(if (dim) 0.45f else 1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) { icon() }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (cooldown > 0f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(overlay, size = Size(size.width, size.height * cooldown.coerceIn(0f, 1f)))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Tuval
// ---------------------------------------------------------------------------

@Composable
private fun BostanCanvas(
    viewModel: BostanViewModel,
    fx: BostanFx,
    fxTick: MutableLongState,
    hud: BostanHud,
    selected: DefenderKind?,
    shovel: Boolean,
    modifier: Modifier = Modifier,
) {
    val frame by viewModel.frame.collectAsStateWithLifecycle()
    val desc = stringResource(R.string.bostan_board_desc_fmt, hud.water, hud.wave, hud.totalWaves, hud.enemiesAlive)
    val hint = stringResource(R.string.bostan_tap_hint)
    val textMeasurer = rememberTextMeasurer()
    val textCache = remember { HashMap<String, TextLayoutResult>() }
    val path = remember { Path() }
    val haptics = LocalZaHaptics.current
    val sound = LocalZaSound.current
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .semantics { contentDescription = desc }
            .pointerInput(viewModel) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                    up.consume()
                    val cell = bostanCellAt(size.width.toFloat(), size.height.toFloat(), up.position.x, up.position.y) ?: return@awaitEachGesture
                    val outcome = viewModel.tapCell(cell.first, cell.second)
                    fx.onTap(outcome, cell.first, cell.second, sound, haptics)
                    fxTick.longValue += 1
                }
            },
    ) {
        val tick = frame + fxTick.longValue
        if (tick < 0L) return@Canvas
        drawField(viewModel.state, fx, selected, shovel, frame, path, textMeasurer, textCache, hint)
    }
}

private fun DrawScope.drawField(
    state: BostanState,
    fx: BostanFx,
    selected: DefenderKind?,
    shovel: Boolean,
    frame: Long,
    path: Path,
    textMeasurer: TextMeasurer,
    cache: HashMap<String, TextLayoutResult>,
    hint: String,
) {
    val g = fieldGeometry(size.width, size.height)
    val cs = g.cs
    val cols = BostanState.COLS
    val rows = BostanState.ROWS
    drawRect(Grass)
    translate(fx.shakeX * cs, fx.shakeY * cs) {
        // Orman şeridi ve çit.
        drawRect(Forest, topLeft = Offset(g.px(0f), g.py(TOP_Y)), size = Size(cs * cols, 0.8f * cs))
        for (i in 0 until cols * 2) {
            val tx = g.px(0.25f + i * 0.5f)
            val ty = g.py(TOP_Y + 0.15f + (i % 2) * 0.1f)
            path.reset()
            path.moveTo(tx, ty)
            path.lineTo(tx - 0.22f * cs, ty + 0.55f * cs)
            path.lineTo(tx + 0.22f * cs, ty + 0.55f * cs)
            path.close()
            drawPath(path, if (i % 3 == 0) TreeLight else Tree)
        }
        // Toprak hücreler.
        for (row in 0 until rows) {
            for (lane in 0 until cols) {
                val left = g.px(lane.toFloat())
                val top = g.py(row - 0.5f)
                drawRect(if ((row + lane) % 2 == 0) Soil else SoilAlt, topLeft = Offset(left, top), size = Size(cs, cs))
                val sr = cs * 0.045f
                drawCircle(Sprout, sr, Offset(left + cs * 0.25f, top + cs * 0.3f))
                drawCircle(Sprout, sr, Offset(left + cs * 0.7f, top + cs * 0.55f))
                drawCircle(Sprout, sr * 0.8f, Offset(left + cs * 0.4f, top + cs * 0.8f))
            }
        }
        for (lane in 1 until cols) {
            drawLine(GrassDark.copy(alpha = 0.5f), Offset(g.px(lane.toFloat()), g.py(-0.5f)), Offset(g.px(lane.toFloat()), g.py(rows - 0.5f)), strokeWidth = cs * 0.02f)
        }
        // Çitler.
        for (i in 0..cols * 3) {
            val fxp = g.px(i / 3f)
            drawLine(Fence, Offset(fxp, g.py(-0.5f) - cs * 0.12f), Offset(fxp, g.py(-0.5f) + cs * 0.04f), strokeWidth = cs * 0.05f)
        }
        drawLine(Fence, Offset(g.px(0f), g.py(-0.5f) - cs * 0.05f), Offset(g.px(cols.toFloat()), g.py(-0.5f) - cs * 0.05f), strokeWidth = cs * 0.03f)
        // Kulübe şeridi.
        val hutTop = g.py(rows - 0.5f)
        drawRect(GrassDark, topLeft = Offset(g.px(0f), hutTop), size = Size(cs * cols, cs * 0.06f))
        val hx = g.px(cols / 2f)
        val hy = hutTop + cs * 0.38f
        drawRect(HutWall, topLeft = Offset(hx - cs * 0.42f, hy - cs * 0.12f), size = Size(cs * 0.84f, cs * 0.36f))
        drawRect(HutDoor, topLeft = Offset(hx - cs * 0.1f, hy), size = Size(cs * 0.2f, cs * 0.24f))
        path.reset()
        path.moveTo(hx - cs * 0.5f, hy - cs * 0.1f)
        path.lineTo(hx, hy - cs * 0.36f)
        path.lineTo(hx + cs * 0.5f, hy - cs * 0.1f)
        path.close()
        drawPath(path, HutRoof)
        for (i in 0 until 2) {
            val bx = g.px(if (i == 0) 0.7f else cols - 0.7f)
            drawCircle(Tree, cs * 0.22f, Offset(bx, hutTop + cs * 0.4f))
            drawCircle(TreeLight, cs * 0.12f, Offset(bx - cs * 0.08f, hutTop + cs * 0.32f))
        }

        // Yerleştirme ipucu: seçili kart için uygun hücreler, kürek için dolu hücreler.
        if (selected != null || shovel) {
            for (row in 0 until rows) {
                for (lane in 0 until cols) {
                    val d = state.defenderAt(lane, row)
                    val ok = if (shovel) d != null else d == null && state.canPlace(selected!!, lane, row)
                    if (ok) drawRect(Highlight, topLeft = Offset(g.px(lane.toFloat()) + cs * 0.06f, g.py(row - 0.5f) + cs * 0.06f), size = Size(cs * 0.88f, cs * 0.88f))
                }
            }
        }

        // Savunmalar.
        for (d in state.defenders) {
            val cx = g.px(d.lane + 0.5f)
            val cy = g.py(d.row.toFloat())
            drawDefenderShadow(cx, cy, cs)
            drawDefender(d.kind, cx, cy, cs, d.flash, d.armed, if (d.kind == DefenderKind.TUZAK) (d.timer / BostanState.TRAP_ARM).coerceIn(0f, 1f) else (frame % 40) / 40f)
            if (d.hp < d.kind.hp) drawHp(cx, cy + cs * 0.42f, cs * 0.6f, cs * 0.06f, d.hp / d.kind.hp)
        }
        // Jetler.
        for (j in state.jets) {
            val jx = g.px(j.lane + 0.5f)
            val jy = g.py(j.y)
            drawLine(WaterLight, Offset(jx, jy), Offset(jx, jy + cs * 0.18f), strokeWidth = cs * 0.06f)
            drawCircle(WaterBlue, cs * 0.07f, Offset(jx, jy))
        }
        // Saldırganlar: alttakiler üsttekileri örter.
        for (e in state.enemies.sortedBy { it.y }) {
            val cx = g.px(e.lane + 0.5f)
            val cy = g.py(e.y)
            drawEnemy(e, cx, cy, cs, frame)
            if (e.hp < e.kind.hp) drawHp(cx, cy + cs * 0.5f * enemyScale(e.kind), cs * 0.6f, cs * 0.06f, e.hp / e.kind.hp)
        }
        // Damlalar.
        for (dr in state.drops) {
            val dx = g.px(dr.lane + 0.5f)
            val dy = g.py(dr.row.toFloat())
            val pulse = 1f + 0.08f * sin(frame * 0.2f + dr.id)
            val fade = if (dr.ttl < 1.5f) 0.35f + 0.65f * abs(sin(frame * 0.3f)) else 1f
            drawOval(Shadow, topLeft = Offset(dx - cs * 0.2f, dy + cs * 0.18f), size = Size(cs * 0.4f, cs * 0.12f))
            drawDrop(dx, dy, cs * 0.2f * pulse, fade)
        }
        // Parçacıklar ve uçan yazılar.
        for (p in fx.particles) {
            drawCircle(p.color.copy(alpha = (p.life / p.maxLife).coerceIn(0f, 1f)), p.size * cs, Offset(g.px(p.x), g.py(p.y)))
        }
        for (t in fx.texts) {
            val layout = cache.getOrPut("${t.text}|${t.big}|${t.color.value}") {
                textMeasurer.measure(
                    AnnotatedString(t.text),
                    style = TextStyle(fontSize = if (t.big) 22.sp else 13.sp, fontWeight = FontWeight.Black, color = t.color, textAlign = TextAlign.Center),
                )
            }
            drawText(layout, topLeft = Offset(g.px(t.x) - layout.size.width / 2f, g.py(t.y) - layout.size.height / 2f), alpha = (t.life / t.maxLife).coerceIn(0f, 1f))
        }
        if (state.status == BostanStatus.RUNNING && state.time < 8f && state.defenders.isEmpty() && state.level.waves.isNotEmpty()) {
            val layout = cache.getOrPut("hint|$hint") {
                textMeasurer.measure(
                    AnnotatedString(hint),
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center),
                )
            }
            val alpha = if (state.time < 7f) 1f else (8f - state.time)
            drawText(layout, topLeft = Offset(g.px(cols / 2f) - layout.size.width / 2f, g.py(3f) - layout.size.height / 2f), alpha = alpha.coerceIn(0f, 1f) * 0.9f)
        }
    }
    if (fx.flash > 0f) drawRect(fx.flashColor.copy(alpha = 0.3f * fx.flash))
}

private fun DrawScope.drawHp(cx: Float, y: Float, w: Float, h: Float, frac: Float) {
    drawRoundRect(HpBack, topLeft = Offset(cx - w / 2f, y), size = Size(w, h), cornerRadius = CornerRadius(h / 2f, h / 2f))
    drawRoundRect(if (frac > 0.5f) HpGood else HpBad, topLeft = Offset(cx - w / 2f, y), size = Size(w * frac.coerceIn(0f, 1f), h), cornerRadius = CornerRadius(h / 2f, h / 2f))
}

private fun DrawScope.drawDrop(cx: Float, cy: Float, r: Float, alpha: Float) {
    val p = Path()
    p.moveTo(cx, cy - r * 1.6f)
    p.quadraticBezierTo(cx + r * 1.1f, cy - r * 0.2f, cx + r * 0.95f, cy + r * 0.35f)
    p.quadraticBezierTo(cx + r * 0.6f, cy + r * 1.35f, cx, cy + r * 1.3f)
    p.quadraticBezierTo(cx - r * 0.6f, cy + r * 1.35f, cx - r * 0.95f, cy + r * 0.35f)
    p.quadraticBezierTo(cx - r * 1.1f, cy - r * 0.2f, cx, cy - r * 1.6f)
    p.close()
    drawPath(p, WaterBlue.copy(alpha = alpha))
    drawPath(p, WaterDark.copy(alpha = alpha), style = Stroke(width = r * 0.12f))
    drawCircle(WaterLight.copy(alpha = alpha), r * 0.25f, Offset(cx - r * 0.35f, cy + r * 0.2f))
}

private fun DrawScope.drawDefenderShadow(cx: Float, cy: Float, cs: Float) {
    drawOval(Shadow, topLeft = Offset(cx - cs * 0.3f, cy + cs * 0.25f), size = Size(cs * 0.6f, cs * 0.18f))
}

/**
 * Savunma çizimi (kart simgesi ve tarla ortak): [flash] vuruş parlaması,
 * [armed] tuzak kuruldu mu, [phase] animasyon payı (tuzak için kurulma
 * ilerlemesi).
 */
private fun DrawScope.drawDefender(kind: DefenderKind, cx: Float, cy: Float, cs: Float, flash: Float, armed: Boolean, phase: Float) {
    when (kind) {
        DefenderKind.KUYU -> {
            drawLine(Wood, Offset(cx - cs * 0.26f, cy + cs * 0.05f), Offset(cx - cs * 0.26f, cy - cs * 0.34f), strokeWidth = cs * 0.06f)
            drawLine(Wood, Offset(cx + cs * 0.26f, cy + cs * 0.05f), Offset(cx + cs * 0.26f, cy - cs * 0.34f), strokeWidth = cs * 0.06f)
            val roof = Path()
            roof.moveTo(cx - cs * 0.38f, cy - cs * 0.3f)
            roof.lineTo(cx, cy - cs * 0.5f)
            roof.lineTo(cx + cs * 0.38f, cy - cs * 0.3f)
            roof.close()
            drawPath(roof, HutRoof)
            drawCircle(StoneDark, cs * 0.3f, Offset(cx, cy + cs * 0.08f))
            drawCircle(Stone, cs * 0.3f, Offset(cx, cy + cs * 0.08f), style = Stroke(width = cs * 0.07f))
            drawCircle(WaterDark, cs * 0.18f, Offset(cx, cy + cs * 0.08f))
            drawCircle(WaterLight, cs * 0.05f, Offset(cx - cs * 0.06f, cy + cs * 0.02f))
        }
        DefenderKind.FISKIYE -> {
            drawRoundRect(StoneDark, topLeft = Offset(cx - cs * 0.22f, cy + cs * 0.12f), size = Size(cs * 0.44f, cs * 0.2f), cornerRadius = CornerRadius(cs * 0.05f, cs * 0.05f))
            drawRect(Pipe, topLeft = Offset(cx - cs * 0.07f, cy - cs * 0.25f), size = Size(cs * 0.14f, cs * 0.4f))
            drawCircle(WaterBlue, cs * 0.15f, Offset(cx, cy - cs * 0.28f))
            drawCircle(WaterLight, cs * 0.05f, Offset(cx - cs * 0.05f, cy - cs * 0.33f))
            for (i in 0 until 3) {
                val a = -1.57f + (i - 1) * 0.45f
                val d = cs * (0.3f + 0.12f * ((phase + i * 0.33f) % 1f))
                drawCircle(WaterLight.copy(alpha = 0.8f), cs * 0.035f, Offset(cx + cos(a) * d, cy - cs * 0.28f + sin(a) * d))
            }
        }
        DefenderKind.KORKULUK -> {
            drawLine(Wood, Offset(cx, cy + cs * 0.42f), Offset(cx, cy - cs * 0.2f), strokeWidth = cs * 0.07f)
            drawLine(Wood, Offset(cx - cs * 0.34f, cy - cs * 0.02f), Offset(cx + cs * 0.34f, cy - cs * 0.02f), strokeWidth = cs * 0.06f)
            drawRoundRect(Straw, topLeft = Offset(cx - cs * 0.2f, cy - cs * 0.08f), size = Size(cs * 0.4f, cs * 0.26f), cornerRadius = CornerRadius(cs * 0.06f, cs * 0.06f))
            drawCircle(Sack, cs * 0.15f, Offset(cx, cy - cs * 0.28f))
            drawCircle(CrowBody, cs * 0.025f, Offset(cx - cs * 0.05f, cy - cs * 0.3f))
            drawCircle(CrowBody, cs * 0.025f, Offset(cx + cs * 0.05f, cy - cs * 0.3f))
            drawRect(Straw, topLeft = Offset(cx - cs * 0.24f, cy - cs * 0.42f), size = Size(cs * 0.48f, cs * 0.05f))
            drawRect(Straw, topLeft = Offset(cx - cs * 0.13f, cy - cs * 0.52f), size = Size(cs * 0.26f, cs * 0.12f))
        }
        DefenderKind.KOVAN -> {
            drawRoundRect(HiveYellow, topLeft = Offset(cx - cs * 0.26f, cy - cs * 0.22f), size = Size(cs * 0.52f, cs * 0.48f), cornerRadius = CornerRadius(cs * 0.1f, cs * 0.1f))
            drawRect(HiveDark, topLeft = Offset(cx - cs * 0.26f, cy - cs * 0.06f), size = Size(cs * 0.52f, cs * 0.05f))
            drawRect(HiveDark, topLeft = Offset(cx - cs * 0.26f, cy + cs * 0.1f), size = Size(cs * 0.52f, cs * 0.05f))
            drawCircle(HiveDark, cs * 0.05f, Offset(cx, cy + cs * 0.2f))
            drawRect(HiveDark, topLeft = Offset(cx - cs * 0.3f, cy - cs * 0.3f), size = Size(cs * 0.6f, cs * 0.09f))
            for (i in 0 until 3) {
                val a = phase * 6.283f + i * 2.1f
                drawCircle(Bee, cs * 0.04f, Offset(cx + cos(a) * cs * 0.36f, cy - cs * 0.3f + sin(a) * cs * 0.14f))
            }
        }
        DefenderKind.TUZAK -> {
            drawCircle(TrapMetal, cs * 0.3f, Offset(cx, cy), style = Stroke(width = cs * 0.08f))
            for (i in 0 until 8) {
                val a = i * 0.785f
                drawLine(TrapMetal, Offset(cx + cos(a) * cs * 0.22f, cy + sin(a) * cs * 0.22f), Offset(cx + cos(a) * cs * 0.12f, cy + sin(a) * cs * 0.12f), strokeWidth = cs * 0.05f)
            }
            if (armed) {
                drawCircle(TrapRed, cs * 0.1f, Offset(cx, cy))
            } else {
                drawCircle(StoneDark, cs * 0.1f, Offset(cx, cy))
                drawArc(TrapRed, -90f, 360f * phase, useCenter = false, topLeft = Offset(cx - cs * 0.3f, cy - cs * 0.3f), size = Size(cs * 0.6f, cs * 0.6f), style = Stroke(width = cs * 0.05f))
            }
        }
    }
    if (flash > 0f) drawCircle(Color.White.copy(alpha = 0.5f * (flash / BostanState.FLASH).coerceIn(0f, 1f)), cs * 0.4f, Offset(cx, cy))
}

private fun DrawScope.drawShovel(cx: Float, cy: Float, cs: Float) {
    drawLine(Wood, Offset(cx - cs * 0.22f, cy - cs * 0.3f), Offset(cx + cs * 0.1f, cy + cs * 0.1f), strokeWidth = cs * 0.09f)
    drawRoundRect(Wood, topLeft = Offset(cx - cs * 0.34f, cy - cs * 0.42f), size = Size(cs * 0.22f, cs * 0.1f), cornerRadius = CornerRadius(cs * 0.05f, cs * 0.05f))
    val blade = Path()
    blade.moveTo(cx + cs * 0.02f, cy + cs * 0.02f)
    blade.lineTo(cx + cs * 0.34f, cy + cs * 0.12f)
    blade.lineTo(cx + cs * 0.2f, cy + cs * 0.4f)
    blade.lineTo(cx - cs * 0.06f, cy + cs * 0.24f)
    blade.close()
    drawPath(blade, StoneDark)
    drawPath(blade, Stone, style = Stroke(width = cs * 0.04f))
}

private fun enemyScale(kind: EnemyKind): Float = when (kind) {
    EnemyKind.KARGA -> 0.8f
    EnemyKind.TAVSAN -> 0.7f
    EnemyKind.KECI -> 0.9f
    EnemyKind.DOMUZ -> 1f
    EnemyKind.AYI -> 1.2f
}

/** Kuşbakışı saldırgan: baş önde (+y), yürürken hafif salınım, kemirirken öne atılma. */
private fun DrawScope.drawEnemy(e: Enemy, cx0: Float, cy0: Float, cs: Float, frame: Long) {
    val k = cs * enemyScale(e.kind)
    val wob = if (e.biting) 0f else sin(frame * 0.25f + e.id) * k * 0.04f
    val lunge = if (e.biting) abs(sin(frame * 0.35f + e.id)) * k * 0.06f else 0f
    val cx = cx0 + wob
    val cy = cy0 + lunge
    drawOval(Shadow, topLeft = Offset(cx - k * 0.3f, cy + k * 0.2f), size = Size(k * 0.6f, k * 0.2f))
    when (e.kind) {
        EnemyKind.KARGA -> {
            val flap = sin(frame * 0.5f + e.id) * k * 0.08f
            val wing = Path()
            wing.moveTo(cx - k * 0.08f, cy - k * 0.05f)
            wing.lineTo(cx - k * 0.46f, cy - k * 0.3f + flap)
            wing.lineTo(cx - k * 0.3f, cy + k * 0.1f + flap)
            wing.close()
            drawPath(wing, CrowBody)
            wing.reset()
            wing.moveTo(cx + k * 0.08f, cy - k * 0.05f)
            wing.lineTo(cx + k * 0.46f, cy - k * 0.3f + flap)
            wing.lineTo(cx + k * 0.3f, cy + k * 0.1f + flap)
            wing.close()
            drawPath(wing, CrowBody)
            drawOval(CrowBody, topLeft = Offset(cx - k * 0.14f, cy - k * 0.3f), size = Size(k * 0.28f, k * 0.5f))
            drawCircle(CrowBody, k * 0.12f, Offset(cx, cy + k * 0.2f))
            val beak = Path()
            beak.moveTo(cx - k * 0.05f, cy + k * 0.26f)
            beak.lineTo(cx + k * 0.05f, cy + k * 0.26f)
            beak.lineTo(cx, cy + k * 0.42f)
            beak.close()
            drawPath(beak, Beak)
            drawCircle(Color.White, k * 0.03f, Offset(cx - k * 0.05f, cy + k * 0.17f))
            drawCircle(Color.White, k * 0.03f, Offset(cx + k * 0.05f, cy + k * 0.17f))
        }
        EnemyKind.TAVSAN -> {
            drawOval(RabbitEar, topLeft = Offset(cx - k * 0.2f, cy - k * 0.5f), size = Size(k * 0.12f, k * 0.34f))
            drawOval(RabbitEar, topLeft = Offset(cx + k * 0.08f, cy - k * 0.5f), size = Size(k * 0.12f, k * 0.34f))
            drawOval(RabbitBody, topLeft = Offset(cx - k * 0.22f, cy - k * 0.24f), size = Size(k * 0.44f, k * 0.5f))
            drawCircle(RabbitBody, k * 0.17f, Offset(cx, cy + k * 0.25f))
            drawCircle(Color.White, k * 0.07f, Offset(cx, cy - k * 0.22f))
            drawCircle(CrowBody, k * 0.03f, Offset(cx - k * 0.07f, cy + k * 0.28f))
            drawCircle(CrowBody, k * 0.03f, Offset(cx + k * 0.07f, cy + k * 0.28f))
            drawCircle(Snout, k * 0.035f, Offset(cx, cy + k * 0.38f))
        }
        EnemyKind.KECI -> {
            drawOval(GoatBody, topLeft = Offset(cx - k * 0.24f, cy - k * 0.3f), size = Size(k * 0.48f, k * 0.56f))
            drawCircle(GoatBody, k * 0.18f, Offset(cx, cy + k * 0.3f))
            drawArc(Horn, 200f, 130f, useCenter = false, topLeft = Offset(cx - k * 0.34f, cy + k * 0.02f), size = Size(k * 0.3f, k * 0.36f), style = Stroke(width = k * 0.06f))
            drawArc(Horn, 210f, 130f, useCenter = false, topLeft = Offset(cx + k * 0.04f, cy + k * 0.02f), size = Size(k * 0.3f, k * 0.36f), style = Stroke(width = k * 0.06f))
            drawCircle(CrowBody, k * 0.03f, Offset(cx - k * 0.07f, cy + k * 0.3f))
            drawCircle(CrowBody, k * 0.03f, Offset(cx + k * 0.07f, cy + k * 0.3f))
            drawLine(Horn, Offset(cx, cy + k * 0.42f), Offset(cx, cy + k * 0.54f), strokeWidth = k * 0.05f)
        }
        EnemyKind.DOMUZ -> {
            drawOval(BoarBody, topLeft = Offset(cx - k * 0.3f, cy - k * 0.34f), size = Size(k * 0.6f, k * 0.66f))
            drawLine(Horn, Offset(cx, cy - k * 0.3f), Offset(cx, cy + k * 0.2f), strokeWidth = k * 0.06f)
            drawCircle(BoarBody, k * 0.22f, Offset(cx, cy + k * 0.3f))
            drawCircle(BoarBody, k * 0.08f, Offset(cx - k * 0.2f, cy + k * 0.2f))
            drawCircle(BoarBody, k * 0.08f, Offset(cx + k * 0.2f, cy + k * 0.2f))
            drawOval(Snout, topLeft = Offset(cx - k * 0.12f, cy + k * 0.36f), size = Size(k * 0.24f, k * 0.16f))
            drawLine(Color.White, Offset(cx - k * 0.16f, cy + k * 0.42f), Offset(cx - k * 0.24f, cy + k * 0.56f), strokeWidth = k * 0.05f)
            drawLine(Color.White, Offset(cx + k * 0.16f, cy + k * 0.42f), Offset(cx + k * 0.24f, cy + k * 0.56f), strokeWidth = k * 0.05f)
            drawCircle(TrapRed, k * 0.03f, Offset(cx - k * 0.09f, cy + k * 0.26f))
            drawCircle(TrapRed, k * 0.03f, Offset(cx + k * 0.09f, cy + k * 0.26f))
        }
        EnemyKind.AYI -> {
            drawCircle(BearBody, k * 0.4f, Offset(cx, cy - k * 0.02f))
            drawCircle(BearBody, k * 0.12f, Offset(cx - k * 0.24f, cy + k * 0.14f))
            drawCircle(BearBody, k * 0.12f, Offset(cx + k * 0.24f, cy + k * 0.14f))
            drawCircle(BearBody, k * 0.26f, Offset(cx, cy + k * 0.3f))
            drawCircle(BearMuzzle, k * 0.12f, Offset(cx, cy + k * 0.4f))
            drawCircle(CrowBody, k * 0.05f, Offset(cx, cy + k * 0.46f))
            drawCircle(CrowBody, k * 0.035f, Offset(cx - k * 0.1f, cy + k * 0.26f))
            drawCircle(CrowBody, k * 0.035f, Offset(cx + k * 0.1f, cy + k * 0.26f))
        }
    }
    if (e.flash > 0f) drawCircle(Color.White.copy(alpha = 0.55f * (e.flash / BostanState.FLASH).coerceIn(0f, 1f)), k * 0.42f, Offset(cx, cy))
}

// ---------------------------------------------------------------------------
// Kartlar
// ---------------------------------------------------------------------------

@Composable
private fun StartCard(
    dailyMode: Boolean,
    difficulty: BostanDifficulty,
    daily: BostanDaily?,
    freeBest: List<Int>,
    onMode: (Boolean) -> Unit,
    onDifficulty: (BostanDifficulty) -> Unit,
    onStart: () -> Unit,
    onExit: () -> Unit,
) {
    val exhausted = dailyMode && daily != null && daily.attempts >= BostanViewModel.DAILY_ATTEMPTS
    OverlayCard {
        Text(
            text = stringResource(R.string.game_bostan),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.bostan_intro),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
        Text(
            text = stringResource(R.string.bostan_rules),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeChip(stringResource(R.string.mode_daily), dailyMode, Modifier.weight(1f)) { onMode(true) }
            ModeChip(stringResource(R.string.mode_free), !dailyMode, Modifier.weight(1f)) { onMode(false) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (d in BostanDifficulty.entries) {
                ModeChip(difficultyName(d), difficulty == d, Modifier.weight(1f)) { onDifficulty(d) }
            }
        }
        val best = if (dailyMode) daily?.bestOf(difficulty) ?: 0 else freeBest.getOrElse(difficulty.ordinal) { 0 }
        if (dailyMode) {
            if (daily != null) {
                Text(
                    text = stringResource(R.string.bostan_daily_status_fmt, daily.attempts, BostanViewModel.DAILY_ATTEMPTS, best),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = stringResource(if (exhausted) R.string.bostan_daily_exhausted else R.string.bostan_daily_desc),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        } else if (best > 0) {
            Text(
                text = stringResource(R.string.bostan_best_fmt, difficultyName(difficulty), best),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(4.dp))
        if (exhausted) {
            Button(onClick = { onMode(false) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.play_free))
            }
        } else {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.bostan_start))
            }
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

@Composable
private fun PauseCard(onResume: () -> Unit, onRestart: () -> Unit, onMenu: () -> Unit, onExit: () -> Unit) {
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
        TextButton(onClick = onMenu, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.bostan_to_menu))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

@Composable
private fun OverCard(
    hud: BostanHud,
    level: BostanLevel,
    difficulty: BostanDifficulty,
    daily: Boolean,
    attemptsLeft: Int,
    isRecord: Boolean,
    onRestart: () -> Unit,
    onMenu: () -> Unit,
    onExit: () -> Unit,
) {
    val won = hud.status == BostanStatus.WON
    val result = stringResource(R.string.bostan_result_fmt, hud.wave, hud.totalWaves, hud.kills, hud.lives)
    val expert = stringResource(R.string.bostan_expert_fmt, level.expertLives, level.expertScore)
    val diff = difficultyName(difficulty)
    OverlayCard {
        Text(
            text = stringResource(if (won) R.string.bostan_won_title else R.string.bostan_lost_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        if (won) {
            Text(
                text = "★".repeat(hud.lives.coerceIn(0, BostanState.LIVES)) + "☆".repeat((BostanState.LIVES - hud.lives).coerceIn(0, BostanState.LIVES)),
                style = MaterialTheme.typography.headlineMedium,
                color = Straw,
            )
        }
        Text(
            text = formatScore(hud.score.toLong()),
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
        Text(
            text = "$diff · $result",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
        Text(
            text = expert,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        if (daily && attemptsLeft <= 0) {
            Text(
                text = stringResource(R.string.bostan_daily_exhausted),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        ShareButton(
            ShareContent(
                gameId = "bostan",
                headline = if (won) stringResource(R.string.bostan_share_won_fmt, hud.score) else stringResource(R.string.bostan_share_lost_fmt, hud.wave, hud.score),
                details = listOf("$diff · $result", modeShareLabel(daily, null)),
            ),
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text(
                when {
                    daily && attemptsLeft > 0 -> stringResource(R.string.bostan_retry_fmt, attemptsLeft)
                    daily -> stringResource(R.string.play_free)
                    else -> stringResource(R.string.restart)
                },
            )
        }
        TextButton(onClick = onMenu, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.bostan_to_menu))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
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
            maxLines = 1,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
