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
