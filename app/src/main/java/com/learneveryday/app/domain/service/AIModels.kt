package com.learneveryday.app.domain.service

import com.learneveryday.app.domain.model.Difficulty
import com.learneveryday.app.domain.model.GenerationMode

/**
 * Result wrapper for AI operations
 */
sealed class AIResult<out T> {
    data class Success<T>(val data: T) : AIResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : AIResult<Nothing>()
    data class Retry(val attemptNumber: Int, val maxAttempts: Int) : AIResult<Nothing>()
}

/**
 * AI generation request configuration
 */
data class GenerationRequest(
    val topic: String,
    val description: String,
    val difficulty: Difficulty,
    val estimatedHours: Int,
    val mode: GenerationMode,
    val maxLessons: Int = 20,
    val provider: String,
    val apiKey: String,
    val modelName: String,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 8000
)

/**
 * Simplified request for UI usage
 */
data class CurriculumRequest(
    val topic: String,
    val difficulty: String,
    val numberOfLessons: Int
)

/**
 * Curriculum outline response from AI
 */
data class CurriculumOutline(
    val title: String,
    val description: String,
    val difficulty: String,
    val estimatedHours: Int,
    val lessons: List<LessonOutlineItem>,
    val tags: List<String>
)

/**
 * Individual lesson outline
 */
data class LessonOutlineItem(
    val title: String,
    val description: String,
    val estimatedMinutes: Int,
    val keyPoints: List<String>,
    val content: String? = null
)

/**
 * Lesson content generation request
 */
data class LessonGenerationRequest(
    val curriculumTitle: String,
    val lessonTitle: String,
    val lessonDescription: String,
    val difficulty: Difficulty,
    val keyPoints: List<String>,
    val previousLessonTitles: List<String> = emptyList(),
    val provider: String,
    val apiKey: String,
    val modelName: String,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 8000
)

/**
 * Generated lesson content
 */
data class LessonContent(
    val content: String, // Markdown formatted
    val keyPoints: List<String>,
    val practiceExercise: String?,
    val prerequisites: List<String>,
    val nextSteps: List<String>
)

/**
 * Request for generating topic suggestions
 */
data class TopicSuggestionsRequest(
    val count: Int = 8,
    val excludeTopics: List<String> = emptyList(),
    val provider: String,
    val apiKey: String,
    val modelName: String,
    val temperature: Float = 0.9f,
    val maxTokens: Int = 4000
)

/**
 * AI-generated topic suggestion
 */
data class TopicSuggestion(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val category: String,
    val tags: List<String>
)

/**
 * Response wrapper for topic suggestions
 */
data class TopicSuggestionsResponse(
    val topics: List<TopicSuggestion>
)
