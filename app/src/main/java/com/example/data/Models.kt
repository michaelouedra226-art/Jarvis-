package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey val id: String,
    val title: String,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "sent", // "sending", "sent", "error"
    val isRegenerated: Boolean = false,
    val attachedFileName: String? = null,
    val attachedFileType: String? = null,
    val attachedFileContent: String? = null // Base64 or plain text
)

@Entity(tableName = "memory")
data class MemoryItem(
    @PrimaryKey val id: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "uploaded_files")
data class UploadedFile(
    @PrimaryKey val id: String,
    val name: String,
    val mimeType: String,
    val size: String,
    val content: String, // Base64 or plain text
    val timestamp: Long = System.currentTimeMillis()
)
