package com.skywatch.cg.data.siros

import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class SirosStreamParserRealFileTest {

    private val csvFile = File(System.getProperty("user.home"), "Downloads/siros-registros.csv")

    private fun usedMemoryMb(): Long {
        System.gc()
        val rt = Runtime.getRuntime()
        return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
    }

    @Test
    fun parseRealCsvStreamingCountsRecordsAndKeepsMemoryFlat() {
        assumeTrue("CSV real nao encontrado em ${csvFile.absolutePath}", csvFile.exists())

        val baselineMb = usedMemoryMb()
        var peakMb = baselineMb
        var records = 0
        var sbcgRecords = 0
        val startNs = System.nanoTime()

        csvFile.inputStream().use { input ->
            for (record in SirosStreamParser.parseStream(input)) {
                records++
                if (record.codigoOrigem == "SBCG" || record.codigoDestino == "SBCG") {
                    sbcgRecords++
                }
                if (records % 50_000 == 0) {
                    peakMb = maxOf(peakMb, usedMemoryMb())
                }
            }
        }

        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000
        val growthMb = peakMb - baselineMb
        val totalLines = csvFile.useLines(Charsets.ISO_8859_1) { lines -> lines.count() }

        println("REAL_CSV total_lines=$totalLines records=$records sbcg=$sbcgRecords baseline_mb=$baselineMb peak_live_mb=$peakMb growth_mb=$growthMb elapsed_ms=$elapsedMs")

        assertTrue("Nenhum registro lido do CSV real", records > 0)
        assertTrue("Nenhum registro com SBCG encontrado", sbcgRecords > 0)
        assertTrue("Memoria viva cresceu $growthMb MB, o parser pode estar acumulando registros", growthMb < 100)
    }
}
