package com.learneveryday.app.data.service

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
        val url = URL("$GEMINI_API_URL/$modelName:generateContent?key=$apiKey")
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
            
            val requestBody = buildGeminiRequest(prompt, temperature, maxTokens)
            
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody)
                writer.flush()
            }
            
            val responseCode = connection.responseCode
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    reader.readText()
                }
                
                extractTextFromGeminiResponse(response)
            } else {
                val errorResponse = BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { reader ->
                    reader.readText()
                }
                throw Exception("Gemini API error ($responseCode): $errorResponse")
            }
        } finally {
            connection.disconnect()
        }
    }
    
    private fun buildGeminiRequest(prompt: String, temperature: Float, maxTokens: Int): String {
        val request = mapOf(
            "contents" to listOf(
                mapOf(
                    "parts" to listOf(
                        mapOf("text" to prompt)
                    )
                )
            ),
            "generationConfig" to mapOf(
                "temperature" to temperature,
                "maxOutputTokens" to maxTokens,
                "responseMimeType" to "application/json"
            )
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
            
            return text ?: throw Exception("No text found in Gemini response")
        } catch (e: Exception) {
            throw Exception("Failed to parse Gemini response: ${e.message}", e)
        }
    }
}
