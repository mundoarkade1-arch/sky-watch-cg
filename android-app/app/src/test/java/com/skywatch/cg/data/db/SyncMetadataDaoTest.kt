package com.skywatch.cg.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.sqlite.db.SimpleSQLiteQuery

@RunWith(RobolectricTestRunner::class)
class SyncMetadataDaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        
        // Manually add the CHECK constraint since in-memory DB doesn't run migrations
        runBlocking {
            val sqlDb = db.openHelper.writableDatabase
            sqlDb.execSQL("DROP TABLE IF EXISTS `sync_metadata`")
            sqlDb.execSQL(
                """
                CREATE TABLE `sync_metadata` (
                    `id` INTEGER NOT NULL,
                    `lastSuccessfulSyncAt` INTEGER NOT NULL,
                    `lastSyncStatus` TEXT NOT NULL,
                    `lastSyncErrorMessage` TEXT,
                    `recordsInserted` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    CHECK (`id` = 1)
                )
                """.trimIndent()
            )
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testSyncMetadataInsertAndRead() = runBlocking {
        val dao = db.syncMetadataDao()

        // Initially no metadata
        val initial = dao.getSyncMetadata()
        assertNull(initial)

        // Insert metadata
        val metadata = SyncMetadata(
            id = 1,
            lastSuccessfulSyncAt = 1700000000000L,
            lastSyncStatus = "SUCCESS",
            lastSyncErrorMessage = null,
            recordsInserted = 42
        )
        dao.upsertSyncMetadata(metadata)

        // Read back
        val retrieved = dao.getSyncMetadata()
        assertNotNull(retrieved)
        assertEquals(1, retrieved?.id)
        assertEquals(1700000000000L, retrieved?.lastSuccessfulSyncAt)
        assertEquals("SUCCESS", retrieved?.lastSyncStatus)
        assertNull(retrieved?.lastSyncErrorMessage)
        assertEquals(42, retrieved?.recordsInserted)
    }

    @Test
    fun testSyncMetadataUpdate() = runBlocking {
        val dao = db.syncMetadataDao()

        // Insert initial
        val initial = SyncMetadata(
            id = 1,
            lastSuccessfulSyncAt = 1000L,
            lastSyncStatus = "SUCCESS",
            lastSyncErrorMessage = null,
            recordsInserted = 10
        )
        dao.upsertSyncMetadata(initial)

        // Update with new values
        val updated = SyncMetadata(
            id = 1,
            lastSuccessfulSyncAt = 2000L,
            lastSyncStatus = "ERROR",
            lastSyncErrorMessage = "Network timeout",
            recordsInserted = 0
        )
        dao.upsertSyncMetadata(updated)

        // Verify update
        val retrieved = dao.getSyncMetadata()
        assertNotNull(retrieved)
        assertEquals(1, retrieved?.id)
        assertEquals(2000L, retrieved?.lastSuccessfulSyncAt)
        assertEquals("ERROR", retrieved?.lastSyncStatus)
        assertEquals("Network timeout", retrieved?.lastSyncErrorMessage)
        assertEquals(0, retrieved?.recordsInserted)
    }

    @Test
    fun testSyncMetadataSingleRowEnforced() = runBlocking {
        val dao = db.syncMetadataDao()

        // Insert first
        dao.upsertSyncMetadata(SyncMetadata(
            id = 1,
            lastSuccessfulSyncAt = 1000L,
            lastSyncStatus = "SUCCESS",
            lastSyncErrorMessage = null,
            recordsInserted = 10
        ))

        // Insert second with same id (should replace)
        dao.upsertSyncMetadata(SyncMetadata(
            id = 1,
            lastSuccessfulSyncAt = 2000L,
            lastSyncStatus = "ERROR",
            lastSyncErrorMessage = "Error",
            recordsInserted = 0
        ))

        // Should only have one row
        val retrieved = dao.getSyncMetadata()
        assertNotNull(retrieved)
        assertEquals(2000L, retrieved?.lastSuccessfulSyncAt)
    }

    @Test(expected = android.database.sqlite.SQLiteConstraintException::class)
    fun testSyncMetadataRejectId2() {
        runBlocking {
            val dao = db.syncMetadataDao()
            // Attempting to insert id = 2 should violate the CHECK (id = 1) constraint
            dao.upsertSyncMetadata(SyncMetadata(
                id = 2,
                lastSuccessfulSyncAt = 1000L,
                lastSyncStatus = "SUCCESS",
                lastSyncErrorMessage = null,
                recordsInserted = 10
            ))
        }
    }

    @Test(expected = android.database.sqlite.SQLiteConstraintException::class)
    fun testSyncMetadataRejectId3() {
        runBlocking {
            val dao = db.syncMetadataDao()
            // Attempting to insert id = 3 should violate the CHECK (id = 1) constraint
            dao.upsertSyncMetadata(SyncMetadata(
                id = 3,
                lastSuccessfulSyncAt = 1000L,
                lastSyncStatus = "SUCCESS",
                lastSyncErrorMessage = null,
                recordsInserted = 5
            ))
        }
    }
}