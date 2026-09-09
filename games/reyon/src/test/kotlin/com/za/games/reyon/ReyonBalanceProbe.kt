package com.za.games.reyon

import org.junit.Test

/**
 * Reyon denge ölçümü — birim testi değil, rapor üretir
 * (bkz. docs/oyun-testi.md). `./gradlew :games:reyon:probe` ile koşar.
 *
 * Sıra tabanlı bulmaca: soru yetişilebilirlik değil adilliktir. Tek çözüm ve
 * tahminsizlik üreticide garanti; burada ölçülen, zorluk merdiveninin gerçek
 * olup olmadığı (hangi teknikler gerekiyor), brifin uzunluğu ve karışımı,
 * verili sayısı ve üretim bütçesi.
 */
class ReyonBalanceProbe {

    private val seeds = 1L..40L

    private fun kindOf(c: Clue): String = when (c) {
        is Clue.Placed -> "verili"
        is Clue.OnShelf -> "raf"
        is Clue.InSlot -> "göz"
        is Clue.AtEdge -> "kenar"
        is Clue.Adjacent -> "yan yana"
        is Clue.LeftOf -> "solunda"
        is Clue.SameShelf -> "aynı raf"
        is Clue.DifferentShelf -> "farklı raf"
        is Clue.Above -> "üstünde"
        Clue.EyeLevel -> "göz hizası"
        Clue.HeavyBottom -> "ağır alt"
        is Clue.CategoryBlock -> "kategori bloğu"
        is Clue.CategoriesApart -> "kategoriler ayrı"
        is Clue.BrandVertical -> "marka dikey"
        is Clue.SizeFlow -> "boy akışı"
    }

    @Test
    fun report() {
        println("=== Reyon denge ölçümü (${seeds.count()} tohum/zorluk) ===")
        for (level in ReyonLevel.entries) {
            val puzzles = ArrayList<ReyonPuzzle>()
            var timeMs = 0L
            var worstMs = 0L
            for (seed in seeds) {
                val t0 = System.nanoTime()
                puzzles += ReyonGenerator.generate(seed, level)
                val ms = (System.nanoTime() - t0) / 1_000_000
                timeMs += ms
                worstMs = maxOf(worstMs, ms)
            }
            val n = puzzles.size.toFloat()
            val mix = HashMap<String, Int>()
            for (p in puzzles) for (c in p.clues) mix[kindOf(c)] = (mix[kindOf(c)] ?: 0) + 1
            fun pct(pred: (ReyonPuzzle) -> Boolean) = "%.0f%%".format(100f * puzzles.count(pred) / n)
            fun solvable(p: ReyonPuzzle, tech: Int) = ReyonDeducer(Propagator(p.board, p.products, p.clues)).solves(tech)
            val basic = pct { solvable(it, Tech.BASIC) }
            val cover = pct { solvable(it, Tech.BASIC or Tech.COVER) }
            val capacity = pct { solvable(it, Tech.BASIC or Tech.COVER or Tech.CAPACITY) }
            val all = pct { solvable(it, Tech.ALL) }
            println("--- $level (${level.rows}×${level.cols}) ---")
            println("ürün ort %.1f (%d–%d) · brif ort %.1f (en çok %d, sınır %d) · verili ort %.1f".format(
                puzzles.map { it.products.size }.average(), puzzles.minOf { it.products.size }, puzzles.maxOf { it.products.size },
                puzzles.map { it.brief.size }.average(), puzzles.maxOf { it.brief.size }, level.maxClues,
                puzzles.map { it.givens.size }.average()))
            println("tahminsiz çözüm: yalnız tekil+ikili $basic · +örtü $cover · +kapasite $capacity · tüm teknikler $all")
            val techs = HashMap<String, Int>()
            for (p in puzzles) for (t in Tech.names(p.stats.techniques)) techs[t] = (techs[t] ?: 0) + 1
            println("kullanılan teknikler: " + techs.entries.sortedByDescending { it.value }.joinToString { "${it.key} %.0f%%".format(100f * it.value / n) })
            println("ipucu karışımı: " + mix.entries.sortedByDescending { it.value }.joinToString { "${it.key} ${it.value}" })
            println("üretim: deneme ort %.1f (en çok %d) · düğüm ort %.0f (en çok %d) · aday ort %.0f · süre ort %d ms (en kötü %d ms, yalnız rapor)".format(
                puzzles.map { it.stats.attempts }.average(), puzzles.maxOf { it.stats.attempts },
                puzzles.map { it.stats.solverNodes }.average(), puzzles.maxOf { it.stats.solverNodes },
                puzzles.map { it.stats.candidates }.average(), timeMs / puzzles.size, worstMs))
            val sample = puzzles.first()
            println("örnek (tohum ${sample.seed}): " + sample.products.joinToString { "${it.kind.name.lowercase()}×${it.facings}" })
            println("  brif: " + sample.brief.joinToString(" · ") { kindOf(it) + " " + it.toString().substringAfter('(').substringBefore(')') })
        }
    }
}
