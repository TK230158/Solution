package com.example.mainproject.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_registrations",
    indices = [
        Index("sync_status"),
        Index("created_at"),
        Index("worker_id")
    ]
)
data class PendingRegistration(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val process_id: Int,
    val division: String,           // "start" or "end"
    val worker_id: Int,
    val device_id: String,
    val registered_at: String,      // ISO8601
    val created_at: String,
    val sync_status: String = "pending", // "pending" / "syncing" / "synced" / "failed"
    val sync_attempted_at: String? = null,
    val retry_count: Int = 0,
    val error_message: String? = null
)
