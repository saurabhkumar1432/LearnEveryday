package com.learneveryday.app.data.local.dao

import androidx.room.*
import com.learneveryday.app.data.local.entity.LessonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {
    
    @Query("SELECT * FROM lessons WHERE curriculumId = :curriculumId ORDER BY orderIndex ASC")
    fun getLessonsByCurriculum(curriculumId: String): Flow<List<LessonEntity>>
    
    @Query("SELECT * FROM lessons WHERE curriculumId = :curriculumId ORDER BY orderIndex ASC")
    suspend fun getLessonsByCurriculumSync(curriculumId: String): List<LessonEntity>
    
    @Query("SELECT * FROM lessons WHERE id = :id")
    fun getLessonById(id: String): Flow<LessonEntity?>
    
    @Query("SELECT * FROM lessons WHERE id = :id")
    suspend fun getLessonByIdSync(id: String): LessonEntity?
    
    @Query("SELECT * FROM lessons WHERE curriculumId = :curriculumId AND isCompleted = 0 ORDER BY orderIndex ASC LIMIT 1")
    fun getNextIncompleteLesson(curriculumId: String): Flow<LessonEntity?>
    
    @Query("SELECT * FROM lessons WHERE curriculumId = :curriculumId AND isCompleted = 0 ORDER BY orderIndex ASC LIMIT 1")
    fun getNextIncompleteLessonSync(curriculumId: String): LessonEntity?
    
    @Query("SELECT * FROM lessons WHERE curriculumId = :curriculumId AND isGenerated = 0 ORDER BY orderIndex ASC")
    fun getPendingLessons(curriculumId: String): Flow<List<LessonEntity>>
    
    @Query("SELECT * FROM lessons WHERE curriculumId = :curriculumId AND isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedLessons(curriculumId: String): Flow<List<LessonEntity>>
    
    @Query("UPDATE lessons SET isCompleted = :isCompleted, completedAt = :completedAt WHERE id = :id")
    suspend fun updateCompletionStatus(id: String, isCompleted: Boolean, completedAt: Long?)
    
    @Query("UPDATE lessons SET lastReadPosition = :position WHERE id = :id")
    suspend fun updateReadPosition(id: String, position: Int)
    
    @Query("UPDATE lessons SET timeSpentMinutes = :minutes WHERE id = :id")
    suspend fun updateTimeSpent(id: String, minutes: Int)
    
    @Query("UPDATE lessons SET isGenerated = 1, content = :content WHERE id = :id")
    suspend fun updateLessonContent(id: String, content: String)
    
    @Query("UPDATE lessons SET keyPoints = :keyPoints, practiceExercise = :practiceExercise, prerequisites = :prerequisites, nextSteps = :nextSteps WHERE id = :id")
    suspend fun updateLessonMetadata(id: String, keyPoints: String, practiceExercise: String?, prerequisites: String, nextSteps: String)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: LessonEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(lessons: List<LessonEntity>)
    
    @Update
    suspend fun updateLesson(lesson: LessonEntity)
    
    @Delete
    suspend fun deleteLesson(lesson: LessonEntity)
    
    @Query("DELETE FROM lessons WHERE curriculumId = :curriculumId")
    suspend fun deleteLessonsByCurriculum(curriculumId: String)
    
    @Query("SELECT COUNT(*) FROM lessons WHERE curriculumId = :curriculumId")
    fun getLessonCount(curriculumId: String): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM lessons WHERE curriculumId = :curriculumId AND isCompleted = 1")
    fun getCompletedLessonCount(curriculumId: String): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM lessons WHERE curriculumId = :curriculumId AND isGenerated = 0")
    fun getPendingLessonCount(curriculumId: String): Flow<Int>
    
    @Query("SELECT COALESCE(SUM(estimatedMinutes), 0) FROM lessons WHERE curriculumId = :curriculumId")
    suspend fun getTotalEstimatedMinutes(curriculumId: String): Int
    
    @Query("SELECT COALESCE(SUM(estimatedMinutes), 0) FROM lessons WHERE curriculumId = :curriculumId")
    fun getTotalEstimatedMinutesFlow(curriculumId: String): Flow<Int>
}
