package com.learneveryday.app.domain.service

/**
 * Interface for AI providers (Gemini, OpenAI, etc.)
 */
interface AIProvider {
    suspend fun sendRequest(
        prompt: String,
        apiKey: String,
        modelName: String,
        temperature: Float,
        maxTokens: Int
    ): String
}
