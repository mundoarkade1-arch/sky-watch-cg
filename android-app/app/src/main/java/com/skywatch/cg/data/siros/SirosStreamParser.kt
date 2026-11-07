package com.skywatch.cg.data.siros

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PushbackInputStream
import java.nio.charset.Charset

object SirosStreamParser {

    private const val CHARSET_NAME = "ISO-8859-1"
    private const val DESCRIPTIVE_LINE_PREFIX = "Importante: Horários em UTC"
    private const val HEADER_MARKER = "Cód. Empresa"
    private const val EXPECTED_FIELD_COUNT = 28
    private const val DELIMITER = ';'

    private fun openReader(inputStream: InputStream): BufferedReader {
        val pushback = PushbackInputStream(inputStream, 3)
        val head = ByteArray(3)
        var read = 0
        while (read < 3) {
            val n = pushback.read(head, read, 3 - read)
            if (n < 0) break
            read += n
        }
        val hasBom = read == 3 &&
            head[0] == 0xEF.toByte() &&
            head[1] == 0xBB.toByte() &&
            head[2] == 0xBF.toByte()
        if (!hasBom && read > 0) {
            pushback.unread(head, 0, read)
        }
        val charset = if (hasBom) Charsets.UTF_8 else Charset.forName(CHARSET_NAME)
        return BufferedReader(InputStreamReader(pushback, charset))
    }

    fun parseStream(inputStream: InputStream): Sequence<SirosRecord> = sequence {
        openReader(inputStream).use { reader ->
            var headerFound = false

            for (line in reader.lineSequence()) {
                val trimmed = line.trim()
                if (trimmed.isNotBlank()) {
                    if (trimmed.startsWith(DESCRIPTIVE_LINE_PREFIX)) {
                        continue
                    }
                    if (trimmed.contains(HEADER_MARKER)) {
                        headerFound = true
                        continue
                    }
                    if (!headerFound) {
                        continue
                    }
                    val fields = splitLine(trimmed)
                    if (fields.size >= EXPECTED_FIELD_COUNT) {
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
                            yield(record)
                        }
                    }
                }
            }
            if (!headerFound) {
                throw IllegalArgumentException("Formato do CSV mudou: cabeçalho SIROS não reconhecido")
            }
        }
    }

    fun parse(inputStream: InputStream): List<SirosRecord> = parseStream(inputStream).toList()
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
                c == DELIMITER && !inQuotes -> {
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

    fun parseStreamWithCallback(
        inputStream: InputStream,
        onRecord: (SirosRecord) -> Unit,
        onError: ((Int, String) -> Unit)? = null
    ) {
        val reader = openReader(inputStream)
        var headerFound = false
        var lineNumber = 0

        try {
            reader.forEachLine { line ->
                lineNumber++
                val trimmed = line.trim()
                if (trimmed.isBlank()) {
                    return@forEachLine
                }
                if (trimmed.startsWith(DESCRIPTIVE_LINE_PREFIX)) {
                    return@forEachLine
                }
                if (trimmed.contains(HEADER_MARKER)) {
                    headerFound = true
                    return@forEachLine
                }
                if (!headerFound) {
                    return@forEachLine
                }
                val fields = splitLine(trimmed)
                if (fields.size < EXPECTED_FIELD_COUNT) {
                    onError?.invoke(lineNumber, "Campos insuficientes: ${fields.size}")
                    return@forEachLine
                }
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
                    onRecord(record)
                }
            }
            if (!headerFound) {
                onError?.invoke(lineNumber, "Formato do CSV mudou: cabeçalho SIROS não reconhecido")
            }
        } catch (e: Exception) {
            onError?.invoke(lineNumber, e.message ?: "Erro desconhecido")
        }
    }
}
