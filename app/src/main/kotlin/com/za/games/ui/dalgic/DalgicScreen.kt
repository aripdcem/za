package com.za.games.ui.dalgic

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.za.games.dalgic.DalgicHud
import com.za.games.dalgic.DalgicStatus
import com.za.games.dalgic.DalgicWorld
import com.za.games.dalgic.FoeKind
import com.za.games.dalgic.LifeCause
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
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.isActive

private val SkyColor = Color(0xFFBAE6FD)
private val WaterTop = Color(0xFF38BDF8)
private val WaterBottom = Color(0xFF0C2A5A)
private val SurfaceLine = Color(0xFFF0F9FF)
private val Sand = Color(0xFFD6B27A)
private val SandDark = Color(0xFFB08A55)
private val Weed = Color(0xFF15803D)
private val CurrentBand = Color(0x22F0F9FF)
private val CurrentDash = Color(0x66F0F9FF)
private val SubBody = Color(0xFFFACC15)
private val SubDark = Color(0xFFCA8A04)
private val SubWindow = Color(0xFF7DD3FC)
private val DiverSuit = Color(0xFF1F2937)
private val DiverSkin = Color(0xFFFCD9B6)
private val SharkBody = Color(0xFF94A3B8)
private val SharkBelly = Color(0xFFE2E8F0)
private val EnemyBody = Color(0xFF991B1B)
private val EnemyDark = Color(0xFF450A0A)
private val MineBody = Color(0xFF111827)
private val MineSpike = Color(0xFF374151)
private val Chain = Color(0x88111827)
private val FriendlyTorpedo = Color(0xFFFDE68A)
private val EnemyTorpedo = Color(0xFFFCA5A5)
private val Bubble = Color(0x99F0F9FF)
private val OxygenOk = Color(0xFF4ADE80)
private val OxygenLow = Color(0xFFF87171)

/** Sürükleme katsayısı: 2B sürüklemede 1,3; kort genişliğini geçmek bir başparmak hamlesi. */
private const val DRAG_GAIN = 1.3f

@Composable
private fun lifeLabel(cause: LifeCause): String = stringResource(
    when (cause) {
        LifeCause.SHARK -> R.string.dalgic_life_shark
        LifeCause.ENEMY_SUB -> R.string.dalgic_life_sub
        LifeCause.TORPEDO -> R.string.dalgic_life_torpedo
        LifeCause.MINE -> R.string.dalgic_life_mine
        LifeCause.OXYGEN -> R.string.dalgic_life_oxygen
        LifeCause.EMPTY_SURFACE -> R.string.dalgic_life_empty
    },
)

@Composable
fun DalgicScreen(
    highScore: Long,
    onScore: (Long) -> Unit,
    onExit: () -> Unit,
    viewModel: DalgicViewModel = viewModel(),
) {
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val dailyMode by viewModel.dailyMode.collectAsStateWithLifecycle()
    val daily by viewModel.daily.collectAsStateWithLifecycle()
    val freeBest by viewModel.freeBest.collectAsStateWithLifecycle()
    val record by viewModel.record.collectAsStateWithLifecycle()
    val hud by viewModel.hud.collectAsStateWithLifecycle()
    val runId by viewModel.runId.collectAsStateWithLifecycle()
    val haptics = LocalZaHaptics.current
    val sound = LocalZaSound.current
    val fx = remember { DalgicFx() }
    val fxTick = remember { mutableLongStateOf(0L) }
    fx.fullLabel = stringResource(R.string.dalgic_full)
    fx.oxygenLabel = stringResource(R.string.dalgic_oxygen_low)
    val labels = LifeCause.entries.associateWith { lifeLabel(it) }
    fx.lifeLabel = { labels[it] ?: it.name }

    val latestScore by rememberUpdatedState(hud.score.toLong())
    val latestOnScore by rememberUpdatedState(onScore)
    DisposableEffect(Unit) {
        onDispose { latestOnScore(latestScore) }
    }
    LaunchedEffect(phase) {
        if (phase == DalgicPhase.OVER) latestOnScore(hud.score.toLong())
    }
    LaunchedEffect(runId) { fx.reset() }
    LaunchedEffect(Unit) { viewModel.refresh() }
    LifecycleResumeEffect(Unit) {
        onPauseOrDispose { viewModel.pause() }
    }
    BackHandler {
        if (phase == DalgicPhase.PLAYING) viewModel.pause() else onExit()
    }

    LaunchedEffect(phase, runId) {
        if (phase != DalgicPhase.PLAYING && phase != DalgicPhase.OVER) return@LaunchedEffect
        var last = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = now - last
                    val world = viewModel.world
                    for (event in viewModel.advance(dt)) fx.onEvent(event, world, sound, haptics)
                    fx.update(dt / 1_000_000_000f, world)
                    fxTick.longValue += 1
                }
                last = now
            }
            if (phase == DalgicPhase.OVER && !fx.isBusy) break
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
    val attemptsLeft = DalgicViewModel.DAILY_ATTEMPTS - (daily?.attempts ?: 0)
    val best = if (dailyMode) daily?.best ?: 0 else freeBest

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        GameTopBar(title = stringResource(R.string.game_dalgic), onExit = onExit) {
            if (phase == DalgicPhase.PLAYING || phase == DalgicPhase.PAUSED) {
                TextButton(onClick = viewModel::togglePause) {
                    Text(stringResource(if (phase == DalgicPhase.PAUSED) R.string.resume else R.string.pause))
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
            ScoreCard(label = stringResource(R.string.dalgic_wave), value = (hud.wave + 1).toString(), modifier = Modifier.weight(0.7f))
            ScoreCard(label = stringResource(R.string.dalgic_lives), value = if (hud.lives > 0) "♥".repeat(hud.lives) else "–", modifier = Modifier.weight(0.8f))
            ScoreCard(label = stringResource(R.string.dalgic_best), value = formatScore(maxOf(best, hud.score).toLong()), modifier = Modifier.weight(1f))
        }

        StatusBar(hud = hud, visible = phase != DalgicPhase.MENU)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            DalgicCanvas(viewModel = viewModel, fx = fx, fxTick = fxTick, hud = hud, modifier = Modifier.fillMaxSize())
            when (phase) {
                DalgicPhase.MENU -> StartCard(
                    dailyMode = dailyMode,
                    daily = daily,
                    freeBest = freeBest,
                    onMode = viewModel::setDailyMode,
                    onStart = startRun,
                    onExit = onExit,
                )
                DalgicPhase.PAUSED -> PauseCard(
                    onResume = viewModel::resume,
                    onRestart = restartRun,
                    onMenu = viewModel::toMenu,
                    onExit = onExit,
                )
                DalgicPhase.OVER -> OverCard(
                    hud = hud,
                    daily = dailyMode,
                    attemptsLeft = attemptsLeft,
                    isRecord = record || hud.score > previousBest,
                    onRestart = restartRun,
                    onMenu = viewModel::toMenu,
                    onExit = onExit,
                )
                DalgicPhase.PLAYING -> Unit
            }
        }
    }
}

/** Oksijen çubuğu ve dalgıç yuvaları. */
@Composable
private fun StatusBar(hud: DalgicHud, visible: Boolean) {
    val alpha = if (visible) 1f else 0f
    val low = hud.oxygen <= DalgicWorld.OXYGEN_LOW / DalgicWorld.OXYGEN_MAX
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.dalgic_oxygen),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = (if (low) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.8f * alpha),
        )
        val track = MaterialTheme.colorScheme.surfaceVariant
        val fill = if (low) OxygenLow else OxygenOk
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(CircleShape),
        ) {
            drawRect(track.copy(alpha = alpha))
            drawRect(fill.copy(alpha = alpha), size = Size(size.width * hud.oxygen.coerceIn(0f, 1f), size.height))
        }
        Text(
            text = stringResource(R.string.dalgic_divers),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f * alpha),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (i in 0 until DalgicWorld.CAPACITY) {
                val filled = i < hud.divers
                Surface(
                    shape = CircleShape,
                    color = (if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant).copy(alpha = alpha),
                    modifier = Modifier.size(10.dp),
                ) {}
            }
        }
        Spacer(Modifier.width(2.dp))
    }
}

// ---------------------------------------------------------------------------
// Çizim
// ---------------------------------------------------------------------------

private fun seaScale(width: Float, height: Float): Float =
    min(width / DalgicWorld.WIDTH, height / DalgicWorld.HEIGHT)

@Composable
private fun DalgicCanvas(
    viewModel: DalgicViewModel,
    fx: DalgicFx,
    fxTick: MutableLongState,
    hud: DalgicHud,
    modifier: Modifier = Modifier,
) {
    val frame by viewModel.frame.collectAsStateWithLifecycle()
    val desc = stringResource(R.string.dalgic_board_desc_fmt, hud.score, hud.divers, (hud.oxygen * 100f).roundToInt())
    val hint = stringResource(R.string.dalgic_drag_hint)
    val textMeasurer = rememberTextMeasurer()
    val textCache = remember { HashMap<String, TextLayoutResult>() }
    val path = remember { Path() }
    val water = remember { Brush.verticalGradient(0f to WaterTop, 1f to WaterBottom) }
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .semantics { contentDescription = desc }
            .pointerInput(viewModel) {
                // Olaylar doğrudan okunur: ilk pikselden itibaren sürükleme (Filo bulgusu).
                awaitEachGesture {
                    val first = awaitFirstDown(requireUnconsumed = false)
                    var last = first.position
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == first.id } ?: break
                        if (!change.pressed) break
                        val dx = change.position.x - last.x
                        val dy = change.position.y - last.y
                        last = change.position
                        if (dx != 0f || dy != 0f) {
                            val scale = seaScale(size.width.toFloat(), size.height.toFloat())
                            if (scale > 0f) viewModel.drag(dx * DRAG_GAIN / scale, dy * DRAG_GAIN / scale)
                        }
                        change.consume()
                    }
                }
            },
    ) {
        val tick = frame + fxTick.longValue
        if (tick < 0L) return@Canvas
        drawSea(viewModel.world, fx, path, water, textMeasurer, textCache, hint)
    }
}

private fun DrawScope.drawSea(
    world: DalgicWorld,
    fx: DalgicFx,
    path: Path,
    water: Brush,
    textMeasurer: TextMeasurer,
    cache: HashMap<String, TextLayoutResult>,
    hint: String,
) {
    val w = size.width
    val h = size.height
    val s = seaScale(w, h)
    val cw = DalgicWorld.WIDTH * s
    val ch = DalgicWorld.HEIGHT * s
    val ox = (w - cw) / 2f
    val oy = (h - ch) / 2f

    drawRect(SkyColor)
    translate(ox + fx.shakeX * s, oy + fx.shakeY * s) {
        val surfaceY = DalgicWorld.SURFACE_Y * s
        drawRect(water, topLeft = Offset(0f, surfaceY), size = Size(cw, ch - surfaceY))
        // Yüzey dalgası.
        path.reset()
        path.moveTo(0f, surfaceY)
        var x = 0f
        while (x <= cw) {
            path.lineTo(x, surfaceY + sin(x / s * 18f + world.frames * 0.08f) * 0.006f * s)
            x += 6f
        }
        drawPath(path, SurfaceLine, style = Stroke(width = 0.008f * s))
        // Akıntı bantları.
        for (c in world.currents) {
            val top = (c.y - c.half) * s
            drawRect(CurrentBand, topLeft = Offset(0f, top), size = Size(cw, c.half * 2f * s))
            val phase = (world.frames * c.speed * c.dir * 0.4f) % 0.12f
            var dx = -0.12f + phase
            while (dx < 1.1f) {
                val x1 = dx * s
                drawLine(CurrentDash, Offset(x1, c.y * s), Offset(x1 + 0.05f * c.dir * s, c.y * s), strokeWidth = 0.004f * s)
                dx += 0.12f
            }
        }
        // Taban: kum, kaya ve yosun.
        val floor = DalgicWorld.FLOOR_Y * s
        drawRect(Sand, topLeft = Offset(0f, floor), size = Size(cw, ch - floor))
        drawRect(SandDark, topLeft = Offset(0f, floor), size = Size(cw, 0.008f * s))
        for (i in 0 until 6) {
            val bx = (0.08f + i * 0.17f) * s
            val sway = sin(world.frames * 0.05f + i) * 0.01f * s
            drawLine(Weed, Offset(bx, floor), Offset(bx + sway, floor - 0.07f * s), strokeWidth = 0.012f * s)
            drawLine(Weed, Offset(bx + 0.02f * s, floor), Offset(bx + 0.02f * s - sway, floor - 0.05f * s), strokeWidth = 0.01f * s)
        }
        drawCircle(SandDark, radius = 0.03f * s, center = Offset(0.55f * s, floor))
        drawCircle(SandDark, radius = 0.02f * s, center = Offset(0.9f * s, floor))
        // Kabarcıklar.
        for (b in fx.bubbles) drawCircle(Bubble, radius = b.r * s, center = Offset(b.x * s, b.y * s), style = Stroke(width = 1.5f))
        // Mayınlar (zincirli).
        for (f in world.foes) if (f.alive && f.kind == FoeKind.MINE) drawMine(f.x * s, f.y * s, f.kind.radius * s, floor)
        // Dalgıçlar.
        for (dv in world.diverList) if (dv.alive) drawDiver(path, dv.x * s, dv.y * s, DalgicWorld.DIVER_R * s, dv.dir, dv.t)
        // Köpekbalıkları ve düşman denizaltılar.
        for (f in world.foes) {
            if (!f.alive) continue
            when (f.kind) {
                FoeKind.SHARK -> drawShark(path, f.x * s, f.y * s, f.kind.radius * s, f.dir, f.t)
                FoeKind.ENEMY_SUB -> drawEnemySub(f.x * s, f.y * s, f.kind.radius * s, f.dir)
                FoeKind.MINE -> Unit
            }
        }
        // Torpidolar.
        for (t in world.torpedoes) {
            val len = 0.04f * s
            val col = if (t.friendly) FriendlyTorpedo else EnemyTorpedo
            drawRoundRect(col, topLeft = Offset(t.x * s - len / 2f, t.y * s - DalgicWorld.TORPEDO_R * s), size = Size(len, DalgicWorld.TORPEDO_R * 2f * s), cornerRadius = CornerRadius(len / 2f, len / 2f))
        }
        // Denizaltı.
        val blink = world.invuln > 0f && (world.frames / 4) % 2 == 0
        if (!blink) drawSub(world.subX * s, world.subY * s, DalgicWorld.SUB_R * s, world.facing, world.status == DalgicStatus.OVER)
        // Parçacıklar ve yazılar.
        for (p in fx.particles) {
            drawCircle(p.color.copy(alpha = (p.life / p.maxLife).coerceIn(0f, 1f)), p.size * s, Offset(p.x * s, p.y * s))
        }
        for (t in fx.texts) {
            val layout = cache.getOrPut("${t.text}|${t.big}|${t.color.value}") {
                textMeasurer.measure(
                    AnnotatedString(t.text),
                    style = TextStyle(fontSize = if (t.big) 22.sp else 14.sp, fontWeight = FontWeight.Black, color = t.color, textAlign = TextAlign.Center),
                )
            }
            drawText(layout, topLeft = Offset(t.x * s - layout.size.width / 2f, t.y * s - layout.size.height / 2f), alpha = (t.life / t.maxLife).coerceIn(0f, 1f))
        }
        if (world.frames in 1..240 && world.status == DalgicStatus.RUNNING) {
            val layout = cache.getOrPut("hint|$hint") {
                textMeasurer.measure(
                    AnnotatedString(hint),
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center),
                )
            }
            val alpha = if (world.frames > 180) (240 - world.frames) / 60f else 1f
            drawText(layout, topLeft = Offset(cw / 2f - layout.size.width / 2f, 0.6f * s - layout.size.height / 2f), alpha = alpha * 0.85f)
        }
    }
    if (fx.flash > 0f) drawRect(fx.flashColor.copy(alpha = 0.3f * fx.flash))
}

private fun DrawScope.drawSub(cx: Float, cy: Float, r: Float, facing: Int, sunk: Boolean) {
    val f = facing.toFloat()
    val body = if (sunk) SubDark else SubBody
    drawRoundRect(body, topLeft = Offset(cx - r * 1.6f, cy - r * 0.7f), size = Size(r * 3.2f, r * 1.4f), cornerRadius = CornerRadius(r * 0.7f, r * 0.7f))
    drawRoundRect(SubDark, topLeft = Offset(cx - r * 0.5f, cy - r * 1.35f), size = Size(r * 1f, r * 0.75f), cornerRadius = CornerRadius(r * 0.2f, r * 0.2f))
    drawRect(SubDark, topLeft = Offset(cx + f * r * 0.1f - r * 0.08f, cy - r * 1.9f), size = Size(r * 0.16f, r * 0.6f))
    drawRect(SubDark, topLeft = Offset(cx + f * r * 0.1f - (if (f > 0) 0f else r * 0.35f), cy - r * 1.9f), size = Size(r * 0.35f, r * 0.14f))
    drawCircle(SubWindow, radius = r * 0.28f, center = Offset(cx + f * r * 0.8f, cy - r * 0.05f))
    drawCircle(SubDark, radius = r * 0.28f, center = Offset(cx + f * r * 0.8f, cy - r * 0.05f), style = Stroke(width = r * 0.08f))
    // Pervane ve dümen (arkada).
    drawRect(SubDark, topLeft = Offset(cx - f * r * 1.9f - (if (f > 0) r * 0.25f else 0f), cy - r * 0.9f), size = Size(r * 0.25f, r * 1.8f))
}

private fun DrawScope.drawDiver(path: Path, cx: Float, cy: Float, r: Float, dir: Int, t: Float) {
    val f = dir.toFloat()
    val kick = sin(t * 6f) * r * 0.5f
    drawCircle(DiverSkin, radius = r * 0.45f, center = Offset(cx + f * r * 0.9f, cy - r * 0.3f))
    drawRoundRect(DiverSuit, topLeft = Offset(cx - r * 0.9f, cy - r * 0.45f), size = Size(r * 1.8f, r * 0.9f), cornerRadius = CornerRadius(r * 0.45f, r * 0.45f))
    path.reset()
    path.moveTo(cx - f * r * 0.8f, cy)
    path.lineTo(cx - f * r * 1.7f, cy - r * 0.35f + kick)
    path.lineTo(cx - f * r * 1.7f, cy + r * 0.35f + kick)
    path.close()
    drawPath(path, DiverSuit)
    drawCircle(SubWindow, radius = r * 0.22f, center = Offset(cx + f * r * 1.05f, cy - r * 0.35f))
}

private fun DrawScope.drawShark(path: Path, cx: Float, cy: Float, r: Float, dir: Int, t: Float) {
    val f = dir.toFloat()
    val tail = sin(t * 5f) * r * 0.4f
    path.reset()
    path.moveTo(cx + f * r * 1.8f, cy)
    path.lineTo(cx + f * r * 0.4f, cy - r * 0.75f)
    path.lineTo(cx - f * r * 1.2f, cy - r * 0.35f)
    path.lineTo(cx - f * r * 1.9f, cy - r * 0.9f + tail)
    path.lineTo(cx - f * r * 1.5f, cy)
    path.lineTo(cx - f * r * 1.9f, cy + r * 0.9f + tail)
    path.lineTo(cx - f * r * 1.2f, cy + r * 0.35f)
    path.lineTo(cx + f * r * 0.4f, cy + r * 0.75f)
    path.close()
    drawPath(path, SharkBody)
    drawRoundRect(SharkBelly, topLeft = Offset(cx - f * r * 0.9f - (if (f > 0) 0f else r * 1.6f), cy + r * 0.15f), size = Size(r * 1.6f, r * 0.45f), cornerRadius = CornerRadius(r * 0.25f, r * 0.25f))
    path.reset()
    path.moveTo(cx - f * r * 0.1f, cy - r * 0.6f)
    path.lineTo(cx - f * r * 0.5f, cy - r * 1.5f)
    path.lineTo(cx - f * r * 0.9f, cy - r * 0.55f)
    path.close()
    drawPath(path, SharkBody)
    drawCircle(Color.Black, radius = r * 0.12f, center = Offset(cx + f * r * 1.0f, cy - r * 0.25f))
}

private fun DrawScope.drawEnemySub(cx: Float, cy: Float, r: Float, dir: Int) {
    val f = dir.toFloat()
    drawRoundRect(EnemyBody, topLeft = Offset(cx - r * 1.6f, cy - r * 0.65f), size = Size(r * 3.2f, r * 1.3f), cornerRadius = CornerRadius(r * 0.65f, r * 0.65f))
    drawRoundRect(EnemyDark, topLeft = Offset(cx - r * 0.4f, cy - r * 1.2f), size = Size(r * 0.8f, r * 0.6f), cornerRadius = CornerRadius(r * 0.15f, r * 0.15f))
    drawCircle(EnemyDark, radius = r * 0.2f, center = Offset(cx + f * r * 0.7f, cy))
    drawRect(EnemyDark, topLeft = Offset(cx - f * r * 1.85f - (if (f > 0) r * 0.2f else 0f), cy - r * 0.8f), size = Size(r * 0.2f, r * 1.6f))
}

private fun DrawScope.drawMine(cx: Float, cy: Float, r: Float, floor: Float) {
    drawLine(Chain, Offset(cx, cy + r), Offset(cx, floor), strokeWidth = r * 0.12f)
    for (i in 0 until 8) {
        val a = i * 0.785f
        drawLine(MineSpike, Offset(cx, cy), Offset(cx + kotlin.math.cos(a) * r * 1.4f, cy + sin(a) * r * 1.4f), strokeWidth = r * 0.2f)
    }
    drawCircle(MineBody, radius = r, center = Offset(cx, cy))
    drawCircle(MineSpike, radius = r * 0.3f, center = Offset(cx - r * 0.3f, cy - r * 0.3f))
}

// ---------------------------------------------------------------------------
// Kartlar
// ---------------------------------------------------------------------------

@Composable
private fun StartCard(
    dailyMode: Boolean,
    daily: DalgicDaily?,
    freeBest: Int,
    onMode: (Boolean) -> Unit,
    onStart: () -> Unit,
    onExit: () -> Unit,
) {
    val exhausted = dailyMode && daily != null && daily.attempts >= DalgicViewModel.DAILY_ATTEMPTS
    OverlayCard {
        Text(
            text = stringResource(R.string.game_dalgic),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.dalgic_intro),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
        Text(
            text = stringResource(R.string.dalgic_rules),
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
                    text = stringResource(R.string.dalgic_daily_status_fmt, daily.attempts, DalgicViewModel.DAILY_ATTEMPTS, daily.best),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = stringResource(if (exhausted) R.string.dalgic_daily_exhausted else R.string.dalgic_daily_desc),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        } else if (freeBest > 0) {
            Text(
                text = stringResource(R.string.dalgic_best_fmt, freeBest),
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
                Text(stringResource(R.string.dalgic_start))
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
            Text(stringResource(R.string.dalgic_to_menu))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

@Composable
private fun OverCard(
    hud: DalgicHud,
    daily: Boolean,
    attemptsLeft: Int,
    isRecord: Boolean,
    onRestart: () -> Unit,
    onMenu: () -> Unit,
    onExit: () -> Unit,
) {
    val result = stringResource(R.string.dalgic_result_fmt, hud.rescued, hud.wave + 1)
    OverlayCard {
        Text(
            text = stringResource(R.string.dalgic_over_title),
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
                text = stringResource(R.string.dalgic_daily_exhausted),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        ShareButton(
            ShareContent(
                gameId = "dalgic",
                headline = stringResource(R.string.dalgic_share_fmt, hud.rescued, hud.score),
                details = listOf(result, modeShareLabel(daily, null)),
            ),
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text(
                when {
                    daily && attemptsLeft > 0 -> stringResource(R.string.dalgic_retry_fmt, attemptsLeft)
                    daily -> stringResource(R.string.play_free)
                    else -> stringResource(R.string.restart)
                },
            )
        }
        TextButton(onClick = onMenu, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.dalgic_to_menu))
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
