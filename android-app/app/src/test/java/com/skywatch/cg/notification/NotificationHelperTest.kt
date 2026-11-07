package com.skywatch.cg.notification

import android.app.NotificationManager
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class NotificationHelperTest {
    @Test
    fun createsSeparateSoundAndSilentChannels() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        NotificationHelper.createNotificationChannel(context)

        val manager = context.getSystemService(NotificationManager::class.java)
        val sound = manager.getNotificationChannel(NotificationHelper.CHANNEL_ID)
        val silent = manager.getNotificationChannel(NotificationHelper.CHANNEL_ID_SILENT)

        assertNotNull(sound)
        assertNotNull(silent)
        assertTrue(sound!!.shouldVibrate())
        assertFalse(silent!!.shouldVibrate())
    }

    @Test
    fun notificationSettingsButtonTargetsThisApp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = NotificationHelper.notificationSettingsIntent(context)

        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, intent.action)
        assertEquals(context.packageName, intent.getStringExtra(Settings.EXTRA_APP_PACKAGE))
    }
}
