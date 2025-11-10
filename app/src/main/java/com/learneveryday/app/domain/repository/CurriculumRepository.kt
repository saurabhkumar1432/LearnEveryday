package com.learneveryday.app.domain.repository

import com.learneveryday.app.domain.model.*
import kotlinx.coroutines.flow.Flow

interface CurriculumRepository {
    
    // Query operations
    fun getAllCurriculums(): Flow<List<Curriculum>>
    fun getCurriculumById(id: String): Flow<Curriculum?>
    fun getInProgressCurriculums(): Flow<List<Curriculum>>
    fun getCompletedCurriculums(): Flow<List<Curriculum>>
    fun getCurriculumsByStatus(status: GenerationStatus): Flow<List<Curriculum>>
    fun getCurriculumsByDifficulty(difficulty: Difficulty): Flow<List<Curriculum>>
    fun getCurriculumCount(): Flow<Int>
    
    // Mutation operations
    suspend fun insertCurriculum(curriculum: Curriculum): Long
    suspend fun updateCurriculum(curriculum: Curriculum)
    suspend fun deleteCurriculum(curriculum: Curriculum)
    suspend fun deleteCurriculumById(id: String)
    suspend fun updateLastAccessed(id: String)
    suspend fun updateProgress(id: String, completed: Int, isCompleted: Boolean)
    suspend fun updateGenerationStatus(id: String, status: GenerationStatus)
}
