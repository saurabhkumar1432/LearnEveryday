package com.learneveryday.app

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.learneveryday.app.data.local.AppDatabase
import com.learneveryday.app.data.repository.AIConfigRepositoryImpl
import com.learneveryday.app.domain.model.AIConfig as DomainAIConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LearnEverydayApp : Application() {
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        
        // Apply saved theme preference
        val prefsManager = PreferencesManager(this)
        val savedMode = prefsManager.getDarkModePreference()
        
        if (savedMode != -1) {
            AppCompatDelegate.setDefaultNightMode(savedMode)
        }
        
        // Sync AI config from SharedPreferences to Room database
        syncAIConfigToDatabase(prefsManager)
    }
    
    /**
     * Migrate AI configuration from SharedPreferences to Room database.
     * This ensures background workers can access the config.
     */
    private fun syncAIConfigToDatabase(prefsManager: PreferencesManager) {
        applicationScope.launch {
            try {
                val database = AppDatabase.getInstance(applicationContext)
                val aiConfigRepo = AIConfigRepositoryImpl(database.aiConfigDao())
                
                // Check if there's already an active config in the database
                val existingConfig = aiConfigRepo.getActiveConfigSync()
                
                if (existingConfig == null) {
                    // No active config in database, try to migrate from SharedPreferences
                    val prefsConfig = prefsManager.getAIConfig()
                    
                    if (prefsConfig != null && prefsConfig.apiKey.isNotBlank()) {
                        Log.d("LearnEverydayApp", "Migrating AI config from SharedPreferences to database")
                        
                        val domainConfig = DomainAIConfig(
                            provider = prefsConfig.provider.name,
                            apiKey = prefsConfig.apiKey,
                            modelName = prefsConfig.modelName,
                            temperature = prefsConfig.temperature,
                            maxTokens = prefsConfig.maxTokens,
                            endpoint = prefsConfig.customEndpoint,
                            isActive = true,
                            lastUsed = System.currentTimeMillis(),
                            successfulGenerations = 0,
                            failedGenerations = 0
                        )
                        
                        aiConfigRepo.insertOrUpdateConfig(domainConfig)
                        aiConfigRepo.setActiveProvider(prefsConfig.provider.name)
                        
                        Log.d("LearnEverydayApp", "AI config migrated successfully: ${prefsConfig.provider.name}")
                    } else {
                        Log.d("LearnEverydayApp", "No AI config found in SharedPreferences to migrate")
                    }
                } else {
                    Log.d("LearnEverydayApp", "Active AI config already exists in database: ${existingConfig.provider}")
                }
            } catch (e: Exception) {
                Log.e("LearnEverydayApp", "Failed to sync AI config to database", e)
            }
        }
    }
}
