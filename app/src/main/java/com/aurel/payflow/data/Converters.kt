package com.aurel.payflow.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromQueueStatus(value: QueueStatus): String = value.name

    @TypeConverter
    fun toQueueStatus(value: String): QueueStatus = QueueStatus.valueOf(value)
}
