package com.za.games.ui.tuse

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.za.games.R
import com.za.games.platform.LocalZaHaptics
import com.za.games.platform.LocalZaSound
import com.za.games.platform.Sfx
import com.za.games.platform.ShareContent
import com.za.games.platform.SoundPlayer
import com.za.games.tuse.Song
import com.za.games.tuse.TuseEvent
import com.za.games.tuse.TuseHud
import com.za.games.tuse.TuseStatus
import com.za.games.tuse.TuseWorld
import com.za.games.ui.common.GameTopBar
import com.za.games.ui.common.OverlayCard
import com.za.games.ui.common.ScoreCard
import com.za.games.ui.common.ShareButton
import com.za.games.ui.common.modeShareLabel
import java.util.Locale
import kotlinx.coroutines.isActive

private val BoardColor = Color(0xFFF7F3EA)
private val LaneLine = Color(0x1F000000)
private val TileColor = Color(0xFF1F2937)
private val TileHit = Color(0x2E1F2937)
private val TileLabel = Color(0xFFF7F3EA)
private val CurrentRing = Color(0xFFE879F9)
private val MissColor = Color(0xFFEF4444)
private val BottomLine = Color(0x33000000)

/** Süreyi "8,43 s" biçiminde yazar (yerel ondalık ayracıyla). */
fun formatSeconds(ms: Long): String = String.format(Locale.getDefault(), "%.2f s", ms / 1000f)

private fun formatRate(value: Float): String = String.format(Locale.getDefault(), "%.1f", value)

@Composable
fun songName(song: Song): String = stringResource(
    when (song.id) {
        "ode" -> R.string.tuse_song_ode
        "elise" -> R.string.tuse_song_elise
        "turca" -> R.string.tuse_song_turca
        "twinkle" -> R.string.tuse_song_twinkle
        "birthday" -> R.string.tuse_song_birthday
        "minuet" -> R.string.tuse_song_minuet
        else -> R.string.tuse_song_greensleeves
    },
)

@Composable
private fun songCredit(song: Song): String = stringResource(
    when (song.id) {
        "ode" -> R.string.tuse_credit_ode
        "elise" -> R.string.tuse_credit_elise
        "turca" -> R.string.tuse_credit_turca
        "twinkle" -> R.string.tuse_credit_twinkle
        "birthday" -> R.string.tuse_credit_birthday
        "minuet" -> R.string.tuse_credit_minuet
        else -> R.string.tuse_credit_greensleeves
    },
)

@Composable
fun TuseScreen(
    highScore: Long,
    onScore: (Long) -> Unit,
    onExit: () -> Unit,
    viewModel: TuseViewModel = viewModel(),
) {
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val menu by viewModel.menu.collectAsStateWithLifecycle()
    val songId by viewModel.songId.collectAsStateWithLifecycle()
    val daily by viewModel.daily.collectAsStateWithLifecycle()
    val classicBest by viewModel.classicBest.collectAsStateWithLifecycle()
    val arcadeBest by viewModel.arcadeBest.collectAsStateWithLifecycle()
    val hud by viewModel.hud.collectAsStateWithLifecycle()
    val runId by viewModel.runId.collectAsStateWithLifecycle()
    val record by viewModel.record.collectAsStateWithLifecycle()
    val haptics = LocalZaHaptics.current
    val sound = LocalZaSound.current
    val context = LocalContext.current
    val notes = remember(sound) { if (sound != null) NotePlayer(context) { sound.isEnabled } else null }
    DisposableEffect(notes) {
        onDispose { notes?.release() }
    }
    val fxTick = remember { mutableLongStateOf(0L) }

    // Menüdeki parça ve süren koşunun parçası: notaları önceden hazırla.
    val menuSong = viewModel.songForMenu()
    val runSong = viewModel.runSong
    LaunchedEffect(notes, menuSong.id, runId) {
        notes?.prepare(if (phase == TusePhase.MENU) menuSong.notes else runSong.notes)
    }

    // Ana menü rekoru: Sonsuz'da vurulan karo.
    val arcadeScore = if (viewModel.runMenu == TuseMenuMode.ARCADE) hud.tapped.toLong() else 0L
    val latestScore by rememberUpdatedState(arcadeScore)
    val latestOnScore by rememberUpdatedState(onScore)
    DisposableEffect(Unit) {
        onDispose { latestOnScore(latestScore) }
    }
    LaunchedEffect(phase) {
        if (phase == TusePhase.OVER) latestOnScore(arcadeScore)
    }
    LaunchedEffect(Unit) { viewModel.refresh() }
    LifecycleResumeEffect(Unit) {
        onPauseOrDispose { viewModel.abandon() }
    }
    BackHandler {
        if (phase == TusePhase.PLAYING) viewModel.toMenu() else onExit()
    }

    LaunchedEffect(phase, runId) {
        if (phase != TusePhase.PLAYING) return@LaunchedEffect
        var last = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (last != 0L) {
                    for (event in viewModel.advance(now - last)) onEvent(event, notes, sound, haptics)
                    fxTick.longValue += 1
                }
                last = now
            }
        }
    }

    var previousBest by remember { mutableLongStateOf(highScore) }
    val startRun = {
        previousBest = maxOf(previousBest, latestScore)
        viewModel.start()
    }
    val restartRun = {
        previousBest = maxOf(previousBest, latestScore)
        viewModel.restart()
    }
    val attemptsLeft = TuseViewModel.DAILY_ATTEMPTS - (daily?.attempts ?: 0)
    val courtMenu = if (phase == TusePhase.MENU) menu else viewModel.runMenu

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        GameTopBar(title = stringResource(R.string.game_tuse), onExit = onExit) {
            if (phase == TusePhase.PLAYING) {
                TextButton(onClick = viewModel::toMenu) {
                    Text(stringResource(R.string.tuse_to_menu))
                }
            }
        }

        ScoreRow(
            menu = courtMenu,
            hud = hud,
            classicBest = if (courtMenu == TuseMenuMode.DAILY) daily?.bestMs ?: 0L else classicBest,
            arcadeBest = maxOf(arcadeBest, if (courtMenu == TuseMenuMode.ARCADE) hud.tapped else 0),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            TuseCanvas(
                viewModel = viewModel,
                fxTick = fxTick,
                hud = hud,
                onTap = { lane -> for (event in viewModel.tap(lane)) onEvent(event, notes, sound, haptics) },
                modifier = Modifier.fillMaxSize(),
            )
            when (phase) {
                TusePhase.MENU -> StartCard(
                    menu = menu,
                    song = menuSong,
                    daily = daily,
                    classicBest = classicBest,
                    arcadeBest = arcadeBest,
                    onMenu = viewModel::setMenu,
                    onShiftSong = viewModel::shiftSong,
                    onStart = startRun,
                    onExit = onExit,
                )
                TusePhase.OVER -> OverCard(
                    menu = viewModel.runMenu,
                    song = runSong,
                    hud = hud,
                    isRecord = record || (viewModel.runMenu == TuseMenuMode.ARCADE && arcadeScore > previousBest),
                    attemptsLeft = attemptsLeft,
                    onRestart = restartRun,
                    onMenu = viewModel::toMenu,
                    onExit = onExit,
                )
                TusePhase.PLAYING -> Unit
            }
        }
    }
}

private fun onEvent(event: TuseEvent, notes: NotePlayer?, sound: SoundPlayer?, haptics: HapticFeedback) {
    when (event) {
        is TuseEvent.Hit -> {
            notes?.play(event.note)
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        is TuseEvent.Miss -> {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            sound?.play(Sfx.OVER, volume = 0.8f)
        }
        is TuseEvent.Passed -> {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            sound?.play(Sfx.OVER, volume = 0.8f, rate = 0.9f)
        }
        TuseEvent.Done -> sound?.play(Sfx.BIG, volume = 0.7f, rate = 1.1f)
        TuseEvent.Started -> Unit
    }
}

@Composable
private fun ScoreRow(menu: TuseMenuMode, hud: TuseHud, classicBest: Long, arcadeBest: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (menu == TuseMenuMode.ARCADE) {
            ScoreCard(label = stringResource(R.string.tuse_tiles), value = hud.tapped.toString(), modifier = Modifier.weight(1f), highlight = true)
            ScoreCard(label = stringResource(R.string.tuse_speed), value = formatRate(hud.speed), modifier = Modifier.weight(1f))
            ScoreCard(label = stringResource(R.string.tuse_best), value = if (arcadeBest > 0) arcadeBest.toString() else "–", modifier = Modifier.weight(1f))
        } else {
            val total = if (hud.total > 0) hud.total else TuseWorld.CLASSIC_TILES
            ScoreCard(label = stringResource(R.string.tuse_tiles), value = "${hud.tapped}/$total", modifier = Modifier.weight(1f), highlight = true)
            ScoreCard(label = stringResource(R.string.tuse_time), value = formatSeconds(hud.elapsedMs), modifier = Modifier.weight(1.1f))
            ScoreCard(label = stringResource(R.string.tuse_best), value = if (classicBest > 0L) formatSeconds(classicBest) else "–", modifier = Modifier.weight(1.1f))
        }
    }
}

// ---------------------------------------------------------------------------
// Çizim
// ---------------------------------------------------------------------------

@Composable
private fun TuseCanvas(
    viewModel: TuseViewModel,
    fxTick: MutableLongState,
    hud: TuseHud,
    onTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val frame by viewModel.frame.collectAsStateWithLifecycle()
    val desc = stringResource(R.string.tuse_board_desc_fmt, hud.tapped, hud.nextLane + 1)
    val startLabel = stringResource(R.string.tuse_start)
    val textMeasurer = rememberTextMeasurer()
    val textCache = remember { HashMap<String, TextLayoutResult>() }
    val currentOnTap by rememberUpdatedState(onTap)
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .semantics { contentDescription = desc }
            .pointerInput(Unit) {
                // Her parmak basışı bir dokunuş: şerit, basılan x'ten. Sürükleme
                // yok sayılır; iki parmak aynı anda iki dokunuş sayılır.
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        for (change in event.changes) {
                            if (change.changedToDownIgnoreConsumed()) {
                                val lane = (change.position.x / (size.width / TuseWorld.LANES.toFloat())).toInt().coerceIn(0, TuseWorld.LANES - 1)
                                currentOnTap(lane)
                            }
                            change.consume()
                        }
                    }
                }
            },
    ) {
        val tick = frame + fxTick.longValue
        if (tick < 0L) return@Canvas
        drawBoard(viewModel.world, textMeasurer, textCache, startLabel)
    }
}

private fun DrawScope.drawBoard(
    world: TuseWorld,
    textMeasurer: TextMeasurer,
    cache: HashMap<String, TextLayoutResult>,
    startLabel: String,
) {
    val w = size.width
    val h = size.height
    val laneW = w / TuseWorld.LANES
    val rowH = h / TuseWorld.ROWS
    drawRect(BoardColor)
    for (l in 1 until TuseWorld.LANES) {
        drawLine(LaneLine, Offset(laneW * l, 0f), Offset(laneW * l, h), strokeWidth = 2f)
    }
    drawLine(BottomLine, Offset(0f, h - 2f), Offset(w, h - 2f), strokeWidth = 4f)

    // Yanlış tuş ya da kaçan karo: şerit kızarır.
    val redLane = if (world.missLane >= 0) world.missLane else world.passedLane
    if (redLane >= 0) {
        drawRect(MissColor.copy(alpha = 0.18f), topLeft = Offset(laneW * redLane, 0f), size = Size(laneW, h))
        if (world.missLane >= 0) {
            drawRoundRect(MissColor.copy(alpha = 0.85f), topLeft = Offset(laneW * redLane + 4f, h - rowH + 4f), size = Size(laneW - 8f, rowH - 8f), cornerRadius = CornerRadius(10f, 10f))
        }
    }

    val first = world.firstVisible()
    for (i in first until first + TuseWorld.ROWS + 2) {
        val bottom = h - (i - world.scroll) * rowH
        val top = bottom - rowH
        if (bottom < 0f || top > h) continue
        val lane = world.lane(i)
        val hit = i < world.tapped
        val rect = Offset(laneW * lane + 4f, top + 4f)
        val sz = Size(laneW - 8f, rowH - 8f)
        drawRoundRect(if (hit) TileHit else TileColor, topLeft = rect, size = sz, cornerRadius = CornerRadius(10f, 10f))
        if (!hit && i == world.tapped) {
            drawRoundRect(CurrentRing, topLeft = rect, size = sz, cornerRadius = CornerRadius(10f, 10f), style = Stroke(width = 5f))
            if (!world.started) {
                val layout = cache.getOrPut("start|$startLabel|${(rowH * 0.22f).toInt()}") {
                    textMeasurer.measure(
                        AnnotatedString(startLabel),
                        style = TextStyle(fontSize = (rowH * 0.22f).toSp(), fontWeight = FontWeight.Black, color = TileLabel, textAlign = TextAlign.Center),
                    )
                }
                drawText(layout, topLeft = Offset(rect.x + (sz.width - layout.size.width) / 2f, rect.y + (sz.height - layout.size.height) / 2f))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Kartlar
// ---------------------------------------------------------------------------

@Composable
private fun StartCard(
    menu: TuseMenuMode,
    song: Song,
    daily: TuseDaily?,
    classicBest: Long,
    arcadeBest: Int,
    onMenu: (TuseMenuMode) -> Unit,
    onShiftSong: (Int) -> Unit,
    onStart: () -> Unit,
    onExit: () -> Unit,
) {
    val exhausted = menu == TuseMenuMode.DAILY && daily != null && daily.attempts >= TuseViewModel.DAILY_ATTEMPTS
    OverlayCard {
        Text(
            text = stringResource(R.string.game_tuse),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.tuse_intro),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeChip(stringResource(R.string.tuse_mode_classic), menu == TuseMenuMode.CLASSIC, Modifier.weight(1f)) { onMenu(TuseMenuMode.CLASSIC) }
            ModeChip(stringResource(R.string.tuse_mode_arcade), menu == TuseMenuMode.ARCADE, Modifier.weight(1f)) { onMenu(TuseMenuMode.ARCADE) }
            ModeChip(stringResource(R.string.mode_daily), menu == TuseMenuMode.DAILY, Modifier.weight(1f)) { onMenu(TuseMenuMode.DAILY) }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (menu != TuseMenuMode.DAILY) {
                IconButton(onClick = { onShiftSong(-1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.tuse_prev_song))
                }
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = songName(song),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = songCredit(song),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            if (menu != TuseMenuMode.DAILY) {
                IconButton(onClick = { onShiftSong(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.tuse_next_song))
                }
            }
        }
        when (menu) {
            TuseMenuMode.CLASSIC -> if (classicBest > 0L) {
                RecordLine(stringResource(R.string.tuse_classic_best_fmt, formatSeconds(classicBest)))
            }
            TuseMenuMode.ARCADE -> if (arcadeBest > 0) {
                RecordLine(stringResource(R.string.tuse_arcade_best_fmt, arcadeBest))
            }
            TuseMenuMode.DAILY -> {
                if (daily != null) {
                    RecordLine(
                        stringResource(
                            R.string.tuse_daily_status_fmt,
                            daily.attempts,
                            TuseViewModel.DAILY_ATTEMPTS,
                            if (daily.bestMs > 0L) formatSeconds(daily.bestMs) else "–",
                        ),
                    )
                }
                Text(
                    text = stringResource(if (exhausted) R.string.tuse_daily_exhausted else R.string.tuse_daily_desc),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        if (exhausted) {
            Button(onClick = { onMenu(TuseMenuMode.CLASSIC) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.play_free))
            }
        } else {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.tuse_start))
            }
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

@Composable
private fun RecordLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun OverCard(
    menu: TuseMenuMode,
    song: Song,
    hud: TuseHud,
    isRecord: Boolean,
    attemptsLeft: Int,
    onRestart: () -> Unit,
    onMenu: () -> Unit,
    onExit: () -> Unit,
) {
    val arcade = menu == TuseMenuMode.ARCADE
    val done = hud.status == TuseStatus.DONE
    val title = stringResource(
        when {
            done -> R.string.tuse_done
            hud.missLane >= 0 -> R.string.tuse_wrong_key
            hud.passedLane >= 0 -> R.string.tuse_missed_tile
            else -> R.string.tuse_abandoned
        },
    )
    val seconds = formatSeconds(hud.elapsedMs)
    val big = when {
        arcade -> hud.tapped.toString()
        done -> seconds
        else -> "${hud.tapped}/${hud.total}"
    }
    val result = when {
        arcade -> stringResource(R.string.tuse_result_arcade_fmt, hud.tapped, formatRate(hud.speed))
        done -> stringResource(R.string.tuse_result_classic_fmt, hud.tapped, seconds, formatRate(if (hud.elapsedMs > 0L) hud.tapped * 1000f / hud.elapsedMs else 0f))
        else -> stringResource(R.string.tuse_result_partial_fmt, hud.tapped, hud.total, seconds)
    }
    val name = songName(song)
    val headline = if (arcade) {
        stringResource(R.string.tuse_share_arcade_fmt, name, hud.tapped)
    } else {
        stringResource(R.string.tuse_share_classic_fmt, name, hud.tapped, seconds)
    }
    val modeLabel = when (menu) {
        TuseMenuMode.CLASSIC -> stringResource(R.string.tuse_mode_classic)
        TuseMenuMode.ARCADE -> stringResource(R.string.tuse_mode_arcade)
        TuseMenuMode.DAILY -> modeShareLabel(true, null)
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
            text = "$name · $result",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
        if (menu == TuseMenuMode.DAILY && attemptsLeft <= 0) {
            Text(
                text = stringResource(R.string.tuse_daily_exhausted),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        ShareButton(ShareContent(gameId = "tuse", headline = headline, details = listOf(modeLabel, result)))
        Spacer(Modifier.height(4.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text(
                when {
                    menu == TuseMenuMode.DAILY && attemptsLeft > 0 -> stringResource(R.string.tuse_retry_fmt, attemptsLeft)
                    menu == TuseMenuMode.DAILY -> stringResource(R.string.play_free)
                    else -> stringResource(R.string.restart)
                },
            )
        }
        TextButton(onClick = onMenu, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.tuse_to_menu))
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
