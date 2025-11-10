package com.learneveryday.app.domain.repository

import com.learneveryday.app.domain.model.Progress
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    
    // Query operations
    fun getProgressByCurriculum(curriculumId: String): Flow<Progress?>
    suspend fun getProgressByCurriculumSync(curriculumId: String): Progress?
    
    // Mutation operations
    suspend fun insertOrUpdateProgress(progress: Progress)
    suspend fun updateProgress(curriculumId: String, completed: Int, percentage: Float)
    suspend fun updateCurrentLesson(curriculumId: String, lessonId: String)
    suspend fun updateTimeSpent(curriculumId: String, minutes: Int)
    suspend fun deleteProgress(curriculumId: String)
}
