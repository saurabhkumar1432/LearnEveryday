package com.learneveryday.app.domain.repository

import com.learneveryday.app.domain.model.AIConfig
import kotlinx.coroutines.flow.Flow

interface AIConfigRepository {
    
    // Query operations
    fun getAllConfigs(): Flow<List<AIConfig>>
    fun getConfigByProvider(provider: String): Flow<AIConfig?>
    suspend fun getConfigByProviderSync(provider: String): AIConfig?
    fun getActiveConfig(): Flow<AIConfig?>
    suspend fun getActiveConfigSync(): AIConfig?
    
    // Mutation operations
    suspend fun insertOrUpdateConfig(config: AIConfig)
    suspend fun insertConfigs(configs: List<AIConfig>)
    suspend fun setActiveProvider(provider: String)
    suspend fun updateLastUsed(provider: String)
    suspend fun incrementSuccessCount(provider: String)
    suspend fun incrementFailureCount(provider: String)
    suspend fun deleteConfig(provider: String)
}
