package com.example.core.engine.online

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class OnlineAIRequest(
    val prompt: String,
    val systemPrompt: String? = null,
    val model: String = "gpt-4o-mini",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val stream: Boolean = true
)

class OpenAICompatibleClient(
    private var baseUrl: String = "https://api.openai.com/v1",
    private var apiKey: String = ""
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    fun updateConfig(newBaseUrl: String, newApiKey: String) {
        baseUrl = newBaseUrl.trimEnd('/')
        apiKey = newApiKey.trim()
    }

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = if (baseUrl.endsWith("/v1")) "$baseUrl/models" else "$baseUrl/v1/models"
            val requestBuilder = Request.Builder()
                .url(url)
                .get()

            if (apiKey.isNotEmpty()) {
                requestBuilder.header("Authorization", "Bearer $apiKey")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful) {
                Result.success("Connection Successful (HTTP ${response.code})")
            } else {
                Result.failure(Exception("Failed: HTTP ${response.code} ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Connection Error: ${e.localizedMessage ?: "Unknown network error"}"))
        }
    }

    suspend fun streamChatCompletion(
        request: OnlineAIRequest,
        onToken: (String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        val endpoint = if (baseUrl.endsWith("/chat/completions")) {
            baseUrl
        } else if (baseUrl.endsWith("/v1")) {
            "$baseUrl/chat/completions"
        } else {
            "$baseUrl/v1/chat/completions"
        }

        val jsonBody = JSONObject().apply {
            put("model", request.model)
            put("stream", true)
            put("temperature", request.temperature)
            put("max_tokens", request.maxTokens)

            val messages = JSONArray()
            request.systemPrompt?.let { sys ->
                messages.put(JSONObject().apply {
                    put("role", "system")
                    put("content", sys)
                })
            }
            messages.put(JSONObject().apply {
                put("role", "user")
                put("content", request.prompt)
            })
            put("messages", messages)
        }

        val body = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val reqBuilder = Request.Builder()
            .url(endpoint)
            .post(body)
            .header("Content-Type", "application/json")

        if (apiKey.isNotEmpty()) {
            reqBuilder.header("Authorization", "Bearer $apiKey")
        }

        var response: Response? = null
        try {
            response = client.newCall(reqBuilder.build()).execute()
            if (!response.isSuccessful) {
                val errText = response.body?.string() ?: response.message
                return@withContext Result.failure(Exception("API Error (${response.code}): $errText"))
            }

            val responseBody = response.body ?: return@withContext Result.failure(Exception("Empty response body"))
            val reader = BufferedReader(InputStreamReader(responseBody.byteStream()))
            val fullContent = StringBuilder()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: break
                if (currentLine.startsWith("data: ")) {
                    val data = currentLine.removePrefix("data: ").trim()
                    if (data == "[DONE]") break

                    try {
                        val json = JSONObject(data)
                        val choices = json.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val delta = choices.getJSONObject(0).optJSONObject("delta")
                            val contentChunk = delta?.optString("content", "") ?: ""
                            if (contentChunk.isNotEmpty()) {
                                fullContent.append(contentChunk)
                                withContext(Dispatchers.Main) {
                                    onToken(contentChunk)
                                }
                            }
                        }
                    } catch (ignored: Exception) {
                        // Skip malformed chunk
                    }
                }
            }

            Result.success(fullContent.toString())
        } catch (e: CancellationException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(Exception("Network Stream Error: ${e.localizedMessage}"))
        } finally {
            response?.close()
        }
    }
}
