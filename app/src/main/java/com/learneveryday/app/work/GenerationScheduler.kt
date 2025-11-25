package com.learneveryday.app.work

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

object GenerationScheduler {

    private const val TAG = "GenerationScheduler"
    private const val UNIQUE_NAME_PREFIX = "generation_"
    private const val LESSON_CONTENT_PREFIX = "lesson_content_"

    /**
     * Clear any failed/cancelled work for a curriculum before re-enqueueing.
     * This ensures retry buttons actually restart the work.
     */
    private fun clearFailedWork(context: Context, tag: String) {
        try {
            val workManager = WorkManager.getInstance(context)
            // Prune finished work to clear failed state
            workManager.pruneWork()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to prune work: ${e.message}")
        }
    }

    fun enqueueForCurriculum(
        context: Context,
        curriculumId: String,
        maxTokens: Int = GenerationWorker.DEFAULT_MAX_TOKENS,
        chunkSize: Int = GenerationWorker.DEFAULT_CHUNK_SIZE,
        expedite: Boolean = true
    ) {
        Log.d(TAG, "Enqueueing generation for curriculum: $curriculumId")
        
        // Clear any stale failed work first
        clearFailedWork(context, UNIQUE_NAME_PREFIX + curriculumId)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val input = workDataOf(
            GenerationWorker.KEY_CURRICULUM_ID to curriculumId,
            GenerationWorker.KEY_MAX_TOKENS to maxTokens,
            GenerationWorker.KEY_CHUNK_SIZE to chunkSize
        )

        val requestBuilder = OneTimeWorkRequestBuilder<GenerationWorker>()
            .setConstraints(constraints)
            .setInputData(input)
            .addTag(UNIQUE_NAME_PREFIX + curriculumId)
            // Add retry policy with exponential backoff
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )

        if (expedite) {
            requestBuilder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        }

        val request = requestBuilder.build()

        // Use REPLACE to ensure new work starts even if previous failed
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_NAME_PREFIX + curriculumId,
            ExistingWorkPolicy.REPLACE,
            request
        )
        
        Log.d(TAG, "Generation work enqueued with ID: ${request.id}")
    }

    fun enqueueLessonContent(
        context: Context,
        lessonId: String,
        curriculumId: String? = null,
        maxTokens: Int = LessonContentWorker.DEFAULT_MAX_TOKENS,
        expedite: Boolean = false
    ) {
        Log.d(TAG, "Enqueueing lesson content generation for: $lessonId")
        
        // Clear any stale failed work for this lesson
        clearFailedWork(context, LESSON_CONTENT_PREFIX + lessonId)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val input = workDataOf(
            LessonContentWorker.KEY_LESSON_ID to lessonId,
            LessonContentWorker.KEY_MAX_TOKENS to maxTokens
        )

        val builder = OneTimeWorkRequestBuilder<LessonContentWorker>()
            .setConstraints(constraints)
            .setInputData(input)
            // Add retry policy with exponential backoff
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
        
        // Add curriculum tag if provided for progress observation
        if (curriculumId != null) {
            builder.addTag(UNIQUE_NAME_PREFIX + curriculumId)
        }
        // Add lesson-specific tag for individual tracking
        builder.addTag(LESSON_CONTENT_PREFIX + lessonId)
        
        if (expedite) builder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)

        val request = builder.build()

        // Use REPLACE instead of KEEP - this ensures retry actually restarts work
        WorkManager.getInstance(context).enqueueUniqueWork(
            LESSON_CONTENT_PREFIX + lessonId,
            ExistingWorkPolicy.REPLACE,
            request
        )
        
        Log.d(TAG, "Lesson content work enqueued with ID: ${request.id}")
    }
    
    /**
     * Cancel all generation work for a curriculum
     */
    fun cancelForCurriculum(context: Context, curriculumId: String) {
        Log.d(TAG, "Cancelling all work for curriculum: $curriculumId")
        WorkManager.getInstance(context).cancelAllWorkByTag(UNIQUE_NAME_PREFIX + curriculumId)
    }
    
    /**
     * Cancel content generation for a specific lesson
     */
    fun cancelLessonContent(context: Context, lessonId: String) {
        Log.d(TAG, "Cancelling work for lesson: $lessonId")
        WorkManager.getInstance(context).cancelUniqueWork(LESSON_CONTENT_PREFIX + lessonId)
    }
}
