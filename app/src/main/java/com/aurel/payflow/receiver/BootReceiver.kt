package com.aurel.payflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aurel.payflow.service.SmsForegroundService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, SmsForegroundService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
