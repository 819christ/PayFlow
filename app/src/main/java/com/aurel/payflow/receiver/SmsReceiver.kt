package com.aurel.payflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            for (sms in messages) {
                val sender = sms.displayOriginatingAddress ?: "Inconnu"
                val body = sms.displayMessageBody ?: ""
                val timestamp = sms.timestampMillis

                Log.d("Payflow", "📩 SMS reçu → De: $sender | Message: $body")

                // Transmission au service
                val serviceIntent = Intent(context, com.aurel.payflow.service.SmsForegroundService::class.java).apply {
                    putExtra("action", "NEW_SMS")
                    putExtra("sender", sender)
                    putExtra("body", body)
                    putExtra("timestamp", timestamp)
                }
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
