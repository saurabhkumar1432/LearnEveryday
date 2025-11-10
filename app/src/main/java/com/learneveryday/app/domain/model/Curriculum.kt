package com.learneveryday.app.domain.model

data class Curriculum(
    val id: String,
    val title: String,
    val description: String,
    val difficulty: Difficulty,
    val estimatedHours: Int,
    val provider: String,
    val modelUsed: String,
    val tags: List<String>,
    val totalLessons: Int,
    val completedLessons: Int,
    val generationMode: GenerationMode,
    val generationStatus: GenerationStatus,
    val isOutlineOnly: Boolean,
    val isCompleted: Boolean,
    val createdAt: Long,
    val lastAccessedAt: Long,
    val lastGeneratedAt: Long?
) {
    val progressPercentage: Float
        get() = if (totalLessons > 0) (completedLessons.toFloat() / totalLessons) * 100 else 0f
    
    val isInProgress: Boolean
        get() = completedLessons > 0 && !isCompleted
}

enum class Difficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    EXPERT
}

enum class GenerationMode {
    QUICK_START,      // Generate outline only, lessons on-demand
    FULL_GENERATION,  // Generate all lessons upfront
    SMART_MODE        // Generate outline + first few lessons, rest on-demand
}

enum class GenerationStatus {
    GENERATING,
    PARTIAL,
    COMPLETE,
    FAILED
}
