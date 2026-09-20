# Sürüm geçmişi

Uygulama içindeki sürüm notlarının (`app/src/main/kotlin/com/za/games/platform/Changelog.kt`) depo kopyası. En yeni en üstte.

## 0.41.0 (2026-09-20)
- **Kelime oyunları 14 dilde:** Beş Harf, Kıskaç, Türetme ve Dizgi artık her dilde kendi sözlüğüyle oynanıyor. Önceden İngilizce telefonda arayüz İngilizceydi ama kelimeler Türkçe geliyordu, yani oyun oynanamıyordu
- **Kelime dili seçicisi:** arayüzün dilinden ayrı. Almanya'daki bir oyuncu uygulamayı Almanca kullanıp Beş Harf'i Türkçe oynayabilir; seçim dört oyunun kurulum kartında ve kalıcı. Seçim yoksa arayüzün diline uyulur, o dilin listesi yoksa İngilizceye düşülür
- Her dilin **kendi klavyesi** (QWERTZ, AZERTY, ЙЦУКЕН, Arapça, 29 tuşlu Türkçe), **kendi sözlük sırası** (Almanca'da ä a ile aynı yere, İsveççe'de ä z'den sonra, İspanyolca'da ñ n ile o arasına) ve **kendi günlük bulmacası**
- Dizgi'nin harf puanları ve torba dağılımı her dil için o dilin derleminden türetildi; İngilizce tablo gerçek Scrabble'a çok yakın çıktı (e = 9 taş / 1 puan, q = 1 taş / 9 puan)
- Türkçe listeler birebir korundu: günün kelimesi dizisi kaymadı. Bir yan bulgu düzeltildi — listeler Unicode sırasındaydı, Türk alfabesi sırasında değil; Kıskaç'ın "önce mi sonra mı" ipucu bundan etkileniyordu
- Büyük harfe çevirme oyunun diliyle yapılıyor. Sabit Türkçe yerel ayar Almanca'da "i"yi "İ" yapıyordu; tahtadaki `2H`/`3K` kısaltmaları da koda gömülüydü, artık dile göre (`2L`/`3W`, `2B`/`3W`…)
- APK 2,9 MB'dan 5,7 MB'a çıktı: 14 dilin sözlükleri. Listeler ön-kodlu yazılıyor (her satır önceki kelimeyle paylaşılan ön ekin uzunluğu + kalanı), bu 1,7 MB kazandırıyor
- Yeni denetim `tools/check_wordlists.py` CI'da: dil tablosunun iki kopyası (üretim betiği ve oyun kodu) ayrışırsa, bir kelime alfabe dışı harf içerirse, liste yanlış sırada olursa ya da Dizgi'nin torbası 98 taş olmazsa sürüm çıkmaz

## 0.40.0 (2026-09-19)
- **Uygulama 14 dilde:** Türkçe, İngilizce, Almanca, Fransızca, Hollandaca, İspanyolca, Portekizce, İtalyanca, Danca, İsveççe, Norveççe (bokmål), Fince, Rusça ve Arapça — her dilde 911 metin. Uygulama telefonun diline uyar; ana menüdeki dil düğmesinden de seçilebilir (Android 13 ve üstünde sistem dil seçicisi, altında uygulama kendi ayarını saklar)
- Varsayılan dil İngilizce oldu. Önceden Türkçe `values/` içindeydi, yani Play'de Türkiye dışındaki her telefon uygulamayı Türkçe açacaktı
- Beş Harf, Kıskaç, Türetme ve Dizgi Türkçe kelime listeleriyle oynandığı için metinleri (`strings_words.xml`) çevrilmiyor: telefon Türkçeyse Türkçe, değilse İngilizce. Oyunlar bütün dillerde listede kalır, o dillerde kelime listeleri hazırlanınca çevrilecek
- Sayı biçimleri yerel ayara uyar ama rakamlar Latin kalır (Arapça'da `1.234` yerine `١٢٣٤` çıkmaz); büyük harfe çevirme artık arayüzün diliyle yapılıyor — Türkçe kilitli `uppercase` "Continue" kelimesini "CONTİNUE" yapıyordu
- Play listelemesi 14 dilde (`store/play/<dil>/`); dil kodu → Play yerel ayarı eşlemesi `store/README.md`'de
- İki denetim betiği CI'a bağlandı: `tools/check_strings.py` (dil listeleri tutarlı mı, her dilde bütün metinler var mı, biçim belirteçleri uyuşuyor mu, listede olup çevirisi olmayan dil var mı) ve `tools/check_store.py` (mağaza metinlerinin sınırları, dil kapsamı, görünmez karakterler)

## 0.39.0 (2026-09-19)
- Play yayını için yapı hazırlığı: sürüm iş akışı artık imzalı APK'nın yanında Play'e yüklenecek **AAB** de üretiyor; `targetSdk`/`compileSdk` 36 (Android 16); manifeste oyun kategorisi
- Blok tamamen kendi adıyla: modül `games/blok`, paket `com.za.games.blok`, sınıflar ve metin kimlikleri `blok_*`. Eski kimlikle saklanan rekor ve "son oynananlar" kaydı bir kez taşınır
- Sürüm kapısı bayatlamıyor: motor testleri artık tek `:games:engineTests` göreviyle koşuyor, liste `games/` altından türetiliyor. Önceki hâlinde sürüm iş akışı 26 motorun 18'ini koşuyordu

## 0.38.0 (2026-09-13)
- Viraj kontrolü Filo'nunki gibi: sol/sağ bölge yerine **sürükleme**. Parmağı kaydırmak aracın hedef çizgisini taşır, direksiyon orantılı kırar (uzak hedef tam kilit, yakın hedef az), araç çizgiye varınca düzelir ve parmak kalkınca çizgiyi tutar. Fren: parmağı aşağı çekmek ya da ikinci parmak. Yağda hedef takibi askıya alınır, kayarken yalnız o anki sürükleme ters yöne kırar. Yol fiziği (merkezkaç, tutulabilen azami hız) değişmedi

## 0.37.1 (2026-09-11)
- Cici cihaz bulgusu: kedi konturu 1,4 → 2,5 dp, siyah kedi smokin desenli (açık burun ve göğüs) — koyu uzayda gövdesiyle de görünür

## 0.37.0 (2026-09-11)
- Yeni oyun: **Cici** — Bölüm 1: Uzayda. Beyaz muhabbet kuşu Cici uzayda süzülür, parmakla sürüklenir; kenarlardan gelen ballı yem (7), kuş yemi (5) ve suyu (2) yakalar, uzay kedilerinden ve kenarlardan seken kırmızı toptan kaçar (3 can). Her ikramda sevinir, 3 s içinde art arda yakalamalar seriyi büyütür; 3 s kıpırdamayınca sıkılır ve saniyede 1 puan kaybeder. Zorluk 3 dakikada artar, 2. dakikada ikinci top. Günlük uzay (3 deneme) ve serbest mod

## 0.36.1 (2026-09-11)
- Kuyu cihaz bulgusu: tek parmağın kısa dokunuş eşiği 220 → 130 ms, 150–200 ms'lik yürüme dürtmeleri artık zıplatmıyor; yürüme parmağını yukarı kaydırmak da zıplatır (yürürken de, 160 ms içinde 28 dp)

## 0.36.0 (2026-09-11)
- Kuyu ve Viraj'da kontrol tuşları kalktı, tuval ekranı kaplıyor. Kuyu: parmağını tut, oyuncu parmağının sütununa yürür; ikinci parmak yerdeyken zıplatır, havada basılı tutulunca aşağı ateş eder; tek parmağın kısa dokunuşu da zıplatır. Viraj: sol/sağ yarı direksiyon, orta şerit ya da ikinci parmak fren; ilk saniyelerde bölge ipuçları çizilir
- Filo: gemi yalnız yatay değil ileri geri de sürüklenir (dikey bant ekranın üst üçte birinden alt kenara dek); düşman nişanı ve çarpışmalar geminin gerçek konumunu izler

## 0.35.2 (2026-09-11)
- Çekirge cihaz bulguları: sürüklemede ilk 8 dp artık yutulmuyor; yeşil sıra ve kraliçe koyulaştı, çekirgelere kontur; balya hücrelerine koyu kenar; tükürük kalın koyu halka ve koyu çekirdekle çizilir

## 0.35.1 (2026-09-11)
- Sincap cihaz bulgusu: erişim ipucu ("^") koyu konturla çizilir, gökyüzüne karşı 1,32:1 yerine yüksek kontrast; gök degradesi önbelleklenerek kare başına gölgelendirici kurulumu kaldırıldı

## 0.35.0 (2026-09-11)
- Yeni oyun: **Çekirge** — formasyon ateşi: 7 × 5 çekirge sürüsü yana yürüyüp kenarda iner, seyreldikçe hızlanır; alt sıra tükürük bırakır. Sürükle yürü, dokun fıskırt; tek fıskırtma kuralı: öncekisi hedefe varmadan yenisi atılamaz. Saman balyaları hücre hücre aşınır ve onarılmaz, kraliçe üstten geçer (bonus), sürü çiftçi hizasına inerse istila. Günlük tarla (3 deneme) ve serbest mod

## 0.34.1 (2026-09-11)
- Bostan cihaz bulguları: hücreler dikdörtgen (dar ekranda 38 dp kare yerine tam genişlik × sığan yükseklik), su ve dalga çubuğu tuvalin orman şeridinde; basılı tut – kaydır – bırak yerleştirme (hedef hücre bırakmadan görünür); yakındaki damla kart seçiliyken de önce toplanır; dalga duyuruları koyu şerit üstünde; kart beklerken kalan saniye ve daha koyu örtü

## 0.34.0 (2026-09-10)
- Yeni oyun: **Sincap** — çınarda dikey tırmanış: sola ya da sağa dokun, sincap o yöndeki en yakın üst dala atlar (en çok iki basamak); o yönde dal yoksa düşer. Kuru dal konduktan bir saniye sonra kırılır, yılanlı dala konan ölür, kargalar dalın üstünden geçerken düşürür; kedi gövdeden tırmanır ve yükseldikçe hızlanır. Fındık ve altın fındık puan, her basamak +10. Üretim her basamaktan güvenli bir kaçış garanti eder. Günlük çınar (3 deneme) ve serbest mod

## 0.33.0 (2026-09-10)
- Yeni oyun: **Bostan** — şerit savunması: 5 şerit × 7 hücre tarla; kuyu su üretir, fıskiye jet atar, korkuluk yolu keser, kovan üç şeride arı salar, tuzak kurulunca basanı patlatır. Karga, tavşan, keçi, domuz ve ayı dalgalar hâlinde iner; gökten düşen damlalar dokununca su verir; kulübeye ulaşan can götürür (3 can). Kolay/Orta/Zor (6/8/10 dalga); her seviye uzman politikasıyla oynatılıp kazanılabilir olduğu doğrulanarak üretilir, sonuç kartı uzmanın aynı bostandaki sonucunu gösterir. Günlük bostan (3 deneme) ve serbest mod

## 0.32.0 (2026-09-10)
- Yeni oyun: **Dalgıç** — denizaltıyla dalgıç kurtarma. Denizaltı parmakla sürüklenir, torpidolar baktığı yöne kendiliğinden gider; dalgıçlar altışar toplanıp yüzeyde teslim edilir (tam yük +300), oksijen dalarken azalır yüzeyde dolar, daldıktan sonra dalgıçsız yüzeye çıkmak can götürür. Köpekbalıkları, torpido atan düşman denizaltılar, zincirli mayınlar ve Boğaz akıntı bantları; her teslimde dalga artar. Günlük deniz (3 deneme) ve serbest mod

## 0.31.0 (2026-09-10)
- Yeni oyun: **Uçurtma** — basılı tut yüksel, bırak alçal; çatılar, bacalar, elektrik telleri (alçak, yüksek, çift) ve rakip uçurtmalar arasında sonsuz uçuş. Uçurtma kavgası kuralı: rakibin üstünden geçince ipini kesersin (bonus), altından geçince senin ipin kesilir. Kurdeleler puan; her 100 m kilometre taşı
- Görevler ve ekipman: üç görev aynı anda açık (mesafe, kurdele, tel altı, çatı sıyırma, kesme), tamamlanan yerine sıradaki gelir; 2, 5 ve 9 görevde Kuyruk (yavaş alçalma), Makara (güçlü yükseliş) ve Cam tozu (rakip ipine bağışık, kesme bonusu iki kat) açılır. Günlük gökyüzü (3 deneme) ve serbest mod

## 0.30.0 (2026-09-10)
- Yeni oyun: **Tuşe** — piyano karoları. Dört şerit, her satırda bir karo; sıradaki karonun şeridine dokun, ezginin bir notası çalar, yanlış şerit koşuyu bitirir. Klasik (50 karo, en kısa süre), Sonsuz (vurulan karo başına hızlanan akış, kaçan karo bitirir) ve Günlük (günün parçası ve şeritleri herkese aynı, 3 deneme). Yedi telifsiz parça: Neşeye Övgü, Für Elise, Türk Marşı, Daha Dün Annemizin, Mutlu Yıllar, Sol Majör Menuet, Greensleeves
- Notalar uygulamaya ses dosyası olarak konmadı: kısa piyano tonları cihazda sentezlenip önbelleğe yazılıyor (`NoteSynth`, izin gerekmez); ana menüdeki ses düğmesi notaları da kapatıyor. Ana menü rekoru Sonsuz'daki karo sayısı

## 0.29.0 (2026-09-10)
- Yeni oyun: **Raket** — dikey kortta raket oyunu; vuruş noktası açıyı, raketin hareketi falsoyu verir, her vuruşta top %6 hızlanır, 11 sayıya iki farkla ulaşan kazanır. Üç seviyeli bilgisayar (kolay topu izler, orta düşüşü tahmin eder, zor kenarla rakibin uzağına vurur), aynı telefonda iki kişi (herkes kendi yarısında sürükler, üst oyuncunun sayısı ona dönük) ve duvara karşı ralli (günlük top ya da serbest; her 8 vuruşta raket daralır). Ana menü rekoru en uzun ralli
- Motor `games/raket`: süpürmeli çarpışma (tavan hızda tünelleme yok); seviye botları tepki gecikmesi, hız sınırı ve topla birlikte büyüyen nişan hatasıyla insanı taklit eder — hata büyümeyince iki tahmin eden raket sonsuz ralli yapıyordu (denge ölçümü `:games:raket:probe`)

## 0.28.2 (2026-09-10)
- Reyon Sipariş: hafta sonucu kartında hafta grafiği — her günün kârı çubuk, biriken stok devri çizgi, uzmanın devri kesikli çizgi; grafiğin ekran okuyucu açıklaması gün gün kârı okuyor
- Gün kapanışında rafta kalan stok gün özetine yazılıyor (kayıtla birlikte); 0.28.1 öncesi kaydedilmiş haftalar da yükleniyor, o günlerde devir çizgisi çizilmiyor
- Dokunarak keşif (TalkBack) açıkken alta durum çubuğu yüksekliği kadar pay: SM-A515F'te erişilebilirlik penceresi y = 0'a çakılı geldiğinden alanın son 33 dp'si ağaçtan düşüyordu; en alttaki eylem satırı artık o bandın üstünde

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
