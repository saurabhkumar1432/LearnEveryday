package com.learneveryday.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lessons",
    foreignKeys = [
        ForeignKey(
            entity = CurriculumEntity::class,
            parentColumns = ["id"],
            childColumns = ["curriculumId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("curriculumId"), Index("orderIndex")]
)
data class LessonEntity(
    @PrimaryKey
    val id: String,
    val curriculumId: String,
    val orderIndex: Int,
    val title: String,
    val content: String, // Markdown formatted content
    val difficulty: String,
    val estimatedMinutes: Int,
    val keyPoints: String, // JSON array stored as string
    val practiceExercise: String? = null,
    val prerequisites: String? = null, // JSON array
    val nextSteps: String? = null, // JSON array
    val isGenerated: Boolean = true, // False for outline-only lessons
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val lastReadPosition: Int = 0, // For resuming reading
    val timeSpentMinutes: Int = 0
)
