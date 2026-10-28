package com.mm2vault.dynamicisland

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class IslandNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return
        val safeTitle = title.take(60)
        val safeText = text.take(100)
        sendBroadcast(android.content.Intent(ACTION_NOTIFICATION).setPackage(packageName)
            .putExtra(EXTRA_TEXT, listOf(safeTitle, safeText).filter { it.isNotBlank() }.joinToString(" · ")))
    }

    companion object {
        const val ACTION_NOTIFICATION = "com.mm2vault.dynamicisland.NOTIFICATION"
        const val EXTRA_TEXT = "notification_text"
    }
}
