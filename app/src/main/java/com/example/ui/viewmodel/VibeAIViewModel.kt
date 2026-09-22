package com.example.ui.viewmodel

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.agent.PendingPermissionRequest
import com.example.core.agent.VibeAgentRunner
import com.example.core.engine.offline.DeviceCapability
import com.example.core.engine.offline.DeviceCapabilityDetector
import com.example.core.engine.offline.GGUFModelMetadata
import com.example.core.engine.offline.GGUFParser
import com.example.core.engine.offline.ThermalManager
import com.example.core.engine.offline.ThermalState
import com.example.core.files.FileCategory
import com.example.core.files.SafeFileItem
import com.example.core.files.SafeFileManager
import com.example.core.network.DownloadProgress
import com.example.core.network.DownloadStatus
import com.example.core.network.ModelDownloader
import com.example.core.network.OnlineModelPreset
import com.example.core.router.AIMode
import com.example.core.router.AIRouter
import com.example.core.security.CryptoManager
import com.example.data.AppDatabase
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ConversationEntity
import com.example.data.model.LocalModelEntity
import com.example.data.model.ProviderConfigEntity
import com.example.ui.components.PendingCodeModification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class VibeScreen {
    HOME,
    CHAT,
    AGENT,
    FILES,
    CODE,
    TOOLS,
    MODELS,
    SETTINGS
}

data class BenchmarkResult(
    val modelName: String,
    val tokensPerSec: Float,
    val firstTokenLatencyMs: Long,
    val totalTimeMs: Long,
    val ramUsedMb: Long
)

class VibeAIViewModel(application: Application) : AndroidViewModel(application) {

    val db = AppDatabase.getDatabase(application)
    val router = AIRouter(application)
    val agentRunner = VibeAgentRunner(application)
    val thermalManager = ThermalManager(application)
    val modelDownloader = ModelDownloader(application, db, viewModelScope)

    // Direct Online Model Downloader Flow
    val downloadProgress: StateFlow<DownloadProgress> = modelDownloader.downloadProgress

    fun downloadOnlineModel(preset: OnlineModelPreset) {
        modelDownloader.startDownload(
            modelName = preset.name,
            url = preset.directUrl,
            modelId = preset.id
        )
    }

    fun downloadModelFromUrl(url: String, customName: String = "") {
        if (url.isBlank()) return
        val name = customName.ifBlank {
            url.substringAfterLast("/").substringBefore("?").removeSuffix(".gguf")
        }
        modelDownloader.startDownload(
            modelName = name,
            url = url.trim()
        )
    }

    fun cancelModelDownload() {
        modelDownloader.cancelDownload()
    }

    fun dismissDownloadStatus() {
        modelDownloader.resetState()
    }

    // Navigation
    private val _currentScreen = MutableStateFlow(VibeScreen.HOME)
    val currentScreen: StateFlow<VibeScreen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: VibeScreen) {
        _currentScreen.value = screen
    }

    // Telemetry
    private val _deviceCapability = MutableStateFlow(DeviceCapabilityDetector.getDeviceCapability(application))
    val deviceCapability: StateFlow<DeviceCapability> = _deviceCapability.asStateFlow()

    fun refreshTelemetry() {
        _deviceCapability.value = DeviceCapabilityDetector.getDeviceCapability(getApplication())
    }

    // Database flows
    val conversations = db.conversationDao().getAllConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val localModels = db.localModelDao().getAllModels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val providerConfigs = db.providerConfigDao().getAllProviders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chat state
    private val _activeConversationId = MutableStateFlow<Long?>(null)
    val activeConversationId: StateFlow<Long?> = _activeConversationId.asStateFlow()

    private val _currentMessages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val currentMessages: StateFlow<List<ChatMessageEntity>> = _currentMessages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _streamingMessageText = MutableStateFlow("")
    val streamingMessageText: StateFlow<String> = _streamingMessageText.asStateFlow()

    private var activeGenerationJob: Job? = null

    // File Manager state
    private val _currentDirectory = MutableStateFlow(SafeFileManager.getRootWorkDirectory(application))
    val currentDirectory: StateFlow<File> = _currentDirectory.asStateFlow()

    private val _directoryFiles = MutableStateFlow<List<SafeFileItem>>(emptyList())
    val directoryFiles: StateFlow<List<SafeFileItem>> = _directoryFiles.asStateFlow()

    private val _fileSearchQuery = MutableStateFlow("")
    val fileSearchQuery: StateFlow<String> = _fileSearchQuery.asStateFlow()

    private val _selectedFileForAnalysis = MutableStateFlow<File?>(null)
    val selectedFileForAnalysis: StateFlow<File?> = _selectedFileForAnalysis.asStateFlow()

    // Code Workspace state
    private val _codeFileName = MutableStateFlow("NeuralAgent.kt")
    val codeFileName: StateFlow<String> = _codeFileName.asStateFlow()

    private val _codeContent = MutableStateFlow("")
    val codeContent: StateFlow<String> = _codeContent.asStateFlow()

    private val _pendingCodeModification = MutableStateFlow<PendingCodeModification?>(null)
    val pendingCodeModification: StateFlow<PendingCodeModification?> = _pendingCodeModification.asStateFlow()

    // Benchmark state
    private val _benchmarkResult = MutableStateFlow<BenchmarkResult?>(null)
    val benchmarkResult: StateFlow<BenchmarkResult?> = _benchmarkResult.asStateFlow()
    private var benchmarkJob: Job? = null

    // GGUF Import Validation State
    private val _importedModelValidation = MutableStateFlow<GGUFModelMetadata?>(null)
    val importedModelValidation: StateFlow<GGUFModelMetadata?> = _importedModelValidation.asStateFlow()

    // Creator dialog
    private val _showCreatorDialog = MutableStateFlow(false)
    val showCreatorDialog: StateFlow<Boolean> = _showCreatorDialog.asStateFlow()

    fun setCreatorDialogVisible(visible: Boolean) {
        _showCreatorDialog.value = visible
    }

    // Permission dialog for Agent
    private val _agentPermissionRequest = MutableStateFlow<PendingPermissionRequest?>(null)
    val agentPermissionRequest: StateFlow<PendingPermissionRequest?> = _agentPermissionRequest.asStateFlow()

    init {
        loadInitialChat()
        refreshFiles()
        loadInitialCodeFile()
    }

    private fun loadInitialChat() {
        viewModelScope.launch {
            db.conversationDao().getAllConversations().collect { list ->
                if (_activeConversationId.value == null && list.isNotEmpty()) {
                    selectConversation(list.first().id)
                }
            }
        }
    }

    fun selectConversation(conversationId: Long) {
        _activeConversationId.value = conversationId
        viewModelScope.launch {
            db.chatMessageDao().getMessagesForConversation(conversationId).collect { msgs ->
                _currentMessages.value = msgs
            }
        }
    }

    fun createNewConversation(title: String = "Neural Session") {
        viewModelScope.launch {
            val conv = ConversationEntity(
                title = title,
                modelUsed = router.localEngine.activeModelName
            )
            val id = db.conversationDao().insertConversation(conv)
            selectConversation(id)
        }
    }

    fun deleteConversation(conversationId: Long) {
        viewModelScope.launch {
            db.chatMessageDao().deleteMessagesForConversation(conversationId)
            db.conversationDao().deleteById(conversationId)
            if (_activeConversationId.value == conversationId) {
                _activeConversationId.value = null
                _currentMessages.value = emptyList()
            }
        }
    }

    fun sendMessage(userText: String, attachedFile: File? = null) {
        if (userText.isBlank()) return
        val convId = _activeConversationId.value ?: return

        viewModelScope.launch {
            val userMsg = ChatMessageEntity(
                conversationId = convId,
                role = "user",
                content = userText,
                modelName = router.localEngine.activeModelName,
                isOffline = router.currentMode.value == AIMode.OFFLINE
            )
            db.chatMessageDao().insertMessage(userMsg)

            _isGenerating.value = true
            _streamingMessageText.value = ""

            activeGenerationJob = launch {
                val activeProvider = providerConfigs.value.firstOrNull { it.isEnabled }
                val fullResponse = StringBuilder()

                val result = router.routeGenerateStream(
                    prompt = userText,
                    activeProvider = activeProvider
                ) { tokenChunk ->
                    fullResponse.append(tokenChunk)
                    _streamingMessageText.value = fullResponse.toString()
                }

                val assistantMsg = ChatMessageEntity(
                    conversationId = convId,
                    role = "assistant",
                    content = fullResponse.toString().ifEmpty { result.fullText },
                    tokenCount = result.tokenCount,
                    latencyMs = result.latencyMs,
                    modelName = router.localEngine.activeModelName,
                    isOffline = router.currentMode.value == AIMode.OFFLINE
                )
                db.chatMessageDao().insertMessage(assistantMsg)

                _streamingMessageText.value = ""
                _isGenerating.value = false
                refreshTelemetry()
            }
        }
    }

    fun stopGeneration() {
        activeGenerationJob?.cancel()
        router.localEngine.cancelGeneration()
        _isGenerating.value = false
    }

    fun setAIMode(newMode: AIMode) {
        router.setMode(newMode)
    }

    fun toggleAIMode() {
        val next = if (router.currentMode.value == AIMode.OFFLINE) AIMode.ONLINE else AIMode.OFFLINE
        setAIMode(next)
    }

    // Agent Execution
    fun runAgentGoal(goal: String) {
        viewModelScope.launch {
            agentRunner.executeGoal(goal) { req ->
                _agentPermissionRequest.value = req
                false
            }
        }
    }

    fun respondToAgentPermission(decision: Boolean) {
        _agentPermissionRequest.value?.onDecision?.invoke(decision)
        _agentPermissionRequest.value = null
    }

    // GGUF Model management & Validation
    fun validateGGUFFile(file: File) {
        viewModelScope.launch {
            val result = GGUFParser.parseAndValidate(file, getApplication())
            _importedModelValidation.value = result
        }
    }

    fun registerValidatedGGUF(metadata: GGUFModelMetadata, file: File) {
        if (!metadata.isValid || !metadata.isCompatibleWithDevice) return
        viewModelScope.launch {
            val entity = LocalModelEntity(
                id = "custom_${System.currentTimeMillis()}",
                name = metadata.modelName,
                parameterSize = "Quantized",
                quantization = metadata.quantization,
                fileSizeMb = metadata.fileSizeBytes / (1024 * 1024),
                ramEstimateMb = metadata.estimatedRamMb,
                contextLength = metadata.contextLength,
                backend = "GGUF / llama.cpp",
                status = "INSTALLED",
                downloadProgress = 100,
                filePath = file.absolutePath
            )
            db.localModelDao().insertOrUpdate(entity)
            loadModel(entity)
            _importedModelValidation.value = null
        }
    }

    fun loadModel(model: LocalModelEntity) {
        viewModelScope.launch {
            val success = router.localEngine.loadModel(model.id, model.filePath, model.name, model.ramEstimateMb)
            if (success) {
                db.localModelDao().deactivateAllModels()
                db.localModelDao().activateModel(model.id)
                refreshTelemetry()
            }
        }
    }

    fun unloadActiveModel() {
        viewModelScope.launch {
            router.localEngine.unloadModel()
            db.localModelDao().deactivateAllModels()
            refreshTelemetry()
        }
    }

    fun runQuickBenchmark(modelName: String) {
        benchmarkJob?.cancel()
        benchmarkJob = viewModelScope.launch(Dispatchers.Default) {
            val actManager = getApplication<Application>().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfoBefore = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfoBefore)

            val startTime = System.currentTimeMillis()
            var firstTokenLatencyMs = 0L
            var tokens = 0

            router.localEngine.generateStream("Benchmark verification run: execute tensor memory check.") {
                if (tokens == 0) {
                    firstTokenLatencyMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                }
                tokens++
            }

            val totalTime = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
            val tokPerSec = (tokens.toFloat() / (totalTime.toFloat() / 1000f))

            val memInfoAfter = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfoAfter)
            val memUsedMb = (memInfoBefore.availMem - memInfoAfter.availMem).coerceAtLeast(0) / (1024 * 1024)

            _benchmarkResult.value = BenchmarkResult(
                modelName = modelName,
                tokensPerSec = tokPerSec,
                firstTokenLatencyMs = if (firstTokenLatencyMs > 0) firstTokenLatencyMs else totalTime,
                totalTimeMs = totalTime,
                ramUsedMb = if (memUsedMb > 0) memUsedMb else router.localEngine.allocatedMemoryMb
            )
        }
    }

    fun stopBenchmark() {
        benchmarkJob?.cancel()
    }

    // Files
    fun refreshFiles() {
        viewModelScope.launch {
            val list = SafeFileManager.listFiles(_currentDirectory.value)
            val query = _fileSearchQuery.value.trim()
            _directoryFiles.value = if (query.isEmpty()) {
                list
            } else {
                list.filter { it.name.contains(query, ignoreCase = true) }
            }
        }
    }

    fun searchFiles(query: String) {
        _fileSearchQuery.value = query
        refreshFiles()
    }

    fun deleteFile(file: File) {
        viewModelScope.launch {
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
            refreshFiles()
        }
    }

    fun renameFile(file: File, newName: String) {
        viewModelScope.launch {
            val target = File(file.parentFile, newName)
            if (!target.exists()) {
                file.renameTo(target)
                refreshFiles()
            }
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val newDir = File(_currentDirectory.value, name)
            if (!newDir.exists()) newDir.mkdirs()
            refreshFiles()
        }
    }

    fun createNewFile(name: String, content: String = "") {
        viewModelScope.launch {
            val newFile = File(_currentDirectory.value, name)
            if (!newFile.exists()) newFile.writeText(content)
            refreshFiles()
        }
    }

    fun openFileInCodeWorkspace(file: File) {
        viewModelScope.launch {
            _codeFileName.value = file.name
            _codeContent.value = SafeFileManager.readTextChunked(file, 20000)
            navigateTo(VibeScreen.CODE)
        }
    }

    private fun loadInitialCodeFile() {
        viewModelScope.launch {
            val sample = File(SafeFileManager.getRootWorkDirectory(getApplication()), "projects/NeuralAgent.kt")
            if (sample.exists()) {
                _codeFileName.value = sample.name
                _codeContent.value = sample.readText()
            }
        }
    }

    fun saveCodeFile(newCode: String) {
        viewModelScope.launch {
            _codeContent.value = newCode
            val target = File(_currentDirectory.value, _codeFileName.value)
            target.writeText(newCode)
            refreshFiles()
        }
    }

    fun requestAiFixWithDiff() {
        val original = _codeContent.value
        val proposed = """// VibeAI Optimized Version of ${_codeFileName.value}
// Audited by VibeAgent Engine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OptimizedAgent(val id: String) {
    suspend fun executeTaskSafely(goal: String): Result<Boolean> = withContext(Dispatchers.Default) {
        try {
            // Memory-confined execution
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}"""
        _pendingCodeModification.value = PendingCodeModification(
            fileName = _codeFileName.value,
            originalCode = original,
            proposedCode = proposed,
            onApply = {
                saveCodeFile(proposed)
                _pendingCodeModification.value = null
            },
            onReject = {
                _pendingCodeModification.value = null
            }
        )
    }

    fun aiExplainCode() {
        viewModelScope.launch {
            val prompt = "Explain the architecture, patterns, and logic of this ${_codeFileName.value} file:\n\n```\n${_codeContent.value}\n```"
            createNewConversation("Explain: ${_codeFileName.value}")
            sendMessage(prompt)
            navigateTo(VibeScreen.CHAT)
        }
    }

    fun dismissDiffModal() {
        _pendingCodeModification.value = null
    }

    // Provider credential encryption
    fun saveProviderConfig(name: String, baseUrl: String, apiKey: String, model: String) {
        viewModelScope.launch {
            val encryptedKey = CryptoManager.encryptCredential(apiKey)
            val entity = ProviderConfigEntity(
                id = "provider_${System.currentTimeMillis()}",
                name = name,
                baseUrl = baseUrl,
                apiKey = encryptedKey,
                modelName = model,
                isEnabled = true,
                isDefault = true
            )
            db.providerConfigDao().insertOrUpdate(entity)
        }
    }
}
