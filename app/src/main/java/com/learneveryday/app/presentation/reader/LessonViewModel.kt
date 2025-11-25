package com.learneveryday.app.presentation.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learneveryday.app.domain.model.Lesson
import com.learneveryday.app.domain.repository.CurriculumRepository
import com.learneveryday.app.domain.repository.LessonRepository
import com.learneveryday.app.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.FlowPreview

class LessonViewModel(
    private val lessonId: String,
    private val lessonRepository: LessonRepository,
    private val progressRepository: ProgressRepository,
    private val curriculumRepository: CurriculumRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LessonUiState())
    val uiState: StateFlow<LessonUiState> = _uiState.asStateFlow()
    
    private var readingStartTime: Long = 0
    private val readPositionEvents = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    
    // Cache for content to prevent unnecessary re-renders
    private var lastRenderedContent: String? = null
    
    // Navigation state
    private var allLessons: List<Lesson> = emptyList()
    private var currentLessonIndex: Int = -1
    
    init {
        loadLesson()
        observeReadPosition()
    }
    
    private fun loadLesson() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                lessonRepository.getLessonById(lessonId)
                    .flowOn(Dispatchers.IO)
                    .distinctUntilChanged()
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
                            // Load all lessons for this curriculum for navigation
                            loadCurriculumLessons(lesson.curriculumId, lesson.id)
                            
                            val contentChanged = lesson.content != lastRenderedContent
                            lastRenderedContent = lesson.content
                            
                            _uiState.update { currentState ->
                                currentState.copy(
                                    lesson = lesson,
                                    isLoading = false,
                                    readPosition = if (currentState.lesson == null) lesson.lastReadPosition else currentState.readPosition,
                                    contentChanged = contentChanged,
                                    isCompleted = lesson.isCompleted
                                )
                            }
                            if (readingStartTime == 0L) {
                                readingStartTime = System.currentTimeMillis()
                            }
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
    
    private fun loadCurriculumLessons(curriculumId: String, currentId: String) {
        viewModelScope.launch {
            try {
                lessonRepository.getLessonsByCurriculum(curriculumId)
                    .flowOn(Dispatchers.IO)
                    .collect { lessons ->
                        allLessons = lessons.sortedBy { it.orderIndex }
                        currentLessonIndex = allLessons.indexOfFirst { it.id == currentId }
                        val completedCount = allLessons.count { it.isCompleted }
                        
                        _uiState.update { currentState ->
                            currentState.copy(
                                currentIndex = currentLessonIndex,
                                totalLessons = allLessons.size,
                                hasPrevious = currentLessonIndex > 0,
                                hasNext = currentLessonIndex < allLessons.size - 1,
                                completedLessons = completedCount
                            )
                        }
                    }
            } catch (e: Exception) {
                // Silent failure for navigation
            }
        }
    }
    
    fun navigateToPrevious(): String? {
        if (currentLessonIndex > 0 && allLessons.isNotEmpty()) {
            return allLessons[currentLessonIndex - 1].id
        }
        return null
    }
    
    fun navigateToNext(): String? {
        if (currentLessonIndex < allLessons.size - 1 && allLessons.isNotEmpty()) {
            return allLessons[currentLessonIndex + 1].id
        }
        return null
    }
    
    /**
     * Acknowledge that content has been rendered, reset the contentChanged flag
     */
    fun onContentRendered() {
        _uiState.update { it.copy(contentChanged = false) }
    }
    
    fun updateReadPosition(position: Int) {
        _uiState.update { it.copy(readPosition = position) }
        // Debounce DB writes
        readPositionEvents.tryEmit(position)
    }

    @OptIn(FlowPreview::class)
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
                    
                    // Update curriculum progress
                    val completedCount = allLessons.count { it.isCompleted } + 1
                    val totalLessons = allLessons.size
                    val isCompleted = completedCount >= totalLessons
                    
                    curriculumRepository.updateProgress(lesson.curriculumId, completedCount, isCompleted)
                    
                    // Update progress entity
                    val percentage = if (totalLessons > 0) (completedCount.toFloat() / totalLessons) * 100 else 0f
                    progressRepository.updateProgress(lesson.curriculumId, completedCount, percentage)
                    
                    // Update local state for UI
                    _uiState.update { it.copy(completedLessons = completedCount) }
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
                
                _uiState.value.lesson?.let { lesson ->
                    // Update curriculum progress
                    val completedCount = maxOf(0, allLessons.count { it.isCompleted } - 1)
                    val totalLessons = allLessons.size
                    
                    curriculumRepository.updateProgress(lesson.curriculumId, completedCount, false)
                    
                    // Update progress entity
                    val percentage = if (totalLessons > 0) (completedCount.toFloat() / totalLessons) * 100 else 0f
                    progressRepository.updateProgress(lesson.curriculumId, completedCount, percentage)
                    
                    // Update local state for UI
                    _uiState.update { it.copy(completedLessons = completedCount) }
                }
                
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
    val isCompleted: Boolean = false,
    val contentChanged: Boolean = false,
    // Navigation state
    val currentIndex: Int = 0,
    val totalLessons: Int = 0,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
    // Curriculum completion progress
    val completedLessons: Int = 0
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
    
    val lessonProgress: String
        get() = if (totalLessons > 0) "Lesson ${currentIndex + 1} of $totalLessons" else ""
    
    // Curriculum completion percentage (0-100)
    val curriculumProgressPercent: Int
        get() = if (totalLessons > 0) ((completedLessons.toFloat() / totalLessons) * 100).toInt() else 0
    
    // Content identifier for efficient diffing
    val contentId: String
        get() = lesson?.content?.hashCode()?.toString() ?: ""
}
