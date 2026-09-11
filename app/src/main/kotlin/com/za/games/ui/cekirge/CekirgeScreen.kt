package com.za.games.ui.cekirge

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
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
import com.za.games.cekirge.Bug
import com.za.games.cekirge.BugKind
import com.za.games.cekirge.CekirgeHud
import com.za.games.cekirge.CekirgeStatus
import com.za.games.cekirge.CekirgeWorld
import com.za.games.platform.LocalZaHaptics
import com.za.games.platform.LocalZaSound
import com.za.games.platform.ShareContent
import com.za.games.ui.common.GameTopBar
import com.za.games.ui.common.OverlayCard
import com.za.games.ui.common.ScoreCard
import com.za.games.ui.common.ShareButton
import com.za.games.ui.common.formatScore
import com.za.games.ui.common.modeShareLabel
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.isActive

private val SkyTop = Color(0xFF7DD3FC)
private val SkyBottom = Color(0xFFE0F2FE)
private val Cloud = Color(0xAAFFFFFF)
private val Hedge = Color(0xFF3F6212)
private val Wheat = Color(0xFFE9B949)
private val WheatDark = Color(0xFFC98F2B)
private val Straw = Color(0xFFFACC15)
private val StrawDark = Color(0xFFCA8A04)
private val BugKara = Color(0xFF1F2937)
private val BugYesil = Color(0xFF65A30D)
private val BugKahve = Color(0xFFA16207)
private val BugEye = Color(0xFFF8FAFC)
private val QueenBody = Color(0xFFF59E0B)
private val QueenWing = Color(0x88FFFFFF)
private val SpitColor = Color(0xFF4ADE80)
private val SpitDark = Color(0xFF166534)
private val ShotColor = Color(0xFFBAE6FD)
private val ShotCore = Color(0xFFFFFFFF)
private val Overalls = Color(0xFF1D4ED8)
private val Skin = Color(0xFFFCD9B6)
private val Hat = Color(0xFFFDE68A)
private val HatBand = Color(0xFF92400E)
private val Tank = Color(0xFF64748B)
private val TankDark = Color(0xFF334155)
private val Pill = Color(0x8C000000)
private val Shadow = Color(0x33000000)

/** Sürükleme katsayısı: parmak tarlanın genişliğini bir başparmak hamlesinde geçsin. */
private const val DRAG_GAIN = 1.2f

/** Kıpırdamadan bu süre içinde kalkan parmak "dokunuş" sayılır ve sıkar. */
private const val TAP_MS = 300L

private fun arenaScale(width: Float, height: Float): Float = min(width / CekirgeWorld.WIDTH, height / CekirgeWorld.HEIGHT)

@Composable
fun CekirgeScreen(
    highScore: Long,
    onScore: (Long) -> Unit,
    onExit: () -> Unit,
    viewModel: CekirgeViewModel = viewModel(),
) {
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val dailyMode by viewModel.dailyMode.collectAsStateWithLifecycle()
    val daily by viewModel.daily.collectAsStateWithLifecycle()
    val freeBest by viewModel.freeBest.collectAsStateWithLifecycle()
    val freeWave by viewModel.freeWave.collectAsStateWithLifecycle()
    val record by viewModel.record.collectAsStateWithLifecycle()
    val hud by viewModel.hud.collectAsStateWithLifecycle()
    val runId by viewModel.runId.collectAsStateWithLifecycle()
    val haptics = LocalZaHaptics.current
    val sound = LocalZaSound.current
    val fx = remember { CekirgeFx() }
    val fxTick = remember { mutableLongStateOf(0L) }
    val waveFmt = stringResource(R.string.cekirge_wave_fmt)
    fx.waveLabel = { String.format(waveFmt, it) }
    fx.clearedLabel = stringResource(R.string.cekirge_cleared)
    fx.lifeLabel = stringResource(R.string.cekirge_life_lost)
    fx.invadedLabel = stringResource(R.string.cekirge_over_invaded)
    fx.lostLabel = stringResource(R.string.cekirge_over_lives)

    val latestScore by rememberUpdatedState(hud.score.toLong())
    val latestOnScore by rememberUpdatedState(onScore)
    DisposableEffect(Unit) {
        onDispose { latestOnScore(latestScore) }
    }
    LaunchedEffect(phase) {
        if (phase == CekirgePhase.OVER) latestOnScore(hud.score.toLong())
    }
    LaunchedEffect(runId) { fx.reset() }
    LaunchedEffect(Unit) { viewModel.refresh() }
    LifecycleResumeEffect(Unit) {
        onPauseOrDispose { viewModel.pause() }
    }
    BackHandler {
        if (phase == CekirgePhase.PLAYING) viewModel.pause() else onExit()
    }

    LaunchedEffect(phase, runId) {
        if (phase != CekirgePhase.PLAYING && phase != CekirgePhase.OVER) return@LaunchedEffect
        var last = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = now - last
                    val world = viewModel.world
                    for (event in viewModel.advance(dt)) fx.onEvent(event, world, sound, haptics)
                    fx.update(dt / 1_000_000_000f)
                    fxTick.longValue += 1
                }
                last = now
            }
            if (phase == CekirgePhase.OVER && !fx.isBusy) break
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
    val attemptsLeft = CekirgeViewModel.DAILY_ATTEMPTS - (daily?.attempts ?: 0)
    val best = if (dailyMode) daily?.best ?: 0 else freeBest
    val inRun = phase != CekirgePhase.MENU

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        GameTopBar(title = stringResource(R.string.game_cekirge), onExit = onExit) {
            if (phase == CekirgePhase.PLAYING || phase == CekirgePhase.PAUSED) {
                TextButton(onClick = viewModel::togglePause) {
                    Text(stringResource(if (phase == CekirgePhase.PAUSED) R.string.resume else R.string.pause))
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
            ScoreCard(label = stringResource(R.string.cekirge_wave), value = if (inRun) hud.wave.toString() else "–", modifier = Modifier.weight(0.7f))
            ScoreCard(label = stringResource(R.string.cekirge_lives), value = if (inRun && hud.lives > 0) "♥".repeat(hud.lives) else "–", modifier = Modifier.weight(0.8f))
            ScoreCard(label = stringResource(R.string.cekirge_best), value = formatScore(maxOf(best, if (inRun) hud.score else 0).toLong()), modifier = Modifier.weight(1f))
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            CekirgeCanvas(viewModel = viewModel, fx = fx, fxTick = fxTick, hud = hud, modifier = Modifier.fillMaxSize())
            when (phase) {
                CekirgePhase.MENU -> StartCard(
                    dailyMode = dailyMode,
                    daily = daily,
                    freeBest = freeBest,
                    freeWave = freeWave,
                    onMode = viewModel::setDailyMode,
                    onStart = startRun,
                    onExit = onExit,
                )
                CekirgePhase.PAUSED -> PauseCard(
                    onResume = viewModel::resume,
                    onRestart = restartRun,
                    onMenu = viewModel::toMenu,
                    onExit = onExit,
                )
                CekirgePhase.OVER -> OverCard(
                    hud = hud,
                    daily = dailyMode,
                    attemptsLeft = attemptsLeft,
                    isRecord = record || hud.score > previousBest,
                    onRestart = restartRun,
                    onMenu = viewModel::toMenu,
                    onExit = onExit,
                )
                CekirgePhase.PLAYING -> Unit
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Tuval
// ---------------------------------------------------------------------------

@Composable
private fun CekirgeCanvas(
    viewModel: CekirgeViewModel,
    fx: CekirgeFx,
    fxTick: MutableLongState,
    hud: CekirgeHud,
    modifier: Modifier = Modifier,
) {
    val frame by viewModel.frame.collectAsStateWithLifecycle()
    val desc = stringResource(R.string.cekirge_board_desc_fmt, hud.score, hud.alive, hud.lives)
    val hint = stringResource(R.string.cekirge_hint)
    val textMeasurer = rememberTextMeasurer()
    val textCache = remember { HashMap<String, TextLayoutResult>() }
    val path = remember { Path() }
    val sky = remember { Brush.verticalGradient(0f to SkyTop, 1f to SkyBottom) }
    val sound = LocalZaSound.current
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .semantics { contentDescription = desc }
            .pointerInput(viewModel) {
                // Her parmak ayrı izlenir: kayan parmak çiftçiyi sürükler (ilk
                // pikselden itibaren, tolerans yutulmaz), kıpırdamadan kalkan
                // parmak sıkar. İki başparmakla oynanabilir: biri yürütür, öteki sıkar.
                awaitEachGesture {
                    val first = awaitFirstDown(requireUnconsumed = false)
                    val slop = viewConfiguration.touchSlop
                    val starts = HashMap<PointerId, Offset>()
                    val downAt = HashMap<PointerId, Long>()
                    val lastX = HashMap<PointerId, Float>()
                    val moved = HashSet<PointerId>()
                    starts[first.id] = first.position
                    downAt[first.id] = first.uptimeMillis
                    lastX[first.id] = first.position.x
                    first.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val scale = arenaScale(size.width.toFloat(), size.height.toFloat())
                        for (c in event.changes) {
                            if (c.changedToDownIgnoreConsumed()) {
                                starts[c.id] = c.position
                                downAt[c.id] = c.uptimeMillis
                                lastX[c.id] = c.position.x
                            }
                            val start = starts[c.id] ?: continue
                            if (c.pressed) {
                                val dx = c.position.x - (lastX[c.id] ?: c.position.x)
                                lastX[c.id] = c.position.x
                                if (c.id !in moved && (c.position - start).getDistance() > slop) moved += c.id
                                if (c.id in moved && dx != 0f && scale > 0f) viewModel.drag(dx * DRAG_GAIN / scale)
                            } else if (c.changedToUpIgnoreConsumed()) {
                                if (c.id !in moved && c.uptimeMillis - (downAt[c.id] ?: c.uptimeMillis) <= TAP_MS) {
                                    if (!viewModel.fire()) fx.onFireBlocked(sound)
                                    fxTick.longValue += 1
                                }
                                starts.remove(c.id)
                                downAt.remove(c.id)
                                lastX.remove(c.id)
                                moved.remove(c.id)
                            }
                            c.consume()
                        }
                        if (event.changes.none { it.pressed }) break
                    }
                }
            },
    ) {
        val tick = frame + fxTick.longValue
        if (tick < 0L) return@Canvas
        drawFarm(viewModel.world, fx, frame, path, sky, textMeasurer, textCache, hint)
    }
}

private fun DrawScope.drawFarm(
    world: CekirgeWorld,
    fx: CekirgeFx,
    frame: Long,
    path: Path,
    sky: Brush,
    textMeasurer: TextMeasurer,
    cache: HashMap<String, TextLayoutResult>,
    hint: String,
) {
    val w = size.width
    val h = size.height
    val s = arenaScale(w, h)
    val aw = CekirgeWorld.WIDTH * s
    val ah = CekirgeWorld.HEIGHT * s
    val ox = (w - aw) / 2f
    val oy = (h - ah) / 2f
    drawRect(SkyBottom)
    translate(ox + fx.shakeX * s, oy + fx.shakeY * s) {
        drawRect(sky, size = Size(aw, ah))
        // Bulutlar ve çit; buğday tarlası.
        for (i in 0 until 3) {
            val cx = (0.15f + i * 0.35f) * s + sin(frame * 0.004f + i) * 0.02f * s
            val cy = (0.05f + (i % 2) * 0.06f) * s
            drawOval(Cloud, topLeft = Offset(cx - 0.09f * s, cy - 0.02f * s), size = Size(0.18f * s, 0.04f * s))
            drawOval(Cloud, topLeft = Offset(cx - 0.04f * s, cy - 0.04f * s), size = Size(0.1f * s, 0.05f * s))
        }
        val ground = 1.2f * s
        drawRect(Wheat, topLeft = Offset(0f, ground), size = Size(aw, ah - ground))
        drawRect(Hedge, topLeft = Offset(0f, ground - 0.012f * s), size = Size(aw, 0.012f * s))
        var wx = 0.02f * s
        while (wx < aw) {
            drawLine(WheatDark, Offset(wx, ah), Offset(wx + 0.01f * s, ground + 0.03f * s), strokeWidth = 0.006f * s)
            wx += 0.045f * s
        }
        // Balyalar.
        for (b in world.bales) {
            val cw = b.cellW * s
            val ch = b.cellH * s
            for (cy in 0 until CekirgeWorld.BALE_CY) {
                for (cx in 0 until CekirgeWorld.BALE_CX) {
                    if (!b.cells[cy][cx]) continue
                    val x = (b.left + cx * b.cellW) * s
                    val y = (b.top + cy * b.cellH) * s
                    drawRect(Straw, topLeft = Offset(x, y), size = Size(cw + 0.5f, ch + 0.5f))
                    drawLine(StrawDark, Offset(x + cw * 0.2f, y + ch * 0.7f), Offset(x + cw * 0.8f, y + ch * 0.4f), strokeWidth = ch * 0.12f)
                }
            }
        }
        // Sürü.
        val stride = world.swarmStride % 2 == 0
        for (b in world.bugs) {
            if (!b.alive) continue
            drawBug(b, world.bugX(b) * s, world.bugY(b) * s, s, stride)
        }
        // Kraliçe.
        val q = world.queen
        if (q != null) drawQueen(q.x * s, CekirgeWorld.QUEEN_Y * s, s, q.dir, frame)
        // Tükürükler ve fıskırtma.
        for (sp in world.spits) {
            val x = sp.x * s
            val y = sp.y * s
            drawLine(SpitColor.copy(alpha = 0.5f), Offset(x, y - 0.03f * s), Offset(x, y), strokeWidth = 0.012f * s, cap = StrokeCap.Round)
            drawCircle(SpitDark, CekirgeWorld.SPIT_HALF * s * 1.1f, Offset(x, y))
            drawCircle(SpitColor, CekirgeWorld.SPIT_HALF * s * 0.8f, Offset(x, y))
        }
        val shot = world.shot
        if (shot != null) {
            val x = shot.x * s
            val y = shot.y * s
            drawLine(ShotColor, Offset(x, y + 0.05f * s), Offset(x, y - 0.015f * s), strokeWidth = 0.012f * s, cap = StrokeCap.Round)
            drawCircle(ShotCore, 0.008f * s, Offset(x, y - 0.01f * s))
        }
        // Çiftçi.
        val blink = world.invuln > 0f && (frame / 4) % 2 == 0L
        if (!blink) drawFarmer(world.farmerX * s, CekirgeWorld.FARMER_Y * s, s, world.shot == null && world.status == CekirgeStatus.RUNNING, frame)
        // Parçacıklar ve yazılar.
        for (p in fx.particles) {
            drawCircle(p.color.copy(alpha = (p.life / p.maxLife).coerceIn(0f, 1f)), p.size * s, Offset(p.x * s, p.y * s))
        }
        for (t in fx.texts) {
            val layout = cache.getOrPut("${t.text}|${t.big}|${t.color.value}") {
                textMeasurer.measure(
                    AnnotatedString(t.text),
                    style = TextStyle(fontSize = if (t.big) 22.sp else 13.sp, fontWeight = FontWeight.Black, color = t.color, textAlign = TextAlign.Center),
                )
            }
            val a = (t.life / t.maxLife).coerceIn(0f, 1f)
            val tx = t.x * s - layout.size.width / 2f
            val ty = t.y * s - layout.size.height / 2f
            if (t.big) {
                val pad = 0.02f * s
                drawRoundRect(Pill.copy(alpha = Pill.alpha * a), topLeft = Offset(tx - pad, ty - pad * 0.4f), size = Size(layout.size.width + pad * 2f, layout.size.height + pad * 0.8f), cornerRadius = CornerRadius(pad, pad))
            }
            drawText(layout, topLeft = Offset(tx, ty), alpha = a)
        }
        if (world.status == CekirgeStatus.RUNNING && world.time < 6f && world.wave == 1) {
            val layout = cache.getOrPut("hint|$hint") {
                textMeasurer.measure(
                    AnnotatedString(hint),
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center),
                )
            }
            val alpha = if (world.time < 5f) 1f else 6f - world.time
            val pad = 0.02f * s
            val tx = aw / 2f - layout.size.width / 2f
            val ty = 0.95f * s - layout.size.height / 2f
            drawRoundRect(Pill.copy(alpha = Pill.alpha * alpha.coerceIn(0f, 1f)), topLeft = Offset(tx - pad, ty - pad * 0.4f), size = Size(layout.size.width + pad * 2f, layout.size.height + pad * 0.8f), cornerRadius = CornerRadius(pad, pad))
            drawText(layout, topLeft = Offset(tx, ty), alpha = alpha.coerceIn(0f, 1f))
        }
    }
    if (fx.flash > 0f) drawRect(Color(0xFFEF4444).copy(alpha = 0.3f * fx.flash))
}

private fun bugColor(kind: BugKind): Color = when (kind) {
    BugKind.KARA -> BugKara
    BugKind.YESIL -> BugYesil
    BugKind.KAHVE -> BugKahve
}

/** Kuşbakışı çekirge, başı aşağıda (çiftçiye dönük); bacaklar her adımda öne/arkaya. */
private fun DrawScope.drawBug(b: Bug, cx: Float, cy: Float, s: Float, stride: Boolean) {
    val color = bugColor(b.kind)
    val bw = CekirgeWorld.BUG_HALF_W * s
    val bh = CekirgeWorld.BUG_HALF_H * s
    val legOff = if (stride) 1f else -1f
    for (i in 0 until 3) {
        val ly = cy - bh * 0.5f + i * bh * 0.5f
        val k = if (i == 1) -legOff else legOff
        drawLine(color, Offset(cx - bw * 0.5f, ly), Offset(cx - bw * 1.1f, ly + k * bh * 0.5f), strokeWidth = bh * 0.18f, cap = StrokeCap.Round)
        drawLine(color, Offset(cx + bw * 0.5f, ly), Offset(cx + bw * 1.1f, ly + k * bh * 0.5f), strokeWidth = bh * 0.18f, cap = StrokeCap.Round)
    }
    drawOval(color, topLeft = Offset(cx - bw * 0.6f, cy - bh * 0.9f), size = Size(bw * 1.2f, bh * 1.8f))
    drawCircle(color, bh * 0.55f, Offset(cx, cy + bh * 0.85f))
    drawLine(color, Offset(cx - bh * 0.2f, cy + bh * 1.1f), Offset(cx - bh * 0.6f, cy + bh * 1.7f), strokeWidth = bh * 0.12f)
    drawLine(color, Offset(cx + bh * 0.2f, cy + bh * 1.1f), Offset(cx + bh * 0.6f, cy + bh * 1.7f), strokeWidth = bh * 0.12f)
    drawCircle(BugEye, bh * 0.14f, Offset(cx - bh * 0.25f, cy + bh * 0.9f))
    drawCircle(BugEye, bh * 0.14f, Offset(cx + bh * 0.25f, cy + bh * 0.9f))
}

private fun DrawScope.drawQueen(cx: Float, cy: Float, s: Float, dir: Int, frame: Long) {
    val qw = CekirgeWorld.QUEEN_HALF_W * s
    val qh = CekirgeWorld.QUEEN_HALF_H * s
    val flap = 0.6f + 0.4f * sin(frame * 0.9f)
    drawOval(QueenWing, topLeft = Offset(cx - qw * 1.1f, cy - qh * 1.6f * flap), size = Size(qw * 0.9f, qh * 1.6f * flap))
    drawOval(QueenWing, topLeft = Offset(cx + qw * 0.2f, cy - qh * 1.6f * flap), size = Size(qw * 0.9f, qh * 1.6f * flap))
    drawOval(QueenBody, topLeft = Offset(cx - qw, cy - qh * 0.7f), size = Size(qw * 2f, qh * 1.4f))
    drawCircle(QueenBody, qh * 0.7f, Offset(cx + dir * qw * 1.05f, cy - qh * 0.1f))
    drawCircle(BugEye, qh * 0.2f, Offset(cx + dir * qw * 1.2f, cy - qh * 0.25f))
    // Taç.
    val crown = Path()
    crown.moveTo(cx + dir * qw * 0.8f, cy - qh * 0.7f)
    crown.lineTo(cx + dir * qw * 0.95f, cy - qh * 1.5f)
    crown.lineTo(cx + dir * qw * 1.1f, cy - qh * 0.9f)
    crown.lineTo(cx + dir * qw * 1.25f, cy - qh * 1.5f)
    crown.lineTo(cx + dir * qw * 1.4f, cy - qh * 0.7f)
    crown.close()
    drawPath(crown, Straw)
    for (i in 0 until 3) {
        val ly = cy - qh * 0.3f + i * qh * 0.4f
        drawLine(QueenBody, Offset(cx - qw * 0.3f, ly), Offset(cx - qw * 0.8f, ly + qh * 0.7f), strokeWidth = qh * 0.15f)
        drawLine(QueenBody, Offset(cx + qw * 0.3f, ly), Offset(cx + qw * 0.8f, ly + qh * 0.7f), strokeWidth = qh * 0.15f)
    }
}

/** Sırt pompalı çiftçi; [ready] ise fıskırtma ucu parlar. */
private fun DrawScope.drawFarmer(cx: Float, cy: Float, s: Float, ready: Boolean, frame: Long) {
    val hw = CekirgeWorld.FARMER_HALF_W * s
    val hh = CekirgeWorld.FARMER_HALF_H * s
    drawOval(Shadow, topLeft = Offset(cx - hw * 1.1f, cy + hh * 0.9f), size = Size(hw * 2.2f, hh * 0.6f))
    // Sırt pompası (tank) ve hortum.
    drawRoundRect(Tank, topLeft = Offset(cx + hw * 0.35f, cy - hh * 1.2f), size = Size(hw * 0.7f, hh * 1.9f), cornerRadius = CornerRadius(hw * 0.2f, hw * 0.2f))
    drawRect(TankDark, topLeft = Offset(cx + hw * 0.45f, cy - hh * 0.4f), size = Size(hw * 0.5f, hh * 0.15f))
    // Gövde ve tulum.
    drawRoundRect(Overalls, topLeft = Offset(cx - hw * 0.6f, cy - hh * 0.6f), size = Size(hw * 1.2f, hh * 1.7f), cornerRadius = CornerRadius(hw * 0.2f, hw * 0.2f))
    drawCircle(Skin, hh * 0.55f, Offset(cx - hw * 0.05f, cy - hh * 1.1f))
    // Hasır şapka.
    drawOval(Hat, topLeft = Offset(cx - hw * 0.85f, cy - hh * 1.65f), size = Size(hw * 1.6f, hh * 0.55f))
    drawRoundRect(Hat, topLeft = Offset(cx - hw * 0.4f, cy - hh * 2.2f), size = Size(hw * 0.7f, hh * 0.75f), cornerRadius = CornerRadius(hw * 0.1f, hw * 0.1f))
    drawRect(HatBand, topLeft = Offset(cx - hw * 0.4f, cy - hh * 1.65f), size = Size(hw * 0.7f, hh * 0.12f))
    // Fıskırtma çubuğu: omuzdan yukarı, ucu hazırsa parlar.
    val wandX = cx - hw * 0.55f
    drawLine(TankDark, Offset(cx + hw * 0.4f, cy - hh * 0.3f), Offset(wandX, cy - hh * 0.2f), strokeWidth = hh * 0.16f)
    drawLine(Tank, Offset(wandX, cy - hh * 0.2f), Offset(wandX, cy - hh * 2.4f), strokeWidth = hh * 0.22f, cap = StrokeCap.Round)
    val tip = if (ready) ShotColor else TankDark
    drawCircle(tip, hh * (if (ready) 0.2f + 0.05f * sin(frame * 0.3f) else 0.15f), Offset(wandX, cy - hh * 2.5f))
}

// ---------------------------------------------------------------------------
// Kartlar
// ---------------------------------------------------------------------------

@Composable
private fun StartCard(
    dailyMode: Boolean,
    daily: CekirgeDaily?,
    freeBest: Int,
    freeWave: Int,
    onMode: (Boolean) -> Unit,
    onStart: () -> Unit,
    onExit: () -> Unit,
) {
    val exhausted = dailyMode && daily != null && daily.attempts >= CekirgeViewModel.DAILY_ATTEMPTS
    OverlayCard {
        Text(
            text = stringResource(R.string.game_cekirge),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.cekirge_intro),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
        Text(
            text = stringResource(R.string.cekirge_rules),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeChip(stringResource(R.string.mode_daily), dailyMode, Modifier.weight(1f)) { onMode(true) }
            ModeChip(stringResource(R.string.mode_free), !dailyMode, Modifier.weight(1f)) { onMode(false) }
        }
        if (dailyMode) {
            if (daily != null) {
                Text(
                    text = stringResource(R.string.cekirge_daily_status_fmt, daily.attempts, CekirgeViewModel.DAILY_ATTEMPTS, daily.best),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = stringResource(if (exhausted) R.string.cekirge_daily_exhausted else R.string.cekirge_daily_desc),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        } else if (freeBest > 0) {
            Text(
                text = stringResource(R.string.cekirge_best_fmt, freeBest, freeWave),
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
                Text(stringResource(R.string.cekirge_start))
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
            Text(stringResource(R.string.cekirge_to_menu))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

@Composable
private fun OverCard(
    hud: CekirgeHud,
    daily: Boolean,
    attemptsLeft: Int,
    isRecord: Boolean,
    onRestart: () -> Unit,
    onMenu: () -> Unit,
    onExit: () -> Unit,
) {
    val result = stringResource(R.string.cekirge_result_fmt, hud.wave, hud.kills)
    OverlayCard {
        Text(
            text = stringResource(if (hud.invaded) R.string.cekirge_over_invaded else R.string.cekirge_over_lives),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
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
            text = result,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
        if (daily && attemptsLeft <= 0) {
            Text(
                text = stringResource(R.string.cekirge_daily_exhausted),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        ShareButton(
            ShareContent(
                gameId = "cekirge",
                headline = stringResource(R.string.cekirge_share_fmt, hud.wave, hud.score),
                details = listOf(result, modeShareLabel(daily, null)),
            ),
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text(
                when {
                    daily && attemptsLeft > 0 -> stringResource(R.string.cekirge_retry_fmt, attemptsLeft)
                    daily -> stringResource(R.string.play_free)
                    else -> stringResource(R.string.restart)
                },
            )
        }
        TextButton(onClick = onMenu, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.cekirge_to_menu))
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
