package com.learneveryday.app.data.repository

import com.learneveryday.app.data.local.dao.AIConfigDao
import com.learneveryday.app.data.mapper.toDomain
import com.learneveryday.app.data.mapper.toEntity
import com.learneveryday.app.domain.model.AIConfig
import com.learneveryday.app.domain.repository.AIConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AIConfigRepositoryImpl(
    private val aiConfigDao: AIConfigDao
) : AIConfigRepository {
    
    override fun getAllConfigs(): Flow<List<AIConfig>> {
        return aiConfigDao.getAllConfigs().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getConfigByProvider(provider: String): Flow<AIConfig?> {
        return aiConfigDao.getConfigByProvider(provider).map { entity ->
            entity?.toDomain()
        }
    }
    
    override suspend fun getConfigByProviderSync(provider: String): AIConfig? {
        return aiConfigDao.getConfigByProviderSync(provider)?.toDomain()
    }
    
    override fun getActiveConfig(): Flow<AIConfig?> {
        return aiConfigDao.getActiveConfig().map { entity ->
            entity?.toDomain()
        }
    }
    
    override suspend fun getActiveConfigSync(): AIConfig? {
        return aiConfigDao.getActiveConfigSync()?.toDomain()
    }
    
    override suspend fun insertOrUpdateConfig(config: AIConfig) {
        aiConfigDao.insertConfig(config.toEntity())
    }
    
    override suspend fun insertConfigs(configs: List<AIConfig>) {
        aiConfigDao.insertConfigs(configs.map { it.toEntity() })
    }
    
    override suspend fun setActiveProvider(provider: String) {
        aiConfigDao.deactivateAllConfigs()
        aiConfigDao.setActiveProvider(provider)
    }
    
    override suspend fun updateLastUsed(provider: String) {
        aiConfigDao.updateLastUsed(provider, System.currentTimeMillis())
    }
    
    override suspend fun incrementSuccessCount(provider: String) {
        aiConfigDao.incrementSuccessCount(provider, System.currentTimeMillis())
    }
    
    override suspend fun incrementFailureCount(provider: String) {
        aiConfigDao.incrementFailureCount(provider, System.currentTimeMillis())
    }
    
    override suspend fun deleteConfig(provider: String) {
        aiConfigDao.deleteConfigByProvider(provider)
    }
}
