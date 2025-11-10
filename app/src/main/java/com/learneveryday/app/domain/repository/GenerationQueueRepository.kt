package com.learneveryday.app.domain.repository

import com.learneveryday.app.domain.model.GenerationQueueItem
import com.learneveryday.app.domain.model.QueueStatus
import kotlinx.coroutines.flow.Flow

interface GenerationQueueRepository {
    
    // Query operations
    fun getAllQueueItems(): Flow<List<GenerationQueueItem>>
    fun getQueueItemById(id: String): Flow<GenerationQueueItem?>
    fun getQueueItemsByCurriculum(curriculumId: String): Flow<List<GenerationQueueItem>>
    fun getQueueItemsByStatus(status: QueueStatus): Flow<List<GenerationQueueItem>>
    suspend fun getNextPendingItem(): GenerationQueueItem?
    fun getPendingItemsForCurriculum(curriculumId: String): Flow<List<GenerationQueueItem>>
    fun getPendingCount(): Flow<Int>
    fun getPendingCountForCurriculum(curriculumId: String): Flow<Int>
    
    // Mutation operations
    suspend fun insertQueueItem(item: GenerationQueueItem): Long
    suspend fun insertQueueItems(items: List<GenerationQueueItem>)
    suspend fun updateQueueItem(item: GenerationQueueItem)
    suspend fun deleteQueueItem(item: GenerationQueueItem)
    suspend fun deleteQueueItemById(id: String)
    suspend fun deleteQueueItemsByCurriculum(curriculumId: String)
    suspend fun deleteCompletedItems()
    suspend fun updateStatus(id: String, status: QueueStatus)
    suspend fun incrementAttempt(id: String, status: QueueStatus)
    suspend fun markAsFailed(id: String, error: String)
    suspend fun updatePriority(id: String, priority: Int)
}
