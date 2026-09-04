package com.za.games.ui.tavla

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.za.games.R
import com.za.games.platform.LocalZaHaptics
import com.za.games.platform.LocalZaSound
import com.za.games.platform.Sfx
import com.za.games.tavla.Move
import com.za.games.tavla.Phase
import com.za.games.tavla.TavlaLogic
import com.za.games.tavla.TavlaMode
import com.za.games.tavla.TavlaState
import com.za.games.ui.common.GameTopBar
import com.za.games.ui.common.OverlayCard
import com.za.games.ui.common.PadButton
import kotlinx.coroutines.delay
import kotlin.math.min

// Tahta renkleri: koyu ceviz zemin, iki ton hane, fildişi ve abanoz pullar.
private val BoardColor = Color(0xFF2B1D17)
private val FrameColor = Color(0xFF17100C)
private val PointDark = Color(0xFF3E2A22)
private val PointLight = Color(0xFF7A5236)
private val BarColor = Color(0xFF1C110D)
private val TrayColor = Color(0xFF1C110D)
private val P0Fill = Color(0xFFF5F5F4)
private val P0Edge = Color(0xFFA8A29E)
private val P1Fill = Color(0xFF3B3532)
private val P1Edge = Color(0xFF8C837C)
private val Highlight = Color(0xFF22D3EE)
private val LastMoveColor = Color(0xFFFBBF24)
private val PrisonRing = Color(0xFFEF4444)
private val Ink = Color(0xFF1C1917)

private val TARGETS = listOf(1, 3, 5)

@Composable
fun TavlaScreen(
    highScore: Long,
    onScore: (Long) -> Unit,
    onExit: () -> Unit,
    viewModel: TavlaViewModel = viewModel(),
) {
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val setup by viewModel.setup.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val matchId by viewModel.matchId.collectAsStateWithLifecycle()
    val thinking by viewModel.thinking.collectAsStateWithLifecycle()
    val wins by viewModel.wins.collectAsStateWithLifecycle()
    val lastMove by viewModel.lastMove.collectAsStateWithLifecycle()
    val latestOnScore by rememberUpdatedState(onScore)
    val haptics = LocalZaHaptics.current
    val sound = LocalZaSound.current

    // Rekor = bilgisayara karşı kazanılan maç sayısı; platform en büyüğü saklar.
    LaunchedEffect(wins) {
        if (wins > 0) latestOnScore(wins.toLong())
    }

    // Ses ve titreşim: bir önceki durumla karşılaştırarak zar, hamle, vuruş ve bitişi yakalar.
    var previous by remember(matchId) { mutableStateOf<TavlaState?>(null) }
    LaunchedEffect(state) {
        val cur = state
        val prev = previous
        previous = cur
        if (cur == null || prev == null) return@LaunchedEffect
        val ended = cur.phase == Phase.GAME_OVER || cur.phase == Phase.MATCH_OVER
        val wasEnded = prev.phase == Phase.GAME_OVER || prev.phase == Phase.MATCH_OVER
        when {
            ended && !wasEnded -> {
                val humanWon = cur.winner != null && (!viewModel.matchVsComputer || cur.winner == 0)
                sound?.play(if (humanWon) Sfx.BIG else Sfx.OVER)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            cur.phase == Phase.MOVING && prev.phase != Phase.MOVING -> {
                sound?.play(Sfx.DROP, volume = 0.6f)
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            cur.points != prev.points || cur.bar != prev.bar || cur.off != prev.off -> {
                val hit = (0..1).any { cur.bar[it] > prev.bar[it] }
                val pinned = cur.points.count { it.pinned } > prev.points.count { it.pinned }
                if (hit || pinned) {
                    sound?.play(Sfx.STOMP, volume = 0.7f)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                } else {
                    sound?.play(Sfx.POP, volume = 0.5f)
                }
            }
        }
    }

    // İnsanın hamlesi yoksa kısa bir bekleyişle sıra geçer (bilgisayarınkini ViewModel yönetir).
    // Küp kullanılamıyorsa zar kendiliğinden atılır; küp varsa oyuncu önce katlayabilsin diye beklenir.
    LaunchedEffect(state) {
        val s = state ?: return@LaunchedEffect
        if (!viewModel.humanCanAct(s)) return@LaunchedEffect
        when {
            s.phase == Phase.MOVING && !s.canMove -> {
                delay(1100L)
                viewModel.endTurn()
            }
            s.phase == Phase.TO_ROLL && !s.canDouble -> {
                delay(900L)
                viewModel.roll()
            }
        }
    }

    BackHandler {
        if (phase == TavlaPhase.PLAYING) viewModel.toSetup() else onExit()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        GameTopBar(title = stringResource(R.string.game_tavla), onExit = onExit) {
            Text(
                text = "🏆 ${maxOf(wins.toLong(), highScore)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 14.dp),
            )
        }

        val current = state
        if (phase == TavlaPhase.SETUP || current == null) {
            SetupPanel(
                setup = setup,
                wins = wins,
                viewModel = viewModel,
                onStart = viewModel::start,
                onExit = onExit,
            )
        } else {
            PlayingPanel(
                state = current,
                matchId = matchId,
                thinking = thinking,
                lastMove = lastMove,
                wins = wins,
                viewModel = viewModel,
                onExit = onExit,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Kurulum
// ---------------------------------------------------------------------------

@Composable
private fun SetupPanel(
    setup: TavlaSetup,
    wins: Int,
    viewModel: TavlaViewModel,
    onStart: () -> Unit,
    onExit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.game_tavla_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TavlaMode.entries.forEach { mode ->
                OptionChip(
                    label = stringResource(modeLabel(mode)),
                    selected = setup.mode == mode,
                    modifier = Modifier.weight(1f),
                ) { viewModel.setMode(mode) }
            }
        }
        Text(
            text = stringResource(modeDescription(setup.mode)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        SectionLabel(stringResource(R.string.tavla_opponent_label))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionChip(
                label = stringResource(R.string.tavla_opponent_computer),
                selected = setup.vsComputer,
                modifier = Modifier.weight(1f),
            ) { viewModel.setVsComputer(true) }
            OptionChip(
                label = stringResource(R.string.tavla_opponent_human),
                selected = !setup.vsComputer,
                modifier = Modifier.weight(1f),
            ) { viewModel.setVsComputer(false) }
        }

        SectionLabel(stringResource(R.string.tavla_target_label))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TARGETS.forEach { target ->
                OptionChip(
                    label = stringResource(R.string.tavla_target_fmt, target),
                    selected = setup.target == target,
                    modifier = Modifier.weight(1f),
                ) { viewModel.setTarget(target) }
            }
        }

        if (setup.mode.pinning) {
            SwitchRow(
                title = stringResource(R.string.tavla_nopin),
                description = null,
                checked = setup.noPinInOpponentHome,
                onToggle = viewModel::setNoPin,
            )
        }
        SwitchRow(
            title = stringResource(R.string.tavla_cube),
            description = stringResource(R.string.tavla_cube_desc),
            checked = setup.cube,
            onToggle = viewModel::setCube,
        )

        if (setup.vsComputer) {
            Text(
                text = "${stringResource(R.string.tavla_wins_label)}: $wins",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        Spacer(Modifier.height(4.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(stringResource(R.string.tavla_start), fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
        Text(
            text = stringResource(R.string.tavla_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun modeLabel(mode: TavlaMode): Int = when (mode) {
    TavlaMode.KLASIK -> R.string.tavla_mode_klasik
    TavlaMode.TAPA -> R.string.tavla_mode_tapa
    TavlaMode.HAPIS -> R.string.tavla_mode_hapis
}

private fun modeDescription(mode: TavlaMode): Int = when (mode) {
    TavlaMode.KLASIK -> R.string.tavla_mode_desc_klasik
    TavlaMode.TAPA -> R.string.tavla_mode_desc_tapa
    TavlaMode.HAPIS -> R.string.tavla_mode_desc_hapis
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    )
}

@Composable
private fun SwitchRow(
    title: String,
    description: String?,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@Composable
private fun OptionChip(
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
        modifier = modifier.height(46.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Oyun
// ---------------------------------------------------------------------------

@Composable
private fun playerName(player: Int, vsComputer: Boolean): String = when {
    !vsComputer -> stringResource(R.string.tavla_player_fmt, player + 1)
    player == 0 -> stringResource(R.string.tavla_you)
    else -> stringResource(R.string.tavla_computer)
}

@Composable
private fun ColumnScope.PlayingPanel(
    state: TavlaState,
    matchId: Int,
    thinking: Boolean,
    lastMove: Move?,
    wins: Int,
    viewModel: TavlaViewModel,
    onExit: () -> Unit,
) {
    val vsComputer = viewModel.matchVsComputer
    val humanActs = viewModel.humanCanAct(state)
    val legal = remember(state) { state.legalMoves() }
    val sources = remember(legal) { legal.map { it.from }.toSet() }
    val haptics = LocalZaHaptics.current
    var selectedRaw by remember(matchId) { mutableStateOf<Int?>(null) }
    // Seçim ancak hâlâ oynanabilir bir kaynaksa geçerlidir; tek kaynak varsa kendiliğinden seçilir.
    val selected: Int? = when {
        !humanActs || state.phase != Phase.MOVING -> null
        selectedRaw != null && selectedRaw in sources -> selectedRaw
        sources.size == 1 -> sources.first()
        else -> null
    }
    val destinations = remember(legal, selected) {
        if (selected == null) emptySet() else legal.filter { it.from == selected }.map { it.to }.toSet()
    }
    val names = listOf(playerName(0, vsComputer), playerName(1, vsComputer))
    val canPlay = humanActs && state.phase == Phase.MOVING

    fun destinationsOf(from: Int): Set<Int> = legal.filter { it.from == from }.map { it.to }.toSet()

    fun play(from: Int, to: Int) {
        selectedRaw = null
        viewModel.move(from, to)
    }

    /**
     * Dokunma: seçili pulun hedefine dokununca oynar; pula dokununca tek hedefi varsa
     * hemen oynar, yoksa seçer (ikinci dokunuş seçimi kaldırır); boş bir hedefe dokununca
     * oraya yalnız tek bir pul gidebiliyorsa o pul oynanır.
     */
    fun tap(target: Int) {
        // Zar atılmadan tahtaya dokunmak zar atar (düğmeyi aramaya gerek kalmasın).
        if (humanActs && state.phase == Phase.TO_ROLL) {
            viewModel.roll()
            return
        }
        if (!canPlay) return
        when {
            selected != null && target in destinations -> play(selected, target)
            target in sources -> {
                val targets = destinationsOf(target)
                when {
                    targets.size == 1 -> play(target, targets.first())
                    target == selected -> selectedRaw = null
                    else -> {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedRaw = target
                    }
                }
            }
            else -> {
                val froms = legal.filter { it.to == target }.map { it.from }.distinct()
                if (froms.size == 1) play(froms.first(), target) else selectedRaw = null
            }
        }
    }

    /** Sürükleme başlangıcı: kaynak oynanabilir bir pulsa seçer ve sürüklemeye izin verir. */
    fun dragStart(source: Int): Boolean {
        if (!canPlay || source !in sources) return false
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        selectedRaw = source
        return true
    }

    /** Bırakma: hedef yasalsa oynar; değilse seçim kalır, pul yerine döner. */
    fun drop(source: Int, target: Int) {
        if (!canPlay) return
        if (target in destinationsOf(source)) play(source, target)
    }

    ScoreRow(state = state, names = names)
    StatusLine(state = state, names = names, thinking = thinking, vsComputer = vsComputer)

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        TavlaBoard(
            state = state,
            selected = selected,
            sources = if (selected == null) sources else emptySet(),
            destinations = destinations,
            lastMove = lastMove,
            turnName = names[state.turn],
            onTap = ::tap,
            onDragStart = ::dragStart,
            onDrop = ::drop,
            modifier = Modifier.fillMaxSize(),
        )
        if (state.phase == Phase.DOUBLE_OFFERED && humanActs) {
            DoubleOfferCard(
                offerer = names[state.turn],
                cube = state.cubeValue,
                onAccept = viewModel::acceptDouble,
                onDecline = viewModel::declineDouble,
            )
        }
        if (state.phase == Phase.GAME_OVER) {
            GameOverCard(
                state = state,
                names = names,
                onNext = viewModel::nextGame,
                onSetup = viewModel::toSetup,
            )
        }
        if (state.phase == Phase.MATCH_OVER) {
            MatchOverCard(
                state = state,
                names = names,
                wins = if (vsComputer) wins else null,
                onNewMatch = viewModel::start,
                onSetup = viewModel::toSetup,
                onExit = onExit,
            )
        }
    }

    ActionRow(state = state, humanActs = humanActs, viewModel = viewModel)
}

@Composable
private fun ScoreRow(state: TavlaState, names: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerCard(
            name = names[0],
            score = state.scores[0],
            pips = state.pips(0),
            off = state.off[0],
            active = state.turn == 0 && state.phase != Phase.GAME_OVER && state.phase != Phase.MATCH_OVER,
            light = true,
            modifier = Modifier.weight(1.2f),
        )
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.tavla_game_fmt, state.game, state.rules.target),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
            Text(
                text = "${state.scores[0]} – ${state.scores[1]}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            if (state.rules.cube) {
                Text(
                    text = stringResource(R.string.tavla_cube_fmt, state.cubeValue),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        PlayerCard(
            name = names[1],
            score = state.scores[1],
            pips = state.pips(1),
            off = state.off[1],
            active = state.turn == 1 && state.phase != Phase.GAME_OVER && state.phase != Phase.MATCH_OVER,
            light = false,
            modifier = Modifier.weight(1.2f),
        )
    }
}

@Composable
private fun PlayerCard(
    name: String,
    score: Int,
    pips: Int,
    off: Int,
    active: Boolean,
    light: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (active) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .width(12.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (light) P0Fill else P1Fill),
                )
                Text(
                    text = name.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    color = if (active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Text(
                text = stringResource(R.string.tavla_pips_fmt, pips, off),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                maxLines = 1,
            )
        }
    }
}

/** Açılış turu mu: tahta başlangıç dizilişinde ve ilk zar henüz oynanmamış. */
private fun TavlaState.isOpeningTurn(): Boolean =
    phase == Phase.MOVING && played.isEmpty() && openingDice.size == 2 &&
        off == listOf(0, 0) && bar == listOf(0, 0) && points == TavlaLogic.initialPoints(rules.mode)

@Composable
private fun StatusLine(state: TavlaState, names: List<String>, thinking: Boolean, vsComputer: Boolean) {
    val aiTurn = vsComputer && when (state.phase) {
        Phase.DOUBLE_OFFERED -> state.responder == TavlaViewModel.AI_PLAYER
        Phase.TO_ROLL, Phase.MOVING -> state.turn == TavlaViewModel.AI_PLAYER
        else -> false
    }
    val text = when {
        state.phase == Phase.MOVING && !state.canMove -> stringResource(R.string.tavla_no_moves)
        state.isOpeningTurn() -> stringResource(
            R.string.tavla_opening_fmt,
            state.openingDice[0],
            state.openingDice[1],
            names[state.turn],
        )
        aiTurn && thinking -> stringResource(R.string.tavla_thinking)
        state.phase == Phase.TO_ROLL || state.phase == Phase.MOVING || state.phase == Phase.DOUBLE_OFFERED ->
            stringResource(R.string.tavla_turn_fmt, names[state.turn])
        else -> " "
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
    )
}

@Composable
private fun ActionRow(state: TavlaState, humanActs: Boolean, viewModel: TavlaViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            humanActs && state.phase == Phase.TO_ROLL -> {
                PadButton(
                    label = stringResource(R.string.tavla_roll),
                    description = stringResource(R.string.tavla_roll),
                    accent = true,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    onAction = viewModel::roll,
                )
                if (state.canDouble) {
                    PadButton(
                        label = stringResource(R.string.tavla_double),
                        description = stringResource(R.string.tavla_double),
                        fontSize = 16.sp,
                        modifier = Modifier.weight(0.6f).fillMaxSize(),
                        onAction = viewModel::offerDouble,
                    )
                }
            }
            humanActs && state.phase == Phase.MOVING && state.played.isNotEmpty() -> {
                PadButton(
                    label = stringResource(R.string.undo),
                    description = stringResource(R.string.undo),
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    onAction = viewModel::undo,
                )
            }
            else -> {
                Text(
                    text = if (state.phase == Phase.MOVING && humanActs) stringResource(R.string.tavla_hint) else " ",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Kartlar
// ---------------------------------------------------------------------------

@Composable
private fun DoubleOfferCard(offerer: String, cube: Int, onAccept: () -> Unit, onDecline: () -> Unit) {
    OverlayCard {
        Text(
            text = stringResource(R.string.tavla_double_offer_fmt, offerer, cube, cube * 2),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onAccept, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.tavla_accept))
        }
        OutlinedButton(onClick = onDecline, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.tavla_decline))
        }
    }
}

@Composable
private fun GameOverCard(state: TavlaState, names: List<String>, onNext: () -> Unit, onSetup: () -> Unit) {
    OverlayCard {
        val winner = state.winner
        Text(
            text = if (winner == null) {
                stringResource(R.string.tavla_draw)
            } else {
                stringResource(R.string.tavla_won_fmt, names[winner])
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        val detail = when {
            winner == null -> null
            state.resigned -> stringResource(R.string.tavla_resigned)
            state.deadlock -> stringResource(R.string.tavla_deadlock)
            state.mars -> stringResource(R.string.tavla_mars)
            else -> null
        }
        if (detail != null) {
            Text(
                text = detail,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = "${state.scores[0]} – ${state.scores[1]}",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.tavla_next_game))
        }
        TextButton(onClick = onSetup, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.tavla_to_setup))
        }
    }
}

@Composable
private fun MatchOverCard(
    state: TavlaState,
    names: List<String>,
    wins: Int?,
    onNewMatch: () -> Unit,
    onSetup: () -> Unit,
    onExit: () -> Unit,
) {
    OverlayCard {
        val winner = state.winner ?: 0
        Text(
            text = stringResource(R.string.tavla_match_won_fmt, names[winner], state.scores[0], state.scores[1]),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        if (state.mars && !state.resigned) {
            Text(
                text = stringResource(R.string.tavla_mars),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        if (wins != null) {
            Text(
                text = "${stringResource(R.string.tavla_wins_label)}: $wins",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(4.dp))
        Button(onClick = onNewMatch, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.tavla_new_match))
        }
        OutlinedButton(onClick = onSetup, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.tavla_to_setup))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

// ---------------------------------------------------------------------------
// Tahta
// ---------------------------------------------------------------------------

/**
 * Dikey tahta geometrisi: üstte 12, altta 12 hane; ortada dikey bar, sağda
 * toplama sütunu. Oyuncu 0 (alt) sağ üstten (24) başlar, saat yönünün
 * tersine sağ alta (1) gelir; evi sağ alt çeyrektir. Oyuncu 1 tam tersi.
 */
private class BoardGeometry(val width: Float, val height: Float) {
    val trayW = width * 0.075f
    val barW = width * 0.07f
    val pointW = (width - trayW - barW) / 12f
    val barX = pointW * 6f
    val trayX = width - trayW
    val halfH = height / 2f
    val triH = height * 0.42f
    val radius = min(pointW * 0.47f, height / 24f)

    fun column(point: Int): Int = if (point < 12) 11 - point else point - 12

    fun isTop(point: Int): Boolean = point >= 12

    /** Hanenin sol kenar x'i. */
    fun left(point: Int): Float {
        val col = column(point)
        return if (col < 6) col * pointW else barX + barW + (col - 6) * pointW
    }

    fun centerX(point: Int): Float = left(point) + pointW / 2f

    /** Yığındaki k. pulun merkezi (0 = kenardaki, tabandaki pul). */
    fun stackY(point: Int, k: Int, total: Int): Float {
        val step = stackStep(total)
        val offset = radius + k * step
        return if (isTop(point)) offset else height - offset
    }

    fun stackStep(total: Int): Float {
        if (total <= 1) return 0f
        val span = triH + radius - 2f * radius
        return min(2f * radius, span / (total - 1))
    }

    /** Dokunulan yer: [Move.BAR], [Move.OFF], hane dizini ya da boşluk için null. */
    fun hit(x: Float, y: Float): Int? {
        if (x < 0f || x > width || y < 0f || y > height) return null
        if (x >= trayX) return Move.OFF
        if (x >= barX && x < barX + barW) return Move.BAR
        val col = if (x < barX) (x / pointW).toInt() else 6 + ((x - barX - barW) / pointW).toInt()
        val c = col.coerceIn(0, 11)
        return if (y < halfH) 12 + c else 11 - c
    }
}

@Composable
private fun TavlaBoard(
    state: TavlaState,
    selected: Int?,
    sources: Set<Int>,
    destinations: Set<Int>,
    lastMove: Move?,
    turnName: String,
    onTap: (Int) -> Unit,
    onDragStart: (Int) -> Boolean,
    onDrop: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentTap by rememberUpdatedState(onTap)
    val currentDragStart by rememberUpdatedState(onDragStart)
    val currentDrop by rememberUpdatedState(onDrop)
    val textMeasurer = rememberTextMeasurer()
    val diceText = state.dice.joinToString("-")
    val desc = stringResource(R.string.tavla_board_desc, turnName, diceText)
    val trayDesc = stringResource(R.string.tavla_off_tray)
    // Sürüklenen pul: kaynağı ve parmağın konumu (yalnız çizim için).
    var dragFrom by remember { mutableStateOf<Int?>(null) }
    var dragPos by remember { mutableStateOf<Offset?>(null) }
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .semantics { contentDescription = "$desc. $trayDesc" }
            .pointerInput(Unit) {
                // Tek algılayıcı: parmak kalkınca dokunma, eşik aşılınca sürükleme.
                // Tüketilmiş olaylar da izlenir ki üstteki hiçbir katman dokunuşu yutmasın.
                val slop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val g = BoardGeometry(size.width.toFloat(), size.height.toFloat())
                    val origin = g.hit(down.position.x, down.position.y)
                    var dragging = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.changedToUpIgnoreConsumed()) {
                            change.consume()
                            if (dragging) {
                                val from = dragFrom
                                val target = g.hit(change.position.x, change.position.y)
                                dragFrom = null
                                dragPos = null
                                if (from != null && target != null) currentDrop(from, target)
                            } else if (origin != null) {
                                currentTap(origin)
                            }
                            break
                        }
                        if (!change.pressed) {
                            dragFrom = null
                            dragPos = null
                            break
                        }
                        if (!dragging && origin != null && origin != Move.OFF &&
                            (change.position - down.position).getDistance() > slop &&
                            currentDragStart(origin)
                        ) {
                            dragging = true
                            dragFrom = origin
                        }
                        if (dragging) {
                            dragPos = change.position
                            change.consume()
                        }
                    }
                }
            },
    ) {
        val g = BoardGeometry(size.width, size.height)
        val lifted = if (dragPos != null) dragFrom else null
        drawBoardBase(g)
        drawPoints(g, state, selected, sources, destinations, lastMove, lifted, textMeasurer)
        drawBar(g, state, selected, lifted, textMeasurer)
        drawTrays(g, state, destinations, textMeasurer)
        drawDice(g, state)
        if (state.rules.cube) drawCube(g, state, textMeasurer)
        val pos = dragPos
        if (lifted != null && pos != null) {
            // Havadaki pul parmağın biraz üstünde durur ki görünsün.
            drawChecker(pos.x, pos.y - g.radius * 1.6f, g.radius * 1.15f, state.turn)
        }
    }
}

private fun DrawScope.drawBoardBase(g: BoardGeometry) {
    drawRect(FrameColor)
    drawRect(BoardColor, topLeft = Offset(0f, 0f), size = Size(g.trayX, g.height))
    drawRect(BarColor, topLeft = Offset(g.barX, 0f), size = Size(g.barW, g.height))
    drawRect(TrayColor, topLeft = Offset(g.trayX, 0f), size = Size(g.trayW, g.height))
    // Orta çizgi: tepsi sütununu ikiye böler (üst = oyuncu 1, alt = oyuncu 0).
    drawLine(
        color = Color.White.copy(alpha = 0.12f),
        start = Offset(g.trayX, g.halfH),
        end = Offset(g.width, g.halfH),
        strokeWidth = 2f,
    )
}

private fun trianglePath(g: BoardGeometry, point: Int): Path {
    val x0 = g.left(point)
    val x1 = x0 + g.pointW
    val xm = x0 + g.pointW / 2f
    return Path().apply {
        if (g.isTop(point)) {
            moveTo(x0, 0f)
            lineTo(x1, 0f)
            lineTo(xm, g.triH)
        } else {
            moveTo(x0, g.height)
            lineTo(x1, g.height)
            lineTo(xm, g.height - g.triH)
        }
        close()
    }
}

private fun DrawScope.drawPoints(
    g: BoardGeometry,
    state: TavlaState,
    selected: Int?,
    sources: Set<Int>,
    destinations: Set<Int>,
    lastMove: Move?,
    lifted: Int?,
    textMeasurer: TextMeasurer,
) {
    for (i in 0 until TavlaLogic.POINTS) {
        val col = g.column(i)
        val dark = (col + (if (g.isTop(i)) 1 else 0)) % 2 == 0
        val path = trianglePath(g, i)
        drawPath(path, if (dark) PointDark else PointLight)
        if (i in destinations) drawPath(path, Highlight.copy(alpha = 0.38f))
        if (lastMove != null && lastMove.to == i) drawPath(path, LastMoveColor.copy(alpha = 0.16f))
    }
    for (i in 0 until TavlaLogic.POINTS) {
        val point = state.points[i]
        val count = if (i == lifted) point.count - 1 else point.count
        val total = count + (if (point.pinned) 1 else 0)
        if (total == 0) {
            if (i in destinations) drawLandingMark(g, i, 0)
            continue
        }
        val cx = g.centerX(i)
        var k = 0
        if (point.pinned) {
            // Mahkûm: yığının tabanında rakip pulu, kırmızı halkayla.
            val cy = g.stackY(i, 0, total)
            drawChecker(cx, cy, g.radius, 1 - point.owner)
            drawCircle(PrisonRing, radius = g.radius * 0.72f, center = Offset(cx, cy), style = Stroke(width = g.radius * 0.16f))
            k = 1
        }
        repeat(count) { n ->
            drawChecker(cx, g.stackY(i, k + n, total), g.radius, point.owner)
        }
        val topK = total - 1
        val topY = g.stackY(i, topK, total)
        if (count > 5) {
            drawCenteredText(textMeasurer, count.toString(), cx, topY, 11.sp, if (point.owner == 0) Ink else P0Fill)
        }
        when {
            i == selected && i != lifted -> drawSelectionRing(cx, topY, g.radius)
            i in sources -> drawSourceDot(cx, topY, g.radius)
            lastMove != null && lastMove.to == i -> drawCircle(LastMoveColor.copy(alpha = 0.85f), radius = g.radius * 1.02f, center = Offset(cx, topY), style = Stroke(width = g.radius * 0.14f))
        }
        if (i in destinations) drawLandingMark(g, i, total)
    }
}

/** Hedef hanede bir sonraki pulun ineceği yere içi boş halka. */
private fun DrawScope.drawLandingMark(g: BoardGeometry, point: Int, total: Int) {
    val cx = g.centerX(point)
    val cy = g.stackY(point, total, total + 1)
    drawCircle(Highlight.copy(alpha = 0.35f), radius = g.radius * 0.8f, center = Offset(cx, cy))
    drawCircle(Highlight, radius = g.radius * 0.8f, center = Offset(cx, cy), style = Stroke(width = g.radius * 0.2f))
}

/** Seçili pul: parlayan halka. */
private fun DrawScope.drawSelectionRing(cx: Float, cy: Float, r: Float) {
    drawCircle(Highlight.copy(alpha = 0.35f), radius = r * 1.45f, center = Offset(cx, cy))
    drawCircle(Highlight, radius = r * 1.08f, center = Offset(cx, cy), style = Stroke(width = r * 0.26f))
}

/** Oynanabilir pul: üstünde küçük camgöbeği nokta (açık pulda da görünür). */
private fun DrawScope.drawSourceDot(cx: Float, cy: Float, r: Float) {
    drawCircle(Ink.copy(alpha = 0.5f), radius = r * 0.34f, center = Offset(cx, cy))
    drawCircle(Highlight, radius = r * 0.26f, center = Offset(cx, cy))
}

private fun DrawScope.drawChecker(cx: Float, cy: Float, r: Float, owner: Int) {
    val fill = if (owner == 0) P0Fill else P1Fill
    val edge = if (owner == 0) P0Edge else P1Edge
    drawCircle(Color.Black.copy(alpha = 0.35f), radius = r, center = Offset(cx + r * 0.08f, cy + r * 0.1f))
    drawCircle(fill, radius = r, center = Offset(cx, cy))
    drawCircle(edge, radius = r, center = Offset(cx, cy), style = Stroke(width = r * 0.14f))
    drawCircle(edge.copy(alpha = 0.6f), radius = r * 0.55f, center = Offset(cx, cy), style = Stroke(width = r * 0.08f))
}

private fun DrawScope.drawBar(g: BoardGeometry, state: TavlaState, selected: Int?, lifted: Int?, textMeasurer: TextMeasurer) {
    val cx = g.barX + g.barW / 2f
    val r = min(g.radius, g.barW * 0.45f)
    // Oyuncu 0'ın kırık pulları üst yarıda (rakip evine girer), oyuncu 1'inkiler alt yarıda.
    for (player in 0..1) {
        val n = if (lifted == Move.BAR && player == state.turn) state.bar[player] - 1 else state.bar[player]
        if (n <= 0) continue
        val baseY = if (player == 0) g.halfH - r * 1.4f else g.halfH + r * 1.4f
        val dir = if (player == 0) -1f else 1f
        val shown = min(n, 4)
        for (k in 0 until shown) {
            drawChecker(cx, baseY + dir * k * r * 1.1f, r, player)
        }
        val topY = baseY + dir * (shown - 1) * r * 1.1f
        if (n > 1) drawCenteredText(textMeasurer, n.toString(), cx, topY, 11.sp, if (player == 0) Ink else P0Fill)
        if (selected == Move.BAR && state.turn == player && lifted != Move.BAR) {
            drawSelectionRing(cx, topY, r)
        }
    }
}

private fun DrawScope.drawTrays(g: BoardGeometry, state: TavlaState, destinations: Set<Int>, textMeasurer: TextMeasurer) {
    val slotH = (g.halfH - g.radius * 2f) / TavlaLogic.CHECKERS
    val inset = g.trayW * 0.18f
    val w = g.trayW - inset * 2f
    for (player in 0..1) {
        val n = state.off[player]
        val fill = if (player == 0) P0Fill else P1Fill
        val edge = if (player == 0) P0Edge else P1Edge
        for (k in 0 until n) {
            // Oyuncu 0 alttan yukarı, oyuncu 1 üstten aşağı dizilir.
            val y = if (player == 0) g.height - g.radius - (k + 1) * slotH else g.radius + k * slotH
            drawRoundRect(fill, topLeft = Offset(g.trayX + inset, y + slotH * 0.12f), size = Size(w, slotH * 0.76f), cornerRadius = CornerRadius(3f, 3f))
            drawRoundRect(edge, topLeft = Offset(g.trayX + inset, y + slotH * 0.12f), size = Size(w, slotH * 0.76f), cornerRadius = CornerRadius(3f, 3f), style = Stroke(width = 1.5f))
        }
        if (n > 0) {
            val ty = if (player == 0) g.height - g.radius * 0.7f else g.radius * 0.7f
            drawCenteredText(textMeasurer, n.toString(), g.trayX + g.trayW / 2f, ty, 10.sp, Color.White.copy(alpha = 0.8f))
        }
    }
    if (Move.OFF in destinations) {
        val top = if (state.turn == 0) g.halfH else 0f
        drawRoundRect(
            Highlight.copy(alpha = 0.28f),
            topLeft = Offset(g.trayX + 2f, top + 2f),
            size = Size(g.trayW - 4f, g.halfH - 4f),
            cornerRadius = CornerRadius(8f, 8f),
        )
        drawRoundRect(
            Highlight,
            topLeft = Offset(g.trayX + 2f, top + 2f),
            size = Size(g.trayW - 4f, g.halfH - 4f),
            cornerRadius = CornerRadius(8f, 8f),
            style = Stroke(width = 3f),
        )
    }
}

private val PIP_LAYOUT: Map<Int, List<Pair<Float, Float>>> = mapOf(
    1 to listOf(0f to 0f),
    2 to listOf(-1f to -1f, 1f to 1f),
    3 to listOf(-1f to -1f, 0f to 0f, 1f to 1f),
    4 to listOf(-1f to -1f, 1f to -1f, -1f to 1f, 1f to 1f),
    5 to listOf(-1f to -1f, 1f to -1f, 0f to 0f, -1f to 1f, 1f to 1f),
    6 to listOf(-1f to -1f, -1f to 0f, -1f to 1f, 1f to -1f, 1f to 0f, 1f to 1f),
)

private fun DrawScope.drawDice(g: BoardGeometry, state: TavlaState) {
    if (state.dice.isEmpty() || state.phase != Phase.MOVING) return
    val doubles = state.dice.size == 2 && state.dice[0] == state.dice[1]
    val faces = if (doubles) List(4) { state.dice[0] } else state.dice
    val usedCount = faces.size - state.remaining.size
    val size = min(g.pointW * 1.05f, g.halfH * 0.32f)
    val gap = size * 0.22f
    val totalW = faces.size * size + (faces.size - 1) * gap
    // Sıradaki oyuncu kendi sağındaki çeyrekte atar: oyuncu 0 sağ, oyuncu 1 sol çeyrek.
    val quadCenter = if (state.turn == 0) g.barX + g.barW + g.pointW * 3f else g.pointW * 3f
    var x = quadCenter - totalW / 2f
    val y = g.halfH - size / 2f
    val remainingPool = state.remaining.toMutableList()
    faces.forEachIndexed { index, face ->
        val used = if (doubles) index < usedCount else !remainingPool.remove(face)
        drawDie(x, y, size, face, state.turn, used)
        x += size + gap
    }
}

private fun DrawScope.drawDie(x: Float, y: Float, size: Float, face: Int, owner: Int, used: Boolean) {
    val alpha = if (used) 0.35f else 1f
    val fill = (if (owner == 0) P0Fill else P1Fill).copy(alpha = alpha)
    val pip = (if (owner == 0) Ink else P0Fill).copy(alpha = alpha)
    drawRoundRect(Color.Black.copy(alpha = 0.3f * alpha), topLeft = Offset(x + 2f, y + 3f), size = Size(size, size), cornerRadius = CornerRadius(size * 0.22f, size * 0.22f))
    drawRoundRect(fill, topLeft = Offset(x, y), size = Size(size, size), cornerRadius = CornerRadius(size * 0.22f, size * 0.22f))
    drawRoundRect((if (owner == 0) P0Edge else P1Edge).copy(alpha = alpha), topLeft = Offset(x, y), size = Size(size, size), cornerRadius = CornerRadius(size * 0.22f, size * 0.22f), style = Stroke(width = 1.5f))
    val cx = x + size / 2f
    val cy = y + size / 2f
    val spread = size * 0.26f
    val r = size * 0.085f
    PIP_LAYOUT[face]?.forEach { (dx, dy) ->
        drawCircle(pip, radius = r, center = Offset(cx + dx * spread, cy + dy * spread))
    }
}

private fun DrawScope.drawCube(g: BoardGeometry, state: TavlaState, textMeasurer: TextMeasurer) {
    val size = g.trayW * 0.8f
    val cx = g.trayX + g.trayW / 2f
    val cy = when (state.cubeOwner) {
        null -> g.halfH
        0 -> g.halfH + size * 1.2f
        else -> g.halfH - size * 1.2f
    }
    val shown = if (state.phase == Phase.DOUBLE_OFFERED) state.cubeValue * 2 else state.cubeValue
    drawRoundRect(Color(0xFFFDE68A), topLeft = Offset(cx - size / 2f, cy - size / 2f), size = Size(size, size), cornerRadius = CornerRadius(size * 0.2f, size * 0.2f))
    drawRoundRect(Color(0xFFB45309), topLeft = Offset(cx - size / 2f, cy - size / 2f), size = Size(size, size), cornerRadius = CornerRadius(size * 0.2f, size * 0.2f), style = Stroke(width = 2f))
    drawCenteredText(textMeasurer, shown.toString(), cx, cy, 10.sp, Ink, bold = true)
}

private fun DrawScope.drawCenteredText(
    measurer: TextMeasurer,
    text: String,
    cx: Float,
    cy: Float,
    fontSize: TextUnit,
    color: Color,
    bold: Boolean = true,
) {
    val layout = measurer.measure(
        text = text,
        style = TextStyle(
            fontSize = fontSize,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        ),
    )
    drawText(
        textLayoutResult = layout,
        color = color,
        topLeft = Offset(cx - layout.size.width / 2f, cy - layout.size.height / 2f),
    )
}
