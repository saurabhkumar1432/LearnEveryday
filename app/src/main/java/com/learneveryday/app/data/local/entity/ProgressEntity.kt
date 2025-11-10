package com.learneveryday.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "progress",
    foreignKeys = [
        ForeignKey(
            entity = CurriculumEntity::class,
            parentColumns = ["id"],
            childColumns = ["curriculumId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("curriculumId")]
)
data class ProgressEntity(
    @PrimaryKey
    val id: String,
    val curriculumId: String,
    val totalLessons: Int,
    val completedLessons: Int,
    val currentLessonId: String?,
    val totalTimeSpentMinutes: Int,
    val lastUpdated: Long,
    val progressPercentage: Float // Calculated: (completedLessons / totalLessons) * 100
)
