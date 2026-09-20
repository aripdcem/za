# Yayın öncesi kontrol listesi (Google Play)

Durum: **2026-09-20**, sürüm 0.41.1. İşaret kutusu `[x]` = bitti, `[ ]` = açık.
Sahip kolonu kimin yapacağını söyler: **C** = Cem (hesap, cihaz, karar), **A** =
asistan (depo, metin, görsel üretimi).

## 1. Hesap ve uygulama

| | Madde | Sahip |
| --- | --- | --- |
| [x] | Geliştirici hesabı açıldı | C |
| [ ] | Kimlik doğrulaması tamamlandı (Console'un istediği belgeler) | C |
| [ ] | Ödeme profili (ücretsiz uygulama için zorunlu değil; ücretli katman için gerekir) | C |
| [ ] | Uygulama oluştur: ad `ZA Games: Sıfır Reklam`, varsayılan dil Türkçe, tür Oyun, ücretsiz | C |
| [x] | Paket adı `com.za.games` | A |
| [ ] | **Play App Signing** açık; yükleme anahtarı = sürüm iş akışındaki release anahtarı | C |
| [ ] | İletişim e-postası seçimi: `za-games@aripd.com` (öneri) ya da `dev@aripd.com`. Listelemede herkese görünür | C |
| [ ] | Seçilen adres uygulama içine de eklenecek mi? Hakkında ekranında bugün GitHub "Sorun bildir" var, e-posta yok | C karar, A uygular |

## 2. Uygulama içi zorunluluklar (eskiden "satışa hazır" listesinin 1. maddesi)

| | Madde | Sahip |
| --- | --- | --- |
| [x] | Düşen blok oyununun adı: kullanıcıya görünen her yerde **Blok** (v0.21.0), modül/paket/sınıf/metin kimlikleri de Blok (v0.39.0) | A |
| [x] | Lisans ekranı: Hakkında içinde açık kaynak bileşenleri, CC BY-SA notu, Apache 2.0 metni, GPL-3.0 satırı | A |
| [x] | Gizlilik politikası sayfası ve uygulama içi bağlantısı: https://za.aripd.com/gizlilik.html | A |
| [x] | Hata bildirme bağlantısı: Hakkında → "Sorun bildir" (GitHub issues) | A |
| [x] | Web sitesi ve kaynak kodu bağlantıları | A |

## 3. Mağaza listesi

| | Madde | Sahip |
| --- | --- | --- |
| [x] | Başlık, kısa ve tam açıklama 14 dilde: `store/play/<dil>/` (sınırlar, dil kapsamı ve görünmez karakterler `tools/check_store.py` ile denetlenir, CI'da koşar) | A |
| [ ] | Her dilin Play yerel ayarı Console'da açılmalı (eşleme: `store/README.md`); açılmayan dil İngilizce listeleme görür | C |
| [x] | Sürüm notları her sürüm için: `store/play/release-notes/<sürüm>.txt` | A |
| [x] | Uygulama simgesi 512×512 PNG (32 bit): `store/graphics/icon-512.png` | A |
| [x] | Öne çıkan görsel 1024×500: `store/graphics/feature-1024.png` (metinsiz, 14 dilde geçerli) | A |
| [ ] | Telefon ekran görüntüleri: en az 2, en çok 8; 9:16; kısa kenar ≥ 320 px, uzun kenar ≤ 3840 px | A çekim listesi + betik, C cihazda çeker |
| [ ] | İsteğe bağlı: 7" ve 10" tablet ekran görüntüleri (bkz. Android 16 dikey kilit nüansı, madde 5) | C |
| [ ] | Kategori: Oyunlar > Bulmaca; etiketler: kelime, sudoku, tavla, çevrimdışı | C |

## 4. Politika formları

| | Madde | Sahip |
| --- | --- | --- |
| [x] | Veri güvenliği formu cevapları hazır: `store/data-safety.md` | A |
| [x] | İçerik derecelendirme (IARC) anketi cevapları hazır: `store/icerik-derecelendirme.md` | A |
| [ ] | Anketi Console'da doldur ve derecelendirmeyi al | C |
| [ ] | Hedef kitle ve içerik: 13+ (gerekçe içerik derecelendirme dosyasında) | C |
| [x] | Reklam beyanı: reklam yok | A |
| [ ] | Haber uygulaması / COVID / finans / sağlık / devlet: hepsine hayır | C |

## 5. Yapı

| | Madde | Sahip |
| --- | --- | --- |
| [x] | Android App Bundle: `release.yml` her sürümde `za-<etiket>-play.aab` çıkarır (APK yandan kurulum için kalır) | A |
| [x] | `versionCode` her yüklemede artar (etiketten türetilir) | A |
| [x] | `targetSdk`/`compileSdk` 36 (Android 16); Play tabanı her yıl yükseltir, güncel değeri Console söyler | A |
| [x] | R8 + kaynak küçültme açık; paket ~2,4 MB, izin yok, ağ yok, üçüncü taraf SDK yok | A |
| [x] | Motor testleri sürüm kapısında eksiksiz: `./gradlew :games:engineTests` (liste `games/` altından türetilir) | A |
| [ ] | Android 16 nüansı: hedef 36 olduğu için büyük ekranlarda dikey kilit yok sayılabilir. Telefonda etkisi yok; tablet ekran görüntüsü verilecekse yerleşim önce cihazda görülmeli | C ölçer, A düzeltir |

## 6. Kalite kapısı (yayından önce)

| | Madde | Sahip |
| --- | --- | --- |
| [ ] | **Dahili test kanalına AAB yükle**; Play'in ön lansman raporu (robot testi) gerçek cihazlarda koşar | C |
| [ ] | Android Vitals: çökme ve ANR verisini izle. Uygulamaya izleyici koymadan en büyük kör noktayı kapatır | C |
| [ ] | **Cihaz matrisi**: en az üç ekran boyutu, üç Android sürümü, büyük yazı ölçeği ve hareket navigasyonu ile her oyunda tam bir tur | C |
| [x] | Cihaz protokolü A–E ve eşikler: [`docs/oyun-testi.md`](../docs/oyun-testi.md); 27 oyunun A aşaması, sürekli çizenlerin B, sürüklemelilerin C aşaması SM-A515F'te yapıldı | A + C |
| [ ] | Vitals'ta beklenen uyarı: "yavaş çizim". Kare düşmüyor ama kare gecikmesi Viraj 34 ms, Filo 31 ms, jank %100/%81. Engel değil, sürpriz olmasın | A izler |

### Cihaz matrisinin bugünkü durumu

Ölçülen tek cihaz **SM-A515F** (Android 13, 1080×2400, 420 dpi). Eksikler:

- [ ] Küçük ekran (≈ 360 dp genişlik) ve büyük ekran (≈ 600 dp+) turu
- [ ] İkinci ve üçüncü Android sürümü (ör. 10–12 ve 15–16)
- [ ] Yazı ölçeği %130 ve %200: kartlar, HUD ve tuval üstü etiketler taşıyor mu
- [ ] Hareket navigasyonu (gesture nav) ile geri jesti: oyun içinde duraklatma, kartlarda çıkış
- [ ] TalkBack turu: `python3 tools/cihaz_testi.py erisim` her ekranda etiketsiz öğe bulmamalı

## 7. Test kanalları ve yayın

| | Madde | Sahip |
| --- | --- | --- |
| [ ] | Kapalı test: yeni bireysel hesaplarda zorunlu. Yaklaşık 12–20 test kullanıcısı, 14 gün; güncel sayıyı Console söyler | C |
| [ ] | Testçi geri bildirimini düzeltmelere çevir (her düzeltme kendi sürümüyle) | A |
| [ ] | Üretim: aşamalı yayın (ör. %20 → %100) | C |

## 8. 1.0 ve ücretli katman (henüz açılmayacak)

| | Madde | Sahip |
| --- | --- | --- |
| [ ] | Kodda `play`/`libre` ayrımı: aynı kaynaktan iki dağıtım, tek yapı akışı | A |
| [ ] | Kilit arayüzü: kilitli oyun kartı, açma ekranı, geri yükleme | A |
| [ ] | Play Billing entegrasyonu hazır ama kapalı (bayrakla) | A |
| [ ] | Vitals temiz ve testçi geri bildirimi kapandıysa **1.0** | C karar |
| [ ] | Fiyat ile derinliği eşle: Tavla, Kakuro, Kuyu, Dizgi ve Türkçe kelime oyunları tek başına 1 USD'yi hak eder; Vergici, Toplam Kapma, Yılan ve 2048 paket olmalı | C karar |

> Ücretli katman notu: kaynak GPL-3.0. Satış hukuken mümkün, ama alan herkes
> derleyip dağıtabilir; pratik koruma "ZA" markası, Play'in kolaylığı ve
> güncellemelerdir. Kilit arayüzü tasarlanırken bu gerçek varsayılmalı, DRM
> hayaliyle uğraşılmamalı.

## Oyun testi protokolü (sürüm öncesi, cihazda)

Protokol ve eşikler: [`docs/oyun-testi.md`](../docs/oyun-testi.md). D aşaması
(denge) ve türetilen değişmezler CI'da koşar; **A, B ve C gerçek cihaz ister**
ve otomatikleşmez — emülatörün kare süreleri ve dokunma ölçeği gerçeği
temsil etmez.

- [ ] Sürüm derlemesini gerçek cihaza kur (hata ayıklama derlemesiyle ölçme):
      `./gradlew :app:assembleRelease` → `apksigner sign` → `adb install -r`
- [ ] **A · koşum:** `python3 tools/cihaz_testi.py tarama` — 27 oyun açılıyor,
      oynanıyor, `logcat` temiz
- [ ] **B · kare hızı:** sürekli çizen oyunlarda (Yılan, Kuyu, Geçit, Balkon,
      Viraj, Filo) `python3 tools/cihaz_testi.py kare --sure 15`; kare/s ≥ 58
      ve kaçan vsync ~0. Pencerenin tamamı oynanmalı, yoksa ölçüm kirlidir.
- [ ] **C · giriş:** sürüklemeli oyunlarda `python3 tools/cihaz_testi.py alan`
      ve `surukle`; ölü bölge ve oran beklenen mi
- [ ] **E · erişilebilirlik:** her ekranda `python3 tools/cihaz_testi.py erisim`
      — etiketsiz dokunulabilir öğe olmamalı (kontrast CI'da korunuyor)
- [ ] Sonuçları `docs/oyun-testi.md` içindeki sonuç kütüğüne işle
- [ ] Yeni oyun eklendiyse D aşaması için `*Probe` yazılmış olmalı
      (`./gradlew :games:<oyun>:probe`)
