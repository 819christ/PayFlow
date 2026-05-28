package com.aurel.payflow.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.util.Log

class SmsForegroundService : Service() {

    private val CHANNEL_ID = "payflow_sms_channel"
    private val NOTIFICATION_ID = 1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        Log.d("Payflow", "🚀 Service Payflow démarré en foreground")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (it.getStringExtra("action") == "NEW_SMS") {
                val sender = it.getStringExtra("sender") ?: "Inconnu"
                val body = it.getStringExtra("body") ?: ""
                
                Log.d("Payflow", "📤 SMS traité → De: $sender | Message: $body")
                
                // Pour l'instant on affiche juste (on ajoutera le vrai routage plus tard)
            }
        }
        return START_STICKY
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
