import kotlin.math.floor

const val HORIZONTAL_BOUND = 1920
const val VERTICAL_BOUND = 1080

const val PRETTY_DRAWING_MODE = false
val PARTICLE_HUE_RANGE = FloatRange(0f, 1f)

val PARTICLE_RADIUS_RANGE = DoubleRange(9.0, 16.0)
val PARTICLE_START_VELOCITY_RANGE = DoubleRange(120.0, 160.0)
val PARTICLE_LIFETIME_RANGE = DoubleRange(3.5, 4.5)

val PARTICLE_SPAWN_RATE = DoubleRange(160.0, 180.0) //DoubleRange(25.0, 40.0)

val GLOBAL_GRAVITY = Vector2D(0.0, 20.0)

const val TICK_RATE = 120
val TICK_INTERVAL_NS = floor(1e9 / TICK_RATE).toLong()

// Controls transparency: the 'goal' alpha value of the afterimage after 1 second (0 = fully transparent/gone, 1 = fully there/opaque)
// The maths doesn't work out like I'd hoped so this more of a trail-and-error value
const val TRAIL_TARGET_ALPHA_1_SECOND = 0.03
const val PARTICLE_ALPHA = 0.3

const val QUADTREE_MAX_HEIGHT = 5
const val QUADTREE_MAX_PARTICLES_PER_LEAF = 8
