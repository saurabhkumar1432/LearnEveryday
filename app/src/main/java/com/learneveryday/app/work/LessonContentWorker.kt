package com.learneveryday.app.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.learneveryday.app.data.local.AppDatabase
import com.learneveryday.app.data.repository.AIConfigRepositoryImpl
import com.learneveryday.app.data.repository.CurriculumRepositoryImpl
import com.learneveryday.app.data.repository.LessonRepositoryImpl
import com.learneveryday.app.data.service.AIProviderFactory
import com.learneveryday.app.domain.service.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Generates full markdown content for a single lesson.
 */
class LessonContentWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "LessonContentWorker"
        const val KEY_LESSON_ID = "lesson_id"
        const val KEY_MAX_TOKENS = "max_tokens"
        const val DEFAULT_MAX_TOKENS = 7000
        
        // Max retries for this worker
        private const val MAX_RUN_ATTEMPTS = 3
    }

    private val database = AppDatabase.getInstance(appContext)
    private val lessonRepo = LessonRepositoryImpl(database.lessonDao())
    private val curriculumRepo = CurriculumRepositoryImpl(database.curriculumDao(), database.lessonDao())
    private val aiConfigRepo = AIConfigRepositoryImpl(database.aiConfigDao())

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val lessonId = inputData.getString(KEY_LESSON_ID)
        val maxTokens = inputData.getInt(KEY_MAX_TOKENS, DEFAULT_MAX_TOKENS)
        
        Log.d(TAG, "LessonContentWorker started for lesson: $lessonId, attempt: $runAttemptCount")
        
        if (lessonId.isNullOrBlank()) {
            Log.e(TAG, "Missing lessonId")
            return@withContext Result.failure(
                Data.Builder()
                    .putString("error", "Missing lessonId")
                    .build()
            )
        }
        
        // Check retry limit
        if (runAttemptCount > MAX_RUN_ATTEMPTS) {
            Log.e(TAG, "Max retry attempts ($MAX_RUN_ATTEMPTS) exceeded for lesson: $lessonId")
            return@withContext Result.failure(
                Data.Builder()
                    .putString("error", "Content generation failed after $MAX_RUN_ATTEMPTS attempts")
                    .putString("lessonId", lessonId)
                    .build()
            )
        }

        val activeConfig = aiConfigRepo.getActiveConfigSync()
        if (activeConfig == null) {
            Log.e(TAG, "No active AI config")
            return@withContext Result.failure(
                Data.Builder()
                    .putString("error", "No active AI config. Please configure your API key in settings.")
                    .putString("lessonId", lessonId)
                    .build()
            )
        }

        val lesson = lessonRepo.getLessonByIdSync(lessonId)
        if (lesson == null) {
            Log.e(TAG, "Lesson not found: $lessonId")
            return@withContext Result.failure(
                Data.Builder()
                    .putString("error", "Lesson not found")
                    .putString("lessonId", lessonId)
                    .build()
            )
        }
        
        val curriculum = curriculumRepo.getCurriculumByIdSync(lesson.curriculumId)
        if (curriculum == null) {
            Log.e(TAG, "Curriculum not found for lesson: $lessonId")
            return@withContext Result.failure(
                Data.Builder()
                    .putString("error", "Curriculum not found")
                    .putString("lessonId", lessonId)
                    .build()
            )
        }

        // If content already exists, nothing to do
        if (lesson.content.isNotBlank()) {
            Log.d(TAG, "Lesson already has content: $lessonId")
            return@withContext Result.success(
                Data.Builder()
                    .putString("status", "Already has content")
                    .putString("lessonId", lessonId)
                    .build()
            )
        }

        Log.d(TAG, "Generating content for lesson: ${lesson.title}")

        // Build previous lesson titles context
        val previousTitles = lessonRepo.getLessonsByCurriculumSync(lesson.curriculumId)
            .filter { it.orderIndex < lesson.orderIndex }
            .sortedBy { it.orderIndex }
            .map { it.title }

        val providerType = AIProviderFactory.getProviderFromName(activeConfig.provider)
        val aiProvider = AIProviderFactory.createProvider(providerType)
        val aiService = AIServiceImpl(aiProvider, Gson())

        val request = LessonGenerationRequest(
            curriculumTitle = curriculum.title,
            lessonTitle = lesson.title,
            lessonDescription = lesson.description.ifBlank { lesson.title }, // Use stored description
            difficulty = lesson.difficulty,
            keyPoints = lesson.keyPoints,
            previousLessonTitles = previousTitles,
            provider = activeConfig.provider,
            apiKey = activeConfig.apiKey,
            modelName = activeConfig.modelName,
            temperature = activeConfig.temperature,
            maxTokens = maxTokens
        )

        try {
            when (val result = aiService.generateLessonContent(request)) {
                is AIResult.Success -> {
                    Log.d(TAG, "Successfully generated content for lesson: $lessonId, length: ${result.data.content.length}")
                    
                    // Save content and mark generated
                    lessonRepo.updateLessonContent(lesson.id, result.data.content)
                    
                    // Also update additional fields if available
                    if (result.data.keyPoints.isNotEmpty() || 
                        result.data.practiceExercise != null ||
                        result.data.prerequisites.isNotEmpty() ||
                        result.data.nextSteps.isNotEmpty()) {
                        lessonRepo.updateLessonMetadata(
                            lessonId = lesson.id,
                            keyPoints = result.data.keyPoints.ifEmpty { lesson.keyPoints },
                            practiceExercise = result.data.practiceExercise,
                            prerequisites = result.data.prerequisites,
                            nextSteps = result.data.nextSteps
                        )
                    }
                    
                    return@withContext Result.success(
                        Data.Builder()
                            .putString("lessonId", lesson.id)
                            .putInt("contentLength", result.data.content.length)
                            .putString("status", "Content generated successfully")
                            .build()
                    )
                }
                is AIResult.Retry -> {
                    Log.d(TAG, "AI service requested retry for lesson: $lessonId")
                    return@withContext Result.retry()
                }
                is AIResult.Error -> {
                    Log.e(TAG, "AI generation error for lesson $lessonId: ${result.message}")
                    
                    // Return retry for transient errors
                    return@withContext if (isRetryableError(result.message)) {
                        Log.d(TAG, "Error is retryable, requesting retry")
                        Result.retry()
                    } else {
                        Result.failure(
                            Data.Builder()
                                .putString("error", result.message)
                                .putString("lessonId", lessonId)
                                .build()
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during content generation for lesson: $lessonId", e)
            return@withContext if (isRetryableException(e)) {
                Result.retry()
            } else {
                Result.failure(
                    Data.Builder()
                        .putString("error", "Generation failed: ${e.message}")
                        .putString("lessonId", lessonId)
                        .build()
                )
            }
        }
    }
    
    /**
     * Check if error message indicates a retryable error
     */
    private fun isRetryableError(message: String): Boolean {
        val retryablePatterns = listOf(
            "network", "timeout", "connection", "socket",
            "temporarily", "rate limit", "quota", "overloaded",
            "503", "502", "500", "429"
        )
        val lowerMessage = message.lowercase()
        return retryablePatterns.any { lowerMessage.contains(it) }
    }
    
    /**
     * Check if exception is retryable
     */
    private fun isRetryableException(e: Exception): Boolean {
        return e is java.net.SocketTimeoutException ||
               e is java.net.UnknownHostException ||
               e is java.io.IOException
    }
}
