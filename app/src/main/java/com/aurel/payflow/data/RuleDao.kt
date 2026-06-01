package com.aurel.payflow.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM routing_rules ORDER BY name ASC")
    fun getAllRulesFlow(): Flow<List<RoutingRule>>

    @Query("SELECT * FROM routing_rules")
    suspend fun getAllRules(): List<RoutingRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: RoutingRule)

    @Update
    suspend fun update(rule: RoutingRule)

    @Delete
    suspend fun delete(rule: RoutingRule)
}
