import java.lang.System.nanoTime
import kotlin.system.measureNanoTime

// const val DEBUG_TIME_PRINT_ENABLED = true

enum class MetricType {
    TIME, RATE
}

val UPDATE_CYCLE_COUNTER = PerformanceCounter("UpdateCycle", MetricType.RATE)
val SIMULATE_PARTICLES_COUNTER = PerformanceCounter("SimulateParticles", MetricType.TIME)
val SPAWN_PARTICLES_COUNTER = PerformanceCounter("SpawnParticles", MetricType.TIME)
val FIND_COLLISIONS_COUNTER = PerformanceCounter("FindCollisions", MetricType.TIME)
val DRAW_PARTICLES_COUNTER = PerformanceCounter("DrawParticles", MetricType.TIME)
val DRAW_HUD_COUNTER = PerformanceCounter("DrawHud", MetricType.TIME)
val CANVAS_REPAINT_COUNTER = PerformanceCounter("CanvasRepaint", MetricType.TIME)

class PerformanceCounter(private val name: String, private val preferredMetric: MetricType, numSamples: Int = 60) {
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
        rateList.add(if (millis != 0.0) 1000.0/millis else 1.0) // Not the best way to handle div-by-zero, but it will do
    }

    fun getAverageTime() = timeList.average()
    fun getAverageRate() = rateList.average()
    fun getTimeRangePlusMinus() = (timeList.maxOrNull()!! - timeList.minOrNull()!!) / 2.0
    fun getRateRangePlusMinus() = (rateList.maxOrNull()!! - rateList.minOrNull()!!) / 2.0

    fun getMetricString() = when (preferredMetric) {
        MetricType.TIME -> getTimeString()
        MetricType.RATE -> getRateString()
    }
    fun getTimeString() = formatPlusMinusString(getAverageTime(), getTimeRangePlusMinus())
    fun getRateString() = formatPlusMinusString(getAverageRate(), getRateRangePlusMinus())

    private fun formatPlusMinusString(value: Double, plusMinus: Double) =
        "$name.$preferredMetric: ${String.format("%3.3f", value)} +- ${String.format("%2.2f", plusMinus)}"
}
