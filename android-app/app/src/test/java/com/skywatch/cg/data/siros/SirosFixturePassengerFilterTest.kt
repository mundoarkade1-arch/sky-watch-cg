package com.skywatch.cg.data.siros

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SirosFixturePassengerFilterTest {

    @Test
    fun fixtureUsesPassengerObjectAndServiceAndRejectsCargoVariant() {
        val input = requireNotNull(javaClass.classLoader?.getResourceAsStream("siros_cgr_real_sample.csv"))
        val utf8WithBom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + input.use { it.readBytes() }
        val records = SirosStreamParser.parse(utf8WithBom.inputStream())

        assertEquals(2, records.size)
        assertTrue(records.all { it.objetoTransporte.equals("PASSAGEIROS", ignoreCase = true) })
        assertTrue(records.all { it.tipoServico?.contains("PASSAGEIROS", ignoreCase = true) == true })

        val cargoVariant = records.first().copy(
            objetoTransporte = "CARGA",
            tipoServico = "REGULAR DE CARGA"
        )
        val normalized = SirosNormalizer.normalize(
            records + cargoVariant,
            referenceDate = LocalDate.of(2026, 11, 3)
        )

        assertEquals(2, normalized.size)
        assertTrue(normalized.all { it.flightNumber == "2655" || it.flightNumber == "2656" })
    }
}
