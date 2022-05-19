import java.awt.Color

class Particle(
    val id: Long,
    val radius: Double,
    val color: Color,
    var ttl: Double,
    val position: Vector2D,
    val velocity: Vector2D,
    var enclosingTreeNode: ParticleQuadtree? = null,
) {

}