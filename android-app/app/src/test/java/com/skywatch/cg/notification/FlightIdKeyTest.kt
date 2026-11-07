package com.skywatch.cg.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.skywatch.cg.data.siros.OperationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FlightIdKeyTest {
    private val arrivalId = "SIROS-1-E1-${OperationType.ARRIVAL}-2026-11-03"
    private val departureId = "SIROS-1-E2-${OperationType.DEPARTURE}-2026-11-03"

    @Test
    fun flightIdSurvivesNavigationUriEncoding() {
        assertEquals(arrivalId, Uri.decode(Uri.encode(arrivalId)))
        assertEquals(departureId, Uri.decode(Uri.encode(departureId)))
    }

    @Test
    fun distinctFlightIdsProduceDistinctAlertAndPendingIntentKeys() {
        assertNotEquals(arrivalId, departureId)
        assertNotEquals(arrivalId.hashCode(), departureId.hashCode())
        assertNotEquals(
            FlightAlertScheduleLogic.plan(arrivalId, 1_000L, 15).alertId,
            FlightAlertScheduleLogic.plan(departureId, 1_000L, 15).alertId
        )

        val context = ApplicationProvider.getApplicationContext<Context>()
        val arrivalIntent = Intent(context, FlightAlertReceiver::class.java)
            .putExtra(FlightAlertReceiver.EXTRA_FLIGHT_ID, arrivalId)
        val departureIntent = Intent(context, FlightAlertReceiver::class.java)
            .putExtra(FlightAlertReceiver.EXTRA_FLIGHT_ID, departureId)
        val arrivalPendingIntent = PendingIntent.getBroadcast(
            context, arrivalId.hashCode(), arrivalIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val departurePendingIntent = PendingIntent.getBroadcast(
            context, departureId.hashCode(), departureIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        assertNotEquals(arrivalPendingIntent, departurePendingIntent)
    }
}
