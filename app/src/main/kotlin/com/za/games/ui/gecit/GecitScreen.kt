package com.za.games.ui.gecit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.za.games.gecit.DeathCause
import com.za.games.gecit.GecitGen
import com.za.games.gecit.GecitHud
import com.za.games.gecit.GecitStatus
import com.za.games.gecit.GecitWorld
import com.za.games.gecit.Lane
import com.za.games.gecit.LaneKind
import com.za.games.gecit.Mover
import com.za.games.gecit.Move
import com.za.games.gecit.RailPhase
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
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

private val GrassA = Color(0xFF3F6212)
private val GrassB = Color(0xFF4D7C0F)
private val Road = Color(0xFF334155)
private val RoadLine = Color(0xFF64748B)
private val RailBed = Color(0xFF44403C)
private val Tie = Color(0xFF292524)
private val RailLine = Color(0xFFA8A29E)
private val River = Color(0xFF1D4ED8)
private val Wave = Color(0xFF3B82F6)
private val LogColor = Color(0xFF92400E)
private val LogEnd = Color(0xFF78350F)
private val Tree = Color(0xFF166534)
private val Trunk = Color(0xFF713F12)
private val Frog = Color(0xFFA3E635)
private val FrogDark = Color(0xFF365314)
private val TrainColor = Color(0xFF7F1D1D)
private val TrainWindow = Color(0xFFFDE68A)
private val EagleColor = Color(0xFF1C1917)
private val Light = Color(0xFFEF4444)
private val Glass = Color(0xFF0F172A)
private val Headlight = Color(0xFFFEF3C7)
private val CarColors = listOf(
    Color(0xFFF87171), Color(0xFFFBBF24), Color(0xFF60A5FA),
    Color(0xFFF472B6), Color(0xFFA78BFA), Color(0xFFE2E8F0),
)
private val ShadowColor = Color(0x46000000)
private val GemColor = GecitFx.GEM
private val RiverEdge = Color(0xFF1E40AF)
private val Flower = Color(0xFFFDE68A)
private val Pebble = Color(0xFF65A30D)

@Composable
fun GecitScreen(
    highScore: Long,
    onScore: (Long) -> Unit,
    onExit: () -> Unit,
    viewModel: GecitViewModel = viewModel(),
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
    val fx = remember { GecitFx() }
    val fxTick = remember { mutableLongStateOf(0L) }

    val latestScore by rememberUpdatedState(hud.score)
    val latestOnScore by rememberUpdatedState(onScore)
    DisposableEffect(Unit) {
        onDispose { latestOnScore(latestScore) }
    }
    LaunchedEffect(phase) {
        if (phase == GecitPhase.OVER) latestOnScore(hud.score)
    }
    LaunchedEffect(runId) { fx.reset(viewModel.world.camera) }
    LaunchedEffect(Unit) { viewModel.refreshDaily() }
    LifecycleResumeEffect(Unit) {
        onPauseOrDispose { viewModel.pause() }
    }
    BackHandler {
        if (phase == GecitPhase.PLAYING) viewModel.pause() else onExit()
    }

    // Kare döngüsü: oyun koşarken ve bitiş efektleri sönene dek.
    LaunchedEffect(phase, runId) {
        if (phase != GecitPhase.PLAYING && phase != GecitPhase.OVER) return@LaunchedEffect
        var last = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = now - last
                    val world = viewModel.world
                    for (event in viewModel.advance(dt)) {
                        fx.onEvent(event, world, sound, haptics, resources)
                    }
                    fx.update(dt / 1_000_000_000f, world.camera)
                    fxTick.longValue += 1
                }
                last = now
            }
            if (phase == GecitPhase.OVER && !fx.isBusy) break
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
    val attemptsLeft = GecitViewModel.DAILY_ATTEMPTS - (daily?.attempts ?: 0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        GameTopBar(title = stringResource(R.string.game_gecit), onExit = onExit) {
            if (phase == GecitPhase.PLAYING || phase == GecitPhase.PAUSED) {
                TextButton(onClick = viewModel::togglePause) {
                    Text(
                        text = stringResource(
                            if (phase == GecitPhase.PAUSED) R.string.resume else R.string.pause,
                        ),
                    )
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
            if (mode == GecitMode.DAILY) {
                ScoreCard(
                    label = stringResource(R.string.gecit_attempt),
                    value = stringResource(
                        R.string.gecit_attempt_fmt,
                        daily?.attempts ?: 0,
                        GecitViewModel.DAILY_ATTEMPTS,
                    ),
                    modifier = Modifier.weight(1f),
                )
            } else {
                ScoreCard(
                    label = stringResource(R.string.time_label),
                    value = formatTime(hud.seconds),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        EagleBar(
            fraction = hud.idleFraction,
            gems = hud.gems,
            visible = phase == GecitPhase.PLAYING || phase == GecitPhase.PAUSED,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            GecitCanvas(
                viewModel = viewModel,
                fx = fx,
                fxTick = fxTick,
                hud = hud,
                onMove = viewModel::move,
                modifier = Modifier.fillMaxSize(),
            )
            when (phase) {
                GecitPhase.MENU -> StartCard(
                    mode = mode,
                    daily = daily,
                    leftHanded = leftHanded,
                    onHand = viewModel::setLeftHanded,
                    onMode = viewModel::setMode,
                    onStart = startRun,
                    onExit = onExit,
                )
                GecitPhase.PAUSED -> PauseCard(
                    daily = mode == GecitMode.DAILY,
                    attemptsLeft = attemptsLeft,
                    leftHanded = leftHanded,
                    onHand = viewModel::setLeftHanded,
                    onResume = viewModel::resume,
                    onRestart = restartRun,
                    onMenu = viewModel::toMenu,
                    onExit = onExit,
                )
                GecitPhase.OVER -> OverCard(
                    hud = hud,
                    daily = mode == GecitMode.DAILY,
                    attemptsLeft = attemptsLeft,
                    isRecord = hud.score > previousBest,
                    onRestart = restartRun,
                    onMenu = viewModel::toMenu,
                    onExit = onExit,
                )
                GecitPhase.PLAYING -> Unit
            }
        }

        Controls(leftHanded = leftHanded, onMove = viewModel::move)
    }
}

private fun formatTime(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

/** Kartal sayacı: ileri gitmeden geçen sürenin oranı; dolunca kartal iner. */
@Composable
private fun EagleBar(fraction: Float, gems: Int, visible: Boolean) {
    val alpha = if (visible) 1f else 0f
    val gemsDesc = stringResource(R.string.gecit_gems) + " " + gems
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.gecit_eagle),
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
                    .background(
                        if (fraction > 0.6f) {
                            MaterialTheme.colorScheme.error.copy(alpha = alpha)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                        },
                    ),
            )
        }
        Text(
            text = "◆ " + gems,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = GemColor,
            modifier = Modifier.semantics { contentDescription = gemsDesc },
        )
    }
}

/**
 * Kontrol satırı, iki başparmak: yön tuşları bir yanda, geri ve büyük ileri
 * tuşu öbür yanda; ileri tuşu seçilen başparmağın tarafındadır (ayar Kuyu ile
 * ortak). Tuşlar basılı tutulunca art arda zıplatır.
 */
@Composable
private fun Controls(leftHanded: Boolean, onMove: (Move) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(84.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (leftHanded) {
            HopButtons(forwardFirst = true, onMove = onMove)
            Spacer(Modifier.weight(0.2f))
            SideButtons(onMove)
        } else {
            SideButtons(onMove)
            Spacer(Modifier.weight(0.2f))
            HopButtons(forwardFirst = false, onMove = onMove)
        }
    }
}

@Composable
private fun RowScope.SideButtons(onMove: (Move) -> Unit) {
    PadButton(
        label = "◀",
        description = stringResource(R.string.gecit_ctrl_left),
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        repeatIntervalMs = 170L,
        fontSize = 26.sp,
    ) { onMove(Move.LEFT) }
    PadButton(
        label = "▶",
        description = stringResource(R.string.gecit_ctrl_right),
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        repeatIntervalMs = 170L,
        fontSize = 26.sp,
    ) { onMove(Move.RIGHT) }
}

@Composable
private fun RowScope.HopButtons(forwardFirst: Boolean, onMove: (Move) -> Unit) {
    if (forwardFirst) ForwardButton(onMove)
    PadButton(
        label = "▼",
        description = stringResource(R.string.gecit_ctrl_back),
        modifier = Modifier
            .weight(0.7f)
            .fillMaxHeight(),
        fontSize = 22.sp,
    ) { onMove(Move.BACK) }
    if (!forwardFirst) ForwardButton(onMove)
}

@Composable
private fun RowScope.ForwardButton(onMove: (Move) -> Unit) {
    PadButton(
        label = "▲",
        description = stringResource(R.string.gecit_ctrl_forward),
        modifier = Modifier
            .weight(1.5f)
            .fillMaxHeight(),
        repeatIntervalMs = 150L,
        accent = true,
        fontSize = 30.sp,
    ) { onMove(Move.FORWARD) }
}

@Composable
private fun HandChips(leftHanded: Boolean, onHand: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ModeChip(
            label = stringResource(R.string.kuyu_hand_right),
            selected = !leftHanded,
            modifier = Modifier.weight(1f),
        ) { onHand(false) }
        ModeChip(
            label = stringResource(R.string.kuyu_hand_left),
            selected = leftHanded,
            modifier = Modifier.weight(1f),
        ) { onHand(true) }
    }
}

@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = modifier.height(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun StartCard(
    mode: GecitMode,
    daily: GecitDaily?,
    leftHanded: Boolean,
    onHand: (Boolean) -> Unit,
    onMode: (GecitMode) -> Unit,
    onStart: () -> Unit,
    onExit: () -> Unit,
) {
    val exhausted = daily != null && daily.attempts >= GecitViewModel.DAILY_ATTEMPTS
    OverlayCard {
        Text(
            text = stringResource(R.string.game_gecit),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.gecit_intro),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ModeChip(
                label = stringResource(R.string.mode_daily),
                selected = mode == GecitMode.DAILY,
                modifier = Modifier.weight(1f),
            ) { onMode(GecitMode.DAILY) }
            ModeChip(
                label = stringResource(R.string.mode_free),
                selected = mode == GecitMode.FREE,
                modifier = Modifier.weight(1f),
            ) { onMode(GecitMode.FREE) }
        }
        if (mode == GecitMode.DAILY) {
            if (daily != null) {
                Text(
                    text = stringResource(
                        R.string.gecit_daily_status_fmt,
                        daily.attempts,
                        GecitViewModel.DAILY_ATTEMPTS,
                        daily.best,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = stringResource(
                    if (exhausted) R.string.gecit_daily_exhausted else R.string.gecit_daily_desc,
                ),
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
            text = stringResource(R.string.gecit_hand_hint),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        )
        Text(
            text = stringResource(R.string.gecit_hint),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        )
        Spacer(Modifier.height(4.dp))
        if (mode == GecitMode.DAILY && exhausted) {
            Button(onClick = { onMode(GecitMode.FREE) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.play_free))
            }
        } else {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.gecit_start))
            }
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

@Composable
private fun RetryLabel(daily: Boolean, attemptsLeft: Int) {
    Text(
        text = when {
            !daily -> stringResource(R.string.restart)
            attemptsLeft > 0 -> stringResource(R.string.gecit_retry_fmt, attemptsLeft)
            else -> stringResource(R.string.play_free)
        },
    )
}

@Composable
private fun PauseCard(
    daily: Boolean,
    attemptsLeft: Int,
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
            RetryLabel(daily = daily, attemptsLeft = attemptsLeft)
        }
        TextButton(onClick = onMenu, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.gecit_to_start))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

private fun DeathCause.messageRes(): Int = when (this) {
    DeathCause.CAR -> R.string.gecit_death_car
    DeathCause.TRAIN -> R.string.gecit_death_train
    DeathCause.WATER -> R.string.gecit_death_water
    DeathCause.CARRIED -> R.string.gecit_death_carried
    DeathCause.CAMERA -> R.string.gecit_death_camera
    DeathCause.EAGLE -> R.string.gecit_death_eagle
}

@Composable
private fun OverCard(
    hud: GecitHud,
    daily: Boolean,
    attemptsLeft: Int,
    isRecord: Boolean,
    onRestart: () -> Unit,
    onMenu: () -> Unit,
    onExit: () -> Unit,
) {
    OverlayCard {
        Text(
            text = stringResource(hud.cause?.messageRes() ?: R.string.game_over),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
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
            text = stringResource(R.string.gecit_result_fmt, hud.score - hud.gems, hud.gems, hud.seconds),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
        if (daily && attemptsLeft <= 0) {
            Text(
                text = stringResource(R.string.gecit_daily_exhausted),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        Spacer(Modifier.height(4.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            RetryLabel(daily = daily, attemptsLeft = attemptsLeft)
        }
        TextButton(onClick = onMenu, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.gecit_to_start))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

/**
 * Geçit tuvali: 9 sütun ekran genişliğine sığar, satırlar aşağıdan yukarıya
 * ilerler; alt kenar [GecitWorld.camera] satırıdır. Dokunuş ileri, kaydırma
 * yöne zıplatır.
 */
@Composable
private fun GecitCanvas(
    viewModel: GecitViewModel,
    fx: GecitFx,
    fxTick: MutableLongState,
    hud: GecitHud,
    onMove: (Move) -> Unit,
    modifier: Modifier = Modifier,
) {
    val frame by viewModel.frame.collectAsStateWithLifecycle()
    val currentMove by rememberUpdatedState(onMove)
    val desc = stringResource(R.string.gecit_board_desc, hud.row, hud.score)
    val textMeasurer = rememberTextMeasurer()
    val textCache = remember { HashMap<String, TextLayoutResult>() }
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .semantics { contentDescription = desc }
            .pointerInput(Unit) {
                val threshold = 18.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown()
                    var dx = 0f
                    var dy = 0f
                    var fired = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.changedToUpIgnoreConsumed()) {
                            if (!fired) currentMove(Move.FORWARD)
                            change.consume()
                            break
                        }
                        val amount = change.positionChange()
                        dx += amount.x
                        dy += amount.y
                        if (!fired) {
                            if (abs(dx) > abs(dy) && abs(dx) > threshold) {
                                currentMove(if (dx > 0) Move.RIGHT else Move.LEFT)
                                fired = true
                            } else if (abs(dy) > threshold) {
                                currentMove(if (dy > 0) Move.BACK else Move.FORWARD)
                                fired = true
                            }
                        }
                        change.consume()
                    }
                }
            },
    ) {
        if (frame < 0L || fxTick.longValue < 0L) return@Canvas
        val world = viewModel.world
        val cell = size.width / GecitWorld.WIDTH
        val camera = if (fx.renderCamera.isNaN()) world.camera else fx.renderCamera
        val rowsVisible = size.height / cell
        drawRect(GrassA)
        translate(fx.shakeX * cell, fx.shakeY * cell) {
            val first = floor(camera).toInt() - 1
            val last = floor(camera + rowsVisible).toInt() + 1
            for (r in first..last) {
                val top = size.height - (r + 1 - camera) * cell
                drawLane(world.lane(r), world.lane(r + 1), world.seed, r, top, cell, frame)
            }
            drawPlayer(world, fx, cell, camera)
            drawEagle(world, fx, cell, camera, frame)
            drawParticles(fx, cell, camera)
            drawTexts(fx, textMeasurer, textCache, cell, camera)
        }
    }
}

/** Yerdeki nesnelerin altına yumuşak gölge: derinlik hissi. */
private fun DrawScope.drawShadow(cx: Float, cy: Float, w: Float, h: Float) {
    drawOval(ShadowColor, Offset(cx - w / 2f, cy - h / 2f), Size(w, h))
}

/** Çim süsleri: satır tohumundan türeyen çiçek ve çakıllar (ağaçsız hücrelerde). */
private fun DrawScope.drawGrassDecor(lane: Lane, seed: Long, r: Int, top: Float, cell: Float) {
    val rng = Random(GecitGen.mix(seed, r, 77))
    val count = rng.nextInt(3)
    repeat(count) {
        val c = rng.nextInt(GecitWorld.WIDTH)
        if (lane.trees[c] || lane.gemCol == c) return@repeat
        val x = c * cell + cell * (0.2f + rng.nextFloat() * 0.6f)
        val y = top + cell * (0.2f + rng.nextFloat() * 0.6f)
        if (rng.nextBoolean()) {
            drawCircle(Flower, cell * 0.07f, Offset(x, y))
            drawCircle(Pebble, cell * 0.03f, Offset(x, y))
        } else {
            drawOval(Pebble, Offset(x - cell * 0.08f, y - cell * 0.05f), Size(cell * 0.16f, cell * 0.1f))
        }
    }
}

private fun DrawScope.drawGem(col: Int, top: Float, cell: Float, frame: Long) {
    val bob = sin(frame * 0.12f) * cell * 0.06f
    val cx = (col + 0.5f) * cell
    val cy = top + cell * 0.5f + bob
    drawShadow(cx, top + cell * 0.8f, cell * 0.3f, cell * 0.1f)
    val r = cell * 0.2f
    val path = Path().apply {
        moveTo(cx, cy - r)
        lineTo(cx + r * 0.7f, cy)
        lineTo(cx, cy + r)
        lineTo(cx - r * 0.7f, cy)
        close()
    }
    drawPath(path, GemColor)
    drawCircle(Color.White.copy(alpha = 0.8f), r * 0.18f, Offset(cx - r * 0.2f, cy - r * 0.35f))
}

private fun DrawScope.drawLane(lane: Lane?, above: Lane?, seed: Long, r: Int, top: Float, cell: Float, frame: Long) {
    val h = cell + 1f
    val w = size.width
    val grass = if (r % 2 == 0) GrassA else GrassB
    if (lane == null) {
        // Başlangıcın gerisi: düz çim.
        drawRect(grass, Offset(0f, top), Size(w, h))
        return
    }
    when (lane.kind) {
        LaneKind.GRASS -> {
            drawRect(grass, Offset(0f, top), Size(w, h))
            drawGrassDecor(lane, seed, r, top, cell)
            if (lane.gemCol >= 0) drawGem(lane.gemCol, top, cell, frame)
            for (c in 0 until GecitWorld.WIDTH) if (lane.trees[c]) drawTree(c * cell, top, cell)
        }
        LaneKind.ROAD -> {
            drawRect(Road, Offset(0f, top), Size(w, h))
            if (above?.kind == LaneKind.ROAD) {
                // Bitişik yol şeritleri arasında kesikli çizgi.
                var x = cell * 0.2f
                while (x < w) {
                    drawRect(RoadLine, Offset(x, top - cell * 0.02f), Size(cell * 0.4f, cell * 0.04f))
                    x += cell * 0.8f
                }
            } else {
                drawRect(RoadLine.copy(alpha = 0.6f), Offset(0f, top), Size(w, cell * 0.04f))
            }
            if (lane.gemCol >= 0) drawGem(lane.gemCol, top, cell, frame)
            for (m in lane.movers) drawCar(lane, m, top, cell)
        }
        LaneKind.RAIL -> {
            drawRect(RailBed, Offset(0f, top), Size(w, h))
            var x = 0f
            while (x < w) {
                drawRect(Tie, Offset(x + cell * 0.1f, top + cell * 0.15f), Size(cell * 0.22f, cell * 0.7f))
                x += cell * 0.5f
            }
            drawRect(RailLine, Offset(0f, top + cell * 0.3f), Size(w, cell * 0.06f))
            drawRect(RailLine, Offset(0f, top + cell * 0.66f), Size(w, cell * 0.06f))
            if (lane.railPhase == RailPhase.WARNING && (frame / 8) % 2 == 0L) {
                drawCircle(Light, cell * 0.16f, Offset(cell * 0.45f, top + cell * 0.5f))
                drawCircle(Light, cell * 0.16f, Offset(w - cell * 0.45f, top + cell * 0.5f))
            }
            lane.trainX()?.let { tx -> drawTrain(tx, lane.dir, top, cell) }
        }
        LaneKind.RIVER -> {
            drawRect(River, Offset(0f, top), Size(w, h))
            drawRect(RiverEdge, Offset(0f, top), Size(w, cell * 0.08f))
            drawRect(RiverEdge, Offset(0f, top + cell - cell * 0.08f), Size(w, cell * 0.08f))
            val drift = lane.phase * cell * 0.5f
            for (i in 0 until 8) {
                val x = (((i * cell * 1.3f + drift) % (w + cell)) + (w + cell)) % (w + cell) - cell * 0.5f
                drawRect(Wave, Offset(x, top + cell * 0.3f + (i % 2) * cell * 0.35f), Size(cell * 0.5f, cell * 0.05f))
            }
            for (m in lane.movers) drawLog(lane, m, top, cell)
        }
    }
}

private fun DrawScope.drawTree(x: Float, top: Float, cell: Float) {
    drawShadow(x + cell * 0.55f, top + cell * 0.88f, cell * 0.6f, cell * 0.2f)
    drawRect(Trunk, Offset(x + cell * 0.42f, top + cell * 0.5f), Size(cell * 0.16f, cell * 0.4f))
    drawCircle(Tree, cell * 0.36f, Offset(x + cell * 0.5f, top + cell * 0.4f))
    drawCircle(Pebble.copy(alpha = 0.55f), cell * 0.14f, Offset(x + cell * 0.4f, top + cell * 0.3f))
}

private fun DrawScope.drawCar(lane: Lane, m: Mover, top: Float, cell: Float) {
    val x = lane.moverX(m)
    if (x + m.len < -0.5f || x > GecitWorld.WIDTH + 0.5f) return
    val px = x * cell
    val len = m.len * cell
    val color = CarColors[m.style % CarColors.size]
    drawShadow(px + len / 2f + cell * 0.04f, top + cell * 0.55f, len, cell * 0.72f)
    drawRoundRect(
        color = color,
        topLeft = Offset(px + cell * 0.05f, top + cell * 0.15f),
        size = Size(len - cell * 0.1f, cell * 0.7f),
        cornerRadius = CornerRadius(cell * 0.2f, cell * 0.2f),
    )
    if (m.len > 1) {
        // Kamyon: arka kasa daha koyu, kabin önde.
        val boxX = if (lane.dir > 0) px + cell * 0.05f else px + cell * 0.75f
        drawRoundRect(
            color = Glass.copy(alpha = 0.35f),
            topLeft = Offset(boxX, top + cell * 0.12f),
            size = Size(len - cell * 0.8f, cell * 0.76f),
            cornerRadius = CornerRadius(cell * 0.12f, cell * 0.12f),
        )
    }
    val frontX = if (lane.dir > 0) px + len - cell * 0.6f else px + cell * 0.25f
    drawRect(Glass.copy(alpha = 0.6f), Offset(frontX, top + cell * 0.24f), Size(cell * 0.35f, cell * 0.52f))
    val lightX = if (lane.dir > 0) px + len - cell * 0.13f else px + cell * 0.06f
    drawRect(Headlight, Offset(lightX, top + cell * 0.2f), Size(cell * 0.07f, cell * 0.14f))
    drawRect(Headlight, Offset(lightX, top + cell * 0.66f), Size(cell * 0.07f, cell * 0.14f))
    val tailX = if (lane.dir > 0) px + cell * 0.06f else px + len - cell * 0.13f
    drawRect(Light, Offset(tailX, top + cell * 0.2f), Size(cell * 0.07f, cell * 0.14f))
    drawRect(Light, Offset(tailX, top + cell * 0.66f), Size(cell * 0.07f, cell * 0.14f))
}

private fun DrawScope.drawTrain(tx: Float, dir: Int, top: Float, cell: Float) {
    val px = tx * cell
    val len = Lane.TRAIN_LEN * cell
    drawShadow(px + len / 2f, top + cell * 0.6f, len, cell * 0.9f)
    drawRect(TrainColor, Offset(px, top + cell * 0.08f), Size(len, cell * 0.84f))
    drawRect(Light.copy(alpha = 0.5f), Offset(px, top + cell * 0.08f), Size(len, cell * 0.1f))
    var x = px + cell * 0.3f
    while (x < px + len - cell * 0.4f) {
        drawRect(TrainWindow, Offset(x, top + cell * 0.28f), Size(cell * 0.32f, cell * 0.3f))
        x += cell * 0.7f
    }
    val lightX = if (dir > 0) px + len - cell * 0.12f else px + cell * 0.04f
    drawRect(Headlight, Offset(lightX, top + cell * 0.4f), Size(cell * 0.08f, cell * 0.2f))
}

private fun DrawScope.drawLog(lane: Lane, m: Mover, top: Float, cell: Float) {
    val x = lane.moverX(m)
    if (x + m.len < -0.5f || x > GecitWorld.WIDTH + 0.5f) return
    val px = x * cell
    val len = m.len * cell
    drawShadow(px + len / 2f, top + cell * 0.58f, len, cell * 0.62f)
    drawRoundRect(
        color = LogColor,
        topLeft = Offset(px + cell * 0.03f, top + cell * 0.2f),
        size = Size(len - cell * 0.06f, cell * 0.6f),
        cornerRadius = CornerRadius(cell * 0.3f, cell * 0.3f),
    )
    drawCircle(LogEnd, cell * 0.18f, Offset(px + cell * 0.3f, top + cell * 0.5f))
    drawCircle(LogEnd, cell * 0.18f, Offset(px + len - cell * 0.3f, top + cell * 0.5f))
}

private fun DrawScope.drawPlayer(world: GecitWorld, fx: GecitFx, cell: Float, camera: Float) {
    val p = world.player
    val t = p.hopT
    val ease = 1f - (1f - t) * (1f - t)
    val arc = sin(PI.toFloat() * t)
    val x = p.fromX + (p.x - p.fromX) * ease
    val row = p.fromRow + (p.row - p.fromRow) * ease
    val cx = (x + 0.5f) * cell
    val cause = if (world.status == GecitStatus.OVER) world.cause else null
    val lift = if (cause == DeathCause.EAGLE) fx.eagleLift else 0f
    val cy = size.height - (row + 0.5f - camera) * cell - arc * cell * 0.35f - lift * cell
    if (cause != DeathCause.WATER && cause != DeathCause.CARRIED) {
        drawShadow(cx, size.height - (row + 0.15f - camera) * cell, cell * 0.6f * (1f - arc * 0.3f), cell * 0.2f)
    }
    when (cause) {
        DeathCause.CAR, DeathCause.TRAIN -> {
            drawRoundRect(
                color = Frog,
                topLeft = Offset(cx - cell * 0.42f, cy),
                size = Size(cell * 0.84f, cell * 0.22f),
                cornerRadius = CornerRadius(cell * 0.1f, cell * 0.1f),
            )
            return
        }
        DeathCause.WATER, DeathCause.CARRIED -> {
            drawCircle(Wave, cell * 0.25f, Offset(cx, cy), style = Stroke(cell * 0.05f))
            drawCircle(Wave, cell * 0.42f, Offset(cx, cy), style = Stroke(cell * 0.04f))
            return
        }
        else -> Unit
    }
    val scale = 1f + 0.2f * arc
    val bw = cell * 0.62f * scale
    val bh = cell * 0.56f * scale
    // Bacaklar.
    for (sx in listOf(-1f, 1f)) {
        for (sy in listOf(-1f, 1f)) {
            drawRoundRect(
                color = FrogDark,
                topLeft = Offset(cx + sx * bw * 0.42f - bw * 0.14f, cy + sy * bh * 0.38f - bh * 0.14f),
                size = Size(bw * 0.28f, bh * 0.28f),
                cornerRadius = CornerRadius(bw * 0.1f, bw * 0.1f),
            )
        }
    }
    drawRoundRect(
        color = Frog,
        topLeft = Offset(cx - bw / 2f, cy - bh / 2f),
        size = Size(bw, bh),
        cornerRadius = CornerRadius(bw * 0.35f, bw * 0.35f),
    )
    val (fx, fy) = when (p.facing) {
        Move.FORWARD -> 0f to -1f
        Move.BACK -> 0f to 1f
        Move.LEFT -> -1f to 0f
        Move.RIGHT -> 1f to 0f
    }
    val eyeR = bw * 0.13f
    for (side in listOf(-1f, 1f)) {
        val ex = cx + fx * bh * 0.28f + (-fy) * side * bw * 0.22f
        val ey = cy + fy * bh * 0.28f + fx * side * bw * 0.22f
        drawCircle(Color.White, eyeR, Offset(ex, ey))
        drawCircle(Glass, eyeR * 0.5f, Offset(ex + fx * eyeR * 0.3f, ey + fy * eyeR * 0.3f))
    }
}

private fun DrawScope.drawEagle(world: GecitWorld, fx: GecitFx, cell: Float, camera: Float, frame: Long) {
    val dead = world.status == GecitStatus.OVER && world.cause == DeathCause.EAGLE
    val fraction = world.idleFraction
    if (!dead && (!world.started || fraction < 0.55f)) return
    val p = world.player
    val cx = (p.x + 0.5f) * cell
    val cy = size.height - (p.row + 0.5f - camera) * cell - (if (dead) fx.eagleLift * cell else 0f)
    val a = if (dead) 1f else ((fraction - 0.55f) / 0.45f).coerceIn(0f, 1f)
    val rx = cell * (0.3f + 0.45f * a)
    drawOval(
        color = EagleColor.copy(alpha = 0.35f * a),
        topLeft = Offset(cx - rx, cy - rx * 0.4f),
        size = Size(rx * 2f, rx * 0.8f),
    )
    if (a < 0.6f) return
    // Kuş: yukarıdan iner, ölümde oyuncunun üstündedir.
    val descent = if (dead) 0f else (1f - (a - 0.6f) / 0.4f) * cell * 3f
    val by = cy - cell * 0.4f - descent
    val flap = sin(frame * 0.6f) * cell * 0.15f
    val wings = Path().apply {
        moveTo(cx, by)
        lineTo(cx - cell * 0.9f, by - cell * 0.35f + flap)
        lineTo(cx - cell * 0.2f, by + cell * 0.1f)
        close()
        moveTo(cx, by)
        lineTo(cx + cell * 0.9f, by - cell * 0.35f + flap)
        lineTo(cx + cell * 0.2f, by + cell * 0.1f)
        close()
    }
    drawPath(wings, EagleColor)
    drawOval(EagleColor, Offset(cx - cell * 0.22f, by - cell * 0.12f), Size(cell * 0.44f, cell * 0.36f))
    drawCircle(Headlight, cell * 0.05f, Offset(cx + cell * 0.08f, by - cell * 0.02f))
}

private fun DrawScope.drawParticles(fx: GecitFx, cell: Float, camera: Float) {
    for (q in fx.particles) {
        val s = q.size * cell
        val alpha = (q.life / q.maxLife).coerceIn(0f, 1f)
        drawRect(
            color = q.color.copy(alpha = alpha),
            topLeft = Offset(q.x * cell - s / 2f, size.height - (q.row - camera) * cell - s / 2f),
            size = Size(s, s),
        )
    }
}

private fun DrawScope.drawTexts(
    fx: GecitFx,
    measurer: TextMeasurer,
    cache: HashMap<String, TextLayoutResult>,
    cell: Float,
    camera: Float,
) {
    for (t in fx.texts) {
        val key = t.text + "|" + cell.toInt()
        val layout = cache.getOrPut(key) {
            measurer.measure(
                text = t.text,
                style = TextStyle(fontSize = (cell * 0.5f).toSp(), fontWeight = FontWeight.Bold),
            )
        }
        val alpha = (t.life / t.maxLife).coerceIn(0f, 1f)
        val maxX = (size.width - layout.size.width).coerceAtLeast(0f)
        val x = (t.x * cell - layout.size.width / 2f).coerceIn(0f, maxX)
        drawText(
            textLayoutResult = layout,
            color = Color.White,
            topLeft = Offset(x, size.height - (t.row - camera) * cell - layout.size.height),
            alpha = alpha,
        )
    }
}
