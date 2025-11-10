package com.learneveryday.app.domain.model

data class Progress(
    val curriculumId: String,
    val totalLessons: Int,
    val completedLessons: Int,
    val currentLessonId: String?,
    val totalTimeSpentMinutes: Int,
    val lastUpdated: Long,
    val progressPercentage: Float
) {
    val isCompleted: Boolean
        get() = completedLessons >= totalLessons && totalLessons > 0
    
    val remainingLessons: Int
        get() = maxOf(0, totalLessons - completedLessons)
    
    val estimatedTimeRemaining: Int
        get() = remainingLessons * 15 // Assuming 15 min average per lesson
}
