package com.learneveryday.app.domain.model

data class Lesson(
    val id: String,
    val curriculumId: String,
    val orderIndex: Int,
    val title: String,
    val description: String, // Lesson description/overview from outline
    val content: String,
    val difficulty: Difficulty,
    val estimatedMinutes: Int,
    val keyPoints: List<String>,
    val practiceExercise: String?,
    val prerequisites: List<String>,
    val nextSteps: List<String>,
    val isGenerated: Boolean,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val lastReadPosition: Int,
    val timeSpentMinutes: Int
) {
    val hasContent: Boolean
        get() = content.isNotBlank()
    
    val completionPercentage: Float
        get() = if (content.isNotBlank()) (lastReadPosition.toFloat() / content.length) * 100 else 0f
    
    val isStarted: Boolean
        get() = lastReadPosition > 0 || timeSpentMinutes > 0
}
