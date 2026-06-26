package com.example.data

import kotlinx.coroutines.flow.Flow

class JarvisRepository(private val jarvisDao: JarvisDao) {

    val allConversations: Flow<List<Conversation>> = jarvisDao.getAllConversations()
    val allMemoryItems: Flow<List<MemoryItem>> = jarvisDao.getAllMemoryItems()
    val allUploadedFiles: Flow<List<UploadedFile>> = jarvisDao.getAllUploadedFiles()

    fun getMessagesForConversation(conversationId: String): Flow<List<Message>> {
        return jarvisDao.getMessagesForConversation(conversationId)
    }

    suspend fun insertConversation(conversation: Conversation) {
        jarvisDao.insertConversation(conversation)
    }

    suspend fun updateConversation(conversation: Conversation) {
        jarvisDao.updateConversation(conversation)
    }

    suspend fun deleteConversation(conversation: Conversation) {
        jarvisDao.deleteConversation(conversation)
    }

    suspend fun deleteConversationById(id: String) {
        jarvisDao.deleteConversationById(id)
        jarvisDao.deleteMessagesForConversation(id)
    }

    suspend fun insertMessage(message: Message) {
        jarvisDao.insertMessage(message)
    }

    suspend fun updateMessage(message: Message) {
        jarvisDao.updateMessage(message)
    }

    suspend fun deleteMessage(message: Message) {
        jarvisDao.deleteMessage(message)
    }

    suspend fun deleteMessageById(messageId: String) {
        jarvisDao.deleteMessageById(messageId)
    }

    suspend fun insertMemoryItem(item: MemoryItem) {
        jarvisDao.insertMemoryItem(item)
    }

    suspend fun updateMemoryItem(item: MemoryItem) {
        jarvisDao.updateMemoryItem(item)
    }

    suspend fun deleteMemoryItem(item: MemoryItem) {
        jarvisDao.deleteMemoryItem(item)
    }

    suspend fun deleteMemoryItemById(id: String) {
        jarvisDao.deleteMemoryItemById(id)
    }

    suspend fun clearAllMemory() {
        jarvisDao.clearAllMemory()
    }

    suspend fun insertUploadedFile(file: UploadedFile) {
        jarvisDao.insertUploadedFile(file)
    }

    suspend fun deleteUploadedFile(file: UploadedFile) {
        jarvisDao.deleteUploadedFile(file)
    }

    suspend fun deleteUploadedFileById(id: String) {
        jarvisDao.deleteUploadedFileById(id)
    }
}
