package com.itis.ocrapp.utils

object TextProcessingUtils {
    fun extractValue(line: String, keywords: List<String>, nextLine: String? = null): String? {
        var value = line
        keywords.forEach { keyword ->
            value = value.replace(keyword, "", ignoreCase = true).trim()
        }
        return if (value.isNotBlank() && value.length > 2 && !keywords.any { value.lowercase().contains(it) }) {
            value
        } else {
            nextLine?.takeIf { it.isNotBlank() && it.length > 2 }
        }
    }

    fun extractDate(text: String): String? {
        val normalizedText = text.replace("o", "0", ignoreCase = true)
        val datePattern = Regex("\\b(\\d{1,2})[\\s/|.-]*(\\d{1,2})[\\s/|.-]*(\\d{4})\\b")
        return datePattern.find(normalizedText)?.let {
            val day = it.groupValues[1].padStart(2, '0')
            val month = it.groupValues[2].padStart(2, '0')
            val year = it.groupValues[3]
            if (isValidDate(day.toIntOrNull(), month.toIntOrNull(), year.toIntOrNull())) {
                "$day/$month/$year"
            } else {
                null
            }
        }
    }

    private fun isValidDate(day: Int?, month: Int?, year: Int?): Boolean {
        if (day == null || month == null || year == null) return false
        if (month < 1 || month > 12) return false
        if (day < 1 || day > 31) return false
        if (year < 1900 || year > 9999) return false
        val maxDays = when (month) {
            4, 6, 9, 11 -> 30
            2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
            else -> 31
        }
        return day <= maxDays
    }

    fun extractSex(text: String): String? {
        val normalizedText = text.lowercase()
        return when {
            normalizedText.contains("nu") || normalizedText.contains("nữ") || normalizedText.contains("f") -> "Nữ"
            normalizedText.contains("nam") || normalizedText.contains("m") -> "Nam"
            else -> null
        }
    }

    fun extractDocumentNumber(text: String, documentType: String): String? {
        return when (documentType) {
            "passport" -> {
                val passportPattern = Regex("[A-Z]\\d{7}")
                passportPattern.find(text)?.value
            }
            "citizen_id" -> {
                val idPattern = Regex("\\b\\d{9}(\\d{3})?\\b")
                idPattern.find(text)?.value
            }
            else -> null
        }
    }

    fun extractIdCardNumber(text: String): String? {
        val idCardPattern = Regex("\\b\\d{9}(\\d{3})?\\b")
        return idCardPattern.find(text)?.value
    }

    fun extractLicenseClass(text: String): String? {
        val classPattern = Regex("\\b[A-F]\\d?\\b")
        return classPattern.find(text)?.value
    }

    fun String.containsAny(keywords: List<String>): Boolean {
        val lowerCase = this.lowercase()
        return keywords.any { lowerCase.contains(it) }
    }
}