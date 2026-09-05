package com.za.games.sayi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SayiTest {

    @Test
    fun vergiciRules() {
        var s = VergiciState.start(6)
        assertEquals(listOf(2, 3, 4, 5, 6), s.takeable) // 1'in böleni yok
        assertEquals(listOf(1, 2, 3), s.divisorsOf(6))
        s = s.take(6)
        assertEquals(6, s.player)
        assertEquals(listOf(1, 2, 3), s.lastTaxed)
        // 1, 2 ve 3 gidince 4 ve 5 alınamaz: oyun biter, kalanlar vergiciye.
        assertTrue(s.over)
        assertEquals(6 + 9, s.taxman) // 1+2+3, sonra 4+5
        assertEquals(listOf(4, 5), s.leftovers)
        assertTrue(s.boardNumbers.isEmpty())
        // Bitmeden önce: 5 alınırsa yalnız 1 gider, 4 hâlâ alınabilir (böleni 2 duruyor).
        val u = VergiciState.start(6).take(5)
        assertFalse(u.over)
        assertEquals(listOf(1), u.lastTaxed)
        assertTrue(u.canTake(4))
        assertFalse(u.canTake(3)) // asal, 1 gitti
        assertEquals(21, s.total)
        // Aynı durum alınamayan sayıda değişmez.
        val t = VergiciState.start(6)
        assertEquals(t, t.take(1))
        assertEquals(t, t.take(7))
    }

    @Test
    fun vergiciBestPlay() {
        // n=6: en iyi sıra 5 (vergici 1), 4 (vergici 2), 6 (vergici 3) → 15'e karşı 6.
        val best = VergiciSolver.optimal(6)!!
        assertEquals(15, best)
        assertTrue(VergiciSolver.optimal(12)!! >= VergiciSolver.greedyScore(12))
        for (n in VergiciState.SIZES) {
            val t0 = System.nanoTime()
            val opt = VergiciSolver.optimal(n, budget = 400_000)
            println("n=$n optimal=$opt greedy=${VergiciSolver.greedyScore(n)} nodes=${VergiciSolver.lastNodes} ${(System.nanoTime() - t0) / 1_000_000} ms")
            if (opt != null) assertTrue(opt >= VergiciSolver.greedyScore(n))
        }
    }

    @Test
    fun toplamRules() {
        var s = ToplamState()
        assertEquals((1..9).toList(), s.available)
        s = s.pick(8).pick(2).pick(4).pick(6).pick(3) // 8+4+3 = 15
        assertTrue(s.over)
        assertEquals(0, s.winner)
        assertEquals(listOf(3, 4, 8), s.winningTriple)
        assertEquals(s, s.pick(1)) // bitti
        // Berabere: kimse 15 yapmadan dokuz sayı.
        var d = ToplamState()
        for (x in listOf(5, 9, 1, 8, 2, 4, 6, 7, 3)) d = d.pick(x)
        assertTrue(d.over)
        assertNull(d.winner)
        // Yinelenen sayı alınamaz.
        val p = ToplamState().pick(5)
        assertEquals(p, p.pick(5))
        assertEquals(0, p.ownerOf(5))
        assertNull(p.ownerOf(6))
    }

    @Test
    fun toplamPerfectAiNeverLoses() {
        val rng = Random(9)
        var aiLosses = 0
        repeat(200) {
            var s = ToplamState()
            val aiPlayer = it % 2
            while (!s.over) {
                val move = if (s.turn == aiPlayer) ToplamAi.choose(s, perfect = true, rng = rng) else s.available[rng.nextInt(s.available.size)]
                s = s.pick(move)
            }
            if (s.winner != null && s.winner != aiPlayer) aiLosses++
        }
        assertEquals(0, aiLosses)
        // Kusursuz ikisi de: hep berabere.
        var s = ToplamState()
        while (!s.over) s = s.pick(ToplamAi.choose(s, perfect = true, rng = rng))
        assertNull(s.winner)
        // Kolay seviye bazen kaybeder.
        var easyLosses = 0
        repeat(200) {
            var g = ToplamState()
            while (!g.over) {
                val move = if (g.turn == 1) ToplamAi.choose(g, perfect = false, rng = rng) else ToplamAi.choose(g, perfect = true, rng = rng)
                g = g.pick(move)
            }
            if (g.winner == 0) easyLosses++
        }
        assertTrue(easyLosses > 0)
        assertNotNull(ToplamState.MAGIC_SQUARE)
        assertEquals(15, ToplamState.MAGIC_SQUARE.take(3).sum())
    }
}
