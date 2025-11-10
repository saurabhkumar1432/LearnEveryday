package com.learneveryday.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_configs")
data class AIConfigEntity(
    @PrimaryKey
    val provider: String, // "GEMINI", "OPENROUTER", "OPENAI", "ANTHROPIC", "CUSTOM"
    val apiKey: String,
    val modelName: String,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 8000,
    val endpoint: String? = null, // For CUSTOM provider
    val isActive: Boolean = false, // Currently selected provider
    val lastUsed: Long? = null,
    val successfulGenerations: Int = 0,
    val failedGenerations: Int = 0
)
