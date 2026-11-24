@file:Suppress("NewApi")

package com.skywatch.cg.ui.screens

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.skywatch.cg.data.radar.hasRotationVectorSensor
import com.skywatch.cg.data.radar.smoothAzimuthDegrees
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

@Composable
fun RadarScreen(
    viewModel: RadarViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val preferences = remember { AppPreferences.getInstance(context) }
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    val rotationSensor = remember { sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    var rotateWithDevice by remember { mutableStateOf(rotationSensor != null) }
    var azimuthDegrees by remember { mutableStateOf(0f) }
    var keepScreenOn by remember { mutableStateOf(preferences.keepScreenOn) }
    var radarWatchEnabled by remember { mutableStateOf(preferences.radarWatchEnabled) }
    DisposableEffect(keepScreenOn) {
        val window = (context as? Activity)?.window
        if (keepScreenOn) window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    DisposableEffect(rotationSensor, rotateWithDevice) {
        if (!rotateWithDevice || rotationSensor == null || sensorManager == null) {
            onDispose { }
        } else {
            val rotationMatrix = FloatArray(9)
            val orientation = FloatArray(3)
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val current = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    azimuthDegrees = smoothAzimuthDegrees(azimuthDegrees, current)
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
            }
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
            onDispose { sensorManager.unregisterListener(listener) }
        }
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
                            text = stringResource(R.string.radar_title),
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
            radarWatchEnabled = radarWatchEnabled,
            onRadarWatchChanged = {
                radarWatchEnabled = it
                preferences.radarWatchEnabled = it
            },
            rotateWithDevice = rotateWithDevice && hasRotationVectorSensor(rotationSensor != null),
            sensorAvailable = hasRotationVectorSensor(rotationSensor != null),
            onRotateWithDeviceChanged = { rotateWithDevice = it },
            headingDegrees = if (rotateWithDevice && rotationSensor != null) azimuthDegrees else 0f,
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
    radarWatchEnabled: Boolean,
    onRadarWatchChanged: (Boolean) -> Unit,
    rotateWithDevice: Boolean,
    sensorAvailable: Boolean,
    onRotateWithDeviceChanged: (Boolean) -> Unit,
    headingDegrees: Float,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rangeNm by remember { mutableStateOf(15.0) }
    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onRotateWithDeviceChanged(false) }) {
                    Text(stringResource(R.string.radar_orientation_north))
                }
                Button(onClick = { onRotateWithDeviceChanged(true) }, enabled = sensorAvailable) {
                    Text(stringResource(R.string.radar_orientation_device))
                }
            }
            Text(
                text = stringResource(
                    if (rotateWithDevice && sensorAvailable) {
                        R.string.radar_orientation_device_active
                    } else {
                        R.string.radar_orientation_north_active
                    }
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (!sensorAvailable) {
                Text(stringResource(R.string.radar_orientation_unavailable), style = MaterialTheme.typography.labelSmall)
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5.0, 15.0, 50.0).forEach { option ->
                    Button(onClick = { rangeNm = option }) {
                        Text(stringResource(R.string.radar_range_chip, option.toInt()))
                    }
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.radar_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.radar_watch_label),
                    style = MaterialTheme.typography.bodyMedium
                )
                androidx.compose.material3.Switch(
                    checked = radarWatchEnabled,
                    onCheckedChange = onRadarWatchChanged
                )
            }
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
            val visibleAircraft = snapshot.aircraft.filter { it.distanceNauticalMiles <= rangeNm }
            item { RadarScope(snapshot.copy(aircraft = visibleAircraft), rangeNm, headingDegrees, rotateWithDevice && sensorAvailable) }
            item {
                Text(
                    text = stringResource(R.string.radar_airport_legend),
                    style = MaterialTheme.typography.labelMedium,
                    color = SkyWatchColors.DepartureAccent
                )
            }
            item { SnapshotTime(snapshot) }
            if (visibleAircraft.isEmpty()) {
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
                        text = stringResource(R.string.radar_aircraft_count, visibleAircraft.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(visibleAircraft, key = { it.icaoHex }) { aircraft ->
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
                text = stringResource(R.string.radar_attribution_plain),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RadarScope(snapshot: RadarSnapshot, rangeNm: Double, headingDegrees: Float, showLookArrow: Boolean) {
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
            // A rosa inteira gira junto: desenho, pontos, cardeais e cabeceiras.
            // Isso evita que os rótulos fiquem presos à tela enquanto o radar gira.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = -headingDegrees }
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
                        (aircraft.distanceNauticalMiles / rangeNm)
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
                    aircraft.trackDegrees?.let { track ->
                        val headingRadians = Math.toRadians(track)
                        drawLine(
                            color = SkyWatchColors.ArrivalAccent,
                            start = point,
                            end = Offset(
                                point.x + (sin(headingRadians) * 18f).toFloat(),
                                point.y - (cos(headingRadians) * 18f).toFloat()
                            ),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
            Text(stringResource(R.string.radar_north), Modifier.align(Alignment.TopCenter), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
            Text(stringResource(R.string.radar_east), Modifier.align(Alignment.CenterEnd), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
            Text(stringResource(R.string.radar_south), Modifier.align(Alignment.BottomCenter), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
            Text(stringResource(R.string.radar_west), Modifier.align(Alignment.CenterStart), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
            Text(stringResource(R.string.radar_runway_06), Modifier.align(Alignment.BottomStart).padding(20.dp), color = SkyWatchColors.DepartureAccent, fontSize = 12.sp)
            Text(stringResource(R.string.radar_runway_24), Modifier.align(Alignment.TopEnd).padding(20.dp), color = SkyWatchColors.DepartureAccent, fontSize = 12.sp)
            Text(
                text = stringResource(R.string.radar_airport_marker),
                modifier = Modifier.align(Alignment.Center).padding(top = 28.dp),
                color = SkyWatchColors.DepartureAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            }
            if (showLookArrow) {
                Text(stringResource(R.string.radar_look_arrow), Modifier.align(Alignment.TopCenter).padding(top = 18.dp), color = SkyWatchColors.DepartureAccent, fontSize = 22.sp)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.radar_north), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
            Text(
                text = stringResource(R.string.radar_center_marker),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Text(stringResource(R.string.radar_range_chip, rangeNm.toInt()), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
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
