# ZA · Zero Ad Games

> **Sıfır reklam. Sıfır izleyici. Sıfır izin. Saf oyun.**

ZA, Android telefonlar için **"zero ad game play"** konseptiyle geliştirilen bir mobil oyun platformudur. Çatı altındaki her oyun tamamen reklamsızdır; uygulama hiçbir izin istemez (İNTERNET izni dahil), hiçbir veri toplamaz ve hiçbir şey satmaz. Uygulama tam ekran açılır; sistem çubukları kenardan kaydırınca geçici görünür. Oyunlar: **Tetris**, **2048**, **Yılan**, **Sudoku**, **Mayın Tarlası**, **Beş Harf**, **Kıskaç**, **Türetme**, **Dizgi**, **Kuyu**, **Geçit**, **Tavla**, **Balkon**, **Kakuro**.

Ana menüde oyunlar gruplara ayrılır (Kelime, Bulmaca, Arcade, Masa; süzgeç çipleri, seçim kalıcı) ve en üstte son oynanan dört oyun için hızlı erişim şeridi bulunur.

## Manifesto

| Söz | Uygulamadaki karşılığı |
| --- | --- |
| 0 reklam | Hiçbir reklam SDK'sı yok |
| 0 izleyici | Analitik/izleme kütüphanesi yok |
| 0 izin | `AndroidManifest.xml` tek bir `uses-permission` içermez |
| 0 satın alma | Ödeme/abonelik kodu yok |
| Saf oyun | Skorlar yalnızca cihazda saklanır |

## Mimari

```
za/
├── app/                          # Android uygulaması (Kotlin + Jetpack Compose)
│   └── com.za.games
│       ├── platform/             # Çekirdek: GameRegistry, ScoreStore, SettingsStore, SoundPlayer
│       ├── ui/common/            # Oyunların paylaştığı bileşenler (tuşlar, katmanlar, kartlar)
│       ├── ui/hub/               # Ana menü (oyun listesi + manifesto + ses düğmesi)
│       ├── ui/&lt;oyun&gt;/            # Oyunların Compose arayüzleri
│       └── ui/theme/             # ZA teması
├── games/
│   ├── tetris/  g2048/  snake/   # Oyun motorları: saf Kotlin/JVM, Android'e
│   └── sudoku/ mines/ besharf/ kiskac/ turetme/ dizgi/ kuyu/ gecit/ tavla/ balkon/ kakuro/ # bağımsız, her biri kendi birim testleriyle
└── tools/                        # gen_sfx.py (sesler), gen_words.py + gen_turetme.py + gen_dizgi.py (kelime listeleri)
```

Temel ilke: **oyun kuralları saf Kotlin modüllerinde, arayüz `app` içinde** yaşar. Motorlar Android'e bağımlı olmadığı için cihazsız test edilir ve ileride başka platformlara taşınabilir.

### Yeni oyun eklemek

1. `games/<oyun>/` altında saf Kotlin motor modülü oluşturun (testleriyle birlikte) ve `settings.gradle.kts`'e ekleyin.
2. `app/src/main/kotlin/com/za/games/ui/<oyun>/` altında Compose ekranını yazın.
3. `GameRegistry.games` listesine bir `GameEntry` ekleyin — ana menü kartı ve rekor takibi kendiliğinden çalışır.

## Oyunlar

Tüm motorlar deterministiktir: aynı tohumla (seed) başlayan iki oyun, aynı hamlelerle birebir aynı sonucu üretir. Rekorlar cihazda saklanır; oyunlar arka plana geçince kendiliğinden duraklar. Ses efektleri prosedürel üretilmiş küçük WAV'lardır ve ana menüden tamamen kapatılabilir.

### Tetris
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
- **Kuyuya düşüş** (Downwell türü, kendi tasarımımız): tek tuş — yerdeyken zıplar, havada basılı
  tutulunca **botlar aşağı ateş eder** ve düşüşü yavaşlatır; şarjör 8, yere inince ya da düşmana
  basınca dolar
- Düşmanlar: topak ve yarasa (üstüne basılır), dikenli (yalnız mermiyle), duvar tırmanıcısı; havada art
  arda öldürdükçe **kombo**, inişte kombo × 2 taş bonusu; kırılabilir bloklar, taş bırakan bloklar
- Kuyu 12 sütun genişliğinde, 16 satırlık parçalarla tohumdan üretilir: her satırda en az 3 boşluk,
  dibe kadar geçilebilirlik testle garanti (`games/kuyu`); 3 bölge, derinlikle daha çok ve daha hızlı düşman
- **Günlük kuyu**: gün numarasından türeyen tohum, herkes aynı kuyuyu oynar, tek deneme; ayrıca serbest mod
- Kontrol eli ayarı (sağ/sol, Geçit ile ortak): ateş tuşu başparmağın tarafına gelir; 60 Hz sabit adımlı
  simülasyon, aynı tohum + aynı girdi = aynı oyun
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

### Kakuro
- Toplam bulmacası: kara hücrelerdeki ipucu, sağındaki yatay ve altındaki dikey koşunun toplamı; koşudaki rakamlar
  farklı. Üç boy: 7×7, 9×9, 11×11 (ipucu kenarı hariç). Notlar, geri alma, çakışma ve tamamlanan koşu vurgusu,
  seçili hücrenin koşularında kalan toplam; yarım kalan bulmaca cihazda saklanır
- **Tek çözüm garantisi**: 180° simetrik rastgele düzen (koşular 2–9), koşu içinde yinelenmeyen rastgele doldurma,
  ardından yayılımlı çözücüyle (toplam–uzunluk kombinasyonları, aday daraltma, geri izleme) iki çözüm arandığında
  farklı hücreler yeniden dağıtılır; birkaç aday arasından koşuları en sıkı kılan seçilir. Tohumdan deterministik
  (`games/kakuro`, 8 test)

## Derleme

Gereksinimler: JDK 17+, Android SDK (compileSdk 35). Android Studio ile açıp çalıştırabilir veya komut satırından derleyebilirsiniz:

```bash
./gradlew :app:assembleDebug        # APK: app/build/outputs/apk/debug/
./gradlew :games:tetris:test :games:g2048:test :games:snake:test :games:sudoku:test :games:mines:test :games:besharf:test :games:kiskac:test :games:turetme:test :games:dizgi:test :games:kuyu:test :games:gecit:test :games:tavla:test :games:balkon:test :games:kakuro:test
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

## Yol haritası

- [x] Yeni oyunlar: 2048 ✓, yılan ✓, sudoku ✓, mayın tarlası ✓, beş harf ✓, kıskaç ✓, türetme ✓, dizgi ✓, kuyu ✓, geçit ✓, tavla ✓, balkon ✓, kakuro ✓
- [x] Ses efektleri (kapatılabilir) ve satır temizleme animasyonları
- [x] Sürümün etiketten türetilmesi, SHA256 sağlamaları ve kaynak arşivleri
- [ ] Oyun içi istatistikler (toplam satır, en uzun oturum)
- [ ] Uygulamada açık tema seçeneği (web sitesi sistem temasına uyar)
- [ ] F-Droid / Play Store yayını

---

### English summary

**ZA** is an Android platform for truly ad-free games ("zero ad game play"): no ads, no trackers, no permissions (not even INTERNET), no purchases. It ships **Tetris** (SRS wall kicks, 7-bag, hold, ghost piece, line-clear flash + sound), **2048** and **Snake**. Game rules live in deterministic, fully unit-tested pure Kotlin modules under `games/`; the Compose UI lives in `app`. Sound effects are tiny procedurally generated WAVs (`tools/gen_sfx.py`) and can be muted from the hub. Add a game by writing an engine module, a Compose screen, and one `GameEntry` in `GameRegistry`. Build with `./gradlew :app:assembleDebug`, test engines with `./gradlew :games:tetris:test :games:g2048:test :games:snake:test`.
