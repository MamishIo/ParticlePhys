import java.awt.*
import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.MICROSECONDS
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.math.*
import kotlin.random.Random.Default as random

data class Vector2(var x: Double, var y: Double)
data class Particle(val radius: Double, val color: Color, var ttl: Double, val position: Vector2, val velocity: Vector2)

val canvasImage = BufferedImage(HORIZONTAL_BOUND, VERTICAL_BOUND, BufferedImage.TYPE_INT_ARGB)
var canvasComponent = object : JPanel() {
    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        g?.drawImage(canvasImage, 0, 0, HORIZONTAL_BOUND, VERTICAL_BOUND, null)
    }
}

val particles: MutableList<Particle> = mutableListOf()
var particlesToSpawnAccumulator = 0.0
var lastTickNano = System.nanoTime()

fun main() {
    Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(::onTick, 0, TICK_INTERVAL_US, MICROSECONDS)
    createWindow()
}

fun onTick() {
    val timeDelta = 1.0 / TICK_RATE // Uses a fixed time delta for now
    simulateAllParticles(timeDelta)
    spawnNewParticles(timeDelta)
    drawParticles(timeDelta)
    canvasComponent.repaint()
    lastTickNano = System.nanoTime()
}

fun simulateAllParticles(timeDelta: Double) {
    val iterator = particles.listIterator()
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
    particle.velocity.y += GLOBAL_GRAVITY * timeDelta
    particle.position.x += particle.velocity.x * timeDelta
    particle.position.y += particle.velocity.y * timeDelta
}

fun spawnNewParticles(timeDelta: Double) {
    particlesToSpawnAccumulator += PARTICLE_SPAWN_RATE.next() * timeDelta
    while (particlesToSpawnAccumulator >= 1.0) {
        particles.add(randomParticle())
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
    val g = canvasImage.createGraphics()

    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.background = Color.BLACK

    val alphaForTrails = 1.0 - TRAIL_TARGET_ALPHA_1_SECOND.pow(timeDelta)
    g.color = Color(0f, 0f, 0f, alphaForTrails.toFloat())
    g.fillRect(0, 0, HORIZONTAL_BOUND, VERTICAL_BOUND)

    particles.forEach { p ->
        g.color = p.color
        val drawX = (p.position.x - p.radius).roundToInt()
        val drawY = (p.position.y - p.radius).roundToInt()
        val drawS = (p.radius * 2.0).roundToInt()
        g.fillOval(drawX, drawY, drawS, drawS)
    }
}

fun randomParticle(): Particle {
    val radius = PARTICLE_RADIUS_RANGE.next()
    val color = randomColor()
    val ttl = PARTICLE_LIFETIME_RANGE.next()
    val position = Vector2(random.nextDouble(HORIZONTAL_BOUND.toDouble()), random.nextDouble(VERTICAL_BOUND.toDouble()))
    val velocityMagnitude = PARTICLE_START_VELOCITY_RANGE.next()
    val velocityDirection = random.nextDouble(PI * 2.0)
    val velocity = Vector2(velocityMagnitude * cos(velocityDirection), velocityMagnitude * sin(velocityDirection))

    return Particle(radius, color, ttl, position, velocity)
}

fun randomColor(): Color {
    val rgb = Color.HSBtoRGB(PARTICLE_HUE_RANGE.next(), 1f, 1f)
    with(ColorModel.getRGBdefault()) {
        return Color(getRed(rgb), getGreen(rgb), getBlue(rgb), (PARTICLE_ALPHA * 255).toInt())
    }
}
