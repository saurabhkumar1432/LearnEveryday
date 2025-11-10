package com.learneveryday.app.data.repository

import com.learneveryday.app.data.local.dao.CurriculumDao
import com.learneveryday.app.data.mapper.toDomain
import com.learneveryday.app.data.mapper.toEntity
import com.learneveryday.app.domain.model.Curriculum
import com.learneveryday.app.domain.model.Difficulty
import com.learneveryday.app.domain.model.GenerationStatus
import com.learneveryday.app.domain.repository.CurriculumRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CurriculumRepositoryImpl(
    private val curriculumDao: CurriculumDao
) : CurriculumRepository {
    
    override fun getAllCurriculums(): Flow<List<Curriculum>> {
        return curriculumDao.getAllCurriculums().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getCurriculumById(id: String): Flow<Curriculum?> {
        return curriculumDao.getCurriculumById(id).map { entity ->
            entity?.toDomain()
        }
    }
    
    override fun getInProgressCurriculums(): Flow<List<Curriculum>> {
        return curriculumDao.getInProgressCurriculums().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getCompletedCurriculums(): Flow<List<Curriculum>> {
        return curriculumDao.getCompletedCurriculums().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getCurriculumsByStatus(status: GenerationStatus): Flow<List<Curriculum>> {
        return curriculumDao.getCurriculumsByGenerationStatus(status.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getCurriculumsByDifficulty(difficulty: Difficulty): Flow<List<Curriculum>> {
        return curriculumDao.getCurriculumsByDifficulty(difficulty.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getCurriculumCount(): Flow<Int> {
        return curriculumDao.getCurriculumCount()
    }
    
    override suspend fun insertCurriculum(curriculum: Curriculum): Long {
        return curriculumDao.insertCurriculum(curriculum.toEntity())
    }
    
    override suspend fun updateCurriculum(curriculum: Curriculum) {
        curriculumDao.updateCurriculum(curriculum.toEntity())
    }
    
    override suspend fun deleteCurriculum(curriculum: Curriculum) {
        curriculumDao.deleteCurriculum(curriculum.toEntity())
    }
    
    override suspend fun deleteCurriculumById(id: String) {
        curriculumDao.deleteCurriculumById(id)
    }
    
    override suspend fun updateLastAccessed(id: String) {
        curriculumDao.updateLastAccessed(id, System.currentTimeMillis())
    }
    
    override suspend fun updateProgress(id: String, completed: Int, isCompleted: Boolean) {
        curriculumDao.updateProgress(id, completed, isCompleted)
    }
    
    override suspend fun updateGenerationStatus(id: String, status: GenerationStatus) {
        curriculumDao.updateGenerationStatus(id, status.name, System.currentTimeMillis())
    }

    // Sync helpers for background workers
    suspend fun getCurriculumByIdSync(id: String) = curriculumDao.getCurriculumByIdSync(id)?.toDomain()
}
