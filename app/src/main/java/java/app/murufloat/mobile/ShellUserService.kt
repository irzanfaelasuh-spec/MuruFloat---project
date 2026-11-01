package app.murufloat.mobile

import android.os.RemoteException
import java.util.concurrent.TimeUnit

/**
 * Runs one narrowly-scoped Activity Manager request as the Shizuku shell user.
 * The app side never sends arbitrary shell text to this service.
 */
class ShellUserService : ICommandExecutor.Stub() {
    @Throws(RemoteException::class)
    override fun launchActivity(packageName: String, activityName: String, windowingMode: Int): String {
        if (!PACKAGE_PATTERN.matches(packageName)) return "error: invalid package name"
        if (!ACTIVITY_PATTERN.matches(activityName)) return "error: invalid activity name"
        if (windowingMode != MODE_FULLSCREEN && windowingMode != MODE_FREEFORM) return "error: unsupported windowing mode"

        val fullActivity = if (activityName.startsWith(".")) packageName + activityName else activityName
        return try {
            val process = ProcessBuilder(
                "/system/bin/am", "start", "-W",
                "--windowingMode", windowingMode.toString(),
                "-n", "$packageName/$fullActivity"
            ).redirectErrorStream(true).start()
            val completed = process.waitFor(18, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                return "error: launch request timed out"
            }
            val output = process.inputStream.bufferedReader().use { it.readText() }
            "exit=${process.exitValue()}\n$output".trim()
        } catch (error: Throwable) {
            "error: ${error.javaClass.simpleName}: ${error.message.orEmpty()}"
        }
    }

    companion object {
        private const val MODE_FULLSCREEN = 1
        private const val MODE_FREEFORM = 5
        private val PACKAGE_PATTERN = Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+")
        private val ACTIVITY_PATTERN = Regex("[A-Za-z0-9_.$]+")
    }
}
