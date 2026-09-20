package com.aripd.zagames.ui.reyon

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reyon ekranlarının kısa telefon yerleşimi.
 *
 * Üç tür de aynı iskelete oturuyor: üstte raf tuvali, altında kaydırılabilir
 * bir panel (Diziliş'te planogram brifi, Satış'ta puan kuralları, Sipariş'te
 * sipariş listesi), en altta tepsi. Tuval yalnız genişlikten ölçülüyordu — en
 * boy oranı yüksekliği belirliyordu — ve sütunda ağırlıksız olduğu için
 * yüksekliği önce o alıyordu. 360×640 dp bir telefonda tuval ile tepsi
 * yüksekliğin tamamını yiyor, panele ~0 kalıyordu: v0.43.1 cihaz koşumunda
 * Diziliş'in brifi hiç çizilmedi (kural satırı ≈13 dp, ikinci kuralın
 * erişilebilirlik kutusu sıfır) ve brif olmadan bulmaca çözülemediği için mod
 * o ekranda oynanamaz durumdaydı.
 *
 * Kural: tuval oyun alanının en çok [SHELF_SHARE] payını alır, tepsi de panele
 * [PANEL_MIN] kalacak kadar. İki tavan da uzun telefonda doğal yüksekliklerin
 * üstünde kaldığı için orada yerleşim aynen sürer; yalnız kısa ekranda devreye
 * girer.
 *
 * İki tavan da `BoxWithConstraints`'in içinde, ama `Column`'un dışında
 * hesaplanmalı: `ColumnScope` da `@LayoutScopeMarker` taşıdığı için sütunun
 * içinde `BoxWithConstraintsScope` örtülüyor ve maxWidth/maxHeight örtük
 * alıcıyla okunamıyor (derleme hatası).
 */
internal const val SHELF_SHARE = 0.34f

/** Panele bırakılan taban: başlık + üç kural satırı. */
internal val PANEL_MIN = 120.dp

/** Tepsinin tabanı; bundan aşağısında ürün seçmek zorlaşır. */
private val TRAY_MIN = 96.dp

/**
 * Raf tuvalinin yüksekliği: en boy oranının istediği kadar, ama oyun alanının
 * [SHELF_SHARE] payını geçmeden.
 *
 * Tavan neden en boy oranı değiştirilerek uygulanıyor: `fillMaxWidth()`
 * genişliği sabitliyor, bu yüzden `heightIn(max = ...)` ile `aspectRatio(...)`
 * birlikte çalışmıyor — oran hiçbir boyutu kısıtı sağlayacak şekilde
 * bulamayınca kısıtı yok sayıp yine genişlikten hesaplıyor. Tuval tam
 * genişlikte kalsın istediğimiz için tavan yüksekliğe uygulanıyor: kısa ekranda
 * gözler basıklaşıyor, ama çizim de dokunma da tuvalin ölçülen boyutundan
 * türediği (`ShelfGeom(size.width, size.height, …)`) için eşleme bozulmuyor.
 */
internal fun shelfHeight(width: Dp, available: Dp, ratio: Float): Dp =
    minOf(width / ratio, available * SHELF_SHARE)

/**
 * Tepsinin tavanı: rafın altında panele [PANEL_MIN] kalacak kadar; taşan tepsi
 * kendi içinde kayar. Uzun telefonda doğal yükseklik bu tavanın altında kaldığı
 * için bağlamaz.
 */
internal fun trayHeight(available: Dp, shelf: Dp): Dp =
    (available - shelf - PANEL_MIN).coerceAtLeast(TRAY_MIN)
