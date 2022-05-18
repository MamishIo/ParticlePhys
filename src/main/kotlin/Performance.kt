import java.lang.System.nanoTime

const val FPS_AVERAGE_NUM_SAMPLES = 40
const val DEBUG_TIME_PRINT_ENABLED = false

var fpsSampleList: MutableList<Double> = MutableList(FPS_AVERAGE_NUM_SAMPLES) { 0.0 }
var lastTickNanos = nanoTime()

fun finishTickAndUpdatePerformanceStats() {
    val thisTickNanos = nanoTime()
    val deltaNanos = thisTickNanos - lastTickNanos
    lastTickNanos = thisTickNanos

    val instantFps = 1e9 / deltaNanos.toDouble()
    fpsSampleList.removeAt(0)
    fpsSampleList.add(instantFps)
}

fun debugReportTimeTaken(alias: String, timedAction: () -> Unit) {
    if (DEBUG_TIME_PRINT_ENABLED) {
        val t0 = nanoTime()
        timedAction.invoke()
        val dtMillis = (nanoTime() - t0) / 1e6
        println("Time to $alias = ${String.format("%3.3f", dtMillis)}")
    } else {
        timedAction.invoke()
    }
}

fun getAverageFps() = fpsSampleList.average()

fun getFpsRangePlusMinus() = (fpsSampleList.maxOrNull()!! - fpsSampleList.minOrNull()!!) / 2.0