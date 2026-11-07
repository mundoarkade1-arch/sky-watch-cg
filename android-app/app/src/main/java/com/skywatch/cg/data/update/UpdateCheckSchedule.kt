package com.skywatch.cg.data.update

object UpdateCheckSchedule {
    const val INTERVAL_MILLIS = 6 * 60 * 60 * 1000L

    fun shouldCheck(nowMillis: Long, lastCheckMillis: Long): Boolean =
        lastCheckMillis <= 0L || nowMillis - lastCheckMillis >= INTERVAL_MILLIS
}
