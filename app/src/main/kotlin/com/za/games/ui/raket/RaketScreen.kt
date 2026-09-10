package com.za.games.ui.raket

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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.za.games.R
import com.za.games.platform.LocalZaHaptics
import com.za.games.platform.LocalZaSound
import com.za.games.platform.ShareContent
import com.za.games.raket.Paddle
import com.za.games.raket.RaketHud
import com.za.games.raket.RaketMode
import com.za.games.raket.RaketStatus
import com.za.games.raket.RaketWorld
import com.za.games.raket.Side
import com.za.games.ui.common.GameTopBar
import com.za.games.ui.common.OverlayCard
import com.za.games.ui.common.ScoreCard
import com.za.games.ui.common.ShareButton
import com.za.games.ui.common.modeShareLabel
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.isActive

private val TableTop = Color(0xFF0B2A3A)
private val TableBottom = Color(0xFF123D52)
private val CourtLine = Color(0x8CE2E8F0)
private val CentreLine = Color(0x66E2E8F0)
private val BackWall = Color(0xFFE2E8F0)
private val BottomPaddle = Color(0xFF5EEAD4)
private val TopPaddle = Color(0xFFFB7185)
private val PaddleShade = Color(0x55000000)
private val BallColor = Color(0xFFFDE68A)
private val BallCore = Color(0xFFFFFBEB)
private val Digit = Color(0x2EE2E8F0)
private val HintColor = Color(0xB3E2E8F0)
private val LoseTint = Color(0xFFFB7185)
private val ServeRing = Color(0x99FDE68A)

/**
 * Sürükleme katsayısı: 1,0 birebir. Kortu uçtan uca geçmek için gereken
 * parmak yolu 1,25 ile bir başparmak hamlesine iner (Filo'daki 1,35'ten
 * biraz düşük; raket vuruş noktası ince ayar ister).
 */
private const val DRAG_GAIN = 1.25f

@Composable
fun RaketScreen(
    highScore: Long,
    onScore: (Long) -> Unit,
    onExit: () -> Unit,
    viewModel: RaketViewModel = viewModel(),
) {
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val level by viewModel.level.collectAsStateWithLifecycle()
    val wallDaily by viewModel.wallDaily.collectAsStateWithLifecycle()
    val daily by viewModel.daily.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val wallBest by viewModel.wallBest.collectAsStateWithLifecycle()
    val hud by viewModel.hud.collectAsStateWithLifecycle()
    val runId by viewModel.runId.collectAsStateWithLifecycle()
    val record by viewModel.record.collectAsStateWithLifecycle()
    val haptics = LocalZaHaptics.current
    val sound = LocalZaSound.current
    val fx = remember { RaketFx() }
    val fxTick = remember { mutableLongStateOf(0L) }

    // Ana menü rekoru: en uzun ralli (tüm modlar).
    val latestBest by rememberUpdatedState(hud.bestRally.toLong())
    val latestOnScore by rememberUpdatedState(onScore)
    DisposableEffect(Unit) {
        onDispose { latestOnScore(latestBest) }
    }
    LaunchedEffect(phase) {
        if (phase == RaketPhase.OVER) latestOnScore(hud.bestRally.toLong())
    }
    LaunchedEffect(runId) { fx.reset() }
    LaunchedEffect(Unit) { viewModel.refresh() }
    LifecycleResumeEffect(Unit) {
        onPauseOrDispose { viewModel.pause() }
    }
    BackHandler {
        if (phase == RaketPhase.PLAYING) viewModel.pause() else onExit()
    }

    LaunchedEffect(phase, runId) {
        if (phase != RaketPhase.PLAYING && phase != RaketPhase.OVER) return@LaunchedEffect
        var last = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = now - last
                    val world = viewModel.world
                    for (event in viewModel.advance(dt)) {
                        fx.onEvent(event, world, sound, haptics)
                    }
                    fx.update(dt / 1_000_000_000f, world)
                    fxTick.longValue += 1
                }
                last = now
            }
            if (phase == RaketPhase.OVER && !fx.isBusy) break
        }
    }

    var previousBest by remember { mutableLongStateOf(highScore) }
    val startRun = {
        previousBest = maxOf(previousBest, hud.bestRally.toLong())
        viewModel.start()
    }
    val restartRun = {
        previousBest = maxOf(previousBest, hud.bestRally.toLong())
        viewModel.restart()
    }
    val courtMode = if (phase == RaketPhase.MENU) mode else viewModel.world.mode
    val storedBest = if (wallDaily) daily?.best ?: 0 else wallBest

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        GameTopBar(title = stringResource(R.string.game_raket), onExit = onExit) {
            if (phase == RaketPhase.PLAYING || phase == RaketPhase.PAUSED) {
                TextButton(onClick = viewModel::togglePause) {
                    Text(stringResource(if (phase == RaketPhase.PAUSED) R.string.resume else R.string.pause))
                }
            }
        }

        ScoreRow(mode = courtMode, hud = hud, best = maxOf(storedBest, hud.rally))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            RaketCanvas(viewModel = viewModel, fx = fx, fxTick = fxTick, hud = hud, mode = courtMode, modifier = Modifier.fillMaxSize())
            when (phase) {
                RaketPhase.MENU -> StartCard(
                    mode = mode,
                    level = level,
                    wallDaily = wallDaily,
                    daily = daily,
                    records = records,
                    wallBest = wallBest,
                    onMode = viewModel::setMode,
                    onLevel = viewModel::setLevel,
                    onWallDaily = viewModel::setWallDaily,
                    onStart = startRun,
                    onExit = onExit,
                )
                RaketPhase.PAUSED -> PauseCard(
                    onResume = viewModel::resume,
                    onRestart = restartRun,
                    onMenu = viewModel::toMenu,
                    onExit = onExit,
                )
                RaketPhase.OVER -> OverCard(
                    mode = viewModel.world.mode,
                    level = viewModel.world.level,
                    hud = hud,
                    hits = viewModel.world.hits,
                    wallDaily = wallDaily,
                    isRecord = record || hud.bestRally > previousBest,
                    onRestart = restartRun,
                    onMenu = viewModel::toMenu,
                    onExit = onExit,
                )
                RaketPhase.PLAYING -> Unit
            }
        }
    }
}

@Composable
private fun ScoreRow(mode: RaketMode, hud: RaketHud, best: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (mode) {
            RaketMode.SOLO -> {
                ScoreCard(label = stringResource(R.string.raket_you), value = hud.bottom.toString(), modifier = Modifier.weight(1f), highlight = true)
                ScoreCard(label = stringResource(R.string.raket_cpu), value = hud.top.toString(), modifier = Modifier.weight(1f))
                ScoreCard(label = stringResource(R.string.raket_rally), value = hud.rally.toString(), modifier = Modifier.weight(0.8f))
            }
            RaketMode.DUO -> {
                ScoreCard(label = stringResource(R.string.raket_bottom), value = hud.bottom.toString(), modifier = Modifier.weight(1f), highlight = true)
                ScoreCard(label = stringResource(R.string.raket_top), value = hud.top.toString(), modifier = Modifier.weight(1f))
                ScoreCard(label = stringResource(R.string.raket_rally), value = hud.rally.toString(), modifier = Modifier.weight(0.8f))
            }
            RaketMode.WALL -> {
                ScoreCard(label = stringResource(R.string.raket_rally), value = hud.rally.toString(), modifier = Modifier.weight(1f), highlight = true)
                ScoreCard(label = stringResource(R.string.raket_best), value = best.toString(), modifier = Modifier.weight(1f))
                ScoreCard(label = stringResource(R.string.raket_speed), value = "%${(hud.speed * 100f).roundToInt()}", modifier = Modifier.weight(0.8f))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Çizim
// ---------------------------------------------------------------------------

private fun courtScale(width: Float, height: Float): Float =
    min(width / RaketWorld.WIDTH, height / RaketWorld.HEIGHT)

@Composable
private fun RaketCanvas(
    viewModel: RaketViewModel,
    fx: RaketFx,
    fxTick: MutableLongState,
    hud: RaketHud,
    mode: RaketMode,
    modifier: Modifier = Modifier,
) {
    val frame by viewModel.frame.collectAsStateWithLifecycle()
    val desc = if (mode == RaketMode.WALL) {
        stringResource(R.string.raket_wall_desc_fmt, hud.rally)
    } else {
        stringResource(R.string.raket_board_desc_fmt, hud.bottom, hud.top, hud.rally)
    }
    val hint = stringResource(if (mode == RaketMode.DUO) R.string.raket_duo_drag_hint else R.string.raket_drag_hint)
    val textMeasurer = rememberTextMeasurer()
    val textCache = remember { HashMap<String, TextLayoutResult>() }
    // Gradyan her karede yeniden kurulmaz (bkz. FiloScreen).
    val table = remember { Brush.verticalGradient(listOf(TableTop, TableBottom)) }
    val dash = remember { PathEffect.dashPathEffect(floatArrayOf(18f, 14f), 0f) }
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .semantics { contentDescription = desc }
            .pointerInput(viewModel, mode) {
                // Her parmak kendi raketini sürer: iki kişide üst yarıya basan üst
                // raketi, alt yarıya basan alt raketi alır; öbür modlarda her dokunuş
                // alt raketindir. Olaylar doğrudan okunur ki ilk piksel de işlesin
                // (Filo'daki 8 dp tolerans bulgusu).
                val sides = HashMap<PointerId, Side>()
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val scale = courtScale(size.width.toFloat(), size.height.toFloat())
                        for (change in event.changes) {
                            if (change.changedToDownIgnoreConsumed()) {
                                sides[change.id] = if (mode == RaketMode.DUO && change.position.y < size.height / 2f) Side.TOP else Side.BOTTOM
                            } else if (change.pressed) {
                                val side = sides[change.id] ?: Side.BOTTOM
                                val dx = change.position.x - change.previousPosition.x
                                if (dx != 0f && scale > 0f) viewModel.move(side, dx * DRAG_GAIN / scale)
                            } else {
                                sides.remove(change.id)
                            }
                            change.consume()
                        }
                    }
                }
            },
    ) {
        // Kare sayaçları okunur ki her adımda yeniden çizilsin.
        val tick = frame + fxTick.longValue
        if (tick < 0L) return@Canvas
        drawCourt(viewModel.world, fx, mode, table, dash, textMeasurer, textCache, hint)
    }
}

private fun DrawScope.drawCourt(
    world: RaketWorld,
    fx: RaketFx,
    mode: RaketMode,
    table: Brush,
    dash: PathEffect,
    textMeasurer: TextMeasurer,
    cache: HashMap<String, TextLayoutResult>,
    hint: String,
) {
    val w = size.width
    val h = size.height
    val s = courtScale(w, h)
    val cw = RaketWorld.WIDTH * s
    val ch = RaketWorld.HEIGHT * s
    val ox = (w - cw) / 2f
    val oy = (h - ch) / 2f

    drawRect(table)
    translate(ox, oy) {
        // Sayıyı kaybeden yarı kısa süre kızarır.
        val flashSide = fx.flashSide
        if (flashSide != null && fx.flashTimer > 0f) {
            val alpha = 0.22f * (fx.flashTimer / RaketFx.FLASH_TIME)
            val top = if (flashSide == Side.TOP) 0f else ch / 2f
            drawRect(LoseTint.copy(alpha = alpha), topLeft = Offset(0f, top), size = Size(cw, ch / 2f))
        }
        // Kenar çizgileri ve orta çizgi ya da arka duvar.
        val line = 0.008f * s
        drawLine(CourtLine, Offset(line / 2f, 0f), Offset(line / 2f, ch), strokeWidth = line)
        drawLine(CourtLine, Offset(cw - line / 2f, 0f), Offset(cw - line / 2f, ch), strokeWidth = line)
        if (mode == RaketMode.WALL) {
            drawRect(BackWall, topLeft = Offset(0f, 0f), size = Size(cw, 0.02f * s))
        } else {
            drawLine(CentreLine, Offset(0f, ch / 2f), Offset(cw, ch / 2f), strokeWidth = line * 0.8f, pathEffect = dash)
        }
        // Büyük soluk rakamlar; iki kişide üst oyuncununki ona dönük.
        val digitSize = 0.24f * s
        if (mode == RaketMode.WALL) {
            drawDigits(world.hits.toString(), cw / 2f, ch * 0.3f, digitSize, textMeasurer, cache, rotated = false)
        } else {
            drawDigits(world.scoreBottom.toString(), cw / 2f, ch * 0.74f, digitSize, textMeasurer, cache, rotated = false)
            drawDigits(world.scoreTop.toString(), cw / 2f, ch * 0.26f, digitSize, textMeasurer, cache, rotated = mode == RaketMode.DUO)
        }
        // Raketler.
        drawPaddle(world.bottom, s, BottomPaddle)
        if (world.hasTopPaddle) drawPaddle(world.top, s, TopPaddle)
        // Top izi ve top.
        val r = RaketWorld.BALL_R * s
        for (i in 0 until fx.trailCount) {
            val k = 1f - (i + 1f) / (RaketFx.TRAIL + 1f)
            drawCircle(BallColor.copy(alpha = 0.35f * k), radius = r * (0.45f + 0.5f * k), center = Offset(fx.trailX(i) * s, fx.trailY(i) * s))
        }
        val bx = world.ball.x * s
        val by = world.ball.y * s
        if (world.status == RaketStatus.SERVING) {
            val pulse = 0.5f + 0.5f * sin(world.frames * 0.25f)
            drawCircle(ServeRing.copy(alpha = 0.25f + 0.35f * pulse), radius = r * (2.2f + 0.6f * pulse), center = Offset(bx, by), style = Stroke(width = r * 0.35f))
            // Servisin yönü: küçük ok.
            val dir = if (world.serveTo == Side.TOP) -1f else 1f
            val tip = by + dir * r * 4.2f
            val base = by + dir * r * 2.9f
            drawLine(ServeRing, Offset(bx, base), Offset(bx, tip), strokeWidth = r * 0.3f)
            drawLine(ServeRing, Offset(bx - r * 0.7f, tip - dir * r * 0.8f), Offset(bx, tip), strokeWidth = r * 0.3f)
            drawLine(ServeRing, Offset(bx + r * 0.7f, tip - dir * r * 0.8f), Offset(bx, tip), strokeWidth = r * 0.3f)
        }
        if (world.status != RaketStatus.OVER || mode != RaketMode.WALL) {
            drawCircle(BallColor.copy(alpha = 0.35f), radius = r * 1.6f, center = Offset(bx, by))
            drawCircle(BallColor, radius = r, center = Offset(bx, by))
            drawCircle(BallCore, radius = r * 0.45f, center = Offset(bx - r * 0.25f, by - r * 0.25f))
        }
        // İlk servis ipucu.
        if (world.frames > 0 && fx.hintAlpha > 0f) {
            drawHint(hint, cw / 2f, ch * 0.62f, s, textMeasurer, cache, fx.hintAlpha, rotated = false)
            if (mode == RaketMode.DUO) drawHint(hint, cw / 2f, ch * 0.38f, s, textMeasurer, cache, fx.hintAlpha, rotated = true)
        }
    }
}

private fun DrawScope.drawPaddle(p: Paddle, s: Float, color: Color) {
    val pw = p.width * s
    val ph = RaketWorld.PADDLE_H * s
    val left = p.left * s
    val top = p.y * s - ph / 2f
    drawRoundRect(PaddleShade, topLeft = Offset(left + ph * 0.15f, top + ph * 0.25f), size = Size(pw, ph), cornerRadius = CornerRadius(ph / 2f, ph / 2f))
    drawRoundRect(color, topLeft = Offset(left, top), size = Size(pw, ph), cornerRadius = CornerRadius(ph / 2f, ph / 2f))
    if (p.flash > 0f) {
        val a = (p.flash / RaketWorld.FLASH_TIME).coerceIn(0f, 1f)
        drawRoundRect(Color.White.copy(alpha = 0.7f * a), topLeft = Offset(left, top), size = Size(pw, ph), cornerRadius = CornerRadius(ph / 2f, ph / 2f))
    }
}

private fun DrawScope.drawDigits(
    text: String,
    cx: Float,
    cy: Float,
    px: Float,
    textMeasurer: TextMeasurer,
    cache: HashMap<String, TextLayoutResult>,
    rotated: Boolean,
) {
    val layout = cache.getOrPut("digit|$text|${px.toInt()}") {
        textMeasurer.measure(
            AnnotatedString(text),
            style = TextStyle(fontSize = px.toSp(), fontWeight = FontWeight.Black, color = Digit, textAlign = TextAlign.Center),
        )
    }
    val topLeft = Offset(cx - layout.size.width / 2f, cy - layout.size.height / 2f)
    if (rotated) {
        rotate(180f, pivot = Offset(cx, cy)) { drawText(layout, topLeft = topLeft) }
    } else {
        drawText(layout, topLeft = topLeft)
    }
}

private fun DrawScope.drawHint(
    text: String,
    cx: Float,
    cy: Float,
    s: Float,
    textMeasurer: TextMeasurer,
    cache: HashMap<String, TextLayoutResult>,
    alpha: Float,
    rotated: Boolean,
) {
    val px = 0.045f * s
    val layout = cache.getOrPut("hint|$text|${px.toInt()}") {
        textMeasurer.measure(
            AnnotatedString(text),
            style = TextStyle(fontSize = px.toSp(), fontWeight = FontWeight.Bold, color = HintColor, textAlign = TextAlign.Center),
        )
    }
    val topLeft = Offset(cx - layout.size.width / 2f, cy - layout.size.height / 2f)
    if (rotated) {
        rotate(180f, pivot = Offset(cx, cy)) { drawText(layout, topLeft = topLeft, alpha = alpha) }
    } else {
        drawText(layout, topLeft = topLeft, alpha = alpha)
    }
}

// ---------------------------------------------------------------------------
// Kartlar
// ---------------------------------------------------------------------------

@Composable
private fun StartCard(
    mode: RaketMode,
    level: Int,
    wallDaily: Boolean,
    daily: RaketDaily?,
    records: List<RaketRecord>,
    wallBest: Int,
    onMode: (RaketMode) -> Unit,
    onLevel: (Int) -> Unit,
    onWallDaily: (Boolean) -> Unit,
    onStart: () -> Unit,
    onExit: () -> Unit,
) {
    OverlayCard {
        Text(
            text = stringResource(R.string.game_raket),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.raket_intro),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeChip(stringResource(R.string.raket_mode_solo), mode == RaketMode.SOLO, Modifier.weight(1f)) { onMode(RaketMode.SOLO) }
            ModeChip(stringResource(R.string.raket_mode_duo), mode == RaketMode.DUO, Modifier.weight(1f)) { onMode(RaketMode.DUO) }
            ModeChip(stringResource(R.string.raket_mode_wall), mode == RaketMode.WALL, Modifier.weight(1f)) { onMode(RaketMode.WALL) }
        }
        when (mode) {
            RaketMode.SOLO -> {
                Text(
                    text = stringResource(R.string.difficulty_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeChip(stringResource(R.string.difficulty_easy), level == 0, Modifier.weight(1f)) { onLevel(0) }
                    ModeChip(stringResource(R.string.difficulty_medium), level == 1, Modifier.weight(1f)) { onLevel(1) }
                    ModeChip(stringResource(R.string.difficulty_hard), level == 2, Modifier.weight(1f)) { onLevel(2) }
                }
                val rec = records.getOrNull(level)
                Text(
                    text = if (rec != null && rec.wins + rec.losses > 0) {
                        stringResource(R.string.raket_record_fmt, rec.wins, rec.losses)
                    } else {
                        stringResource(R.string.raket_solo_hint)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            RaketMode.DUO -> Text(
                text = stringResource(R.string.raket_duo_hint),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            RaketMode.WALL -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeChip(stringResource(R.string.mode_daily), wallDaily, Modifier.weight(1f)) { onWallDaily(true) }
                    ModeChip(stringResource(R.string.mode_free), !wallDaily, Modifier.weight(1f)) { onWallDaily(false) }
                }
                val best = if (wallDaily) daily?.best ?: 0 else wallBest
                if (best > 0) {
                    Text(
                        text = stringResource(if (wallDaily) R.string.raket_daily_status_fmt else R.string.raket_best_fmt, best),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = stringResource(if (wallDaily) R.string.raket_daily_desc else R.string.raket_wall_hint),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.raket_start))
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
            Text(stringResource(R.string.raket_to_menu))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

@Composable
private fun OverCard(
    mode: RaketMode,
    level: Int,
    hud: RaketHud,
    hits: Int,
    wallDaily: Boolean,
    isRecord: Boolean,
    onRestart: () -> Unit,
    onMenu: () -> Unit,
    onExit: () -> Unit,
) {
    val won = hud.winner == Side.BOTTOM
    val title = when (mode) {
        RaketMode.SOLO -> stringResource(if (won) R.string.raket_won else R.string.raket_lost)
        RaketMode.DUO -> stringResource(if (won) R.string.raket_bottom_won else R.string.raket_top_won)
        RaketMode.WALL -> stringResource(R.string.raket_wall_over)
    }
    val big = if (mode == RaketMode.WALL) hits.toString() else "${hud.bottom}–${hud.top}"
    val result = if (mode == RaketMode.WALL) {
        stringResource(R.string.raket_wall_result_fmt, hits)
    } else {
        stringResource(R.string.raket_result_fmt, hud.bottom, hud.top, hud.bestRally)
    }
    val levelLabel = when (level.coerceIn(0, RaketWorld.MAX_LEVEL)) {
        0 -> stringResource(R.string.difficulty_easy)
        1 -> stringResource(R.string.difficulty_medium)
        else -> stringResource(R.string.difficulty_hard)
    }
    val headline = when (mode) {
        RaketMode.SOLO -> stringResource(if (won) R.string.raket_share_win_fmt else R.string.raket_share_loss_fmt, hud.bottom, hud.top)
        RaketMode.DUO -> stringResource(R.string.raket_share_duo_fmt, hud.bottom, hud.top)
        RaketMode.WALL -> stringResource(R.string.raket_share_wall_fmt, hits)
    }
    val details = when (mode) {
        RaketMode.SOLO -> listOf(stringResource(R.string.raket_mode_solo) + " · " + levelLabel, result)
        RaketMode.DUO -> listOf(stringResource(R.string.raket_mode_duo), result)
        RaketMode.WALL -> listOf(stringResource(R.string.raket_mode_wall) + " · " + modeShareLabel(wallDaily, null))
    }
    OverlayCard {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = big,
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
        ShareButton(ShareContent(gameId = "raket", headline = headline, details = details))
        Spacer(Modifier.height(4.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.restart))
        }
        TextButton(onClick = onMenu, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.raket_to_menu))
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        )
    }
}
