import java.awt.Color
import java.awt.image.ColorModel
import kotlin.math.*
import kotlin.random.Random.Default as random

// Different data structures for different traversals: list for fast exactly-once iteration, and tree for at-least-once spatial traversal
val particleList = ArrayList<Particle>()
var particleTree: ParticleQuadtree = ParticleQuadtree.Leaf(null, Vector2D(0.0, 0.0), Vector2D(HORIZONTAL_BOUND.toDouble(), VERTICAL_BOUND.toDouble()), 0)
var particlesToSpawnAccumulator = 0.0

// TODO Just clean up Performance.kt and this timing code all over

fun simulationTick(timeDelta: Double) {
    debugReportTimeTaken("simulate") { simulateAllParticles(timeDelta) }
    debugReportTimeTaken("spawn") { spawnNewParticles(timeDelta) }
}

fun simulateAllParticles(timeDelta: Double) {
    val iterator = particleList.iterator()
    while (iterator.hasNext()) {
        val p = iterator.next()
        p.ttl -= timeDelta
        if (p.ttl <= 0.0) {
            iterator.remove()
            p.enclosingTreeNode.removeParticle(p)
        } else {
            simulateParticle(p, timeDelta)
        }
    }
}

fun simulateParticle(particle: Particle, timeDelta: Double) {
    simulateParticleMotion(particle, timeDelta)
    relocateParticleInTree(particle)
    particleTree = particleTree.resizeTree()
}

fun simulateParticleMotion(particle: Particle, timeDelta: Double) {
    particle.run {
        velocity.addMult(GLOBAL_GRAVITY, timeDelta)
        position.addMult(velocity, timeDelta)
        // This repetition sucks, figure out how to make it better
        if (position.x < radius) {
            velocity.x = abs(velocity.x)
        }
        if (position.x > HORIZONTAL_BOUND - radius) {
            velocity.x = -abs(velocity.x)
        }
        if (position.y < radius) {
            velocity.y = abs(velocity.y)
        }
        if (position.y > VERTICAL_BOUND - radius) {
            velocity.y = -abs(velocity.y)
        }
    }
}

fun relocateParticleInTree(p: Particle) {
        // If enclosing node is no longer completely enclosing, move up the tree to find a bigger node that does enclose this
        while (!p.enclosingTreeNode.enclosesParticle(p) && p.enclosingTreeNode.parent != null) {
            p.enclosingTreeNode = p.enclosingTreeNode.parent!!
        }
        // Recursively remove the particle from the enclosing node and all sub-trees and re-add it to refresh its position
        // Because this is scoped to the enclosing node, it usually will only need to traverse a fraction of the tree
        p.enclosingTreeNode.removeParticle(p)
        p.enclosingTreeNode.addParticleIfTouching(p)
}

fun spawnNewParticles(timeDelta: Double) {
    particlesToSpawnAccumulator += PARTICLE_SPAWN_RATE.next() * timeDelta
    while (particlesToSpawnAccumulator >= 1.0) {
        val p = randomParticle()
        particleList.add(p)
        particleTree.addParticleIfTouching(p)
        particlesToSpawnAccumulator--
    }
}

fun randomParticle(): Particle {
    val id = (random.nextLong() - 1).absoluteValue
    val radius = PARTICLE_RADIUS_RANGE.next()
    val color = randomColor()
    val ttl = PARTICLE_LIFETIME_RANGE.next()
    //val position = listOf(Vector2D(960.0, 540.0), Vector2D(480.0, 270.0), Vector2D(480.0, 540.0)).random()
    val position = Vector2D(squareWeightedRandom(HORIZONTAL_BOUND), squareWeightedRandom(VERTICAL_BOUND))
    val velocityMagnitude = PARTICLE_START_VELOCITY_RANGE.next()
    //val velocityMagnitude = 300.0
    val velocityDirection = random.nextDouble(PI * 2.0)
    val velocity = Vector2D(velocityMagnitude * cos(velocityDirection), velocityMagnitude * sin(velocityDirection))

    return Particle(id, radius, color, ttl, position, velocity, particleTree)
}

fun squareWeightedRandom(max: Int) = random.nextDouble().pow(2) * max

fun randomColor(): Color {
    val rgb = Color.HSBtoRGB(PARTICLE_HUE_RANGE.next(), 1f, 1f)
    with(ColorModel.getRGBdefault()) {
        val alpha = if (PRETTY_DRAWING_MODE) (PARTICLE_ALPHA * 255).toInt() else 255
        return Color(getRed(rgb), getGreen(rgb), getBlue(rgb), alpha)
    }
}
