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
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.TextView

class IslandService : Service() {
    private lateinit var windowManager: WindowManager
    private var island: LinearLayout? = null
    private var details: TextView? = null
    private var expanded = false
    private var batteryText = "Battery status ready"
    private var notificationText = "Tap to expand"
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val percent = if (level >= 0 && scale > 0) level * 100 / scale else -1
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            batteryText = if (percent >= 0) (if (charging) "Charging · " else "Battery · ") + "$percent%" else "Battery status unavailable"
            refreshText()
        }
    }
    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            notificationText = intent?.getStringExtra(IslandNotificationListener.EXTRA_TEXT) ?: return
            refreshText()
            expandBriefly()
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createChannel()
        val notification = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL)
            .setContentTitle("Dynamic Island is active")
            .setContentText("Tap the notification to return to settings")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true).build()
        else Notification.Builder(this).setContentTitle("Dynamic Island is active").setSmallIcon(android.R.drawable.ic_dialog_info).build()
        startForeground(42, notification)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), Context.RECEIVER_NOT_EXPORTED)
            registerReceiver(notificationReceiver, IntentFilter(IslandNotificationListener.ACTION_NOTIFICATION), Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            @Suppress("DEPRECATION")
            registerReceiver(notificationReceiver, IntentFilter(IslandNotificationListener.ACTION_NOTIFICATION))
        }
        showIsland()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "Island status", NotificationManager.IMPORTANCE_LOW))
        }
    }

    private fun showIsland() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(12))
            setBackgroundColor(Color.BLACK)
            elevation = dp(14).toFloat()
        }
        val title = TextView(this).apply {
            text = "●  DYNAMIC ISLAND"
            setTextColor(0xFFB68CFF.toInt())
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }
        details = TextView(this).apply {
            text = batteryText
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(0, dp(6), 0, 0)
        }
        root.addView(title)
        root.addView(details)
        root.setOnClickListener {
            expanded = !expanded
            root.animate().scaleX(if (expanded) 1.04f else 1f).scaleY(if (expanded) 1.04f else 1f).setDuration(180).setInterpolator(OvershootInterpolator()).start()
            refreshText()
        }
        val params = WindowManager.LayoutParams(
            dp(if (expanded) 280 else 210), WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; y = dp(12) }
        island = root
        try { windowManager.addView(root, params) } catch (_: Exception) { stopSelf(); return }
        root.alpha = 0f
        root.animate().alpha(1f).setDuration(260).start()
    }

    private fun refreshText() {
        details?.text = if (expanded) "$batteryText\n$notificationText" else batteryText
        island?.let { view ->
            val params = view.layoutParams as? WindowManager.LayoutParams ?: return
            params.width = dp(if (expanded) 280 else 210)
            try { windowManager.updateViewLayout(view, params) } catch (_: Exception) {}
        }
    }

    private fun expandBriefly() {
        expanded = true
        refreshText()
        island?.postDelayed({ expanded = false; refreshText() }, 4500)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        try { unregisterReceiver(batteryReceiver) } catch (_: Exception) {}
        try { unregisterReceiver(notificationReceiver) } catch (_: Exception) {}
        island?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        island = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object { const val CHANNEL = "dynamic_island_status" }
}
