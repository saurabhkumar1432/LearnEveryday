package com.learneveryday.app.data.local.dao

import androidx.room.*
import com.learneveryday.app.data.local.entity.GenerationQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GenerationQueueDao {
    
    @Query("SELECT * FROM generation_queue ORDER BY priority DESC, createdAt ASC")
    fun getAllQueueItems(): Flow<List<GenerationQueueEntity>>
    
    @Query("SELECT * FROM generation_queue WHERE id = :id")
    fun getQueueItemById(id: String): Flow<GenerationQueueEntity?>
    
    @Query("SELECT * FROM generation_queue WHERE curriculumId = :curriculumId ORDER BY priority DESC, createdAt ASC")
    fun getQueueItemsByCurriculum(curriculumId: String): Flow<List<GenerationQueueEntity>>
    
    @Query("SELECT * FROM generation_queue WHERE status = :status ORDER BY priority DESC, createdAt ASC")
    fun getQueueItemsByStatus(status: String): Flow<List<GenerationQueueEntity>>
    
    @Query("SELECT * FROM generation_queue WHERE status = 'PENDING' ORDER BY priority DESC, createdAt ASC LIMIT 1")
    suspend fun getNextPendingItem(): GenerationQueueEntity?
    
    @Query("SELECT * FROM generation_queue WHERE status = 'PENDING' AND curriculumId = :curriculumId ORDER BY priority DESC, createdAt ASC")
    fun getPendingItemsForCurriculum(curriculumId: String): Flow<List<GenerationQueueEntity>>
    
    @Query("UPDATE generation_queue SET status = :status, lastAttemptAt = :timestamp WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, timestamp: Long)
    
    @Query("UPDATE generation_queue SET attempts = attempts + 1, lastAttemptAt = :timestamp, status = :status WHERE id = :id")
    suspend fun incrementAttempt(id: String, status: String, timestamp: Long)
    
    @Query("UPDATE generation_queue SET status = 'FAILED', errorMessage = :error, lastAttemptAt = :timestamp WHERE id = :id")
    suspend fun markAsFailed(id: String, error: String, timestamp: Long)
    
    @Query("UPDATE generation_queue SET priority = :priority WHERE id = :id")
    suspend fun updatePriority(id: String, priority: Int)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItem(item: GenerationQueueEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItems(items: List<GenerationQueueEntity>)
    
    @Update
    suspend fun updateQueueItem(item: GenerationQueueEntity)
    
    @Delete
    suspend fun deleteQueueItem(item: GenerationQueueEntity)
    
    @Query("DELETE FROM generation_queue WHERE id = :id")
    suspend fun deleteQueueItemById(id: String)
    
    @Query("DELETE FROM generation_queue WHERE curriculumId = :curriculumId")
    suspend fun deleteQueueItemsByCurriculum(curriculumId: String)
    
    @Query("DELETE FROM generation_queue WHERE status = 'COMPLETED'")
    suspend fun deleteCompletedItems()
    
    @Query("SELECT COUNT(*) FROM generation_queue WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM generation_queue WHERE curriculumId = :curriculumId AND status = 'PENDING'")
    fun getPendingCountForCurriculum(curriculumId: String): Flow<Int>
}
