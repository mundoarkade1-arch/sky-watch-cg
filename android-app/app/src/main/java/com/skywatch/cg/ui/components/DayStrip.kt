package com.skywatch.cg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.skywatch.cg.ui.theme.SkyWatchColors
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DayStrip(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    val dates = (0..14).map { yesterday.plusDays(it.toLong()) }
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        dates.forEach { date ->
            val isSelected = date == selectedDate
            val dayOfWeekStr = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("pt", "BR"))
                .uppercase(Locale("pt", "BR"))
                .take(3)
            val dayNum = date.dayOfMonth.toString()

            val backgroundColor = if (isSelected) SkyWatchColors.ArrivalAccent else SkyWatchColors.Surface
            val textColor = if (isSelected) SkyWatchColors.ArrivalAccentContainer else SkyWatchColors.OnBackground
            val subColor = if (isSelected) SkyWatchColors.ArrivalAccentContainer.copy(alpha = 0.8f) else SkyWatchColors.OnSurfaceVariant

            Surface(
                modifier = Modifier
                    .width(60.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onDateSelected(date) },
                color = backgroundColor,
                shape = RoundedCornerShape(16.dp),
                border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, SkyWatchColors.Border) else null
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = dayOfWeekStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = subColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dayNum,
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}