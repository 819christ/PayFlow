package com.aurel.payflow.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "history_entries")
data class HistoryEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val timestamp: Long,
    val smsText: String,
    val status: QueueStatus = QueueStatus.PENDING,
    val amount: Int = 0,
    val sender: String = "",
    val txId: String = "",
    val ruleName: String = "",
    val errorMessage: String? = null
)
