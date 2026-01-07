package com.adygyes.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.adygyes.app.data.local.dao.AttractionDao
import com.adygyes.app.data.local.entities.AttractionEntity
import com.adygyes.app.data.local.entities.Converters

/**
 * Main Room database for the Adygyes app
 */
@Database(
    entities = [AttractionEntity::class],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AdygyesDatabase : RoomDatabase() {
    
    abstract fun attractionDao(): AttractionDao
    
    companion object {
        const val DATABASE_NAME = "adygyes_database"
        
        /**
         * Migration from version 1 to 2
         * Adds Supabase integration fields:
         * - Extended attraction info (operatingSeason, duration, bestTimeToVisit)
         * - Reviews aggregate (reviewsCount, averageRating)
         * - Supabase metadata (isPublished, createdAt, updatedAt)
         * - Sync tracking (lastSyncedAt)
         * - Renames lastUpdated to lastSyncedAt
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add extended info fields
                database.execSQL("ALTER TABLE attractions ADD COLUMN operatingSeason TEXT")
                database.execSQL("ALTER TABLE attractions ADD COLUMN duration TEXT")
                database.execSQL("ALTER TABLE attractions ADD COLUMN bestTimeToVisit TEXT")
                
                // Add reviews aggregate fields
                database.execSQL("ALTER TABLE attractions ADD COLUMN reviewsCount INTEGER")
                database.execSQL("ALTER TABLE attractions ADD COLUMN averageRating REAL")
                
                // Add Supabase metadata fields
                database.execSQL("ALTER TABLE attractions ADD COLUMN isPublished INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE attractions ADD COLUMN createdAt TEXT")
                database.execSQL("ALTER TABLE attractions ADD COLUMN updatedAt TEXT")
                
                // Add sync tracking (renaming lastUpdated to lastSyncedAt)
                // SQLite doesn't support column rename, so we add new column
                database.execSQL("ALTER TABLE attractions ADD COLUMN lastSyncedAt INTEGER NOT NULL DEFAULT 0")
                
                // Copy data from lastUpdated to lastSyncedAt if it exists
                // Note: This is safe even if lastUpdated doesn't exist (will just use default)
                try {
                    database.execSQL("UPDATE attractions SET lastSyncedAt = lastUpdated WHERE lastUpdated IS NOT NULL")
                } catch (e: Exception) {
                    // lastUpdated column might not exist in some cases, ignore
                }
            }
        }
        
        /**
         * Migration from version 2 to 3
         * Removes obsolete 'difficulty' column if it exists
         * (SQLite doesn't support DROP COLUMN before version 3.35.0, so we recreate the table)
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Check if difficulty column exists (it shouldn't based on schema, but some DBs might have it)
                // Recreate the table without 'difficulty' column
                
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS attractions_new (
                        id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        category TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        address TEXT,
                        directions TEXT,
                        images TEXT NOT NULL,
                        rating REAL,
                        reviewsCount INTEGER,
                        averageRating REAL,
                        workingHours TEXT,
                        phoneNumber TEXT,
                        email TEXT,
                        website TEXT,
                        tags TEXT NOT NULL,
                        priceInfo TEXT,
                        amenities TEXT NOT NULL,
                        operatingSeason TEXT,
                        duration TEXT,
                        bestTimeToVisit TEXT,
                        isPublished INTEGER NOT NULL,
                        createdAt TEXT,
                        updatedAt TEXT,
                        isFavorite INTEGER NOT NULL,
                        lastSyncedAt INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                """.trimIndent())
                
                // Copy data from old table (excluding difficulty column if it exists)
                database.execSQL("""
                    INSERT INTO attractions_new 
                    SELECT id, name, description, category, latitude, longitude, 
                           address, directions, images, rating, reviewsCount, averageRating,
                           workingHours, phoneNumber, email, website, tags, priceInfo, amenities,
                           operatingSeason, duration, bestTimeToVisit, isPublished, createdAt, updatedAt,
                           isFavorite, lastSyncedAt
                    FROM attractions
                """.trimIndent())
                
                // Drop old table
                database.execSQL("DROP TABLE attractions")
                
                // Rename new table
                database.execSQL("ALTER TABLE attractions_new RENAME TO attractions")
            }
        }
        
        /**
         * Migration from version 3 to 4
         * Removes deprecated 'rating' column (replaced by averageRating computed from reviews)
         * Room requires table recreation to drop column on older SQLite versions
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Recreate table without 'rating' column
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS attractions_new (
                        id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        category TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        address TEXT,
                        directions TEXT,
                        images TEXT NOT NULL,
                        reviewsCount INTEGER,
                        averageRating REAL,
                        workingHours TEXT,
                        phoneNumber TEXT,
                        email TEXT,
                        website TEXT,
                        tags TEXT NOT NULL,
                        priceInfo TEXT,
                        amenities TEXT NOT NULL,
                        operatingSeason TEXT,
                        duration TEXT,
                        bestTimeToVisit TEXT,
                        isPublished INTEGER NOT NULL,
                        createdAt TEXT,
                        updatedAt TEXT,
                        isFavorite INTEGER NOT NULL,
                        lastSyncedAt INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                """.trimIndent())
                
                // Copy data from old table (excluding rating column)
                database.execSQL("""
                    INSERT INTO attractions_new 
                    SELECT id, name, description, category, latitude, longitude, 
                           address, directions, images, reviewsCount, averageRating,
                           workingHours, phoneNumber, email, website, tags, priceInfo, amenities,
                           operatingSeason, duration, bestTimeToVisit, isPublished, createdAt, updatedAt,
                           isFavorite, lastSyncedAt
                    FROM attractions
                """.trimIndent())
                
                // Drop old table
                database.execSQL("DROP TABLE attractions")
                
                // Rename new table
                database.execSQL("ALTER TABLE attractions_new RENAME TO attractions")
            }
        }
        
        /**
         * Get all migrations array
         */
        fun getMigrations(): Array<Migration> {
            return arrayOf(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4
            )
        }
    }
}
