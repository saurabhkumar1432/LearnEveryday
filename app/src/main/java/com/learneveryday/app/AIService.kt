package com.learneveryday.app

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class AIService(private val config: AIConfig) {
    
    private val gson = Gson()
    
    suspend fun generateCurriculum(request: CurriculumRequest): CurriculumResponse = withContext(Dispatchers.IO) {
        try {
            val prompt = buildCurriculumPrompt(request)
            val response = when (config.provider) {
                AIProvider.GEMINI -> callGeminiAPI(prompt)
                AIProvider.OPENROUTER -> callOpenRouterAPI(prompt)
                AIProvider.OPENAI -> callOpenAIAPI(prompt)
                AIProvider.ANTHROPIC -> callAnthropicAPI(prompt)
                AIProvider.CUSTOM -> callCustomAPI(prompt)
            }
            
            parseCurriculumResponse(response, request)
        } catch (e: java.net.UnknownHostException) {
            CurriculumResponse(
                success = false,
                error = "Network Error: Cannot reach API server.\n\nPlease check your internet connection and try again.\n\nHost: ${e.message}"
            )
        } catch (e: java.net.SocketTimeoutException) {
            CurriculumResponse(
                success = false,
                error = "Network Timeout: The API server took too long to respond.\n\nPlease check your internet connection and try again."
            )
        } catch (e: java.io.IOException) {
            CurriculumResponse(
                success = false,
                error = "Network Error: ${e.message}\n\nPlease check your internet connection and try again."
            )
        } catch (e: Exception) {
            CurriculumResponse(
                success = false,
                error = "Failed to generate curriculum: ${e.message}"
            )
        }
    }
    
    private fun buildCurriculumPrompt(request: CurriculumRequest): String {
        return """
Create a comprehensive learning curriculum for: ${request.topic}

Requirements:
- Difficulty level: ${request.difficulty}
- Number of lessons: ${request.numberOfLessons}
${if (request.focusAreas.isNotEmpty()) "- Focus areas: ${request.focusAreas.joinToString(", ")}" else ""}

IMPORTANT: Respond with ONLY a valid JSON object. Do not include any explanatory text before or after the JSON.

JSON Format:
{
  "title": "Course title",
  "description": "Brief course description (2-3 sentences)",
  "estimatedHours": estimated_total_hours,
  "tags": ["tag1", "tag2", "tag3"],
  "lessons": [
    {
      "id": 1,
      "title": "Lesson title",
      "content": "Detailed lesson content with examples, explanations, and practical tips. Should be comprehensive but digestible in 10-15 minutes.",
      "estimatedMinutes": 10,
      "difficulty": "Beginner|Intermediate|Advanced"
    }
  ]
}

Guidelines for lesson content:
1. Start from absolute basics for beginners
2. Progress logically from simple to complex
3. Include practical examples and code snippets where applicable
4. Each lesson should be 300-500 words
5. Use clear explanations with bullet points
6. Include tips and best practices
7. Add "Try this" suggestions for hands-on practice
8. Progress naturally from ${request.difficulty.split(" ").first()} to ${request.difficulty.split(" ").last()} level

Make the curriculum comprehensive, practical, and engaging. Focus on real-world applications and build upon previous lessons.

RESPOND WITH ONLY THE JSON OBJECT - NO MARKDOWN CODE BLOCKS, NO EXPLANATIONS, JUST THE RAW JSON.
        """.trimIndent()
    }
    
    private suspend fun callGeminiAPI(prompt: String): String = withContext(Dispatchers.IO) {
        val url = URL("${config.provider.baseUrl}/${config.modelName}:generateContent?key=${config.apiKey}")
        val connection = url.openConnection() as HttpURLConnection
        
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        
        val requestBody = JsonObject().apply {
            add("contents", gson.toJsonTree(listOf(
                mapOf(
                    "parts" to listOf(
                        mapOf("text" to prompt)
                    )
                )
            )))
            add("generationConfig", JsonObject().apply {
                addProperty("temperature", config.temperature)
                addProperty("maxOutputTokens", config.maxTokens)
                addProperty("responseMimeType", "application/json")
            })
        }
        
        OutputStreamWriter(connection.outputStream).use { it.write(requestBody.toString()) }
        
        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                val response = reader.readText()
                val jsonResponse = gson.fromJson(response, JsonObject::class.java)
                jsonResponse.getAsJsonArray("candidates")
                    ?.get(0)?.asJsonObject
                    ?.getAsJsonObject("content")
                    ?.getAsJsonArray("parts")
                    ?.get(0)?.asJsonObject
                    ?.get("text")?.asString ?: throw Exception("Invalid response format")
            }
        } else {
            val error = BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
            throw Exception("API Error ($responseCode): $error")
        }
    }
    
    private suspend fun callOpenRouterAPI(prompt: String): String = withContext(Dispatchers.IO) {
        val url = URL(config.provider.baseUrl!!)
        val connection = url.openConnection() as HttpURLConnection
        
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Authorization", "Bearer ${config.apiKey}")
        connection.setRequestProperty("HTTP-Referer", "https://github.com/saurabhkumar1432/LearnEveryday")
        connection.doOutput = true
        
        val requestBody = JsonObject().apply {
            addProperty("model", config.modelName)
            add("messages", gson.toJsonTree(listOf(
                mapOf(
                    "role" to "user",
                    "content" to prompt
                )
            )))
            addProperty("temperature", config.temperature)
            addProperty("max_tokens", config.maxTokens)
        }
        
        OutputStreamWriter(connection.outputStream).use { it.write(requestBody.toString()) }
        
        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                val response = reader.readText()
                val jsonResponse = gson.fromJson(response, JsonObject::class.java)
                jsonResponse.getAsJsonArray("choices")
                    ?.get(0)?.asJsonObject
                    ?.getAsJsonObject("message")
                    ?.get("content")?.asString ?: throw Exception("Invalid response format")
            }
        } else {
            val error = BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
            throw Exception("API Error ($responseCode): $error")
        }
    }
    
    private suspend fun callOpenAIAPI(prompt: String): String = withContext(Dispatchers.IO) {
        val url = URL(config.provider.baseUrl!!)
        val connection = url.openConnection() as HttpURLConnection
        
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Authorization", "Bearer ${config.apiKey}")
        connection.doOutput = true
        
        val requestBody = JsonObject().apply {
            addProperty("model", config.modelName)
            add("messages", gson.toJsonTree(listOf(
                mapOf(
                    "role" to "system",
                    "content" to "You are an expert educator creating comprehensive learning curriculums. Always respond with valid JSON only."
                ),
                mapOf(
                    "role" to "user",
                    "content" to prompt
                )
            )))
            addProperty("temperature", config.temperature)
            addProperty("max_tokens", config.maxTokens)
        }
        
        OutputStreamWriter(connection.outputStream).use { it.write(requestBody.toString()) }
        
        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                val response = reader.readText()
                val jsonResponse = gson.fromJson(response, JsonObject::class.java)
                jsonResponse.getAsJsonArray("choices")
                    ?.get(0)?.asJsonObject
                    ?.getAsJsonObject("message")
                    ?.get("content")?.asString ?: throw Exception("Invalid response format")
            }
        } else {
            val error = BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
            throw Exception("API Error ($responseCode): $error")
        }
    }
    
    private suspend fun callAnthropicAPI(prompt: String): String = withContext(Dispatchers.IO) {
        val url = URL(config.provider.baseUrl!!)
        val connection = url.openConnection() as HttpURLConnection
        
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("x-api-key", config.apiKey)
        connection.setRequestProperty("anthropic-version", "2023-06-01")
        connection.doOutput = true
        
        val requestBody = JsonObject().apply {
            addProperty("model", config.modelName)
            addProperty("max_tokens", config.maxTokens)
            addProperty("temperature", config.temperature)
            add("messages", gson.toJsonTree(listOf(
                mapOf(
                    "role" to "user",
                    "content" to prompt
                )
            )))
        }
        
        OutputStreamWriter(connection.outputStream).use { it.write(requestBody.toString()) }
        
        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                val response = reader.readText()
                val jsonResponse = gson.fromJson(response, JsonObject::class.java)
                jsonResponse.getAsJsonArray("content")
                    ?.get(0)?.asJsonObject
                    ?.get("text")?.asString ?: throw Exception("Invalid response format")
            }
        } else {
            val error = BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
            throw Exception("API Error ($responseCode): $error")
        }
    }
    
    private suspend fun callCustomAPI(prompt: String): String = withContext(Dispatchers.IO) {
        val url = URL(config.customEndpoint!!)
        val connection = url.openConnection() as HttpURLConnection
        
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Authorization", "Bearer ${config.apiKey}")
        connection.doOutput = true
        
        val requestBody = JsonObject().apply {
            addProperty("prompt", prompt)
            addProperty("temperature", config.temperature)
            addProperty("max_tokens", config.maxTokens)
        }
        
        OutputStreamWriter(connection.outputStream).use { it.write(requestBody.toString()) }
        
        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
        } else {
            val error = BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
            throw Exception("API Error ($responseCode): $error")
        }
    }
    
    private fun parseCurriculumResponse(response: String, request: CurriculumRequest): CurriculumResponse {
        return try {
            // Extract JSON from markdown code blocks or find JSON object in the response
            val jsonString = when {
                response.contains("```json") -> {
                    response.substringAfter("```json").substringBefore("```").trim()
                }
                response.contains("```") -> {
                    response.substringAfter("```").substringBefore("```").trim()
                }
                response.contains("{") -> {
                    // Find the first { and last } to extract JSON
                    val startIndex = response.indexOf('{')
                    val endIndex = response.lastIndexOf('}')
                    if (startIndex >= 0 && endIndex > startIndex) {
                        response.substring(startIndex, endIndex + 1).trim()
                    } else {
                        response.trim()
                    }
                }
                else -> response.trim()
            }
            
            val jsonObject = gson.fromJson(jsonString, JsonObject::class.java)
            
            val lessons = mutableListOf<Lesson>()
            jsonObject.getAsJsonArray("lessons")?.forEach { lessonElement ->
                val lessonObj = lessonElement.asJsonObject
                lessons.add(
                    Lesson(
                        id = lessonObj.get("id").asInt,
                        title = lessonObj.get("title").asString,
                        content = lessonObj.get("content").asString,
                        estimatedMinutes = lessonObj.get("estimatedMinutes")?.asInt ?: 10,
                        difficulty = lessonObj.get("difficulty")?.asString ?: "Beginner"
                    )
                )
            }
            
            val tags = mutableListOf<String>()
            jsonObject.getAsJsonArray("tags")?.forEach { tags.add(it.asString) }
            
            val topic = LearningTopic(
                id = request.topic.lowercase().replace(" ", "_") + "_" + System.currentTimeMillis(),
                title = jsonObject.get("title").asString,
                description = jsonObject.get("description").asString,
                difficulty = request.difficulty,
                estimatedHours = jsonObject.get("estimatedHours")?.asInt ?: lessons.size / 6,
                lessons = lessons,
                isAIGenerated = true,
                generatedAt = System.currentTimeMillis(),
                tags = tags
            )
            
            CurriculumResponse(success = true, topic = topic)
        } catch (e: com.google.gson.JsonSyntaxException) {
            CurriculumResponse(
                success = false,
                error = "Failed to parse AI response: Invalid JSON format.\n\n" +
                        "This often happens when the response is too long and gets cut off.\n\n" +
                        "Try:\n" +
                        "1. Increase 'Max Tokens' in settings (try 8000-16000)\n" +
                        "2. Request fewer lessons\n" +
                        "3. Use a more concise model\n\n" +
                        "Error: ${e.message}"
            )
        } catch (e: java.io.EOFException) {
            CurriculumResponse(
                success = false,
                error = "Response was truncated (incomplete).\n\n" +
                        "The AI's response was cut off before finishing.\n\n" +
                        "Solutions:\n" +
                        "1. Go to Settings and increase 'Max Tokens' to 8000 or higher\n" +
                        "2. Request fewer lessons (try 3-5 instead of 10)\n" +
                        "3. The current setting may be too low for a complete curriculum\n\n" +
                        "Current response length: ${response.length} characters"
            )
        } catch (e: Exception) {
            // Provide more detailed error information
            val errorDetails = StringBuilder()
            errorDetails.append("Failed to parse AI response: ${e.javaClass.simpleName}\n")
            errorDetails.append("${e.message}\n\n")
            errorDetails.append("Response preview (first 500 chars):\n")
            errorDetails.append(response.take(500))
            if (response.length > 500) {
                errorDetails.append("\n...(truncated, total: ${response.length} chars)")
            }
            
            CurriculumResponse(
                success = false,
                error = errorDetails.toString()
            )
        }
    }
}
