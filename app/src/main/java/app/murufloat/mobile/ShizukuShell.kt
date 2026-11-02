
package app.murufloat.mobile

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import rikka.shizuku.Shizuku
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object ShizukuShell {

    private val lock = Any()

    @Volatile
    private var remote: ICommandExecutor? = null

    private var connecting = false
    private var connectionLatch: CountDownLatch? = null

    private var connectionError = ""

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(
            name: ComponentName?,
            service: IBinder?
        ) {
            synchronized(lock) {
                if (service == null) {
                    remote = null
                    connectionError = "Binder User Service kosong."
                } else {
                    remote = ICommandExecutor.Stub.asInterface(service)
                    connectionError = ""
                }

                connecting = false
                connectionLatch?.countDown()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            synchronized(lock) {
                remote = null
                connecting = false
                connectionError = "Shizuku User Service terputus."
                connectionLatch?.countDown()
            }
        }
    }

    fun launchActivity(
        context: Context,
        app: LaunchableApp,
        windowingMode: Int
    ): String {

        if (!AppLauncher.hasShizukuPermission()) {
            return "ERROR: Shizuku permission tidak tersedia."
        }

        val executor = acquire(context)

        if (executor == null) {
            return "ERROR: ${connectionError.ifBlank {
                "Gagal menghubungkan Shizuku User Service."
            }}"
        }

        return try {
            executor.launchActivity(
                app.packageName,
                app.activityName,
                windowingMode
            )
        } catch (error: Throwable) {
            synchronized(lock) {
                remote = null
                connectionError =
                    "${error.javaClass.simpleName}: ${error.message}"
            }

            "ERROR: Gagal menjalankan User Service: $connectionError"
        }
    }

    private fun acquire(context: Context): ICommandExecutor? {

        synchronized(lock) {

            remote?.let { return it }

            if (!Shizuku.pingBinder()) {
                connectionError = "Shizuku Binder tidak aktif."
                return null
            }

            if (!connecting) {

                connecting = true
                connectionError = "Menunggu koneksi User Service..."

                connectionLatch = CountDownLatch(1)

                val args = Shizuku.UserServiceArgs(
                    ComponentName(
                        context.packageName,
                        ShellUserService::class.java.name
                    )
                )
                    .daemon(false)
                    .tag("murufloat-shell-launcher")
                    .version(1)
                    .processNameSuffix("murufloat_shell")

                try {
                    Shizuku.bindUserService(args, connection)

                } catch (error: Throwable) {
                    connecting = false
                    connectionError =
                        "Bind gagal: ${error.javaClass.simpleName}: ${error.message}"

                    connectionLatch?.countDown()
                }
            }
        }

        val latch = synchronized(lock) {
            connectionLatch
        }

        val completed = try {
            latch?.await(8, TimeUnit.SECONDS) == true
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }

        if (!completed) {
            synchronized(lock) {
                if (remote == null) {
                    connecting = false
                    connectionError =
                        "Timeout: Shizuku User Service tidak merespons dalam 8 detik."
                }
            }
        }

        return synchronized(lock) {
            remote
        }
    }
}
