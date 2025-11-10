package com.learneveryday.app.presentation.generate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learneveryday.app.domain.model.*
import com.learneveryday.app.domain.repository.CurriculumRepository
import com.learneveryday.app.domain.repository.LessonRepository
import com.learneveryday.app.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class GenerateViewModel(
    private val curriculumRepository: CurriculumRepository,
    private val lessonRepository: LessonRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GenerateUiState())
    val uiState: StateFlow<GenerateUiState> = _uiState.asStateFlow()
    
    fun updateTopic(topic: String) {
        _uiState.update { it.copy(topic = topic, topicError = null) }
    }
    
    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }
    
    fun updateDifficulty(difficulty: Difficulty) {
        _uiState.update { it.copy(difficulty = difficulty) }
    }
    
    fun updateGenerationMode(mode: GenerationMode) {
        _uiState.update { it.copy(generationMode = mode) }
    }
    
    fun updateEstimatedHours(hours: Int) {
        _uiState.update { it.copy(estimatedHours = hours) }
    }
    
    fun addTag(tag: String) {
        if (tag.isNotBlank() && !_uiState.value.tags.contains(tag)) {
            _uiState.update { it.copy(tags = it.tags + tag) }
        }
    }
    
    fun removeTag(tag: String) {
        _uiState.update { it.copy(tags = it.tags - tag) }
    }
    
    fun validateAndPrepare(): Boolean {
        val currentState = _uiState.value
        
        return when {
            currentState.topic.isBlank() -> {
                _uiState.update { it.copy(topicError = "Topic is required") }
                false
            }
            currentState.topic.length < 3 -> {
                _uiState.update { it.copy(topicError = "Topic must be at least 3 characters") }
                false
            }
            else -> {
                _uiState.update { it.copy(topicError = null) }
                true
            }
        }
    }
    
    fun createCurriculumOutline(
        provider: String,
        modelUsed: String,
        lessons: List<LessonOutline>
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isGenerating = true, error = null) }
                
                val currentState = _uiState.value
                val curriculumId = UUID.randomUUID().toString()
                val timestamp = System.currentTimeMillis()
                
                // Create curriculum
                val curriculum = Curriculum(
                    id = curriculumId,
                    title = currentState.topic,
                    description = currentState.description,
                    difficulty = currentState.difficulty,
                    estimatedHours = currentState.estimatedHours,
                    provider = provider,
                    modelUsed = modelUsed,
                    tags = currentState.tags,
                    totalLessons = lessons.size,
                    completedLessons = 0,
                    generationMode = currentState.generationMode,
                    generationStatus = when (currentState.generationMode) {
                        GenerationMode.QUICK_START -> GenerationStatus.PARTIAL
                        GenerationMode.FULL_GENERATION -> GenerationStatus.GENERATING
                        GenerationMode.SMART_MODE -> GenerationStatus.GENERATING
                    },
                    isOutlineOnly = currentState.generationMode == GenerationMode.QUICK_START,
                    isCompleted = false,
                    createdAt = timestamp,
                    lastAccessedAt = timestamp,
                    lastGeneratedAt = timestamp
                )
                
                curriculumRepository.insertCurriculum(curriculum)
                
                // Create lesson entities
                val lessonEntities = lessons.mapIndexed { index, outline ->
                    Lesson(
                        id = UUID.randomUUID().toString(),
                        curriculumId = curriculumId,
                        orderIndex = index,
                        title = outline.title,
                        content = outline.content ?: "",
                        difficulty = currentState.difficulty,
                        estimatedMinutes = outline.estimatedMinutes,
                        keyPoints = outline.keyPoints,
                        practiceExercise = null,
                        prerequisites = emptyList(),
                        nextSteps = emptyList(),
                        isGenerated = outline.content != null,
                        isCompleted = false,
                        completedAt = null,
                        lastReadPosition = 0,
                        timeSpentMinutes = 0
                    )
                }
                
                lessonRepository.insertLessons(lessonEntities)
                
                // Create initial progress
                val progress = Progress(
                    curriculumId = curriculumId,
                    totalLessons = lessons.size,
                    completedLessons = 0,
                    currentLessonId = lessonEntities.firstOrNull()?.id,
                    totalTimeSpentMinutes = 0,
                    lastUpdated = timestamp,
                    progressPercentage = 0f
                )
                
                progressRepository.insertOrUpdateProgress(progress)
                
                _uiState.update { 
                    it.copy(
                        isGenerating = false,
                        isSuccess = true,
                        generatedCurriculumId = curriculumId
                    )
                }
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isGenerating = false,
                        error = "Failed to create curriculum: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun resetState() {
        _uiState.update { GenerateUiState() }
    }
}

data class GenerateUiState(
    val topic: String = "",
    val description: String = "",
    val difficulty: Difficulty = Difficulty.BEGINNER,
    val generationMode: GenerationMode = GenerationMode.SMART_MODE,
    val estimatedHours: Int = 10,
    val tags: List<String> = emptyList(),
    val isGenerating: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val topicError: String? = null,
    val generatedCurriculumId: String? = null
) {
    val canGenerate: Boolean
        get() = topic.isNotBlank() && !isGenerating
}

data class LessonOutline(
    val title: String,
    val estimatedMinutes: Int,
    val keyPoints: List<String>,
    val content: String? = null // Null for outline-only lessons
)
