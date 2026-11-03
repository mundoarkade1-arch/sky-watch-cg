package com.skywatch.cg.data.siros

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.skywatch.cg.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class SirosRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: SirosRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SirosRepository(db, Dispatchers.IO, SirosRemoteSource.INSTANCE)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testSaveAndRetrieveFlightsByDate() = runBlocking {
        val flightDate1 = LocalDate.of(2024, 10, 15)
        val flightDate2 = LocalDate.of(2024, 10, 16)

        val flight1 = NormalizedFlight(
            flightId = "GLO-1234-2024-10-15",
            sirosRecordId = "GLO-001",
            airline = "GOL",
            flightNumber = "1234",
            equipment = "B738",
            origin = "SBCG",
            originName = "CAMPO GRANDE",
            originTimezone = "America/Campo_Grande",
            destination = "SBGR",
            destinationName = "GUARULHOS",
            destinationTimezone = "America/Sao_Paulo",
            operationType = OperationType.DEPARTURE,
            scheduledDepartureUtc = LocalTime.of(10, 0),
            scheduledArrivalUtc = LocalTime.of(11, 30),
            flightDate = flightDate1,
            arrivalDate = flightDate1,
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
            validFrom = LocalDate.of(2024, 10, 1),
            validTo = LocalDate.of(2025, 3, 31),
            serviceType = "REGULAR DE PASSAGEIROS",
            sirosStatus = "Em Operação"
        )

        val flight2 = NormalizedFlight(
            flightId = "GLO-1234-2024-10-16",
            sirosRecordId = "GLO-001",
            airline = "GOL",
            flightNumber = "1234",
            equipment = "B738",
            origin = "SBCG",
            originName = "CAMPO GRANDE",
            originTimezone = "America/Campo_Grande",
            destination = "SBGR",
            destinationName = "GUARULHOS",
            destinationTimezone = "America/Sao_Paulo",
            operationType = OperationType.DEPARTURE,
            scheduledDepartureUtc = LocalTime.of(10, 0),
            scheduledArrivalUtc = LocalTime.of(11, 30),
            flightDate = flightDate2,
            arrivalDate = flightDate2,
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
            validFrom = LocalDate.of(2024, 10, 1),
            validTo = LocalDate.of(2025, 3, 31),
            serviceType = "REGULAR DE PASSAGEIROS",
            sirosStatus = "Em Operação"
        )

        repository.saveNormalizedFlights(listOf(flight1, flight2))

        val departuresDate1 = repository.getDeparturesForUi(flightDate1)
        assertEquals(1, departuresDate1.size)
        assertEquals("GLO-1234-2024-10-15", departuresDate1[0].flightId)

        val departuresDate2 = repository.getDeparturesForUi(flightDate2)
        assertEquals(1, departuresDate2.size)
        assertEquals("GLO-1234-2024-10-16", departuresDate2[0].flightId)

        // Test duplicate prevention / upsert behavior for same flightId
        val updatedFlight1 = flight1.copy(sirosStatus = "Confirmado")
        repository.saveNormalizedFlights(listOf(updatedFlight1))

        val retrieved = repository.getFlightById("GLO-1234-2024-10-15")
        assertNotNull(retrieved)
        assertEquals("Confirmado", retrieved?.sirosStatus)
    }

    @Test
    fun testClearAndOldFlightsDeletion() = runBlocking {
        val flightDate = LocalDate.of(2024, 10, 15)
        val flight = NormalizedFlight(
            flightId = "TAM-3500-2024-10-15",
            sirosRecordId = "TAM-001",
            airline = "LATAM",
            flightNumber = "3500",
            equipment = "A320",
            origin = "SBGR",
            originName = "GUARULHOS",
            originTimezone = "America/Sao_Paulo",
            destination = "SBCG",
            destinationName = "CAMPO GRANDE",
            destinationTimezone = "America/Campo_Grande",
            operationType = OperationType.ARRIVAL,
            scheduledDepartureUtc = LocalTime.of(8, 0),
            scheduledArrivalUtc = LocalTime.of(9, 15),
            flightDate = flightDate,
            arrivalDate = flightDate,
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
            validFrom = LocalDate.of(2024, 10, 1),
            validTo = LocalDate.of(2025, 3, 31),
            serviceType = "REGULAR DE PASSAGEIROS",
            sirosStatus = "Em Operação"
        )

        repository.saveNormalizedFlights(listOf(flight))
        assertNotNull(repository.getFlightById("TAM-3500-2024-10-15"))

        repository.deleteOldFlights(LocalDate.of(2024, 10, 16))
        assertNull(repository.getFlightById("TAM-3500-2024-10-15"))
    }

    // Fake remote source for testing syncFromRemote
    private class FakeSirosRemoteSource(
        private val csvContent: String,
        private val shouldFail: Boolean = false,
        private val failureMessage: String = "Erro de download"
    ) : SirosRemoteSource("http://fake", SirosRemoteSource.Companion.TEST_CLIENT) {
        override suspend fun downloadCsvToFileInstance(tempFile: File): DownloadResult {
            if (shouldFail) {
                return DownloadResult.Failure(failureMessage, 500)
            }
            tempFile.parentFile?.mkdirs()
            tempFile.writeText(csvContent, Charset.forName("ISO-8859-1"))
            return DownloadResult.Success(csvContent.length.toLong(), 10L, 200)
        }
    }

    @Test
    fun testSyncFromRemoteSuccess() = runBlocking {
        val testCsv = """
            Importante: Horários em UTC
            Cód. Empresa;Empresa;Nº Voo;Equip.;Seg;Ter;Qua;Qui;Sex;Sáb;Dom;Quant. Assentos;Nº SIROS;Situação SIROS;Data Registro;Início Operação;Fim Operação;Natureza Operação;Nº Etapa;Cód. Origem;Arpt Origem;Cód Destino;Arpt Destino;Horário Partida;Horário Chegada;Tipo Serviço;Objeto Transporte;Codeshare
            GLO;GOL LINHAS AEREAS;1234;B738;1;2;3;4;5;6;7;186;GLO-001;Em Operação;01/01/2024 10:00:00;2026-01-01;2026-12-31;DOMÉSTICA;1;SBGR;CAMPO GRANDE;SBCG;GUARULHOS;10:00;11:30;REGULAR DE PASSAGEIROS;PASSAGEIROS;
            TAM;LATAM AIRLINES;3500;A320;1;2;3;4;5;6;7;180;TAM-001;Em Operação;01/01/2024 10:00:00;2026-01-01;2026-12-31;DOMÉSTICA;1;SBCG;CAMPO GRANDE;SBGR;GUARULHOS;08:00;09:15;REGULAR DE PASSAGEIROS;PASSAGEIROS;
        """.trimIndent()

        val testDb = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        val fakeRemoteSource = FakeSirosRemoteSource(testCsv)
        val testRepository = SirosRepository(testDb, Dispatchers.IO, fakeRemoteSource)

        val testDate = LocalDate.of(2026, 6, 15)
        val result = testRepository.syncFromRemote(testDate)

        assertTrue(result is SirosRepository.SyncResult.Success)
        val success = result as SirosRepository.SyncResult.Success
        // SirosNormalizer expande cada registro para todos os dias válidos no intervalo [referenceDate-1, referenceDate+45].
        // Com 2 registros operando todos os dias da semana (1-7) e intervalo de 47 dias, gera ~94 voos normalizados.
        assertEquals(94, success.totalSynced)

        // Query for a date within the valid range (2026-06-15 is within 2026-01-01 to 2026-12-31)
        val arrivals = testRepository.getArrivalsForUi(testDate)
        val departures = testRepository.getDeparturesForUi(testDate)
        assertEquals(1, arrivals.size)
        assertEquals(1, departures.size)
        // O CSV usa nomes completos das empresas: "GOL LINHAS AEREAS" e "LATAM AIRLINES"
        assertEquals("GOL LINHAS AEREAS", arrivals[0].airline)
        assertEquals("LATAM AIRLINES", departures[0].airline)

        testDb.close()
    }

    @Test
    fun testSyncFromRemoteDownloadFailurePreservesGrid() = runBlocking {
        val testDb = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        val testRepository = SirosRepository(testDb, Dispatchers.IO, FakeSirosRemoteSource("", true, "Erro de rede"))

        val existingFlight = NormalizedFlight(
            flightId = "GLO-1234-2024-10-15",
            sirosRecordId = "GLO-001",
            airline = "GOL",
            flightNumber = "1234",
            equipment = "B738",
            origin = "SBGR",
            originName = "GUARULHOS",
            originTimezone = "America/Sao_Paulo",
            destination = "SBCG",
            destinationName = "CAMPO GRANDE",
            destinationTimezone = "America/Campo_Grande",
            operationType = OperationType.DEPARTURE,
            scheduledDepartureUtc = LocalTime.of(10, 0),
            scheduledArrivalUtc = LocalTime.of(11, 30),
            flightDate = LocalDate.of(2024, 10, 15),
            arrivalDate = LocalDate.of(2024, 10, 15),
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
            validFrom = LocalDate.of(2024, 10, 1),
            validTo = LocalDate.of(2025, 3, 31),
            serviceType = "REGULAR DE PASSAGEIROS",
            sirosStatus = "Em Operação"
        )
        testRepository.saveNormalizedFlights(listOf(existingFlight))

        val result = testRepository.syncFromRemote()

        assertTrue(result is SirosRepository.SyncResult.Failure)

        val departures = testRepository.getDeparturesForUi(LocalDate.of(2024, 10, 15))
        assertEquals(1, departures.size)
        assertEquals("GLO-1234-2024-10-15", departures[0].flightId)

        val metadata = testRepository.getSyncMetadata()
        assertNotNull(metadata)
        assertEquals("FAILURE", metadata?.lastSyncStatus)

        testDb.close()
    }

    @Test
    fun testSyncFromRemoteZeroSbcgFlightsPreservesGrid() = runBlocking {
        val testDb = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        val testCsv = """
            Importante: Horários em UTC
            Cód. Empresa;Empresa;Nº Voo;Equip.;Seg;Ter;Qua;Qui;Sex;Sáb;Dom;Quant. Assentos;Nº SIROS;Situação SIROS;Data Registro;Início Operação;Fim Operação;Natureza Operação;Nº Etapa;Cód. Origem;Arpt Origem;Cód Destino;Arpt Destino;Horário Partida;Horário Chegada;Tipo Serviço;Objeto Transporte;Codeshare
            GLO;GOL LINHAS AEREAS;1234;B738;1;2;3;4;5;6;7;186;GLO-001;Em Operação;01/01/2024 10:00:00;2024-10-01;2025-03-31;DOMÉSTICA;1;SBGR;CAMPO GRANDE;SBGR;GUARULHOS;10:00;11:30;REGULAR DE PASSAGEIROS;PASSAGEIROS;
        """.trimIndent()

        val fakeRemoteSource = FakeSirosRemoteSource(testCsv)
        val testRepository = SirosRepository(testDb, Dispatchers.IO, fakeRemoteSource)

        val existingFlight = NormalizedFlight(
            flightId = "GLO-1234-2024-10-15",
            sirosRecordId = "GLO-001",
            airline = "GOL",
            flightNumber = "1234",
            equipment = "B738",
            origin = "SBGR",
            originName = "GUARULHOS",
            originTimezone = "America/Sao_Paulo",
            destination = "SBCG",
            destinationName = "CAMPO GRANDE",
            destinationTimezone = "America/Campo_Grande",
            operationType = OperationType.DEPARTURE,
            scheduledDepartureUtc = LocalTime.of(10, 0),
            scheduledArrivalUtc = LocalTime.of(11, 30),
            flightDate = LocalDate.of(2024, 10, 15),
            arrivalDate = LocalDate.of(2024, 10, 15),
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
            validFrom = LocalDate.of(2024, 10, 1),
            validTo = LocalDate.of(2025, 3, 31),
            serviceType = "REGULAR DE PASSAGEIROS",
            sirosStatus = "Em Operação"
        )
        testRepository.saveNormalizedFlights(listOf(existingFlight))

        val result = testRepository.syncFromRemote(LocalDate.of(2024, 10, 15))

        assertTrue(result is SirosRepository.SyncResult.Failure)
        val failure = result as SirosRepository.SyncResult.Failure
        assertTrue(failure.errorMessage.contains("Nenhum voo SBCG"))

        val departures = testRepository.getDeparturesForUi(LocalDate.of(2024, 10, 15))
        assertEquals(1, departures.size)
        assertEquals("GLO-1234-2024-10-15", departures[0].flightId)

        testDb.close()
    }
}