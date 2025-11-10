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
        val providerName = prefs.getString("ai_provider", "CUSTOM") ?: "CUSTOM"
        return try {
            AIProvider.valueOf(providerName)
        } catch (e: IllegalArgumentException) {
            AIProvider.CUSTOM
        }
    }
    
    fun setAIProvider(provider: AIProvider) {
        prefs.edit().putString("ai_provider", provider.name).apply()
    }
    
    // Per-provider API Key storage
    fun getAPIKey(provider: AIProvider? = null): String? {
        val targetProvider = provider ?: getAIProvider()
        return prefs.getString("api_key_${targetProvider.name}", null)
    }
    
    fun setAPIKey(key: String, provider: AIProvider? = null) {
        val targetProvider = provider ?: getAIProvider()
        prefs.edit().putString("api_key_${targetProvider.name}", key).apply()
    }
    
    // Per-provider Model Name storage
    fun getModelName(provider: AIProvider? = null): String {
        val targetProvider = provider ?: getAIProvider()
        return prefs.getString("model_name_${targetProvider.name}", "") ?: ""
    }
    
    fun setModelName(model: String, provider: AIProvider? = null) {
        val targetProvider = provider ?: getAIProvider()
        prefs.edit().putString("model_name_${targetProvider.name}", model).apply()
    }
    
    fun getCustomAPIEndpoint(): String? {
        return prefs.getString("custom_api_endpoint", null)
    }
    
    fun setCustomAPIEndpoint(endpoint: String) {
        prefs.edit().putString("custom_api_endpoint", endpoint).apply()
    }
    
    fun getTemperature(): Float {
        return prefs.getFloat("temperature", 0.7f)
    }
    
    fun setTemperature(temp: Float) {
        prefs.edit().putFloat("temperature", temp).apply()
    }
    
    fun getMaxTokens(): Int {
        return prefs.getInt("max_tokens", 8000)
    }
    
    fun setMaxTokens(tokens: Int) {
        prefs.edit().putInt("max_tokens", tokens).apply()
    }
    
    fun getAIConfig(): AIConfig? {
        val provider = getAIProvider()
        val apiKey = getAPIKey() ?: return null
        
        return AIConfig(
            provider = provider,
            apiKey = apiKey,
            modelName = getModelName(),
            temperature = getTemperature(),
            maxTokens = getMaxTokens(),
            customEndpoint = if (provider == AIProvider.CUSTOM) getCustomAPIEndpoint() else null
        )
    }
    
    fun saveGeneratedTopic(topic: LearningTopic) {
        val json = gson.toJson(topic)
        prefs.edit().putString("topic_${topic.id}", json).apply()
    }
    
    fun getGeneratedTopic(topicId: String): LearningTopic? {
        val json = prefs.getString("topic_$topicId", null)
        return json?.let { gson.fromJson(it, LearningTopic::class.java) }
    }
    
    fun getAllGeneratedTopics(): List<LearningTopic> {
        val topics = mutableListOf<LearningTopic>()
        prefs.all.forEach { (key, value) ->
            if (key.startsWith("topic_") && value is String) {
                try {
                    val topic = gson.fromJson(value, LearningTopic::class.java)
                    topics.add(topic)
                } catch (e: Exception) {
                    // Skip invalid topics
                }
            }
        }
        return topics.sortedByDescending { it.generatedAt }
    }
    
    fun deleteTopic(topicId: String) {
        prefs.edit().remove("topic_$topicId").apply()
        prefs.edit().remove("progress_$topicId").apply()
    }
    
    fun isAIEnabled(): Boolean {
        return !getAPIKey().isNullOrEmpty()
    }
    
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}

enum class AIProvider(
    val displayName: String, 
    val baseUrl: String?,
    val defaultModels: List<String>
) {
    GEMINI(
        "Google Gemini", 
        "https://generativelanguage.googleapis.com/v1beta/models",
        listOf("gemini-1.5-pro", "gemini-1.5-flash", "gemini-pro")
    ),
    OPENROUTER(
        "OpenRouter", 
        "https://openrouter.ai/api/v1/chat/completions",
        listOf(
            "anthropic/claude-3.5-sonnet",
            "openai/gpt-4-turbo",
            "google/gemini-pro-1.5",
            "meta-llama/llama-3.1-70b-instruct"
        )
    ),
    OPENAI(
        "OpenAI",
        "https://api.openai.com/v1/chat/completions",
        listOf("gpt-4-turbo", "gpt-4", "gpt-3.5-turbo")
    ),
    ANTHROPIC(
        "Anthropic Claude",
        "https://api.anthropic.com/v1/messages",
        listOf("claude-3-5-sonnet-20241022", "claude-3-opus-20240229", "claude-3-sonnet-20240229")
    ),
    CUSTOM("Custom API", null, emptyList())
}
