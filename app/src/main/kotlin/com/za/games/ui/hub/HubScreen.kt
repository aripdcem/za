package com.za.games.ui.hub

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.za.games.R
import com.za.games.platform.GameEntry
import java.util.Locale

/** Ana menü: platform manifestosu ve oyun listesi. */
@Composable
fun HubScreen(
    games: List<GameEntry>,
    highScores: Map<String, Long>,
    onPlay: (GameEntry) -> Unit,
    soundOn: Boolean = true,
    onToggleSound: () -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { HubHeader(soundOn, onToggleSound) }
        items(games, key = { it.id }) { game ->
            GameCard(
                game = game,
                highScore = highScores[game.id] ?: 0L,
                onPlay = { onPlay(game) },
            )
        }
        item { ComingSoonCard() }
    }
}

@Composable
private fun HubHeader(soundOn: Boolean, onToggleSound: () -> Unit) {
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
            val toggleDescription = stringResource(
                if (soundOn) R.string.sound_off else R.string.sound_on,
            )
            IconButton(
                onClick = onToggleSound,
                modifier = Modifier.semantics { contentDescription = toggleDescription },
            ) {
                Text(text = if (soundOn) "🔊" else "🔇", fontSize = 22.sp)
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
