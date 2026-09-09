package com.za.games.ui.filo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import com.za.games.filo.Enemy
import com.za.games.filo.EnemyKind
import com.za.games.filo.FiloHud
import com.za.games.filo.FiloWorld
import com.za.games.filo.PowerKind
import com.za.games.platform.LocalZaHaptics
import com.za.games.platform.LocalZaSound
import com.za.games.platform.ShareContent
import com.za.games.ui.common.GameTopBar
import com.za.games.ui.common.OverlayCard
import com.za.games.ui.common.PadButton
import com.za.games.ui.common.ScoreCard
import com.za.games.ui.common.ShareButton
import com.za.games.ui.common.formatScore
import com.za.games.ui.common.modeShareLabel
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val SpaceTop = Color(0xFF060B1C)
private val SpaceBottom = Color(0xFF111A3A)
private val StarColor = Color(0xFFE2E8F0)
private val Hull = Color(0xFFE2E8F0)
private val HullDark = Color(0xFF94A3B8)
private val Stripe = Color(0xFFFB7185)
private val Cockpit = Color(0xFF22D3EE)
private val Flame = Color(0xFFFB923C)
private val FlameCore = Color(0xFFFDE68A)
private val ShieldRing = Color(0xFF93C5FD)
private val PlayerShot = Color(0xFF67E8F9)
private val PlayerShotCore = Color(0xFFF0FDFF)
private val EnemyShot = Color(0xFFFB7185)
private val EnemyShotGlow = Color(0x66FB7185)
private val DroneBody = Color(0xFFF87171)
private val DroneEye = Color(0xFFFEE2E2)
private val WaspBody = Color(0xFFFBBF24)
private val WaspStripe = Color(0xFF1F2937)
private val TankBody = Color(0xFF94A3B8)
private val TankCore = Color(0xFF475569)
private val Rock = Color(0xFF78716C)
private val RockDark = Color(0xFF57534E)
private val BossBody = Color(0xFF7C3AED)
private val BossWing = Color(0xFF5B21B6)
private val BossEye = Color(0xFFE9D5FF)
private val BossGlow = Color(0xFFF472B6)
private val HpBack = Color(0x66000000)
private val HpFill = Color(0xFF4ADE80)
private val PowerWeapon = Color(0xFF22D3EE)
private val PowerShield = Color(0xFF93C5FD)
private val PowerBomb = Color(0xFFFB923C)
private val PowerScore = Color(0xFFFDE68A)
private val PowerText = Color(0xFF0F172A)
private val Flash = Color(0xFFFFFFFF)
private val AsteroidRadii = floatArrayOf(1f, 0.8f, 1.05f, 0.85f, 1f, 0.75f, 0.95f)

@Composable
fun FiloScreen(
    highScore: Long,
    onScore: (Long) -> Unit,
    onExit: () -> Unit,
    viewModel: FiloViewModel = viewModel(),
) {
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val daily by viewModel.daily.collectAsStateWithLifecycle()
    val hud by viewModel.hud.collectAsStateWithLifecycle()
    val runId by viewModel.runId.collectAsStateWithLifecycle()
    val leftHanded by viewModel.leftHanded.collectAsStateWithLifecycle()
    val haptics = LocalZaHaptics.current
    val sound = LocalZaSound.current
    val resources = LocalContext.current.resources
    val fx = remember { FiloFx() }
    val fxTick = remember { mutableLongStateOf(0L) }

    val latestScore by rememberUpdatedState(hud.score)
    val latestOnScore by rememberUpdatedState(onScore)
    DisposableEffect(Unit) {
        onDispose { latestOnScore(latestScore) }
    }
    LaunchedEffect(phase) {
        if (phase == FiloPhase.OVER) latestOnScore(hud.score)
    }
    LaunchedEffect(runId) { fx.reset() }
    LaunchedEffect(Unit) { viewModel.refreshDaily() }
    LifecycleResumeEffect(Unit) {
        onPauseOrDispose { viewModel.pause() }
    }
    BackHandler {
        if (phase == FiloPhase.PLAYING) viewModel.pause() else onExit()
    }

    LaunchedEffect(phase, runId) {
        if (phase != FiloPhase.PLAYING && phase != FiloPhase.OVER) return@LaunchedEffect
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
            if (phase == FiloPhase.OVER && !fx.isBusy) break
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
    val attemptsLeft = FiloViewModel.DAILY_ATTEMPTS - (daily?.attempts ?: 0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        GameTopBar(title = stringResource(R.string.game_filo), onExit = onExit) {
            if (phase == FiloPhase.PLAYING || phase == FiloPhase.PAUSED) {
                TextButton(onClick = viewModel::togglePause) {
                    Text(stringResource(if (phase == FiloPhase.PAUSED) R.string.resume else R.string.pause))
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
                modifier = Modifier.weight(1.2f),
                highlight = true,
            )
            ScoreCard(
                label = stringResource(R.string.filo_wave),
                value = hud.wave.toString(),
                modifier = Modifier.weight(0.7f),
            )
            ScoreCard(
                label = stringResource(R.string.filo_lives),
                value = if (hud.lives > 0) "♥".repeat(hud.lives) else "–",
                modifier = Modifier.weight(0.9f),
            )
        }

        StatusBar(hud = hud, visible = phase == FiloPhase.PLAYING || phase == FiloPhase.PAUSED)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            FiloCanvas(viewModel = viewModel, fx = fx, fxTick = fxTick, hud = hud, modifier = Modifier.fillMaxSize())
            when (phase) {
                FiloPhase.MENU -> StartCard(
                    mode = mode,
                    daily = daily,
                    leftHanded = leftHanded,
                    onHand = viewModel::setLeftHanded,
                    onMode = viewModel::setMode,
                    onStart = startRun,
                    onExit = onExit,
                )
                FiloPhase.PAUSED -> PauseCard(
                    leftHanded = leftHanded,
                    onHand = viewModel::setLeftHanded,
                    onResume = viewModel::resume,
                    onRestart = restartRun,
                    onMenu = viewModel::toMenu,
                    onExit = onExit,
                )
                FiloPhase.OVER -> OverCard(
                    hud = hud,
                    daily = mode == FiloMode.DAILY,
                    attemptsLeft = attemptsLeft,
                    isRecord = hud.score > previousBest,
                    onRestart = restartRun,
                    onMenu = viewModel::toMenu,
                    onExit = onExit,
                )
                FiloPhase.PLAYING -> Unit
            }
        }

        Controls(leftHanded = leftHanded, bombs = hud.bombs, onBomb = { viewModel.bomb() })
    }
}

/** Dalga ilerlemesi ya da patron canı; sağda zincir çarpanı, silah seviyesi ve kalkan. */
@Composable
private fun StatusBar(hud: FiloHud, visible: Boolean) {
    val alpha = if (visible) 1f else 0f
    val boss = hud.bossHp >= 0f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(if (boss) R.string.filo_boss_hp else R.string.filo_wave),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = (if (boss) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.8f * alpha),
        )
        val fill = if (boss) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        val track = MaterialTheme.colorScheme.surfaceVariant
        val fraction = if (boss) hud.bossHp else hud.waveProgress
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(CircleShape),
        ) {
            drawRect(track.copy(alpha = alpha))
            drawRect(fill.copy(alpha = alpha), size = Size(size.width * fraction.coerceIn(0f, 1f), size.height))
        }
        StatusChip(
            text = stringResource(R.string.filo_multiplier_fmt, hud.multiplier),
            strong = hud.multiplier > 1,
            alpha = alpha,
        )
        StatusChip(
            text = stringResource(R.string.filo_weapon_fmt, hud.weapon),
            strong = hud.weapon > 1,
            alpha = alpha,
        )
        if (hud.shield) {
            StatusChip(text = stringResource(R.string.filo_shield_on), strong = true, alpha = alpha)
        }
    }
}

@Composable
private fun StatusChip(text: String, strong: Boolean, alpha: Float) {
    val base = if (strong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    Surface(
        shape = CircleShape,
        color = base.copy(alpha = (if (strong) 0.25f else 1f) * alpha),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (strong) FontWeight.Bold else FontWeight.Medium,
            color = (if (strong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = alpha),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Çizim
// ---------------------------------------------------------------------------

/** Oyun alanı birimi başına piksel: alan tuvale sığacak şekilde ölçeklenir. */
private fun arenaScale(width: Float, height: Float): Float =
    min(width / FiloWorld.WIDTH, height / FiloWorld.HEIGHT)

@Composable
private fun FiloCanvas(
    viewModel: FiloViewModel,
    fx: FiloFx,
    fxTick: MutableLongState,
    hud: FiloHud,
    modifier: Modifier = Modifier,
) {
    val frame by viewModel.frame.collectAsStateWithLifecycle()
    val desc = stringResource(R.string.filo_board_desc, hud.wave, hud.lives, hud.bombs)
    val textMeasurer = rememberTextMeasurer()
    val textCache = remember { HashMap<String, TextLayoutResult>() }
    val path = remember { Path() }
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .semantics { contentDescription = desc }
            .pointerInput(viewModel) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val scale = arenaScale(size.width.toFloat(), size.height.toFloat())
                    if (scale > 0f) viewModel.drag(dragAmount.x / scale)
                }
            },
    ) {
        // Kare sayaçları okunur ki her adımda yeniden çizilsin.
        val tick = frame + fxTick.longValue
        if (tick < 0L) return@Canvas
        drawScene(viewModel.world, fx, path, textMeasurer, textCache)
    }
}

private fun DrawScope.drawScene(
    world: FiloWorld,
    fx: FiloFx,
    path: Path,
    textMeasurer: TextMeasurer,
    cache: HashMap<String, TextLayoutResult>,
) {
    val w = size.width
    val h = size.height
    val s = arenaScale(w, h)
    val ox = (w - FiloWorld.WIDTH * s) / 2f
    val oy = (h - FiloWorld.HEIGHT * s) / 2f

    drawRect(Brush.verticalGradient(listOf(SpaceTop, SpaceBottom)))
    for (star in fx.stars) {
        drawCircle(StarColor.copy(alpha = star.alpha), radius = star.size * s, center = Offset(star.x * w, star.y * h))
    }

    translate(ox + fx.shakeX * s, oy + fx.shakeY * s) {
        for (p in world.powers) drawPower(p.x * s, p.y * s, FiloWorld.POWER_RADIUS * s, p.kind, world.frames, textMeasurer, cache)
        for (e in world.enemies) drawEnemy(path, e, s)
        for (b in world.enemyBullets) {
            drawCircle(EnemyShotGlow, radius = b.radius * s * 2f, center = Offset(b.x * s, b.y * s))
            drawCircle(EnemyShot, radius = b.radius * s, center = Offset(b.x * s, b.y * s))
        }
        for (b in world.bullets) {
            val bw = b.radius * s * 1.6f
            val bh = 0.045f * s
            drawRoundRect(PlayerShot, topLeft = Offset(b.x * s - bw / 2f, b.y * s - bh / 2f), size = Size(bw, bh), cornerRadius = CornerRadius(bw / 2f, bw / 2f))
            drawRoundRect(PlayerShotCore, topLeft = Offset(b.x * s - bw / 4f, b.y * s - bh / 2.4f), size = Size(bw / 2f, bh / 1.2f), cornerRadius = CornerRadius(bw / 4f, bw / 4f))
        }
        drawPlayer(path, world, s)
        for (q in fx.particles) {
            drawCircle(q.color.copy(alpha = (q.life / q.maxLife).coerceIn(0f, 1f)), radius = q.size * s, center = Offset(q.x * s, q.y * s))
        }
        drawTexts(fx, s, textMeasurer, cache)
    }

    if (fx.flash > 0f) drawRect(fx.flashColor.copy(alpha = 0.35f * fx.flash))
}

private fun DrawScope.polygon(
    path: Path,
    cx: Float,
    cy: Float,
    r: Float,
    sides: Int,
    rotation: Float,
    color: Color,
    radii: FloatArray? = null,
    alpha: Float = 1f,
) {
    path.reset()
    for (i in 0 until sides) {
        val a = rotation + i * 2f * PI.toFloat() / sides
        val rr = r * (radii?.get(i % radii.size) ?: 1f)
        val px = cx + rr * cos(a)
        val py = cy + rr * sin(a)
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, color, alpha = alpha)
}

private fun DrawScope.drawEnemy(path: Path, e: Enemy, s: Float) {
    val cx = e.x * s
    val cy = e.y * s
    val r = e.kind.radius * s
    when (e.kind) {
        EnemyKind.DRONE -> {
            path.reset()
            path.moveTo(cx, cy + r)
            path.lineTo(cx + r, cy - r * 0.6f)
            path.lineTo(cx, cy - r * 0.25f)
            path.lineTo(cx - r, cy - r * 0.6f)
            path.close()
            drawPath(path, DroneBody)
            drawCircle(DroneEye, radius = r * 0.22f, center = Offset(cx, cy + r * 0.1f))
        }
        EnemyKind.WASP -> {
            drawRoundRect(WaspBody, topLeft = Offset(cx - r * 1.3f, cy - r * 0.3f), size = Size(r * 2.6f, r * 0.6f), cornerRadius = CornerRadius(r * 0.3f, r * 0.3f))
            path.reset()
            path.moveTo(cx, cy + r * 1.1f)
            path.lineTo(cx + r * 0.55f, cy - r * 0.9f)
            path.lineTo(cx - r * 0.55f, cy - r * 0.9f)
            path.close()
            drawPath(path, WaspBody)
            drawRect(WaspStripe, topLeft = Offset(cx - r * 0.4f, cy - r * 0.1f), size = Size(r * 0.8f, r * 0.22f))
            drawRect(WaspStripe, topLeft = Offset(cx - r * 0.3f, cy + r * 0.35f), size = Size(r * 0.6f, r * 0.2f))
        }
        EnemyKind.TANK -> {
            polygon(path, cx, cy, r, 6, PI.toFloat() / 6f, TankBody)
            polygon(path, cx, cy, r * 0.55f, 6, PI.toFloat() / 6f, TankCore)
            drawRect(TankCore, topLeft = Offset(cx - r * 0.12f, cy + r * 0.4f), size = Size(r * 0.24f, r * 0.7f))
            if (e.hp < e.maxHp) drawHp(cx, cy - r * 1.35f, r * 1.6f, e.hp / e.maxHp.toFloat())
        }
        EnemyKind.ASTEROID -> {
            polygon(path, cx, cy, r, AsteroidRadii.size, e.t * 1.1f, Rock, AsteroidRadii)
            polygon(path, cx + r * 0.15f, cy + r * 0.1f, r * 0.45f, AsteroidRadii.size, e.t * 1.1f + 0.4f, RockDark, AsteroidRadii)
        }
        EnemyKind.BOSS -> {
            path.reset()
            path.moveTo(cx - r * 1.6f, cy - r * 0.5f)
            path.lineTo(cx - r * 0.7f, cy + r * 0.5f)
            path.lineTo(cx + r * 0.7f, cy + r * 0.5f)
            path.lineTo(cx + r * 1.6f, cy - r * 0.5f)
            path.lineTo(cx + r * 1.2f, cy - r * 0.7f)
            path.lineTo(cx - r * 1.2f, cy - r * 0.7f)
            path.close()
            drawPath(path, BossWing)
            drawRoundRect(BossBody, topLeft = Offset(cx - r * 0.75f, cy - r), size = Size(r * 1.5f, r * 2f), cornerRadius = CornerRadius(r * 0.5f, r * 0.5f))
            val pulse = 0.5f + 0.5f * sin(e.t * 6f)
            drawCircle(BossGlow.copy(alpha = 0.35f + 0.35f * pulse), radius = r * 0.42f, center = Offset(cx, cy + r * 0.35f))
            drawCircle(BossEye, radius = r * 0.22f, center = Offset(cx, cy + r * 0.35f))
            drawCircle(BossEye, radius = r * 0.1f, center = Offset(cx - r * 0.45f, cy - r * 0.35f))
            drawCircle(BossEye, radius = r * 0.1f, center = Offset(cx + r * 0.45f, cy - r * 0.35f))
        }
    }
    if (e.flash > 0f) {
        drawCircle(Flash.copy(alpha = (e.flash / 0.12f).coerceIn(0f, 1f) * 0.6f), radius = r * 1.15f, center = Offset(cx, cy))
    }
}

private fun DrawScope.drawHp(cx: Float, cy: Float, width: Float, fraction: Float) {
    val hgt = width * 0.12f
    drawRoundRect(HpBack, topLeft = Offset(cx - width / 2f, cy - hgt / 2f), size = Size(width, hgt), cornerRadius = CornerRadius(hgt / 2f, hgt / 2f))
    drawRoundRect(HpFill, topLeft = Offset(cx - width / 2f, cy - hgt / 2f), size = Size(width * fraction.coerceIn(0f, 1f), hgt), cornerRadius = CornerRadius(hgt / 2f, hgt / 2f))
}

private fun DrawScope.drawPower(
    cx: Float,
    cy: Float,
    r: Float,
    kind: PowerKind,
    frames: Int,
    textMeasurer: TextMeasurer,
    cache: HashMap<String, TextLayoutResult>,
) {
    val (color, letter) = when (kind) {
        PowerKind.WEAPON -> PowerWeapon to "W"
        PowerKind.SHIELD -> PowerShield to "S"
        PowerKind.BOMB -> PowerBomb to "B"
        PowerKind.SCORE -> PowerScore to "+"
    }
    val pulse = 1f + 0.08f * sin(frames * 0.2f)
    drawCircle(color.copy(alpha = 0.3f), radius = r * 1.5f * pulse, center = Offset(cx, cy))
    drawCircle(color, radius = r * pulse, center = Offset(cx, cy))
    val layout = cache.getOrPut("power|$letter|${r.toInt()}") {
        textMeasurer.measure(
            AnnotatedString(letter),
            style = TextStyle(fontSize = (r * 1.2f).toSp(), fontWeight = FontWeight.Black, color = PowerText, textAlign = TextAlign.Center),
        )
    }
    drawText(layout, topLeft = Offset(cx - layout.size.width / 2f, cy - layout.size.height / 2f))
}

private fun DrawScope.drawPlayer(path: Path, world: FiloWorld, s: Float) {
    val cx = world.playerX * s
    val cy = world.playerY * s
    val r = FiloWorld.PLAYER_RADIUS * s
    val blink = world.invuln > 0f && (world.frames / 4) % 2 == 0
    val alpha = if (blink) 0.35f else 1f

    // Alev.
    val flick = 0.7f + 0.3f * sin(world.frames * 0.9f)
    path.reset()
    path.moveTo(cx - r * 0.35f, cy + r * 0.75f)
    path.lineTo(cx, cy + r * (0.9f + 1.1f * flick))
    path.lineTo(cx + r * 0.35f, cy + r * 0.75f)
    path.close()
    drawPath(path, Flame, alpha = alpha)
    path.reset()
    path.moveTo(cx - r * 0.18f, cy + r * 0.75f)
    path.lineTo(cx, cy + r * (0.9f + 0.6f * flick))
    path.lineTo(cx + r * 0.18f, cy + r * 0.75f)
    path.close()
    drawPath(path, FlameCore, alpha = alpha)

    // Gövde.
    path.reset()
    path.moveTo(cx, cy - r * 1.35f)
    path.lineTo(cx + r * 0.45f, cy - r * 0.1f)
    path.lineTo(cx + r * 1.25f, cy + r * 0.85f)
    path.lineTo(cx + r * 0.4f, cy + r * 0.75f)
    path.lineTo(cx, cy + r * 0.5f)
    path.lineTo(cx - r * 0.4f, cy + r * 0.75f)
    path.lineTo(cx - r * 1.25f, cy + r * 0.85f)
    path.lineTo(cx - r * 0.45f, cy - r * 0.1f)
    path.close()
    drawPath(path, Hull, alpha = alpha)
    drawRect(Stripe.copy(alpha = alpha), topLeft = Offset(cx - r * 0.12f, cy - r * 0.9f), size = Size(r * 0.24f, r * 1.3f))
    drawCircle(Cockpit.copy(alpha = alpha), radius = r * 0.2f, center = Offset(cx, cy - r * 0.45f))
    if (world.weapon >= 2) {
        drawRect(HullDark.copy(alpha = alpha), topLeft = Offset(cx - r * 1.05f, cy + r * 0.05f), size = Size(r * 0.16f, r * 0.55f))
        drawRect(HullDark.copy(alpha = alpha), topLeft = Offset(cx + r * 0.89f, cy + r * 0.05f), size = Size(r * 0.16f, r * 0.55f))
    }
    if (world.weapon >= 3) {
        drawRect(Cockpit.copy(alpha = alpha), topLeft = Offset(cx - r * 0.6f, cy + r * 0.15f), size = Size(r * 0.12f, r * 0.4f))
        drawRect(Cockpit.copy(alpha = alpha), topLeft = Offset(cx + r * 0.48f, cy + r * 0.15f), size = Size(r * 0.12f, r * 0.4f))
    }
    if (world.shield) {
        val pulse = 0.85f + 0.15f * sin(world.frames * 0.25f)
        drawCircle(ShieldRing.copy(alpha = 0.18f * pulse), radius = r * 1.9f, center = Offset(cx, cy))
        drawCircle(ShieldRing.copy(alpha = 0.9f * pulse), radius = r * 1.9f, center = Offset(cx, cy), style = Stroke(width = r * 0.12f))
    }
}

private fun DrawScope.drawTexts(fx: FiloFx, s: Float, textMeasurer: TextMeasurer, cache: HashMap<String, TextLayoutResult>) {
    for (t in fx.texts) {
        val key = "${t.text}|${t.big}|${t.color.value}"
        val layout = cache.getOrPut(key) {
            textMeasurer.measure(
                AnnotatedString(t.text),
                style = TextStyle(
                    fontSize = if (t.big) 26.sp else 16.sp,
                    fontWeight = FontWeight.Black,
                    color = t.color,
                    textAlign = TextAlign.Center,
                ),
            )
        }
        val alpha = (t.life / t.maxLife).coerceIn(0f, 1f)
        drawText(layout, topLeft = Offset(t.x * s - layout.size.width / 2f, t.y * s - layout.size.height / 2f), alpha = alpha)
    }
}

// ---------------------------------------------------------------------------
// Kartlar ve kontroller
// ---------------------------------------------------------------------------

@Composable
private fun StartCard(
    mode: FiloMode,
    daily: FiloDaily?,
    leftHanded: Boolean,
    onHand: (Boolean) -> Unit,
    onMode: (FiloMode) -> Unit,
    onStart: () -> Unit,
    onExit: () -> Unit,
) {
    val exhausted = daily != null && daily.attempts >= FiloViewModel.DAILY_ATTEMPTS
    OverlayCard {
        Text(
            text = stringResource(R.string.game_filo),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.filo_intro),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeChip(stringResource(R.string.mode_daily), mode == FiloMode.DAILY, Modifier.weight(1f)) { onMode(FiloMode.DAILY) }
            ModeChip(stringResource(R.string.mode_free), mode == FiloMode.FREE, Modifier.weight(1f)) { onMode(FiloMode.FREE) }
        }
        if (mode == FiloMode.DAILY) {
            if (daily != null) {
                Text(
                    text = stringResource(R.string.filo_daily_status_fmt, daily.attempts, FiloViewModel.DAILY_ATTEMPTS, daily.best),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = stringResource(if (exhausted) R.string.filo_daily_exhausted else R.string.filo_daily_desc),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        Text(
            text = stringResource(R.string.kuyu_hand_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        HandChips(leftHanded = leftHanded, onHand = onHand)
        Text(
            text = stringResource(R.string.filo_hand_hint),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        )
        Spacer(Modifier.height(4.dp))
        if (mode == FiloMode.DAILY && exhausted) {
            Button(onClick = { onMode(FiloMode.FREE) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.play_free))
            }
        } else {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.filo_start))
            }
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

@Composable
private fun PauseCard(
    leftHanded: Boolean,
    onHand: (Boolean) -> Unit,
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
        HandChips(leftHanded = leftHanded, onHand = onHand)
        Spacer(Modifier.height(4.dp))
        Button(onClick = onResume, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.resume))
        }
        OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.restart))
        }
        TextButton(onClick = onMenu, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.filo_to_menu))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

@Composable
private fun OverCard(
    hud: FiloHud,
    daily: Boolean,
    attemptsLeft: Int,
    isRecord: Boolean,
    onRestart: () -> Unit,
    onMenu: () -> Unit,
    onExit: () -> Unit,
) {
    val result = stringResource(R.string.filo_result_fmt, hud.wave, hud.kills)
    OverlayCard {
        Text(
            text = stringResource(R.string.filo_over_title),
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
                text = stringResource(R.string.filo_daily_exhausted),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        ShareButton(
            ShareContent(
                gameId = "filo",
                headline = stringResource(R.string.share_score_fmt, formatScore(hud.score)),
                details = listOf(result, modeShareLabel(daily, null)),
            ),
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text(
                if (daily && attemptsLeft > 0) {
                    stringResource(R.string.filo_retry_fmt, attemptsLeft)
                } else if (daily) {
                    stringResource(R.string.play_free)
                } else {
                    stringResource(R.string.restart)
                },
            )
        }
        TextButton(onClick = onMenu, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.filo_to_menu))
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

@Composable
private fun HandChips(leftHanded: Boolean, onHand: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ModeChip(stringResource(R.string.kuyu_hand_right), !leftHanded, Modifier.weight(1f)) { onHand(false) }
        ModeChip(stringResource(R.string.kuyu_hand_left), leftHanded, Modifier.weight(1f)) { onHand(true) }
    }
}

/** Bomba tuşu seçilen başparmağın tarafında; öbür yanda sürükleme ipucu. */
@Composable
private fun Controls(leftHanded: Boolean, bombs: Int, onBomb: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(64.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val hint = @Composable {
            Text(
                text = stringResource(R.string.filo_drag_hint),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.width(120.dp),
            )
        }
        val bomb = @Composable {
            PadButton(
                label = stringResource(R.string.filo_bomb_count_fmt, bombs),
                description = stringResource(R.string.filo_ctrl_bomb),
                modifier = Modifier
                    .width(150.dp)
                    .fillMaxHeight(),
                accent = true,
                fontSize = 18.sp,
                onAction = onBomb,
            )
        }
        if (leftHanded) {
            bomb()
            Spacer(Modifier.weight(1f))
            hint()
        } else {
            hint()
            Spacer(Modifier.weight(1f))
            bomb()
        }
    }
}
