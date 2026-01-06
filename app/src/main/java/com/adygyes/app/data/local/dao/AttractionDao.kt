package com.adygyes.app.data.local.dao

import androidx.room.*
import com.adygyes.app.data.local.entities.AttractionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for attraction-related database operations
 */
@Dao
interface AttractionDao {
    
    @Query("SELECT * FROM attractions")
    fun getAllAttractions(): Flow<List<AttractionEntity>>
    
    @Query("SELECT * FROM attractions WHERE id = :attractionId")
    suspend fun getAttractionById(attractionId: String): AttractionEntity?
    
    @Query("SELECT * FROM attractions WHERE category = :category")
    fun getAttractionsByCategory(category: String): Flow<List<AttractionEntity>>
    
    @Query("SELECT * FROM attractions WHERE isFavorite = 1")
    fun getFavoriteAttractions(): Flow<List<AttractionEntity>>
    
    /**
     * Get list of favorite attraction IDs (for preserving during sync)
     */
    @Query("SELECT id FROM attractions WHERE isFavorite = 1")
    suspend fun getFavoriteIds(): List<String>
    
    @Query("SELECT * FROM attractions WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchAttractions(query: String): Flow<List<AttractionEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttraction(attraction: AttractionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttractions(attractions: List<AttractionEntity>)
    
    @Update
    suspend fun updateAttraction(attraction: AttractionEntity)
    
    @Query("UPDATE attractions SET isFavorite = :isFavorite WHERE id = :attractionId")
    suspend fun updateFavoriteStatus(attractionId: String, isFavorite: Boolean)
    
    @Delete
    suspend fun deleteAttraction(attraction: AttractionEntity)
    
    /**
     * Delete attraction by ID (used by sync for tombstones)
     */
    @Query("DELETE FROM attractions WHERE id = :attractionId")
    suspend fun deleteAttractionById(attractionId: String)
    
    @Query("DELETE FROM attractions")
    suspend fun deleteAllAttractions()
    
    @Query("DELETE FROM attractions")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM attractions")
    suspend fun getAttractionsCount(): Int
    
    /**
     * Get attractions updated after a specific timestamp
     * Used for checking local data freshness
     */
    @Query("SELECT * FROM attractions WHERE lastSyncedAt > :timestamp")
    suspend fun getAttractionsSyncedAfter(timestamp: Long): List<AttractionEntity>
}
