package com.skywatch.cg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skywatch.cg.data.siros.NormalizedFlight
import com.skywatch.cg.data.siros.OperationType
import com.skywatch.cg.ui.theme.SkyWatchColors
import java.time.Duration
import java.time.LocalTime

@Composable
fun NextFlightCard(
    flight: NormalizedFlight,
    currentTime: LocalTime = LocalTime.now(),
    onClick: () -> Unit
) {
    val isArrival = flight.operationType == OperationType.ARRIVAL
    val flightTime = if (isArrival) flight.scheduledArrivalUtc else flight.scheduledDepartureUtc
    val timeDiff = Duration.between(currentTime, flightTime).toMinutes()
    val timeDesc = if (timeDiff > 0) {
        if (timeDiff < 60) "$timeDiff min / para ${if (isArrival) "chegar" else "partir"}"
        else "${timeDiff / 60}h ${timeDiff % 60}m / para ${if (isArrival) "chegar" else "partir"}"
    } else {
        "Em andamento / agora"
    }

    val city = if (isArrival) (flight.originName ?: flight.origin) else (flight.destinationName ?: flight.destination)
    val cityShort = city.split(" ").firstOrNull() ?: city
    val code = if (isArrival) flight.origin else flight.destination

    val accentColor = if (isArrival) SkyWatchColors.ArrivalAccent else SkyWatchColors.DepartureAccent

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SkyWatchColors.NextFlightBorder),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SkyWatchColors.NextFlightGradientStart,
                            SkyWatchColors.NextFlightGradientEnd
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PRÓXIMO VOO · ${if (isArrival) "CHEGADA" else "PARTIDA"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = timeDesc,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SkyWatchColors.OnSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = flightTime.toString(),
                            style = MaterialTheme.typography.displayLarge,
                            color = SkyWatchColors.OnBackground,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${flight.airline} ${flight.flightNumber} · $cityShort ($code)",
                            style = MaterialTheme.typography.titleMedium,
                            color = SkyWatchColors.OnSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}