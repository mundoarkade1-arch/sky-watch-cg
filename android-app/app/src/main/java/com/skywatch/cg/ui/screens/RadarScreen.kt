package com.skywatch.cg.ui.screens

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.skywatch.cg.R
import com.skywatch.cg.data.radar.RadarAircraft
import com.skywatch.cg.data.radar.RadarRemoteSource
import com.skywatch.cg.data.radar.RadarSnapshot
import com.skywatch.cg.data.prefs.AppPreferences
import com.skywatch.cg.ui.theme.SkyWatchColors
import com.skywatch.cg.ui.viewmodel.RadarViewModel
import com.skywatch.cg.ui.viewmodel.RadarUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun RadarScreen(
    viewModel: RadarViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val preferences = remember { AppPreferences.getInstance(context) }
    var keepScreenOn by remember { mutableStateOf(preferences.keepScreenOn) }
    DisposableEffect(keepScreenOn) {
        val window = (context as? Activity)?.window
        if (keepScreenOn) window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    LaunchedEffect(viewModel) {
        viewModel.refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.radar_experimental),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.radar_range_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    androidx.compose.material3.Switch(
                        checked = keepScreenOn,
                        onCheckedChange = {
                            keepScreenOn = it
                            preferences.keepScreenOn = it
                        }
                    )
                    IconButton(onClick = viewModel::refresh, enabled = !uiState.isLoading) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(12.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.radar_refresh)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = { BottomNavBar(navController) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        RadarContent(
            uiState = uiState,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

@Composable
private fun RadarContent(
    uiState: RadarUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.radar_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        uiState.errorMessage?.let { message ->
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val retrySeconds = uiState.retryAfterSeconds
                        Text(
                            text = if (retrySeconds != null) {
                                stringResource(R.string.radar_sources_unavailable, retrySeconds)
                            } else message,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        if (uiState.snapshot == null) {
                            Button(onClick = onRefresh) {
                                Text(stringResource(R.string.radar_retry))
                            }
                        }
                    }
                }
            }
        }

        val snapshot = uiState.snapshot
        if (snapshot == null && uiState.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (snapshot != null) {
            item { RadarScope(snapshot) }
            item { SnapshotTime(snapshot) }
            if (snapshot.aircraft.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.radar_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                item {
                    Text(
                        text = stringResource(R.string.radar_aircraft_count, snapshot.aircraft.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(snapshot.aircraft, key = { it.icaoHex }) { aircraft ->
                    RadarAircraftCard(aircraft)
                }
            }
        } else if (!uiState.isLoading && uiState.errorMessage == null) {
            item {
                Button(onClick = onRefresh) {
                    Text(stringResource(R.string.radar_load))
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.radar_attribution),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RadarScope(snapshot: RadarSnapshot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension * 0.44f
                val scopeColor = SkyWatchColors.ArrivalAccent.copy(alpha = 0.6f)
                for (ring in 1..4) {
                    drawCircle(
                        color = scopeColor.copy(alpha = 0.25f + ring * 0.08f),
                        radius = radius * ring / 4f,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                drawLine(
                    color = scopeColor.copy(alpha = 0.35f),
                    start = Offset(center.x - radius, center.y),
                    end = Offset(center.x + radius, center.y),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = scopeColor.copy(alpha = 0.35f),
                    start = Offset(center.x, center.y - radius),
                    end = Offset(center.x, center.y + radius),
                    strokeWidth = 1.dp.toPx()
                )
                drawCircle(
                    color = SkyWatchColors.DepartureAccent,
                    radius = 5.dp.toPx(),
                    center = center
                )

                snapshot.aircraft.forEach { aircraft ->
                    val normalizedDistance =
                        (aircraft.distanceNauticalMiles / RadarRemoteSource.RANGE_NAUTICAL_MILES)
                            .coerceIn(0.0, 1.0)
                    val bearingRadians = Math.toRadians(aircraft.bearingDegrees)
                    val point = Offset(
                        x = center.x + (sin(bearingRadians) * radius * normalizedDistance).toFloat(),
                        y = center.y - (cos(bearingRadians) * radius * normalizedDistance).toFloat()
                    )
                    drawCircle(
                        color = SkyWatchColors.ArrivalAccent,
                        radius = 4.dp.toPx(),
                        center = point
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("N", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text(
                text = stringResource(R.string.radar_center_marker),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Text("50 NM", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SnapshotTime(snapshot: RadarSnapshot) {
    val observedAt = Instant.ofEpochMilli(snapshot.observedAtMillis)
        .atZone(ZoneId.of("America/Campo_Grande"))
        .format(DateTimeFormatter.ofPattern("HH:mm:ss", Locale.forLanguageTag("pt-BR")))
    Text(
        text = stringResource(
            R.string.radar_source_status,
            snapshot.sourceName,
            snapshot.aircraft.size,
            observedAt
        ),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun RadarAircraftCard(aircraft: RadarAircraft) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = aircraft.callsign?.let {
                    stringResource(R.string.radar_callsign, it)
                } ?: stringResource(R.string.radar_no_callsign),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.radar_hex, aircraft.icaoHex),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val altitude = aircraft.altitudeFeet?.let {
                stringResource(R.string.radar_altitude, it)
            } ?: stringResource(R.string.radar_altitude_unknown)
            val speed = aircraft.groundSpeedKnots?.let {
                stringResource(R.string.radar_speed, it.toInt())
            } ?: stringResource(R.string.radar_speed_unknown)
            Text(
                text = stringResource(
                    R.string.radar_aircraft_metrics,
                    altitude,
                    speed,
                    aircraft.distanceNauticalMiles.toInt(),
                    aircraft.bearingDegrees.toInt()
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            aircraft.secondsSincePosition?.let { age ->
                Text(
                    text = stringResource(R.string.radar_position_age, age.toInt()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
