package app.murufloat.mobile

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object AppLauncher {
    enum class Result { FREEFORM_REQUESTED, FULLSCREEN, FAILED }

    fun open(
        context: Context,
        app: LaunchableApp,
        tryFreeform: Boolean = true,
        fullscreen: Boolean = false,
        allowFallback: Boolean = true
    ): Result {
        val requestedMode = when { fullscreen -> 1; tryFreeform -> 5; else -> 0 }
        if (requestedMode != 0 && hasShizukuPermission()) {
            val output = ShizukuShell.launchActivity(context, app, requestedMode)
            val lower = output.orEmpty().lowercase()
            val success = output != null && output.contains("exit=0") &&
                lower.contains("status: ok") && !lower.contains("error") &&
                !lower.contains("exception") && !lower.contains("unable to")
            if (success) {
                saveLastApp(context, app)
                return if (fullscreen) Result.FULLSCREEN else Result.FREEFORM_REQUESTED
            }
        } else if (requestedMode != 0 && !allowFallback && !fullscreen) {
            return Result.FAILED
        }

        if (requestedMode == 5 && !allowFallback) {
            return Result.FAILED
        }

        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(
                    ComponentName(app.packageName, app.activityName)
                )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (fullscreen) intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(intent)
            saveLastApp(context, app)
            Result.FULLSCREEN
        } catch (_: Throwable) {
            Result.FAILED
        }
    }

    fun hasShizukuPermission(): Boolean = try {
        Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) {
        false
    }

    private fun saveLastApp(context: Context, app: LaunchableApp) {
        context.getSharedPreferences("murufloat", Context.MODE_PRIVATE).edit()
            .putString("last_package", app.packageName)
            .putString("last_activity", app.activityName)
            .putString("last_label", app.label)
            .apply()
    }

}
