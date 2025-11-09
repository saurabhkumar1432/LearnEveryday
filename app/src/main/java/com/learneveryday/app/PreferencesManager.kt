package com.learneveryday.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("LearnEverydayPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun saveUserProgress(progress: UserProgress) {
        prefs.edit().putString("progress_${progress.topicId}", gson.toJson(progress)).apply()
    }
    
    fun getUserProgress(topicId: String): UserProgress? {
        val json = prefs.getString("progress_$topicId", null)
        return json?.let { gson.fromJson(it, UserProgress::class.java) }
    }
    
    fun getCurrentTopicId(): String? {
        return prefs.getString("current_topic_id", null)
    }
    
    fun setCurrentTopicId(topicId: String) {
        prefs.edit().putString("current_topic_id", topicId).apply()
    }
    
    fun isNotificationsEnabled(): Boolean {
        return prefs.getBoolean("notifications_enabled", true)
    }
    
    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
    }
    
    // AI API Configuration
    fun getAIProvider(): AIProvider {
        val providerName = prefs.getString("ai_provider", "NONE") ?: "NONE"
        return try {
            AIProvider.valueOf(providerName)
        } catch (e: IllegalArgumentException) {
            AIProvider.NONE
        }
    }
    
    fun setAIProvider(provider: AIProvider) {
        prefs.edit().putString("ai_provider", provider.name).apply()
    }
    
    fun getAPIKey(): String? {
        return prefs.getString("api_key", null)
    }
    
    fun setAPIKey(key: String) {
        prefs.edit().putString("api_key", key).apply()
    }
    
    fun getCustomAPIEndpoint(): String? {
        return prefs.getString("custom_api_endpoint", null)
    }
    
    fun setCustomAPIEndpoint(endpoint: String) {
        prefs.edit().putString("custom_api_endpoint", endpoint).apply()
    }
    
    fun isAIEnabled(): Boolean {
        return getAIProvider() != AIProvider.NONE && !getAPIKey().isNullOrEmpty()
    }
    
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}

enum class AIProvider(val displayName: String, val baseUrl: String?) {
    NONE("Use Built-in Curriculum", null),
    GEMINI("Google Gemini", "https://generativelanguage.googleapis.com/v1/models/gemini-pro:generateContent"),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1/chat/completions"),
    CUSTOM("Custom API", null)
}
