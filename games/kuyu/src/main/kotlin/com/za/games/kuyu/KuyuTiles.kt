package com.za.games.kuyu

/** Kuyu hücresi. [WALL] kırılmaz; [BLOCK] ve [GEM_BLOCK] mermiyle kırılır. */
enum class Tile(val solid: Boolean, val breakable: Boolean) {
    EMPTY(false, false),
    WALL(true, false),
    BLOCK(true, true),

    /** Kırılınca taş bırakan blok. */
    GEM_BLOCK(true, true),
}

/**
 * Düşman türleri. [stompable] olmayanın üstüne basan oyuncu hasar alır.
 * Boyutlar kare (hücre) birimindedir.
 */
enum class EnemyKind(
    val hp: Int,
    val gems: Int,
    val stompable: Boolean,
    val speed: Float,
    val w: Float,
    val h: Float,
) {
    /** Çıkıntıda devriye gezen topak. */
    BLOB(1, 3, true, 1.6f, 0.8f, 0.7f),

    /** Havada yatay uçan, hafifçe salınan yarasa. */
    BAT(1, 4, true, 2.6f, 0.8f, 0.6f),

    /** Üstüne basılamayan dikenli; yalnız mermiyle ölür. */
    SPIKY(2, 6, false, 0.9f, 0.85f, 0.75f),

    /** Duvar boyunca aşağı yukarı tırmanan. */
    CRAWLER(2, 5, true, 2.8f, 0.7f, 0.8f),
}

/** Bir parçanın düşman doğum noktası; satır dünya satırıdır, hücre boştur. */
data class KuyuSpawn(val kind: EnemyKind, val row: Int, val col: Int)

/** Kuyunun [KuyuGen.CHUNK_ROWS] satırlık bir parçası; kırılan bloklar yerinde güncellenir. */
class KuyuChunk(
    val index: Int,
    private val tiles: Array<Tile>,
    val spawns: List<KuyuSpawn>,
) {
    init {
        require(tiles.size == KuyuGen.CHUNK_ROWS * KuyuGen.WIDTH) { "Parça boyutu yanlış" }
    }

    fun tile(localRow: Int, col: Int): Tile = tiles[localRow * KuyuGen.WIDTH + col]

    fun set(localRow: Int, col: Int, tile: Tile) {
        tiles[localRow * KuyuGen.WIDTH + col] = tile
    }
}
