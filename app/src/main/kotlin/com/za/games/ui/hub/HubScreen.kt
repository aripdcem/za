package com.za.games.ui.hub

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.za.games.R
import com.za.games.platform.GameCategory
import com.za.games.platform.GameEntry
import java.util.Locale

/** Ana menü: platform manifestosu ve oyun listesi. */
@Composable
fun HubScreen(
    games: List<GameEntry>,
    highScores: Map<String, Long>,
    onPlay: (GameEntry) -> Unit,
    lastPlayed: Map<String, Long> = emptyMap(),
    category: GameCategory? = null,
    onCategory: (GameCategory?) -> Unit = {},
    soundOn: Boolean = true,
    onToggleSound: () -> Unit = {},
    hapticsOn: Boolean = true,
    onToggleHaptics: () -> Unit = {},
) {
    // Liste sırası kayıt sırasıdır (kararlı); hızlı erişim için ayrı bir "son oynananlar" şeridi var.
    val visible = if (category == null) games else games.filter { it.category == category }
    val recent = games
        .filter { (lastPlayed[it.id] ?: 0L) > 0L }
        .sortedByDescending { lastPlayed[it.id] ?: 0L }
        .take(RECENT_LIMIT)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { HubHeader(soundOn, onToggleSound, hapticsOn, onToggleHaptics) }
        item { CategoryChips(selected = category, onSelect = onCategory) }
        if (category == null && recent.isNotEmpty()) {
            item { RecentRow(games = recent, onPlay = onPlay) }
        }
        items(visible, key = { it.id }) { game ->
            GameCard(
                game = game,
                highScore = highScores[game.id] ?: 0L,
                onPlay = { onPlay(game) },
            )
        }
        item { ComingSoonCard() }
        item { HubFooter() }
    }
}

private const val RECENT_LIMIT = 4

/** Grup süzgeci: Tümü + kategoriler; seçim kalıcıdır. */
@Composable
private fun CategoryChips(selected: GameCategory?, onSelect: (GameCategory?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(label = stringResource(R.string.hub_cat_all), selected = selected == null) { onSelect(null) }
        GameCategory.entries.forEach { cat ->
            FilterChip(label = stringResource(cat.labelRes), selected = selected == cat) { onSelect(cat) }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

/** Son oynanan oyunlar: küçük kartlar, dokununca doğrudan açılır. */
@Composable
private fun RecentRow(games: List<GameEntry>, onPlay: (GameEntry) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.hub_recent).uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            games.forEach { game ->
                RecentCard(game = game, onPlay = { onPlay(game) }, modifier = Modifier.weight(1f))
            }
            repeat(RECENT_LIMIT - games.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun RecentCard(game: GameEntry, onPlay: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onPlay,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(game.accent.copy(alpha = 0.16f), Color.Transparent)))
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            game.art(Modifier.size(40.dp))
            Text(
                text = stringResource(game.titleRes),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HubFooter() {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }
    Text(
        text = "ZA v$version · zero-ads games",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    )
}

@Composable
private fun HubHeader(
    soundOn: Boolean,
    onToggleSound: () -> Unit,
    hapticsOn: Boolean,
    onToggleHaptics: () -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = stringResource(R.string.hub_title),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 4.sp,
                modifier = Modifier.weight(1f),
            )
            val soundToggleDescription = stringResource(
                if (soundOn) R.string.sound_off else R.string.sound_on,
            )
            IconButton(
                onClick = onToggleSound,
                modifier = Modifier.semantics { contentDescription = soundToggleDescription },
            ) {
                Text(text = if (soundOn) "🔊" else "🔇", fontSize = 22.sp)
            }
            val hapticsToggleDescription = stringResource(
                if (hapticsOn) R.string.haptics_off else R.string.haptics_on,
            )
            IconButton(
                onClick = onToggleHaptics,
                modifier = Modifier.semantics { contentDescription = hapticsToggleDescription },
            ) {
                Text(text = if (hapticsOn) "📳" else "📴", fontSize = 22.sp)
            }
        }
        Text(
            text = stringResource(R.string.hub_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZeroChip(stringResource(R.string.chip_no_ads))
            ZeroChip(stringResource(R.string.chip_no_trackers))
            ZeroChip(stringResource(R.string.chip_no_permissions))
        }
    }
}

@Composable
private fun ZeroChip(label: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun GameCard(game: GameEntry, highScore: Long, onPlay: () -> Unit) {
    Surface(
        onClick = onPlay,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier.background(
                Brush.horizontalGradient(
                    listOf(game.accent.copy(alpha = 0.14f), Color.Transparent),
                ),
            ),
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                game.art(Modifier.size(56.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(game.titleRes),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(game.taglineRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    )
                    if (highScore > 0L) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(
                                R.string.high_score_fmt,
                                String.format(Locale.getDefault(), "%,d", highScore),
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = game.accent,
                        )
                    }
                }
                Surface(shape = CircleShape, color = game.accent) {
                    Text(
                        text = stringResource(R.string.play),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF06121D),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ComingSoonCard() {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "+",
                fontSize = 30.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )
            Text(
                text = stringResource(R.string.coming_soon),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
    }
}

/** Menü kartı için tetromino kolajı. */
@Composable
fun TetrominoArt(modifier: Modifier = Modifier) {
    val purple = Color(0xFFA78BFA)
    val cyan = Color(0xFF22D3EE)
    val yellow = Color(0xFFFACC15)
    Canvas(modifier = modifier) {
        val cell = size.minDimension / 4f
        val corner = CornerRadius(cell * 0.25f, cell * 0.25f)
        fun block(r: Int, c: Int, color: Color) {
            drawRoundRect(
                color = color,
                topLeft = Offset(c * cell + 1f, r * cell + 1f),
                size = Size(cell - 2f, cell - 2f),
                cornerRadius = corner,
            )
        }
        // T taşı
        block(0, 1, purple)
        block(1, 0, purple)
        block(1, 1, purple)
        block(1, 2, purple)
        // I taşı
        block(2, 0, cyan)
        block(2, 1, cyan)
        block(2, 2, cyan)
        block(2, 3, cyan)
        // O taşının köşesi
        block(3, 0, yellow)
        block(3, 1, yellow)
    }
}

/** 2048 kartı için mini taş kolajı. */
@Composable
fun Art2048(modifier: Modifier = Modifier) {
    val tiles = listOf(
        Color(0xFFEEE4DA), Color(0xFFF2B179),
        Color(0xFFEDCF72), Color(0xFFF65E3B),
    )
    Canvas(modifier = modifier) {
        val cell = size.minDimension / 2f
        val corner = CornerRadius(cell * 0.2f, cell * 0.2f)
        tiles.forEachIndexed { i, color ->
            val r = i / 2
            val c = i % 2
            drawRoundRect(
                color = color,
                topLeft = Offset(c * cell + 2f, r * cell + 2f),
                size = Size(cell - 4f, cell - 4f),
                cornerRadius = corner,
            )
        }
    }
}

/** Yılan kartı için S kıvrımlı mini yılan + yem. */
@Composable
fun SnakeArt(modifier: Modifier = Modifier) {
    val green = Color(0xFF4ADE80)
    val red = Color(0xFFF87171)
    Canvas(modifier = modifier) {
        val cell = size.minDimension / 4f
        val corner = CornerRadius(cell * 0.3f, cell * 0.3f)
        fun seg(r: Int, c: Int, alpha: Float) {
            drawRoundRect(
                color = green.copy(alpha = alpha),
                topLeft = Offset(c * cell + 1.5f, r * cell + 1.5f),
                size = Size(cell - 3f, cell - 3f),
                cornerRadius = corner,
            )
        }
        seg(3, 0, 0.5f)
        seg(3, 1, 0.62f)
        seg(2, 1, 0.74f)
        seg(1, 1, 0.86f)
        seg(1, 2, 0.95f)
        seg(0, 2, 1f) // baş
        drawCircle(red, radius = cell * 0.3f, center = Offset(0.5f * cell, 0.5f * cell))
    }
}

/** Sudoku kartı için mini ızgara. */
@Composable
fun SudokuArt(modifier: Modifier = Modifier) {
    val blue = Color(0xFF60A5FA)
    Canvas(modifier = modifier) {
        val cell = size.minDimension / 3f
        val line = Color.White.copy(alpha = 0.35f)
        for (i in 0..3) {
            drawLine(line, Offset(i * cell, 0f), Offset(i * cell, size.minDimension), 2f)
            drawLine(line, Offset(0f, i * cell), Offset(size.minDimension, i * cell), 2f)
        }
        fun dot(r: Int, c: Int, color: Color) {
            drawRoundRect(
                color = color,
                topLeft = Offset(c * cell + cell * 0.22f, r * cell + cell * 0.22f),
                size = Size(cell * 0.56f, cell * 0.56f),
                cornerRadius = CornerRadius(cell * 0.16f, cell * 0.16f),
            )
        }
        dot(0, 0, blue)
        dot(1, 2, blue.copy(alpha = 0.7f))
        dot(2, 1, blue.copy(alpha = 0.45f))
    }
}

/** Mayın Tarlası kartı için mini tahta: bayrak + mayın. */
@Composable
fun MinesArt(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cell = size.minDimension / 3f
        val corner = CornerRadius(cell * 0.2f, cell * 0.2f)
        for (r in 0 until 3) {
            for (c in 0 until 3) {
                val revealed = (r + c) % 2 == 0
                drawRoundRect(
                    color = if (revealed) Color.White.copy(alpha = 0.07f) else Color(0xFF223049),
                    topLeft = Offset(c * cell + 1.5f, r * cell + 1.5f),
                    size = Size(cell - 3f, cell - 3f),
                    cornerRadius = corner,
                )
            }
        }
        // Bayrak (sol üst)
        val flagBase = Offset(cell * 0.5f, cell * 0.28f)
        drawLine(Color(0xFFE4EAF5), flagBase, flagBase + Offset(0f, cell * 0.5f), 3f)
        drawPath(
            path = androidx.compose.ui.graphics.Path().apply {
                moveTo(flagBase.x, flagBase.y)
                lineTo(flagBase.x + cell * 0.4f, flagBase.y + cell * 0.14f)
                lineTo(flagBase.x, flagBase.y + cell * 0.28f)
                close()
            },
            color = Color(0xFFF87171),
        )
        // Mayın (sağ alt)
        drawCircle(
            Color(0xFF0B0F1A),
            radius = cell * 0.26f,
            center = Offset(2.5f * cell, 2.5f * cell),
        )
    }
}

/** Kıskaç kartı: iki sınır çubuğu arasında sıkışan kelime kutusu. */
@Composable
fun KiskacArt(modifier: Modifier = Modifier) {
    val pink = Color(0xFFF472B6)
    Canvas(modifier = modifier) {
        val w = size.minDimension
        val barHeight = w * 0.2f
        val corner = CornerRadius(w * 0.07f, w * 0.07f)
        drawRoundRect(
            color = pink,
            topLeft = Offset(0f, w * 0.06f),
            size = Size(w, barHeight),
            cornerRadius = corner,
        )
        drawRoundRect(
            color = pink.copy(alpha = 0.55f),
            topLeft = Offset(0f, w - barHeight - w * 0.06f),
            size = Size(w, barHeight),
            cornerRadius = corner,
        )
        drawRoundRect(
            color = Color(0xFFFACC15),
            topLeft = Offset(w * 0.33f, (w - barHeight) / 2f),
            size = Size(w * 0.34f, barHeight),
            cornerRadius = corner,
        )
    }
}

/** Dizgi kartı: kesişen iki taş dizisi — mini kelime tahtası. */
@Composable
fun DizgiArt(modifier: Modifier = Modifier) {
    val face = Color(0xFFEADFC8)
    val accent = Color(0xFFFB923C)
    Canvas(modifier = modifier) {
        val w = size.minDimension
        val tile = w / 5f
        val gap = tile * 0.12f
        val corner = CornerRadius(tile * 0.2f, tile * 0.2f)
        fun tileAt(row: Int, col: Int, color: Color) {
            drawRoundRect(
                color = color,
                topLeft = Offset(col * tile + gap, row * tile + gap),
                size = Size(tile - 2 * gap, tile - 2 * gap),
                cornerRadius = corner,
            )
        }
        // Yatay dizi (2. satır) ve onu kesen dikey dizi (2. sütun).
        for (col in 0 until 5) tileAt(2, col, if (col == 2) accent else face)
        tileAt(0, 2, face.copy(alpha = 0.85f))
        tileAt(1, 2, face.copy(alpha = 0.92f))
        tileAt(3, 2, face.copy(alpha = 0.92f))
        tileAt(4, 2, face.copy(alpha = 0.85f))
    }
}

/** Türetme kartı: harf çarkı — merkez ve çevresinde harf daireleri. */
@Composable
fun TuretmeArt(modifier: Modifier = Modifier) {
    val purple = Color(0xFFA78BFA)
    Canvas(modifier = modifier) {
        val c = size.minDimension / 2f
        val center = Offset(c, c)
        drawCircle(purple.copy(alpha = 0.25f), radius = c * 0.95f, center = center)
        drawCircle(purple, radius = c * 0.28f, center = center)
        val orbit = c * 0.62f
        repeat(6) { i ->
            val angle = Math.toRadians(60.0 * i - 90.0)
            drawCircle(
                color = purple.copy(alpha = 0.75f),
                radius = c * 0.16f,
                center = center + Offset(
                    (orbit * kotlin.math.cos(angle)).toFloat(),
                    (orbit * kotlin.math.sin(angle)).toFloat(),
                ),
            )
        }
    }
}

/** Beş Harf kartı için mini tahmin satırları. */
@Composable
fun BesHarfArt(modifier: Modifier = Modifier) {
    val correct = Color(0xFF4ADE80)
    val present = Color(0xFFFACC15)
    val absent = Color(0xFF313A4E)
    // Üç satırlık mini sonuç deseni: giderek çözülen bir kelime.
    val patternRows = listOf(
        listOf(absent, present, absent, absent, present),
        listOf(present, correct, absent, correct, absent),
        listOf(correct, correct, correct, correct, correct),
    )
    Canvas(modifier = modifier) {
        val cell = size.minDimension / 5f
        val corner = CornerRadius(cell * 0.22f, cell * 0.22f)
        val originY = (size.minDimension - 3 * cell) / 2f
        patternRows.forEachIndexed { r, colors ->
            colors.forEachIndexed { c, color ->
                drawRoundRect(
                    color = color,
                    topLeft = Offset(c * cell + 1.5f, originY + r * cell + 1.5f),
                    size = Size(cell - 3f, cell - 3f),
                    cornerRadius = corner,
                )
            }
        }
    }
}

/** Kuyu kartı: iki duvar arasında düşen oyuncu, altındaki mermiler ve düşmanlar. */
@Composable
fun KuyuArt(modifier: Modifier = Modifier) {
    val wall = Color(0xFF475569)
    val player = Color(0xFFF1F5F9)
    val enemy = Color(0xFFF87171)
    Canvas(modifier = modifier) {
        val w = size.minDimension
        val unit = w / 5f
        drawRoundRect(
            color = Color(0xFF0B0F1A),
            size = Size(w, w),
            cornerRadius = CornerRadius(unit * 0.5f, unit * 0.5f),
        )
        drawRect(wall, topLeft = Offset(0f, 0f), size = Size(unit * 0.9f, w))
        drawRect(wall, topLeft = Offset(w - unit * 0.9f, 0f), size = Size(unit * 0.9f, w))
        drawRoundRect(
            color = player,
            topLeft = Offset(unit * 2.15f, unit * 0.6f),
            size = Size(unit * 0.7f, unit * 0.8f),
            cornerRadius = CornerRadius(unit * 0.15f, unit * 0.15f),
        )
        drawRect(player.copy(alpha = 0.7f), Offset(unit * 2.44f, unit * 1.6f), Size(unit * 0.12f, unit * 0.35f))
        drawRect(player.copy(alpha = 0.4f), Offset(unit * 2.44f, unit * 2.2f), Size(unit * 0.12f, unit * 0.35f))
        drawCircle(enemy, radius = unit * 0.32f, center = Offset(unit * 1.55f, unit * 3.5f))
        drawCircle(enemy, radius = unit * 0.32f, center = Offset(unit * 3.45f, unit * 4.25f))
    }
}

/** Geçit kartı: nehir, yol ve çim şeritleri; kütük, arabalar ve ortada kurbağa. */
@Composable
fun GecitArt(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.minDimension
        val lane = w / 5f
        val lanes = listOf(
            Color(0xFF1D4ED8), Color(0xFF334155), Color(0xFF3F6212), Color(0xFF334155), Color(0xFF4D7C0F),
        )
        lanes.forEachIndexed { i, color ->
            drawRect(color, topLeft = Offset(0f, i * lane), size = Size(w, lane + 1f))
        }
        val r = CornerRadius(lane * 0.3f, lane * 0.3f)
        drawRoundRect(Color(0xFF92400E), Offset(lane * 0.8f, lane * 0.2f), Size(lane * 2f, lane * 0.6f), r)
        drawRoundRect(Color(0xFFF87171), Offset(lane * 3.2f, lane * 1.15f), Size(lane * 1.1f, lane * 0.7f), r)
        drawRoundRect(Color(0xFFA3E635), Offset(lane * 2.2f, lane * 2.2f), Size(lane * 0.6f, lane * 0.6f), r)
        drawRoundRect(Color(0xFFFBBF24), Offset(lane * 0.3f, lane * 3.15f), Size(lane * 1.9f, lane * 0.7f), r)
        drawCircle(Color(0xFF166534), radius = lane * 0.32f, center = Offset(lane * 4.3f, lane * 4.45f))
    }
}

/** Tavla kartı: karşılıklı üçgen haneler, iki renkte pullar. */
@Composable
fun TavlaArt(modifier: Modifier = Modifier) {
    val board = Color(0xFF2A1F1B)
    val dark = Color(0xFF5B4636)
    val light = Color(0xFF8B6B4F)
    val white = Color(0xFFF5F5F4)
    val black = Color(0xFF292524)
    Canvas(modifier = modifier) {
        val w = size.minDimension
        drawRoundRect(board, size = Size(w, w), cornerRadius = CornerRadius(w * 0.12f, w * 0.12f))
        val rows = 5
        val rowH = w / rows
        for (r in 0 until rows) {
            val y = r * rowH
            val leftTri = Path().apply {
                moveTo(0f, y)
                lineTo(0f, y + rowH)
                lineTo(w * 0.42f, y + rowH / 2f)
                close()
            }
            val rightTri = Path().apply {
                moveTo(w, y)
                lineTo(w, y + rowH)
                lineTo(w * 0.58f, y + rowH / 2f)
                close()
            }
            drawPath(leftTri, if (r % 2 == 0) dark else light)
            drawPath(rightTri, if (r % 2 == 0) light else dark)
        }
        val r = rowH * 0.36f
        drawCircle(white, r, Offset(r * 1.1f, rowH * 0.5f))
        drawCircle(white, r, Offset(r * 3.0f, rowH * 0.5f))
        drawCircle(black, r, Offset(w - r * 1.1f, rowH * 4.5f))
        drawCircle(black, r, Offset(w - r * 3.0f, rowH * 4.5f))
        drawCircle(black, r, Offset(r * 1.1f, rowH * 3.5f))
        drawCircle(white, r, Offset(w - r * 1.1f, rowH * 1.5f))
    }
}

/** Balkon kartı: yukarıdan sokak, korkuluk, aşağıda güvercin ve yeşil bir leke. */
@Composable
fun BalkonArt(modifier: Modifier = Modifier) {
    val wall = Color(0xFF7C5C46)
    val rail = Color(0xFF1F2937)
    val pavement = Color(0xFFA1A7B0)
    val asphalt = Color(0xFF3F3F46)
    val line = Color(0xFFE5E7EB)
    val pigeon = Color(0xFFD1D5DB)
    val splat = Color(0xFF4ADE80)
    val skin = Color(0xFFF1C27D)
    val hair = Color(0xFF3B2A20)
    Canvas(modifier = modifier) {
        val w = size.minDimension
        drawRoundRect(pavement, size = Size(w, w), cornerRadius = CornerRadius(w * 0.12f, w * 0.12f))
        drawRect(asphalt, topLeft = Offset(0f, w * 0.52f), size = Size(w, w * 0.3f))
        drawLine(line, Offset(w * 0.08f, w * 0.67f), Offset(w * 0.3f, w * 0.67f), strokeWidth = w * 0.03f)
        drawLine(line, Offset(w * 0.45f, w * 0.67f), Offset(w * 0.67f, w * 0.67f), strokeWidth = w * 0.03f)
        drawRect(wall, topLeft = Offset(0f, 0f), size = Size(w, w * 0.22f))
        drawLine(rail, Offset(0f, w * 0.2f), Offset(w, w * 0.2f), strokeWidth = w * 0.04f)
        drawCircle(skin, radius = w * 0.06f, center = Offset(w * 0.36f, w * 0.2f))
        drawCircle(skin, radius = w * 0.06f, center = Offset(w * 0.64f, w * 0.2f))
        drawCircle(hair, radius = w * 0.1f, center = Offset(w * 0.5f, w * 0.14f))
        drawOval(splat, topLeft = Offset(w * 0.52f, w * 0.36f), size = Size(w * 0.3f, w * 0.16f))
        drawCircle(splat, radius = w * 0.04f, center = Offset(w * 0.5f, w * 0.34f))
        drawOval(pigeon, topLeft = Offset(w * 0.12f, w * 0.86f), size = Size(w * 0.22f, w * 0.12f))
        drawCircle(Color(0xFF6B7280), radius = w * 0.04f, center = Offset(w * 0.35f, w * 0.92f))
    }
}

/** Kakuro kartı: çapraz ipucu hücreleri ve doldurulmuş beyaz hücreler. */
@Composable
fun KakuroArt(modifier: Modifier = Modifier) {
    val bg = Color(0xFF0F1628)
    val clue = Color(0xFF1E293B)
    val white = Color(0xFFE8ECF3)
    val green = Color(0xFFBBF7D0)
    val line = Color(0x59FFFFFF)
    Canvas(modifier = modifier) {
        val w = size.minDimension
        val cell = w / 4f
        drawRoundRect(bg, size = Size(w, w), cornerRadius = CornerRadius(w * 0.12f, w * 0.12f))
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                val x = c * cell
                val y = r * cell
                val isClue = r == 0 || c == 0 || (r == 2 && c == 2)
                val color = when {
                    isClue -> clue
                    r == 1 -> green
                    else -> white
                }
                drawRect(color, topLeft = Offset(x + 1f, y + 1f), size = Size(cell - 2f, cell - 2f))
                if (isClue && (r + c) > 0) {
                    drawLine(line, Offset(x, y), Offset(x + cell, y + cell), strokeWidth = 1.5f)
                }
            }
        }
        // Sayı yerine küçük noktalar: dolu hücre izlenimi.
        val dot = Color(0xFF0F172A)
        for ((r, c) in listOf(1 to 1, 1 to 2, 1 to 3, 2 to 1, 3 to 2)) {
            drawCircle(dot, radius = cell * 0.13f, center = Offset(c * cell + cell / 2f, r * cell + cell / 2f))
        }
    }
}
