package com.za.games.ui.common

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.za.games.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

fun formatScore(value: Long): String =
    String.format(Locale.getDefault(), "%,d", value)

fun formatTime(totalSeconds: Int): String =
    String.format(Locale.getDefault(), "%d:%02d", totalSeconds / 60, totalSeconds % 60)

/** Oyun ekranlarının ortak üst çubuğu: geri, başlık, sağda oyuna özel aksiyon. */
@Composable
fun GameTopBar(
    title: String,
    onExit: () -> Unit,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onExit) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
            )
        }
        Text(
            text = title.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            letterSpacing = 3.sp,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

/** Skor/rekor gibi değerler için kart. */
@Composable
fun ScoreCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (highlight) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

/**
 * Oyun tuşu. [repeatIntervalMs] verilirse basılı tutuldukça tekrarlar
 * (ilk tekrar 220 ms sonra başlar).
 */
@Composable
fun PadButton(
    label: String,
    description: String,
    modifier: Modifier = Modifier,
    repeatIntervalMs: Long = 0L,
    accent: Boolean = false,
    fontSize: TextUnit = 22.sp,
    onAction: () -> Unit,
) {
    val currentAction by rememberUpdatedState(onAction)
    val interactionSource = remember { MutableInteractionSource() }
    val repeatable = repeatIntervalMs > 0L

    if (repeatable) {
        val pressed by interactionSource.collectIsPressedAsState()
        LaunchedEffect(pressed) {
            if (pressed) {
                currentAction()
                delay(220L)
                while (isActive) {
                    currentAction()
                    delay(repeatIntervalMs)
                }
            }
        }
    }

    Surface(
        onClick = { if (!repeatable) currentAction() },
        modifier = modifier.semantics { contentDescription = description },
        shape = RoundedCornerShape(16.dp),
        color = if (accent) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        interactionSource = interactionSource,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = fontSize,
                color = if (accent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
fun OverlayCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        // Saydam yüzey rengi şemadaki hiçbir renkle eşleşmediğinden içerik rengi
        // kendiliğinden türetilemez; başlıklar için açıkça verilir.
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            content()
        }
    }
}

@Composable
fun PausedOverlay(onResume: () -> Unit, onRestart: () -> Unit, onExit: () -> Unit) {
    OverlayCard {
        Text(
            text = stringResource(R.string.paused),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onResume, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.resume))
        }
        OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.restart))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}

/**
 * Zorluk seçimi: 0 = kolay, 1 = orta, 2 = zor. [descriptions] verilirse her
 * düğmenin altında küçük bir açıklama gösterilir (örn. ipucu/mayın sayısı).
 */
@Composable
fun DifficultyOverlay(descriptions: List<String> = emptyList(), onPick: (Int) -> Unit) {
    OverlayCard {
        Text(
            text = stringResource(R.string.pick_difficulty),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = { onPick(0) }, modifier = Modifier.fillMaxWidth()) {
            DifficultyButtonContent(stringResource(R.string.difficulty_easy), descriptions.getOrNull(0))
        }
        Button(onClick = { onPick(1) }, modifier = Modifier.fillMaxWidth()) {
            DifficultyButtonContent(stringResource(R.string.difficulty_medium), descriptions.getOrNull(1))
        }
        Button(onClick = { onPick(2) }, modifier = Modifier.fillMaxWidth()) {
            DifficultyButtonContent(stringResource(R.string.difficulty_hard), descriptions.getOrNull(2))
        }
    }
}

@Composable
private fun DifficultyButtonContent(label: String, description: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label)
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = LocalContentColor.current.copy(alpha = 0.75f),
            )
        }
    }
}

@Composable
fun GameOverOverlay(
    score: Long,
    isRecord: Boolean,
    onRestart: () -> Unit,
    onExit: () -> Unit,
) {
    OverlayCard {
        Text(
            text = stringResource(R.string.game_over),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = formatScore(score),
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
        Spacer(Modifier.height(4.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.restart))
        }
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exit_to_hub))
        }
    }
}
