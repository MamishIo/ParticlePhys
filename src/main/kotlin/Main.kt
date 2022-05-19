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

            val timeDelta = 1.0 / TICK_RATE // Uses a fixed time delta for now
            simulationTick(timeDelta)
            render(timeDelta)

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
    debugReportTimeTaken("drawParticles", { drawParticles(timeDelta) })
    debugReportTimeTaken("drawHud", { drawHud() })
    debugReportTimeTaken("canvasRepaint", { canvasComponent.repaint() })
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
        particleTree.debugDraw(g)
        drawCollisionLines(g)
        g.color = Color.WHITE
        g.drawString(fpsString, 16, 16)
        g.dispose()
    }
}
