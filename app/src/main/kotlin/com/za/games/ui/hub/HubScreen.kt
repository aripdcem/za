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
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.za.games.R
import com.za.games.platform.GameCategory
import com.za.games.platform.Changelog
import com.za.games.platform.GameEntry
import com.za.games.platform.ReleaseNote
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
    onAbout: () -> Unit = {},
    /** Güncellemeden sonra gösterilen sürüm notu; null = kart yok. */
    whatsNew: ReleaseNote? = null,
    onDismissWhatsNew: () -> Unit = {},
    /** Bu sürüm kodundan sonra eklenen oyunlar "Yeni" rozeti alır. */
    newSinceCode: Int = Int.MAX_VALUE,
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
            .safeDrawingPadding()
            .testTag(HUB_LIST_TAG),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { HubHeader(soundOn, onToggleSound, hapticsOn, onToggleHaptics, onAbout) }
        item { CategoryChips(selected = category, onSelect = onCategory) }
        if (whatsNew != null) {
            item { WhatsNewCard(note = whatsNew, onDismiss = onDismissWhatsNew) }
        }
        if (category == null && recent.isNotEmpty()) {
            item { RecentRow(games = recent, onPlay = onPlay) }
        }
        items(visible, key = { it.id }) { game ->
            GameCard(
                game = game,
                highScore = highScores[game.id] ?: 0L,
                isNew = Changelog.versionCode(game.since) > newSinceCode,
                onPlay = { onPlay(game) },
            )
        }
        item { ComingSoonCard() }
        item { HubFooter() }
    }
}

private const val RECENT_LIMIT = 4

/** Testlerin liste düğümünü bulup kaydırması için. */
const val HUB_LIST_TAG = "hub_list"

/** Güncellemeden sonraki ilk açılışta en üstte görünen sürüm notu kartı. */
@Composable
private fun WhatsNewCard(note: ReleaseNote, onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.whats_new_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.whats_new_version_fmt, note.version, note.date),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            note.notes().forEach { line ->
                Text(text = "• $line", style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = onDismiss, modifier = Modifier.padding(top = 4.dp)) {
                Text(stringResource(R.string.whats_new_dismiss))
            }
        }
    }
}

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
    onAbout: () -> Unit,
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
            val aboutDescription = stringResource(R.string.about_title)
            IconButton(
                onClick = onAbout,
                modifier = Modifier.semantics { contentDescription = aboutDescription },
            ) {
                Text(text = "ⓘ", fontSize = 22.sp)
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
private fun GameCard(game: GameEntry, highScore: Long, isNew: Boolean, onPlay: () -> Unit) {
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(game.titleRes),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        if (isNew) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary) {
                                Text(
                                    text = stringResource(R.string.badge_new),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
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

/** Vergici kartı: sayı taşları ve vergicinin aldığı kırmızı bölenler. */
@Composable
fun VergiciArt(modifier: Modifier = Modifier) {
    val bg = Color(0xFF14201C)
    val tile = Color(0xFF2E3B2F)
    val coin = Color(0xFFA3E635)
    val tax = Color(0xFFF87171)
    Canvas(modifier = modifier) {
        val w = size.minDimension
        drawRoundRect(bg, size = Size(w, w), cornerRadius = CornerRadius(w * 0.12f, w * 0.12f))
        val cell = w / 3f
        for (r in 0 until 3) {
            for (c in 0 until 3) {
                val idx = r * 3 + c
                val color = when (idx) {
                    4 -> coin
                    0, 1, 3 -> tax
                    else -> tile
                }
                drawRoundRect(color, topLeft = Offset(c * cell + 3f, r * cell + 3f), size = Size(cell - 6f, cell - 6f), cornerRadius = CornerRadius(5f, 5f))
            }
        }
        drawCircle(Color(0xFF14201C), radius = cell * 0.18f, center = Offset(cell * 1.5f, cell * 1.5f))
    }
}

/** Toplam Kapma kartı: dokuz sayı pulu, üçü kazanan yeşil. */
@Composable
fun ToplamArt(modifier: Modifier = Modifier) {
    val bg = Color(0xFF1C1430)
    val chip = Color(0xFF3B2F55)
    val p0 = Color(0xFF22D3EE)
    val p1 = Color(0xFFF472B6)
    val win = Color(0xFF4ADE80)
    Canvas(modifier = modifier) {
        val w = size.minDimension
        drawRoundRect(bg, size = Size(w, w), cornerRadius = CornerRadius(w * 0.12f, w * 0.12f))
        val cell = w / 3f
        val colors = listOf(win, chip, p1, p0, win, chip, chip, p1, win)
        for (r in 0 until 3) {
            for (c in 0 until 3) {
                drawCircle(colors[r * 3 + c], radius = cell * 0.36f, center = Offset(c * cell + cell / 2f, r * cell + cell / 2f))
            }
        }
    }
}

/** Viraj kartı: ufka giden yol, kırmızı-beyaz bordürler ve bir kart. */
@Composable
fun VirajArt(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.minDimension
        val sky = Color(0xFF0B1E3D)
        val grass = Color(0xFF2F6B2F)
        val road = Color(0xFF3B4252)
        drawRoundRect(sky, size = Size(w, w), cornerRadius = CornerRadius(w * 0.12f, w * 0.12f))
        drawRect(grass, topLeft = Offset(0f, w * 0.42f), size = Size(w, w * 0.58f))
        val roadPath = Path().apply {
            moveTo(w * 0.44f, w * 0.42f)
            lineTo(w * 0.56f, w * 0.42f)
            lineTo(w * 1.02f, w)
            lineTo(-w * 0.02f, w)
            close()
        }
        drawPath(roadPath, road)
        val curb = Path().apply {
            moveTo(w * 0.44f, w * 0.42f)
            lineTo(w * 0.47f, w * 0.42f)
            lineTo(w * 0.12f, w)
            lineTo(-w * 0.02f, w)
            close()
        }
        drawPath(curb, Color(0xFFDC2626))
        val curb2 = Path().apply {
            moveTo(w * 0.56f, w * 0.42f)
            lineTo(w * 0.53f, w * 0.42f)
            lineTo(w * 0.88f, w)
            lineTo(w * 1.02f, w)
            close()
        }
        drawPath(curb2, Color(0xFFE5E7EB))
        drawCircle(Color(0xFFFDE68A), radius = w * 0.07f, center = Offset(w * 0.74f, w * 0.2f))
        val r = CornerRadius(w * 0.05f, w * 0.05f)
        drawRoundRect(Color(0xFF1F2937), Offset(w * 0.36f, w * 0.78f), Size(w * 0.28f, w * 0.14f), r)
        drawRoundRect(Color(0xFF4DE1FF), Offset(w * 0.4f, w * 0.7f), Size(w * 0.2f, w * 0.16f), r)
        drawCircle(Color(0xFFF8FAFC), radius = w * 0.035f, center = Offset(w * 0.5f, w * 0.7f))
    }
}

/** Filo kartı: yıldızlı uzay, üstte düşman üçlüsü, altta gemi, mermi ve alev. */
@Composable
fun FiloArt(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.minDimension
        drawRoundRect(Color(0xFF0B1226), size = Size(w, w), cornerRadius = CornerRadius(w * 0.12f, w * 0.12f))
        val star = Color(0xFFE2E8F0)
        listOf(0.12f to 0.2f, 0.3f to 0.55f, 0.55f to 0.1f, 0.82f to 0.4f, 0.9f to 0.75f, 0.2f to 0.85f, 0.7f to 0.62f).forEach { (x, y) ->
            drawCircle(star.copy(alpha = 0.7f), radius = w * 0.018f, center = Offset(w * x, w * y))
        }
        val drone = Color(0xFFF87171)
        for (i in 0 until 3) {
            val cx = w * (0.3f + i * 0.2f)
            val cy = w * (if (i == 1) 0.32f else 0.24f)
            val r = w * 0.075f
            val p = Path().apply {
                moveTo(cx, cy + r)
                lineTo(cx + r, cy - r * 0.6f)
                lineTo(cx, cy - r * 0.25f)
                lineTo(cx - r, cy - r * 0.6f)
                close()
            }
            drawPath(p, drone)
        }
        drawRoundRect(Color(0xFF67E8F9), Offset(w * 0.485f, w * 0.48f), Size(w * 0.03f, w * 0.12f), CornerRadius(w * 0.015f, w * 0.015f))
        val sx = w * 0.5f
        val sy = w * 0.76f
        val r = w * 0.1f
        val flame = Path().apply {
            moveTo(sx - r * 0.35f, sy + r * 0.75f)
            lineTo(sx, sy + r * 1.8f)
            lineTo(sx + r * 0.35f, sy + r * 0.75f)
            close()
        }
        drawPath(flame, Color(0xFFFB923C))
        val ship = Path().apply {
            moveTo(sx, sy - r * 1.35f)
            lineTo(sx + r * 0.45f, sy - r * 0.1f)
            lineTo(sx + r * 1.25f, sy + r * 0.85f)
            lineTo(sx + r * 0.4f, sy + r * 0.75f)
            lineTo(sx, sy + r * 0.5f)
            lineTo(sx - r * 0.4f, sy + r * 0.75f)
            lineTo(sx - r * 1.25f, sy + r * 0.85f)
            lineTo(sx - r * 0.45f, sy - r * 0.1f)
            close()
        }
        drawPath(ship, Color(0xFFE2E8F0))
        drawRect(Color(0xFFFB7185), Offset(sx - r * 0.12f, sy - r * 0.9f), Size(r * 0.24f, r * 1.3f))
        drawCircle(Color(0xFF22D3EE), radius = r * 0.2f, center = Offset(sx, sy - r * 0.45f))
    }
}

/** Reyon kartı: üç raf, marka renkli ürün blokları ve göz hizasında bir ★. */
@Composable
fun ReyonArt(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.minDimension
        drawRoundRect(Color(0xFF0F1628), size = Size(w, w), cornerRadius = CornerRadius(w * 0.12f, w * 0.12f))
        val plank = Color(0xFF475569)
        val rows = 3
        val sh = w * 0.8f / rows
        val top = w * 0.1f
        for (r in 0 until rows) {
            val y = top + (r + 1) * sh - sh * 0.14f
            drawRoundRect(plank, topLeft = Offset(w * 0.05f, y), size = Size(w * 0.9f, sh * 0.14f), cornerRadius = CornerRadius(w * 0.02f, w * 0.02f))
        }
        val blocks = listOf(
            Triple(0, 0.08f, 0.36f) to Color(0xFF60A5FA),
            Triple(0, 0.48f, 0.44f) to Color(0xFF60A5FA),
            Triple(1, 0.08f, 0.26f) to Color(0xFFFBBF24),
            Triple(1, 0.38f, 0.54f) to Color(0xFF4ADE80),
            Triple(2, 0.08f, 0.54f) to Color(0xFF4ADE80),
            Triple(2, 0.66f, 0.26f) to Color(0xFFF472B6),
        )
        for ((spec, color) in blocks) {
            val (r, x, bw) = spec
            val y = top + r * sh + sh * 0.1f
            drawRoundRect(color, topLeft = Offset(w * x, y), size = Size(w * bw, sh * 0.62f), cornerRadius = CornerRadius(w * 0.03f, w * 0.03f))
        }
        val cx = w * 0.65f
        val cy = top + sh * 1.41f
        val radius = w * 0.06f
        val star = Path().apply {
            for (i in 0 until 10) {
                val a = -PI.toFloat() / 2f + i * PI.toFloat() / 5f
                val rr = if (i % 2 == 0) radius else radius * 0.45f
                val px = cx + rr * cos(a)
                val py = cy + rr * sin(a)
                if (i == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }
        drawPath(star, Color(0xFF0F172A))
    }
}

@Composable
fun RaketArt(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.minDimension
        drawRoundRect(Color(0xFF0E3446), size = Size(w, w), cornerRadius = CornerRadius(w * 0.12f, w * 0.12f))
        for (i in 0 until 6) {
            drawRect(Color(0x66E2E8F0), Offset(w * (0.08f + i * 0.15f), w * 0.49f), Size(w * 0.08f, w * 0.02f))
        }
        drawRoundRect(Color(0xFFFB7185), Offset(w * 0.28f, w * 0.1f), Size(w * 0.3f, w * 0.055f), CornerRadius(w * 0.03f, w * 0.03f))
        drawRoundRect(Color(0xFF5EEAD4), Offset(w * 0.45f, w * 0.845f), Size(w * 0.3f, w * 0.055f), CornerRadius(w * 0.03f, w * 0.03f))
        val ball = Color(0xFFFDE68A)
        listOf(0.36f to 0.22f, 0.42f to 0.34f, 0.48f to 0.46f, 0.54f to 0.58f).forEachIndexed { i, (x, y) ->
            drawCircle(ball.copy(alpha = 0.15f + 0.15f * i), radius = w * (0.02f + 0.006f * i), center = Offset(w * x, w * y))
        }
        drawCircle(ball, radius = w * 0.05f, center = Offset(w * 0.6f, w * 0.7f))
        drawCircle(Color(0xFFFFFBEB), radius = w * 0.02f, center = Offset(w * 0.585f, w * 0.685f))
    }
}

@Composable
fun TuseArt(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.minDimension
        drawRoundRect(Color(0xFFF7F3EA), size = Size(w, w), cornerRadius = CornerRadius(w * 0.12f, w * 0.12f))
        for (l in 1 until 4) {
            drawRect(Color(0x1F000000), Offset(w * 0.25f * l - w * 0.005f, w * 0.06f), Size(w * 0.01f, w * 0.88f))
        }
        val tile = Color(0xFF1F2937)
        val cellW = w * 0.25f
        val cellH = w * 0.2f
        listOf(2 to 0, 0 to 1, 3 to 2, 1 to 3, 2 to 4).forEach { (lane, row) ->
            val alpha = if (row == 4) 0.18f else 1f
            drawRoundRect(tile.copy(alpha = alpha), Offset(cellW * lane + w * 0.02f, cellH * row + w * 0.02f), Size(cellW - w * 0.04f, cellH - w * 0.04f), CornerRadius(w * 0.03f, w * 0.03f))
        }
        drawRoundRect(Color(0xFFE879F9), Offset(cellW * 1 + w * 0.02f, cellH * 3 + w * 0.02f), Size(cellW - w * 0.04f, cellH - w * 0.04f), CornerRadius(w * 0.03f, w * 0.03f), style = Stroke(width = w * 0.02f))
    }
}

@Composable
fun UcurtmaArt(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.minDimension
        drawRoundRect(Color(0xFF7DD3FC), size = Size(w, w), cornerRadius = CornerRadius(w * 0.12f, w * 0.12f))
        drawCircle(Color(0xFFFDE68A), radius = w * 0.09f, center = Offset(w * 0.8f, w * 0.2f))
        listOf(0.0f to 0.3f, 0.22f to 0.45f, 0.48f to 0.28f, 0.7f to 0.4f, 0.88f to 0.22f).forEach { (x, h) ->
            drawRect(Color(0xFF9A3412), Offset(w * x, w * (1f - h)), Size(w * 0.2f, w * h))
        }
        drawRect(Color(0xFF7F1D1D), Offset(w * 0.3f, w * 0.47f), Size(w * 0.05f, w * 0.08f))
        drawLine(Color(0xFF0F172A), Offset(w * 0.45f, w * 0.62f), Offset(w * 1f, w * 0.62f), strokeWidth = w * 0.012f)
        drawLine(Color(0x99334155), Offset(w * 0.48f, w * 0.3f), Offset(w * 0.08f, w * 1f), strokeWidth = w * 0.01f)
        val p = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.48f, w * 0.14f)
            lineTo(w * 0.62f, w * 0.3f)
            lineTo(w * 0.48f, w * 0.5f)
            lineTo(w * 0.34f, w * 0.3f)
            close()
        }
        drawPath(p, Color(0xFFEF4444))
        drawLine(Color(0xFFFDE68A), Offset(w * 0.48f, w * 0.14f), Offset(w * 0.48f, w * 0.5f), strokeWidth = w * 0.012f)
        drawLine(Color(0xFFFDE68A), Offset(w * 0.34f, w * 0.3f), Offset(w * 0.62f, w * 0.3f), strokeWidth = w * 0.012f)
        listOf(0.42f to 0.58f, 0.36f to 0.66f, 0.3f to 0.72f).forEach { (x, y) -> drawCircle(Color(0xFFFDE68A), radius = w * 0.025f, center = Offset(w * x, w * y)) }
    }
}

@Composable
fun DalgicArt(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.minDimension
        drawRoundRect(Color(0xFF0C4A6E), size = Size(w, w), cornerRadius = CornerRadius(w * 0.12f, w * 0.12f))
        drawRect(Color(0xFFBAE6FD), Offset(0f, 0f), Size(w, w * 0.14f))
        drawRect(Color(0xFFF0F9FF), Offset(0f, w * 0.13f), Size(w, w * 0.015f))
        drawRect(Color(0xFFD6B27A), Offset(0f, w * 0.9f), Size(w, w * 0.1f))
        // Denizaltı.
        drawRoundRect(Color(0xFFFACC15), Offset(w * 0.28f, w * 0.4f), Size(w * 0.36f, w * 0.15f), CornerRadius(w * 0.075f, w * 0.075f))
        drawRoundRect(Color(0xFFCA8A04), Offset(w * 0.4f, w * 0.32f), Size(w * 0.11f, w * 0.09f), CornerRadius(w * 0.02f, w * 0.02f))
        drawCircle(Color(0xFF7DD3FC), radius = w * 0.03f, center = Offset(w * 0.56f, w * 0.47f))
        drawRect(Color(0xFFCA8A04), Offset(w * 0.24f, w * 0.38f), Size(w * 0.04f, w * 0.19f))
        // Dalgıç.
        drawCircle(Color(0xFFFCD9B6), radius = w * 0.035f, center = Offset(w * 0.8f, w * 0.7f))
        drawRoundRect(Color(0xFF1F2937), Offset(w * 0.64f, w * 0.68f), Size(w * 0.14f, w * 0.06f), CornerRadius(w * 0.03f, w * 0.03f))
        // Köpekbalığı.
        val shark = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.1f, w * 0.75f)
            lineTo(w * 0.3f, w * 0.68f)
            lineTo(w * 0.42f, w * 0.75f)
            lineTo(w * 0.3f, w * 0.82f)
            close()
        }
        drawPath(shark, Color(0xFF94A3B8))
        val fin = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.24f, w * 0.7f)
            lineTo(w * 0.28f, w * 0.6f)
            lineTo(w * 0.33f, w * 0.7f)
            close()
        }
        drawPath(fin, Color(0xFF94A3B8))
        drawCircle(Color(0xFF111827), radius = w * 0.03f, center = Offset(w * 0.6f, w * 0.86f))
    }
}

/** Bostan: orman şeridi, toprak şeritler, korkuluk, karga ve damla. */
@Composable
fun BostanArt(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.minDimension
        drawRoundRect(Color(0xFF4D7C0F), size = Size(w, w), cornerRadius = CornerRadius(w * 0.12f, w * 0.12f))
        for (i in 0 until 5) {
            drawRect(if (i % 2 == 0) Color(0xFF7C4A1E) else Color(0xFF8A5A2B), Offset(w * (0.08f + i * 0.168f), w * 0.2f), Size(w * 0.168f, w * 0.7f))
        }
        drawRect(Color(0xFF14532D), Offset(0f, w * 0.06f), Size(w, w * 0.14f))
        for (i in 0 until 6) {
            val tx = w * (0.08f + i * 0.17f)
            val tree = androidx.compose.ui.graphics.Path().apply {
                moveTo(tx, w * 0.04f)
                lineTo(tx - w * 0.06f, w * 0.2f)
                lineTo(tx + w * 0.06f, w * 0.2f)
                close()
            }
            drawPath(tree, if (i % 2 == 0) Color(0xFF22C55E) else Color(0xFF166534))
        }
        // Korkuluk.
        drawLine(Color(0xFF92400E), Offset(w * 0.5f, w * 0.85f), Offset(w * 0.5f, w * 0.45f), strokeWidth = w * 0.05f)
        drawLine(Color(0xFF92400E), Offset(w * 0.3f, w * 0.58f), Offset(w * 0.7f, w * 0.58f), strokeWidth = w * 0.045f)
        drawRoundRect(Color(0xFFFACC15), Offset(w * 0.4f, w * 0.55f), Size(w * 0.2f, w * 0.16f), CornerRadius(w * 0.03f, w * 0.03f))
        drawCircle(Color(0xFFF5D0A9), radius = w * 0.09f, center = Offset(w * 0.5f, w * 0.44f))
        drawRect(Color(0xFFFACC15), Offset(w * 0.36f, w * 0.34f), Size(w * 0.28f, w * 0.04f))
        drawRect(Color(0xFFFACC15), Offset(w * 0.42f, w * 0.27f), Size(w * 0.16f, w * 0.08f))
        // Karga.
        drawOval(Color(0xFF111827), Offset(w * 0.72f, w * 0.3f), Size(w * 0.16f, w * 0.22f))
        drawCircle(Color(0xFF111827), radius = w * 0.06f, center = Offset(w * 0.8f, w * 0.52f))
        val beak = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.77f, w * 0.55f)
            lineTo(w * 0.83f, w * 0.55f)
            lineTo(w * 0.8f, w * 0.62f)
            close()
        }
        drawPath(beak, Color(0xFFF97316))
        // Damla.
        drawCircle(Color(0xFF38BDF8), radius = w * 0.07f, center = Offset(w * 0.2f, w * 0.7f))
        val tip = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.14f, w * 0.68f)
            lineTo(w * 0.2f, w * 0.56f)
            lineTo(w * 0.26f, w * 0.68f)
            close()
        }
        drawPath(tip, Color(0xFF38BDF8))
    }
}

/** Sincap: gökyüzü, çınar gövdesi, yapraklı dallar, sincap, karga ve kedi. */
@Composable
fun SincapArt(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.minDimension
        drawRoundRect(Color(0xFF7DD3FC), size = Size(w, w), cornerRadius = CornerRadius(w * 0.12f, w * 0.12f))
        drawOval(Color(0x99FFFFFF), Offset(w * 0.55f, w * 0.1f), Size(w * 0.3f, w * 0.1f))
        drawRect(Color(0xFF7C4A1E), Offset(w * 0.42f, 0f), Size(w * 0.16f, w))
        drawRect(Color(0xFF5B3414), Offset(w * 0.42f, 0f), Size(w * 0.03f, w))
        // Dallar ve yapraklar.
        for ((i, y) in listOf(0.3f, 0.55f, 0.8f).withIndex()) {
            val left = i != 1
            val x0 = if (left) w * 0.42f else w * 0.58f
            val x1 = if (left) w * 0.08f else w * 0.92f
            drawLine(Color(0xFF92400E), Offset(x0, w * y), Offset(x1, w * (y - 0.03f)), strokeWidth = w * 0.05f)
            val lx = if (left) w * 0.18f else w * 0.8f
            drawCircle(Color(0xFF4D7C0F), radius = w * 0.09f, center = Offset(lx, w * (y - 0.08f)))
            drawCircle(Color(0xFF84CC16), radius = w * 0.07f, center = Offset(lx + (if (left) w * 0.12f else -w * 0.12f), w * (y - 0.1f)))
        }
        // Fındık.
        drawCircle(Color(0xFFB45309), radius = w * 0.045f, center = Offset(w * 0.2f, w * 0.72f))
        // Sincap (sağ dalda).
        drawOval(Color(0xFFB45309), Offset(w * 0.66f, w * 0.34f), Size(w * 0.16f, w * 0.18f))
        drawCircle(Color(0xFFB45309), radius = w * 0.06f, center = Offset(w * 0.82f, w * 0.34f))
        drawCircle(Color(0xFF111827), radius = w * 0.012f, center = Offset(w * 0.845f, w * 0.325f))
        val tail = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.66f, w * 0.48f)
            quadraticBezierTo(w * 0.52f, w * 0.5f, w * 0.56f, w * 0.26f)
        }
        drawPath(tail, Color(0xFFD97706), style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.07f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        // Karga.
        drawOval(Color(0xFF111827), Offset(w * 0.1f, w * 0.12f), Size(w * 0.16f, w * 0.08f))
        val wing = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.16f, w * 0.15f)
            lineTo(w * 0.06f, w * 0.05f)
            lineTo(w * 0.2f, w * 0.12f)
            close()
        }
        drawPath(wing, Color(0xFF111827))
        // Kedi (gövdede, altta).
        drawRoundRect(Color(0xFFF97316), Offset(w * 0.43f, w * 0.78f), Size(w * 0.14f, w * 0.2f), CornerRadius(w * 0.06f, w * 0.06f))
        drawCircle(Color(0xFFF97316), radius = w * 0.07f, center = Offset(w * 0.5f, w * 0.76f))
        drawCircle(Color(0xFF4ADE80), radius = w * 0.014f, center = Offset(w * 0.475f, w * 0.75f))
        drawCircle(Color(0xFF4ADE80), radius = w * 0.014f, center = Offset(w * 0.525f, w * 0.75f))
    }
}

/** Çekirge: gökyüzü, buğday tarlası, formasyon hâlinde çekirgeler, saman balyası ve pompalı çiftçi. */
@Composable
fun CekirgeArt(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.minDimension
        drawRoundRect(Color(0xFF7DD3FC), size = Size(w, w), cornerRadius = CornerRadius(w * 0.12f, w * 0.12f))
        drawRect(Color(0xFFE9B949), Offset(0f, w * 0.82f), Size(w, w * 0.18f))
        drawRect(Color(0xFF3F6212), Offset(0f, w * 0.81f), Size(w, w * 0.02f))
        for (i in 0 until 5) {
            drawLine(Color(0xFFC98F2B), Offset(w * (0.1f + i * 0.2f), w), Offset(w * (0.12f + i * 0.2f), w * 0.86f), strokeWidth = w * 0.015f)
        }
        // Sürü: 4 × 3.
        for (r in 0 until 3) {
            for (c in 0 until 4) {
                val cx = w * (0.2f + c * 0.2f)
                val cy = w * (0.14f + r * 0.14f)
                val color = when (r) {
                    0 -> Color(0xFF1F2937)
                    1 -> Color(0xFF3F6212)
                    else -> Color(0xFFA16207)
                }
                drawLine(color, Offset(cx - w * 0.06f, cy - w * 0.02f), Offset(cx + w * 0.06f, cy - w * 0.02f), strokeWidth = w * 0.012f)
                drawLine(color, Offset(cx - w * 0.06f, cy + w * 0.03f), Offset(cx + w * 0.06f, cy + w * 0.03f), strokeWidth = w * 0.012f)
                drawOval(color, Offset(cx - w * 0.035f, cy - w * 0.045f), Size(w * 0.07f, w * 0.09f))
                drawCircle(Color(0xFFF8FAFC), radius = w * 0.008f, center = Offset(cx - w * 0.012f, cy + w * 0.03f))
                drawCircle(Color(0xFFF8FAFC), radius = w * 0.008f, center = Offset(cx + w * 0.012f, cy + w * 0.03f))
            }
        }
        // Balyalar.
        drawRect(Color(0xFFFACC15), Offset(w * 0.12f, w * 0.62f), Size(w * 0.16f, w * 0.08f))
        drawRect(Color(0xFFFACC15), Offset(w * 0.72f, w * 0.62f), Size(w * 0.16f, w * 0.08f))
        // Fıskırtma.
        drawLine(Color(0xFFBAE6FD), Offset(w * 0.5f, w * 0.72f), Offset(w * 0.5f, w * 0.6f), strokeWidth = w * 0.02f)
        // Çiftçi.
        drawRoundRect(Color(0xFF1D4ED8), Offset(w * 0.44f, w * 0.76f), Size(w * 0.12f, w * 0.14f), CornerRadius(w * 0.03f, w * 0.03f))
        drawCircle(Color(0xFFFCD9B6), radius = w * 0.045f, center = Offset(w * 0.5f, w * 0.72f))
        drawOval(Color(0xFFFDE68A), Offset(w * 0.4f, w * 0.66f), Size(w * 0.2f, w * 0.05f))
        drawRoundRect(Color(0xFF64748B), Offset(w * 0.56f, w * 0.74f), Size(w * 0.07f, w * 0.14f), CornerRadius(w * 0.02f, w * 0.02f))
    }
}
