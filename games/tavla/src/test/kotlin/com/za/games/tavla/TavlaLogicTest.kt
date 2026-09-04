package com.za.games.tavla

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TavlaLogicTest {

    private val klasik = TavlaRules(TavlaMode.KLASIK)
    private val hapis = TavlaRules(TavlaMode.HAPIS)

    private fun board(vararg entries: Pair<Int, Point>): List<Point> {
        val points = MutableList(TavlaLogic.POINTS) { Point() }
        for ((i, p) in entries) points[i] = p
        return points
    }

    private fun total(points: List<Point>, bar: List<Int>, off: List<Int>, player: Int): Int {
        var n = bar[player] + off[player]
        for (p in points) {
            if (p.owner == player) n += p.count
            if (p.pinned && p.owner == 1 - player) n += 1
        }
        return n
    }

    @Test
    fun setupsHaveFifteenCheckersEach() {
        for (mode in TavlaMode.entries) {
            val points = TavlaLogic.initialPoints(mode)
            for (player in 0..1) assertEquals(15, total(points, listOf(0, 0), listOf(0, 0), player))
        }
        val tapa = TavlaLogic.initialPoints(TavlaMode.TAPA)
        assertEquals(Point(0, 15), tapa[23])
        assertEquals(Point(1, 15), tapa[0])
        val classic = TavlaLogic.initialPoints(TavlaMode.KLASIK)
        assertEquals(Point(0, 2), classic[23])
        assertEquals(Point(1, 5), classic[18])
    }

    @Test
    fun openingMovesAndLargerDieRule() {
        val points = TavlaLogic.initialPoints(TavlaMode.KLASIK)
        val turns = TavlaLogic.legalTurns(klasik, points, listOf(0, 0), listOf(0, 0), 0, listOf(3, 1))
        assertTrue(turns.isNotEmpty())
        assertTrue(turns.all { it.size == 2 })
        // 8/5 6/5 kapı yapma açılışı mümkün olmalı.
        assertTrue(turns.any { t -> t.any { it.from == 7 && it.to == 4 } && t.any { it.from == 5 && it.to == 4 } })

        // Yalnız tek zar oynanabiliyorsa büyük olan zorunlu: iki pulu ile 6 oynanabiliyor, 1 oynanamıyor.
        val blocked = board(
            23 to Point(0, 1), // oyuncu 0'ın tek pulu
            22 to Point(1, 2), // 1 için hedef kapalı
            17 to Point(1, 1), // 6 ile vurulabilir
            16 to Point(1, 2), 15 to Point(1, 2), 14 to Point(1, 2), 13 to Point(1, 2), 12 to Point(1, 2), 10 to Point(1, 2),
        )
        val t2 = TavlaLogic.legalTurns(klasik, blocked, listOf(0, 0), listOf(0, 0), 0, listOf(6, 1))
        assertEquals(1, t2.size)
        assertEquals(listOf(Move(23, 17, 6)), t2[0])
    }

    @Test
    fun hitSendsToBarAndBarMustEnterFirst() {
        val points = board(23 to Point(0, 1), 20 to Point(1, 1), 5 to Point(0, 2))
        val applied = TavlaLogic.apply(klasik, points, listOf(0, 0), listOf(0, 0), 0, Move(23, 20, 3))
        assertEquals(Point(0, 1), applied.points[20])
        assertEquals(1, applied.bar[1])
        // Bar'daki pul girmeden başka hamle yok; 1 ile giriş hanesi (0) oyuncu 0'ın kapısıysa giremez.
        val entryBlocked = board(0 to Point(0, 2), 1 to Point(0, 2), 10 to Point(1, 3))
        val moves = TavlaLogic.singleMoves(klasik, entryBlocked, listOf(0, 1), 1, 1)
        assertTrue(moves.isEmpty())
        val moves3 = TavlaLogic.singleMoves(klasik, entryBlocked, listOf(0, 1), 1, 3)
        assertEquals(listOf(Move(Move.BAR, 2, 3)), moves3)
    }

    @Test
    fun bearingOffNeedsAllHomeAndAllowsOvershootFromHighest() {
        val notHome = board(5 to Point(0, 14), 7 to Point(0, 1))
        assertFalse(TavlaLogic.allInHome(notHome, listOf(0, 0), 0))
        assertTrue(TavlaLogic.singleMoves(klasik, notHome, listOf(0, 0), 0, 6).none { it.to == Move.OFF })

        val home = board(5 to Point(0, 2), 3 to Point(0, 3), 0 to Point(0, 10))
        val six = TavlaLogic.singleMoves(klasik, home, listOf(0, 0), 0, 6)
        assertTrue(six.contains(Move(5, Move.OFF, 6)))
        // 4 ile: 4. haneden (dizin 3) tam toplama; 1. haneden fazla zarla toplama yok (daha uzakta pul var).
        val four = TavlaLogic.singleMoves(klasik, home, listOf(0, 0), 0, 4)
        assertTrue(four.contains(Move(3, Move.OFF, 4)))
        assertTrue(four.none { it.from == 0 && it.to == Move.OFF })
        assertTrue(four.contains(Move(5, 1, 4)))
        // Sadece 3. hanede pul kalınca 6 ile toplanır (fazla zar, en uzaktaki pul).
        val low = board(2 to Point(0, 15))
        assertEquals(listOf(Move(2, Move.OFF, 6)), TavlaLogic.singleMoves(klasik, low, listOf(0, 0), 0, 6))
    }

    @Test
    fun pinningTrapsAndReleases() {
        val points = board(23 to Point(0, 1), 20 to Point(1, 1))
        val pinned = TavlaLogic.apply(hapis, points, listOf(0, 0), listOf(0, 0), 0, Move(23, 20, 3))
        assertEquals(Point(0, 1, pinned = true), pinned.points[20])
        assertEquals(listOf(0, 0), pinned.bar) // kırılma yok
        // Hapisteki pul oynayamaz; oyuncu 1'in tek pulu hapiste → hiç hamle yok.
        assertTrue(TavlaLogic.singleMoves(hapis, pinned.points, listOf(0, 0), 1, 4).isEmpty())
        // Hapsedeni hapsetmek yok: oyuncu 1'in başka bir pulu bu haneye inemez.
        val withOther = pinned.points.toMutableList().also { it[16] = Point(1, 1) }
        assertNull(TavlaLogic.landing(hapis, withOther, 1, 20))
        // Hapsedenin üstüne kendi pulu eklenebilir.
        assertEquals(Landing.OWN, TavlaLogic.landing(hapis, withOther, 0, 20))
        // Hapseden gidince mahkûm serbest kalır.
        val released = TavlaLogic.apply(hapis, pinned.points, listOf(0, 0), listOf(0, 0), 0, Move(20, 15, 5))
        assertEquals(Point(1, 1, pinned = false), released.points[20])
        assertEquals(Point(0, 1), released.points[15])
        // Toplam pul korunur.
        assertEquals(1, total(released.points, released.bar, released.off, 1))
    }

    @Test
    fun noPinInOpponentHomeOption() {
        val rules = TavlaRules(TavlaMode.HAPIS, noPinInOpponentHome = true)
        val points = board(23 to Point(0, 1), 20 to Point(1, 1), 10 to Point(1, 1))
        assertNull(TavlaLogic.landing(rules, points, 0, 20)) // 20 ∈ oyuncu 1'in evi (18..23)
        assertEquals(Landing.PIN, TavlaLogic.landing(rules, points, 0, 10))
        assertEquals(Landing.PIN, TavlaLogic.landing(hapis, points, 0, 20))
    }

    @Test
    fun pinnedCheckersBlockBearOffAndCountAsPips() {
        // Oyuncu 0'ın bir pulu 10. dizinde hapis: evde değil → toplama yok.
        val points = board(10 to Point(1, 1, pinned = true), 3 to Point(0, 14))
        assertFalse(TavlaLogic.allInHome(points, listOf(0, 0), 0))
        assertEquals(11 + 4 * 14, TavlaLogic.pips(points, listOf(0, 0), 0))
        assertEquals(14, TavlaLogic.pips(points, listOf(0, 0), 1))
    }

    @Test
    fun doublesGiveFourMovesAndTurnsAreMaximal() {
        val points = board(23 to Point(0, 2), 12 to Point(0, 5), 7 to Point(0, 3), 5 to Point(0, 5), 0 to Point(1, 2))
        val turns = TavlaLogic.legalTurns(klasik, points, listOf(0, 0), listOf(0, 0), 0, listOf(2, 2, 2, 2))
        assertTrue(turns.isNotEmpty())
        assertTrue(turns.all { it.size == 4 })
        assertTrue(turns.all { t -> t.all { it.die == 2 } })
    }
}
