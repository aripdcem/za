package com.za.games.tavla

import kotlin.random.Random

/**
 * Sezgisel bilgisayar rakip: tüm yasal turları dener, sonuç tahtayı puanlar,
 * en iyisini seçer (eşitlikte tohumlu rastgele). Puanlama: yarış (pip),
 * kapılar ve zincirler, açık pullar, bar/hapis avantajı, toplama ilerlemesi.
 */
object TavlaAi {

    /** Kazanma olasılığı kestirimi: değerlendirme farkının lojistiği. */
    fun winChance(state: TavlaState, player: Int): Double {
        val own = evaluate(state.rules, state.points, state.bar, state.off, player)
        val other = evaluate(state.rules, state.points, state.bar, state.off, 1 - player)
        val x = (own - other) / 60.0
        return 1.0 / (1.0 + kotlin.math.exp(-x))
    }

    /** Zar atmadan önce katlama teklif etsin mi? Belirgin üstünlükte, ezici değilken. */
    fun wantsDouble(state: TavlaState): Boolean {
        if (!state.canDouble) return false
        val p = winChance(state, state.turn)
        return p >= 0.68 && p <= 0.93
    }

    /** Teklifi kabul etsin mi? Dörtte bir kazanma şansı yeterlidir. */
    fun acceptsDouble(state: TavlaState): Boolean {
        if (state.phase != Phase.DOUBLE_OFFERED) return false
        return winChance(state, state.responder) >= 0.25
    }

    fun chooseTurn(state: TavlaState, rng: Random = Random(state.rng)): List<Move> {
        if (state.phase != Phase.MOVING) return emptyList()
        val turns = TavlaLogic.legalTurns(state.rules, state.points, state.bar, state.off, state.turn, state.remaining)
        if (turns.isEmpty()) return emptyList()
        var best = emptyList<Move>()
        var bestScore = Double.NEGATIVE_INFINITY
        for (turn in turns) {
            var p = state.points
            var b = state.bar
            var o = state.off
            for (m in turn) {
                val a = TavlaLogic.apply(state.rules, p, b, o, state.turn, m)
                p = a.points
                b = a.bar
                o = a.off
            }
            val score = evaluate(state.rules, p, b, o, state.turn) + rng.nextDouble() * 0.01
            if (score > bestScore) {
                bestScore = score
                best = turn
            }
        }
        return best
    }

    fun evaluate(rules: TavlaRules, points: List<Point>, bar: List<Int>, off: List<Int>, player: Int): Double {
        val opp = 1 - player
        var score = 0.0
        if (off[player] == TavlaLogic.CHECKERS) return 100_000.0
        score += (TavlaLogic.pips(points, bar, opp) - TavlaLogic.pips(points, bar, player)).toDouble()
        score += off[player] * 12.0
        score -= off[opp] * 8.0

        val home = TavlaLogic.home(player)
        var streak = 0
        for (i in 0 until TavlaLogic.POINTS) {
            val point = points[i]
            val mine = point.owner == player && point.count > 0
            if (mine && point.count >= 2) {
                streak++
                score += 2.0 + streak * streak * 0.6 + (if (i in home) 2.5 else 0.0)
                if (point.count > 5) score -= (point.count - 5) * 0.8 // yığılma: esneklik kaybı
            } else {
                streak = 0
            }
            if (mine && point.count == 1 && !point.pinned) {
                // Açık pul: rakibin ulaşabileceği mesafedeyse ceza.
                if (reachableByOpponent(points, bar, player, i)) score -= if (rules.mode.pinning) 5.0 else 7.0
            }
            if (point.pinned) {
                val prisonerOwner = 1 - point.owner
                if (prisonerOwner == opp) {
                    // Rakibi hapsetmek, evine uzaklığı kadar değerli.
                    score += 6.0 + TavlaLogic.distance(opp, i) * 0.4 + (point.count - 1) * 1.5
                } else {
                    score -= 8.0 + TavlaLogic.distance(player, i) * 0.3
                }
            }
        }
        if (!rules.mode.pinning) {
            score += bar[opp] * 9.0
            val homePoints = home.count { points[it].owner == player && points[it].count >= 2 }
            score += bar[opp] * homePoints * 2.0
            score -= bar[player] * 6.0
        }
        return score
    }

    /** Rakibin herhangi bir pulu (bar dahil) bu haneye 1-12 pip içinden gelebilir mi? */
    private fun reachableByOpponent(points: List<Point>, bar: List<Int>, player: Int, index: Int): Boolean {
        val opp = 1 - player
        val dir = TavlaLogic.direction(opp)
        if (bar[opp] > 0) {
            for (die in 1..6) {
                val e = TavlaLogic.entry(opp, die)
                if (e == index) return true
                if ((index - e) * dir in 1..12) return true
            }
        }
        for (i in 0 until TavlaLogic.POINTS) {
            val point = points[i]
            val oppMovable = point.owner == opp && point.count > 0
            if (!oppMovable) continue
            val d = (index - i) * dir
            if (d in 1..12) return true
        }
        return false
    }
}
