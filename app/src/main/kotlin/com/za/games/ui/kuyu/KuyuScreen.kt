package com.za.games.ui.kuyu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.drawscope.translate
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
import com.za.games.kuyu.EnemyKind
import com.za.games.kuyu.KuyuHud
import com.za.games.kuyu.KuyuWorld
import com.za.games.kuyu.Player
import com.za.games.kuyu.Tile
import com.za.games.platform.LocalZaHaptics
import com.za.games.platform.LocalZaSound
import com.za.games.ui.common.GameTopBar
import com.za.games.ui.common.HoldButton
import com.za.games.ui.common.OverlayCard
import com.za.games.ui.common.ScoreCard
import com.za.games.ui.common.formatScore
import kotlinx.coroutines.isActive
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sin

/** Bölge paleti: zemin, kırılmaz duvar, kırılabilir blok, duvarın boşluğa bakan yüzü. */
private class AreaPalette(val bg: Color, val wall: Color, val block: Color, val wallEdge: Color)

private val Palettes = listOf(
    AreaPalette(Color(0xFF0B0F1A), Color(0xFF2B3550), Color(0xFF445273), Color(0xFF3D4B6E)),
    AreaPalette(Color(0xFF08120E), Color(0xFF1F3A2E), Color(0xFF2F5A47), Color(0xFF2C5040)),
    AreaPalette(Color(0xFF16090F), Color(0xFF3F1F2E), Color(0xFF64304A), Color(0xFF572B42)),
)

private val PlayerColor = KuyuFx.PLAYER
private val EnemyColor = KuyuFx.ENEMY
private val GemColor = KuyuFx.GEM
private val MuzzleColor = KuyuFx.MUZZLE
private val DarkColor = Color(0xFF06121D)

@Composable
fun KuyuScreen(
    highScore: Long,
    onScore: (Long) -> Unit,
    onExit: () -> Unit,
    viewModel: KuyuViewModel = viewModel(),
) {
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val leftHanded by viewModel.leftHanded.collectAsStateWithLifecycle()
    val daily by viewModel.daily.collectAsStateWithLifecycle()
    val hud by viewModel.hud.collectAsStateWithLifecycle()
    val runId by viewModel.runId.collectAsStateWithLifecycle()
    val haptics = LocalZaHaptics.current
    val sound = LocalZaSound.current
    val resources = LocalContext.current.resources
    val fx = remember { KuyuFx() }
    val fxTick = remember { mutableLongStateOf(0L) }

    // Skor bitişte ve ekrandan çıkarken bildirilir; rekor deposu en yükseği tutar.
    val latestScore by rememberUpdatedState(hud.score)
    val latestOnScore by rememberUpdatedState(onScore)
    DisposableEffect(Unit) {
        onDispose { latestOnScore(latestScore) }
    }
    LaunchedEffect(phase) {
        if (phase == KuyuPhase.OVER) latestOnScore(hud.score)
    }
    LaunchedEffect(runId) { fx.reset() }
    LaunchedEffect(Unit) { viewModel.refreshDaily() }
    LifecycleResumeEffect(Unit) {
        onPauseOrDispose { viewModel.pause() }
    }
    BackHandler {
        if (phase == KuyuPhase.PLAYING) viewModel.pause() else onExit()
    }

    // Kare döngüsü: oyun koşarken ve bitiş parçacıkları sönene dek. Ekrandan
    // çıkınca durur; ViewModel dünyayı duraklatılmış tutar.
    LaunchedEffect(phase, runId) {
        if (phase != KuyuPhase.PLAYING && phase != KuyuPhase.OVER) return@LaunchedEffect
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
            if (phase == KuyuPhase.OVER && !fx.isBusy) break
        }
    }

    // Rekor karşılaştırması: koşu başlamadan önceki en iyi.
    var previousBest by remember { mutableLongStateOf(highScore) }
    val startRun = {
        previousBest = maxOf(previousBest, hud.score)
        viewModel.start()
    }
    val restartRun = {
        previousBest = maxOf(previousBest, hud.score)
        viewModel.restart()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        GameTopBar(title = stringResource(R.string.game_kuyu), onExit = onExit) {
            if (phase == KuyuPhase.PLAYING || phase == KuyuPhase.PAUSED) {
                TextButton(onClick = viewModel::togglePause) {
                    Text(
                        text = stringResource(
                            if (phase == KuyuPhase.PAUSED) R.string.resume else R.string.pause,
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
            ScoreCard(
                label = stringResource(R.string.kuyu_depth),
                value = stringResource(R.string.kuyu_depth_fmt, hud.depth),
                modifier = Modifier.weight(1f),
            )
        }

        StatusRow(hud)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            KuyuCanvas(
                viewModel = viewModel,
                fx = fx,
                fxTick = fxTick,
                hud = hud,
                modifier = Modifier.fillMaxSize(),
            )
            when (phase) {
                KuyuPhase.MENU -> StartCard(
                    mode = mode,
                    daily = daily,
                    leftHanded = leftHanded,
                    onMode = viewModel::setMode,
                    onHand = viewModel::setLeftHanded,
                    onStart = startRun,
                    onExit = onExit,
                )
                KuyuPhase.PAUSED -> PauseCard(
                    daily = mode == KuyuMode.DAILY,
                    leftHanded = leftHanded,
                    onHand = viewModel::setLeftHanded,
                    onResume = viewModel::resume,
                    onRestart = restartRun,
                    onMenu = viewModel::toMenu,
                    onExit = onExit,
                )
                KuyuPhase.OVER -> OverCard(
                    hud = hud,
                    daily = mode == KuyuMode.DAILY,
                    isRecord = hud.score > previousBest,
                    onRestart = restartRun,
                    onMenu = viewModel::toMenu,
                    onExit = onExit,
                )
                KuyuPhase.PLAYING -> Unit
            }
        }

        Controls(leftHanded = leftHanded, viewModel = viewModel)
    }
}

@Composable
private fun StatusRow(hud: KuyuHud) {
    val hpDesc = stringResource(R.string.kuyu_hp_desc_fmt, hud.hp, KuyuWorld.MAX_HP)
    val ammoDesc = stringResource(R.string.kuyu_ammo_desc_fmt, hud.ammo, KuyuWorld.AMMO)
    val gemsDesc = stringResource(R.string.kuyu_gems) + " " + hud.gems
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "♥".repeat(hud.hp) + "♡".repeat(KuyuWorld.MAX_HP - hud.hp),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { contentDescription = hpDesc },
        )
        AmmoPips(ammo = hud.ammo, modifier = Modifier.semantics { contentDescription = ammoDesc })
        Spacer(Modifier.weight(1f))
        if (hud.combo >= 2) {
            Text(
                text = stringResource(R.string.kuyu_combo_fmt, hud.combo),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Text(
            text = "◆ " + hud.gems,
            color = GemColor,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.semantics { contentDescription = gemsDesc },
        )
    }
}

@Composable
private fun AmmoPips(ammo: Int, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .width(88.dp)
            .height(12.dp),
    ) {
        val gap = 3.dp.toPx()
        val w = (size.width - gap * (KuyuWorld.AMMO - 1)) / KuyuWorld.AMMO
        for (i in 0 until KuyuWorld.AMMO) {
            drawRoundRect(
                color = if (i < ammo) MuzzleColor else MuzzleColor.copy(alpha = 0.2f),
                topLeft = Offset(i * (w + gap), 0f),
                size = Size(w, size.height),
                cornerRadius = CornerRadius(3f, 3f),
            )
        }
    }
}

/** Kontrol satırı: ateş tuşu seçilen başparmağın tarafında. */
@Composable
private fun Controls(leftHanded: Boolean, viewModel: KuyuViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(84.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (leftHanded) {
            FireButton(viewModel)
            Spacer(Modifier.weight(0.25f))
            MoveButtons(viewModel)
        } else {
            MoveButtons(viewModel)
            Spacer(Modifier.weight(0.25f))
            FireButton(viewModel)
        }
    }
}

@Composable
private fun RowScope.MoveButtons(viewModel: KuyuViewModel) {
    HoldButton(
        label = "◀",
        description = stringResource(R.string.kuyu_ctrl_left),
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        onPressChange = viewModel::pressLeft,
    )
    HoldButton(
        label = "▶",
        description = stringResource(R.string.kuyu_ctrl_right),
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        onPressChange = viewModel::pressRight,
    )
}

@Composable
private fun RowScope.FireButton(viewModel: KuyuViewModel) {
    HoldButton(
        label = "●",
        description = stringResource(R.string.kuyu_ctrl_fire),
        modifier = Modifier
            .weight(1.3f)
            .fillMaxHeight(),
        accent = true,
        fontSize = 30.sp,
        onPressChange = viewModel::pressFire,
    )
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
private fun StartCard(
    mode: KuyuMode,
    daily: KuyuDaily?,
    leftHanded: Boolean,
    onMode: (KuyuMode) -> Unit,
    onHand: (Boolean) -> Unit,
    onStart: () -> Unit,
    onExit: () -> Unit,
) {
    OverlayCard {
        Text(
            text = stringResource(R.string.game_kuyu),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.kuyu_intro),
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
                selected = mode == KuyuMode.DAILY,
                modifier = Modifier.weight(1f),
            ) { onMode(KuyuMode.DAILY) }
            ModeChip(
                label = stringResource(R.string.mode_free),
                selected = mode == KuyuMode.FREE,
                modifier = Modifier.weight(1f),
            ) { onMode(KuyuMode.FREE) }
        }
        if (mode == KuyuMode.DAILY) {
            if (daily != null) {
                Text(
                    text = stringResource(R.string.kuyu_daily_done_fmt, daily.score, daily.depth),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.kuyu_tomorrow),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            } else {
                Text(
                    text = stringResource(R.string.kuyu_daily_desc),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
        Text(
            text = stringResource(R.string.kuyu_hand_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        HandChips(leftHanded = leftHanded, onHand = onHand)
        Text(
            text = stringResource(R.string.kuyu_hand_hint),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        )
        Spacer(Modifier.height(4.dp))
        if (mode == KuyuMode.DAILY && daily != null) {
            Button(onClick = { onMode(KuyuMode.FREE) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.play_free))
            }
        } else {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.kuyu_start))
            }
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

@Composable
private fun PauseCard(
    daily: Boolean,
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
            Text(stringResource(if (daily) R.string.play_free else R.string.restart))
        }
        TextButton(onClick = onMenu, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.kuyu_to_start))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

@Composable
private fun OverCard(
    hud: KuyuHud,
    daily: Boolean,
    isRecord: Boolean,
    onRestart: () -> Unit,
    onMenu: () -> Unit,
    onExit: () -> Unit,
) {
    OverlayCard {
        Text(
            text = stringResource(R.string.kuyu_over_title),
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
            text = stringResource(R.string.kuyu_result_fmt, hud.depth, hud.gems, hud.bestCombo),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
        if (daily) {
            Text(
                text = stringResource(R.string.kuyu_tomorrow),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        Spacer(Modifier.height(4.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (daily) R.string.play_free else R.string.restart))
        }
        TextButton(onClick = onMenu, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.kuyu_to_start))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

/**
 * Kuyu tuvali: 12 sütun ekran genişliğine sığdırılır, kamera [KuyuWorld.viewTop]
 * satırından başlar. Dünya doğrudan okunur; kare sayaçları her adımda yeniden
 * çizim tetikler.
 */
@Composable
private fun KuyuCanvas(
    viewModel: KuyuViewModel,
    fx: KuyuFx,
    fxTick: MutableLongState,
    hud: KuyuHud,
    modifier: Modifier = Modifier,
) {
    val frame by viewModel.frame.collectAsStateWithLifecycle()
    val desc = stringResource(R.string.kuyu_board_desc, hud.depth, hud.hp, hud.ammo)
    val textMeasurer = rememberTextMeasurer()
    val textCache = remember { HashMap<String, TextLayoutResult>() }
    val palette = Palettes[hud.area.coerceIn(0, Palettes.size - 1)]
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .semantics { contentDescription = desc },
    ) {
        // Sayaçlar çizim evresinde okunur: simülasyon adımı ve efekt karesi yeniden çizim tetikler.
        if (frame < 0L || fxTick.longValue < 0L) return@Canvas
        val world = viewModel.world
        val cell = size.width / KuyuWorld.WIDTH
        drawRect(palette.bg)
        val top = world.viewTop
        val visibleRows = ceil(size.height / cell).toInt() + 1
        val firstRow = floor(top).toInt()
        translate(fx.shakeX * cell, fx.shakeY * cell) {
            drawTiles(world, palette, cell, top, firstRow, visibleRows)
            drawGems(world, cell, top, visibleRows)
            drawBullets(world, cell, top)
            drawEnemies(world, cell, top, firstRow, visibleRows, frame)
            if (world.player.invincible == 0 || (frame / 3) % 2 == 0L) {
                drawPlayer(world.player, cell, top)
            }
            drawParticles(fx, cell, top)
            drawTexts(fx, textMeasurer, textCache, cell, top)
        }
        if (fx.flash > 0f) drawRect(EnemyColor.copy(alpha = fx.flash * 0.35f))
    }
}

private fun DrawScope.drawTiles(
    world: KuyuWorld,
    palette: AreaPalette,
    cell: Float,
    top: Float,
    firstRow: Int,
    rows: Int,
) {
    val inset = cell * 0.06f
    val corner = CornerRadius(cell * 0.18f, cell * 0.18f)
    val edge = cell * 0.12f
    for (r in firstRow..firstRow + rows) {
        val y = (r - top) * cell
        for (c in 0 until KuyuWorld.WIDTH) {
            val t = world.tile(r, c)
            if (t == Tile.EMPTY) continue
            val x = c * cell
            if (t == Tile.WALL) {
                drawRect(palette.wall, Offset(x, y), Size(cell + 1f, cell + 1f))
                if (c + 1 < KuyuWorld.WIDTH && !world.tile(r, c + 1).solid) {
                    drawRect(palette.wallEdge, Offset(x + cell - edge, y), Size(edge, cell + 1f))
                }
                if (c - 1 >= 0 && !world.tile(r, c - 1).solid) {
                    drawRect(palette.wallEdge, Offset(x, y), Size(edge, cell + 1f))
                }
            } else {
                drawRoundRect(
                    color = palette.block,
                    topLeft = Offset(x + inset, y + inset),
                    size = Size(cell - 2 * inset, cell - 2 * inset),
                    cornerRadius = corner,
                )
                if (t == Tile.GEM_BLOCK) {
                    drawDiamond(Offset(x + cell / 2f, y + cell / 2f), cell * 0.16f, GemColor)
                }
            }
        }
    }
}

private fun DrawScope.drawDiamond(center: Offset, r: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - r)
        lineTo(center.x + r, center.y)
        lineTo(center.x, center.y + r)
        lineTo(center.x - r, center.y)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawGems(world: KuyuWorld, cell: Float, top: Float, rows: Int) {
    for (g in world.gems) {
        if (g.y < top - 1f || g.y > top + rows + 1f) continue
        drawDiamond(Offset(g.x * cell, (g.y - top) * cell), cell * 0.18f, GemColor)
    }
}

private fun DrawScope.drawBullets(world: KuyuWorld, cell: Float, top: Float) {
    for (b in world.bullets) {
        drawRect(
            color = PlayerColor,
            topLeft = Offset(b.x * cell - cell * 0.05f, (b.y - top) * cell - cell * 0.35f),
            size = Size(cell * 0.1f, cell * 0.35f),
        )
    }
}

private fun DrawScope.drawEnemies(
    world: KuyuWorld,
    cell: Float,
    top: Float,
    firstRow: Int,
    rows: Int,
    frame: Long,
) {
    val limit = firstRow + rows + 1f
    for (e in world.enemies) {
        if (!e.alive || e.y > limit || e.y + e.h < top - 1f) continue
        val x = e.x * cell
        val y = (e.y - top) * cell
        val w = e.w * cell
        val h = e.h * cell
        val color = if (e.hitFlash > 0) PlayerColor else EnemyColor
        when (e.kind) {
            EnemyKind.BLOB -> {
                drawRoundRect(color, Offset(x, y), Size(w, h), CornerRadius(h * 0.45f, h * 0.45f))
                val eyeY = y + h * 0.4f
                val eyeR = h * 0.09f
                val look = e.dir * w * 0.06f
                drawCircle(DarkColor, eyeR, Offset(x + w * 0.35f + look, eyeY))
                drawCircle(DarkColor, eyeR, Offset(x + w * 0.65f + look, eyeY))
            }
            EnemyKind.BAT -> {
                val flap = sin(frame * 0.5f) * h * 0.3f
                drawOval(color, Offset(x + w * 0.28f, y + h * 0.15f), Size(w * 0.44f, h * 0.7f))
                val wings = Path().apply {
                    moveTo(x + w * 0.32f, y + h * 0.5f)
                    lineTo(x, y + h * 0.25f + flap)
                    lineTo(x + w * 0.1f, y + h * 0.8f)
                    close()
                    moveTo(x + w * 0.68f, y + h * 0.5f)
                    lineTo(x + w, y + h * 0.25f + flap)
                    lineTo(x + w * 0.9f, y + h * 0.8f)
                    close()
                }
                drawPath(wings, color)
                drawCircle(DarkColor, h * 0.07f, Offset(x + w * 0.5f, y + h * 0.4f))
            }
            EnemyKind.SPIKY -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y + h * 0.4f),
                    size = Size(w, h * 0.6f),
                    cornerRadius = CornerRadius(h * 0.2f, h * 0.2f),
                )
                val spikes = Path().apply {
                    for (i in 0 until 3) {
                        val left = x + w * (i / 3f)
                        moveTo(left, y + h * 0.42f)
                        lineTo(left + w / 6f, y)
                        lineTo(left + w / 3f, y + h * 0.42f)
                        close()
                    }
                }
                drawPath(spikes, MuzzleColor)
                drawCircle(DarkColor, h * 0.07f, Offset(x + w * 0.5f, y + h * 0.68f))
            }
            EnemyKind.CRAWLER -> {
                drawRoundRect(color, Offset(x, y), Size(w, h), CornerRadius(w * 0.3f, w * 0.3f))
                for (i in 1..3) {
                    drawRect(DarkColor, Offset(x + w * 0.15f, y + h * (i / 4f)), Size(w * 0.7f, h * 0.06f))
                }
            }
        }
    }
}

private fun DrawScope.drawPlayer(p: Player, cell: Float, top: Float) {
    val x = p.x * cell
    val y = (p.y - top) * cell
    val w = KuyuWorld.PLAYER_W * cell
    val h = KuyuWorld.PLAYER_H * cell
    drawRoundRect(PlayerColor, Offset(x, y), Size(w, h * 0.78f), CornerRadius(w * 0.25f, w * 0.25f))
    val visorX = if (p.facing > 0) x + w * 0.5f else x + w * 0.15f
    drawRect(DarkColor, Offset(visorX, y + h * 0.18f), Size(w * 0.35f, h * 0.14f))
    val bootY = y + h * 0.78f
    drawRect(MuzzleColor, Offset(x + w * 0.08f, bootY), Size(w * 0.34f, h * 0.22f))
    drawRect(MuzzleColor, Offset(x + w * 0.58f, bootY), Size(w * 0.34f, h * 0.22f))
    if (p.shotCooldown > KuyuWorld.SHOT_INTERVAL - 3) {
        val flame = Path().apply {
            moveTo(x + w * 0.2f, y + h)
            lineTo(x + w * 0.8f, y + h)
            lineTo(x + w * 0.5f, y + h + cell * 0.45f)
            close()
        }
        drawPath(flame, MuzzleColor.copy(alpha = 0.85f))
    }
}

private fun DrawScope.drawParticles(fx: KuyuFx, cell: Float, top: Float) {
    for (q in fx.particles) {
        val s = q.size * cell
        val alpha = (q.life / q.maxLife).coerceIn(0f, 1f)
        drawRect(
            color = q.color.copy(alpha = alpha),
            topLeft = Offset(q.x * cell - s / 2f, (q.y - top) * cell - s / 2f),
            size = Size(s, s),
        )
    }
}

private fun DrawScope.drawTexts(
    fx: KuyuFx,
    measurer: TextMeasurer,
    cache: HashMap<String, TextLayoutResult>,
    cell: Float,
    top: Float,
) {
    for (t in fx.texts) {
        val key = t.text + "|" + cell.toInt()
        val layout = cache.getOrPut(key) {
            measurer.measure(
                text = t.text,
                style = TextStyle(fontSize = (cell * 0.55f).toSp(), fontWeight = FontWeight.Bold),
            )
        }
        val alpha = (t.life / t.maxLife).coerceIn(0f, 1f)
        val maxX = (size.width - layout.size.width).coerceAtLeast(0f)
        val x = (t.x * cell - layout.size.width / 2f).coerceIn(0f, maxX)
        drawText(
            textLayoutResult = layout,
            color = t.color,
            topLeft = Offset(x, (t.y - top) * cell - layout.size.height),
            alpha = alpha,
        )
    }
}
