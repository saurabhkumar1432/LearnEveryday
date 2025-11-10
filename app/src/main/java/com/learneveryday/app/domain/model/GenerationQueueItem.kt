package com.learneveryday.app.domain.model

data class GenerationQueueItem(
    val id: String,
    val curriculumId: String,
    val lessonId: String?,
    val lessonTitle: String,
    val lessonDescription: String?,
    val keyTopics: List<String>,
    val priority: Int,
    val status: QueueStatus,
    val attempts: Int,
    val maxAttempts: Int,
    val lastAttemptAt: Long?,
    val errorMessage: String?,
    val createdAt: Long
) {
    val canRetry: Boolean
        get() = attempts < maxAttempts && status == QueueStatus.FAILED
    
    val isPending: Boolean
        get() = status == QueueStatus.PENDING
    
    val isProcessing: Boolean
        get() = status == QueueStatus.IN_PROGRESS
}

enum class QueueStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}
