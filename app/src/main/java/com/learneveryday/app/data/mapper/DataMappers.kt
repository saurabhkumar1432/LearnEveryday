package com.learneveryday.app.data.mapper


import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.learneveryday.app.data.local.entity.*
import com.learneveryday.app.domain.model.*

private val gson = Gson()

// Helper functions for JSON serialization
private fun String?.toStringList(): List<String> {
    if (this.isNullOrBlank()) return emptyList()
    return try {
        val type = object : TypeToken<List<String>>() {}.type
        gson.fromJson(this, type) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

private fun List<String>?.toJsonString(): String {
    if (this == null || this.isEmpty()) return "[]"
    return gson.toJson(this)
}

private fun safeDifficulty(name: String): Difficulty {
    return try {
        Difficulty.valueOf(name)
    } catch (e: IllegalArgumentException) {
        Difficulty.BEGINNER
    }
}

private fun safeGenerationMode(name: String): GenerationMode {
    return try {
        GenerationMode.valueOf(name)
    } catch (e: IllegalArgumentException) {
        GenerationMode.FULL_GENERATION
    }
}

private fun safeGenerationStatus(name: String): GenerationStatus {
    return try {
        GenerationStatus.valueOf(name)
    } catch (e: IllegalArgumentException) {
        GenerationStatus.COMPLETE
    }
}

// Entity to Domain Model
fun CurriculumEntity.toDomain(): Curriculum {
    return Curriculum(
        id = id,
        title = title,
        description = description,
        difficulty = safeDifficulty(difficulty),
        estimatedHours = estimatedHours,
        provider = provider,
        modelUsed = modelUsed,
        tags = tags.toStringList(),
        totalLessons = totalLessons,
        completedLessons = completedLessons,
        generationMode = safeGenerationMode(generationMode),
        generationStatus = safeGenerationStatus(generationStatus),
        isOutlineOnly = isOutlineOnly,
        isCompleted = isCompleted,
        createdAt = createdAt,
        lastAccessedAt = lastAccessedAt,
        lastGeneratedAt = lastGeneratedAt,
        totalEstimatedMinutes = 0 // Will be 0 when not using WithTotalTime query
    )
}

// CurriculumWithTotalTime to Domain Model
fun CurriculumWithTotalTime.toDomain(): Curriculum {
    return Curriculum(
        id = curriculum.id,
        title = curriculum.title,
        description = curriculum.description,
        difficulty = safeDifficulty(curriculum.difficulty),
        estimatedHours = curriculum.estimatedHours,
        provider = curriculum.provider,
        modelUsed = curriculum.modelUsed,
        tags = curriculum.tags.toStringList(),
        totalLessons = curriculum.totalLessons,
        completedLessons = curriculum.completedLessons,
        generationMode = safeGenerationMode(curriculum.generationMode),
        generationStatus = safeGenerationStatus(curriculum.generationStatus),
        isOutlineOnly = curriculum.isOutlineOnly,
        isCompleted = curriculum.isCompleted,
        createdAt = curriculum.createdAt,
        lastAccessedAt = curriculum.lastAccessedAt,
        lastGeneratedAt = curriculum.lastGeneratedAt,
        totalEstimatedMinutes = totalEstimatedMinutes
    )
}

// Domain Model to Entity
fun Curriculum.toEntity(): CurriculumEntity {
    return CurriculumEntity(
        id = id,
        title = title,
        description = description,
        difficulty = difficulty.name,
        estimatedHours = estimatedHours,
        provider = provider,
        modelUsed = modelUsed,
        tags = tags.toJsonString(),
        totalLessons = totalLessons,
        completedLessons = completedLessons,
        generationMode = generationMode.name,
        generationStatus = generationStatus.name,
        isOutlineOnly = isOutlineOnly,
        isCompleted = isCompleted,
        createdAt = createdAt,
        lastAccessedAt = lastAccessedAt,
        lastGeneratedAt = lastGeneratedAt
    )
}

fun LessonEntity.toDomain(): Lesson {
    return Lesson(
        id = id,
        curriculumId = curriculumId,
        orderIndex = orderIndex,
        title = title,
        description = description,
        content = content,
        difficulty = safeDifficulty(difficulty),
        estimatedMinutes = estimatedMinutes,
        keyPoints = keyPoints.toStringList(),
        practiceExercise = practiceExercise,
        prerequisites = prerequisites.toStringList(),
        nextSteps = nextSteps.toStringList(),
        isGenerated = isGenerated,
        isCompleted = isCompleted,
        completedAt = completedAt,
        lastReadPosition = lastReadPosition,
        timeSpentMinutes = timeSpentMinutes
    )
}

fun Lesson.toEntity(): LessonEntity {
    return LessonEntity(
        id = id,
        curriculumId = curriculumId,
        orderIndex = orderIndex,
        title = title,
        description = description,
        content = content,
        difficulty = difficulty.name,
        estimatedMinutes = estimatedMinutes,
        keyPoints = keyPoints.toJsonString(),
        practiceExercise = practiceExercise,
        prerequisites = prerequisites.toJsonString(),
        nextSteps = nextSteps.toJsonString(),
        isGenerated = isGenerated,
        isCompleted = isCompleted,
        completedAt = completedAt,
        lastReadPosition = lastReadPosition,
        timeSpentMinutes = timeSpentMinutes
    )
}

fun ProgressEntity.toDomain(): Progress {
    return Progress(
        curriculumId = curriculumId,
        totalLessons = totalLessons,
        completedLessons = completedLessons,
        currentLessonId = currentLessonId,
        totalTimeSpentMinutes = totalTimeSpentMinutes,
        lastUpdated = lastUpdated,
        progressPercentage = progressPercentage
    )
}

fun Progress.toEntity(): ProgressEntity {
    return ProgressEntity(
        id = curriculumId, // Use curriculumId as the primary key
        curriculumId = curriculumId,
        totalLessons = totalLessons,
        completedLessons = completedLessons,
        currentLessonId = currentLessonId,
        totalTimeSpentMinutes = totalTimeSpentMinutes,
        lastUpdated = lastUpdated,
        progressPercentage = progressPercentage
    )
}

fun AIConfigEntity.toDomain(): AIConfig {
    return AIConfig(
        provider = provider,
        apiKey = apiKey,
        modelName = modelName,
        temperature = temperature,
        maxTokens = maxTokens,
        endpoint = endpoint,
        isActive = isActive,
        lastUsed = lastUsed,
        successfulGenerations = successfulGenerations,
        failedGenerations = failedGenerations
    )
}

fun AIConfig.toEntity(): AIConfigEntity {
    return AIConfigEntity(
        provider = provider,
        apiKey = apiKey,
        modelName = modelName,
        temperature = temperature,
        maxTokens = maxTokens,
        endpoint = endpoint,
        isActive = isActive,
        lastUsed = lastUsed,
        successfulGenerations = successfulGenerations,
        failedGenerations = failedGenerations
    )
}

fun GenerationQueueEntity.toDomain(): GenerationQueueItem {
    return GenerationQueueItem(
        id = id,
        curriculumId = curriculumId,
        lessonId = lessonId,
        lessonTitle = lessonTitle,
        lessonDescription = lessonDescription,
        keyTopics = keyTopics.toStringList(),
        priority = priority,
        status = QueueStatus.valueOf(status),
        attempts = attempts,
        maxAttempts = maxAttempts,
        lastAttemptAt = lastAttemptAt,
        errorMessage = errorMessage,
        createdAt = createdAt
    )
}

fun GenerationQueueItem.toEntity(): GenerationQueueEntity {
    return GenerationQueueEntity(
        id = id,
        curriculumId = curriculumId,
        lessonId = lessonId ?: "",
        lessonTitle = lessonTitle,
        lessonDescription = lessonDescription ?: "",
        keyTopics = keyTopics.toJsonString(),
        priority = priority,
        status = status.name,
        attempts = attempts,
        maxAttempts = maxAttempts,
        lastAttemptAt = lastAttemptAt,
        errorMessage = errorMessage,
        createdAt = createdAt
    )
}
