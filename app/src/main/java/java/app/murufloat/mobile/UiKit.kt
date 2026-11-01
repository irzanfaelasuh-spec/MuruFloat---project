package app.murufloat.mobile

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

object UiKit {
    const val BG = 0xFF0A0D12.toInt()
    const val SURFACE = 0xFF121923.toInt()
    const val SURFACE_2 = 0xFF182231.toInt()
    const val LINE = 0xFF293647.toInt()
    const val TEXT = 0xFFE8EEF8.toInt()
    const val MUTED = 0xFF94A3B8.toInt()
    const val ACCENT = 0xFFA5B4FC.toInt()
    const val GREEN = 0xFF5ED6A0.toInt()
    const val YELLOW = 0xFFF2C66D.toInt()
    const val RED = 0xFFF28C8C.toInt()

    fun dp(context: Context, value: Float): Int = (value * context.resources.displayMetrics.density).toInt()

    fun rounded(color: Int, radius: Float, stroke: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
            if (stroke != null) setStroke(1, stroke)
        }

    fun text(context: Context, value: CharSequence, size: Float = 14f, color: Int = TEXT, bold: Boolean = false): TextView =
        TextView(context).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            includeFontPadding = false
        }

    fun sectionTitle(context: Context, title: String, subtitle: String? = null): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(text(context, title, 17f, TEXT, true))
            if (!subtitle.isNullOrBlank()) {
                val sub = text(context, subtitle, 12f, MUTED)
                val lp = LinearLayout.LayoutParams(-1, -2)
                lp.topMargin = dp(context, 5f)
                addView(sub, lp)
            }
        }

    fun card(context: Context, padding: Int = 16): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(SURFACE, dp(context, 18f).toFloat(), LINE)
            setPadding(dp(context, padding.toFloat()), dp(context, padding.toFloat()), dp(context, padding.toFloat()), dp(context, padding.toFloat()))
        }

    fun action(context: Context, title: String, primary: Boolean = false): TextView =
        text(context, title, 13f, if (primary) BG else TEXT, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(context, 15f), dp(context, 11f), dp(context, 15f), dp(context, 11f))
            background = rounded(if (primary) ACCENT else SURFACE_2, dp(context, 12f).toFloat(), if (primary) null else LINE)
            isClickable = true
            isFocusable = true
        }

    fun divider(context: Context): View = View(context).apply {
        setBackgroundColor(LINE)
        layoutParams = LinearLayout.LayoutParams(-1, dp(context, 1f)).apply {
            topMargin = dp(context, 12f)
            bottomMargin = dp(context, 12f)
        }
    }

    fun row(context: Context, horizontal: Boolean = true): LinearLayout = LinearLayout(context).apply {
        orientation = if (horizontal) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
        gravity = if (horizontal) Gravity.CENTER_VERTICAL else Gravity.START
    }

    fun addGap(context: Context, parent: LinearLayout, height: Int = 10) {
        parent.addView(View(context), LinearLayout.LayoutParams(1, dp(context, height.toFloat())))
    }
}
