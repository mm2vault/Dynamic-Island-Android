package com.mm2vault.dynamicisland

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.mm2vault.dynamicisland.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

/** Starts the island after reboot only if the user enabled it and the overlay permission is still granted. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val enabled = SettingsRepository(app).flow.first().autostart
                if (enabled && Settings.canDrawOverlays(app)) IslandService.start(app)
            } catch (_: Exception) {
            } finally {
                pending.finish()
            }
        }
    }
}
