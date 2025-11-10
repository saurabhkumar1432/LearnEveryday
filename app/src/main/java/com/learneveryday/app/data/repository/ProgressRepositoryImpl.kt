package com.learneveryday.app.data.repository

import com.learneveryday.app.data.local.dao.ProgressDao
import com.learneveryday.app.data.mapper.toDomain
import com.learneveryday.app.data.mapper.toEntity
import com.learneveryday.app.domain.model.Progress
import com.learneveryday.app.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProgressRepositoryImpl(
    private val progressDao: ProgressDao
) : ProgressRepository {
    
    override fun getProgressByCurriculum(curriculumId: String): Flow<Progress?> {
        return progressDao.getProgressByCurriculum(curriculumId).map { entity ->
            entity?.toDomain()
        }
    }
    
    override suspend fun getProgressByCurriculumSync(curriculumId: String): Progress? {
        return progressDao.getProgressByCurriculumSync(curriculumId)?.toDomain()
    }
    
    override suspend fun insertOrUpdateProgress(progress: Progress) {
        progressDao.insertProgress(progress.toEntity())
    }
    
    override suspend fun updateProgress(curriculumId: String, completed: Int, percentage: Float) {
        progressDao.updateProgress(curriculumId, completed, percentage, System.currentTimeMillis())
    }
    
    override suspend fun updateCurrentLesson(curriculumId: String, lessonId: String) {
        progressDao.updateCurrentLesson(curriculumId, lessonId, System.currentTimeMillis())
    }
    
    override suspend fun updateTimeSpent(curriculumId: String, minutes: Int) {
        progressDao.updateTimeSpent(curriculumId, minutes, System.currentTimeMillis())
    }
    
    override suspend fun deleteProgress(curriculumId: String) {
        progressDao.deleteProgressByCurriculum(curriculumId)
    }
}
