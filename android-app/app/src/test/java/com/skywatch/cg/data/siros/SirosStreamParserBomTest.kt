package com.skywatch.cg.data.siros

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.Charset

class SirosStreamParserBomTest {

    private val sampleCsv = """Importante: Horários em UTC
Cód. Empresa;Empresa;Nº Voo;Equip.;Seg;Ter;Qua;Qui;Sex;Sáb;Dom;Quant. Assentos;Nº SIROS;Situação SIROS;Data Registro;Início Operação;Fim Operação;Natureza Operação;Nº Etapa;Cód. Origem;Arpt Origem;Cód Destino;Arpt Destino;Horário Partida;Horário Chegada;Tipo Serviço;Objeto Transporte;Codeshare
GLO;GOL LINHAS AÉREAS;1234;B738;1;2;3;4;5;6;7;186;GLO-001;Em Operação;01/01/2024 10:00:00;2024-10-01;2025-03-31;DOMÉSTICA;1;SBCG;CAMPO GRANDE;SBGR;GUARULHOS;10:00;11:30;REGULAR DE PASSAGEIROS;PASSAGEIROS;"""

    @Test
    fun testParseStreamWithUtf8Bom() {
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val bodyBytes = sampleCsv.toByteArray(Charsets.UTF_8)
        val inputStream = ByteArrayInputStream(bom + bodyBytes)

        val records = SirosStreamParser.parseStream(inputStream).toList()
        assertEquals(1, records.size)
        assertEquals("GLO", records[0].codigoEmpresa)
        assertEquals("GOL LINHAS AÉREAS", records[0].empresa)
    }

    @Test
    fun testParseStreamWithCallbackUtf8Bom() {
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val bodyBytes = sampleCsv.toByteArray(Charsets.UTF_8)
        val inputStream = ByteArrayInputStream(bom + bodyBytes)

        val collected = mutableListOf<SirosRecord>()
        SirosStreamParser.parseStreamWithCallback(inputStream, onRecord = { collected.add(it) })
        assertEquals(1, collected.size)
    }

    @Test
    fun testParseStreamWithoutBomIso88591() {
        val inputStream = ByteArrayInputStream(sampleCsv.toByteArray(Charset.forName("ISO-8859-1")))

        val records = SirosStreamParser.parseStream(inputStream).toList()
        assertEquals(1, records.size)
        assertEquals("GLO", records[0].codigoEmpresa)
    }
}
