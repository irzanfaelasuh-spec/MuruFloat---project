package app.murufloat.mobile

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import rikka.shizuku.Shizuku

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("murufloat", MODE_PRIVATE) }
    private val handler = Handler(Looper.getMainLooper())
    private var currentPage = "home"
    private var pendingOverlayEnable = false
    private lateinit var pageHost: LinearLayout
    private lateinit var navBar: LinearLayout

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener { runOnUiThread { renderCurrentPage() } }
    private val binderDeadListener = Shizuku.OnBinderDeadListener { runOnUiThread { renderCurrentPage() } }
    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == SHIZUKU_REQUEST_CODE) {
            Toast.makeText(
                this,
                if (grantResult == PackageManager.PERMISSION_GRANTED) "Shizuku access authorized." else "Shizuku access was not granted.",
                Toast.LENGTH_SHORT
            ).show()
            renderCurrentPage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = UiKit.BG
        window.navigationBarColor = UiKit.BG
        showSplash()
        handler.postDelayed({
            renderShell()
            renderCurrentPage()
        }, 720L)
    }

    override fun onResume() {
        super.onResume()
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        } catch (_: Throwable) { }
        if (pendingOverlayEnable && Settings.canDrawOverlays(this)) {
            pendingOverlayEnable = false
            startOverlayService()
        }
        if (::pageHost.isInitialized) renderCurrentPage()
    }

    override fun onPause() {
        super.onPause()
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        } catch (_: Throwable) { }
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_REQUEST_CODE) {
            pendingOverlayEnable = false
            if (Settings.canDrawOverlays(this)) startOverlayService()
            else Toast.makeText(this, "Display-over-other-apps permission is still off.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSplash() {
        val frame = FrameLayout(this).apply { setBackgroundColor(UiKit.BG) }
        val stack = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        val mark = TextView(this).apply {
            text = "M"
            textSize = 32f
            setTextColor(UiKit.TEXT)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = UiKit.rounded(UiKit.SURFACE_2, UiKit.dp(this@MainActivity, 24f).toFloat(), UiKit.LINE)
        }
        stack.addView(mark, LinearLayout.LayoutParams(UiKit.dp(this, 76f), UiKit.dp(this, 76f)))
        val title = UiKit.text(this, "MuruFloat", 25f, UiKit.TEXT, true)
        title.gravity = Gravity.CENTER
        val titleLp = LinearLayout.LayoutParams(-2, -2).apply { topMargin = UiKit.dp(this@MainActivity, 18f) }
        stack.addView(title, titleLp)
        val subtitle = UiKit.text(this, "A quieter way to multitask", 12f, UiKit.MUTED)
        subtitle.gravity = Gravity.CENTER
        val subLp = LinearLayout.LayoutParams(-2, -2).apply { topMargin = UiKit.dp(this@MainActivity, 7f) }
        stack.addView(subtitle, subLp)
        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            progressTintList = android.content.res.ColorStateList.valueOf(UiKit.ACCENT)
            indeterminateTintList = android.content.res.ColorStateList.valueOf(UiKit.ACCENT)
        }
        val progressLp = LinearLayout.LayoutParams(UiKit.dp(this, 90f), UiKit.dp(this, 3f)).apply {
            topMargin = UiKit.dp(this@MainActivity, 28f)
        }
        stack.addView(progress, progressLp)
        frame.addView(stack, FrameLayout.LayoutParams(-1, -1))
        setContentView(frame)
        frame.alpha = 0f
        frame.animate().alpha(1f).setDuration(220L).start()
    }

    private fun renderShell() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(UiKit.BG)
            setPadding(UiKit.dp(this@MainActivity, 18f), UiKit.dp(this@MainActivity, 12f), UiKit.dp(this@MainActivity, 18f), UiKit.dp(this@MainActivity, 8f))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val logo = TextView(this).apply {
            text = "M"
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(UiKit.TEXT)
            typeface = Typeface.DEFAULT_BOLD
            background = UiKit.rounded(UiKit.SURFACE_2, UiKit.dp(this@MainActivity, 11f).toFloat(), UiKit.LINE)
        }
        header.addView(logo, LinearLayout.LayoutParams(UiKit.dp(this, 38f), UiKit.dp(this, 38f)))
        val nameStack = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val name = UiKit.text(this, "MuruFloat", 18f, UiKit.TEXT, true)
        val version = UiKit.text(this, "ANDROID MULTITASKING  ·  0.1.0", 9f, UiKit.MUTED, true)
        nameStack.addView(name)
        nameStack.addView(version)
        val nameLp = LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = UiKit.dp(this@MainActivity, 11f) }
        header.addView(nameStack, nameLp)
        val dot = TextView(this).apply {
            text = "●"
            textSize = 10f
            setTextColor(UiKit.GREEN)
            gravity = Gravity.CENTER
            background = UiKit.rounded(UiKit.SURFACE, 20f, UiKit.LINE)
            setPadding(UiKit.dp(this@MainActivity, 10f), UiKit.dp(this@MainActivity, 7f), UiKit.dp(this@MainActivity, 10f), UiKit.dp(this@MainActivity, 7f))
        }
        header.addView(dot)
        root.addView(header)

        pageHost = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val pageLp = LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = UiKit.dp(this@MainActivity, 16f) }
        root.addView(pageHost, pageLp)
        navBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = UiKit.rounded(UiKit.SURFACE, UiKit.dp(this@MainActivity, 16f).toFloat(), UiKit.LINE)
            setPadding(UiKit.dp(this@MainActivity, 5f), UiKit.dp(this@MainActivity, 5f), UiKit.dp(this@MainActivity, 5f), UiKit.dp(this@MainActivity, 5f))
        }
        val navLp = LinearLayout.LayoutParams(-1, UiKit.dp(this, 54f)).apply { topMargin = UiKit.dp(this@MainActivity, 8f) }
        root.addView(navBar, navLp)
        setContentView(root)
        updateNavigation()
    }

    private fun updateNavigation() {
        if (!::navBar.isInitialized) return
        navBar.removeAllViews()
        listOf("home" to "Overview", "apps" to "Apps", "settings" to "Settings").forEach { (key, label) ->
            val selected = currentPage == key
            val item = UiKit.text(this, label, 12f, if (selected) UiKit.TEXT else UiKit.MUTED, selected).apply {
                gravity = Gravity.CENTER
                background = if (selected) UiKit.rounded(UiKit.SURFACE_2, UiKit.dp(this@MainActivity, 12f).toFloat()) else null
                setOnClickListener {
                    currentPage = key
                    renderCurrentPage()
                }
            }
            navBar.addView(item, LinearLayout.LayoutParams(0, -1, 1f).apply {
                leftMargin = UiKit.dp(this@MainActivity, 3f)
                rightMargin = UiKit.dp(this@MainActivity, 3f)
            })
        }
    }

    private fun renderCurrentPage() {
        if (!::pageHost.isInitialized) return
        pageHost.removeAllViews()
        updateNavigation()
        when (currentPage) {
            "apps" -> pageHost.addView(buildAppsPage(), LinearLayout.LayoutParams(-1, -1))
            "settings" -> pageHost.addView(buildSettingsPage(), LinearLayout.LayoutParams(-1, -1))
            else -> pageHost.addView(buildHomePage(), LinearLayout.LayoutParams(-1, -1))
        }
    }

    private fun buildHomePage(): View {
        val scroll = ScrollView(this).apply { clipToPadding = false }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 18f))
        }
        scroll.addView(content)

        val intro = UiKit.card(this, 18)
        val eyebrow = UiKit.text(this, "FLOATING WORKSPACE", 10f, UiKit.ACCENT, true)
        intro.addView(eyebrow)
        UiKit.addGap(this, intro, 8)
        intro.addView(UiKit.text(this, "Your apps, one layer closer.", 21f, UiKit.TEXT, true))
        UiKit.addGap(this, intro, 6)
        intro.addView(UiKit.text(this, "Open installed apps from a compact edge handle. Freeform depends on your Android build.", 12f, UiKit.MUTED))
        UiKit.addGap(this, intro, 14)
        val actions = UiKit.row(this)
        val enable = UiKit.action(this, if (isOverlayRunning()) "Stop overlay" else "Enable overlay", true)
        enable.setOnClickListener { if (isOverlayRunning()) stopOverlayService() else requestOverlayPermissionAndStart() }
        actions.addView(enable)
        val apps = UiKit.action(this, "Browse apps")
        apps.setOnClickListener { currentPage = "apps"; renderCurrentPage() }
        actions.addView(apps, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = UiKit.dp(this@MainActivity, 8f) })
        intro.addView(actions)
        content.addView(intro)

        UiKit.addGap(this, content, 12)
        val shizuku = UiKit.card(this, 16)
        val shHeader = UiKit.row(this)
        shHeader.addView(UiKit.text(this, "Shizuku authorization", 16f, UiKit.TEXT, true), LinearLayout.LayoutParams(0, -2, 1f))
        val status = shizukuStatus()
        val statusChip = statusChip(status.first, status.second)
        shHeader.addView(statusChip)
        shizuku.addView(shHeader)
        UiKit.addGap(this, shizuku, 7)
        shizuku.addView(UiKit.text(this, status.third, 12f, UiKit.MUTED))
        UiKit.addGap(this, shizuku, 12)
        val connect = UiKit.action(this, if (status.first == "Authorized") "Authorization ready" else if (status.first == "Connected") "Authorize MuruFloat" else "Open Shizuku", status.first != "Authorized")
        connect.setOnClickListener { handleShizukuAction() }
        shizuku.addView(connect, LinearLayout.LayoutParams(-1, -2))
        content.addView(shizuku)

        UiKit.addGap(this, content, 12)
        val overlay = UiKit.card(this, 16)
        val ovHead = UiKit.row(this)
        ovHead.addView(UiKit.text(this, "Floating controls", 16f, UiKit.TEXT, true), LinearLayout.LayoutParams(0, -2, 1f))
        ovHead.addView(statusChip(if (isOverlayRunning()) "Running" else if (Settings.canDrawOverlays(this)) "Ready" else "Permission", if (isOverlayRunning()) UiKit.GREEN else UiKit.YELLOW))
        overlay.addView(ovHead)
        UiKit.addGap(this, overlay, 6)
        overlay.addView(UiKit.text(this, "The edge handle stays on the left edge and moves only up or down. The app bubble can be dragged anywhere.", 12f, UiKit.MUTED))
        UiKit.addGap(this, overlay, 12)
        val manage = UiKit.action(this, "Configure floating controls")
        manage.setOnClickListener { currentPage = "settings"; renderCurrentPage() }
        overlay.addView(manage, LinearLayout.LayoutParams(-1, -2))
        content.addView(overlay)

        UiKit.addGap(this, content, 12)
        val note = UiKit.card(this, 15)
        note.addView(UiKit.text(this, "Device compatibility", 14f, UiKit.TEXT, true))
        UiKit.addGap(this, note, 5)
        note.addView(UiKit.text(this, "MuruFloat can display its own overlay on Android 8+. Opening another app in a real resizable window requires freeform support from the phone firmware. Shizuku cannot add that capability when the ROM removes it.", 11f, UiKit.MUTED))
        content.addView(note)
        return scroll
    }

    private fun buildAppsPage(): View {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val heading = UiKit.sectionTitle(this, "Installed apps", "Tap an app to request a window · long-press to open fullscreen.")
        layout.addView(heading)
        UiKit.addGap(this, layout, 12)
        val search = EditText(this).apply {
            hint = "Search installed apps"
            setHintTextColor(UiKit.MUTED)
            setTextColor(UiKit.TEXT)
            textSize = 13f
            singleLine = true
            background = UiKit.rounded(UiKit.SURFACE, UiKit.dp(this@MainActivity, 12f).toFloat(), UiKit.LINE)
            setPadding(UiKit.dp(this@MainActivity, 13f), UiKit.dp(this@MainActivity, 11f), UiKit.dp(this@MainActivity, 13f), UiKit.dp(this@MainActivity, 11f))
        }
        layout.addView(search, LinearLayout.LayoutParams(-1, UiKit.dp(this, 46f)))
        UiKit.addGap(this, layout, 10)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val listScroll = ScrollView(this).apply { clipToPadding = false }
        listScroll.addView(list)
        layout.addView(listScroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val apps = AppCatalog.load(this, prefs.getBoolean("hide_games", true))
        fun populate(query: String) {
            list.removeAllViews()
            val filtered = apps.filter { it.label.contains(query, true) || it.packageName.contains(query, true) }
            if (filtered.isEmpty()) {
                list.addView(UiKit.text(this, "No matching launchable apps found.", 12f, UiKit.MUTED))
            } else {
                filtered.forEachIndexed { index, app ->
                    list.addView(appRow(app) { launchFromAppsPage(app, fullscreen = false) })
                    if (index != filtered.lastIndex) list.addView(UiKit.divider(this))
                }
            }
        }
        populate("")
        search.addTextChangedListener(SimpleTextWatcher { populate(it) })
        val footer = UiKit.text(this, "${apps.size} apps detected  ·  tap an app to launch", 10f, UiKit.MUTED)
        val footerLp = LinearLayout.LayoutParams(-1, -2).apply { topMargin = UiKit.dp(this@MainActivity, 8f) }
        layout.addView(footer, footerLp)
        return layout
    }

    private fun appRow(app: LaunchableApp, onClick: () -> Unit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(UiKit.dp(this@MainActivity, 5f), UiKit.dp(this@MainActivity, 9f), UiKit.dp(this@MainActivity, 5f), UiKit.dp(this@MainActivity, 9f))
            setOnClickListener { onClick() }
        }
        val icon = ImageView(this).apply { setImageDrawable(app.icon) }
        row.addView(icon, LinearLayout.LayoutParams(UiKit.dp(this, 42f), UiKit.dp(this, 42f)))
        val names = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        names.addView(UiKit.text(this, app.label, 13f, UiKit.TEXT, true))
        val sub = UiKit.text(this, app.category + if (app.isSystem) "  ·  system" else "", 10f, UiKit.MUTED)
        val subLp = LinearLayout.LayoutParams(-1, -2).apply { topMargin = UiKit.dp(this@MainActivity, 4f) }
        names.addView(sub, subLp)
        row.addView(names, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = UiKit.dp(this, 11f) })
        row.addView(UiKit.text(this, "OPEN  ›", 10f, UiKit.ACCENT, true))
        row.setOnLongClickListener {
            launchFromAppsPage(app, fullscreen = true)
            true
        }
        return row
    }

    private fun launchFromAppsPage(app: LaunchableApp, fullscreen: Boolean) {
        Thread {
            val result = AppLauncher.open(
                applicationContext, app,
                tryFreeform = prefs.getBoolean("try_freeform", true) && !fullscreen,
                fullscreen = fullscreen,
                allowFallback = prefs.getBoolean("fallback_fullscreen", true)
            )
            handler.post {
                val message = when (result) {
                    AppLauncher.Result.FREEFORM_REQUESTED -> "Freeform launch requested for ${app.label}."
                    AppLauncher.Result.FULLSCREEN -> if (fullscreen) "Opening ${app.label} fullscreen." else "Opened ${app.label} normally; freeform may not be supported."
                    AppLauncher.Result.FAILED -> "Could not open ${app.label}. Check Shizuku authorization and device support."
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                if (result != AppLauncher.Result.FAILED && prefs.getBoolean("auto_bubble", true) && isOverlayRunning()) {
                    val update = Intent(this, FloatingService::class.java).apply {
                        action = FloatingService.ACTION_SET_LAST_APP
                        putExtra(FloatingService.EXTRA_PACKAGE, app.packageName)
                        putExtra(FloatingService.EXTRA_ACTIVITY, app.activityName)
                        putExtra(FloatingService.EXTRA_LABEL, app.label)
                    }
                    startServiceCompat(update)
                }
            }
        }.start()
    }

    private fun buildSettingsPage(): View {
        val scroll = ScrollView(this).apply { clipToPadding = false }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 16f))
        }
        scroll.addView(content)
        content.addView(UiKit.sectionTitle(this, "Settings", "Tune the launcher without clutter."))
        UiKit.addGap(this, content, 12)

        val setup = UiKit.card(this, 15)
        setup.addView(UiKit.text(this, "Setup checklist", 15f, UiKit.TEXT, true))
        UiKit.addGap(this, setup, 4)
        val shizukuReady = try { Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED } catch (_: Throwable) { false }
        addSetupRow(setup, "Shizuku authorization", shizukuReady, if (shizukuReady) "Authorized" else "Connect and authorize", ::handleShizukuAction)
        val overlayReady = Settings.canDrawOverlays(this)
        addSetupRow(setup, "Display over other apps", overlayReady, if (overlayReady) "Allowed" else "Required for floating controls") {
            try { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) }
            catch (_: Throwable) { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) }
        }
        val notificationsReady = Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        addSetupRow(setup, "Notifications", notificationsReady, if (notificationsReady) "Allowed" else "Recommended for service status") {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_REQUEST_CODE)
            } else {
                val intent = if (Build.VERSION.SDK_INT >= 26) Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                startActivity(intent)
            }
        }
        val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val batteryUnrestricted = Build.VERSION.SDK_INT < 23 || try { powerManager.isIgnoringBatteryOptimizations(packageName) } catch (_: Throwable) { false }
        addSetupRow(setup, "Battery management", batteryUnrestricted, if (batteryUnrestricted) "Not optimized" else "Check OEM battery settings") {
            try { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
            catch (_: Throwable) { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) }
        }
        content.addView(setup)

        UiKit.addGap(this, content, 12)
        val general = UiKit.card(this, 15)
        general.addView(UiKit.text(this, "Launcher", 15f, UiKit.TEXT, true))
        addSwitch(general, "Enable edge handle", "Show a narrow handle fixed to the screen edge.", "edge_enabled", true)
        addSwitch(general, "Show app bubble", "Show a draggable shortcut bubble for the last selected app.", "auto_bubble", true)
        addSwitch(general, "Hide games", "Filter apps marked as games and common game titles.", "hide_games", true)
        addSwitch(general, "Compact app list", "Use tighter spacing in the floating launcher.", "compact_list", false)
        content.addView(general)

        UiKit.addGap(this, content, 12)
        val system = UiKit.card(this, 15)
        system.addView(UiKit.text(this, "Window behavior", 15f, UiKit.TEXT, true))
        addSwitch(system, "Try freeform with Shizuku", "Ask Android to launch selected apps in windowing mode 5.", "try_freeform", true)
        addSwitch(system, "Fallback to fullscreen", "Open normally if the freeform request is rejected.", "fallback_fullscreen", true)
        addSwitch(system, "Smooth overlay animation", "Use short fade and scale transitions.", "animations", true)
        addSwitch(system, "Haptic feedback", "Small vibration on handle and bubble interactions.", "haptics", true)
        content.addView(system)

        UiKit.addGap(this, content, 12)
        val display = UiKit.card(this, 15)
        display.addView(UiKit.text(this, "Display", 15f, UiKit.TEXT, true))
        val alphaLabel = UiKit.text(this, "Handle opacity  ·  ${prefs.getInt("handle_alpha", 92)}%", 12f, UiKit.MUTED)
        display.addView(alphaLabel)
        val slider = SeekBar(this).apply {
            max = 100
            progress = prefs.getInt("handle_alpha", 92)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val adjusted = progress.coerceIn(35, 100)
                    prefs.edit().putInt("handle_alpha", adjusted).apply()
                    alphaLabel.text = "Handle opacity  ·  ${adjusted}%"
                    refreshOverlayService()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
        display.addView(slider)
        addSwitch(display, "Pin handle to left edge", "Off places it on the right edge. Dragging remains vertical-only.", "handle_left_edge", true)
        content.addView(display)

        UiKit.addGap(this, content, 12)
        val maintenance = UiKit.card(this, 15)
        maintenance.addView(UiKit.text(this, "Permissions & system", 15f, UiKit.TEXT, true))
        val overlayButton = UiKit.action(this, "Manage display-over-other-apps")
        overlayButton.setOnClickListener {
            try { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) }
            catch (_: Throwable) { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) }
        }
        maintenance.addView(overlayButton, LinearLayout.LayoutParams(-1, -2))
        UiKit.addGap(this, maintenance, 8)
        val notifButton = UiKit.action(this, "Open notification settings")
        notifButton.setOnClickListener {
            val intent = if (Build.VERSION.SDK_INT >= 26) Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            startActivity(intent)
        }
        maintenance.addView(notifButton, LinearLayout.LayoutParams(-1, -2))
        UiKit.addGap(this, maintenance, 8)
        val devOptionsButton = UiKit.action(this, "Open Android developer options")
        devOptionsButton.setOnClickListener {
            try { startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
            catch (_: Throwable) { Toast.makeText(this, "Developer options are not available on this build.", Toast.LENGTH_LONG).show() }
        }
        maintenance.addView(devOptionsButton, LinearLayout.LayoutParams(-1, -2))
        UiKit.addGap(this, maintenance, 8)
        val stopButton = UiKit.action(this, "Stop MuruFloat service")
        stopButton.setOnClickListener { stopOverlayService(); Toast.makeText(this, "Floating service stopped.", Toast.LENGTH_SHORT).show() }
        maintenance.addView(stopButton, LinearLayout.LayoutParams(-1, -2))
        UiKit.addGap(this, maintenance, 10)
        maintenance.addView(UiKit.text(this, "MuruFloat does not read message content or app data. It only reads launcher entries, labels and icons to build the installed-app list.", 10f, UiKit.MUTED))
        content.addView(maintenance)
        return scroll
    }

    private fun addSetupRow(parent: LinearLayout, title: String, ready: Boolean, detail: String, action: () -> Unit) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, UiKit.dp(this@MainActivity, 9f), 0, UiKit.dp(this@MainActivity, 9f))
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
        val labels = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        labels.addView(UiKit.text(this, title, 12f, UiKit.TEXT, true))
        val detailView = UiKit.text(this, detail, 10f, UiKit.MUTED)
        val detailLp = LinearLayout.LayoutParams(-1, -2).apply { topMargin = UiKit.dp(this@MainActivity, 3f) }
        labels.addView(detailView, detailLp)
        row.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
        val status = statusChip(if (ready) "Ready" else "Action", if (ready) UiKit.GREEN else UiKit.YELLOW)
        row.addView(status)
        row.addView(UiKit.text(this, "  ›", 17f, UiKit.MUTED))
        parent.addView(row)
        parent.addView(UiKit.divider(this))
    }

    private fun addSwitch(parent: LinearLayout, title: String, description: String, key: String, defaultValue: Boolean) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, UiKit.dp(this@MainActivity, 10f), 0, UiKit.dp(this@MainActivity, 7f))
        }
        val labels = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        labels.addView(UiKit.text(this, title, 12f, UiKit.TEXT, true))
        val desc = UiKit.text(this, description, 10f, UiKit.MUTED)
        val descLp = LinearLayout.LayoutParams(-1, -2).apply { topMargin = UiKit.dp(this@MainActivity, 4f) }
        labels.addView(desc, descLp)
        row.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
        val toggle = Switch(this).apply {
            isChecked = prefs.getBoolean(key, defaultValue)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(key, checked).apply()
                if (key == "edge_enabled" && checked && !isOverlayRunning()) requestOverlayPermissionAndStart()
                if (key == "edge_enabled" && !checked) stopOverlayService()
                else refreshOverlayService()
                if (key == "hide_games" && currentPage == "apps") renderCurrentPage()
            }
        }
        row.addView(toggle)
        parent.addView(row)
        parent.addView(UiKit.divider(this))
    }

    private fun statusChip(label: String, color: Int): TextView = UiKit.text(this, label.uppercase(), 9f, color, true).apply {
        background = UiKit.rounded(UiKit.SURFACE_2, UiKit.dp(this@MainActivity, 20f).toFloat())
        setPadding(UiKit.dp(this@MainActivity, 9f), UiKit.dp(this@MainActivity, 6f), UiKit.dp(this@MainActivity, 9f), UiKit.dp(this@MainActivity, 6f))
    }

    private fun shizukuStatus(): Triple<String, Int, String> {
        val connected = try { Shizuku.pingBinder() } catch (_: Throwable) { false }
        if (!connected) return Triple("Offline", UiKit.YELLOW, "Start Shizuku first. When Shizuku is running, authorize MuruFloat here.")
        val authorized = try { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED } catch (_: Throwable) { false }
        return if (authorized) Triple("Authorized", UiKit.GREEN, "MuruFloat has Shizuku permission. Shell commands are still limited by Android's ADB permissions.")
        else Triple("Connected", UiKit.YELLOW, "Shizuku is running, but MuruFloat has not been authorized yet.")
    }

    private fun handleShizukuAction() {
        val connected = try { Shizuku.pingBinder() } catch (_: Throwable) { false }
        if (!connected) {
            val launchIntent = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
            if (launchIntent != null) startActivity(launchIntent)
            else startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")))
            Toast.makeText(this, "Start Shizuku, then return here.", Toast.LENGTH_LONG).show()
            return
        }
        val authorized = try { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED } catch (_: Throwable) { false }
        if (authorized) {
            Toast.makeText(this, "MuruFloat is already authorized.", Toast.LENGTH_SHORT).show()
        } else {
            try { Shizuku.requestPermission(SHIZUKU_REQUEST_CODE) }
            catch (_: Throwable) { Toast.makeText(this, "Could not request Shizuku permission. Reopen Shizuku and try again.", Toast.LENGTH_LONG).show() }
        }
        renderCurrentPage()
    }

    private fun requestOverlayPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            pendingOverlayEnable = true
            try {
                startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")), OVERLAY_REQUEST_CODE)
            } catch (_: Throwable) {
                pendingOverlayEnable = false
                Toast.makeText(this, "Open App info and allow Display over other apps.", Toast.LENGTH_LONG).show()
            }
            return
        }
        startOverlayService()
    }

    private fun startOverlayService() {
        prefs.edit().putBoolean("overlay_enabled", true).apply()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_REQUEST_CODE)
        }
        val intent = Intent(this, FloatingService::class.java).apply { action = FloatingService.ACTION_START }
        startServiceCompat(intent)
        Toast.makeText(this, "MuruFloat overlay enabled.", Toast.LENGTH_SHORT).show()
        renderCurrentPage()
        handler.postDelayed({ renderCurrentPage() }, 350L)
    }

    private fun stopOverlayService() {
        prefs.edit().putBoolean("overlay_enabled", false).apply()
        stopService(Intent(this, FloatingService::class.java))
        renderCurrentPage()
        handler.postDelayed({ renderCurrentPage() }, 250L)
    }

    private fun refreshOverlayService() {
        if (!isOverlayRunning()) return
        startServiceCompat(Intent(this, FloatingService::class.java).apply { action = FloatingService.ACTION_REFRESH })
    }

    private fun startServiceCompat(intent: Intent) {
        try {
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
        } catch (_: Throwable) {
            try { startService(intent) } catch (_: Throwable) { }
        }
    }

    private fun isOverlayRunning(): Boolean = FloatingService.isActive && prefs.getBoolean("overlay_enabled", false) && Settings.canDrawOverlays(this)

    companion object {
        private const val SHIZUKU_REQUEST_CODE = 4201
        private const val OVERLAY_REQUEST_CODE = 4202
        private const val NOTIFICATION_REQUEST_CODE = 4203
    }
}

private class SimpleTextWatcher(private val callback: (String) -> Unit) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { callback(s?.toString().orEmpty()) }
    override fun afterTextChanged(s: android.text.Editable?) = Unit
}
