package com.za.games.sozluk

/**
 * Kelime oyunlarının (Beş Harf, Kıskaç, Türetme, Dizgi) desteklediği diller.
 *
 * Her dilin üç ayrı harf dizisi vardır ve üçü de aynı kümeyi kapsar:
 *  * [letters] alfabe, Dizgi'nin harf tablosuyla aynı sırada;
 *  * [order] sözlük sırası — Kıskaç'ın "önce mi sonra mı" ipucu buna dayanır ve
 *    kelime listeleri diskte zaten bu sırayla durur. Unicode sırası birçok dilde
 *    yanlıştır: Almanca'da ä, a ile aynı yere girer; İsveççe'de ä, z'den sonra;
 *    Türkçe'de ı, i'den önce gelir;
 *  * [keyRows] ekran klavyesi, ülkenin kendi düzeniyle (QWERTZ, AZERTY, ЙЦУКЕН…).
 *
 * Tablo tools/wordlang.py ile aynıdır; tools/check_wordlists.py ikisini
 * karşılaştırır, ayrışırsa CI hata verir.
 */
enum class WordLang(
    val tag: String,
    val letters: String,
    val order: String,
    val keyRows: List<String>,
) {
    EN(
        tag = "en",
        letters = "abcdefghijklmnopqrstuvwxyz",
        order = "abcdefghijklmnopqrstuvwxyz",
        keyRows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm"),
    ),
    TR(
        tag = "tr",
        letters = "abcçdefgğhıijklmnoöprsştuüvyz",
        order = "abcçdefgğhıijklmnoöprsştuüvyz",
        keyRows = listOf("ertyuıopğü", "asdfghjklşi", "zcvbnmöç"),
    ),
    DE(
        tag = "de",
        letters = "abcdefghijklmnopqrstuvwxyzäöü",
        order = "aäbcdefghijklmnoöpqrstuüvwxyz",
        keyRows = listOf("qwertzuiopü", "asdfghjklöä", "yxcvbnm"),
    ),
    FR(
        tag = "fr",
        letters = "abcdefghijklmnopqrstuvwxyz",
        order = "abcdefghijklmnopqrstuvwxyz",
        keyRows = listOf("azertyuiop", "qsdfghjklm", "wxcvbn"),
    ),
    NL(
        tag = "nl",
        letters = "abcdefghijklmnopqrstuvwxyz",
        order = "abcdefghijklmnopqrstuvwxyz",
        keyRows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm"),
    ),
    ES(
        tag = "es",
        letters = "abcdefghijklmnopqrstuvwxyzñ",
        order = "abcdefghijklmnñopqrstuvwxyz",
        keyRows = listOf("qwertyuiop", "asdfghjklñ", "zxcvbnm"),
    ),
    PT(
        tag = "pt",
        letters = "abcdefghijklmnopqrstuvwxyz",
        order = "abcdefghijklmnopqrstuvwxyz",
        keyRows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm"),
    ),
    IT(
        tag = "it",
        letters = "abcdefghijklmnopqrstuvwxyz",
        order = "abcdefghijklmnopqrstuvwxyz",
        keyRows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm"),
    ),
    DA(
        tag = "da",
        letters = "abcdefghijklmnopqrstuvwxyzæøå",
        order = "abcdefghijklmnopqrstuvwxyzæøå",
        keyRows = listOf("qwertyuiopå", "asdfghjkløæ", "zxcvbnm"),
    ),
    SV(
        tag = "sv",
        letters = "abcdefghijklmnopqrstuvwxyzåäö",
        order = "abcdefghijklmnopqrstuvwxyzåäö",
        keyRows = listOf("qwertyuiopå", "asdfghjklöä", "zxcvbnm"),
    ),
    NB(
        tag = "nb",
        letters = "abcdefghijklmnopqrstuvwxyzæøå",
        order = "abcdefghijklmnopqrstuvwxyzæøå",
        keyRows = listOf("qwertyuiopå", "asdfghjkløæ", "zxcvbnm"),
    ),
    FI(
        tag = "fi",
        letters = "abcdefghijklmnopqrstuvwxyzåäö",
        order = "abcdefghijklmnopqrstuvwxyzåäö",
        keyRows = listOf("qwertyuiopå", "asdfghjklöä", "zxcvbnm"),
    ),
    RU(
        tag = "ru",
        letters = "абвгдежзийклмнопрстуфхцчшщъыьэюя",
        order = "абвгдежзийклмнопрстуфхцчшщъыьэюя",
        keyRows = listOf("йцукенгшщзхъ", "фывапролджэ", "ячсмитьбю"),
    ),
    AR(
        tag = "ar",
        letters = "ابتثجحخدذرزسشصضطظعغفقكلمنهوي",
        order = "ابتثجحخدذرزسشصضطظعغفقكلمنهوي",
        keyRows = listOf("ضصثقفغعهخحج", "شسيبلاتنمك", "ظطذدزرو"),
    ),
    ;

    /** Oyunun kabul ettiği harfler. */
    val alphabet: Set<Char> = letters.toSet()

    private val ranks: Map<Char, Int> =
        order.withIndex().associate { (index, letter) -> letter to index }

    /** Tablo dışı bir karakter (olmamalı) alfabenin sonuna sıralanır; çökmez. */
    fun rankOf(letter: Char): Int = ranks[letter] ?: (order.length + letter.code)

    /** Dilin sözlük sırasına göre karşılaştırır. */
    fun compare(a: String, b: String): Int {
        val n = minOf(a.length, b.length)
        for (i in 0 until n) {
            val d = rankOf(a[i]) - rankOf(b[i])
            if (d != 0) return d
        }
        return a.length - b.length
    }

    /** [sorted] bu dille sıralı olmalı; bulunamazsa ekleme noktası döner. */
    fun indexOf(sorted: List<String>, word: String): Int {
        var low = 0
        var high = sorted.size - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            when {
                compare(sorted[mid], word) < 0 -> low = mid + 1
                compare(sorted[mid], word) > 0 -> high = mid - 1
                else -> return mid
            }
        }
        return low
    }

    companion object {
        /** Kelime listesi olmayan bir dilde oyun bu dille açılır. */
        val DEFAULT = EN

        /** Dil etiketinden ("de", "pt") oyunun dili; tanımadıysa null. */
        fun of(tag: String): WordLang? = entries.firstOrNull { it.tag == tag }

        /** Telefonun dili desteklenmiyorsa İngilizce. */
        fun forTagOrDefault(tag: String): WordLang = of(tag) ?: DEFAULT
    }
}
