import java.awt.Graphics2D
import kotlin.math.roundToInt

const val MAX_TREE_HEIGHT = 6
const val MAX_PARTICLES_TO_ATTEMPT_SUBDIVIDE = 4

sealed class ParticleQuadtree(val parent: ParticleQuadtree?, val position: Vector2D, val size: Vector2D, val depth: Int) {

    class Branch(parent: ParticleQuadtree?, position: Vector2D, size: Vector2D, depth: Int): ParticleQuadtree(parent, position, size, depth) {
        // Order: NE, NW, SW, SE (if this ever becomes semantically important, should denote it in the type somehow
        var quadrants: Array<ParticleQuadtree> = arrayOf(
            makeQuadrant(true, false),
            makeQuadrant(false, false),
            makeQuadrant(false, true),
            makeQuadrant(true, true)
        )

        private fun makeQuadrant(shiftx: Boolean, shifty: Boolean): Leaf {
            val halfw = size.x / 2
            val halfh = size.y / 2
            val shift = Vector2D(if (shiftx) halfw else 0.0, if (shifty) halfh else 0.0)
            return Leaf(this, shift.add(position), Vector2D(halfw, halfh), depth + 1)
        }
    }

    class Leaf(parent: ParticleQuadtree?, position: Vector2D, size: Vector2D, depth: Int): ParticleQuadtree(parent, position, size, depth) {
        val particles = mutableSetOf<Particle>()
    }

//    fun getParticleIterator(): MutableIterator<Particle> {
//        return when(this) {
//            is Leaf -> particles.iterator()
//            is Branch -> object: MutableIterator<Particle> {
//                var quadrantsDone = 0
//                var quadrantIterator = quadrants[0].getParticleIterator()
//
//                override fun hasNext(): Boolean {
//                    advanceQuadrantIfNeeded()
//                    return quadrantIterator.hasNext()
//                }
//
//                override fun next(): Particle {
//                    advanceQuadrantIfNeeded()
//                    return quadrantIterator.next()
//                }
//
//                override fun remove() = quadrantIterator.remove()
//
//                private fun advanceQuadrantIfNeeded() {
//                    while (!quadrantIterator.hasNext() and (quadrantsDone < 4)) {
//                        quadrantIterator = quadrants[quadrantsDone++].getParticleIterator()
//                    }
//                }
//            }
//        }
//    }

    fun debugDraw(graphics: Graphics2D) {
        val midX = (position.x + (size.x / 2)).roundToInt()
        val midY = (position.y + (size.y / 2)).roundToInt()
        when (this) {
            is Leaf -> {
                // In center of region, draw number of particles
                graphics.drawString(particles.size.toString(), midX, midY)
            }
            is Branch -> {
                // Draw cross dividing space
                graphics.drawLine(midX, position.y.roundToInt(), midX, (position.y + size.y).roundToInt())
                graphics.drawLine(position.x.roundToInt(), midY, (position.x + size.x).roundToInt(), midY)
                // Recursively draw sub-nodes
                quadrants.forEach { q -> q.debugDraw(graphics) }
            }
        }
    }

    // TODO next step: add logic to track enclosing node in particles

    fun addParticleIfTouching(particle: Particle) {
        if (touchesParticle(particle)) {
            when (this) {
                is Leaf -> particles.add(particle)
                is Branch -> quadrants.forEach { it.addParticleIfTouching(particle) }
            }
            if (enclosesParticle(particle)) {
                particle.enclosingTreeNode = this
            }
        }
    }

    fun removeParticle(particle: Particle) {
        when (this) {
            is Leaf -> particles.remove(particle)
            is Branch -> quadrants.forEach { it.removeParticle(particle) }
        }
    }

    fun subdivideIntoBranch(): Branch {
        return when (this) {
            is Branch -> this
            is Leaf -> {
                val newBranch = Branch(parent, position, size, depth)
                particles.forEach(newBranch::addParticleIfTouching)
                newBranch
            }
        }
    }

    fun mergeIntoLeaf(): Leaf {
        return when (this) {
            is Leaf -> this
            is Branch -> {
                val newLeaf = Leaf(parent, position, size, depth)
                (0..quadrants.size).forEach { i ->
                    quadrants[i] = quadrants[i].mergeIntoLeaf().also { l -> newLeaf.particles.addAll(l.particles) }
                }
                newLeaf
            }
        }
    }

    fun enclosesParticle(particle: Particle): Boolean {
        val surroundsX = (particle.position.x > position.x + particle.radius) and (particle.position.x < position.x + size.x - particle.radius)
        val surroundsY = (particle.position.y > position.y + particle.radius) and (particle.position.y < position.y + size.y - particle.radius)
        return surroundsX and surroundsY
    }

    fun touchesParticle(particle: Particle): Boolean {
        val touchesX = (particle.position.x >= position.x - particle.radius) and (particle.position.x <= position.x + size.x + particle.radius)
        val touchesY = (particle.position.y >= position.y - particle.radius) and (particle.position.y <= position.y + size.y + particle.radius)
        return touchesX and touchesY
    }
}
