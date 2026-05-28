package com.aurel.payflow

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PayflowScreen(this)
        }
    }
}

@Composable
fun PayflowScreen(activity: MainActivity) {
    var logs by remember { mutableStateOf("🚀 Payflow démarré\n====================\n") }

    fun addLog(message: String) {
        logs += "[${java.time.LocalTime.now()}] $message\n"
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Payflow", style = MaterialTheme.typography.headlineLarge)

                Button(onClick = {
                    val intent = Intent(activity, SmsForegroundService::class.java)
                    activity.startForegroundService(intent)
                    addLog("✅ Service démarré manuellement")
                }) {
                    Text("▶️ Démarrer le Service")
                }

                Button(onClick = {
                    addLog("🔄 Sync delta des SMS manqués forcée")
                    // On implémentera plus tard
                }) {
                    Text("🔄 Forcer Sync Delta")
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Logs en temps réel :", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = logs,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Text("Appuyez sur Démarrer le Service pour tester", 
                     style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
