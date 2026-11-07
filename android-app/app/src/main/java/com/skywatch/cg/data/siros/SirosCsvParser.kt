package com.skywatch.cg.data.siros

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset

/**
 * Parser para o CSV do SIROS/ANAC.
 */
object SirosCsvParser {

    private const val CHARSET_NAME = "ISO-8859-1"
    private const val HEADER_PREFIX = "Importante: Horários em UTC"

    fun parse(inputStream: InputStream): List<SirosRecord> {
        val records = mutableListOf<SirosRecord>()
        BufferedReader(InputStreamReader(inputStream, Charset.forName(CHARSET_NAME))).use { reader ->
            reader.forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.isNotBlank()) {
                    if (trimmed.startsWith(HEADER_PREFIX) || trimmed.contains("Cód. Empresa")) {
                        return@forEachLine
                    }
                    val fields = splitLine(trimmed)
                    if (fields.size >= 28) {
                        val record = SirosRecord(
                            codigoEmpresa = fields[0].takeIf { it.isNotBlank() },
                            empresa = fields[1].takeIf { it.isNotBlank() },
                            numeroVoo = fields[2].takeIf { it.isNotBlank() },
                            equipamento = fields[3].takeIf { it.isNotBlank() },
                            seg = fields[4].takeIf { it.isNotBlank() },
                            ter = fields[5].takeIf { it.isNotBlank() },
                            qua = fields[6].takeIf { it.isNotBlank() },
                            qui = fields[7].takeIf { it.isNotBlank() },
                            sex = fields[8].takeIf { it.isNotBlank() },
                            sab = fields[9].takeIf { it.isNotBlank() },
                            dom = fields[10].takeIf { it.isNotBlank() },
                            quantidadeAssentos = fields[11].takeIf { it.isNotBlank() },
                            numeroSiros = fields[12].takeIf { it.isNotBlank() },
                            situacaoSiros = fields[13].takeIf { it.isNotBlank() },
                            dataRegistro = fields[14].takeIf { it.isNotBlank() },
                            inicioOperacao = fields[15].takeIf { it.isNotBlank() },
                            fimOperacao = fields[16].takeIf { it.isNotBlank() },
                            naturezaOperacao = fields[17].takeIf { it.isNotBlank() },
                            numeroEtapa = fields[18].takeIf { it.isNotBlank() },
                            codigoOrigem = fields[19].takeIf { it.isNotBlank() },
                            arptOrigem = fields[20].takeIf { it.isNotBlank() },
                            codigoDestino = fields[21].takeIf { it.isNotBlank() },
                            arptDestino = fields[22].takeIf { it.isNotBlank() },
                            horarioPartida = fields[23].takeIf { it.isNotBlank() },
                            horarioChegada = fields[24].takeIf { it.isNotBlank() },
                            tipoServico = fields[25].takeIf { it.isNotBlank() },
                            objetoTransporte = fields[26].takeIf { it.isNotBlank() },
                            codeshare = fields[27].takeIf { it.isNotBlank() }
                        )
                        if (record.isValid()) {
                            records.add(record)
                        }
                    }
                }
            }
        }
        return records
    }

    private fun splitLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var index = 0
        while (index < line.length) {
            val c = line[index]
            when {
                c == '"' && inQuotes && index + 1 < line.length && line[index + 1] == '"' -> {
                    current.append('"')
                    index++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ';' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(c)
            }
            index++
        }
        result.add(current.toString().trim())
        return result
    }
}
