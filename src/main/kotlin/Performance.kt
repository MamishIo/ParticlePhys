import MetricType.RATE
import MetricType.TIME
import java.lang.System.nanoTime
import kotlin.system.measureNanoTime

// const val DEBUG_TIME_PRINT_ENABLED = true

enum class MetricType {
    TIME, RATE
}

val countersToDraw = mutableListOf<PerformanceCounter>()
private fun makeCounter(name: String, preferredMetric: MetricType, drawOnHud: Boolean, numSamples: Int = 60): PerformanceCounter {
    return PerformanceCounter(name, preferredMetric, numSamples).also { if (drawOnHud) countersToDraw.add(it) }
}

val UPDATE_CYCLE_COUNTER = makeCounter("UpdateCycle", RATE, true)
val UPDATE_CYCLE_INNER_COUNTER = makeCounter("UpdateCycle.Inner", TIME, true)

val SIMULATE_PHYSICS_COUNTER = makeCounter("SimulatePhysics", TIME, true)
val SIMULATE_PHYSICS_MOVE_COUNTER = makeCounter("SimulatePhysics.Move", TIME, true)
val SIMULATE_PHYSICS_LEAF_PRUNE_COUNTER = makeCounter("SimulatePhysics.LeafPrune", TIME, true)
val SIMULATE_PHYSICS_RE_ADD_COUNTER = makeCounter("SimulatePhysics.ReAdd", TIME, true)
val SIMULATE_PHYSICS_RESIZE_COUNTER = makeCounter("SimulatePhysics.Resize", TIME, true)
val SIMULATE_PHYSICS_SPAWN_PARTICLES_COUNTER = makeCounter("SimulatePhysics.Spawn", TIME, false)
val SIMULATE_PHYSICS_DETECT_COLLIDE_COUNTER = makeCounter("SimulatePhysics.DetectCollide", TIME, true)
val SIMULATE_PHYSICS_RESOLVE_COLLISIONS_COUNTER = makeCounter("SimulatePhysics.ResolveCollisions", TIME, true)

val DRAW_PARTICLES_COUNTER = makeCounter("DrawParticles", TIME, true)
val DRAW_HUD_COUNTER = makeCounter("DrawHud", TIME, true)

val CANVAS_REPAINT_COUNTER = makeCounter("CanvasRepaint", TIME, false)

class PerformanceCounter(private val name: String, private val preferredMetric: MetricType, numSamples: Int) {
    private val timeList = MutableList(numSamples) { 0.0 }
    private val rateList = MutableList(numSamples) { 0.0 }
    private var lastTick = nanoTime()

    fun time(action: () -> Unit) {
        val millisTaken = measureNanoTime(action) / 1e6
        addTime(millisTaken)
    }

    fun addTimeTick() {
        val now = nanoTime()
        addTime((now - lastTick) / 1e6)
        lastTick = now
    }

    fun addTime(millis: Double) {
        timeList.removeAt(0)
        timeList.add(millis)
        rateList.removeAt(0)
        rateList.add(if (millis != 0.0) 1000.0 / millis else 1.0) // Not the best way to handle div-by-zero, but it will do
    }

    fun getAverageTime() = timeList.average()
    fun getAverageRate() = rateList.average()
    fun getTimeRangePlusMinus() = (timeList.maxOrNull()!! - timeList.minOrNull()!!) / 2.0
    fun getRateRangePlusMinus() = (rateList.maxOrNull()!! - rateList.minOrNull()!!) / 2.0

    fun getMetricString() = when (preferredMetric) {
        TIME -> getTimeString()
        RATE -> getRateString()
    }

    fun getTimeString() = formatPlusMinusString(getAverageTime(), getTimeRangePlusMinus())
    fun getRateString() = formatPlusMinusString(getAverageRate(), getRateRangePlusMinus())

    private fun formatPlusMinusString(value: Double, plusMinus: Double) =
        "$name.$preferredMetric: ${String.format("%3.3f", value)} +- ${String.format("%2.2f", plusMinus)}"
}
