# Sürüm geçmişi

Uygulama içindeki sürüm notlarının (`app/src/main/kotlin/com/za/games/platform/Changelog.kt`) depo kopyası. En yeni en üstte.

## 0.28.2 (2026-09-10)
- Reyon Sipariş: hafta sonucu kartında hafta grafiği — her günün kârı çubuk, biriken stok devri çizgi, uzmanın devri kesikli çizgi; grafiğin ekran okuyucu açıklaması gün gün kârı okuyor
- Gün kapanışında rafta kalan stok gün özetine yazılıyor (kayıtla birlikte); 0.28.1 öncesi kaydedilmiş haftalar da yükleniyor, o günlerde devir çizgisi çizilmiyor

## 0.28.1 (2026-09-10)
- Menü, duraklatma ve bitiş kartları (`OverlayCard`) kabın yüksekliğine sığmazsa içi kayıyor; 360×640'ta Reyon menü kartı ekrandan taşıyor, "Haftaya başla" dokunulamıyordu (cihaz bulgusu)
- Reyon: dört tür çipi dar kartta iki satıra bölünüyor ("Denetim" 360 dp'de "Deneti" diye kırpılıyordu), çip metni sığmazsa üç nokta; dört brif kısaltıldı
- Reyon Sipariş: satır kartının alt payı 10 dp, koli adımlayıcısının 48 dp dokunma alanı artık kırpılmıyor (cihazda 44 dp ölçülmüştü)
- Dokunarak keşif (TalkBack) açıkken sistem çubukları gizlenmiyor: gizli çubuğun bölgesine çizilen düğmelerin erişilebilirlik sınırı sıfırlanıyor, ekran okuyucu oraya inemiyordu; `tools/cihaz_testi.py erisim` sıfır sınırlı düğümleri artık raporluyor

## 0.28.0 (2026-09-10)
- Reyon: Sipariş modu (stok devri). Raf planı sabit; beş–yedi gün boyunca her gün ürün başına kaç koli sipariş edileceğine karar verilir. Talep aralık olarak görünür, gerçekleşen talep tohumdan gelir; siparişler ertesi sabah (Zor'da ağırlar iki gün sonra) gelir, rafa sığmayan iade olur; satış marj kazandırır, akşam stoğu bekleme bedeli öder, raf ömrü dolan fire olur; hafta sonu ve promosyon talebi yükseltir. Hedef, aynı tahminleri gören uzman politikanın kârı; yıldızlar hedefe göre; stok devri ve hizmet düzeyi uzmanla karşılaştırılır; gün kapanış dökümü, ipucu (uzman önerisi), günlük hafta, en iyi kâr kaydı

## 0.27.1 (2026-09-10)
- Reyon: blok adları göze sığdırılıyor: önce tek satır, sığmazsa boşluktan iki satır, sonra hafif yatay daraltma; üç nokta en son çare (tek yüzlü gözde "Bulaşık deterjanı" artık okunuyor)
- Reyon Denetim: plan ve raf tuvalleri yükseklik bütçesinden boyutlanıyor; kısa ekranda raf daralıp sola yaslanmıyor, bulunanlar listesi kaybolmuyor; uzun ekranda bloklar büyüyor; plana dokununca büyütülmüş plan açılıyor; son bulunan sapma listenin başında
- Reyon Satış: katkı rozeti bloğun sağ altına taşındı, adla çakışmıyor

## 0.27.0 (2026-09-10)
- Reyon: Satış modu. Ürünleri serbestçe diz; puan konum (göz hizası, ağırlar alta, yüksek marj), tamamlayıcı komşuluk, çakışma, kategori ve marka bloğu kurallarından gelir; hedef tavlamalı iyileştiricinin bulduğu en iyi diziliş, yıldızlar hedefe göre; canlı puan dökümü, hedef dizilişi görme, günlük ürün seti, en iyi puan kaydı

## 0.26.0 (2026-09-10)
- Reyon: Denetim modu. Üstte plan, altta gerçek raf; yer değişimi, boş göz, yabancı ürün, yanlış marka/boy ve taşma sapmalarına dokunarak bul; süre, hata ve ipucu; günlük ve serbest, üç zorluk

## 0.25.0 (2026-09-09)
- Yeni oyun: Reyon (planogram mantık bulmacası; göz hizası, ağır ürünler alta, kategori ve marka blokları, boy akışı kurallarından tek dizilişi çıkar; üç zorluk, tahminsizlik garantisi, günlük raf)

## 0.24.3 (2026-09-10)
- Tavla Hapis: karşılıklı kilitlenme kademeli ölçütle (pip → hapis geriliği → ev kapıları → çıkmaz anındaki üstünlük) karara bağlanıyor; beraberlik oranı %26'dan sıfıra indi
- Kuyu: RAPID yükseltmesi artık şarjörü de artırıyor (+4); havada kalma süresi kısalmıyor, şarjör başına hasar %50 artıyor

## 0.24.2 (2026-09-09)
- Mayın Tarlası: tahtalar tahminsiz çözülebilir; üretim çözücüyle doğruluyor (kör tahminle kaybetme kalktı)
- Kıskaç: tahmin hakkı 12 → 13; gösterilen kelime listesinde ikili arama en kötü durumda 13 tahmin istiyordu

## 0.24.1 (2026-09-09)
- Filo: gemi parmağı ilk milimetreden izliyor (dokunma toleransı ölü bölgesi kalktı); sürükleme daha az yol istiyor
- Filo: silah 3 artık patronlara karşı da en güçlü seviye (yan mermiler patron menzilinde ıskalıyordu)

## 0.24.0 (2026-09-09)
- Yeni oyun: Filo (dikey uzay savaşı; dalgalar, patronlar, güç artırımları, bomba; günlük filo, üç deneme)

## 0.23.0 (2026-09-09)
- Yeni oyun: Viraj (sözde-3D yarış; rakipler, eşyalar, kontrol noktaları; günlük pist, üç deneme)

## 0.22.0 (2026-09-08)
- Güncellemeden sonra ana menüde Yenilikler kartı; yeni eklenen oyunlarda Yeni rozeti
- Hakkında ekranında sürüm notları
- Arayüz katmanına otomatik testler (Robolectric + Compose; CI ve sürüm iş akışında koşar)
- Play mağaza metinleri, veri güvenliği cevapları ve yayın kontrol listesi (`store/`)

## 0.21.1 (2026-09-08)
- Kaynak kod GPL-3.0 lisansıyla açık; ZA adı ve logosu lisans dışı

## 0.21.0 (2026-09-08)
- Tetris'in adı Blok oldu (Tetris tescilli marka)
- Hakkında ekranı: sürüm, bağlantılar, açık kaynak lisansları
- Gizlilik politikası sayfası; sitede oyunlar gruplandı

## 0.20.2 (2026-09-07)
- Geçit: aynı yönde ardışık nehirlerde geçiş her zaman açık (köprü kütükleri ya da hız farkı)

## 0.20.1 (2026-09-06)
- Paylaşım bağlantısı za.aripd.com

## 0.20.0 (2026-09-06)
- Her oyunun bitiş kartında Paylaş: görsel sonuç kartı ve metin
- Kakuro, Sudoku, Mayın Tarlası, Beş Harf, 2048 ve Dizgi'de kartta bitmiş tahta

## 0.19.1 (2026-09-06)
- Kakuro notları büyük ve okunur; not modu bilgi satırında görünür

## 0.19.0 (2026-09-05)
- Yeni oyunlar: Vergici ve Toplam Kapma

## 0.18.1 (2026-09-05)
- Ana menüde gruplar (Kelime, Bulmaca, Arcade, Masa) ve son oynananlar

## 0.18.0 (2026-09-05)
- Yeni oyun: Kakuro (üç boy, notlar, tek çözüm garantisi)

## 0.17.0 (2026-09-04)
- Yeni oyun: Balkon (kabak çekirdeği, su balonu ya da tükürük; rüzgâr, mega)

## 0.16.0 – 0.16.3 (2026-09-04)
- Yeni oyun: Tavla (Klasik, Tapa, Hapis; bilgisayar ya da iki oyuncu)
- Pul taşıma: sürükle-bırak, bağışlayıcı dokunma, dokununca yalnızca seçim

## 0.15.0 – 0.15.6 (2026-09-03)
- Kuyu: yükseltmeler, dükkân, bekçi ve hazine oyukları
- Geçit: görsel derinlik ve his iyileştirmeleri

## 0.14.0 (2026-09-03)
- Yeni oyun: Geçit (karşıya geçiş; günlük mod, üç deneme)

## 0.13.0 (2026-09-02)
- Yeni oyun: Kuyu (düşüş, zıplama, bot atışı; günlük kuyu)

## 0.12.0 – 0.12.11 (2026-09-01 – 2026-09-02)
- Yeni oyun: Dizgi (elden ele kelime tahtası)
- Sesler, titreşim ve düzeltmeler

## 0.1.0 – 0.11.0 (2026-08-29 – 2026-09-01)
- İlk oyunlar: Blok, 2048, Yılan, Sudoku, Mayın Tarlası, Beş Harf, Kıskaç, Türetme
