package com.za.games.tavla

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TavlaStateTest {

    private fun match(mode: TavlaMode = TavlaMode.KLASIK, seed: Long = 7L, cube: Boolean = false, target: Int = 5) =
        TavlaState.newMatch(TavlaRules(mode, target = target, cube = cube), seed)

    /** Bir yasal turu oynar (yapay zekâ seçimiyle); tur bitince rakip zar atmaya hazırdır. */
    private fun playTurn(state: TavlaState): TavlaState {
        var s = state
        if (s.phase == Phase.TO_ROLL) s = s.roll()
        if (s.phase != Phase.MOVING) return s
        if (!s.canMove) return s.endTurn()
        val turn = TavlaAi.chooseTurn(s)
        for (m in turn) {
            val before = s
            s = s.move(m.from, m.to)
            assertNotEquals("hamle reddedildi: $m", before, s)
            if (s.phase != Phase.MOVING) break
        }
        if (s.phase == Phase.MOVING) s = s.endTurn()
        return s
    }

    @Test
    fun openingRollPicksStarterAndUsesBothDice() {
        val s = match().openingRoll()
        assertEquals(Phase.MOVING, s.phase)
        assertEquals(2, s.openingDice.size)
        assertNotEquals(s.openingDice[0], s.openingDice[1])
        val expectedStarter = if (s.openingDice[0] > s.openingDice[1]) 0 else 1
        assertEquals(expectedStarter, s.turn)
        assertEquals(s.openingDice.sortedDescending(), s.dice)
        assertEquals(2, s.remaining.size)
        assertTrue(s.canMove)
    }

    @Test
    fun sameSeedGivesSameMatch() {
        var a = match(seed = 42L).openingRoll()
        var b = match(seed = 42L).openingRoll()
        repeat(40) {
            a = playTurn(a)
            b = playTurn(b)
        }
        assertEquals(a, b)
    }

    @Test
    fun moveUsesDieAndTurnEndsWhenDiceRunOut() {
        var s = match().openingRoll()
        val first = s.legalMoves().first()
        val before = s.remaining.size
        s = s.move(first.from, first.to)
        assertEquals(before - 1, s.remaining.size + (if (s.phase == Phase.TO_ROLL) 1 else 0) - (if (s.phase == Phase.TO_ROLL) 1 else 0))
        assertEquals(1, s.played.size)
        // Yasadışı hamle durumu değiştirmez.
        val illegal = s.move(0, 23)
        assertEquals(s, illegal)
        // Kalan zarı da oyna: tur rakibe geçer.
        if (s.phase == Phase.MOVING) {
            val second = s.legalMoves().first()
            s = s.move(second.from, second.to)
        }
        assertEquals(Phase.TO_ROLL, s.phase)
        assertTrue(s.played.isEmpty())
    }

    @Test
    fun undoRestoresBoardWithinTurn() {
        var s = match().openingRoll()
        val before = s
        val m = s.legalMoves().first()
        s = s.move(m.from, m.to)
        if (s.phase != Phase.MOVING) return // tek hamlede tur bittiyse geri alma yok
        s = s.undo()
        assertEquals(before.points, s.points)
        assertEquals(before.remaining, s.remaining)
        assertTrue(s.played.isEmpty())
    }

    @Test
    fun gameEndsWithScoreAndMarsAndMatchProgresses() {
        // Oyuncu 0'ın son pulu 1. hanede, oyuncu 1 hiç toplamamış: toplayınca mars (2 puan).
        val base = match(target = 3)
        val points = MutableList(TavlaLogic.POINTS) { Point() }
        points[0] = Point(0, 1)
        points[18] = Point(1, 15)
        var s = base.copy(points = points, off = listOf(14, 0), turn = 0, phase = Phase.MOVING, dice = listOf(3, 1), remaining = listOf(3, 1))
        s = s.move(0, Move.OFF)
        assertEquals(Phase.GAME_OVER, s.phase)
        assertEquals(0, s.winner)
        assertTrue(s.mars)
        assertEquals(listOf(2, 0), s.scores)
        val next = s.nextGame()
        assertEquals(Phase.OPENING, next.phase)
        assertEquals(2, next.game)
        assertEquals(listOf(2, 0), next.scores)
        assertEquals(15, next.points.sumOf { if (it.owner == 0) it.count else 0 })
        // İkinci oyun: hedef 3'e ulaşınca maç biter.
        val p2 = MutableList(TavlaLogic.POINTS) { Point() }
        p2[0] = Point(0, 1)
        p2[18] = Point(1, 14)
        var t = next.copy(points = p2, off = listOf(14, 1), turn = 0, phase = Phase.MOVING, dice = listOf(1, 1), remaining = listOf(1, 1, 1, 1))
        t = t.move(0, Move.OFF)
        assertEquals(Phase.MATCH_OVER, t.phase)
        assertFalse(t.mars)
        assertEquals(listOf(3, 0), t.scores)
    }

    @Test
    fun mutualDeadlockIsDecidedByPipsOrDrawn() {
        val base = match(TavlaMode.HAPIS, target = 5)
        val points = MutableList(TavlaLogic.POINTS) { Point() }
        points[0] = Point(0, 14, pinned = true) // altında oyuncu 1'in mahkûmu
        points[23] = Point(1, 14, pinned = true) // altında oyuncu 0'ın mahkûmu
        var s = base.copy(points = points, turn = 0, phase = Phase.TO_ROLL)
        assertTrue(s.isStuck(0) && s.isStuck(1))
        s = s.roll()
        assertFalse(s.canMove)
        s = s.endTurn()
        assertEquals(Phase.GAME_OVER, s.phase)
        assertTrue(s.deadlock)
        assertEquals(null, s.winner) // pipler eşit: berabere
        assertEquals(listOf(0, 0), s.scores)
        assertEquals(Phase.OPENING, s.nextGame().phase)

        // Oyuncu 0 bir pul toplamışsa pipi daha az: 1 puanla kazanır.
        val p2 = points.toMutableList()
        p2[0] = Point(0, 13, pinned = true)
        var t = base.copy(points = p2, off = listOf(1, 0), turn = 1, phase = Phase.TO_ROLL).roll().endTurn()
        assertEquals(Phase.GAME_OVER, t.phase)
        assertTrue(t.deadlock)
        assertEquals(0, t.winner)
        assertEquals(listOf(1, 0), t.scores)
    }

    @Test
    fun cubeOfferAcceptAndDecline() {
        var s = match(cube = true).openingRoll()
        s = playTurn(s) // ilk tur oynandı; rakip TO_ROLL
        assertEquals(Phase.TO_ROLL, s.phase)
        assertTrue(s.canDouble)
        val offerer = s.turn
        val offered = s.offerDouble()
        assertEquals(Phase.DOUBLE_OFFERED, offered.phase)
        assertEquals(1 - offerer, offered.responder)
        // Kabul: küp 2, sahibi kabul eden; teklif eden zar atar.
        val accepted = offered.acceptDouble()
        assertEquals(Phase.TO_ROLL, accepted.phase)
        assertEquals(2, accepted.cubeValue)
        assertEquals(1 - offerer, accepted.cubeOwner)
        assertFalse(accepted.canDouble) // küp artık rakipte
        // Pes: teklif eden 1 puan (küp değeri) alır.
        val declined = offered.declineDouble()
        assertEquals(Phase.GAME_OVER, declined.phase)
        assertEquals(offerer, declined.winner)
        assertTrue(declined.resigned)
        assertEquals(1, declined.scores[offerer])
        // Küp kapalıyken teklif yok.
        assertFalse(match(cube = false).openingRoll().let { playTurn(it) }.canDouble)
    }

    @Test
    fun cubeMultipliesGameScoreAndResetsNextGame() {
        val base = match(cube = true, target = 9)
        val points = MutableList(TavlaLogic.POINTS) { Point() }
        points[0] = Point(0, 1)
        points[18] = Point(1, 15)
        var s = base.copy(points = points, off = listOf(14, 0), turn = 0, phase = Phase.MOVING, dice = listOf(2, 1), remaining = listOf(2, 1), cubeValue = 4, cubeOwner = 0)
        s = s.move(0, Move.OFF)
        assertEquals(listOf(8, 0), s.scores) // 4 × mars
        val next = s.nextGame()
        assertEquals(1, next.cubeValue)
        assertEquals(null, next.cubeOwner)
    }

    @Test
    fun aiPlaysWholeMatchesLegallyInEveryMode() {
        for (mode in TavlaMode.entries) {
            for (seed in 1L..3L) {
                var s = TavlaState.newMatch(TavlaRules(mode, target = 1, noPinInOpponentHome = seed == 2L), seed).openingRoll()
                var turns = 0
                while (s.phase != Phase.MATCH_OVER && turns < 2000) {
                    s = if (s.phase == Phase.GAME_OVER) s.nextGame().openingRoll() else playTurn(s)
                    turns++
                }
                assertEquals("mod $mode tohum $seed bitmedi", Phase.MATCH_OVER, s.phase)
                if (!s.deadlock) assertEquals(15, s.off[s.winner!!])
                // Pul sayısı korunmuş olmalı.
                for (player in 0..1) {
                    var n = s.bar[player] + s.off[player]
                    for (p in s.points) {
                        if (p.owner == player) n += p.count
                        if (p.pinned && p.owner == 1 - player) n += 1
                    }
                    assertEquals(15, n)
                }
            }
        }
    }

    @Test
    fun aiDoublingDecisionsAreSane() {
        val base = match(cube = true)
        // Oyuncu 0 neredeyse bitirmiş: katlamak ister; rakip pes eder.
        val points = MutableList(TavlaLogic.POINTS) { Point() }
        points[0] = Point(0, 3)
        points[23] = Point(1, 13)
        val strong = base.copy(points = points, off = listOf(12, 2), turn = 0, phase = Phase.TO_ROLL)
        assertTrue(TavlaAi.winChance(strong, 0) > 0.9)
        assertFalse(TavlaAi.acceptsDouble(strong.offerDouble()))
        // Dengeli açılış: katlamak istemez.
        val even = base.openingRoll().let { playTurn(it) }
        assertFalse(TavlaAi.wantsDouble(even))
    }
}
