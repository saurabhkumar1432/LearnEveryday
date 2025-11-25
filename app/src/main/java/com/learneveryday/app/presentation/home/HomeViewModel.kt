package com.learneveryday.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learneveryday.app.domain.model.Curriculum
import com.learneveryday.app.domain.model.Difficulty
import com.learneveryday.app.domain.model.GenerationStatus
import com.learneveryday.app.domain.repository.CurriculumRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(
    private val curriculumRepository: CurriculumRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadCurriculums()
    }
    
    fun loadCurriculums() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                curriculumRepository.getAllCurriculumsWithTime()
                    .catch { error ->
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                error = error.message ?: "Failed to load curriculums"
                            ) 
                        }
                    }
                    .collect { curriculums ->
                        _uiState.update { 
                            it.copy(
                                curriculums = curriculums,
                                filteredCurriculums = applyFilters(curriculums, it.filterType, it.selectedDifficulty),
                                isLoading = false,
                                error = null
                            ) 
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
    
    fun filterByType(filterType: FilterType) {
        _uiState.update { 
            it.copy(
                filterType = filterType,
                filteredCurriculums = applyFilters(it.curriculums, filterType, it.selectedDifficulty)
            )
        }
    }
    
    fun filterByDifficulty(difficulty: Difficulty?) {
        _uiState.update { 
            it.copy(
                selectedDifficulty = difficulty,
                filteredCurriculums = applyFilters(it.curriculums, it.filterType, difficulty)
            )
        }
    }
    
    fun searchCurriculums(query: String) {
        _uiState.update { 
            val filtered = if (query.isBlank()) {
                applyFilters(it.curriculums, it.filterType, it.selectedDifficulty)
            } else {
                it.curriculums.filter { curriculum ->
                    curriculum.title.contains(query, ignoreCase = true) ||
                    curriculum.description.contains(query, ignoreCase = true) ||
                    curriculum.tags.any { tag -> tag.contains(query, ignoreCase = true) }
                }
            }
            it.copy(
                searchQuery = query,
                filteredCurriculums = filtered
            )
        }
    }
    
    fun deleteCurriculum(curriculumId: String) {
        viewModelScope.launch {
            try {
                curriculumRepository.deleteCurriculumById(curriculumId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete: ${e.message}") }
            }
        }
    }
    
    fun updateLastAccessed(curriculumId: String) {
        viewModelScope.launch {
            try {
                curriculumRepository.updateLastAccessed(curriculumId)
            } catch (e: Exception) {
                // Silent failure for analytics
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    private fun applyFilters(
        curriculums: List<Curriculum>,
        filterType: FilterType,
        difficulty: Difficulty?
    ): List<Curriculum> {
        var filtered = when (filterType) {
            FilterType.ALL -> curriculums
            FilterType.IN_PROGRESS -> curriculums.filter { it.isInProgress }
            FilterType.COMPLETED -> curriculums.filter { it.isCompleted }
            FilterType.GENERATING -> curriculums.filter { 
                it.generationStatus == GenerationStatus.GENERATING ||
                it.generationStatus == GenerationStatus.PARTIAL
            }
        }
        
        if (difficulty != null) {
            filtered = filtered.filter { it.difficulty == difficulty }
        }
        
        return filtered
    }
}

data class HomeUiState(
    val curriculums: List<Curriculum> = emptyList(),
    val filteredCurriculums: List<Curriculum> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val filterType: FilterType = FilterType.ALL,
    val selectedDifficulty: Difficulty? = null,
    val searchQuery: String = ""
) {
    val isEmpty: Boolean
        get() = !isLoading && filteredCurriculums.isEmpty()
    
    val hasContent: Boolean
        get() = filteredCurriculums.isNotEmpty()
}

enum class FilterType {
    ALL,
    IN_PROGRESS,
    COMPLETED,
    GENERATING
}
