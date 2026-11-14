package com.hiyako.app.data.db

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isGroup: Boolean = false,
    val groupModelsJson: String = "[]" // تخزين النماذج المشاركة في القروب
)

@Entity(tableName = "chat_messages")
data class ChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val conversationId: Int,
    val role: String, // "user", "ai", "system"
    val content: String,
    val messageType: String = "TEXT", // "TEXT", "IMAGE", "FILE", "AUDIO", "VIDEO"
    val attachmentUri: String? = null,
    val senderName: String? = null, // اسم النموذج في القروب
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForChat(conversationId: Int): Flow<List<ChatEntity>>

    @Insert
    suspend fun insertMessage(message: ChatEntity)

    @Query("DELETE FROM chat_messages WHERE role = 'ai' AND content = 'Thinking...' AND conversationId = :conversationId")
    suspend fun deleteThinkingMessage(conversationId: Int)

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun clearChat(conversationId: Int)
    
    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, timestamp DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Insert
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Query("UPDATE conversations SET title = :newTitle WHERE id = :conversationId")
    suspend fun updateTitle(conversationId: Int, newTitle: String)
    
    @Query("UPDATE conversations SET isPinned = :isPinned WHERE id = :conversationId")
    suspend fun updatePin(conversationId: Int, isPinned: Boolean)

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: Int)
    
    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>
    @Insert
    suspend fun insertMemory(memory: MemoryEntity)
    @Query("DELETE FROM memories")
    suspend fun clearMemories()
}

@Database(entities = [ChatEntity::class, ConversationEntity::class, MemoryEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun conversationDao(): ConversationDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nexus_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
