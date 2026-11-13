package com.hiyako.app.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// جدول المحادثات الجديد
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)

// جدول الرسائل (تمت إضافة conversationId لربطه بالمحادثة)
@Entity(tableName = "chat_messages")
data class ChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val conversationId: Int, // الربط بالمحادثة
    val role: String, // "user" or "ai"
    val content: String,
    val messageType: String = "TEXT", // "TEXT", "IMAGE", "VIDEO"
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
    suspend fun clearAllChats()
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

@Database(entities = [ChatEntity::class, ConversationEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun conversationDao(): ConversationDao

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
