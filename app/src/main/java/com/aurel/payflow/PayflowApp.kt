package com.aurel.payflow

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.aurel.payflow.data.PayflowDatabase

class PayflowApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Payflow SMS Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Service de surveillance des SMS en arriere-plan"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "payflow_sms_channel"

        @Volatile
        lateinit var instance: PayflowApp
            private set

        val database: PayflowDatabase by lazy {
            PayflowDatabase.getDatabase(instance)
        }
    }
}
