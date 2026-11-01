package com.skywatch.cg.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.skywatch.cg.R
import com.skywatch.cg.data.db.SyncMetadata
import com.skywatch.cg.data.siros.NormalizedFlight
import com.skywatch.cg.data.siros.OperationType
import com.skywatch.cg.ui.viewmodel.FlightListViewModel
import com.skywatch.cg.ui.viewmodel.FlightUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

sealed class Screen(val route: String, val titleRes: Int) {
    object Arrivals : Screen("arrivals", R.string.tab_arrivals)
    object Departures : Screen("departures", R.string.tab_departures)
}

private val campoGrandeZone = ZoneId.of("America/Campo_Grande")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val localDateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Campo_Grande"))

private fun SyncMetadata.getDisplayLastUpdate(): String {
    return if (lastSuccessfulSyncAt > 0) {
        localDateTimeFormatter.format(Instant.ofEpochMilli(lastSuccessfulSyncAt))
    } else {
        "Nunca sincronizado"
    }
}

private fun NormalizedFlight.getDisplayTime(isArrival: Boolean): String {
    val utcTime = if (isArrival) scheduledArrivalUtc else scheduledDepartureUtc
    val flightDateTime = flightDate.atTime(utcTime)
    val utcZoned = flightDateTime.atZone(ZoneId.of("UTC"))
    val localZoned = utcZoned.withZoneSameInstant(campoGrandeZone)
    return localZoned.format(timeFormatter)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: FlightListViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    val syncMetadata by viewModel.syncMetadata.collectAsState()

    val items = listOf(Screen.Arrivals, Screen.Departures)

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = stringResource(id = R.string.app_name),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Text(
                                text = stringResource(id = R.string.airport_subtitle),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(id = R.string.refresh_description)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.disclaimer_siros),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    syncMetadata?.let { metadata ->
                        Text(
                            text = "${stringResource(id = R.string.last_update_prefix)} ${metadata.getDisplayLastUpdate()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            val iconText = if (screen == Screen.Arrivals) "↓" else "↑"
                            Text(text = iconText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        },
                        label = { Text(stringResource(screen.titleRes)) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Arrivals.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Arrivals.route) {
                FlightListContent(uiState = uiState, operationType = OperationType.ARRIVAL)
            }
            composable(Screen.Departures.route) {
                FlightListContent(uiState = uiState, operationType = OperationType.DEPARTURE)
            }
        }
    }
}

@Composable
fun FlightListContent(uiState: FlightUiState, operationType: OperationType) {
    when (uiState) {
        is FlightUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = stringResource(id = R.string.loading_flights))
                }
            }
        }
        is FlightUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "${stringResource(id = R.string.error_loading)}: ${uiState.message}",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        is FlightUiState.Success -> {
            val flights = if (operationType == OperationType.ARRIVAL) uiState.arrivals else uiState.departures
            if (flights.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(id = R.string.empty_flights),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(flights) { flight ->
                        FlightCard(flight = flight)
                    }
                }
            }
        }
    }
}

@Composable
fun FlightCard(flight: NormalizedFlight) {
    val isArrival = flight.operationType == OperationType.ARRIVAL
    val timeLabel = if (isArrival) "Chegada: ${flight.getDisplayTime(true)}" else "Partida: ${flight.getDisplayTime(false)}"
    val locationLabel = if (isArrival) "Origem: ${flight.origin} (${flight.originName ?: ""})" else "Destino: ${flight.destination} (${flight.destinationName ?: ""})"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${flight.airline} ${flight.flightNumber}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isArrival) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = if (isArrival) "CHEGADA" else "PARTIDA",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = locationLabel, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = timeLabel, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.secondary)
                Text(text = "Horário local (UTC-4)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                if (!flight.equipment.isNullOrBlank()) {
                    Text(text = "Aeronave: ${flight.equipment}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Status SIROS: ${flight.sirosStatus} (${flight.flightDate})", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}