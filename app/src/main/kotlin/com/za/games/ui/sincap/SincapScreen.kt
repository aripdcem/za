package com.za.games.ui.sincap

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
import androidx.compose.ui.graphics.lerp
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
import com.za.games.sincap.BranchKind
import com.za.games.sincap.DeathCause
import com.za.games.sincap.Side
import com.za.games.sincap.SincapHud
import com.za.games.sincap.SincapStatus
import com.za.games.sincap.SincapWorld
import com.za.games.sincap.present
import com.za.games.ui.common.GameTopBar
import com.za.games.ui.common.OverlayCard
import com.za.games.ui.common.ScoreCard
import com.za.games.ui.common.ShareButton
import com.za.games.ui.common.formatScore
import com.za.games.ui.common.modeShareLabel
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.isActive

private val SkyTopLow = Color(0xFF7DD3FC)
private val SkyBottomLow = Color(0xFFE0F2FE)
private val SkyTopHigh = Color(0xFF1E3A8A)
private val SkyBottomHigh = Color(0xFF60A5FA)
private val Cloud = Color(0x99FFFFFF)
private val Trunk = Color(0xFF7C4A1E)
private val TrunkEdge = Color(0xFF5B3414)
private val Bark = Color(0xFF6B3F17)
private val Wood = Color(0xFF92400E)
private val DryWood = Color(0xFFA8A29E)
private val DryCrack = Color(0xFF57534E)
private val Leaf1 = Color(0xFF4D7C0F)
private val Leaf2 = Color(0xFF65A30D)
private val Leaf3 = Color(0xFF84CC16)
private val NutShell = Color(0xFFB45309)
private val NutCap = Color(0xFF78350F)
private val GoldShell = Color(0xFFFACC15)
private val GoldCap = Color(0xFFCA8A04)
private val Snake = Color(0xFF16A34A)
private val SnakeDark = Color(0xFF166534)
private val Tongue = Color(0xFFEF4444)
private val CrowBody = Color(0xFF111827)
private val Beak = Color(0xFFF97316)
private val CatBody = Color(0xFFF97316)
private val CatStripe = Color(0xFFC2410C)
private val CatEye = Color(0xFF4ADE80)
private val Squirrel = Color(0xFFB45309)
private val SquirrelBelly = Color(0xFFFDE68A)
private val SquirrelTail = Color(0xFFD97706)
private val Hint = Color(0xF2FFFFFF)
private val HintOutline = Color(0xE6143D6B)
private val Shadow = Color(0x33000000)

/** Ekranda görünen basamak sayısı ve sincabın alt kenardan yüksekliği (basamak). */
private const val VIEW_LEVELS = 7.5f
private const val CAM_OFFSET = 2.4f

/**
 * Gökyüzü degradesi yükseklik kovasına göre önbellekte: her karede yeni
 * gölgelendirici kurmak yerine renk ancak fark edilir kadar değişince (1/40)
 * yenilenir (cihaz bulgusu: Sincap'ın kare gecikmesi kütüğün en yükseğiydi).
 */
private class SkyBrush {
    private var bucket = -1
    private var brush: Brush? = null

    fun at(alt: Float): Brush {
        val b = (alt * 40f).toInt()
        val cached = brush
        if (cached != null && b == bucket) return cached
        bucket = b
        val t = b / 40f
        return Brush.verticalGradient(0f to lerp(SkyTopLow, SkyTopHigh, t), 1f to lerp(SkyBottomLow, SkyBottomHigh, t)).also { brush = it }
    }
}

/** Dünya → tuval: x −1..1 genişliğe, y basamakları [VIEW_LEVELS] parçaya. */
private class Cam(val w: Float, val h: Float, val camY: Float) {
    val unit: Float = h / VIEW_LEVELS
    fun sx(x: Float): Float = w / 2f + x * (w / 2f)
    fun sy(y: Float): Float = h - (y - camY) * unit
}

@Composable
private fun causeLabel(cause: DeathCause): String = stringResource(
    when (cause) {
        DeathCause.FALL -> R.string.sincap_over_fall
        DeathCause.BROKE -> R.string.sincap_over_broke
        DeathCause.CROW -> R.string.sincap_over_crow
        DeathCause.SNAKE -> R.string.sincap_over_snake
        DeathCause.CAT -> R.string.sincap_over_cat
    },
)

@Composable
fun SincapScreen(
    highScore: Long,
    onScore: (Long) -> Unit,
    onExit: () -> Unit,
    viewModel: SincapViewModel = viewModel(),
) {
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val dailyMode by viewModel.dailyMode.collectAsStateWithLifecycle()
    val daily by viewModel.daily.collectAsStateWithLifecycle()
    val freeBest by viewModel.freeBest.collectAsStateWithLifecycle()
    val freeHeight by viewModel.freeHeight.collectAsStateWithLifecycle()
    val record by viewModel.record.collectAsStateWithLifecycle()
    val hud by viewModel.hud.collectAsStateWithLifecycle()
    val runId by viewModel.runId.collectAsStateWithLifecycle()
    val haptics = LocalZaHaptics.current
    val sound = LocalZaSound.current
    val fx = remember { SincapFx() }
    val fxTick = remember { mutableLongStateOf(0L) }
    val camera = remember { floatArrayOf(-CAM_OFFSET) }
    fx.crackingLabel = stringResource(R.string.sincap_cracking)
    fx.catLabel = stringResource(R.string.sincap_cat_close)
    val milestoneFmt = stringResource(R.string.sincap_milestone_fmt)
    fx.milestoneLabel = { String.format(milestoneFmt, it) }
    val causes = DeathCause.entries.associateWith { causeLabel(it) }
    fx.overLabel = { causes[it] ?: it.name }

    val latestScore by rememberUpdatedState(hud.score.toLong())
    val latestOnScore by rememberUpdatedState(onScore)
    DisposableEffect(Unit) {
        onDispose { latestOnScore(latestScore) }
    }
    LaunchedEffect(phase) {
        if (phase == SincapPhase.OVER) latestOnScore(hud.score.toLong())
    }
    LaunchedEffect(runId) {
        fx.reset()
        camera[0] = -CAM_OFFSET
    }
    LaunchedEffect(Unit) { viewModel.refresh() }
    LifecycleResumeEffect(Unit) {
        onPauseOrDispose { viewModel.pause() }
    }
    BackHandler {
        if (phase == SincapPhase.PLAYING) viewModel.pause() else onExit()
    }

    LaunchedEffect(phase, runId) {
        if (phase != SincapPhase.PLAYING && phase != SincapPhase.OVER) return@LaunchedEffect
        var last = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = (now - last) / 1_000_000_000f
                    val world = viewModel.world
                    for (event in viewModel.advance(now - last)) fx.onEvent(event, world, sound, haptics)
                    fx.update(dt)
                    val target = world.y - CAM_OFFSET
                    camera[0] += (target - camera[0]) * (1f - exp(-8f * dt))
                    fxTick.longValue += 1
                }
                last = now
            }
            if (phase == SincapPhase.OVER && !fx.isBusy) break
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
    val attemptsLeft = SincapViewModel.DAILY_ATTEMPTS - (daily?.attempts ?: 0)
    val best = if (dailyMode) daily?.best ?: 0 else freeBest
    val inRun = phase != SincapPhase.MENU

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        GameTopBar(title = stringResource(R.string.game_sincap), onExit = onExit) {
            if (phase == SincapPhase.PLAYING || phase == SincapPhase.PAUSED) {
                TextButton(onClick = viewModel::togglePause) {
                    Text(stringResource(if (phase == SincapPhase.PAUSED) R.string.resume else R.string.pause))
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
            ScoreCard(label = stringResource(R.string.sincap_height), value = stringResource(R.string.sincap_height_fmt, if (inRun) hud.height else 0), modifier = Modifier.weight(0.9f))
            ScoreCard(label = stringResource(R.string.sincap_nuts), value = if (inRun) hud.nuts.toString() else "–", modifier = Modifier.weight(0.7f))
            ScoreCard(label = stringResource(R.string.sincap_best), value = formatScore(maxOf(best, if (inRun) hud.score else 0).toLong()), modifier = Modifier.weight(1f))
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            SincapCanvas(viewModel = viewModel, fx = fx, fxTick = fxTick, camera = camera, hud = hud, modifier = Modifier.fillMaxSize())
            when (phase) {
                SincapPhase.MENU -> StartCard(
                    dailyMode = dailyMode,
                    daily = daily,
                    freeBest = freeBest,
                    freeHeight = freeHeight,
                    onMode = viewModel::setDailyMode,
                    onStart = startRun,
                    onExit = onExit,
                )
                SincapPhase.PAUSED -> PauseCard(
                    onResume = viewModel::resume,
                    onRestart = restartRun,
                    onMenu = viewModel::toMenu,
                    onExit = onExit,
                )
                SincapPhase.OVER -> OverCard(
                    hud = hud,
                    daily = dailyMode,
                    attemptsLeft = attemptsLeft,
                    isRecord = record || hud.score > previousBest,
                    onRestart = restartRun,
                    onMenu = viewModel::toMenu,
                    onExit = onExit,
                )
                SincapPhase.PLAYING -> Unit
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Tuval
// ---------------------------------------------------------------------------

@Composable
private fun SincapCanvas(
    viewModel: SincapViewModel,
    fx: SincapFx,
    fxTick: MutableLongState,
    camera: FloatArray,
    hud: SincapHud,
    modifier: Modifier = Modifier,
) {
    val frame by viewModel.frame.collectAsStateWithLifecycle()
    val desc = stringResource(R.string.sincap_board_desc_fmt, hud.height, hud.score)
    val hint = stringResource(R.string.sincap_tap_hint)
    val catFmt = stringResource(R.string.sincap_cat_gap_fmt)
    val textMeasurer = rememberTextMeasurer()
    val textCache = remember { HashMap<String, TextLayoutResult>() }
    val path = remember { Path() }
    val sky = remember { SkyBrush() }
    val haptics = LocalZaHaptics.current
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .semantics { contentDescription = desc }
            .pointerInput(viewModel) {
                // İlk temasta tepki: sol yarı sola, sağ yarı sağa.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    val side = if (down.position.x < size.width / 2f) Side.LEFT else Side.RIGHT
                    if (viewModel.tap(side)) haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                }
            },
    ) {
        val tick = frame + fxTick.longValue
        if (tick < 0L) return@Canvas
        drawTree(viewModel.world, fx, camera[0], frame, path, sky, textMeasurer, textCache, hint, catFmt)
    }
}

private fun DrawScope.drawTree(
    world: SincapWorld,
    fx: SincapFx,
    camY: Float,
    frame: Long,
    path: Path,
    sky: SkyBrush,
    textMeasurer: TextMeasurer,
    cache: HashMap<String, TextLayoutResult>,
    hint: String,
    catFmt: String,
) {
    val w = size.width
    val h = size.height
    val cam = Cam(w, h, camY)
    val u = cam.unit
    val alt = (camY / 250f).coerceIn(0f, 1f)
    drawRect(sky.at(alt))
    // Bulutlar: yarı hızla kayan paralaks katmanı.
    val par = camY * 0.5f
    val first = floor(par / 3.2f).toInt() - 1
    for (s in first..first + 5) {
        if (s < 0 || s % 2 != 0) continue
        val cx = cam.sx(((s * 37) % 17) / 17f * 1.5f - 0.75f)
        val cy = h - (s * 3.2f + 1.5f - par) * u
        drawOval(Cloud, topLeft = Offset(cx - u * 0.7f, cy - u * 0.22f), size = Size(u * 1.4f, u * 0.44f))
        drawOval(Cloud, topLeft = Offset(cx - u * 0.3f, cy - u * 0.4f), size = Size(u * 0.8f, u * 0.5f))
    }
    translate(fx.shakeX * u, fx.shakeY * u) {
        // Gövde ve kabuk.
        val tl = cam.sx(-0.14f)
        val tr = cam.sx(0.14f)
        drawRect(Trunk, topLeft = Offset(tl, 0f), size = Size(tr - tl, h))
        drawRect(TrunkEdge, topLeft = Offset(tl, 0f), size = Size(u * 0.06f, h))
        drawRect(TrunkEdge, topLeft = Offset(tr - u * 0.06f, 0f), size = Size(u * 0.06f, h))
        val lo = floor(camY).toInt() - 1
        val hi = lo + VIEW_LEVELS.toInt() + 3
        for (i in lo..hi) {
            if (i < 0) continue
            val by = cam.sy(i + 0.35f + ((i * 7) % 5) * 0.08f)
            val bx = tl + u * 0.12f + ((i * 13) % 7) * u * 0.05f
            drawLine(Bark, Offset(bx, by), Offset(bx + u * 0.16f, by + u * 0.04f), strokeWidth = u * 0.035f, cap = StrokeCap.Round)
            val by2 = cam.sy(i + 0.8f)
            drawLine(Bark, Offset(tr - u * 0.3f, by2), Offset(tr - u * 0.14f, by2 - u * 0.03f), strokeWidth = u * 0.03f, cap = StrokeCap.Round)
        }
        // Dallar.
        for (i in lo..hi) {
            if (i < 0 || i >= world.levels.size) continue
            val lv = world.levels[i]
            for (side in Side.entries) {
                val kind = lv.at(side)
                if (!kind.present) continue
                val current = i == world.level && side == world.side && !world.jumping && !world.falling
                val shake = if (current && world.onDry) sin(frame * 1.3f) * u * 0.05f else 0f
                drawBranch(cam, i, side, kind, shake, if (current && world.onDry) 1f - world.dryLeft / SincapWorld.DRY_HOLD else 0f, frame, path)
            }
        }
        // Erişim ipucu: her yönde konulabilecek ilk dal.
        if (world.status == SincapStatus.RUNNING && !world.jumping && !world.falling) {
            val pulse = 0.5f + 0.5f * sin(frame * 0.2f)
            for (side in Side.entries) {
                val t = world.target(side)
                if (t < 0) continue
                val cx = cam.sx(side.x * 0.5f)
                val cy = cam.sy(t + 0.55f) - pulse * u * 0.06f
                // Koyu kontur üstüne açık çizgi: gökyüzüne karşı kontrast (cihaz bulgusu: tek açık çizgi 1,32:1 kalıyordu).
                drawLine(HintOutline, Offset(cx - u * 0.14f, cy + u * 0.09f), Offset(cx, cy - u * 0.05f), strokeWidth = u * 0.12f, cap = StrokeCap.Round)
                drawLine(HintOutline, Offset(cx + u * 0.14f, cy + u * 0.09f), Offset(cx, cy - u * 0.05f), strokeWidth = u * 0.12f, cap = StrokeCap.Round)
                drawLine(Hint, Offset(cx - u * 0.14f, cy + u * 0.09f), Offset(cx, cy - u * 0.05f), strokeWidth = u * 0.05f, cap = StrokeCap.Round)
                drawLine(Hint, Offset(cx + u * 0.14f, cy + u * 0.09f), Offset(cx, cy - u * 0.05f), strokeWidth = u * 0.05f, cap = StrokeCap.Round)
            }
        }
        // Kedi: görünürse gövdede, değilse alt kenarda gösterge.
        val catScreen = cam.sy(world.catY)
        if (catScreen < h + u * 0.5f) {
            drawCat(cam.sx(0f), catScreen, u, frame)
        } else if (world.status == SincapStatus.RUNNING) {
            val label = String.format(catFmt, world.catGap.roundToInt())
            val layout = cache.getOrPut("cat|$label") {
                textMeasurer.measure(AnnotatedString(label), style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White))
            }
            val pw = layout.size.width + u * 0.7f
            val px = w / 2f - pw / 2f
            val py = h - u * 0.5f
            drawRoundRect(Color(0xAA7C2D12), topLeft = Offset(px, py - u * 0.2f), size = Size(pw, u * 0.4f), cornerRadius = CornerRadius(u * 0.2f, u * 0.2f))
            drawCatHead(px + u * 0.25f, py, u * 0.13f, frame)
            drawText(layout, topLeft = Offset(px + u * 0.5f, py - layout.size.height / 2f))
        }
        // Kargalar.
        for (c in world.crows) drawCrow(cam.sx(c.x), cam.sy(c.level.toFloat() + 0.25f), u, c.dir, frame, c.id)
        // Sincap.
        val arc = if (world.jumping) 0.45f * sin(world.jumpProgress * 3.1416f) else 0f
        val facing = if (world.jumping || world.falling) sideOf(world.x) else world.side.x
        drawSquirrel(cam.sx(world.x), cam.sy(world.y + arc), u, facing, world.jumping || world.falling, frame)
        // Parçacıklar ve yazılar.
        for (p in fx.particles) {
            drawCircle(p.color.copy(alpha = (p.life / p.maxLife).coerceIn(0f, 1f)), p.size * u, Offset(cam.sx(p.x), cam.sy(p.y)))
        }
        for (t in fx.texts) {
            val layout = cache.getOrPut("${t.text}|${t.big}|${t.color.value}") {
                textMeasurer.measure(
                    AnnotatedString(t.text),
                    style = TextStyle(fontSize = if (t.big) 22.sp else 13.sp, fontWeight = FontWeight.Black, color = t.color, textAlign = TextAlign.Center),
                )
            }
            drawText(layout, topLeft = Offset(cam.sx(t.x) - layout.size.width / 2f, cam.sy(t.y) - layout.size.height / 2f), alpha = (t.life / t.maxLife).coerceIn(0f, 1f))
        }
        if (world.status == SincapStatus.RUNNING && world.time < 6f && world.height == 0) {
            val layout = cache.getOrPut("hint|$hint") {
                textMeasurer.measure(
                    AnnotatedString(hint),
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center),
                )
            }
            val alpha = if (world.time < 5f) 1f else 6f - world.time
            drawText(layout, topLeft = Offset(w / 2f - layout.size.width / 2f, cam.sy(world.y + 3.2f) - layout.size.height / 2f), alpha = alpha.coerceIn(0f, 1f) * 0.9f)
        }
    }
    if (fx.flash > 0f) drawRect(Color(0xFFEF4444).copy(alpha = 0.3f * fx.flash))
}

private fun sideOf(x: Float): Float = if (x < 0f) -1f else 1f

private fun DrawScope.drawBranch(cam: Cam, level: Int, side: Side, kind: BranchKind, shake: Float, dryFrac: Float, frame: Long, path: Path) {
    val u = cam.unit
    val f = side.x
    val x0 = cam.sx(f * 0.13f)
    val x1 = cam.sx(f * 0.62f) + shake
    val y0 = cam.sy(level.toFloat()) + shake * 0.3f
    val y1 = y0 - u * 0.1f
    val dry = kind == BranchKind.DRY
    val wood = if (dry) lerp(DryWood, Color(0xFFEF4444), dryFrac * 0.6f) else Wood
    drawLine(Shadow, Offset(x0, y0 + u * 0.06f), Offset(x1, y1 + u * 0.06f), strokeWidth = u * 0.1f, cap = StrokeCap.Round)
    drawLine(wood, Offset(x0, y0), Offset(x1, y1), strokeWidth = if (dry) u * 0.075f else u * 0.1f, cap = StrokeCap.Round)
    if (dry) {
        val cx = cam.sx(f * 0.38f) + shake
        drawLine(DryCrack, Offset(cx - u * 0.05f, y0 - u * 0.02f), Offset(cx + u * 0.03f, y0 + u * 0.05f), strokeWidth = u * 0.025f)
        drawLine(DryCrack, Offset(cx + u * 0.03f, y0 + u * 0.05f), Offset(cx + u * 0.08f, y0 - u * 0.03f), strokeWidth = u * 0.025f)
        return
    }
    // Çınar yaprakları.
    val sway = sin(frame * 0.05f + level) * u * 0.02f
    drawCircle(Leaf1, u * 0.17f, Offset(cam.sx(f * 0.3f) + shake + sway, y0 - u * 0.16f))
    drawCircle(Leaf2, u * 0.16f, Offset(cam.sx(f * 0.46f) + shake + sway, y0 - u * 0.2f))
    drawCircle(Leaf3, u * 0.13f, Offset(cam.sx(f * 0.6f) + shake + sway, y0 - u * 0.12f))
    when (kind) {
        BranchKind.NUT, BranchKind.GOLD -> {
            val nx = cam.sx(f * 0.42f) + shake
            val ny = y0 - u * 0.12f
            val gold = kind == BranchKind.GOLD
            drawCircle(if (gold) GoldShell else NutShell, u * 0.11f, Offset(nx, ny))
            path.reset()
            path.moveTo(nx - u * 0.11f, ny - u * 0.02f)
            path.quadraticBezierTo(nx, ny - u * 0.18f, nx + u * 0.11f, ny - u * 0.02f)
            path.close()
            drawPath(path, if (gold) GoldCap else NutCap)
            if (gold) {
                val s = u * (0.14f + 0.04f * sin(frame * 0.3f))
                drawLine(Color.White, Offset(nx - s, ny + u * 0.05f), Offset(nx + s, ny + u * 0.05f), strokeWidth = u * 0.02f)
                drawLine(Color.White, Offset(nx, ny + u * 0.05f - s), Offset(nx, ny + u * 0.05f + s), strokeWidth = u * 0.02f)
            }
        }
        BranchKind.SNAKE -> {
            val sx0 = cam.sx(f * 0.5f) + shake
            drawCircle(Snake, u * 0.13f, Offset(sx0, y0 - u * 0.1f))
            drawCircle(SnakeDark, u * 0.13f, Offset(sx0, y0 - u * 0.1f), style = Stroke(width = u * 0.02f))
            drawCircle(Snake, u * 0.1f, Offset(sx0 - f * u * 0.18f, y0 - u * 0.08f))
            val hx = sx0 - f * u * 0.36f
            val hy = y0 - u * 0.16f + sin(frame * 0.15f) * u * 0.02f
            drawCircle(Snake, u * 0.08f, Offset(hx, hy))
            drawCircle(Color.White, u * 0.025f, Offset(hx - f * u * 0.03f, hy - u * 0.02f))
            drawCircle(CrowBody, u * 0.012f, Offset(hx - f * u * 0.035f, hy - u * 0.02f))
            drawLine(Tongue, Offset(hx - f * u * 0.08f, hy), Offset(hx - f * u * 0.15f, hy + u * 0.01f), strokeWidth = u * 0.015f)
        }
        else -> Unit
    }
}

private fun DrawScope.drawCrow(cx: Float, cy: Float, u: Float, dir: Int, frame: Long, id: Int) {
    val f = dir.toFloat()
    val flap = sin(frame * 0.6f + id) * u * 0.12f
    val wing = Path()
    wing.moveTo(cx - f * u * 0.05f, cy)
    wing.lineTo(cx - f * u * 0.32f, cy - u * 0.2f + flap)
    wing.lineTo(cx - f * u * 0.22f, cy + u * 0.02f)
    wing.close()
    drawPath(wing, CrowBody)
    wing.reset()
    wing.moveTo(cx + f * u * 0.05f, cy)
    wing.lineTo(cx + f * u * 0.28f, cy - u * 0.24f + flap)
    wing.lineTo(cx + f * u * 0.2f, cy + u * 0.02f)
    wing.close()
    drawPath(wing, CrowBody)
    drawOval(CrowBody, topLeft = Offset(cx - u * 0.22f, cy - u * 0.1f), size = Size(u * 0.44f, u * 0.2f))
    drawCircle(CrowBody, u * 0.09f, Offset(cx + f * u * 0.22f, cy - u * 0.06f))
    val beak = Path()
    beak.moveTo(cx + f * u * 0.28f, cy - u * 0.1f)
    beak.lineTo(cx + f * u * 0.28f, cy - u * 0.02f)
    beak.lineTo(cx + f * u * 0.4f, cy - u * 0.06f)
    beak.close()
    drawPath(beak, Beak)
    drawCircle(Color.White, u * 0.025f, Offset(cx + f * u * 0.24f, cy - u * 0.08f))
}

private fun DrawScope.drawCat(cx: Float, cy: Float, u: Float, frame: Long) {
    val bob = sin(frame * 0.3f) * u * 0.03f
    drawRoundRect(CatBody, topLeft = Offset(cx - u * 0.24f, cy - u * 0.2f + bob), size = Size(u * 0.48f, u * 0.7f), cornerRadius = CornerRadius(u * 0.2f, u * 0.2f))
    for (i in 0 until 3) {
        val y = cy + u * (0.02f + i * 0.14f) + bob
        drawLine(CatStripe, Offset(cx - u * 0.18f, y), Offset(cx + u * 0.18f, y), strokeWidth = u * 0.04f, cap = StrokeCap.Round)
    }
    drawCatHead(cx, cy - u * 0.32f + bob, u * 0.24f, frame)
    // Kuyruk.
    val tail = Path()
    tail.moveTo(cx + u * 0.2f, cy + u * 0.45f + bob)
    tail.quadraticBezierTo(cx + u * 0.5f, cy + u * 0.4f + bob, cx + u * 0.42f, cy + u * 0.05f + bob + sin(frame * 0.2f) * u * 0.05f)
    drawPath(tail, CatBody, style = Stroke(width = u * 0.08f, cap = StrokeCap.Round))
}

private fun DrawScope.drawCatHead(cx: Float, cy: Float, r: Float, frame: Long) {
    val ear = Path()
    ear.moveTo(cx - r * 0.9f, cy - r * 0.3f)
    ear.lineTo(cx - r * 0.6f, cy - r * 1.4f)
    ear.lineTo(cx - r * 0.1f, cy - r * 0.8f)
    ear.close()
    drawPath(ear, CatBody)
    ear.reset()
    ear.moveTo(cx + r * 0.9f, cy - r * 0.3f)
    ear.lineTo(cx + r * 0.6f, cy - r * 1.4f)
    ear.lineTo(cx + r * 0.1f, cy - r * 0.8f)
    ear.close()
    drawPath(ear, CatBody)
    drawCircle(CatBody, r, Offset(cx, cy))
    val blink = if ((frame / 90) % 7 == 3L && frame % 90 < 8) 0.3f else 1f
    drawOval(CatEye, topLeft = Offset(cx - r * 0.55f, cy - r * 0.2f * blink), size = Size(r * 0.35f, r * 0.4f * blink))
    drawOval(CatEye, topLeft = Offset(cx + r * 0.2f, cy - r * 0.2f * blink), size = Size(r * 0.35f, r * 0.4f * blink))
    drawCircle(CrowBody, r * 0.08f, Offset(cx - r * 0.37f, cy))
    drawCircle(CrowBody, r * 0.08f, Offset(cx + r * 0.37f, cy))
    drawCircle(Tongue, r * 0.1f, Offset(cx, cy + r * 0.25f))
}

private fun DrawScope.drawSquirrel(cx: Float, cy: Float, u: Float, facing: Float, airborne: Boolean, frame: Long) {
    val f = facing
    val stretch = if (airborne) 1.25f else 1f
    val bob = if (airborne) 0f else sin(frame * 0.15f) * u * 0.01f
    // Kuyruk: gövdeden geriye kıvrılan kalın yay.
    val tail = Path()
    tail.moveTo(cx - f * u * 0.1f, cy - u * 0.05f + bob)
    tail.quadraticBezierTo(cx - f * u * 0.42f, cy + u * 0.05f, cx - f * u * 0.36f, cy - u * 0.5f + bob + sin(frame * 0.2f) * u * 0.03f)
    drawPath(tail, SquirrelTail, style = Stroke(width = u * 0.16f, cap = StrokeCap.Round))
    drawOval(Squirrel, topLeft = Offset(cx - u * 0.16f * stretch, cy - u * 0.3f + bob), size = Size(u * 0.32f * stretch, u * 0.4f))
    drawOval(SquirrelBelly, topLeft = Offset(cx - u * 0.07f + f * u * 0.04f, cy - u * 0.18f + bob), size = Size(u * 0.14f, u * 0.22f))
    val hx = cx + f * u * 0.14f
    val hy = cy - u * 0.36f + bob
    drawCircle(Squirrel, u * 0.14f, Offset(hx, hy))
    val ear = Path()
    ear.moveTo(hx - f * u * 0.02f, hy - u * 0.1f)
    ear.lineTo(hx - f * u * 0.06f, hy - u * 0.26f)
    ear.lineTo(hx + f * u * 0.07f, hy - u * 0.12f)
    ear.close()
    drawPath(ear, Squirrel)
    drawCircle(Color.White, u * 0.035f, Offset(hx + f * u * 0.06f, hy - u * 0.03f))
    drawCircle(CrowBody, u * 0.02f, Offset(hx + f * u * 0.07f, hy - u * 0.03f))
    drawCircle(CrowBody, u * 0.02f, Offset(hx + f * u * 0.14f, hy + u * 0.02f))
    // Pençeler.
    drawLine(NutCap, Offset(cx + f * u * 0.02f, cy + u * 0.08f + bob), Offset(cx + f * u * 0.1f, cy + u * 0.1f + bob), strokeWidth = u * 0.04f, cap = StrokeCap.Round)
    drawLine(NutCap, Offset(cx - f * u * 0.06f, cy + u * 0.09f + bob), Offset(cx - f * u * 0.12f, cy + u * 0.1f + bob), strokeWidth = u * 0.04f, cap = StrokeCap.Round)
}

// ---------------------------------------------------------------------------
// Kartlar
// ---------------------------------------------------------------------------

@Composable
private fun StartCard(
    dailyMode: Boolean,
    daily: SincapDaily?,
    freeBest: Int,
    freeHeight: Int,
    onMode: (Boolean) -> Unit,
    onStart: () -> Unit,
    onExit: () -> Unit,
) {
    val exhausted = dailyMode && daily != null && daily.attempts >= SincapViewModel.DAILY_ATTEMPTS
    OverlayCard {
        Text(
            text = stringResource(R.string.game_sincap),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.sincap_intro),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
        Text(
            text = stringResource(R.string.sincap_rules),
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
                    text = stringResource(R.string.sincap_daily_status_fmt, daily.attempts, SincapViewModel.DAILY_ATTEMPTS, daily.best),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = stringResource(if (exhausted) R.string.sincap_daily_exhausted else R.string.sincap_daily_desc),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        } else if (freeBest > 0) {
            Text(
                text = stringResource(R.string.sincap_best_fmt, freeBest, freeHeight),
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
                Text(stringResource(R.string.sincap_start))
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
            Text(stringResource(R.string.sincap_to_menu))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

@Composable
private fun OverCard(
    hud: SincapHud,
    daily: Boolean,
    attemptsLeft: Int,
    isRecord: Boolean,
    onRestart: () -> Unit,
    onMenu: () -> Unit,
    onExit: () -> Unit,
) {
    val result = stringResource(R.string.sincap_result_fmt, hud.height, hud.nuts)
    val title = hud.cause?.let { causeLabel(it) } ?: stringResource(R.string.sincap_over_fall)
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
        if (daily && attemptsLeft <= 0) {
            Text(
                text = stringResource(R.string.sincap_daily_exhausted),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        ShareButton(
            ShareContent(
                gameId = "sincap",
                headline = stringResource(R.string.sincap_share_fmt, hud.height, hud.score),
                details = listOf(result, modeShareLabel(daily, null)),
            ),
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text(
                when {
                    daily && attemptsLeft > 0 -> stringResource(R.string.sincap_retry_fmt, attemptsLeft)
                    daily -> stringResource(R.string.play_free)
                    else -> stringResource(R.string.restart)
                },
            )
        }
        TextButton(onClick = onMenu, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.sincap_to_menu))
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
