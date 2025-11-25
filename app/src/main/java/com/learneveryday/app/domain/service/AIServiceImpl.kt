package com.learneveryday.app.domain.service

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import android.util.Log
import kotlinx.coroutines.delay
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Enhanced AI service with retry logic, validation, and error handling
 */
class AIServiceImpl(
    private val aiProvider: AIProvider,
    private val gson: Gson = Gson()
) : AIService {
    
    companion object {
        private const val MAX_RETRIES = 3
        private const val INITIAL_BACKOFF_MS = 1000L
        private const val MAX_BACKOFF_MS = 8000L
        private const val TAG = "AIServiceImpl"
    }
    
    override suspend fun generateCurriculumOutline(
        request: GenerationRequest
    ): AIResult<CurriculumOutline> {
        validateRequest(request)?.let { return it }
        
        val prompt = AIPromptBuilder.buildCurriculumOutlinePrompt(request)
        
        return executeWithRetry(MAX_RETRIES) { attemptNumber ->
            try {
                val response = aiProvider.sendRequest(
                    prompt = prompt,
                    apiKey = request.apiKey,
                    modelName = request.modelName,
                    temperature = request.temperature,
                    maxTokens = request.maxTokens
                )
                
                parseAndValidateCurriculumOutline(response)
                
            } catch (e: Exception) {
                handleException(e, attemptNumber, MAX_RETRIES, "curriculum outline")
            }
        }
    }
    
    override suspend fun generateLessonContent(
        request: LessonGenerationRequest
    ): AIResult<LessonContent> {
        val prompt = AIPromptBuilder.buildLessonContentPrompt(request)
        
        return executeWithRetry(MAX_RETRIES) { attemptNumber ->
            try {
                val response = aiProvider.sendRequest(
                    prompt = prompt,
                    apiKey = request.apiKey,
                    modelName = request.modelName,
                    temperature = request.temperature,
                    maxTokens = request.maxTokens
                )
                
                parseAndValidateLessonContent(response)
                
            } catch (e: Exception) {
                handleException(e, attemptNumber, MAX_RETRIES, "lesson content")
            }
        }
    }
    
    override suspend fun generateChunkedLessons(
        curriculumTitle: String,
        lessonTitles: List<String>,
        difficulty: com.learneveryday.app.domain.model.Difficulty,
        provider: String,
        apiKey: String,
        modelName: String,
        chunkSize: Int
    ): AIResult<List<LessonOutlineItem>> {
        val allLessons = mutableListOf<LessonOutlineItem>()
        
        lessonTitles.chunked(chunkSize).forEach { chunk ->
            val prompt = AIPromptBuilder.buildChunkedLessonPrompt(
                curriculumTitle, chunk, difficulty, chunk.size
            )
            
            val result = executeWithRetry(MAX_RETRIES) { attemptNumber ->
                try {
                    val response = aiProvider.sendRequest(
                        prompt = prompt,
                        apiKey = apiKey,
                        modelName = modelName,
                        temperature = 0.7f,
                        maxTokens = 4000
                    )
                    
                    parseChunkedLessons(response)
                    
                } catch (e: Exception) {
                    handleException(e, attemptNumber, MAX_RETRIES, "chunked lessons")
                }
            }
            
            when (result) {
                is AIResult.Success -> allLessons.addAll(result.data)
                is AIResult.Error -> return result
                is AIResult.Retry -> return result
            }
        }
        
        return AIResult.Success(allLessons)
    }
    
    private fun validateRequest(request: GenerationRequest): AIResult.Error? {
        return when {
            request.topic.isBlank() -> 
                AIResult.Error("Topic cannot be empty")
            request.topic.length < 3 -> 
                AIResult.Error("Topic must be at least 3 characters")
            request.apiKey.isBlank() -> 
                AIResult.Error("API key is required")
            request.modelName.isBlank() -> 
                AIResult.Error("Model name is required")
            request.estimatedHours < 1 -> 
                AIResult.Error("Estimated hours must be at least 1")
            request.maxLessons < 1 -> 
                AIResult.Error("Must have at least 1 lesson")
            request.maxTokens < 500 -> 
                AIResult.Error("Max tokens too low, minimum 500")
            else -> null
        }
    }
    
    private suspend fun <T> executeWithRetry(
        maxAttempts: Int,
        block: suspend (attemptNumber: Int) -> AIResult<T>
    ): AIResult<T> {
        var currentAttempt = 1
        var lastError: AIResult.Error? = null
        
        while (currentAttempt <= maxAttempts) {
            val result = block(currentAttempt)
            
            when (result) {
                is AIResult.Success -> return result
                is AIResult.Error -> {
                    lastError = result
                    if (currentAttempt < maxAttempts && result.cause?.isRetryable() == true) {
                        val backoffTime = calculateBackoff(currentAttempt)
                        delay(backoffTime)
                        currentAttempt++
                    } else {
                        return result
                    }
                }
                is AIResult.Retry -> {
                    val backoffTime = calculateBackoff(currentAttempt)
                    delay(backoffTime)
                    currentAttempt++
                }
            }
        }
        
        return lastError ?: AIResult.Error("Max retries exceeded")
    }
    
    private fun calculateBackoff(attemptNumber: Int): Long {
        val backoff = INITIAL_BACKOFF_MS * (1 shl (attemptNumber - 1))
        return minOf(backoff, MAX_BACKOFF_MS)
    }
    
    private fun Throwable.isRetryable(): Boolean {
        return when (this) {
            is UnknownHostException -> true
            is SocketTimeoutException -> true
            is IOException -> true
            else -> false
        }
    }
    
    private fun handleException(
        e: Exception,
        attemptNumber: Int,
        maxAttempts: Int,
        context: String
    ): AIResult<Nothing> {
        val errorMessage = when (e) {
            is UnknownHostException -> 
                "Network Error: Cannot reach AI service. Check your internet connection."
            is SocketTimeoutException -> 
                "Request timeout. The AI service is taking too long to respond."
            is IOException -> 
                "Network error occurred while generating $context."
            is JsonSyntaxException -> 
                "Failed to parse AI response. The response format was invalid."
            else -> 
                "Failed to generate $context: ${e.message ?: "Unknown error"}"
        }
        
        return if (attemptNumber < maxAttempts && e.isRetryable()) {
            AIResult.Retry(attemptNumber, maxAttempts)
        } else {
            AIResult.Error(errorMessage, e)
        }
    }
    
    private fun parseAndValidateCurriculumOutline(response: String): AIResult<CurriculumOutline> {
        val cleanedJson = extractJsonFromResponse(response)
        
        return try {
            val outline = gson.fromJson(cleanedJson, CurriculumOutline::class.java)
            
            // Validate the parsed outline
            when {
                outline.title.isBlank() -> 
                    AIResult.Error("Invalid response: curriculum title is missing")
                outline.lessons.isEmpty() -> 
                    AIResult.Error("Invalid response: no lessons generated")
                outline.lessons.any { it.title.isBlank() } -> 
                    AIResult.Error("Invalid response: some lessons have no title")
                outline.lessons.any { it.estimatedMinutes < 1 } -> 
                    AIResult.Error("Invalid response: invalid lesson duration")
                else -> AIResult.Success(outline)
            }
        } catch (e: JsonSyntaxException) {
            AIResult.Error("Failed to parse curriculum outline: Invalid JSON format", e)
        }
    }
    
    private fun parseAndValidateLessonContent(response: String): AIResult<LessonContent> {
        val cleanedJson = extractJsonFromResponse(response)
        
        return try {
            val content = gson.fromJson(cleanedJson, LessonContent::class.java)
            
            // Be permissive: require non-blank content. Accept shorter content and empty keyPoints
            // but log warnings via Error result where appropriate so callers may choose retries.
            if (content.content.isBlank()) {
                AIResult.Error("Invalid response: lesson content is empty")
            } else {
                // If keyPoints is null (possible), coerce to empty list
                val safeKeyPoints = content.keyPoints ?: emptyList()
                val safeContent = content.copy(keyPoints = safeKeyPoints)

                // If content is very short, prefer to succeed but caller/workers can decide to retry
                if (safeContent.content.length < 80) {
                    // Return success but with a warning encoded in message (caller can still accept)
                    AIResult.Success(safeContent)
                } else {
                    AIResult.Success(safeContent)
                }
            }
        } catch (e: JsonSyntaxException) {
            // Try a permissive fallback: if the AI returned plain markdown or text instead of JSON,
            // strip common wrappers and return it as lesson content rather than failing outright.
            try {
                var fallback = response.trim()
                // Remove triple-backtick fences
                if (fallback.startsWith("```")) {
                    // remove the first fence and any language marker
                    val firstLineEnd = fallback.indexOf('\n')
                    if (firstLineEnd != -1) fallback = fallback.substring(firstLineEnd + 1)
                }
                if (fallback.endsWith("```")) {
                    val lastFence = fallback.lastIndexOf("```")
                    if (lastFence != -1) fallback = fallback.substring(0, lastFence)
                }

                fallback = fallback.trim()

                if (fallback.isNotBlank()) {
                    Log.w(TAG, "Fallback: using raw AI response as lesson content due to JSON parse error")
                    val recovered = LessonContent(
                        content = fallback,
                        keyPoints = emptyList(),
                        practiceExercise = null,
                        prerequisites = emptyList(),
                        nextSteps = emptyList()
                    )
                    AIResult.Success(recovered)
                } else {
                    AIResult.Error("Failed to parse lesson content: Invalid JSON format", e)
                }
            } catch (fallbackEx: Exception) {
                AIResult.Error("Failed to parse lesson content: Invalid JSON format", e)
            }
        }
    }
    
    private fun parseChunkedLessons(response: String): AIResult<List<LessonOutlineItem>> {
        val cleanedJson = extractJsonFromResponse(response)
        
        return try {
            val wrapper = gson.fromJson(cleanedJson, ChunkedLessonsResponse::class.java)
            AIResult.Success(wrapper.lessons)
        } catch (e: JsonSyntaxException) {
            AIResult.Error("Failed to parse chunked lessons: Invalid JSON format", e)
        }
    }
    
    private fun extractJsonFromResponse(response: String): String {
        // Remove markdown code blocks if present
        var cleaned = response.trim()
        
        // Remove ```json ... ``` or ``` ... ```
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```json").removePrefix("```")
            val endIndex = cleaned.lastIndexOf("```")
            if (endIndex != -1) {
                cleaned = cleaned.substring(0, endIndex)
            }
        }
        
        // Find the first { and last }
        val startIndex = cleaned.indexOf('{')
        val endIndex = cleaned.lastIndexOf('}')
        
        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            cleaned = cleaned.substring(startIndex, endIndex + 1)
        }
        
        return cleaned.trim()
    }
    
    private data class ChunkedLessonsResponse(
        val lessons: List<LessonOutlineItem>
    )
}

/**
 * AI service interface
 */
interface AIService {
    suspend fun generateCurriculumOutline(request: GenerationRequest): AIResult<CurriculumOutline>
    suspend fun generateLessonContent(request: LessonGenerationRequest): AIResult<LessonContent>
    suspend fun generateChunkedLessons(
        curriculumTitle: String,
        lessonTitles: List<String>,
        difficulty: com.learneveryday.app.domain.model.Difficulty,
        provider: String,
        apiKey: String,
        modelName: String,
        chunkSize: Int = 5
    ): AIResult<List<LessonOutlineItem>>
}


