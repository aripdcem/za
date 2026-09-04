package com.za.games.tavla

enum class Phase { OPENING, TO_ROLL, DOUBLE_OFFERED, MOVING, GAME_OVER, MATCH_OVER }

/** Bir tur içinde geri almak için hamle öncesi anlık görüntü. */
data class Snapshot(val points: List<Point>, val bar: List<Int>, val off: List<Int>, val remaining: List<Int>)

/**
 * Değişmez tavla durumu; her işlem yeni durum döndürür. Zarlar [rng]
 * sayacından deterministik üretilir: aynı tohum + aynı hamleler = aynı maç.
 * Tur akışı: OPENING (açılış zarı) → MOVING → TO_ROLL (rakip) → …;
 * pullarını toplayan oyunu, hedefe ulaşan maçı bitirir.
 */
data class TavlaState(
    val rules: TavlaRules,
    val points: List<Point>,
    val bar: List<Int> = listOf(0, 0),
    val off: List<Int> = listOf(0, 0),
    val turn: Int = 0,
    val phase: Phase = Phase.OPENING,
    val dice: List<Int> = emptyList(),
    val remaining: List<Int> = emptyList(),
    val played: List<Move> = emptyList(),
    val snapshots: List<Snapshot> = emptyList(),
    val scores: List<Int> = listOf(0, 0),
    val game: Int = 1,
    val winner: Int? = null,
    val mars: Boolean = false,
    /** Açılış zarları (oyuncu 0, oyuncu 1); gösterim için. */
    val openingDice: List<Int> = emptyList(),
    /** Turun sonunda rakibe geçerken "hamle yok" bilgisi (arayüz mesajı). */
    val passed: Boolean = false,
    /** Küp değeri (1 = katlanmamış) ve sahibi; null = ortada, ikisi de katlayabilir. */
    val cubeValue: Int = 1,
    val cubeOwner: Int? = null,
    /** Oyun pes ile bittiyse (küp teklifi reddedildi). */
    val resigned: Boolean = false,
    /** Karşılıklı kilitlenmeyle bittiyse: pip sayısı az olan kazanır, eşitse berabere ([winner] null). */
    val deadlock: Boolean = false,
    val rng: Long = 0L,
) {
    /** Sıradaki oyuncu zar atmadan önce katlama teklif edebilir mi? */
    val canDouble: Boolean
        get() = rules.cube && phase == Phase.TO_ROLL && cubeValue < 64 && (cubeOwner == null || cubeOwner == turn)

    /** Katlama teklifini yanıtlayacak oyuncu (teklif beklerken). */
    val responder: Int get() = opponent

    fun offerDouble(): TavlaState = if (canDouble) copy(phase = Phase.DOUBLE_OFFERED) else this

    /** Rakip kabul eder: küp ikiye katlanır ve kabul edene geçer; teklif eden zar atar. */
    fun acceptDouble(): TavlaState {
        if (phase != Phase.DOUBLE_OFFERED) return this
        return copy(phase = Phase.TO_ROLL, cubeValue = cubeValue * 2, cubeOwner = responder)
    }

    /** Rakip pes eder: teklif eden küp değeri kadar puan alır (mars sayılmaz). */
    fun declineDouble(): TavlaState {
        if (phase != Phase.DOUBLE_OFFERED) return this
        return finishGame(gain = cubeValue, isMars = false, resigned = true)
    }
    val current: Int get() = turn
    val opponent: Int get() = 1 - turn

    /** Şu anki tur için oynanabilir sonraki tek hamleler. */
    fun legalMoves(): List<Move> {
        if (phase != Phase.MOVING || remaining.isEmpty()) return emptyList()
        return TavlaLogic.legalTurns(rules, points, bar, off, turn, remaining).map { it.first() }.distinct()
    }

    /** Bu turda hiç hamle var mı? */
    val canMove: Boolean get() = legalMoves().isNotEmpty()

    fun pips(player: Int): Int = TavlaLogic.pips(points, bar, player)

    /** Açılış: her oyuncu bir zar atar, büyük olan iki zarla başlar; eşitlikte tekrar. */
    fun openingRoll(): TavlaState {
        if (phase != Phase.OPENING) return this
        var state = this
        var d0: Int
        var d1: Int
        do {
            val (a, s1) = state.nextDie()
            val (b, s2) = s1.nextDie()
            d0 = a
            d1 = b
            state = s2
        } while (d0 == d1)
        val starter = if (d0 > d1) 0 else 1
        val dice = listOf(maxOf(d0, d1), minOf(d0, d1))
        return state.copy(
            turn = starter,
            phase = Phase.MOVING,
            dice = dice,
            remaining = dice,
            played = emptyList(),
            snapshots = emptyList(),
            openingDice = listOf(d0, d1),
            passed = false,
        )
    }

    /** Sıradaki oyuncu zar atar; çift zar dört hamle sayılır. */
    fun roll(): TavlaState {
        if (phase != Phase.TO_ROLL) return this
        val (a, s1) = nextDie()
        val (b, s2) = s1.nextDie()
        val dice = listOf(maxOf(a, b), minOf(a, b))
        val remaining = if (a == b) listOf(a, a, a, a) else dice
        return s2.copy(phase = Phase.MOVING, dice = dice, remaining = remaining, played = emptyList(), snapshots = emptyList(), passed = false)
    }

    /** Tek pul hamlesi; yasal değilse durum değişmez. Oynanacak zar kalmayınca tur rakibe geçer. */
    fun move(from: Int, to: Int): TavlaState {
        if (phase != Phase.MOVING) return this
        // Aynı hedefe birden çok zarla gidilebiliyorsa (yalnız toplamada olur) küçük zar harcanır.
        val move = legalMoves().filter { it.from == from && it.to == to }.minByOrNull { it.die } ?: return this
        val applied = TavlaLogic.apply(rules, points, bar, off, turn, move)
        val rest = remaining.toMutableList().also { it.remove(move.die) }
        var next = copy(
            points = applied.points,
            bar = applied.bar,
            off = applied.off,
            remaining = rest,
            played = played + move,
            snapshots = snapshots + Snapshot(points, bar, off, remaining),
        )
        if (applied.off[turn] == TavlaLogic.CHECKERS) return next.finishGame()
        if (!next.canMove) next = next.endTurn()
        return next
    }

    /** Bu turdaki son hamleyi geri alır. */
    fun undo(): TavlaState {
        if (phase != Phase.MOVING || snapshots.isEmpty()) return this
        val last = snapshots.last()
        return copy(
            points = last.points,
            bar = last.bar,
            off = last.off,
            remaining = last.remaining,
            played = played.dropLast(1),
            snapshots = snapshots.dropLast(1),
        )
    }

    /** Oyuncunun hiçbir zarla oynayabileceği tek bir hamle bile yok mu? */
    fun isStuck(player: Int): Boolean =
        (1..6).all { die -> TavlaLogic.singleMoves(rules, points, bar, player, die).isEmpty() }

    /**
     * Turu bitirir (zar kalmadıysa ya da hamle yoksa). Pas geçen oyuncu ve rakibi
     * hiçbir zarla oynayamıyorsa (yalnız hapis kurallarında olur) oyun kilitlenmiştir:
     * pip sayısı az olan kazanır, eşitlikte berabere.
     */
    fun endTurn(): TavlaState {
        if (phase != Phase.MOVING) return this
        if (played.isEmpty() && isStuck(turn) && isStuck(opponent)) return finishDeadlock()
        return copy(
            turn = opponent,
            phase = Phase.TO_ROLL,
            dice = emptyList(),
            remaining = emptyList(),
            played = emptyList(),
            snapshots = emptyList(),
            passed = played.isEmpty(),
        )
    }

    private fun finishGame(): TavlaState {
        val isMars = off[opponent] == 0
        return finishGame(gain = cubeValue * (if (isMars) 2 else 1), isMars = isMars, resigned = false)
    }

    private fun finishDeadlock(): TavlaState {
        val p0 = pips(0)
        val p1 = pips(1)
        if (p0 == p1) {
            return copy(
                phase = Phase.GAME_OVER,
                winner = null,
                mars = false,
                deadlock = true,
                dice = emptyList(),
                remaining = emptyList(),
                snapshots = emptyList(),
            )
        }
        val better = if (p0 < p1) 0 else 1
        return copy(turn = better).finishGame(gain = cubeValue, isMars = false, resigned = false).copy(deadlock = true)
    }

    private fun finishGame(gain: Int, isMars: Boolean, resigned: Boolean): TavlaState {
        val newScores = scores.toMutableList().also { it[turn] = it[turn] + gain }
        val matchOver = newScores[turn] >= rules.target
        return copy(
            phase = if (matchOver) Phase.MATCH_OVER else Phase.GAME_OVER,
            winner = turn,
            mars = isMars,
            resigned = resigned,
            scores = newScores,
            dice = emptyList(),
            remaining = emptyList(),
            snapshots = emptyList(),
        )
    }

    /** Maç sürüyorsa yeni oyun; skor ve tohum sayacı korunur, küp ortaya döner. */
    fun nextGame(): TavlaState {
        if (phase != Phase.GAME_OVER) return this
        return TavlaState(rules = rules, points = TavlaLogic.initialPoints(rules.mode), scores = scores, game = game + 1, rng = rng)
    }

    /** splitmix64 tabanlı zar; durum sayacı ilerler. */
    private fun nextDie(): Pair<Int, TavlaState> {
        var z = rng + -0x61C8864680B583EBL
        val next = z
        z = (z xor (z ushr 30)) * -0x40A7B892E31B1A47L
        z = (z xor (z ushr 27)) * -0x6B2FB644ECCEEE15L
        z = z xor (z ushr 31)
        val die = ((z ushr 33) % 6).toInt() + 1
        return die to copy(rng = next)
    }

    companion object {
        fun newMatch(rules: TavlaRules, seed: Long): TavlaState =
            TavlaState(rules = rules, points = TavlaLogic.initialPoints(rules.mode), rng = seed)
    }
}
