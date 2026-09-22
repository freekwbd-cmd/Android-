package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val modelUsed: String = "TinyLlama-1.1B",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false
)

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversationId"])]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val attachmentsJson: String = "[]",
    val tokenCount: Int = 0,
    val latencyMs: Long = 0,
    val modelName: String = "",
    val isOffline: Boolean = true
)

@Entity(tableName = "local_models")
data class LocalModelEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val parameterSize: String, // e.g. "1.1B", "2B", "3.8B"
    val quantization: String, // e.g. "Q4_K_M", "Q4_0"
    val fileSizeMb: Long,
    val ramEstimateMb: Long,
    val contextLength: Int,
    val backend: String = "GGUF / llama.cpp",
    val status: String, // "ACTIVE", "INSTALLED", "NOT_DOWNLOADED", "DOWNLOADING"
    val downloadProgress: Int = 0,
    val isDefault: Boolean = false,
    val filePath: String? = null
)

@Entity(tableName = "provider_configs")
data class ProviderConfigEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val modelName: String,
    val isEnabled: Boolean = true,
    val isDefault: Boolean = false,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048
)
