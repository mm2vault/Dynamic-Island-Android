package com.mm2vault.dynamicisland.live

import android.app.Notification
import android.media.session.MediaController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A notification preview. Kept in memory only, never written to disk or logged. */
data class NoticeInfo(val key: String, val app: String, val title: String, val text: String, val postedAt: Long)

data class MediaInfo(val title: String, val artist: String, val playing: Boolean)

/** Pure rules for which notifications may be previewed. Unit tested. */
object NoticeRules {
    fun isPreviewable(flags: Int, category: String?, visibility: Int, isOwnPackage: Boolean, hasText: Boolean): Boolean {
        if (isOwnPackage || !hasText) return false
        if (flags and Notification.FLAG_ONGOING_EVENT != 0) return false
        if (flags and Notification.FLAG_GROUP_SUMMARY != 0) return false
        if (flags and Notification.FLAG_FOREGROUND_SERVICE != 0) return false
        if (category == Notification.CATEGORY_TRANSPORT || category == Notification.CATEGORY_PROGRESS ||
            category == Notification.CATEGORY_SERVICE || category == Notification.CATEGORY_SYSTEM
        ) return false
        // Notifications the owner marked secret on the lock screen are never previewed.
        if (visibility == Notification.VISIBILITY_SECRET) return false
        return true
    }
}

/** In-memory bridge between the notification listener and the overlay. */
object LiveContent {
    private val _notice = MutableStateFlow<NoticeInfo?>(null)
    val notice: StateFlow<NoticeInfo?> = _notice.asStateFlow()

    private val _media = MutableStateFlow<MediaInfo?>(null)
    val media: StateFlow<MediaInfo?> = _media.asStateFlow()

    @Volatile private var controller: MediaController? = null

    fun postNotice(n: NoticeInfo) { _notice.value = n }
    fun clearNotice(key: String? = null) { if (key == null || _notice.value?.key == key) _notice.value = null }
    fun setMedia(info: MediaInfo?, c: MediaController?) { controller = c; _media.value = info }
    fun clearAll() { _notice.value = null; _media.value = null; controller = null }

    fun togglePlayPause() {
        val c = controller ?: return
        try {
            val playing = c.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING
            if (playing) c.transportControls.pause() else c.transportControls.play()
        } catch (_: Exception) { }
    }
    fun next() { try { controller?.transportControls?.skipToNext() } catch (_: Exception) { } }
    fun previous() { try { controller?.transportControls?.skipToPrevious() } catch (_: Exception) { } }
}
