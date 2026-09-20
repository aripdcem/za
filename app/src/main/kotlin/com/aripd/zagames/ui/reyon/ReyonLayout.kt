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
 * o ekranda oynanamaz durumdaydı. Aynı açlık Satış'ta da vardı: beş puan
 * kuralından ikisi görünüyordu (Robolectric ölçümü).
 *
 * Kural iki adımda:
 *
 *  1. Tuval oyun alanının en çok [SHELF_SHARE] payını alır ([shelfHeight]).
 *  2. Kalan yükseklik panel ile tepsi arasında bölünür: tepsi, panele
 *     [PANEL_MIN] bırakacak kadar yer alır ([trayHeight]), taşan kısım kendi
 *     içinde kayar. "Kalan" tahmin edilmiyor, ölçülüyor: panel ile tepsi kendi
 *     `BoxWithConstraints`'inin içinde duruyor, böylece aradaki ipucu/döküm
 *     satırı gibi değişken yükseklikler hesaba kendiliğinden giriyor.
 *
 * Böylece panel en az [PANEL_MIN] yüksekliğinde oluyor — başlık + üç satır.
 * Uzun telefonda iki tavan da doğal yüksekliklerin üstünde kaldığı için orada
 * yerleşim aynen sürüyor.
 *
 * Tek istisna, kalanın iki tabana birden yetmediği çok kısa ekran: orada tepsi
 * kendi tabanını ([TRAY_MIN]) korur ve panele kalan ne varsa o düşer. Ölçülen
 * örnek: 480 dp'lik uygulama alanında kalan 177,5 dp, tepsi 72 dp, panel 105,5
 * dp — dört kuraldan üçü görünüyor, dördüncüsü kaydırmayla geliyor. İki taban
 * aynı yükseklikten beslendiği için biri artınca öbürü azalıyor; ürün
 * seçilemeyen bir tepsi de bulmacayı çözülemez yapardı.
 *
 * Tavanlar `BoxWithConstraints`'in içinde, ama `Column`'un dışında hesaplanmalı:
 * `ColumnScope` da `@LayoutScopeMarker` taşıdığı için sütunun içinde
 * `BoxWithConstraintsScope` örtülüyor ve maxWidth/maxHeight örtük alıcıyla
 * okunamıyor (derleme hatası).
 */
internal const val SHELF_SHARE = 0.40f

/** Panele bırakılan taban: başlık + üç satır. */
internal val PANEL_MIN = 140.dp

/** Tepsinin tabanı: başlık + bir sıra ürün. Ürün seçilemezse bulmaca çözülemez. */
internal val TRAY_MIN = 72.dp

/** Panelin ölçüm etiketi; yerleşim garantisi testte bundan okunuyor. */
const val REYON_PANEL_TAG = "reyon_panel"

/** Tepsinin ölçüm etiketi; panelin payı tepsinin tabanına bağlı olduğu için gerekli. */
const val REYON_TRAY_TAG = "reyon_tray"

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
 * Tepsinin tavanı: panel ile tepsiye kalan [rest] yükseklikten panele
 * [PANEL_MIN] bırakacak kadar. Doğal yükseklik bunun altında kalırsa
 * bağlamıyor, yani uzun telefonda tepsi eskisi gibi tam görünüyor.
 */
internal fun trayHeight(rest: Dp): Dp = (rest - PANEL_MIN).coerceAtLeast(TRAY_MIN)
