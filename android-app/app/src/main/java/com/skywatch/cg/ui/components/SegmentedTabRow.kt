package com.skywatch.cg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skywatch.cg.ui.theme.SkyWatchColors

@Composable
fun SegmentedTabRow(
    selectedTab: Int, // 0 = Arrivals, 1 = Departures
    onTabSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(48.dp),
        color = SkyWatchColors.Surface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SkyWatchColors.Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedEqual
        ) {
            val isArrivals = selectedTab == 0
            val arrivalsBg = if (isArrivals) SkyWatchColors.SurfaceContainer else Color.Transparent
            val arrivalsText = if (isArrivals) SkyWatchColors.ArrivalAccent else SkyWatchColors.OnSurfaceVariant

            val isDepartures = selectedTab == 1
            val departuresBg = if (isDepartures) SkyWatchColors.SurfaceContainer else Color.Transparent
            val departuresText = if (isDepartures) SkyWatchColors.DepartureAccent else SkyWatchColors.OnSurfaceVariant

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(arrivalsBg)
                    .clickable { onTabSelected(0) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chegadas",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = arrivalsText
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(departuresBg)
                    .clickable { onTabSelected(1) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Partidas",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = departuresText
                )
            }
        }
    }
}

// Extension helper for equal weight arrangement in Row
private val Arrangement.spacedEqual: Arrangement
    get() = Arrangement.spacedBy(4.dp)