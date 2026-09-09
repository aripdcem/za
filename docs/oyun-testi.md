# Oyun test protokolü

Motor birim testleri **kuralların doğru** olduğunu gösterir; oyunun **oynanabilir**
olduğunu göstermez. Bir motor, testleri tam geçerken cihazda ulaşılmaz derecede
zor, kontrolü tepkisiz ya da kare hızı düşük olabilir — çünkü birim testleri
parmağı, ekranı ve gerçek işlemciyi bilmez.

Bu belge, her oyunun yayına girmeden önce geçmesi gereken dört aşamayı tanımlar.

> **Kural:** Yeni bir oyun eklendiğinde ya da bir oyunun dengesi/kontrolü
> değiştiğinde A–D aşamaları koşulur ve sonuçları [Sonuç kütüğü](#sonuç-kütüğü)
> bölümüne işlenir. Motor testleri yeşil diye bu adımlar atlanmaz.

## Neden

Filo (v0.24.0) 41 birim testiyle yeşil olarak yayına hazırdı. Cihaz üstü ilk
ölçümde üç sorun çıktı: silah 3 patronlara karşı silah 2'den yavaş, her parmak
basışında 8 dp ölü bölge, ve orta seviye bir telefonda hedeflenen 60 kare/s
yerine ~34. Hiçbiri birim testiyle görünmezdi.

Ölçümün kendisi de ders verdi: kare hızı sorunu önce Filo'nun çizim koduna
yazıldı, `framestats` sütunları doğru eşlenince maliyetin GPU tarafında ve
**uygulama geneli** olduğu çıktı (bkz. [Açık konu](#açık-konu-uygulama-geneli-kare-hızı)).
Tahmine göre değil, faz dökümüne göre düzeltin.

## Gereksinimler

- USB hata ayıklama açık bir Android cihaz (`adb devices` ile görünmeli)
- `ANDROID_HOME` ya da `~/Android/Sdk`
- Python: `Pillow`, `numpy`

Ölçüm aracı: `tools/cihaz_testi.py`.

---

## A · Cihaz koşumu

Amaç: oyun gerçekten açılıyor, oynanıyor ve çökmüyor mu?

```bash
ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:installDebug
adb shell am start -n com.za.games/.MainActivity
adb logcat -c && adb logcat AndroidRuntime:E '*:S'   # ayrı bir kabukta
```

Kontrol listesi:

- [ ] Ana menüden oyun açılıyor
- [ ] Bir tur baştan sona oynanıyor; bitiş kartı ve skor doğru
- [ ] Duraklat/devam, geri tuşu, arka plana alıp geri dönme çalışıyor
- [ ] `logcat` boş (istisna yok)
- [ ] Günlük mod deneme hakkı doğru azalıyor

> Ölçüm yaparken **Serbest** modu kullanın: günlük modun günde üç deneme hakkı
> vardır ve ölçüm koşumları hakları tüketir.

## B · Kare hızı

Amaç: oyun 60 kare/s hedefini tutuyor mu?

Oyun **oynanırken** (duraklatılmış ya da bitmiş değil):

```bash
python3 tools/cihaz_testi.py kare --sure 15
```

Eşikler:

| Ölçüt | Hedef | Uyarı |
| --- | --- | --- |
| Ortanca kare süresi | ≤ 16 ms | > 20 ms |
| Takılan kare oranı | < %5 | > %20 |
| 90. yüzdelik | ≤ 20 ms | > 32 ms |

Notlar:

- **Sürüm (release) derlemesiyle ölçün.** Hata ayıklama derlemesinde Compose
  belirgin biçimde yavaştır; ama tersi de doğru değil — Filo'da sürüm derlemesi
  hata ayıklamadan daha iyi çıkmadı, yani sorun gerçek.
- `Total frames rendered: 0` çıkarsa oyun çizmiyordur (tur bitmiş olabilir):
  ölçüm geçersizdir, turu yeniden başlatıp tekrarlayın.
- Ölçüm penceresinde ekran görüntüsü almayın; `screencap` kare süresini bozar.

Toplam yavaşsa **nerede** yavaş olduğunu sorun:

```bash
python3 tools/cihaz_testi.py fazlar
```

| Yüksek faz | Anlamı | Nereye bakılır |
| --- | --- | --- |
| çizim kaydı (CPU) | çizim kodu pahalı | `drawScene`, kare başına ayırma, metin ölçümü |
| GPU | dolgu/aşırı çizim pahalı | tam ekran katmanlar, alfa karışımı, `debug.hwui.overdraw show` |
| ölçüm/yerleşim | gereksiz yeniden besteleme | her karede değişen `State` okumaları |
| girdi→traversal | ana iş parçacığı tıkalı | kare döngüsündeki iş |

> **Tuzak:** `framestats` sütun düzeni ROM'a göre değişir (bu cihazda 14 değil
> 23 sütun). Sabit sütun numarası varsaymak tamamen yanlış sonuç verir —
> `tools/cihaz_testi.py fazlar` eşlemeyi başlık satırından çıkarır. İlk
> ölçümde "çizim kaydı 31 ms" görünmüştü; doğru eşlemede gerçek değer 1,3 ms
> ve darboğaz GPU'daydı.

Sürüm derlemesini cihaza atmak için (CI imzası olmadan):

```bash
ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleRelease
~/Android/Sdk/build-tools/35.0.0/apksigner sign \
  --ks ~/.android/debug.keystore --ks-pass pass:android --key-pass pass:android \
  --out /tmp/za-release.apk app/build/outputs/apk/release/app-release-unsigned.apk
adb install -r /tmp/za-release.apk
```

## C · Giriş kalibrasyonu

Amaç: parmak hareketi ekrandaki nesneye ne kadar sadık aktarılıyor?

Önce oyun alanının piksel karşılığını bulun, sonra sürüklemeyi tarayın:

```bash
python3 tools/cihaz_testi.py alan
python3 tools/cihaz_testi.py surukle --y 1500 --y0 1935 --y1 2035 \
                                     --mesafeler 20,40,80,160 --tekrar 3
```

`--y0/--y1`, oyuncu nesnesinin bulunduğu yatay bant (ekran görüntüsünden okuyun).
Araç bu banttaki **en geniş bitişik parlak küme**yi nesne kabul eder; yıldız ve
kıvılcım gibi küçük noktalar elenir.

Bakılacaklar:

- **Ölü bölge:** hareketi sıfır çıkan en büyük parmak yolu. Dokunma toleransı
  (touch slop) her parmak basışında bir kez ödenir; 8 dp tipiktir.
- **Oran:** ölü bölge çıkarıldıktan sonra hareket parmak yoluna eşit olmalı
  (1:1) ya da bilinçli bir katsayı uygulanmalı.
- **Uçtan uca maliyet:** oyun alanını baştan sona geçmek kaç mm parmak yolu
  istiyor? 55 mm'nin üzeri, tek elle oynarken parmağı kaldırıp yeniden basmayı
  zorunlu kılar — ve her yeniden basış ölü bölgeyi tekrar ödetir.

Milimetre karşılığı: `mm = px / yoğunluk * 25.4` (yoğunluk: `adb shell wm density`).

Tuzak: vuruş anındaki **ekran sarsıntısı** nesneyi olduğu yerden kaydırıp
ölçümü bozar. Bu yüzden her mesafe en az 3 kez denenip **medyan** alınır.

## D · Denge ölçümü

Amaç: zorluk eğrisi, ödül dengesi ve "yetişilebilirlik" sayısal olarak doğru mu?

Motor saf Kotlin ve deterministik olduğu için denge, cihaz olmadan ve
tekrarlanabilir biçimde ölçülebilir. Her oyun modülünde bir ölçüm koşumu bulunur:

```bash
ANDROID_HOME=$HOME/Android/Sdk ./gradlew :games:filo:probe
```

Ölçüm koşumları `*Probe` adını taşır. Bunlar birim testi **değildir**: geçme/kalma
yerine rapor basarlar, yavaştırlar ve CI'daki `test` görevinden dışlanırlar.
Yeni bir oyuna eklerken modülün `build.gradle.kts` dosyasına:

```kotlin
tasks.test {
    useJUnit()
    filter { excludeTestsMatching("*Probe") }
}

tasks.register<Test>("probe") {
    description = "Denge ölçüm koşumunu çalıştırır ve raporu basar."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnit()
    filter { includeTestsMatching("*Probe") }
    outputs.upToDateWhen { false }
    testLogging { showStandardStreams = true }
}
```

### Bot gerçekçi olmalı

Denge ölçümünün kalbi, oyuncuyu **insan sınırlarıyla** taklit eden bir bottur.
Motor gemiyi anında ışınlayabilir; parmak ışınlanamaz. Bot en az şunları
taşımalı:

- **azami hareket hızı** (mm/s cinsinden ölçülüp oyun birimine çevrilir)
- **tepki gecikmesi** (150–350 ms)
- yalnızca ekranda **görünen** bilgiyi kullanma (motorun içini okumak yerine
  düşman hızını kareler arası farktan kestirmek)

Örnek: `games/filo/src/test/kotlin/com/za/games/filo/FiloBalanceProbe.kt`.

> Botun sonucu bir **alt sınırdır**, tavan değil: iyi bir insan daha ileri gider.
> "Bot 11. dalgada ölüyor" tek başına "oyun çok zor" demek değildir. Ama botun
> beceri ayarını değiştirip sonucun nasıl kaydığına bakmak, zorluğun nereden
> geldiğini gösterir. Bot kalitesine bağlı olmayan ölçümleri (öldürme süresi,
> düşman hızı, mermi iniş süresi) ayrıca raporlayın.

### Her oyunda bakılacaklar

| Ölçüt | Soru |
| --- | --- |
| İlerleme | Bot ortalama nereye kadar gidiyor? İlk ölüm nerede? |
| Zorluk eğrisi | Zorluk kademeli mi, yoksa bir yerde duvara mı çarpıyor? |
| Öldürme süresi | Patron/hedef canı ile oyuncunun hasarı orantılı mı? |
| Yükseltme değeri | **Her yükseltme bir öncekinden iyi mi?** |
| Yetişilebilirlik | Tehdide tepki için kalan süre, insan tepki süresinden uzun mu? |
| Yoğunluk | Aynı anda kaç düşman/mermi? Kaçacak yer kalıyor mu? |

---

## Sonuç kütüğü

| Oyun | A koşum | B kare | C giriş | D denge | Tarih |
| --- | --- | --- | --- | --- | --- |
| Filo | ✅ | ⚠️ ~34 kare/s (uygulama geneli) | ✅ düzeltildi | ✅ düzeltildi | 2026-09-09 |
| Blok, 2048, Yılan, Sudoku, Mayın Tarlası, Beş Harf, Kıskaç, Türetme, Dizgi, Kuyu, Geçit, Tavla, Balkon, Kakuro, Vergici, Toplam Kapma, Viraj | — | — | — | — | bekliyor |

### Filo · 2026-09-09 (SM-A515F, Android 13, 1080×2400 @420 dpi)

Oyun alanı ölçeği: 1 birim = 1016 px = 61,4 mm (ekranın tamamı değil).

**A — koşum:** açıldı, oynandı, çökme yok. Ölçüm sırasında kaydedilen sapma:
oyun bittiğinde çizim döngüsü durduğu için `gfxinfo` sıfır kare gösterir.

**B — kare hızı (sürüm derlemesi, 15 s oyun içi):**

| | ölçülen | hedef |
| --- | --- | --- |
| Ortanca kare | 29 ms | ≤ 16 ms |
| 90. yüzdelik | 61 ms | ≤ 20 ms |
| Takılan kare | %62,8 | < %5 |
| GPU ortanca | 12 ms | — |

18 ms'nin altında **tek bir kare bile** yok. GPU süresi toplamın yarısından az;
darboğaz çizim kaydında (CPU). Hata ayıklama derlemesi ~40 kare/s, sürüm ~34:
sorun derleme türünden bağımsız.

**C — sürükleme (düzeltme öncesi):** `detectDragGestures` + 1:1, yumuşatma yok
(`playerX = targetX`, hız sınırı yok).

| Parmak yolu | Gemi hareketi | Kayıp |
| --- | --- | --- |
| 20 px | **0,0 px** (3/3 deneme) | 20 px |
| 40 px | 17,3 px | 22,7 px |
| 80 px | 46,0 px | 34,0 px |
| 160 px | 138,8 px | 21,2 px |

Kayıp sabit ~21 px = **8 dp = 1,3 mm**: dokunma toleransı, her parmak basışında
bir kez. 1,3 mm'den kısa düzeltmeler hiçbir şey yapmaz. Kenardan kenara
(0,90 birim) **55,3 mm** parmak yolu gerekir; tek elle başparmak bunu tek
hamlede yapamaz, yeniden basış gerekir ve her basış ölü bölgeyi tekrar ödetir.
En dar kaçış (gemi + mermi yarıçapı) 3,3 mm — ölü bölgenin yalnızca 2,5 katı.

**C — düzeltme sonrası:** `detectDragGestures` yerine olaylar doğrudan okunuyor
(dokunma toleransı beklenmiyor) ve `DRAG_GAIN = 1,35` uygulanıyor.

| Parmak yolu | Önce | Sonra |
| --- | --- | --- |
| 20 px | **0,0 px** | 25,2 px |
| 40 px | 17,3 px | 53,4 px |
| 80 px | 46,0 px | 103,3 px |
| 160 px | 138,8 px | 212,1 px |

Ölü bölge kalktı; oran 1,29–1,34 (hedef 1,35). Kenardan kenara maliyet
55,3 mm → **41,0 mm**: tek başparmak hamlesiyle geçilebilir. Birebir his
istenirse `FiloScreen.DRAG_GAIN` 1,0 yapılır.

**D — denge** (`./gradlew :games:filo:probe`):

Patron canı `30 + 15×(n/5 − 1) + ⌊10·d⌋`; oyuncu hasarı sabit (mermi başına 1,
0,17 s'de bir atış). Patronu kusursuz takip eden, hiç kaçınmayan ölçüm:

| Dalga | Can | Silah 1 | Silah 2 | Silah 3 |
| --- | --- | --- | --- | --- |
| 5 | 31 | 7,3 s (%68) | **4,1 s** (%60) | 6,7 s (%25) |
| 10 | 48 | 10,9 s (%72) | **6,3 s** (%62) | 10,1 s (%26) |
| 20 | 82 | 18,4 s (%74) | **10,1 s** (%68) | 17,2 s (%26) |
| 30 | 115 | 25,4 s (%76) | **13,6 s** (%70) | 24,5 s (%26) |

**Silah 3, patronlara karşı silah 2'den ~1,8 kat yavaş** ve silah 1'e denk.
Sebep: silah 3'ün iki yan mermisi `vx = ±0,5` ile açılı gider; oyuncudan patrona
1,2 birimlik yolda yanal kayma ≈ 0,23 birim, patron yarıçapı ise 0,11 — yan
mermiler ıskalar, isabet oranı 1/3'e düşer. Silah 2'nin iki paralel mermisi
(0,044 aralık) patron diskinin içinde kalır, ikisi de isabet eder. Sonuç:
silah 2'deyken silah kutusu toplamak patron hasarını **düşürür**.

**D — düzeltme sonrası:** `SPREAD_VX` 0,5 → 0,15 (yan mermilerin patron
menzilindeki kayması 0,23 → 0,09 birim; patron yarıçapı 0,122).

| Dalga | Can | Silah 1 | Silah 2 | Silah 3 (önce) | Silah 3 (sonra) |
| --- | --- | --- | --- | --- | --- |
| 5 | 31 | 7,3 s | 4,1 s | 6,7 s | **3,1 s** |
| 20 | 82 | 18,4 s | 10,1 s | 17,2 s | **7,5 s** |
| 30 | 115 | 25,4 s | 13,6 s | 24,5 s | **10,6 s** |

İsabet oranı %26 → %60; yükseltme artık her seviyede kazanç.
`FiloWorldTest.silahYukseltmesiPatronHasariniDusurmez` bunu kalıcı olarak
korur (SPREAD_VX 0,5'e döndürülünce test kırmızıya döner).

Diğer ölçümler: düşman hızları 1. dalgadan 26'ya %60 artıyor (DIVE 0,42 → 0,70
birim/s; oyuncuya varış 3,6 s → 2,1 s) — tepki için yeterli. Yakın menzilden
atılan mermide 26. dalgada 0,40 s kalıyor; 350 ms tepkiyle acemi oyuncunun payı
4,9 mm, gereken 3,3 mm — sınırda ama mümkün. Dalga yoğunluğu 9 düşmandan
(1. dalga) 18'e (32. dalga) çıkıyor; tepe düşman mermisi 3 → 6.

İnsan sınırlı bot (10 tohum): ortalama 10–11. dalga, ilk ölüm 5–7. dalga.
Bot bir alt sınırdır.

## Açık konu: uygulama geneli kare hızı

Filo'nun çizimi **suçlu değil**. Faz dökümü (sürüm derlemesi, oyun içi):

| Faz | Ortanca |
| --- | --- |
| çizim kaydı (CPU) | 1,3 ms |
| ölçüm/yerleşim | 0,2 ms |
| komut→swap | 6,7 ms |
| **GPU** | **17,0 ms** |
| toplam | 32,7 ms |

Doğrulama olarak tam ekran gradyan **ve** 70 yıldızın ikisi birden çizimden
çıkarıldı: takılma %62,8 → %52,3, GPU 12 → 11 ms, yani ölçülebilir kazanç yok.
Aynı ölçüm **Viraj**'da da benzer çıkıyor (%73 takılma, ortanca 32 ms, GPU
12 ms). Cihazda güç tasarrufu kapalı, ekran 60 Hz, termal kısıt yok.

Sonuç: ~30 kare/s tavanı tek bir oyunun çizim kodundan değil, uygulama
kabuğundan ya da bu cihaz sınıfının GPU dolgu kapasitesinden geliyor. Kare
başına toplam iş (~19 ms) 16,7 ms bütçesini biraz aşıyor ve kare hızı yarıya
düşüyor. Bu, tek tek oyunlarda değil **uygulama düzeyinde** ele alınmalı:
pencerenin tamamının aşırı çizimi, Material yüzeylerin katman maliyeti ve
sprite'ların kenar yumuşatma yükü ölçülmeli (Perfetto / Mali profil).

Bu koşumda uygulanan tek çizim değişikliği: arka plan gradyanı artık her
karede yeniden kurulmuyor (`remember`); ölçülebilir kazanç vermedi ama kare
başına gölgelendirici ayırmayı kaldırdığı için tutuldu. Ölçümle kazanç
göstermeyen kırpma/yeniden yapılandırma denemeleri geri alındı.
