package com.za.games.viraj

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan
import kotlin.random.Random

/**
 * Viraj simülasyonu: sözde-3D yolda süreye karşı sürüş. Sabit 1/60 s adım;
 * aynı tohum + aynı giriş dizisi = aynı koşu. Birimler dünya birimidir
 * ([SEGMENT_LENGTH] uzunluğunda parçalar); yanal konumlar yol yarı
 * genişliği kesridir (−1..1 asfalt, ötesi toprak).
 *
 * Kurallar: gaz otomatiktir, [steer] ile direksiyon, [brake] ile fren. Yol
 * dışı yavaşlatır ve kenar nesnelerine çarpmak hızı keser. Daha yavaş bir
 * rakibe çarpmak hızı düşürür ve geriye iter; rakibi geçmek puan verir. Her
 * [CHECKPOINT_EVERY] parçada kontrol noktası süre ekler; süre bitince koşu
 * biter. Eşyalar: turbo şeridi, yağ (kayma), koni (yavaşlama), sarı kutu
 * (turbo, kalkan ya da süre).
 */
class VirajWorld(val seed: Long) {

    companion object {
        const val STEP = 1f / 60f
        const val SEGMENT_LENGTH = 200f
        const val ROAD_WIDTH = 2000f
        const val CAMERA_HEIGHT = 1000f
        const val FOV_DEGREES = 100f
        val CAMERA_DEPTH: Float = 1f / tan((FOV_DEGREES / 2f) * PI.toFloat() / 180f)

        /** Oyuncu aracının kameradan uzaklığı; oyuncunun bulunduğu parça buna göre bulunur. */
        val PLAYER_Z: Float = CAMERA_HEIGHT * CAMERA_DEPTH

        const val MAX_SPEED = SEGMENT_LENGTH * 60f
        const val ACCEL = MAX_SPEED / 5f
        const val BRAKE = -MAX_SPEED
        const val OFF_ROAD_DECEL = -MAX_SPEED / 2f
        const val OFF_ROAD_LIMIT = MAX_SPEED / 4f
        const val CENTRIFUGAL = 0.3f
        const val PLAYER_WIDTH = 0.3f
        const val CAR_WIDTH = 0.3f
        const val ITEM_WIDTH = 0.34f
        const val MAX_X = 2.2f

        const val START_TIME = 40f
        const val MAX_TIME = 60f
        const val CHECKPOINT_EVERY = 600
        const val CHECKPOINT_BONUS = 16f
        const val TURBO_TIME = 2.5f
        const val TURBO_FACTOR = 1.25f
        const val SLIP_TIME = 1.2f
        const val CRASH_COOLDOWN = 0.6f
        const val TIME_GIFT = 5f
        const val OVERTAKE_POINTS = 50L
        const val CHECKPOINT_POINTS = 200L
        const val PICKUP_POINTS = 25L
        const val METERS_PER_UNIT = 1f / 40f
        const val KMH_AT_MAX = 240
        const val MIN_CARS = 8
        const val MAX_CARS = 14
        const val BEHIND_SEGMENTS = 40
        const val SPAWN_MIN_SEGMENTS = 120
        const val SPAWN_RANGE_SEGMENTS = 260

        fun dailySeed(epochDay: Long): Long = VirajTrack.mix(epochDay, 0x56, 0x52)
    }

    val track = VirajTrack(seed)
    private val rng = Random(VirajTrack.mix(seed, 0x7A, 0x21))
    private val events = ArrayList<VirajEvent>()

    /** Kamera konumu (yol boyunca); oyuncu aracı [PLAYER_Z] kadar öndedir. */
    var position = 0f
        private set
    var playerX = 0f
        private set
    var speed = 0f
        private set
    var timeLeft = START_TIME
        private set
    var status = VirajStatus.RUNNING
        private set
    var score = 0L
        private set
    var overtakes = 0
        private set
    var checkpoints = 0
        private set
    var shield = false
        private set
    var turboT = 0f
        private set
    var slipT = 0f
        private set
    var frames = 0
        private set

    /** Giriş: −1 sol, 0 düz, 1 sağ. */
    var steer = 0
    var brake = false

    val cars = ArrayList<Car>()

    private var crashCooldown = 0f
    private var slipDrift = 0f
    private var lastKm = 0
    private var lastCheckpointSegment = 0

    init {
        repeat(MIN_CARS) { i ->
            val ahead = 40 + i * 45 + rng.nextInt(30)
            cars += Car(ahead * SEGMENT_LENGTH, laneX(), carSpeed(), rng.nextInt(6))
        }
    }

    val playerZ: Float get() = position + PLAYER_Z
    val playerSegmentIndex: Int get() = segmentIndexOf(playerZ)
    val meters: Int get() = (position * METERS_PER_UNIT).toInt()
    val kmh: Int get() = (speed / MAX_SPEED * KMH_AT_MAX).toInt()

    fun segmentIndexOf(z: Float): Int = max(0, floor(z / SEGMENT_LENGTH).toInt())

    fun hud(): VirajHud {
        val next = (playerSegmentIndex / CHECKPOINT_EVERY + 1) * CHECKPOINT_EVERY
        val progress = 1f - (next - playerSegmentIndex) / CHECKPOINT_EVERY.toFloat()
        return VirajHud(
            score = score,
            kmh = kmh,
            timeLeft = timeLeft,
            meters = meters,
            overtakes = overtakes,
            checkpoints = checkpoints,
            shield = shield,
            turbo = (turboT / TURBO_TIME).coerceIn(0f, 1f),
            slip = (slipT / SLIP_TIME).coerceIn(0f, 1f),
            checkpointProgress = progress.coerceIn(0f, 1f),
            status = status,
        )
    }

    /** Bir kare ilerletir; bu karede olan olayları döndürür. */
    fun step(): List<VirajEvent> {
        events.clear()
        if (status != VirajStatus.RUNNING) return emptyList()
        frames++
        val dt = STEP
        val before = playerSegmentIndex
        val seg = track.segment(before)
        val speedPct = speed / MAX_SPEED
        val dx = dt * 2f * speedPct

        // Direksiyon ve merkezkaç; yağda direksiyon ters ve araç kayar.
        val dir = if (slipT > 0f) -steer else steer
        playerX += dir * dx
        playerX -= dx * speedPct * seg.curve * CENTRIFUGAL
        if (slipT > 0f) playerX += slipDrift * dt
        playerX = playerX.coerceIn(-MAX_X, MAX_X)

        // Hız: fren, turbo ya da gaz.
        val maxNow = if (turboT > 0f) MAX_SPEED * TURBO_FACTOR else MAX_SPEED
        speed = when {
            brake -> speed + BRAKE * dt
            turboT > 0f -> maxNow
            else -> speed + ACCEL * dt
        }
        if (abs(playerX) > 1f) {
            if (speed > OFF_ROAD_LIMIT) speed += OFF_ROAD_DECEL * dt
            for (sprite in seg.sprites) {
                if (VirajTrack.overlap(playerX, PLAYER_WIDTH, sprite.x, sprite.kind.width)) {
                    hitObstacle(OFF_ROAD_LIMIT * 0.5f)
                    break
                }
            }
        }
        speed = speed.coerceIn(0f, maxNow)
        position += speed * dt

        updateCars(dt)
        collideCars()

        // Geçilen parçalar: eşyalar, kontrol noktaları, kilometre taşları.
        val after = playerSegmentIndex
        for (i in (before + 1)..after) {
            val s = track.segment(i)
            s.item?.let { item ->
                if (!item.taken && VirajTrack.overlap(playerX, PLAYER_WIDTH, item.x, ITEM_WIDTH, 0.9f)) {
                    item.taken = true
                    pickUp(item, i)
                }
            }
            if (s.checkpoint && i > lastCheckpointSegment) {
                lastCheckpointSegment = i
                checkpoints++
                val bonus = CHECKPOINT_BONUS - min(4f, 4f * VirajTrack.difficulty(i))
                timeLeft = min(MAX_TIME, timeLeft + bonus)
                score += CHECKPOINT_POINTS
                events += VirajEvent.Checkpoint(checkpoints, bonus.toInt())
                spawnMore(i)
            }
        }
        score += (after - before).toLong()
        val km = meters / 1000
        if (km > lastKm) {
            lastKm = km
            events += VirajEvent.Milestone(km)
        }

        // Sayaçlar.
        turboT = max(0f, turboT - dt)
        slipT = max(0f, slipT - dt)
        crashCooldown = max(0f, crashCooldown - dt)
        timeLeft -= dt
        if (timeLeft <= 0f) {
            timeLeft = 0f
            status = VirajStatus.OVER
            events += VirajEvent.Over
        }
        return events.toList()
    }

    private fun hitObstacle(newSpeed: Float) {
        if (crashCooldown > 0f) {
            speed = min(speed, newSpeed)
            return
        }
        crashCooldown = CRASH_COOLDOWN
        if (shield) {
            shield = false
            events += VirajEvent.ShieldUsed
            return
        }
        speed = min(speed, newSpeed)
        events += VirajEvent.Crash
    }

    private fun pickUp(item: Item, segment: Int) {
        when (item.kind) {
            ItemKind.TURBO -> {
                turboT = TURBO_TIME
                score += PICKUP_POINTS
                events += VirajEvent.Pickup(ItemKind.TURBO, null)
            }
            ItemKind.OIL -> {
                slipT = SLIP_TIME
                slipDrift = (if (playerX >= 0f) 1f else -1f) * 0.9f
                speed *= 0.85f
                events += VirajEvent.Slip
            }
            ItemKind.CONE -> {
                speed *= 0.65f
                events += VirajEvent.Cone
            }
            ItemKind.BOX -> {
                val gift = BoxGift.entries[Random(VirajTrack.mix(seed, segment, 0x60)).nextInt(BoxGift.entries.size)]
                when (gift) {
                    BoxGift.TURBO -> turboT = TURBO_TIME
                    BoxGift.SHIELD -> shield = true
                    BoxGift.TIME -> timeLeft = min(MAX_TIME, timeLeft + TIME_GIFT)
                }
                score += PICKUP_POINTS
                events += VirajEvent.Pickup(ItemKind.BOX, gift)
            }
        }
    }

    // -----------------------------------------------------------------------
    // Rakipler
    // -----------------------------------------------------------------------

    private fun laneX(): Float = -0.8f + rng.nextFloat() * 1.6f

    private fun carSpeed(): Float {
        val d = VirajTrack.difficulty(playerSegmentIndex)
        return min(0.9f * MAX_SPEED, MAX_SPEED * (0.28f + rng.nextFloat() * 0.42f) * (1f + 0.3f * d))
    }

    private fun spawnMore(segment: Int) {
        val target = min(MAX_CARS, MIN_CARS + (6f * VirajTrack.difficulty(segment)).toInt())
        while (cars.size < target) {
            val car = Car(0f, 0f, 0f, rng.nextInt(6))
            respawn(car)
            cars += car
        }
    }

    private fun respawn(car: Car) {
        val ahead = SPAWN_MIN_SEGMENTS + rng.nextInt(SPAWN_RANGE_SEGMENTS)
        car.z = playerZ + ahead * SEGMENT_LENGTH
        car.x = laneX()
        car.speed = carSpeed()
        car.passed = false
    }

    private fun updateCars(dt: Float) {
        val playerIndex = playerSegmentIndex
        for (car in cars) {
            val carIndex = segmentIndexOf(car.z)
            car.x = (car.x + carOffset(car, carIndex, playerIndex)).coerceIn(-0.85f, 0.85f)
            car.z += car.speed * dt
            if (car.z < playerZ) {
                if (!car.passed) {
                    car.passed = true
                    overtakes++
                    score += OVERTAKE_POINTS
                    events += VirajEvent.Overtake(overtakes)
                }
                if (car.z < playerZ - BEHIND_SEGMENTS * SEGMENT_LENGTH) respawn(car)
            } else if (car.passed) {
                car.passed = false // yeniden öne geçti; tekrar sollamak yine puan
            }
        }
    }

    /** Rakip yapay zekâsı: öndeki daha yavaş araçtan ve oyuncudan kaçınır, kenardan içeri döner. */
    private fun carOffset(car: Car, carIndex: Int, playerIndex: Int): Float {
        val lookahead = 20
        val d = playerIndex - carIndex
        if (d in 1..lookahead && car.speed > speed && VirajTrack.overlap(playerX, PLAYER_WIDTH, car.x, CAR_WIDTH, 1.2f)) {
            val dir = if (playerX > car.x) -1f else 1f
            return dir / lookahead * (car.speed - speed) / MAX_SPEED
        }
        for (other in cars) {
            if (other === car) continue
            val gap = segmentIndexOf(other.z) - carIndex
            if (gap in 1..lookahead && car.speed > other.speed && VirajTrack.overlap(car.x, CAR_WIDTH, other.x, CAR_WIDTH, 1.2f)) {
                val dir = when {
                    other.x > 0.5f -> -1f
                    other.x < -0.5f -> 1f
                    car.x > other.x -> 1f
                    else -> -1f
                }
                return dir / lookahead * (car.speed - other.speed) / MAX_SPEED
            }
        }
        return when {
            car.x < -0.7f -> 0.004f
            car.x > 0.7f -> -0.004f
            else -> 0f
        }
    }

    private fun collideCars() {
        for (car in cars) {
            if (abs(car.z - playerZ) > SEGMENT_LENGTH) continue
            if (speed <= car.speed) continue
            if (!VirajTrack.overlap(playerX, PLAYER_WIDTH, car.x, CAR_WIDTH, 0.8f)) continue
            if (crashCooldown > 0f) {
                speed = min(speed, car.speed)
                position = car.z - SEGMENT_LENGTH * 0.6f - PLAYER_Z
                continue
            }
            crashCooldown = CRASH_COOLDOWN
            if (shield) {
                shield = false
                events += VirajEvent.ShieldUsed
                // Kalkan çarpışmayı yumuşatır: rakibin hızına iner, geriye itilmez.
                speed = max(car.speed, speed * 0.9f)
                continue
            }
            speed = car.speed * 0.6f
            position = car.z - SEGMENT_LENGTH * 0.6f - PLAYER_Z
            events += VirajEvent.Crash
            break
        }
    }

    // -----------------------------------------------------------------------
    // Test kancaları
    // -----------------------------------------------------------------------

    internal fun addCarForTest(aheadSegments: Float, x: Float, speed: Float): Car {
        val car = Car(playerZ + aheadSegments * SEGMENT_LENGTH, x, speed, 0)
        cars += car
        return car
    }

    internal fun clearCarsForTest() = cars.clear()

    internal fun setSpeedForTest(value: Float) {
        speed = value
    }

    internal fun grantShieldForTest() {
        shield = true
    }

    /** Oyuncuyu verilen parçanın başına taşır (kamera konumu buna göre ayarlanır). */
    internal fun jumpToSegmentForTest(index: Int) {
        position = index * SEGMENT_LENGTH - PLAYER_Z + 1f
        lastKm = meters / 1000
    }
}
