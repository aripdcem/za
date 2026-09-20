package com.za.games.sozluk

/**
 * Kelime listelerinin dosya biçimi ve okuyucusu.
 *
 * Uzun listeler (geçerli tahminler, Türetme ve Dizgi sözlükleri) **ön-kodlu**
 * yazılır: her satır, bir önceki kelimeyle paylaşılan ön ekin uzunluğu artı
 * kalan harflerdir. "kalem, kalemlik, kalemtıraş" -> "0kalem", "5lik", "5tıraş".
 *
 * Düz metin de sıkışır ama APK'nın deflate'i tekrarı ancak 32 KB'lık pencerede
 * görür; ön ek payı baştan atılınca 14 dilin listeleri 4,6 MB yerine 3,0 MB yer
 * kaplar. Kısa listeler (cevap havuzu, Türetme tabanları, harf tablosu) düz
 * kalır: elle okunup gözden geçiriliyorlar.
 *
 * Dosyalar tools/gen_wordlists.py ile üretilir ve dilin sözlük sırasında durur.
 */
object WordFile {

    private const val ZERO = '0'

    /** Ön-kodlu satırları kelimelere çevirir; sıra korunur. */
    fun decode(lines: Sequence<String>): List<String> {
        val words = ArrayList<String>()
        var previous = ""
        for (line in lines) {
            if (line.isEmpty()) continue
            val shared = line[0] - ZERO
            require(shared in 0..previous.length) {
                "Bozuk kelime listesi: '$line' için $shared harflik ön ek, " +
                    "önceki kelime '$previous' ${previous.length} harf"
            }
            val word = previous.substring(0, shared) + line.substring(1)
            words.add(word)
            previous = word
        }
        return words
    }

    /** Ön-kodlu kaynak dosyasını okur. */
    fun read(owner: Class<*>, path: String): List<String> =
        open(owner, path).useLines { decode(it) }

    /** Düz (kodlanmamış) kaynak dosyasını okur: boş satırlar atlanır. */
    fun readPlain(owner: Class<*>, path: String): List<String> =
        open(owner, path).useLines { lines ->
            lines.map { it.trim() }.filter { it.isNotEmpty() }.toList()
        }

    private fun open(owner: Class<*>, path: String) =
        requireNotNull(owner.getResourceAsStream(path)) {
            "Kelime listesi bulunamadı: $path"
        }.bufferedReader(Charsets.UTF_8)
}
