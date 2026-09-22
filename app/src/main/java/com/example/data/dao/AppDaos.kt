package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ConversationEntity
import com.example.data.model.LocalModelEntity
import com.example.data.model.ProviderConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: Long): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Delete
    suspend fun deleteConversation(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: Long): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Update
    suspend fun updateMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: Long)
}

@Dao
interface LocalModelDao {
    @Query("SELECT * FROM local_models ORDER BY isDefault DESC, name ASC")
    fun getAllModels(): Flow<List<LocalModelEntity>>

    @Query("SELECT * FROM local_models WHERE id = :id LIMIT 1")
    suspend fun getModelById(id: String): LocalModelEntity?

    @Query("SELECT * FROM local_models WHERE status = 'ACTIVE' LIMIT 1")
    fun getActiveModel(): Flow<LocalModelEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(model: LocalModelEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(models: List<LocalModelEntity>)

    @Update
    suspend fun update(model: LocalModelEntity)

    @Delete
    suspend fun delete(model: LocalModelEntity)

    @Query("UPDATE local_models SET status = 'INSTALLED' WHERE status = 'ACTIVE'")
    suspend fun deactivateAllModels()

    @Query("UPDATE local_models SET status = 'ACTIVE' WHERE id = :id")
    suspend fun activateModel(id: String)
}

@Dao
interface ProviderConfigDao {
    @Query("SELECT * FROM provider_configs")
    fun getAllProviders(): Flow<List<ProviderConfigEntity>>

    @Query("SELECT * FROM provider_configs WHERE isDefault = 1 LIMIT 1")
    fun getDefaultProvider(): Flow<ProviderConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(provider: ProviderConfigEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(providers: List<ProviderConfigEntity>)

    @Delete
    suspend fun delete(provider: ProviderConfigEntity)
}
