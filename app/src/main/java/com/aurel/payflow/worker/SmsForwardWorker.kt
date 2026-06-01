package com.aurel.payflow.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aurel.payflow.PayflowApp
import com.aurel.payflow.data.HistoryEntry
import com.aurel.payflow.data.QueueStatus
import com.aurel.payflow.data.RoutingRule
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SmsForwardWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = PayflowApp.database
        val historyDao = db.historyDao()
        val ruleDao = db.ruleDao()

        val pendingEntries = historyDao.getPending()
        if (pendingEntries.isEmpty()) {
            return Result.success()
        }

        val rules = ruleDao.getAllRules()
        val rulesMap = rules.associateBy { it.name }

        val prefs = applicationContext.getSharedPreferences("PayflowPrefs", Context.MODE_PRIVATE)
        val globalWebhook = prefs.getString("webhook_url", "") ?: ""
        val globalKey = prefs.getString("global_key", "") ?: ""

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        var hasTemporaryError = false

        for (entry in pendingEntries) {
            val rule = rulesMap[entry.ruleName]
            
            val webhookUrl = if (rule != null && rule.webhookUrl.isNotEmpty()) rule.webhookUrl else globalWebhook
            val accessKey = if (rule != null && rule.accessKey.isNotEmpty()) rule.accessKey else globalKey

            if (webhookUrl.isEmpty()) {
                val updated = entry.copy(status = QueueStatus.FAILED, errorMessage = "Webhook URL non configuree")
                historyDao.update(updated)
                continue
            }

            val json = JSONObject().apply {
                put("sms_text", entry.smsText)
            }
            val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val requestBuilder = Request.Builder()
                .url(webhookUrl)
                .post(requestBody)
            
            if (accessKey.isNotEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $accessKey")
            }

            val request = requestBuilder.build()

            try {
                client.newCall(request).execute().use { response ->
                    val code = response.code
                    if (response.isSuccessful || code == 409) {
                        val updated = entry.copy(status = QueueStatus.SUCCESS, errorMessage = null)
                        historyDao.update(updated)
                    } else if (code == 400 || code == 401) {
                        val updated = entry.copy(status = QueueStatus.FAILED, errorMessage = "Erreur HTTP $code")
                        historyDao.update(updated)
                    } else {
                        val updated = entry.copy(errorMessage = "Erreur HTTP $code")
                        historyDao.update(updated)
                        hasTemporaryError = true
                    }
                }
            } catch (e: Exception) {
                Log.e("Payflow", "Erreur réseau lors de l'envoi du SMS", e)
                val updated = entry.copy(errorMessage = e.message ?: "Erreur de connexion")
                historyDao.update(updated)
                hasTemporaryError = true
            }
        }

        return if (hasTemporaryError) {
            Result.retry()
        } else {
            Result.success()
        }
    }
}
