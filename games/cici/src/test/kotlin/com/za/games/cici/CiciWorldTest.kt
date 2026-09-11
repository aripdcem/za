package com.za.games.cici

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

class CiciWorldTest {

    /** Boş uzay: doğum kapalı. */
    private fun space(seed: Long = 1L): CiciWorld = CiciWorld(seed).also { it.freezeSpawnForTest() }

    /** [frames] adım; [stir] ile her karede küçük bir parmak girişi verilir ki hareketsizlik sayılmasın. */
    private fun run(w: CiciWorld, frames: Int, stir: Boolean = false): List<CiciEvent> {
        val out = ArrayList<CiciEvent>()
        repeat(frames) { i ->
            if (stir) w.steerBy(if (i % 2 == 0) 0.001f else -0.001f, 0f)
            out += w.step()
        }
        return out
    }

    private fun runUntil(w: CiciWorld, max: Int, pred: (List<CiciEvent>) -> Boolean): List<CiciEvent> {
        val out = ArrayList<CiciEvent>()
        repeat(max) {
            out += w.step()
            if (pred(out)) return out
        }
        throw AssertionError("beklenen durum $max karede oluşmadı; olaylar: $out")
    }

    @Test
    fun sameSeedSameInputsSameRun() {
        val a = CiciWorld(11L)
        val b = CiciWorld(11L)
        val rng = Random(5)
        var frames = 0
        while (a.status == CiciStatus.RUNNING && frames < 5400) {
            if (frames % 3 == 0) {
                val dx = (rng.nextFloat() - 0.5f) * 0.06f
                val dy = (rng.nextFloat() - 0.5f) * 0.06f
                a.steerBy(dx, dy)
                b.steerBy(dx, dy)
            }
            assertEquals(a.step(), b.step())
            frames++
        }
        assertEquals(a.ciciX, b.ciciX, 0f)
        assertEquals(a.ciciY, b.ciciY, 0f)
        assertEquals(a.score, b.score)
        assertEquals(a.lives, b.lives)
        assertEquals(a.treats.map { it.id }, b.treats.map { it.id })
        assertEquals(a.cats.map { it.id }, b.cats.map { it.id })
        assertEquals(CiciWorld.dailySeed(3L), CiciWorld.dailySeed(3L))
        assertNotEquals(CiciWorld.dailySeed(3L), CiciWorld.dailySeed(4L))
        assertEquals(CiciChapter.SPACE, a.chapter)
    }

    @Test
    fun steeringIsBoundedSpeedLimitedAndSetsFacing() {
        val w = space()
        w.steerBy(5f, 5f)
        run(w, 300)
        assertEquals(CiciWorld.WIDTH - CiciWorld.CICI_R, w.ciciX, 1e-5f)
        assertEquals(CiciWorld.HEIGHT - CiciWorld.CICI_R, w.ciciY, 1e-5f)
        assertEquals(1, w.facing)
        w.steerBy(-0.5f, 0f)
        val x0 = w.ciciX
        w.step()
        assertEquals(-1, w.facing)
        assertEquals("adım başına en çok CICI_SPEED/60", CiciWorld.CICI_SPEED * CiciWorld.STEP, x0 - w.ciciX, 1e-4f)
        w.steerBy(-5f, -5f)
        run(w, 400)
        assertEquals(CiciWorld.CICI_R, w.ciciX, 1e-5f)
        assertEquals(CiciWorld.CICI_R, w.ciciY, 1e-5f)
        w.steerBy(0f, 0.3f)
        assertEquals("dikey hareket yönü değiştirmez", -1, w.facing)
        w.steerTo(0.9f, 0.9f)
        assertEquals(1, w.facing)
    }

    @Test
    fun treatsScoreTheirPointsAndBuildAStreak() {
        val w = space()
        w.setCiciForTest(0.5f, 0.8f)
        w.addTreatForTest(TreatKind.HONEY, 0.5f, 0.8f)
        var ev = w.step()
        assertEquals(listOf(CiciEvent.Caught(TreatKind.HONEY, 7, 1, 0.5f, 0.8f)), ev)
        assertEquals(7, w.score)
        assertEquals(Mood.HAPPY, w.mood)
        w.addTreatForTest(TreatKind.SEED, 0.5f, 0.8f)
        ev = w.step()
        assertEquals(CiciEvent.Caught(TreatKind.SEED, 5, 2, 0.5f, 0.8f), ev.single())
        w.addTreatForTest(TreatKind.WATER, 0.5f, 0.8f)
        ev = w.step()
        assertEquals(CiciEvent.Caught(TreatKind.WATER, 2, 3, 0.5f, 0.8f), ev.single())
        assertEquals(14, w.score)
        assertEquals(3, w.streak)
        assertEquals(3, w.caught)
        assertEquals(1, w.caughtOf(TreatKind.HONEY))
        assertEquals(1, w.caughtOf(TreatKind.SEED))
        assertEquals(1, w.caughtOf(TreatKind.WATER))
        assertEquals(3, w.hud().streak)
        // Sevinç penceresi (3 s) geçince seri başa döner; en iyi seri kalır.
        run(w, 185, stir = true)
        assertEquals(Mood.CALM, w.mood)
        w.addTreatForTest(TreatKind.SEED, w.ciciX, w.ciciY)
        ev = w.step()
        assertEquals(1, (ev.single() as CiciEvent.Caught).streak)
        assertEquals(3, w.bestStreak)
        assertEquals(19, w.score)
        // Uzaktaki ikram alınmaz; ekranı terk edince silinir.
        val far = w.addTreatForTest(TreatKind.HONEY, 0.1f, 0.2f, vx = -0.5f)
        run(w, 40, stir = true)
        assertTrue(far.alive)
        assertEquals(19, w.score)
        assertTrue("terk eden ikram silinir", w.treats.none { it === far })
    }

    @Test
    fun moodTurnsHappyOnACatchAndCalmsDown() {
        val w = space()
        w.setCiciForTest(0.5f, 0.8f)
        assertEquals(Mood.CALM, w.mood)
        run(w, 100, stir = true)
        assertEquals(Mood.CALM, w.mood)
        w.addTreatForTest(TreatKind.WATER, 0.5f, 0.8f)
        w.step()
        assertEquals(Mood.HAPPY, w.mood)
        run(w, 89, stir = true)
        assertEquals(Mood.HAPPY, w.mood)
        run(w, 2, stir = true)
        assertEquals(Mood.CALM, w.mood)
    }

    @Test
    fun idlePenaltyStartsAfterThreeSecondsAndNeverGoesBelowZero() {
        val w = space()
        w.setCiciForTest(0.5f, 0.8f)
        w.setScoreForTest(5)
        var ev = run(w, 119)
        assertTrue(ev.isEmpty())
        assertEquals(Mood.CALM, w.mood)
        ev = w.step()
        assertEquals("2 s: sıkıldı", listOf(CiciEvent.Bored), ev)
        assertEquals(Mood.BORED, w.mood)
        ev = run(w, 59)
        assertTrue(ev.isEmpty())
        assertEquals(5, w.score)
        ev = w.step()
        assertEquals("3 s: ilk puan gider", listOf(CiciEvent.PointLost(4)), ev)
        ev = run(w, 240)
        assertEquals(listOf(CiciEvent.PointLost(3), CiciEvent.PointLost(2), CiciEvent.PointLost(1), CiciEvent.PointLost(0)), ev)
        assertEquals(0, w.score)
        assertEquals(5, w.lostPoints)
        ev = run(w, 180)
        assertTrue("sıfırın altına inmez, olay da üretmez", ev.isEmpty())
        assertEquals(Mood.BORED, w.mood)
        assertTrue(w.hud().idleSeconds > 9f)
        // Parmak kıpırdayınca sayaç sıfırlanır.
        w.steerBy(0.01f, 0f)
        assertEquals(0f, w.idleSeconds, 0f)
        w.setScoreForTest(5)
        ev = run(w, 179)
        assertTrue(ev.contains(CiciEvent.Bored))
        assertTrue(ev.none { it is CiciEvent.PointLost })
        assertEquals(5, w.score)
    }

    @Test
    fun catContactCostsALifeInvulnerabilityProtectsThenTheBallEndsIt() {
        val w = space()
        w.setCiciForTest(0.5f, 0.8f)
        w.addTreatForTest(TreatKind.SEED, 0.5f, 0.8f)
        w.step()
        assertEquals(1, w.streak)
        val cat = w.addCatForTest(0.5f, 0.8f)
        val ev = w.step()
        assertEquals(listOf(CiciEvent.Hit(HazardKind.CAT, 2)), ev)
        assertEquals(2, w.lives)
        assertEquals("vuruş seriyi bozar", 0, w.streak)
        assertEquals(Mood.CALM, w.mood)
        assertTrue(w.hud().invulnerable)
        assertTrue(cat.alive)
        run(w, 100, stir = true)
        assertEquals("2 s dokunulmazlık", 2, w.lives)
        run(w, 25, stir = true)
        assertEquals("dokunulmazlık bitince yine çarpar", 1, w.lives)
        w.addBallForTest(0.5f, 0.8f, 0f, 0f)
        val ev2 = runUntil(w, 130) { it.any { e -> e is CiciEvent.Hit } }
        val last = ev2.filterIsInstance<CiciEvent.Hit>().last()
        assertEquals(0, last.lives)
        assertTrue(last.by == HazardKind.CAT || last.by == HazardKind.BALL)
        assertEquals(CiciStatus.OVER, w.status)
        assertEquals(CiciEvent.Over, ev2.last())
        assertTrue(w.step().isEmpty())
        assertEquals(CiciStatus.OVER, w.hud().status)
    }

    @Test
    fun theBallBouncesInsideTheArenaAndSpeedsUpWithTime() {
        val w = space()
        w.setLivesForTest(99)
        w.setCiciForTest(0.5f, 0.8f)
        val ball = w.addBallForTest(0.3f, 0.3f, 0.2f, 0.12f)
        var flips = 0
        var lastVx = ball.vx
        var lastVy = ball.vy
        repeat(900) {
            w.step()
            assertTrue("top içeride: ${ball.x},${ball.y}", ball.x >= CiciWorld.BALL_R - 1e-4f && ball.x <= CiciWorld.WIDTH - CiciWorld.BALL_R + 1e-4f)
            assertTrue(ball.y >= CiciWorld.BALL_R - 1e-4f && ball.y <= CiciWorld.HEIGHT - CiciWorld.BALL_R + 1e-4f)
            if ((ball.vx > 0f) != (lastVx > 0f) || (ball.vy > 0f) != (lastVy > 0f)) flips++
            lastVx = ball.vx
            lastVy = ball.vy
        }
        assertTrue("kenarlardan seker: $flips", flips >= 3)
        assertEquals("hız rampayı izler", CiciWorld.ballSpeed(w.seconds), hypot(ball.vx, ball.vy), 1e-3f)
        assertTrue(hypot(ball.vx, ball.vy) > CiciWorld.BALL_SPEED0)
        w.setSecondsForTest(CiciWorld.RAMP_TIME)
        w.step()
        assertEquals("rampa sonunda azami hız", CiciWorld.BALL_SPEED_MAX, hypot(ball.vx, ball.vy), 1e-3f)
        ball.x = w.ciciX
        ball.y = w.ciciY
        val ev = w.step()
        assertEquals("top can götürür", listOf(CiciEvent.Hit(HazardKind.BALL, 98)), ev)
    }

    @Test
    fun difficultyRampTightensSpawnsAndSpeeds() {
        assertEquals(CiciWorld.TREAT_INTERVAL0, CiciWorld.treatInterval(0f), 1e-6f)
        assertEquals(CiciWorld.TREAT_INTERVAL_MIN, CiciWorld.treatInterval(CiciWorld.RAMP_TIME), 1e-6f)
        assertEquals(CiciWorld.TREAT_INTERVAL_MIN, CiciWorld.treatInterval(999f), 1e-6f)
        assertEquals(CiciWorld.CAT_INTERVAL0, CiciWorld.catInterval(0f), 1e-6f)
        assertEquals(CiciWorld.CAT_INTERVAL_MIN, CiciWorld.catInterval(CiciWorld.RAMP_TIME), 1e-6f)
        assertEquals(0f, CiciWorld.catHoming(0f), 0f)
        assertEquals(0f, CiciWorld.catHoming(CiciWorld.RAMP_TIME * 0.25f), 1e-6f)
        assertEquals(CiciWorld.CAT_HOMING_MAX, CiciWorld.catHoming(CiciWorld.RAMP_TIME), 1e-6f)
        assertEquals(1f + CiciWorld.TREAT_SPEED_GAIN, CiciWorld.treatSpeedMul(CiciWorld.RAMP_TIME), 1e-6f)
        var prevT = 9f
        var prevC = 99f
        var prevB = 0f
        for (s in 0..200 step 5) {
            val t = CiciWorld.treatInterval(s.toFloat())
            val c = CiciWorld.catInterval(s.toFloat())
            val b = CiciWorld.ballSpeed(s.toFloat())
            assertTrue(t <= prevT && c <= prevC && b >= prevB)
            prevT = t
            prevC = c
            prevB = b
        }
    }

    /** Değişmez: her şey kenardan girer, arenanın dışında çok kalmaz; üç ikram türü de ve kediler gelir; toplar sınırda. */
    @Test
    fun spawnsEnterFromTheEdgesAndLeaveTheScreen() {
        for (seed in 1L..6L) {
            val w = CiciWorld(seed)
            w.setLivesForTest(999)
            val kinds = HashSet<TreatKind>()
            var cats = 0
            var maxTreats = 0
            var maxCats = 0
            val seenIds = HashSet<Int>()
            repeat(60 * 90) {
                w.step()
                for (t in w.treats) {
                    assertTrue("ikram sınırda: ${t.x},${t.y}", t.x > -0.16f && t.x < CiciWorld.WIDTH + 0.16f && t.y > -0.16f && t.y < CiciWorld.HEIGHT + 0.16f)
                    if (seenIds.add(t.id)) {
                        kinds += t.kind
                        assertTrue("ikram kenardan girer: ${t.x},${t.y}", t.x <= 0f || t.x >= CiciWorld.WIDTH || t.y <= 0f || t.y >= CiciWorld.HEIGHT)
                    }
                }
                for (c in w.cats) {
                    assertTrue(c.y >= CiciWorld.CAT_R - 1e-4f && c.y <= CiciWorld.HEIGHT - CiciWorld.CAT_R + 1e-4f)
                    if (seenIds.add(c.id)) {
                        cats++
                        assertTrue("kedi yandan girer: ${c.x}", c.x <= 0f || c.x >= CiciWorld.WIDTH)
                    }
                }
                for (b in w.balls) {
                    assertTrue(b.x >= CiciWorld.BALL_R - 1e-4f && b.x <= CiciWorld.WIDTH - CiciWorld.BALL_R + 1e-4f)
                    assertTrue(b.y >= CiciWorld.BALL_R - 1e-4f && b.y <= CiciWorld.HEIGHT - CiciWorld.BALL_R + 1e-4f)
                }
                val expectBalls = if (w.seconds >= CiciWorld.BALL_DELAY) 1 else 0
                assertEquals("top sayısı ${w.seconds} s", expectBalls, w.balls.size)
                maxTreats = maxOf(maxTreats, w.treats.size)
                maxCats = maxOf(maxCats, w.cats.size)
            }
            assertEquals("tohum $seed: üç ikram türü de gelir", TreatKind.entries.toSet(), kinds)
            assertTrue("tohum $seed: kedi gelir ($cats)", cats >= 8)
            assertTrue("ekranı terk edenler silinir: $maxTreats ikram, $maxCats kedi", maxTreats < 40 && maxCats < 12)
            assertTrue("kıpırdamayan Cici puan kaybeder ama sıfırın altına inmez", w.score >= 0)
            assertEquals(CiciStatus.RUNNING, w.status)
        }
        val w = CiciWorld(3L)
        w.setLivesForTest(999)
        w.setSecondsForTest(CiciWorld.SECOND_BALL_AT)
        w.step()
        assertEquals("120 s'de ikinci top", CiciWorld.MAX_BALLS, w.balls.size)
    }

    /** Değişmez: pilot 12 tohumun en az 9'unda 100 puanı geçer, ortalama 45 s'den uzun uçar ve hareketsizlikten puan yitirmez. */
    @Test
    fun aPilotCanScoreAndSurvive() {
        val flights = (1L..12L).map { CiciBots.play(it, CiciBots.Pilot(0.12f, seed = it)) }
        val scored = flights.count { it.score >= 100 }
        assertTrue("100 puanı geçen uçuş: $scored / 12 — ${flights.map { it.score }}", scored >= 9)
        val avg = flights.map { it.seconds }.average()
        assertTrue("ortalama süre $avg s — ${flights.map { it.seconds.toInt() }}", avg >= 45.0)
        assertTrue(flights.all { it.lostPoints <= 3 })
        assertTrue("her koşuda üç ikram türü de yakalanır", flights.all { f -> TreatKind.entries.all { (f.caught[it] ?: 0) >= 1 } })
    }

    @Test
    fun hudSummarizesTheInitialState() {
        val h = CiciWorld(2L).hud()
        assertEquals(0, h.score)
        assertEquals(CiciWorld.LIVES, h.lives)
        assertEquals(Mood.CALM, h.mood)
        assertEquals(0, h.streak)
        assertEquals(0, h.bestStreak)
        assertEquals(0, h.seconds)
        assertEquals(0, h.caught)
        assertEquals(0f, h.idleSeconds, 0f)
        assertFalse(h.invulnerable)
        assertEquals(CiciStatus.RUNNING, h.status)
        assertEquals(1, h.facing)
        assertTrue(abs(CiciWorld(2L).ciciX - 0.5f) < 1e-6f)
    }
}
