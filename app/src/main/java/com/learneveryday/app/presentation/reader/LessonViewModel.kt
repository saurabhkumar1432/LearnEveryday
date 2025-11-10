package com.learneveryday.app.presentation.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learneveryday.app.domain.model.Lesson
import com.learneveryday.app.domain.repository.LessonRepository
import com.learneveryday.app.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LessonViewModel(
    private val lessonId: String,
    private val lessonRepository: LessonRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LessonUiState())
    val uiState: StateFlow<LessonUiState> = _uiState.asStateFlow()
    
    private var readingStartTime: Long = 0
    private val readPositionEvents = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    
    init {
        loadLesson()
        observeReadPosition()
    }
    
    private fun loadLesson() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                lessonRepository.getLessonById(lessonId)
                    .catch { error ->
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Failed to load lesson"
                            )
                        }
                    }
                    .collect { lesson ->
                        if (lesson != null) {
                            _uiState.update { 
                                it.copy(
                                    lesson = lesson,
                                    isLoading = false,
                                    readPosition = lesson.lastReadPosition
                                )
                            }
                            readingStartTime = System.currentTimeMillis()
                        } else {
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    error = "Lesson not found"
                                )
                            }
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }
    
    fun updateReadPosition(position: Int) {
        _uiState.update { it.copy(readPosition = position) }
        // Debounce DB writes
        readPositionEvents.tryEmit(position)
    }

    private fun observeReadPosition() {
        viewModelScope.launch {
            readPositionEvents
                .debounce(300)
                .distinctUntilChanged()
                .collect { pos ->
                    try {
                        lessonRepository.updateReadPosition(lessonId, pos)
                    } catch (_: Exception) { }
                }
        }
    }
    
    fun markComplete() {
        viewModelScope.launch {
            try {
                val timeSpent = calculateTimeSpent()
                
                lessonRepository.updateCompletionStatus(
                    id = lessonId,
                    isCompleted = true,
                    completedAt = System.currentTimeMillis()
                )
                
                lessonRepository.updateTimeSpent(
                    id = lessonId,
                    minutes = (_uiState.value.lesson?.timeSpentMinutes ?: 0) + timeSpent
                )
                
                _uiState.value.lesson?.let { lesson ->
                    progressRepository.updateCurrentLesson(lesson.curriculumId, lessonId)
                }
                
                _uiState.update { it.copy(isCompleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to mark complete: ${e.message}") }
            }
        }
    }
    
    fun markIncomplete() {
        viewModelScope.launch {
            try {
                lessonRepository.updateCompletionStatus(
                    id = lessonId,
                    isCompleted = false,
                    completedAt = null
                )
                
                _uiState.update { it.copy(isCompleted = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to mark incomplete: ${e.message}") }
            }
        }
    }
    
    fun onPause() {
        viewModelScope.launch {
            try {
                val timeSpent = calculateTimeSpent()
                if (timeSpent > 0) {
                    val lesson = _uiState.value.lesson
                    val currentTime = lesson?.timeSpentMinutes ?: 0
                    // Update lesson
                    lessonRepository.updateTimeSpent(lessonId, currentTime + timeSpent)
                    // Update aggregate curriculum progress time
                    if (lesson != null) {
                        val existing = withContext(Dispatchers.IO) { progressRepository.getProgressByCurriculumSync(lesson.curriculumId) }
                        val aggregate = (existing?.totalTimeSpentMinutes ?: 0) + timeSpent
                        progressRepository.updateTimeSpent(lesson.curriculumId, aggregate)
                    }
                }
            } catch (e: Exception) {
                // Silent failure
            }
        }
    }
    
    fun onResume() {
        readingStartTime = System.currentTimeMillis()
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    private fun calculateTimeSpent(): Int {
        if (readingStartTime == 0L) return 0
        val elapsedMillis = System.currentTimeMillis() - readingStartTime
        return (elapsedMillis / 60000).toInt() // Convert to minutes
    }
}

data class LessonUiState(
    val lesson: Lesson? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val readPosition: Int = 0,
    val isCompleted: Boolean = false
) {
    val hasContent: Boolean
        get() = lesson?.hasContent == true
    
    val progressPercentage: Float
        get() = lesson?.completionPercentage ?: 0f
    
    val estimatedReadTime: String
        get() {
            val minutes = lesson?.estimatedMinutes ?: 0
            return when {
                minutes < 60 -> "$minutes min"
                else -> "${minutes / 60}h ${minutes % 60}m"
            }
        }
}
