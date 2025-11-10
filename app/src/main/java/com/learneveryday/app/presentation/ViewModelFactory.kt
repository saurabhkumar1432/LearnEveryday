package com.learneveryday.app.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.learneveryday.app.data.local.AppDatabase
import com.learneveryday.app.data.repository.*
import com.learneveryday.app.domain.repository.*
import com.learneveryday.app.presentation.detail.CurriculumDetailViewModel
import com.learneveryday.app.presentation.generate.GenerateViewModel
import com.learneveryday.app.presentation.home.HomeViewModel
import com.learneveryday.app.presentation.reader.LessonViewModel

class ViewModelFactory(
    private val context: Context,
    private val curriculumId: String? = null,
    private val lessonId: String? = null
) : ViewModelProvider.Factory {
    
    private val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }
    
    private val curriculumRepository: CurriculumRepository by lazy {
        CurriculumRepositoryImpl(database.curriculumDao())
    }
    
    private val lessonRepository: LessonRepository by lazy {
        LessonRepositoryImpl(database.lessonDao())
    }
    
    private val progressRepository: ProgressRepository by lazy {
        ProgressRepositoryImpl(database.progressDao())
    }
    
    private val aiConfigRepository: AIConfigRepository by lazy {
        AIConfigRepositoryImpl(database.aiConfigDao())
    }
    
    private val queueRepository: GenerationQueueRepository by lazy {
        GenerationQueueRepositoryImpl(database.generationQueueDao())
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(curriculumRepository) as T
            }
            modelClass.isAssignableFrom(GenerateViewModel::class.java) -> {
                GenerateViewModel(
                    curriculumRepository,
                    lessonRepository,
                    progressRepository
                ) as T
            }
            modelClass.isAssignableFrom(CurriculumDetailViewModel::class.java) -> {
                require(curriculumId != null) { "curriculumId is required for CurriculumDetailViewModel" }
                CurriculumDetailViewModel(
                    curriculumId,
                    curriculumRepository,
                    lessonRepository,
                    progressRepository
                ) as T
            }
            modelClass.isAssignableFrom(LessonViewModel::class.java) -> {
                require(lessonId != null) { "lessonId is required for LessonViewModel" }
                LessonViewModel(
                    lessonId,
                    lessonRepository,
                    progressRepository
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
