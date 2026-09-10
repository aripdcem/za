package com.za.games.tuse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class TuseWorldTest {

    private fun classic(seed: Long = 1L, song: Song = Songs.ODE, total: Int = 20): TuseWorld =
        TuseWorld(seed, TuseMode.CLASSIC, song, total)

    private fun runUntil(world: TuseWorld, max: Int, pred: (List<TuseEvent>) -> Boolean): List<TuseEvent> {
        val out = ArrayList<TuseEvent>()
        repeat(max) {
            out += world.step()
            if (pred(out)) return out
        }
        throw AssertionError("beklenen durum $max karede oluşmadı; olaylar: $out")
    }

    @Test
    fun lanesAreDeterministicAndRarelyRepeat() {
        for (seed in 1L..20L) {
            val a = TuseWorld(seed, TuseMode.ARCADE, Songs.ODE)
            val b = TuseWorld(seed, TuseMode.ARCADE, Songs.ODE)
            val la = (0 until 400).map { a.lane(it) }
            val lb = (0 until 400).map { b.lane(it) }
            assertEquals(la, lb)
            assertTrue(la.all { it in 0 until TuseWorld.LANES })
            val repeats = (1 until 400).count { la[it] == la[it - 1] }
            assertTrue("tohum $seed tekrar sayısı: $repeats", repeats in 20..100)
            assertEquals("her şerit kullanılır", TuseWorld.LANES, la.toSet().size)
        }
        assertEquals(TuseWorld.dailySeed(1L), TuseWorld.dailySeed(1L))
        assertNotEquals(TuseWorld.dailySeed(1L), TuseWorld.dailySeed(2L))
    }

    @Test
    fun classicHitsAdvanceAndFinishWithTheTime() {
        val w = classic(total = 20)
        assertEquals(0L, w.elapsedMs(5_000L))
        assertFalse(w.started)
        assertEquals(20, w.hud(0L).total)
        var now = 1_000L
        for (i in 0 until 20) {
            val ev = w.tap(w.lane(i), now)
            if (i == 0) assertEquals(TuseEvent.Started, ev.first())
            assertTrue("vuruş $i: $ev", ev.contains(TuseEvent.Hit(i, w.lane(i), w.note(i))))
            if (i < 19) assertEquals(TuseStatus.RUNNING, w.status) else assertEquals(TuseEvent.Done, ev.last())
            now += 150L
        }
        assertEquals(TuseStatus.DONE, w.status)
        assertEquals(20, w.tapped)
        assertTrue(w.isComplete)
        assertEquals(150L * 19, w.elapsedMs(99_999L))
        assertEquals(150L * 19, w.hud(99_999L).elapsedMs)
        assertTrue("bitince dokunuş etkisiz", w.tap(w.lane(20), now).isEmpty())
        assertTrue(w.step().isEmpty())
    }

    @Test
    fun wrongLaneEndsTheRun() {
        val w = classic()
        val right = w.lane(0)
        val wrong = (right + 1) % TuseWorld.LANES
        val ev = w.tap(wrong, 500L)
        assertEquals(listOf(TuseEvent.Started, TuseEvent.Miss(wrong, right)), ev)
        assertEquals(TuseStatus.OVER, w.status)
        assertEquals(wrong, w.missLane)
        assertEquals(0, w.tapped)
        assertEquals(0L, w.elapsedMs(9_000L))
        assertTrue(w.step().isEmpty())
        assertTrue(w.tap(right, 600L).isEmpty())
        // Geçersiz şerit yok sayılır.
        val v = classic()
        assertTrue(v.tap(-1, 0L).isEmpty())
        assertTrue(v.tap(TuseWorld.LANES, 0L).isEmpty())
        assertEquals(TuseStatus.RUNNING, v.status)
    }

    @Test
    fun classicBoardSlidesOneRowPerHitAndNeverOvershoots() {
        val w = classic()
        assertEquals(0f, w.scroll, 0f)
        w.tap(w.lane(0), 0L)
        w.tap(w.lane(1), 10L)
        var frames = 0
        while (w.scroll < 2f && frames < 60) {
            w.step()
            frames++
            assertTrue("kayma hedefi aşmaz: ${w.scroll}", w.scroll <= 2f)
        }
        assertEquals(2f, w.scroll, 1e-6f)
        assertTrue("2 satır ≈ 8 kare: $frames", frames in 7..9)
        repeat(10) { w.step() }
        assertEquals(2f, w.scroll, 0f)
        assertEquals(2, w.tapped)
        assertEquals("kayma 2: karo 0 çıktı, karo 1 sınırda", 1, w.firstVisible())
    }

    @Test
    fun arcadeScrollsAfterTheFirstTapSpeedsUpAndEndsWhenATilePasses() {
        val w = TuseWorld(3L, TuseMode.ARCADE, Songs.TWINKLE)
        repeat(30) { w.step() }
        assertEquals("başlamadan akmaz", 0f, w.scroll, 0f)
        assertEquals(-1, w.hud(0L).total)
        assertEquals(TuseWorld.ARCADE_BASE, w.speed, 1e-6f)
        w.tap(w.lane(0), 0L)
        val s0 = w.speed
        assertEquals(TuseWorld.ARCADE_BASE + TuseWorld.ARCADE_GAIN, s0, 1e-6f)
        repeat(30) { w.step() }
        assertEquals("yarım saniyede hızın yarısı kadar satır", s0 / 2f, w.scroll, 0.01f)
        assertEquals(TuseStatus.RUNNING, w.status)
        var guard = 0
        while (w.speed < TuseWorld.ARCADE_MAX && guard++ < 1000) w.tap(w.lane(w.tapped), 0L)
        assertEquals(TuseWorld.ARCADE_MAX, w.speed, 0f)
        assertEquals(TuseStatus.RUNNING, w.status)
        val next = w.lane(w.tapped)
        val ev = runUntil(w, 6000) { it.any { e -> e is TuseEvent.Passed } }
        assertEquals(TuseEvent.Passed(next), ev.last())
        assertEquals(TuseStatus.OVER, w.status)
        assertEquals(next, w.passedLane)
        assertEquals(-1, w.missLane)
        assertTrue(w.scroll >= w.tapped + 1f)
        assertTrue(w.elapsedMs(0L) > 0L)
    }

    @Test
    fun arcadeTileCanBeTappedUntilItFullyLeaves() {
        val w = TuseWorld(4L, TuseMode.ARCADE, Songs.ODE)
        w.tap(w.lane(0), 0L)
        while (w.scroll < 1.9f) w.step()
        assertEquals(TuseStatus.RUNNING, w.status)
        val ev = w.tap(w.lane(1), 500L)
        assertTrue(ev.any { it is TuseEvent.Hit })
        assertEquals(2, w.tapped)
        assertEquals("kayma 1,9: karo 0 hâlâ kısmen görünür", 0, w.firstVisible())
    }

    @Test
    fun abandonEndsARunningRunOnly() {
        val w = classic()
        w.tap(w.lane(0), 100L)
        w.abandon(700L)
        assertEquals(TuseStatus.OVER, w.status)
        assertEquals(-1, w.missLane)
        assertEquals(600L, w.elapsedMs(5_000L))
        val d = classic(total = 1)
        d.tap(d.lane(0), 0L)
        d.abandon(9L)
        assertEquals(TuseStatus.DONE, d.status)
    }

    @Test
    fun songsAreValidAndNotesCycle() {
        assertEquals(Songs.ALL.size, Songs.ALL.map { it.id }.toSet().size)
        for (s in Songs.ALL) {
            assertTrue(s.id, s.notes.size >= 24)
            assertTrue(s.id, s.notes.all { it in NoteSynth.MIN_MIDI..NoteSynth.MAX_MIDI })
            assertTrue("${s.id}: en az 5 farklı nota", s.notes.distinct().size >= 5)
            for (i in 1 until s.notes.size) {
                assertTrue("${s.id} ${i}. nota bir oktavdan uzağa atlar", abs(s.notes[i] - s.notes[i - 1]) <= 12)
            }
        }
        val w = TuseWorld(1L, TuseMode.CLASSIC, Songs.BIRTHDAY, total = 60)
        for (i in 0 until 60) assertEquals(Songs.BIRTHDAY.notes[i % Songs.BIRTHDAY.notes.size], w.note(i))
        for (d in 0L..20L) assertTrue(Songs.ofDay(d) in Songs.ALL)
        assertEquals(Songs.ofDay(0L), Songs.ofDay(Songs.ALL.size.toLong()))
        assertNotEquals(Songs.ofDay(0L), Songs.ofDay(1L))
        assertEquals(Songs.ODE, Songs.byId("yok"))
        assertEquals(Songs.MINUET, Songs.byId("minuet"))
    }

    @Test
    fun synthProducesTheRightPitchAndAValidWav() {
        for (midi in listOf(48, 60, 69, 84, 96)) {
            val pcm = NoteSynth.pcm(midi)
            assertEquals((NoteSynth.SAMPLE_RATE * NoteSynth.DURATION).toInt(), pcm.size)
            val peak = pcm.maxOf { abs(it.toInt()) }
            assertTrue("$midi tepe $peak", peak in 6_000..32_767)
            // 100–300 ms arası sıfır geçişleri: harmonikler sönmüş, temel frekans okunur.
            val from = NoteSynth.SAMPLE_RATE / 10
            val to = NoteSynth.SAMPLE_RATE * 3 / 10
            var crossings = 0
            for (i in from + 1 until to) if ((pcm[i - 1] < 0) != (pcm[i] < 0)) crossings++
            val estimated = crossings / 2.0 / ((to - from).toDouble() / NoteSynth.SAMPLE_RATE)
            val expected = NoteSynth.frequency(midi)
            assertEquals("$midi frekans", expected, estimated, expected * 0.03)
            val wav = NoteSynth.wav(pcm)
            assertEquals(44 + pcm.size * 2, wav.size)
            assertEquals("RIFF", String(wav, 0, 4, Charsets.US_ASCII))
            assertEquals("WAVE", String(wav, 8, 4, Charsets.US_ASCII))
            assertEquals("data", String(wav, 36, 4, Charsets.US_ASCII))
            val dataLen = (wav[40].toInt() and 0xFF) or ((wav[41].toInt() and 0xFF) shl 8) or ((wav[42].toInt() and 0xFF) shl 16) or ((wav[43].toInt() and 0xFF) shl 24)
            assertEquals(pcm.size * 2, dataLen)
            assertEquals(pcm[0], ((wav[44].toInt() and 0xFF) or (wav[45].toInt() shl 8)).toShort())
        }
        assertEquals(440.0, NoteSynth.frequency(69), 1e-9)
        assertEquals(261.63, NoteSynth.frequency(60), 0.01)
    }
}
