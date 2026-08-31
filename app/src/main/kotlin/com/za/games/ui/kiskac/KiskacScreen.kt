package com.za.games.ui.kiskac

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.za.games.R
import com.za.games.kiskac.KiskacInvalid
import com.za.games.kiskac.KiskacState
import com.za.games.kiskac.KiskacStatus
import com.za.games.platform.LocalZaHaptics
import com.za.games.platform.LocalZaSound
import com.za.games.platform.Sfx
import com.za.games.ui.common.GameTopBar
import com.za.games.ui.common.OverlayCard
import kotlinx.coroutines.delay
import java.util.Locale

private val TrLocale: Locale = Locale.forLanguageTag("tr")

private fun Char.upperTr(): String = toString().uppercase(TrLocale)

private fun String.upperTr(): String = uppercase(TrLocale)

private val KEY_ROWS = listOf("ertyuıopğü", "asdfghjklşi", "zcvbnmöç")

private val AccentPink = Color(0xFFF472B6)
private val OnFilledDark = Color(0xFF06121D)

@Composable
fun KiskacScreen(
    highScore: Long,
    onScore: (Long) -> Unit,
    onExit: () -> Unit,
    viewModel: KiskacViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val streak by viewModel.streak.collectAsStateWithLifecycle()
    val haptics = LocalZaHaptics.current
    val sound = LocalZaSound.current
    val latestOnScore by rememberUpdatedState(onScore)

    // Rekor = en uzun seri; platform max ile saklar.
    LaunchedEffect(streak) {
        if (streak > 0) latestOnScore(streak.toLong())
    }
    // Gün değişmiş olabilir; günlük tahtayı tazele.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshDaily()
        onPauseOrDispose { }
    }
    // Bitiş efekti yalnızca canlı bitişte.
    var celebrated by remember(state.answer) {
        mutableStateOf(state.status != KiskacStatus.RUNNING)
    }
    LaunchedEffect(state.status) {
        if (state.status != KiskacStatus.RUNNING && !celebrated) {
            celebrated = true
            sound?.play(if (state.status == KiskacStatus.WON) Sfx.BIG else Sfx.OVER)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    // Geçerli tahmin sesi: yalnızca canlı eklenen tahminler.
    var seenGuesses by remember(state.answer) { mutableIntStateOf(state.guesses.size) }
    LaunchedEffect(state.guesses.size) {
        if (state.guesses.size > seenGuesses) sound?.play(Sfx.DROP, volume = 0.5f)
        seenGuesses = state.guesses.size
    }
    // Geçersiz kelime uyarısı.
    var invalidMessage by remember { mutableStateOf<KiskacInvalid?>(null) }
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
    var showResult by remember(state.answer, state.status) { mutableStateOf(true) }
    BackHandler { onExit() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        GameTopBar(title = stringResource(R.string.game_kiskac), onExit = onExit) {
            Text(
                text = "🔥 $streak",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 14.dp),
            )
        }

        ModeChips(mode = mode, onSelect = viewModel::setMode)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            KiskacBoard(state = state, invalidMessage = invalidMessage)
            if (state.status != KiskacStatus.RUNNING && showResult) {
                ResultOverlay(
                    state = state,
                    mode = mode,
                    streak = streak,
                    onPlayFree = { viewModel.setMode(KiskacMode.FREE) },
                    onNewWord = viewModel::newFreeGame,
                    onDismiss = { showResult = false },
                    onExit = onExit,
                )
            }
        }

        KiskacKeyboard(
            possibleFirst = state.possibleFirstLetters(),
            typingFirstLetter = state.current.isEmpty(),
            onKey = viewModel::type,
            onEnter = viewModel::submit,
            onErase = viewModel::erase,
        )
    }
}

@Composable
private fun ModeChips(mode: KiskacMode, onSelect: (KiskacMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ModeChip(
            label = stringResource(R.string.mode_daily),
            selected = mode == KiskacMode.DAILY,
            modifier = Modifier.weight(1f),
        ) { onSelect(KiskacMode.DAILY) }
        ModeChip(
            label = stringResource(R.string.mode_free),
            selected = mode == KiskacMode.FREE,
            modifier = Modifier.weight(1f),
        ) { onSelect(KiskacMode.FREE) }
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

@Composable
private fun KiskacBoard(state: KiskacState, invalidMessage: KiskacInvalid?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 24.dp),
    ) {
        BoundCard(
            word = state.lowerBound,
            placeholder = "A",
            label = stringResource(R.string.kiskac_lower_label),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(KiskacState.WORD_LENGTH) { index ->
                InputTile(letter = state.current.getOrNull(index))
            }
        }
        Text(
            text = stringResource(
                R.string.kiskac_attempts_fmt,
                state.guesses.size,
                KiskacState.MAX_GUESSES,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        )

        BoundCard(
            word = state.upperBound,
            placeholder = "Z",
            label = stringResource(R.string.kiskac_upper_label),
        )

        Text(
            text = when (invalidMessage) {
                KiskacInvalid.ALREADY_TRIED -> stringResource(R.string.already_tried)
                KiskacInvalid.NOT_IN_LIST -> stringResource(R.string.not_in_list)
                null -> " "
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

/** Alfabetik sınır kartı: kelime yoksa uçtaki harf soluk gösterilir. */
@Composable
private fun BoundCard(word: String?, placeholder: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label.uppercase(TrLocale),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
        Spacer(Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (word != null) {
                AccentPink.copy(alpha = 0.22f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            },
        ) {
            Text(
                text = (word ?: placeholder).upperTr(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 6.sp,
                color = if (word != null) {
                    AccentPink
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun InputTile(letter: Char?) {
    val borderColor = if (letter != null) {
        Color.White.copy(alpha = 0.45f)
    } else {
        Color.White.copy(alpha = 0.14f)
    }
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (letter != null) {
            Text(
                text = letter.upperTr(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun KiskacKeyboard(
    possibleFirst: Set<Char>,
    typingFirstLetter: Boolean,
    onKey: (Char) -> Unit,
    onEnter: () -> Unit,
    onErase: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            KEY_ROWS[0].forEach { letter ->
                KeyButton(
                    label = letter.upperTr(),
                    faded = typingFirstLetter && letter !in possibleFirst,
                    modifier = Modifier.weight(1f),
                ) { onKey(letter) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            KEY_ROWS[1].forEach { letter ->
                KeyButton(
                    label = letter.upperTr(),
                    faded = typingFirstLetter && letter !in possibleFirst,
                    modifier = Modifier.weight(1f),
                ) { onKey(letter) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            KeyButton(
                label = stringResource(R.string.key_enter),
                faded = false,
                modifier = Modifier.weight(1.6f),
                accent = true,
                onClick = onEnter,
            )
            KEY_ROWS[2].forEach { letter ->
                KeyButton(
                    label = letter.upperTr(),
                    faded = typingFirstLetter && letter !in possibleFirst,
                    modifier = Modifier.weight(1f),
                ) { onKey(letter) }
            }
            KeyButton(
                label = "⌫",
                faded = false,
                modifier = Modifier.weight(1.6f),
                onClick = onErase,
            )
        }
    }
}

@Composable
private fun KeyButton(
    label: String,
    faded: Boolean,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (accent) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (faded) 0.35f else 1f)
        },
        modifier = modifier.height(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    accent -> MaterialTheme.colorScheme.primary
                    faded -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun ResultOverlay(
    state: KiskacState,
    mode: KiskacMode,
    streak: Int,
    onPlayFree: () -> Unit,
    onNewWord: () -> Unit,
    onDismiss: () -> Unit,
    onExit: () -> Unit,
) {
    OverlayCard {
        Text(
            text = stringResource(
                if (state.status == KiskacStatus.WON) R.string.congrats else R.string.lost_title,
            ),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        if (state.status == KiskacStatus.WON) {
            Text(
                text = stringResource(
                    R.string.guesses_fmt,
                    state.guesses.size,
                    KiskacState.MAX_GUESSES,
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Text(
                text = stringResource(R.string.answer_was, state.answer.upperTr()),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = "🔥 ${stringResource(R.string.streak_label)}: $streak",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(4.dp))
        if (mode == KiskacMode.DAILY) {
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
