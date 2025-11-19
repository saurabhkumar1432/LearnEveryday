package com.learneveryday.app.data.service

import com.google.gson.Gson
import com.learneveryday.app.domain.service.AIProvider

/**
 * Factory for creating AI providers
 */
object AIProviderFactory {
    
    private val gson = Gson()
    
    enum class ProviderType {
        GEMINI,
        OPENAI,
        ANTHROPIC,
        OPENROUTER,
        CUSTOM
    }
    
    fun createProvider(providerType: ProviderType, customEndpoint: String? = null): AIProvider {
        return when (providerType) {
            ProviderType.GEMINI -> GeminiAIProvider(gson)
            ProviderType.OPENAI -> OpenAIProvider(gson)
            ProviderType.ANTHROPIC -> AnthropicProvider(gson)
            ProviderType.OPENROUTER -> OpenRouterProvider(gson)
            ProviderType.CUSTOM -> CustomProvider(gson, customEndpoint ?: "")
        }
    }
    
    fun getProviderFromName(name: String): ProviderType {
        return when (name.uppercase()) {
            "GEMINI" -> ProviderType.GEMINI
            "OPENAI" -> ProviderType.OPENAI
            "ANTHROPIC" -> ProviderType.ANTHROPIC
            "OPENROUTER" -> ProviderType.OPENROUTER
            "CUSTOM" -> ProviderType.CUSTOM
            else -> ProviderType.GEMINI // Default fallback
        }
    }
}

/**
 * Custom provider for user-defined endpoints
 */
class CustomProvider(private val gson: Gson, private val endpoint: String) : AIProvider {
    override suspend fun sendRequest(
        prompt: String,
        apiKey: String,
        modelName: String,
        temperature: Float,
        maxTokens: Int
    ): String {
        // TODO: Implement custom endpoint support
        throw NotImplementedError("Custom provider not yet implemented. Use Gemini or OpenAI.")
    }
}
