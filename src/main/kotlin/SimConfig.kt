import kotlin.math.floor

const val HORIZONTAL_BOUND = 1920
const val VERTICAL_BOUND = 1080

val PARTICLE_RADIUS_RANGE = DoubleRange(12.0, 38.0)
val PARTICLE_HUE_RANGE = FloatRange(0f, 1f)
val PARTICLE_START_VELOCITY_RANGE = DoubleRange(25.0, 600.0)
val PARTICLE_LIFETIME_RANGE = DoubleRange(2.5, 3.5)

const val GLOBAL_GRAVITY = 200.0

val PARTICLE_SPAWN_RATE = DoubleRange(25.0, 40.0)

const val TICK_RATE = 90
val TICK_INTERVAL_US = floor(1e6 / TICK_RATE).toLong()

// Controls transparency: the 'goal' alpha value of the afterimage after 1 second (0 = fully transparent/gone, 1 = fully there/opaque)
// The maths doesn't work out like I'd hoped so this more of a trail-and-error value
const val TRAIL_TARGET_ALPHA_1_SECOND = 0.03
const val PARTICLE_ALPHA = 0.45
