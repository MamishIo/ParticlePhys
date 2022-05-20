import java.awt.Color
import java.awt.Graphics2D
import kotlin.math.roundToInt

sealed class ParticleQuadtree(val parent: ParticleQuadtree?, val position: Vector2D, val size: Vector2D, val depth: Int) {

    class Branch(parent: ParticleQuadtree?, position: Vector2D, size: Vector2D, depth: Int) : ParticleQuadtree(parent, position, size, depth) {
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

        fun reassignEachQuadrant(fn: (ParticleQuadtree) -> ParticleQuadtree) {
            quadrants.indices.forEach { i ->
                quadrants[i] = fn(quadrants[i])
            }
        }
    }

    class Leaf(parent: ParticleQuadtree?, position: Vector2D, size: Vector2D, depth: Int) : ParticleQuadtree(parent, position, size, depth) {
        val particles = HashSet<Particle>(64)
    }

    fun getLeafIterator(): Iterator<Leaf> {
        return when (this) {
            is Leaf -> object : Iterator<Leaf> {
                var leaf: Leaf? = this@ParticleQuadtree
                override fun hasNext() = leaf != null
                override fun next() = leaf!!.also { leaf = null }
            }
            is Branch -> object : Iterator<Leaf> {
                var quadrantsDone = 0
                var quadrantIterator = quadrants[0].getLeafIterator()

                override fun hasNext() = nextQuadrantIterator().hasNext()
                override fun next() = nextQuadrantIterator().next()

                private fun nextQuadrantIterator(): Iterator<Leaf> {
                    while (!quadrantIterator.hasNext() and (quadrantsDone < 3)) {
                        quadrantIterator = quadrants[++quadrantsDone].getLeafIterator()
                    }
                    return quadrantIterator
                }
            }
        }
    }

    fun debugDraw(graphics: Graphics2D) {
        graphics.color = Color.GRAY
        val midX = position.x + (size.x / 2)
        val midY = position.y + (size.y / 2)
        when (this) {
            is Leaf -> {
                // In center of region, draw number of particles
                val numParticlesStr = particles.size.toString()
                val fontMetrics = graphics.fontMetrics
                val strStartX = (size.x - fontMetrics.stringWidth(numParticlesStr)) / 2.0
                val strStartY = fontMetrics.ascent + ((size.y - fontMetrics.height) / 2.0)
                graphics.drawString(particles.size.toString(), (position.x + strStartX).rnd(), (position.y + strStartY).rnd())
            }
            is Branch -> {
                // Draw cross dividing space
                graphics.drawLine(midX.rnd(), position.y.roundToInt(), midX.rnd(), (position.y + size.y).rnd())
                graphics.drawLine(position.x.rnd(), midY.rnd(), (position.x + size.x).roundToInt(), midY.rnd())
                // Recursively draw sub-nodes
                quadrants.forEach { q -> q.debugDraw(graphics) }
            }
        }
    }

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

    // TODO: Maybe this should merge leaves at a low size threshold rather than zero?
    fun resizeTree(): ParticleQuadtree {
        // If this is a leaf that has gained too many particles, subdivide it (up to a depth limit)
        if (this is Leaf && (particles.size > QUADTREE_MAX_PARTICLES_PER_LEAF && depth < QUADTREE_MAX_HEIGHT)) {
            val replacementBranch = Branch(parent, position, size, depth)
            particles.forEach(replacementBranch::addParticleIfTouching)
            // Recursively call resize on the new branch, in case it has any quadrants that are still too full
            return replacementBranch.resizeTree()
        }

        if (this is Branch) {
            reassignEachQuadrant { it.resizeTree() }
            if (quadrants.all { it is Leaf && it.particles.isEmpty() }) {
                return Leaf(parent, position, size, depth)
            }
        }

        return this
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

    private fun Double.rnd() = roundToInt()

    override fun toString(): String {
        return "ParticleQuadtree(parent=$parent, position=$position, size=$size, depth=$depth)"
    }
}
