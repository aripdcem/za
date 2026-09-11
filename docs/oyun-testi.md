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

### Protokolün CI'daki karşılığı

Aşamalar farklı otomatikleşiyor; ayrımı bilerek koruyoruz:

| Aşama | Nerede koşar |
| --- | --- |
| **A** cihaz koşumu | Sürüm öncesi, gerçek cihazda ([`store/checklist.md`](../store/checklist.md)) |
| **B** kare hızı | Sürüm öncesi, gerçek cihazda — emülatörün kare süreleri gerçeği temsil etmez |
| **C** giriş kalibrasyonu | Sürüm öncesi, gerçek cihazda — gerçek dokunma ve ekran ölçeği gerekir |
| **D** denge | **CI**: değişmez testleri `test` görevinde, ölçüm koşumları `probe` adımında |
| **E** erişilebilirlik | Kontrast **CI**'da (`ThemeContrastTest`); etiketler sürüm öncesi cihazda |

CI'daki `probe` adımı geçme/kalma vermez; amacı **ölçüm koşumlarının
çürümesini engellemek**. Asıl koruma, ölçülen doğruların değişmez testine
çevrilmesidir:

| Değişmez | Nerede |
| --- | --- |
| Silah yükseltmesi patron hasarını düşürmez | `FiloWorldTest` |
| Filo: gemi dikey bantta kalır, düşman mermisi ve çarpışma geminin canlı konumunu izler | `FiloWorldTest` |
| Yükseltme havada kalma bütçesini kısaltmaz | `KuyuWorldTest` |
| Üretilen tahta tahminsiz çözülebilir | `MinesStateTest` |
| Tahmin hakkı ikili arama derinliğini karşılar | `KiskacStateTest` |
| Kilitlenme berabere değil kararla biter | `TavlaStateTest` |
| Hiçbir şerit bekleme bütçesinden uzun kapalı kalmaz | `GecitWorldTest` |
| Her cevap hak içinde çözülebilir | `BesHarfStateTest` |
| Her taban yeterli hedef verir | `TuretmeStateTest` |
| Rastgele ellerin çoğu kelime kurar | `DizgiStateTest` |
| Kolay tahtalar en basit teknikle çözülür | `SudokuStateTest` |
| Üretim iş bütçesini aşmaz | `KakuroTest` |
| Her bulmaca tahminsiz çözülür, brif kısa kalır, üretim bütçede | `ReyonGeneratorTest` |
| Denetim sapmaları ayrık ve görünür; plan ile raf yalnızca sapma gözlerinde ayrışır | `ReyonAuditTest` |
| Satış hedefi tabanı geçer, geçerli tam doluluktur; iyileştirici bütçede | `ReyonSalesTest` |
| Sipariş: gerçekleşen talep tahmin aralığında; uzman siparişlerinin tekrar oynanışı hedefi birebir verir; gün kuralları elle izlenen haftayla eşleşir; kayıt tur dönüşü | `ReyonOrderTest` |
| Reyon blok adları en dar gerçek gözde kırpılmaz (35 ad, TR ve EN; 360 dp telefonda Zor planı, tek yüz) | `ReyonBlockLabelTest` |
| Denetimde plan ve raf 360×640'ta da aynı genişlikte ve ekran içinde; plan büyütme açılıp kapanır | `ReyonAuditLayoutTest` |
| Raket: orta bir oyuncu botu kolay bilgisayarı yener, zora yenilir, seviyeler sıralı ve her maç biter; tavan hızda vuruş kaçmaz (tünelleme yok) | `RaketWorldTest` |
| Tuşe: şerit dizisi tohumdan deterministik, her şerit kullanılır, tekrar payı sınırlı; Sonsuz'da sıradaki karo tamamen çıkana dek vurulabilir; parçalar aralıkta ve oktav sıçramasız; sentez notanın frekansını %3 içinde tutar | `TuseWorldTest` |
| Uçurtma: üretilen dünya her sütunda ≥ 0,3 birim boşluk bırakır (tavan zorlukta da); rakibin üstünden geçen keser, altından geçen kesilir; dikkatli pilot 12 uçuşun en az 8'inde 300 m'yi geçer | `UcurtmaWorldTest` |
| Dalgıç: doğan her şey şeritlerde ve suda kalır, mayınlar alt şeritlerde ve en çok üç; boş yüzeye çıkış can götürür ama başta değil; pilot 20 dalışın en az 14'ünde teslim eder | `DalgicWorldTest` |
| Bostan: üretilen her seviye uzman politikasıyla kazanılır (6 tohum × 3 zorluk), türler dalga dizinine göre açılır, bütçe aşılmaz, zorluklar saldırgan sayısında sıralı; tuzak kurulmadan kemirilir, kurulunca kemirene patlar | `BostanStateTest` |
| Sincap: her basamakta en az bir dal ve güvenli kaçış (30 tohum × 400 basamak, güvenli yol araması), kargalı basamağın altında kuru dal yok; pilot boşluğa atlamaz, 10 tohumda ortalama ≥ 20 basamak | `SincapWorldTest` |
| Çekirge: tek fıskırtma kuralı, sürü kenarda dönüp iner ve seyreldikçe hızlanır, dokunulmazlıkta tükürük can götürmez, balyalar üç kaynaktan aşınır, sürü çiftçi hizasında istila; pilot 6 tohumun en az 4'ünde ilk dalgayı temizler | `CekirgeWorldTest` |
| Metin kontrastı WCAG AA eşiğini tutar | `ThemeContrastTest` |

Yeni bir ölçüm bulgusu düzeltildiğinde, mümkünse **paylı bir değişmez** olarak
buraya eklenir: ölçülen değerin kendisi değil, altına düşülmemesi gereken
sınır iddia edilir (ör. "en az 8 hedef", ölçülen 15 iken).

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

Ölçüm aracı: `tools/cihaz_testi.py`. İki parmak gerektiren ölçümler için
`tools/coklu_dokunus.py` (uinput ile sanal dokunmatik; `input`/`sendevent`
tek parmakla sınırlı).

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

## E · Erişilebilirlik

Amaç: oyun, ekran okuyucuyla ve düşük görme keskinliğiyle kullanılabiliyor mu?

### E1 · Kontrast (CI)

Tema tek yerde tanımlı olduğu için bu, cihaz gerektirmeyen bir birim testidir:
`ThemeContrastTest` metin/zemin çiftlerinin WCAG AA eşiğini (4,5:1) tuttuğunu
doğrular. Palet değiştiğinde okunabilirlik sessizce bozulamaz.

Ölçülen (2026-09-10): en düşük 6,18:1 (`onError/error`), en yüksek 15,84:1
(`onBackground/background`). Hepsi eşiğin üstünde.

### E2 · Etiketler (cihaz)

> Sıfır sınırlı düğümler: `erisim` çıktısındaki "sınırı sıfır dokunulabilir
> düğüm" satırı 0 değilse ekran okuyucu o düğmelere dokunarak inemez; gizli
> sistem çubuğunun bölgesine çizilen öğeler bazı cihazlarda böyle gelir.
> Uygulama TalkBack açıkken çubukları gizlemez; ölçümü TalkBack açıkken de
> yineleyin. Uyarı: SM-A515F'te bu tek başına yetmiyor — çubuklar görünürken
> bile uygulama alanının son 33 dp'si ağaçtan düşüyor (aşağıda, Reyon Sipariş
> doğrulama turu).

```bash
python3 tools/cihaz_testi.py erisim
```

Her dokunulabilir öğenin sınırları içinde bir etiket bulunmalı; yoksa TalkBack
"düğme" der ama ne yaptığını söylemez. **Etiket çoğu zaman çocuk düğümdedir**,
bu yüzden düğümün kendisine değil sınırlarını kapsayan etikete bakılır — ilk
ölçümde bunu atlayınca hub'daki 14 öğenin hepsi "etiketsiz" görünmüştü.

18 oyunun taramasında tek gerçek bulgu Kıskaç'taki kolay mod anahtarıydı
(kendi metni olmayan `Switch`); satırın etiketi anahtara verilerek düzeltildi.

### E3 · Dokunma hedefi — neden ölçmüyoruz

Ölçmeyi denedik ve **güvenilmez olduğu için bıraktık.** Compose'da
`Surface(onClick)` gibi bileşenlerde semantik düğüm, dokunma alanını değil
içindeki metnin sınırlarını bildirebiliyor: Geçit'in 84 dp yüksekliğindeki yön
tuşları taramada **11 dp** görünüyordu. Şeridin dışına dokunmak çalıştığı
(ekran değişti) için ölçüm yanlış alarmdı.

Yoğun ızgaralarda ve klavyelerde 48 dp zaten geometrik olarak imkânsız:
Sudoku'nun 9×9 tahtası 411 dp genişlikte en çok 45 dp hücre verebilir, 29
harflik klavye satırına 10 tuş sığdırınca tuş 40 dp olur. Buton boyutu kodda
tanımlı olduğu için bu eksen kod incelemesine bırakıldı.

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
| Mayın Tarlası | ✅ | olay güdümlü (0 · 0) | — | ✅ düzeltildi | 2026-09-09 |
| Beş Harf | ✅ | olay güdümlü (0 · 0) | — | ✅ hepsi çözülebilir | 2026-09-09 |
| Kıskaç | ✅ | olay güdümlü (0 · 0) | — | ✅ düzeltildi | 2026-09-09 |
| Türetme | ✅ | olay güdümlü (0 · 0) | — | ✅ dengeli | 2026-09-09 |
| Dizgi | ✅ | olay güdümlü (0 · 0) | — | ✅ torba sağlam | 2026-09-09 |
| Kuyu | ✅ | **61 · 0 · %0 · 21 ms** | bekliyor | ✅ düzeltildi | 2026-09-09 |
| Geçit | ✅ | **60 · 1 · %1,2 · 22 ms** | — (ayrık hamle) | ✅ adil | 2026-09-09 |
| Tavla | ✅ | olay güdümlü (0 · 0) | — | ✅ düzeltildi | 2026-09-09 |
| Balkon | ✅ | **60 · 2 · %39,6 · 25 ms** | — (nokta nişan) | ✅ bilinçli tercih | 2026-09-09 |
| Kakuro | ✅ | olay güdümlü (0 · 0) | — | ✅ üretim bütçesi | 2026-09-09 |
| Vergici | ✅ | olay güdümlü (0 · 0) | — | ✅ düğüm bütçeli çözücü | 2026-09-09 |
| Toplam Kapma | ✅ | olay güdümlü (0 · 0) | — | ✅ mevcut testlerle | 2026-09-09 |
| Viraj | ✅ | **60 · 3 · %100 · 34 ms** | bekliyor (v0.36: dokunmatik) | ✅ kusur yok | 2026-09-09 |
| Filo | ✅ | **60 · 1 · %81 · 31 ms** | ✅ düzeltildi | ✅ düzeltildi | 2026-09-09 |
| Reyon | ✅ | olay güdümlü (boşta 0) · kaydırmada **60 · 0 · %6,8 · 20 ms** | ✅ adımlayıcı 48×48 dp (v0.28.1) | ✅ ölçüldü (diziliş + denetim + satış + sipariş) | 2026-09-10 |
| Raket | ✅ | **58 · 15 · %48 · 26 ms** (GPU 21 ms) | ✅ kazanç 1,24 · ölü bölge yok · iki parmak ayrı | ✅ seviyeler sıralı | 2026-09-10 |
| Tuşe | ✅ | olay güdümlü (vuruşta 21 ms, GPU 15,5) | ✅ iki parmak 15 ms arayla da sayılıyor | ✅ Sonsuz eğrisi | 2026-09-10 |
| Uçurtma | ✅ | **59 · 5 · %60 · 34 ms** (GPU 20 ms) | ✅ tutuşa yanıt ~100 ms · tel 2,3 dp / 8,2:1 | ✅ pilot eğrisi | 2026-09-10 |
| Dalgıç | ✅ | **58 · 14 · %12 · 24 ms** | ✅ 2B kazanç 1,3 · ölü bölge yok · zincir 1,5 dp / 1,54:1 | ✅ tehdit dağılımı | 2026-09-10 |
| Bostan | ✅ | **61 · 1 · %87 · 34 ms** (v0.34.1) | ✅ hücre 411 dp'de 77×77, 360 dp'de 67×47 dp | ✅ ölçek ve uzman | 2026-09-11 |
| Sincap | ✅ | **58 · 11 · %66 · 31 ms** (toplam 39,6 ms) | ⚠ erişim ipucu 1,32:1 · zıplama 150 ms | ✅ pilot ve dağılım | 2026-09-11 |
| Çekirge | ✅ | **60 · 7 · %84 · 29 ms** | ✅ eşik 290–310 ms · iki başparmak ✓ · ⚠ tükürük 1,05:1 | ✅ formasyon ve pilot | 2026-09-11 |

**E · erişilebilirlik:** tüm oyunlarda etiketsiz dokunulabilir öğe kalmadı
(tek bulgu Kıskaç'ın kolay mod anahtarıydı, düzeltildi). Kontrast CI'da
korunuyor.

19 oyunun tamamı açıldı, oynandı ve **hiçbirinde çökme yok** (`logcat` temiz).
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

**Düzeltildi (v0.24.2).** Çözücü ana kaynağa taşındı (`MinesSolver`) ve üretim
onu kullanıyor: mayın yerleşimi, tahta tahminsiz çözülebilene dek yeniden
deneniyor (en çok `PLACEMENT_TRIES` = 200 deneme, sonra son yerleşim kullanılır
ki oyun her hâlükârda başlasın).

| Zorluk | Önce tahminsiz | Sonra | Ort. tahmin (önce → sonra) | Üretim |
| --- | --- | --- | --- | --- |
| Kolay | %80 | **%100** | 0,38 → 0,00 | 1,7 ms |
| Orta | %26 | **%100** | 2,50 → 0,00 | 7,3 ms |
| Zor | %3 | **%100** | 5,67 → 0,00 | 14,0 ms |

Üretim maliyeti ihmal edilebilir (en yavaş 14 ms). `MinesStateTest`'teki
`generated boards are solvable without guessing` testi garantiyi kalıcı kılıyor;
üretimdeki kontrol kaldırıldığında kırmızıya döndüğü doğrulandı.

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

### Reyon · 2026-09-09

**D — adillik ve zorluk** (`./gradlew :games:reyon:probe`, 40 tohum/zorluk).
Diziliş için A–C cihazda henüz koşulmadı (v0.25.0 ile birlikte);
Sipariş'in cihaz koşumu aşağıda.

Üretici tek çözümü (`ReyonSolver.count == 1`) ve tahminsizliği (oyuncunun
gördüğü bilgiyle çalışan `ReyonDeducer` sonuna kadar gidiyor) üretim anında
garantiliyor; ipucu kümesi bu iki koşul korunarak en küçüğe indiriliyor.
Ölçülen, zorluk merdiveninin ne anlama geldiği:

| Zorluk | Raf | Ürün | Brif (sınır) | Verili | Yalnız tekil+ikili | +örtü | +kapasite | Tüm teknikler |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Kolay | 3×4 | 6,8 | 3,7 (6) | 1,4 | %0 | %98 | %100 | %100 |
| Orta | 4×5 | 9,6 | 7,5 (9) | 0,6 | %0 | %70 | %80 | %100 |
| Zor | 4×6 | 11,6 | 10,8 (12) | 0,0 | %0 | %63 | %88 | %100 |

Okuma: hiçbir bulmaca yalnız tekil ve ikili ipuçlarıyla bitmiyor; **örtü**
(her göz tam bir ürünle dolar: son ürün kalan boşluğa oturur) her seviyede
gerekli ve bu bilerek böyle — rafın tam dolması bulmacanın temel fikri.
Kolay'ın tamamı örtü ve kapasiteyle bitiyor; Orta'nın beşte biri, Zor'un
yaklaşık yedide biri **çoklu** teknik (marka dikey bloğunu birlikte
düşünme) istiyor. Merdiven gerçek: zorluk yalnız boyuttan değil, gereken
akıl yürütmeden geliyor.

İpucu karışımı ayarlandı: ilk ölçümde "üstünde" ipucu brifin %40'ını
kaplıyordu; ağırlıklar düşürülünce Orta'da %19'a indi ve planogram ilkeleri
(göz hizası, kategori bloğu, marka dikey, boy akışı, kategoriler ayrı, ağır
alt) brifin dörtte birinden fazlasına çıktı. Zor'da en sık ipucu artık
"yan yana" (%29).

Üretim bütçesi: deneme ort 2–7 (en kötü 26), çözücü düğümü ort 74–2626
(en kötü 56 bin), süre ort 2–16 ms (en kötü 67 ms, yalnız rapor).
`ReyonGeneratorTest.generationStaysWithinWorkBudget` sınırları 80 deneme ve
400 bin düğüm (gözlenenin 3–7 katı).

**D — Denetim modu** (`auditReport`, 40 tohum/zorluk, v0.26.0). Denetimde
adillik sorusu "sapma gerçekten görünür mü ve görünenden başka fark var mı"
diye sorulur; üretici her denetimde bunu doğrular (`ReyonAuditGenerator.verify`:
sapma maskeleri ayrık, plan ile raf yalnızca bu gözlerde ayrışır, her sapmanın
en az bir ayrışan gözü var). Ölçülen, tür karışımı ve incelik:

| Zorluk | Sapma | Tür karışımı | İnce sapma (marka/boy) | Sapma başına ayrışan göz |
| --- | --- | --- | --- | --- |
| Kolay | 2 | boş göz %44, yer değişimi %29, yabancı %28 | %0 | 1,98 |
| Orta | 3 | yer değişimi %23, yabancı %21, boş göz %19, marka %19, taşma %18 | %19 | 2,07 |
| Zor | 5 | boş göz %20, yer değişimi %18, yabancı %17, marka %16, boy %16, taşma %14 | %32 | 2,12 |

Okuma: Kolay yalnızca bariz sapmalarla (boş göz, yer değişimi, yabancı ürün)
oynanıyor; Orta marka ve taşmayı, Zor boyu ekliyor ve sapmaların üçte biri
"ince" oluyor (yalnızca renk şeridi ya da boy noktası değişir). Zorluk
merdiveni sapma sayısından çok sapmanın inceliğinden geliyor. Sapma başına
ayrışan göz sayısı 2 civarında: her sapma en az bir, çoğunlukla iki gözde
görünür.

**D — Satış modu** (`salesReport`, 40 tohum/zorluk, v0.27.0). Satışta adillik
sorusu "hedef dürüst mü" diye sorulur: hedef, tavlamalı yerel aramanın
(eşit genişlik takası, raf içi komşu takası, raf takası, ürün↔eşit genişlikli
koşu takası; 4 yeniden başlatma) bulduğu en iyi puan. Ölçülen: hedefin
başlangıç planını ne kadar geçtiği, puanın kurallara dağılımı ve iyileştiricinin
farklı tohumla aynı hedefi bulup bulmadığı.

| Zorluk | Taban ort | Hedef ort | Kazanç | Konum | Tamamlayıcı | Çakışma | Kategori | Marka | Yeniden koşum sapması | Süre |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Kolay | 89 | 96 | %8 (0–32) | %65 | %6 | %−1 | %14 | %16 | %0,0 | 18 ms |
| Orta | 132 | 151 | %15 (3–29) | %63 | %7 | %−2 | %12 | %20 | %0,0 | 36 ms |
| Zor | 172 | 189 | %11 (1–23) | %61 | %6 | %−1 | %13 | %20 | %0,0 | 70 ms |

Okuma: iyileştirici farklı tohumla 10 turun 10'unda **aynı** hedefi buluyor;
bu boyutta arama uzayı küçük, hedef büyük olasılıkla gerçek en iyi. Yani
"hedefi geç" nadir ama mümkün bir olay değil, hedef bir tavan. Puanın
%61–65'i konumdan (talep × yüz × raf çarpanı), üçte biri komşuluk
kurallarından geliyor: önce doğru rafa koymak, sonra komşuluğu düzeltmek
diye okunur. Çakışma payı hedefte bile sıfır değil: raf genişlikleri bazen
temizlik ile gıdayı aynı rafa zorluyor. Üretim süresi masaüstünde en çok
70 ms; telefonda arka planda hesaplanır, dönen gösterge var.

**E — küçük ekran** (v0.27.1, masabaşı geometri + Robolectric). Cihaz
koşumundan önce yerleşim hesabı iki sorun gösterdi. (1) Blok adı puntosu blok
yüksekliğinden türetilip 8 sp tabanına dayanıyordu; 360 dp genişlikte Zor
rafında blok 33 dp, Denetim planında 26 dp yüksekliğinde kalıyor, "Bulaşık
deterjanı" gibi adlar tek yüzlü gözde (yazı alanı ~41 dp) üç noktayla
kırpılıyordu. (2) Denetimde iki tuval sabit en-boy oranıyla diziliyordu;
640 dp yükseklikte Orta rafı sığmayınca raf tuvali daralıp sola yaslanıyor,
bulunanlar listesi sıfır yükseklik alıyordu. Düzeltme: ad, bloğa
sığdırılıyor (en büyük puntodan başlayarak tek satır, sonra boşluktan iki
satır, en küçük puntoda %72'ye kadar yatay daraltma, üç nokta en son;
`BlockLabeler.fit`); Denetim yerleşimi yükseklik bütçesinden hesaplanıyor
(raf satırı göz genişliğinin 0,62–0,90 katı, plan satırı rafın 0,85'i; taban
katsayıda bile sığmazsa iki tuval birlikte daraltılıp ortalanıyor;
`auditLayout`) ve plana dokununca büyütülmüş plan açılıyor. 360 dp'de Zor
planında en dar durum yazı alanı 45 dp, ad bölgesi 19 dp, 8 sp; 35 adın hepsi
(TR ve EN) bu alanda kırpılmadan sığıyor (`ReyonBlockLabelTest`, gerçek yazı
ölçümüyle). Cihazda doğrulama Sipariş koşumunda yapıldı: blok adları 360 dp'de
kırpılmıyor, ama menü kartının kendisi ekrandan taşıyor (aşağıda, bulgu 1).

**D — Sipariş modu** (`orderReport`, 40 tohum/zorluk, v0.28.0). Siparişte
adillik sorusu "hedef dürüst mü ve ulaşılabilir mi" diye sorulur. Hedef,
oyuncunun gördüğü bilgiyle (tahmin ortası ve aralık) çalışan uzman
politikanın aynı haftadaki kârı; ölçüm onu üç referansla karşılaştırır:
hiç sipariş vermeyen (yalnız başlangıç stoğunu satan), tahmin ortasını
emniyetsiz karşılayan naif politika ve gerçekleşen talebi bilen kâhin
(oyuncu bilmez; üst sınır).

| Zorluk | Gün | Ürün | Sipariş yok | Naif | Uzman | Kâhin | Uzman/kâhin | Kayıp satış | Hizmet | Devir (kâhin) |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Kolay | 5 | 6,7 | 101 | 319 | 319 | 321 | %99 | %4,1 | %95 | 8,1 (7,1) |
| Orta | 6 | 9,7 | 152 | 616 | 613 | 629 | %98 | %6,9 | %92 | 11,2 (9,5) |
| Zor | 7 | 11,6 | 185 | 901 | 889 | 921 | %96 | %7,9 | %92 | 13,1 (11,6) |

Okuma: uzman, tam bilgili kâhinin %96–99'una geliyor; yani tahmin
belirsizliğinin bedeli küçük ve hedef bir tavan değil, ulaşılabilir bir
çıta. Naif politika (yarının tahmin ortasını karşıla, emniyet ve sığma
düşünme) uzmanla başa baş (±%1): parametre taraması (emniyet payı 0–1,5 ×
aralık, sipariş eşiği, sığma payı) kâr yüzeyinin tepede düz olduğunu
gösterdi; uzmanın ayırt edici yanı daha yüksek devir (aynı kâra daha az
stokla). Kârın bileşimi uzmanda: bekleme marjın ~%14'ü, iade %1'in altı,
fire sıfıra yakın (bozulan ürünler yalnızca fazla siparişi cezalandırır;
bu yüzden raf ömürleri 2–4 güne çekildi). Kayıp satış %4–8: tahmin
aralığı ±%25–35 iken bir kısım talep kaçınılmaz olarak karşılanamıyor,
hizmet düzeyi %92–95. Üretim süresi ölçülemeyecek kadar kısa (uzman
simülasyonu dahil 1 ms altı). Cihaz koşumu (A–C) aşağıda.

### Reyon · Sipariş cihazda · 2026-09-10

A–C aşamaları SM-A515F'te, sürüm derlemesiyle ve **Serbest** modda koşuldu.
Sürücü, `uiautomator` dökümünden okuyup dokunan yerel bir betik: siparişleri
oyuncunun gördüğü bilgiyle (stok, tahmin aralığı, koli boyu) veren naif
politika — "yarının tahmin ortasını karşıla, rafa sığdır".

**A — koşum.** Orta zorlukta iki hafta baştan sona oynandı. İlk koşumda politika
yalnızca ekranda duran satırları gördüğü için listenin üstündeki ürünler hiç
sipariş edilmedi: 357/675 kâr (**%52**), hizmet düzeyi %45. İkincisinde liste
her günün başında başa sarıldı ve on ürünün hepsi karşılandı: **731/797 kâr
(%91)**, ★★☆, stok devri 16,5 (uzman 13,1), hizmet %83 (uzman 93). Yani hedef
cihazda da ulaşılabilir bir çıta; ölçüm koşumunun "naif politika uzmanla başa
baş" sonucu parmakla da doğrulanıyor ve kaybın tamamı **taranmayan raftan**
geliyor, karardan değil.

Gün kapanışı kartı, hafta sonucu kartı, rekor yazımı ("Yeni rekor!"), geri
tuşuyla hub'a çıkış, arka plana alıp geri dönme ve **süreç ölümü**
(`am force-stop`) sınandı: hafta, gün ve bekleyen koliler üçünde de aynen geri
yüklendi. `logcat` `AndroidRuntime:E` boş, çökme yok (kalan tek uyarı
`OnBackInvokedCallback` etkin değil bilgisi; öngörülü geri kullanılmıyor).
Sipariş'in günlük modunda deneme hakkı yoktur — günün en iyi kârı kaydedilir —
yani ölçüm hak tüketmez.

**A — bulgu 1: 360 dp'de menü kartı ekrandan taşıyor (engelleyici).**
`wm size 1080x1920` + `wm density 480` (sw360dp, uygulamaya 562 dp yükseklik)
ile Reyon menü kartının altı ekranın dışında kalıyor: zorluk açıklaması, mod
bilgisi, **"Haftaya başla"** ve "Menüye dön" ne dökümde görünüyor ne
dokunulabiliyor. Kart kaydırılamadığı için mod **hiç başlatılamıyor**. Dört
türün hepsinde aynı (`OverlayCard`'ın yükseklik bütçesi yok, içerik
kaydırılmıyor); Sipariş en kötüsü, brifi 360 dp'de dokuz satır. Oyun ekranının
kendisi 360 dp'de sağlam: raf tuvali, gün başlığı (iki satıra sarıyor), sipariş
listesi ve alt eylem satırı sığıyor — bir buçuk satır görünüp liste kayıyor.

**A — bulgu 2: "Denetim" çipi 360 dp'de kırpılıyor.** Dört tür çipi satırı eşit
paylaştığı için 360 dp'de ikinci çip **"Deneti"** diye kesiliyor, üç nokta da
konmuyor (`KindChips`, `compact = true`). 411 dp'de dördü de tam sığıyor.

**A — bulgu 3: alt eylem satırı erişilebilirlik ağacında sınırsız.** "İpucu" ve
"Günü kapat" düğümleri ağaçta var ama sınırları `[0,0][0,0]`. Sebep: uygulama
sistem çubuklarını gizliyor (`hide(systemBars())`) ve 2400 px'in tamamına
çiziyor, oysa pencerenin bildirilen uygulama sınırı **2186 px**'te bitiyor (üç
düğmeli gezinme şeridi). 2186'nın altına düşen her şeyin sınırı sıfırlanıyor:
kural metni tam orada kırpılıyor, eylem satırı bütünüyle altta kalıyor. Sonuç,
TalkBack'in dokunarak keşfi bu iki düğmeye inemez ve
`tools/cihaz_testi.py erisim` onları **sessizce atlar** — sıfır sınırlı
düğümleri elediği için bu ekran "10 dokunulabilir öğe, etiketsiz 0" diye temiz
görünüyor. Gözle ve parmakla düğmeler çalışıyor (y = 2306'ya dokunmak günü
kapatıyor); sorun ölçümün ve ekran okuyucunun onları görmemesi.

**B — kare hızı** (sürüm derlemesi).

| Pencere | Kare | Okuma |
| --- | --- | --- |
| boşta 15 s, dokunmadan | **0** | olay güdümlü; beklenen sonuç |
| liste kaydırma, 10 × 700 ms sürükleme | 468 / 8,0 s | pencere ortalaması 58,5 kare/s |
| adımlayıcıya 16 durum değiştiren dokunuş | 76 / 1,5 s | dokunuş başına ~4,8 kare |

Kaydırma penceresinde `framestats`: kesintisiz çizim aralığı ortanca **16,6 ms
= 60,1 kare/s**, kaçan vsync **0**, jank %6,8, p50 20 ms. Pencere ortalamasının
58,5'te kalması kare düşmesi değil, sürüklemeler arasındaki çizim boşlukları
(119 aralıktan 4'ü 25 ms üstü ve hepsi sürükleme sınırında). Adımlayıcıda p50
22 ms, kaçan vsync 0; dokunuş başına birkaç kare düğme dalgacığı.

**C — giriş kalibrasyonu.** Sipariş'te sürükleme yok; kalibre edilecek şey koli
adımlayıcısının **dokunma hedefi**. Çizilen düğüm 44,2×32,0 dp (semantik sınır
= çizilen sınır, E3'teki uyarı). Gerçek dokunma bandı, düğüm merkezinden dp
kaydırarak dokunup koli sayacına bakarak ölçüldü (her denemede satır sıfırlanıp
tek dokunuş):

| Eksen | ±20 dp | ±22 dp | ±24 dp | ±26 dp |
| --- | --- | --- | --- | --- |
| yatay | ✓ | ✓ | ✓ | — |
| dikey, yukarı | ✓ | ✓ | ✓ | — |
| dikey, aşağı | ✓ | — | — | — |

Etkin dokunma alanı **48 dp geniş × 44 dp yüksek** (24 dp yukarı, 20 dp aşağı).
Yatayda Material'ın 48 dp tabanı tutuyor; dikeyde 4 dp eksik, çünkü büyütme
satır kartının alt kenarında kesiliyor — düğüm kartın alt kenarının yalnızca
6 dp üstünde duruyor. Bu yoğunlukta 48 dp = 7,6 mm, 44 dp = 7,0 mm.

Işkalayan dokunuş komşu ürünü oynatmıyor: bandın dışına dokunmak hiçbir sayacı
değiştirmedi, yani hatanın bedeli yalnızca bir kez daha dokunmak.

Hızlı dokunuş sadakati: arka arkaya (beklemesiz) üç dokunuş üç kez denendi, her
seferinde 0→3 koli ve 3→0 koli — düşen dokunuş yok.

**Düzeltmeler (v0.28.1).** Bulgu 1: `OverlayCard` kabın yüksekliğine sığmazsa
içi kayıyor (yükseklik sınırsızsa kaydırma eklenmez); dört Reyon brifi
kısaltıldı. Robolectric 360×640'ta Sipariş menüsünde "Haftaya başla" ve "Menüye
dön" kaydırılıp görünür alana geliyor (`ReyonAuditLayoutTest`). Bulgu 2:
`KindChips` kart içi genişlik 280 dp'nin altındaysa (411 dp'de 299, 360 dp'de 248) 2×2 diziliyor, çip metni
sığmazsa üç nokta; 411 dp'de tek satır korunuyor. Bulgu 3: dokunarak keşif
açıkken uygulama sistem çubuklarını gizlemiyor (`MainActivity`, dinleyiciyle
canlı), içerik çubukların üstünde kalıyor; `cihaz_testi.py erisim` sıfır
sınırlı dokunulabilir düğümleri sayıp listeliyor ("sınırı sıfır … 0"
beklenir). C: sipariş satırının alt payı 10 dp, adımlayıcının 48 dp dokunma
alanı kartın kırpma sınırında kesilmiyor. Cihazda doğrulanacaklar: TalkBack
açıkken `erisim` çıktısında sıfır sınırlı düğüm 0; adımlayıcı dikey bandı
±24 dp; 360 dp'de menü kaydırılarak başlıyor.

**Doğrulama turu (v0.28.1, aynı cihaz, kullanıcının kendi sürüm derlemesi).**

- **Adımlayıcı bandı ✅.** İki eksende de ±24 dp kayıt alıyor, ±26 dp almıyor:
  etkin dokunma alanı **48×48 dp** (önce 48×44). Satır kartının alt payı 9,9 dp
  ölçüldü; büyütme artık kartın kenarında kesilmiyor.
- **360 dp'de menü ✅.** Kart kayıyor ve dört türün başlatma düğmesi de kaydırma
  sonrası geliyor: Sipariş "Haftaya başla" (dokunuldu, hafta başladı), Diziliş
  "Başla", Denetim "Denetime başla", Satış "Dizmeye başla" — "Menüye dön" de
  erişilebilir. Tür çipleri 360 dp'de 2×2 diziliyor ve dört ad da tam;
  "Denetim" artık kırpılmıyor.
- **TalkBack açıkken `erisim` ⚠️ yarım.** Dokunarak keşif açılınca uygulama
  çubukları gerçekten gösteriyor (ekranda doğrulandı, içerik çubukların üstünde)
  ve tarayıcı sıfır sınırlı düğümleri artık sayıp listeliyor. Ama sayı **0
  değil, 2**: "İpucu" ve "Günü kapat" TalkBack açıkken de `[0,0][0,0]` geliyor.

  Ölçülen sebep, çubukları göstermenin çözemediği bir çerçeve kayması: çubuklar
  görünürken uygulama alanı ekranda y = 88…2274, erişilebilirlik pencere
  dikdörtgeni ise **(0,0)–(1080,2186)** — yani uygulama sınırının *boyutu*
  (2274 − 88) y = 0'a çakılmış. Alanın **son 88 px'i (33 dp)** ağaçtan düşüyor;
  eylem satırının dolgusu tam orada (ölçülen y = 2190…2266), kural metni de
  2186'da kırpılıyor. Bu ROM'da çare çubukları göstermek değil, **son 33 dp'ye
  dokunulabilir öğe koymamak**: dokunarak keşif açıkken alta durum çubuğu
  kadar ek pay vermek ya da eylem satırını kural metninin üstüne almak.

  **Düzeltme (v0.28.2):** dokunarak keşif açıkken uygulama kökü alta durum
  çubuğu yüksekliği kadar pay veriyor (`MainActivity.ExplorationInset`); içerik
  2186 px'te bitiyor, eylem satırı bandın üstünde kalıyor. Cihazda
  doğrulanacak: TalkBack açıkken `erisim` → "sınırı sıfır … 0".

**Hafta grafiği (v0.28.2, cihazda).** Kolay bir hafta sonuna kadar oynanıp sonuç
kartı 411 dp ve 360 dp'de bakıldı: çubuklar (1. gün +32, 2. gün +26, kalan üç
gün 0), biriken devir çizgisi kartın yazdığı sayıya varıyor (12,5), uzman devri
kesikli çizgide (3,9). 360 dp'de kart kayıyor; grafik de altındaki düğmeler de
okunuyor. Ekran okuyucu açıklaması günleri kârıyla veriyor: "Hafta grafiği:
1. gün +32, 2. gün +26, … · stok devri 12,5 (uzman 3,9)". Çökme yok, `logcat`
temiz.

### Raket · 2026-09-10

**D — bilgisayar seviyeleri** (`./gradlew :games:raket:probe`, 30 maç/hücre,
v0.29.0). Bilgisayar raketi bir bottur: tepki gecikmesi, hız sınırı ve nişan
hatası. Aynı sınıf testte alt raketi süren üç "oyuncu botu" olarak da
kullanılır (zayıf: topu izler, 0,95 birim/s, 0,26 s; orta: tahmin eder,
1,4 birim/s, 0,14 s, hata 0,28; güçlü: 2,4 birim/s, 0,05 s, hata 0,08), ve
seviyeler bu botlara karşı ölçülür.

**Bulgu 1 (düzeltildi).** İlk ölçümde tahmin eden iki raket sonsuz ralli
yapıyordu: orta bot orta ve zor seviyeye karşı 30 maçın hiçbirini bitiremedi
(600 s tavan, 900+ vuruşluk ralliler). Nişan hatası raketin yarı genişliğinin
0,3 katı, yani her zaman raketin içinde kalıyordu; hız tavanı da kimseyi
kaçırtmıyor (tavan hızda düz top 0,63 s'de karşıya varıyor, orta raket o sürede
kortu geçiyor). **Düzeltme:** hata normal dağılımlı ve top hızıyla büyüyor
(tavanda taban hatanın 4,5 katı; `RaketAi.ERROR_SPEED`), hız rampası %4,5'ten
%6'ya çıktı (tavana 17 vuruşta). Böylece yavaş topu herkes karşılıyor, tavan
hızda orta bir raket üç dönüşten birini kaçırıyor.

| oyuncu botu | seviye | bot galibiyeti | ort. skor | ort. süre | ort. ralli/sayı |
| --- | --- | --- | --- | --- | --- |
| zayıf | kolay | %76 | 10,3–6,6 | 106 s | 2,2 |
| zayıf | orta | %0 | 0,8–11,0 | 126 s | 5,5 |
| zayıf | zor | %0 | 0,0–11,0 | 123 s | 6,1 |
| orta | kolay | %100 | 11,0–0,3 | 103 s | 4,5 |
| orta | orta | %53 | 9,5–8,9 | 332 s | 12,6 |
| orta | zor | %0 | 1,2–11,0 | 249 s | 15,7 |
| güçlü | kolay | %100 | 11,0–0,0 | 110 s | 5,5 |
| güçlü | orta | %100 | 11,0–0,0 | 217 s | 15,8 |
| güçlü | zor | %96 (1 bitmedi) | 11,0–0,0 | 429 s | 43,4 |

Okuma: kolay, topu izleyen acemi bota bile çoğunlukla yeniliyor; orta, orta
botla başa baş; zor orta botu hiç kaçırmıyor ama çok hızlı ve isabetli bir
oyuncuya yeniliyor (kenar vuruşu tavan hızda risklidir). Orta ve zor maçlar
4–6 dakika sürüyor; kolay 2 dakikadan kısa. Değişmez teste çevrilen: orta bot
kolayı ≥ %70 yener, zora ≤ %35 yenilir, seviyeler sıralı, her maç biter
(`aiLevelsAreOrderedAndTheEasyOneIsBeatable`). Ayrıca tavan hızda 210
konumda vuruşun kaçmadığı (`noTunnelingAtTopSpeed`) ve falsonun raket hızından
geldiği doğrulanır. Cihaz koşumu (A–C) aşağıda.

### Tuşe · 2026-09-10

**D — Sonsuz eğrisi** (`./gradlew :games:tuse:probe`, 20 tohum/bot, v0.30.0).
Sonsuz'da akış hızı vurulan karo başına artar (3,2 + 0,045 × karo, tavan 11
satır/s); koşu, oyuncunun temposu akış hızının altında kalınca biter. Botlar
karoyu gördükten bir tepki süresi sonra, sınırlı bir tempoyla dokunur:

| bot | tepki | tempo | karo | süre |
| --- | --- | --- | --- | --- |
| acemi | 0,28 s | 4,5/s | 16 | 4,8 s |
| orta | 0,18 s | 7/s | 73 | 15,3 s |
| hızlı | 0,12 s | 9,5/s | 116 | 20,8 s |
| uzman | 0,08 s | 12/s | 213 | 30,1 s |

Okuma: skor yaklaşık (tempo − 3,2) / 0,045'e dayanıyor, yani tavana kadar
tempo belirleyici; 11 satır/s tavanı (saniyede 11 dokunuş) insan sınırının
üstünde, sonsuz koşu yok. Klasik ve Günlük'te akış olmadığı için ölçüt
yalnızca dokunuş hızı; 50 karo iyi bir oyuncuda 8–10 s. Değişmez teste
çevrilen: sıradaki karo tamamen çıkana dek vurulabilir (`arcadeTileCanBeTappedUntilItFullyLeaves`),
hız karo başına artar ve kaçan karo koşuyu bitirir. Cihaz koşumu (A–C),
dokunuş–nota gecikmesi ve iki parmak ölçümü aşağıda.

### Raket · cihazda · 2026-09-10

Sürüm sayfasındaki v0.30.0 APK'sıyla, SM-A515F'te.

**A — koşum.** Üç mod da baştan sona oynandı: duvar (ralli + "Top kaçtı" kartı),
iki kişi (11–2, "Alt oyuncu kazandı"), bilgisayar/Kolay (9–11, "Bilgisayar
kazandı"). Ana ekrana alıp dönünce skor korunuyor ve oyun kendiliğinden
duraklıyor ("Devam et"); geri tuşu hub'a çıkarıyor. `logcat` `AndroidRuntime:E`
boş.

**B — kare hızı.** 12 s'de 700 kare (58,3 kare/s), kaçan vsync 12, jank %35,7,
p50 25 ms; 15 s'de 869 kare (57,9 kare/s), kaçan vsync 15, jank %48, p50 26 ms.
Faz dökümü: **GPU 21,0 ms** (90p 22,5), çizim kaydı 1,2 ms, girdi→traversal
1,0 ms, toplam 25,8 ms. Aşırı çizim ölçüldü: kortun %97,7'si 2×, %0,6'sı 4×+ —
yani sorun katman sayısı değil, dolgu. Okuma: kare hızı hedefin sınırında ama
Raket, kütükteki **kaçan vsync'i sıfırdan belirgin biçimde ayrılan ilk oyun**
(saniyede ~1 kare). GPU 16,7 ms bütçesinin üstünde; bakılacak yer tam ekran
gradyan + topun hâlesi ve izi (`drawCircle` alfa katmanları).

**C — sürükleme kazancı.** Parmak yolu → raket hareketi (medyan, sayı olunca
raket ortaya döndüğü için yönü tutmayan örnekler elendi):

| parmak | 10 px | 20 px | 40 px | 80 px | 160 px |
| --- | --- | --- | --- | --- | --- |
| raket | 12,1 px | 25,0 px | 49,5 px | 98,9 px | 199,1 px |
| kazanç | 1,21 | 1,25 | 1,24 | 1,24 | 1,24 |

Koddaki 1,25 cihazda birebir çıkıyor ve **ölü bölge yok**: 10 px'lik (3,8 dp)
parmak yolu bile rakete geçiyor — olaylar doğrudan okunduğu için Filo'daki 8 dp
dokunma toleransı burada ödenmiyor. Raketin gidebildiği aralık 143 → 936 px
(793 px); uçtan uca parmak yolu 793 / 1,25 = 634 px = **38 mm**, yani tek
başparmak hamlesiyle geçilebiliyor (Filo'da düzeltme sonrası 41 mm; sınır
55 mm).

**C — iki parmak aynı anda.** `adb shell input` tek parmak enjekte ediyor,
`sendevent` ise SELinux yüzünden reddediliyor ("Permission denied"; shell
`/dev/input`'a yazamıyor). Bu yüzden CTS'in yolu kullanıldı: `uinput` ile sanal
bir dokunmatik kaydedilip olaylar oradan basıldı
([`tools/coklu_dokunus.py`](../tools/coklu_dokunus.py)). Tek jestte iki parmak,
zıt yönlere:

| deneme | üst parmak | üst raket | alt parmak | alt raket |
| --- | --- | --- | --- | --- |
| 1 | −260 px | −330 px | +260 px | +325 px |
| 2 | +260 px | +330 px | −260 px | −325 px |
| 3 | −260 px | −342 px | +260 px | +325 px |

İki raket aynı anda ve birbirinden bağımsız sürülüyor; taraf ataması ekranın
ortasına göre doğru çalışıyor ve kazanç iki tarafta da 1,25. (Raket duvara
dayanmışsa o parmak 0 hareket veriyor — kırpma beklenen davranış.)

**Ses ve titreşim.** 8 saniyelik rallide HAL'in `fast_out` akışı 137 satır
günlük bastı; titreşim geçmişinde `com.za.games` için 45–50 ms'lik TOUCH
darbelerinden 51 kayıt var. İkisi de çalışıyor.

**360 dp.** Menü kartı: mod çipleri, zorluk çipleri ve "Başla" görünüyor,
"Menüye dön" kaydırınca geliyor.

### Tuşe · cihazda · 2026-09-10

**A — koşum.** Klasik/Türk Marşı 50 karo baştan sona vuruldu ("Bitti!", rekor
kartı; sürücünün temposu 0,3 karo/s olduğu için süre 163,79 s). Arka plandan
dönünce durum korunuyor, geri tuşu hub'a çıkıyor, `logcat` temiz.

**B — kare hızı.** Klasik oyuncunun temposuyla ilerlediği için çizim yalnız
karo animasyonunda sürüyor: 161 s'lik turda 6777 kare, p50 21 ms, kaçan vsync
44 (saniyede 0,3), jank %7,7. Dokunuşlu bölümün faz dökümü: toplam 21,4 ms
(90p 21,8), GPU 15,5 ms, çizim kaydı 1,8 ms, girdi→traversal 2,8 ms.

**C — dokunuş ile nota arasındaki gecikme.** Nota, dokunuşu işleyen aynı
çağrıda çalınıyor; ölçülebilen iki parça: (1) girdi→kare tamamlandı **21,4 ms**
(medyan; 90p 21,8), (2) ses yolu — cihazın miksleri 48 kHz, hızlı çıkış
periyodu 4 ms ve gecikmesi **7,96 ms**, ve notalar çalarken HAL günlüğü akışın
`fast_out` üzerinde olduğunu yazıyor ("This stream has 1 tracks"). Yani nota,
dokunuştan ~10–20 ms sonra ses yoluna giriyor; **akustik uçtan uca gecikme
mikrofon olmadan ölçülemez**, bu kadarı yazılım tarafının temiz olduğunu
gösterir. Not: notalar 22.050 Hz üretiliyor, mikser 48.000 Hz — SoundPool yine
de hızlı yola girdi, ama 48 kHz üretmek yeniden örneklemeyi tamamen kaldırır.

**C — iki parmakla art arda dokunuş.** Sıradaki iki karonun şeridi ekrandan
okunup ikisine üst üste dokunuldu:

| dokunuşlar arası | sonuç |
| --- | --- |
| 116–146 ms (`input tap` ×2) | 4/4 denemede ikisi de sayıldı (+2) |
| 30 ms (`uinput`) | 2/2 (+2) |
| 15 ms (`uinput`) | 2/2 (+2) |
| aynı çerçevede (iki parmak birlikte iniyor) | 2/2 (+2) |

Farklı şeritlerde de tutuyor; dokunuşlar kaybolmuyor.

**360 dp.** Menü kartı "Başla"ya kadar sığıyor, "Menüye dön" kaydırınca geliyor.

**Ezgiler.** Nota dizileri elle okundu; Türk Marşı'nın girişi Rondo alla
Turca'nın ilk üç ölçüsüyle birebir:

```
ölçülen:  B4 A4 G#4 A4 C5 | D5 C5 B4 C5 E5 | F5 E5 D#5 E5 B5
beklenen: B4 A4 G#4 A4 C5 | D5 C5 B4 C5 E5 | F5 E5 D#5 E5 B5
```

Devamı (A5 G#5 A5 B5 · A5 G#5 A5 C6 A5) aynı figürün sadeleştirilmiş hâli —
yanlış değil, düzenleme. Öbürlerinin girişleri de yerinde: Daha Dün Annemizin
(C C G G A A G), Mutlu Yıllar (G G A G C B), Neşeye Övgü (E E F G G F E D C C
D E E D D), Für Elise (E5 D#5 E5 D#5 E5 B4 D5 C5 A4), Menuet (D5 G A B C D G
G), Greensleeves (A C D E F E D B G).

**Tını (ölçüm).** `NoteSynth` çıktısı çözümlendi (A4): süre 0,50 s, tepe 0,565
(kırpma yok), harmonikler temele göre **0 / −6,9 / −13,7 / −20,9 / −27,7 dB**
ve beşincinin üstünde bileşen yok; zarf 9 ms'de tepe yapıyor, 160 ms'de
yarılanıyor, sonunda −22 dB. Okuma: yumuşak, koyu ve kısa bir ton; atak
gürültüsü ve 2,2 kHz üstü bileşen olmadığı için gerçek piyanonun yanında
"boğuk" duyulur. Beğenilmezse iki düğme de tek yerde: `NoteSynth.HARMONICS`
(daha çok ve daha güçlü üst harmonik) ve `exp(-t * (3,5 + 2k))` sönümü (küçük
katsayı = uzun kuyruk). Kulakla doğrulama için notalar ve ezgiler WAV olarak
üretilip dinlenmek üzere gönderildi.

### Uçurtma · 2026-09-10

**D — pilot ölçümü** (`./gradlew :games:ucurtma:probe`, 20 uçuş/pilot,
v0.31.0). Pilot tepki süresinde bir tüm ekrana bakar, ilerideki ilk engel
kümesinin uçurtmaya en yakın yeterli boşluğunu hedefler, rakibin üstüne
çıkar; sönümlü kontrolle (0,25 s ileri bakış) salınmaz.

**Bulgu 1 (düzeltildi).** İlk ölçümde uzman pilot bile ortalama 80 m'de
düşüyordu; ölümlerin çoğu tel ve ip. İki neden: pilotun sönümsüz aç-kapa
kontrolü 0,4 birimlik salınım yapıyordu (v²/2a) ve yalnızca 0,35 birim
ileri bakıyordu; tavan hızda tel ekranda belirdiğinde 0,5 s kalıyor, bu
sürede en çok 0,3 birim dikey yol alınabiliyor. Pilot düzeltildi. Ayrıca
tavanda uçmak her şeyden kaçıyordu: **yüksek tel** eklendi (y 0,14–0,30,
üstünden geçilemez). Rakip yatay süzülmesi 0,06–0,18'e indirildi ve telden
sonra rakip gelmiyor: rakip sola süzülerek önceki öbeğe girer, yüksek telle
çakışsa kaçış kalmazdı.

| pilot | tepki | ort. m | en az | en çok | ort. skor | çatı | tel | ip |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| acemi | 0,30 s | 735 | 205 | 1328 | 1155 | 1 | 18 | 1 |
| orta | 0,18 s | 1046 | 556 | 2010 | 1692 | 2 | 17 | 1 |
| uzman | 0,08 s | 1153 | 560 | 2536 | 1868 | 2 | 18 | 0 |

Ekipman (orta pilot): kuyruk 991 m / 1583 puan, makara 1049 / 1684, cam
tozu 1079 / 2213 (kesme bonusu 50). Okuma: ölümlerin neredeyse tamamı tel;
çatı ve ip nadiren. Tel, tavan hızda (10,4 m/s, 1500 m sonrası) ekranda
0,5 s göründüğünden reaksiyonu belirleyen engel; pilotlar 700–1150 m
arasında, insan için 300–600 m makul bir ilk hedef. Değişmez teste
çevrilen: geçilebilirlik (her sütunda ≥ 0,3 birim boşluk, 3 mesafede 12
tohum), kesme kuralı ve pilotun açılışı geçmesi. Cihaz koşumu (A–C) aşağıda.

### Uçurtma · cihazda · 2026-09-10

Sürüm sayfasındaki v0.31.0 APK'sıyla (aynı imza, yerinde güncelleme).

**A — koşum.** Serbest modda birkaç uçuş yapıldı. Hiç dokunulmayan uçuş 20 m'de
çatıya çarpıyor ve kart geliyor ("Çatıya çarptın", mesafe · kurdele · kesme,
Paylaş / Yeniden başlat / Başa dön / Menüye dön). Enjekte edilen basılı-tut
örüntüleriyle 49–84 m uçuldu; bir uçuşta "tel altı 2/2" görevi tamamlandı. Ana
ekrana alıp dönünce mesafe ve uçuş korunuyor, geri tuşu hub'a çıkıyor, `logcat`
`AndroidRuntime:E` boş. Günlük modun günde üç denemesi olduğu için ölçümler
Serbest'te yapıldı.

**B — kare hızı.** Uçuş süren 7 s'lik pencerede 414 kare (**59,1 kare/s**),
kaçan vsync 5, jank %60, p50 34 ms, p90 40 ms. Fazlar: **GPU 20,0 ms** (90p
29,9), çizim kaydı 1,4 ms, girdi→traversal 1,6 ms, toplam 26,6 ms. Raket'le
aynı desen: kare hızı hedefi tutuyor ama GPU 16,7 ms bütçesinin üstünde ve p50
34 ms ile kütüğün en yüksek kare gecikmesi (Viraj'la aynı). Sebep aynı yerde
aranmalı: tam ekran gökyüzü gradyanı + bulut/bina katmanları.

**C — basılı tutma gecikmesi.** 60 kare/s ekran kaydı alınıp uçurtmanın y'si
kare kare izlendi; dokunuşlar `uinput` ile bilinen örüntüyle basıldı (1000 ms
tut / 700 ms bırak).

| ölçüt | değer |
| --- | --- |
| parmak indi → gözle görülür ilk hareket (10 px ≈ 3,8 dp) | 83–119 ms (medyan ~100 ms) |
| bunun sistem payı (girdi→kare tamamlandı) | ~26 ms |
| yerden tavana çıkış | ~2,1 s (1330 px) |
| yükseliş / alçalış hızı (90p) | 0,72 / 0,90 birim/s (VMAX 1,15) |

Okuma: kontrol aç-kapa değil rampalı; kısa tutuşlarda tavan hıza varılmıyor ve
parmak kalkınca dönüş yumuşak. Gecikmenin büyük kısmı fizik rampası, sistem
payı değil. **His yargısı ölçümle verilemez**: gecikme ve rampa bunlar, ama
"iyi hissettiriyor mu" sorusunu gerçek bir parmak yanıtlar.

**C — tel görünürlüğü.** Aynı kayıttan telin dikey kesiti:

- koyu çekirdek **6 px = 2,3 dp**, hemen üstünde 2 px beyaz vurgu
- gökyüzüne karşı kontrast **8,2:1** (gök 156,211,248 · tel 32,49,71); grafik
  ögeleri için WCAG eşiği 3:1
- telin sağ kenardan uçurtma sütununa (732 px) gelişi: ölçülen kayma hızı
  **850 px/s = 0,80 birim/s** (BASE_SPEED) → **0,86 s**; tavan hızda
  (1,3 birim/s) **0,53 s**. D bölümündeki "tavan hızda 0,5 s" cihazda doğrulandı.
- o 0,53 s'de uçurtmanın alabileceği dikey yol ≈ 0,31 birim.

Yani telin sorunu kalınlık ya da kontrast değil, **süre**: çizgi ince ama koyu
ve beyaz vurgusuyla ayırt ediliyor; öldüren şey ekranda kaldığı yarım saniye.
Görünürlük artırılacaksa kalınlıktan çok erken uyarı (direk gölgesi, telin
ekrana girmeden önce beliren işareti) işe yarar.

**360 dp.** Menü kartı görev listesi + ekipman satırıyla ekranı aşıyor:
"Başla" ve "Menüye dön" ilk ekranda görünmüyor, **bir kaydırmayla geliyor** ve
kart içeriği kırpılmıyor. (0.28.1'deki `OverlayCard` kaydırması burada da
tutuyor.)

**Gerçek zorluk hissi — ölçülemedi.** Sürücü insan değil: ekranı okuyup karar
veremediği için sabit örüntüyle uçtu ve 20–84 m'de kaldı; motor içi pilot
ölçümü ise 700–1150 m. İkisi de bir insanın ne yaşayacağını söylemiyor.
Cihazda söylenebilecek olan tepki bütçesi: **tel ekrana girdikten sonra
0,53–0,86 s**, o sürede en çok ~0,3 birim dikey yol. Gerisi gerçek bir el ister.

> Ölçüm notu: sonuç kartı ekranın ortasını kapladığı için kör enjekte edilen
> basılı-tutuşlar iki kez "Paylaş" düğmesine denk geldi ve sistem paylaşım
> sayfasını açtı. Uçuş örüntüleri kartın üstünde kalan bir noktaya (y ≈ 600)
> alınmalı.

### Dalgıç · 2026-09-10

**D — pilot ölçümü** (`./gradlew :games:dalgic:probe`, 20 dalış/pilot,
v0.32.0). Pilot en yakın dalgıca gider, yük dolunca ya da oksijen 9 s'nin
altına inip elinde dalgıç varsa yüzeye çıkar, önündeki tehditten bir şerit
kayarak kaçar.

**Bulgu 1 (pilot).** İlk sürümde pilot oksijen azalınca dalgıçsız da yüzeye
çıkıyordu; 60 can kaybının 20–25'i "boş yüzeye çıkış"tı. Kural doğru (yüzeyi
bedava oksijen deposu yapmanın bedeli), pilot yanlıştı: dalgıç yoksa sonuna
dek aramak, can bedeli aynı olduğundan hep daha iyi. Düzeltildi. Ayrıca ilk
dalga yumuşatıldı: doğum aralığı 1,5 → 1,7 s, köpekbalığı hızı 0,25–0,40 →
0,22–0,36 birim/s.

| pilot | tepki | ort. skor | ort. dalga | ort. dalgıç | ort. süre | köpekbalığı | düşman | torpido | mayın | oksijen | boş çıkış |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| acemi | 0,35 s | 1566 | 6,1 | 13,8 | 75 s | 25 | 13 | 2 | 8 | 12 | 0 |
| orta | 0,20 s | 1412 | 6,3 | 13,4 | 71 s | 14 | 9 | 2 | 16 | 19 | 0 |
| uzman | 0,10 s | 1000 | 5,5 | 10,5 | 67 s | 22 | 6 | 1 | 10 | 21 | 0 |

Okuma: üç canla 65–75 s, iki–üç tam yük; can kayıpları köpekbalığı, mayın
ve oksijen arasında dağılıyor, tek bir tehdit baskın değil. "Uzman" pilotun
daha kötü olması pilotun sık hedef değiştirip titremesinden, oyundan değil;
insan için ilk hedef 1000 puan ve 5. dalga. Değişmez teste çevrilen: doğum
şeritlerde kalır (6 tohum × 60 s), boş yüzeye çıkış kuralı, pilot 20
dalışın en az 14'ünde teslim eder. Cihaz koşumu (A–C) aşağıda.

### Dalgıç · cihazda · 2026-09-10

Sürüm sayfasındaki v0.32.0 APK'sıyla (yerinde güncelleme). Günlük modun günde
üç denemesi olduğu için ölçümler Serbest'te.

**A — koşum.** Dalışlar oynandı (en iyisi 290 puan, 4. dalga); "Denizaltı
battı" kartı, can göstergesi ve rekor yazımı çalışıyor. Ana ekrana alıp
dönünce oyun kendiliğinden duraklıyor ("Devam et") ve skor/can korunuyor, geri
tuşu hub'a çıkıyor, `logcat` `AndroidRuntime:E` boş.

**B — kare hızı.** Dalış süren 9,3 s'lik pencerede 540 kare (**58,1 kare/s**),
kaçan vsync 14, jank %11,7, p50 24 ms, p90 34 ms. Raket ve Uçurtma'yla aynı
aile: kare hızı hedefte, kare gecikmesi ve kaçan vsync yeni oyunlarda eskilerin
üstünde.

**C — iki eksenli sürükleme kazancı.** Sarı denizaltının merkezi izlenerek her
eksende ayrı ölçüldü:

| parmak | 10 px | 20 px | 40 px | 160 px |
| --- | --- | --- | --- | --- |
| dikey | 12,9 px (1,29) | 23,9 px (1,20) | 51,1 px (1,28) | 198,4 px (1,24) |
| yatay | 13,1 px (1,31) | — | 49,1 px (1,23) | — |

Koddaki 1,3 iki eksende de çıkıyor ve **ölü bölge yok**: 10 px'lik parmak yolu
bile 13 px hareket veriyor (olaylar `awaitFirstDown`'dan itibaren okunuyor).

> Ölçüm tuzağı: denizaltı sürüklenen yöne dönüyor, dönünce sarı kütlenin
> merkezi ~4 px kayıyor. Yön değiştirerek ölçülen ilk seri bu yüzden sahte bir
> "4 px ölü bölge" gösterdi; aynı yönde iki kaydırmayla (ilki döndürür, ikincisi
> ölçülür) rakam 1,3'e oturdu.

**Filo'nun 1,35'iyle aynı his mi?** Sayılar bunu ayırmaya yetmiyor: Filo 1,35
ayarında 1,29–1,34, Dalgıç 1,3 ayarında 1,20–1,31 ölçüyor — iki aralık iç içe.
Farkı yapan katsayı değil, **yol bütçesi**: denizin yatayı 1016 px, dikeyi
1629 px; 1,3 kazançla uçtan uca parmak yolu yatayda 781 px = **47 mm**,
dikeyde 1253 px = **76 mm**. Yani yatay tek başparmak hamlesiyle geçiliyor
(sınır 55 mm), dikey geçilmiyor — 2B'de pahalı olan eksen dikey. Kazanç
kurcalanacaksa Filo'ya yaklaştırmak yerine dikey ekseni ayrı düşünmek gerekir.

**C — akıntı bantlarının okunurluğu.** Ölçülen (411 dp, su içi):

| ne | ölçüm |
| --- | --- |
| bant dolgusu ↔ çevresindeki su | **1,10:1** |
| kesik çizgi ↔ bant dolgusu | 1,70:1 |
| kesik çizgi ↔ bant dışı su | 1,87:1 |
| kesik çizgi boyu / kalınlığı | 20 dp / 1,9 dp, satır başına 8–9 parça |

Okuma: bandın kendisi (alfa 0x22) tek başına görünmüyor; akıntıyı okutan şey
kesik çizgiler ve onların akması. Duran bir ekran görüntüsünde bant sınırını
bulmak zor, harekette kolay. Bant sınırının kendisi işaret edilecekse dolgu
alfası değil, kenara ince bir çizgi daha etkili olur.

**C — mayın zincirlerinin görünürlüğü.** 4. dalgada mayınlı bir kare yakalandı:

| derinlik | zincir kalınlığı | zincir ↔ su kontrastı |
| --- | --- | --- |
| orta su (y≈1750) | 5 px = 1,9 dp | **1,84:1** |
| daha derin (y≈1950) | 4–5 px = 1,5–1,9 dp | 1,66:1 |
| dip (y≈2050) | 4–5 px = 1,5–1,9 dp | **1,54:1** |

Mayının gövdesi (dikenli, neredeyse siyah daire) her derinlikte rahat
seçiliyor; sorun zincir. Zincirin alfası sabit (0x88) ama su derinleştikçe
koyulaşıyor, dolayısıyla **zincir tam da en uzun olduğu yerde en zor görünür
hâle geliyor**. Derinlikle açılan bir zincir rengi (ya da alfayı derinlikle
artırmak) bunu ucuza çözer.

**Oksijen uyarısının duyulabilirliği.** Uyarı, 30 saniyelik oksijenin 8 saniyesi
kalınca **bir kez** çalıyor (`lowWarned`): `Sfx.HORN`, ses 0,6 · hız 1,3, yanında
titreşim ve denizaltının üstünde yazı. Çalınan sesin ölçümü:

| ses | süre | tepe | RMS | temel |
| --- | --- | --- | --- | --- |
| oksijen uyarısı (0,6 · 1,3) | 0,25 s | −16,7 dBFS | −23,6 dBFS | ~508 Hz |
| mayın patlaması | 0,13 s | −13,0 | −27,1 | — |
| dalgıç teslimi | 0,43 s | −14,0 | −22,7 | — |
| yüzeye çıkış | 0,12 s | −14,2 | −29,1 | — |

Yani uyarı, oyunun en gür seslerinden 3–4 dB aşağıda ama sürekli bir ton olduğu
için RMS'i onlardan yüksek; duyulmama riski seviyeden çok **tek seferlik
olmasından** geliyor: kaçıran oyuncu bir daha duymuyor. Cihazda ses yolunun
çalıştığı (HAL `fast_out`) ve titreşimin tetiklendiği doğrulandı, ama sürücü
denizaltıyı 22 saniye boyunca su altında tutamadığı için (düşmanlar can
götürüyor, denizaltı yüzeye dönüyor) **olayın kendisi yerinde yakalanamadı**.
Duyulabilirlik kararı için sesin çalındığı hâli WAV olarak üretildi ve
dinlenmek üzere gönderildi.

### Bostan · 2026-09-10

**D — uzman ölçümü** (`./gradlew :games:bostan:probe`, 30 tohum/zorluk,
v0.33.0). Uzman politika yarım saniyede bir karar verir: yaşı 0,8 s'yi
geçen damlayı toplar, sonra öncelik sırasıyla en fazla bir savunma koyar
(acil kesme, erken kuyular, her şeride bir fıskiye, tehdit altındaki şeride
korkuluk ya da vakti varsa tuzak, kovanlar, ek kuyular, ek fıskiyeler).
Üretici aynı politikayla doğrular: uzman kaybederse bütçe ×0,85 küçültülüp
yeniden üretilir (en çok altı ölçek, taban 0,44).

| zorluk | dalga | ölçek 1,0 kazanma | ort. can | ort. süre | ort. saldırgan | ort. yerleşim | üretici ölçek dağılımı |
| --- | --- | --- | --- | --- | --- | --- | --- |
| kolay | 6 | 30/30 | 2,80 | 123 s | 25,6 | 24,4 | 1,00 × 30 |
| orta | 8 | 20/30 | 1,77 | 166 s | 50,9 | 28,7 | 1,00 × 20 · 0,85 × 3 · 0,72 × 6 · 0,52 × 1 (ort. 0,91) |
| zor | 10 | 20/30 | 1,83 | 240 s | 92,4 | 36,6 | 1,00 × 20 · 0,85 × 4 · 0,72 × 6 (ort. 0,92) |

Bütçeler (ölçek 1,0; `*` büyük dalga): kolay 2,4 4,0 8,4* 7,2 8,8 15,6*;
orta 3,6 6,0 12,6* 10,8 13,2 23,4* 18,0 20,4; zor 4,8 8,0 16,8* 14,4 17,6
31,2* 24,0 27,2 45,6* 33,6. Zor/tohum 1 örneği: 10 dalga, 96 saldırgan,
ölçek 1,00, uzman 3 can ve 2855 puan.

**Bulgu 1 (tuzak).** Test yazarken çıktı: domuzun önüne konan tuzak
kurulamıyor — domuz saniyede 2 kemirir, tuzak 4 can, kurulma 3 s; tuzak 2
s'de biter. Kural doğru (tuzak pusudur, siper değil), uzman ona göre
yazıldı: tuzağı yalnızca saldırgan hücreye 3,5 s'den uzakken koyar.
Değişmez teste çevrildi: karga (1/s) kemirirken tuzak kurulup patlar, domuz
kurulmadan yer.

**Bulgu 2 (ölçek).** Ölçek 1,0'da uzman kolayda 30/30, orta ve zorda 20/30
kazanır; kaybedilenleri üretici 0,85–0,52'ye çeker, ortalama ölçek 0,91–0,92.
Yani seviyelerin üçte ikisi tam bütçeyle, üçte biri küçültülerek gelir;
kazanılamayan seviye üretilmez (6 tohum × 3 zorluk testi). Uzman insan
gibi yavaş tutuldu (damla gecikmesi, adım başına tek yerleşim) ki ölçek
insanın erişemeyeceği bir standarda göre kesilmesin.

Okuma: kolay 2, zor 4 dakika. Uzmanın kolayda ortalama 2,8, ortada 1,8 can
bırakması insan için hedef: kolayı üç yıldızla, ortayı bir–iki yıldızla
bitirmek. Cihaz koşumu (A–C) aşağıda.

### Bostan · cihazda · 2026-09-11

v0.33.0 APK'sıyla, Serbest · Kolay (Günlük'ün günde üç denemesi var).

**A — koşum.** Kart seçip hücreye yerleştirme çalışıyor (su 150 → 100), dalgalar
ilerliyor, bostan çiğnenince sonuç kartı geliyor ve uzmanın aynı bostandaki
sonucunu da yazıyor ("Uzman aynı bostanda: 2 can · 605 puan"). `logcat`
`AndroidRuntime:E` boş.

**B — kare hızı.** Oyun sürerken 12,5 s'de 748 kare (**59,8 kare/s**), kaçan
vsync 5, jank %47, p50 28 ms, p90 36 ms.

**C — kart ve hücre dokunma hedefi.** Izgara ve kart şeridi iki ekran
genişliğinde ölçüldü:

| | hücre | kart |
| --- | --- | --- |
| 411 × 891 dp (ölçüm cihazı) | **72 × 72 dp** | 59 × 79 dp (6 dp aralık) |
| 360 × 640 dp | **38 × 38 dp** | 51 dp genişlik |

360 dp'de tarla **yükseklikle sınırlanıyor**: 5×7 ızgara sığmak için küçülüyor
ve ortalanıyor (iki yanda geniş yeşil boşluk kalıyor), hücre 38 dp'ye iniyor —
Material'ın 48 dp tabanının ve kütükteki "~54 dp" tahmininin altında. Kartlar
iki ekranda da tabanın üstünde. Hücreler bitişik olduğu için ıskalanan dokunuş
boşa gitmiyor, **komşu hücreye ekiyor**; dar ekranda yanlış satıra ekme riski
gerçek. Yükseklik sıkışınca ızgarayı küçültmek yerine satır sayısını koruyup
hücreyi dikdörtgen yapmak (genişlik bol) 48 dp'yi kurtarır.

**C — damla.** Ömür 6 s (kod). Sprite 24 × 37 dp, toprağa karşı kontrast
**4,6:1** — duran ekranda bile göze çarpıyor. Toplama hedefi sprite değil
**hücrenin tamamı** (`collectDrop(lane, row)`); cihazda doğrulandı: sprite
merkezinden 30 dp uzağa dokunmak suyu +25 yaptı.

> Bulgu: **kart seçiliyken damlaya dokunmak onu toplamıyor, oraya ekiyor.**
> Ölçüm sırasında aynı dokunuşlar su +25 yerine −50/−25 verdi. Damlayı almak
> için önce kart seçimini bırakmak gerekiyor; altı saniyelik pencerede bu,
> oyuncunun kaçırmasının en olası yolu. Damla dokunuşuna ekimden öncelik
> vermek (damla varsa önce onu topla) tek satırlık bir kural değişikliği olur.

360 dp'de damla da küçülüyor (~13 dp) ama pencere aynı 6 s.

**C — "Bostan hazırlanıyor…" süresi.** 60 kare/s ekran kaydıyla üç kez ölçüldü:
örtü **70–150 ms** görünüyor, bir sonraki karede tarla hazır. Telefonda bekleme
hissi yok; JVM'deki ~20 ms'lik üretim cihazda da tek karelik bir parlamaya
dönüşüyor.

**C — dalga duyurusu ve kart bekleme örtüsü.**

- Duyuru tarlanın üst şeridinde beliriyor, harf yüksekliği ~21 dp, tam
  görünürlük ~0,8 s (kodda ömür 1,8 s, sonu solarak biter). Ölçülen kontrast:
  normal dalga (sarı 239,216,131) toprağa karşı **4,27:1**; büyük dalga
  (pembe 252,165,165) **3,18:1**. İkisi de büyük yazı için 3:1 eşiğinin
  üstünde, ama **en kritik duyuru en zayıf kontrasta sahip** — büyük dalga
  rengi normal dalganınkinden bir tık daha soluk kalıyor.
- Kart bekleme örtüsü: bekleyen kart hazır karta göre yalnızca **%15–25 daha
  sönük**; düzen, simge ve fiyat etiketi aynı yerde duruyor. Ayırt ediliyor ama
  zayıf; kalan süreyi gösteren bir halka ya da daha belirgin soluklaştırma
  "neden basamıyorum" sorusunu ortadan kaldırır.

**Doğrulama turu (v0.34.1, aynı cihaz).**

- **Hücre ölçüsü düzeldi.** 411 dp'de **77 × 77 dp** (önce 72 × 72), 360 × 640
  dp'de **67 × 47 dp** (önce 38 × 38). Genişlik artık tuvalin tamamını
  kullanıyor, yükseklik sığdığı kadar kısalıyor; hedef alanı dar ekranda
  3149 dp² (önce 1444 dp²). Yükseklik 48 dp tabanının 1 dp altında kalıyor ama
  parmak için belirleyici olan dar eksen artık 47 dp ve geniş eksen 67 dp.
- **Damla önceliği doğrulandı.** Kart seçiliyken damlaya dokunmak **damlayı
  topluyor** (su +25), üç denemenin üçünde; ekim yapmıyor. (Önceki turda
  görülen −50, damlanın ölçüm gecikmesi içinde sönmesiyle karışmıştı; ekran
  görüntüsüyle dokunuş arasını sıkıştırınca yeni davranış net çıkıyor.)
- **Yeni yerleştirme jesti** (basılı tut → kaydır → bırak) çalışıyor: sürükleyip
  bırakınca kart yerleşiyor (su 150 → 100).
- **B:** 8 s'de 490 kare (61 kare/s), kaçan vsync **1** (önce 5), p50 34 ms,
  jank %87. `logcat` temiz.
- Küçük not: 360 dp'de su sayacı ve "Sıradaki dalga" yazısı tarlanın üst orman
  şeridiyle üst üste biniyor.

**Düzeltme (v0.34.1).** Beş bulgunun beşi ele alındı:

- *Hücre boyu.* Hücreler dikdörtgen: genişlik hep tuvalin tamamı, yükseklik
  sığdığı kadar (en çok genişlik kadar); orman ve kulübe şeritleri inceltildi
  (0,8 + 0,7 → 0,5 + 0,55 hücre) ve durum çubuğu (su, dalga çubuğu, geri
  sayım) tuvalin orman şeridine taşınarak ~36 dp dikey yer kazanıldı.
  360 × 640 dp için tahmin: hücre **67 × 46 dp** (önce 38 × 38); 411 dp'de
  78 × 78. Cihazda ölçülecek.
- *Komşuya ekme.* Yerleştirme basılı tut – kaydırarak ayarla – bırakınca
  uygula: parmak altındaki hücre beyaz (uygun) ya da kırmızı (uygun değil)
  çerçeveyle görünür, bırakılan yer uygulanır. Iskalanan dokunuş görülüp
  düzeltilebilir.
- *Damla.* Dokunuşa 0,75 hücre yakın damla her şeyden önce toplanır — kart
  seçiliyken de, komşu hücreden de (`nearestDrop`, motor testi; ekran testi:
  kart seçiliyken komşu hücreden dokunuş +25 verir, ekim olmaz, seçim kalır).
- *Duyuru kontrastı.* Büyük uçan yazılar (dalga, büyük dalga, can kaybı,
  bitiş) koyu bir şerit üstünde (%55 siyah); büyük dalga rengi 252,165,165 →
  255,228,230, ömür 1,8 → 2,2 s.
- *Kart bekleme.* Örtü %45 → %62, alt kenarında beyaz çizgi; etiket beklerken
  fiyat yerine kalan saniyeyi yazar ("7 s"). Kartlar biraz kısaldı (en-boy
  0,82 → 0,9).

### Sincap · 2026-09-10

**D — pilot ölçümü** (`./gradlew :games:sincap:probe`, 20 tırmanış/pilot,
v0.34.0). Pilot konduktan tepki süresi kadar sonra karar verir: yılanlı dalı
seçmez, hedef basamaktan geçen karga varış anında dalın üstünde olacaksa
bekler, kuru dal kırılmak üzereyse ya da kedi yaklaştıysa beklemez; fındık ve
iki basamaklık sıçramayı tercih eder.

**Bulgu 1 (kedi).** İlk ölçümde üç pilot da 300 s boyunca sağ kaldı
(680–1046 basamak) ve hiçbir neden ölüm üretmedi: kedi en çok 1,3 basamak/s
idi, pilotların temposu 2,3–3,5. Kedi 2,4'e çıkarılınca yine sağ kaldılar:
pilot kedi yaklaşınca tepki süresini atlayıp anında zıplıyordu (4,5
basamak/s) — insanın yapamayacağı şey. Pilot düzeltildi (tepki süresi hep
uygulanır), kedi 250 basamakta 3,2'ye çıkıp sonra yavaşça artmayı
sürdürüyor. Sonuç: koşuyu bitiren kedi, belirleyici olan tempo.

| pilot | tepki | ort. yük. | en iyi | ort. skor | ort. fındık | ort. süre | basamak/s | nedenler |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| acemi | 0,35 s | 211 | 236 | 4012 | 54,8 | 103 s | 2,05 | kedi 20 |
| orta | 0,20 s | 294 | 308 | 5532 | 75,7 | 107 s | 2,74 | kedi 20 |
| uzman | 0,10 s | 500 | 559 | 9230 | 124,8 | 143 s | 3,50 | kedi 19 · karga 1 |

Okuma: tempo ile yükseklik doğrusal (2,05 → 211, 2,74 → 294, 3,5 → 500).
Kedi eğrisinden hesapla insan hedefleri: 1 basamak/s'lik acemi ~90 m, 1,5
ile ~145 m, 2 ile ~200 m; koşu 1,5–2,5 dakika. Kuru dal, yılan ve karga
pilotu neredeyse hiç öldürmüyor (60 koşuda 1 karga): bunlar okuma hatasının
cezası, insan için asıl ölüm nedenleri olacak. Basamak dağılımı (tohum 1,
ilk 200): tek dallı basamak %50, kuru dal %12, yılan %4, fındık %16, kargalı
basamak %16. Değişmez teste çevrilen: her basamakta dal ve kaçış (30 tohum ×
400), kargalı basamağın altında kuru dal yok, pilot boşluğa atlamaz ve 10
tohumda ortalama ≥ 20 basamak. Cihaz koşumu (A–C) aşağıda.

### Sincap · cihazda · 2026-09-11

v0.34.0 APK'sıyla, Serbest (Günlük'ün günde üç denemesi var). Zıplama ve karga
ölçümleri 60 kare/s ekran kaydından, dokunuşlar `uinput` ile bilinen aralıkla
(1,17 s) basılarak çıkarıldı.

**A — koşum.** Sola/sağa dokunuşla tırmanış çalışıyor (12–15 m, 180–190 puan),
fındık toplanıyor, koşu bitince sonuç kartı geliyor; `logcat`
`AndroidRuntime:E` boş.

**B — kare hızı.** Tırmanış sürerken 8 s'de 465 kare (**58,1 kare/s**), kaçan
vsync 11, jank %66, p50 31 ms, p90 34 ms. Fazlar: **toplam 39,6 ms** (90p 40,4),
GPU 24,9 ms, komut→swap 13,9 ms, çizim kaydı 1,5 ms. Bu, kütükteki **en yüksek
kare gecikmesi** (önceki tavan Viraj ve Uçurtma'da 34 ms) ve GPU tarafı yeni
oyunların en pahalısı.

**C — ilk temasta zıplama hissi.** Kayıttan 14 atlayış:

| ölçüt | değer |
| --- | --- |
| havada kalma | 133–168 ms (medyan **150 ms**) |
| yay tepesi | ~180 px = 69 dp |
| atlayışlar arası | 1,13–1,20 s (enjekte edilen dokunuş aralığı 1,17 s) |
| girdi→kare tamamlandı | 39,6 ms (medyan) |

Okuma: **her dokunuş bir atlayış üretti, hiçbiri düşmedi ya da kuyruğa
takılmadı** — aralıklar enjekte edilenle birebir. Zıplamanın kendisi kısa ve
kavisli; gecikmenin sistem payı 39,6 ms, yani yaklaşık 2,4 kare. His yargısı
ölçümle verilmez ama girdi tarafında kayıp yok; hissedilecek gecikme varsa
kaynağı kare gecikmesi (B'deki 39,6 ms), zıplama mantığı değil.

**C — erişim ipucunun okunurluğu (zayıf halka).** İpucu, sincabın iki yanındaki
"^" işaretleri: **28 × 16 dp**, rengi (238,249,255), gökyüzü (171,225,253) →
kontrast **1,32:1**. Yani hangi dala erişebileceğini söyleyen tek işaret,
gökyüzüyle neredeyse aynı parlaklıkta. Duran ekranda seçiliyor ama zayıf;
ince bir koyu kontur ya da gölge ucuz bir düzeltme olur (karşılaştırma: kedi
göstergesi 3,78:1, dalga duyurusu 4,27:1).

**C — kedi göstergesi.** Alt kenarda hap: **118 × 39 dp**, hap ↔ zemin
**3,78:1**, yazı ↔ hap **4,41:1**, kedi yüzü simgesi ve mesafe ("Kedi 8 m").
Okunurluk sorunu yok; göz hattının dışında durduğu için tırmanışın ortasında
fark edilmesi ayrı bir soru, ama işaret net.

**C — kuru dal ve 1,1 s.** Kuru dal **konmadan önce** ayırt ediliyor: yapraksız,
gri odun (164,162,158) ve üzerinde çatlak işareti; normal dal kahverengi ve
yapraklı. Gökyüzüne karşı kontrastı yalnızca 1,63:1, yani ayrımı renk değil
**biçim** yapıyor (çıplak dal ↔ yapraklı dal); kalınlık 7,6 dp, normal dal
~10 dp. Kondouktan sonraki uyarı: titreme `sin(kare·1,3)·u·0,05` → ölçülen
basamak aralığında (246 px) **±12 px = ±4,7 dp, ~12 Hz**, üstüne odun rengi
süre bitene kadar kırmızıya kayıyor.

Bütçe: atlayış 150–220 ms sürüyor, yani 1,1 s'lik tutuş **karar için ~0,9 s**
bırakıyor — insan tepkisi (250 ms) için rahat. Asıl güvenlik payı zaten
zıplamadan önce dalın okunabilmesinde; titreme ikinci bir şans.

**C — 7,5 basamaklık görüş alanında kargayı görme süresi.** Ölçülen basamak
aralığı **246 px = 94 dp**, tuval yüksekliği 1736 px → görüş alanı **~7
basamak** (belgede 7,5). Karga 5 basamak önden doğuyor, yani doğduğu anda
görüş alanının içinde. Kayıttan karga yatayda **~430 px/s ≈ 1,7 basamak/s**
gidiyor; ekranı bir uçtan bir uca **~2,1 s**'de tarıyor.

| tırmanış temposu | kargayı görme süresi (5 basamak) |
| --- | --- |
| ölçümdeki tempo, 1,15 s/basamak | **~5,8 s** |
| hızlı oyuncu, 0,4 s/basamak | ~2,0 s |

Okuma: normal temposunda uyarı cömert; hızlandıkça daralıyor ve asıl kısıt
kargayı görmek değil, **o basamağa vardığında karganın şeridin neresinde
olacağı** — ekranı 2,1 s'de geçtiği için zamanlama sorusu bu.

**Düzeltme (v0.35.1).** Erişim ipucu çift çizgi oldu: koyu mavi kontur
(20,61,107; genişlik 0,12 basamak) üstüne açık çizgi; işaret biraz da büyüdü
(yarım genişlik 0,12 → 0,14 basamak). Kontur gökyüzüne karşı ~7:1, açık çizgi
kontura karşı ~11:1; ölçüm cihazda yenilenecek. Kare gecikmesi için ucuz bir
adım: gökyüzü degradesi her karede yeniden kurulmak yerine yükseklik kovasına
göre (1/40) önbellekleniyor; GPU payı (24,9 ms) ayrıca ölçülmeli. Kuru dal,
kedi göstergesi ve karga görme süresi bulguları eylem gerektirmedi.

### Çekirge · 2026-09-11

**D — pilot ölçümü** (`./gradlew :games:cekirge:probe`, 20 koşu/pilot,
v0.35.0). Pilot tepki süresinde bir en yakın sütunun alt çekirgesini hedefler
(fıskırtmanın varış anındaki x'i tahmin eder), başparmak hızıyla oraya kayar,
hizaya gelince ve fıskırtma boşsa sıkar; 1 s içinde varacak tükürüğün
yolundaysa en yakın güvenli x'e kaçar, hedef tehlikedeyse yerinde bekler.

**Bulgu 1 (formasyon).** İlk ölçümde üç pilot da 14–20 s'de istilaya
uğradı (20/20): 8 sütunluk sürü tarlaya göre fazla genişti, yan yolculuk
0,11 birimdi ve her 1,6 s'de bir iniyordu. Sürü 7 sütuna daraltıldı (aralık
0,105 → 0,095), başlangıç 0,2 → 0,16, iniş 0,045 → 0,03, hız 0,07 → 0,06:
tam hızda istila 82 s'ye çıktı.

**Bulgu 2 (pilot).** Sonraki ölçümde istila sıfır, ama 20/20 koşu tükürükle
bitti (20–38 s): pilot tükürükten kaçıp hemen hedefe — tükürüğün altına —
dönüyordu. Kaçış "güvenli hedef" kuralına çevrildi (bulunduğu yer tehlikedeyse
en yakın güvenli x, hedef tehlikedeyse yerinde kal).

**Bulgu 3 (tempo).** Tek fıskırtma kuralıyla 1,25 birim/s'lik fıskırtma
0,6–0,9 s'de bir atış demekti; 35 çekirge en iyi hâlde 25 s sürerken sürü
tükürük menziline iniyordu. Fıskırtma 1,8 birim/s, tükürük aralığı 1,3 →
1,8 s ve ilk dalgada aynı anda 2 tükürük (sonra 3).

| pilot | tepki | başparmak | nişan | ort. skor | ort. dalga | ort. vuruş | ort. süre | istila | can |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| acemi | 0,40 s | 0,6 birim/s | 0,030 | 1799 | 2,55 | 81,9 | 73 s | 0 | 20 |
| orta | 0,25 s | 0,8 | 0,020 | 1997 | 2,95 | 89,9 | 64 s | 0 | 20 |
| uzman | 0,12 s | 1,0 | 0,015 | 1577 | 2,50 | 74,5 | 51 s | 0 | 20 |

Okuma: koşular 1–1,5 dakika, 2–3 dalga; bitiren hep tükürük, istila yok.
"Uzman" pilotun daha kötü olması sık hedef değiştirip sürünün altında daha
çok durmasından; insan için ilk hedef 1000 puan ve 2. dalga. Balyalar
oyuncunun kendi fıskırtmasını da yutar (klasik kural): sütunlar balyaların
arasından ya da açılan kanaldan vurulur — testler bunu 3. sütunla ve
kraliçeyi orta boşluktan vurarak kurar. Cihaz koşumu (A–C) aşağıda.

### Çekirge · cihazda · 2026-09-11

v0.35.0 APK'sıyla, Serbest. Dokunuşlar `uinput` ile bilinen sürelerde basıldı
(`tools/coklu_dokunus.py`), sonuç oyunun kendi durumundan okundu
("Tarla: skor N, M çekirge, K can").

**A — koşum.** Sürükleyerek yürüme, dokunarak fıskırtma ve öldürme çalışıyor
(8 dokunuşta 35 → 27 çekirge, skor 100); can bitince sonuç kartı geliyor, rekor
yazılıyor. Ana ekrana alıp dönünce durum korunuyor, geri tuşu hub'a çıkıyor,
`logcat` `AndroidRuntime:E` boş.

**B — kare hızı.** 9,5 s'lik oyun penceresinde 566 kare (**59,6 kare/s**), kaçan
vsync 7, jank %84, p50 29 ms, p90 32 ms. Fazlar: toplam 24,3 ms, GPU 19,8 ms,
komut→swap 10,5 ms, çizim kaydı 1,2 ms. 35 sprite kare bütçesini zorlamıyor;
maliyet yine dolguda.

**C — sürükleme ile dokunuş ayrımı (300 ms).** Parmak kıpırdamadan basılı tutulup
bırakıldı:

| basılı tutma | 150 ms | 250 ms | 290 ms | 310 ms | 350 ms | 500 ms |
| --- | --- | --- | --- | --- | --- | --- |
| fıskırtma | ✓ | ✓ | ✓ | — | — | — |

Eşik ölçümde **290 ms ile 310 ms arasında**; kodda `TAP_MS = 300`. Ayrım net:
uzun basış fıskırtmıyor, kısa dokunuş her seferinde fıskırtıyor.

Sürükleme tarafı (kazanç 1,2):

| parmak yolu | 20 px (7,6 dp) | 60 px | 150 px |
| --- | --- | --- | --- |
| çiftçi | **0 px** | 57 px (0,95) | 180 px (**1,20**) |

Yani kısa hareketler dokunma toleransına gidiyor (tolerans bilerek yutulmuyor,
sürükleme onu aşınca başlıyor); uzun sürüklemede kazanç tam 1,2. Dokunuşu
yürüyüşten ayıran şey bu tolerans, bedeli de ilk ~8 dp.

**C — iki başparmakla oynanabilirlik.** Bir parmak basılı tutup sürüklerken
(yürüme) ikinci parmak kısa dokunuşla fıskırttı: çiftçi 541 → 988 px yürüdü
**ve** aynı jestte fıskırtma çıktı (35 → 34 çekirge, skor +10). Ters yön de
tutuyor (988 → 628). Kod parmak kimliği başına karar verdiği için iki
başparmak birbirini kesmiyor.

**C — 35 çekirgeyle sprite okunurluğu.** Sprite **34 dp** geniş, sütun aralığı
37 dp (yani ~3 dp boşluk), 7 sütun × 5 satır. Gökyüzüne karşı kontrast:

| sıra | renk | kontrast |
| --- | --- | --- |
| kara | 0x1F2937 | **9,2:1** |
| kahverengi | 0xA16207 | 3,1:1 |
| yeşil | 0x65A30D | **1,9:1** |
| kraliçe | 0xF59E0B | 1,3:1 (palet) |

Okuma: boyut sorun değil (34 dp), ayrım renkte. Yeşil sıra gökyüzüne karşı
1,9:1'de kalıyor, kraliçe daha da zayıf — kalabalıkta ilk kaybolan bunlar.

**C — balya hücrelerinin görünürlüğü.** 411 dp'de balya **50 × 24 dp**, hücre
**8,4 dp** (6 × 3); 360 dp'de balya 44 × 20 dp, hücre **7,3 dp**. Balya ↔
gökyüzü **parlaklık** kontrastı yalnızca **1,04–1,10:1** — sarı ile açık mavi
neredeyse aynı parlaklıkta, ayrımı tamamen renk tonu yapıyor. Hücre içi çizgi
balyaya karşı 1,92:1. Sonuç: tek bir hücrenin aşınması küçük ekranda 7 dp'lik
bir boşluk bırakıyor ve bunu parlaklık farkı değil renk taşıyor; gri tonlamada
ya da düşük ışıkta balyanın yenmiş kısmı zor seçilir.

**C — tükürüğün kontrastı.** Cihazda ölçülen tükürük **7,6 × 7,6 dp**. Çekirdek
(0x4ADE80) buğday zeminine karşı **1,05:1**; çevresindeki koyu halka
(0x166534) palet olarak gökyüzünde 4,28:1, buğdayda 3,90:1 veriyor ama halka
~1 dp kalınlığında ve kenar yumuşatmayla soluyor: ölçülen en koyu piksel
zemine karşı **1,61:1**. Çit şeridinin üstünde ilişki tersine dönüyor (halka
1,01:1, çekirdek 4,06:1).

Okuma: tükürük her zeminde bir yarısıyla okunuyor, ama 7,6 dp'lik bir cisimde
1 dp'lik halkaya bel bağlanıyor. En ucuz iyileştirme halkayı kalınlaştırmak
(ya da çekirdeği koyulaştırmak); mermi zaten küçük ve hızlı.

**Düzeltme (v0.35.2).** Dört bulgu ele alındı, ikisi eylem gerektirmedi:

- *Sürükleme toleransı.* Eşik aşılınca ilk hareket başlangıç noktasından
  itibaren tümüyle uygulanıyor: 20 px'lik sürüklemede çiftçi artık 0 değil
  ~24 px yürür; dokunuş ayrımı (300 ms, tolerans) aynen duruyor.
- *Sprite kontrastı.* Yeşil sıra 0x65A30D → 0x3F6212 (gökyüzüne karşı ~1,9 →
  ~5:1), kraliçe 0xF59E0B → 0xB45309 (~1,3 → ~3,5:1); bütün çekirgelere ve
  kraliçeye koyu kontur. Kara ve kahverengi sıra olduğu gibi.
- *Balya.* Dolgu 0xFACC15 → 0xD97706, her hücreye 1,2 dp koyu kenar
  (0x7C2D12): aşınan hücre artık parlaklık farkıyla da okunur.
- *Tükürük.* Halka kalınlaştı (yarıçap ×1,1 → ×1,4, 0x052E16), çekirdek
  koyulaştı (0x4ADE80 → 0x15803D), üstüne parlak nokta; buğday ve gökyüzü
  zeminlerinde çekirdek ~2,7–3,6:1, halka 10:1'in üstünde.
- Sürükleme–dokunuş eşiği (290–310 ms) ve iki başparmak ölçümleri tasarımı
  doğruladı; kare hızı (59,6, 24,3 ms) eylem gerektirmedi. Kontrast
  değerleri palet hesabı; cihazda yenilenecek.

### Dokunmatik kontroller (Kuyu, Viraj, Filo) · 2026-09-11

**D — tasarım ve motor** (v0.36.0). Kuyu ile Viraj'daki tuş sırası kaldırıldı;
tuval artık üst çubuk ve kartlar dışındaki alanın tamamını kaplar (dar
telefonda tuş sırası kadar, ~96 dp, oyun alanı kazanılır). Filo'ya dikey
sürükleme eklendi. Üçü de ekranın `pointerInput` katmanında çözülür, motorlar
girdiyi eskisi gibi alır; determinizm testleri aynen geçer.

- *Kuyu.* İlk parmak yürütür: oyuncu her adımda parmağın sütununa doğru
  yürür, merkeze `STEER_DEAD` = 0,25 karo yaklaşınca durur; parmak kaydıkça
  hedef güncellenir. Sonraki her parmak "ateş" tuşunun yerini alır: yerdeyken
  zıplatır, havada basılı tutulunca botlar aşağı ateş eder. Tek parmağın
  kısa dokunuşu (`TAP_MS` = 220 ms, tolerans içinde, ateş parmağı yokken)
  `TAP_FRAMES` = 3 adımlık bir zıplama darbesi verir. Roller basışta verilir,
  parmak kalkana dek değişmez; yürüme parmağı kalkarsa sıradaki basış yürüme
  olur. Kontrol eli ayarı Kuyu'dan kalktı (Geçit ve Filo'da duruyor).
- *Viraj.* Tuvalin sol %40'ı sola, sağ %40'ı sağa kırar; orta şerit
  (`ZONE_LEFT`..`ZONE_RIGHT` = 0,4..0,6) düz gidip fren yapar. İlk basan
  parmak direksiyonu belirler, ikinci parmak (nerede olursa olsun) fren.
  Parmak bölgeler arasında kayınca direksiyon anında güncellenir. Koşunun
  ilk 8 s'inde (`ZONE_HINT_FRAMES` = 480) köşelerde oklar ve ortada FREN
  etiketi çizilir, son 1,5 s'de söner.
- *Filo.* Sürükleme iki eksende `DRAG_GAIN` = 1,35 ile geçer; gemi
  `PLAYER_MIN_Y` = 0,55 ile `PLAYER_MAX_Y` = 1,52 (yükseklik 1,6) arasında
  kırpılır, yani ekranın üst üçte birinden alt kenara dek bir bant. Düşman
  nişanı, mermi çıkışı, güç toplama ve çarpışma zaten canlı `playerY`
  okuduğundan yukarı çıkmak yaklaşmak demek: mermiler daha çabuk varır.

Testler: `KuyuScreenTest` (menüde tuş yok; basılı parmak yürütür, kayınca
yön değişir, kalkınca durur; ikinci parmak zıplatır; kısa dokunuş zıplatır),
`VirajScreenTest` (yarılar ve orta şerit, ikinci parmak, kalkış; direksiyonun
simülasyona geçmesi ve duraklatma), `FiloWorldTest` (+2: dikey bant kırpması,
yalnız x verilince y'nin korunması; yukarıdaki gemiye mermi çarpması).

**Cihazda ölçülecekler (A–C, bekliyor).** Kuyu'da 220 ms dokunuş eşiği:
yürümeye başlarken parmak 220 ms içinde kalkarsa istenmeyen zıplama olur mu;
bir başparmak yürütüp öbürü zıplatırken ergonomi. Viraj'da 1080 px genişlikte
bölge sınırları 432 / 648 px: başparmaklar orta şeride rahat ulaşıyor mu,
yoksa fren için ikinci parmak mı tercih ediliyor; ipuç oklarının kontrastı.
Filo'da dikey bandın üst ucu (0,55) baş parmakla gemi arasında görüş bırakıyor
mu, gemi parmağın altında kalıyor mu.

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

