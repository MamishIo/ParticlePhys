import org.lwjgl.system.MemoryStack.stackPush
import java.nio.charset.StandardCharsets.US_ASCII
import org.lwjgl.opencl.CL30 as CL


fun main() {
    stackPush().use {
        val numPlatformsBuf = it.mallocInt(1)
        val platformIdsBuf = it.mallocPointer(16)
        assertSuccess(CL.clGetPlatformIDs(platformIdsBuf, numPlatformsBuf))
        println("Got ${numPlatformsBuf.get(0)} platforms")

        (0 until numPlatformsBuf.get(0)).map { i -> platformIdsBuf.get(i) }.forEach { platformId ->
            println("Got CL platform ID $platformId")

            fun printPlatformInfoString(name: String, param: Int) {
                val charsBuf = it.malloc(128)
                val sizeRetBuf = it.mallocPointer(1)
                assertSuccess(CL.clGetPlatformInfo(platformId, param, charsBuf, sizeRetBuf))
                val asString = US_ASCII.decode(charsBuf.slice(0, sizeRetBuf.get(0).toInt() - 1))
                println("Platform $platformId: $name = '$asString'")
            }
            printPlatformInfoString("PLATFORM_NAME", CL.CL_PLATFORM_NAME)
            printPlatformInfoString("PLATFORM_PROFILE", CL.CL_PLATFORM_PROFILE)
            printPlatformInfoString("PLATFORM_VENDOR", CL.CL_PLATFORM_VENDOR)
            printPlatformInfoString("PLATFORM_VERSION", CL.CL_PLATFORM_VERSION)

            val numDevicesBuf = it.mallocInt(1)
            val deviceIdsBuf = it.mallocPointer(16)
            assertSuccess(CL.clGetDeviceIDs(platformId, CL.CL_DEVICE_TYPE_ALL.toLong(), deviceIdsBuf, numDevicesBuf))
            println("Got ${numDevicesBuf.get(0)} devices")

            (0 until numDevicesBuf.get(0)).map { i -> deviceIdsBuf.get(i) }.forEach {deviceId ->
                println("Got CL device ID $deviceId")

                fun printDeviceInfoString(name: String, param: Int) {
                    val charsBuf = it.malloc(128)
                    val sizeRetBuf = it.mallocPointer(1)
                    assertSuccess(CL.clGetDeviceInfo(deviceId, param, charsBuf, sizeRetBuf))
                    val asString = US_ASCII.decode(charsBuf.slice(0, sizeRetBuf.get(0).toInt() - 1))
                    println("Platform $platformId: $name = '$asString'")
                }

                printDeviceInfoString("CL_DEVICE_NAME", CL.CL_DEVICE_NAME)
            }
        }
    }
}

private fun assertSuccess(returnValue: Int) {
    if (returnValue != CL.CL_SUCCESS) {
        throw RuntimeException("Bad return code $returnValue")
    }
}