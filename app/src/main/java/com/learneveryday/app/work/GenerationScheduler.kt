package com.learneveryday.app.work

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object GenerationScheduler {

    private const val UNIQUE_NAME_PREFIX = "generation_"

    fun enqueueForCurriculum(
        context: Context,
        curriculumId: String,
        maxTokens: Int = GenerationWorker.DEFAULT_MAX_TOKENS,
        chunkSize: Int = GenerationWorker.DEFAULT_CHUNK_SIZE,
        expedite: Boolean = true
    ) {
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

        if (expedite) {
            requestBuilder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        }

        val request = requestBuilder.build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_NAME_PREFIX + curriculumId,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun enqueueLessonContent(
        context: Context,
        lessonId: String,
        maxTokens: Int = LessonContentWorker.DEFAULT_MAX_TOKENS,
        expedite: Boolean = false
    ) {
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
        if (expedite) builder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)

        WorkManager.getInstance(context).enqueue(builder.build())
    }
}
