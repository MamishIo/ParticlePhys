import java.awt.Color

// This is not a data class because it actually needs to be comparable by identity for tracking in list/tree.
// The default value-based equals/hashCode for data classes causes particle deletion to stop working.
class Particle(
    val id: Long,
    val radius: Double,
    val color: Color,
    var ttl: Double,
    val position: Vector2D,
    val velocity: Vector2D,
    var enclosingTreeNode: ParticleQuadtree
) {

    override fun toString(): String {
        return "Particle(id=$id, radius=$radius, color=$color, ttl=$ttl, position=$position, velocity=$velocity)"
    }
}
