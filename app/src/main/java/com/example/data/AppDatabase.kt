package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.ChatMessageDao
import com.example.data.dao.ConversationDao
import com.example.data.dao.LocalModelDao
import com.example.data.dao.ProviderConfigDao
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ConversationEntity
import com.example.data.model.LocalModelEntity
import com.example.data.model.ProviderConfigEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ConversationEntity::class,
        ChatMessageEntity::class,
        LocalModelEntity::class,
        ProviderConfigEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun localModelDao(): LocalModelDao
    abstract fun providerConfigDao(): ProviderConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vibe_ai_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }

            private suspend fun populateInitialData(database: AppDatabase) {
                val modelDao = database.localModelDao()
                val initialModels = listOf(
                    LocalModelEntity(
                        id = "tinyllama-1.1b",
                        name = "TinyLlama 1.1B Chat",
                        parameterSize = "1.1B",
                        quantization = "Q4_K_M",
                        fileSizeMb = 669,
                        ramEstimateMb = 900,
                        contextLength = 2048,
                        backend = "GGUF / llama.cpp",
                        status = "NOT_DOWNLOADED",
                        downloadProgress = 0,
                        isDefault = false
                    ),
                    LocalModelEntity(
                        id = "qwen2.5-coder-1.5b",
                        name = "Qwen 2.5 Coder 1.5B",
                        parameterSize = "1.5B",
                        quantization = "Q4_K_M",
                        fileSizeMb = 980,
                        ramEstimateMb = 1400,
                        contextLength = 4096,
                        backend = "GGUF / llama.cpp",
                        status = "NOT_DOWNLOADED",
                        downloadProgress = 0,
                        isDefault = false
                    ),
                    LocalModelEntity(
                        id = "gemma-2b-it",
                        name = "Gemma 2B Instruct",
                        parameterSize = "2B",
                        quantization = "Q4_0",
                        fileSizeMb = 1400,
                        ramEstimateMb = 1800,
                        contextLength = 2048,
                        backend = "GGUF / llama.cpp",
                        status = "NOT_DOWNLOADED",
                        downloadProgress = 0,
                        isDefault = false
                    ),
                    LocalModelEntity(
                        id = "phi3-mini-4k",
                        name = "Phi-3 Mini 3.8B Instruct",
                        parameterSize = "3.8B",
                        quantization = "Q4_K_M",
                        fileSizeMb = 2390,
                        ramEstimateMb = 3100,
                        contextLength = 4096,
                        backend = "GGUF / llama.cpp",
                        status = "NOT_DOWNLOADED",
                        downloadProgress = 0,
                        isDefault = false
                    ),
                    LocalModelEntity(
                        id = "deepseek-coder-1.3b",
                        name = "DeepSeek Coder 1.3B",
                        parameterSize = "1.3B",
                        quantization = "Q4_K_S",
                        fileSizeMb = 820,
                        ramEstimateMb = 1200,
                        contextLength = 4096,
                        backend = "GGUF / llama.cpp",
                        status = "NOT_DOWNLOADED",
                        downloadProgress = 0,
                        isDefault = false
                    )
                )
                modelDao.insertAll(initialModels)

                val providerDao = database.providerConfigDao()
                val initialProviders = listOf(
                    ProviderConfigEntity(
                        id = "openai-official",
                        name = "OpenAI API",
                        baseUrl = "https://api.openai.com/v1",
                        apiKey = "",
                        modelName = "gpt-4o-mini",
                        isEnabled = true,
                        isDefault = true
                    ),
                    ProviderConfigEntity(
                        id = "groq-fast",
                        name = "Groq Cloud",
                        baseUrl = "https://api.groq.com/openai/v1",
                        apiKey = "",
                        modelName = "llama-3.1-8b-instant",
                        isEnabled = true,
                        isDefault = false
                    ),
                    ProviderConfigEntity(
                        id = "local-ollama",
                        name = "Local Ollama / LAN",
                        baseUrl = "http://192.168.1.100:11434/v1",
                        apiKey = "ollama",
                        modelName = "llama3:latest",
                        isEnabled = false,
                        isDefault = false
                    )
                )
                providerDao.insertAll(initialProviders)

                // Create initial welcoming conversation
                val convDao = database.conversationDao()
                val convId = convDao.insertConversation(
                    ConversationEntity(
                        title = "Welcome to VibeAI Workstation",
                        modelUsed = "TinyLlama 1.1B Chat",
                        isPinned = true
                    )
                )
                val msgDao = database.chatMessageDao()
                msgDao.insertMessage(
                    ChatMessageEntity(
                        conversationId = convId,
                        role = "assistant",
                        content = """# VibeAI — Your Pocket AI Agent
Welcome to your offline cyberpunk AI workstation, built by **Shorif Uddin Piash**.

### Capabilities:
- **Offline LLM Engine**: Fast local neural reasoning without internet.
- **Online OpenAI-Compatible Provider**: Connect any OpenAI, Groq, or LAN endpoint.
- **Autonomous Agent Mode**: Plan, scan projects, analyze code, and execute multi-step tool pipelines safely.
- **Code Workspace & File Manager**: Integrated syntax viewer, safe ZIP tools, and AES file encryption.

Select a model above or try asking:
- *"Analyze my code for potential bugs"*
- *"Run an autonomous Agent task on my project"*
- *"Explain how offline GGUF inference works on mobile"*""",
                        modelName = "TinyLlama 1.1B Chat",
                        isOffline = true,
                        tokenCount = 142,
                        latencyMs = 380
                    )
                )
            }
        }
    }
}
