package app.murufloat.mobile

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import java.util.Locale

 data class LaunchableApp(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable,
    val category: String,
    val isSystem: Boolean
)

object AppCatalog {
    private val gameHints = listOf(
        "game", "minecraft", "roblox", "pubg", "free fire", "freefire", "genshin",
        "mobile legends", "mlbb", "valorant", "codm", "call of duty", "clash of",
        "stumble guys", "brawl stars", "honkai", "zenless", "efootball", "fc mobile",
        "fifa mobile", "subway surfers", "terraria", "among us", "blockman go",
        "geometry dash", "pokemon", "arena breakout", "blood strike", "wild rift",
        "league of legends", "delta force", "critical ops", "asphalt", "war thunder"
    )

    fun load(context: Context, hideGames: Boolean = true): List<LaunchableApp> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = try {
            if (Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            }
        } catch (_: Throwable) {
            emptyList()
        }

        val found = LinkedHashMap<String, LaunchableApp>()
        for (item in resolved) {
            val activity = item.activityInfo ?: continue
            val packageName = activity.packageName ?: continue
            if (packageName == context.packageName || packageName == "${context.packageName}.debug") continue
            val appInfo = activity.applicationInfo ?: continue
            val label = try { item.loadLabel(pm).toString() } catch (_: Throwable) { packageName }
            val isGame = appInfo.category == ApplicationInfo.CATEGORY_GAME ||
                gameHints.any { label.lowercase(Locale.ROOT).contains(it) || packageName.lowercase(Locale.ROOT).contains(it.replace(" ", "")) }
            if (hideGames && isGame) continue
            val icon = try { item.loadIcon(pm) } catch (_: Throwable) { pm.defaultActivityIcon }
            val category = classify(packageName, label)
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val key = "$packageName/${activity.name}"
            found[key] = LaunchableApp(label, packageName, activity.name, icon, category, isSystem)
        }
        return found.values.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    private fun classify(pkg: String, label: String): String {
        val value = (pkg + " " + label).lowercase(Locale.ROOT)
        return when {
            listOf("tiktok", "instagram", "facebook", "telegram", "whatsapp", "snapchat", "threads", "twitter", "x.com", "com.twitter").any { value.contains(it) } -> "Social"
            listOf("spotify", "youtube", "music", "netflix", "soundcloud", "video").any { value.contains(it) } -> "Media"
            listOf("chrome", "browser", "firefox", "edge", "brave").any { value.contains(it) } -> "Browser"
            listOf("mail", "gmail", "outlook", "message", "messaging", "phone", "dialer", "signal", "discord", "messenger").any { value.contains(it) } -> "Communication"
            else -> "Other"
        }
    }
}
