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
 * Uzun ekranda panele bırakılacak pay: Satış'ın beş puan kuralının tamamı.
 *
 * [PANEL_MIN] bir **taban**, "içerik sığsın" güvencesi değil. Uzun ekranda o
 * taban hiç bağlamıyor, çünkü tepsi ağırlıksız ölçülüp doğal boyunu önce alıyor
 * ve panele artan kalıyor: 411 dp'de tepsi ürün adları sarınca iki sıra yerine
 * üç sıra oluyor (167 ↔ 219 dp) ve panel onunla 240 ↔ 208 dp arasında gidiyor.
 * 208 dp'de beşinci kuralın adı kırpılıyor — cihazda ölçüldü.
 *
 * 240 dp tahmin değil, ölçüm: v0.43.2'de tepsinin iki sıra kaldığı turlarda
 * panel tam bu boydaydı ve beş kuralın beşi de görünüyordu. v0.43.3'ün satırları
 * ~7 dp daha sıkı, yani payı var.
 */
internal val PANEL_WANT = 240.dp

/**
 * Tepsiye her hâlükârda bırakılan pay: başlık + iki sıra ürün (cihazda 167 dp).
 *
 * [PANEL_WANT] ancak bunun üstünde yer kalırsa uygulanıyor; altında kural
 * [PANEL_MIN]'e düşüyor, yani kısa ekranda yerleşim **birebir eskisi gibi**
 * kalıyor. Ayrım `rest` 308 dp'yi geçince başlıyor.
 */
internal val TRAY_KEEP = 168.dp

/**
 * Tepsinin tavanı: panel ile tepsiye kalan [rest] yükseklikten panele
 * [panelWant] bırakacak kadar — ama tepsiye [TRAY_KEEP] kalıyorsa. Kalmıyorsa
 * pay [PANEL_MIN]'e iniyor. Doğal yükseklik tavanın altında kalırsa tavan
 * bağlamıyor, taşarsa tepsi kendi içinde kayıyor.
 *
 * Varsayılan [panelWant] = [PANEL_MIN] olduğu için çağıranların davranışı
 * değişmiyor; payı yalnız Satış yükseltiyor, kuralları oradaki panel taşıyor.
 */
internal fun trayHeight(rest: Dp, panelWant: Dp = PANEL_MIN): Dp {
    val pay = minOf(panelWant, rest - TRAY_KEEP).coerceAtLeast(PANEL_MIN)
    return (rest - pay).coerceAtLeast(TRAY_MIN)
}
