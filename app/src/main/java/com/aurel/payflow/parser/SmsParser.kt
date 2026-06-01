package com.aurel.payflow.parser

import java.text.Normalizer
import java.util.regex.Pattern

object SmsParser {

    private fun String.normalizeText(): String {
        val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
        val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        return pattern.matcher(temp).replaceAll("").lowercase()
    }

    fun matches(smsBody: String, keywords: String, senderFilter: String): Boolean {
        val bodyNorm = smsBody.normalizeText()
        
        val kwOk = if (keywords.isNotEmpty()) {
            keywords.split(Regex("[.,;]"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .all { bodyNorm.contains(it.normalizeText()) }
        } else {
            true
        }

        val sfOk = if (senderFilter.isNotEmpty()) {
            bodyNorm.contains(senderFilter.normalizeText())
        } else {
            true
        }

        return kwOk && sfOk
    }

    fun extractAmount(body: String, amountKeyword: String): Int {
        if (amountKeyword.isEmpty()) return 0
        val bodyNorm = body.normalizeText()
        val kwNorm = amountKeyword.normalizeText()
        val index = bodyNorm.indexOf(kwNorm)
        if (index != -1) {
            val after = body.substring(index + amountKeyword.length)
            val matcher = Pattern.compile("\\d+([\\s.,]\\d+)*").matcher(after)
            if (matcher.find()) {
                val numStr = matcher.group()
                val cleaned = numStr.replace(Regex("[^0-9]"), "")
                return cleaned.toIntOrNull() ?: 0
            }
        }
        return 0
    }

    fun extractTxId(body: String): String {
        val patterns = listOf(
            Pattern.compile("(?i)id\\s*:\\s*([A-Za-z0-9]+)"),
            Pattern.compile("(?i)ref\\s*:\\s*([A-Za-z0-9]+)"),
            Pattern.compile("(?i)reference\\s*:\\s*([A-Za-z0-9]+)"),
            Pattern.compile("(?i)id transaction\\s*:\\s*([A-Za-z0-9]+)")
        )
        for (pattern in patterns) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                return matcher.group(1) ?: ""
            }
        }
        return ""
    }
}
