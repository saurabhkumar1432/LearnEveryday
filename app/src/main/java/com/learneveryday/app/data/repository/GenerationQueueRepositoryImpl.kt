package com.learneveryday.app.data.repository

import com.learneveryday.app.data.local.dao.GenerationQueueDao
import com.learneveryday.app.data.mapper.toDomain
import com.learneveryday.app.data.mapper.toEntity
import com.learneveryday.app.domain.model.GenerationQueueItem
import com.learneveryday.app.domain.model.QueueStatus
import com.learneveryday.app.domain.repository.GenerationQueueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GenerationQueueRepositoryImpl(
    private val queueDao: GenerationQueueDao
) : GenerationQueueRepository {
    
    override fun getAllQueueItems(): Flow<List<GenerationQueueItem>> {
        return queueDao.getAllQueueItems().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getQueueItemById(id: String): Flow<GenerationQueueItem?> {
        return queueDao.getQueueItemById(id).map { entity ->
            entity?.toDomain()
        }
    }
    
    override fun getQueueItemsByCurriculum(curriculumId: String): Flow<List<GenerationQueueItem>> {
        return queueDao.getQueueItemsByCurriculum(curriculumId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getQueueItemsByStatus(status: QueueStatus): Flow<List<GenerationQueueItem>> {
        return queueDao.getQueueItemsByStatus(status.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getNextPendingItem(): GenerationQueueItem? {
        return queueDao.getNextPendingItem()?.toDomain()
    }
    
    override fun getPendingItemsForCurriculum(curriculumId: String): Flow<List<GenerationQueueItem>> {
        return queueDao.getPendingItemsForCurriculum(curriculumId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getPendingCount(): Flow<Int> {
        return queueDao.getPendingCount()
    }
    
    override fun getPendingCountForCurriculum(curriculumId: String): Flow<Int> {
        return queueDao.getPendingCountForCurriculum(curriculumId)
    }
    
    override suspend fun insertQueueItem(item: GenerationQueueItem): Long {
        return queueDao.insertQueueItem(item.toEntity())
    }
    
    override suspend fun insertQueueItems(items: List<GenerationQueueItem>) {
        queueDao.insertQueueItems(items.map { it.toEntity() })
    }
    
    override suspend fun updateQueueItem(item: GenerationQueueItem) {
        queueDao.updateQueueItem(item.toEntity())
    }
    
    override suspend fun deleteQueueItem(item: GenerationQueueItem) {
        queueDao.deleteQueueItem(item.toEntity())
    }
    
    override suspend fun deleteQueueItemById(id: String) {
        queueDao.deleteQueueItemById(id)
    }
    
    override suspend fun deleteQueueItemsByCurriculum(curriculumId: String) {
        queueDao.deleteQueueItemsByCurriculum(curriculumId)
    }
    
    override suspend fun deleteCompletedItems() {
        queueDao.deleteCompletedItems()
    }
    
    override suspend fun updateStatus(id: String, status: QueueStatus) {
        queueDao.updateStatus(id, status.name, System.currentTimeMillis())
    }
    
    override suspend fun incrementAttempt(id: String, status: QueueStatus) {
        queueDao.incrementAttempt(id, status.name, System.currentTimeMillis())
    }
    
    override suspend fun markAsFailed(id: String, error: String) {
        queueDao.markAsFailed(id, error, System.currentTimeMillis())
    }
    
    override suspend fun updatePriority(id: String, priority: Int) {
        queueDao.updatePriority(id, priority)
    }
}
