package com.za.games.ui.about

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.za.games.R
import com.za.games.platform.Changelog
import com.za.games.platform.ZaLinks
import com.za.games.ui.common.GameTopBar

/** Uygulamada kullanılan açık kaynak bileşen; metinler yerelleştirilir. */
private class OssComponent(
    @StringRes val nameRes: Int,
    val license: String,
    @StringRes val descRes: Int,
    val url: String,
)

private val Components = listOf(
    OssComponent(R.string.about_lic_zemberek_name, "Apache-2.0", R.string.about_lic_zemberek_desc, ZaLinks.ZEMBEREK),
    OssComponent(R.string.about_lic_freq_name, "CC BY-SA 4.0", R.string.about_lic_freq_desc, ZaLinks.FREQUENCY_WORDS),
    OssComponent(R.string.about_lic_androidx_name, "Apache-2.0", R.string.about_lic_androidx_desc, ZaLinks.ANDROIDX),
    OssComponent(R.string.about_lic_kotlin_name, "Apache-2.0", R.string.about_lic_kotlin_desc, ZaLinks.KOTLIN),
)

/**
 * Hakkında: sürüm, gizlilik özeti, bağlantılar (site, kaynak kodu, sorun
 * bildirme, gizlilik politikası) ve açık kaynak lisansları. Uygulama ağa
 * çıkmaz; bağlantılar cihazın tarayıcısında açılır.
 */
@Composable
fun AboutScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val version = remember { Changelog.installedVersion(context) }
    var showApache by rememberSaveable { mutableStateOf(false) }
    val apacheText = remember(showApache) {
        if (showApache) context.resources.openRawResource(R.raw.license_apache2).bufferedReader().use { it.readText() } else ""
    }
    BackHandler { onExit() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        GameTopBar(title = stringResource(R.string.about_title), onExit = onExit)
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.about_version_fmt, version),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.hub_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(stringResource(R.string.chip_no_ads))
                    Chip(stringResource(R.string.chip_no_trackers))
                    Chip(stringResource(R.string.chip_no_permissions))
                }
            }
            item { InfoCard(stringResource(R.string.about_privacy_summary)) }

            item { SectionTitle(stringResource(R.string.about_links)) }
            item {
                LinkButton(stringResource(R.string.about_website), ZaLinks.SITE)
                LinkButton(stringResource(R.string.about_source), ZaLinks.SOURCE)
                LinkButton(stringResource(R.string.about_report), ZaLinks.REPORT)
                LinkButton(stringResource(R.string.about_privacy), ZaLinks.PRIVACY)
            }

            item { SectionTitle(stringResource(R.string.about_licenses)) }
            item {
                InfoCard(stringResource(R.string.about_app_license))
                TextButton(onClick = { ZaLinks.open(context, ZaLinks.LICENSE) }) {
                    Text(stringResource(R.string.about_app_license_link))
                }
            }
            item {
                Text(
                    text = stringResource(R.string.about_licenses_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                )
            }
            items(Components.size) { index -> ComponentCard(Components[index]) }
            item {
                Text(
                    text = stringResource(R.string.about_cc_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )
                TextButton(onClick = { ZaLinks.open(context, ZaLinks.CC_BY_SA) }) {
                    Text(ZaLinks.CC_BY_SA)
                }
            }
            item { SectionTitle(stringResource(R.string.about_release_notes)) }
            items(Changelog.entries.size) { index ->
                val note = Changelog.entries[index]
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    Text(
                        text = stringResource(R.string.whats_new_version_fmt, note.version, note.date),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    note.notes().forEach { line ->
                        Text(
                            text = "• $line",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        )
                    }
                }
            }
            item {
                OutlinedButton(onClick = { showApache = !showApache }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(if (showApache) R.string.about_license_hide else R.string.about_license_show))
                }
                if (showApache) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = apacheText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun InfoCard(text: String) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun Chip(label: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun LinkButton(label: String, url: String) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = { ZaLinks.open(context, url) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label)
            Text(
                text = url.removePrefix("https://"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun ComponentCard(component: OssComponent) {
    val context = LocalContext.current
    Surface(
        onClick = { ZaLinks.open(context, component.url) },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(component.nameRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = component.license,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = stringResource(component.descRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
            Text(
                text = component.url.removePrefix("https://"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            )
        }
    }
}
