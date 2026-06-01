package com.aurel.payflow.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "routing_rules")
data class RoutingRule(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val senderFilter: String = "",
    val keywords: String = "",      // mots-cles separes par des virgules
    val amountKeyword: String = "", // mot-cle qui precede le montant dans le SMS
    val webhookUrl: String = "",    // URL specifique a cette regle (vide = URL globale)
    val accessKey: String = "",     // Cle specifique (vide = cle globale)
    val enabled: Boolean = true
)
