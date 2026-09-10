package com.za.games.tuse

/**
 * Parçalar: yüz yılı geçmiş besteler ve anonim ezgiler, elle yazılmış nota
 * dizileri. Ses dosyası taşınmaz; notalar [NoteSynth] ile çalışma anında
 * sentezlenir. Adlar ve künyeler uygulama dizelerinde (`tuse_song_<id>`,
 * `tuse_credit_<id>`).
 */
object Songs {

    /** Neşeye Övgü — Beethoven, 9. Senfoni (1824). */
    val ODE = Song(
        "ode",
        intArrayOf(
            64, 64, 65, 67, 67, 65, 64, 62, 60, 60, 62, 64, 64, 62, 62,
            64, 64, 65, 67, 67, 65, 64, 62, 60, 60, 62, 64, 62, 60, 60,
            62, 62, 64, 60, 62, 64, 65, 64, 60, 62, 64, 65, 64, 62, 60, 62, 55,
            64, 64, 65, 67, 67, 65, 64, 62, 60, 60, 62, 64, 62, 60, 60,
        ),
    )

    /** Für Elise — Beethoven (1810), ana tema. */
    val ELISE = Song(
        "elise",
        intArrayOf(
            76, 75, 76, 75, 76, 71, 74, 72, 69, 60, 64, 69, 71, 64, 68, 71, 72,
            64, 76, 75, 76, 75, 76, 71, 74, 72, 69, 60, 64, 69, 71, 64, 72, 71, 69,
            71, 72, 74, 76, 67, 77, 76, 74, 65, 76, 74, 72, 64, 74, 72, 71,
            64, 76, 75, 76, 75, 76, 71, 74, 72, 69, 60, 64, 69, 71, 64, 68, 71, 72,
            64, 76, 75, 76, 75, 76, 71, 74, 72, 69, 60, 64, 69, 71, 64, 72, 71, 69,
        ),
    )

    /** Türk Marşı — Mozart, Rondo alla Turca (1783), giriş teması. */
    val TURCA = Song(
        "turca",
        intArrayOf(
            71, 69, 68, 69, 72, 74, 72, 71, 72, 76,
            77, 76, 75, 76, 83, 81, 80, 81, 83, 81, 80, 81, 84, 81,
            72, 71, 69, 68, 69, 71, 69, 68, 69, 72,
            71, 69, 68, 69, 72, 74, 72, 71, 72, 76,
            77, 76, 75, 76, 83, 81, 80, 81, 83, 81, 80, 81, 84, 81,
        ),
    )

    /** Daha Dün Annemizin — anonim (Ah! vous dirai-je, maman, 18. yy). */
    val TWINKLE = Song(
        "twinkle",
        intArrayOf(
            60, 60, 67, 67, 69, 69, 67, 65, 65, 64, 64, 62, 62, 60,
            67, 67, 65, 65, 64, 64, 62, 67, 67, 65, 65, 64, 64, 62,
            60, 60, 67, 67, 69, 69, 67, 65, 65, 64, 64, 62, 62, 60,
        ),
    )

    /** Mutlu Yıllar — geleneksel. */
    val BIRTHDAY = Song(
        "birthday",
        intArrayOf(
            67, 67, 69, 67, 72, 71, 67, 67, 69, 67, 74, 72,
            67, 67, 79, 76, 72, 71, 69, 77, 77, 76, 72, 74, 72,
        ),
    )

    /** Sol Majör Menuet — Petzold (yak. 1725). */
    val MINUET = Song(
        "minuet",
        intArrayOf(
            74, 67, 69, 71, 72, 74, 67, 67, 76, 72, 74, 76, 78, 79, 67, 67,
            72, 74, 72, 71, 69, 71, 72, 71, 69, 67, 66, 67, 69, 71, 67, 71, 69,
            74, 67, 69, 71, 72, 74, 67, 67, 76, 72, 74, 76, 78, 79, 67, 67,
            72, 74, 72, 71, 69, 71, 72, 71, 69, 67, 69, 71, 69, 67, 66, 67,
        ),
    )

    /** Greensleeves — anonim İngiliz halk ezgisi (16. yy). */
    val GREENSLEEVES = Song(
        "greensleeves",
        intArrayOf(
            69, 72, 74, 76, 77, 76, 74, 71, 67, 69, 71, 72, 71, 69, 68, 66, 68, 69, 69,
            69, 72, 74, 76, 77, 76, 74, 71, 67, 69, 71, 72, 71, 69, 68, 69, 69,
            79, 79, 78, 76, 74, 71, 67, 69, 71, 72, 71, 69, 68, 66, 68, 69, 69,
            79, 79, 78, 76, 74, 71, 67, 69, 71, 72, 71, 69, 68, 69, 69,
        ),
    )

    val ALL: List<Song> = listOf(ODE, ELISE, TURCA, TWINKLE, BIRTHDAY, MINUET, GREENSLEEVES)

    fun byId(id: String?): Song = ALL.firstOrNull { it.id == id } ?: ODE

    /** Günün parçası: takvim gününe göre sırayla. */
    fun ofDay(epochDay: Long): Song = ALL[((epochDay % ALL.size) + ALL.size).toInt() % ALL.size]
}
