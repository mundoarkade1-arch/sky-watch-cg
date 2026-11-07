package com.skywatch.cg.data.siros

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.skywatch.cg.data.db.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
class SirosStageIdentityTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun sameSirosRecordWithDifferentStagesKeepsArrivalAndDeparture() = runBlocking {
        val resource = requireNotNull(javaClass.classLoader?.getResourceAsStream("siros_cgr_real_sample.csv"))
        val csvBytes = resource.readBytes()
        val utf8WithBom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + csvBytes
        val realRecords = SirosStreamParser.parseStream(ByteArrayInputStream(utf8WithBom)).toList()
        val syntheticSameRecord = realRecords[1].copy(
            numeroSiros = realRecords[0].numeroSiros,
            numeroEtapa = "2"
        )

        val flights = SirosNormalizer.normalize(
            records = listOf(realRecords[0], syntheticSameRecord),
            referenceDate = LocalDate.of(2026, 11, 3)
        )
        database.flightDao().upsertAll(flights.map { com.skywatch.cg.data.db.FlightEntity.fromNormalizedFlight(it) })

        assertEquals(2, flights.size)
        assertNotEquals(flights[0].flightId, flights[1].flightId)
        assertEquals(1, database.flightDao().getAllArrivals().size)
        assertEquals(1, database.flightDao().getAllDepartures().size)
        assertEquals(2, database.flightDao().getBySirosRecordId(realRecords[0].numeroSiros!!).size)
    }
}
