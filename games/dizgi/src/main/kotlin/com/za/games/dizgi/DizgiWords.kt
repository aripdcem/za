package com.za.games.dizgi

/** Gömülü sözlük: 2-15 harfli geçerli kökler (tools/gen_dizgi.py üretir). */
object DizgiWords {

    val valid: Set<String> by lazy {
        DizgiWords::class.java.getResourceAsStream("/dizgi/valid.txt")!!
            .bufferedReader(Charsets.UTF_8)
            .readLines()
            .filterTo(HashSet()) { it.isNotBlank() }
    }
}
