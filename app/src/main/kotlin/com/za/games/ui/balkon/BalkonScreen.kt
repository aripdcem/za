package com.za.games.ui.balkon

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.za.games.R
import com.za.games.balkon.BalkonHud
import com.za.games.balkon.BalkonWorld
import com.za.games.balkon.Shot
import com.za.games.balkon.Target
import com.za.games.balkon.TargetKind
import com.za.games.platform.LocalZaHaptics
import com.za.games.platform.LocalZaSound
import com.za.games.ui.common.GameTopBar
import com.za.games.ui.common.OverlayCard
import com.za.games.ui.common.PadButton
import com.za.games.ui.common.ScoreCard
import com.za.games.ui.common.formatScore
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val Wall = Color(0xFF7C5C46)
private val WallDark = Color(0xFF5C4133)
private val BalconyFloor = Color(0xFF4A3A30)
private val Rail = Color(0xFF1F2937)
private val PavementA = Color(0xFFA1A7B0)
private val PavementB = Color(0xFF959BA4)
private val Curb = Color(0xFFD1D5DB)
private val Asphalt = Color(0xFF3F3F46)
private val AsphaltEdge = Color(0xFF52525B)
private val RoadLine = Color(0xFFE5E7EB)
private val TreeColor = Color(0xFF166534)
private val TreeLight = Color(0xFF22863A)
private val BinColor = Color(0xFF374151)
private val Skin = Color(0xFFF1C27D)
private val Hair = Color(0xFF3B2A20)
private val Shirt = Color(0xFF2563EB)
private val ShadowColor = Color(0x55000000)
private val PigeonBody = Color(0xFF9CA3AF)
private val PigeonHead = Color(0xFF6B7280)
private val CatBody = Color(0xFFF59E0B)
private val CatDark = Color(0xFFB45309)
private val BallA = Color(0xFFEF4444)
private val BallB = Color(0xFFFDE68A)
private val BikeFrame = Color(0xFF16A34A)
private val Wheel = Color(0xFF111827)
private val Helmet = Color(0xFFF97316)
private val ScooterBody = Color(0xFFE11D48)
private val CourierBox = Color(0xFFF59E0B)
private val CarWindow = Color(0xFF93C5FD)
private val CarColors = listOf(
    Color(0xFFF87171), Color(0xFF60A5FA), Color(0xFFE2E8F0),
    Color(0xFFA78BFA), Color(0xFFFBBF24), Color(0xFF4ADE80),
)
private val Tray = Color(0xFF8B5E3C)
private val Simit = Color(0xFFD97706)
private val JanitorVest = Color(0xFF1D4ED8)
private val Broom = Color(0xFFB45309)
private val Scarf = Color(0xFFDB2777)
private val ScarfDot = Color(0xFFFDE68A)
private val Bag = Color(0xFF7C3AED)
private val Seed = Color(0xFFE7D3A1)
private val SeedDark = Color(0xFFB08D57)
private val Balloon = Color(0xFF38BDF8)
private val BalloonLight = Color(0xFFBAE6FD)
private val Bucket = Color(0xFF9CA3AF)
private val BucketDark = Color(0xFF4B5563)
private val Slime = Color(0xFF4ADE80)
private val SlimeDark = Color(0xFF15803D)
private val Phlegm = Color(0xFF65A30D)
private val Puddle = Color(0x8838BDF8)
private val FlagColor = Color(0xFFEF4444)

@Composable
fun BalkonScreen(
    highScore: Long,
    onScore: (Long) -> Unit,
    onExit: () -> Unit,
    viewModel: BalkonViewModel = viewModel(),
) {
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val hud by viewModel.hud.collectAsStateWithLifecycle()
    val runId by viewModel.runId.collectAsStateWithLifecycle()
    val megaArmed by viewModel.megaArmed.collectAsStateWithLifecycle()
    val bestLevel by viewModel.bestLevel.collectAsStateWithLifecycle()
    val haptics = LocalZaHaptics.current
    val sound = LocalZaSound.current
    val resources = LocalContext.current.resources
    val fx = remember { BalkonFx() }
    val fxTick = remember { mutableLongStateOf(0L) }

    val latestScore by rememberUpdatedState(hud.score)
    val latestOnScore by rememberUpdatedState(onScore)
    DisposableEffect(Unit) {
        onDispose { latestOnScore(latestScore) }
    }
    LaunchedEffect(phase) {
        if (phase == BalkonPhase.OVER) latestOnScore(hud.score)
    }
    LaunchedEffect(runId) { fx.reset(viewModel.runTheme) }
    LifecycleResumeEffect(Unit) {
        onPauseOrDispose { viewModel.pause() }
    }
    BackHandler {
        if (phase == BalkonPhase.PLAYING) viewModel.pause() else onExit()
    }

    // Kare döngüsü: oyun koşarken ve bitiş efektleri sönene dek.
    LaunchedEffect(phase, runId) {
        if (phase != BalkonPhase.PLAYING && phase != BalkonPhase.OVER) return@LaunchedEffect
        var last = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = now - last
                    val world = viewModel.world
                    for (event in viewModel.advance(dt)) {
                        fx.onEvent(event, world, sound, haptics, resources)
                    }
                    fx.update(dt / 1_000_000_000f)
                    fxTick.longValue += 1
                }
                last = now
            }
            if (phase == BalkonPhase.OVER && !fx.isBusy) break
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
    val megaName = stringResource(megaNameRes(viewModel.runTheme))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        GameTopBar(title = stringResource(R.string.game_balkon), onExit = onExit) {
            if (phase == BalkonPhase.PLAYING || phase == BalkonPhase.PAUSED) {
                TextButton(onClick = viewModel::togglePause) {
                    Text(text = stringResource(if (phase == BalkonPhase.PAUSED) R.string.resume else R.string.pause))
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
                label = stringResource(R.string.high_score),
                value = formatScore(maxOf(highScore, hud.score)),
                modifier = Modifier.weight(1f),
            )
            ScoreCard(
                label = stringResource(R.string.balkon_level_label),
                value = hud.level.toString(),
                modifier = Modifier.weight(0.8f),
            )
        }

        StatusRow(hud = hud, visible = phase == BalkonPhase.PLAYING || phase == BalkonPhase.PAUSED)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            BalkonCanvas(
                viewModel = viewModel,
                fx = fx,
                fxTick = fxTick,
                hud = hud,
                modifier = Modifier.fillMaxSize(),
            )
            when (phase) {
                BalkonPhase.MENU -> ThemeCard(
                    theme = theme,
                    bestLevel = bestLevel,
                    onTheme = viewModel::setTheme,
                    onStart = startRun,
                    onExit = onExit,
                )
                BalkonPhase.PAUSED -> PauseCard(
                    onResume = viewModel::resume,
                    onRestart = restartRun,
                    onMenu = viewModel::toMenu,
                    onExit = onExit,
                )
                BalkonPhase.OVER -> OverCard(
                    hud = hud,
                    isRecord = hud.score > previousBest,
                    onRestart = restartRun,
                    onMenu = viewModel::toMenu,
                    onExit = onExit,
                )
                BalkonPhase.PLAYING -> Unit
            }
        }

        Controls(
            hud = hud,
            megaName = megaName,
            armed = megaArmed,
            enabled = phase == BalkonPhase.PLAYING,
            onMega = viewModel::toggleMega,
        )
    }
}

private fun themeNameRes(theme: BalkonTheme): Int = when (theme) {
    BalkonTheme.CEKIRDEK -> R.string.balkon_theme_cekirdek
    BalkonTheme.BALON -> R.string.balkon_theme_balon
    BalkonTheme.TUKURUK -> R.string.balkon_theme_tukuruk
}

private fun megaNameRes(theme: BalkonTheme): Int = when (theme) {
    BalkonTheme.CEKIRDEK -> R.string.balkon_mega_cekirdek
    BalkonTheme.BALON -> R.string.balkon_mega_balon
    BalkonTheme.TUKURUK -> R.string.balkon_mega_tukuruk
}

/** Süre çubuğu, isabet sayacı ve rüzgâr göstergesi. */
@Composable
private fun StatusRow(hud: BalkonHud, visible: Boolean) {
    val alpha = if (visible) 1f else 0f
    val timeDesc = stringResource(R.string.time_label) + " " + hud.seconds
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "${hud.seconds}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = (if (hud.seconds <= 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground).copy(alpha = alpha),
            modifier = Modifier
                .width(30.dp)
                .semantics { contentDescription = timeDesc },
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(hud.timeFraction)
                    .fillMaxHeight()
                    .background(
                        (if (hud.seconds <= 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary).copy(alpha = alpha),
                    ),
            )
        }
        Text(
            text = stringResource(R.string.balkon_hits_fmt, hud.hits, hud.required),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha),
        )
        WindIndicator(wind = hud.wind, alpha = alpha)
    }
}

/** Rüzgâr oku: yön ve şiddet (çentikler). */
@Composable
private fun WindIndicator(wind: Float, alpha: Float) {
    val label = stringResource(R.string.balkon_wind_label)
    val color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha)
    val strength = min(1f, abs(wind) / BalkonWorld.MAX_WIND)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f * alpha))
        Canvas(modifier = Modifier.size(width = 44.dp, height = 18.dp)) {
            val cy = size.height / 2f
            val len = size.width * (0.35f + 0.65f * strength)
            val x0 = if (wind >= 0f) 0f else size.width
            val x1 = if (wind >= 0f) len else size.width - len
            if (abs(wind) < 0.005f) {
                drawCircle(color, radius = 3f, center = Offset(size.width / 2f, cy))
                return@Canvas
            }
            drawLine(color, Offset(x0, cy), Offset(x1, cy), strokeWidth = 4f)
            val dir = if (wind >= 0f) 1f else -1f
            val head = Path().apply {
                moveTo(x1, cy)
                lineTo(x1 - dir * 8f, cy - 6f)
                lineTo(x1 - dir * 8f, cy + 6f)
                close()
            }
            drawPath(head, color)
        }
    }
}

@Composable
private fun Controls(hud: BalkonHud, megaName: String, armed: Boolean, enabled: Boolean, onMega: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PadButton(
            label = stringResource(R.string.balkon_mega_button_fmt, megaName, hud.charges),
            description = stringResource(R.string.balkon_mega_button_fmt, megaName, hud.charges),
            accent = armed && enabled,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            onAction = onMega,
        )
        Text(
            text = when {
                !enabled -> " "
                hud.stunned -> stringResource(R.string.balkon_stunned)
                armed -> stringResource(R.string.balkon_mega_armed)
                else -> stringResource(R.string.balkon_hint)
            },
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.weight(1.4f),
        )
    }
}

// ---------------------------------------------------------------------------
// Kartlar
// ---------------------------------------------------------------------------

@Composable
private fun ThemeCard(
    theme: BalkonTheme,
    bestLevel: Int,
    onTheme: (BalkonTheme) -> Unit,
    onStart: () -> Unit,
    onExit: () -> Unit,
) {
    OverlayCard {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.balkon_theme_label),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            BalkonTheme.entries.forEach { option ->
                ThemeOption(option = option, selected = option == theme) { onTheme(option) }
            }
            Text(
                text = stringResource(R.string.balkon_rules),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            if (bestLevel > 0) {
                Text(
                    text = stringResource(R.string.balkon_best_level_fmt, bestLevel),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(2.dp))
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.balkon_start))
            }
            TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.exit_to_hub))
            }
        }
    }
}

@Composable
private fun ThemeOption(option: BalkonTheme, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Canvas(modifier = Modifier.size(34.dp)) {
                val c = Offset(size.width / 2f, size.height / 2f)
                drawProjectile(option, c, size.width * 0.34f, mega = false)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(themeNameRes(option)),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.balkon_mega_desc_fmt, stringResource(megaNameRes(option))),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                )
            }
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
            Text(stringResource(R.string.balkon_to_menu))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

@Composable
private fun OverCard(hud: BalkonHud, isRecord: Boolean, onRestart: () -> Unit, onMenu: () -> Unit, onExit: () -> Unit) {
    OverlayCard {
        Text(
            text = stringResource(R.string.balkon_time_up),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = formatScore(hud.score),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.balkon_level_reached_fmt, hud.level),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        )
        if (isRecord) {
            Text(
                text = stringResource(R.string.new_record),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Spacer(Modifier.height(4.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.restart))
        }
        OutlinedButton(onClick = onMenu, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.balkon_to_menu))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

// ---------------------------------------------------------------------------
// Sahne
// ---------------------------------------------------------------------------

@Composable
private fun BalkonCanvas(
    viewModel: BalkonViewModel,
    fx: BalkonFx,
    fxTick: MutableLongState,
    hud: BalkonHud,
    modifier: Modifier = Modifier,
) {
    val frame by viewModel.frame.collectAsStateWithLifecycle()
    val desc = stringResource(R.string.balkon_board_desc, hud.level, hud.seconds)
    val textMeasurer = rememberTextMeasurer()
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .semantics { contentDescription = desc }
            .pointerInput(viewModel) {
                detectTapGestures(
                    onTap = { pos -> viewModel.throwAt(pos.x / size.width, pos.y / size.height) },
                    onLongPress = { pos -> viewModel.throwMegaAt(pos.x / size.width, pos.y / size.height) },
                )
            },
    ) {
        // Durum okumaları: kare ve efekt sayacı değişince yeniden çizilir.
        @Suppress("UNUSED_VARIABLE")
        val f = frame
        @Suppress("UNUSED_VARIABLE")
        val tick = fxTick.longValue
        val world = viewModel.world
        val theme = fx.theme
        val w = size.width
        val h = size.height
        val sx = fx.shakeX * w * 0.5f
        val sy = fx.shakeY * h * 0.5f
        drawScene(w, h)
        for (s in fx.splats) drawSplat(s, theme, w, h)
        val sorted = world.targets.sortedBy { it.y }
        for (t in sorted) drawTarget(t, w, h, sx, sy)
        for (s in world.shots) drawShotShadow(s, w, h)
        drawBalcony(world, theme, w, h, sx, sy)
        for (s in world.shots) drawShot(s, theme, w, h)
        for (p in fx.particles) {
            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            drawCircle(p.color.copy(alpha = alpha), radius = p.size * w, center = Offset(p.x * w + sx, p.y * h + sy))
        }
        for (t in fx.texts) drawFloatingText(t, textMeasurer, w, h)
    }
}

private fun DrawScope.drawScene(w: Float, h: Float) {
    // Bina duvarı ve balkon zemini (üst şerit).
    val wallH = BalkonWorld.MIN_DEPTH * h
    drawRect(Wall, topLeft = Offset(0f, 0f), size = Size(w, wallH))
    val brickH = wallH / 6f
    for (i in 0 until 6) {
        val y = i * brickH
        drawLine(WallDark, Offset(0f, y), Offset(w, y), strokeWidth = 2f)
        val shift = if (i % 2 == 0) 0f else w * 0.06f
        var x = shift
        while (x < w) {
            drawLine(WallDark, Offset(x, y), Offset(x, y + brickH), strokeWidth = 2f)
            x += w * 0.12f
        }
    }
    drawRect(BalconyFloor, topLeft = Offset(0f, wallH * 0.62f), size = Size(w, wallH * 0.38f))
    // Yakın kaldırım.
    val nearTop = wallH
    val roadTop = 0.40f * h
    val roadBottom = 0.74f * h
    drawRect(PavementA, topLeft = Offset(0f, nearTop), size = Size(w, roadTop - nearTop))
    drawPaving(nearTop, roadTop, w)
    drawRect(Curb, topLeft = Offset(0f, roadTop - 4f), size = Size(w, 4f))
    // Yol.
    drawRect(Asphalt, topLeft = Offset(0f, roadTop), size = Size(w, roadBottom - roadTop))
    drawRect(AsphaltEdge, topLeft = Offset(0f, roadTop), size = Size(w, 3f))
    drawRect(AsphaltEdge, topLeft = Offset(0f, roadBottom - 3f), size = Size(w, 3f))
    val midY = (roadTop + roadBottom) / 2f
    var x = 0f
    val dash = w * 0.06f
    while (x < w) {
        drawLine(RoadLine, Offset(x, midY), Offset(x + dash, midY), strokeWidth = 3f)
        x += dash * 2f
    }
    // Uzak kaldırım.
    drawRect(Curb, topLeft = Offset(0f, roadBottom), size = Size(w, 4f))
    drawRect(PavementB, topLeft = Offset(0f, roadBottom), size = Size(w, h - roadBottom))
    drawPaving(roadBottom, h, w)
    // Süsler: çöp kutusu, ağaçlar.
    drawRoundRect(BinColor, topLeft = Offset(w * 0.90f, nearTop + 6f), size = Size(w * 0.05f, w * 0.05f), cornerRadius = CornerRadius(4f, 4f))
    for (tx in listOf(0.10f, 0.50f, 0.88f)) {
        val c = Offset(tx * w, 0.95f * h)
        drawCircle(TreeColor, radius = w * 0.05f, center = c)
        drawCircle(TreeLight, radius = w * 0.03f, center = Offset(c.x - w * 0.012f, c.y - w * 0.012f))
    }
}

private fun DrawScope.drawPaving(top: Float, bottom: Float, w: Float) {
    val step = w * 0.1f
    var x = 0f
    while (x < w) {
        drawLine(PavementB.copy(alpha = 0.6f), Offset(x, top), Offset(x, bottom), strokeWidth = 1.5f)
        x += step
    }
}

/** Balkon korkuluğu, oyuncu (yukarıdan: saç, eller) ve rüzgâr bayrağı. */
private fun DrawScope.drawBalcony(world: BalkonWorld, theme: BalkonTheme, w: Float, h: Float, sx: Float, sy: Float) {
    val railY = BalkonWorld.MIN_DEPTH * h * 0.72f
    drawLine(Rail, Offset(0f, railY), Offset(w, railY), strokeWidth = 5f)
    var x = w * 0.02f
    while (x < w) {
        drawLine(Rail, Offset(x, railY), Offset(x, railY + h * 0.05f), strokeWidth = 3f)
        x += w * 0.06f
    }
    // Bayrak: rüzgârın yönü ve şiddeti.
    val poleX = w * 0.06f
    drawLine(Rail, Offset(poleX, railY - h * 0.075f), Offset(poleX, railY), strokeWidth = 3f)
    val wind = world.wind
    val len = w * (0.03f + 0.55f * abs(wind))
    val dir = if (wind >= 0f) 1f else -1f
    val flag = Path().apply {
        moveTo(poleX, railY - h * 0.075f)
        lineTo(poleX + dir * len, railY - h * 0.06f)
        lineTo(poleX, railY - h * 0.045f)
        close()
    }
    drawPath(flag, FlagColor)
    // Oyuncu: kafa üstten, iki el korkulukta; atarken öne eğilir.
    val ax = world.avatarX * w + sx
    val lean = if (world.throwAge < 0.25f) h * 0.012f else 0f
    val headY = railY - h * 0.025f + lean + sy
    val r = w * 0.045f
    drawCircle(Skin, radius = r * 0.8f, center = Offset(ax - r * 1.5f, railY + h * 0.004f))
    drawCircle(Skin, radius = r * 0.8f, center = Offset(ax + r * 1.5f, railY + h * 0.004f))
    drawCircle(Shirt, radius = r * 1.5f, center = Offset(ax, headY - r * 0.9f))
    drawCircle(Hair, radius = r, center = Offset(ax, headY))
    drawCircle(Hair.copy(alpha = 0.6f), radius = r * 0.55f, center = Offset(ax - r * 0.2f, headY - r * 0.25f))
    if (world.throwAge < 0.2f) {
        drawProjectile(theme, Offset(ax, headY + r * 1.1f), r * 0.35f, mega = false)
    }
    if (world.stun > 0f) {
        drawCircle(BalkonFx.DANGER.copy(alpha = 0.35f), radius = r * 2.2f, center = Offset(ax, headY))
    }
}

private fun DrawScope.drawTarget(t: Target, w: Float, h: Float, sx: Float, sy: Float) {
    val r = t.kind.radius * w * 1.3f
    var cx = t.x * w + sx
    var cy = t.y * h + sy
    var scale = 1f
    var alpha = 1f
    if (t.hit) {
        val k = (t.hitAge / BalkonWorld.HIT_ANIM).coerceIn(0f, 1f)
        when (t.kind) {
            TargetKind.PIGEON -> {
                cy -= k * h * 0.12f
                scale = 1f + k * 0.8f
                alpha = 1f - k
            }
            TargetKind.CAT -> {
                scale = 1f + 0.35f * sin(k * PI.toFloat())
                cx += (if (t.dir > 0) -1f else 1f) * k * w * 0.05f
                alpha = 1f - k * 0.6f
            }
            TargetKind.BALL -> {
                scale = 1f - 0.3f * sin(k * PI.toFloat())
                alpha = 1f - k * 0.5f
            }
            else -> {
                cx += sin(k * 18f) * w * 0.008f
                alpha = 1f - k * 0.5f
            }
        }
    }
    val c = Offset(cx, cy)
    val flip = t.dir < 0
    drawCircle(ShadowColor, radius = r * 0.9f * scale, center = Offset(cx + r * 0.15f, cy + r * 0.25f))
    when (t.kind) {
        TargetKind.PIGEON -> {
            val flap = 0.6f + 0.4f * abs(sin(t.age * 14f))
            drawOval(PigeonBody.copy(alpha = alpha), topLeft = Offset(cx - r * scale, cy - r * 0.6f * scale), size = Size(2f * r * scale, 1.2f * r * scale))
            drawOval(PigeonBody.copy(alpha = alpha * 0.85f), topLeft = Offset(cx - r * 1.3f * scale, cy - r * 0.35f * scale * flap), size = Size(2.6f * r * scale, 0.7f * r * scale * flap))
            drawCircle(PigeonHead.copy(alpha = alpha), radius = r * 0.35f * scale, center = Offset(cx + (if (flip) -1f else 1f) * r * 0.9f * scale, cy))
        }
        TargetKind.CAT -> {
            val body = 1.15f * r * scale
            drawOval(CatBody.copy(alpha = alpha), topLeft = Offset(cx - body, cy - body * 0.5f), size = Size(2f * body, body))
            val hx = cx + (if (flip) -1f else 1f) * body * 0.95f
            drawCircle(CatBody.copy(alpha = alpha), radius = body * 0.42f, center = Offset(hx, cy))
            drawCircle(CatDark.copy(alpha = alpha), radius = body * 0.12f, center = Offset(hx - body * 0.2f, cy - body * 0.35f))
            drawCircle(CatDark.copy(alpha = alpha), radius = body * 0.12f, center = Offset(hx + body * 0.2f, cy - body * 0.35f))
            val tailX = cx - (if (flip) -1f else 1f) * body * 0.95f
            val tail = Path().apply {
                moveTo(tailX, cy)
                quadraticBezierTo(tailX - (if (flip) -1f else 1f) * body * 0.5f, cy - body * 0.5f + body * 0.3f * sin(t.age * 5f), tailX - (if (flip) -1f else 1f) * body * 0.9f, cy - body * 0.2f)
            }
            drawPath(tail, CatDark.copy(alpha = alpha), style = Stroke(width = body * 0.18f))
        }
        TargetKind.BALL -> {
            val rr = r * scale
            drawCircle(BallA.copy(alpha = alpha), radius = rr, center = c)
            val a = t.x * 40f
            for (i in 0 until 3) {
                val ang = a + i * 2.094f
                drawCircle(BallB.copy(alpha = alpha), radius = rr * 0.28f, center = Offset(cx + cos(ang) * rr * 0.55f, cy + sin(ang) * rr * 0.55f))
            }
        }
        TargetKind.BIKE -> {
            val len = r * 1.6f
            drawLine(BikeFrame.copy(alpha = alpha), Offset(cx - len, cy), Offset(cx + len, cy), strokeWidth = r * 0.35f)
            drawOval(Wheel.copy(alpha = alpha), topLeft = Offset(cx - len - r * 0.35f, cy - r * 0.22f), size = Size(r * 0.7f, r * 0.44f))
            drawOval(Wheel.copy(alpha = alpha), topLeft = Offset(cx + len - r * 0.35f, cy - r * 0.22f), size = Size(r * 0.7f, r * 0.44f))
            drawCircle(Helmet.copy(alpha = alpha), radius = r * 0.5f, center = c)
        }
        TargetKind.SCOOTER -> {
            val len = r * 1.7f
            drawRoundRect(ScooterBody.copy(alpha = alpha), topLeft = Offset(cx - len, cy - r * 0.45f), size = Size(2f * len, r * 0.9f), cornerRadius = CornerRadius(r * 0.4f, r * 0.4f))
            val boxX = if (flip) cx + len * 0.35f else cx - len * 0.95f
            drawRect(CourierBox.copy(alpha = alpha), topLeft = Offset(boxX, cy - r * 0.6f), size = Size(len * 0.6f, r * 1.2f))
            drawCircle(Helmet.copy(alpha = alpha), radius = r * 0.5f, center = Offset(cx + (if (flip) -1f else 1f) * len * 0.2f, cy))
        }
        TargetKind.CAR -> {
            val len = r * 1.05f
            val color = CarColors[t.id % CarColors.size]
            drawRoundRect(color.copy(alpha = alpha), topLeft = Offset(cx - len, cy - r * 0.55f), size = Size(2f * len, r * 1.1f), cornerRadius = CornerRadius(r * 0.3f, r * 0.3f))
            drawRoundRect(CarWindow.copy(alpha = alpha), topLeft = Offset(cx - len * 0.55f, cy - r * 0.42f), size = Size(len * 0.35f, r * 0.84f), cornerRadius = CornerRadius(r * 0.1f, r * 0.1f))
            drawRoundRect(CarWindow.copy(alpha = alpha), topLeft = Offset(cx + len * 0.25f, cy - r * 0.42f), size = Size(len * 0.3f, r * 0.84f), cornerRadius = CornerRadius(r * 0.1f, r * 0.1f))
            val frontX = if (flip) cx - len else cx + len - r * 0.12f
            drawRect(BallB.copy(alpha = alpha), topLeft = Offset(frontX, cy - r * 0.45f), size = Size(r * 0.12f, r * 0.2f))
            drawRect(BallB.copy(alpha = alpha), topLeft = Offset(frontX, cy + r * 0.25f), size = Size(r * 0.12f, r * 0.2f))
        }
        TargetKind.SIMIT -> {
            drawPerson(c, r, Shirt, alpha)
            val trayR = r * 1.25f
            drawCircle(Tray.copy(alpha = alpha), radius = trayR, center = Offset(cx, cy - r * 0.2f))
            for (i in 0 until 5) {
                val ang = i * 1.2566f + t.age
                val p = Offset(cx + cos(ang) * trayR * 0.55f, cy - r * 0.2f + sin(ang) * trayR * 0.55f)
                drawCircle(Simit.copy(alpha = alpha), radius = trayR * 0.28f, center = p, style = Stroke(width = trayR * 0.14f))
            }
        }
        TargetKind.JANITOR -> {
            drawPerson(c, r, JanitorVest, alpha)
            val bx = cx + (if (flip) -1f else 1f) * r * 0.9f
            drawLine(Broom.copy(alpha = alpha), Offset(bx, cy - r * 1.4f), Offset(bx, cy + r * 1.2f), strokeWidth = r * 0.18f)
            drawRect(Seed.copy(alpha = alpha), topLeft = Offset(bx - r * 0.35f, cy + r * 1.1f), size = Size(r * 0.7f, r * 0.45f))
        }
        TargetKind.NEIGHBOR -> {
            drawPerson(c, r, Scarf, alpha)
            for (i in 0 until 4) {
                val ang = i * 1.57f + 0.5f
                drawCircle(ScarfDot.copy(alpha = alpha), radius = r * 0.12f, center = Offset(cx + cos(ang) * r * 0.5f, cy + sin(ang) * r * 0.5f))
            }
            drawRoundRect(Bag.copy(alpha = alpha), topLeft = Offset(cx + (if (flip) -1.9f else 1.1f) * r, cy - r * 0.2f), size = Size(r * 0.8f, r * 0.9f), cornerRadius = CornerRadius(r * 0.15f, r * 0.15f))
        }
    }
}

/** Yukarıdan insan: omuzlar (giysi rengi) ve baş. */
private fun DrawScope.drawPerson(c: Offset, r: Float, shirt: Color, alpha: Float) {
    drawOval(shirt.copy(alpha = alpha), topLeft = Offset(c.x - r * 1.1f, c.y - r * 0.55f), size = Size(2.2f * r, 1.1f * r))
    drawCircle(Skin.copy(alpha = alpha), radius = r * 0.62f, center = c)
    drawCircle(Hair.copy(alpha = alpha), radius = r * 0.5f, center = Offset(c.x - r * 0.05f, c.y - r * 0.1f))
}

private fun DrawScope.drawShotShadow(s: Shot, w: Float, h: Float) {
    val base = (if (s.mega) BalkonWorld.MEGA_RADIUS else BalkonWorld.RADIUS) * w
    val rr = base * (0.5f + 0.5f * s.t)
    val alpha = 0.12f + 0.35f * s.t
    drawOval(Color.Black.copy(alpha = alpha), topLeft = Offset(s.groundX * w - rr, s.groundY * h - rr * 0.55f), size = Size(2f * rr, rr * 1.1f))
}

private fun DrawScope.drawShot(s: Shot, theme: BalkonTheme, w: Float, h: Float) {
    val arc = sin(s.t * PI.toFloat()) * h * 0.05f
    val scale = 1.5f - 0.9f * s.t
    val base = (if (s.mega) BalkonWorld.MEGA_RADIUS * 0.5f else BalkonWorld.RADIUS) * w
    drawProjectile(theme, Offset(s.groundX * w, s.groundY * h - arc), base * scale, s.mega)
}

/** Temanın mermisi ([mega] büyük sürümü): çekirdek/avuç, balon/kova, tükürük/balgam. */
private fun DrawScope.drawProjectile(theme: BalkonTheme, c: Offset, r: Float, mega: Boolean) {
    when (theme) {
        BalkonTheme.CEKIRDEK -> if (mega) {
            for (i in 0 until 7) {
                val ang = i * 0.9f
                val p = Offset(c.x + cos(ang) * r * 0.7f, c.y + sin(ang) * r * 0.7f)
                drawSeed(p, r * 0.55f, ang)
            }
        } else {
            drawSeed(c, r, 0.6f)
        }
        BalkonTheme.BALON -> if (mega) {
            val path = Path().apply {
                moveTo(c.x - r, c.y - r * 0.8f)
                lineTo(c.x + r, c.y - r * 0.8f)
                lineTo(c.x + r * 0.75f, c.y + r)
                lineTo(c.x - r * 0.75f, c.y + r)
                close()
            }
            drawPath(path, Bucket)
            drawPath(path, BucketDark, style = Stroke(width = r * 0.12f))
            drawOval(Balloon, topLeft = Offset(c.x - r * 0.85f, c.y - r * 1.05f), size = Size(r * 1.7f, r * 0.5f))
        } else {
            drawCircle(Balloon, radius = r, center = c)
            drawCircle(BalloonLight, radius = r * 0.3f, center = Offset(c.x - r * 0.35f, c.y - r * 0.35f))
        }
        BalkonTheme.TUKURUK -> {
            val body = if (mega) Phlegm else Slime
            drawCircle(body, radius = r, center = c)
            drawCircle(body, radius = r * 0.45f, center = Offset(c.x + r * 0.8f, c.y + r * 0.3f))
            drawCircle(SlimeDark.copy(alpha = 0.5f), radius = r * 0.35f, center = Offset(c.x - r * 0.25f, c.y + r * 0.2f))
            if (mega) drawCircle(SlimeDark, radius = r * 0.25f, center = Offset(c.x + r * 0.3f, c.y - r * 0.4f))
        }
    }
}

private fun DrawScope.drawSeed(c: Offset, r: Float, angle: Float) {
    val dx = cos(angle) * r
    val dy = sin(angle) * r
    val path = Path().apply {
        moveTo(c.x - dx, c.y - dy)
        quadraticBezierTo(c.x - dy * 0.6f, c.y + dx * 0.6f, c.x + dx, c.y + dy)
        quadraticBezierTo(c.x + dy * 0.6f, c.y - dx * 0.6f, c.x - dx, c.y - dy)
        close()
    }
    drawPath(path, Seed)
    drawPath(path, SeedDark, style = Stroke(width = maxOf(1f, r * 0.12f)))
}

/** Yerde kalan iz: kabuk yığını, su birikintisi ya da yeşil leke; zamanla solar. */
private fun DrawScope.drawSplat(s: BalkonSplat, theme: BalkonTheme, w: Float, h: Float) {
    val alpha = (s.life / s.maxLife).coerceIn(0f, 1f) * 0.9f
    val base = (if (s.mega) BalkonWorld.MEGA_RADIUS else BalkonWorld.RADIUS) * w
    val c = Offset(s.x * w, s.y * h)
    when (theme) {
        BalkonTheme.CEKIRDEK -> {
            val n = if (s.mega) 14 else 5
            for (i in 0 until n) {
                val ang = (s.seed + i * 97) % 360 / 57.3f
                val d = ((s.seed shr (i % 8)) and 15) / 15f * base
                val p = Offset(c.x + cos(ang) * d, c.y + sin(ang) * d * 0.6f)
                drawSeedFaded(p, base * 0.18f, ang, alpha)
            }
        }
        BalkonTheme.BALON -> {
            drawOval(Puddle.copy(alpha = Puddle.alpha * alpha), topLeft = Offset(c.x - base * 1.3f, c.y - base * 0.7f), size = Size(base * 2.6f, base * 1.4f))
            drawOval(BalloonLight.copy(alpha = 0.35f * alpha), topLeft = Offset(c.x - base * 0.6f, c.y - base * 0.35f), size = Size(base * 1.2f, base * 0.5f))
        }
        BalkonTheme.TUKURUK -> {
            val color = (if (s.mega) Phlegm else Slime).copy(alpha = alpha)
            drawOval(color, topLeft = Offset(c.x - base * 1.1f, c.y - base * 0.65f), size = Size(base * 2.2f, base * 1.3f))
            for (i in 0 until 4) {
                val ang = (s.seed + i * 131) % 360 / 57.3f
                drawCircle(color, radius = base * 0.25f, center = Offset(c.x + cos(ang) * base * 1.3f, c.y + sin(ang) * base * 0.8f))
            }
        }
    }
}

private fun DrawScope.drawSeedFaded(c: Offset, r: Float, angle: Float, alpha: Float) {
    val dx = cos(angle) * r
    val dy = sin(angle) * r
    val path = Path().apply {
        moveTo(c.x - dx, c.y - dy)
        quadraticBezierTo(c.x - dy * 0.6f, c.y + dx * 0.6f, c.x + dx, c.y + dy)
        quadraticBezierTo(c.x + dy * 0.6f, c.y - dx * 0.6f, c.x - dx, c.y - dy)
        close()
    }
    drawPath(path, Seed.copy(alpha = alpha))
}

private fun DrawScope.drawFloatingText(t: BalkonText, measurer: TextMeasurer, w: Float, h: Float) {
    val alpha = (t.life / t.maxLife).coerceIn(0f, 1f)
    drawCenteredText(measurer, t.text, t.x * w, t.y * h, if (t.big) 22.sp else 13.sp, t.color.copy(alpha = alpha))
}

private fun DrawScope.drawCenteredText(measurer: TextMeasurer, text: String, cx: Float, cy: Float, fontSize: TextUnit, color: Color) {
    val layout = measurer.measure(text = text, style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Bold))
    val x = (cx - layout.size.width / 2f).coerceIn(0f, maxOf(0f, size.width - layout.size.width))
    drawText(textLayoutResult = layout, color = Color.Black.copy(alpha = color.alpha * 0.6f), topLeft = Offset(x + 1.5f, cy - layout.size.height / 2f + 1.5f))
    drawText(textLayoutResult = layout, color = color, topLeft = Offset(x, cy - layout.size.height / 2f))
}
