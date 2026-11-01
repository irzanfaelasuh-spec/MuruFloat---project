package app.murufloat.mobile

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import rikka.shizuku.Shizuku
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Maintains a Shizuku UserService connection and exposes only a safe launch operation. */
object ShizukuShell {
    private val lock = Any()
    @Volatile private var remote: ICommandExecutor? = null
    private var connecting = false
    private var connectionLatch: CountDownLatch? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            synchronized(lock) {
                remote = ICommandExecutor.Stub.asInterface(service)
                connecting = false
                connectionLatch?.countDown()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            synchronized(lock) {
                remote = null
                connecting = false
                connectionLatch?.countDown()
            }
        }
    }

    fun launchActivity(context: Context, app: LaunchableApp, windowingMode: Int): String? {
        if (!AppLauncher.hasShizukuPermission()) return null
        val executor = acquire(context) ?: return null
        return try {
            executor.launchActivity(app.packageName, app.activityName, windowingMode)
        } catch (_: Throwable) {
            synchronized(lock) { remote = null }
            null
        }
    }

    private fun acquire(context: Context): ICommandExecutor? {
        synchronized(lock) {
            remote?.let { return it }
            if (!Shizuku.pingBinder()) return null
            if (!connecting) {
                connecting = true
                connectionLatch = CountDownLatch(1)
                val args = Shizuku.UserServiceArgs(
                    ComponentName(context.packageName, ShellUserService::class.java.name)
                )
                    .daemon(false)
                    .tag("murufloat-shell-launcher")
                    .version(1)
                    .processNameSuffix("murufloat_shell")
                try {
                    Shizuku.bindUserService(args, connection)
                } catch (_: Throwable) {
                    connecting = false
                    connectionLatch?.countDown()
                }
            }
        }

        val latch = synchronized(lock) { connectionLatch }
        val completed = try { latch?.await(8, TimeUnit.SECONDS) == true } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
        if (!completed) {
            synchronized(lock) {
                if (remote == null) connecting = false
            }
        }
        return synchronized(lock) { remote }
    }
}
