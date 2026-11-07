package com.skywatch.cg.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class DetailScreenDisplayTest {
    @Test
    fun detailDisplayUsesFormattedAirportNames() {
        assertEquals(
            "Campinas (SBKP)",
            detailAirportDisplay("VIRACOPOS - CAMPINAS - SP - BRASIL (SBKP)", "SBKP")
        )
    }
}
