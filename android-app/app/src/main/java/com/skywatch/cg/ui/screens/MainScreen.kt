package com.skywatch.cg.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.skywatch.cg.BuildConfig
import com.skywatch.cg.R
import com.skywatch.cg.data.db.SyncMetadata
import com.skywatch.cg.data.siros.NormalizedFlight
import com.skywatch.cg.data.siros.OperationType
import com.skywatch.cg.ui.viewmodel.RadarViewModel
import com.skywatch.cg.ui.components.DayStrip
import com.skywatch.cg.ui.components.FlightCard
import com.skywatch.cg.ui.components.NextFlightCard
import com.skywatch.cg.ui.components.SegmentedTabRow
import com.skywatch.cg.ui.viewmodel.FlightDetailUiState
import com.skywatch.cg.ui.viewmodel.FlightListViewModel
import com.skywatch.cg.ui.viewmodel.FlightUiState
import com.skywatch.cg.util.getDisplayLastUpdate
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: FlightListViewModel, radarViewModel: RadarViewModel) {
    val navController = rememberNavController()
    
    NavHost(navController, startDestination = "voos") {
        composable("voos") {
            FlightsScreen(
                viewModel = viewModel,
                navController = navController
            )
        }
        composable("sobre") {
            AboutScreen(
                navController = navController,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "detalhe/{flightId}",
            arguments = listOf(navArgument("flightId") { type = NavType.StringType })
        ) { backStackEntry ->
            val flightId = backStackEntry.arguments?.getString("flightId")
            val detailState by viewModel.flightDetailUiState.collectAsState()

            LaunchedEffect(flightId) {
                flightId?.let(viewModel::loadFlightDetails)
            }

            if (flightId == null) {
                ErrorState(
                    message = "Identificador do voo inválido.",
                    onRetry = { navController.popBackStack() }
                )
            } else {
                when (val state = detailState) {
                    is FlightDetailUiState.Success -> {
                        if (state.flightId == flightId) {
                            DetailScreen(
                                flight = state.flight,
                                onBack = { navController.popBackStack() }
                            )
                        } else {
                            LoadingState()
                        }
                    }
                    is FlightDetailUiState.Error -> {
                        if (state.flightId == flightId) {
                            ErrorState(
                                message = state.message,
                                onRetry = { viewModel.loadFlightDetails(flightId) }
                            )
                        } else {
                            LoadingState()
                        }
                    }
                    else -> LoadingState()
                }
            }
        }
        composable("radar") {
            RadarScreen(
                viewModel = radarViewModel,
                navController = navController
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightsScreen(
    viewModel: FlightListViewModel,
    navController: androidx.navigation.NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val syncMetadata by viewModel.syncMetadata.collectAsState()

    val isRefreshing = uiState is FlightUiState.Refreshing
    val lastSyncError = syncMetadata?.let {
        if (it.lastSyncStatus == "FAILURE" && !it.lastSyncErrorMessage.isNullOrBlank()) {
            it.lastSyncErrorMessage
        } else null
    }

    val selectedTab = remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBarWrapper(
                viewModel = viewModel,
                uiState = uiState,
                isRefreshing = isRefreshing,
                syncMetadata = syncMetadata,
                lastSyncError = lastSyncError,
                selectedTab = selectedTab.value,
                onTabSelected = { selectedTab.value = it }
            )
        },
        bottomBar = { BottomNavBar(navController) }
    ) { innerPadding ->
        MainContent(
            viewModel = viewModel,
            uiState = uiState,
            selectedTab = selectedTab.value,
            onFlightSelected = { flight ->
                navController.navigate("detalhe/${Uri.encode(flight.flightId)}")
            },
            innerPadding = innerPadding
        )
    }
}

@Composable
fun TopAppBarWrapper(
    viewModel: FlightListViewModel,
    uiState: FlightUiState,
    isRefreshing: Boolean,
    syncMetadata: SyncMetadata?,
    lastSyncError: String?,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
    ) {
        val currentDate = when (uiState) {
            is FlightUiState.Success -> uiState.selectedDate
            is FlightUiState.Refreshing -> uiState.selectedDate
            else -> LocalDate.now()
        }

        val isToday = currentDate == LocalDate.now()
        val headerTitle = if (isToday) "Voos de hoje" else currentDate.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("pt", "BR"))).replaceFirstChar { it.uppercase() }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CAMPO GRANDE · SBCG",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = headerTitle,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            IconButton(
                onClick = viewModel::refresh,
                enabled = !isRefreshing,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Atualizar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        DayStrip(
            selectedDate = currentDate,
            onDateSelected = { viewModel.loadFlights(it) }
        )

        SegmentedTabRow(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected
        )

        if (syncMetadata != null) {
            Text(
                text = "Última sincronização: ${syncMetadata.lastSuccessfulSyncAt.getDisplayLastUpdate()}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }

        if (lastSyncError != null) {
            Text(
                text = "Última falha: $lastSyncError",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }
    }
}
@Composable
fun MainContent(
    viewModel: FlightListViewModel,
    uiState: FlightUiState,
    selectedTab: Int,
    onFlightSelected: (NormalizedFlight) -> Unit,
    innerPadding: PaddingValues
) {
    when (uiState) {
        is FlightUiState.Loading -> LoadingState()
        is FlightUiState.Error -> ErrorState(message = uiState.message, onRetry = { viewModel.refresh() })
        is FlightUiState.Success, is FlightUiState.Refreshing -> {
            val arrivals = if (uiState is FlightUiState.Success) uiState.arrivals else (uiState as FlightUiState.Refreshing).arrivals
            val departures = if (uiState is FlightUiState.Success) uiState.departures else (uiState as FlightUiState.Refreshing).departures
            val selectedDate = if (uiState is FlightUiState.Success) uiState.selectedDate else (uiState as FlightUiState.Refreshing).selectedDate

            val flights = if (selectedTab == 0) arrivals else departures
            val operationType = if (selectedTab == 0) OperationType.ARRIVAL else OperationType.DEPARTURE
            val isToday = selectedDate == LocalDate.now()

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = innerPadding
            ) {
                if (isToday) {
                    val now = LocalTime.now(ZoneId.of("America/Campo_Grande"))
                    val nextFlight = flights
                        .filter { it.isAfter(now, ZoneId.of("America/Campo_Grande")) }
                        .minByOrNull { it.getTimeUntil(ZoneId.of("America/Campo_Grande")) }

                    if (nextFlight != null) {
                        item {
                            NextFlightCard(flight = nextFlight) { onFlightSelected(nextFlight) }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                val futureFlights = if (isToday) {
                    flights.filter { it.isAfter(LocalTime.now(ZoneId.of("America/Campo_Grande")), ZoneId.of("America/Campo_Grande")) }
                } else { flights }

                if (futureFlights.isNotEmpty()) {
                    item { SectionHeader("MAIS TARDE") }
                    items(futureFlights) { flight ->
                        FlightCard(flight = flight, onClick = { onFlightSelected(flight) })
                    }
                }

                val pastFlights = if (isToday) {
                    flights.filter { !it.isAfter(LocalTime.now(ZoneId.of("America/Campo_Grande")), ZoneId.of("America/Campo_Grande")) }
                } else { emptyList() }

                if (pastFlights.isNotEmpty()) {
                    item { SectionHeader("JÁ PASSARAM", isDimmed = true) }
                    items(pastFlights) { flight ->
                        FlightCard(flight = flight, isDimmed = true, onClick = { onFlightSelected(flight) })
                    }
                }

                if (flights.isEmpty()) { item { EmptyState(operationType = operationType) } }

                item { SIROSDisclaimer() }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, isDimmed: Boolean = false) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isDimmed) 0.5f else 1f),
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun EmptyState(operationType: OperationType) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (operationType == OperationType.ARRIVAL) "Nenhuma chegada programada" else "Nenhuma partida programada",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Os horários são programados (SIROS/ANAC) e podem não refletir a operação em tempo real.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun SIROSDisclaimer() {
    Text(
        text = "Horários programados (SIROS/ANAC). Não confirmam a operação em tempo real.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 16.dp)
    )
}

@Composable
fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Carregando voos...", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Erro ao carregar", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text("Tentar de novo")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: androidx.navigation.NavController,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.fillMaxWidth(),
                title = {
                    Text(
                        text = "Sobre",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar para voos"
                        )
                    }
                }
            )
        },
        bottomBar = { BottomNavBar(navController) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "SKY WATCH CG",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Fonte dos horários",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "CSV público de registros vigentes da ANAC (SIROS).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Os horários exibidos são programados. Eles não confirmam o status operacional nem a posição atual de uma aeronave.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Versão ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(navController: androidx.navigation.NavController) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route ?: "voos"
    
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(bottom = 8.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        NavigationBarItem(
            icon = { Icon(painter = painterResource(R.drawable.ic_voos), contentDescription = null) },
            label = { Text(text = "Voos", style = MaterialTheme.typography.labelSmall) },
            selected = currentRoute == "voos",
            onClick = {
                navController.navigate("voos") {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(painter = painterResource(R.drawable.ic_sobre), contentDescription = null) },
            label = { Text(text = "Sobre", style = MaterialTheme.typography.labelSmall) },
            selected = currentRoute == "sobre",
            onClick = {
                navController.navigate("sobre") {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(painter = painterResource(R.drawable.ic_radar), contentDescription = null) },
            label = { Text(text = stringResource(R.string.radar_experimental), style = MaterialTheme.typography.labelSmall) },
            selected = currentRoute == "radar",
            onClick = {
                navController.navigate("radar") {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}

fun NormalizedFlight.isAfter(currentTime: LocalTime, zone: ZoneId): Boolean {
    val flightTime = if (operationType == OperationType.ARRIVAL) scheduledArrivalUtc else scheduledDepartureUtc
    val localFlightTime = flightTime.atDate(flightDate).atZone(ZoneOffset.UTC).withZoneSameInstant(zone).toLocalTime()
    return localFlightTime > currentTime
}

fun NormalizedFlight.getTimeUntil(zone: ZoneId): Long {
    val flightTime = if (operationType == OperationType.ARRIVAL) scheduledArrivalUtc else scheduledDepartureUtc
    val localFlightTime = flightTime.atDate(flightDate).atZone(ZoneOffset.UTC).withZoneSameInstant(zone).toLocalTime()
    val now = LocalTime.now(zone)
    return java.time.Duration.between(now, localFlightTime).toMinutes()
}
