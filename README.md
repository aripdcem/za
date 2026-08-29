# ZA · Zero Ad Games

> **Sıfır reklam. Sıfır izleyici. Sıfır izin. Saf oyun.**

ZA, Android telefonlar için **"zero ad game play"** konseptiyle geliştirilen bir mobil oyun platformudur. Çatı altındaki her oyun tamamen reklamsızdır; uygulama hiçbir izin istemez (İNTERNET izni dahil), hiçbir veri toplamaz ve hiçbir şey satmaz. Oyunlar: **Tetris**, **2048**, **Yılan**, **Sudoku**, **Mayın Tarlası**.

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
│   └── sudoku/  mines/           # bağımsız, her biri kendi birim testleriyle
└── tools/gen_sfx.py              # Ses efektlerini prosedürel üreten betik (res/raw)
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
- Kaydırma jestleri + yön tuşları; ters yöne dönüş engellenir

### Sudoku
- Geri izlemeli üreteç: her bulmacada **tek çözüm garantisi** (MRV'li çözüm sayacıyla doğrulanır)
- Üç zorluk (40/32/26 ipucu), kalem notları, çakışma vurgusu, rakam başına kalan sayacı
- Değer girilince komşu hücrelerdeki aynı rakam notları otomatik silinir
- Rekor = toplam çözülen bulmaca sayısı

### Mayın Tarlası
- Üç zorluk: 9×12/14, 10×14/25, 12×17/40 — **ilk dokunuş her zaman güvenli** (mayınlar sonra yerleşir)
- Sıfır hücrelerde akan açılım, uzun basış veya bayrak moduyla işaretleme, sayıya dokununca chord
- Rekor = toplam kazanılan oyun sayısı

## Derleme

Gereksinimler: JDK 17+, Android SDK (compileSdk 35). Android Studio ile açıp çalıştırabilir veya komut satırından derleyebilirsiniz:

```bash
./gradlew :app:assembleDebug        # APK: app/build/outputs/apk/debug/
./gradlew :games:tetris:test :games:g2048:test :games:snake:test :games:sudoku:test :games:mines:test
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

- [x] Yeni oyunlar: 2048 ✓, yılan ✓, sudoku ✓, mayın tarlası ✓
- [x] Ses efektleri (kapatılabilir) ve satır temizleme animasyonları
- [x] Sürümün etiketten türetilmesi, SHA256 sağlamaları ve kaynak arşivleri
- [ ] Oyun içi istatistikler (toplam satır, en uzun oturum)
- [ ] Uygulamada açık tema seçeneği (web sitesi sistem temasına uyar)
- [ ] F-Droid / Play Store yayını

---

### English summary

**ZA** is an Android platform for truly ad-free games ("zero ad game play"): no ads, no trackers, no permissions (not even INTERNET), no purchases. It ships **Tetris** (SRS wall kicks, 7-bag, hold, ghost piece, line-clear flash + sound), **2048** and **Snake**. Game rules live in deterministic, fully unit-tested pure Kotlin modules under `games/`; the Compose UI lives in `app`. Sound effects are tiny procedurally generated WAVs (`tools/gen_sfx.py`) and can be muted from the hub. Add a game by writing an engine module, a Compose screen, and one `GameEntry` in `GameRegistry`. Build with `./gradlew :app:assembleDebug`, test engines with `./gradlew :games:tetris:test :games:g2048:test :games:snake:test`.
