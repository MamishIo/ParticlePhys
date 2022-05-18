import java.awt.*
import java.awt.RenderingHints.*
import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.math.*
import kotlin.random.Random.Default as random

data class Particle(
    val id: Long,
    val radius: Double,
    val color: Color,
    var ttl: Double,
    val position: Vector2D,
    val velocity: Vector2D,
    var enclosingTreeNode: ParticleQuadtree? = null,
)

// val particles: MutableList<Particle> = mutableListOf()
val particleTree = ParticleQuadtree.Branch(Vector2D(0.0, 0.0), Vector2D(HORIZONTAL_BOUND.toDouble(), VERTICAL_BOUND.toDouble()), 0)
var particlesToSpawnAccumulator = 0.0

const val PRETTY_DRAWING_MODE = true
val mainImage = BufferedImage(HORIZONTAL_BOUND, VERTICAL_BOUND, BufferedImage.TYPE_INT_ARGB)
val hudImage = BufferedImage(HORIZONTAL_BOUND, VERTICAL_BOUND, BufferedImage.TYPE_INT_ARGB)
var canvasComponent = object : JPanel() {
    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        g?.drawImage(mainImage, 0, 0, HORIZONTAL_BOUND, VERTICAL_BOUND, null)
        g?.drawImage(hudImage, 0, 0, HORIZONTAL_BOUND, VERTICAL_BOUND, null)
    }
}

// TODO Just clean up Performance.kt and this timing code all over

var nextFrameStartDeadlineNanos = System.nanoTime()

fun main() {
    Thread {
        while (true) {
            nextFrameStartDeadlineNanos += TICK_INTERVAL_NS
            tick()

            val tickFinishedNanos = System.nanoTime()
            // If we're late for the next tick, bump the deadline forward to keep timing consistent for the next tick
            if (tickFinishedNanos >= nextFrameStartDeadlineNanos) {
                nextFrameStartDeadlineNanos = tickFinishedNanos
            // Otherwise if we have spare time, do a semi-busy-wait to wait it out, without changing deadline
            } else {
                while (System.nanoTime() < nextFrameStartDeadlineNanos) {
                    Thread.sleep(1)
                }
            }
            finishTickAndUpdatePerformanceStats()
        }
    }.start()
    createWindow()
}

fun tick() {
    val timeDelta = 1.0 / TICK_RATE // Uses a fixed time delta for now
    debugReportTimeTaken("simulate") { simulateAllParticles(timeDelta) }
    debugReportTimeTaken("spawn") { spawnNewParticles(timeDelta) }
    debugReportTimeTaken("drawParticles") { drawParticles(timeDelta) }
    debugReportTimeTaken("drawHud") { drawHud() }
    debugReportTimeTaken("repaint") { canvasComponent.repaint() } // This might redraw async which would make the performance calc wrong, TODO confirm
}

fun simulateAllParticles(timeDelta: Double) {
    val iterator = particleTree.getParticleIterator()
    while (iterator.hasNext()) {
        val p = iterator.next()
        p.ttl -= timeDelta
        if (p.ttl <= 0.0) {
            iterator.remove()
        } else {
            simulateParticle(p, timeDelta)
        }
    }
}

fun simulateParticle(particle: Particle, timeDelta: Double) {
    with(particle) {
        velocity.y += GLOBAL_GRAVITY * timeDelta
        position.x += velocity.x * timeDelta
        position.y += velocity.y * timeDelta
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

fun spawnNewParticles(timeDelta: Double) {
    particlesToSpawnAccumulator += PARTICLE_SPAWN_RATE.next() * timeDelta
    while (particlesToSpawnAccumulator >= 1.0) {
        particleTree.addParticleIfTouching(randomParticle())
        particlesToSpawnAccumulator--
    }
}

fun createWindow() {
    val window = JFrame("ParticlePhys")
    window.background = Color.BLACK
    window.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    window.layout = BorderLayout()
    canvasComponent.preferredSize = Dimension(HORIZONTAL_BOUND, VERTICAL_BOUND)
    window.add(canvasComponent, BorderLayout.CENTER)
    window.pack()
    window.isVisible = true
}

fun drawParticles(timeDelta: Double) {
    with(mainImage.createGraphics()) {
        setRenderingHint(KEY_ANTIALIASING, if (PRETTY_DRAWING_MODE) VALUE_ANTIALIAS_ON else VALUE_ANTIALIAS_OFF)

        background = Color.BLACK
        val alphaForClear = if (PRETTY_DRAWING_MODE) 1.0 - TRAIL_TARGET_ALPHA_1_SECOND.pow(timeDelta) else 1.0
        color = Color(0f, 0f, 0f, alphaForClear.toFloat())
        fillRect(0, 0, mainImage.width, mainImage.height)

        particleTree.getParticleIterator().forEach { p ->
            color = p.color
            val drawX = (p.position.x - p.radius).roundToInt()
            val drawY = (p.position.y - p.radius).roundToInt()
            val drawS = (p.radius * 2.0).roundToInt()
            fillOval(drawX, drawY, drawS, drawS)
        }

        dispose()
    }
}

fun drawHud() {
    val averageFpsClamped = getAverageFps().coerceAtMost(999.0)
    val fpsPlusMinusClamped = getFpsRangePlusMinus().coerceAtMost(99.0)
    val fpsString = "${String.format("%3.1f", averageFpsClamped)} +- ${String.format("%2.1f", fpsPlusMinusClamped)}"
    with(hudImage.createGraphics()) {
        color = Color(0, 0, 0, 0)
        composite = AlphaComposite.Clear
        fillRect(0, 0, hudImage.width, hudImage.height)
        dispose()
    }
    hudImage.createGraphics().let { g ->
        g.color = Color.GREEN
        g.drawString(fpsString, 16, 16)
        particleTree.debugDraw(g)
        g.dispose()
    }
}

fun randomParticle(): Particle {
    val id = (random.nextLong() - 1).absoluteValue
    val radius = PARTICLE_RADIUS_RANGE.next()
    val color = randomColor()
    val ttl = PARTICLE_LIFETIME_RANGE.next()
    //val position = listOf(Vector2D(960.0, 540.0), Vector2D(480.0, 270.0), Vector2D(480.0, 540.0)).random()
    val position = Vector2D(random.nextDouble(HORIZONTAL_BOUND.toDouble()), random.nextDouble(VERTICAL_BOUND.toDouble()))
    val velocityMagnitude = PARTICLE_START_VELOCITY_RANGE.next()
    //val velocityMagnitude = 300.0
    val velocityDirection = random.nextDouble(PI * 2.0)
    val velocity = Vector2D(velocityMagnitude * cos(velocityDirection), velocityMagnitude * sin(velocityDirection))

    return Particle(id, radius, color, ttl, position, velocity)
}

fun randomColor(): Color {
    val rgb = Color.HSBtoRGB(PARTICLE_HUE_RANGE.next(), 1f, 1f)
    with(ColorModel.getRGBdefault()) {
        val alpha = if (PRETTY_DRAWING_MODE) (PARTICLE_ALPHA * 255).toInt() else 255
        return Color(getRed(rgb), getGreen(rgb), getBlue(rgb), alpha)
    }
}
