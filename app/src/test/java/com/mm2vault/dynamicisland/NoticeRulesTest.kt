package com.mm2vault.dynamicisland

import android.app.Notification
import com.mm2vault.dynamicisland.live.NoticeRules
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoticeRulesTest {
    private fun ok(flags: Int = 0, cat: String? = null, vis: Int = Notification.VISIBILITY_PRIVATE, own: Boolean = false, text: Boolean = true) =
        NoticeRules.isPreviewable(flags, cat, vis, own, text)

    @Test fun normalNotificationIsShown() = assertTrue(ok(cat = Notification.CATEGORY_MESSAGE))
    @Test fun ownAndEmptyAreHidden() { assertFalse(ok(own = true)); assertFalse(ok(text = false)) }
    @Test fun ongoingAndServiceHidden() {
        assertFalse(ok(flags = Notification.FLAG_ONGOING_EVENT))
        assertFalse(ok(flags = Notification.FLAG_FOREGROUND_SERVICE))
        assertFalse(ok(flags = Notification.FLAG_GROUP_SUMMARY))
    }
    @Test fun transportAndProgressHidden() {
        assertFalse(ok(cat = Notification.CATEGORY_TRANSPORT))
        assertFalse(ok(cat = Notification.CATEGORY_PROGRESS))
    }
    @Test fun secretIsNeverShown() = assertFalse(ok(vis = Notification.VISIBILITY_SECRET))
}
