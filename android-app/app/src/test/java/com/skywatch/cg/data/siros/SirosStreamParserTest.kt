package com.skywatch.cg.data.siros

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.Charset

class SirosStreamParserTest {

    private val sampleCsv = """Importante: Horários em UTC
Cód. Empresa;Empresa;Nº Voo;Equip.;Seg;Ter;Qua;Qui;Sex;Sáb;Dom;Quant. Assentos;Nº SIROS;Situação SIROS;Data Registro;Início Operação;Fim Operação;Natureza Operação;Nº Etapa;Cód. Origem;Arpt Origem;Cód Destino;Arpt Destino;Horário Partida;Horário Chegada;Tipo Serviço;Objeto Transporte;Codeshare
GLO;GOL LINHAS AEREAS;1234;B738;1;2;3;4;5;6;7;186;GLO-001;Em Operação;01/01/2024 10:00:00;2024-10-01;2025-03-31;DOMÉSTICA;1;SBCG;CAMPO GRANDE;SBGR;GUARULHOS;10:00;11:30;REGULAR DE PASSAGEIROS;PASSAGEIROS;
GLO;GOL LINHAS AEREAS;1235;B738;1;0;1;0;1;0;0;186;GLO-002;A operar;01/01/2024 10:00:00;2024-11-01;2025-03-31;DOMÉSTICA;1;SBGR;GUARULHOS;SBCG;CAMPO GRANDE;12:00;13:30;REGULAR DE PASSAGEIROS;PASSAGEIROS;
TAM;LATAM AIRLINES;5678;A320;1;1;1;1;1;1;1;180;TAM-001;Em Operação;01/01/2024 10:00:00;2024-10-01;2025-03-31;DOMÉSTICA;1;SBCG;CAMPO GRANDE;SBSP;CONGONHAS;14:00;15:30;REGULAR DE PASSAGEIROS;PASSAGEIROS;
AZU;AZUL LINHAS AEREAS;9999;E195;1;1;1;1;1;1;1;118;AZU-001;Cancelada;01/01/2024 10:00:00;2024-10-01;2025-03-31;DOMÉSTICA;1;SBCG;CAMPO GRANDE;SBGR;GUARULHOS;16:00;17:30;REGULAR DE PASSAGEIROS;PASSAGEIROS;
CAR;CARGO BRASIL;8888;B73F;1;1;1;1;1;1;1;0;CAR-001;Em Operação;01/01/2024 10:00:00;2024-10-01;2025-03-31;DOMÉSTICA;1;SBCG;CAMPO GRANDE;SBGR;GUARULHOS;18:00;19:30;REGULAR DE CARGA;CARGA;""".trimIndent()

    @Test
    fun testParseStreamValidLine() {
        val inputStream = ByteArrayInputStream(sampleCsv.toByteArray(Charset.forName("ISO-8859-1")))
        val records = SirosStreamParser.parseStream(inputStream).toList()
        assertEquals(5, records.size)
        assertEquals("GLO", records[0].codigoEmpresa)
    }

    @Test
    fun testParseStreamSkipsDescriptiveLine() {
        val inputStream = ByteArrayInputStream(sampleCsv.toByteArray(Charset.forName("ISO-8859-1")))
        val records = SirosStreamParser.parseStream(inputStream).toList()
        assertFalse(records.any { it.codigoEmpresa == "Importante" })
        assertEquals(5, records.size)
    }

    @Test
    fun testParseStreamSkipsHeader() {
        val inputStream = ByteArrayInputStream(sampleCsv.toByteArray(Charset.forName("ISO-8859-1")))
        val records = SirosStreamParser.parseStream(inputStream).toList()
        assertFalse(records.any { it.codigoEmpresa == "Cód. Empresa" })
        assertEquals(5, records.size)
    }

    @Test
    fun testParseStreamEmptyFields() {
        val csvWithEmpty = """Importante: Horários em UTC
Cód. Empresa;Empresa;Nº Voo;Equip.;Seg;Ter;Qua;Qui;Sex;Sáb;Dom;Quant. Assentos;Nº SIROS;Situação SIROS;Data Registro;Início Operação;Fim Operação;Natureza Operação;Nº Etapa;Cód. Origem;Arpt Origem;Cód Destino;Arpt Destino;Horário Partida;Horário Chegada;Tipo Serviço;Objeto Transporte;Codeshare
GLO;GOL LINHAS AEREAS;1234;;1;2;3;4;5;6;7;186;GLO-001;Em Operação;01/01/2024 10:00:00;2024-10-01;2025-03-31;DOMÉSTICA;1;SBCG;CAMPO GRANDE;SBGR;GUARULHOS;10:00;11:30;REGULAR DE PASSAGEIROS;PASSAGEIROS;""".trimIndent()

        val inputStream = ByteArrayInputStream(csvWithEmpty.toByteArray(Charset.forName("ISO-8859-1")))
        val records = SirosStreamParser.parseStream(inputStream).toList()

        assertEquals(1, records.size)
        assertNull(records[0].equipamento)
    }

    @Test
    fun testParseStreamMalformedLine() {
        val csvWithBadLine = """Importante: Horários em UTC
Cód. Empresa;Empresa;Nº Voo;Equip.;Seg;Ter;Qua;Qui;Sex;Sáb;Dom;Quant. Assentos;Nº SIROS;Situação SIROS;Data Registro;Início Operação;Fim Operação;Natureza Operação;Nº Etapa;Cód. Origem;Arpt Origem;Cód Destino;Arpt Destino;Horário Partida;Horário Chegada;Tipo Serviço;Objeto Transporte;Codeshare
GLO;GOL LINHAS AEREAS;1234;B738;1;2;3;4;5;6;7;186;GLO-001;Em Operação;01/01/2024 10:00:00;2024-10-01;2025-03-31;DOMÉSTICA;1;SBCG;CAMPO GRANDE;SBGR;GUARULHOS;10:00;11:30;REGULAR DE PASSAGEIROS;PASSAGEIROS;
MALFORMADA;LINHA
GLO;GOL LINHAS AEREAS;1235;B738;1;0;1;0;1;0;0;186;GLO-002;A operar;01/01/2024 10:00:00;2024-11-01;2025-03-31;DOMÉSTICA;1;SBGR;GUARULHOS;SBCG;CAMPO GRANDE;12:00;13:30;REGULAR DE PASSAGEIROS;PASSAGEIROS;""".trimIndent()

        val inputStream = ByteArrayInputStream(csvWithBadLine.toByteArray(Charset.forName("ISO-8859-1")))
        val records = SirosStreamParser.parseStream(inputStream).toList()
        assertEquals(2, records.size)
    }

    @Test
    fun testParseStreamMultipleRecords() {
        val inputStream = ByteArrayInputStream(sampleCsv.toByteArray(Charset.forName("ISO-8859-1")))
        val records = SirosStreamParser.parseStream(inputStream).toList()
        assertEquals(5, records.size)
    }

    @Test
    fun testParseStreamLazyEvaluation() {
        val inputStream = ByteArrayInputStream(sampleCsv.toByteArray(Charset.forName("ISO-8859-1")))
        val sequence = SirosStreamParser.parseStream(inputStream)
        val iterator = sequence.iterator()
        assertTrue(iterator.hasNext())
        assertEquals("GLO", iterator.next().codigoEmpresa)
    }

    @Test
    fun testParseStreamLatin1Encoding() {
        val csvWithAccents = """Importante: Horários em UTC
Cód. Empresa;Empresa;Nº Voo;Equip.;Seg;Ter;Qua;Qui;Sex;Sáb;Dom;Quant. Assentos;Nº SIROS;Situação SIROS;Data Registro;Início Operação;Fim Operação;Natureza Operação;Nº Etapa;Cód. Origem;Arpt Origem;Cód Destino;Arpt Destino;Horário Partida;Horário Chegada;Tipo Serviço;Objeto Transporte;Codeshare
GLO;GOL LINHAS AÉREAS;1234;B738;1;2;3;4;5;6;7;186;GLO-001;Em Operação;01/01/2024 10:00:00;2024-10-01;2025-03-31;DOMÉSTICA;1;SBCG;CAMPO GRANDE;SBGR;GUARULHOS;10:00;11:30;REGULAR DE PASSAGEIROS;PASSAGEIROS;""".trimIndent()

        val inputStream = ByteArrayInputStream(csvWithAccents.toByteArray(Charset.forName("ISO-8859-1")))
        val records = SirosStreamParser.parseStream(inputStream).toList()
        assertEquals("GOL LINHAS AÉREAS", records[0].empresa)
    }

    @Test
    fun testParseStreamWithCallback() {
        val inputStream = ByteArrayInputStream(sampleCsv.toByteArray(Charset.forName("ISO-8859-1")))
        val collected = mutableListOf<SirosRecord>()
        SirosStreamParser.parseStreamWithCallback(inputStream, onRecord = { collected.add(it) })
        assertEquals(5, collected.size)
    }

    @Test
    fun testParseStreamPreservesQuotedDelimiterAndEscapedQuote() {
        val quotedCsv = sampleCsv
            .replace("SBCG;CAMPO GRANDE;SBGR", "SBCG;\"CAMPO; GRANDE\";SBGR")
            .replace("GOL LINHAS AEREAS", "\"GOL \"\"LINHAS\"\" AEREAS\"")

        val records = SirosStreamParser.parseStream(
            ByteArrayInputStream(quotedCsv.toByteArray(Charset.forName("ISO-8859-1")))
        ).toList()

        assertEquals("GOL \"LINHAS\" AEREAS", records[0].empresa)
        assertEquals("CAMPO; GRANDE", records[0].arptOrigem)
    }

    @Test
    fun testParseStreamWithCallbackReportsShortLine() {
        val csv = "Importante: Horários em UTC\nCód. Empresa;Empresa\nGLO;LINHA"
        val errors = mutableListOf<Pair<Int, String>>()

        SirosStreamParser.parseStreamWithCallback(
            ByteArrayInputStream(csv.toByteArray(Charset.forName("ISO-8859-1"))),
            onRecord = {},
            onError = { line, message -> errors += line to message }
        )

        assertEquals(1, errors.size)
        assertEquals(3, errors.single().first)
    }
}
