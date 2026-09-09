# Oyun test protokolü

Motor birim testleri **kuralların doğru** olduğunu gösterir; oyunun **oynanabilir**
olduğunu göstermez. Bir motor, testleri tam geçerken cihazda ulaşılmaz derecede
zor, kontrolü tepkisiz ya da kare hızı düşük olabilir — çünkü birim testleri
parmağı, ekranı ve gerçek işlemciyi bilmez.

Bu belge, her oyunun yayına girmeden önce geçmesi gereken dört aşamayı tanımlar.

> **Kural:** Yeni bir oyun eklendiğinde ya da bir oyunun dengesi/kontrolü
> değiştiğinde A–D aşamaları koşulur ve sonuçları [Sonuç kütüğü](#sonuç-kütüğü)
> bölümüne işlenir. Motor testleri yeşil diye bu adımlar atlanmaz.

Tüm ölçüm koşumlarını birden çalıştırmak için:

```bash
ANDROID_HOME=$HOME/Android/Sdk ./gradlew probe
```

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

### Sıra tabanlı oyunlarda soru farklıdır

Tepki oyunlarında ölçüm "yetişilebilir mi" diye sorar; sıra tabanlı oyunlarda
zaman baskısı yoktur, o yüzden soru **adilliğe** kayar:

| Ölçüt | Soru |
| --- | --- |
| Tek çözüm | Bulmacanın tek bir çözümü olduğu garanti mi? |
| Tahminsizlik | Çözüm baştan sona mantıkla ilerliyor mu, yoksa kör seçim gerekiyor mu? |
| Üretim bütçesi | Üretim, cihazı bekletmeyecek kadar ucuz mu? (bkz. duvar saati notu) |
| Zorluk dağılımı | "Zor" gerçekten daha mı zor, yoksa yalnızca daha mı büyük? |

Tahminsizlik ölçmek için oyunun çözücüsünü değil, **oyuncunun görebildiği
bilgiyle** çalışan ayrı bir çözücü yazılır: bilinen kısıtlardan kesin sonuç
çıkarır, çıkaramayınca "burada tahmin gerekti" der. Böyle bir çözücü kısıtları
bağımsız bileşenlere ayırmalı; yoksa sayım üstel patlar ve ölçüm tahmini
olduğundan fazla gösterir.

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
| Blok | ✅ | olay güdümlü (1 · 0) | — | ✅ 11. seviyede tavan | 2026-09-09 |
| 2048 | ✅ | olay güdümlü (0 · 0) | — | ✅ kazanılabilir | 2026-09-09 |
| Yılan | ✅ | **61 · 0 · %0 · 22 ms** | — | ✅ tutarlı | 2026-09-09 |
| Sudoku | ✅ | olay güdümlü (1 · 0) | — | ✅ merdiven doğru | 2026-09-09 |
| Mayın Tarlası | ✅ | olay güdümlü (0 · 0) | — | ⚠️ tahmin zorunlu | 2026-09-09 |
| Beş Harf | ✅ | olay güdümlü (0 · 0) | — | ✅ hepsi çözülebilir | 2026-09-09 |
| Kıskaç | ✅ | olay güdümlü (0 · 0) | — | ⚠️ 12 hak yetmiyor | 2026-09-09 |
| Türetme | ✅ | olay güdümlü (0 · 0) | — | ✅ dengeli | 2026-09-09 |
| Dizgi | ✅ | olay güdümlü (0 · 0) | — | ✅ torba sağlam | 2026-09-09 |
| Kuyu | ✅ | **61 · 0 · %0 · 21 ms** | bekliyor | ⚠️ RAPID takası | 2026-09-09 |
| Geçit | ✅ | **60 · 1 · %1,2 · 22 ms** | — (ayrık hamle) | ✅ adil | 2026-09-09 |
| Tavla | ✅ | olay güdümlü (0 · 0) | — | ⚠️ Hapis beraberliği | 2026-09-09 |
| Balkon | ✅ | **60 · 2 · %39,6 · 25 ms** | — (nokta nişan) | ⚠️ scooter penceresi | 2026-09-09 |
| Kakuro | ✅ | olay güdümlü (0 · 0) | — | ✅ üretim bütçesi | 2026-09-09 |
| Vergici | ✅ | olay güdümlü (0 · 0) | — | ✅ düğüm bütçeli çözücü | 2026-09-09 |
| Toplam Kapma | ✅ | olay güdümlü (0 · 0) | — | ✅ mevcut testlerle | 2026-09-09 |
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

### Türetme · 2026-09-09

**D — adillik** (`./gradlew :games:turetme:probe`). **Sorun yok.**

Oyuncu taban kelimenin harflerinden başka kelimeler üretiyor. Denge iki yanlı:
taban çok az kelime verirse tur sönük geçer, çok fazla verirse tamamlama bonusu
ulaşılmaz olur.

1200 taban, 15 829 geçerli kelime. Taban başına hedef sayısı:

| En az | %10 | Ortanca | %90 | En çok | Ortalama |
| --- | --- | --- | --- | --- | --- |
| 15 | 17 | 27 | 47 | 66 | 29,6 |

5'ten az hedefi olan taban **yok**; 60'tan çok hedefi olan yalnızca 10 taban
(%0). Bütün tabanlar geçerli kelime listesinde ve kendi hedef kümelerinde
(taban bonusu için gerekli). Günlük döngü: 1200 günde 1200 farklı taban.

### Dizgi · 2026-09-09

**D — torba ve el** (`./gradlew :games:dizgi:probe`). **Sorun yok.**

100 taş, 2 joker; %40 sesli, %58 sessiz. Torba sözlük derleminin harf
sıklığından türetilmiş ve ölçümde bunu tutuyor: en sık 12 harfte torba oranıyla
sözlük oranı arasındaki en büyük sapma 2,68 puan.

El oynanabilirliği: rastgele çekilen 7 taşın **%98'i** en az bir kelime
kurabiliyor, ilk 40 elde ortalama 45 seçenek. Yani oyuncu nadiren pas geçmek
zorunda kalıyor.

### Kıskaç · 2026-09-09

**D — adillik** (`./gradlew :games:kiskac:probe`).

Kıskaç bir **ikili arama** oyunudur: her tahminde gizli kelimenin alfabetik
olarak önce mi sonra mı olduğu söylenir. Bu yüzden adillik tam olarak
hesaplanabilir. Gizli kelime 1684'lük cevap havuzundan seçiliyor, ama oyuncunun
ekranda gördüğü ve tahmin edebildiği sıralı liste 7797 kelimelik geçerli
tahminler listesi.

| Arama uzayı | Teorik alt sınır | Ölçülen en kötü | 12 hakka sığmayan |
| --- | --- | --- | --- |
| Cevap havuzu (1684) | 11 | 11 | **0** (%0) |
| Geçerli tahminler (7797) | 13 | 13 | **820** (%48) |

**Bulgu: oyuncunun gördüğü liste üzerinde 12 hak yetmiyor.** Doğal strateji
ekrandaki sıralı liste üzerinde ikili aramadır; o uzayda cevapların **%48'i**
12 hakla bulunamıyor (13 gerekiyor). Kazanmak için oyuncunun ayrıca "cevap
yaygın bir kelimedir" sezgisiyle aramayı daraltması gerekiyor — yani oyun,
tanıttığı mekaniğin ötesinde kelime dağarcığı istiyor.

Cevap havuzu üzerinde arama yapılabilseydi 11 tahmin yeterdi (1 hak pay). Karar
tasarımın: hak 13–14'e çıkarılabilir, ekranda cevap havuzu gösterilebilir, ya
da mevcut hâl "ipucu gerektiren zorluk" olarak bilinçle korunabilir.

Günlük döngü sağlam: 1684 günde 1684 farklı kelime, tekrar yok.

### Tavla · 2026-09-09

**D — zar, denge ve rakip** (`./gradlew :games:tavla:probe`).

**Zar düzgün.** 47 880 zar atışında ki-kare 2,33 (5 sd, %99 eşiği 15,09); en
büyük yüz sapması %1,2. Oyuncunun ilk şüphesi hep zar olduğu için bu ölçüm
kütükte durmalı.

**İlk oynayan avantajı** (yapay zekâ – yapay zekâ, 120 oyun/mod):

| Mod | Başlayan kazandı | Kilitlenme | Berabere |
| --- | --- | --- | --- |
| Klasik | %59 | 0 | 0 |
| Tapa | %44 | 1 | 1 |
| Hapis | %35 | **31** | **31** |

Klasik %59 ile beklenen bantta. Hapis'te tablo başka bir şey söylüyor:
**oyunların dörtte biri kilitlenmeyle berabere bitiyor.**

**Bulgu: Hapis'te kilitlenme her zaman berabere.** 31 kilitlenmenin 31'i de
beraberlikle sonuçlandı. Nedeni yapısal: karşılıklı tam blokaj konumunda iki
taraf da tam olarak 38 pip, bar ve toplanan boş oluyor — konum simetrik.
Dolayısıyla `finishDeadlock` içindeki "pip sayısı az olan kazanır" kuralı
pratikte hiç devreye girmiyor; Hapis'te kilitlenme = berabere.

Rastgele rakibe karşı da benzer (120 oyunda 21 beraberlik), yani iki tarafın
aynı sezgiseli oynamasından kaynaklanan bir yapaylık değil. Karar tasarımın:
beraberlik oranı kabul edilebilir mi, yoksa kilitlenme başka bir kuralla mı
çözülmeli (örneğin son hamleyi yapan kaybeder, ya da toplanan pula bakmak)?

**Bilgisayar rakip gücü** (rastgele yasal hamleye karşı, 120 oyun/mod):
Klasik %100, Tapa %98, Hapis %79 (+21 beraberlik). Sezgisel çalışıyor;
rastgeleye şans tanımıyor.

### 2048 · 2026-09-09

**D — kazanılabilirlik** (`./gradlew :games:g2048:probe`). **Sorun yok.**

Taş doğuşu kurala uygun: 29 447 doğuşta %90,0 iki, %10,0 dört.

Kazanılabilirlik, tekdüzelik + boş hücre + köşe sezgiseli ve bir kat ileri
bakışla oynayan bir modelle ölçüldü (60 oyun): **%28'i 2048'e ulaşıyor**,
32 oyun 1024'te bitiyor, ortalama skor 17 274.

> Ölçüm yazarken kendi hatam öğreticiydi: ilk sezgisel yalnızca boş hücre ve
> köşe bakıyordu ve 2048'e **hiç** ulaşamıyordu (en iyi 512). O sayı oyun
> hakkında değil sezgisel hakkında bilgi verirdi. Tekdüzelik terimi eklenince
> tablo gerçekçi oldu. Denge ölçümünde oyuncu modeli zayıfsa, sonuç oyunu değil
> modeli ölçer.

### Yılan · 2026-09-09

**D — hız eğrisi** (`./gradlew :games:snake:probe`). **Sorun yok.**

Tek zorluk kaynağı hız: adım aralığı `160 − yem×3`, taban 70 ms.

| Yem | Adım aralığı | Saniyede adım |
| --- | --- | --- |
| 0 | 160 ms | 6,3 |
| 20 | 100 ms | 10,0 |
| 30 | **70 ms (taban)** | 14,3 |

Hız 30 yemde tabana oturuyor; sonrası sabit. 70 ms insan tepki süresinin
altında, yani tavan hızda oyuncu tek tek adımlara tepki veremez, önden plan
yapar — yılan oyunlarında beklenen budur. Tahta 300 hücre olduğu için oyunun
geri kalanı sabit hızda, artan uzunlukla oynanıyor: zorluk hızdan değil yerden
geliyor. Tutarlı tasarım.

### Blok · 2026-09-09

**D — zorluk eğrisi** (`./gradlew :games:tetris:probe`). **Sorun yok.**

Blok'ta zaman baskısı tek yerden gelir: yerçekimi. Seviye her 10 satırda artar,
düşme aralığı Guideline formülüyle kısalır ve 50 ms tabanında durur.

| Seviye | Satır | Hücre başına | Tepeden dibe | 8 girişlik tempo |
| --- | --- | --- | --- | --- |
| 1 | 0 | 1000 ms | 20,0 s | 2500 ms — rahat |
| 5 | 40 | 355 ms | 7,1 s | 888 ms — rahat |
| 9 | 80 | 93 ms | 1,86 s | 233 ms — rahat |
| 11 | 100 | **50 ms (taban)** | 1,00 s | 125 ms — sıkı |
| 20 | 190 | 50 ms | 1,00 s | 125 ms — sıkı |

Yerçekimi **11. seviyede (100. satır) tabana oturuyor**; sonrasında oyun
hızlanmıyor. Tabandaki 1 saniyelik düşüş, en kötü durumda gereken 8 girişe
(3 dönüş + 5 yatay adım) 125 ms'lik tempoyla tam yetiyor — sıkı ama insan üstü
değil. Yani usta oyuncu 100. satırdan sonra teorik olarak sonsuza dek oynar.
Birçok Blok uyarlaması böyledir; bilinçliyse sorun yok.

### Beş Harf · 2026-09-09

**D — adillik** (`./gradlew :games:besharf:probe`). **Sorun yok.**

Kelime oyunlarında adillik sorusu: her cevap hakla çözülebiliyor mu? Tuzak
kelimeler altı hakkı yakabilir ve o gün herkes kaybeder.

Havuz sağlamlığı: 1684 cevap, 7797 geçerli tahmin; yanlış uzunlukta cevap yok,
tahmin olarak kabul edilmeyen cevap yok, tekrar eden cevap yok. Günlük kelime
kalıcı bir permütasyondan seçildiği için havuz tükenmeden tekrar gelmiyor —
1684 günlük (≈4,6 yıl) döngü.

Çözülebilirlik (421 cevaplık örneklem, her adımda en kötü durumda en çok
eleyeni seçen çözücü, açılış "amber"):

| Tahmin | Cevap sayısı |
| --- | --- |
| 2 | 21 |
| 3 | 147 |
| 4 | 199 |
| 5 | 51 |
| 6 | 3 |

**Altı hakka sığmayan cevap yok (%0).** Çoğu cevap 3–4 tahminde çözülüyor.
Çözücü örneklemeyle zayıflatıldığı için bu bir üst sınır: kusursuz oyun daha
da iyisini yapar.

### Sudoku · 2026-09-09

**D — adillik ve zorluk** (`./gradlew :games:sudoku:probe`). **Sorun yok.**

Üretici tek çözümü zaten garantiliyor (`countSolutions == 1`), yani adillik
tarafı sağlam. Açık soru zorluğun ne anlama geldiğiydi: zorluk yalnızca ipucu
sayısıyla tanımlanıyor (40/32/26) ve ipucu sayısı zorluğun zayıf bir
göstergesidir. Tahtaların hangi insan teknikleriyle çözülebildiği ölçüldü
(40 tohum/zorluk):

| Zorluk | İpucu (hedef / gerçek) | Tek adayla | Gizli tekle | Daha ileri |
| --- | --- | --- | --- | --- |
| Kolay | 40 / 40,0 | **%95** | %5 | %0 |
| Orta | 32 / 32,0 | %42 | %50 | %7 |
| Zor | 26 / 26,1 | %2 | %52 | **%45** |

Merdiven gerçekten çalışıyor: kolay tahtaların neredeyse tamamı en basit
teknikle (hücrede tek seçenek) çözülüyor, orta seviye gizli tek gerektiriyor,
zorun yarısı bu iki tekniğin ötesine geçiyor. Üretici hedef ipucu sayısını da
birebir tutturuyor.

Not: zor tahtaların %45'i çift/üçlü çıkarımı ya da deneme gerektiriyor. Tek
çözüm garantisi durduğu için bu adaletsizlik değil, "zor"un tanımı — ama not
alma desteğinin neden gerekli olduğunu açıklıyor.

### Mayın Tarlası · 2026-09-09

**D — adillik** (`./gradlew :games:mines:probe`).

İlk tık hep güvenli: mayınlar ilk tıktan sonra, tıklanan hücre ve komşuları
hariç yerleştiriliyor. Ama tahtanın **kalanının** mantıkla çözülebileceği
garanti edilmiyor. Oyuncunun görebildiği bilgiyle çalışan bir çözücü yazılıp
her tahtada kaç kez kör tahmine zorlandığı sayıldı (60 tohum/zorluk):

| Zorluk | Tahta | Mayın | Yoğunluk | Tahminsiz biten | Ort. tahmin | En kötü |
| --- | --- | --- | --- | --- | --- | --- |
| Kolay | 9×12 | 14 | %13 | **%80** | 0,38 | 6 |
| Orta | 10×14 | 25 | %18 | **%26** | 2,50 | 11 |
| Zor | 12×17 | 40 | %20 | **%3** | 5,67 | 16 |

Yani orta zorlukta tahtaların dörtte üçü, zor zorlukta neredeyse tamamı bir
noktada **kör seçim** gerektiriyor. Bu seçimler yazı tura: kaybı beceriyle
önlenemez, üstelik zor tahtada ortalama beş kez üst üste tutturmak gerekiyor.

Sayılar muhafazakâr bir alt sınırdır: çözücü 24 hücreden büyük bileşenleri ve
toplam mayın sayısı kısıtını atlıyor, yani gerçek "tahminsiz" oranı bir miktar
daha yüksek olabilir — ama yön değişmez.

Karar tasarımın. Klasik Mayın Tarlası da böyledir; ama "tahminsiz üretim"
(üretirken çözücüyü çalıştırıp tahmin gerektiren tahtayı atmak) yaygın bir
iyileştirmedir ve bu depoda gereken çözücü zaten yazıldı. Maliyeti üretim
süresidir; Kakuro'da olduğu gibi iş sayacıyla sınırlanabilir.

### Balkon · 2026-09-09

**D — denge** (`./gradlew :games:balkon:probe`).

Balkon bir **öndeleme** oyunu: atış 0,45–0,95 s havada kalır, bu sırada hedef
yürür ve rüzgâr iniş noktasını kaydırır. Nişan, hedefin şimdiki değil
inişteki yerine alınır.

Gereken öndeleme, isabet yarıçapıyla karşılaştırıldığında (11. seviye ve
sonrası, hız çarpanı tavanda):

| Hedef | Öndeleme | Yarıçapın katı | Zamanlama penceresi |
| --- | --- | --- | --- |
| SIMIT · NEIGHBOR | 0,08 | 1,0× | 675–731 ms |
| PIGEON | 0,11 | 1,7× | 401 ms |
| CAT · CAR | 0,16–0,24 | 2,3× | 307 ms |
| BALL · BIKE | 0,27–0,32 | 4,3× | 161 ms |
| **SCOOTER** | **0,49** | **6,7×** | **104 ms** |

Öndeleme her hedefte yarıçaptan büyük, yani "olduğu yere at" hiçbir zaman
çalışmıyor — oyunun temel becerisi bu ve doğru kurulmuş. Ama **scooter 11.
seviyeden sonra 104 ms'lik pencereye iniyor**; insan dokunma hassasiyeti
~50–100 ms olduğuna göre bu, nişanı büyük ölçüde şansa bırakıyor. Mega atış
(yarıçap 0,10) pencereyi 197 ms'ye çıkarıp sorunu çözüyor; yani scooter fiilen
"mega ile vurulacak hedef" hâline geliyor. Bilinçliyse sorun yok, değilse
scooter'ın hız aralığı ya da yarıçapı gözden geçirilmeli.

Rüzgâr telafi edilebilir: kayma `rüzgâr × uçuş` ile atış anında sabitlenir ve
HUD'da gösterilir, yani tahmin değil hesap işi. Azami rüzgârda uzak atışta
kayma yarıçapın 3,8 katı — telafi edilmezse uzak hedef vurulamaz.

Seviye hedefi (`6 + 2 × seviye`) atış hızını hiç zorlamıyor: 20. seviyede bile
gereken isabet hızı, atış tavanının yalnızca %24'ü. Yani baskı isabet
oranında, tempo değil.

**Kusursuz nişan tavanı (5 tohum):** öndelemeyi ve rüzgârı tam hesaplayan bot
18–24. seviyede (ortalama 21,8) süreye takılıyor. İnsan bunun altında kalır;
oyunun doğal tavanı burası.

### Geçit · 2026-09-09

**D — denge** (`./gradlew :games:gecit:probe`). **Adillik sorunu bulunmadı.**

Geçit'in dengesi tek gerilime iner: geçmek için trafikte boşluk beklemek
gerekir, ama beklerken kamera yaklaşır ve 3,5 saniyede kartal kapar. Ölçüm bu
iki tarafı karşılaştırıyor.

Bekleme bütçesi (hangisi önce dolarsa):

| Satır | Kamera hızı | Kamera payı | Geçerli bütçe |
| --- | --- | --- | --- |
| 0 | 0,35 sat/s | 11,4 s | 3,50 s (kartal) |
| 100 | 0,80 sat/s | 5,0 s | 3,50 s (kartal) |
| 200+ | 1,25 sat/s | 3,2 s | **3,20 s (kamera)** |

Şeritlerin en uzun kesintisiz kapalı kalma süresi (sütun başına, en kötü):

| Tür | Derinlik 10 | 60 | 250 | Bütçe |
| --- | --- | --- | --- | --- |
| ROAD | 1,17 s | 0,67 s | 0,47 s | 3,2–3,5 s |
| RAIL | — | 0,53 s | 0,53 s | 3,2–3,5 s |
| RIVER | **3,15 s** | 2,27 s | 1,93 s | 3,2–3,5 s |

Ölçülen hiçbir sütun bütçeyi aşmıyor (**%0**), ve bir şeridin *bütün*
sütunlarının aynı anda kapalı kaldığı en uzun süre 0,23 s (tren). Yani her an
geçilebilir bir sütun var; oyun adil.

Dikkat çeken tek yer: **erken nehirler**. 10. satırda bir sütun 3,15 s kapalı
kalabiliyor, bütçe 3,50 s — payı 0,35 s. Üstelik yana hamle kartal sayacını
sıfırlamaz, 1,75 s'e çeker. Zorluk arttıkça nehir hızlandığı için bu süre
kısalıyor (250. satırda 1,93 s), yani en dar an oyunun en acemi anında.

**İlerleme botu (8 tohum):** acemi 27 · orta 19 · usta 39 satır (en iyi 104).
Bot yazarken kendi hatam öğreticiydi: ilk sürüm yalnızca "şu an boş mu" diye
bakıyordu ve ölümlerin tamamı CAR'dı — zıplama 0,12 s sürerken trafik akıyor.
Bot inişi öngörecek biçimde düzeltildi. Ölçüm koşumunda bot yazarken **hamlenin
tamamlandığı anı** modellemek şart.

### Kuyu · 2026-09-09

**D — denge** (`./gradlew :games:kuyu:probe`).

Kuyu Downwell düzenindedir: ayrı zıplama tuşu yok, tek tuş yerdeyken zıplatır,
havada basılı tutulunca aşağı ateş eder ve düşüşü `SHOT_FALL` ile sınırlar.
Şarjör inişte dolar. Yani şarjör = **havada kalma bütçesi**, oyunun çekirdek
kaynağı budur ve ölçüm buna odaklandı.

Bir şarjörün karşılığı:

| Kurulum | Atış | Havada süre | Düşüş (kare) | Hasar/şarjör |
| --- | --- | --- | --- | --- |
| taban | 8 | 0,80 s | 4,15 | 8 |
| +RAPID | 8 | **0,53 s** | 2,46 | 8 |
| +AMMO ×1 | 10 | 1,00 s | 5,18 | 10 |
| +SPREAD | 8 | 0,80 s | 4,15 | **24** |

Motor üstünde dipsiz kuyuda doğrulandı: taban 0,72 s / 3,37 kare, RAPID
0,48 s / 1,94 kare (kestirimle uyumlu; küçük fark ilk atışın düşüş başladıktan
sonra gelmesinden).

**Bulgu: RAPID bir takas, yükseltme değil.** Şarjör atış *başına* düşer, mermi
başına değil; RAPID atış aralığını 6→4 kareye indirdiği için aynı 8 atış daha
çabuk tükenir: havada kalma **%33 kısalır**, şarjör başına hasar **değişmez**.
Karşılığında düşüşü daha sık frenler (şarjör başına 4,15 yerine 2,46 kare iner)
ve anlık atış hızı %50 artar. Filo'daki silah 3 gibi düpedüz gerileme değil —
ama "yükseltme" olarak sunulan bir kutunun hayatta kalma süresini kısaltması
oyuncuya ceza gibi gelebilir. Karar tasarımın: takas kalacaksa RAPID'in şarjörü
de artırmalı (`+RAPID +AMMO ×2` ölçümü tabanla aynı 0,80 s'i geri veriyor).

Buna karşılık **SPREAD güçlü ve tutarlı**: tek atışta üç mermi attığı, şarjör
ise atış başına düştüğü için hasarı üçe katlar, havada kalmayı hiç kısaltmaz.

**İniş botu (6 tohum):** acemi 74 · orta 56 · usta 11 satır (en iyi 218).
Kuyu'da kendiliğinden düşülmez — zemindeki deliği bulup oraya yürümek gerekir,
bu yüzden bot Filo/Viraj'dakilerden farklı olarak yol bulmak zorunda. Bot kaba:
beceri sırası ters çıkıyor (düşük gecikme yön kararını sık değiştirip
salınıma sokuyor), o yüzden bu sayılar **yalnızca kaba bir alt sınır**;
zorluk eğrisi yorumu için yeterli değil. Kuyu'nun ilerleme ölçümü açık kalıyor.

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

