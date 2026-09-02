package com.za.games.ui.dizgi

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.za.games.R
import com.za.games.dizgi.DizgiBoard
import com.za.games.dizgi.DizgiInvalid
import com.za.games.dizgi.DizgiLetters
import com.za.games.dizgi.DizgiMoveKind
import com.za.games.dizgi.DizgiState
import com.za.games.dizgi.DizgiStatus
import com.za.games.dizgi.Premium
import com.za.games.platform.LocalZaHaptics
import com.za.games.platform.LocalZaSound
import com.za.games.platform.Sfx
import com.za.games.ui.common.GameTopBar
import com.za.games.ui.common.OverlayCard
import com.za.games.ui.common.PadButton
import com.za.games.ui.common.ScoreCard
import com.za.games.ui.common.formatScore
import kotlinx.coroutines.delay
import java.util.Locale

private val TrLocale: Locale = Locale.forLanguageTag("tr")

private fun String.upperTr(): String = uppercase(TrLocale)

private val AccentOrange = Color(0xFFFB923C)
private val TileFace = Color(0xFFEADFC8)
private val TilePendingFace = Color(0xFFFFF3D6)
private val TileInk = Color(0xFF1F2937)
private val JokerInk = Color(0xFF7C3AED)
private val ActionFont = 15.sp

@Composable
fun DizgiScreen(
    highScore: Long,
    onScore: (Long) -> Unit,
    onExit: () -> Unit,
    viewModel: DizgiViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val matchId by viewModel.matchId.collectAsStateWithLifecycle()
    val haptics = LocalZaHaptics.current
    val sound = LocalZaSound.current
    val latestOnScore by rememberUpdatedState(onScore)

    // Hamle sesleri: yalnızca canlı hamlelerde (ekrana dönüşte çalmaz).
    var seenMove by remember(matchId) { mutableIntStateOf(state.moveCount) }
    LaunchedEffect(state.moveCount) {
        if (state.moveCount > seenMove) {
            when (state.lastMove?.kind) {
                DizgiMoveKind.PLACE ->
                    sound?.play(if (state.lastMove?.bingo == true) Sfx.BIG else Sfx.CLEAR)
                DizgiMoveKind.PASS, DizgiMoveKind.EXCHANGE -> sound?.play(Sfx.DROP)
                null -> {}
            }
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        seenMove = state.moveCount
    }
    // Bitiş: rekor (kazananın skoru) bir kez işlenir, ses bir kez çalar.
    var endHeard by remember(matchId) { mutableStateOf(state.status == DizgiStatus.FINISHED) }
    LaunchedEffect(state.status) {
        if (state.status == DizgiStatus.FINISHED && !endHeard) {
            endHeard = true
            val best = state.players.maxOf { it.score }
            if (best > 0) latestOnScore(best.toLong())
            sound?.play(Sfx.OVER)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    // Geçersiz hamle uyarısı. Hem motor olaylarını hem yerel uyarıları (ör.
    // torba yetersizken Değiş) tek bir jetonla yönetir: yeni bir uyarı her
    // zaman önceki uyarının bekleyen gizleme gecikmesini iptal eder, bu yüzden
    // iki bağımsız zamanlayıcı birbirinin mesajını erken silemez.
    var invalidMessage by remember { mutableStateOf<Pair<DizgiInvalid, List<String>>?>(null) }
    var seenInvalid by remember { mutableIntStateOf(state.invalidEvents) }
    var messageToken by remember { mutableIntStateOf(0) }
    LaunchedEffect(state.invalidEvents) {
        val previous = seenInvalid
        seenInvalid = state.invalidEvents
        if (state.invalidEvents > previous) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            invalidMessage = state.lastInvalid?.let { it to state.invalidWords }
            messageToken += 1
        }
    }
    LaunchedEffect(messageToken) {
        if (messageToken > 0) {
            delay(2200L)
            invalidMessage = null
        }
    }

    // Tur içi ekran durumu: her hamlede/maçta sıfırlanır.
    var selectedRack by remember(matchId, state.moveCount) { mutableStateOf<Int?>(null) }
    var exchangeMode by remember(matchId, state.moveCount) { mutableStateOf(false) }
    var exchangePicks by remember(matchId, state.moveCount) { mutableStateOf(setOf<Int>()) }
    var jokerCell by remember(matchId, state.moveCount) { mutableStateOf<Int?>(null) }
    var confirmNew by remember(matchId, phase) { mutableStateOf(false) }
    var confirmPass by remember(matchId, state.moveCount) { mutableStateOf(false) }
    var showResult by remember(matchId, state.status) { mutableStateOf(true) }

    BackHandler { onExit() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
        GameTopBar(title = stringResource(R.string.game_dizgi), onExit = onExit) {
            if (phase != DizgiPhase.SETUP && state.status == DizgiStatus.RUNNING) {
                TextButton(onClick = { confirmNew = true }) {
                    Text(stringResource(R.string.new_game))
                }
            }
        }

        when (phase) {
            DizgiPhase.SETUP -> SetupPane(onStart = viewModel::start)

            DizgiPhase.HANDOVER -> HandoverPane(
                state = state,
                onReady = viewModel::beginTurn,
            )

            DizgiPhase.PLAY -> PlayPane(
                state = state,
                highScore = highScore,
                selectedRack = selectedRack,
                exchangeMode = exchangeMode,
                exchangePicks = exchangePicks,
                invalidMessage = invalidMessage,
                showResult = showResult,
                onRackTap = { index ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (exchangeMode) {
                        exchangePicks =
                            if (index in exchangePicks) exchangePicks - index else exchangePicks + index
                    } else {
                        selectedRack = if (selectedRack == index) null else index
                    }
                },
                onCellTap = { cell ->
                    if (state.status != DizgiStatus.RUNNING || exchangeMode) return@PlayPane
                    when {
                        cell in state.pending -> {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.recall(cell)
                        }
                        selectedRack != null && cell !in state.board -> {
                            val index = selectedRack ?: return@PlayPane
                            val tile = state.players[state.current].rack.getOrNull(index)
                                ?: return@PlayPane
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (tile.isJoker) {
                                jokerCell = cell
                            } else {
                                viewModel.place(cell, index)
                                selectedRack = null
                            }
                        }
                    }
                },
                onRecallAll = viewModel::recallAll,
                onPass = { confirmPass = true },
                onExchange = {
                    if (state.bag.size < DizgiState.RACK_SIZE) {
                        invalidMessage = DizgiInvalid.EXCHANGE_UNAVAILABLE to emptyList()
                        messageToken += 1
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    } else {
                        viewModel.recallAll()
                        selectedRack = null
                        exchangeMode = true
                    }
                },
                onExchangeConfirm = {
                    if (exchangePicks.isEmpty()) {
                        // Seçim yokken çıkma; yönlendirme satırı zaten görünür.
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    } else {
                        viewModel.exchange(exchangePicks.toList())
                    }
                },
                onExchangeCancel = {
                    exchangeMode = false
                    exchangePicks = emptySet()
                },
                onSubmit = viewModel::submit,
                onNewMatch = viewModel::toSetup,
                onShowBoard = { showResult = false },
                onExit = onExit,
            )
        }
        }

        // Katmanlar: joker harfi, pas onayı, yeni oyun onayı.
        jokerCell?.let { cell ->
            FullOverlay {
                JokerPicker(
                    onPick = { letter ->
                        selectedRack?.let { viewModel.place(cell, it, letter) }
                        selectedRack = null
                        jokerCell = null
                    },
                    onDismiss = { jokerCell = null },
                )
            }
        }
        if (confirmPass) {
            FullOverlay {
                ConfirmCard(
                    title = stringResource(R.string.dizgi_pass_confirm),
                    detail = stringResource(R.string.dizgi_pass_detail),
                    confirmLabel = stringResource(R.string.dizgi_pass),
                    onConfirm = {
                        confirmPass = false
                        viewModel.pass()
                    },
                    onDismiss = { confirmPass = false },
                )
            }
        }
        if (confirmNew) {
            FullOverlay {
                ConfirmCard(
                    title = stringResource(R.string.dizgi_new_confirm),
                    detail = stringResource(R.string.dizgi_new_detail),
                    confirmLabel = stringResource(R.string.new_game),
                    onConfirm = {
                        confirmNew = false
                        viewModel.toSetup()
                    },
                    onDismiss = { confirmNew = false },
                )
            }
        }
    }
}

@Composable
private fun FullOverlay(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

// --- Kurulum ---

@Composable
private fun SetupPane(onStart: (Int) -> Unit) {
    var count by remember { mutableIntStateOf(2) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        OverlayCard {
            Text(
                text = stringResource(R.string.dizgi_players_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (n in DizgiState.MIN_PLAYERS..DizgiState.MAX_PLAYERS) {
                    Surface(
                        onClick = { count = n },
                        shape = RoundedCornerShape(12.dp),
                        color = if (count == n) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.size(52.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "$n",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = if (count == n) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }
            Text(
                text = stringResource(R.string.dizgi_players_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
            )
            Button(onClick = { onStart(count) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.dizgi_start))
            }
        }
    }
}

// --- El değişimi perdesi ---

@Composable
private fun HandoverPane(state: DizgiState, onReady: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        OverlayCard {
            Text(
                text = stringResource(
                    R.string.dizgi_turn_of,
                    stringResource(R.string.dizgi_player_n, state.current + 1),
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            LastMoveText(state)
            state.players.forEachIndexed { i, p ->
                Text(
                    text = "${stringResource(R.string.dizgi_player_n, i + 1)}: ${formatScore(p.score.toLong())}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (i == state.current) FontWeight.Bold else FontWeight.Normal,
                    color = if (i == state.current) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    },
                )
            }
            Text(
                text = stringResource(R.string.dizgi_handover),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
            )
            Button(onClick = onReady, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.dizgi_ready))
            }
        }
    }
}

@Composable
private fun LastMoveText(state: DizgiState) {
    val move = state.lastMove ?: return
    val name = stringResource(R.string.dizgi_player_n, move.player + 1)
    val text = when (move.kind) {
        DizgiMoveKind.PASS -> stringResource(R.string.dizgi_move_pass, name)
        DizgiMoveKind.EXCHANGE -> stringResource(R.string.dizgi_move_exchange, name)
        DizgiMoveKind.PLACE -> stringResource(
            R.string.dizgi_move_words,
            name,
            move.words.joinToString(", ") { it.upperTr() },
            move.gained,
        ) + if (move.bingo) "  ·  " + stringResource(R.string.dizgi_bingo) else ""
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        textAlign = TextAlign.Center,
    )
}

// --- Oyun ekranı ---

@Composable
private fun PlayPane(
    state: DizgiState,
    highScore: Long,
    selectedRack: Int?,
    exchangeMode: Boolean,
    exchangePicks: Set<Int>,
    invalidMessage: Pair<DizgiInvalid, List<String>>?,
    showResult: Boolean,
    onRackTap: (Int) -> Unit,
    onCellTap: (Int) -> Unit,
    onRecallAll: () -> Unit,
    onPass: () -> Unit,
    onExchange: () -> Unit,
    onExchangeConfirm: () -> Unit,
    onExchangeCancel: () -> Unit,
    onSubmit: () -> Unit,
    onNewMatch: () -> Unit,
    onShowBoard: () -> Unit,
    onExit: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.players.forEachIndexed { i, p ->
                ScoreCard(
                    label = stringResource(R.string.dizgi_player_n, i + 1),
                    value = formatScore(p.score.toLong()),
                    modifier = Modifier.weight(1f),
                    highlight = i == state.current && state.status == DizgiStatus.RUNNING,
                )
            }
        }

        Text(
            text = stringResource(R.string.dizgi_bag_count, state.bag.size) +
                "  ·  ${stringResource(R.string.high_score)}: ${formatScore(highScore)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            DizgiBoardCanvas(
                state = state,
                onCellTap = onCellTap,
                modifier = Modifier.aspectRatio(1f),
            )
            if (state.status == DizgiStatus.FINISHED && showResult) {
                FinishedOverlay(
                    state = state,
                    onNewMatch = onNewMatch,
                    onShowBoard = onShowBoard,
                    onExit = onExit,
                )
            }
        }

        val invalidText = invalidMessage?.let { (reason, words) ->
                when (reason) {
                    DizgiInvalid.EMPTY -> stringResource(R.string.dizgi_inv_empty)
                    DizgiInvalid.NOT_LINE -> stringResource(R.string.dizgi_inv_line)
                    DizgiInvalid.GAP -> stringResource(R.string.dizgi_inv_gap)
                    DizgiInvalid.NOT_CONNECTED -> stringResource(R.string.dizgi_inv_connect)
                    DizgiInvalid.CENTER_REQUIRED -> stringResource(R.string.dizgi_inv_center)
                    DizgiInvalid.SHORT_WORD -> stringResource(R.string.dizgi_inv_short)
                    DizgiInvalid.INVALID_WORD -> stringResource(
                        R.string.dizgi_inv_word,
                        words.joinToString(", ") { it.upperTr() },
                    )
                    DizgiInvalid.EXCHANGE_UNAVAILABLE -> stringResource(R.string.dizgi_inv_exchange)
                }
        }
        Text(
            text = invalidText ?: if (exchangeMode) {
                stringResource(R.string.dizgi_exchange_hint, exchangePicks.size)
            } else {
                " "
            },
            style = MaterialTheme.typography.labelLarge,
            color = if (invalidText != null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        RackRow(
            state = state,
            selectedRack = selectedRack,
            exchangeMode = exchangeMode,
            exchangePicks = exchangePicks,
            onRackTap = onRackTap,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when {
                state.status != DizgiStatus.RUNNING -> {
                    PadButton(
                        label = stringResource(R.string.new_game),
                        description = stringResource(R.string.new_game),
                        modifier = Modifier.weight(1f).height(52.dp),
                        fontSize = ActionFont,
                        accent = true,
                        onAction = onNewMatch,
                    )
                }
                exchangeMode -> {
                    PadButton(
                        label = stringResource(R.string.dizgi_cancel),
                        description = stringResource(R.string.dizgi_cancel),
                        modifier = Modifier.weight(1f).height(52.dp),
                        fontSize = ActionFont,
                        onAction = onExchangeCancel,
                    )
                    PadButton(
                        label = stringResource(R.string.dizgi_exchange_n, exchangePicks.size),
                        description = stringResource(R.string.dizgi_exchange),
                        modifier = Modifier.weight(1.4f).height(52.dp),
                        fontSize = ActionFont,
                        accent = true,
                        onAction = onExchangeConfirm,
                    )
                }
                else -> {
                    PadButton(
                        label = stringResource(R.string.dizgi_recall),
                        description = stringResource(R.string.dizgi_recall),
                        modifier = Modifier.weight(1f).height(52.dp),
                        fontSize = ActionFont,
                        onAction = onRecallAll,
                    )
                    PadButton(
                        label = stringResource(R.string.dizgi_pass),
                        description = stringResource(R.string.dizgi_pass),
                        modifier = Modifier.weight(0.8f).height(52.dp),
                        fontSize = ActionFont,
                        onAction = onPass,
                    )
                    PadButton(
                        label = stringResource(R.string.dizgi_exchange),
                        description = stringResource(R.string.dizgi_exchange),
                        modifier = Modifier.weight(0.8f).height(52.dp),
                        fontSize = ActionFont,
                        onAction = onExchange,
                    )
                    PadButton(
                        label = stringResource(R.string.dizgi_confirm_move),
                        description = stringResource(R.string.dizgi_confirm_move),
                        modifier = Modifier.weight(1.3f).height(52.dp),
                        fontSize = ActionFont,
                        accent = true,
                        onAction = onSubmit,
                    )
                }
            }
        }
    }
}

// --- Tahta ---

@Composable
private fun DizgiBoardCanvas(
    state: DizgiState,
    onCellTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val currentTap by rememberUpdatedState(onCellTap)

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                val cell = size.width / DizgiBoard.SIZE.toFloat()
                val col = (offset.x / cell).toInt().coerceIn(0, DizgiBoard.SIZE - 1)
                val row = (offset.y / cell).toInt().coerceIn(0, DizgiBoard.SIZE - 1)
                currentTap(row * DizgiBoard.SIZE + col)
            }
        },
    ) {
        val cell = size.width / DizgiBoard.SIZE
        val corner = CornerRadius(cell * 0.18f, cell * 0.18f)
        val pad = cell * 0.06f

        fun textAt(text: String, index: Int, color: Color, scale: Float) {
            val layout = textMeasurer.measure(
                AnnotatedString(text),
                style = TextStyle(
                    fontSize = with(this) { (cell * scale).toSp() },
                    fontWeight = FontWeight.Bold,
                    color = color,
                ),
            )
            val r = index / DizgiBoard.SIZE
            val c = index % DizgiBoard.SIZE
            drawText(
                layout,
                topLeft = Offset(
                    c * cell + (cell - layout.size.width) / 2f,
                    r * cell + (cell - layout.size.height) / 2f,
                ),
            )
        }

        drawRoundRect(Color(0xFF0F1628), cornerRadius = CornerRadius(14f, 14f))

        for (index in 0 until DizgiBoard.CELLS) {
            val r = index / DizgiBoard.SIZE
            val c = index % DizgiBoard.SIZE
            val topLeft = Offset(c * cell + pad, r * cell + pad)
            val boxSize = Size(cell - 2 * pad, cell - 2 * pad)
            val tile = state.pending[index] ?: state.board[index]

            if (tile != null) {
                val isPending = index in state.pending
                drawRoundRect(
                    color = if (isPending) TilePendingFace else TileFace,
                    topLeft = topLeft,
                    size = boxSize,
                    cornerRadius = corner,
                )
                if (isPending) {
                    drawRoundRect(
                        color = AccentOrange,
                        topLeft = topLeft,
                        size = boxSize,
                        cornerRadius = corner,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = cell * 0.08f),
                    )
                }
                textAt(
                    tile.letter.toString().upperTr(),
                    index,
                    if (tile.isJoker) JokerInk else TileInk,
                    scale = 0.58f,
                )
            } else {
                val fill = when (DizgiBoard.premium(index)) {
                    Premium.NONE -> Color.White.copy(alpha = 0.045f)
                    Premium.DL -> Color(0xFF3B82F6).copy(alpha = 0.30f)
                    Premium.TL -> Color(0xFF2563EB).copy(alpha = 0.60f)
                    Premium.DW -> AccentOrange.copy(alpha = 0.28f)
                    Premium.TW -> Color(0xFFF87171).copy(alpha = 0.55f)
                }
                drawRoundRect(
                    color = fill,
                    topLeft = topLeft,
                    size = boxSize,
                    cornerRadius = corner,
                )
                if (index == DizgiBoard.CENTER) {
                    textAt("★", index, Color.White.copy(alpha = 0.75f), scale = 0.55f)
                }
            }
        }
    }
}

// --- Taş rafı ---

@Composable
private fun RackRow(
    state: DizgiState,
    selectedRack: Int?,
    exchangeMode: Boolean,
    exchangePicks: Set<Int>,
    onRackTap: (Int) -> Unit,
) {
    val rack = state.players[state.current].rack
    val used = state.pendingRack.values.toSet()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        rack.forEachIndexed { index, tile ->
            val placed = index in used
            val selected = !exchangeMode && selectedRack == index
            val picked = exchangeMode && index in exchangePicks
            Surface(
                onClick = { if (!placed && state.status == DizgiStatus.RUNNING) onRackTap(index) },
                shape = RoundedCornerShape(10.dp),
                color = when {
                    placed -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    selected || picked -> TilePendingFace
                    exchangeMode -> TileFace.copy(alpha = 0.55f)
                    else -> TileFace
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .then(
                        when {
                            selected -> Modifier.border(
                                2.dp, AccentOrange, RoundedCornerShape(10.dp),
                            )
                            picked -> Modifier.border(
                                2.dp, Color(0xFFF87171), RoundedCornerShape(10.dp),
                            )
                            else -> Modifier
                        },
                    ),
            ) {
                if (!placed) {
                    Box(Modifier.fillMaxSize()) {
                        Text(
                            text = if (tile.isJoker) "★" else tile.letter.toString().upperTr(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = if (tile.isJoker) JokerInk else TileInk,
                            modifier = Modifier.align(Alignment.Center),
                        )
                        if (!tile.isJoker) {
                            Text(
                                text = "${tile.points}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TileInk.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 4.dp, bottom = 2.dp),
                            )
                        }
                    }
                }
            }
        }
        if (rack.isEmpty()) {
            Spacer(Modifier.height(56.dp))
        }
    }
}

// --- Katman kartları ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JokerPicker(onPick: (Char) -> Unit, onDismiss: () -> Unit) {
    OverlayCard {
        Text(
            text = stringResource(R.string.dizgi_joker_pick),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .heightIn(max = 260.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            DizgiLetters.letters.forEach { letter ->
                Surface(
                    onClick = { onPick(letter) },
                    shape = RoundedCornerShape(8.dp),
                    color = TileFace,
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = letter.toString().upperTr(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = TileInk,
                        )
                    }
                }
            }
        }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.dizgi_cancel))
        }
    }
}

@Composable
private fun ConfirmCard(
    title: String,
    detail: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    OverlayCard {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
            Text(confirmLabel)
        }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.dizgi_cancel))
        }
    }
}

@Composable
private fun FinishedOverlay(
    state: DizgiState,
    onNewMatch: () -> Unit,
    onShowBoard: () -> Unit,
    onExit: () -> Unit,
) {
    val ranking = state.players.withIndex().sortedByDescending { it.value.score }
    val best = ranking.first().value.score
    val tie = ranking.count { it.value.score == best } > 1
    OverlayCard {
        Text(
            text = stringResource(R.string.dizgi_finished),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = if (tie) {
                stringResource(R.string.dizgi_draw)
            } else {
                stringResource(
                    R.string.dizgi_winner,
                    stringResource(R.string.dizgi_player_n, ranking.first().index + 1),
                )
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        ranking.forEach { (index, player) ->
            Text(
                text = "${stringResource(R.string.dizgi_player_n, index + 1)}: ${formatScore(player.score.toLong())}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(4.dp))
        Button(onClick = onNewMatch, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.new_game))
        }
        TextButton(onClick = onShowBoard, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.show_board))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}
