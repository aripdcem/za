# Yayın öncesi kontrol listesi (Google Play)

## Hesap ve uygulama
- [ ] Geliştirici hesabı (bireysel) ve ödeme profili; kimlik doğrulaması
- [ ] Uygulama oluştur: ad `ZA Games: Sıfır Reklam`, varsayılan dil Türkçe, tür Oyun, ücretsiz
- [ ] Paket adı `com.za.games`; **Play App Signing** açık (yükleme anahtarı olarak mevcut release anahtarı)
- [ ] Listelemede zorunlu iletişim e-postası (herkese görünür; ayrı bir adres açılması önerilir)
- [ ] Gizlilik politikası URL'si: https://za.aripd.com/gizlilik.html

## Mağaza listesi
- [ ] Başlık, kısa ve tam açıklama: `store/play/tr`, `store/play/en`
- [ ] Uygulama simgesi 512×512 PNG (32 bit)
- [ ] Öne çıkan görsel 1024×500 (PNG/JPG)
- [ ] Telefon ekran görüntüleri: en az 2, en çok 8; 16:9 ya da 9:16; kısa kenar ≥ 320 px, uzun kenar ≤ 3840 px
- [ ] İsteğe bağlı: 7" ve 10" tablet ekran görüntüleri
- [ ] Kategori: Oyunlar > Bulmaca (etiketler: kelime, sudoku, tavla, çevrimdışı)

## Politika formları
- [ ] Veri güvenliği: `store/data-safety.md`
- [ ] Reklam beyanı: reklam yok
- [ ] İçerik derecelendirme anketi (IARC)
- [ ] Hedef kitle ve içerik: 13+
- [ ] Haber uygulaması / COVID / finans / sağlık: hayır

## Test kanalları
- [ ] Dahili test: AAB yükle, ön lansman raporunu (robot testleri) incele, Android Vitals'ı izle
- [ ] Kapalı test: yeni bireysel hesaplarda zorunlu (Console güncel şartı söyler; yaklaşık 12-20 test kullanıcısı, 14 gün)
- [ ] Geri bildirimleri düzelt, sürüm notlarını `store/play/release-notes/` altında tut
- [ ] Üretim: aşamalı yayın (ör. %20 → %100)

## Oyun testi (cihazda, sürüm öncesi)

Protokol ve eşikler: [`docs/oyun-testi.md`](../docs/oyun-testi.md). D aşaması
(denge) ve türetilen değişmezler CI'da koşar; **A, B ve C gerçek cihaz ister**
ve otomatikleşmez — emülatörün kare süreleri ve dokunma ölçeği gerçeği
temsil etmez.

- [ ] Sürüm derlemesini gerçek cihaza kur (hata ayıklama derlemesiyle ölçme):
      `./gradlew :app:assembleRelease` → `apksigner sign` → `adb install -r`
- [ ] **A · koşum:** `python3 tools/cihaz_testi.py tarama` — 18 oyun açılıyor,
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

## Yapı
- [ ] Android App Bundle (AAB) üret: `./gradlew :app:bundleRelease` (release.yml'e AAB çıktısı eklenmeli)
- [ ] `versionCode` her yüklemede artmalı (release.yml etiketten türetir)
