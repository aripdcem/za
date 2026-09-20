package com.aripd.zagames.ui.common

import androidx.compose.runtime.staticCompositionLocalOf
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
