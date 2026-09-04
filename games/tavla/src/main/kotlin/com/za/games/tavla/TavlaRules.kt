package com.za.games.tavla

/**
 * Oyun türleri. [pinning] açıksa pullar kırılmaz, hapsedilir; [tapaSetup]
 * açıksa 15 pulun tamamı 24. haneden başlar, yoksa klasik 2-5-3-5 dizilişi.
 */
enum class TavlaMode(val pinning: Boolean, val tapaSetup: Boolean) {
    KLASIK(false, false),
    TAPA(true, true),
    HAPIS(true, false),
}

/**
 * Maç kuralları; [target] maç puanı, [noPinInOpponentHome] rakibin evinde
 * hapsetme yasağı, [cube] katlama küpü (2-4-8-16-32-64; Crawford kuralı yok).
 */
data class TavlaRules(
    val mode: TavlaMode,
    val target: Int = 5,
    val noPinInOpponentHome: Boolean = false,
    val cube: Boolean = false,
)

/**
 * Bir hane: üstteki pulların sahibi ve sayısı; [pinned] ise altta rakibin
 * hapsedilmiş tek pulu vardır (yalnız hapis kurallarında).
 */
data class Point(val owner: Int = -1, val count: Int = 0, val pinned: Boolean = false) {
    val isEmpty: Boolean get() = count == 0
}

/** Tek pul hamlesi: [from] [BAR] ya da 0..23, [to] 0..23 ya da [OFF]; [die] kullanılan zar. */
data class Move(val from: Int, val to: Int, val die: Int) {
    companion object {
        const val BAR = -1
        const val OFF = 24
    }
}

/** Hedef haneye inişin sonucu. */
enum class Landing { EMPTY, OWN, HIT, PIN }

/**
 * Kurallar: yön, ev, iniş, hamle uygulama ve yasal tur üretimi. Oyuncu 0
 * dizinleri azaltarak (24 → 1) oynar, evi 0..5; oyuncu 1 artırarak, evi 18..23.
 */
object TavlaLogic {
    const val POINTS = 24
    const val CHECKERS = 15

    fun direction(player: Int): Int = if (player == 0) -1 else 1

    fun home(player: Int): IntRange = if (player == 0) 0..5 else 18..23

    /** Bar'dan giriş hanesi. */
    fun entry(player: Int, die: Int): Int = if (player == 0) POINTS - die else die - 1

    /** Oyuncunun bir hanedeki pulunun evine uzaklığı (pip). */
    fun distance(player: Int, index: Int): Int = if (player == 0) index + 1 else POINTS - index

    fun initialPoints(mode: TavlaMode): List<Point> {
        val points = MutableList(POINTS) { Point() }
        if (mode.tapaSetup) {
            points[23] = Point(0, CHECKERS)
            points[0] = Point(1, CHECKERS)
        } else {
            points[23] = Point(0, 2)
            points[12] = Point(0, 5)
            points[7] = Point(0, 3)
            points[5] = Point(0, 5)
            points[0] = Point(1, 2)
            points[11] = Point(1, 5)
            points[16] = Point(1, 3)
            points[18] = Point(1, 5)
        }
        return points
    }

    fun landing(rules: TavlaRules, points: List<Point>, player: Int, to: Int): Landing? {
        val point = points[to]
        return when {
            point.isEmpty -> Landing.EMPTY
            point.owner == player -> Landing.OWN
            point.count >= 2 -> null
            !rules.mode.pinning -> Landing.HIT
            point.pinned -> null // hapsedeni hapsetmek yok
            rules.noPinInOpponentHome && to in home(1 - player) -> null
            else -> Landing.PIN
        }
    }

    /** Oyuncunun tahtadaki (bar dahil) tüm pulları evinde mi? Hapisteki kendi pulları da sayılır. */
    fun allInHome(points: List<Point>, bar: List<Int>, player: Int): Boolean {
        if (bar[player] > 0) return false
        val home = home(player)
        points.forEachIndexed { i, point ->
            if (i in home) return@forEachIndexed
            if (point.owner == player && point.count > 0) return false
            if (point.pinned && point.owner == 1 - player) return false
        }
        return true
    }

    /** [from] hanesinden daha uzakta (eve göre) oyuncunun pulu var mı? Fazla zarla toplama için. */
    private fun hasCheckerFartherThan(points: List<Point>, player: Int, from: Int): Boolean {
        val d = distance(player, from)
        points.forEachIndexed { i, point ->
            if (distance(player, i) <= d) return@forEachIndexed
            if (point.owner == player && point.count > 0) return true
            if (point.pinned && point.owner == 1 - player) return true
        }
        return false
    }

    /** Tek zar için tek pul hamleleri. */
    fun singleMoves(rules: TavlaRules, points: List<Point>, bar: List<Int>, player: Int, die: Int): List<Move> {
        val moves = ArrayList<Move>()
        if (bar[player] > 0) {
            val to = entry(player, die)
            if (landing(rules, points, player, to) != null) moves += Move(Move.BAR, to, die)
            return moves
        }
        val dir = direction(player)
        val canBearOff = allInHome(points, bar, player)
        for (from in 0 until POINTS) {
            val point = points[from]
            if (point.owner != player || point.count == 0) continue
            val to = from + dir * die
            if (to in 0 until POINTS) {
                if (landing(rules, points, player, to) != null) moves += Move(from, to, die)
            } else if (canBearOff) {
                val exact = distance(player, from) == die
                if (exact || !hasCheckerFartherThan(points, player, from)) moves += Move(from, Move.OFF, die)
            }
        }
        return moves
    }

    /** Hamleyi uygular; yeni haneler ve bar döner. */
    fun apply(rules: TavlaRules, points: List<Point>, bar: List<Int>, off: List<Int>, player: Int, move: Move): Applied {
        val p = points.toMutableList()
        val b = bar.toMutableList()
        val o = off.toMutableList()
        val opp = 1 - player
        if (move.from == Move.BAR) {
            b[player] = b[player] - 1
        } else {
            val src = p[move.from]
            p[move.from] = when {
                src.count == 1 && src.pinned -> Point(opp, 1, false) // mahkûm serbest
                src.count == 1 -> Point()
                else -> src.copy(count = src.count - 1)
            }
        }
        if (move.to == Move.OFF) {
            o[player] = o[player] + 1
        } else {
            val dst = p[move.to]
            p[move.to] = when (landing(rules, p, player, move.to)) {
                Landing.EMPTY -> Point(player, 1, false)
                Landing.OWN -> dst.copy(count = dst.count + 1)
                Landing.HIT -> {
                    b[opp] = b[opp] + 1
                    Point(player, 1, false)
                }
                Landing.PIN -> Point(player, 1, pinned = true)
                null -> error("Yasadışı hamle: $move")
            }
        }
        return Applied(p, b, o)
    }

    data class Applied(val points: List<Point>, val bar: List<Int>, val off: List<Int>)

    /**
     * Zarlarla oynanabilecek tüm yasal turlar. Kural: mümkün olan en çok zar
     * oynanır; tek zar oynanabiliyorsa büyük olan tercih edilir.
     */
    fun legalTurns(rules: TavlaRules, points: List<Point>, bar: List<Int>, off: List<Int>, player: Int, dice: List<Int>): List<List<Move>> {
        val all = LinkedHashSet<List<Move>>()
        fun search(p: List<Point>, b: List<Int>, o: List<Int>, rest: List<Int>, prefix: List<Move>) {
            var extended = false
            for (die in rest.distinct()) {
                for (move in singleMoves(rules, p, b, player, die)) {
                    extended = true
                    val next = apply(rules, p, b, o, player, move)
                    val remaining = rest.toMutableList().also { it.remove(die) }
                    if (next.off[player] == CHECKERS) {
                        all += prefix + move // oyun bitti: tur burada kapanır
                    } else {
                        search(next.points, next.bar, next.off, remaining, prefix + move)
                    }
                }
            }
            if (!extended && prefix.isNotEmpty()) all += prefix
        }
        search(points, bar, off, dice, emptyList())
        if (all.isEmpty()) return emptyList()
        val longest = all.maxOf { it.size }
        var best = all.filter { it.size == longest }
        if (longest == 1 && dice.size == 2 && dice[0] != dice[1]) {
            val larger = maxOf(dice[0], dice[1])
            if (best.any { it[0].die == larger }) best = best.filter { it[0].die == larger }
        }
        return best
    }

    fun pips(points: List<Point>, bar: List<Int>, player: Int): Int {
        var total = bar[player] * (POINTS + 1)
        points.forEachIndexed { i, point ->
            val d = distance(player, i)
            if (point.owner == player) total += d * point.count
            if (point.pinned && point.owner == 1 - player) total += d
        }
        return total
    }
}
