package com.skywatch.cg.data.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckScheduleTest {
    @Test
    fun checksImmediatelyWhenThereIsNoPreviousCheck() {
        assertTrue(UpdateCheckSchedule.shouldCheck(nowMillis = 1_000L, lastCheckMillis = 0L))
    }

    @Test
    fun skipsChecksInsideIntervalAndAllowsThemAfterward() {
        assertFalse(UpdateCheckSchedule.shouldCheck(1_000L, 1_000L))
        assertTrue(UpdateCheckSchedule.shouldCheck(1_000L + UpdateCheckSchedule.INTERVAL_MILLIS, 1_000L))
    }
}
