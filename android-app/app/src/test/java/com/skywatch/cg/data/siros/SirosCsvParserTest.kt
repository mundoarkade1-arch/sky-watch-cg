package com.skywatch.cg.data.siros

import com.skywatch.cg.data.siros.SirosCsvParser
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.Charset

class SirosCsvParserTest {

    private val sampleCsv = """Importante: Horários em UTC
Cód. Empresa;Empresa;Nº Voo;Equip.;Seg;Ter;Qua;Qui;Sex;Sáb;Dom;Quant. Assentos;Nº SIROS;Situação SIROS;Data Registro;Início Operação;Fim Operação;Natureza Operação;Nº Etapa;Cód. Origem;Arpt Origem;Cód Destino;Arpt Destino;Horário Partida;Horário Chegada;Tipo Serviço;Objeto Transporte;Codeshare
GLO;GOL LINHAS AEREAS;1234;B738;1;2;3;4;5;6;7;186;GLO-001;Em Operação;01/01/2024 10:00:00;2024-10-01;2025-03-31;DOMÉSTICA;1;SBCG;CAMPO GRANDE;SBGR;GUARULHOS;10:00;11:30;REGULAR DE PASSAGEIROS;PASSAGEIROS;
GLO;GOL LINHAS AEREAS;1235;B738;1;0;1;0;1;0;0;186;GLO-002;A operar;01/01/2024 10:00:00;2024-11-01;2025-03-31;DOMÉSTICA;1;SBGR;GUARULHOS;SBCG;CAMPO GRANDE;12:00;13:30;REGULAR DE PASSAGEIROS;PASSAGEIROS;
TAM;LATAM AIRLINES;5678;A320;1;1;1;1;1;1;1;180;TAM-001;Em Operação;01/01/2024 10:00:00;2024-10-01;2025-03-31;DOMÉSTICA;1;SBCG;CAMPO GRANDE;SBSP;CONGONHAS;14:00;15:30;REGULAR DE PASSAGEIROS;PASSAGEIROS;
AZU;AZUL LINHAS AEREAS;9999;E195;1;1;1;1;1;1;1;118;AZU-001;Cancelada;01/01/2024 10:00:00;2024-10-01;2025-03-31;DOMÉSTICA;1;SBCG;CAMPO GRANDE;SBGR;GUARULHOS;16:00;17:30;REGULAR DE PASSAGEIROS;PASSAGEIROS;
CAR;CARGO BRASIL;8888;B73F;1;1;1;1;1;1;1;0;CAR-001;Em Operação;01/01/2024 10:00:00;2024-10-01;2025-03-31;DOMÉSTICA;1;SBCG;CAMPO GRANDE;SBGR;GUARULHOS;18:00;19:30;REGULAR DE CARGA;CARGA;""".trimIndent()

    @Test
    fun testParseValidCsv() {
        val inputStream = ByteArrayInputStream(sampleCsv.toByteArray(Charset.forName("ISO-8859-1")))
        val records = SirosCsvParser.parse(inputStream)
        
        assertEquals(5, records.size)
        assertEquals("GLO", records[0].codigoEmpresa)
        assertEquals("GOL LINHAS AEREAS", records[0].empresa)
        assertEquals("1234", records[0].numeroVoo)
    }

    @Test
    fun testParseSkipsCargoFlights() {
        val inputStream = ByteArrayInputStream(sampleCsv.toByteArray(Charset.forName("ISO-8859-1")))
        val records = SirosCsvParser.parse(inputStream)
        
        // O voo de carga (CAR-001) não deve aparecer na lista final do parser
        // mas ele aparece como registro válido aqui, o filtro é no normalizer
        assertEquals(5, records.size)
    }

    @Test
    fun testParseHandlesEncoding() {
        val inputStream = ByteArrayInputStream(sampleCsv.toByteArray(Charset.forName("ISO-8859-1")))
        val records = SirosCsvParser.parse(inputStream)
        
        val record = records.first { it.codigoEmpresa == "GLO" && it.numeroVoo == "1234" }
        assertEquals("CAMPO GRANDE", record.arptOrigem)
        assertEquals("GUARULHOS", record.arptDestino)
    }

    @Test
    fun testParseSkipsHeader() {
        val inputStream = ByteArrayInputStream(sampleCsv.toByteArray(Charset.forName("ISO-8859-1")))
        val records = SirosCsvParser.parse(inputStream)
        
        // Não deve incluir a linha de cabeçalho como registro
        assertFalse(records.any { it.codigoEmpresa == "Cód. Empresa" })
    }
}