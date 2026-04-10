package com.example.mainproject.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_registration_items",
    indices = [
        Index("registration_id"),
        Index("product_number")
    ],
    foreignKeys = [
        ForeignKey(
            entity = PendingRegistration::class,
            parentColumns = ["id"],
            childColumns = ["registration_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PendingRegistrationItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val registration_id: Long,
    val product_number: String,     // 例: BISb30N-7A
    val display_order: Int,
    val created_at: String
)
