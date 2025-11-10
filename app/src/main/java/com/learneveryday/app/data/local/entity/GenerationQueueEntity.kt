package com.learneveryday.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "generation_queue",
    foreignKeys = [
        ForeignKey(
            entity = CurriculumEntity::class,
            parentColumns = ["id"],
            childColumns = ["curriculumId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("curriculumId"), Index("priority"), Index("status")]
)
data class GenerationQueueEntity(
    @PrimaryKey
    val id: String,
    val curriculumId: String,
    val lessonId: String,
    val lessonTitle: String,
    val lessonDescription: String,
    val keyTopics: String, // JSON array
    val priority: Int, // Higher for next lessons to be read
    val status: String, // "PENDING", "IN_PROGRESS", "COMPLETED", "FAILED"
    val attempts: Int = 0,
    val maxAttempts: Int = 3,
    val lastAttemptAt: Long? = null,
    val errorMessage: String? = null,
    val createdAt: Long
)
