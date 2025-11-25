package com.learneveryday.app.data.repository

import com.learneveryday.app.data.local.dao.LessonDao
import com.learneveryday.app.data.mapper.toDomain
import com.learneveryday.app.data.mapper.toEntity
import com.learneveryday.app.domain.model.Lesson
import com.learneveryday.app.domain.repository.LessonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LessonRepositoryImpl(
    private val lessonDao: LessonDao
) : LessonRepository {
    
    override fun getLessonsByCurriculum(curriculumId: String): Flow<List<Lesson>> {
        return lessonDao.getLessonsByCurriculum(curriculumId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getLessonById(id: String): Flow<Lesson?> {
        return lessonDao.getLessonById(id).map { entity ->
            entity?.toDomain()
        }
    }
    
    override suspend fun getLessonByIdSync(id: String): Lesson? {
        return lessonDao.getLessonByIdSync(id)?.toDomain()
    }
    
    override fun getNextIncompleteLesson(curriculumId: String): Flow<Lesson?> {
        return lessonDao.getNextIncompleteLesson(curriculumId).map { entity ->
            entity?.toDomain()
        }
    }
    
    override fun getPendingLessons(curriculumId: String): Flow<List<Lesson>> {
        return lessonDao.getPendingLessons(curriculumId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getCompletedLessons(curriculumId: String): Flow<List<Lesson>> {
        return lessonDao.getCompletedLessons(curriculumId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getLessonCount(curriculumId: String): Flow<Int> {
        return lessonDao.getLessonCount(curriculumId)
    }
    
    override fun getCompletedLessonCount(curriculumId: String): Flow<Int> {
        return lessonDao.getCompletedLessonCount(curriculumId)
    }
    
    override fun getPendingLessonCount(curriculumId: String): Flow<Int> {
        return lessonDao.getPendingLessonCount(curriculumId)
    }
    
    override suspend fun getTotalEstimatedMinutes(curriculumId: String): Int {
        return lessonDao.getTotalEstimatedMinutes(curriculumId)
    }
    
    override fun getTotalEstimatedMinutesFlow(curriculumId: String): Flow<Int> {
        return lessonDao.getTotalEstimatedMinutesFlow(curriculumId)
    }
    
    override suspend fun insertLesson(lesson: Lesson): Long {
        return lessonDao.insertLesson(lesson.toEntity())
    }
    
    override suspend fun insertLessons(lessons: List<Lesson>) {
        lessonDao.insertLessons(lessons.map { it.toEntity() })
    }
    
    override suspend fun updateLesson(lesson: Lesson) {
        lessonDao.updateLesson(lesson.toEntity())
    }
    
    override suspend fun deleteLesson(lesson: Lesson) {
        lessonDao.deleteLesson(lesson.toEntity())
    }
    
    override suspend fun deleteLessonsByCurriculum(curriculumId: String) {
        lessonDao.deleteLessonsByCurriculum(curriculumId)
    }
    
    override suspend fun updateCompletionStatus(id: String, isCompleted: Boolean, completedAt: Long?) {
        lessonDao.updateCompletionStatus(id, isCompleted, completedAt)
    }
    
    override suspend fun updateReadPosition(id: String, position: Int) {
        lessonDao.updateReadPosition(id, position)
    }
    
    override suspend fun updateTimeSpent(id: String, minutes: Int) {
        lessonDao.updateTimeSpent(id, minutes)
    }
    
    override suspend fun updateLessonContent(id: String, content: String) {
        lessonDao.updateLessonContent(id, content)
    }
    
    /**
     * Update lesson metadata from generated content (keyPoints, practiceExercise, etc.)
     */
    suspend fun updateLessonMetadata(
        lessonId: String,
        keyPoints: List<String>,
        practiceExercise: String?,
        prerequisites: List<String>,
        nextSteps: List<String>
    ) {
        val gson = com.google.gson.Gson()
        lessonDao.updateLessonMetadata(
            id = lessonId,
            keyPoints = gson.toJson(keyPoints),
            practiceExercise = practiceExercise,
            prerequisites = gson.toJson(prerequisites),
            nextSteps = gson.toJson(nextSteps)
        )
    }

    // Custom helper for applying generated outline metadata to an existing pending lesson
    suspend fun applyGeneratedOutline(
        lessonId: String,
        description: String,
        estimatedMinutes: Int,
        keyPoints: List<String>
    ) {
        // Fetch existing lesson entity, update outline fields without full content yet
        val existing = lessonDao.getLessonByIdSync(lessonId) ?: return
        val keyPointsJson = com.google.gson.Gson().toJson(keyPoints)
        val updated = existing.copy(
            // Keep existing content (likely blank), mark as not fully generated yet
            description = description, // Save the lesson description from outline
            estimatedMinutes = estimatedMinutes,
            keyPoints = keyPointsJson,
            // isGenerated stays as is; if content blank treat as outline state
        )
        lessonDao.updateLesson(updated)
    }

    suspend fun getLessonsByCurriculumSync(curriculumId: String) : List<Lesson> {
        return lessonDao.getLessonsByCurriculumSync(curriculumId).map { it.toDomain() }
    }
}
