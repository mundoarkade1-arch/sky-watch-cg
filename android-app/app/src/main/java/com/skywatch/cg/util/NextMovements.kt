package com.skywatch.cg.util

import com.skywatch.cg.data.siros.NormalizedFlight
import com.skywatch.cg.data.siros.OperationType
import java.time.LocalDateTime

data class NextMovements(
    val nextArrival: NormalizedFlight?,
    val nextDeparture: NormalizedFlight?
)

fun nextMovements(voos: List<NormalizedFlight>, agoraLocal: LocalDateTime): NextMovements {
    val eligible = voos.map { it to it.getAirportEventDateTime() }
        .filter { (_, event) -> !event.isBefore(agoraLocal) }
    return NextMovements(
        nextArrival = eligible.filter { it.first.operationType == OperationType.ARRIVAL }
            .minByOrNull { it.second }?.first,
        nextDeparture = eligible.filter { it.first.operationType == OperationType.DEPARTURE }
            .minByOrNull { it.second }?.first
    )
}
