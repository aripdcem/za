# ZA · Zero Ad Games

> **Sıfır reklam. Sıfır izleyici. Sıfır izin. Saf oyun.**

ZA, Android telefonlar için **"zero ad game play"** konseptiyle geliştirilen bir mobil oyun platformudur. Çatı altındaki her oyun tamamen reklamsızdır; uygulama hiçbir izin istemez (İNTERNET izni dahil), hiçbir veri toplamaz ve hiçbir şey satmaz. İlk oyun: **Tetris**.

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
│       ├── platform/             # Platform çekirdeği: GameEntry, GameRegistry, ScoreStore
│       ├── ui/hub/               # Ana menü (oyun listesi + manifesto)
│       ├── ui/tetris/            # Tetris'in Compose arayüzü
│       └── ui/theme/             # ZA teması
└── games/
    └── tetris/                   # Tetris motoru: saf Kotlin/JVM, Android'e bağımsız
        └── com.za.games.tetris   # Değişmez durum makinesi + birim testleri
```

Temel ilke: **oyun kuralları saf Kotlin modüllerinde, arayüz `app` içinde** yaşar. Motorlar Android'e bağımlı olmadığı için cihazsız test edilir ve ileride başka platformlara taşınabilir.

### Yeni oyun eklemek

1. `games/<oyun>/` altında saf Kotlin motor modülü oluşturun (testleriyle birlikte) ve `settings.gradle.kts`'e ekleyin.
2. `app/src/main/kotlin/com/za/games/ui/<oyun>/` altında Compose ekranını yazın.
3. `GameRegistry.games` listesine bir `GameEntry` ekleyin — ana menü kartı ve rekor takibi kendiliğinden çalışır.

## Tetris

- 10×20 tahta, yedi standart tetromino
- **7'li torba (7-bag)** rastgeleliği — taş kıtlığı yaşanmaz
- **SRS rotasyon** ve tam duvar tekmesi (wall kick) tabloları
- **Hold**, 3 taşlık **sıradaki** kuyruğu ve **hayalet taş**
- Guideline skorlaması: 100/300/500/800 × seviye; yumuşak düşüş +1, sert düşüş +2/hücre
- Her 10 satırda seviye atlama ve Guideline yerçekimi eğrisi
- Kontroller: ekran tuşları (basılı tutunca tekrar eder) + tahta üstünde dokunma/sürükleme jestleri
- Rekor cihazda saklanır; arka plana geçince oyun kendiliğinden duraklar

Motor deterministiktir: aynı tohumla (seed) başlayan iki oyun, aynı hamlelerle birebir aynı sonucu üretir. Bu, testleri güvenilir kılar ve ileride "yeniden oynatma" özelliğine kapı açar.

## Derleme

Gereksinimler: JDK 17+, Android SDK (compileSdk 35). Android Studio ile açıp çalıştırabilir veya komut satırından derleyebilirsiniz:

```bash
./gradlew :app:assembleDebug        # APK: app/build/outputs/apk/debug/
./gradlew :games:tetris:test        # Motor birim testleri (Android SDK gerektirmez)
```

- minSdk 26 (Android 8.0) · targetSdk 35
- Kotlin 2.1 · Jetpack Compose (Material 3) · AGP 8.10

## Yayınlama

### İmzalı APK (GitHub Release)

`v*` etiketi push'lanınca `release.yml` iş akışı imzalı APK'yı derleyip GitHub Release'e `za.apk` adıyla ekler. En yeni sürüm her zaman şu sabit adresten inebilir:

```
https://github.com/aripdcem/za/releases/latest/download/za.apk
```

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

- [ ] Yeni oyunlar: 2048, yılan, sudoku, mayın tarlası…
- [ ] Ses efektleri (kapatılabilir) ve satır temizleme animasyonları
- [ ] Oyun içi istatistikler (toplam satır, en uzun oturum)
- [ ] Açık tema seçeneği
- [ ] F-Droid / Play Store yayını

---

### English summary

**ZA** is an Android platform for truly ad-free games ("zero ad game play"): no ads, no trackers, no permissions (not even INTERNET), no purchases. Game rules live in pure Kotlin modules (`games/tetris` ships a deterministic, fully unit-tested Tetris engine with 7-bag randomizer and SRS wall kicks); the Compose UI lives in `app`. Add a game by writing an engine module, a Compose screen, and one `GameEntry` in `GameRegistry`. Build with `./gradlew :app:assembleDebug`, test the engine with `./gradlew :games:tetris:test`.
