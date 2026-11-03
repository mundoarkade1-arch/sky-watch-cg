package com.skywatch.cg.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [FlightEntity::class, SyncMetadata::class], version = 3, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun flightDao(): FlightDao
    abstract fun syncMetadataDao(): SyncMetadataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val DATABASE_NAME = "sky_watch_cg.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sync_metadata` (
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Adicionar coluna arrivalDate na tabela flights
                database.execSQL(
                    "ALTER TABLE `flights` ADD COLUMN `arrivalDate` TEXT NOT NULL DEFAULT ''"
                )
                // Atualizar arrivalDate existente = flightDate para voos já persistidos
                database.execSQL(
                    "UPDATE `flights` SET `arrivalDate` = `flightDate` WHERE `arrivalDate` = ''"
                )
                // Garantir CHECK (id = 1) em sync_metadata para instalações novas e migradas
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sync_metadata_new` (
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
                database.execSQL(
                    "INSERT OR IGNORE INTO `sync_metadata_new` SELECT * FROM `sync_metadata`"
                )
                database.execSQL("DROP TABLE IF EXISTS `sync_metadata`")
                database.execSQL("ALTER TABLE `sync_metadata_new` RENAME TO `sync_metadata`")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}