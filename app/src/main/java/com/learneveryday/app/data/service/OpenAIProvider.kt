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
 * OpenAI API provider implementation
 */
class OpenAIProvider(
    private val gson: Gson = Gson()
) : AIProvider {
    
    companion object {
        private const val OPENAI_API_URL = "https://api.openai.com/v1/chat/completions"
        private const val TIMEOUT_MS = 60000 // 60 seconds
    }
    
    override suspend fun sendRequest(
        prompt: String,
        apiKey: String,
        modelName: String,
        temperature: Float,
        maxTokens: Int
    ): String = withContext(Dispatchers.IO) {
        val url = URL(OPENAI_API_URL)
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
            }
            
            val requestBody = buildOpenAIRequest(prompt, modelName, temperature, maxTokens)
            
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody)
                writer.flush()
            }
            
            val responseCode = connection.responseCode
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    reader.readText()
                }
                
                extractTextFromOpenAIResponse(response)
            } else {
                val errorResponse = BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { reader ->
                    reader.readText()
                }
                throw Exception("OpenAI API error ($responseCode): $errorResponse")
            }
        } finally {
            connection.disconnect()
        }
    }
    
    private fun buildOpenAIRequest(prompt: String, model: String, temperature: Float, maxTokens: Int): String {
        val request = mapOf(
            "model" to model,
            "messages" to listOf(
                mapOf(
                    "role" to "system",
                    "content" to "You are an expert curriculum designer and educator. Always respond with valid JSON only."
                ),
                mapOf(
                    "role" to "user",
                    "content" to prompt
                )
            ),
            "temperature" to temperature,
            "max_tokens" to maxTokens,
            "response_format" to mapOf("type" to "json_object")
        )
        
        return gson.toJson(request)
    }
    
    private fun extractTextFromOpenAIResponse(response: String): String {
        try {
            val jsonResponse = gson.fromJson(response, Map::class.java)
            
            @Suppress("UNCHECKED_CAST")
            val choices = jsonResponse["choices"] as? List<Map<String, Any>>
            val message = choices?.firstOrNull()?.get("message") as? Map<String, Any>
            val content = message?.get("content") as? String
            
            return content ?: throw Exception("No content found in OpenAI response")
        } catch (e: Exception) {
            throw Exception("Failed to parse OpenAI response: ${e.message}", e)
        }
    }
}
