package com.aripd.zagames.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aripd.zagames.platform.WordLangs
import com.aripd.zagames.sozluk.WordLang

/**
 * Ekranın o anki kelime dili.
 *
 * Dört kelime oyununun ekranında dil derinlere kadar gerekiyor: klavye sırası,
 * harf büyütme, taş etiketleri. Her bileşene parametre olarak taşımak yerine
 * ekranın kökünde bir kez sağlanır. Varsayılanı İngilizce, çünkü sağlayıcı
 * kurulmadan çizilen bir önizleme de makul bir alfabe görsün.
 */
val LocalWordLang = staticCompositionLocalOf { WordLang.EN }

/**
 * [LocalWordLang]'e konacak değer: oyuncunun seçimi, yoksa arayüzün dili.
 *
 * İki kaynağı da izler. Kelime dili oyun içindeki seçiciden değişince
 * [WordLangs.selection] yayar; arayüz dili değişince çağıran `keys` ile
 * haber verir. İkincisi tek başınayken sözlük yeni dile geçiyor ama klavye
 * eskisinde kalıyordu (v0.43.0 cihaz koşumu, F1) — bu yüzden bağlantı tek
 * yerde durur ve testi de buradan geçer.
 */
@Composable
fun rememberWordLang(vararg keys: Any?): WordLang {
    val context = LocalContext.current
    val chosen by WordLangs.selection.collectAsStateWithLifecycle()
    return remember(chosen, *keys) { WordLangs.current(context) }
}
