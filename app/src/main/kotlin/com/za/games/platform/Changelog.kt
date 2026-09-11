package com.za.games.platform

import android.content.Context
import android.content.pm.PackageManager
import java.util.Locale

/** Bir sürümün notları; metinler iki dilde tutulur (uzun listeler için kaynak dosyası yerine). */
class ReleaseNote(
    val version: String,
    /** ISO tarih (yyyy-aa-gg). */
    val date: String,
    private val tr: List<String>,
    private val en: List<String>,
) {
    val code: Int get() = Changelog.versionCode(version)

    fun notes(locale: Locale = Locale.getDefault()): List<String> = if (locale.language == "tr") tr else en
}

/**
 * Sürüm geçmişi: en yeni en üstte. Ana menüdeki "Yenilikler" kartı, oyun
 * kartlarındaki "Yeni" rozeti ve Hakkında ekranındaki sürüm notları buradan
 * beslenir. CHANGELOG.md aynı içeriğin depo kopyasıdır.
 */
object Changelog {

    /** build.gradle.kts ile aynı kural: major*10000 + minor*100 + patch. */
    fun versionCode(version: String): Int {
        val parts = version.split('.').map { it.toIntOrNull() ?: 0 }
        return parts.getOrElse(0) { 0 } * 10_000 + parts.getOrElse(1) { 0 } * 100 + parts.getOrElse(2) { 0 }
    }

    /** Yüklü uygulamanın sürüm adı (manifestten); bulunamazsa "0.0.0". */
    fun installedVersion(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
    } catch (e: PackageManager.NameNotFoundException) {
        "0.0.0"
    }

    val entries: List<ReleaseNote> = listOf(
        ReleaseNote(
            "0.37.0", "2026-09-11",
            tr = listOf(
                "Yeni oyun: Cici — Bölüm 1: Uzayda. Beyaz muhabbet kuşu Cici'yi sürükle; ballı yem 7, kuş yemi 5, su 2 puan; uzay kedilerinden ve seken kırmızı toptan kaç (3 can). Her ikramda sevinir, art arda yakalayınca sevinci büyür; 3 saniye kıpırdamazsa sıkılır ve puan kaybeder. Günlük uzay (3 deneme) ve serbest mod",
            ),
            en = listOf(
                "New game: Cici — Chapter 1: In Space. Drag Cici the white budgie; honey sticks 7, seed 5, water 2 points; dodge the space cats and the bouncing red ball (3 lives). Every treat makes her happy and streaks grow the joy; sit still for 3 seconds and she gets bored and loses points. Daily space (3 attempts) and free mode",
            ),
        ),
        ReleaseNote(
            "0.36.1", "2026-09-11",
            tr = listOf(
                "Kuyu: kısa dokunuş eşiği 130 ms'ye indi, kısa yürüme dürtmeleri artık zıplatmıyor; yürüme parmağını yukarı kaydırmak da zıplatır",
            ),
            en = listOf(
                "Kuyu: the quick-tap threshold dropped to 130 ms so short walking nudges no longer jump; flicking the walking finger upward also jumps",
            ),
        ),
        ReleaseNote(
            "0.36.0", "2026-09-11",
            tr = listOf(
                "Kuyu ve Viraj: kontrol tuşları kalktı, tuval ekranı kaplıyor. Kuyu'da parmağını tut, oyuncu o sütuna yürür; ikinci parmak zıplatır, havada basılıyken aşağı ateş eder; kısa dokunuş da zıplatır. Viraj'da sol/sağ yarı direksiyon, orta şerit ya da ikinci parmak fren",
                "Filo: gemi ileri geri de sürüklenir; yukarı çıkmak yaklaştırır, riski artırır",
            ),
            en = listOf(
                "Kuyu and Viraj: the control buttons are gone, the canvas fills the screen. Kuyu: hold a finger and the player walks to that column; a second finger jumps and, held in the air, fires downward; a quick tap also jumps. Viraj: left/right half steers, the middle strip or a second finger brakes",
                "Filo: the ship can now also be dragged up and down; moving up gets you closer, and riskier",
            ),
        ),
        ReleaseNote(
            "0.35.2", "2026-09-11",
            tr = listOf(
                "Çekirge: sürüklemede ilk hareket kaybolmuyor; yeşil çekirgeler, kraliçe, balya hücreleri ve tükürük gökyüzünde daha okunur",
            ),
            en = listOf(
                "Çekirge: the first few dp of a drag are no longer lost; green grasshoppers, the queen, bale cells and spit read better against the sky",
            ),
        ),
        ReleaseNote(
            "0.35.1", "2026-09-11",
            tr = listOf(
                "Sincap: erişim ipucu koyu konturla çizilir, gökyüzünde artık okunur; gök degradesi önbelleklendi",
            ),
            en = listOf(
                "Sincap: the reach hint is drawn with a dark outline and now reads against the sky; the sky gradient is cached",
            ),
        ),
        ReleaseNote(
            "0.35.0", "2026-09-11",
            tr = listOf(
                "Yeni oyun: Çekirge — tarlaya inen çekirge sürüsüne karşı ilaç pompalı çiftçi. Sürükle yürü, dokun fıskırt; tek fıskırtma kuralı. Sürü seyreldikçe hızlanır, saman balyaları aşınır, kraliçe üstten geçer; günlük tarla",
            ),
            en = listOf(
                "New game: Çekirge — a farmer with a sprayer pump against a descending grasshopper swarm. Drag to walk, tap to spray; one-shot rule. The swarm speeds up as it thins, hay bales erode, the queen crosses the top; daily field",
            ),
        ),
        ReleaseNote(
            "0.34.1", "2026-09-11",
            tr = listOf(
                "Bostan: dar ekranda daha büyük hücreler, basılı tutup kaydırarak yerleştirme (hedef hücre bırakmadan görünür), yakındaki damla kart seçiliyken de önce toplanır, dalga duyuruları daha okunur, kart beklerken kalan süre",
            ),
            en = listOf(
                "Bostan: bigger cells on narrow screens, press-and-drag placement (the target cell shows before you release), a nearby drop is collected first even with a card selected, more legible wave announcements, remaining seconds on recharging cards",
            ),
        ),
        ReleaseNote(
            "0.34.0", "2026-09-10",
            tr = listOf(
                "Yeni oyun: Sincap — çınarda dikey tırmanış: sola ya da sağa dokun, üst dala atla. Kuru dallar, yılanlar, kargalar ve peşinde hızlanan kedi; fındık topla, her basamaktan güvenli bir dal erişilir; günlük çınar",
            ),
            en = listOf(
                "New game: Sincap — climb a plane tree: tap left or right to jump to the next branch. Dry branches, snakes, crows and a cat gaining on you; collect nuts, a safe branch is always within reach; daily tree",
            ),
        ),
        ReleaseNote(
            "0.33.0", "2026-09-10",
            tr = listOf(
                "Yeni oyun: Bostan — şerit savunması: kuyu, fıskiye, korkuluk, kovan ve tuzakla bostanı karga, tavşan, keçi, domuz ve ayıdan koru. Damlalara dokun, su biriktir; her seviye kazanılabilir üretilir; üç zorluk, günlük bostan",
            ),
            en = listOf(
                "New game: Bostan — lane defense: guard the garden from crows, rabbits, goats, boars and bears with wells, sprinklers, scarecrows, hives and traps. Tap drops for water; every level is generated winnable; three difficulties, daily garden",
            ),
        ),
        ReleaseNote(
            "0.32.0", "2026-09-10",
            tr = listOf(
                "Yeni oyun: Dalgıç — denizaltıyla dalgıç kurtar: altışar topla, yüzeyde teslim et, oksijene dikkat. Köpekbalıkları, düşman denizaltılar, mayınlar ve Boğaz akıntısı; günlük deniz",
            ),
            en = listOf(
                "New game: Dalgıç — rescue divers by submarine: collect six, surface to deliver, watch your oxygen. Sharks, enemy subs, mines and the Bosphorus current; daily sea",
            ),
        ),
        ReleaseNote(
            "0.31.0", "2026-09-10",
            tr = listOf(
                "Yeni oyun: Uçurtma — basılı tut yüksel, bırak alçal; çatılar, teller ve rakip uçurtmalar arasında sonsuz uçuş. Rakibin üstünden geç, ipini kes; görevleri tamamla, kuyruk, makara ve cam tozunu aç",
            ),
            en = listOf(
                "New game: Uçurtma — hold to climb, release to dive; an endless flight between rooftops, wires and rival kites. Pass above a rival to cut its string; complete missions to unlock the tail, the reel and glass powder",
            ),
        ),
        ReleaseNote(
            "0.30.0", "2026-09-10",
            tr = listOf(
                "Yeni oyun: Tuşe — piyano karoları. Sıradaki karonun şeridine dokun, ezgi parmaklarında çalsın; Klasik (50 karo, en kısa süre), Sonsuz (hızlanan akış) ve Günlük. Yedi telifsiz parça, notalar cihazda sentezleniyor",
            ),
            en = listOf(
                "New game: Tuşe — piano tiles. Tap the lane of the next tile and the melody plays under your fingers; Classic (50 tiles, fastest time), Endless (accelerating flow) and Daily. Seven public-domain pieces, notes synthesized on the device",
            ),
        ),
        ReleaseNote(
            "0.29.0", "2026-09-10",
            tr = listOf(
                "Yeni oyun: Raket — raketi sürükle; vuruş noktası açıyı, hareketin falsoyu verir, her vuruşta top hızlanır. Üç seviyeli bilgisayar, aynı telefonda iki kişi ya da duvara karşı ralli (günlük top)",
            ),
            en = listOf(
                "New game: Raket — drag the paddle; where the ball hits sets the angle, your motion adds spin, every hit speeds the ball up. Three computer levels, two players on one phone, or a rally against the wall (daily ball)",
            ),
        ),
        ReleaseNote(
            "0.28.2", "2026-09-10",
            tr = listOf(
                "Reyon Sipariş: hafta sonunda kâr ve stok devri grafiği — hangi gün ne kazandırdı, devir uzmanın nerede kaldı",
                "Ekran okuyucu açıkken en alttaki düğmeler erişilebilirlik ağacının dışında kalmıyor (alta durum çubuğu kadar pay)",
            ),
            en = listOf(
                "Reyon Ordering: a profit and stock-turnover chart at the end of the week — which day earned what, and where turnover sits against the expert",
                "With a screen reader on, the bottom buttons no longer fall outside the accessibility tree (extra bottom inset the height of the status bar)",
            ),
        ),
        ReleaseNote(
            "0.28.1", "2026-09-10",
            tr = listOf(
                "Menü ve bitiş kartları kısa ekranda kayıyor; 360 dp'de Reyon menüsünün düğmeleri ekran dışında kalıyordu",
                "Reyon: tür çipleri dar ekranda iki satır, brifler kısaldı; Sipariş adımlayıcısının dokunma alanı 48 dp",
                "Ekran okuyucu (TalkBack) açıkken sistem çubukları gizlenmez; alt düğmeler dokunarak keşifte erişilebilir",
            ),
            en = listOf(
                "Menu and end cards scroll on short screens; at 360 dp the Reyon menu buttons were off screen",
                "Reyon: kind chips wrap to two rows on narrow screens, shorter briefs; the Ordering stepper gets a 48 dp touch target",
                "System bars stay visible while a screen reader (TalkBack) is on, so bottom buttons are reachable by touch exploration",
            ),
        ),
        ReleaseNote(
            "0.28.0", "2026-09-10",
            tr = listOf("Reyon: Sipariş modu (bir haftalık stok yönetimi: talep tahmini, koli siparişi, teslim süresi, raf kapasitesi, raf ömrü ve promosyonlar; uzmanın kârına göre yıldız, stok devri)"),
            en = listOf("Reyon: Ordering mode (a week of stock management: demand forecasts, case orders, lead times, shelf capacity, shelf life and promotions; stars against the expert's profit, stock turnover)"),
        ),
        ReleaseNote(
            "0.27.1", "2026-09-10",
            tr = listOf(
                "Reyon: blok adları göze sığdırılıyor (gerekirse iki satır ya da hafif daraltma; kırpma en son çare)",
                "Reyon Denetim: plan ve raf kısa ekranda da aynı genişlikte ve ekranın içinde; plana dokununca büyür; son bulunan sapma listenin başında",
            ),
            en = listOf(
                "Reyon: product names now fit their slot (two lines or a slight squeeze when needed; truncation is the last resort)",
                "Reyon Audit: plan and shelf keep the same width and stay on screen on short phones; tap the plan to enlarge it; the latest find tops the list",
            ),
        ),
        ReleaseNote(
            "0.27.0", "2026-09-10",
            tr = listOf("Reyon: Satış modu (ürünleri satış kurallarına göre diz, iyileştiricinin hedefine göre yıldız al; canlı puan dökümü, hedef diziliş, günlük ürün seti)"),
            en = listOf("Reyon: Sales mode (arrange products by the sales rules and earn stars against the optimizer's target; live score breakdown, target layout, daily product set)"),
        ),
        ReleaseNote(
            "0.26.0", "2026-09-10",
            tr = listOf("Reyon: Denetim modu (plan ile gerçek rafı karşılaştır, sapmaları bul; altı sapma türü, günlük raf)"),
            en = listOf("Reyon: Audit mode (compare the plan with the real shelf and spot the deviations; six deviation types, daily shelf)"),
        ),
        ReleaseNote(
            "0.25.0", "2026-09-09",
            tr = listOf("Yeni oyun: Reyon (planogram mantık bulmacası; üç zorluk, tek çözüm ve tahminsizlik garantisi; günlük raf)"),
            en = listOf("New game: Reyon (planogram logic puzzle; three levels, unique solution and no-guessing guarantee; daily shelf)"),
        ),
        ReleaseNote(
            "0.24.3", "2026-09-10",
            tr = listOf(
                "Tavla Hapis: karşılıklı kilitlenme artık berabere değil, hapis savaşını kazanan lehine biter",
                "Kuyu: RAPID yükseltmesi şarjörü de artırıyor; havada kalma süresi kısalmıyor",
            ),
            en = listOf(
                "Tavla Hapis: a mutual lock is now decided by the pinning battle instead of ending in a draw",
                "Kuyu: the RAPID upgrade now also grants ammo, so hover time no longer shrinks",
            ),
        ),
        ReleaseNote(
            "0.24.2", "2026-09-09",
            tr = listOf(
                "Mayın Tarlası: tahtalar artık tahmin gerektirmeden çözülebiliyor",
                "Kıskaç: tahmin hakkı 13 (ikili arama her kelimeye yetiyor)",
            ),
            en = listOf(
                "Minesweeper: boards can now be solved without guessing",
                "Kıskaç: 13 guesses, enough for binary search to reach every word",
            ),
        ),
        ReleaseNote(
            "0.24.1", "2026-09-09",
            tr = listOf(
                "Filo: gemi parmağı ilk milimetreden izliyor; sürükleme daha az yol istiyor",
                "Filo: silah 3 artık patronlara karşı da en güçlü seviye",
            ),
            en = listOf(
                "Filo: the ship follows your finger from the first millimetre; dragging needs less travel",
                "Filo: weapon 3 is now the strongest level against bosses too",
            ),
        ),
        ReleaseNote(
            "0.24.0", "2026-09-09",
            tr = listOf("Yeni oyun: Filo (dikey uzay savaşı; dalgalar, patronlar, güç artırımları; günlük filo, üç deneme)"),
            en = listOf("New game: Filo (vertical space shooter; waves, bosses, power-ups; daily fleet, three attempts)"),
        ),
        ReleaseNote(
            "0.23.0", "2026-09-09",
            tr = listOf("Yeni oyun: Viraj (sözde-3D yarış; günlük pist, üç deneme)"),
            en = listOf("New game: Viraj (pseudo-3D racing; daily track, three attempts)"),
        ),
        ReleaseNote(
            "0.22.0", "2026-09-08",
            tr = listOf(
                "Güncellemeden sonra ana menüde Yenilikler kartı; yeni eklenen oyunlarda Yeni rozeti",
                "Hakkında ekranında sürüm notları",
                "Arayüz katmanına otomatik testler (her sürümde CI'da koşar)",
            ),
            en = listOf(
                "What's-new card on the hub after an update; New badge on recently added games",
                "Release notes in the About screen",
                "Automated UI tests (run on CI for every release)",
            ),
        ),
        ReleaseNote(
            "0.21.1", "2026-09-08",
            tr = listOf("Kaynak kod GPL-3.0 lisansıyla açık; ZA adı ve logosu lisans dışı"),
            en = listOf("Source code licensed under GPL-3.0; the ZA name and logo are not covered"),
        ),
        ReleaseNote(
            "0.21.0", "2026-09-08",
            tr = listOf(
                "Tetris'in adı Blok oldu (Tetris tescilli marka)",
                "Hakkında ekranı: sürüm, bağlantılar, açık kaynak lisansları",
                "Gizlilik politikası sayfası; sitede oyunlar gruplandı",
            ),
            en = listOf(
                "Tetris renamed to Blok (Tetris is a registered trademark)",
                "About screen: version, links, open source licenses",
                "Privacy policy page; games grouped on the website",
            ),
        ),
        ReleaseNote(
            "0.20.2", "2026-09-07",
            tr = listOf("Geçit: aynı yönde ardışık nehirlerde geçiş her zaman açık (köprü kütükleri ya da hız farkı)"),
            en = listOf("Geçit: consecutive same-direction rivers are always crossable (bridge logs or a speed gap)"),
        ),
        ReleaseNote(
            "0.20.0", "2026-09-06",
            tr = listOf(
                "Her oyunun bitiş kartında Paylaş: görsel sonuç kartı ve metin",
                "Kakuro, Sudoku, Mayın Tarlası, Beş Harf, 2048 ve Dizgi'de kartta bitmiş tahta",
                "Paylaşım bağlantısı za.aripd.com (0.20.1)",
            ),
            en = listOf(
                "Share button on every end-of-game card: result image and text",
                "Kakuro, Sudoku, Minesweeper, Beş Harf, 2048 and Dizgi include the finished board",
                "Share link points to za.aripd.com (0.20.1)",
            ),
        ),
        ReleaseNote(
            "0.19.1", "2026-09-06",
            tr = listOf("Kakuro notları büyük ve okunur; not modu bilgi satırında görünür"),
            en = listOf("Kakuro notes are larger and readable; notes mode shown in the info line"),
        ),
        ReleaseNote(
            "0.19.0", "2026-09-05",
            tr = listOf("Yeni oyunlar: Vergici ve Toplam Kapma"),
            en = listOf("New games: Vergici (Taxman) and Toplam Kapma (Number Scrabble)"),
        ),
        ReleaseNote(
            "0.18.1", "2026-09-05",
            tr = listOf("Ana menüde gruplar (Kelime, Bulmaca, Arcade, Masa) ve son oynananlar"),
            en = listOf("Hub groups (Word, Puzzle, Arcade, Board) and recently played"),
        ),
        ReleaseNote(
            "0.18.0", "2026-09-05",
            tr = listOf("Yeni oyun: Kakuro (üç boy, notlar, tek çözüm garantisi)"),
            en = listOf("New game: Kakuro (three sizes, notes, unique-solution guarantee)"),
        ),
        ReleaseNote(
            "0.17.0", "2026-09-04",
            tr = listOf("Yeni oyun: Balkon (kabak çekirdeği, su balonu ya da tükürük; rüzgâr, mega)"),
            en = listOf("New game: Balkon (pumpkin seeds, water balloons or spit; wind, mega shots)"),
        ),
        ReleaseNote(
            "0.16.3", "2026-09-04",
            tr = listOf(
                "Yeni oyun: Tavla (Klasik, Tapa, Hapis; bilgisayar ya da iki oyuncu) (0.16.0)",
                "Pul taşıma: sürükle-bırak, bağışlayıcı dokunma, dokununca yalnızca seçim (0.16.1–0.16.3)",
            ),
            en = listOf(
                "New game: Tavla (Classic, Tapa, Hapis; computer or two players) (0.16.0)",
                "Checker moves: drag and drop, forgiving taps, tap only selects (0.16.1–0.16.3)",
            ),
        ),
        ReleaseNote(
            "0.15.6", "2026-09-03",
            tr = listOf("Kuyu: yükseltmeler, dükkân, bekçi ve hazine oyukları (0.15.0)", "Geçit: görsel derinlik ve his iyileştirmeleri"),
            en = listOf("Kuyu: upgrades, shop, guard and treasure nooks (0.15.0)", "Geçit: visual depth and feel improvements"),
        ),
        ReleaseNote(
            "0.14.0", "2026-09-03",
            tr = listOf("Yeni oyun: Geçit (karşıya geçiş; günlük mod, üç deneme)"),
            en = listOf("New game: Geçit (road crossing; daily mode, three attempts)"),
        ),
        ReleaseNote(
            "0.13.0", "2026-09-02",
            tr = listOf("Yeni oyun: Kuyu (düşüş, zıplama, bot atışı; günlük kuyu)"),
            en = listOf("New game: Kuyu (descend, jump, boot shots; daily well)"),
        ),
        ReleaseNote(
            "0.12.11", "2026-09-02",
            tr = listOf("Yeni oyun: Dizgi (elden ele kelime tahtası) (0.12.0)", "Sesler, titreşim ve düzeltmeler"),
            en = listOf("New game: Dizgi (pass-and-play word board) (0.12.0)", "Sounds, haptics and fixes"),
        ),
        ReleaseNote(
            "0.11.0", "2026-09-01",
            tr = listOf("İlk oyunlar: Blok, 2048, Yılan, Sudoku, Mayın Tarlası, Beş Harf, Kıskaç, Türetme (0.1.0–0.11.0)"),
            en = listOf("First games: Blok, 2048, Snake, Sudoku, Minesweeper, Beş Harf, Kıskaç, Türetme (0.1.0–0.11.0)"),
        ),
    )

    val latest: ReleaseNote get() = entries.first()
}
