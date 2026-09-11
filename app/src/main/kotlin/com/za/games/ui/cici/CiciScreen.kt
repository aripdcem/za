package com.za.games.ui.cici

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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.za.games.cici.CiciHud
import com.za.games.cici.CiciStatus
import com.za.games.cici.CiciWorld
import com.za.games.cici.HazardKind
import com.za.games.cici.Mood
import com.za.games.cici.TreatKind
import com.za.games.platform.LocalZaHaptics
import com.za.games.platform.LocalZaSound
import com.za.games.platform.ShareContent
import com.za.games.ui.common.GameTopBar
import com.za.games.ui.common.OverlayCard
import com.za.games.ui.common.ScoreCard
import com.za.games.ui.common.ShareButton
import com.za.games.ui.common.formatScore
import com.za.games.ui.common.modeShareLabel
import java.util.Locale
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.isActive

private val SpaceTop = Color(0xFF0B1026)
private val SpaceBottom = Color(0xFF1E1B4B)
private val StarColor = Color(0xFFE0E7FF)
private val PlanetA = Color(0xFF6D28D9)
private val PlanetABand = Color(0xFFA78BFA)
private val PlanetB = Color(0xFFEA580C)
private val PlanetBBand = Color(0xFFFDBA74)
private val CiciWhite = Color(0xFFF8FAFC)
private val CiciShade = Color(0xFFCBD5E1)
private val CiciCheek = Color(0xFF7DD3FC)
private val CiciBeak = Color(0xFFF59E0B)
private val CiciEye = Color(0xFF0F172A)
private val HelmetFill = Color(0x55BAE6FD)
private val HelmetRim = Color(0xCCE0F2FE)
private val SeedColor = Color(0xFFD4A373)
private val SeedDark = Color(0xFF8B5E34)
private val WaterColor = Color(0xFF38BDF8)
private val WaterRim = Color(0xFF0369A1)
private val WaterShine = Color(0xFFE0F2FE)
private val HoneyColor = Color(0xFFF59E0B)
private val HoneyDark = Color(0xFFB45309)
private val HoneyShine = Color(0xFFFDE68A)
private val CatGray = Color(0xFF9CA3AF)
private val CatOrange = Color(0xFFF97316)
private val CatBlack = Color(0xFF1F2937)
private val CatOutline = Color(0xFFE5E7EB)
private val CatEye = Color(0xFF86EFAC)
private val CatNose = Color(0xFFFDA4AF)
private val BallColor = Color(0xFFEF4444)
private val BallShine = Color(0xFFFCA5A5)
private val BallRim = Color(0xFF7F1D1D)
private val JoyRing = Color(0xFFFDE68A)

/** Sürükleme katsayısı: 2B sürüklemede 1,3; arenayı geçmek bir başparmak hamlesi. */
private const val DRAG_GAIN = 1.3f
private const val STAR_COUNT = 70

@Composable
private fun hitLabel(kind: HazardKind): String = stringResource(
    when (kind) {
        HazardKind.CAT -> R.string.cici_hit_cat
        HazardKind.BALL -> R.string.cici_hit_ball
    },
)

@Composable
private fun moodLabel(mood: Mood): String = stringResource(
    when (mood) {
        Mood.HAPPY -> R.string.cici_mood_happy
        Mood.CALM -> R.string.cici_mood_calm
        Mood.BORED -> R.string.cici_mood_bored
    },
)

private fun clock(seconds: Int): String = String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)

@Composable
fun CiciScreen(
    highScore: Long,
    onScore: (Long) -> Unit,
    onExit: () -> Unit,
    viewModel: CiciViewModel = viewModel(),
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
    val fx = remember { CiciFx() }
    val fxTick = remember { mutableLongStateOf(0L) }
    fx.joyLabel = stringResource(R.string.cici_joy)
    fx.boredLabel = stringResource(R.string.cici_bored)
    val labels = HazardKind.entries.associateWith { hitLabel(it) }
    fx.hitLabel = { labels[it] ?: it.name }

    val latestScore by rememberUpdatedState(hud.score.toLong())
    val latestOnScore by rememberUpdatedState(onScore)
    DisposableEffect(Unit) {
        onDispose { latestOnScore(latestScore) }
    }
    LaunchedEffect(phase) {
        if (phase == CiciPhase.OVER) latestOnScore(hud.score.toLong())
    }
    LaunchedEffect(runId) { fx.reset() }
    LaunchedEffect(Unit) { viewModel.refresh() }
    LifecycleResumeEffect(Unit) {
        onPauseOrDispose { viewModel.pause() }
    }
    BackHandler {
        if (phase == CiciPhase.PLAYING) viewModel.pause() else onExit()
    }

    LaunchedEffect(phase, runId) {
        if (phase != CiciPhase.PLAYING && phase != CiciPhase.OVER) return@LaunchedEffect
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
            if (phase == CiciPhase.OVER && !fx.isBusy) break
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
    val attemptsLeft = CiciViewModel.DAILY_ATTEMPTS - (daily?.attempts ?: 0)
    val best = if (dailyMode) daily?.best ?: 0 else freeBest

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        GameTopBar(title = stringResource(R.string.game_cici), onExit = onExit) {
            if (phase == CiciPhase.PLAYING || phase == CiciPhase.PAUSED) {
                TextButton(onClick = viewModel::togglePause) {
                    Text(stringResource(if (phase == CiciPhase.PAUSED) R.string.resume else R.string.pause))
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
            ScoreCard(label = stringResource(R.string.cici_lives), value = if (hud.lives > 0) "♥".repeat(hud.lives) else "–", modifier = Modifier.weight(0.8f))
            ScoreCard(label = stringResource(R.string.cici_streak), value = "×${hud.streak}", modifier = Modifier.weight(0.7f))
            ScoreCard(label = stringResource(R.string.cici_best), value = formatScore(maxOf(best, hud.score).toLong()), modifier = Modifier.weight(1f))
        }

        StatusBar(hud = hud, visible = phase != CiciPhase.MENU)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            CiciCanvas(viewModel = viewModel, fx = fx, fxTick = fxTick, hud = hud, modifier = Modifier.fillMaxSize())
            when (phase) {
                CiciPhase.MENU -> StartCard(
                    dailyMode = dailyMode,
                    daily = daily,
                    freeBest = freeBest,
                    onMode = viewModel::setDailyMode,
                    onStart = startRun,
                    onExit = onExit,
                )
                CiciPhase.PAUSED -> PauseCard(
                    onResume = viewModel::resume,
                    onRestart = restartRun,
                    onMenu = viewModel::toMenu,
                    onExit = onExit,
                )
                CiciPhase.OVER -> OverCard(
                    hud = hud,
                    seeds = viewModel.world.caughtOf(TreatKind.SEED),
                    waters = viewModel.world.caughtOf(TreatKind.WATER),
                    honeys = viewModel.world.caughtOf(TreatKind.HONEY),
                    daily = dailyMode,
                    attemptsLeft = attemptsLeft,
                    isRecord = record || hud.score > previousBest,
                    onRestart = restartRun,
                    onMenu = viewModel::toMenu,
                    onExit = onExit,
                )
                CiciPhase.PLAYING -> Unit
            }
        }
    }
}

/** Bölüm adı, Cici'nin ruh hâli ve geçen süre. */
@Composable
private fun StatusBar(hud: CiciHud, visible: Boolean) {
    val alpha = if (visible) 1f else 0f
    val moodColor = when (hud.mood) {
        Mood.HAPPY -> MaterialTheme.colorScheme.primary
        Mood.CALM -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        Mood.BORED -> MaterialTheme.colorScheme.error
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.cici_chapter_space),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f * alpha),
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = moodLabel(hud.mood),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = moodColor.copy(alpha = moodColor.alpha * alpha),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = clock(hud.seconds),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f * alpha),
        )
    }
}

// ---------------------------------------------------------------------------
// Çizim
// ---------------------------------------------------------------------------

private fun spaceScale(width: Float, height: Float): Float =
    min(width / CiciWorld.WIDTH, height / CiciWorld.HEIGHT)

/** Yıldız konumları: dizinden türeyen sabit sözde-rastgele pay (0..1). */
private fun starCoord(i: Int): Float = ((i * 2654435761L) and 0xFFFFL).toFloat() / 65535f

@Composable
private fun CiciCanvas(
    viewModel: CiciViewModel,
    fx: CiciFx,
    fxTick: MutableLongState,
    hud: CiciHud,
    modifier: Modifier = Modifier,
) {
    val frame by viewModel.frame.collectAsStateWithLifecycle()
    val desc = stringResource(R.string.cici_board_desc_fmt, hud.score, hud.lives, hud.caught)
    val hint = stringResource(R.string.cici_drag_hint)
    val textMeasurer = rememberTextMeasurer()
    val textCache = remember { HashMap<String, TextLayoutResult>() }
    val path = remember { Path() }
    val space = remember { Brush.verticalGradient(0f to SpaceTop, 1f to SpaceBottom) }
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
                            val scale = spaceScale(size.width.toFloat(), size.height.toFloat())
                            if (scale > 0f) viewModel.drag(dx * DRAG_GAIN / scale, dy * DRAG_GAIN / scale)
                        }
                        change.consume()
                    }
                }
            },
    ) {
        val tick = frame + fxTick.longValue
        if (tick < 0L) return@Canvas
        drawSpace(viewModel.world, fx, path, space, textMeasurer, textCache, hint)
    }
}

private fun DrawScope.drawSpace(
    world: CiciWorld,
    fx: CiciFx,
    path: Path,
    space: Brush,
    textMeasurer: TextMeasurer,
    cache: HashMap<String, TextLayoutResult>,
    hint: String,
) {
    val w = size.width
    val h = size.height
    val s = spaceScale(w, h)
    val cw = CiciWorld.WIDTH * s
    val ch = CiciWorld.HEIGHT * s
    val ox = (w - cw) / 2f
    val oy = (h - ch) / 2f
    val frames = world.frames

    drawRect(space)
    translate(ox + fx.shakeX * s, oy + fx.shakeY * s) {
        // Yıldızlar (üç boy, göz kırpar) ve iki gezegen (yavaş salınım).
        for (i in 0 until STAR_COUNT) {
            val x = starCoord(i) * cw
            val y = starCoord(i + 1000) * ch
            val r = (1f + (i % 3)) * 0.0022f * s
            val a = 0.45f + 0.45f * sin(frames * 0.05f + i * 1.7f)
            drawCircle(StarColor.copy(alpha = a), radius = r, center = Offset(x, y))
        }
        val px = 0.82f * cw + sin(frames * 0.004f) * 0.01f * s
        val py = 0.17f * ch
        drawCircle(PlanetA, radius = 0.13f * s, center = Offset(px, py))
        drawOval(PlanetABand.copy(alpha = 0.55f), topLeft = Offset(px - 0.17f * s, py - 0.028f * s), size = Size(0.34f * s, 0.056f * s))
        drawCircle(PlanetA.copy(alpha = 0.35f), radius = 0.13f * s, center = Offset(px, py))
        val qx = 0.14f * cw
        val qy = 0.8f * ch + sin(frames * 0.003f + 1f) * 0.01f * s
        drawCircle(PlanetB, radius = 0.055f * s, center = Offset(qx, qy))
        drawOval(PlanetBBand.copy(alpha = 0.6f), topLeft = Offset(qx - 0.075f * s, qy - 0.012f * s), size = Size(0.15f * s, 0.024f * s))

        for (t in world.treats) if (t.alive) drawTreat(t.kind, t.x * s, t.y * s, t.kind.radius * s, t.t)
        for (b in world.balls) drawBall(b.x * s, b.y * s, CiciWorld.BALL_R * s)
        for (c in world.cats) if (c.alive) drawCat(path, c.x * s, c.y * s, CiciWorld.CAT_R * s, if (c.vx < 0f) -1 else 1, c.tint, c.t)

        if (fx.joyPulse > 0f) {
            val k = 1f - fx.joyPulse
            drawCircle(JoyRing.copy(alpha = fx.joyPulse * 0.6f), radius = CiciWorld.CICI_R * s * (1.4f + k * 1.6f), center = Offset(world.ciciX * s, world.ciciY * s), style = Stroke(width = 0.006f * s))
        }
        val blink = world.invuln > 0f && (frames / 4) % 2 == 0
        if (!blink) drawCici(path, world.ciciX * s, world.ciciY * s, CiciWorld.CICI_R * s, world.facing, world.mood, frames, world.status == CiciStatus.OVER)

        for (p in fx.particles) {
            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            val cx = p.x * s
            val cy = p.y * s
            val r = p.size * s
            when (p.shape) {
                CiciFx.Shape.DOT -> drawCircle(p.color.copy(alpha = alpha), r, Offset(cx, cy))
                CiciFx.Shape.HEART -> drawHeart(path, cx, cy, r, p.color.copy(alpha = alpha))
                CiciFx.Shape.FEATHER -> rotate(degrees = p.spin * 57f, pivot = Offset(cx, cy)) {
                    drawOval(p.color.copy(alpha = alpha), topLeft = Offset(cx - r * 0.35f, cy - r), size = Size(r * 0.7f, r * 2f))
                }
            }
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
        if (frames in 1..240 && world.status == CiciStatus.RUNNING) {
            val layout = cache.getOrPut("hint|$hint") {
                textMeasurer.measure(
                    AnnotatedString(hint),
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center),
                )
            }
            val alpha = if (frames > 180) (240 - frames) / 60f else 1f
            drawText(layout, topLeft = Offset(cw / 2f - layout.size.width / 2f, 0.4f * s - layout.size.height / 2f), alpha = alpha * 0.85f)
        }
    }
    if (fx.flash > 0f) drawRect(fx.flashColor.copy(alpha = 0.3f * fx.flash))
}

/** Cici: beyaz muhabbet kuşu, saydam kaskla. Ruh hâli gözde okunur (mutlu: kapalı gülen göz, sıkkın: yarı kapalı). */
private fun DrawScope.drawCici(path: Path, cx: Float, cy: Float, r: Float, facing: Int, mood: Mood, frames: Int, over: Boolean) {
    val f = facing.toFloat()
    val flap = sin(frames * (if (mood == Mood.HAPPY) 0.6f else 0.3f)) * r * 0.25f
    // Kuyruk.
    drawLine(CiciShade, Offset(cx - f * r * 1.05f, cy + r * 0.25f), Offset(cx - f * r * 2.2f, cy + r * 0.95f), strokeWidth = r * 0.3f, cap = StrokeCap.Round)
    drawLine(CiciWhite, Offset(cx - f * r * 1.05f, cy + r * 0.1f), Offset(cx - f * r * 2.1f, cy + r * 0.6f), strokeWidth = r * 0.24f, cap = StrokeCap.Round)
    // Gövde ve kanat.
    drawOval(CiciWhite, topLeft = Offset(cx - r * 1.2f, cy - r * 0.55f), size = Size(r * 2.4f, r * 1.7f))
    drawOval(CiciShade, topLeft = Offset(cx - r * 0.95f - f * r * 0.1f, cy - r * 0.35f + flap), size = Size(r * 1.4f, r * 0.8f))
    // Baş, yanak, gaga.
    val hx = cx + f * r * 0.55f
    val hy = cy - r * 0.85f
    drawCircle(CiciWhite, radius = r * 0.85f, center = Offset(hx, hy))
    drawCircle(CiciCheek, radius = r * 0.16f, center = Offset(hx + f * r * 0.42f, hy + r * 0.3f))
    path.reset()
    path.moveTo(hx + f * r * 0.55f, hy - r * 0.05f)
    path.lineTo(hx + f * r * 1.05f, hy + r * 0.14f)
    path.lineTo(hx + f * r * 0.5f, hy + r * 0.4f)
    path.close()
    drawPath(path, CiciBeak)
    // Göz.
    val ex = hx + f * r * 0.24f
    val ey = hy - r * 0.18f
    when {
        over -> {
            drawLine(CiciEye, Offset(ex - r * 0.14f, ey - r * 0.14f), Offset(ex + r * 0.14f, ey + r * 0.14f), strokeWidth = r * 0.08f, cap = StrokeCap.Round)
            drawLine(CiciEye, Offset(ex - r * 0.14f, ey + r * 0.14f), Offset(ex + r * 0.14f, ey - r * 0.14f), strokeWidth = r * 0.08f, cap = StrokeCap.Round)
        }
        mood == Mood.HAPPY -> drawArc(
            CiciEye,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(ex - r * 0.2f, ey - r * 0.1f),
            size = Size(r * 0.4f, r * 0.32f),
            style = Stroke(width = r * 0.09f, cap = StrokeCap.Round),
        )
        mood == Mood.BORED -> {
            drawCircle(CiciEye, radius = r * 0.14f, center = Offset(ex, ey))
            drawRect(CiciWhite, topLeft = Offset(ex - r * 0.16f, ey - r * 0.16f), size = Size(r * 0.32f, r * 0.16f))
            drawLine(CiciEye, Offset(ex - r * 0.16f, ey), Offset(ex + r * 0.16f, ey), strokeWidth = r * 0.05f)
        }
        else -> {
            drawCircle(CiciEye, radius = r * 0.14f, center = Offset(ex, ey))
            drawCircle(Color.White, radius = r * 0.045f, center = Offset(ex + f * r * 0.04f, ey - r * 0.05f))
        }
    }
    // Kask: başın çevresinde saydam küre ve parıltı.
    drawCircle(HelmetFill, radius = r * 1.25f, center = Offset(hx, hy))
    drawCircle(HelmetRim, radius = r * 1.25f, center = Offset(hx, hy), style = Stroke(width = r * 0.08f))
    drawArc(
        Color.White.copy(alpha = 0.7f),
        startAngle = 205f,
        sweepAngle = 45f,
        useCenter = false,
        topLeft = Offset(hx - r * 1.02f, hy - r * 1.02f),
        size = Size(r * 2.04f, r * 2.04f),
        style = Stroke(width = r * 0.07f, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawTreat(kind: TreatKind, cx: Float, cy: Float, r: Float, t: Float) {
    when (kind) {
        TreatKind.SEED -> {
            val spin = sin(t * 2f) * r * 0.1f
            for ((dx, dy) in listOf(-0.45f to 0.15f, 0.4f to 0.2f, 0f to -0.45f)) {
                val ox = cx + dx * r + spin
                val oy = cy + dy * r
                drawOval(SeedColor, topLeft = Offset(ox - r * 0.42f, oy - r * 0.3f), size = Size(r * 0.84f, r * 0.6f))
                drawOval(SeedDark, topLeft = Offset(ox - r * 0.42f, oy - r * 0.3f), size = Size(r * 0.84f, r * 0.6f), style = Stroke(width = r * 0.08f))
            }
        }
        TreatKind.WATER -> {
            drawCircle(WaterColor, radius = r, center = Offset(cx, cy))
            drawCircle(WaterRim, radius = r, center = Offset(cx, cy), style = Stroke(width = r * 0.14f))
            drawCircle(WaterShine, radius = r * 0.28f, center = Offset(cx - r * 0.35f, cy - r * 0.35f))
        }
        TreatKind.HONEY -> {
            drawLine(HoneyDark, Offset(cx + r * 1.1f, cy), Offset(cx + r * 1.9f, cy), strokeWidth = r * 0.22f, cap = StrokeCap.Round)
            drawRoundRect(HoneyColor, topLeft = Offset(cx - r * 1.3f, cy - r * 0.5f), size = Size(r * 2.5f, r * 1f), cornerRadius = CornerRadius(r * 0.5f, r * 0.5f))
            drawRoundRect(HoneyDark, topLeft = Offset(cx - r * 1.3f, cy - r * 0.5f), size = Size(r * 2.5f, r * 1f), cornerRadius = CornerRadius(r * 0.5f, r * 0.5f), style = Stroke(width = r * 0.1f))
            drawCircle(HoneyShine, radius = r * 0.2f, center = Offset(cx - r * 0.6f, cy - r * 0.15f))
            drawCircle(HoneyDark, radius = r * 0.13f, center = Offset(cx + r * 0.35f, cy + r * 0.12f))
            drawCircle(HoneyDark, radius = r * 0.1f, center = Offset(cx - r * 0.1f, cy + r * 0.2f))
        }
    }
}

private fun DrawScope.drawBall(cx: Float, cy: Float, r: Float) {
    drawCircle(BallColor, radius = r, center = Offset(cx, cy))
    drawCircle(BallRim, radius = r, center = Offset(cx, cy), style = Stroke(width = r * 0.14f))
    drawCircle(BallShine, radius = r * 0.28f, center = Offset(cx - r * 0.32f, cy - r * 0.34f))
}

/**
 * Uzay kedisi: kasklı yuvarlak baş, kulaklar, yeşil gözler; koyu gövdeye açık kontur.
 * Cihaz ölçümü (docs/oyun-testi.md): siyah gövde uzaya karşı 1,2:1, kediyi 1,4 dp'lik
 * kontur taşıyordu; kontur kalınlaştı (r × 0,12 ≈ 2,5 dp) ve siyah kedi smokin desenli
 * (açık burun ve göğüs), gövdesi de zeminden ayrılsın diye.
 */
private fun DrawScope.drawCat(path: Path, cx: Float, cy: Float, r: Float, dir: Int, tint: Int, t: Float) {
    val body = when (tint) {
        0 -> CatGray
        1 -> CatOrange
        else -> CatBlack
    }
    val tuxedo = tint == 2
    val outline = r * 0.12f
    val f = dir.toFloat()
    val y = cy + sin(t * 3f) * r * 0.1f
    // Kuyruk ve gövde (arkada).
    drawLine(CatOutline, Offset(cx - f * r * 1.9f, y + r * 0.1f), Offset(cx - f * r * 2.6f, y - r * 0.5f + sin(t * 4f) * r * 0.3f), strokeWidth = r * 0.24f + outline, cap = StrokeCap.Round)
    drawLine(body, Offset(cx - f * r * 1.9f, y + r * 0.1f), Offset(cx - f * r * 2.6f, y - r * 0.5f + sin(t * 4f) * r * 0.3f), strokeWidth = r * 0.24f, cap = StrokeCap.Round)
    val bx = cx - f * r * 1.4f
    drawOval(body, topLeft = Offset(bx - r * 0.8f, y - r * 0.42f), size = Size(r * 1.6f, r * 0.9f))
    if (tuxedo) drawOval(CatOutline, topLeft = Offset(bx - r * 0.45f + f * r * 0.2f, y - r * 0.1f), size = Size(r * 0.8f, r * 0.5f))
    drawOval(CatOutline, topLeft = Offset(bx - r * 0.8f, y - r * 0.42f), size = Size(r * 1.6f, r * 0.9f), style = Stroke(width = outline))
    // Kulaklar.
    for (side in intArrayOf(-1, 1)) {
        path.reset()
        path.moveTo(cx + side * r * 0.75f, y - r * 0.45f)
        path.lineTo(cx + side * r * 0.6f, y - r * 1.3f)
        path.lineTo(cx + side * r * 0.12f, y - r * 0.8f)
        path.close()
        drawPath(path, body)
        drawPath(path, CatOutline, style = Stroke(width = outline))
    }
    // Baş; smokin kedide açık burun yaması.
    drawCircle(body, radius = r, center = Offset(cx, y))
    if (tuxedo) drawOval(CatOutline, topLeft = Offset(cx - r * 0.5f, y - r * 0.02f), size = Size(r * 1f, r * 0.62f))
    drawCircle(CatOutline, radius = r, center = Offset(cx, y), style = Stroke(width = outline))
    // Gözler ve dikey göz bebekleri.
    for (side in intArrayOf(-1, 1)) {
        val ex = cx + side * r * 0.38f
        drawOval(CatEye, topLeft = Offset(ex - r * 0.18f, y - r * 0.42f), size = Size(r * 0.36f, r * 0.42f))
        drawOval(CiciEye, topLeft = Offset(ex - r * 0.06f, y - r * 0.38f), size = Size(r * 0.12f, r * 0.34f))
    }
    // Burun ve bıyıklar.
    drawCircle(CatNose, radius = r * 0.1f, center = Offset(cx, y + r * 0.15f))
    for (side in intArrayOf(-1, 1)) {
        for (k in -1..1) {
            drawLine(CatOutline.copy(alpha = 0.8f), Offset(cx + side * r * 0.2f, y + r * 0.2f + k * r * 0.1f), Offset(cx + side * r * 0.95f, y + r * 0.1f + k * r * 0.22f), strokeWidth = r * 0.04f)
        }
    }
    // Kask.
    drawCircle(HelmetFill, radius = r * 1.5f, center = Offset(cx, y - r * 0.15f))
    drawCircle(HelmetRim, radius = r * 1.5f, center = Offset(cx, y - r * 0.15f), style = Stroke(width = r * 0.06f))
}

private fun DrawScope.drawHeart(path: Path, x: Float, y: Float, s: Float, color: Color) {
    path.reset()
    path.moveTo(x, y + s * 0.9f)
    path.cubicTo(x - s * 1.6f, y - s * 0.3f, x - s * 0.6f, y - s * 1.3f, x, y - s * 0.4f)
    path.cubicTo(x + s * 0.6f, y - s * 1.3f, x + s * 1.6f, y - s * 0.3f, x, y + s * 0.9f)
    path.close()
    drawPath(path, color)
}

// ---------------------------------------------------------------------------
// Kartlar
// ---------------------------------------------------------------------------

@Composable
private fun StartCard(
    dailyMode: Boolean,
    daily: CiciDaily?,
    freeBest: Int,
    onMode: (Boolean) -> Unit,
    onStart: () -> Unit,
    onExit: () -> Unit,
) {
    val exhausted = dailyMode && daily != null && daily.attempts >= CiciViewModel.DAILY_ATTEMPTS
    OverlayCard {
        Text(
            text = stringResource(R.string.game_cici),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            ModeChip(stringResource(R.string.cici_chapter_space), true, Modifier.fillMaxWidth(0.7f)) {}
        }
        Text(
            text = stringResource(R.string.cici_intro),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
        Text(
            text = stringResource(R.string.cici_rules),
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
                    text = stringResource(R.string.cici_daily_status_fmt, daily.attempts, CiciViewModel.DAILY_ATTEMPTS, daily.best),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = stringResource(if (exhausted) R.string.cici_daily_exhausted else R.string.cici_daily_desc),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        } else if (freeBest > 0) {
            Text(
                text = stringResource(R.string.cici_best_fmt, freeBest),
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
                Text(stringResource(R.string.cici_start))
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
            Text(stringResource(R.string.cici_to_menu))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

@Composable
private fun OverCard(
    hud: CiciHud,
    seeds: Int,
    waters: Int,
    honeys: Int,
    daily: Boolean,
    attemptsLeft: Int,
    isRecord: Boolean,
    onRestart: () -> Unit,
    onMenu: () -> Unit,
    onExit: () -> Unit,
) {
    val result = stringResource(R.string.cici_result_fmt, seeds, waters, honeys, hud.bestStreak, clock(hud.seconds))
    OverlayCard {
        Text(
            text = stringResource(R.string.cici_over_title),
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
                text = stringResource(R.string.cici_daily_exhausted),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        ShareButton(
            ShareContent(
                gameId = "cici",
                headline = stringResource(R.string.cici_share_fmt, hud.score, hud.caught),
                details = listOf(result, modeShareLabel(daily, null)),
            ),
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text(
                when {
                    daily && attemptsLeft > 0 -> stringResource(R.string.cici_retry_fmt, attemptsLeft)
                    daily -> stringResource(R.string.play_free)
                    else -> stringResource(R.string.restart)
                },
            )
        }
        TextButton(onClick = onMenu, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.cici_to_menu))
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
