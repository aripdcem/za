package com.za.games.ui.besharf

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.za.games.R
import com.za.games.besharf.BesHarfState
import com.za.games.besharf.BesHarfStatus
import com.za.games.besharf.LetterMark
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

private val CorrectColor = Color(0xFF4ADE80)
private val PresentColor = Color(0xFFFACC15)
private val AbsentColor = Color(0xFF313A4E)
private val OnFilledDark = Color(0xFF06121D)

@Composable
fun BesHarfScreen(
    highScore: Long,
    onScore: (Long) -> Unit,
    onExit: () -> Unit,
    viewModel: BesHarfViewModel = viewModel(),
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
    // Ekran her öne geldiğinde gün değişmiş olabilir; günlük tahtayı tazele.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshDaily()
        onPauseOrDispose { }
    }
    // Bitiş sesi: yalnızca canlı bitişte bir kez (geri girişte, geri
    // yüklemede veya mod geçişinde tekrar çalmasın).
    var celebrated by remember(state.answer) {
        mutableStateOf(state.status != BesHarfStatus.RUNNING)
    }
    LaunchedEffect(state.status) {
        if (state.status != BesHarfStatus.RUNNING && !celebrated) {
            celebrated = true
            sound?.play(if (state.status == BesHarfStatus.WON) Sfx.BIG else Sfx.OVER)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    // Geçerli tahmin sesi: yalnızca bu oyunda canlı eklenen tahminler için.
    var seenGuesses by remember(state.answer) { mutableIntStateOf(state.guesses.size) }
    LaunchedEffect(state.guesses.size) {
        if (state.guesses.size > seenGuesses) sound?.play(Sfx.DROP, volume = 0.5f)
        seenGuesses = state.guesses.size
    }
    // Geçersiz kelime uyarısı: sayaç yalnızca arttığında (girişte tekrar etmesin).
    // Eksik kelime ile listede olmayan kelime ayrı mesaj alır.
    var invalidMessage by remember { mutableStateOf<Int?>(null) }
    var seenInvalid by remember { mutableIntStateOf(state.invalidEvents) }
    LaunchedEffect(state.invalidEvents) {
        val previous = seenInvalid
        seenInvalid = state.invalidEvents
        if (state.invalidEvents > previous) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            invalidMessage = if (state.current.length < BesHarfState.WORD_LENGTH) {
                R.string.too_short
            } else {
                R.string.not_in_list
            }
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
        GameTopBar(title = stringResource(R.string.game_besharf), onExit = onExit) {
            Text(
                text = "🔥 $streak",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 14.dp),
            )
        }

        ModeChips(mode = mode, onSelect = viewModel::setMode)

        Text(
            text = stringResource(R.string.besharf_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GuessGrid(state)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = invalidMessage?.let { stringResource(it) } ?: " ",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (state.status != BesHarfStatus.RUNNING && showResult) {
                ResultOverlay(
                    state = state,
                    mode = mode,
                    streak = streak,
                    onPlayFree = { viewModel.setMode(BesHarfMode.FREE) },
                    onNewWord = viewModel::newFreeGame,
                    onDismiss = { showResult = false },
                    onExit = onExit,
                )
            }
        }

        BesHarfKeyboard(
            keyMarks = state.keyMarks(),
            onKey = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.type(it)
            },
            onEnter = viewModel::submit,
            onErase = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.erase()
            },
        )
    }
}

@Composable
private fun ModeChips(mode: BesHarfMode, onSelect: (BesHarfMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ModeChip(
            label = stringResource(R.string.mode_daily),
            selected = mode == BesHarfMode.DAILY,
            modifier = Modifier.weight(1f),
        ) { onSelect(BesHarfMode.DAILY) }
        ModeChip(
            label = stringResource(R.string.mode_free),
            selected = mode == BesHarfMode.FREE,
            modifier = Modifier.weight(1f),
        ) { onSelect(BesHarfMode.FREE) }
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
        modifier = modifier.height(48.dp),
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
private fun GuessGrid(state: BesHarfState) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(BesHarfState.MAX_GUESSES) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(BesHarfState.WORD_LENGTH) { col ->
                    when {
                        row < state.guesses.size -> Tile(
                            letter = state.guesses[row][col],
                            mark = state.marks[row][col],
                        )
                        row == state.guesses.size && state.status == BesHarfStatus.RUNNING ->
                            Tile(letter = state.current.getOrNull(col), mark = null, active = true)
                        else -> Tile(letter = null, mark = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun Tile(letter: Char?, mark: LetterMark?, active: Boolean = false) {
    val background = when (mark) {
        LetterMark.CORRECT -> CorrectColor
        LetterMark.PRESENT -> PresentColor
        LetterMark.ABSENT -> AbsentColor
        null -> Color.Transparent
    }
    val borderColor = when {
        mark != null -> Color.Transparent
        active && letter != null -> Color.White.copy(alpha = 0.45f)
        else -> Color.White.copy(alpha = 0.14f)
    }
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .border(2.dp, borderColor, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (letter != null) {
            Text(
                text = letter.upperTr(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = when (mark) {
                    LetterMark.CORRECT, LetterMark.PRESENT -> OnFilledDark
                    LetterMark.ABSENT -> Color.White.copy(alpha = 0.6f)
                    null -> MaterialTheme.colorScheme.onBackground
                },
            )
        }
    }
}

@Composable
private fun BesHarfKeyboard(
    keyMarks: Map<Char, LetterMark>,
    onKey: (Char) -> Unit,
    onEnter: () -> Unit,
    onErase: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Aralar dar tutulur: 29 harfli Türkçe klavyede her piksel tuş
        // genişliğine gider (en yoğun satır 11 tuş barındırıyor).
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            KEY_ROWS[0].forEach { letter ->
                KeyButton(
                    label = letter.upperTr(),
                    mark = keyMarks[letter],
                    modifier = Modifier.weight(1f),
                ) { onKey(letter) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            KEY_ROWS[1].forEach { letter ->
                KeyButton(
                    label = letter.upperTr(),
                    mark = keyMarks[letter],
                    modifier = Modifier.weight(1f),
                ) { onKey(letter) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            KeyButton(
                label = stringResource(R.string.key_enter),
                mark = null,
                modifier = Modifier.weight(1.6f),
                accent = true,
                onClick = onEnter,
            )
            KEY_ROWS[2].forEach { letter ->
                KeyButton(
                    label = letter.upperTr(),
                    mark = keyMarks[letter],
                    modifier = Modifier.weight(1f),
                ) { onKey(letter) }
            }
            KeyButton(
                label = "⌫",
                mark = null,
                modifier = Modifier.weight(1.6f),
                onClick = onErase,
            )
        }
    }
}

@Composable
private fun KeyButton(
    label: String,
    mark: LetterMark?,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    onClick: () -> Unit,
) {
    val background = when {
        accent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        mark == LetterMark.CORRECT -> CorrectColor
        mark == LetterMark.PRESENT -> PresentColor
        mark == LetterMark.ABSENT -> AbsentColor.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        accent -> MaterialTheme.colorScheme.primary
        mark == LetterMark.CORRECT || mark == LetterMark.PRESENT -> OnFilledDark
        mark == LetterMark.ABSENT -> Color.White.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = background,
        modifier = modifier.height(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
            )
        }
    }
}

@Composable
private fun ResultOverlay(
    state: BesHarfState,
    mode: BesHarfMode,
    streak: Int,
    onPlayFree: () -> Unit,
    onNewWord: () -> Unit,
    onDismiss: () -> Unit,
    onExit: () -> Unit,
) {
    OverlayCard {
        Text(
            text = stringResource(
                if (state.status == BesHarfStatus.WON) R.string.congrats else R.string.lost_title,
            ),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        if (state.status == BesHarfStatus.WON) {
            Text(
                text = stringResource(
                    R.string.guesses_fmt,
                    state.guesses.size,
                    BesHarfState.MAX_GUESSES,
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
        if (mode == BesHarfMode.DAILY) {
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
