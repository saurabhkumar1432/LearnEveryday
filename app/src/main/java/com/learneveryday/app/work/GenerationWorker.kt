package com.learneveryday.app.work

import android.content.Context
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
import com.learneveryday.app.domain.service.*
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
        const val KEY_CURRICULUM_ID = "curriculum_id"
        const val KEY_MAX_TOKENS = "max_tokens"
        const val KEY_CHUNK_SIZE = "chunk_size"
        const val DEFAULT_CHUNK_SIZE = 4
        const val DEFAULT_MAX_TOKENS = 6000
    }

    private val database = AppDatabase.getInstance(appContext)
    private val queueRepo = GenerationQueueRepositoryImpl(database.generationQueueDao())
    private val lessonRepo = LessonRepositoryImpl(database.lessonDao())
    private val curriculumRepo = CurriculumRepositoryImpl(database.curriculumDao(), database.lessonDao())
    private val aiConfigRepo = AIConfigRepositoryImpl(database.aiConfigDao())

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val curriculumId = inputData.getString(KEY_CURRICULUM_ID)
        val maxTokens = inputData.getInt(KEY_MAX_TOKENS, DEFAULT_MAX_TOKENS)
        val chunkSize = inputData.getInt(KEY_CHUNK_SIZE, DEFAULT_CHUNK_SIZE)

        val activeConfig = aiConfigRepo.getActiveConfigSync()
            ?: return@withContext Result.failure(Data.Builder().putString("error", "No active AI config").build())

        val curriculum = curriculumId?.let { curriculumRepo.getCurriculumByIdSync(it) }
            ?: return@withContext Result.failure(Data.Builder().putString("error", "Curriculum not found").build())

        // Collect pending lessons (no content yet)
        val pendingLessons = lessonRepo.getLessonsByCurriculumSync(curriculum.id)
            .filter { it.content.isBlank() }
            .sortedBy { it.orderIndex }

        if (pendingLessons.isEmpty()) {
            return@withContext Result.success(Data.Builder().putString("status", "Nothing to generate").build())
        }

        // Build list of titles to generate outlines/content for
        val titles = pendingLessons.map { it.title }

        val providerType = AIProviderFactory.getProviderFromName(activeConfig.provider)
        val aiProvider = AIProviderFactory.createProvider(providerType)
        val aiService = AIServiceImpl(aiProvider, Gson())

        // Decide adaptive chunk size based on remaining tokens estimate
        val adaptiveChunkSize = com.learneveryday.app.util.ChunkSizingUtil.calculateAdaptiveChunkSize(titles.size, maxTokens, chunkSize)

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
                // Merge generated outline data into lessons
                val outlineMap = outlineResult.data.associateBy { it.title }
                pendingLessons.forEach { lesson ->
                    val outline = outlineMap[lesson.title]
                    if (outline != null) {
                        lessonRepo.applyGeneratedOutline(
                            lessonId = lesson.id,
                            description = outline.description,
                            estimatedMinutes = outline.estimatedMinutes,
                            keyPoints = outline.keyPoints
                        )
                    }
                }
                return@withContext Result.success(Data.Builder()
                    .putInt("generated_outlines", outlineResult.data.size)
                    .putInt("remaining_lessons", pendingLessons.size - outlineResult.data.size)
                    .build())
            }
            is AIResult.Error -> return@withContext Result.failure(Data.Builder().putString("error", outlineResult.message).build())
            is AIResult.Retry -> return@withContext Result.retry()
        }
    }
}
