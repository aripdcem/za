package com.za.games.ui.viraj

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
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
import com.za.games.ui.common.GameTopBar
import com.za.games.ui.common.OverlayCard
import com.za.games.ui.common.ScoreCard
import com.za.games.ui.common.ShareButton
import com.za.games.ui.common.formatScore
import com.za.games.ui.common.modeShareLabel
import com.za.games.viraj.Item
import com.za.games.viraj.ItemKind
import com.za.games.viraj.Sprite
import com.za.games.viraj.SpriteKind
import com.za.games.viraj.VirajHud
import com.za.games.viraj.VirajWorld
import kotlinx.coroutines.isActive
import java.util.Locale
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

private const val DRAW = 90
private const val RUMBLE = 3

private val SkyTop = Color(0xFF0B1E3D)
private val SkyHorizon = Color(0xFF7DD3FC)
private val Sun = Color(0xFFFDE68A)
private val HillFar = Color(0xFF1E3A5F)
private val HillNear = Color(0xFF14532D)
private val GrassDark = Color(0xFF2F6B2F)
private val GrassLight = Color(0xFF3A7D3A)
private val RoadDark = Color(0xFF3B4252)
private val RoadLight = Color(0xFF444C5E)
private val CheckpointRoad = Color(0xFF64748B)
private val RumbleDark = Color(0xFFE5E7EB)
private val RumbleLight = Color(0xFFDC2626)
private val LaneColor = Color(0xFFE5E7EB)
private val Trunk = Color(0xFF713F12)
private val Canopy = Color(0xFF166534)
private val Bush = Color(0xFF4D7C0F)
private val Boulder = Color(0xFF78716C)
private val SignPost = Color(0xFFA8A29E)
private val SignFace = Color(0xFFFBBF24)
private val Pole = Color(0xFFD6D3D1)
private val TurboStrip = Color(0xFF4DE1FF)
private val Oil = Color(0xFF111827)
private val ConeColor = Color(0xFFFB923C)
private val BoxColor = Color(0xFFFACC15)
private val GateRed = Color(0xFFDC2626)
private val GateWhite = Color(0xFFF8FAFC)
private val PlayerBody = Color(0xFF4DE1FF)
private val PlayerDark = Color(0xFF0E7490)
private val Helmet = Color(0xFFF8FAFC)
private val Wheel = Color(0xFF1F2937)
private val ShadowColor = Color(0x55000000)
private val ShieldRing = Color(0x8893C5FD)
private val Flame = Color(0xFFFB923C)
private val CarColors = listOf(
    Color(0xFFF87171), Color(0xFFFBBF24), Color(0xFFA78BFA),
    Color(0xFFF472B6), Color(0xFF34D399), Color(0xFFE2E8F0),
)

/** Dokunmatik bölgeler: tuval genişliğinin bu payından solu sola kırar, sağı sağa; aradaki şerit fren. */
private const val ZONE_LEFT = 0.4f
private const val ZONE_RIGHT = 0.6f

/** Bölge ipuçlarının gösterildiği kare sayısı (60 Hz). */
private const val ZONE_HINT_FRAMES = 480

@Composable
fun VirajScreen(
    highScore: Long,
    onScore: (Long) -> Unit,
    onExit: () -> Unit,
    viewModel: VirajViewModel = viewModel(),
) {
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val daily by viewModel.daily.collectAsStateWithLifecycle()
    val hud by viewModel.hud.collectAsStateWithLifecycle()
    val runId by viewModel.runId.collectAsStateWithLifecycle()
    val haptics = LocalZaHaptics.current
    val sound = LocalZaSound.current
    val resources = LocalContext.current.resources
    val fx = remember { VirajFx() }
    val fxTick = remember { mutableLongStateOf(0L) }

    val latestScore by rememberUpdatedState(hud.score)
    val latestOnScore by rememberUpdatedState(onScore)
    DisposableEffect(Unit) {
        onDispose { latestOnScore(latestScore) }
    }
    LaunchedEffect(phase) {
        if (phase == VirajPhase.OVER) latestOnScore(hud.score)
    }
    LaunchedEffect(runId) { fx.reset() }
    LaunchedEffect(Unit) { viewModel.refreshDaily() }
    LifecycleResumeEffect(Unit) {
        onPauseOrDispose { viewModel.pause() }
    }
    BackHandler {
        if (phase == VirajPhase.PLAYING) viewModel.pause() else onExit()
    }

    LaunchedEffect(phase, runId) {
        if (phase != VirajPhase.PLAYING && phase != VirajPhase.OVER) return@LaunchedEffect
        var last = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = now - last
                    val world = viewModel.world
                    for (event in viewModel.advance(dt)) {
                        fx.onEvent(event, world, sound, haptics, resources)
                    }
                    fx.update(dt / 1_000_000_000f, world)
                    fxTick.longValue += 1
                }
                last = now
            }
            if (phase == VirajPhase.OVER && !fx.isBusy) break
        }
    }

    var previousBest by remember { mutableLongStateOf(highScore) }
    val startRun = {
        previousBest = maxOf(previousBest, hud.score)
        viewModel.start()
    }
    val restartRun = {
        previousBest = maxOf(previousBest, hud.score)
        viewModel.restart()
    }
    val attemptsLeft = VirajViewModel.DAILY_ATTEMPTS - (daily?.attempts ?: 0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        GameTopBar(title = stringResource(R.string.game_viraj), onExit = onExit) {
            if (phase == VirajPhase.PLAYING || phase == VirajPhase.PAUSED) {
                TextButton(onClick = viewModel::togglePause) {
                    Text(stringResource(if (phase == VirajPhase.PAUSED) R.string.resume else R.string.pause))
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
                label = stringResource(R.string.score),
                value = formatScore(hud.score),
                modifier = Modifier.weight(1f),
                highlight = true,
            )
            ScoreCard(
                label = stringResource(R.string.time_label),
                value = hud.timeLeft.toInt().toString(),
                modifier = Modifier.weight(0.8f),
            )
            ScoreCard(
                label = stringResource(R.string.viraj_speed),
                value = stringResource(R.string.viraj_kmh_fmt, hud.kmh),
                modifier = Modifier.weight(1f),
            )
        }

        CheckpointBar(
            fraction = hud.checkpointProgress,
            overtakes = hud.overtakes,
            shield = hud.shield,
            visible = phase == VirajPhase.PLAYING || phase == VirajPhase.PAUSED,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            VirajCanvas(viewModel = viewModel, fx = fx, fxTick = fxTick, hud = hud, modifier = Modifier.fillMaxSize())
            when (phase) {
                VirajPhase.MENU -> StartCard(
                    mode = mode,
                    daily = daily,
                    onMode = viewModel::setMode,
                    onStart = startRun,
                    onExit = onExit,
                )
                VirajPhase.PAUSED -> PauseCard(
                    onResume = viewModel::resume,
                    onRestart = restartRun,
                    onMenu = viewModel::toMenu,
                    onExit = onExit,
                )
                VirajPhase.OVER -> OverCard(
                    hud = hud,
                    daily = mode == VirajMode.DAILY,
                    attemptsLeft = attemptsLeft,
                    isRecord = hud.score > previousBest,
                    onRestart = restartRun,
                    onMenu = viewModel::toMenu,
                    onExit = onExit,
                )
                VirajPhase.PLAYING -> Unit
            }
        }

    }
}

/** Sıradaki kontrol noktasına ilerleme, sollama sayısı ve kalkan durumu. */
@Composable
private fun CheckpointBar(fraction: Float, overtakes: Int, shield: Boolean, visible: Boolean) {
    val alpha = if (visible) 1f else 0f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.viraj_next_checkpoint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f * alpha),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha)),
            )
        }
        Text(
            text = (if (shield) "🛡 " else "") + "⇈ " + overtakes,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = VirajFx.POINTS.copy(alpha = alpha),
        )
    }
}

// ---------------------------------------------------------------------------
// Tuval: sözde-3D yol
// ---------------------------------------------------------------------------

private class Proj {
    var x = 0f
    var y = 0f
    var w = 0f
    var scale = 0f
    var camZ = 0f
}

private fun projectTo(
    p: Proj,
    camX: Float,
    worldY: Float,
    camY: Float,
    worldZ: Float,
    camZ: Float,
    width: Float,
    height: Float,
) {
    val cz = worldZ - camZ
    p.camZ = cz
    p.scale = if (cz > 1f) VirajWorld.CAMERA_DEPTH / cz else 0f
    p.x = width / 2f + p.scale * (-camX) * width / 2f
    p.y = height / 2f - p.scale * (worldY - camY) * height / 2f
    p.w = p.scale * VirajWorld.ROAD_WIDTH * width / 2f
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

@Composable
private fun VirajCanvas(
    viewModel: VirajViewModel,
    fx: VirajFx,
    fxTick: MutableLongState,
    hud: VirajHud,
    modifier: Modifier = Modifier,
) {
    val frame by viewModel.frame.collectAsStateWithLifecycle()
    val desc = stringResource(R.string.viraj_board_desc, hud.kmh, hud.meters, hud.overtakes)
    val textMeasurer = rememberTextMeasurer()
    val textCache = remember { HashMap<String, TextLayoutResult>() }
    val proj = remember { Array(DRAW + 1) { Proj() } }
    val clip = remember { FloatArray(DRAW + 1) }
    val path = remember { Path() }
    val brakeLabel = stringResource(R.string.viraj_ctrl_brake)
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .semantics { contentDescription = desc }
            .pointerInput(viewModel) {
                // Dokunmatik kontrol, tuş yok: ilk parmağın bölgesi direksiyon (sol
                // yarı sola, sağ yarı sağa, orta şerit düz ve fren), her ek parmak fren.
                // Bölge parmak kayınca güncellenir; roller kalkana dek değişmez.
                awaitEachGesture {
                    val first = awaitFirstDown(requireUnconsumed = false)
                    val order = ArrayList<PointerId>()
                    val zones = HashMap<PointerId, Int>()
                    fun zoneOf(x: Float): Int {
                        val f = x / size.width
                        return if (f < ZONE_LEFT) -1 else if (f > ZONE_RIGHT) 1 else 0
                    }
                    fun apply() {
                        val lead = order.firstOrNull()
                        if (lead == null) {
                            viewModel.setTouch(0, false)
                            return
                        }
                        val z = zones[lead] ?: 0
                        viewModel.setTouch(z, z == 0 || order.size >= 2)
                    }
                    order += first.id
                    zones[first.id] = zoneOf(first.position.x)
                    first.consume()
                    apply()
                    while (true) {
                        val event = awaitPointerEvent()
                        var changed = false
                        for (c in event.changes) {
                            if (c.changedToDownIgnoreConsumed() && c.id !in zones) {
                                order += c.id
                                zones[c.id] = zoneOf(c.position.x)
                                changed = true
                            } else if (c.pressed && c.id in zones) {
                                val z = zoneOf(c.position.x)
                                if (z != zones[c.id]) {
                                    zones[c.id] = z
                                    changed = true
                                }
                            }
                            if (c.changedToUpIgnoreConsumed() && c.id in zones) {
                                order.remove(c.id)
                                zones.remove(c.id)
                                changed = true
                            }
                            c.consume()
                        }
                        if (changed) apply()
                        if (event.changes.none { it.pressed }) break
                    }
                    viewModel.setTouch(0, false)
                }
            },
    ) {
        // Kare sayaçları okunur ki her adımda yeniden çizilsin.
        val tick = frame + fxTick.longValue
        if (tick < 0L) return@Canvas
        drawScene(viewModel.world, fx, proj, clip, path, textMeasurer, textCache)
        drawTouchZones(viewModel.world, textMeasurer, textCache, brakeLabel)
    }
}

/** Koşunun ilk saniyelerinde bölge ipuçları: köşelerde oklar, ortada fren etiketi; sonra söner. */
private fun DrawScope.drawTouchZones(world: VirajWorld, textMeasurer: TextMeasurer, cache: HashMap<String, TextLayoutResult>, brakeLabel: String) {
    if (world.frames <= 0 || world.frames > ZONE_HINT_FRAMES) return
    val alpha = ((ZONE_HINT_FRAMES - world.frames) / 90f).coerceIn(0f, 1f) * 0.85f
    val w = size.width
    val h = size.height
    val y = h * 0.86f
    val u = w * 0.03f
    val color = Color.White.copy(alpha = alpha)
    val edge = Color.Black.copy(alpha = alpha * 0.6f)
    for (side in intArrayOf(-1, 1)) {
        val cx = if (side < 0) w * 0.2f else w * 0.8f
        val tip = cx + side * u * 1.2f
        for ((c, sw) in listOf(edge to u * 0.9f, color to u * 0.45f)) {
            drawLine(c, Offset(cx - side * u * 0.6f, y - u * 1.2f), Offset(tip, y), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(c, Offset(cx - side * u * 0.6f, y + u * 1.2f), Offset(tip, y), strokeWidth = sw, cap = StrokeCap.Round)
        }
    }
    val layout = cache.getOrPut("zone|$brakeLabel") {
        textMeasurer.measure(AnnotatedString(brakeLabel), style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White))
    }
    val pad = u * 0.8f
    val tx = w / 2f - layout.size.width / 2f
    val ty = y - layout.size.height / 2f
    drawRoundRect(Color.Black.copy(alpha = alpha * 0.55f), topLeft = Offset(tx - pad, ty - pad * 0.4f), size = Size(layout.size.width + pad * 2f, layout.size.height + pad * 0.8f), cornerRadius = CornerRadius(pad, pad))
    drawText(layout, topLeft = Offset(tx, ty), alpha = alpha)
}

private fun DrawScope.drawScene(
    world: VirajWorld,
    fx: VirajFx,
    proj: Array<Proj>,
    clip: FloatArray,
    path: Path,
    textMeasurer: TextMeasurer,
    cache: HashMap<String, TextLayoutResult>,
) {
    val width = size.width
    val height = size.height
    translate(fx.shakeX * width, fx.shakeY * height) {
        drawSky(width, height, fx.bgOffset)

        val track = world.track
        val segLen = VirajWorld.SEGMENT_LENGTH
        val base = world.segmentIndexOf(world.position)
        val basePercent = ((world.position - base * segLen) / segLen).coerceIn(0f, 1f)
        val playerIndex = world.playerSegmentIndex
        val playerPrevY = if (playerIndex > 0) track.segment(playerIndex - 1).y else 0f
        val playerPercent = ((world.playerZ - playerIndex * segLen) / segLen).coerceIn(0f, 1f)
        val cameraY = lerp(playerPrevY, track.segment(playerIndex).y, playerPercent) + VirajWorld.CAMERA_HEIGHT

        var x = 0f
        var dx = -(track.segment(base).curve * basePercent)
        for (n in 0..DRAW) {
            val idx = base + n
            val prevY = if (idx > 0) track.segment(idx - 1).y else 0f
            projectTo(proj[n], world.playerX * VirajWorld.ROAD_WIDTH - x, prevY, cameraY, idx * segLen, world.position, width, height)
            x += dx
            dx += track.segment(idx).curve
        }

        var maxy = height
        for (n in 0 until DRAW) {
            val p1 = proj[n]
            val p2 = proj[n + 1]
            clip[n] = maxy
            if (p1.camZ <= VirajWorld.CAMERA_DEPTH || p2.y >= p1.y || p2.y >= maxy) continue
            val idx = base + n
            val t = n / DRAW.toFloat()
            renderSegment(path, width, p1, p2, dark = (idx / RUMBLE) % 2 == 0, fog = t * t * 0.85f, checkpoint = track.segment(idx).checkpoint)
            maxy = p1.y
        }
        clip[DRAW] = maxy

        for (n in DRAW - 1 downTo 1) {
            val seg = track.segment(base + n)
            val p = proj[n]
            if (p.scale <= 0f) continue
            val clipY = clip[n]
            if (seg.checkpoint) drawGate(p, clipY, width)
            for (sprite in seg.sprites) drawSprite(sprite, p, clipY, width)
            seg.item?.let { if (!it.taken) drawItem(path, it, p, proj[n + 1], clipY, width) }
        }

        val cars = world.cars.sortedByDescending { it.z }
        for (car in cars) {
            val n = world.segmentIndexOf(car.z) - base
            if (n < 1 || n >= DRAW) continue
            val p1 = proj[n]
            val p2 = proj[n + 1]
            if (p1.scale <= 0f || p2.scale <= 0f) continue
            val percent = ((car.z - (base + n) * segLen) / segLen).coerceIn(0f, 1f)
            val scale = lerp(p1.scale, p2.scale, percent)
            val roadHalf = scale * VirajWorld.ROAD_WIDTH * width / 2f
            val cx = lerp(p1.x, p2.x, percent) + car.x * roadHalf
            val cy = lerp(p1.y, p2.y, percent)
            val clipY = clip[n]
            if (cy - roadHalf * 0.3f > clipY) continue
            clipRect(0f, 0f, width, clipY) {
                drawKart(cx, cy, roadHalf * VirajWorld.CAR_WIDTH, CarColors[car.style % CarColors.size], 0f)
            }
        }

        drawPlayer(path, world, width, height)
        drawParticles(fx, width, height)
        if (world.turboT > 0f) drawTurboStreaks(width, height, world.turboT / VirajWorld.TURBO_TIME)
        if (fx.flash > 0f) drawRect(Color(0xFFEF4444).copy(alpha = 0.22f * fx.flash))
        drawTexts(fx, width, height, textMeasurer, cache)
    }
}

private fun DrawScope.drawSky(width: Float, height: Float, bgOffset: Float) {
    val horizon = height / 2f
    drawRect(Brush.verticalGradient(listOf(SkyTop, SkyHorizon), startY = 0f, endY = horizon), size = Size(width, horizon))
    drawCircle(Sun, radius = width * 0.07f, center = Offset(width * 0.72f + bgOffset * width * 0.3f, horizon * 0.42f))
    drawRect(GrassDark, topLeft = Offset(0f, horizon), size = Size(width, height - horizon))
    drawHills(width, horizon, bgOffset * 0.5f, 0.16f, 1.1f, HillFar)
    drawHills(width, horizon, bgOffset, 0.10f, 2.3f, HillNear)
}

private fun DrawScope.drawHills(width: Float, horizon: Float, offset: Float, amplitude: Float, freq: Float, color: Color) {
    val path = Path()
    path.moveTo(0f, horizon + 1f)
    val steps = 24
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val phase = (t + offset) * freq * 2f * PI.toFloat()
        val h = (0.55f + 0.45f * sin(phase) * sin(phase * 0.5f + 1f)) * amplitude * horizon
        path.lineTo(t * width, horizon - h)
    }
    path.lineTo(width, horizon + 1f)
    path.close()
    drawPath(path, color)
}

private fun fogged(color: Color, fog: Float): Color = lerp(color, SkyHorizon, fog)

private fun DrawScope.polygon(
    path: Path,
    x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, x4: Float, y4: Float,
    color: Color,
) {
    path.reset()
    path.moveTo(x1, y1)
    path.lineTo(x2, y2)
    path.lineTo(x3, y3)
    path.lineTo(x4, y4)
    path.close()
    drawPath(path, color)
}

private fun DrawScope.renderSegment(path: Path, width: Float, p1: Proj, p2: Proj, dark: Boolean, fog: Float, checkpoint: Boolean) {
    drawRect(fogged(if (dark) GrassDark else GrassLight, fog), topLeft = Offset(0f, p2.y), size = Size(width, p1.y - p2.y))
    val r1 = p1.w / 6f
    val r2 = p2.w / 6f
    val rumble = fogged(if (dark) RumbleDark else RumbleLight, fog)
    polygon(path, p1.x - p1.w - r1, p1.y, p1.x - p1.w, p1.y, p2.x - p2.w, p2.y, p2.x - p2.w - r2, p2.y, rumble)
    polygon(path, p1.x + p1.w + r1, p1.y, p1.x + p1.w, p1.y, p2.x + p2.w, p2.y, p2.x + p2.w + r2, p2.y, rumble)
    val road = fogged(if (checkpoint) CheckpointRoad else if (dark) RoadDark else RoadLight, fog)
    polygon(path, p1.x - p1.w, p1.y, p1.x + p1.w, p1.y, p2.x + p2.w, p2.y, p2.x - p2.w, p2.y, road)
    if (dark) {
        val lanes = 3
        val l1 = p1.w / 32f
        val l2 = p2.w / 32f
        val step1 = p1.w * 2f / lanes
        val step2 = p2.w * 2f / lanes
        var lx1 = p1.x - p1.w + step1
        var lx2 = p2.x - p2.w + step2
        val lane = fogged(LaneColor, fog)
        for (i in 1 until lanes) {
            polygon(path, lx1 - l1 / 2f, p1.y, lx1 + l1 / 2f, p1.y, lx2 + l2 / 2f, p2.y, lx2 - l2 / 2f, p2.y, lane)
            lx1 += step1
            lx2 += step2
        }
    }
}

private fun DrawScope.drawSprite(sprite: Sprite, p: Proj, clipY: Float, width: Float) {
    val sx = p.x + sprite.x * p.w
    val (wr, hr) = when (sprite.kind) {
        SpriteKind.TREE -> 0.7f to 1.5f
        SpriteKind.BUSH -> 0.45f to 0.3f
        SpriteKind.BOULDER -> 0.5f to 0.4f
        SpriteKind.SIGN -> 0.35f to 0.6f
        SpriteKind.POLE -> 0.06f to 0.8f
    }
    val w = wr * p.w
    val h = hr * p.w
    if (w < 1f || p.y - h >= clipY || sx + w < 0f || sx - w > width) return
    clipRect(0f, 0f, width, clipY) {
        when (sprite.kind) {
            SpriteKind.TREE -> {
                drawRect(Trunk, topLeft = Offset(sx - w * 0.08f, p.y - h * 0.45f), size = Size(w * 0.16f, h * 0.45f))
                drawCircle(Canopy, radius = w * 0.5f, center = Offset(sx, p.y - h * 0.68f))
                drawCircle(Canopy.copy(alpha = 0.85f), radius = w * 0.36f, center = Offset(sx - w * 0.28f, p.y - h * 0.5f))
                drawCircle(Canopy.copy(alpha = 0.85f), radius = w * 0.36f, center = Offset(sx + w * 0.28f, p.y - h * 0.5f))
            }
            SpriteKind.BUSH -> drawOval(Bush, topLeft = Offset(sx - w / 2f, p.y - h), size = Size(w, h))
            SpriteKind.BOULDER -> drawRoundRect(Boulder, topLeft = Offset(sx - w / 2f, p.y - h), size = Size(w, h), cornerRadius = CornerRadius(w * 0.3f, w * 0.3f))
            SpriteKind.SIGN -> {
                drawRect(SignPost, topLeft = Offset(sx - w * 0.05f, p.y - h), size = Size(w * 0.1f, h))
                drawRoundRect(SignFace, topLeft = Offset(sx - w / 2f, p.y - h), size = Size(w, h * 0.45f), cornerRadius = CornerRadius(w * 0.08f, w * 0.08f))
            }
            SpriteKind.POLE -> drawRect(Pole, topLeft = Offset(sx - w / 2f, p.y - h), size = Size(max(1f, w), h))
        }
    }
}

private fun DrawScope.drawItem(path: Path, item: Item, p: Proj, next: Proj, clipY: Float, width: Float) {
    val sx = p.x + item.x * p.w
    val depth = max(2f, (p.y - next.y) * 0.9f)
    val w = VirajWorld.ITEM_WIDTH * p.w
    if (w < 1f || p.y > clipY + depth) return
    clipRect(0f, 0f, width, clipY) {
        when (item.kind) {
            ItemKind.TURBO -> {
                drawRoundRect(TurboStrip, topLeft = Offset(sx - w / 2f, p.y - depth), size = Size(w, depth), cornerRadius = CornerRadius(w * 0.1f, w * 0.1f))
                polygon(path, sx - w * 0.3f, p.y - depth * 0.15f, sx, p.y - depth * 0.85f, sx + w * 0.3f, p.y - depth * 0.15f, sx, p.y - depth * 0.45f, GateWhite)
            }
            ItemKind.OIL -> drawOval(Oil, topLeft = Offset(sx - w * 0.6f, p.y - depth), size = Size(w * 1.2f, depth))
            ItemKind.CONE -> {
                val h = p.w * 0.22f
                polygon(path, sx - w * 0.3f, p.y, sx + w * 0.3f, p.y, sx + w * 0.08f, p.y - h, sx - w * 0.08f, p.y - h, ConeColor)
                drawRect(GateWhite, topLeft = Offset(sx - w * 0.16f, p.y - h * 0.55f), size = Size(w * 0.32f, h * 0.12f))
            }
            ItemKind.BOX -> {
                val h = p.w * 0.2f
                drawRoundRect(BoxColor, topLeft = Offset(sx - w * 0.3f, p.y - h), size = Size(w * 0.6f, h), cornerRadius = CornerRadius(w * 0.08f, w * 0.08f))
                drawRect(Oil.copy(alpha = 0.6f), topLeft = Offset(sx - w * 0.05f, p.y - h * 0.8f), size = Size(w * 0.1f, h * 0.6f))
            }
        }
    }
}

private fun DrawScope.drawGate(p: Proj, clipY: Float, width: Float) {
    val postH = p.w * 0.75f
    val postW = max(1.5f, p.w * 0.04f)
    val left = p.x - p.w * 1.12f
    val right = p.x + p.w * 1.12f
    if (p.w < 2f || p.y - postH >= clipY) return
    clipRect(0f, 0f, width, clipY) {
        drawRect(Pole, topLeft = Offset(left - postW / 2f, p.y - postH), size = Size(postW, postH))
        drawRect(Pole, topLeft = Offset(right - postW / 2f, p.y - postH), size = Size(postW, postH))
        val barH = max(2f, p.w * 0.1f)
        val stripes = 8
        val stripeW = (right - left) / stripes
        for (i in 0 until stripes) {
            drawRect(if (i % 2 == 0) GateRed else GateWhite, topLeft = Offset(left + i * stripeW, p.y - postH), size = Size(stripeW + 0.5f, barH))
        }
    }
}

private fun DrawScope.drawKart(cx: Float, cy: Float, w: Float, color: Color, tilt: Float) {
    if (w < 1.5f) {
        drawRect(color, topLeft = Offset(cx - w / 2f, cy - w * 0.5f), size = Size(max(1f, w), max(1f, w * 0.5f)))
        return
    }
    val h = w * 0.7f
    rotate(tilt, pivot = Offset(cx, cy)) {
        drawOval(ShadowColor, topLeft = Offset(cx - w * 0.55f, cy - h * 0.12f), size = Size(w * 1.1f, h * 0.24f))
        drawRect(Wheel, topLeft = Offset(cx - w * 0.5f, cy - h * 0.42f), size = Size(w * 0.2f, h * 0.42f))
        drawRect(Wheel, topLeft = Offset(cx + w * 0.3f, cy - h * 0.42f), size = Size(w * 0.2f, h * 0.42f))
        drawRoundRect(color, topLeft = Offset(cx - w * 0.36f, cy - h * 0.72f), size = Size(w * 0.72f, h * 0.6f), cornerRadius = CornerRadius(w * 0.1f, w * 0.1f))
        drawRect(color.copy(alpha = 0.6f), topLeft = Offset(cx - w * 0.42f, cy - h * 0.98f), size = Size(w * 0.84f, h * 0.1f))
        drawCircle(Helmet, radius = w * 0.12f, center = Offset(cx, cy - h * 0.78f))
    }
}

private fun DrawScope.drawPlayer(path: Path, world: VirajWorld, width: Float, height: Float) {
    val w = width * 0.26f
    val cx = width / 2f
    val cy = height - w * 0.12f
    val speedPct = world.speed / VirajWorld.MAX_SPEED
    val wobble = if (world.slipT > 0f) sin(world.frames * 0.9f) * 10f else 0f
    val bounce = if (speedPct > 0.1f) sin(world.frames * 1.7f) * speedPct * w * 0.01f else 0f
    val tilt = world.steer * (if (world.slipT > 0f) -1f else 1f) * 7f * (0.3f + 0.7f * speedPct) + wobble
    if (world.turboT > 0f) {
        val flameH = w * (0.25f + 0.15f * sin(world.frames * 1.3f))
        polygon(path, cx - w * 0.32f, cy, cx - w * 0.18f, cy, cx - w * 0.25f, cy + flameH, cx - w * 0.25f, cy + flameH * 0.6f, Flame)
        polygon(path, cx + w * 0.18f, cy, cx + w * 0.32f, cy, cx + w * 0.25f, cy + flameH, cx + w * 0.25f, cy + flameH * 0.6f, Flame)
    }
    drawKart(cx, cy + bounce, w, PlayerBody, tilt)
    drawRect(PlayerDark, topLeft = Offset(cx - w * 0.2f, cy - w * 0.7f * 0.5f), size = Size(w * 0.4f, w * 0.06f))
    if (world.shield) {
        drawCircle(ShieldRing, radius = w * 0.62f, center = Offset(cx, cy - w * 0.35f))
    }
}

private fun DrawScope.drawTurboStreaks(width: Float, height: Float, fraction: Float) {
    val alpha = 0.35f * fraction
    for (i in 0 until 6) {
        val y = height * (0.15f + i * 0.13f)
        drawLine(TurboStrip.copy(alpha = alpha), Offset(0f, y), Offset(width * 0.14f, y + height * 0.04f), strokeWidth = 2f)
        drawLine(TurboStrip.copy(alpha = alpha), Offset(width, y), Offset(width * 0.86f, y + height * 0.04f), strokeWidth = 2f)
    }
}

private fun DrawScope.drawParticles(fx: VirajFx, width: Float, height: Float) {
    for (q in fx.particles) {
        drawCircle(q.color.copy(alpha = (q.life / q.maxLife).coerceIn(0f, 1f)), radius = q.size * width, center = Offset(q.x * width, q.y * height))
    }
}

private fun DrawScope.drawTexts(fx: VirajFx, width: Float, height: Float, textMeasurer: TextMeasurer, cache: HashMap<String, TextLayoutResult>) {
    for (t in fx.texts) {
        val key = "${t.text}|${t.big}|${t.color.value}"
        val layout = cache.getOrPut(key) {
            textMeasurer.measure(
                AnnotatedString(t.text),
                style = TextStyle(
                    fontSize = if (t.big) 26.sp else 17.sp,
                    fontWeight = FontWeight.Black,
                    color = t.color,
                    textAlign = TextAlign.Center,
                ),
            )
        }
        val alpha = (t.life / t.maxLife).coerceIn(0f, 1f)
        drawText(layout, topLeft = Offset(t.x * width - layout.size.width / 2f, t.y * height - layout.size.height / 2f), alpha = alpha)
    }
}

// ---------------------------------------------------------------------------
// Kartlar ve kontroller
// ---------------------------------------------------------------------------

@Composable
private fun StartCard(
    mode: VirajMode,
    daily: VirajDaily?,
    onMode: (VirajMode) -> Unit,
    onStart: () -> Unit,
    onExit: () -> Unit,
) {
    val exhausted = daily != null && daily.attempts >= VirajViewModel.DAILY_ATTEMPTS
    OverlayCard {
        Text(
            text = stringResource(R.string.game_viraj),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.viraj_intro),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeChip(stringResource(R.string.mode_daily), mode == VirajMode.DAILY, Modifier.weight(1f)) { onMode(VirajMode.DAILY) }
            ModeChip(stringResource(R.string.mode_free), mode == VirajMode.FREE, Modifier.weight(1f)) { onMode(VirajMode.FREE) }
        }
        if (mode == VirajMode.DAILY) {
            if (daily != null) {
                Text(
                    text = stringResource(R.string.viraj_daily_status_fmt, daily.attempts, VirajViewModel.DAILY_ATTEMPTS, daily.best),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = stringResource(if (exhausted) R.string.viraj_daily_exhausted else R.string.viraj_daily_desc),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        Text(
            text = stringResource(R.string.viraj_touch_hint),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(4.dp))
        if (mode == VirajMode.DAILY && exhausted) {
            Button(onClick = { onMode(VirajMode.FREE) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.play_free))
            }
        } else {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.viraj_start))
            }
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

@Composable
private fun PauseCard(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onMenu: () -> Unit,
    onExit: () -> Unit,
) {
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
            Text(stringResource(R.string.viraj_to_menu))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

@Composable
private fun OverCard(
    hud: VirajHud,
    daily: Boolean,
    attemptsLeft: Int,
    isRecord: Boolean,
    onRestart: () -> Unit,
    onMenu: () -> Unit,
    onExit: () -> Unit,
) {
    val km = String.format(Locale.getDefault(), "%.1f", hud.meters / 1000f)
    val result = stringResource(R.string.viraj_result_fmt, km, hud.overtakes, hud.checkpoints)
    OverlayCard {
        Text(
            text = stringResource(R.string.viraj_time_up),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = formatScore(hud.score),
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
                text = stringResource(R.string.viraj_daily_exhausted),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        ShareButton(
            ShareContent(
                gameId = "viraj",
                headline = stringResource(R.string.share_score_fmt, formatScore(hud.score)),
                details = listOf(result, modeShareLabel(daily, null)),
            ),
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text(
                if (daily && attemptsLeft > 0) {
                    stringResource(R.string.viraj_retry_fmt, attemptsLeft)
                } else if (daily) {
                    stringResource(R.string.play_free)
                } else {
                    stringResource(R.string.restart)
                },
            )
        }
        TextButton(onClick = onMenu, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.viraj_to_menu))
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
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

