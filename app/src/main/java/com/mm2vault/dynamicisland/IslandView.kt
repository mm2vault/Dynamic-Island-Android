package com.mm2vault.dynamicisland

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.TypedValue
import android.view.MotionEvent
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.PathInterpolator
import com.mm2vault.dynamicisland.data.BatteryState
import com.mm2vault.dynamicisland.data.ChargeState
import com.mm2vault.dynamicisland.data.IslandSettings
import com.mm2vault.dynamicisland.data.PlugType
import com.mm2vault.dynamicisland.live.MediaInfo
import com.mm2vault.dynamicisland.live.NoticeInfo
import java.util.Locale

/**
 * Canvas-drawn capsule. No child views, no blur: a few strokes and one gradient,
 * so it stays cheap on mid-range devices. Only animates while changing state.
 */
class IslandView(context: Context) : View(context) {
    var settings = IslandSettings()
        set(value) { field = value.sanitized(); requestLayout(); invalidate() }
    var battery = BatteryState()
        set(value) { field = value; updateDescription(); invalidate() }
    var onExpandedChanged: ((Boolean) -> Unit)? = null

    /** Current music session, or null. Controls are only drawn and hit-tested while this is set. */
    var media: MediaInfo? = null
        set(value) { field = value; invalidate() }
    /** 0 = previous, 1 = play/pause, 2 = next. */
    var onMediaAction: ((Int) -> Unit)? = null

    private var noticeActive = false
    var notice: NoticeInfo? = null
        set(value) {
            field = value
            removeCallbacks(noticeTimeout)
            if (value == null) { noticeActive = false; invalidate(); return }
            noticeActive = true
            if (!isExpanded()) setExpanded(true)
            postDelayed(noticeTimeout, NOTICE_MS)
            invalidate()
        }
    private val noticeTimeout = Runnable {
        noticeActive = false
        if (isExpanded()) setExpanded(false) else invalidate()
    }
    private val prevHit = RectF(); private val playHit = RectF(); private val nextHit = RectF()
    private val shape = Path()

    private var progress = 0f
    private var target = 0f
    private var press = 0f
    private var animator: ValueAnimator? = null
    private val d = resources.displayMetrics.density
    private val rect = RectF()
    private val boltPath = Path()

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val ringTrack = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val ringArc = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPrimary = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) }
    private val textSecondary = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create("sans-serif", Typeface.NORMAL) }

    private fun dp(v: Float) = v * d
    private fun sp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)
    private val glowPad get() = dp(14f)

    init {
        isClickable = true
        contentDescription = context.getString(R.string.overlay_cd)
    }

    private fun expandedWidth(): Float {
        val screen = resources.displayMetrics.widthPixels.toFloat()
        val want = maxOf(dp(settings.widthDp * 1.9f), dp(232f))
        return want.coerceAtMost(screen - dp(24f) - glowPad * 2)
    }

    private fun capsuleWidth() = dp(settings.widthDp.toFloat()) + (expandedWidth() - dp(settings.widthDp.toFloat())) * progress
    private fun capsuleHeight() = dp(settings.heightDp.toFloat()) + (dp(88f) - dp(settings.heightDp.toFloat())) * progress

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            (capsuleWidth() + glowPad * 2).toInt(),
            (capsuleHeight() + glowPad * 2).toInt()
        )
    }

    fun isExpanded() = target >= 1f

    fun toggle() = setExpanded(!isExpanded())

    fun setExpanded(expanded: Boolean, animate: Boolean = true) {
        if (!expanded) { noticeActive = false; removeCallbacks(noticeTimeout) }
        target = if (expanded) 1f else 0f
        animator?.cancel()
        if (!animate || settings.animation <= 0.02f) {
            progress = target; requestLayout(); invalidate(); onExpandedChanged?.invoke(expanded); return
        }
        val dist = kotlin.math.abs(target - progress).coerceAtLeast(0.2f)
        animator = ValueAnimator.ofFloat(progress, target).apply {
            duration = ((180 + 220 * settings.animation) * dist).toLong().coerceAtLeast(120)
            interpolator = if (expanded) PathInterpolator(0.2f, 1.25f, 0.4f, 1f) else PathInterpolator(0.3f, 0f, 0.2f, 1f)
            addUpdateListener { progress = it.animatedValue as Float; requestLayout(); invalidate() }
            start()
        }
        onExpandedChanged?.invoke(expanded)
    }

    fun playEntrance() {
        alpha = 0f; translationY = -dp(14f)
        animate().alpha(1f).translationY(0f)
            .setDuration((140 + 220 * settings.animation).toLong()).start()
    }

    fun playExit(end: () -> Unit) {
        animate().alpha(0f).translationY(-dp(10f)).setDuration(160).withEndAction(end).start()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { press = 1f; invalidate() }
            MotionEvent.ACTION_UP -> {
                press = 0f; invalidate()
                val action = mediaActionAt(event.x, event.y)
                if (action >= 0) {
                    if (settings.haptics) performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onMediaAction?.invoke(action)
                } else performClick()
            }
            MotionEvent.ACTION_CANCEL -> { press = 0f; invalidate() }
        }
        return true
    }

    private fun mediaActionAt(x: Float, y: Float): Int {
        if (!isExpanded() || progress < 0.9f || noticeActive || media == null) return -1
        return when {
            prevHit.contains(x, y) -> 0
            playHit.contains(x, y) -> 1
            nextHit.contains(x, y) -> 2
            else -> -1
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        if (settings.haptics) {
            performHapticFeedback(
                if (android.os.Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
            )
        }
        toggle()
        return true
    }

    private fun updateDescription() {
        val p = if (battery.percent >= 0) "${battery.percent}%" else ""
        contentDescription = context.getString(R.string.overlay_cd) + " " + p + " " + statusLabel()
    }

    private fun statusLabel() = context.getString(
        when (battery.charge) {
            ChargeState.CHARGING -> R.string.overlay_charging
            ChargeState.FULL -> R.string.overlay_full
            ChargeState.DISCHARGING -> R.string.overlay_discharging
            ChargeState.UNKNOWN -> R.string.overlay_unknown
        }
    )

    private fun detailLabel(): String {
        val parts = ArrayList<String>(2)
        if (battery.plug != PlugType.NONE) {
            parts += context.getString(
                when (battery.plug) {
                    PlugType.AC -> R.string.plug_ac
                    PlugType.USB -> R.string.plug_usb
                    PlugType.WIRELESS -> R.string.plug_wireless
                    else -> R.string.plug_unknown
                }
            )
        }
        if (battery.hasTemp) {
            parts += context.getString(R.string.temp_format, String.format(Locale("tr", "TR"), "%.1f", battery.tempTenths / 10f))
        }
        return parts.joinToString(" · ")
    }

    private fun fit(p: Paint, text: String, max: Float): String {
        var shown = text
        while (shown.length > 3 && p.measureText(shown) > max) shown = shown.dropLast(2)
        return if (shown != text) shown.trimEnd() + "…" else shown
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(noticeTimeout)
        animator?.cancel(); animator = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val w = capsuleWidth(); val h = capsuleHeight()
        val scale = 1f - 0.03f * press
        canvas.save()
        canvas.scale(scale, scale, width / 2f, height / 2f)
        rect.set(glowPad, glowPad, glowPad + w, glowPad + h)
        val r = h / 2f - if (progress > 0f) (h / 2f - dp(30f)).coerceAtLeast(0f) * progress else 0f
        val radius = r.coerceAtLeast(dp(20f)).coerceAtMost(h / 2f)

        // soft violet glow: three widening strokes, cheap and no blur
        val g = settings.glow
        if (g > 0.02f) {
            for (i in 1..3) {
                glowPaint.color = Color.argb((38 * g / i).toInt(), 169, 112, 255)
                glowPaint.strokeWidth = dp(3f * i)
                val inset = -dp(1.5f * i)
                canvas.drawRoundRect(rect.left - inset, rect.top - inset, rect.right + inset, rect.bottom + inset, radius - inset, radius - inset, glowPaint)
            }
        }
        fill.color = Color.rgb(3, 2, 6)
        canvas.drawRoundRect(rect, radius, radius, fill)
        rim.strokeWidth = dp(1f)
        rim.color = Color.argb((70 + 90 * g).toInt().coerceAtMost(200), 169, 112, 255)
        canvas.drawRoundRect(rect, radius, radius, rim)

        val cx = rect.left; val cy = rect.centerY()
        val cAlpha = ((1f - progress * 2f).coerceIn(0f, 1f) * 255).toInt()
        val eAlpha = ((progress * 2f - 1f).coerceIn(0f, 1f) * 255).toInt()
        val percentText = if (battery.percent >= 0) "${battery.percent}%" else "—"
        if (cAlpha > 0) drawCollapsed(canvas, cx, cy, w, cAlpha, percentText)
        if (eAlpha > 0) drawExpanded(canvas, cx, cy, w, eAlpha, percentText)
        canvas.restore()
    }

    private fun drawCollapsed(c: Canvas, left: Float, cy: Float, w: Float, a: Int, percent: String) {
        val charging = battery.charge == ChargeState.CHARGING
        val pad = dp(14f)
        if (charging) drawBolt(c, left + pad + dp(5f), cy, dp(7f), a) else {
            dot.color = Color.argb(a, 213, 184, 255)
            c.drawCircle(left + pad + dp(3f), cy, dp(3f), dot)
        }
        textPrimary.textSize = sp(13f)
        textPrimary.color = Color.argb(a, 247, 242, 255)
        textPrimary.textAlign = Paint.Align.RIGHT
        c.drawText(percent, left + w - pad, cy - (textPrimary.ascent() + textPrimary.descent()) / 2f, textPrimary)
    }

    private fun drawExpanded(c: Canvas, left: Float, cy: Float, w: Float, a: Int, percent: String) {
        val n = notice
        val m = media
        prevHit.setEmpty(); playHit.setEmpty(); nextHit.setEmpty()
        when {
            noticeActive && n != null -> drawNoticePage(c, left, cy, w, a, n)
            m != null -> drawMediaPage(c, left, cy, w, a, m, percent)
            else -> drawBatteryPage(c, left, cy, w, a, percent)
        }
    }

    private fun drawNoticePage(c: Canvas, left: Float, cy: Float, w: Float, a: Int, n: NoticeInfo) {
        val pad = dp(18f)
        val max = w - pad * 2
        val x = left + pad
        textSecondary.textAlign = Paint.Align.LEFT
        textSecondary.textSize = sp(11f)
        textSecondary.color = Color.argb(a, 213, 184, 255)
        c.drawText(fit(textSecondary, n.app.uppercase(Locale("tr", "TR")), max), x, cy - dp(14f), textSecondary)
        textPrimary.textAlign = Paint.Align.LEFT
        textPrimary.textSize = sp(15f)
        textPrimary.color = Color.argb(a, 247, 242, 255)
        val title = n.title.ifEmpty { n.text }
        c.drawText(fit(textPrimary, title, max), x, cy + dp(5f), textPrimary)
        if (n.title.isNotEmpty() && n.text.isNotEmpty()) {
            textSecondary.textSize = sp(12f)
            textSecondary.color = Color.argb(a, 166, 155, 184)
            c.drawText(fit(textSecondary, n.text, max), x, cy + dp(23f), textSecondary)
        }
    }

    private fun drawMediaPage(c: Canvas, left: Float, cy: Float, w: Float, a: Int, m: MediaInfo, percent: String) {
        val pad = dp(18f)
        val x = left + pad
        textSecondary.textSize = sp(11f)
        textSecondary.textAlign = Paint.Align.RIGHT
        textSecondary.color = Color.argb(a, 166, 155, 184)
        c.drawText(percent, left + w - pad, cy - dp(16f), textSecondary)
        val max = w - pad * 2 - dp(40f)
        textPrimary.textAlign = Paint.Align.LEFT
        textPrimary.textSize = sp(14f)
        textPrimary.color = Color.argb(a, 247, 242, 255)
        c.drawText(fit(textPrimary, m.title, max), x, cy - dp(16f), textPrimary)
        textSecondary.textAlign = Paint.Align.LEFT
        textSecondary.textSize = sp(12f)
        if (m.artist.isNotEmpty()) c.drawText(fit(textSecondary, m.artist, w - pad * 2), x, cy, textSecondary)

        val by = cy + dp(26f)
        val gap = dp(56f)
        val bx = left + w / 2f
        val touch = dp(24f)
        prevHit.set(bx - gap - touch, by - touch, bx - gap + touch, by + touch)
        playHit.set(bx - touch, by - touch, bx + touch, by + touch)
        nextHit.set(bx + gap - touch, by - touch, bx + gap + touch, by + touch)
        dot.color = Color.argb(a, 213, 184, 255)
        val s = dp(7f)
        // previous: bar + left triangle
        c.drawRect(bx - gap - s, by - s, bx - gap - s + dp(2f), by + s, dot)
        shape.reset(); shape.moveTo(bx - gap + s, by - s); shape.lineTo(bx - gap - s + dp(3f), by); shape.lineTo(bx - gap + s, by + s); shape.close()
        c.drawPath(shape, dot)
        // next: right triangle + bar
        shape.reset(); shape.moveTo(bx + gap - s, by - s); shape.lineTo(bx + gap + s - dp(3f), by); shape.lineTo(bx + gap - s, by + s); shape.close()
        c.drawPath(shape, dot)
        c.drawRect(bx + gap + s - dp(2f), by - s, bx + gap + s, by + s, dot)
        // play / pause in a soft violet disc
        dot.color = Color.argb((a * 0.25f).toInt(), 169, 112, 255)
        c.drawCircle(bx, by, dp(16f), dot)
        dot.color = Color.argb(a, 247, 242, 255)
        if (m.playing) {
            c.drawRect(bx - dp(5f), by - dp(6f), bx - dp(1.5f), by + dp(6f), dot)
            c.drawRect(bx + dp(1.5f), by - dp(6f), bx + dp(5f), by + dp(6f), dot)
        } else {
            shape.reset(); shape.moveTo(bx - dp(4f), by - dp(7f)); shape.lineTo(bx + dp(6f), by); shape.lineTo(bx - dp(4f), by + dp(7f)); shape.close()
            c.drawPath(shape, dot)
        }
    }

    private fun drawBatteryPage(c: Canvas, left: Float, cy: Float, w: Float, a: Int, percent: String) {
        val pad = dp(16f)
        val ringR = dp(19f)
        val rx = left + pad + ringR; 
        ringTrack.strokeWidth = dp(4f); ringArc.strokeWidth = dp(4f)
        ringTrack.color = Color.argb((a * 0.22f).toInt(), 169, 112, 255)
        val done = battery.charge == ChargeState.FULL
        ringArc.color = if (done) Color.argb(a, 139, 229, 176) else Color.argb(a, 169, 112, 255)
        val oval = RectF(rx - ringR, cy - ringR, rx + ringR, cy + ringR)
        c.drawArc(oval, 0f, 360f, false, ringTrack)
        val sweep = if (battery.percent >= 0) 360f * battery.percent / 100f else 0f
        c.drawArc(oval, -90f, sweep, false, ringArc)
        if (battery.charge == ChargeState.CHARGING) drawBolt(c, rx, cy, dp(7f), a)

        val tx = rx + ringR + dp(14f)
        val max = left + w - pad - tx
        textPrimary.textSize = sp(22f)
        textPrimary.color = Color.argb(a, 247, 242, 255)
        textPrimary.textAlign = Paint.Align.LEFT
        val detail = detailLabel()
        val lines = if (detail.isEmpty()) 2 else 3
        val blockH = dp(24f) + dp(16f) + if (lines == 3) dp(15f) else 0f
        var y = cy - blockH / 2f + dp(22f)
        c.drawText(percent, tx, y, textPrimary)
        textSecondary.textSize = sp(12f)
        textSecondary.color = Color.argb(a, if (done) 139 else 213, if (done) 229 else 184, if (done) 176 else 255)
        textSecondary.textAlign = Paint.Align.LEFT
        y += dp(17f)
        c.drawText(fit(textSecondary, statusLabel(), max), tx, y, textSecondary)
        if (detail.isNotEmpty()) {
            textSecondary.textSize = sp(11f)
            textSecondary.color = Color.argb(a, 166, 155, 184)
            y += dp(15f)
            c.drawText(fit(textSecondary, detail, max), tx, y, textSecondary)
        }
    }

    private fun drawBolt(c: Canvas, x: Float, y: Float, s: Float, a: Int) {
        boltPath.reset()
        boltPath.moveTo(x + s * 0.15f, y - s)
        boltPath.lineTo(x - s * 0.65f, y + s * 0.15f)
        boltPath.lineTo(x - s * 0.05f, y + s * 0.15f)
        boltPath.lineTo(x - s * 0.15f, y + s)
        boltPath.lineTo(x + s * 0.65f, y - s * 0.15f)
        boltPath.lineTo(x + s * 0.05f, y - s * 0.15f)
        boltPath.close()
        dot.color = Color.argb(a, 213, 184, 255)
        c.drawPath(boltPath, dot)
    }

    private companion object { const val NOTICE_MS = 5000L }
}
