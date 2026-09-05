package com.za.games.ui.toplam

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.za.games.R
import com.za.games.platform.LocalZaHaptics
import com.za.games.platform.LocalZaSound
import com.za.games.platform.Sfx
import com.za.games.sayi.ToplamState
import com.za.games.ui.common.GameTopBar
import com.za.games.ui.common.OverlayCard
import com.za.games.ui.common.ScoreCard

private val P0Color = Color(0xFF22D3EE)
private val P1Color = Color(0xFFF472B6)
private val WinColor = Color(0xFF4ADE80)
private val Ink = Color(0xFF06121D)

@Composable
fun ToplamScreen(
    highScore: Long,
    onScore: (Long) -> Unit,
    onExit: () -> Unit,
    viewModel: ToplamViewModel = viewModel(),
) {
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val setup by viewModel.setup.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val thinking by viewModel.thinking.collectAsStateWithLifecycle()
    val tally by viewModel.tally.collectAsStateWithLifecycle()
    val wins by viewModel.wins.collectAsStateWithLifecycle()
    val gameId by viewModel.gameId.collectAsStateWithLifecycle()
    val haptics = LocalZaHaptics.current
    val sound = LocalZaSound.current
    val latestOnScore by rememberUpdatedState(onScore)
    var showSecret by remember { mutableStateOf(false) }
    val vsComputer = viewModel.matchVsComputer
    val names = listOf(
        if (vsComputer) stringResource(R.string.toplam_you) else stringResource(R.string.toplam_player_fmt, 1),
        if (vsComputer) stringResource(R.string.toplam_computer) else stringResource(R.string.toplam_player_fmt, 2),
    )

    LaunchedEffect(wins) {
        if (wins > 0) latestOnScore(wins.toLong())
    }
    LaunchedEffect(state.over, gameId) {
        if (!state.over) return@LaunchedEffect
        val humanWon = state.winner != null && (!vsComputer || state.winner == 0)
        sound?.play(if (state.winner == null) Sfx.CLEAR else if (humanWon) Sfx.BIG else Sfx.OVER)
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    LaunchedEffect(gameId) { showSecret = false }
    BackHandler {
        if (phase == ToplamPhase.PLAYING) viewModel.toSetup() else onExit()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        GameTopBar(title = stringResource(R.string.game_toplam), onExit = onExit) {
            Text(
                text = "🏆 ${maxOf(wins.toLong(), highScore)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 14.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ScoreCard(label = names[0], value = tally.p0.toString(), modifier = Modifier.weight(1f), highlight = true)
            ScoreCard(label = stringResource(R.string.toplam_draws), value = tally.draws.toString(), modifier = Modifier.weight(0.8f))
            ScoreCard(label = names[1], value = tally.p1.toString(), modifier = Modifier.weight(1f))
        }

        Text(
            text = when {
                phase != ToplamPhase.PLAYING -> stringResource(R.string.toplam_rules)
                state.over && state.winner == null -> stringResource(R.string.toplam_draw)
                state.over -> stringResource(R.string.toplam_won_fmt, names[state.winner ?: 0])
                thinking -> stringResource(R.string.toplam_thinking)
                else -> stringResource(R.string.toplam_turn_fmt, names[state.turn])
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                NumberRow(
                    state = state,
                    enabled = phase == ToplamPhase.PLAYING && viewModel.humanCanAct(),
                    onPick = { x ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        sound?.play(Sfx.POP, volume = 0.5f)
                        viewModel.pick(x)
                    },
                )
                HandRow(name = names[0], picks = state.picks[0], color = P0Color, winning = if (state.winner == 0) state.winningTriple else emptyList())
                HandRow(name = names[1], picks = state.picks[1], color = P1Color, winning = if (state.winner == 1) state.winningTriple else emptyList())
                if (showSecret) MagicSquare(state = state)
            }
            if (phase == ToplamPhase.SETUP) {
                SetupCard(
                    setup = setup,
                    onVsComputer = viewModel::setVsComputer,
                    onPerfect = viewModel::setPerfect,
                    onStart = viewModel::start,
                    onExit = onExit,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (phase == ToplamPhase.PLAYING && state.over) {
                Button(onClick = viewModel::nextGame, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.toplam_again))
                }
                OutlinedButton(onClick = { showSecret = !showSecret }, modifier = Modifier.weight(0.7f)) {
                    Text(stringResource(R.string.toplam_secret))
                }
                TextButton(onClick = viewModel::toSetup, modifier = Modifier.weight(0.7f)) {
                    Text(stringResource(R.string.toplam_to_setup))
                }
            } else {
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun NumberRow(state: ToplamState, enabled: Boolean, onPick: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (x in 1..9) {
            val owner = state.ownerOf(x)
            val winning = x in state.winningTriple
            val desc = when (owner) {
                null -> stringResource(R.string.toplam_chip_free_fmt, x)
                else -> stringResource(R.string.toplam_chip_taken_fmt, x)
            }
            Surface(
                onClick = { onPick(x) },
                enabled = enabled && owner == null,
                shape = CircleShape,
                color = when {
                    winning -> WinColor
                    owner == 0 -> P0Color.copy(alpha = 0.85f)
                    owner == 1 -> P1Color.copy(alpha = 0.85f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .semantics { contentDescription = desc },
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = x.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (owner != null || winning) Ink else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun HandRow(name: String, picks: List<Int>, color: Color, winning: List<Int>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 4.dp),
        )
        if (picks.isEmpty()) {
            Text(text = "—", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        }
        picks.sorted().forEach { x ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (x in winning) WinColor else color.copy(alpha = 0.25f),
            ) {
                Text(
                    text = x.toString(),
                    fontWeight = FontWeight.Bold,
                    color = if (x in winning) Ink else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/** Sır: sayılar sihirli karede; satır, sütun ve köşegenler 15 → üç taş. */
@Composable
private fun MagicSquare(state: ToplamState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.toplam_secret_text),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
        )
        for (r in 0 until 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (c in 0 until 3) {
                    val x = ToplamState.MAGIC_SQUARE[r * 3 + c]
                    val owner = state.ownerOf(x)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                when (owner) {
                                    0 -> P0Color.copy(alpha = 0.8f)
                                    1 -> P1Color.copy(alpha = 0.8f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                RoundedCornerShape(8.dp),
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = x.toString(),
                            fontWeight = FontWeight.Bold,
                            color = if (owner != null) Ink else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupCard(
    setup: ToplamSetup,
    onVsComputer: (Boolean) -> Unit,
    onPerfect: (Boolean) -> Unit,
    onStart: () -> Unit,
    onExit: () -> Unit,
) {
    OverlayCard {
        Text(
            text = stringResource(R.string.toplam_rules),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        )
        Text(
            text = stringResource(R.string.toplam_opponent_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionChip(stringResource(R.string.toplam_computer), setup.vsComputer, Modifier.weight(1f)) { onVsComputer(true) }
            OptionChip(stringResource(R.string.toplam_two_players), !setup.vsComputer, Modifier.weight(1f)) { onVsComputer(false) }
        }
        if (setup.vsComputer) {
            Text(
                text = stringResource(R.string.toplam_level_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionChip(stringResource(R.string.toplam_level_easy), !setup.perfect, Modifier.weight(1f)) { onPerfect(false) }
                OptionChip(stringResource(R.string.toplam_level_perfect), setup.perfect, Modifier.weight(1f)) { onPerfect(true) }
            }
        }
        Spacer(Modifier.height(2.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.toplam_start))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

@Composable
private fun OptionChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.height(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
