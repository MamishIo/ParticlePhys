import kotlin.math.pow
import kotlin.math.sqrt

class Vector2D(var x: Double, var y: Double) {

    fun addMult(v2: Vector2D, m: Double): Vector2D {
        x += v2.x * m
        y += v2.y * m
        return this
    }

    fun add(v2: Vector2D): Vector2D {
        x += v2.x
        y += v2.y
        return this
    }

    fun magnitude(): Double {
        return sqrt(x.pow(2) + y.pow(2))
    }

    override fun toString(): String {
        return String.format("(x=%4.3f,y=%4.3f)", x, y)
    }
}