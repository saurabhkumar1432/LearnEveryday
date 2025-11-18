package com.learneveryday.app

import com.learneveryday.app.domain.model.Lesson

data class LearningTopic(
    val id: String,
    val title: String,
    val description: String,
    val difficulty: String = "Beginner to Advanced",
    val estimatedHours: Int = 0,
    val lessons: List<Lesson> = emptyList(),
    val isAIGenerated: Boolean = false,
    val generatedAt: Long = 0,
    val tags: List<String> = emptyList()
)

data class UserProgress(
    val topicId: String,
    val currentLessonIndex: Int = 0,
    val completedLessons: MutableSet<String> = mutableSetOf(),
    val startedAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val totalTimeSpent: Long = 0
) {
    fun getCompletionPercentage(totalLessons: Int): Int {
        return if (totalLessons > 0) {
            (completedLessons.size * 100) / totalLessons
        } else 0
    }
}

data class AIConfig(
    val provider: AIProvider,
    val apiKey: String,
    val modelName: String,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2000,
    val customEndpoint: String? = null
)

data class CurriculumRequest(
    val topic: String,
    val difficulty: String = "Beginner to Advanced",
    val numberOfLessons: Int = 20,
    val focusAreas: List<String> = emptyList()
)

data class CurriculumResponse(
    val success: Boolean,
    val topic: LearningTopic? = null,
    val error: String? = null
)
