package com.za.games.turetme

/**
 * Gömülü kelime listeleri. tools/gen_turetme.py ile Zemberek (Apache-2.0)
 * kök sözlüğünden ve FrequencyWords (CC-BY-SA-4.0) sıklık verisinden
 * türetilir; tamamen çevrimdışıdır.
 */
object TuretmeWords {

    /** 3-7 harfli geçerli kökler (alt kelime doğrulaması). */
    val valid: Set<String> by lazy { load("valid.txt").toHashSet() }

    /** 6-7 harfli taban kelimeler; her biri en az 15 alt kelime garantili. */
    val bases: List<String> by lazy { load("bases.txt") }

    private fun load(name: String): List<String> =
        requireNotNull(TuretmeWords::class.java.getResourceAsStream("/turetme/$name")) {
            "Kelime listesi bulunamadı: $name"
        }.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.map { it.trim() }.filter { it.isNotEmpty() }.toList()
        }
}
