package com.learneveryday.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "curriculums")
data class CurriculumEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val difficulty: String,
    val estimatedHours: Int,
    val provider: String,
    val modelUsed: String,
    val tags: String, // JSON array stored as string
    val totalLessons: Int,
    val completedLessons: Int = 0,
    val generationMode: String, // "QUICK_START", "FULL_GENERATION", "SMART_MODE"
    val generationStatus: String, // "GENERATING", "PARTIAL", "COMPLETE", "FAILED"
    val isOutlineOnly: Boolean = false,
    val isCompleted: Boolean = false,
    val createdAt: Long,
    val lastAccessedAt: Long,
    val lastGeneratedAt: Long? = null
)
