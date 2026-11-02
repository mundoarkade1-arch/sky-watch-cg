package com.skywatch.cg.data.siros

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class SirosNormalizerTest {

    private val recordSbcgDeparture = SirosRecord(
        codigoEmpresa = "GLO",
        empresa = "GOL",
        numeroVoo = "1234",
        equipamento = "B738",
        seg = "1", ter = "2", qua = "3", qui = "4", sex = "5", sab = "6", dom = "7",
        quantidadeAssentos = "186",
        numeroSiros = "GLO-001",
        situacaoSiros = "Em Operação",
        dataRegistro = "01/01/2024 10:00:00",
        inicioOperacao = "2024-10-01",
        fimOperacao = "2025-03-31",
        naturezaOperacao = "DOMÉSTICA",
        numeroEtapa = "1",
        codigoOrigem = "SBCG",
        arptOrigem = "CAMPO GRANDE",
        codigoDestino = "SBGR",
        arptDestino = "GUARULHOS",
        horarioPartida = "10:00",
        horarioChegada = "11:30",
        tipoServico = "REGULAR DE PASSAGEIROS",
        objetoTransporte = "PASSAGEIROS",
        codeshare = null
    )

    private val recordSbcgArrival = SirosRecord(
        codigoEmpresa = "TAM",
        empresa = "LATAM",
        numeroVoo = "5678",
        equipamento = "A320",
        seg = "1", ter = "0", qua = "3", qui = "0", sex = "5", sab = "0", dom = "0",
        quantidadeAssentos = "180",
        numeroSiros = "TAM-001",
        situacaoSiros = "A operar", // Voo futuro/planejado
        dataRegistro = "01/01/2024 10:00:00",
        inicioOperacao = "2024-11-01",
        fimOperacao = "2025-03-31",
        naturezaOperacao = "DOMÉSTICA",
        numeroEtapa = "1",
        codigoOrigem = "SBGR",
        arptOrigem = "GUARULHOS",
        codigoDestino = "SBCG",
        arptDestino = "CAMPO GRANDE",
        horarioPartida = "12:00",
        horarioChegada = "13:30",
        tipoServico = "REGULAR DE PASSAGEIROS",
        objetoTransporte = "PASSAGEIROS",
        codeshare = null
    )

    private val recordCargo = SirosRecord(
        codigoEmpresa = "CAR",
        empresa = "CARGO",
        numeroVoo = "8888",
        equipamento = "B73F",
        seg = "1", ter = "2", qua = "3", qui = "4", sex = "5", sab = "6", dom = "7",
        quantidadeAssentos = "0",
        numeroSiros = "CAR-001",
        situacaoSiros = "Em Operação",
        dataRegistro = "01/01/2024 10:00:00",
        inicioOperacao = "2024-10-01",
        fimOperacao = "2025-03-31",
        naturezaOperacao = "DOMÉSTICA",
        numeroEtapa = "1",
        codigoOrigem = "SBCG",
        arptOrigem = "CAMPO GRANDE",
        codigoDestino = "SBGR",
        arptDestino = "GUARULHOS",
        horarioPartida = "18:00",
        horarioChegada = "19:30",
        tipoServico = "REGULAR DE CARGA",
        objetoTransporte = "CARGA",
        codeshare = null
    )

    @Test
    fun testNormalizeFiltersPassengerFlights() {
        val referenceDate = LocalDate.of(2024, 10, 15)
        val normalized = SirosNormalizer.normalize(listOf(recordSbcgDeparture, recordCargo), referenceDate)
        
        // Deve incluir o voo de passageiros e excluir o de carga
        assertTrue(normalized.any { it.flightNumber == "1234" })
        assertFalse(normalized.any { it.flightNumber == "8888" })
    }

    @Test
    fun testNormalizeIncludesAOperarStatus() {
        val referenceDate = LocalDate.of(2024, 11, 15)
        val normalized = SirosNormalizer.normalize(listOf(recordSbcgArrival), referenceDate)
        
        // Voo "A operar" dentro da vigência deve ser incluído
        assertTrue(normalized.any { it.flightNumber == "5678" && it.sirosStatus == "A operar" })
    }

    @Test
    fun testNormalizeCorrectOperationType() {
        val referenceDate = LocalDate.of(2024, 10, 15)
        val normalized = SirosNormalizer.normalize(listOf(recordSbcgDeparture, recordSbcgArrival), referenceDate)
        
        val dep = normalized.first { it.flightNumber == "1234" }
        assertEquals(OperationType.DEPARTURE, dep.operationType)
        assertEquals("America/Campo_Grande", dep.originTimezone)

        val arr = normalized.first { it.flightNumber == "5678" }
        assertEquals(OperationType.ARRIVAL, arr.operationType)
        assertEquals("America/Campo_Grande", arr.destinationTimezone)
    }

    @Test
    fun testNormalizeTimezones() {
        val referenceDate = LocalDate.of(2024, 10, 15)
        val normalized = SirosNormalizer.normalize(listOf(recordSbcgDeparture), referenceDate)
        
        val flight = normalized.first()
        // Partida em SBCG -> Origem fuso America/Campo_Grande
        assertEquals("America/Campo_Grande", flight.originTimezone)
        // Destino em SBGR -> Destino fuso America/Sao_Paulo
        assertEquals("America/Sao_Paulo", flight.destinationTimezone)
    }
@Test
    fun testNormalizeOnlySaturday() {
        // Registro que opera apenas aos sábados (sab="6")
        val recordOnlySaturday = SirosRecord(
            codigoEmpresa = "GLO",
            empresa = "GOL",
            numeroVoo = "9999",
            equipamento = "B738",
            seg = "0", ter = "0", qua = "0", qui = "0", sex = "0", sab = "6", dom = "0",
            quantidadeAssentos = "186",
            numeroSiros = "GLO-999",
            situacaoSiros = "Em Operação",
            dataRegistro = "01/01/2024 10:00:00",
            inicioOperacao = "2024-10-01",
            fimOperacao = "2024-10-31",
            naturezaOperacao = "DOMÉSTICA",
            numeroEtapa = "1",
            codigoOrigem = "SBCG",
            arptOrigem = "CAMPO GRANDE",
            codigoDestino = "SBGR",
            arptDestino = "GUARULHOS",
            horarioPartida = "10:00",
            horarioChegada = "11:30",
            tipoServico = "REGULAR DE PASSAGEIROS",
            objetoTransporte = "PASSAGEIROS",
            codeshare = null
        )

        val referenceDate = LocalDate.of(2024, 10, 15)
        val normalized = SirosNormalizer.normalize(listOf(recordOnlySaturday), referenceDate)

        // Deve gerar voos apenas nos sábados da vigência (outubro 2024: 5, 12, 19, 26)
        assertTrue(normalized.isNotEmpty())
        for (flight in normalized) {
            assertEquals(OperationType.DEPARTURE, flight.operationType)
            // dayOfWeek 6 = Sábado
            assertEquals(6, flight.flightDate.dayOfWeek.value)
        }
        // 4 sábados em outubro 2024
        assertEquals(4, normalized.size)
    }

    @Test
    fun testNormalizeAllDaysOfWeek() {
        // Registro que opera todos os dias da semana (1..7)
        val recordAllDays = SirosRecord(
            codigoEmpresa = "TAM",
            empresa = "LATAM",
            numeroVoo = "7777",
            equipamento = "A320",
            seg = "1", ter = "2", qua = "3", qui = "4", sex = "5", sab = "6", dom = "7",
            quantidadeAssentos = "180",
            numeroSiros = "TAM-777",
            situacaoSiros = "Em Operação",
            dataRegistro = "01/01/2024 10:00:00",
            inicioOperacao = "2024-10-01",
            fimOperacao = "2024-10-07", // Uma semana completa
            naturezaOperacao = "DOMÉSTICA",
            numeroEtapa = "1",
            codigoOrigem = "SBCG",
            arptOrigem = "CAMPO GRANDE",
            codigoDestino = "SBGR",
            arptDestino = "GUARULHOS",
            horarioPartida = "10:00",
            horarioChegada = "11:30",
            tipoServico = "REGULAR DE PASSAGEIROS",
            objetoTransporte = "PASSAGEIROS",
            codeshare = null
        )

        val referenceDate = LocalDate.of(2024, 10, 1)
        val normalized = SirosNormalizer.normalize(listOf(recordAllDays), referenceDate)

        // Deve gerar 7 voos (um para cada dia de 1 a 7 de outubro)
        assertEquals(7, normalized.size)
        val daysOfWeek = normalized.map { it.flightDate.dayOfWeek.value }.toSet()
        // Deve conter todos os dias 1 a 7
        assertEquals(setOf(1, 2, 3, 4, 5, 6, 7), daysOfWeek)
    }
}