package com.aurel.payflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            val prefs = context.getSharedPreferences("PayflowPrefs", Context.MODE_PRIVATE)
            val monitorActive = prefs.getBoolean("monitor_active", true)
            val startDateStr = prefs.getString("start_date", "") ?: ""

            if (!monitorActive) {
                Log.d("Payflow", "SMS ignore : la surveillance est inactive")
                return
            }

            var limitTimestamp = 0L
            if (startDateStr.isNotEmpty()) {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    val date = sdf.parse(startDateStr)
                    if (date != null) {
                        limitTimestamp = date.time
                    }
                } catch (e: Exception) {
                    Log.e("Payflow", "Erreur parsing date de depart : $startDateStr", e)
                }
            }

            for (sms in messages) {
                val sender = sms.displayOriginatingAddress ?: "Inconnu"
                val body = sms.displayMessageBody ?: ""
                val timestamp = sms.timestampMillis

                if (timestamp < limitTimestamp) {
                    Log.d("Payflow", "SMS ignore : recu avant la date limite ($timestamp < $limitTimestamp)")
                    continue
                }

                Log.d("Payflow", "SMS recu - De: $sender | Message: $body")

                val serviceIntent = Intent(context, com.aurel.payflow.service.SmsForegroundService::class.java).apply {
                    action = "NEW_SMS"
                    putExtra("sender", sender)
                    putExtra("body", body)
                    putExtra("timestamp", timestamp)
                }
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
