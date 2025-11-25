package com.learneveryday.app.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Data
import com.google.gson.Gson
import com.learneveryday.app.data.local.AppDatabase
import com.learneveryday.app.data.repository.GenerationQueueRepositoryImpl
import com.learneveryday.app.data.repository.LessonRepositoryImpl
import com.learneveryday.app.data.repository.CurriculumRepositoryImpl
import com.learneveryday.app.data.repository.AIConfigRepositoryImpl
import com.learneveryday.app.data.service.AIProviderFactory
import com.learneveryday.app.data.service.AIProviderFactory.ProviderType
import com.learneveryday.app.domain.model.QueueStatus
import com.learneveryday.app.domain.model.Lesson
import com.learneveryday.app.domain.service.*
import com.learneveryday.app.work.GenerationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Background worker that processes the lesson generation queue in small token-safe batches.
 */
class GenerationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "GenerationWorker"
        const val KEY_CURRICULUM_ID = "curriculum_id"
        const val KEY_MAX_TOKENS = "max_tokens"
        const val KEY_CHUNK_SIZE = "chunk_size"
        const val DEFAULT_CHUNK_SIZE = 4
        const val DEFAULT_MAX_TOKENS = 6000
        
        // Max retries for this worker (WorkManager will handle backoff)
        private const val MAX_RUN_ATTEMPTS = 3
    }

    private val database = AppDatabase.getInstance(appContext)
    private val queueRepo = GenerationQueueRepositoryImpl(database.generationQueueDao())
    private val lessonRepo = LessonRepositoryImpl(database.lessonDao())
    private val curriculumRepo = CurriculumRepositoryImpl(database.curriculumDao(), database.lessonDao())
    private val aiConfigRepo = AIConfigRepositoryImpl(database.aiConfigDao())

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "GenerationWorker started, attempt: $runAttemptCount")
        
        val curriculumId = inputData.getString(KEY_CURRICULUM_ID)
        val maxTokens = inputData.getInt(KEY_MAX_TOKENS, DEFAULT_MAX_TOKENS)
        val chunkSize = inputData.getInt(KEY_CHUNK_SIZE, DEFAULT_CHUNK_SIZE)

        // Check retry limit
        if (runAttemptCount > MAX_RUN_ATTEMPTS) {
            Log.e(TAG, "Max retry attempts ($MAX_RUN_ATTEMPTS) exceeded")
            return@withContext Result.failure(
                Data.Builder()
                    .putString("error", "Generation failed after $MAX_RUN_ATTEMPTS attempts. Please check your network and API key.")
                    .build()
            )
        }

        val activeConfig = aiConfigRepo.getActiveConfigSync()
        if (activeConfig == null) {
            Log.e(TAG, "No active AI config found")
            return@withContext Result.failure(
                Data.Builder()
                    .putString("error", "No active AI config. Please configure your API key in settings.")
                    .build()
            )
        }

        val curriculum = curriculumId?.let { curriculumRepo.getCurriculumByIdSync(it) }
        if (curriculum == null) {
            Log.e(TAG, "Curriculum not found: $curriculumId")
            return@withContext Result.failure(
                Data.Builder()
                    .putString("error", "Curriculum not found")
                    .build()
            )
        }

        // Collect pending lessons (no content yet)
        val pendingLessons = lessonRepo.getLessonsByCurriculumSync(curriculum.id)
            .filter { it.content.isBlank() }
            .sortedBy { it.orderIndex }
        val firstPendingLessonId = pendingLessons.firstOrNull()?.id

        if (pendingLessons.isEmpty()) {
            Log.d(TAG, "No pending lessons to generate")
            return@withContext Result.success(
                Data.Builder()
                    .putString("status", "Nothing to generate")
                    .build()
            )
        }

        Log.d(TAG, "Found ${pendingLessons.size} pending lessons to generate")

        // Build list of titles to generate outlines/content for
        val titles = pendingLessons.map { it.title }

        val providerType = AIProviderFactory.getProviderFromName(activeConfig.provider)
        val aiProvider = AIProviderFactory.createProvider(providerType)
        val aiService = AIServiceImpl(aiProvider, Gson())

        // Decide adaptive chunk size based on remaining tokens estimate
        val adaptiveChunkSize = com.learneveryday.app.util.ChunkSizingUtil.calculateAdaptiveChunkSize(titles.size, maxTokens, chunkSize)
        Log.d(TAG, "Using adaptive chunk size: $adaptiveChunkSize")

        try {
            val outlineResult = aiService.generateChunkedLessons(
                curriculumTitle = curriculum.title,
                lessonTitles = titles,
                difficulty = curriculum.difficulty,
                provider = activeConfig.provider,
                apiKey = activeConfig.apiKey,
                modelName = activeConfig.modelName,
                chunkSize = adaptiveChunkSize
            )

            when (outlineResult) {
                is AIResult.Success -> {
                    Log.d(TAG, "Successfully generated ${outlineResult.data.size} lesson outlines")
                    
                    // Use fuzzy matching to associate outlines with lessons
                    val matchedCount = matchAndApplyOutlines(pendingLessons, outlineResult.data)
                    Log.d(TAG, "Matched and applied $matchedCount outlines")
                    
                    // Enqueue content generation for all pending lessons
                    pendingLessons.forEach { lesson ->
                        GenerationScheduler.enqueueLessonContent(
                            context = applicationContext,
                            lessonId = lesson.id,
                            curriculumId = curriculum.id,
                            expedite = lesson.id == firstPendingLessonId
                        )
                    }
                    
                    return@withContext Result.success(
                        Data.Builder()
                            .putInt("generated_outlines", outlineResult.data.size)
                            .putInt("matched_lessons", matchedCount)
                            .putInt("remaining_lessons", pendingLessons.size - matchedCount)
                            .build()
                    )
                }
                is AIResult.Error -> {
                    Log.e(TAG, "AI generation error: ${outlineResult.message}")
                    // Return retry for transient errors
                    return@withContext if (isRetryableError(outlineResult.message)) {
                        Log.d(TAG, "Error is retryable, requesting retry")
                        Result.retry()
                    } else {
                        Result.failure(
                            Data.Builder()
                                .putString("error", outlineResult.message)
                                .build()
                        )
                    }
                }
                is AIResult.Retry -> {
                    Log.d(TAG, "AI service requested retry")
                    return@withContext Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during generation", e)
            return@withContext if (isRetryableException(e)) {
                Result.retry()
            } else {
                Result.failure(
                    Data.Builder()
                        .putString("error", "Generation failed: ${e.message}")
                        .build()
                )
            }
        }
    }
    
    /**
     * Match generated outlines to lessons using fuzzy title matching.
     * This handles cases where AI returns slightly different titles.
     */
    private suspend fun matchAndApplyOutlines(
        lessons: List<Lesson>,
        outlines: List<LessonOutlineItem>
    ): Int {
        var matchedCount = 0
        
        // Create a map with normalized titles for fuzzy matching
        val outlineMap = outlines.associateBy { normalizeTitle(it.title) }
        val usedOutlines = mutableSetOf<String>()
        
        for (lesson in lessons) {
            val normalizedLessonTitle = normalizeTitle(lesson.title)
            
            // Try exact match first
            var outline = outlineMap[normalizedLessonTitle]
            
            // If no exact match, try fuzzy matching
            if (outline == null) {
                outline = outlines.firstOrNull { item ->
                    val normalizedOutlineTitle = normalizeTitle(item.title)
                    normalizedOutlineTitle !in usedOutlines && 
                    (normalizedLessonTitle.contains(normalizedOutlineTitle) ||
                     normalizedOutlineTitle.contains(normalizedLessonTitle) ||
                     calculateSimilarity(normalizedLessonTitle, normalizedOutlineTitle) > 0.7)
                }
            }
            
            if (outline != null) {
                val normalizedOutlineTitle = normalizeTitle(outline.title)
                usedOutlines.add(normalizedOutlineTitle)
                
                lessonRepo.applyGeneratedOutline(
                    lessonId = lesson.id,
                    description = outline.description,
                    estimatedMinutes = outline.estimatedMinutes,
                    keyPoints = outline.keyPoints
                )
                matchedCount++
                Log.d(TAG, "Matched lesson '${lesson.title}' with outline '${outline.title}'")
            } else {
                Log.w(TAG, "No outline match found for lesson: ${lesson.title}")
            }
        }
        
        return matchedCount
    }
    
    /**
     * Normalize title for comparison (lowercase, remove punctuation, trim)
     */
    private fun normalizeTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
    
    /**
     * Calculate string similarity using Levenshtein distance ratio
     */
    private fun calculateSimilarity(s1: String, s2: String): Double {
        val longer = if (s1.length > s2.length) s1 else s2
        val shorter = if (s1.length > s2.length) s2 else s1
        
        if (longer.isEmpty()) return 1.0
        
        val distance = levenshteinDistance(longer, shorter)
        return (longer.length - distance).toDouble() / longer.length
    }
    
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        
        return dp[s1.length][s2.length]
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
