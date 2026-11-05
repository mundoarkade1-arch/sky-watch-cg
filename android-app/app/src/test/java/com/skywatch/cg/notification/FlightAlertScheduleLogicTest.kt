package com.skywatch.cg.notification

import org.junit.Assert.assertEquals
import org.junit.Test

class FlightAlertScheduleLogicTest {
    @Test fun schedulesAtEventMinusSelectedMinutes() {
        val plan = FlightAlertScheduleLogic.plan("siros-2026-11-05", 1_000_000L, 15)
        assertEquals(100_000L, plan.triggerAtMillis)
        assertEquals("siros-2026-11-05:15", plan.alertId)
    }

    @Test fun neverUsesNegativeMinutes() {
        val plan = FlightAlertScheduleLogic.plan("flight", 1_000L, -5)
        assertEquals(1_000L, plan.triggerAtMillis)
    }
}
