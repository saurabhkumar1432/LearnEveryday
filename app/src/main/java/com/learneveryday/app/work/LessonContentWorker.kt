package com.learneveryday.app.work

import android.content.Context
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
        const val KEY_LESSON_ID = "lesson_id"
        const val KEY_MAX_TOKENS = "max_tokens"
        const val DEFAULT_MAX_TOKENS = 7000
    }

    private val database = AppDatabase.getInstance(appContext)
    private val lessonRepo = LessonRepositoryImpl(database.lessonDao())
    private val curriculumRepo = CurriculumRepositoryImpl(database.curriculumDao(), database.lessonDao())
    private val aiConfigRepo = AIConfigRepositoryImpl(database.aiConfigDao())

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val lessonId = inputData.getString(KEY_LESSON_ID)
            ?: return@withContext Result.failure(Data.Builder().putString("error", "Missing lessonId").build())
        val maxTokens = inputData.getInt(KEY_MAX_TOKENS, DEFAULT_MAX_TOKENS)

        val activeConfig = aiConfigRepo.getActiveConfigSync()
            ?: return@withContext Result.failure(Data.Builder().putString("error", "No active AI config").build())

        val lesson = lessonRepo.getLessonByIdSync(lessonId)
            ?: return@withContext Result.failure(Data.Builder().putString("error", "Lesson not found").build())
        val curriculum = curriculumRepo.getCurriculumByIdSync(lesson.curriculumId)
            ?: return@withContext Result.failure(Data.Builder().putString("error", "Curriculum not found").build())

        // If content already exists, nothing to do
        if (lesson.content.isNotBlank()) {
            return@withContext Result.success(Data.Builder().putString("status", "Already has content").build())
        }

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

        when (val result = aiService.generateLessonContent(request)) {
            is AIResult.Success -> {
                // Save content and mark generated
                lessonRepo.updateLessonContent(lesson.id, result.data.content)
                // Optionally update key points/practice/prereqs/next steps later
                return@withContext Result.success(
                    Data.Builder()
                        .putString("lessonId", lesson.id)
                        .putInt("contentLength", result.data.content.length)
                        .build()
                )
            }
            is AIResult.Retry -> return@withContext Result.retry()
            is AIResult.Error -> return@withContext Result.failure(Data.Builder().putString("error", result.message).build())
        }
    }
}
