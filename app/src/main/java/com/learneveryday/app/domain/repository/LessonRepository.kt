package com.learneveryday.app.domain.repository

import com.learneveryday.app.domain.model.Lesson
import kotlinx.coroutines.flow.Flow

interface LessonRepository {
    
    // Query operations
    fun getLessonsByCurriculum(curriculumId: String): Flow<List<Lesson>>
    fun getLessonById(id: String): Flow<Lesson?>
    suspend fun getLessonByIdSync(id: String): Lesson?
    fun getNextIncompleteLesson(curriculumId: String): Flow<Lesson?>
    fun getPendingLessons(curriculumId: String): Flow<List<Lesson>>
    fun getCompletedLessons(curriculumId: String): Flow<List<Lesson>>
    fun getLessonCount(curriculumId: String): Flow<Int>
    fun getCompletedLessonCount(curriculumId: String): Flow<Int>
    fun getPendingLessonCount(curriculumId: String): Flow<Int>
    suspend fun getTotalEstimatedMinutes(curriculumId: String): Int
    fun getTotalEstimatedMinutesFlow(curriculumId: String): Flow<Int>
    
    // Mutation operations
    suspend fun insertLesson(lesson: Lesson): Long
    suspend fun insertLessons(lessons: List<Lesson>)
    suspend fun updateLesson(lesson: Lesson)
    suspend fun deleteLesson(lesson: Lesson)
    suspend fun deleteLessonsByCurriculum(curriculumId: String)
    suspend fun updateCompletionStatus(id: String, isCompleted: Boolean, completedAt: Long?)
    suspend fun updateReadPosition(id: String, position: Int)
    suspend fun updateTimeSpent(id: String, minutes: Int)
    suspend fun updateLessonContent(id: String, content: String)
}
