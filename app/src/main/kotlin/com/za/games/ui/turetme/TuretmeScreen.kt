package com.za.games.ui.turetme

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.za.games.R
import com.za.games.platform.LocalZaHaptics
import com.za.games.platform.LocalZaSound
import com.za.games.platform.Sfx
import com.za.games.turetme.TuretmeInvalid
import com.za.games.turetme.TuretmeState
import com.za.games.turetme.TuretmeStatus
import com.za.games.ui.common.GameTopBar
import com.za.games.ui.common.OverlayCard
import com.za.games.ui.common.PadButton
import com.za.games.ui.common.ScoreCard
import com.za.games.ui.common.formatScore
import kotlinx.coroutines.delay
import java.util.Locale

private val TrLocale: Locale = Locale.forLanguageTag("tr")

private fun Char.upperTr(): String = toString().uppercase(TrLocale)

private fun String.upperTr(): String = uppercase(TrLocale)

private val AccentPurple = Color(0xFFA78BFA)

@Composable
fun TuretmeScreen(
    highScore: Long,
    onScore: (Long) -> Unit,
    onExit: () -> Unit,
    viewModel: TuretmeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val haptics = LocalZaHaptics.current
    val sound = LocalZaSound.current
    val latestOnScore by rememberUpdatedState(onScore)

    // Rekor = en yüksek tur puanı; platform max ile saklar.
    LaunchedEffect(state.score) {
        if (state.score > 0) latestOnScore(state.score)
    }
    // Gün değişmiş olabilir; günlük turu tazele.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshDaily()
        onPauseOrDispose { }
    }
    // Kelime bulma sesi: yalnızca canlı bulunanlarda (geri yüklemede çalmaz).
    var seenFound by remember(state.base, state.dailyDay) {
        mutableIntStateOf(state.found.size)
    }
    LaunchedEffect(state.found.size) {
        if (state.found.size > seenFound) {
            sound?.play(Sfx.POP, rate = 0.9f + state.found.size * 0.02f)
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        seenFound = state.found.size
    }
    // Tur tamamlama kutlaması: yalnızca canlı geçişte.
    var celebrated by remember(state.base, state.dailyDay) {
        mutableStateOf(state.status == TuretmeStatus.COMPLETED)
    }
    LaunchedEffect(state.status) {
        if (state.status == TuretmeStatus.COMPLETED && !celebrated) {
            celebrated = true
            sound?.play(Sfx.BIG)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    // Geçersiz gönderim uyarısı.
    var invalidMessage by remember { mutableStateOf<TuretmeInvalid?>(null) }
    var seenInvalid by remember { mutableIntStateOf(state.invalidEvents) }
    LaunchedEffect(state.invalidEvents) {
        val previous = seenInvalid
        seenInvalid = state.invalidEvents
        if (state.invalidEvents > previous) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            invalidMessage = state.lastInvalid
            delay(1400L)
            invalidMessage = null
        }
    }
    var showResult by remember(state.base, state.status) { mutableStateOf(true) }
    BackHandler { onExit() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        GameTopBar(title = stringResource(R.string.game_turetme), onExit = onExit) {
            if (mode == TuretmeMode.FREE) {
                TextButton(onClick = viewModel::newFreeGame) {
                    Text(stringResource(R.string.new_word))
                }
            }
        }

        ModeChips(mode = mode, onSelect = viewModel::setMode)

        Text(
            text = stringResource(R.string.turetme_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ScoreCard(
                label = stringResource(R.string.score),
                value = formatScore(state.score),
                modifier = Modifier.weight(1f),
                highlight = true,
            )
            ScoreCard(
                label = stringResource(R.string.high_score),
                value = formatScore(maxOf(highScore, state.score)),
                modifier = Modifier.weight(1f),
            )
            ScoreCard(
                label = stringResource(R.string.found_label),
                value = "${state.found.size}/${state.targets.size}",
                modifier = Modifier.weight(1f),
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            FoundWords(state)
            if (state.status == TuretmeStatus.COMPLETED && showResult) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CompletedOverlay(
                        state = state,
                        mode = mode,
                        onPlayFree = { viewModel.setMode(TuretmeMode.FREE) },
                        onNewWord = viewModel::newFreeGame,
                        onDismiss = { showResult = false },
                        onExit = onExit,
                    )
                }
            }
        }

        Text(
            text = invalidMessage?.let {
                stringResource(
                    when (it) {
                        TuretmeInvalid.TOO_SHORT -> R.string.too_short
                        TuretmeInvalid.NOT_WORD -> R.string.not_in_list
                        TuretmeInvalid.ALREADY_FOUND -> R.string.already_found
                    },
                )
            } ?: " ",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        // Seçilen harflerin oluşturduğu kelime adayı.
        Text(
            text = if (state.current.isEmpty()) " " else state.current.upperTr(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 5.sp,
            color = AccentPurple,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        )

        LetterRow(
            letters = state.letters,
            usedIndices = state.usedIndices,
            enabled = state.status == TuretmeStatus.RUNNING,
            onPick = { index ->
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.pick(index)
            },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PadButton(
                label = "⌫",
                description = stringResource(R.string.erase),
                modifier = Modifier.weight(1f).height(52.dp),
                repeatIntervalMs = 150L,
                onAction = viewModel::erase,
            )
            PadButton(
                label = "🔀",
                description = stringResource(R.string.shuffle_letters),
                modifier = Modifier.weight(1f).height(52.dp),
                onAction = viewModel::shuffle,
            )
            PadButton(
                label = stringResource(R.string.key_enter),
                description = stringResource(R.string.key_enter),
                modifier = Modifier.weight(1.6f).height(52.dp),
                accent = true,
                onAction = viewModel::submit,
            )
        }
    }
}

@Composable
private fun ModeChips(mode: TuretmeMode, onSelect: (TuretmeMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ModeChip(
            label = stringResource(R.string.mode_daily),
            selected = mode == TuretmeMode.DAILY,
            modifier = Modifier.weight(1f),
        ) { onSelect(TuretmeMode.DAILY) }
        ModeChip(
            label = stringResource(R.string.mode_free),
            selected = mode == TuretmeMode.FREE,
            modifier = Modifier.weight(1f),
        ) { onSelect(TuretmeMode.FREE) }
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
        modifier = modifier.height(40.dp),
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FoundWords(state: TuretmeState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            state.found.sortedWith(compareBy({ it.length }, { it })).forEach { word ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (word == state.base) {
                        AccentPurple.copy(alpha = 0.3f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ) {
                    Text(
                        text = word.upperTr(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (word == state.base) {
                            AccentPurple
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
        if (state.found.isEmpty()) {
            Text(
                text = stringResource(R.string.turetme_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            )
        }
    }
}

@Composable
private fun LetterRow(
    letters: List<Char>,
    usedIndices: List<Int>,
    enabled: Boolean,
    onPick: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        letters.forEachIndexed { index, letter ->
            val used = index in usedIndices
            Surface(
                onClick = { if (enabled && !used) onPick(index) },
                shape = RoundedCornerShape(14.dp),
                color = if (used) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                } else {
                    AccentPurple.copy(alpha = 0.25f)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = letter.upperTr(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = if (used) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        } else {
                            AccentPurple
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletedOverlay(
    state: TuretmeState,
    mode: TuretmeMode,
    onPlayFree: () -> Unit,
    onNewWord: () -> Unit,
    onDismiss: () -> Unit,
    onExit: () -> Unit,
) {
    OverlayCard {
        Text(
            text = stringResource(R.string.turetme_completed),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = formatScore(state.score),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        if (mode == TuretmeMode.DAILY) {
            Text(
                text = stringResource(R.string.tomorrow_new_word),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
            Button(onClick = onPlayFree, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.play_free))
            }
        } else {
            Button(onClick = onNewWord, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.new_word))
            }
        }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.show_board))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}
