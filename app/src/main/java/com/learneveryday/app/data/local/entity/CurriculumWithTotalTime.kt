package com.learneveryday.app.data.local.entity

import androidx.room.Embedded

/**
 * Data class representing a Curriculum with computed total estimated time from lessons.
 * Used for efficient queries that include aggregated lesson data.
 */
data class CurriculumWithTotalTime(
    @Embedded
    val curriculum: CurriculumEntity,
    val totalEstimatedMinutes: Int
)
