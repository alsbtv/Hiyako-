package com.hiyako.app.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** A report a user filed for an AI reply. Stored on the device until a moderation backend exists. */
@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val messageId: Int,
    val conversationId: Int,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface ReportDao {
    @Insert
    suspend fun insert(report: ReportEntity)
}

/** Adds the reports table. Must match the ReportEntity column definitions exactly. */
val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `reports` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`messageId` INTEGER NOT NULL, " +
                "`conversationId` INTEGER NOT NULL, " +
                "`content` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL)"
        )
    }
}
