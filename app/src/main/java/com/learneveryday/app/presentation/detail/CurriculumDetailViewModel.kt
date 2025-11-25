package com.learneveryday.app.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learneveryday.app.domain.model.Curriculum
import com.learneveryday.app.domain.model.GenerationStatus
import com.learneveryday.app.domain.model.Lesson
import com.learneveryday.app.domain.model.Progress
import com.learneveryday.app.domain.repository.CurriculumRepository
import com.learneveryday.app.domain.repository.LessonRepository
import com.learneveryday.app.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CurriculumDetailViewModel(
    private val curriculumId: String,
    private val curriculumRepository: CurriculumRepository,
    private val lessonRepository: LessonRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CurriculumDetailUiState())
    val uiState: StateFlow<CurriculumDetailUiState> = _uiState.asStateFlow()
    
    init {
        loadCurriculumDetails()
    }
    
    private fun loadCurriculumDetails() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        // Curriculum stream
        viewModelScope.launch {
            curriculumRepository.getCurriculumById(curriculumId)
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load curriculum"
                        )
                    }
                }
                .collect { curriculum ->
                    _uiState.update {
                        it.copy(
                            curriculum = curriculum,
                            isLoading = false
                        )
                    }
                }
        }

        // Lessons stream
        viewModelScope.launch {
            lessonRepository.getLessonsByCurriculum(curriculumId)
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to load lessons: ${error.message}"
                        )
                    }
                }
                .collect { lessons ->
                    _uiState.update {
                        it.copy(
                            lessons = lessons,
                            isLoading = false
                        )
                    }
                }
        }

        // Progress stream
        viewModelScope.launch {
            progressRepository.getProgressByCurriculum(curriculumId)
                .catch { /* Optional data; ignore errors */ }
                .collect { progress ->
                    _uiState.update { it.copy(progress = progress) }
                }
        }
    }
    
    fun markLessonComplete(lessonId: String) {
        viewModelScope.launch {
            try {
                lessonRepository.updateCompletionStatus(
                    id = lessonId,
                    isCompleted = true,
                    completedAt = System.currentTimeMillis()
                )
                
                // Update curriculum progress
                val completedCount = _uiState.value.lessons.count { it.isCompleted } + 1
                val totalLessons = _uiState.value.lessons.size
                val isCompleted = completedCount >= totalLessons
                
                curriculumRepository.updateProgress(curriculumId, completedCount, isCompleted)
                
                // Update progress entity
                val percentage = if (totalLessons > 0) (completedCount.toFloat() / totalLessons) * 100 else 0f
                progressRepository.updateProgress(curriculumId, completedCount, percentage)
                
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to mark complete: ${e.message}") }
            }
        }
    }
    
    fun markLessonIncomplete(lessonId: String) {
        viewModelScope.launch {
            try {
                lessonRepository.updateCompletionStatus(
                    id = lessonId,
                    isCompleted = false,
                    completedAt = null
                )
                
                // Update curriculum progress
                val completedCount = _uiState.value.lessons.count { it.isCompleted } - 1
                curriculumRepository.updateProgress(curriculumId, maxOf(0, completedCount), false)
                
                // Update progress entity
                val totalLessons = _uiState.value.lessons.size
                val percentage = if (totalLessons > 0) (completedCount.toFloat() / totalLessons) * 100 else 0f
                progressRepository.updateProgress(curriculumId, maxOf(0, completedCount), percentage)
                
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to mark incomplete: ${e.message}") }
            }
        }
    }
    
    fun setCurrentLesson(lessonId: String) {
        viewModelScope.launch {
            try {
                progressRepository.updateCurrentLesson(curriculumId, lessonId)
                curriculumRepository.updateLastAccessed(curriculumId)
            } catch (e: Exception) {
                // Silent failure for analytics
            }
        }
    }
    
    fun deleteCurriculum() {
        viewModelScope.launch {
            try {
                curriculumRepository.deleteCurriculumById(curriculumId)
                _uiState.update { it.copy(isDeleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete: ${e.message}") }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class CurriculumDetailUiState(
    val curriculum: Curriculum? = null,
    val lessons: List<Lesson> = emptyList(),
    val progress: Progress? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isDeleted: Boolean = false
) {
    val completedLessons: Int
        get() = lessons.count { it.isCompleted }
    
    val totalLessons: Int
        get() = lessons.size
    
    val progressPercentage: Float
        get() = if (totalLessons > 0) (completedLessons.toFloat() / totalLessons) * 100 else 0f
    
    val nextLesson: Lesson?
        get() = lessons.firstOrNull { !it.isCompleted }
    
    val hasLessons: Boolean
        get() = lessons.isNotEmpty()
    
    val isGenerating: Boolean
        get() = curriculum?.generationStatus == GenerationStatus.GENERATING ||
                curriculum?.generationStatus == GenerationStatus.PARTIAL
    
    val isComplete: Boolean
        get() = curriculum?.isCompleted == true
}
