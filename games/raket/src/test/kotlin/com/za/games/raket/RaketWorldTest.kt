package com.za.games.raket

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min
import kotlin.random.Random

class RaketWorldTest {

    private fun run(world: RaketWorld, frames: Int): List<RaketEvent> {
        val out = ArrayList<RaketEvent>()
        repeat(frames) { out += world.step() }
        return out
    }

    /** En çok [max] kare ilerler; [pred] biriken olaylar için doğru olunca durur. */
    private fun runUntil(world: RaketWorld, max: Int, pred: (List<RaketEvent>) -> Boolean): List<RaketEvent> {
        val out = ArrayList<RaketEvent>()
        repeat(max) {
            out += world.step()
            if (pred(out)) return out
        }
        throw AssertionError("beklenen durum $max karede oluşmadı; olaylar: $out")
    }

    /** Duvar modu: üst raket yok, bilgisayar yok; top ve alt raket elle konumlanır. */
    private fun court(seed: Long = 1L): RaketWorld = RaketWorld(seed, RaketMode.WALL)

    /** Topu alt raketin [above] birim üstüne, dikey aşağı inecek biçimde koyar. */
    private fun dropOn(w: RaketWorld, x: Float, speed: Float = RaketWorld.BASE_SPEED, above: Float = 0.05f) {
        w.placeForTest(x, w.contactY(Side.BOTTOM) - above, 0f, speed, speed)
    }

    private fun firstHit(ev: List<RaketEvent>): RaketEvent.PaddleHit = ev.filterIsInstance<RaketEvent.PaddleHit>().first()

    @Test
    fun sameSeedSameInputsSameRun() {
        val a = RaketWorld(11L, RaketMode.SOLO, 1)
        val b = RaketWorld(11L, RaketMode.SOLO, 1)
        val rng = Random(5)
        repeat(3600) {
            val dx = (rng.nextFloat() - 0.5f) * 0.06f
            a.move(Side.BOTTOM, dx)
            b.move(Side.BOTTOM, dx)
            assertEquals(a.step(), b.step())
        }
        assertEquals(a.ball.x, b.ball.x, 0f)
        assertEquals(a.ball.y, b.ball.y, 0f)
        assertEquals(a.scoreBottom, b.scoreBottom)
        assertEquals(a.scoreTop, b.scoreTop)
        assertTrue("bir dakikada sayı yazılmalı: ${a.scoreBottom}-${a.scoreTop}", a.scoreBottom + a.scoreTop >= 2)
        assertEquals(RaketWorld.dailySeed(20_000L), RaketWorld.dailySeed(20_000L))
        assertNotEquals(RaketWorld.dailySeed(20_000L), RaketWorld.dailySeed(20_001L))
    }

    @Test
    fun serveWaitsThenLaunchesTowardTheReceiver() {
        val w = RaketWorld(3L, RaketMode.SOLO, 0)
        assertEquals(RaketStatus.SERVING, w.status)
        assertEquals(Side.TOP, w.hud().serving)
        val frames = (RaketWorld.SERVE_DELAY / RaketWorld.STEP).toInt()
        val before = run(w, frames - 1)
        assertTrue(before.none { it is RaketEvent.Serve })
        assertEquals(0f, w.ball.vy, 0f)
        val ev = runUntil(w, 3) { it.any { e -> e is RaketEvent.Serve } }
        assertEquals(RaketEvent.Serve(Side.TOP), ev.last { it is RaketEvent.Serve })
        assertTrue("servis yukarı gitmeli", w.ball.vy < 0f)
        assertEquals(RaketWorld.BASE_SPEED, hypot(w.ball.vx, w.ball.vy), 1e-4f)
        assertTrue("servis açısı sınırda", abs(atan2(w.ball.vx, -w.ball.vy)) <= RaketWorld.SERVE_ANGLE + 1e-4f)
        assertNull(w.hud().serving)
        assertEquals(RaketStatus.RALLY, w.status)
    }

    @Test
    fun hitPointSetsTheAngleAndTheBallMissesBeyondTheEdge() {
        val w = court()
        dropOn(w, w.bottom.x)
        val centre = firstHit(runUntil(w, 10) { it.any { e -> e is RaketEvent.PaddleHit } })
        assertEquals(0f, centre.angle, 0.02f)
        assertTrue(w.ball.vy < 0f)
        assertEquals(0f, w.ball.vx, 0.02f)
        assertEquals(1, w.rally)

        val w2 = court()
        dropOn(w2, w2.bottom.right)
        val edge = firstHit(runUntil(w2, 10) { it.any { e -> e is RaketEvent.PaddleHit } })
        assertEquals(RaketWorld.MAX_ANGLE, edge.angle, 0.02f)
        assertTrue("sağ kenardan sağa gider", w2.ball.vx > 0f)
        assertTrue("yine yukarı gider", w2.ball.vy < 0f)

        val w3 = court()
        dropOn(w3, w3.bottom.left)
        val left = firstHit(runUntil(w3, 10) { it.any { e -> e is RaketEvent.PaddleHit } })
        assertEquals(-RaketWorld.MAX_ANGLE, left.angle, 0.02f)

        val w4 = court()
        dropOn(w4, w4.bottom.right + RaketWorld.BALL_R * RaketWorld.HIT_SLACK + 0.01f)
        val miss = runUntil(w4, 60) { it.any { e -> e is RaketEvent.Over } }
        assertTrue(miss.none { it is RaketEvent.PaddleHit })
        assertEquals(RaketEvent.Over(null), miss.last())
        assertEquals(RaketStatus.OVER, w4.status)
        assertTrue(w4.step().isEmpty())
    }

    @Test
    fun movingPaddleAddsSpin() {
        for (dir in listOf(1f, -1f)) {
            val w = court()
            w.placePaddleForTest(Side.BOTTOM, 0.5f)
            dropOn(w, 0.5f, above = 0.2f)
            // Raket vuruşa kadar aynı yönde sürüklenir (0,02 birim/kare = 1,2 birim/s);
            // top hep raket merkezine düşer ki açı yalnızca falsodan gelsin.
            var hit: RaketEvent.PaddleHit? = null
            var guard = 0
            while (hit == null && guard++ < 60) {
                w.move(Side.BOTTOM, dir * 0.02f)
                w.placeForTest(w.bottom.x, w.ball.y, 0f, w.ball.vy, w.speed)
                hit = w.step().filterIsInstance<RaketEvent.PaddleHit>().firstOrNull()
            }
            assertNotNull("yön $dir: vuruş olmalı", hit)
            assertEquals("yön $dir", dir * RaketWorld.SPIN_MAX, hit!!.angle, 0.06f)
            assertTrue(if (dir > 0f) w.ball.vx > 0f else w.ball.vx < 0f)
        }
        // Durgun raket: falso yok.
        val still = court()
        dropOn(still, still.bottom.x, above = 0.2f)
        assertEquals(0f, firstHit(runUntil(still, 30) { it.any { e -> e is RaketEvent.PaddleHit } }).angle, 0.01f)
    }

    @Test
    fun speedRampsPerHitAndCapsWhileTheWallPaddleShrinks() {
        val w = court()
        var expected = RaketWorld.BASE_SPEED
        repeat(40) { i ->
            dropOn(w, w.bottom.x, speed = w.speed)
            val hit = firstHit(runUntil(w, 10) { it.any { e -> e is RaketEvent.PaddleHit } })
            expected = min(RaketWorld.MAX_SPEED, expected * RaketWorld.SPEED_STEP)
            assertEquals("vuruş $i", expected, hit.speed, 1e-4f)
            assertEquals("vuruş $i top hızı", expected, hypot(w.ball.vx, w.ball.vy), 1e-3f)
        }
        assertEquals(RaketWorld.MAX_SPEED, w.speed, 0f)
        assertEquals(1f, w.hud().speed, 0f)
        assertEquals(40, w.hits)
        assertEquals(40, w.rally)
        assertEquals(40, w.bestRally)
        assertEquals(RaketWorld.PADDLE_W - 5 * RaketWorld.WALL_SHRINK, w.bottom.width, 1e-5f)
        assertTrue(w.bottom.width >= RaketWorld.WALL_MIN_W)
        // Duvar modunda tavan 0,13'ün altına inmez.
        repeat(200) {
            dropOn(w, w.bottom.x, speed = w.speed)
            runUntil(w, 10) { it.any { e -> e is RaketEvent.PaddleHit } }
        }
        assertEquals(RaketWorld.WALL_MIN_W, w.bottom.width, 1e-5f)
    }

    /** Değişmez: tavan hızda top bir adımda raket kalınlığından çok yol alır; yine de her vuruş yakalanır. */
    @Test
    fun noTunnelingAtTopSpeed() {
        val perStep = RaketWorld.MAX_SPEED * RaketWorld.STEP
        assertTrue("adım yolu raket kalınlığını aşmalı ki test anlamlı olsun", perStep > RaketWorld.PADDLE_H)
        var checked = 0
        for (k in 0..20) {
            for (j in 0..9) {
                val world = court()
                val half = world.bottom.width / 2f
                val x = world.bottom.x - half + (2f * half) * k / 20f
                val above = perStep * j / 10f + 0.001f
                world.placeForTest(x, world.contactY(Side.BOTTOM) - above, 0f, RaketWorld.MAX_SPEED, RaketWorld.MAX_SPEED)
                val ev = run(world, 3)
                assertTrue("x=$x above=$above: vuruş kaçtı", ev.any { it is RaketEvent.PaddleHit })
                assertTrue(world.ball.vy < 0f)
                assertTrue("top raketin üstünde kalmalı", world.ball.y + RaketWorld.BALL_R <= world.bottom.y - RaketWorld.PADDLE_H / 2f + 1e-4f)
                checked++
            }
        }
        assertEquals(210, checked)
    }

    @Test
    fun pointsAlternateTheServeAndTheMatchEndsAtElevenByTwo() {
        val w = RaketWorld(7L, RaketMode.DUO)
        // Raketler sola çekilir; top hep sağ şeritten kaçar.
        w.placePaddleForTest(Side.BOTTOM, 0.11f)
        w.placePaddleForTest(Side.TOP, 0.11f)
        var expectServe = w.serveTo

        fun score(scorer: Side): List<RaketEvent> {
            val up = scorer == Side.BOTTOM
            w.placeForTest(0.9f, RaketWorld.HEIGHT / 2f, 0f, if (up) -2f else 2f, 2f)
            val ev = runUntil(w, 120) { it.any { e -> e is RaketEvent.Point } }
            assertEquals(scorer, ev.filterIsInstance<RaketEvent.Point>().single().scorer)
            return ev
        }

        score(Side.BOTTOM)
        assertEquals(1, w.scoreBottom)
        assertEquals(0, w.scoreTop)
        assertEquals(RaketStatus.SERVING, w.status)
        expectServe = if (expectServe == Side.TOP) Side.BOTTOM else Side.TOP
        assertEquals(expectServe, w.serveTo)
        assertEquals(RaketWorld.BASE_SPEED, w.speed, 0f)
        assertEquals(RaketWorld.WIDTH / 2f, w.ball.x, 0f)
        assertEquals(0, w.rally)
        val serve = runUntil(w, 60) { it.any { e -> e is RaketEvent.Serve } }.filterIsInstance<RaketEvent.Serve>().single()
        assertEquals(expectServe, serve.toward)
        assertTrue(if (expectServe == Side.TOP) w.ball.vy < 0f else w.ball.vy > 0f)

        score(Side.TOP)
        expectServe = if (expectServe == Side.TOP) Side.BOTTOM else Side.TOP
        assertEquals(expectServe, w.serveTo)
        assertEquals(RaketEvent.Point(Side.TOP, 1, 1), RaketEvent.Point(Side.TOP, w.scoreBottom, w.scoreTop))

        // 10-10: iki sayı farkı gerekir.
        w.setScoreForTest(10, 10)
        score(Side.TOP)
        assertEquals(RaketStatus.SERVING, w.status)
        assertNull(w.winner)
        score(Side.BOTTOM)
        assertEquals(RaketStatus.SERVING, w.status)
        score(Side.BOTTOM)
        assertEquals(RaketStatus.SERVING, w.status)
        val last = score(Side.BOTTOM)
        assertEquals(RaketEvent.Over(Side.BOTTOM), last.last())
        assertEquals(RaketStatus.OVER, w.status)
        assertEquals(Side.BOTTOM, w.winner)
        assertEquals(13, w.scoreBottom)
        assertEquals(11, w.scoreTop)
        assertTrue(w.step().isEmpty())
        val x = w.bottom.x
        w.move(Side.BOTTOM, 0.2f)
        assertEquals("bitince raket kilitli", x, w.bottom.x, 0f)

        // Düz 11-9 da biter.
        val v = RaketWorld(8L, RaketMode.DUO)
        v.placePaddleForTest(Side.TOP, 0.11f)
        v.setScoreForTest(10, 9)
        v.placeForTest(0.9f, RaketWorld.HEIGHT / 2f, 0f, -2f, 2f)
        val ev = runUntil(v, 120) { it.any { e -> e is RaketEvent.Over } }
        assertEquals(RaketEvent.Over(Side.BOTTOM), ev.last())
    }

    @Test
    fun wallModeRalliesUntilTheBallGetsPast() {
        val w = court(5L)
        var hits = 0
        var guard = 0
        while (hits < 20 && guard++ < 6000) {
            if (w.status == RaketStatus.RALLY && w.ball.vy > 0f) {
                val target = w.predictX(w.contactY(Side.BOTTOM))
                w.move(Side.BOTTOM, (target - w.bottom.x).coerceIn(-0.05f, 0.05f))
            }
            for (e in w.step()) {
                if (e is RaketEvent.PaddleHit) hits++
                assertFalse("top kaçmamalı", e is RaketEvent.Over)
            }
        }
        assertEquals(20, hits)
        assertEquals(20, w.hits)
        assertEquals(20, w.rally)
        assertEquals(20, w.bestRally)
        assertEquals(RaketWorld.PADDLE_W - 2 * RaketWorld.WALL_SHRINK, w.bottom.width, 1e-5f)
        assertTrue(w.hud().speed > 0.5f)
        assertEquals(0, w.scoreBottom + w.scoreTop)

        // Raket topun geleceği yerden uzağa çekilir: top kaçar, koşu biter, skor kalır.
        var over: RaketEvent.Over? = null
        guard = 0
        while (over == null && guard++ < 1200) {
            if (w.ball.vy > 0f) {
                val away = if (w.predictX(w.contactY(Side.BOTTOM)) < 0.5f) 0.9f else 0.1f
                w.move(Side.BOTTOM, (away - w.bottom.x).coerceIn(-0.05f, 0.05f))
            }
            over = w.step().filterIsInstance<RaketEvent.Over>().firstOrNull()
        }
        assertEquals(RaketEvent.Over(null), over)
        assertEquals(20, w.bestRally)
        assertEquals(RaketStatus.OVER, w.status)
        assertNull(w.hud().winner)
    }

    @Test
    fun predictionFoldsOffTheSideWalls() {
        val w = court()
        // Top (0,9; 0,8)'den sağ-aşağı: 0,978'de sağ duvara çarpar, sonra sola döner.
        w.placeForTest(0.9f, 0.8f, 1f, 1f, hypot(1f, 1f))
        val targetY = w.contactY(Side.BOTTOM)
        val predicted = w.predictX(targetY)
        var guard = 0
        while (w.ball.y < targetY - 1e-3f && guard++ < 200) {
            // Raketi topun altından çeker ki vuruş olmasın; tahmin salt geometri.
            w.placePaddleForTest(Side.BOTTOM, if (predicted < 0.5f) 0.9f else 0.1f)
            w.step()
        }
        assertEquals("duvar katlaması", predicted, w.ball.x, 0.03f)
        assertTrue(predicted in RaketWorld.BALL_R..(RaketWorld.WIDTH - RaketWorld.BALL_R))
    }

    @Test
    fun movesStayInsideTheCourtAndOnlyDuoDrivesTheTopPaddle() {
        val solo = RaketWorld(1L, RaketMode.SOLO, 2)
        solo.move(Side.BOTTOM, 5f)
        assertEquals(RaketWorld.WIDTH - RaketWorld.PADDLE_W / 2f, solo.bottom.x, 1e-6f)
        solo.move(Side.BOTTOM, -5f)
        assertEquals(RaketWorld.PADDLE_W / 2f, solo.bottom.x, 1e-6f)
        val topX = solo.top.x
        solo.move(Side.TOP, 0.3f)
        assertEquals("SOLO'da üst raket bilgisayarın", topX, solo.top.x, 0f)
        val wall = court()
        assertFalse(wall.hasTopPaddle)
        wall.move(Side.TOP, 0.3f)
        assertEquals(topX, wall.top.x, 0f)
        val duo = RaketWorld(1L, RaketMode.DUO)
        assertTrue(duo.hasTopPaddle)
        assertNull(duo.ai)
        duo.move(Side.TOP, 0.3f)
        assertEquals(topX + 0.3f, duo.top.x, 1e-6f)
    }

    /**
     * Değişmez: orta seviye bir oyuncu botu kolay bilgisayarı çoğunlukla yener,
     * zora çoğunlukla yenilir; seviyeler güçte sıralıdır ve her maç biter.
     */
    @Test
    fun aiLevelsAreOrderedAndTheEasyOneIsBeatable() {
        val seeds = 1L..16L
        val rates = (0..RaketWorld.MAX_LEVEL).map { level ->
            val results = seeds.map { RaketBots.match(it, level, RaketBots.MEDIUM) }
            assertTrue("seviye $level: her maç bitmeli", results.all { it.winner != null })
            results.count { it.winner == Side.BOTTOM } / seeds.count().toFloat()
        }
        assertTrue("orta bot kolayı çoğunlukla yener: ${rates[0]}", rates[0] >= 0.7f)
        assertTrue("orta bot zora çoğunlukla yenilir: ${rates[2]}", rates[2] <= 0.35f)
        assertTrue("seviyeler sıralı: $rates", rates[0] >= rates[1] && rates[1] >= rates[2])
    }
}
