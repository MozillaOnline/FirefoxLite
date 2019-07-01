package org.mozilla.focus.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mozilla.focus.utils.FirebaseHelper.LIFE_FEED_PROVIDERS
import org.mozilla.focus.utils.FirebaseHelper.RATE_APP_DIALOG_THRESHOLD
import org.mozilla.focus.utils.FirebaseHelper.RATE_APP_NOTIFICATION_THRESHOLD
import org.mozilla.focus.utils.FirebaseHelper.SHARE_APP_DIALOG_THRESHOLD

/**
 * Make sure default value will be used.
 */
class AppConfigWrapperTest {

    @Test
    fun `customize default value`() {

        val rateDialog = 3
        val rateNotification = 4
        val shareDialog = 5

        val map = HashMap<String, Any>().apply {
            this[RATE_APP_DIALOG_THRESHOLD] = rateDialog
            this[RATE_APP_NOTIFICATION_THRESHOLD] = rateNotification
            this[SHARE_APP_DIALOG_THRESHOLD] = shareDialog
        }

        FirebaseHelper.replaceContract(FirebaseNoOpImp(map))

        assertEquals(rateDialog, AppConfigWrapper.getRateDialogLaunchTimeThreshold().toInt())

        assertEquals(
                rateNotification,
                AppConfigWrapper.getRateAppNotificationLaunchTimeThreshold().toInt()
        )

        assertEquals(
                shareDialog,
                AppConfigWrapper.getShareDialogLaunchTimeThreshold(false).toInt()
        )

        assertEquals(
                shareDialog + rateNotification - rateDialog,
                AppConfigWrapper.getShareDialogLaunchTimeThreshold(true).toInt()
        )
    }

    @Test
    fun `get migrated news categorize url`() {
        val lifeFeedProviders = """
            |[
            | {"name":"Newspoint","type":"news","url":"LEGACY_URL"},
            | {"name":"NewspointCategory","type":"news","url":"CATEGORY_URL"}
            |]
            """.trimMargin()
        val map = HashMap<String, Any>().apply {
            this[LIFE_FEED_PROVIDERS] = lifeFeedProviders
        }
        FirebaseHelper.replaceContract(FirebaseNoOpImp(map))

        assertEquals("CATEGORY_URL", AppConfigWrapper.getNewsProviderUrl("Newspoint"))
    }
}
