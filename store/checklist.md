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

## Yapı
- [ ] Android App Bundle (AAB) üret: `./gradlew :app:bundleRelease` (release.yml'e AAB çıktısı eklenmeli)
- [ ] `versionCode` her yüklemede artmalı (release.yml etiketten türetir)
