package com.learneveryday.app

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

data class Lesson(
    val id: Int,
    val title: String,
    val content: String,
    var isCompleted: Boolean = false,
    val estimatedMinutes: Int = 10,
    val difficulty: String = "Beginner"
)

data class UserProgress(
    val topicId: String,
    val currentLessonIndex: Int = 0,
    val completedLessons: MutableSet<Int> = mutableSetOf(),
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
