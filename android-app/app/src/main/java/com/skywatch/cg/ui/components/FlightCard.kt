package com.skywatch.cg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skywatch.cg.data.siros.NormalizedFlight
import com.skywatch.cg.data.siros.OperationType
import com.skywatch.cg.ui.theme.SkyWatchColors

@Composable
fun FlightCard(
    flight: NormalizedFlight,
    isDimmed: Boolean = false,
    onClick: () -> Unit
) {
    val isArrival = flight.operationType == OperationType.ARRIVAL
    val flightTime = if (isArrival) flight.scheduledArrivalUtc else flight.scheduledDepartureUtc
    val city = if (isArrival) (flight.originName ?: flight.origin) else (flight.destinationName ?: flight.destination)
    val cityShort = city.split(" ").firstOrNull() ?: city
    val code = if (isArrival) flight.origin else flight.destination

    val alpha = if (isDimmed) 0.5f else 1f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = SkyWatchColors.Surface.copy(alpha = alpha),
        border = androidx.compose.foundation.BorderStroke(1.dp, SkyWatchColors.Border.copy(alpha = alpha))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = flightTime.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = SkyWatchColors.OnBackground.copy(alpha = alpha),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${flight.airline} ${flight.flightNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        color = SkyWatchColors.OnBackground.copy(alpha = alpha),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$cityShort ($code)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SkyWatchColors.OnSurfaceVariant.copy(alpha = alpha),
                        fontSize = 13.sp
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp)),
                color = SkyWatchColors.SurfaceContainer.copy(alpha = alpha),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Programado",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = SkyWatchColors.OnSurfaceVariant.copy(alpha = alpha)
                )
            }
        }
    }
}