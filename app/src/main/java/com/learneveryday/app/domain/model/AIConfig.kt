package com.learneveryday.app.domain.model

data class AIConfig(
    val provider: String,
    val apiKey: String,
    val modelName: String,
    val temperature: Float,
    val maxTokens: Int,
    val endpoint: String?,
    val isActive: Boolean,
    val lastUsed: Long?,
    val successfulGenerations: Int,
    val failedGenerations: Int
) {
    val successRate: Float
        get() {
            val total = successfulGenerations + failedGenerations
            return if (total > 0) (successfulGenerations.toFloat() / total) * 100 else 0f
        }
    
    val isConfigured: Boolean
        get() = apiKey.isNotBlank() && modelName.isNotBlank()
}
