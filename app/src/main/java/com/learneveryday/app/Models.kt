package com.learneveryday.app

data class LearningTopic(
    val id: String,
    val title: String,
    val description: String,
    val lessons: List<Lesson>
)

data class Lesson(
    val id: Int,
    val title: String,
    val content: String,
    var isCompleted: Boolean = false
)

data class UserProgress(
    val topicId: String,
    val currentLessonIndex: Int = 0,
    val completedLessons: MutableSet<Int> = mutableSetOf(),
    val startedAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis()
) {
    fun getCompletionPercentage(totalLessons: Int): Int {
        return if (totalLessons > 0) {
            (completedLessons.size * 100) / totalLessons
        } else 0
    }
}
