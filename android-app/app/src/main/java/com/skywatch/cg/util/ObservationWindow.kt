package com.skywatch.cg.util

import com.skywatch.cg.data.siros.NormalizedFlight
import com.skywatch.cg.data.siros.OperationType
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

internal data class ObservationWindow(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val arrivals: Int,
    val departures: Int
) {
    val totalMovements: Int
        get() = arrivals + departures
}

internal object ObservationWindowCalculator {
    val DEFAULT_DURATION: Duration = Duration.ofHours(3)

    fun findBestWindow(
        flights: List<NormalizedFlight>,
        selectedDate: LocalDate,
        now: LocalDateTime,
        duration: Duration = DEFAULT_DURATION
    ): ObservationWindow? {
        require(!duration.isNegative && !duration.isZero) {
            "A duração do período de observação deve ser positiva."
        }

        val dayEnd = selectedDate.plusDays(1).atStartOfDay()
        val movements = flights.map { flight ->
            flight to flight.getAirportEventDateTime()
        }.filter { (_, eventDateTime) ->
            eventDateTime.toLocalDate() == selectedDate &&
                (selectedDate != now.toLocalDate() || !eventDateTime.isBefore(now))
        }.sortedBy { (_, eventDateTime) -> eventDateTime }

        var bestWindow: ObservationWindow? = null
        for ((_, start) in movements) {
            val end = start.plus(duration)
            if (end.isAfter(dayEnd)) continue

            val inWindow = movements.filter { (_, eventDateTime) ->
                !eventDateTime.isBefore(start) && eventDateTime.isBefore(end)
            }
            val candidate = ObservationWindow(
                start = start,
                end = end,
                arrivals = inWindow.count { (flight, _) ->
                    flight.operationType == OperationType.ARRIVAL
                },
                departures = inWindow.count { (flight, _) ->
                    flight.operationType == OperationType.DEPARTURE
                }
            )
            if (candidate.totalMovements > (bestWindow?.totalMovements ?: 0)) {
                bestWindow = candidate
            }
        }
        return bestWindow
    }
}
