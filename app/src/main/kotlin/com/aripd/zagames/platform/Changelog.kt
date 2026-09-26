package com.aripd.zagames.platform

import android.content.Context
import android.content.pm.PackageManager
import java.util.Locale

/**
 * Bir sürümün notları. Metinler kaynak dosyası yerine burada tutulur: listeler uzun,
 * her satır bir sürüme ait ve yalnız bu ekranlarda görünüyor.
 *
 * Türkçe ve İngilizce her sürüm için yazılır. [others] isteğe bağlıdır; bir dil orada
 * yoksa not İngilizce görünür — arayüzün geri kalanı o dilde olsa bile. Ana menüdeki
 * "Yenilikler" kartı yalnız en yeni sürümü gösterdiği için, kullanıcının ilk gördüğü
 * notu çevirmeye değer; Hakkında ekranındaki geçmişte eski sürümler İngilizce kalır.
 */
class ReleaseNote(
    val version: String,
    /** ISO tarih (yyyy-aa-gg). */
    val date: String,
    private val tr: List<String>,
    private val en: List<String>,
    /** Dil etiketi (`de`, `pt`…) → o dildeki satırlar. Eksik dil İngilizceye düşer. */
    private val others: Map<String, List<String>> = emptyMap(),
) {
    val code: Int get() = Changelog.versionCode(version)

    fun notes(locale: Locale = Locale.getDefault()): List<String> = when {
        locale.language == "tr" -> tr
        else -> ZaLocale.normalize(locale)?.let { others[it] } ?: en
    }
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
            "0.44.0", "2026-09-26",
            tr = listOf(
                "Reyon artık ayrı bir uygulama: Google Play'de Reyon adıyla, ücretsiz ve yine reklamsız; ZA Games'ten bu sürümle çıktı",
                "Reyon'daki ilerleme yeni uygulamaya taşınmıyor: Android iki uygulamanın verisini birbirinden ayrı tutuyor",
            ),
            en = listOf(
                "Reyon is now its own app: find it on Google Play as Reyon, free and still ad-free; it leaves ZA Games with this version",
                "Progress in Reyon does not carry over to the new app: Android keeps each app's data separate",
            ),
            others = mapOf(
                "de" to listOf(
                    "Reyon ist jetzt eine eigene App: bei Google Play unter dem Namen Reyon, kostenlos und weiterhin werbefrei; mit dieser Version verlässt sie ZA Games",
                    "Der Fortschritt in Reyon wird nicht in die neue App übernommen: Android hält die Daten jeder App getrennt",
                ),
                "fr" to listOf(
                    "Reyon devient une appli à part : sur Google Play sous le nom Reyon, gratuite et toujours sans publicité ; elle quitte ZA Games avec cette version",
                    "La progression dans Reyon ne passe pas dans la nouvelle appli : Android garde les données de chaque appli séparées",
                ),
                "nl" to listOf(
                    "Reyon is nu een eigen app: op Google Play onder de naam Reyon, gratis en nog steeds zonder advertenties; met deze versie verdwijnt hij uit ZA Games",
                    "Voortgang in Reyon gaat niet mee naar de nieuwe app: Android houdt de gegevens van elke app gescheiden",
                ),
                "es" to listOf(
                    "Reyon es ahora una app propia: en Google Play con el nombre Reyon, gratis y sigue sin anuncios; con esta versión sale de ZA Games",
                    "El progreso de Reyon no pasa a la nueva app: Android mantiene separados los datos de cada app",
                ),
                "pt" to listOf(
                    "O Reyon agora é um app próprio: no Google Play com o nome Reyon, grátis e ainda sem anúncios; com esta versão ele sai do ZA Games",
                    "O progresso no Reyon não passa para o novo app: o Android mantém os dados de cada app separados",
                ),
                "it" to listOf(
                    "Reyon ora è un'app a sé: su Google Play con il nome Reyon, gratuita e sempre senza pubblicità; con questa versione lascia ZA Games",
                    "I progressi in Reyon non passano alla nuova app: Android tiene separati i dati di ogni app",
                ),
                "da" to listOf(
                    "Reyon er nu sin egen app: på Google Play under navnet Reyon, gratis og stadig uden reklamer; med denne version forlader den ZA Games",
                    "Fremskridt i Reyon følger ikke med til den nye app: Android holder hver apps data adskilt",
                ),
                "sv" to listOf(
                    "Reyon är nu en egen app: på Google Play under namnet Reyon, gratis och fortfarande reklamfri; med den här versionen lämnar den ZA Games",
                    "Framsteg i Reyon följer inte med till den nya appen: Android håller varje apps data åtskilda",
                ),
                "nb" to listOf(
                    "Reyon er nå en egen app: på Google Play under navnet Reyon, gratis og fortsatt reklamefri; med denne versjonen forlater den ZA Games",
                    "Fremgang i Reyon blir ikke med over til den nye appen: Android holder dataene til hver app atskilt",
                ),
                "fi" to listOf(
                    "Reyon on nyt oma sovelluksensa: Google Playssa nimellä Reyon, ilmainen ja edelleen mainokseton; tämän version myötä se poistuu ZA Gamesista",
                    "Reyonin edistyminen ei siirry uuteen sovellukseen: Android pitää jokaisen sovelluksen tiedot erillään",
                ),
                "ru" to listOf(
                    "Reyon теперь отдельное приложение: в Google Play под названием Reyon, бесплатно и по-прежнему без рекламы; с этой версией он уходит из ZA Games",
                    "Прогресс в Reyon не переносится в новое приложение: Android хранит данные каждого приложения отдельно",
                ),
                "ar" to listOf(
                    "أصبح Reyon تطبيقاً مستقلاً: على Google Play باسم Reyon، مجاني ولا يزال بلا إعلانات؛ ويغادر ZA Games مع هذا الإصدار",
                    "لا ينتقل التقدّم في Reyon إلى التطبيق الجديد: يبقي أندرويد بيانات كل تطبيق منفصلة",
                ),
            ),
        ),
        ReleaseNote(
            "0.43.5", "2026-09-21",
            tr = listOf(
                "Reyon · Satış: uzun telefonda beş puan kuralının beşi de görünüyor — panelin payı artık tepsinin kaç sıra tuttuğuna değil kuralların kendi boyuna bağlı",
                "Dar ekranda üçüncü kural her dilde okunuyor: kural açıklamaları orada tek satıra iniyor, satıra dokununca tam açılıyor",
                "Bütün ürünler rafa konunca panel boşalan yeri kullanıyor; puan kurallarını okumak için durulan an tam orası",
            ),
            en = listOf(
                "Reyon · Sales: all five scoring rules are visible on a tall phone — the panel's share now follows the rules' own height, not how many rows the tray takes",
                "On a narrow screen the third rule is readable in every language: rule descriptions drop to one line there, and tapping a row opens it in full",
                "When every product is on the shelf the panel uses the freed space — that is exactly when you stop to read the scoring rules",
            ),
            others = mapOf(
                "de" to listOf(
                    "Reyon · Verkauf: Auf einem hohen Display sind alle fünf Punkteregeln sichtbar — der Anteil des Panels richtet sich jetzt nach der Höhe der Regeln, nicht nach den Reihen im Tablett",
                    "Auf schmalen Bildschirmen ist die dritte Regel in jeder Sprache lesbar: Regelbeschreibungen stehen dort in einer Zeile, ein Tippen öffnet sie vollständig",
                    "Sobald alle Produkte im Regal stehen, nutzt das Panel den frei gewordenen Platz — genau dann liest man die Punkteregeln",
                ),
                "fr" to listOf(
                    "Reyon · Ventes : les cinq règles de score sont visibles sur un écran haut — la part du panneau suit désormais la hauteur des règles, pas le nombre de rangées du plateau",
                    "Sur un écran étroit, la troisième règle est lisible dans toutes les langues : les descriptions tiennent sur une ligne et un appui les ouvre entièrement",
                    "Quand tous les produits sont en rayon, le panneau occupe la place libérée — c'est justement le moment où l'on lit les règles",
                ),
                "nl" to listOf(
                    "Reyon · Verkoop: op een hoog scherm zijn alle vijf scoreregels zichtbaar — het aandeel van het paneel volgt nu de hoogte van de regels, niet het aantal rijen in de lade",
                    "Op een smal scherm is de derde regel in elke taal leesbaar: regelbeschrijvingen staan daar op één regel en een tik opent ze volledig",
                    "Zodra alle producten in het schap staan, gebruikt het paneel de vrijgekomen ruimte — juist dan lees je de scoreregels",
                ),
                "es" to listOf(
                    "Reyon · Ventas: en una pantalla alta se ven las cinco reglas de puntuación; la parte del panel sigue ahora la altura de las reglas y no las filas de la bandeja",
                    "En pantallas estrechas la tercera regla se lee en todos los idiomas: allí las descripciones ocupan una línea y al tocar la fila se abren completas",
                    "Cuando todos los productos están en el estante, el panel usa el espacio liberado: justo el momento en que se leen las reglas",
                ),
                "pt" to listOf(
                    "Reyon · Vendas: em uma tela alta as cinco regras de pontuação aparecem; a parte do painel agora acompanha a altura das regras, não as linhas da bandeja",
                    "Em telas estreitas a terceira regra é legível em todos os idiomas: ali as descrições ficam em uma linha e tocar na linha abre o texto completo",
                    "Quando todos os produtos estão na prateleira, o painel usa o espaço livre — é exatamente quando se lê as regras",
                ),
                "it" to listOf(
                    "Reyon · Vendite: su uno schermo alto si vedono tutte e cinque le regole di punteggio; la quota del pannello segue ora l'altezza delle regole, non le righe del vassoio",
                    "Su schermi stretti la terza regola è leggibile in ogni lingua: lì le descrizioni stanno su una riga e toccando la riga si aprono per intero",
                    "Quando tutti i prodotti sono a scaffale il pannello usa lo spazio liberato: è proprio il momento in cui si leggono le regole",
                ),
                "da" to listOf(
                    "Reyon · Salg: på en høj skærm ses alle fem pointregler — panelets andel følger nu reglernes egen højde, ikke antallet af rækker i bakken",
                    "På en smal skærm kan den tredje regel læses på alle sprog: beskrivelserne fylder én linje, og et tryk åbner dem helt",
                    "Når alle varer står på hylden, bruger panelet den frigjorte plads — præcis når man læser pointreglerne",
                ),
                "sv" to listOf(
                    "Reyon · Försäljning: på en hög skärm syns alla fem poängregler — panelens andel följer nu reglernas egen höjd, inte hur många rader brickan tar",
                    "På en smal skärm går den tredje regeln att läsa på alla språk: beskrivningarna ryms på en rad och ett tryck öppnar dem helt",
                    "När alla varor står i hyllan använder panelen det frigjorda utrymmet — precis då läser man poängreglerna",
                ),
                "nb" to listOf(
                    "Reyon · Salg: på en høy skjerm vises alle fem poengregler — panelets andel følger nå reglenes egen høyde, ikke hvor mange rader brettet tar",
                    "På en smal skjerm er den tredje regelen lesbar på alle språk: beskrivelsene står på én linje, og et trykk åpner dem helt",
                    "Når alle varene står i hyllen, bruker panelet den frigjorte plassen — nettopp da leser man poengreglene",
                ),
                "fi" to listOf(
                    "Reyon · Myynti: korkealla näytöllä kaikki viisi pisteytyssääntöä näkyvät — paneelin osuus seuraa nyt sääntöjen omaa korkeutta, ei tarjottimen rivimäärää",
                    "Kapealla näytöllä kolmas sääntö on luettavissa kaikilla kielillä: kuvaukset mahtuvat siellä yhdelle riville ja riviä napauttamalla ne avautuvat kokonaan",
                    "Kun kaikki tuotteet ovat hyllyssä, paneeli käyttää vapautuneen tilan — juuri silloin sääntöjä luetaan",
                ),
                "ru" to listOf(
                    "Reyon · Продажи: на высоком экране видны все пять правил начисления — доля панели теперь зависит от высоты самих правил, а не от числа рядов в лотке",
                    "На узком экране третье правило читается на всех языках: описания занимают там одну строку, а нажатие на строку раскрывает текст полностью",
                    "Когда все товары на полке, панель занимает освободившееся место — именно тогда читают правила",
                ),
                "ar" to listOf(
                    "Reyon · البيع: على الشاشة الطويلة تظهر قواعد النقاط الخمس كلها؛ صار نصيب اللوحة يتبع ارتفاع القواعد نفسها لا عدد صفوف الصينية",
                    "في الشاشة الضيقة تُقرأ القاعدة الثالثة بكل اللغات: الشروح هناك في سطر واحد، ولمس الصف يفتحها كاملة",
                    "حين تستقر كل المنتجات على الرف تستفيد اللوحة من المساحة المتحررة، وهي لحظة قراءة القواعد",
                ),
            ),
        ),
        ReleaseNote(
            "0.43.4", "2026-09-20",
            tr = listOf(
                "Reyon · Satış: dar ekranda kırpılan puan kuralı açıklaması, satıra dokununca tam açılıyor; kırpılan satırın sağında küçük bir ok duruyor",
            ),
            en = listOf(
                "Reyon · Sales: a scoring rule whose description is cut off on a narrow screen now opens in full when you tap the row; a small arrow marks the rows that are cut off",
            ),
            others = mapOf(
                "de" to listOf(
                    "Reyon · Verkauf: Eine Punkteregel, deren Beschreibung auf schmalen Bildschirmen abgeschnitten wird, öffnet sich beim Tippen auf die Zeile vollständig; ein kleiner Pfeil markiert die abgeschnittenen Zeilen",
                ),
                "fr" to listOf(
                    "Reyon · Ventes : une règle de score dont la description est tronquée sur un écran étroit s'ouvre entièrement en touchant la ligne ; une petite flèche signale les lignes tronquées",
                ),
                "nl" to listOf(
                    "Reyon · Verkoop: een scoreregel waarvan de beschrijving op een smal scherm wordt afgekapt, opent volledig als je op de rij tikt; een klein pijltje markeert de afgekapte rijen",
                ),
                "es" to listOf(
                    "Reyon · Ventas: una regla de puntuación cuya descripción se corta en pantallas estrechas se abre completa al tocar la fila; una pequeña flecha señala las filas cortadas",
                ),
                "pt" to listOf(
                    "Reyon · Vendas: uma regra de pontuação cuja descrição fica cortada em telas estreitas abre por completo ao tocar na linha; uma pequena seta marca as linhas cortadas",
                ),
                "it" to listOf(
                    "Reyon · Vendite: una regola di punteggio con la descrizione troncata su schermi stretti si apre per intero toccando la riga; una piccola freccia indica le righe troncate",
                ),
                "da" to listOf(
                    "Reyon · Salg: en pointregel, hvis beskrivelse bliver afkortet på en smal skærm, åbnes helt, når du trykker på rækken; en lille pil markerer de afkortede rækker",
                ),
                "sv" to listOf(
                    "Reyon · Försäljning: en poängregel vars beskrivning kapas på en smal skärm öppnas i sin helhet när du trycker på raden; en liten pil markerar de kapade raderna",
                ),
                "nb" to listOf(
                    "Reyon · Salg: en poengregel med beskrivelse som blir avkortet på en smal skjerm, åpnes i sin helhet når du trykker på raden; en liten pil markerer de avkortede radene",
                ),
                "fi" to listOf(
                    "Reyon · Myynti: pisteytyssääntö, jonka kuvaus katkeaa kapealla näytöllä, avautuu kokonaan riviä napauttamalla; pieni nuoli merkitsee katkaistut rivit",
                ),
                "ru" to listOf(
                    "Reyon · Продажи: правило начисления, описание которого обрезано на узком экране, раскрывается полностью по нажатию на строку; обрезанные строки отмечены маленькой стрелкой",
                ),
                "ar" to listOf(
                    "Reyon · البيع: قاعدة النقاط التي يُقتطع شرحها في الشاشة الضيقة تنفتح كاملة عند لمس الصف؛ وسهم صغير يشير إلى الصفوف المقتطعة",
                ),
            ),
        ),
        ReleaseNote(
            "0.43.3", "2026-09-20",
            tr = listOf(
                "Reyon · Satış: dar ekranda daha çok puan kuralı görünsün diye kural açıklamaları iki satırla sınırlandı, satır araları daraldı",
            ),
            en = listOf(
                "Reyon · Sales: rule descriptions are capped at two lines and the rows sit tighter, so more scoring rules fit on a narrow screen",
            ),
            others = mapOf(
                "de" to listOf(
                    "Reyon · Verkauf: Regelbeschreibungen sind auf zwei Zeilen begrenzt und die Zeilen stehen enger, damit auf schmalen Bildschirmen mehr Punkteregeln passen",
                ),
                "fr" to listOf(
                    "Reyon · Ventes : les descriptions des règles tiennent en deux lignes et les lignes sont plus serrées, pour afficher plus de règles de score sur un écran étroit",
                ),
                "nl" to listOf(
                    "Reyon · Verkoop: regelbeschrijvingen zijn beperkt tot twee regels en de rijen staan dichter op elkaar, zodat er op een smal scherm meer scoreregels passen",
                ),
                "es" to listOf(
                    "Reyon · Ventas: las descripciones de las reglas se limitan a dos líneas y las filas están más juntas, para que quepan más reglas de puntuación en pantallas estrechas",
                ),
                "pt" to listOf(
                    "Reyon · Vendas: as descrições das regras ficam em duas linhas e as linhas estão mais juntas, para caber mais regras de pontuação em telas estreitas",
                ),
                "it" to listOf(
                    "Reyon · Vendite: le descrizioni delle regole sono limitate a due righe e le righe sono più compatte, così su schermi stretti entrano più regole di punteggio",
                ),
                "da" to listOf(
                    "Reyon · Salg: regelbeskrivelser er begrænset til to linjer, og rækkerne sidder tættere, så der er plads til flere pointregler på en smal skærm",
                ),
                "sv" to listOf(
                    "Reyon · Försäljning: regelbeskrivningarna begränsas till två rader och raderna sitter tätare, så att fler poängregler får plats på en smal skärm",
                ),
                "nb" to listOf(
                    "Reyon · Salg: regelbeskrivelsene er begrenset til to linjer og radene sitter tettere, slik at flere poengregler får plass på en smal skjerm",
                ),
                "fi" to listOf(
                    "Reyon · Myynti: sääntöjen kuvaukset rajattiin kahteen riviin ja rivit ovat tiiviimmin, jotta kapealle näytölle mahtuu enemmän pisteytyssääntöjä",
                ),
                "ru" to listOf(
                    "Reyon · Продажи: описания правил ограничены двумя строками, а строки стали плотнее, чтобы на узком экране помещалось больше правил начисления",
                ),
                "ar" to listOf(
                    "Reyon · البيع: صارت شروح القواعد في سطرين على الأكثر وتقاربت الصفوف، ليظهر في الشاشة الضيقة عدد أكبر من قواعد النقاط",
                ),
            ),
        ),
        ReleaseNote(
            "0.43.2", "2026-09-20",
            tr = listOf(
                "Reyon kısa telefonlarda oynanabilir oldu: 360×640 dp ekranda raf tuvali bütün yüksekliği yutuyor, Diziliş'in planogram brifi hiç çizilmiyordu — brif olmadan bulmaca çözülemez",
                "Aynı tavan Satış'ın puan kurallarına ve Sipariş'in listesine de kondu; sığmayan tepsi kendi içinde kayıyor",
            ),
            en = listOf(
                "Reyon is playable on short phones again: on a 360×640 dp screen the shelf canvas ate the whole height and Arrange's planogram brief was never drawn — the puzzle cannot be solved without it",
                "The same ceiling now applies to the scoring rules in Sales and the list in Ordering; a tray that no longer fits scrolls inside itself",
            ),
            others = mapOf(
                "de" to listOf(
                    "Reyon ist auf kurzen Telefonen wieder spielbar: auf einem Bildschirm mit 360×640 dp nahm das Regal die ganze Höhe ein, und das Planogramm-Briefing von Bestücken wurde nie gezeichnet — ohne das Briefing ist das Rätsel nicht lösbar",
                    "Die gleiche Obergrenze gilt jetzt für die Punkteregeln von Verkauf und die Liste von Bestellen; ein Tablett, das nicht mehr passt, scrollt in sich",
                ),
                "fr" to listOf(
                    "Reyon est de nouveau jouable sur les téléphones courts : sur un écran de 360×640 dp le rayon prenait toute la hauteur et le briefing planogramme de Ranger n'était jamais dessiné — sans lui, l'énigme est insoluble",
                    "Le même plafond s'applique aux règles de score de Ventes et à la liste de Commander ; un plateau qui ne tient plus défile à l'intérieur",
                ),
                "nl" to listOf(
                    "Reyon is weer speelbaar op korte telefoons: op een scherm van 360×640 dp nam het schap de hele hoogte in en werd de planogrambriefing van Vullen nooit getekend — zonder briefing is de puzzel niet op te lossen",
                    "Hetzelfde plafond geldt nu voor de scoreregels van Verkoop en de lijst van Bestellen; een blad dat niet meer past, schuift in zichzelf",
                ),
                "es" to listOf(
                    "Reyon vuelve a ser jugable en teléfonos cortos: en una pantalla de 360×640 dp el estante ocupaba toda la altura y el briefing de planograma de Colocar no se dibujaba — sin él no se puede resolver el puzle",
                    "El mismo techo se aplica a las reglas de puntuación de Ventas y a la lista de Pedidos; una bandeja que ya no cabe se desplaza por dentro",
                ),
                "pt" to listOf(
                    "Reyon voltou a ser jogável em telefones curtos: numa tela de 360×640 dp a prateleira ocupava toda a altura e o briefing de planograma de Arrumar nunca era desenhado — sem ele o quebra-cabeça não tem solução",
                    "O mesmo teto vale para as regras de pontuação de Vendas e a lista de Pedidos; uma bandeja que não cabe mais rola por dentro",
                ),
                "it" to listOf(
                    "Reyon è di nuovo giocabile sui telefoni corti: su uno schermo da 360×640 dp lo scaffale occupava tutta l'altezza e il briefing planogramma di Sistemare non veniva mai disegnato — senza il briefing il rompicapo non si risolve",
                    "Lo stesso tetto vale per le regole di punteggio di Vendite e per la lista di Ordini; un vassoio che non ci sta più scorre al suo interno",
                ),
                "da" to listOf(
                    "Reyon kan spilles på korte telefoner igen: på en skærm på 360×640 dp tog hylden hele højden, og planogram-briefingen i Stil op blev aldrig tegnet — uden briefingen kan opgaven ikke løses",
                    "Samme loft gælder nu pointreglerne i Salg og listen i Bestilling; en bakke, der ikke længere kan være der, ruller indvendigt",
                ),
                "sv" to listOf(
                    "Reyon går att spela på korta telefoner igen: på en skärm på 360×640 dp tog hyllan hela höjden och planogrambriefingen i Ställ upp ritades aldrig — utan briefingen går pusslet inte att lösa",
                    "Samma tak gäller nu poängreglerna i Försäljning och listan i Beställning; en bricka som inte längre får plats rullar inuti sig",
                ),
                "nb" to listOf(
                    "Reyon kan spilles på korte telefoner igjen: på en skjerm på 360×640 dp tok hyllen hele høyden, og planogram-briefen i Still opp ble aldri tegnet — uten briefen kan ikke oppgaven løses",
                    "Samme tak gjelder nå poengreglene i Salg og listen i Bestilling; et brett som ikke lenger får plass, ruller inni seg",
                ),
                "fi" to listOf(
                    "Reyon on taas pelattavissa lyhyillä puhelimilla: 360×640 dp:n näytöllä hylly vei koko korkeuden eikä Järjestä-tilan planogrammiohjeistusta piirretty koskaan — ilman ohjeistusta pulmaa ei voi ratkaista",
                    "Sama katto koskee nyt Myynnin pisteytyssääntöjä ja Tilauksen listaa; tarjotin, joka ei enää mahdu, vierii sisällään",
                ),
                "ru" to listOf(
                    "Reyon снова играется на коротких экранах: при 360×640 dp полка занимала всю высоту, и планограммный бриф «Раскладки» вообще не рисовался — без брифа головоломку не решить",
                    "Тот же предел теперь у правил начисления в «Продажах» и списка в «Заказах»; лоток, который больше не влезает, прокручивается внутри себя",
                ),
                "ar" to listOf(
                    "عاد Reyon قابلاً للعب على الشاشات القصيرة: عند 360×640 dp كان الرف يأكل الطول كله ولا يُرسم موجز المخطط في «الترتيب» أبداً — ولا تُحلّ الأحجية بدون الموجز",
                    "السقف نفسه صار على قواعد النقاط في «البيع» وعلى قائمة «الطلبيات»؛ والصينية التي لم تعد تتسع تتمرّر داخل نفسها",
                ),
            ),
        ),
        ReleaseNote(
            "0.43.1", "2026-09-20",
            tr = listOf(
                "Hakkında ekranına iletişim adresi eklendi: zagames@aripd.com. Dokununca telefonun e-posta uygulaması açılır",
                "Gizlilik politikası artık 14 dilde ve sitenin adresi zagames.aripd.com oldu",
            ),
            en = listOf(
                "The About screen now has a contact address: zagames@aripd.com. Tapping it opens your e-mail app",
                "The privacy policy is now in 14 languages, and the site moved to zagames.aripd.com",
            ),
            others = mapOf(
                "de" to listOf(
                    "Der Info-Bildschirm hat jetzt eine Kontaktadresse: zagames@aripd.com. Ein Tipp öffnet deine E-Mail-App",
                    "Die Datenschutzerklärung gibt es jetzt in 14 Sprachen, und die Seite liegt auf zagames.aripd.com",
                ),
                "fr" to listOf(
                    "L'écran À propos a maintenant une adresse de contact : zagames@aripd.com. Une touche ouvre votre appli e-mail",
                    "La politique de confidentialité existe maintenant en 14 langues, et le site est sur zagames.aripd.com",
                ),
                "nl" to listOf(
                    "Het scherm Over heeft nu een contactadres: zagames@aripd.com. Een tik opent je e-mailapp",
                    "Het privacybeleid staat nu in 14 talen en de site is verhuisd naar zagames.aripd.com",
                ),
                "es" to listOf(
                    "La pantalla Acerca de ahora tiene una dirección de contacto: zagames@aripd.com. Al tocarla se abre tu app de correo",
                    "La política de privacidad ya está en 14 idiomas y el sitio se mudó a zagames.aripd.com",
                ),
                "pt" to listOf(
                    "A tela Sobre agora tem um endereço de contato: zagames@aripd.com. Ao tocar, seu app de e-mail abre",
                    "A política de privacidade agora está em 14 idiomas e o site mudou para zagames.aripd.com",
                ),
                "it" to listOf(
                    "La schermata Informazioni ora ha un indirizzo di contatto: zagames@aripd.com. Toccandolo si apre la tua app di posta",
                    "L'informativa sulla privacy ora è in 14 lingue e il sito si è spostato su zagames.aripd.com",
                ),
                "da" to listOf(
                    "Skærmen Om har nu en kontaktadresse: zagames@aripd.com. Et tryk åbner din e-mail-app",
                    "Privatlivspolitikken findes nu på 14 sprog, og siden er flyttet til zagames.aripd.com",
                ),
                "sv" to listOf(
                    "Skärmen Om har nu en kontaktadress: zagames@aripd.com. En tryckning öppnar din e-postapp",
                    "Integritetspolicyn finns nu på 14 språk och webbplatsen har flyttat till zagames.aripd.com",
                ),
                "nb" to listOf(
                    "Om-skjermen har nå en kontaktadresse: zagames@aripd.com. Et trykk åpner e-postappen din",
                    "Personvernerklæringen finnes nå på 14 språk, og siden har flyttet til zagames.aripd.com",
                ),
                "fi" to listOf(
                    "Tietoja-näytössä on nyt yhteysosoite: zagames@aripd.com. Napautus avaa sähköpostisovelluksesi",
                    "Tietosuojakäytäntö on nyt 14 kielellä ja sivusto siirtyi osoitteeseen zagames.aripd.com",
                ),
                "ru" to listOf(
                    "На экране «О приложении» появился адрес для связи: zagames@aripd.com. Нажатие открывает почтовое приложение",
                    "Политика конфиденциальности теперь на 14 языках, а сайт переехал на zagames.aripd.com",
                ),
                "ar" to listOf(
                    "صارت شاشة «حول» تحمل عنوان تواصل: zagames@aripd.com. النقر عليه يفتح تطبيق بريدك",
                    "صارت سياسة الخصوصية بأربع عشرة لغة وانتقل الموقع إلى zagames.aripd.com",
                ),
            ),
        ),
        ReleaseNote(
            "0.43.0", "2026-09-20",
            tr = listOf(
                "Uygulama her yerde ZA Games adıyla anılıyor: ana menünün başlığı ve paylaşım kartının rozeti kısa \"ZA\" yazıyordu",
            ),
            en = listOf(
                "The app goes by ZA Games everywhere: the hub title and the share card badge used to read just \"ZA\"",
            ),
            others = mapOf(
                "de" to listOf(
                    "Die App heißt überall ZA Games: im Titel des Hauptmenüs und auf dem Abzeichen der Teilen-Karte stand nur \"ZA\"",
                ),
                "fr" to listOf(
                    "L'appli s'appelle ZA Games partout : le titre du menu et le badge de la carte de partage n'affichaient que \"ZA\"",
                ),
                "nl" to listOf(
                    "De app heet overal ZA Games: in de titel van het hoofdmenu en op het insigne van de deelkaart stond alleen \"ZA\"",
                ),
                "es" to listOf(
                    "La app se llama ZA Games en todas partes: el título del menú y el distintivo de la tarjeta para compartir solo decían \"ZA\"",
                ),
                "pt" to listOf(
                    "O app se chama ZA Games em todo lugar: o título do menu e o emblema do cartão de compartilhamento diziam apenas \"ZA\"",
                ),
                "it" to listOf(
                    "L'app si chiama ZA Games ovunque: il titolo del menu e il contrassegno della scheda di condivisione dicevano solo \"ZA\"",
                ),
                "da" to listOf(
                    "Appen hedder ZA Games overalt: titlen i hovedmenuen og mærket på delekortet stod kun som \"ZA\"",
                ),
                "sv" to listOf(
                    "Appen heter ZA Games överallt: rubriken i huvudmenyn och märket på delningskortet stod bara som \"ZA\"",
                ),
                "nb" to listOf(
                    "Appen heter ZA Games overalt: tittelen i hovedmenyen og merket på delekortet sto bare som \"ZA\"",
                ),
                "fi" to listOf(
                    "Sovellus on kaikkialla ZA Games: päävalikon otsikossa ja jakokortin merkissä luki pelkkä \"ZA\"",
                ),
                "ru" to listOf(
                    "Приложение везде называется ZA Games: в заголовке главного меню и на значке карточки для обмена было просто «ZA»",
                ),
                "ar" to listOf(
                    "صار التطبيق يُسمّى ZA Games في كل مكان: كان عنوان القائمة الرئيسية وشارة بطاقة المشاركة تكتب «ZA» فقط",
                ),
            ),
        ),
        ReleaseNote(
            "0.42.0", "2026-09-20",
            tr = listOf(
                "Uygulamanın paket kimliği com.aripd.zagames oldu. Yandan kurulumda bu yeni bir uygulama olarak görünür: eski sürüm telefonda kalır, rekorları taşınmaz",
            ),
            en = listOf(
                "The app's package id is now com.aripd.zagames. When sideloading it shows up as a new app: the old version stays on the phone and its records do not carry over",
            ),
            others = mapOf(
                "de" to listOf(
                    "Die Paket-ID der App ist jetzt com.aripd.zagames. Beim Sideloading erscheint sie als neue App: die alte Version bleibt auf dem Telefon, ihre Rekorde wandern nicht mit",
                ),
                "fr" to listOf(
                    "L'identifiant de paquet de l'appli est désormais com.aripd.zagames. En installation manuelle elle apparaît comme une nouvelle appli : l'ancienne version reste sur le téléphone et ses records ne suivent pas",
                ),
                "nl" to listOf(
                    "De pakket-id van de app is nu com.aripd.zagames. Bij handmatig installeren verschijnt hij als een nieuwe app: de oude versie blijft op de telefoon en de records gaan niet mee",
                ),
                "es" to listOf(
                    "El identificador de paquete de la app ahora es com.aripd.zagames. Al instalarla manualmente aparece como una app nueva: la versión anterior sigue en el teléfono y sus récords no se trasladan",
                ),
                "pt" to listOf(
                    "O identificador de pacote do app agora é com.aripd.zagames. Ao instalar manualmente ele aparece como um app novo: a versão antiga continua no telefone e os recordes não são transferidos",
                ),
                "it" to listOf(
                    "L'identificatore del pacchetto dell'app ora è com.aripd.zagames. Installandola manualmente compare come una nuova app: la versione precedente resta sul telefono e i record non vengono trasferiti",
                ),
                "da" to listOf(
                    "Appens pakke-id er nu com.aripd.zagames. Ved sideloading vises den som en ny app: den gamle version bliver på telefonen, og dens rekorder følger ikke med",
                ),
                "sv" to listOf(
                    "Appens paket-id är nu com.aripd.zagames. Vid sidladdning visas den som en ny app: den gamla versionen blir kvar i telefonen och dess rekord följer inte med",
                ),
                "nb" to listOf(
                    "Appens pakke-id er nå com.aripd.zagames. Ved sidelasting vises den som en ny app: den gamle versjonen blir værende på telefonen, og rekordene følger ikke med",
                ),
                "fi" to listOf(
                    "Sovelluksen pakettitunnus on nyt com.aripd.zagames. Sivuasennuksessa se näkyy uutena sovelluksena: vanha versio jää puhelimeen eivätkä sen ennätykset siirry",
                ),
                "ru" to listOf(
                    "Идентификатор пакета приложения теперь com.aripd.zagames. При ручной установке оно выглядит как новое приложение: старая версия остаётся на телефоне, её рекорды не переносятся",
                ),
                "ar" to listOf(
                    "صار معرّف حزمة التطبيق com.aripd.zagames. عند التثبيت اليدوي يظهر كتطبيق جديد: تبقى النسخة القديمة على الهاتف ولا تنتقل أرقامها القياسية",
                ),
            ),
        ),
        ReleaseNote(
            "0.41.1", "2026-09-20",
            tr = listOf(
                "Reyon: çözülmüş bir tura geri dönünce kutlama yeniden çalıyor ve rekor her girişte 1 artıyordu; artık bir kez sayılıyor",
                "Kıskaç ipucundaki yüzde işareti ve Hakkında'daki lisans notunun tırnakları düzeltildi",
            ),
            en = listOf(
                "Reyon: returning to a solved round replayed the celebration and counted the record again; now it counts once",
                "Fixed the percent sign in the Kıskaç hint and the quotation marks in the licence note under About",
            ),
            others = mapOf(
                "de" to listOf(
                    "Reyon: Die Rückkehr zu einer gelösten Runde ließ den Jubel erneut laufen und zählte den Rekord wieder mit; jetzt zählt sie einmal",
                    "Das Prozentzeichen im Kıskaç-Hinweis und die Anführungszeichen im Lizenzhinweis unter Über sind korrigiert",
                ),
                "fr" to listOf(
                    "Reyon : revenir sur une partie résolue rejouait la célébration et recomptait le record ; elle ne compte plus qu'une fois",
                    "Le signe pourcentage dans l'indice de Kıskaç et les guillemets de la note de licence dans À propos sont corrigés",
                ),
                "nl" to listOf(
                    "Reyon: terugkeren naar een opgeloste ronde speelde de felicitatie opnieuw en telde het record dubbel; nu telt die één keer",
                    "Het procentteken in de Kıskaç-hint en de aanhalingstekens in de licentienotitie bij Over zijn hersteld",
                ),
                "es" to listOf(
                    "Reyon: volver a una partida resuelta repetía la celebración y contaba el récord otra vez; ahora cuenta una sola vez",
                    "Corregidos el signo de porcentaje en la pista de Kıskaç y las comillas de la nota de licencia en Acerca de",
                ),
                "pt" to listOf(
                    "Reyon: voltar a uma rodada resolvida repetia a comemoração e contava o recorde de novo; agora conta uma vez",
                    "Corrigidos o sinal de porcentagem na dica de Kıskaç e as aspas da nota de licença em Sobre",
                ),
                "it" to listOf(
                    "Reyon: tornare su una partita risolta ripeteva la festa e contava di nuovo il record; ora conta una volta sola",
                    "Corretti il segno di percentuale nel suggerimento di Kıskaç e le virgolette della nota di licenza in Informazioni",
                ),
                "da" to listOf(
                    "Reyon: at vende tilbage til en løst runde gentog fejringen og talte rekorden igen; nu tælles den én gang",
                    "Procenttegnet i Kıskaç-hintet og citationstegnene i licensnoten under Om er rettet",
                ),
                "sv" to listOf(
                    "Reyon: att gå tillbaka till en löst runda spelade om hyllningen och räknade rekordet igen; nu räknas den en gång",
                    "Procenttecknet i Kıskaç-ledtråden och citattecknen i licensnoten under Om är rättade",
                ),
                "nb" to listOf(
                    "Reyon: å gå tilbake til en løst runde spilte feiringen om igjen og telte rekorden på nytt; nå telles den én gang",
                    "Prosenttegnet i Kıskaç-hintet og anførselstegnene i lisensnotatet under Om er rettet",
                ),
                "fi" to listOf(
                    "Reyon: ratkaistuun kierrokseen palaaminen toisti juhlinnan ja laski ennätyksen uudelleen; nyt se lasketaan kerran",
                    "Kıskaçin vihjeen prosenttimerkki ja Tietoja-näytön lisenssihuomion lainausmerkit korjattiin",
                ),
                "ru" to listOf(
                    "Reyon: возврат к решённому раунду заново проигрывал поздравление и снова считал рекорд; теперь он считается один раз",
                    "Исправлены знак процента в подсказке Kıskaç и кавычки в заметке о лицензии в разделе «О приложении»",
                ),
                "ar" to listOf(
                    "Reyon: العودة إلى دور محلول كانت تعيد الاحتفال وتحسب الرقم القياسي مرة أخرى؛ الآن يُحسب مرة واحدة",
                    "صُحّحت علامة النسبة في تلميح Kıskaç وعلامات التنصيص في ملاحظة الترخيص داخل «حول»",
                ),
            ),
        ),
        ReleaseNote(
            "0.41.0", "2026-09-20",
            tr = listOf(
                "Beş Harf, Kıskaç, Türetme ve Dizgi artık 14 dilde kendi sözlüğüyle oynanıyor; her dilin kendi klavyesi, kendi alfabe sırası ve kendi günlük bulmacası var",
                "Kelime dili arayüzün dilinden ayrı seçilebiliyor: uygulamayı Almanca kullanıp Beş Harf'i Türkçe oynayabilirsin. Seçim dört oyunun kurulum kartında",
            ),
            en = listOf(
                "Beş Harf, Kıskaç, Türetme and Dizgi now play in 14 languages with their own dictionaries; each language has its own keyboard, alphabetical order and daily puzzle",
                "The word language is picked separately from the app language: use the app in German and play Beş Harf in Turkish. The setting sits in each game's setup card",
            ),
            others = mapOf(
                "de" to listOf(
                    "Beş Harf, Kıskaç, Türetme und Dizgi spielen jetzt in 14 Sprachen mit eigenen Wörterbüchern; jede Sprache hat ihre Tastatur, ihre alphabetische Reihenfolge und ihr tägliches Rätsel",
                    "Die Wortsprache wird getrennt von der App-Sprache gewählt: App auf Deutsch, Beş Harf auf Türkisch. Die Einstellung steht im Startbereich jedes Spiels",
                ),
                "fr" to listOf(
                    "Beş Harf, Kıskaç, Türetme et Dizgi se jouent maintenant en 14 langues avec leurs propres dictionnaires ; chaque langue a son clavier, son ordre alphabétique et son énigme du jour",
                    "La langue des mots se choisit séparément de celle de l'appli : l'appli en français et Beş Harf en turc. Le réglage est dans l'écran de départ de chaque jeu",
                ),
                "nl" to listOf(
                    "Beş Harf, Kıskaç, Türetme en Dizgi spelen nu in 14 talen met hun eigen woordenboeken; elke taal heeft zijn toetsenbord, zijn alfabetische orde en zijn dagpuzzel",
                    "De woordtaal kies je los van de taal van de app: de app in het Nederlands en Beş Harf in het Turks. De instelling staat in het startscherm van elk spel",
                ),
                "es" to listOf(
                    "Beş Harf, Kıskaç, Türetme y Dizgi ya se juegan en 14 idiomas con sus propios diccionarios; cada idioma tiene su teclado, su orden alfabético y su reto diario",
                    "El idioma de las palabras se elige aparte del de la app: la app en español y Beş Harf en turco. El ajuste está en la pantalla de inicio de cada juego",
                ),
                "pt" to listOf(
                    "Beş Harf, Kıskaç, Türetme e Dizgi agora são jogados em 14 idiomas com os seus próprios dicionários; cada idioma tem o seu teclado, a sua ordem alfabética e o seu desafio diário",
                    "O idioma das palavras é escolhido separadamente do idioma do app: o app em português e Beş Harf em turco. O ajuste fica na tela inicial de cada jogo",
                ),
                "it" to listOf(
                    "Beş Harf, Kıskaç, Türetme e Dizgi si giocano ora in 14 lingue con i propri dizionari; ogni lingua ha la sua tastiera, il suo ordine alfabetico e la sua sfida del giorno",
                    "La lingua delle parole si scegli separatamente da quella dell'app: app in italiano e Beş Harf in turco. L'impostazione è nella schermata iniziale di ogni gioco",
                ),
                "da" to listOf(
                    "Beş Harf, Kıskaç, Türetme og Dizgi spilles nu på 14 sprog med deres egne ordbøger; hvert sprog har sit tastatur, sin alfabetiske orden og sin daglige opgave",
                    "Ordsproget vælges uafhængigt af appens sprog: appen på dansk og Beş Harf på tyrkisk. Indstillingen står på hvert spils startskærm",
                ),
                "sv" to listOf(
                    "Beş Harf, Kıskaç, Türetme och Dizgi spelas nu på 14 språk med egna ordböcker; varje språk har sitt tangentbord, sin alfabetiska ordning och sin dagliga uppgift",
                    "Ordspråket väljs separat från appens språk: appen på svenska och Beş Harf på turkiska. Inställningen finns på varje spels startskärm",
                ),
                "nb" to listOf(
                    "Beş Harf, Kıskaç, Türetme og Dizgi spilles nå på 14 språk med egne ordbøker; hvert språk har sitt tastatur, sin alfabetiske rekkefølge og sin daglige oppgave",
                    "Ordspråket velges uavhengig av appens språk: appen på norsk og Beş Harf på tyrkisk. Innstillingen står på hvert spills startskjerm",
                ),
                "fi" to listOf(
                    "Beş Harf, Kıskaç, Türetme ja Dizgi pelataan nyt 14 kielellä omilla sanakirjoillaan; jokaisella kielellä on oma näppäimistö, oma aakkosjärjestys ja oma päivän pulma",
                    "Sanojen kieli valitaan sovelluksen kielestä erikseen: sovellus suomeksi ja Beş Harf turkiksi. Asetus on jokaisen pelin aloitusnäytössä",
                ),
                "ru" to listOf(
                    "Beş Harf, Kıskaç, Türetme и Dizgi теперь играются на 14 языках со своими словарями; у каждого языка своя клавиатура, свой алфавитный порядок и своя задача дня",
                    "Язык слов выбирается отдельно от языка приложения: приложение по-русски, а Beş Harf по-турецки. Настройка — на стартовом экране каждой игры",
                ),
                "ar" to listOf(
                    "صارت Beş Harf وKıskaç وTüretme وDizgi تُلعب بأربع عشرة لغة بقواميسها الخاصة؛ لكل لغة لوحة مفاتيحها وترتيبها الأبجدي ولغز يومها",
                    "تُختار لغة الكلمات مستقلةً عن لغة التطبيق: التطبيق بالعربية وBeş Harf بالتركية. الإعداد في شاشة بداية كل لعبة",
                ),
            ),
        ),
        ReleaseNote(
            "0.40.0", "2026-09-19",
            tr = listOf(
                "Uygulama 14 dilde: Türkçe, İngilizce, Almanca, Fransızca, Hollandaca, İspanyolca, Portekizce, İtalyanca, Danca, İsveççe, Norveççe, Fince, Rusça ve Arapça. Telefonun diline uyar; ana menüdeki dil düğmesinden de seçebilirsin",
                "Beş Harf, Kıskaç, Türetme ve Dizgi Türkçe kelime listeleriyle oynandığı için metinleri Türkçe ya da İngilizce kalıyor; o dillerde kelime listeleri hazırlanınca onlar da çevrilecek",
            ),
            en = listOf(
                "The app now speaks 14 languages: English, Turkish, German, French, Dutch, Spanish, Portuguese, Italian, Danish, Swedish, Norwegian, Finnish, Russian and Arabic. It follows your phone's language, and the language button in the hub lets you pick one",
                "Beş Harf, Kıskaç, Türetme and Dizgi play on Turkish word lists, so their text stays Turkish or English; they will follow once word lists exist for those languages",
            ),
            others = mapOf(
                "de" to listOf(
                    "Die App spricht jetzt 14 Sprachen: Deutsch, Englisch, Türkisch, Französisch, Niederländisch, Spanisch, Portugiesisch, Italienisch, Dänisch, Schwedisch, Norwegisch, Finnisch, Russisch und Arabisch. Sie folgt der Sprache deines Telefons, und mit der Sprachtaste im Hauptmenü kannst du selbst wählen",
                    "Beş Harf, Kıskaç, Türetme und Dizgi spielen mit türkischen Wortlisten, ihre Texte bleiben daher türkisch oder englisch",
                ),
                "fr" to listOf(
                    "L'appli parle maintenant 14 langues : français, anglais, turc, allemand, néerlandais, espagnol, portugais, italien, danois, suédois, norvégien, finnois, russe et arabe. Elle suit la langue du téléphone, et le bouton de langue du menu permet d'en choisir une",
                    "Beş Harf, Kıskaç, Türetme et Dizgi utilisent des listes de mots turcs, leur texte reste donc en turc ou en anglais",
                ),
                "nl" to listOf(
                    "De app spreekt nu 14 talen: Nederlands, Engels, Turks, Duits, Frans, Spaans, Portugees, Italiaans, Deens, Zweeds, Noors, Fins, Russisch en Arabisch. Hij volgt de taal van je toestel, en met de taalknop in het hoofdmenu kies je er zelf een",
                    "Beş Harf, Kıskaç, Türetme en Dizgi spelen met Turkse woordenlijsten, hun tekst blijft daarom Turks of Engels",
                ),
                "es" to listOf(
                    "La app ya habla 14 idiomas: español, inglés, turco, alemán, francés, neerlandés, portugués, italiano, danés, sueco, noruego, finés, ruso y árabe. Sigue el idioma del teléfono, y el botón de idioma del menú te deja elegir",
                    "Beş Harf, Kıskaç, Türetme y Dizgi usan listas de palabras turcas, así que su texto queda en turco o en inglés",
                ),
                "pt" to listOf(
                    "O app agora fala 14 idiomas: português, inglês, turco, alemão, francês, holandês, espanhol, italiano, dinamarquês, sueco, norueguês, finlandês, russo e árabe. Ele segue o idioma do telefone, e o botão de idioma no menu deixa você escolher",
                    "Beş Harf, Kıskaç, Türetme e Dizgi usam listas de palavras turcas, então o texto deles fica em turco ou em inglês",
                ),
                "it" to listOf(
                    "L'app parla ora 14 lingue: italiano, inglese, turco, tedesco, francese, olandese, spagnolo, portoghese, danese, svedese, norvegese, finlandese, russo e arabo. Segue la lingua del telefono, e il pulsante della lingua nel menu ti lascia scegliere",
                    "Beş Harf, Kıskaç, Türetme e Dizgi usano liste di parole turche, quindi il loro testo resta in turco o in inglese",
                ),
                "da" to listOf(
                    "Appen taler nu 14 sprog: dansk, engelsk, tyrkisk, tysk, fransk, nederlandsk, spansk, portugisisk, italiensk, svensk, norsk, finsk, russisk og arabisk. Den følger telefonens sprog, og sprogknappen i menuen lader dig vælge selv",
                    "Beş Harf, Kıskaç, Türetme og Dizgi spiller med tyrkiske ordlister, så deres tekst bliver på tyrkisk eller engelsk",
                ),
                "sv" to listOf(
                    "Appen talar nu 14 språk: svenska, engelska, turkiska, tyska, franska, nederländska, spanska, portugisiska, italienska, danska, norska, finska, ryska och arabiska. Den följer telefonens språk, och språkknappen i menyn låter dig välja själv",
                    "Beş Harf, Kıskaç, Türetme och Dizgi spelar med turkiska ordlistor, så deras text stannar på turkiska eller engelska",
                ),
                "nb" to listOf(
                    "Appen snakker nå 14 språk: norsk, engelsk, tyrkisk, tysk, fransk, nederlandsk, spansk, portugisisk, italiensk, dansk, svensk, finsk, russisk og arabisk. Den følger språket på telefonen, og språkknappen i menyen lar deg velge selv",
                    "Beş Harf, Kıskaç, Türetme og Dizgi spiller med tyrkiske ordlister, så teksten deres blir på tyrkisk eller engelsk",
                ),
                "fi" to listOf(
                    "Sovellus puhuu nyt 14 kieltä: suomi, englanti, turkki, saksa, ranska, hollanti, espanja, portugali, italia, tanska, ruotsi, norja, venäjä ja arabia. Se seuraa puhelimen kieltä, ja valikon kielipainikkeesta voit valita itse",
                    "Beş Harf, Kıskaç, Türetme ja Dizgi käyttävät turkkilaisia sanalistoja, joten niiden teksti pysyy turkkina tai englantina",
                ),
                "ru" to listOf(
                    "Приложение говорит на 14 языках: русский, английский, турецкий, немецкий, французский, нидерландский, испанский, португальский, итальянский, датский, шведский, норвежский, финский и арабский. Оно следует языку телефона, а кнопка языка в меню даёт выбрать самому",
                    "Beş Harf, Kıskaç, Türetme и Dizgi играются на турецких словарях, поэтому их текст остаётся на турецком или английском",
                ),
                "ar" to listOf(
                    "صار التطبيق يتكلّم 14 لغة: العربية والإنجليزية والتركية والألمانية والفرنسية والهولندية والإسبانية والبرتغالية والإيطالية والدنماركية والسويدية والنرويجية والفنلندية والروسية. يتبع لغة الهاتف، وزرّ اللغة في القائمة يتيح لك الاختيار",
                    "تُلعب Beş Harf وKıskaç وTüretme وDizgi بقوائم كلمات تركية، لذلك يبقى نصّها بالتركية أو الإنجليزية",
                ),
            ),
        ),
        ReleaseNote(
            "0.39.0", "2026-09-19",
            tr = listOf(
                "Mağaza yayınına hazırlık: Android 16'ya (API 36) göre derleniyor, sürüm paketi Play biçiminde de üretiliyor",
                "Blok baştan sona kendi adını taşıyor; eski rekor ve son oynananlar kaydı korunur",
            ),
            en = listOf(
                "Store-release prep: built against Android 16 (API 36), the release pipeline also produces the Play bundle",
                "Blok now carries its own name throughout; your old record and recently-played entry are kept",
            ),
        ),
        ReleaseNote(
            "0.38.0", "2026-09-13",
            tr = listOf(
                "Viraj artık Filo gibi sürüklenerek sürülüyor: parmağını kaydır, araç o çizgiye orantılı kırar ve orada kalır; sol/sağ bölge kalktı. Fren için parmağı aşağı çek ya da ikinci parmağını bas",
            ),
            en = listOf(
                "Viraj is now driven by dragging, like Filo: slide your finger and the car steers proportionally to that line and holds it; the left/right zones are gone. Brake by pulling the finger down or with a second finger",
            ),
        ),
        ReleaseNote(
            "0.37.1", "2026-09-11",
            tr = listOf(
                "Cici: kedilerin konturu kalınlaştı, siyah kedi smokin desenli; koyu uzayda daha görünür",
            ),
            en = listOf(
                "Cici: thicker cat outlines and a tuxedo pattern for the black cat; easier to see against dark space",
            ),
        ),
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
                "Düşen blok oyununun adı Blok oldu",
                "Hakkında ekranı: sürüm, bağlantılar, açık kaynak lisansları",
                "Gizlilik politikası sayfası; sitede oyunlar gruplandı",
            ),
            en = listOf(
                "The falling-blocks game is now called Blok",
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
                "Paylaşım bağlantısı zagames.aripd.com (0.20.1)",
            ),
            en = listOf(
                "Share button on every end-of-game card: result image and text",
                "Kakuro, Sudoku, Minesweeper, Beş Harf, 2048 and Dizgi include the finished board",
                "Share link points to zagames.aripd.com (0.20.1)",
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
