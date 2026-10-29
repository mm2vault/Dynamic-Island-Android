package com.mm2vault.dynamicisland.live

import android.app.Notification
import android.content.ComponentName
import android.content.pm.ApplicationInfo
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.mm2vault.dynamicisland.data.IslandSettings
import com.mm2vault.dynamicisland.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Only does anything after the user has both granted notification access in Android settings and
 * switched the matching feature on inside the app. Everything it reads stays in memory.
 */
class IslandNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var settingsJob: Job? = null
    private var settings = IslandSettings()
    private var sessions: MediaSessionManager? = null
    private var component: ComponentName? = null
    private var activeController: MediaController? = null
    private var sessionsListenerSet = false

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        if (settings.mediaEnabled) pickController(list.orEmpty())
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publishMedia()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publishMedia()
        override fun onSessionDestroyed() { detachController(); publishMedia() }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        component = ComponentName(this, IslandNotificationListener::class.java)
        sessions = getSystemService(MediaSessionManager::class.java)
        settingsJob?.cancel()
        settingsJob = scope.launch {
            SettingsRepository(applicationContext).flow.collect { s ->
                settings = s
                applyMediaSetting()
                if (!s.notificationsEnabled) LiveContent.clearNotice()
            }
        }
    }

    override fun onListenerDisconnected() {
        settingsJob?.cancel()
        stopMedia()
        LiveContent.clearAll()
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || !settings.notificationsEnabled) return
        try {
            val n = sbn.notification
            val extras = n.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty().trim()
            val text = (extras.getCharSequence(Notification.EXTRA_TEXT) ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
                ?.toString().orEmpty().trim()
            val allowed = NoticeRules.isPreviewable(
                flags = n.flags, category = n.category, visibility = n.visibility,
                isOwnPackage = sbn.packageName == packageName, hasText = title.isNotEmpty() || text.isNotEmpty()
            )
            if (!allowed) return
            LiveContent.postNotice(NoticeInfo(sbn.key, appLabel(sbn, extras), title, text, sbn.postTime))
        } catch (_: Exception) { }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn?.key?.let { LiveContent.clearNotice(it) }
    }

    @Suppress("DEPRECATION")
    private fun appLabel(sbn: StatusBarNotification, extras: android.os.Bundle): String {
        val info = try {
            if (Build.VERSION.SDK_INT >= 33) extras.getParcelable("android.appInfo", ApplicationInfo::class.java)
            else extras.getParcelable<ApplicationInfo>("android.appInfo")
        } catch (_: Exception) { null }
        val label = try { info?.loadLabel(packageManager)?.toString() } catch (_: Exception) { null }
        return label?.takeIf { it.isNotBlank() } ?: sbn.packageName.substringAfterLast('.')
    }

    private fun applyMediaSetting() {
        if (settings.mediaEnabled) startMedia() else { stopMedia(); LiveContent.setMedia(null, null) }
    }

    private fun startMedia() {
        val sm = sessions ?: return
        val comp = component ?: return
        try {
            if (!sessionsListenerSet) {
                sm.addOnActiveSessionsChangedListener(sessionsListener, comp)
                sessionsListenerSet = true
            }
            pickController(sm.getActiveSessions(comp))
        } catch (_: SecurityException) { }
    }

    private fun stopMedia() {
        detachController()
        if (sessionsListenerSet) {
            try { sessions?.removeOnActiveSessionsChangedListener(sessionsListener) } catch (_: Exception) { }
            sessionsListenerSet = false
        }
    }

    private fun pickController(list: List<MediaController>) {
        val best = list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list.firstOrNull()
        if (best?.sessionToken == activeController?.sessionToken) { publishMedia(); return }
        detachController()
        activeController = best
        try { best?.registerCallback(controllerCallback) } catch (_: Exception) { }
        publishMedia()
    }

    private fun detachController() {
        try { activeController?.unregisterCallback(controllerCallback) } catch (_: Exception) { }
        activeController = null
    }

    private fun publishMedia() {
        val c = activeController
        val meta = c?.metadata
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        if (c == null || title.isBlank()) { LiveContent.setMedia(null, null); return }
        val artist = (meta?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)).orEmpty()
        val playing = c.playbackState?.state == PlaybackState.STATE_PLAYING
        LiveContent.setMedia(MediaInfo(title, artist, playing), c)
    }

    override fun onDestroy() {
        stopMedia()
        LiveContent.clearAll()
        scope.cancel()
        super.onDestroy()
    }
}
