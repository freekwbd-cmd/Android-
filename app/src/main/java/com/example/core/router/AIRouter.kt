package com.example.core.router

import android.content.Context
import com.example.core.engine.offline.GenerationResult
import com.example.core.engine.offline.InferenceParameters
import com.example.core.engine.offline.LocalLLMEngine
import com.example.core.engine.offline.VibeLocalLLMEngine
import com.example.core.engine.online.OnlineAIRequest
import com.example.core.engine.online.OpenAICompatibleClient
import com.example.data.model.ProviderConfigEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AIMode {
    OFFLINE,
    ONLINE
}

class AIRouter(private val context: Context) {

    val localEngine: LocalLLMEngine = VibeLocalLLMEngine(context)
    val onlineClient: OpenAICompatibleClient = OpenAICompatibleClient()

    private val _currentMode = MutableStateFlow(AIMode.OFFLINE)
    val currentMode: StateFlow<AIMode> = _currentMode.asStateFlow()

    private val _is100PercentLocalPrivacy = MutableStateFlow(false)
    val is100PercentLocalPrivacy: StateFlow<Boolean> = _is100PercentLocalPrivacy.asStateFlow()

    fun setMode(mode: AIMode) {
        if (_is100PercentLocalPrivacy.value && mode == AIMode.ONLINE) {
            // Cannot enable online when 100% Local Mode privacy is locked
            return
        }
        _currentMode.value = mode
    }

    fun set100PercentLocalPrivacy(enabled: Boolean) {
        _is100PercentLocalPrivacy.value = enabled
        if (enabled) {
            _currentMode.value = AIMode.OFFLINE
        }
    }

    fun configureOnlineProvider(provider: ProviderConfigEntity) {
        onlineClient.updateConfig(provider.baseUrl, provider.apiKey)
    }

    suspend fun routeGenerateStream(
        prompt: String,
        systemPrompt: String? = null,
        activeProvider: ProviderConfigEntity? = null,
        onToken: (String) -> Unit
    ): GenerationResult {
        if (_currentMode.value == AIMode.ONLINE && !_is100PercentLocalPrivacy.value && activeProvider != null) {
            val startTime = System.currentTimeMillis()
            onlineClient.updateConfig(activeProvider.baseUrl, activeProvider.apiKey)
            val req = OnlineAIRequest(
                prompt = prompt,
                systemPrompt = systemPrompt,
                model = activeProvider.modelName,
                temperature = activeProvider.temperature,
                maxTokens = activeProvider.maxTokens
            )

            val fullTextBuilder = StringBuilder()
            val result = onlineClient.streamChatCompletion(req) { token ->
                fullTextBuilder.append(token)
                onToken(token)
            }

            val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(10)
            return if (result.isSuccess) {
                val fullText = fullTextBuilder.toString()
                val wordCount = fullText.split(" ").size.coerceAtLeast(1)
                GenerationResult(
                    fullText = fullText,
                    tokenCount = wordCount,
                    latencyMs = latency,
                    tokensPerSecond = (wordCount.toFloat() / (latency.toFloat() / 1000f)).coerceAtLeast(10f)
                )
            } else {
                // Graceful fallback to offline engine if online fails!
                val fallbackNotice = "\n\n*(Online API unreachable: ${result.exceptionOrNull()?.message}. Falling back to Offline Local Engine)*\n\n"
                onToken(fallbackNotice)
                localEngine.generateStream(prompt, systemPrompt, onToken = onToken)
            }
        } else {
            // Offline Mode
            return localEngine.generateStream(prompt, systemPrompt, onToken = onToken)
        }
    }
}
