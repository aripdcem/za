# ZA · Zero Ad Games

> **Sıfır reklam. Sıfır izleyici. Sıfır izin. Saf oyun.**

ZA, Android telefonlar için **"zero ad game play"** konseptiyle geliştirilen bir mobil oyun platformudur. Çatı altındaki her oyun tamamen reklamsızdır; uygulama hiçbir izin istemez (İNTERNET izni dahil), hiçbir veri toplamaz ve hiçbir şey satmaz. Uygulama tam ekran açılır; sistem çubukları kenardan kaydırınca geçici görünür. Oyunlar: **Blok**, **2048**, **Yılan**, **Sudoku**, **Mayın Tarlası**, **Beş Harf**, **Kıskaç**, **Türetme**, **Dizgi**, **Kuyu**, **Geçit**, **Tavla**, **Balkon**, **Kakuro**, **Vergici**, **Toplam Kapma**, **Viraj**, **Filo**, **Reyon**, **Raket**, **Tuşe**, **Uçurtma**, **Dalgıç**, **Bostan**, **Sincap**, **Çekirge**, **Cici**.

Ana menüde oyunlar gruplara ayrılır (Kelime, Bulmaca, Arcade, Masa; süzgeç çipleri, seçim kalıcı) ve en üstte son oynanan dört oyun için hızlı erişim şeridi bulunur.

## Manifesto

| Söz | Uygulamadaki karşılığı |
| --- | --- |
| 0 reklam | Hiçbir reklam SDK'sı yok |
| 0 izleyici | Analitik/izleme kütüphanesi yok |
| 0 izin | `AndroidManifest.xml` tek bir `uses-permission` içermez |
| 0 satın alma | Ödeme/abonelik kodu yok |
| Saf oyun | Skorlar yalnızca cihazda saklanır |
| Gizlilik | Politika: [za.aripd.com/gizlilik.html](https://za.aripd.com/gizlilik.html); uygulama içi **Hakkında** ekranı sürümü, bağlantıları (site, kaynak, sorun bildirme) ve açık kaynak lisanslarını gösterir |

## Mimari

```
za/
├── app/                          # Android uygulaması (Kotlin + Jetpack Compose)
│   └── com.za.games
│       ├── platform/             # Çekirdek: GameRegistry, ScoreStore, SettingsStore, SoundPlayer, ShareCard
│       ├── ui/common/            # Oyunların paylaştığı bileşenler (tuşlar, katmanlar, kartlar)
│       ├── ui/hub/               # Ana menü (oyun listesi + manifesto + ses düğmesi)
│       ├── ui/about/             # Hakkında: sürüm, bağlantılar, sürüm notları, açık kaynak lisansları
│   └── src/test/                 # Arayüz testleri: Robolectric + Compose (emülatör gerekmez)
│       ├── ui/&lt;oyun&gt;/            # Oyunların Compose arayüzleri
│       └── ui/theme/             # ZA teması
├── games/
│   ├── tetris/  g2048/  snake/   # Oyun motorları: saf Kotlin/JVM, Android'e
│   └── sudoku/ mines/ besharf/ kiskac/ turetme/ dizgi/ kuyu/ gecit/ tavla/ balkon/ kakuro/ sayi/ viraj/ filo/ reyon/ # bağımsız, her biri kendi birim testleriyle
├── tools/                        # gen_sfx.py (sesler), gen_words.py + gen_turetme.py + gen_dizgi.py (kelime listeleri)
│                                 # cihaz_testi.py (cihaz üstü kare hızı / giriş ölçümü)
└── docs/oyun-testi.md            # her oyunun geçmesi gereken test protokolü ve sonuç kütüğü
```

Temel ilke: **oyun kuralları saf Kotlin modüllerinde, arayüz `app` içinde** yaşar. Motorlar Android'e bağımlı olmadığı için cihazsız test edilir ve ileride başka platformlara taşınabilir.

### Yeni oyun eklemek

1. `games/<oyun>/` altında saf Kotlin motor modülü oluşturun (testleriyle birlikte) ve `settings.gradle.kts`'e ekleyin.
2. `app/src/main/kotlin/com/za/games/ui/<oyun>/` altında Compose ekranını yazın.
3. `GameRegistry.games` listesine bir `GameEntry` ekleyin — ana menü kartı ve rekor takibi kendiliğinden çalışır.
4. **Oyunu cihazda test protokolünden geçirin** ([`docs/oyun-testi.md`](docs/oyun-testi.md)): cihaz koşumu, kare hızı, giriş kalibrasyonu ve denge ölçümü. Birim testleri kuralları doğrular, oynanabilirliği değil.

## Oyunlar

Tüm motorlar deterministiktir: aynı tohumla (seed) başlayan iki oyun, aynı hamlelerle birebir aynı sonucu üretir. Rekorlar cihazda saklanır; oyunlar arka plana geçince kendiliğinden duraklar. Ses efektleri prosedürel üretilmiş küçük WAV'lardır ve ana menüden tamamen kapatılabilir.

**Yenilikler:** güncellemeden sonraki ilk açılışta ana menüde sürüm notu kartı çıkar, o sürümden sonra eklenen oyunlar "Yeni" rozeti alır; tüm notlar Hakkında ekranında (`platform/Changelog.kt`, depo kopyası `CHANGELOG.md`).

**Sonuç paylaşımı:** her oyunun bitiş kartındaki **Paylaş** düğmesi 1080 px genişliğinde bir sonuç kartı (PNG) ve kısa bir metin üretip Android'in paylaşım sayfasına verir (`platform/Share.kt`). Kakuro, Sudoku, Mayın Tarlası, Beş Harf, 2048 ve Dizgi'de karta bitmiş tahta da çizilir; Beş Harf metne 🟩🟨⬛ ızgarasını ekler ve günlük kelimeyi ele vermez. Kart uygulamanın önbelleğine yazılır, yalnızca seçilen uygulamaya ve yalnızca okuma için açılır (`FileProvider`): depolama izni gerekmez, "0 izin" sözü bozulmaz.

### Blok
- Düşen bloklar; eski adı Tetris tescilli marka olduğundan yeniden adlandırıldı (modül adı `games/tetris` teknik olarak kaldı)
- 10×20 tahta, **7'li torba** rastgeleliği, **SRS rotasyon** + tam duvar tekmesi tabloları
- **Hold**, 3 taşlık sıradaki kuyruğu, **hayalet taş**, satır temizlemede parlamalı animasyon + ses
- Guideline skorlaması (100/300/500/800 × seviye; yumuşak +1, sert +2/hücre) ve yerçekimi eğrisi
- Ekran tuşları (basılı tutunca tekrar eder) + tahtada dokunma/sürükleme jestleri

### 2048
- Klasik kurallar: taşlar hamle başına bir kez birleşir, %90/%10 oranında 2/4 doğar
- Kaydırma jestleri, doğan ve birleşen taşlarda yaylı "pop" animasyonu
- 2048'e ulaşınca kutlama; oyun devam eder

### Yılan
- 15×20 tahta; duvar ve gövde çarpışmaları (kuyruğun boşalttığı hücre serbesttir)
- Her yem +10 puan, +1 uzunluk ve kademeli hızlanma
- **Dokunarak yönlendirme:** gitmek istediğin tarafa dokun (yatay giderken üst/alt, dikey giderken sol/sağ) — kaydırma jestleri de çalışır; tahta tam ekran
- İki dönüşlük giriş tamponu; ters yöne dönüş engellenir

### Sudoku
- Geri izlemeli üreteç: her bulmacada **tek çözüm garantisi** (MRV'li çözüm sayacıyla doğrulanır)
- Üç zorluk (40/32/26 ipucu), kalem notları, çakışma vurgusu, rakam başına kalan sayacı
- Değer girilince komşu hücrelerdeki aynı rakam notları otomatik silinir
- Rekor = toplam çözülen bulmaca sayısı

### Mayın Tarlası
- Üç zorluk: 9×12/14, 10×14/25, 12×17/40 — **ilk dokunuş her zaman güvenli** (mayınlar sonra yerleşir)
- Sıfır hücrelerde akan açılım, uzun basış veya bayrak moduyla işaretleme, sayıya dokununca chord
- Rekor = toplam kazanılan oyun sayısı

### Beş Harf
- Türkçe kelime tahmini: 5 harf, 6 deneme; tekrarlı harflerde klasik iki geçişli işaretleme
- **Günlük mod** epoch gününden deterministik kelime seçer — sunucu yok, herkes çevrimdışı aynı kelimeyi görür; gün içinde tahta kaldığı yerden açılır. Ayrıca serbest mod.
- 29 tuşlu Türkçe klavye (İ/ı yerel ayar kurallarıyla), harf durumlarına göre tuş boyama
- Sözlük gömülü ve ~60 KB: 1.684 cevap + 7.797 geçerli tahmin (şapkalı yazımlar
  düzleştirilir: kâğıt → kağıt; cevap havuzu günlük diziyi korumak için sabittir); `tools/gen_words.py`
  Zemberek NLP kök sözlüğünden (Apache-2.0) ve FrequencyWords tr_50k listesinden
  (CC-BY-SA-4.0, OpenSubtitles türevi) türetir
- Rekor = en uzun kazanma serisi

### Kıskaç
- Gizli 5 harfli kelimeyi **alfabetik aralık daraltarak** bul: her tahmin, gizli kelimenin
  ondan önce mi sonra mı geldiğini söyler; kelimeyi iki sınır arasında sıkıştırırsın
- Karşılaştırma **Türk alfabesi sıralamasıyla** yapılır (c<ç, g<ğ, ı<i, o<ö, s<ş, u<ü) — Unicode değil
- Sınırlara göre imkânsızlaşan baş harfler klavyede soluklaşır; 12 tahmin hakkı
- Günlük + serbest mod, seri takibi; kelime listelerini Beş Harf ile paylaşır
- **Kolay mod** (anahtar, varsayılan kapalı): her sınır kartında gizli kelimenin o sınıra uzaklığı, tüm sözlük
  ölçeğinde yüzde olarak (A = %0, Z = %100; "%18 uzakta" / "%15 uzakta"). Ölçek sabit olduğundan yeni bir tahmin
  yalnız taşınan sınırın sayısını değiştirir; kapalıyken oyun yalnız önce/sonra bilgisiyle oynanır

### Türetme
- Verilen 6-7 harften türetilebilen **tüm alt kelimeleri** bul (en az 3 harf); harflere
  dokunarak kelime kur, 🔀 ile karıştır
- Puan: kelime uzunluğu × 10; taban kelimeyi bulana +50, listeyi bitirene +100 bonus
- Günlük mod herkese aynı harf setini verir, bulunanlar gün içinde saklanır; serbest modda sınırsız yeni set
- Tıkandıysan **Pes** de: tur biter, bulamadığın kelimeler çerçeveli çiplerle açıklanır (günlükte kalıcıdır)
- Sözlük gömülü ve ~120 KB: 15.829 geçerli kelime (şapkalı yazımlar düzleştirilir) + 1.200 taban;
  `tools/gen_turetme.py` tabanları 15-60 alt kelimeyle seçer (taban listesi günlük diziyi
  korumak için sabittir; kaynaklar Beş Harf ile aynı)
- Rekor = tek tahtada toplanan en yüksek skor

### Dizgi
- **Elden ele kelime tahtası** (Scrabble türü, kendi tasarımımız): aynı telefonda 2-4 oyuncu,
  15×15 tahta, 100 taş (98 harf + 2 joker), elde 7 taş
- Harf dağılımı ve puanları `tools/gen_dizgi.py` ile **kendi derlemimizden** türetilir;
  premium kare dizilişi de Dizgi'ye özgüdür (köşeler ve köşegenler ÇK, kenar ortaları ÜK);
  kareler 2H/3H/2K/3K etiketli, tahtanın üstünde lejant var
- Kurallar: ilk kelime ortadaki yıldızdan geçer, her hamle mevcut taşlara değer, ana kelime +
  tüm çapraz kelimeler sözlükte olmalı; katlar yalnızca yeni konan taşlara işler; 7 taş birden = +50
- Pas ve taş değişimi (torbada ≥7 taşla); herkes üst üste iki kez puansız geçerse ya da torba
  boşken bir oyuncu elini bitirirse oyun biter (kalan taşlar düşülür/aktarılır)
- Sıra değişiminde perde ekranı: taşları yalnızca sıradaki oyuncu görür
- Sözlük ~180 KB gömülü: 22.569 kök (2-15 harf; şapkalı yazımlar düzleştirilir: belâ → bela); rekor = kazananın skoru

### Kuyu
- **Kuyuya düşüş** (Downwell türü, kendi tasarımımız): tuş yok, tuvale dokunulur — parmağını tut,
  oyuncu parmağının sütununa yürür; ikinci parmak yerdeyken zıplatır, havada basılı tutulunca **botlar
  aşağı ateş eder** ve düşüşü yavaşlatır; tek parmağın kısa dokunuşu (≤ 130 ms) ya da yukarı kaydırması da zıplatır.
  Şarjör 8, yere inince
  ya da düşmana basınca dolar
- Düşmanlar: topak ve yarasa (üstüne basılır), dikenli (yalnız mermiyle), duvar tırmanıcısı; havada art
  arda öldürdükçe **kombo**, inişte kombo × 2 taş bonusu; kırılabilir bloklar, taş bırakan bloklar
- Kuyu 12 sütun genişliğinde, 16 satırlık parçalarla tohumdan üretilir: her satırda en az 3 boşluk,
  dibe kadar geçilebilirlik testle garanti (`games/kuyu`); 3 bölge, derinlikle daha çok ve daha hızlı düşman
- **Günlük kuyu**: gün numarasından türeyen tohum, herkes aynı kuyuyu oynar, tek deneme; ayrıca serbest mod
- Kontrol tuşu yok, tuval ekranı kaplar; 60 Hz sabit adımlı simülasyon, aynı tohum + aynı girdi = aynı oyun
- **Bölge sonu bekçisi**: her bölgenin son parçası arenadır; bekçi kapının üstünde salınır, yarasa çağırır,
  ölünce kapı açılır. Yeni bölgede simülasyon durur: **3 yükseltmeden biri** (şarjör, can, yayılan atış, hızlı
  botlar, menzil, mıknatıs, kombo, açgözlülük, kalkan, yaylı bacak) ve **taş karşılığı dükkân** (iyileş, şarjör +1,
  can +1); harcamak skoru düşürmez. Teklifler tohumdan türer, günlük modda herkese aynı
- **Hazine oyukları**: kalın duvarların içinde 2×2 oyuk ve altında sandık; üstünden aşağı ateş edince 15 taş
- Skor = toplanan taş + derinlik (m); rekor = en yüksek skor

### Geçit
- **Karşıya geçiş** (Frogger/Crossy Road türü, kendi tasarımımız): 9 sütunluk ızgarada zıpla. Altta iki
  başparmak için büyük tuşlar: ◀ ▶ bir yanda, ▼ ve büyük ▲ öbür yanda; ileri tuşu seçilen başparmağın tarafında
  (sağ/sol el ayarı Kuyu ile ortak), basılı tutunca art arda zıplar. Tahtada dokun = ileri, kaydır = yön
- Şeritler tohumdan sonsuz üretilir: çim (ağaçlı), yol (araba/kamyon), demiryolu (uyarı ışığı, sonra tren),
  nehir (kütüğe bin, kütükle sürüklen; kenardan taşınırsan ölürsün); ilerledikçe daha hızlı ve daha sık tehlike
- Çimdeki ağaçlar geçişi asla kapatmaz (üreteç her satırın bir önceki satırdan ulaşılabilir kalmasını garanti eder,
  testle doğrulanır); ilk hamleden sonra kamera yavaşça ilerler, 3,5 s ileri gitmeyeni kartal kapar
- Aynı yönde ardışık nehirler de kapanmaz: ya aynı hızda akar ve her kütüğün üstünde en az bir hücre örtüşen bir
  "köprü" kütüğü vardır, ya da hızları en az 1,2 hücre/s farklıdır (kütükler düzenli hizalanır). Şerit evreleri oyun
  saatine bağlıdır; şeritler ne zaman üretilirse üretilsin bu göreli konum korunur (testle doğrulanır)
- Çim ve yollarda **taşlar** (◆): üstüne zıplayınca alınır, her taş 1 puan; ilk 12 şerit yumuşak (yavaş, seyrek
  araba); yana/geri zıplamak kartal sayacını yarıya indirir ama sıfırlamaz, kartal gelmeden çığlık uyarır
- Skor = geçilen şerit + taş; **günlük mod**: herkes aynı yolu geçer, günde 3 deneme, en iyisi kaydedilir; serbest mod
- Görsel derinlik: araç, kütük, ağaç ve kurbağa gölgeleri, çim süsleri, kamyon kasası, tren çatısı, yumuşak kamera ve
  zıplama eğrisi, kartalın kurbağayı kaçırma animasyonu
- 60 Hz sabit adım, aynı tohum + aynı hamleler = aynı oyun (`games/gecit`)

### Tavla
- **Üç kural seti** tek oyunda: Klasik (açık pul kırılır, bar'dan girer), Tapa (pullar hapsedilir, 15 pul 24.
  haneden başlar), Hapis (hapis kurallı, klasik diziliş). Hapsedilen pul, üstündeki pullar kalkana dek oynayamaz;
  hapsedeni hapsetmek yok. Rakibin evinde hapsetme yasağı ve katlama küpü kurulumda ayarlanabilir
- Kurallar tam: iki zar da mümkünse oynanır, tek zar oynanabiliyorsa büyük olan; çift zar dört hamle; bar'daki
  pul önce girer; toplama için tüm pullar evde, fazla zarla en uzaktaki pul toplanır; mars iki puan; maç 1/3/5 puan
- **Bilgisayar rakip**: tüm yasal turları değerlendirir (pip yarışı, kapılar ve zincirler, açık pullar, bar/hapis,
  toplama); katlama teklif ve kabul kararı kazanma olasılığı kestirimiyle. İki oyuncu modu aynı telefonda
- Karşılıklı kilitlenme (yalnız hapis kurallarında olabilir): pip sayısı az olan kazanır, eşitse berabere
- Dikey ekrana sığan klasik tahta: üstte ve altta 12'şer hane, ortada bar, sağda toplama tepsisi; pula dokun,
  hedefe dokun (yasal hedefler işaretlenir), geri al. Deterministik zar (`games/tavla`, 18 test); rekor =
  bilgisayara karşı kazanılan maç sayısı

### Balkon
- Yukarıdan bakış: apartman balkonundan aşağıdaki sokağa nişan al. Sokağa dokun, oraya atar; atış 0,5–1 s
  havada kalır, bu sürede **rüzgâr** iniş noktasını kaydırır (korkuluktaki bayrak ve HUD oku gösterir) ve
  hedefler yürümeye devam eder: önden nişan almak işin özü
- **Üç tema**, başta seçilir, mekanik ortak: kabak çekirdeği + avuç dolusu, su balonu + kova, tükürük + balgam.
  Yalnızca mermi, iz ve sesler değişir
- Hedefler: güvercin, kedi, top, bisikletli, kurye motoru, araba, simitçi (süre bonusu). **Yasak** hedefler
  kapıcı ve komşu teyze: ceza, kombo sıfırlanır, kısa donma
- **Mega**: üst üste 5 isabet bir şarj verir (en çok 3); düğme ile kurulur ya da uzun basışla atılır. Üç kat
  isabet alanı, iki kat puan, ekran sarsıntısı
- Süreli seviyeler: 45 saniyede gereken isabete ulaş (8, 10, 12…); kalan süre ×10 bonus; hız, hedef sayısı ve
  rüzgâr seviyeyle artar. 60 Hz sabit adım, deterministik hedef akışı (`games/balkon`, 11 test)

### Viraj
- **Sözde-3D yarış** (OutRun türü yol izdüşümü, kendi tasarımımız): dikey ekranda arkadan görünüm, parça parça
  üretilen yol, yumuşatılmış virajlar ve tepeler, yol kenarı ağaç/çalı/kaya/tabela, uzak tepelerde paralaks
- Gaz otomatik; tuş yok: tuvalin sol yarısına dokunmak sola, sağ yarısına dokunmak sağa kırar, orta şerit ya da
  ikinci parmak fren (ilk saniyelerde bölge ipuçları çizilir). Yüksek hızda merkezkaç aracı virajın dışına iter; yol dışı yavaşlatır, kenar nesnelerine çarpmak hızı keser
- Rakip kartlar sollamadan kaçınır ve kendi aralarında yol verir; daha yavaş bir rakibe çarpmak hızı düşürüp geriye
  iter, rakibi geçmek +50. Her 600 parçada (3 km) kontrol noktası süre ekler (zorlukla azalır); süre bitince yarış biter
- Eşyalar: turbo şeridi (2,5 s tavan hız), yağ (direksiyon ters, kayma), koni (yavaşlama), sarı kutu (turbo, bir
  çarpışmayı emen kalkan ya da +5 sn)
- Skor = geçilen parça + sollama + kontrol noktası + eşya; **günlük mod** herkese aynı pisti verir, günde 3 deneme
- Motor `games/viraj`: `VirajTrack` (kesit üreteci, `easeIn`/`easeInOut`), `VirajWorld` (sabit adım, rakip yapay
  zekâsı, çarpışma, eşyalar, sayaçlar); 13 test: determinizm, hızlanma/fren, yol dışı, kontrol noktası, süre, sollama,
  çarpışma ve kalkan, eşyalar, pist sınırları, rakiplerin yeniden doğması

### Filo
- **Dikey kaydırmalı uzay savaşı**: gemi parmakla her yöne sürüklenir (dikeyde ekranın üst üçte birinden alt kenara
  dek bir bant; yukarı çıkmak yaklaştırır), ateş otomatik; BOMBA tuşu sağ/sol el ayarına göre başparmak tarafında
  (ayar Geçit ile ortak)
- Dalgalar tohumdan üretilir: dalış, sinüs, süpürme, halka ve asteroit sürüklenmesi desenlerinde drone, eşek arısı,
  tank ve asteroit grupları; her 5. dalga patron (salınan gövde, yelpaze + nişanlı ateş, canı yarılınca sertleşir).
  Zorluk 26. dalgaya kadar artar (grup sayısı, hız, ateş sıklığı, ağır düşman oranı)
- Puan: düşman türüne göre 50–2500; 2 sn içinde art arda vuruşlar zinciri büyütür, çarpan 1–4. Dalga temizleme bonusu
  250 × dalga. Vurulunca can gider (3 can), 2 sn dokunulmazlık, silah bir seviye düşer, mermiler silinir; kalkan bir
  vuruşu emer. Bomba (2 ile başlar, en çok 4) düşman mermilerini siler, ekrandakilere 3 (patrona 6) hasar verir
- Güç artırımları (%14 düşme şansı; patron her zaman bomba bırakır): W silah (3 seviye: tek, çift, üçlü yelpaze),
  S kalkan, B bomba, + puan (500 × çarpan)
- **Günlük mod** herkese aynı dalgaları verir, günde 3 deneme; serbest mod rastgele tohum
- Motor `games/filo`: `FiloWorld` (sabit adım, dalga üreteci, hareket desenleri, çarpışma, zincir, güç artırımları,
  bomba, patron); 21 test: determinizm, otomatik ateş, vuruş/puan, zincir çarpanı, can/silah kaybı ve dokunulmazlık,
  kalkan, çarpma, bomba, güç artırımları, dalga akışı, patron dalgası, kaçan düşmanlar, oyun sonu, sınırlar

### Kakuro
- Toplam bulmacası: kara hücrelerdeki ipucu, sağındaki yatay ve altındaki dikey koşunun toplamı; koşudaki rakamlar
  farklı. Üç boy: 7×7, 9×9, 11×11 (ipucu kenarı hariç). Notlar, geri alma, çakışma ve tamamlanan koşu vurgusu,
  seçili hücrenin koşularında kalan toplam; yarım kalan bulmaca cihazda saklanır
- **Tek çözüm garantisi**: 180° simetrik rastgele düzen (koşular 2–9), koşu içinde yinelenmeyen rastgele doldurma,
  ardından yayılımlı çözücüyle (toplam–uzunluk kombinasyonları, aday daraltma, geri izleme) iki çözüm arandığında
  farklı hücreler yeniden dağıtılır; birkaç aday arasından koşuları en sıkı kılan seçilir. Tohumdan deterministik
  (`games/kakuro`, 8 test)

### Reyon
- **Planogram mantık bulmacası** (kendi tasarımımız): bir raf ünitesi (3×4, 4×5 ya da 4×6 göz), rafı tam dolduran
  ürünler (1–3 yüz, boy, marka, kategori, ★ yüksek marj, ▼ ağır) ve bir planogram brifi. Brifteki kurallar gerçek
  yerleşim ilkeleridir: göz hizası (yüksek marjlılar 2. rafta), ağırlar en alta, kategori bloğu, kategoriler ayrı,
  marka dikey bloğu, boy akışı (soldan sağa büyür); ayrıca raf/göz/kenar, yan yana, solunda, aynı/farklı raf, üstünde
- Ürüne dokun, göze dokun: blok oturur; uzun basınca tepsiye döner. Blok adı göze sığdırılır (tek satır, gerekirse
  boşluktan iki satır ya da hafif daraltma; kırpma en son çare). Brif satırları tutunca ✓, çelişince ✗ olur ve
  ilgili ürünler kırmızı çerçevelenir; satıra dokununca ilgili ürünler vurgulanır. Geri alma; ipucu önce yanlış
  duranı gösterir, sonra mevcut yerleşimlerden mantıkla çıkan sıradaki adımı yerleştirir (ipucusuz çözümler rekor)
- **Tek çözüm ve tahminsizlik garantisi**: üretici planogram yapısında bir düzen örnekler (kategori bantları, marka
  blokları), doğru olan tüm ipucu adaylarını türetir, ağırlıklı sırayla tek çözüm sağlanana dek ekler, sonra
  oyuncunun gördüğü bilgiyle çalışan çıkarım çözücüsü (tekil, ikili, örtü, kapasite, çoklu teknikleri) sonuna kadar
  gidebildiği sürece ipuçlarını atar: en küçük, çıkarılabilir brif. Günlük mod herkese aynı rafı verir; yarım kalan
  bulmaca cihazda saklanır
- **Denetim modu** (planogram uyum kontrolü): üstte referans plan, altta gerçek raf. Raf plandan K yerde sapar
  (Kolay 2, Orta 3, Zor 5): yer değişimi, boş göz (bir yüz ya da ürün eksik), yabancı ürün, yanlış marka, yanlış boy,
  komşu göze taşma; ince sapmalar (marka/boy) yalnızca üst zorluklarda. Sapmalara dokunulur, yanlış dokunuş hata
  sayılır; bulunanlar açıklamasıyla (en yeni üstte) listelenir. Plan ve raf her ekranda aynı genişlikte kalır
  (yükseklik bütçesine göre boyutlanır), plana dokununca büyütülmüş plan açılır. Üretici sapmaları ayrık tutar ve plan ile rafın **yalnızca** sapma
  gözlerinde ayrıştığını doğrular (gizli fark yok, sahte fark yok). Süre, hata ve ipucu; günlük raf, en iyi süre
- **Satış modu** (açık uçlu diziliş): aynı ürün seti, brif yok; puan beş satış kuralından gelir ve oyuncuya aynen
  anlatılır: konum (talep × yüz × raf çarpanı; göz hizası ×3, alt ×1, ▼ ağır yalnız altta ×3, ★ yalnız göz hizasında
  ×4), tamamlayıcı komşuluk (cips–sos gibi 20 çift; yan yana +6, üst üste +3), çakışma (temizlik gıdanın yanında
  −8), kategori bloğu (+4), marka bloğu (yan yana +3, üst üste +3). Hedef, tavlamalı yerel aramanın (eşit genişlik
  takası, raf içi komşu takası, raf takası, ürün↔eşit genişlikli koşu takası; 4 yeniden başlatma) bulduğu en iyi
  puandır; ≥ hedef 3 yıldız, ≥ %90 2, ≥ %75 1. Canlı puan ve kural dökümü, seçili ürünün katkısı, bloklarda puan
  rozeti; tamamlayınca hedef dizilişi görme ve düzenlemeye dönme; günlük ürün seti (günün en iyi puanı), serbest modda
  hedef yüzdesi rekoru
- **Sipariş modu** (stok devri): raf planı sabit, iş stok. Beş–yedi gün boyunca her gün ürün başına kaç koli
  sipariş edileceğine karar verilir: talep aralık olarak görünür (gerçekleşen talep tohumdan gelir), siparişler ertesi
  sabah gelir (Zor'da ağırlar iki gün sonra), rafa sığmayan iade olur (−1/birim); satış marj kazandırır (+4, ★ +6),
  akşam rafta kalan her birim bekleme öder (−1), raf ömrü dolan fire olur (−2, ★ −4; süt/ayran 2, yumurta/peynir 3,
  tereyağı 4 gün); hafta sonu içecek ve atıştırmalık ×1,4, promosyon günü ×2,5 (menüde duyurulur). Hedef, aynı
  tahminleri gören uzman politikanın (teslim günü talebini emniyet payıyla karşılayan sipariş-üstü düzeyi) aynı
  haftadaki kârı; ≥ hedef 3 yıldız, ≥ %90 2, ≥ %75 1. Raf tuvali stok doluluğunu gösterir; ürün satırlarında stok,
  bugünün ve teslim gününün tahmini, gelen teslimat, bozulacak birimler ve koli adımlayıcısı; ipucu uzmanın önerisini
  yazar; gün kapanış dökümü (satış, kayıp, bekleme, fire, iade, sabah teslimatı); hafta sonunda stok devri ve hizmet
  düzeyi uzmanla karşılaştırılır; günlük hafta (günün en iyi kârı), serbest modda hedef yüzdesi rekoru
- Motor `games/reyon`: `OrderRules`/`OrderExpert`/`ReyonOrderGenerator`/`ReyonOrderState` (sipariş kuralları, uzman
  politika, hafta üretimi, gün kapanışı, kayıt); `SalesRules`/`SalesScorer`/`SalesOptimizer`/`ReyonSalesState` (puan tabloları, kısmi
  puanlama, iyileştirici, satış durumu); `ReyonAuditGenerator`/`ReyonAuditState` (sapma üretimi ve doğrulama, dokunma, ipucu, kayıt);
  `ReyonGenerator` (düzen örnekleme, aday ipuçları, seçim/küçültme, iş
  sayaçları), `Propagator`/`ReyonSolver`/`ReyonDeducer` (kısıt yayılımı, geri izleme, çıkarım izi), `ReyonState`
  (yerleştirme, geri alma, durum, ipucu, kayıt); 46 test: kural değerlendirme, kaba kuvvetle çapraz doğrulama,
  çözümü düşürmeyen yayılım, determinizm, tek çözüm, tahminsizlik, brif uzunluğu, üretim bütçesi, denetim
  değişmezleri (ayrık ve görünür sapmalar, tür kapsamı, dokunma mekaniği), satış puanlaması (elle hesaplı
  örnek), hedef ≥ taban ve geçerli tam doluluk, iyileştirici bütçesi, sipariş gün kuralları (elle izlenen hafta),
  tahmin aralığı, uzman siparişlerinin tekrar oynanışının hedefi birebir vermesi, kayıt tur dönüşü; `ReyonBalanceProbe`

### Raket
- **Raket oyunu** (Pong türü, kendi tasarımımız): dikey kort, altta ve üstte yatay raketler; raket parmakla
  sürüklenir (kazanç 1,25). Vuruş noktası çıkış açısını verir (merkez düz, kenar 62°), raketin vuruş anındaki hızı
  falso ekler (±20°); her vuruşta top %6 hızlanır (tavan 2,7 kat, servisle tabana döner). 11 sayıya iki farkla
  ulaşan kazanır; servisler sırayla iki tarafa gider
- **Üç mod**: Bilgisayar (üç seviye: kolay topu izler, orta düşüşü duvar sekmeleriyle tahmin eder, zor kenarla
  rakibin uzağına vurur; hepsi tepki gecikmesi, hız sınırı ve topla birlikte büyüyen nişan hatasıyla insanı taklit
  eder), İki kişi (aynı telefonda; herkes kendi yarısında sürükler, üst oyuncunun sayısı ona dönük yazılır), Duvar
  (üst raket yok; top arka duvardan hafif sapmayla döner, skor ralli uzunluğu, her 8 vuruşta raket daralır; günlük
  top ya da serbest). Ana menü rekoru en uzun ralli
- Çarpışma süpürmeli: tavan hızda top bir adımda raket kalınlığından fazla yol alsa da vuruş kaçmaz
- Motor `games/raket`: `RaketWorld` (sabit adım, süpürmeli çarpışma, açı/falso, servis, sayı ve maç), `RaketAi`
  (seviye botları; testlerde oyuncu botu olarak da kullanılır); 11 test: determinizm, servis, vuruş açısı ve
  kaçırma, falso, hız rampası ve raket daralması, tünelleme yok, servis sırası ve 11-2 kuralı, duvar rallisi,
  duvar katlamalı tahmin, kort sınırları, seviye sıralaması (orta bot kolayı yener, zora yenilir)

### Tuşe
- **Piyano karoları** (Piano Tiles türü, kendi tasarımımız): dört şerit, her satırda bir karo; sıradaki karonun
  şeridine dokun, ezginin sıradaki notası çalar. Yargı yalnızca şeride bakar (şeritler piyano tuşu gibidir); yanlış
  şerit koşuyu bitirir. Klasik: 50 karo, süre ilk dokunuşla başlar, son vuruşla biter. Sonsuz: ilk dokunuştan sonra
  tahta akar, vurulan karo başına hızlanır (3,2'den 11 satır/s'ye), sıradaki karo dokunulmadan çıkarsa biter.
  Günlük: günün parçası ve şeritleri herkese aynı, 3 deneme, en kısa süre. Ana menü rekoru Sonsuz'daki karo sayısı
- **Yedi telifsiz parça**, elle yazılmış nota dizileri: Neşeye Övgü, Für Elise, Türk Marşı, Daha Dün Annemizin,
  Mutlu Yıllar, Sol Majör Menuet, Greensleeves. Ses dosyası yok: `NoteSynth` beş harmonikli kısa piyano tonunu
  üretir, uygulama WAV'ı önbelleğe yazıp SoundPool ile çalar (izin gerekmez); ana menüdeki ses düğmesi notaları da
  kapatır
- Motor `games/tuse`: `TuseWorld` (şerit dizisi, vuruş/hata, kayma, Sonsuz akışı, süre), `Songs`, `NoteSynth`;
  9 test: şerit determinizmi ve tekrar payı, Klasik akış ve süre, yanlış tuş, kayma, Sonsuz hızlanma ve kaçan karo,
  son ana kadar vuruş, yarıda bırakma, parça geçerliliği (aralık, oktav sıçraması), sentez frekansı ve WAV başlığı;
  Sonsuz eğrisi ölçümü `:games:tuse:probe`

### Uçurtma
- **Basılı tut, uç** (Jetpack Joyride türü, kendi tasarımımız): dünya sağdan sola akar, basılı tutunca ip çekilir
  ve uçurtma yükselir, bırakınca alçalır; zemin güvenli, gök tavanı sert sınır. Akış hızı metreyle artar
  (6,4'ten 10,4 m/s'ye), öbekler arası boşluk ve çatı yüksekliği 1500 m'ye kadar zorlaşır
- **Engeller**: bacalı çatılar, elektrik telleri (alçak, **yüksek** — altından geçmek şart —, çift ya da altında
  çatılı), rakip uçurtmalar, kurdele yayları. **Uçurtma kavgası**: rakibin ipi rakipten sol-alta iner, seninki
  senden; rakibin üstünden geçince ipin onu keser (bonus), altından geçince rakibin ipi seni keser. Gövdeler
  çarpışırsa üstteki kazanır
- **Görevler ve ekipman**: üç görev aynı anda açık (mesafe, kurdele, tel altı, çatı sıyırma, kesme), tamamlanan
  yerine deterministik dizideki sıradaki gelir; 2, 5 ve 9 görevde Kuyruk (alçalma ×0,75), Makara (yükseliş ×1,25)
  ve Cam tozu (rakip ipine bağışık, kesme bonusu 50) açılır. Skor = metre + kurdele × 5 + kesme × 25
- **Günlük mod** herkese aynı gökyüzü, günde 3 deneme; serbest mod rastgele tohum. Ana menü rekoru skor
- Motor `games/ucurtma`: `UcurtmaWorld` (fizik, öbek üretimi, çarpışma, kesme kuralı, görev sayaçları), `Missions`;
  10 test: determinizm, fizik sınırları, ekipman etkisi, hız rampası, çatı/baca/tel/ip çarpmaları, üstten kesme ve
  cam tozu bağışıklığı, kurdele/tel altı/sıyırma sayımı, **geçilebilirlik değişmezi** (her sütunda ≥ 0,3 birim
  boşluk, tavan zorlukta da), görev ilerlemesi, dikkatli pilotun açılışı geçmesi; pilot ölçümü `:games:ucurtma:probe`

### Dalgıç
- **Denizaltıyla kurtarma** (Seaquest türü, kendi tasarımımız): dikey deniz, yüzey üstte, taban altta; denizaltı
  parmakla 2B sürüklenir (kazanç 1,3), yönü son yatay hareketten gelir, torpidolar dalmışken o yöne kendiliğinden
  gider (0,6 s'de bir). Dalgıçlar dokununca toplanır (6 kapasite), yüzeye çıkınca teslim: dalgıç başına 50 + dalga × 10,
  tam yükte +300; her teslim dalgayı artırır (düşman hızı ve doğum sıklığı ×(1 + 0,12 × dalga)), akıntılar yeniden dizilir
- **Oksijen** 30 s, dalarken azalır, yüzeyde saniyede 10 dolar; 8 s'de uyarı, biterse can gider. Daldıktan sonra
  **dalgıçsız yüzeye çıkmak can götürür** (Seaquest kuralı): oksijeni bedavaya doldurmanın bedeli
- **Tehditler**: köpekbalıkları (salınarak yüzer), düşman denizaltılar (2,4 s'de bir torpido; dost torpido düşman
  torpidosunu yolda düşürür), zincirli mayınlar (alt şeritlerde, en çok 3, çarpınca patlar). Can kaybında denizaltı
  yüzeye döner, 2 s dokunulmaz; 3 can. **Boğaz akıntısı**: üç bant denizaltıyı ve dalgıçları yana sürükler
- **Günlük mod** herkese aynı deniz, günde 3 deneme; serbest mod rastgele tohum. Ana menü rekoru skor
- Motor `games/dalgic`: `DalgicWorld` (sürüş, oksijen, şerit doğumu, torpidolar, çarpışma, teslim, akıntılar); 10 test:
  determinizm, sürüş sınırları ve yön, oksijen döngüsü, kapasite ve teslim bonusu, boş yüzeye çıkış kuralı,
  köpekbalığı/mayın ve dokunulmazlık, torpido davranışları (ileri atış, düşman ateşi, torpido çarpışması, dalga puanı),
  akıntılar, doğumun şeritlerde kalması, pilotun teslim edebilmesi; pilot ölçümü `:games:dalgic:probe`

### Bostan
- **Şerit savunması** (Plants vs. Zombies türü, kendi tasarımımız): 5 şerit × 7 hücre tarla; saldırganlar ormandan
  iner, kulübeye ulaşan can götürür (3 can). Karta dokun, hücreye dokun; kürek kaldırır. Su: başlangıç 150/125/100,
  kuyu 8 s'de bir 25, gökten ~7 s'de bir düşen damla dokununca 25 (6 s durur)
- **Savunmalar**: kuyu (50), fıskiye (100; önünde saldırgan varsa 1,4 s'de bir 3 hücre/s jet, 1 hasar), korkuluk
  (50; 24 can, yolu keser), kovan (125; 2 s'de bir çevresindeki üç şeride ±2 satır 2 hasar), tuzak (75; 3 s'de
  kurulur, basana ve ±1 satırına 30 hasar, tükenir; kurulmadan kemirilir). Kart bekleme 5–15 s
- **Saldırganlar**: karga (5 can, 0,18 hücre/s), tavşan (4; 0,36), keçi (12; 0,14), domuz (24; 0,11), ayı (60;
  0,08); önlerine çıkan savunmayı saniyede 0,6–3 kemirir. Puan: karga/tavşan 10, keçi 20, domuz 40, ayı 100,
  temizlenen dalga 50, bitişte can × 100 + su / 5
- **Dalgalar**: Kolay 6, Orta 8, Zor 10; bütçe `zorluk × (0,6 + 0,4 × dalga) × (her 3. dalga 1,5)`, türler dalga
  dizinine göre açılır (tavşan 2., keçi 3., domuz 4., ayı 6. dalga). Dalga temizlenince 4 s sonra sıradaki gelir,
  temizlenmese de süresi + 12 s'de gelir. **Kazanılabilirlik**: üretilen seviye uzman politikasıyla (`BostanExpert`)
  oynatılır, uzman kaybederse bütçe ×0,85 ile yeniden üretilir (en çok altı ölçek); sonuç kartı uzmanın aynı
  bostandaki can ve puanını gösterir
- **Günlük mod** herkese aynı bostan (zorluk başına), günde 3 deneme; serbest mod rastgele tohum; rekorlar zorluk
  başına. Ana menü rekoru skor
- Motor `games/bostan`: `BostanState` (tarla, su, savunma ve saldırgan kuralları, dalga akışı), `BostanExpert`
  (betikli savunma politikası), `BostanGenerator` (dalga üretimi + uzman doğrulaması); 15 test: yerleşim, bekleme
  ve kürek, kuyu ve damla, fıskiye şerit hedefi, korkuluk kesme ve düşüşü, can kaybı ve yenilgi, kovan kapsamı,
  tuzak kurulma/patlama/kemirilme, dalga akışı ve kazanma puanı, bekleme payıyla geçiş, üretici determinizmi ve
  tür açılışı, zorluk sıralaması, her üretilen seviyenin uzmanla kazanılması, günlük tohum; ölçüm
  `:games:bostan:probe`

### Sincap
- **Dikey tırmanış** (Go Bananas türü, kendi tasarımımız): çınar gövdesi ortada, her basamakta solda ve/veya
  sağda bir dal. Sola ya da sağa dokununca sincap o yöndeki en yakın üst dala atlar (en çok 2 basamak; iki
  basamak daha uzun sürer), o yönde erişilen dal yoksa boşluğa düşer. Zıplarken gelen dokunuş bekletilir ve
  konar konmaz uygulanır. Tuvalin sol yarısı sol, sağ yarısı sağ; ilk temasta tepki
- **Tehlikeler**: kuru dal konduktan 1,1 s sonra kırılır; yılanlı dala konan ölür; kargalar kendi basamağından
  ekranı boydan boya geçer (0,9–1,5 birim/s), dalın üstündeyken çarpan düşürür; **kedi** gövdeden tırmanır,
  yükseklikle hızlanır (0,5 → 3,2 basamak/s 250 basamakta, sonra yavaşça artmaya sürer) ve yetişirse yakalar —
  koşuyu bitiren tempo baskısı. Fındık +20, altın fındık +100, her basamak +10; 25 basamakta bir kilometre taşı
- **Üretim** her basamakta en az bir dal bırakır; ilk 3 basamak iki yanda sağlam dal; kuru/yılan/karga oranı
  150 basamağa dek artar. **Kaçış garantisi**: her basamaktan iki yönün de erişilen ilk dalı yılanlı olamaz
  (`repair`); kargalı basamağın altındaki basamakta kuru dal olmaz (bekleyecek yer kalsın)
- **Günlük mod** herkese aynı çınar, günde 3 deneme; serbest mod rastgele tohum, rekor skor ve yükseklik. Ana
  menü rekoru skor
- Motor `games/sincap`: `SincapWorld` (üretim + kaçış onarımı, zıplama/konma, kuru dal, karga, kedi, puan),
  `SincapBots` (ölçüm pilotu); 12 test: determinizm, her basamakta dal ve kaçış (30 tohum × 400 basamak, güvenli
  yol araması), yön ve erişim kuralı, boşluğa düşme, kuru dal kırılma/kaçış, yılan, karga (kendi dalı, başka
  basamak, zıplarken geçen), kedi yakalama ve hızlanma, fındık/altın puanı, bekletilen dokunuş, kilometre taşları
  ve pilot; ölçüm `:games:sincap:probe`

### Çekirge
- **Formasyon ateşi** (Space Invaders türü, kendi tasarımımız): 7 × 5 çekirge sürüsü blok hâlinde yana yürür,
  kenara varınca yön değiştirip 0,03 iner; hız dalgayla (+%15/dalga) ve seyrelmeyle (son çekirge 4 kat) artar.
  Alt sıradaki çekirgeler tükürük bırakır (1. dalgada 2, sonra 3 uçan; aralık 1,8 → 0,45 s). Üst sıra kara 30,
  orta iki sıra yeşil 20, alt iki sıra kahverengi 10 puan
- **Tek fıskırtma kuralı**: ilaç fıskırtması (1,8 birim/s) hedefe varmadan ya da tarlayı terk etmeden yenisi
  atılamaz; fıskırtma tükürüğü de yolda düşürür. Parmak sürüklemesi çiftçiyi ilk pikselden itibaren yürütür,
  kıpırdamadan kalkan parmak (≤ 300 ms) sıkar; iki başparmakla oynanabilir (biri yürütür, öteki sıkar)
- **Saman balyaları** (4 × 6 × 3 hücre) fıskırtma, tükürük ve sürü temasıyla hücre hücre aşınır; dalgalar
  arasında onarılmaz. Tükürük çiftçiye çarparsa can gider (3 can; 1,5 s dokunulmazlık, sürü 1,2 s durur). Sürü
  çiftçi hizasına inerse **istila**: candan bağımsız biter. **Kraliçe** 16–28 s'de bir üstten geçer (100/150/200/300)
- **Dalgalar**: sürü temizlenince 100 × dalga bonus, 1,5 s sonra yeni dalga bir kademe aşağıdan (en çok 0,5) ve daha
  hızlı başlar. **Günlük mod** herkese aynı tarla (tükürük ve kraliçe zamanları tohumdan), günde 3 deneme; serbest mod
  rastgele tohum, rekor skor ve dalga. Ana menü rekoru skor
- Motor `games/cekirge`: `CekirgeWorld` (formasyon, tek mermi, tükürük, balya aşınması, kraliçe, dalga, istila),
  `CekirgeBots` (ölçüm pilotu: varış anı tahminiyle nişan, tükürük yolundan kaçış); 12 test: determinizm, formasyon
  ve türler, tek mermi kuralı, sütunun alt çekirgesi ve puan, kenarda dönüş ve iniş, seyrelince ve dalgayla hızlanma,
  tükürük ve dokunulmazlık, balya aşınması (üç kaynak), kraliçe geçişi ve bonusu, dalga temizliği, istila, günlük
  tohum ve pilot; ölçüm `:games:cekirge:probe`

### Cici
- **Bölüm 1: Uzayda** (kendi tasarımımız): beyaz muhabbet kuşu Cici uzayda süzülür; parmakla sürüklenir (bire bir,
  hız sınırı 1,3 arena/s), dikey arena 1 × 1,6. Kenarlardan süzülen ikramlar dokununca yakalanır: **ballı yem 7**,
  **kuş yemi 5**, **su 2** puan (çıkma oranı %15 / %55 / %30)
- Tehlikeler: **uzay kedileri** yanlardan geçer, 45. saniyeden sonra Cici'nin hizasına kıvrılmaya başlar; **kırmızı top**
  3. saniyede en uzak köşeden çıkar, kenarlardan seker ve hızlanır (0,24 → 0,55 arena/s), 120. saniyede ikincisi gelir.
  Temas can götürür: 3 can, 2 s dokunulmazlık, seri sıfırlanır
- **Sevinç**: her ikramda Cici sevinir (kalpler, cıvıltı); 3 s içinde art arda yakalamalar seriyi büyütür, her üçüncüde
  "Cici çok sevindi!" kutlaması. **Hareketsizlik**: 2 s kıpırdamayınca sıkılır (yarı kapalı göz), 3 s'den sonra her
  saniye 1 puan gider, sıfırın altına inmez; hareket parmak girişiyle ölçülür
- Zorluk 180 s boyunca doğrusal artar: ikram aralığı 1,3 → 0,7 s ve hızı ×1,8, kedi aralığı 8 → 3,5 s ve hızı ×1,9.
  **Günlük uzay** herkese aynı akışı verir (günde 3 deneme); serbest mod rastgele tohum, rekor skor
- Motor `games/cici`: `CiciWorld` (arena, sürükleme hedefi, ikram/kedi/top akışı, temas, sevinç serisi,
  hareketsizlik cezası, zorluk rampası, bölüm sabiti), `CiciBots` (ölçüm pilotu: değer/uzaklık ile ikram seçimi,
  yaklaşan tehdidin yoluna dik kaçış); 11 test: determinizm, sınır ve hız, üç ikramın puanı ve seri, ruh hâli,
  hareketsizlik cezası (2 s uyarı, 3 s sonra saniyede 1, sıfırda durur), kedi/top teması ve dokunulmazlık, topun
  kenarlardan sekmesi ve hızlanması, rampanın tekdüzeliği, doğumun kenardan gelip ekranı terk etmesi, pilotun
  puan alıp hayatta kalması, özet

### Vergici ve Toplam Kapma (`games/sayi`)
- **Vergici** (Taxman): 1–N tahtası; böleni kalmış bir sayıyı alırsın, vergici o sayının tahtadaki tüm bölenlerini
  alır; alınacak sayı kalmayınca kalanlar vergiciye. Aralıklar 1–12, 20, 30, 40; dokununca vergicinin alacakları
  kırmızı görünür, tekrar dokununca ya da "Al" ile onaylanır. Hedef puan: 1–30'a kadar bellekli tam arama (kesin),
  1–40'ta açgözlü sezgisel (yaklaşık). Rekor = vergiciyi yenme sayısı, aralık başına en iyi puan ayrıca saklanır
- **Toplam Kapma** (Number Scrabble / Fifteen): 1–9'dan sırayla sayı al, üç sayın 15 ederse kazan. Bilgisayar
  minimax ile kusursuz ya da yüzde 35 rastgele hata yapan kolay seviyede; iki oyuncu aynı telefonda; başlayan her
  oyunda değişir. Oyun sonunda "Sır": sayılar Lo Shu sihirli karesine yerleşince oyunun üç taş olduğu görülür

## Derleme

Arayüz testleri JVM'de koşar, emülatör gerekmez: `./gradlew :app:testDebugUnitTest` (Robolectric + Compose test kuralı;
ana menü, gezinme, Sudoku/Kakuro/Mayın Tarlası/Beş Harf etkileşimleri, Hakkında ve 19 oyun ekranının duman testi).
CI her itmede motor testleriyle birlikte koşturur; sürüm iş akışı da bunlar geçmeden APK üretmez.

Gereksinimler: JDK 17+, Android SDK (compileSdk 35). Android Studio ile açıp çalıştırabilir veya komut satırından derleyebilirsiniz:

```bash
./gradlew :app:assembleDebug        # APK: app/build/outputs/apk/debug/
./gradlew :games:tetris:test :games:g2048:test :games:snake:test :games:sudoku:test :games:mines:test :games:besharf:test :games:kiskac:test :games:turetme:test :games:dizgi:test :games:kuyu:test :games:gecit:test :games:tavla:test :games:balkon:test :games:kakuro:test :games:sayi:test
```

Motor testleri Android SDK gerektirmez. Sürüm `-PzaVersion=X.Y.Z` özelliğiyle geçilir; release iş akışı bunu etiketten türetir (`versionCode` = `major*10000 + minor*100 + patch`).

- minSdk 26 (Android 8.0) · targetSdk 35
- Kotlin 2.1 · Jetpack Compose (Material 3) · AGP 8.10

## Yayınlama

### İmzalı APK (GitHub Release)

`v*` etiketi push'lanınca (veya iş akışı elle `tag_name` ile tetiklenince) `release.yml` sürümü etiketten türetir, testleri koşar ve şu dosyaları Release'e ekler:

- `za.apk` — imzalı APK, sabit ad (site bunu kullanır)
- `za-vX.Y.Z.apk` — sürümlü kopya
- `za-vX.Y.Z-source.zip` / `.tar.gz` — kaynak arşivleri (`git archive`)
- `SHA256SUMS.txt` — tüm dosyaların sağlama toplamları

En yeni sürüm her zaman şu sabit adresten inebilir:

```
https://github.com/aripdcem/za/releases/latest/download/za.apk
```

Doğrulama: `sha256sum -c SHA256SUMS.txt`

Gerekli depo secret'ları (yalnızca depo sahibi ayarlar):

```bash
keytool -genkeypair -v -keystore keys/za-release.jks -alias za -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 keys/za-release.jks | gh secret set ANDROID_KEYSTORE_BASE64 --repo aripdcem/za
gh secret set ANDROID_KEYSTORE_PASSWORD --repo aripdcem/za
```

Sürüm çıkarmak: `git tag v0.1.0 && git push origin v0.1.0`

### Web sitesi (za.aripd.com)

`site/` klasörü GitHub Pages ile yayınlanır (`pages.yml`). Tek seferlik kurulum:

1. Depo **Settings → Pages** → Source: **GitHub Actions**
2. Aynı sayfada Custom domain: **za.aripd.com**
3. DNS'te `za.aripd.com` için `aripdcem.github.io` hedefli **CNAME** kaydı

## Lisans ve marka

- **Kod:** GNU General Public License v3.0 veya sonraki bir sürümü (`LICENSE`; SPDX: `GPL-3.0-or-later`). Kodu
  alıp değiştirebilir ve dağıtabilirsin; dağıttığın türevin kaynağı da aynı lisansla açık olmalı.
- **Marka:** "ZA" ve "ZA Games" adları ile ZA logosu lisansa dahil değildir. Kodu kullanan ya da türeten projeler
  bu ad ve logoyla dağıtılamaz; kendi adını ve simgesini kullanmalıdır.
- **Kelime listeleri:** Zemberek-NLP kök sözlüğünden (Apache-2.0) ve FrequencyWords tr_50k'dan (CC BY-SA 4.0)
  türetilmiştir; listeler CC BY-SA 4.0 koşullarıyla paylaşılır (`games/*/src/main/resources`).
- Sesler, görseller ve oyun tasarımları ZA'ya aittir ve kodla birlikte aynı lisans kapsamındadır. Katkılar aynı
  lisansla kabul edilir. Uygulama içi **Hakkında** ekranı üçüncü taraf bileşenleri ve lisanslarını listeler.

## Yol haritası

- [x] Yeni oyunlar: 2048 ✓, yılan ✓, sudoku ✓, mayın tarlası ✓, beş harf ✓, kıskaç ✓, türetme ✓, dizgi ✓, kuyu ✓, geçit ✓, tavla ✓, balkon ✓, kakuro ✓, vergici ✓, toplam kapma ✓
- [x] Ses efektleri (kapatılabilir) ve satır temizleme animasyonları
- [x] Sürümün etiketten türetilmesi, SHA256 sağlamaları ve kaynak arşivleri
- [ ] Oyun içi istatistikler (toplam satır, en uzun oturum)
- [ ] Uygulamada açık tema seçeneği (web sitesi sistem temasına uyar)
- [ ] F-Droid / Play Store yayını

---

### English summary

**ZA** is an Android platform for truly ad-free games ("zero ad game play"): no ads, no trackers, no permissions (not even INTERNET), no purchases. It ships **Blok** (a falling-blocks puzzle: SRS-style wall kicks, 7-bag, hold, ghost piece, line-clear flash + sound), **2048** and **Snake**. Game rules live in deterministic, fully unit-tested pure Kotlin modules under `games/`; the Compose UI lives in `app`. Sound effects are tiny procedurally generated WAVs (`tools/gen_sfx.py`) and can be muted from the hub. Add a game by writing an engine module, a Compose screen, and one `GameEntry` in `GameRegistry`. Build with `./gradlew :app:assembleDebug`, test engines with `./gradlew :games:tetris:test :games:g2048:test :games:snake:test`. Licensed under GPL-3.0-or-later; the "ZA" name and logo are not part of the license.
