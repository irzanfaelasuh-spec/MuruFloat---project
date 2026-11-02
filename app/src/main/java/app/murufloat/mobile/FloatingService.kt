package app.murufloat.mobile

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class FloatingService : Service() {
    private lateinit var windowManager: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("murufloat", MODE_PRIVATE) }

    private var handleView: View? = null
    private var handleParams: WindowManager.LayoutParams? = null
    private var panelView: View? = null
    private var launchStatusView: View? = null
    private var bubbleView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var lastApp: LaunchableApp? = null
    private var selectedCategory = "All"
    private var currentSearch = ""

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        isActive = true
        createNotificationChannel()
        enterForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                prefs.edit().putBoolean("overlay_enabled", false).apply()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SET_LAST_APP -> {
                val pkg = intent.getStringExtra(EXTRA_PACKAGE)
                val activity = intent.getStringExtra(EXTRA_ACTIVITY)
                val label = intent.getStringExtra(EXTRA_LABEL) ?: pkg.orEmpty()
                if (!pkg.isNullOrBlank() && !activity.isNullOrBlank()) {
                    rememberLastApp(pkg, activity, label)
                    if (prefs.getBoolean("auto_bubble", true)) showBubbleForLastApp()
                }
            }
            ACTION_REFRESH -> {
                removeHandle()
                if (prefs.getBoolean("edge_enabled", true)) showHandle()
                if (prefs.getBoolean("auto_bubble", true) && !prefs.getString("last_package", null).isNullOrBlank()) showBubbleForLastApp()
                else removeBubble()
            }
            else -> {
                if (Settings.canDrawOverlays(this) && prefs.getBoolean("edge_enabled", true)) showHandle()
                if (prefs.getBoolean("auto_bubble", true) && !prefs.getString("last_package", null).isNullOrBlank()) showBubbleForLastApp()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        removePanel()
        removeLaunchIndicator()
        removeHandle()
        removeBubble()
        try { stopForeground(STOP_FOREGROUND_REMOVE) } catch (_: Throwable) { }
        isActive = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun enterForeground() {
        val openApp = PendingIntent.getActivity(
            this, 20, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setContentIntent(openApp)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setContentIntent(openApp)
                .setOngoing(true)
                .build()
        }
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(CHANNEL_ID, getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
                description = "Keeps the user-enabled MuruFloat controls available."
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun showHandle() {
        if (!Settings.canDrawOverlays(this) || handleView != null) return
        val handle = FrameLayout(this).apply {
            background = UiKit.rounded(UiKit.SURFACE_2, UiKit.dp(this@FloatingService, 18f).toFloat(), UiKit.LINE)
            alpha = (prefs.getInt("handle_alpha", 92) / 100f).coerceIn(0.35f, 1f)
            addView(GripView(this@FloatingService), FrameLayout.LayoutParams(-1, -1))
        }
        val params = WindowManager.LayoutParams(
            UiKit.dp(this, 34f), UiKit.dp(this, 112f),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = handleEdgeX(width) // The handle stays edge-anchored; only its Y position is draggable.
            val maxY = (screenHeight() - height).coerceAtLeast(0)
            y = prefs.getInt("handle_y", screenHeight() / 2 - height / 2).coerceIn(0, maxY)
        }
        attachDragHandle(handle, params)
        try {
            windowManager.addView(handle, params)
            handleView = handle
            handleParams = params
            if (prefs.getBoolean("animations", true)) {
                handle.alpha = 0f
                handle.scaleX = 0.86f
                handle.animate().alpha((prefs.getInt("handle_alpha", 92) / 100f).coerceIn(0.35f, 1f)).scaleX(1f).setDuration(190L).start()
            }
        } catch (_: Throwable) {
            handleView = null
            handleParams = null
        }
    }

    private fun attachDragHandle(view: View, params: WindowManager.LayoutParams) {
        var downRawY = 0f
        var startY = 0
        var moved = false
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downRawY = event.rawY
                    startY = params.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val delta = (event.rawY - downRawY).toInt()
                    if (kotlin.math.abs(delta) > touchSlop) moved = true
                    params.x = handleEdgeX(params.width)
                    params.y = (startY + delta).coerceIn(0, (screenHeight() - params.height).coerceAtLeast(0))
                    try { windowManager.updateViewLayout(view, params) } catch (_: Throwable) { }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    prefs.edit().putInt("handle_y", params.y).apply()
                    if (!moved) {
                        if (prefs.getBoolean("haptics", true)) view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                        showLauncher()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
    }

    private fun removeHandle() {
        val view = handleView ?: return
        handleView = null
        handleParams = null
        if (prefs.getBoolean("animations", true)) {
            view.animate().alpha(0f).scaleX(0.85f).setDuration(150L).withEndAction {
                try { windowManager.removeView(view) } catch (_: Throwable) { }
            }.start()
        } else {
            try { windowManager.removeView(view) } catch (_: Throwable) { }
        }
    }

    private fun showLauncher() {
        if (panelView != null || !Settings.canDrawOverlays(this)) return
        val root = FrameLayout(this).apply {
            setBackgroundColor(0x78030810)
            setOnClickListener { removePanel() }
            alpha = 0f
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = UiKit.rounded(UiKit.BG, UiKit.dp(this@FloatingService, 22f).toFloat(), UiKit.LINE)
            setPadding(UiKit.dp(this@FloatingService, 16f), UiKit.dp(this@FloatingService, 15f), UiKit.dp(this@FloatingService, 16f), UiKit.dp(this@FloatingService, 12f))
            setOnClickListener { /* consume taps inside the panel */ }
        }
        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels
        val width = minOf(UiKit.dp(this, 360f), screenW - UiKit.dp(this, 24f))
        val height = minOf(UiKit.dp(this, 570f), screenH - UiKit.dp(this, 90f))
        root.addView(panel, FrameLayout.LayoutParams(width, height, Gravity.CENTER))

        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val titleBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleBox.addView(UiKit.text(this, "MuruFloat", 18f, UiKit.TEXT, true))
        val subtitle = UiKit.text(this, "Installed apps", 10f, UiKit.MUTED)
        val subLp = LinearLayout.LayoutParams(-1, -2).apply { topMargin = UiKit.dp(this@FloatingService, 3f) }
        titleBox.addView(subtitle, subLp)
        header.addView(titleBox, LinearLayout.LayoutParams(0, -2, 1f))
        val close = UiKit.action(this, "×")
        close.setOnClickListener { removePanel() }
        header.addView(close, LinearLayout.LayoutParams(UiKit.dp(this, 42f), UiKit.dp(this, 38f)))
        panel.addView(header)

        val search = EditText(this).apply {
            hint = "Search apps"
            setHintTextColor(UiKit.MUTED)
            setTextColor(UiKit.TEXT)
            textSize = 13f
            singleLine = true
            background = UiKit.rounded(UiKit.SURFACE, UiKit.dp(this@FloatingService, 12f).toFloat(), UiKit.LINE)
            setPadding(UiKit.dp(this@FloatingService, 12f), UiKit.dp(this@FloatingService, 10f), UiKit.dp(this@FloatingService, 12f), UiKit.dp(this@FloatingService, 10f))
        }
        val searchLp = LinearLayout.LayoutParams(-1, UiKit.dp(this, 44f)).apply { topMargin = UiKit.dp(this@FloatingService, 13f) }
        panel.addView(search, searchLp)

        val categories = listOf("All", "Social", "Media", "Browser", "Communication", "Other")
        val chipScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val chips = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        categories.forEach { category ->
            val chip = UiKit.text(this, category, 10f, if (category == selectedCategory) UiKit.BG else UiKit.MUTED, category == selectedCategory).apply {
                gravity = Gravity.CENTER
                background = UiKit.rounded(if (category == selectedCategory) UiKit.ACCENT else UiKit.SURFACE_2, UiKit.dp(this@FloatingService, 14f).toFloat())
                setPadding(UiKit.dp(this@FloatingService, 11f), UiKit.dp(this@FloatingService, 8f), UiKit.dp(this@FloatingService, 11f), UiKit.dp(this@FloatingService, 8f))
                setOnClickListener {
                    selectedCategory = category
                    // Rebuild the launcher to keep category state and selected style in sync.
                    removePanel()
                    handler.postDelayed({ showLauncher() }, 175L)
                }
            }
            chips.addView(chip, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = UiKit.dp(this@FloatingService, 6f) })
        }
        chipScroll.addView(chips)
        val chipLp = LinearLayout.LayoutParams(-1, -2).apply { topMargin = UiKit.dp(this, 10f) }
        panel.addView(chipScroll, chipLp)

        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val listScroll = ScrollView(this).apply { clipToPadding = false }
        listScroll.addView(list)
        val listLp = LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = UiKit.dp(this@FloatingService, 7f) }
        panel.addView(listScroll, listLp)

        fun populate(query: String) {
            list.removeAllViews()
            val apps = AppCatalog.load(this, prefs.getBoolean("hide_games", true)).filter {
                (selectedCategory == "All" || it.category == selectedCategory) &&
                    (it.label.contains(query, true) || it.packageName.contains(query, true))
            }
            if (apps.isEmpty()) {
                val empty = UiKit.text(this, "No apps in this category.", 12f, UiKit.MUTED)
                empty.gravity = Gravity.CENTER
                list.addView(empty, LinearLayout.LayoutParams(-1, UiKit.dp(this, 86f)))
            } else {
                apps.forEachIndexed { index, app ->
                    list.addView(launcherAppRow(app) {
                        removePanel()
                        launchApp(app)
                    })
                    if (index != apps.lastIndex) list.addView(UiKit.divider(this))
                }
            }
        }
        populate("")
        search.addTextChangedListener(OverlayTextWatcher { query -> currentSearch = query; populate(query) })
        val foot = UiKit.text(this, "Tap an app for a window request  ·  long-press for fullscreen", 9f, UiKit.MUTED)
        foot.gravity = Gravity.CENTER
        val footLp = LinearLayout.LayoutParams(-1, -2).apply { topMargin = UiKit.dp(this@FloatingService, 6f) }
        panel.addView(foot, footLp)

        root.setOnClickListener { removePanel() }
        panel.setOnClickListener { }
        val params = WindowManager.LayoutParams(
            screenW, screenH,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            dimAmount = 0.24f
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        try {
            windowManager.addView(root, params)
            panelView = root
            root.animate().alpha(1f).setDuration(if (prefs.getBoolean("animations", true)) 160L else 0L).start()
            search.requestFocus()
        } catch (_: Throwable) {
            panelView = null
        }
    }

    private fun launcherAppRow(app: LaunchableApp, onClick: () -> Unit): View {
        val compact = prefs.getBoolean("compact_list", false)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(UiKit.dp(this@FloatingService, 4f), UiKit.dp(this@FloatingService, if (compact) 5f else 8f), UiKit.dp(this@FloatingService, 4f), UiKit.dp(this@FloatingService, if (compact) 5f else 8f))
            setOnClickListener { onClick() }
        }
        val icon = ImageView(this).apply { setImageDrawable(app.icon) }
        row.addView(icon, LinearLayout.LayoutParams(UiKit.dp(this, 36f), UiKit.dp(this, 36f)))
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        info.addView(UiKit.text(this, app.label, 12f, UiKit.TEXT, true))
        val sub = UiKit.text(this, app.category, 9f, UiKit.MUTED)
        val subLp = LinearLayout.LayoutParams(-1, -2).apply { topMargin = UiKit.dp(this@FloatingService, 3f) }
        info.addView(sub, subLp)
        row.addView(info, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = UiKit.dp(this@FloatingService, 10f) })
        row.addView(UiKit.text(this, "OPEN", 9f, UiKit.ACCENT, true))
        row.setOnLongClickListener {
            removePanel()
            launchApp(app, fullscreen = true)
            true
        }
        return row
    }

    private fun removePanel() {
        val view = panelView ?: return
        panelView = null
        try { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(view.windowToken, 0) } catch (_: Throwable) { }
        if (prefs.getBoolean("animations", true)) {
            view.animate().alpha(0f).setDuration(140L).withEndAction {
                try { windowManager.removeView(view) } catch (_: Throwable) { }
            }.start()
        } else {
            try { windowManager.removeView(view) } catch (_: Throwable) { }
        }
    }

    private fun launchApp(app: LaunchableApp, fullscreen: Boolean = false) {
        showLaunchIndicator(app.label)
        Thread {
            val result = AppLauncher.open(
                applicationContext, app,
                tryFreeform = prefs.getBoolean("try_freeform", true) && !fullscreen,
                fullscreen = fullscreen,
                allowFallback = prefs.getBoolean("fallback_fullscreen", true)
            )
            handler.post {
                removeLaunchIndicator()
                val message = when (result) {
                    AppLauncher.Result.FREEFORM_REQUESTED -> "Freeform request sent. Window support depends on this ROM."
                    AppLauncher.Result.FULLSCREEN -> "Opening ${app.label} fullscreen."
                    AppLauncher.Result.FAILED -> "Could not open ${app.label}. Check Shizuku and device support."
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                if (result != AppLauncher.Result.FAILED) {
                    rememberLastApp(app.packageName, app.activityName, app.label)
                    if (prefs.getBoolean("auto_bubble", true)) handler.postDelayed({ showBubbleForLastApp() }, 240L)
                }
            }
        }.start()
    }

    private fun showLaunchIndicator(label: String) {
        if (!Settings.canDrawOverlays(this)) return
        removeLaunchIndicator()
        val chip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = UiKit.rounded(UiKit.SURFACE_2, UiKit.dp(this@FloatingService, 16f).toFloat(), UiKit.LINE)
            setPadding(UiKit.dp(this@FloatingService, 14f), UiKit.dp(this@FloatingService, 11f), UiKit.dp(this@FloatingService, 16f), UiKit.dp(this@FloatingService, 11f))
            alpha = 0f
            addView(android.widget.ProgressBar(this@FloatingService).apply {
                isIndeterminate = true
                indeterminateTintList = android.content.res.ColorStateList.valueOf(UiKit.ACCENT)
            }, LinearLayout.LayoutParams(UiKit.dp(this@FloatingService, 19f), UiKit.dp(this@FloatingService, 19f)))
            addView(UiKit.text(this@FloatingService, "Opening $label…", 11f, UiKit.TEXT, true), LinearLayout.LayoutParams(-2, -2).apply {
                leftMargin = UiKit.dp(this@FloatingService, 10f)
            })
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = UiKit.dp(this@FloatingService, 58f)
        }
        try {
            windowManager.addView(chip, params)
            launchStatusView = chip
            chip.animate().alpha(1f).setDuration(if (prefs.getBoolean("animations", true)) 140L else 0L).start()
        } catch (_: Throwable) { launchStatusView = null }
    }

    private fun removeLaunchIndicator() {
        val view = launchStatusView ?: return
        launchStatusView = null
        if (prefs.getBoolean("animations", true)) {
            view.animate().alpha(0f).setDuration(110L).withEndAction {
                try { windowManager.removeView(view) } catch (_: Throwable) { }
            }.start()
        } else {
            try { windowManager.removeView(view) } catch (_: Throwable) { }
        }
    }

    private fun rememberLastApp(pkg: String, activity: String, label: String) {
        prefs.edit().putString("last_package", pkg).putString("last_activity", activity).putString("last_label", label).apply()
    }

    private fun loadLastApp(): LaunchableApp? {
        val pkg = prefs.getString("last_package", null) ?: return null
        val activity = prefs.getString("last_activity", null) ?: return null
        val label = prefs.getString("last_label", pkg) ?: pkg
        return try {
            val pm = packageManager
            val appInfo = if (Build.VERSION.SDK_INT >= 33) pm.getApplicationInfo(pkg, android.content.pm.PackageManager.ApplicationInfoFlags.of(0)) else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(pkg, 0)
            }
            val icon = pm.getApplicationIcon(appInfo)
            LaunchableApp(label, pkg, activity, icon, "App", (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0)
        } catch (_: Throwable) { null }
    }

    private fun showBubbleForLastApp() {
        if (!prefs.getBoolean("auto_bubble", true) || !Settings.canDrawOverlays(this)) {
            removeBubble()
            return
        }
        val app = loadLastApp() ?: return
        lastApp = app
        if (bubbleView != null) {
            val image = (bubbleView as? FrameLayout)?.getChildAt(0) as? ImageView
            image?.setImageDrawable(app.icon)
            return
        }
        val bubble = FrameLayout(this).apply {
            background = UiKit.rounded(UiKit.SURFACE_2, UiKit.dp(this@FloatingService, 30f).toFloat(), UiKit.LINE)
            elevation = UiKit.dp(this@FloatingService, 8f).toFloat()
        }
        val icon = ImageView(this).apply {
            setImageDrawable(app.icon)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(UiKit.dp(this@FloatingService, 8f), UiKit.dp(this@FloatingService, 8f), UiKit.dp(this@FloatingService, 8f), UiKit.dp(this@FloatingService, 8f))
        }
        bubble.addView(icon, FrameLayout.LayoutParams(-1, -1))
        val size = UiKit.dp(this, 58f)
        val params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.getInt("bubble_x", (screenWidth() - size - UiKit.dp(this@FloatingService, 12f)).coerceAtLeast(0))
            y = prefs.getInt("bubble_y", screenHeight() / 2 - size / 2).coerceIn(0, (screenHeight() - size).coerceAtLeast(0))
        }
        attachBubbleTouch(bubble, params)
        try {
            windowManager.addView(bubble, params)
            bubbleView = bubble
            bubbleParams = params
            if (prefs.getBoolean("animations", true)) {
                bubble.alpha = 0f
                bubble.scaleX = 0.55f
                bubble.scaleY = 0.55f
                bubble.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(190L).start()
            }
        } catch (_: Throwable) {
            bubbleView = null
            bubbleParams = null
        }
    }

    private fun attachBubbleTouch(view: View, params: WindowManager.LayoutParams) {
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var moved = false
        var longPressed = false
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        val longPress = Runnable {
            if (!moved) {
                longPressed = true
                dismissBubble()
            }
        }
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = params.x
                    startY = params.y
                    moved = false
                    longPressed = false
                    handler.postDelayed(longPress, ViewConfiguration.getLongPressTimeout().toLong())
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    if (kotlin.math.abs(dx) > slop || kotlin.math.abs(dy) > slop) {
                        moved = true
                        handler.removeCallbacks(longPress)
                    }
                    params.x = (startX + dx).coerceIn(0, (screenWidth() - params.width).coerceAtLeast(0))
                    params.y = (startY + dy).coerceIn(0, (screenHeight() - params.height).coerceAtLeast(0))
                    try { windowManager.updateViewLayout(view, params) } catch (_: Throwable) { }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(longPress)
                    prefs.edit().putInt("bubble_x", params.x).putInt("bubble_y", params.y).apply()
                    if (!moved && !longPressed) {
                        if (prefs.getBoolean("haptics", true)) view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                        val app = lastApp ?: loadLastApp()
                        if (app != null) launchApp(app) else showLauncher()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(longPress)
                    true
                }
                else -> true
            }
        }
    }

    private fun dismissBubble() {
        prefs.edit().remove("last_package").remove("last_activity").remove("last_label").apply()
        lastApp = null
        removeBubble()
    }

    private fun removeBubble() {
        val view = bubbleView ?: return
        bubbleView = null
        bubbleParams = null
        if (prefs.getBoolean("animations", true)) {
            view.animate().alpha(0f).scaleX(0.55f).scaleY(0.55f).setDuration(150L).withEndAction {
                try { windowManager.removeView(view) } catch (_: Throwable) { }
            }.start()
        } else {
            try { windowManager.removeView(view) } catch (_: Throwable) { }
        }
    }

    private fun handleEdgeX(width: Int): Int = if (prefs.getBoolean("handle_left_edge", true)) 0 else (screenWidth() - width).coerceAtLeast(0)
    private fun screenWidth(): Int = resources.displayMetrics.widthPixels
    private fun screenHeight(): Int = resources.displayMetrics.heightPixels

    private class GripView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = UiKit.dp(context, 2.2f).toFloat()
            strokeCap = Paint.Cap.ROUND
        }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val centerX = width / 2f
            val gap = UiKit.dp(context, 7f).toFloat()
            val radius = UiKit.dp(context, 1.3f).toFloat()
            for (i in -1..1) canvas.drawCircle(centerX, height / 2f + i * gap, radius, paint)
        }
    }

    companion object {
        const val ACTION_START = "app.murufloat.mobile.action.START"
        const val ACTION_STOP = "app.murufloat.mobile.action.STOP"
        const val ACTION_REFRESH = "app.murufloat.mobile.action.REFRESH"
        const val ACTION_SET_LAST_APP = "app.murufloat.mobile.action.SET_LAST_APP"
        const val EXTRA_PACKAGE = "package"
        const val EXTRA_ACTIVITY = "activity"
        const val EXTRA_LABEL = "label"
        @Volatile var isActive: Boolean = false
            private set
        private const val CHANNEL_ID = "murufloat_overlay"
        private const val NOTIFICATION_ID = 7314
    }
}

private class OverlayTextWatcher(private val callback: (String) -> Unit) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { callback(s?.toString().orEmpty()) }
    override fun afterTextChanged(s: android.text.Editable?) = Unit
}
