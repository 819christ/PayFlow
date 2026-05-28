package com.aurel.payflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.aurel.payflow.service.SmsForegroundService

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val sender = sms.displayOriginatingAddress
                val body = sms.displayMessageBody
                val timestamp = sms.timestampMillis

                Log.d("Payflow", "SMS reçu de $sender : $body")

                // On va notifier le service
                val serviceIntent = Intent(context, SmsForegroundService::class.java).apply {
                    putExtra("sender", sender)
                    putExtra("body", body)
                    putExtra("timestamp", timestamp)
                }
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
