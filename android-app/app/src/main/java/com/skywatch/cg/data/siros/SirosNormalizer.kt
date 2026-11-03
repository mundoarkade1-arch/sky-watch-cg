package com.skywatch.cg.data.siros

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Normalizador que transforma SirosRecord brutos em NormalizedFlight prontos para uso.
 */
object SirosNormalizer {

    private const val SBCG = "SBCG"
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
    private val dateFormatterBr = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun normalize(records: List<SirosRecord>, referenceDate: LocalDate = LocalDate.now()): List<NormalizedFlight> {
        val result = mutableListOf<NormalizedFlight>()

        for (record in records) {
            if (!isPassengerFlight(record)) continue
            if (!hasSbcg(record)) continue
            if (!isValidStatus(record)) continue
            if (!isValidDateRange(record, referenceDate)) continue

            val daysOfWeek = parseDaysOfWeek(record)
            if (daysOfWeek.isEmpty()) continue

            val validFrom = parseDate(record.inicioOperacao)
            val validTo = parseDate(record.fimOperacao)
            if (validFrom == null || validTo == null) continue

            // Clamp generation to referenceDate - 1 .. referenceDate + 45 (faixa de datas requerida)
            val minDate = referenceDate.minusDays(1)
            val maxDate = referenceDate.plusDays(45)

            var currentDate: LocalDate = if (validFrom.isBefore(minDate)) minDate else validFrom
            val endDate: LocalDate = if (validTo.isAfter(maxDate)) maxDate else validTo

            while (!currentDate.isAfter(endDate)) {
                val dayOfWeek = currentDate.dayOfWeek.value
                if (dayOfWeek in daysOfWeek) {
                    createNormalizedFlights(record, currentDate)?.let { result.addAll(it) }
                }
                currentDate = currentDate.plusDays(1)
            }
        }
        return result
    }

    private fun isPassengerFlight(record: SirosRecord): Boolean {
        return record.objetoTransporte?.equals("PASSAGEIROS", true) == true &&
               record.tipoServico?.contains("PASSAGEIROS", true) == true
    }

    private fun hasSbcg(record: SirosRecord): Boolean {
        return record.codigoOrigem == "SBCG" || record.codigoDestino == "SBCG"
    }

    private fun isValidStatus(record: SirosRecord): Boolean {
        val status = record.situacaoSiros?.uppercase() ?: ""
        return status == "EM OPERAÇÃO" || status == "A OPERAR" || status == "EM OPERACAO"
    }

    private fun isValidDateRange(record: SirosRecord, referenceDate: LocalDate): Boolean {
        val inicio = parseDate(record.inicioOperacao)
        val fim = parseDate(record.fimOperacao)
        if (inicio == null || fim == null) return false
        // The date range must overlap with [referenceDate - 1, referenceDate + 45]
        val minDate = referenceDate.minusDays(1)
        val maxDate = referenceDate.plusDays(45)
        return !fim.isBefore(minDate) && !inicio.isAfter(maxDate) && !inicio.isAfter(fim)
    }

    private fun parseDaysOfWeek(record: SirosRecord): Set<Int> {
        val days = mutableSetOf<Int>()
        if (record.seg == "1") days.add(1)
        if (record.ter == "2") days.add(2)
        if (record.qua == "3") days.add(3)
        if (record.qui == "4") days.add(4)
        if (record.sex == "5") days.add(5)
        if (record.sab == "6") days.add(6)
        if (record.dom == "7") days.add(7)
        return days
    }

    private fun createNormalizedFlights(record: SirosRecord, flightDate: LocalDate): List<NormalizedFlight>? {
        val departure = parseTime(record.horarioPartida)
        val arrival = parseTime(record.horarioChegada)
        if (departure == null || arrival == null) return null

        val isArrival = record.codigoDestino == "SBCG"
        val isDeparture = record.codigoOrigem == "SBCG"
        if (!isArrival && !isDeparture) return null

        val operationType = if (isArrival) OperationType.ARRIVAL else OperationType.DEPARTURE
        val originTz = if (record.codigoOrigem == "SBCG") "America/Campo_Grande" else "America/Sao_Paulo"
        val destTz = if (record.codigoDestino == "SBCG") "America/Campo_Grande" else "America/Sao_Paulo"
        val sirosRecordId = record.numeroSiros ?: "${record.codigoEmpresa}-${record.numeroVoo}"

        // Se chegada UTC for antes da partida UTC, a chegada é no dia seguinte
        val arrivalDate = if (arrival.isBefore(departure)) flightDate.plusDays(1) else flightDate

        return listOf(
            NormalizedFlight(
                flightId = "$sirosRecordId-$flightDate",
                sirosRecordId = sirosRecordId,
                airline = record.empresa ?: "DESCONHECIDA",
                flightNumber = record.numeroVoo ?: "0000",
                equipment = record.equipamento,
                origin = record.codigoOrigem ?: "",
                originName = record.arptOrigem,
                originTimezone = originTz,
                destination = record.codigoDestino ?: "",
                destinationName = record.arptDestino,
                destinationTimezone = destTz,
                operationType = operationType,
                scheduledDepartureUtc = departure,
                scheduledArrivalUtc = arrival,
                flightDate = flightDate,
                arrivalDate = arrivalDate,
                daysOfWeek = parseDaysOfWeek(record).toList(),
                validFrom = parseDate(record.inicioOperacao) ?: flightDate,
                validTo = parseDate(record.fimOperacao) ?: flightDate,
                serviceType = record.tipoServico,
                sirosStatus = record.situacaoSiros ?: "DESCONHECIDO"
            )
        )
    }

    private fun parseTime(str: String?): LocalTime? {
        str?.let {
            try {
                return LocalTime.parse(it.trim(), timeFormatter)
            } catch (e: DateTimeParseException) {
                return null
            }
        }
        return null
    }

    private fun parseDate(str: String?): LocalDate? {
        str?.let {
            try {
                if (it.contains("/")) {
                    return LocalDate.parse(it.trim(), dateFormatterBr)
                } else if (it.contains("-")) {
                    return LocalDate.parse(it.trim(), dateFormatter)
                }
            } catch (e: DateTimeParseException) {
                return null
            }
        }
        return null
    }
}

