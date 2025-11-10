package com.learneveryday.app.data.local.dao

import androidx.room.*
import com.learneveryday.app.data.local.entity.ProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    
    @Query("SELECT * FROM progress WHERE curriculumId = :curriculumId")
    fun getProgressByCurriculum(curriculumId: String): Flow<ProgressEntity?>
    
    @Query("SELECT * FROM progress WHERE curriculumId = :curriculumId")
    suspend fun getProgressByCurriculumSync(curriculumId: String): ProgressEntity?
    
    @Query("UPDATE progress SET completedLessons = :completed, progressPercentage = :percentage, lastUpdated = :timestamp WHERE curriculumId = :curriculumId")
    suspend fun updateProgress(curriculumId: String, completed: Int, percentage: Float, timestamp: Long)
    
    @Query("UPDATE progress SET currentLessonId = :lessonId, lastUpdated = :timestamp WHERE curriculumId = :curriculumId")
    suspend fun updateCurrentLesson(curriculumId: String, lessonId: String, timestamp: Long)
    
    @Query("UPDATE progress SET totalTimeSpentMinutes = :minutes, lastUpdated = :timestamp WHERE curriculumId = :curriculumId")
    suspend fun updateTimeSpent(curriculumId: String, minutes: Int, timestamp: Long)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: ProgressEntity)
    
    @Update
    suspend fun updateProgress(progress: ProgressEntity)
    
    @Delete
    suspend fun deleteProgress(progress: ProgressEntity)
    
    @Query("DELETE FROM progress WHERE curriculumId = :curriculumId")
    suspend fun deleteProgressByCurriculum(curriculumId: String)
}
