package com.za.games.mines

/**
 * Oyuncunun görebildiği bilgiyle çalışan çözücü: mayınların yerini bilmez,
 * yalnızca açılmış sayılardan çıkarım yapar. Üretim bunu kullanarak tahtanın
 * **tahminsiz** çözülebildiğini garanti eder.
 *
 * İki kademe:
 * 1. Basit çıkarım — bir sayının komşularındaki mayınlar kesinleşmişse gerisi
 *    güvenlidir; kalan gizliler tam sayıyı tutuyorsa hepsi mayındır.
 * 2. Sınır sayımı — kalan belirsizlikte, sınır hücrelerinin tüm tutarlı
 *    yerleşimleri denenir; her yerleşimde boş kalan hücre kesin güvenlidir.
 *    Sayım üstel olduğu için kısıtlar bağımsız bileşenlere ayrılır.
 *
 * Bileşenlere ayırmak toplam mayın sayısı kısıtını göz ardı eder: bu yalnızca
 * bazı çıkarımları kaçırabilir, yanlış "güvenli" üretemez. Yani çözücü
 * temkinlidir — "tahminsiz" dediği tahta gerçekten tahminsizdir.
 */
internal object MinesSolver {

    /** Bileşen başına denenecek azami hücre; üstü atlanır (temkinli davranış). */
    private const val MAX_COMPONENT = 24

    /** Bir bileşende denenecek azami yerleşim; aşılırsa bileşen atlanır. */
    private const val MAX_ARRANGEMENTS = 200_000

    /**
     * [first] hücresinden başlayıp yalnızca mantıkla oynandığında tahta
     * tamamen açılabiliyor mu? Açılamıyorsa oyuncu bir noktada kör tahmin
     * yapmak zorundadır.
     */
    fun solvableWithoutGuessing(state: MinesState, first: Int): Boolean {
        var s = state.reveal(first)
        while (s.status == MinesStatus.RUNNING) {
            val safe = certainlySafe(s)
            if (safe.isEmpty()) return false
            for (i in safe) {
                if (s.status != MinesStatus.RUNNING) break
                if (i !in s.revealed) s = s.reveal(i)
            }
        }
        return s.status == MinesStatus.WON
    }

    /** Mantıkla kesin güvenli olduğu gösterilebilen açılmamış hücreler. */
    fun certainlySafe(s: MinesState): Set<Int> {
        val hidden = { i: Int -> i !in s.revealed }
        val safe = HashSet<Int>()

        // 1. Basit çıkarım.
        val knownMines = HashSet<Int>()
        var changed = true
        while (changed) {
            changed = false
            for (i in s.revealed) {
                val hiddenNeighbors = s.neighbors(i).filter(hidden)
                if (hiddenNeighbors.isEmpty()) continue
                val number = s.adjacentMines(i)
                val known = hiddenNeighbors.count { it in knownMines }
                val unknown = hiddenNeighbors.filter { it !in knownMines }
                if (unknown.isEmpty()) continue
                if (number == known) {
                    if (safe.addAll(unknown)) changed = true
                } else if (number - known == unknown.size) {
                    if (knownMines.addAll(unknown)) changed = true
                }
            }
        }
        if (safe.isNotEmpty()) return safe

        // 2. Sınır sayımı, bağımsız bileşenler hâlinde.
        val constraints = s.revealed
            .map { i -> s.neighbors(i).filter(hidden) to s.adjacentMines(i) }
            .filter { it.first.isNotEmpty() }
        if (constraints.isEmpty()) return emptySet()

        val parent = HashMap<Int, Int>()
        fun find(a: Int): Int {
            var k = a
            while (parent[k] != k) {
                parent[k] = parent[parent[k]]!!
                k = parent[k]!!
            }
            return k
        }
        for ((cells, _) in constraints) for (c in cells) parent.putIfAbsent(c, c)
        for ((cells, _) in constraints) {
            val root = find(cells.first())
            for (c in cells.drop(1)) parent[find(c)] = root
        }

        for ((_, component) in parent.keys.groupBy { find(it) }) {
            if (component.size > MAX_COMPONENT) continue
            val order = component.toList()
            val position = order.withIndex().associate { (k, v) -> v to k }
            val related = constraints.filter { (cells, _) -> cells.any { it in position } }
            val assignment = BooleanArray(order.size)
            val canBeMine = BooleanArray(order.size)
            var count = 0

            fun consistent(upTo: Int): Boolean = related.all { (cells, number) ->
                var fixed = 0
                var open = 0
                for (c in cells) {
                    val j = position[c]
                    if (j == null) open++              // başka bileşende: serbest
                    else if (j < upTo) { if (assignment[j]) fixed++ } else open++
                }
                fixed <= number && fixed + open >= number
            }

            fun search(k: Int) {
                if (count > MAX_ARRANGEMENTS) return
                if (k == order.size) {
                    count++
                    for (j in order.indices) if (assignment[j]) canBeMine[j] = true
                    return
                }
                for (value in booleanArrayOf(false, true)) {
                    assignment[k] = value
                    if (consistent(k + 1)) search(k + 1)
                }
                assignment[k] = false
            }
            search(0)
            if (count == 0 || count > MAX_ARRANGEMENTS) continue
            for (j in order.indices) if (!canBeMine[j]) safe += order[j]
        }
        return safe
    }
}
