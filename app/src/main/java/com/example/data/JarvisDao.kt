package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JarvisDao {

    // --- Conversations ---
    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, createdAt DESC")
    fun getAllConversations(): Flow<List<Conversation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: Conversation)

    @Update
    suspend fun updateConversation(conversation: Conversation)

    @Delete
    suspend fun deleteConversation(conversation: Conversation)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversationById(id: String)

    // --- Messages ---
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Update
    suspend fun updateMessage(message: Message)

    @Delete
    suspend fun deleteMessage(message: Message)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    // --- Memory ---
    @Query("SELECT * FROM memory ORDER BY timestamp DESC")
    fun getAllMemoryItems(): Flow<List<MemoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemoryItem(item: MemoryItem)

    @Update
    suspend fun updateMemoryItem(item: MemoryItem)

    @Delete
    suspend fun deleteMemoryItem(item: MemoryItem)

    @Query("DELETE FROM memory WHERE id = :id")
    suspend fun deleteMemoryItemById(id: String)

    @Query("DELETE FROM memory")
    suspend fun clearAllMemory()

    // --- Uploaded Files ---
    @Query("SELECT * FROM uploaded_files ORDER BY timestamp DESC")
    fun getAllUploadedFiles(): Flow<List<UploadedFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUploadedFile(file: UploadedFile)

    @Delete
    suspend fun deleteUploadedFile(file: UploadedFile)

    @Query("DELETE FROM uploaded_files WHERE id = :id")
    suspend fun deleteUploadedFileById(id: String)
}
