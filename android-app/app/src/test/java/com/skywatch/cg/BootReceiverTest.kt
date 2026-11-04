package com.skywatch.cg

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class BootReceiverTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().build()
        )
    }

    @Test
    fun bootCompletedSchedulesSyncAndAlertWork() {
        BootReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        val workManager = WorkManager.getInstance(context)
        val syncWork = workManager
            .getWorkInfosForUniqueWork("siros_periodic_sync")
            .get(5, TimeUnit.SECONDS)
        val alertWork = workManager
            .getWorkInfosForUniqueWork("flight_alert_check")
            .get(5, TimeUnit.SECONDS)

        assertEquals(1, syncWork.size)
        assertEquals(1, alertWork.size)
        assertEquals(WorkInfo.State.ENQUEUED, syncWork.single().state)
        assertEquals(WorkInfo.State.ENQUEUED, alertWork.single().state)
        assertTrue(syncWork.single().tags.any { it.contains("SirosSyncWorker") })
        assertTrue(alertWork.single().tags.any { it.contains("FlightAlertWorker") })
    }
}
