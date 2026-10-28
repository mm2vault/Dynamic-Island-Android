package com.mm2vault.dynamicisland

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.TextView

class IslandService : Service() {
    private lateinit var windowManager: WindowManager
    private var island: LinearLayout? = null
    private var details: TextView? = null
    private var expanded = false
    private var batteryText = "Pil durumu bekleniyor"
    private var batteryPercent = -1
    private var isCharging = false
    private var layoutParams: WindowManager.LayoutParams? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            batteryPercent = if (level >= 0 && scale > 0) level * 100 / scale else -1
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            batteryText = if (batteryPercent >= 0) (if (isCharging) "Şarj oluyor" else "Pil durumu") else "Pil bilgisi yok"
            refreshText()
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createChannel()
        val notification = Notification.Builder(this, CHANNEL)
            .setContentTitle("Dynamic Island etkin")
            .setContentText("Kayan kapsül çalışıyor")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
        startForeground(42, notification)

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(batteryReceiver, filter)
        }
        showIsland()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "Island durumu", NotificationManager.IMPORTANCE_LOW))
        }
    }

    private fun showIsland() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(10), dp(15), dp(10))
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(14, 12, 20), Color.rgb(8, 7, 12))
            ).apply {
                cornerRadius = dp(30).toFloat()
                setStroke(dp(1), Color.rgb(75, 54, 99))
            }
            elevation = dp(18).toFloat()
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val glow = TextView(this).apply {
            text = "●"
            textSize = 14f
            setTextColor(0xFFBE91FF.toInt())
            setPadding(0, 0, dp(9), 0)
        }
        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val title = TextView(this).apply {
            text = "DYNAMIC ISLAND"
            setTextColor(Color.WHITE)
            textSize = 10f
            letterSpacing = 0.08f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        val subtitle = TextView(this).apply {
            text = "PURPLE GLASS"
            setTextColor(0xFFB68CFF.toInt())
            textSize = 8f
            letterSpacing = 0.12f
            setPadding(0, dp(2), 0, 0)
        }
        val divider = TextView(this).apply {
            text = "│"
            setTextColor(0xFF50415F.toInt())
            textSize = 17f
            setPadding(dp(10), 0, dp(9), 0)
        }
        val battery = TextView(this).apply {
            text = "—%"
            setTextColor(0xFFD7C0FF.toInt())
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }
        labels.addView(title)
        labels.addView(subtitle)
        header.addView(glow)
        header.addView(labels, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(divider)
        header.addView(battery)
        root.addView(header)

        details = TextView(this).apply {
            text = batteryText
            setTextColor(0xFFD1CBDC.toInt())
            textSize = 11f
            setPadding(dp(23), dp(8), 0, dp(1))
            visibility = android.view.View.GONE
        }
        root.addView(details)

        root.setOnClickListener {
            expanded = !expanded
            details?.visibility = if (expanded) android.view.View.VISIBLE else android.view.View.GONE
            val params = layoutParams
            if (params != null) {
                params.width = dp(if (expanded) 270 else 210)
                try { windowManager.updateViewLayout(root, params) } catch (_: Exception) { }
            }
            root.animate()
                .scaleX(if (expanded) 1.03f else 1f)
                .scaleY(if (expanded) 1.03f else 1f)
                .setDuration(220)
                .setInterpolator(OvershootInterpolator(0.8f))
                .start()
            refreshText()
        }

        layoutParams = WindowManager.LayoutParams(
            dp(210),
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(10)
        }

        island = root
        try {
            windowManager.addView(root, layoutParams)
        } catch (_: Exception) {
            stopSelf()
            return
        }
        root.alpha = 0f
        root.translationY = -dp(12).toFloat()
        root.animate().alpha(1f).translationY(0f).setDuration(320).start()
        refreshText()
    }

    private fun refreshText() {
        details?.text = if (expanded) {
            val percent = if (batteryPercent >= 0) "$batteryPercent%" else "Bilinmiyor"
            (if (isCharging) "Şarj oluyor" else "Pil durumu") + "  ·  $percent"
        } else {
            batteryText
        }
        val root = island ?: return
        val header = root.getChildAt(0) as? LinearLayout ?: return
        val batteryView = header.getChildAt(3) as? TextView ?: return
        batteryView.text = if (batteryPercent >= 0) "$batteryPercent%" else "—%"
        batteryView.setTextColor(if (isCharging) 0xFFBDA0FF.toInt() else Color.WHITE)
        val params = layoutParams ?: return
        params.width = dp(if (expanded) 270 else 210)
        try { windowManager.updateViewLayout(root, params) } catch (_: Exception) { }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        try { unregisterReceiver(batteryReceiver) } catch (_: Exception) { }
        island?.let { try { windowManager.removeView(it) } catch (_: Exception) { } }
        island = null
        layoutParams = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object { const val CHANNEL = "dynamic_island_status" }
}
