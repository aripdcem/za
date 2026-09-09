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

Filo (v0.24.0) 41 birim testiyle yeşil olarak yayına hazırdı. Cihaz üstünde iki
gerçek sorun çıktı: silah 3 patronlara karşı silah 2'den yavaştı ve her parmak
basışında 8 dp ölü bölge vardı. İkisi de birim testiyle görünmezdi.

Aynı koşumda **yanlış bir bulgu da üretildi** ve düzeltmesi belgenin en değerli
parçası oldu. Önce "orta seviye telefonda 60 yerine ~34 kare/s" denmişti; oysa:

- `~34` rakamı kare sayısından değil, **`p50` kare gecikmesinden** türetilmişti.
  Gecikme kare periyodu değildir: boru hattı derinleştikçe kare süresi 30 ms
  görünürken oyun 60 kare/s akmaya devam eder.
- Ölçüm pencereleri **koşu bitince** çizim durduğu için kirlenmişti; ortalama
  düşük çıkıyordu.
- `framestats` sütunları klasik 14 sütunlu düzene göre eşlenmişti; bu ROM 23
  sütun kullanıyor, dolayısıyla "çizim kaydı 31 ms" tamamen uydurmaydı.

Doğru ölçümde 18 oyunun tamamı 60 kare/s tutuyor ve kare düşürmüyor
(bkz. [Sonuç kütüğü](#sonuç-kütüğü)). Ders: **ölçtüğünüz sayının ne olduğunu
doğrulayın**, ve bir bulguyu koda yazmadan önce bulgunun kendisini sınayın.

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

```bash
python3 tools/cihaz_testi.py tarama          # tüm oyunlar
python3 tools/cihaz_testi.py kare --sure 15  # ön plandaki oyun
```

**Önce oyunun türünü belirleyin.** ZA oyunlarının çoğu olay güdümlüdür: boşta
hiç çizmezler (2048, Sudoku, Mayın, Beş Harf, Kıskaç, Türetme, Dizgi, Tavla,
Kakuro, Vergici, Toplam Kapma) ya da saniyede bir çizerler (Blok'ta yerçekimi
tik'i). Onlarda boşta ölçülen 0 kare **beklenen sonuçtur, başarısızlık değil**;
kare hızı ancak etkileşim sırasında anlamlıdır. Sürekli çizenler: Yılan, Kuyu,
Geçit, Balkon, Viraj, Filo.

Eşikler (yalnızca sürekli çizen oyunlar için):

| Ölçüt | Nasıl okunur | Hedef |
| --- | --- | --- |
| **kare/s** = kare ÷ pencere | asıl kare hızı | ≥ 58 |
| **Number Missed Vsync** | gerçekten düşen kare | ~0 |
| Janky frames % | kare **gecikmesi**, düşen kare değil | bilgi amaçlı |
| 50. yüzdelik | kare başına uçtan uca gecikme | ≤ ~25 ms iyi |

> Takılma oranını kare hızı sanmayın. Geçit %100 "janky" görünürken 60 kare/s
> akıyor ve tek kare düşürmüyor; oradaki tek gerçek, kare gecikmesinin bütçeyi
> aşması. Karar `kare/s` ve `Missed Vsync` ile verilir.

Notlar:

- **Sürüm (release) derlemesiyle ölçün.** Hata ayıklama derlemesinde Compose
  belirgin biçimde yavaştır.
- **Pencerenin tamamının oynandığını doğrulayın.** Koşu ortada biterse çizim
  durur ve ortalama düşük çıkar: kare sayısı ≈ 60 × pencere değilse ölçüm
  kirlidir, turu yeniden başlatıp tekrarlayın. (Filo'da sabit duran gemi ~10 s
  içinde ölüp ölçümü 48 kare/s'e düşürüyor; oysa oynanan pencerede 60.)
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

### Başarım testleri duvar saatine bağlanmaz

Üretim/çözüm süresi ölçen testler paylaşımlı CI koşucularında kırılgandır.
Kakuro'nun `generationIsFastEnough` testi 3000 ms sınırı koyuyordu; geliştirme
makinesinde en kötü değer 1868 ms olduğu için pay 1,6 katıydı ve CI'da ara ara
kırmızı yanıyordu — üretici hiç değişmeden.

Motorlar deterministik olduğundan daha iyi bir ölçüt var: **işin kendisini**
sayın. Kakuro üreticisi zaten düzen/doldurma/onarım sayaçları tutuyor; bunlar
tohumdan türediği için her makinede aynı çıkar ve üretici gerçekten daha çok
denemeye başlarsa büyür (cihazdaki yavaşlamanın da sebebi budur).

- Sınırı gözlenen en kötü değerin birkaç katına koyun: küçük ayarlar testi
  kırmasın, blokaj yakalansın. (Kakuro: gözlenen 2/10, sınır 8/40.)
- Duvar saatini tümüyle atmak gerekmez; **felaket freni** olarak bol paylı bir
  üst sınır bırakın (Kakuro: yerel ~3 s, sınır 30 s) ve bunun bir başarım
  hedefi olmadığını yorumda belirtin.
- Sayacı olmayan bir motorda önce sayacı ekleyin; ölçülemeyen şey korunamaz.

Depodaki iyi örnek Vergici: çözücü süreye değil **düğüm bütçesine** bakıyor
(`VergiciSolver.optimal(n, budget = 400_000)`), test de sonucu deterministik
bir ölçütle karşılaştırıyor (`opt >= greedyScore`). Süre yalnızca rapora
basılıyor, iddiaya girmiyor — doğru kullanım budur.

Tüm motor testleri bu açıdan tarandı (`nanoTime`, `currentTimeMillis`,
`Thread.sleep`): duvar saatine **iddia bağlayan** tek yer Kakuro'ydu ve
düzeltildi. Kakuro ile Vergici'de kalan süre ölçümleri yalnızca rapor amaçlı.

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

Ölçüm cihazı: SM-A515F (Galaxy A51), Android 13, 1080×2400 @420 dpi, 60 Hz,
sürüm derlemesi. A: açılış/oynanış/çökme. B: 12 s pencerede kare ölçümü.

| Oyun | A | B (kare/s · kaçan vsync · jank · p50) | C | D | Tarih |
| --- | --- | --- | --- | --- | --- |
| Blok | ✅ | olay güdümlü (1 · 0) | — | bekliyor | 2026-09-09 |
| 2048 | ✅ | olay güdümlü (0 · 0) | — | bekliyor | 2026-09-09 |
| Yılan | ✅ | **61 · 0 · %0 · 22 ms** | — | bekliyor | 2026-09-09 |
| Sudoku | ✅ | olay güdümlü (1 · 0) | — | bekliyor | 2026-09-09 |
| Mayın Tarlası | ✅ | olay güdümlü (0 · 0) | — | bekliyor | 2026-09-09 |
| Beş Harf | ✅ | olay güdümlü (0 · 0) | — | bekliyor | 2026-09-09 |
| Kıskaç | ✅ | olay güdümlü (0 · 0) | — | bekliyor | 2026-09-09 |
| Türetme | ✅ | olay güdümlü (0 · 0) | — | bekliyor | 2026-09-09 |
| Dizgi | ✅ | olay güdümlü (0 · 0) | — | bekliyor | 2026-09-09 |
| Kuyu | ✅ | **61 · 0 · %0 · 21 ms** | bekliyor | bekliyor | 2026-09-09 |
| Geçit | ✅ | **60 · 1 · %1,2 · 22 ms** | bekliyor | bekliyor | 2026-09-09 |
| Tavla | ✅ | olay güdümlü (0 · 0) | bekliyor | bekliyor | 2026-09-09 |
| Balkon | ✅ | **60 · 2 · %39,6 · 25 ms** | bekliyor | bekliyor | 2026-09-09 |
| Kakuro | ✅ | olay güdümlü (0 · 0) | — | ✅ üretim bütçesi | 2026-09-09 |
| Vergici | ✅ | olay güdümlü (0 · 0) | — | bekliyor | 2026-09-09 |
| Toplam Kapma | ✅ | olay güdümlü (0 · 0) | — | bekliyor | 2026-09-09 |
| Viraj | ✅ | **60 · 3 · %100 · 34 ms** | — (tuşla) | ✅ kusur yok | 2026-09-09 |
| Filo | ✅ | **60 · 1 · %81 · 31 ms** | ✅ düzeltildi | ✅ düzeltildi | 2026-09-09 |

18 oyunun tamamı açıldı, oynandı ve **hiçbirinde çökme yok** (`logcat` temiz).
Sürekli çizen altı oyunun tamamı 60 kare/s tutuyor; kaçan vsync 0–3 (≈%0,4).
Yani **kare hızı sorunu yok**.

### Filo · 2026-09-09 (SM-A515F, Android 13, 1080×2400 @420 dpi)

Oyun alanı ölçeği: 1 birim = 1016 px = 61,4 mm (ekranın tamamı değil).

**A — koşum:** açıldı, oynandı, çökme yok. Ölçüm sırasında kaydedilen sapma:
oyun bittiğinde çizim döngüsü durduğu için `gfxinfo` sıfır kare gösterir; bu,
ölçüm penceresini de kirletir (bkz. B aşamasındaki not).

**B — kare hızı (sürüm derlemesi):** pencere boyunca kesintisiz oynandığında
**60 kare/s** (904 kare / 15 s), kaçan vsync 3. Kare düşmüyor.

Kare gecikmesi yüksek: p50 31 ms, jank %81. Faz dökümü maliyetin GPU tarafında
olduğunu söylüyor (çizim kaydı 1,3 ms, GPU 17 ms), ama tam ekran gradyan **ve**
70 yıldız birlikte kaldırıldığında ölçülebilir kazanç çıkmadı.

> Bu satırın ilk hâlinde "~34 kare/s" yazıyordu; rakam kare sayısından değil
> `p50`'den türetilmiş, ölçüm penceresi de koşu ortada bitince kirlenmişti.
> Düzeltildi. Ayrıntı: [Kare gecikmesi](#kare-gecikmesi-kapanan-bir-konu-ve-kalan-bir-nüans).

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

### Viraj · 2026-09-09

**B — kare hızı:** **60 kare/s** (723 kare / 12 s), kaçan vsync 3. Kare
düşmüyor; kare gecikmesi ölçülen oyunlar içinde en yüksek (p50 34 ms, jank
%100). İlk kayıttaki "~31 kare/s" yanlıştı, düzeltildi.

**C — giriş:** Viraj tuşla sürülüyor (◄ ► ve FREN), sürükleme yok; bu aşama
uygulanmaz. Tuş yinelemesi ayrı bir ölçüm konusu.

**D — denge** (`./gradlew :games:viraj:probe`): kusur bulunmadı, ama zorluk
eğrisi iki ucundan aynı anda sıkıyor; tasarım kararı olarak kayda değer.

Direksiyon yetkisi `2·speedPct`, merkezkaç `2·speedPct²·viraj·0,3`. Yani tam
karşı direksiyona rağmen dışarı savrulma koşulu `speedPct·viraj·0,3 > 1`:

| Viraj | Tutulabilen azami hız |
| --- | --- |
| ≤ 3,33 | tam gaz (240 km/s) |
| 4,0 | %83 (200 km/s) |
| 5,0 | %66 (160 km/s) |
| 6,5 | %51 (123 km/s) |

Yol, zorlukla birlikte daha sert viraj üretiyor (0'da 1,5–3,0; 1'de 1,5–6,5),
yani ileride en sert virajlarda hız yarıya inmek zorunda.

Aynı anda süre bütçesi daralıyor: kontrol noktası arası 120 000 birim ve ödül
16 s'den 12 s'ye düşüyor. Başa baş ortalama hız **zorluk 0'da %62, zorluk 1'de
%83**. Yani geç aşamada ortalama %83 tutmak gerekirken en sert virajlar %51'e
zorluyor — koşu doğal olarak burada bitiyor.

İnsan sınırlı sürücü (8 tohum, ayrık ◄ ► + fren): acemi 19,3 · orta 19,9 ·
usta 20,6 kontrol noktası (~60 km). Beceri sonucu neredeyse değiştirmiyor;
koşuyu bitiren şey tepki hızı değil, hız/süre sıkışması.

Not: turbo sırasında `speedPct` 1,25'e çıktığı için tutulabilen viraj 2,67'ye
iner — turboyu virajlı kesimde almak, frenlemeden kullanılırsa zarar. Hata
değil, ama oyuncuya öğretilmesi gereken bir incelik.

## Kare gecikmesi: kapanan bir konu ve kalan bir nüans

Bu belgenin ilk hâlinde "uygulama geneli kare hızı sorunu" diye bir açık konu
vardı. **Yoktu.** 18 oyunun ölçümü sonrası tablo net: sürekli çizen altı oyunun
hepsi 60 kare/s akıyor ve pratikte kare düşürmüyor.

Geriye gerçek ama küçük bir nüans kalıyor: aynı 60 kare/s'te kare **gecikmesi**
oyunlar arasında ikiye katlanıyor.

| Oyun | p50 kare gecikmesi | jank |
| --- | --- | --- |
| Kuyu | 21 ms | %0 |
| Yılan · Geçit | 22 ms | %0–1 |
| Balkon | 25 ms | %40 |
| Filo | 31 ms | %81 |
| Viraj | 34 ms | %100 |

Filo ve Viraj kare başına ~10 ms fazla harcıyor. Kare düşmediği için görüntü
akıcı; etkilenen şey parmak-ekran gecikmesi. Filo'da faz dökümü maliyetin GPU
tarafında olduğunu gösteriyor (çizim kaydı 1,3 ms, GPU 17 ms), ama tam ekran
gradyan **ve** 70 yıldızın ikisi birden çizimden çıkarıldığında ölçülebilir
kazanç çıkmadı — yani maliyet tek bir çizim çağrısında değil.

Bu bir hata değil, iyileştirme fırsatı. Kovalanacaksa önce ölçülmeli
(`tools/cihaz_testi.py fazlar`, `debug.hwui.overdraw show`); tahminle
optimizasyon bu belgede bir kez denendi ve boşa gitti.

