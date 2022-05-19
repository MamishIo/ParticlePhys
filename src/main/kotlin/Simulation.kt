import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.ColorModel
import kotlin.math.*
import kotlin.random.Random.Default as random

// Different data structures for different traversals: list for fast exactly-once iteration, and tree for at-least-once spatial traversal
val particleList = ArrayList<Particle>()
var particleTree: ParticleQuadtree = ParticleQuadtree.Leaf(null, Vector2D(0.0, 0.0), Vector2D(HORIZONTAL_BOUND.toDouble(), VERTICAL_BOUND.toDouble()), 0)
var particlesToSpawnAccumulator = 0.0

data class ParticlePair(val p0: Particle, val p1: Particle)
val collisionsMap = mutableMapOf<ParticlePair,Boolean>()

// TODO Just clean up Performance.kt and this timing code all over

fun simulationTick(timeDelta: Double) {
    debugReportTimeTaken("simulate") { simulateAllParticles(timeDelta) }
    debugReportTimeTaken("spawn") { spawnNewParticles(timeDelta) }
    debugReportTimeTaken("findAllCollisions") { findAllCollisions() }
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

fun findAllCollisions() {
    collisionsMap.clear()
    // Sort by radius first (collision check maths requires (small,large) order), then ID just for consistency (likely unnecessary)
    val compareByParticleId = compareBy<Particle> { it.id }

    particleTree.getLeafIterator().forEach { leaf ->
        val plist = leaf.particles.toList()
        if (plist.size >= 2) { // Skip if only 1 particle (causes indexing errors)
            plist.subList(0, plist.size - 1).forEachIndexed { i, pi ->
                plist.subList(i, plist.size).forEach { pj ->
                    val pLowerId = minOf(pi, pj, compareByParticleId)
                    val pHigherId = maxOf(pi, pj, compareByParticleId)
                    collisionsMap.computeIfAbsent(ParticlePair(pLowerId, pHigherId)) { particlesAreColliding(it.p0, it.p1) }
                }
            }
        }
    }
}

fun particlesAreColliding(p0: Particle, p1: Particle): Boolean {
    val x0 = p0.position.x
    val y0 = p0.position.y
    val x1 = p1.position.x
    val y1 = p1.position.y
    val r01 = p0.radius + p1.radius

    return x0 in (x1 - r01)..(x1 + r01)
            && y0 in (y1 - r01)..(y1 + r01)
            && sqrt((x1 - x0).pow(2) + (y1 - y0).pow(2)) < r01
}

fun drawCollisionLines(graphics: Graphics2D) {
    val defaultStroke = BasicStroke(1.0f)
    val bigStroke = BasicStroke(2.5f)
    collisionsMap.forEach { (pair, colliding) ->
        graphics.color = if (colliding) Color.RED else Color.BLUE
        graphics.stroke = if (colliding) bigStroke else defaultStroke
        graphics.drawLine(
            pair.p0.position.x.roundToInt(), pair.p0.position.y.roundToInt(),
            pair.p1.position.x.roundToInt(), pair.p1.position.y.roundToInt()
        )
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
