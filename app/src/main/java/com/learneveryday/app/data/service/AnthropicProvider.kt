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
 * Anthropic AI provider implementation
 */
class AnthropicProvider(
    private val gson: Gson = Gson()
) : AIProvider {
    
    companion object {
        private const val ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages"
        private const val TIMEOUT_MS = 60000 // 60 seconds
        private const val ANTHROPIC_VERSION = "2023-06-01"
    }
    
    override suspend fun sendRequest(
        prompt: String,
        apiKey: String,
        modelName: String,
        temperature: Float,
        maxTokens: Int
    ): String = withContext(Dispatchers.IO) {
        val url = URL(ANTHROPIC_API_URL)
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-api-key", apiKey)
                setRequestProperty("anthropic-version", ANTHROPIC_VERSION)
            }
            
            val requestBody = buildAnthropicRequest(prompt, modelName, temperature, maxTokens)
            
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody)
                writer.flush()
            }
            
            val responseCode = connection.responseCode
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    reader.readText()
                }
                
                extractTextFromAnthropicResponse(response)
            } else {
                val errorResponse = BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { reader ->
                    reader.readText()
                }
                throw Exception("Anthropic API error ($responseCode): $errorResponse")
            }
        } finally {
            connection.disconnect()
        }
    }
    
    private fun buildAnthropicRequest(prompt: String, model: String, temperature: Float, maxTokens: Int): String {
        val request = mapOf(
            "model" to model,
            "messages" to listOf(
                mapOf(
                    "role" to "user",
                    "content" to prompt
                )
            ),
            "max_tokens" to maxTokens,
            "temperature" to temperature,
            "system" to "You are an expert curriculum designer and educator. Always respond with valid JSON only."
        )
        
        return gson.toJson(request)
    }
    
    private fun extractTextFromAnthropicResponse(response: String): String {
        try {
            val jsonResponse = gson.fromJson(response, Map::class.java)
            
            @Suppress("UNCHECKED_CAST")
            val contentList = jsonResponse["content"] as? List<Map<String, Any>>
            val textContent = contentList?.firstOrNull { it["type"] == "text" }
            val text = textContent?.get("text") as? String
            
            return text ?: throw Exception("No text found in Anthropic response")
        } catch (e: Exception) {
            throw Exception("Failed to parse Anthropic response: ${e.message}", e)
        }
    }
}
