import java.awt.*
import java.awt.RenderingHints.*
import java.awt.image.BufferedImage
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.math.pow
import kotlin.math.roundToInt

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

            UPDATE_CYCLE_INNER_COUNTER.time {
                val timeDelta = 1.0 / TICK_RATE // Uses a fixed time delta for now
                simulateParticles(timeDelta)
                render(timeDelta)
            }

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
            UPDATE_CYCLE_COUNTER.addTimeTick()
        }
    }.start()
    createWindow()
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

fun render(timeDelta: Double) {
    DRAW_PARTICLES_COUNTER.time { drawParticles(timeDelta) }
    DRAW_HUD_COUNTER.time { drawHud() }
    CANVAS_REPAINT_COUNTER.time { canvasComponent.repaint() }
}

fun drawParticles(timeDelta: Double) {
    with(mainImage.createGraphics()) {
        setRenderingHint(KEY_ANTIALIASING, if (PRETTY_DRAWING_MODE) VALUE_ANTIALIAS_ON else VALUE_ANTIALIAS_OFF)

        background = Color.BLACK
        val alphaForClear = if (PRETTY_DRAWING_MODE) 1.0 - TRAIL_TARGET_ALPHA_1_SECOND.pow(timeDelta) else 1.0
        color = Color(0f, 0f, 0f, alphaForClear.toFloat())
        fillRect(0, 0, mainImage.width, mainImage.height)

        particleList.forEach { p ->
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
    with(hudImage.createGraphics()) {
        color = Color(0, 0, 0, 0)
        composite = AlphaComposite.Clear
        fillRect(0, 0, hudImage.width, hudImage.height)
        dispose()
    }
    hudImage.createGraphics().let { g ->
        particleTree.debugDraw(g)

        drawCollisionLines(g)

        g.color = Color.BLACK
        g.fillRect(0, 0, 300, (1 + countersToDraw.size) * 16)

        g.color = Color.WHITE
        countersToDraw.forEachIndexed { i, counter -> g.drawString(counter.getMetricString(), 16, (i + 1) * 16) }

        g.dispose()
    }
}
