package com.learneveryday.app.data.service

import android.util.Log
import com.google.gson.Gson
import com.learneveryday.app.domain.service.AIProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Gemini AI provider implementation
 */
class GeminiAIProvider(
    private val gson: Gson = Gson()
) : AIProvider {
    
    companion object {
        private const val TAG = "GeminiAIProvider"
        private const val GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val TIMEOUT_MS = 60000 // 60 seconds
    }
    
    override suspend fun sendRequest(
        prompt: String,
        apiKey: String,
        modelName: String,
        temperature: Float,
        maxTokens: Int
    ): String = withContext(Dispatchers.IO) {
        val fullUrl = "$GEMINI_API_URL/$modelName:generateContent?key=$apiKey"
        Log.d(TAG, "Making request to: $GEMINI_API_URL/$modelName:generateContent")
        
        val url = URL(fullUrl)
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Content-Type", "application/json")
            }
            
            // Check if prompt expects JSON response (for curriculum generation)
            val expectsJson = prompt.contains("JSON", ignoreCase = true) || 
                              prompt.contains("```json", ignoreCase = true)
            val requestBody = buildGeminiRequest(prompt, temperature, maxTokens, expectsJson)
            
            Log.d(TAG, "Request body: $requestBody")
            
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody)
                writer.flush()
            }
            
            val responseCode = connection.responseCode
            Log.d(TAG, "Response code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    reader.readText()
                }
                
                extractTextFromGeminiResponse(response)
            } else {
                val errorResponse = BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { reader ->
                    reader.readText()
                }
                Log.e(TAG, "Error response: $errorResponse")
                
                // Parse error for better message
                val errorMessage = try {
                    val errorJson = gson.fromJson(errorResponse, Map::class.java)
                    @Suppress("UNCHECKED_CAST")
                    val error = errorJson["error"] as? Map<String, Any>
                    error?.get("message") as? String ?: errorResponse
                } catch (e: Exception) {
                    errorResponse
                }
                
                throw Exception("Gemini API error ($responseCode): $errorMessage")
            }
        } finally {
            connection.disconnect()
        }
    }
    
    private fun buildGeminiRequest(prompt: String, temperature: Float, maxTokens: Int, expectsJson: Boolean = false): String {
        // Gemini API accepts temperature between 0.0 and 2.0, but some models cap at 1.0
        // Clamp to safe range
        val safeTemperature = temperature.coerceIn(0.0f, 1.0f)
        
        val generationConfig = mutableMapOf<String, Any>(
            "temperature" to safeTemperature,
            "maxOutputTokens" to maxTokens
        )
        
        // Only add responseMimeType for JSON when we expect JSON output
        if (expectsJson) {
            generationConfig["responseMimeType"] = "application/json"
        }
        
        val request = mapOf(
            "contents" to listOf(
                mapOf(
                    "parts" to listOf(
                        mapOf("text" to prompt)
                    )
                )
            ),
            "generationConfig" to generationConfig
        )
        
        return gson.toJson(request)
    }
    
    private fun extractTextFromGeminiResponse(response: String): String {
        try {
            val jsonResponse = gson.fromJson(response, Map::class.java)
            
            @Suppress("UNCHECKED_CAST")
            val candidates = jsonResponse["candidates"] as? List<Map<String, Any>>
            val content = candidates?.firstOrNull()?.get("content") as? Map<String, Any>
            val parts = content?.get("parts") as? List<Map<String, Any>>
            val text = parts?.firstOrNull()?.get("text") as? String
            
            if (text.isNullOrBlank()) {
                 val finishReason = candidates?.firstOrNull()?.get("finishReason") as? String
                 if (finishReason == "MAX_TOKENS") {
                     throw Exception("Response truncated. Try increasing Max Tokens.")
                 }
                 throw Exception("No text found in Gemini response (Reason: $finishReason)")
            }
            
            return text
        } catch (e: Exception) {
            throw Exception("Failed to parse Gemini response: ${e.message}", e)
        }
    }
}
