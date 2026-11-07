package com.skywatch.cg.data.radar

/**
 * Referência aeronáutica fixa usada somente pela lógica do radar.
 * Fonte oficial: AISWEB/DECEA, SBCG AD 2.2 e AD 2.12:
 * https://aisweb.decea.mil.br/eaip/A%2020-2026_2026_10_29/eAIP/AD%202%20SBCG-pt-BR.html
 *
 * A elevação do aeródromo é 559 m / 1834 ft. Os rumos verdadeiros publicados
 * das cabeceiras são 042.26° (06) e 222.26° (24).
 */
object RadarAirportConfig {
    const val SBCG_ICAO = "SBCG"
    const val ELEVATION_METERS = 559.0
    const val ELEVATION_FEET = 1_834
    const val RUNWAY_06_TRUE_HEADING_DEGREES = 42.26
    const val RUNWAY_24_TRUE_HEADING_DEGREES = 222.26
}
