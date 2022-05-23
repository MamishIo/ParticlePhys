import kotlin.math.floor

const val HORIZONTAL_BOUND = 1920
const val VERTICAL_BOUND = 1080

const val MAX_DEBUG_DRAW_COLLISION_LINES = 0 // Set 0 to disable
const val DRAW_HUD_QUADTREE_NODES = false
const val DRAW_PARTICLE_VELOCITY_VECTORS = false

const val PRETTY_DRAWING_MODE = true
val PARTICLE_HUE_RANGE = FloatRange(0f, 1f)

val PARTICLE_RADIUS_RANGE = DoubleRange(2.0, 4.5)
val PARTICLE_START_VELOCITY_RANGE = DoubleRange(20.0, 90.0)
val PARTICLE_LIFETIME_RANGE = DoubleRange(15.0, 20.0)

val PARTICLE_SPAWN_RATE = DoubleRange(150.0, 160.0) //DoubleRange(25.0, 40.0)

//val GRAVITY_WELL_POSITION = Vector2D(HORIZONTAL_BOUND/2.0, VERTICAL_BOUND/2.0)
const val GRAVITATIONAL_CONSTANT = 5000.0
// Real-world gravity uses square-of-distance (i.e. 2) but in simulation this is pretty unsatisfying to watch,
// since it just makes particles hyper-sensitive to gravity very close to the center well.
const val GRAVITATIONAL_DISTANCE_EXPONENT = 0.5
val GLOBAL_GRAVITY = Vector2D(0.0, 0.0)

const val TICK_RATE = 120
val TICK_INTERVAL_NS = floor(1e9 / TICK_RATE).toLong()

// Controls transparency: the 'goal' alpha value of the afterimage after 1 second (0 = fully transparent/gone, 1 = fully there/opaque)
// The maths doesn't work out like I'd hoped so this more of a trail-and-error value
const val TRAIL_TARGET_ALPHA_1_SECOND = 0.03
const val PARTICLE_ALPHA = 0.5

const val QUADTREE_MAX_HEIGHT = 5
const val QUADTREE_MAX_PARTICLES_PER_LEAF = 16
