package com.aripd.zagames.ui.common

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aripd.zagames.R
import com.aripd.zagames.platform.ZaLocale
import com.aripd.zagames.sozluk.WordLang

/** Testlerin kelime dili listesini bulması için. */
const val WORD_LANG_LIST_TAG = "word_lang_list"

/**
 * Kelime oyunlarının dil seçimi.
 *
 * Arayüzün dilinden ayrıdır: Almanya'daki bir oyuncu uygulamayı Almanca
 * kullanıp Beş Harf'i Türkçe oynayabilsin diye. İlk satır "arayüzün dili"dir
 * ve hangi dile düşüldüğünü alt satırda yazar — arayüz dilinin kelime listesi
 * yoksa oyun İngilizce açılır.
 *
 * Seçim oyunu o dilde yeniden kurar; ekran seçimden sonra kapanır.
 *
 * [selected] oyuncunun açık seçimi (null = arayüzün dilini izle),
 * [effective] o an oynanan dil.
 */
@Composable
fun WordLangScreen(
    selected: WordLang?,
    effective: WordLang,
    onPick: (WordLang?) -> Unit,
    onExit: () -> Unit,
) {
    BackHandler { onExit() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        GameTopBar(title = stringResource(R.string.word_lang_title), onExit = onExit)
        LazyColumn(
            modifier = Modifier.testTag(WORD_LANG_LIST_TAG),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                WordLangRow(
                    label = stringResource(R.string.word_lang_follow),
                    detail = if (selected == null) ZaLocale.endonym(effective.tag) else null,
                    checked = selected == null,
                    onClick = { onPick(null) },
                )
            }
            items(WordLang.entries) { lang ->
                WordLangRow(
                    label = ZaLocale.endonym(lang.tag),
                    detail = null,
                    checked = selected == lang,
                    onClick = { onPick(lang) },
                )
            }
            item {
                Text(
                    text = stringResource(R.string.word_lang_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun WordLangRow(label: String, detail: String?, checked: Boolean, onClick: () -> Unit) {
    val state = stringResource(if (checked) R.string.language_selected else R.string.language_select)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (checked) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$label · $state" },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal,
                )
                if (detail != null) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Kurulum kartındaki dil düğmesi: o an oynanan dili gösterir, dokununca
 * [WordLangScreen] açılır.
 */
@Composable
fun WordLangChip(lang: WordLang, onClick: () -> Unit) {
    val label = ZaLocale.endonym(lang.tag)
    val description = stringResource(R.string.word_lang_button, label)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}
