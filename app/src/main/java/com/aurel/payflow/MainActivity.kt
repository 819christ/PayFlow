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
            PayflowScreen()
        }
    }
}

@Composable
fun PayflowScreen() {
    var logs by remember { mutableStateOf("Payflow démarré\n====================") }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Payflow", style = MaterialTheme.typography.headlineLarge)

                Button(onClick = {
                    logs += "\n[INFO] Service démarré manuellement"
                    // On démarrera le service ici plus tard
                }) {
                    Text("Démarrer le Service")
                }

                Button(onClick = {
                    logs += "\n[INFO] Sync des SMS manqués forcée"
                }) {
                    Text("Forcer Sync Delta")
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Logs en temps réel :", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = logs,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
