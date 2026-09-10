package com.za.games.ui.ucurtma

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
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
import com.za.games.platform.LocalZaHaptics
import com.za.games.platform.LocalZaSound
import com.za.games.platform.ShareContent
import com.za.games.ucurtma.CrashKind
import com.za.games.ucurtma.Gadget
import com.za.games.ucurtma.Mission
import com.za.games.ucurtma.MissionKind
import com.za.games.ucurtma.Missions
import com.za.games.ucurtma.UcurtmaHud
import com.za.games.ucurtma.UcurtmaStatus
import com.za.games.ucurtma.UcurtmaWorld
import com.za.games.ui.common.GameTopBar
import com.za.games.ui.common.OverlayCard
import com.za.games.ui.common.ScoreCard
import com.za.games.ui.common.ShareButton
import com.za.games.ui.common.formatScore
import com.za.games.ui.common.modeShareLabel
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.isActive

private val SkyTop = Color(0xFF60B8F5)
private val SkyBottom = Color(0xFFDDEFFB)
private val Sun = Color(0xFFFDE68A)
private val CloudColor = Color(0xFFFFFFFF)
private val Skyline = Color(0x40334155)
private val Street = Color(0xFF475569)
private val Curb = Color(0xFF94A3B8)
private val WindowLit = Color(0xFFFDE68A)
private val WindowDark = Color(0xFF1E293B)
private val Chimney = Color(0xFF7F1D1D)
private val Pole = Color(0xFF78350F)
private val WireColor = Color(0xFF0F172A)
private val KiteBody = Color(0xFFEF4444)
private val KiteAccent = Color(0xFFFDE68A)
private val StringColor = Color(0x991E293B)
private val RivalString = Color(0x99312E81)
private val FlashColor = Color(0xFFFFFFFF)
private val BuildingColors = listOf(Color(0xFFB45309), Color(0xFF9A3412), Color(0xFF6D28D9), Color(0xFF0E7490), Color(0xFF854D0E), Color(0xFF7C2D12))
private val RivalColors = listOf(Color(0xFF2563EB), Color(0xFF16A34A), Color(0xFF9333EA), Color(0xFF0891B2))
private val SkylineBlocks = listOf(0.0f to 0.22f, 0.12f to 0.35f, 0.26f to 0.18f, 0.38f to 0.42f, 0.55f to 0.28f, 0.7f to 0.38f, 0.86f to 0.2f, 1.0f to 0.3f, 1.15f to 0.45f, 1.32f to 0.24f, 1.5f to 0.36f, 1.7f to 0.2f, 1.85f to 0.3f)
private const val SKYLINE_PERIOD = 2.05f

@Composable
fun gadgetName(gadget: Gadget?): String = stringResource(
    when (gadget) {
        null -> R.string.ucurtma_gadget_none
        Gadget.TAIL -> R.string.ucurtma_gadget_tail
        Gadget.REEL -> R.string.ucurtma_gadget_reel
        Gadget.GLASS -> R.string.ucurtma_gadget_glass
    },
)

@Composable
private fun gadgetDesc(gadget: Gadget?): String = stringResource(
    when (gadget) {
        null -> R.string.ucurtma_gadget_none_desc
        Gadget.TAIL -> R.string.ucurtma_gadget_tail_desc
        Gadget.REEL -> R.string.ucurtma_gadget_reel_desc
        Gadget.GLASS -> R.string.ucurtma_gadget_glass_desc
    },
)

@Composable
fun missionText(m: Mission): String = when (m.kind) {
    MissionKind.DISTANCE -> stringResource(R.string.ucurtma_m_distance_fmt, m.target)
    MissionKind.RIBBONS -> stringResource(R.string.ucurtma_m_ribbons_fmt, m.target)
    MissionKind.UNDER_WIRE -> stringResource(R.string.ucurtma_m_under_fmt, m.target)
    MissionKind.NEAR_MISS -> stringResource(R.string.ucurtma_m_near_fmt, m.target)
    MissionKind.CUTS -> stringResource(R.string.ucurtma_m_cuts_fmt, m.target)
}

@Composable
private fun missionShort(kind: MissionKind): String = stringResource(
    when (kind) {
        MissionKind.DISTANCE -> R.string.ucurtma_ms_distance
        MissionKind.RIBBONS -> R.string.ucurtma_ms_ribbons
        MissionKind.UNDER_WIRE -> R.string.ucurtma_ms_under
        MissionKind.NEAR_MISS -> R.string.ucurtma_ms_near
        MissionKind.CUTS -> R.string.ucurtma_ms_cuts
    },
)

@Composable
fun UcurtmaScreen(
    highScore: Long,
    onScore: (Long) -> Unit,
    onExit: () -> Unit,
    viewModel: UcurtmaViewModel = viewModel(),
) {
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val dailyMode by viewModel.dailyMode.collectAsStateWithLifecycle()
    val daily by viewModel.daily.collectAsStateWithLifecycle()
    val freeBest by viewModel.freeBest.collectAsStateWithLifecycle()
    val missions by viewModel.missions.collectAsStateWithLifecycle()
    val completed by viewModel.completed.collectAsStateWithLifecycle()
    val gadget by viewModel.gadget.collectAsStateWithLifecycle()
    val lastDone by viewModel.lastDone.collectAsStateWithLifecycle()
    val lastUnlocked by viewModel.lastUnlocked.collectAsStateWithLifecycle()
    val record by viewModel.record.collectAsStateWithLifecycle()
    val hud by viewModel.hud.collectAsStateWithLifecycle()
    val runId by viewModel.runId.collectAsStateWithLifecycle()
    val haptics = LocalZaHaptics.current
    val sound = LocalZaSound.current
    val fx = remember { UcurtmaFx() }
    val fxTick = remember { mutableLongStateOf(0L) }
    val milestoneFmt = stringResource(R.string.ucurtma_meters_fmt, 0).replace("0", "%d")
    fx.missionDoneLabel = stringResource(R.string.ucurtma_mission_done)
    fx.nearMissLabel = stringResource(R.string.ucurtma_near)
    fx.milestoneLabel = { m -> milestoneFmt.replace("%d", m.toString()) }

    val latestScore by rememberUpdatedState(hud.score.toLong())
    val latestOnScore by rememberUpdatedState(onScore)
    DisposableEffect(Unit) {
        onDispose {
            latestOnScore(latestScore)
            viewModel.hold(false)
        }
    }
    LaunchedEffect(phase) {
        if (phase == UcurtmaPhase.OVER) latestOnScore(hud.score.toLong())
    }
    LaunchedEffect(runId) { fx.reset() }
    LaunchedEffect(Unit) { viewModel.refresh() }
    LifecycleResumeEffect(Unit) {
        onPauseOrDispose { viewModel.pause() }
    }
    BackHandler {
        if (phase == UcurtmaPhase.PLAYING) viewModel.pause() else onExit()
    }

    LaunchedEffect(phase, runId) {
        if (phase != UcurtmaPhase.PLAYING && phase != UcurtmaPhase.OVER) return@LaunchedEffect
        var last = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = now - last
                    for (event in viewModel.advance(dt)) fx.onEvent(event, sound, haptics)
                    fx.update(dt / 1_000_000_000f, viewModel.world)
                    fxTick.longValue += 1
                }
                last = now
            }
            if (phase == UcurtmaPhase.OVER && !fx.isBusy) break
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
    val attemptsLeft = UcurtmaViewModel.DAILY_ATTEMPTS - (daily?.attempts ?: 0)
    val best = if (dailyMode) daily?.best ?: 0 else freeBest

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        GameTopBar(title = stringResource(R.string.game_ucurtma), onExit = onExit) {
            if (phase == UcurtmaPhase.PLAYING || phase == UcurtmaPhase.PAUSED) {
                TextButton(onClick = viewModel::togglePause) {
                    Text(stringResource(if (phase == UcurtmaPhase.PAUSED) R.string.resume else R.string.pause))
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ScoreCard(label = stringResource(R.string.ucurtma_distance), value = stringResource(R.string.ucurtma_meters_fmt, hud.meters), modifier = Modifier.weight(1.1f), highlight = true)
            ScoreCard(label = stringResource(R.string.ucurtma_ribbons), value = hud.ribbons.toString(), modifier = Modifier.weight(0.8f))
            ScoreCard(label = stringResource(R.string.ucurtma_best), value = formatScore(maxOf(best, hud.score).toLong()), modifier = Modifier.weight(1f))
        }

        MissionStrip(missions = viewModel.world.missions.ifEmpty { missions }, hud = hud, visible = phase != UcurtmaPhase.MENU)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            UcurtmaCanvas(viewModel = viewModel, fx = fx, fxTick = fxTick, hud = hud, modifier = Modifier.fillMaxSize())
            when (phase) {
                UcurtmaPhase.MENU -> StartCard(
                    dailyMode = dailyMode,
                    daily = daily,
                    freeBest = freeBest,
                    missions = missions,
                    completed = completed,
                    gadget = gadget,
                    onMode = viewModel::setDailyMode,
                    onGadget = viewModel::setGadget,
                    onStart = startRun,
                    onExit = onExit,
                )
                UcurtmaPhase.PAUSED -> PauseCard(
                    onResume = viewModel::resume,
                    onRestart = restartRun,
                    onMenu = viewModel::toMenu,
                    onExit = onExit,
                )
                UcurtmaPhase.OVER -> OverCard(
                    hud = hud,
                    daily = dailyMode,
                    attemptsLeft = attemptsLeft,
                    isRecord = record || hud.score > previousBest,
                    done = lastDone,
                    unlocked = lastUnlocked,
                    onRestart = restartRun,
                    onMenu = viewModel::toMenu,
                    onExit = onExit,
                )
                UcurtmaPhase.PLAYING -> Unit
            }
        }
    }
}

/** Açık görevler: kısa ad ve ilerleme; tamamlanan vurgulanır. */
@Composable
private fun MissionStrip(missions: List<Mission>, hud: UcurtmaHud, visible: Boolean) {
    val alpha = if (visible) 1f else 0f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(26.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        missions.forEachIndexed { i, m ->
            val progress = hud.progress.getOrNull(i) ?: 0
            val done = hud.done.getOrNull(i) ?: false
            val base = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            Surface(
                shape = CircleShape,
                color = base.copy(alpha = (if (done) 0.25f else 1f) * alpha),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = (if (done) "✓ " else "") + missionShort(m.kind) + " " + stringResource(R.string.ucurtma_progress_fmt, progress, m.target),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (done) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    color = (if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = alpha),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Çizim
// ---------------------------------------------------------------------------

private fun skyScale(width: Float, height: Float): Float =
    min(width / UcurtmaWorld.WIDTH, height / UcurtmaWorld.HEIGHT)

@Composable
private fun UcurtmaCanvas(
    viewModel: UcurtmaViewModel,
    fx: UcurtmaFx,
    fxTick: MutableLongState,
    hud: UcurtmaHud,
    modifier: Modifier = Modifier,
) {
    val frame by viewModel.frame.collectAsStateWithLifecycle()
    val desc = stringResource(R.string.ucurtma_board_desc_fmt, hud.meters, hud.ribbons)
    val hint = stringResource(R.string.ucurtma_hold_hint)
    val textMeasurer = rememberTextMeasurer()
    val textCache = remember { HashMap<String, TextLayoutResult>() }
    val path = remember { Path() }
    val sky = remember { Brush.verticalGradient(listOf(SkyTop, SkyBottom)) }
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .semantics { contentDescription = desc }
            .pointerInput(viewModel) {
                // Parmak tuvalde basılıyken ip çekilir; birden çok parmak da olsa
                // biri basılı kaldıkça çekili sayılır.
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        viewModel.hold(event.changes.any { it.pressed })
                        event.changes.forEach { it.consume() }
                    }
                }
            },
    ) {
        val tick = frame + fxTick.longValue
        if (tick < 0L) return@Canvas
        drawSky(viewModel.world, fx, path, sky, textMeasurer, textCache, hint)
    }
}

private fun DrawScope.drawSky(
    world: UcurtmaWorld,
    fx: UcurtmaFx,
    path: Path,
    sky: Brush,
    textMeasurer: TextMeasurer,
    cache: HashMap<String, TextLayoutResult>,
    hint: String,
) {
    val w = size.width
    val h = size.height
    val s = skyScale(w, h)
    val cw = UcurtmaWorld.WIDTH * s
    val ch = UcurtmaWorld.HEIGHT * s
    val ox = (w - cw) / 2f
    val oy = (h - ch) / 2f

    drawRect(sky)
    translate(ox + fx.shakeX * s, oy + fx.shakeY * s) {
        // Güneş ve bulutlar.
        drawCircle(Sun.copy(alpha = 0.35f), radius = 0.13f * s, center = Offset(0.84f * s, 0.2f * s))
        drawCircle(Sun, radius = 0.08f * s, center = Offset(0.84f * s, 0.2f * s))
        for (c in fx.clouds) {
            val cx = c.x * s
            val cy = c.y * s
            val r = c.size * s
            drawCircle(CloudColor.copy(alpha = c.alpha), r, Offset(cx, cy))
            drawCircle(CloudColor.copy(alpha = c.alpha), r * 0.8f, Offset(cx - r * 1.1f, cy + r * 0.25f))
            drawCircle(CloudColor.copy(alpha = c.alpha), r * 0.85f, Offset(cx + r * 1.1f, cy + r * 0.2f))
        }
        // Uzak şehir silüeti (paralaks).
        val shift = (world.distance * 0.35f) % SKYLINE_PERIOD
        for (k in 0..1) {
            for ((bx, bh) in SkylineBlocks) {
                val x = (bx - shift + k * SKYLINE_PERIOD) * s
                if (x > cw || x + 0.14f * s < 0f) continue
                drawRect(Skyline, topLeft = Offset(x, (UcurtmaWorld.GROUND - bh) * s), size = Size(0.13f * s, bh * s))
            }
        }
        // Sokak.
        drawRect(Street, topLeft = Offset(0f, UcurtmaWorld.GROUND * s), size = Size(cw, ch - UcurtmaWorld.GROUND * s))
        drawRect(Curb, topLeft = Offset(0f, UcurtmaWorld.GROUND * s), size = Size(cw, 0.008f * s))
        // Binalar.
        for (b in world.buildings) {
            val x = world.screenX(b.x) * s
            val bw = b.width * s
            if (x > cw || x + bw < 0f) continue
            val top = b.top * s
            val color = BuildingColors[((b.x * 37f).toInt() and 0x7FFFFFFF) % BuildingColors.size]
            drawRect(color, topLeft = Offset(x, top), size = Size(bw, UcurtmaWorld.GROUND * s - top))
            drawRect(Color.Black.copy(alpha = 0.18f), topLeft = Offset(x, top), size = Size(bw, 0.012f * s))
            var wy = top + 0.05f * s
            var row = 0
            while (wy + 0.03f * s < UcurtmaWorld.GROUND * s) {
                var wx = x + 0.025f * s
                var col = 0
                while (wx + 0.03f * s < x + bw) {
                    val lit = ((b.x * 53f).toInt() + row * 7 + col * 3) % 3 != 0
                    drawRect(if (lit) WindowLit else WindowDark, topLeft = Offset(wx, wy), size = Size(0.03f * s, 0.035f * s))
                    wx += 0.055f * s
                    col++
                }
                wy += 0.07f * s
                row++
            }
            if (b.chimneyX >= 0f) {
                val cx = world.screenX(b.chimneyX) * s
                drawRect(Chimney, topLeft = Offset(cx, b.chimneyTop * s), size = Size(UcurtmaWorld.CHIMNEY_W * s, (b.top - b.chimneyTop) * s))
                drawRect(Color.Black.copy(alpha = 0.3f), topLeft = Offset(cx - 0.004f * s, b.chimneyTop * s), size = Size((UcurtmaWorld.CHIMNEY_W + 0.008f) * s, 0.012f * s))
            }
        }
        // Direkler ve teller.
        for (wr in world.wires) {
            val x1 = world.screenX(wr.x1) * s
            val x2 = world.screenX(wr.x2) * s
            if (x1 > cw || x2 < 0f) continue
            val y = wr.y * s
            for (px in listOf(x1, x2)) {
                drawRect(Pole, topLeft = Offset(px - 0.006f * s, y - 0.05f * s), size = Size(0.012f * s, UcurtmaWorld.GROUND * s - y + 0.05f * s))
                drawRect(Pole, topLeft = Offset(px - 0.03f * s, y - 0.012f * s), size = Size(0.06f * s, 0.008f * s))
            }
            drawLine(WireColor.copy(alpha = 0.85f), Offset(x1, y), Offset(x2, y), strokeWidth = 0.006f * s)
            drawLine(Color.White.copy(alpha = 0.35f), Offset(x1, y - 0.006f * s), Offset(x2, y - 0.006f * s), strokeWidth = 0.002f * s)
        }
        // Kurdeleler.
        for (rb in world.ribbonItems) {
            if (rb.taken) continue
            val x = world.screenX(rb.x) * s
            if (x < -0.05f * s || x > cw + 0.05f * s) continue
            val y = (rb.y + 0.01f * sin(world.frames * 0.15f + rb.x * 10f)) * s
            val color = UcurtmaFx.RibbonColors[((rb.x * 31f).toInt() and 0x7FFFFFFF) % UcurtmaFx.RibbonColors.size]
            val r = UcurtmaWorld.RIBBON_R * s
            path.reset()
            path.moveTo(x, y)
            path.lineTo(x - r * 1.4f, y - r * 0.9f)
            path.lineTo(x - r * 1.4f, y + r * 0.9f)
            path.close()
            drawPath(path, color)
            path.reset()
            path.moveTo(x, y)
            path.lineTo(x + r * 1.4f, y - r * 0.9f)
            path.lineTo(x + r * 1.4f, y + r * 0.9f)
            path.close()
            drawPath(path, color)
            drawCircle(Color.White, r * 0.35f, Offset(x, y))
        }
        // Rakip uçurtmalar ve ipleri.
        for (r in world.rivals) {
            val x = world.screenX(r.x) * s
            if (x < -0.4f * s || x > cw + 0.3f * s) continue
            val y = r.y * s
            val color = RivalColors[r.id % RivalColors.size]
            if (r.alive) {
                drawLine(RivalString, Offset(x, y), Offset(x - UcurtmaWorld.RIVAL_STRING_DX * s, UcurtmaWorld.ANCHOR_Y * s), strokeWidth = 0.004f * s)
            } else {
                drawLine(RivalString, Offset(x, y), Offset(x - 0.04f * s, y + 0.08f * s), strokeWidth = 0.004f * s)
            }
            drawKite(path, x, y, UcurtmaWorld.RIVAL_R * s, if (r.alive) 8f * sin(r.t * 2f) else r.fall * 70f, color, Color.White.copy(alpha = 0.8f), world.frames, tailBows = 2)
        }
        // Bizim uçurtma ve ip.
        val kx = UcurtmaWorld.KITE_X * s
        val ky = world.kiteY * s
        drawLine(StringColor, Offset(kx, ky), Offset(UcurtmaWorld.ANCHOR_X * s, UcurtmaWorld.ANCHOR_Y * s), strokeWidth = 0.004f * s)
        val crashed = world.status == UcurtmaStatus.OVER
        drawKite(path, kx, ky, UcurtmaWorld.KITE_R * s, if (crashed) 35f else world.kiteVy * 18f, KiteBody, KiteAccent, world.frames, tailBows = 3)
        // Parçacıklar ve yazılar.
        for (p in fx.particles) {
            drawCircle(p.color.copy(alpha = (p.life / p.maxLife).coerceIn(0f, 1f)), p.size * s, Offset(p.x * s, p.y * s))
        }
        for (t in fx.texts) {
            val layout = cache.getOrPut("${t.text}|${t.big}|${t.color.value}") {
                textMeasurer.measure(
                    AnnotatedString(t.text),
                    style = TextStyle(fontSize = if (t.big) 24.sp else 15.sp, fontWeight = FontWeight.Black, color = t.color, textAlign = TextAlign.Center),
                )
            }
            drawText(layout, topLeft = Offset(t.x * s - layout.size.width / 2f, t.y * s - layout.size.height / 2f), alpha = (t.life / t.maxLife).coerceIn(0f, 1f))
        }
        // İlk saniyelerde ipucu.
        if (world.frames in 1..240 && world.status == UcurtmaStatus.RUNNING) {
            val layout = cache.getOrPut("hint|$hint") {
                textMeasurer.measure(
                    AnnotatedString(hint),
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), textAlign = TextAlign.Center),
                )
            }
            val alpha = if (world.frames > 180) (240 - world.frames) / 60f else 1f
            drawText(layout, topLeft = Offset(cw / 2f - layout.size.width / 2f, 0.55f * s - layout.size.height / 2f), alpha = alpha * 0.8f)
        }
    }
    if (fx.flash > 0f) drawRect(FlashColor.copy(alpha = 0.3f * fx.flash))
}

private fun DrawScope.drawKite(
    path: Path,
    cx: Float,
    cy: Float,
    r: Float,
    tilt: Float,
    body: Color,
    accent: Color,
    frames: Int,
    tailBows: Int,
) {
    rotate(tilt, pivot = Offset(cx, cy)) {
        path.reset()
        path.moveTo(cx, cy - r * 1.3f)
        path.lineTo(cx + r, cy)
        path.lineTo(cx, cy + r * 1.5f)
        path.lineTo(cx - r, cy)
        path.close()
        drawPath(path, body)
        drawLine(accent, Offset(cx, cy - r * 1.3f), Offset(cx, cy + r * 1.5f), strokeWidth = r * 0.12f)
        drawLine(accent, Offset(cx - r, cy), Offset(cx + r, cy), strokeWidth = r * 0.12f)
        var px = cx
        var py = cy + r * 1.5f
        for (i in 1..tailBows) {
            val nx = cx - i * r * 0.55f
            val ny = cy + r * 1.5f + i * r * 0.5f + sin(frames * 0.3f + i) * r * 0.25f
            drawLine(accent, Offset(px, py), Offset(nx, ny), strokeWidth = r * 0.1f)
            drawCircle(accent, r * 0.18f, Offset(nx, ny))
            px = nx
            py = ny
        }
    }
}

// ---------------------------------------------------------------------------
// Kartlar
// ---------------------------------------------------------------------------

@Composable
private fun StartCard(
    dailyMode: Boolean,
    daily: UcurtmaDaily?,
    freeBest: Int,
    missions: List<Mission>,
    completed: Int,
    gadget: Gadget?,
    onMode: (Boolean) -> Unit,
    onGadget: (Gadget?) -> Unit,
    onStart: () -> Unit,
    onExit: () -> Unit,
) {
    val exhausted = dailyMode && daily != null && daily.attempts >= UcurtmaViewModel.DAILY_ATTEMPTS
    val unlocked = Missions.unlocked(completed)
    OverlayCard {
        Text(
            text = stringResource(R.string.game_ucurtma),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.ucurtma_intro),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeChip(stringResource(R.string.mode_daily), dailyMode, Modifier.weight(1f)) { onMode(true) }
            ModeChip(stringResource(R.string.mode_free), !dailyMode, Modifier.weight(1f)) { onMode(false) }
        }
        if (dailyMode) {
            if (daily != null) {
                Text(
                    text = stringResource(R.string.ucurtma_daily_status_fmt, daily.attempts, UcurtmaViewModel.DAILY_ATTEMPTS, daily.best),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = stringResource(if (exhausted) R.string.ucurtma_daily_exhausted else R.string.ucurtma_daily_desc),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        } else if (freeBest > 0) {
            Text(
                text = stringResource(R.string.ucurtma_best_fmt, freeBest),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = stringResource(R.string.ucurtma_gadget),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ModeChip(gadgetName(null), gadget == null, Modifier.weight(1f)) { onGadget(null) }
            for (g in Gadget.entries) {
                val open = g in unlocked
                ModeChip(if (open) gadgetName(g) else "🔒 " + gadgetName(g), gadget == g, Modifier.weight(1f), enabled = open) { onGadget(g) }
            }
        }
        val lockedNext = Gadget.entries.firstOrNull { it !in unlocked }
        Text(
            text = if (gadget == null && lockedNext != null) {
                gadgetDesc(null) + " · " + gadgetName(lockedNext) + " " + stringResource(R.string.ucurtma_locked_fmt, Missions.unlockAt(lockedNext))
            } else {
                gadgetDesc(gadget)
            },
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Text(
            text = stringResource(R.string.ucurtma_missions) + " · " + stringResource(R.string.ucurtma_completed_fmt, completed),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        for (m in missions) {
            Text(
                text = "• " + missionText(m),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            )
        }
        Spacer(Modifier.height(4.dp))
        if (exhausted) {
            Button(onClick = { onMode(false) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.play_free))
            }
        } else {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.ucurtma_start))
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
            Text(stringResource(R.string.ucurtma_to_menu))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

@Composable
private fun OverCard(
    hud: UcurtmaHud,
    daily: Boolean,
    attemptsLeft: Int,
    isRecord: Boolean,
    done: List<Mission>,
    unlocked: List<Gadget>,
    onRestart: () -> Unit,
    onMenu: () -> Unit,
    onExit: () -> Unit,
) {
    val title = stringResource(
        when (hud.crash) {
            CrashKind.ROOF -> R.string.ucurtma_over_roof
            CrashKind.WIRE -> R.string.ucurtma_over_wire
            CrashKind.STRING -> R.string.ucurtma_over_string
            null -> R.string.ucurtma_abandoned
        },
    )
    val result = stringResource(R.string.ucurtma_result_fmt, hud.meters, hud.ribbons, hud.cuts)
    OverlayCard {
        Text(
            text = title,
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
        for (m in done) {
            Text(
                text = "✓ " + missionText(m),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        for (g in unlocked) {
            Text(
                text = stringResource(R.string.ucurtma_unlocked_fmt, gadgetName(g)),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        if (daily && attemptsLeft <= 0) {
            Text(
                text = stringResource(R.string.ucurtma_daily_exhausted),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        ShareButton(
            ShareContent(
                gameId = "ucurtma",
                headline = stringResource(R.string.ucurtma_share_fmt, hud.meters, hud.score),
                details = listOf(result, modeShareLabel(daily, null)),
            ),
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text(
                when {
                    daily && attemptsLeft > 0 -> stringResource(R.string.ucurtma_retry_fmt, attemptsLeft)
                    daily -> stringResource(R.string.play_free)
                    else -> stringResource(R.string.restart)
                },
            )
        }
        TextButton(onClick = onMenu, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.ucurtma_to_menu))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 1f else 0.5f),
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            color = (if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = if (enabled) 1f else 0.5f),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
        )
    }
}
