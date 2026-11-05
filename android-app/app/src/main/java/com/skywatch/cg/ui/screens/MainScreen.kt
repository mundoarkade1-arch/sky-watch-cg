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
import androidx.compose.ui.platform.LocalContext
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
import com.skywatch.cg.data.prefs.AppPreferences
import com.skywatch.cg.notification.NotificationHelper
import com.skywatch.cg.data.siros.OperationType
import com.skywatch.cg.ui.viewmodel.RadarViewModel
import com.skywatch.cg.ui.components.DayStrip
import com.skywatch.cg.ui.components.FlightCard
import com.skywatch.cg.ui.components.SegmentedTabRow
import com.skywatch.cg.ui.viewmodel.FlightDetailUiState
import com.skywatch.cg.ui.viewmodel.FlightListViewModel
import com.skywatch.cg.ui.viewmodel.FlightUiState
import com.skywatch.cg.util.getDisplayLastUpdate
import com.skywatch.cg.util.getAirportEventDateTime
import com.skywatch.cg.util.ObservationWindow
import com.skywatch.cg.util.ObservationWindowCalculator
import com.skywatch.cg.util.campoGrandeZone
import com.skywatch.cg.util.formatAirlineName
import com.skywatch.cg.util.formatTimeUntil
import com.skywatch.cg.util.nextMovements
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: FlightListViewModel, radarViewModel: RadarViewModel) {
    val navController = rememberNavController()
    val radarState by radarViewModel.uiState.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
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
    radarState.radarAlert?.let { alert ->
        Surface(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
            color = MaterialTheme.colorScheme.tertiaryContainer
        ) {
            Text(
                text = if (alert.type.name == "ARRIVAL") {
                    "Avião chegando · a ${alert.distanceNauticalMiles.toInt()} NM · pouso estimado em ~${alert.estimatedMinutes ?: 0} min"
                } else "Avião saindo",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
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
    val currentLocalDateTime by viewModel.currentLocalDateTime.collectAsState()
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
            currentLocalDateTime = currentLocalDateTime,
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
            else -> LocalDate.now(campoGrandeZone)
        }

        val isToday = currentDate == LocalDate.now(campoGrandeZone)
        val headerTitle = if (isToday) "Voos de hoje" else currentDate.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("pt", "BR"))).replaceFirstChar { it.uppercase() }
        val (arrivalsCount, departuresCount) = when (uiState) {
            is FlightUiState.Success -> uiState.arrivals.size to uiState.departures.size
            is FlightUiState.Refreshing -> uiState.arrivals.size to uiState.departures.size
            else -> 0 to 0
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
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
                Text(
                    text = "Horários locais de Campo Grande · SIROS/ANAC",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
            arrivalsCount = arrivalsCount,
            departuresCount = departuresCount,
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
    currentLocalDateTime: LocalDateTime,
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

            val allFlights = (arrivals + departures)
                .distinctBy { it.flightId }
                .sortedBy { it.getAirportEventDateTime() }
            val flights = when (selectedTab) {
                1 -> allFlights.filter { it.operationType == OperationType.ARRIVAL }
                2 -> allFlights.filter { it.operationType == OperationType.DEPARTURE }
                else -> allFlights
            }
            val isToday = selectedDate == LocalDate.now(campoGrandeZone)
            val now = currentLocalDateTime
            var showPast by remember(selectedDate, selectedTab) { mutableStateOf(false) }
            val bestWindow = ObservationWindowCalculator.findBestWindow(
                flights = allFlights,
                selectedDate = selectedDate,
                now = now
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = innerPadding
            ) {
                item {
                    val next = nextMovements(allFlights, now)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NextMovementCard("PRÓXIMO POUSO", next.nextArrival, now, Modifier.weight(1f))
                        NextMovementCard("PRÓXIMA DECOLAGEM", next.nextDeparture, now, Modifier.weight(1f))
                    }
                }
                item {
                    if (bestWindow != null) {
                        ObservationWindowCard(bestWindow)
                    } else if (isToday && allFlights.isNotEmpty()) {
                        Text(
                            text = "Não há mais um período completo de 3 horas com voos programados hoje.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }

                item {
                    SectionHeader("VOOS PROGRAMADOS · ${flights.size}")
                }

                val pastFlights = flights.filter { isToday && it.getAirportEventDateTime().isBefore(now) }
                val upcomingFlights = flights.filterNot { it in pastFlights }
                if (pastFlights.isNotEmpty()) {
                    item {
                        TextButton(onClick = { showPast = !showPast }) {
                            Text("JÁ PASSARAM (${pastFlights.size})")
                        }
                    }
                    if (showPast) items(pastFlights, key = { "past-${it.flightId}" }) { flight ->
                        FlightCard(
                            flight = flight,
                            isDimmed = true,
                            onClick = { onFlightSelected(flight) }
                        )
                    }
                }
                if (upcomingFlights.isNotEmpty()) {
                    items(upcomingFlights, key = { it.flightId }) { flight ->
                        FlightCard(
                            flight = flight,
                            isDimmed = false,
                            onClick = { onFlightSelected(flight) }
                        )
                    }
                } else if (pastFlights.isEmpty()) {
                    item { EmptyState(selectedTab) }
                }

                item { SIROSDisclaimer() }
            }
        }
    }
}

@Composable
private fun NextMovementCard(
    title: String,
    flight: NormalizedFlight?,
    now: LocalDateTime,
    modifier: Modifier
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            if (flight == null) {
                Text("—", style = MaterialTheme.typography.titleLarge)
            } else {
                val event = flight.getAirportEventDateTime()
                Text(flight.getDisplayTime(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(formatAirlineName(flight.airline), style = MaterialTheme.typography.labelMedium)
                Text("${flight.origin} → ${flight.destination}", style = MaterialTheme.typography.labelSmall)
                Text(formatTimeUntil(java.time.Duration.between(now, event).toMinutes()), style = MaterialTheme.typography.labelSmall)
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
fun EmptyState(selectedTab: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = when (selectedTab) {
                    1 -> "Nenhum pouso programado"
                    2 -> "Nenhuma decolagem programada"
                    else -> "Nenhum voo programado para este dia"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "A lista usa voos de passageiros registrados no SIROS/ANAC para CGR.",
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
        text = "Voos de passageiros ativos encontrados no SIROS/ANAC para CGR. Horários locais de Campo Grande; não confirmam operação real e não abrangem necessariamente cargueiros, privados ou voos fora do cadastro.",
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
    val context = LocalContext.current
    val preferences = remember { AppPreferences.getInstance(context) }
    var soundVibration by remember { mutableStateOf(preferences.soundVibration) }
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Som e vibração", modifier = Modifier.weight(1f))
                        Switch(
                            checked = soundVibration,
                            onCheckedChange = {
                                soundVibration = it
                                preferences.soundVibration = it
                            }
                        )
                    }
                    Button(onClick = {
                        NotificationHelper.showTextAlert(
                            context, "Teste de notificação",
                            "Notificação de teste do SKY WATCH CG", 0x534B59
                        )
                    }) { Text("Testar notificação") }
                    Text(
                        "Em alguns celulares (ex.: Xiaomi), libere o app em Bateria > Sem restrições.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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

@Composable
private fun ObservationWindowCard(window: ObservationWindow) {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "MELHOR FAIXA PARA OBSERVAR · 3 HORAS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${window.start.format(formatter)}–${window.end.format(formatter)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "${window.totalMovements} movimentos: ${window.arrivals} pousos · ${window.departures} decolagens",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Estimativa pelos horários programados no SIROS/ANAC; não garante a operação real.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}
