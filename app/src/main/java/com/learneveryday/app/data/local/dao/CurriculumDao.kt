package com.learneveryday.app.data.local.dao

import androidx.room.*
import com.learneveryday.app.data.local.entity.CurriculumEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurriculumDao {
    
    @Query("SELECT * FROM curriculums ORDER BY lastAccessedAt DESC")
    fun getAllCurriculums(): Flow<List<CurriculumEntity>>
    
    @Query("SELECT * FROM curriculums WHERE id = :id")
    fun getCurriculumById(id: String): Flow<CurriculumEntity?>
    
    @Query("SELECT * FROM curriculums WHERE id = :id")
    suspend fun getCurriculumByIdSync(id: String): CurriculumEntity?
    
    @Query("SELECT * FROM curriculums WHERE isCompleted = 0 ORDER BY lastAccessedAt DESC")
    fun getInProgressCurriculums(): Flow<List<CurriculumEntity>>
    
    @Query("SELECT * FROM curriculums WHERE isCompleted = 1 ORDER BY lastAccessedAt DESC")
    fun getCompletedCurriculums(): Flow<List<CurriculumEntity>>
    
    @Query("SELECT * FROM curriculums WHERE generationStatus = :status ORDER BY lastGeneratedAt DESC")
    fun getCurriculumsByGenerationStatus(status: String): Flow<List<CurriculumEntity>>
    
    @Query("SELECT * FROM curriculums WHERE difficulty = :difficulty ORDER BY lastAccessedAt DESC")
    fun getCurriculumsByDifficulty(difficulty: String): Flow<List<CurriculumEntity>>
    
    @Query("UPDATE curriculums SET lastAccessedAt = :timestamp WHERE id = :id")
    suspend fun updateLastAccessed(id: String, timestamp: Long)
    
    @Query("UPDATE curriculums SET completedLessons = :completed, isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateProgress(id: String, completed: Int, isCompleted: Boolean)
    
    @Query("UPDATE curriculums SET generationStatus = :status, lastGeneratedAt = :timestamp WHERE id = :id")
    suspend fun updateGenerationStatus(id: String, status: String, timestamp: Long)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurriculum(curriculum: CurriculumEntity): Long
    
    @Update
    suspend fun updateCurriculum(curriculum: CurriculumEntity)
    
    @Delete
    suspend fun deleteCurriculum(curriculum: CurriculumEntity)
    
    @Query("DELETE FROM curriculums WHERE id = :id")
    suspend fun deleteCurriculumById(id: String)
    
    @Query("SELECT COUNT(*) FROM curriculums")
    fun getCurriculumCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM curriculums WHERE isCompleted = 0")
    fun getInProgressCount(): Flow<Int>
    
    @Query("SELECT * FROM curriculums WHERE isCompleted = 0 ORDER BY lastAccessedAt DESC LIMIT 1")
    fun getMostRecentCurriculumSync(): CurriculumEntity?
}
