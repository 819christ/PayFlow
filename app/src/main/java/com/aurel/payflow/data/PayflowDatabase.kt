package com.aurel.payflow.data

import android.content.Context
import androidx.room.*

@Database(
    entities = [RoutingRule::class, HistoryEntry::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PayflowDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile private var INSTANCE: PayflowDatabase? = null

        fun getDatabase(context: Context): PayflowDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    PayflowDatabase::class.java,
                    "payflow_db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
