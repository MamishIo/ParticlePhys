import kotlin.random.Random

private val random = Random.Default

interface Range<out T> {
    fun next(): T
}

class FloatRange(private val a: Float, private val b: Float): Range<Float> {
    override fun next() = random.nextDouble(a.toDouble(), b.toDouble()).toFloat()
}

class DoubleRange(private val a: Double, private val b: Double): Range<Double> {
    override fun next() = random.nextDouble(a, b)
}