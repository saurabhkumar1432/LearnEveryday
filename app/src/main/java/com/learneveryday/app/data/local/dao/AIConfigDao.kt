package com.learneveryday.app.data.local.dao

import androidx.room.*
import com.learneveryday.app.data.local.entity.AIConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AIConfigDao {
    
    @Query("SELECT * FROM ai_configs")
    fun getAllConfigs(): Flow<List<AIConfigEntity>>
    
    @Query("SELECT * FROM ai_configs WHERE provider = :provider")
    fun getConfigByProvider(provider: String): Flow<AIConfigEntity?>
    
    @Query("SELECT * FROM ai_configs WHERE provider = :provider")
    suspend fun getConfigByProviderSync(provider: String): AIConfigEntity?
    
    @Query("SELECT * FROM ai_configs WHERE isActive = 1 LIMIT 1")
    fun getActiveConfig(): Flow<AIConfigEntity?>
    
    @Query("SELECT * FROM ai_configs WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveConfigSync(): AIConfigEntity?
    
    @Query("UPDATE ai_configs SET isActive = 0")
    suspend fun deactivateAllConfigs()
    
    @Query("UPDATE ai_configs SET isActive = 1 WHERE provider = :provider")
    suspend fun setActiveProvider(provider: String)
    
    @Query("UPDATE ai_configs SET lastUsed = :timestamp WHERE provider = :provider")
    suspend fun updateLastUsed(provider: String, timestamp: Long)
    
    @Query("UPDATE ai_configs SET successfulGenerations = successfulGenerations + 1, lastUsed = :timestamp WHERE provider = :provider")
    suspend fun incrementSuccessCount(provider: String, timestamp: Long)
    
    @Query("UPDATE ai_configs SET failedGenerations = failedGenerations + 1, lastUsed = :timestamp WHERE provider = :provider")
    suspend fun incrementFailureCount(provider: String, timestamp: Long)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: AIConfigEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfigs(configs: List<AIConfigEntity>)
    
    @Update
    suspend fun updateConfig(config: AIConfigEntity)
    
    @Delete
    suspend fun deleteConfig(config: AIConfigEntity)
    
    @Query("DELETE FROM ai_configs WHERE provider = :provider")
    suspend fun deleteConfigByProvider(provider: String)
}
