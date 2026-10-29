package com.mm2vault.dynamicisland

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import com.mm2vault.dynamicisland.data.BatteryState
import com.mm2vault.dynamicisland.data.IslandSettings
import com.mm2vault.dynamicisland.data.SettingsRepository
import com.mm2vault.dynamicisland.live.LiveContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IslandService : Service() {
    private lateinit var windowManager: WindowManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var island: IslandView? = null
    private var params: WindowManager.LayoutParams? = null
    private var settings = IslandSettings()
    private var battery = BatteryState()
    private var receiverRegistered = false
    private var stopping = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            battery = BatteryState.from(intent)
            island?.battery = battery
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopIsland()
            return START_NOT_STICKY
        }
        if (!enterForeground()) {
            fail()
            return START_NOT_STICKY
        }
        if (!Settings.canDrawOverlays(this)) {
            fail()
            return START_NOT_STICKY
        }
        if (island == null) start()
        return START_NOT_STICKY
    }

    private fun enterForeground(): Boolean = try {
        createChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        true
    } catch (e: Exception) {
        false
    }

    private fun fail() {
        _running.value = false
        stopForegroundCompat()
        stopSelf()
    }

    private fun start() {
        val view = IslandView(this).apply {
            battery = this@IslandService.battery
            onMediaAction = { action ->
                when (action) {
                    0 -> LiveContent.previous()
                    1 -> LiveContent.togglePlayPause()
                    else -> LiveContent.next()
                }
            }
        }
        island = view
        registerBattery()
        val ok = try {
            val p = buildParams()
            params = p
            windowManager.addView(view, p)
            true
        } catch (e: Exception) {
            false
        }
        if (!ok) {
            island = null
            unregisterBattery()
            fail()
            return
        }
        _running.value = true
        view.playEntrance()
        // Live content only reaches the capsule while the matching switch is on.
        scope.launch {
            LiveContent.notice.collect { n ->
                val fresh = n?.takeIf { System.currentTimeMillis() - it.postedAt < 8000L }
                island?.notice = if (settings.notificationsEnabled) fresh else null
            }
        }
        scope.launch {
            LiveContent.media.collect { m -> island?.media = if (settings.mediaEnabled) m else null }
        }
        scope.launch {
            SettingsRepository(applicationContext).flow.collect { s ->
                settings = s
                island?.let { v ->
                    v.settings = s
                    if (!s.notificationsEnabled) v.notice = null
                    if (!s.mediaEnabled) v.media = null
                    applyPosition()
                }
            }
        }
    }

    private fun buildParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        if (Build.VERSION.SDK_INT >= 28) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        y = topOffsetPx()
    }

    /** Vertical position: centred on a top display cutout when one exists, else the user's offset. */
    private fun topOffsetPx(): Int {
        val d = resources.displayMetrics.density
        val glowPad = 14 * d
        val userExtra = settings.offsetDp * d
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                val cutout = windowManager.currentWindowMetrics.windowInsets.displayCutout
                val top = cutout?.boundingRectTop
                if (top != null && !top.isEmpty) {
                    val capsule = settings.heightDp * d
                    val centred = top.centerY() - capsule / 2f - glowPad
                    val extra = (settings.offsetDp - IslandSettings.DEFAULT_OFFSET) * d
                    return (centred + extra).toInt().coerceAtLeast(0)
                }
            } catch (_: Exception) { }
        }
        return (userExtra - glowPad).toInt().coerceAtLeast(0)
    }

    private fun applyPosition() {
        val v = island ?: return
        val p = params ?: return
        p.y = topOffsetPx()
        try { windowManager.updateViewLayout(v, p) } catch (_: Exception) { }
    }

    private fun registerBattery() {
        if (receiverRegistered) return
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        // ACTION_BATTERY_CHANGED is sticky, so the return value is the current state.
        val sticky = if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(batteryReceiver, filter)
        }
        receiverRegistered = true
        battery = BatteryState.from(sticky)
        island?.battery = battery
    }

    private fun unregisterBattery() {
        if (!receiverRegistered) return
        try { unregisterReceiver(batteryReceiver) } catch (_: Exception) { }
        receiverRegistered = false
    }

    private fun stopIsland() {
        if (stopping) return
        stopping = true
        val v = island
        if (v == null) { finish(); return }
        v.playExit { finish() }
    }

    private fun finish() {
        removeOverlay()
        stopForegroundCompat()
        stopSelf()
    }

    private fun removeOverlay() {
        island?.let { try { windowManager.removeView(it) } catch (_: Exception) { } }
        island = null
        params = null
        unregisterBattery()
        _running.value = false
    }

    @Suppress("DEPRECATION")
    private fun stopForegroundCompat() {
        try {
            if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE) else stopForeground(true)
        } catch (_: Exception) { }
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.notif_channel), NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, IslandService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, CHANNEL)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setSmallIcon(R.drawable.ic_stat_island)
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, getString(R.string.notif_stop), stop).build())
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        removeOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL = "dynamic_island_status"
        const val ACTION_STOP = "com.mm2vault.dynamicisland.STOP"
        private const val NOTIFICATION_ID = 42
        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, IslandService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            // Plain startService with a stop action avoids creating a new foreground obligation.
            context.startService(Intent(context, IslandService::class.java).setAction(ACTION_STOP))
        }
    }
}
