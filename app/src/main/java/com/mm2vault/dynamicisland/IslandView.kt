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
import android.view.View
import android.view.animation.PathInterpolator
import com.mm2vault.dynamicisland.data.BatteryState
import com.mm2vault.dynamicisland.data.ChargeState
import com.mm2vault.dynamicisland.data.IslandSettings

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
    private fun capsuleHeight() = dp(settings.heightDp.toFloat()) + (dp(76f) - dp(settings.heightDp.toFloat())) * progress

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            (capsuleWidth() + glowPad * 2).toInt(),
            (capsuleHeight() + glowPad * 2).toInt()
        )
    }

    fun isExpanded() = target >= 1f

    fun toggle() = setExpanded(!isExpanded())

    fun setExpanded(expanded: Boolean, animate: Boolean = true) {
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
            MotionEvent.ACTION_UP -> { press = 0f; invalidate(); performClick() }
            MotionEvent.ACTION_CANCEL -> { press = 0f; invalidate() }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
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

    override fun onDetachedFromWindow() {
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
        textPrimary.textSize = sp(22f)
        textPrimary.color = Color.argb(a, 247, 242, 255)
        textPrimary.textAlign = Paint.Align.LEFT
        c.drawText(percent, tx, cy - dp(2f), textPrimary)
        textSecondary.textSize = sp(12f)
        textSecondary.color = Color.argb(a, if (done) 139 else 166, if (done) 229 else 155, if (done) 176 else 184)
        textSecondary.textAlign = Paint.Align.LEFT
        val label = statusLabel()
        val max = left + w - pad - tx
        var shown = label
        while (shown.length > 3 && textSecondary.measureText(shown) > max) shown = shown.dropLast(2)
        if (shown != label) shown = shown.trimEnd() + "…"
        c.drawText(shown, tx, cy - dp(2f) + dp(18f), textSecondary)
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
}
