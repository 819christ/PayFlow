package com.aurel.payflow.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.aurel.payflow.PayflowApp
import com.aurel.payflow.data.HistoryEntry
import com.aurel.payflow.data.QueueStatus
import com.aurel.payflow.data.RoutingRule
import com.aurel.payflow.parser.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class SmsForegroundService : Service() {

    private val CHANNEL_ID = "payflow_sms_channel"
    private val NOTIFICATION_ID = 1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        Log.d("Payflow", "Service Payflow demarre en foreground")
        
        if (hasReadSmsPermission()) {
            syncMissedSms()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val actionVal = it.action ?: it.getStringExtra("action")
            if (actionVal == "NEW_SMS") {
                val sender = it.getStringExtra("sender") ?: "Inconnu"
                val body = it.getStringExtra("body") ?: ""
                val timestamp = it.getLongExtra("timestamp", System.currentTimeMillis())
                
                Log.d("Payflow", "Nouveau SMS recu dans le service - De: $sender")
                processSms(sender, body, timestamp)
            } else {
                if (hasReadSmsPermission()) {
                    syncMissedSms()
                }
            }
        }
        return START_STICKY
    }

    private fun processSms(sender: String, body: String, timestamp: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = PayflowApp.database
                val historyDao = db.historyDao()
                val ruleDao = db.ruleDao()
                
                val rules = ruleDao.getAllRules().filter { it.enabled }
                var matchedAny = false
                
                for (rule in rules) {
                    if (SmsParser.matches(body, rule.keywords, rule.senderFilter)) {
                        val amount = SmsParser.extractAmount(body, rule.amountKeyword)
                        val txId = SmsParser.extractTxId(body)
                        
                        val entry = HistoryEntry(
                            timestamp = timestamp,
                            smsText = body,
                            status = QueueStatus.PENDING,
                            amount = amount,
                            sender = sender,
                            txId = txId,
                            ruleName = rule.name
                        )
                        historyDao.insert(entry)
                        matchedAny = true
                    }
                }
                
                if (matchedAny) {
                    triggerWorkManager()
                }
            } catch (e: Exception) {
                Log.e("Payflow", "Erreur lors du traitement du SMS", e)
            }
        }
    }

    private fun syncMissedSms() {
        val prefs = getSharedPreferences("PayflowPrefs", Context.MODE_PRIVATE)
        val monitorActive = prefs.getBoolean("monitor_active", true)
        if (!monitorActive) return

        val startDateStr = prefs.getString("start_date", "") ?: ""
        var limitTimestamp = 0L
        if (startDateStr.isNotEmpty()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val date = sdf.parse(startDateStr)
                if (date != null) {
                    limitTimestamp = date.time
                }
            } catch (e: Exception) {
                Log.e("Payflow", "Erreur parsing date de depart", e)
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uri = Uri.parse("content://sms/inbox")
                val projection = arrayOf("address", "body", "date")
                val selection = "date >= ?"
                val selectionArgs = arrayOf(limitTimestamp.toString())
                val cursor = contentResolver.query(uri, projection, selection, selectionArgs, "date ASC")
                
                cursor?.use { c ->
                    val addressIdx = c.getColumnIndexOrThrow("address")
                    val bodyIdx = c.getColumnIndexOrThrow("body")
                    val dateIdx = c.getColumnIndexOrThrow("date")
                    
                    val db = PayflowApp.database
                    val historyDao = db.historyDao()
                    val ruleDao = db.ruleDao()
                    
                    val rules = ruleDao.getAllRules().filter { it.enabled }
                    var newSmsCount = 0

                    while (c.moveToNext()) {
                        val sender = c.getString(addressIdx) ?: "Inconnu"
                        val body = c.getString(bodyIdx) ?: ""
                        val timestamp = c.getLong(dateIdx)
                        
                        val exists = historyDao.exists(timestamp, sender)
                        if (!exists) {
                            var matchedAny = false
                            for (rule in rules) {
                                if (SmsParser.matches(body, rule.keywords, rule.senderFilter)) {
                                    val amount = SmsParser.extractAmount(body, rule.amountKeyword)
                                    val txId = SmsParser.extractTxId(body)
                                    
                                    val entry = HistoryEntry(
                                        timestamp = timestamp,
                                        smsText = body,
                                        status = QueueStatus.PENDING,
                                        amount = amount,
                                        sender = sender,
                                        txId = txId,
                                        ruleName = rule.name
                                    )
                                    historyDao.insert(entry)
                                    matchedAny = true
                                    newSmsCount++
                                }
                            }
                        }
                    }
                    
                    if (newSmsCount > 0) {
                        Log.d("Payflow", "$newSmsCount SMS manques importes de l'historique")
                        triggerWorkManager()
                    }
                }
            } catch (e: Exception) {
                Log.e("Payflow", "Erreur lors du scan de l'historique des SMS", e)
            }
        }
    }

    private fun triggerWorkManager() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val workRequest = OneTimeWorkRequestBuilder<com.aurel.payflow.worker.SmsForwardWorker>()
            .setConstraints(constraints)
            .build()
            
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "SmsForwardWork",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun hasReadSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Payflow SMS Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Service de surveillance des SMS"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Payflow actif")
            .setContentText("Surveillance des SMS en cours...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
